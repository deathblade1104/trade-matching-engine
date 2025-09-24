import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { DataSource, EntityManager } from 'typeorm';
import { OrderStatusHistory } from '../entities/order-status-history.entity';
import { Order } from '../entities/order.entity';
import { OrderSide, OrderStatus, OrderStatusActor } from '../enums/order.enum';
import { OrderStatusHistoryHelper } from './order-status-history.helper';
import { OrderHelper } from './order.helper';
import { TradesHelper } from './trades.helper';

const MAX_COUNTER_ORDERS = 200;

@Injectable()
export class OrderMatchingHelper {
  private readonly logger = new Logger(OrderMatchingHelper.name);

  constructor(
    private readonly dataSource: DataSource,
    private readonly orderHelper: OrderHelper,
    private readonly tradesHelper: TradesHelper,
    private readonly statusHistoryHelper: OrderStatusHistoryHelper,
  ) {}

  /**
   * Entry point: match an order by id
   * Returns updated persisted order
   */
  async matchOrder(orderId: number): Promise<Order> {
    this.logger.debug(`Matching order [id=${orderId}]`);

    const order = await this.orderHelper.findById(orderId);
    if (!order) {
      throw new NotFoundException(`Order ${orderId} not found`);
    }

    if (order.status === OrderStatus.FILLED) {
      this.logger.debug(`Order ${order.id} already filled, skipping`);
      return order;
    }

    let offset = 0;
    let processedCount = 0;

    while (parseFloat(order.remaining) > 0) {
      await this.dataSource.transaction(async (manager) => {
        const chunkResult = await this.processMatchingChunk(
          order,
          manager,
          offset,
        );
        processedCount = chunkResult.processedCount;
      });

      if (
        parseFloat(order.remaining) <= 0 ||
        processedCount < MAX_COUNTER_ORDERS
      ) {
        break;
      }

      offset += processedCount;
    }

    return order;
  }

  /**
   * Process one chunk of counter orders
   */
  private async processMatchingChunk(
    order: Order,
    manager: EntityManager,
    offset: number,
  ): Promise<{ processedCount: number }> {
    const counterSide =
      order.side === OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;

    const counterOrders = await this.orderHelper.findActiveOrdersPaginated(
      counterSide,
      MAX_COUNTER_ORDERS,
      offset,
    );

    const ordersToUpdate: Order[] = [order];

    const tradesToInsert: {
      buy_order_id: number;
      sell_order_id: number;
      price: string;
      quantity: string;
    }[] = [];

    const statusLogs: Partial<OrderStatusHistory>[] = [];

    let processedCount = 0;

    for (const counterOrder of counterOrders) {
      if (parseFloat(order.remaining) <= 0) break;
      if (!this.isPriceMatch(order, counterOrder)) break;

      const { tradeQty, tradePrice } = this.calculateTrade(order, counterOrder);

      const prevOrderStatus = order.status;
      const prevCounterStatus = counterOrder.status;

      this.applyTradeEffects(order, counterOrder, tradeQty);

      ordersToUpdate.push(counterOrder);
      tradesToInsert.push(
        this.buildTrade(order, counterOrder, tradePrice, tradeQty),
      );

      // log status changes only if changed
      this.trackStatusChange(
        statusLogs,
        order,
        prevOrderStatus,
        OrderStatusActor.SYSTEM,
      );

      this.trackStatusChange(
        statusLogs,
        counterOrder,
        prevCounterStatus,
        OrderStatusActor.SYSTEM,
      );

      processedCount++;

      this.logger.debug(
        `Trade executed: ${tradeQty} @ ${tradePrice} | Orders ${order.id} <-> ${counterOrder.id}`,
      );
    }

    await this.flushWrites(ordersToUpdate, tradesToInsert, statusLogs, manager);

    return { processedCount };
  }

  private isPriceMatch(order: Order, counterOrder: Order): boolean {
    if (
      order.side === OrderSide.BUY &&
      parseFloat(order.price) < parseFloat(counterOrder.price)
    ) {
      return false;
    }
    if (
      order.side === OrderSide.SELL &&
      parseFloat(order.price) > parseFloat(counterOrder.price)
    ) {
      return false;
    }
    return true;
  }

  private calculateTrade(newOrder: Order, counterOrder: Order) {
    const tradeQty = Math.min(
      parseFloat(newOrder.remaining),
      parseFloat(counterOrder.remaining),
    ).toString();
    const tradePrice = counterOrder.price;
    return { tradeQty, tradePrice };
  }

  private applyTradeEffects(
    newOrder: Order,
    counterOrder: Order,
    tradeQty: string,
  ) {
    newOrder.remaining = (
      parseFloat(newOrder.remaining) - parseFloat(tradeQty)
    ).toString();
    counterOrder.remaining = (
      parseFloat(counterOrder.remaining) - parseFloat(tradeQty)
    ).toString();

    newOrder.status =
      parseFloat(newOrder.remaining) === 0
        ? OrderStatus.FILLED
        : OrderStatus.PARTIAL;

    counterOrder.status =
      parseFloat(counterOrder.remaining) === 0
        ? OrderStatus.FILLED
        : OrderStatus.PARTIAL;
  }

  private buildTrade(
    newOrder: Order,
    counterOrder: Order,
    tradePrice: string,
    tradeQty: string,
  ): {
    buy_order_id: number;
    sell_order_id: number;
    price: string;
    quantity: string;
  } {
    return {
      buy_order_id:
        newOrder.side === OrderSide.BUY ? newOrder.id : counterOrder.id,
      sell_order_id:
        newOrder.side === OrderSide.SELL ? newOrder.id : counterOrder.id,
      price: tradePrice,
      quantity: tradeQty,
    };
  }

  private async flushWrites(
    ordersToUpdate: Order[],
    tradesToInsert: {
      buy_order_id: number;
      sell_order_id: number;
      price: string;
      quantity: string;
    }[],
    statusLogs: Partial<OrderStatusHistory>[],
    manager: EntityManager,
  ) {
    if (ordersToUpdate.length > 0) {
      await this.orderHelper.saveMany(ordersToUpdate, manager);
    }

    if (tradesToInsert.length > 0) {
      await this.tradesHelper.createMany(tradesToInsert, manager);
    }

    if (statusLogs.length > 0) {
      await this.statusHistoryHelper.logMany(statusLogs, manager);
    }
  }

  private trackStatusChange(
    logs: Partial<OrderStatusHistory>[],
    order: Order,
    prevStatus: OrderStatus,
    actor: OrderStatusActor,
  ): void {
    if (order.status === prevStatus) return;
    logs.push({ order_id: order.id, status: order.status, actor });
    return;
  }
}

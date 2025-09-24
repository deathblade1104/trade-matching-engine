import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { DataSource, EntityManager } from 'typeorm';
import { Order } from '../entities/order.entity';
import { Trade } from '../entities/trades.entity';
import { OrderSide, OrderStatus } from '../enums/order.enum';
import { OrderHelper } from './order.helper';
import { TradesHelper } from './trades.helper';

const MAX_COUNTER_ORDERS = 200; // 👈 cap per chunk

@Injectable()
export class OrderMatchingHelper {
  private readonly logger = new Logger(OrderMatchingHelper.name);

  constructor(
    private readonly dataSource: DataSource,
    private readonly orderHelper: OrderHelper,
    private readonly tradesHelper: TradesHelper,
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

      // stop if:
      // 1. order filled, or
      // 2. price check failed (processMatchingChunk will exit early), or
      // 3. processed too many in this run
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
    const tradesToInsert: Partial<Trade>[] = [];
    let processedCount = 0;

    for (const counterOrder of counterOrders) {
      if (parseFloat(order.remaining) <= 0) break;
      if (!this.isPriceMatch(order, counterOrder)) break;

      const { tradeQty, tradePrice } = this.calculateTrade(order, counterOrder);

      this.applyTradeEffects(order, counterOrder, tradeQty);

      ordersToUpdate.push(counterOrder);
      tradesToInsert.push(
        this.buildTrade(order, counterOrder, tradePrice, tradeQty),
      );

      processedCount++;

      this.logger.debug(
        `Trade executed: ${tradeQty} @ ${tradePrice} | Orders ${order.id} <-> ${counterOrder.id}`,
      );
    }

    await this.flushWrites(ordersToUpdate, tradesToInsert, manager);

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
  ) {
    return {
      buy_order: newOrder.side === OrderSide.BUY ? newOrder : counterOrder,
      sell_order: newOrder.side === OrderSide.SELL ? newOrder : counterOrder,
      price: tradePrice,
      quantity: tradeQty,
    };
  }

  private async flushWrites(
    ordersToUpdate: Order[],
    tradesToInsert: Partial<Trade>[],
    manager: EntityManager,
  ) {
    if (ordersToUpdate.length > 0) {
      await this.orderHelper.saveMany(ordersToUpdate, manager);
    }

    if (tradesToInsert.length > 0) {
      await this.tradesHelper.createMany(tradesToInsert, manager);
    }
  }
}

import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, Repository } from 'typeorm';
import {
  PaginatedResponseDto,
  PaginationQueryDto,
} from '../../../common/dtos/pagination.dto';
import { GenericCrudRepository } from '../../../database/postgres/repository/generic-crud.repository';
import { BullQueueService } from '../../../providers/infra/bullmq/bullmq.service';
import { CreateOrderDto } from '../dtos/create-order.dto';
import {
  OrderbookLevelDto,
  OrderResponseDto,
  OrderStatusHistoryResponseDto,
} from '../dtos/order-response.dto';
import { OrderStatusHistory } from '../entities/order-status-history.entity';
import { Order } from '../entities/order.entity';
import {
  OrderJobNameEnum,
  OrderSide,
  OrderStatusActor,
  OrderTaskQueueEnum,
} from '../enums/order.enum';
import { OrderStatusHistoryHelper } from '../helpers/order-status-history.helper';
import { OrderHelper } from '../helpers/order.helper';
import { JobOptionsMap } from '../order.constants';
import { IOrderExpiryData } from '../processors/order-expiry.processor';
import { IOrderMatchingData } from '../processors/order-matching.processor';

@Injectable()
export class OrdersService {
  private readonly orderRepository: GenericCrudRepository<Order>;

  constructor(
    @InjectRepository(Order)
    private readonly orderRepo: Repository<Order>,
    private readonly orderHelper: OrderHelper,
    private readonly orderStatusHistoryHelper: OrderStatusHistoryHelper,
    private readonly bullQueueService: BullQueueService,
    private readonly dataSource: DataSource,
  ) {
    this.orderRepository = new GenericCrudRepository(orderRepo, Order.name);
  }

  async createOrderTransactional(
    dto: CreateOrderDto,
    userId: number,
  ): Promise<Order> {
    return await this.dataSource.transaction(
      async (manager): Promise<Order> => {
        const newOrder = await this.orderHelper.createOrder(
          {
            side: dto.side,
            price: dto.price.toString(),
            quantity: dto.quantity.toString(),
            remaining: dto.quantity.toString(),
            validity_days: dto.validity_days,
            user_id: userId,
          },
          manager,
        );
        const newStatusHistory = await this.orderStatusHistoryHelper.logStatus(
          {
            order_id: newOrder.id,
            status: newOrder.status,
            actor: OrderStatusActor.USER,
          },
          manager,
        );
        return { ...newOrder, status_history: [newStatusHistory] };
      },
    );
  }

  /**
   * Place a new order → persist,  enqueue for matching, return
   */
  async createOrder(
    dto: CreateOrderDto,
    userId: number,
  ): Promise<OrderResponseDto> {
    const newOrder = await this.createOrderTransactional(dto, userId);
    const scheduleTimeForExpiry = newOrder.validity_days * 24 * 60 * 60 * 1000;

    //add matching and expiry jobs for orders in queue
    await Promise.all([
      this.bullQueueService.addJob<IOrderMatchingData>(
        OrderTaskQueueEnum.PROCESS_ORDER,
        OrderJobNameEnum.PROCESS_ORDER,
        { order_id: newOrder.id, reprocess_count: 0 },
        JobOptionsMap[OrderJobNameEnum.PROCESS_ORDER],
      ),
      this.bullQueueService.addJob<IOrderExpiryData>(
        OrderTaskQueueEnum.EXPIRE_ORDER,
        OrderJobNameEnum.EXPIRE_ORDER,
        { order_id: newOrder.id },
        {
          ...JobOptionsMap[OrderJobNameEnum.EXPIRE_ORDER],
          delay: scheduleTimeForExpiry,
        },
      ),
    ]);

    return this.toOrderResponseDto(newOrder);
  }

  /**
   * Fetch a single order with its history (DESC)
   */
  async getOrderById(
    orderId: number,
    userId: number,
  ): Promise<OrderResponseDto> {
    const order = await this.orderHelper.findById(orderId, {
      relations: ['status_history'],
    });

    if (!order) {
      throw new NotFoundException('Order not found');
    }

    if (order.user_id !== userId) {
      throw new ForbiddenException('You are not allowed to access this order');
    }

    return this.toOrderResponseDto(order);
  }

  /**
   * Get current orderbook (active orders only, grouped by price)
   */
  async getOrderbook(
    orderSide: string,
    pagination: PaginationQueryDto,
  ): Promise<PaginatedResponseDto<OrderbookLevelDto>> {
    if (!Object.values(OrderSide).includes(orderSide as OrderSide)) {
      throw new BadRequestException(
        `side param should be in [${Object.values(OrderSide).join(',')}]. `,
      );
    }

    const side = orderSide as OrderSide;

    const { page, limit } = pagination;
    const offset = (page - 1) * limit;
    const query = this.orderRepository
      .createQueryBuilder('o')
      .leftJoinAndSelect('o.user', 'u')
      .select([
        'o.id AS order_id',
        'o.price AS price',
        'o.created_at AS created_at',
        'o.remaining AS remaining',
        'o.status AS status',
        'u.name AS user_name',
      ])
      .where('o.side = :side', { side })
      .andWhere("o.status IN ('OPEN','PARTIAL')");

    if (side === OrderSide.BUY) {
      query.orderBy('o.price', 'DESC');
    } else {
      query.orderBy('o.price', 'ASC');
    }

    const orders = (await query.offset(offset).limit(limit).getRawMany()) || [];

    return {
      total: orders.length,
      page,
      limit,
      items:
        orders.length == 0
          ? []
          : orders.map((o) => ({
              price: o.price,
              remaining: parseFloat(o.remaining),
              status: o.status,
              user_name: o.user_name,
              created_at: o.created_at.toISOString(),
              side,
            })),
    };
  }

  async getOrdersByUserId(
    userId: number,
    pagination: PaginationQueryDto,
  ): Promise<PaginatedResponseDto<OrderResponseDto>> {
    const { page, limit } = pagination;
    const skip = (page - 1) * limit;

    const { items: orders, total } = await this.orderRepository.findAllAndCount(
      {
        where: {
          user: { id: userId },
        },
        order: {
          created_at: 'DESC',
        },
        skip,
        take: limit,
      },
    );

    return {
      total,
      page,
      limit,
      items: orders.map((o) => this.toOrderResponseDto(o)),
    };
  }
  private toOrderResponseDto(order: Order): OrderResponseDto {
    // Segregate user data from order data using spread operator
    const { status_history: statusHistoryArr, ...orderData } = order;

    // Return only order data with formatted timestamp
    const resp: OrderResponseDto = {
      ...orderData,
      created_at: order.created_at.toISOString(),
      updated_at: order.updated_at.toISOString(),
    };

    if (Array.isArray(statusHistoryArr) && statusHistoryArr.length > 0) {
      statusHistoryArr.sort(
        (a, b) => b.created_at.getTime() - a.created_at.getTime(),
      );
      resp.status_history = statusHistoryArr.map((history) =>
        this.toOrderStatusHistoryResponseDto(history),
      );
    }

    return resp;
  }

  private toOrderStatusHistoryResponseDto(
    orderStatusHistory: OrderStatusHistory,
  ): OrderStatusHistoryResponseDto {
    return {
      status: orderStatusHistory.status,
      actor: orderStatusHistory.actor,
      created_at: orderStatusHistory.created_at.toISOString(),
    };
  }
}

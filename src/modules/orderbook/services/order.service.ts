import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
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
} from '../dtos/order-response.dto';
import { Order } from '../entities/order.entity';
import {
  OrderJobNameEnum,
  OrderStatus,
  OrderTaskQueueEnum,
} from '../enums/order.enum';
import { OrderHelper } from '../helpers/order.helper';
import { JobOptionsMap } from '../order.constants';
import { IOrderMatchingData } from '../processors/order-matching.processor';

@Injectable()
export class OrdersService {
  private readonly orderRepository: GenericCrudRepository<Order>;

  constructor(
    @InjectRepository(Order)
    private readonly orderRepo: Repository<Order>,
    private readonly orderHelper: OrderHelper,
    private readonly bullQueueService: BullQueueService,
  ) {
    this.orderRepository = new GenericCrudRepository(orderRepo, Order.name);
  }

  /**
   * Place a new order → persist,  enqueue for matching, return
   */
  async createOrder(
    dto: CreateOrderDto,
    userId: number,
  ): Promise<OrderResponseDto> {
    const newOrder = await this.orderHelper.createOrder(
      dto.side,
      dto.price.toString(),
      dto.quantity.toString(),
      userId,
    );

    await this.bullQueueService.addJob<IOrderMatchingData>(
      OrderTaskQueueEnum.PROCESS_ORDER,
      OrderJobNameEnum.PROCESS_ORDER,
      { order_id: newOrder.id, reprocess_count: 0 },
      JobOptionsMap[OrderJobNameEnum.PROCESS_ORDER],
    );

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
      relations: ['user'],
    });

    if (!order) {
      throw new NotFoundException('Order not found');
    }

    if (order.user.id !== userId) {
      throw new ForbiddenException('You are not allowed to access this order');
    }

    return this.toOrderResponseDto(order);
  }

  /**
   * Get current orderbook (active orders only, grouped by price)
   */
  async getOrderbook(
    pagination: PaginationQueryDto,
  ): Promise<PaginatedResponseDto<OrderbookLevelDto>> {
    const { page, limit } = pagination;
    const offset = (page - 1) * limit;

    // BUY side
    const buys = await this.orderRepo
      .createQueryBuilder('o')
      .select(['o.price AS price', 'SUM(o.remaining)::float AS total'])
      .where('o.side = :side', { side: 'BUY' })
      .andWhere("o.status IN ('OPEN','PARTIAL')")
      .groupBy('o.price')
      .orderBy('o.price', 'DESC')
      .offset(offset)
      .limit(limit)
      .getRawMany();

    // SELL side
    const sells = await this.orderRepo
      .createQueryBuilder('o')
      .select(['o.price AS price', 'SUM(o.remaining)::float AS total'])
      .where('o.side = :side', { side: 'SELL' })
      .andWhere("o.status IN ('OPEN','PARTIAL')")
      .groupBy('o.price')
      .orderBy('o.price', 'ASC')
      .offset(offset)
      .limit(limit)
      .getRawMany();

    return {
      total: buys.length + sells.length, // crude count
      page,
      limit,
      items: [
        ...buys.map((b) => ({ price: b.price, remaining: b.total })),
        ...sells.map((s) => ({ price: s.price, remaining: s.total })),
      ],
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
          status: In([OrderStatus.OPEN, OrderStatus.PARTIAL]),
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
    const { user, ...orderData } = order;

    // Return only order data with formatted timestamp
    const resp: OrderResponseDto = {
      ...orderData,
      created_at: order.created_at.toISOString(),
      updated_at: order.updated_at.toISOString(),
    };

    if (user && user.id) {
      resp.user_id = user.id;
    }

    return resp;
  }
}

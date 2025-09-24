import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, FindOneOptions, In, Repository } from 'typeorm';
import { GenericCrudRepository } from '../../../database/postgres/repository/generic-crud.repository';
import { Order } from '../entities/order.entity';
import { OrderSide, OrderStatus } from '../enums/order.enum';

@Injectable()
export class OrderHelper {
  private readonly orderRepository: GenericCrudRepository<Order>;

  constructor(
    @InjectRepository(Order)
    private readonly repo: Repository<Order>,
  ) {
    this.orderRepository = new GenericCrudRepository(repo, Order.name);
  }

  async createOrder(
    orderDto: Partial<Order>,
    manager?: EntityManager,
  ): Promise<Order> {
    return await this.orderRepository.create(
      {
        ...orderDto,
        status: OrderStatus.OPEN,
      },
      manager,
    );
  }

  async findById(
    orderId: number,
    options?: FindOneOptions<Order>,
  ): Promise<Order | null> {
    return await this.orderRepository.findOneOrNone({
      where: { id: orderId },
      ...(options || {}),
    });
  }

  /**
   * Fetch active orders in paginated batches (for large matching workloads)
   */
  async findActiveOrdersPaginated(
    side: OrderSide,
    limit: number,
    offset: number,
  ): Promise<Order[]> {
    return await this.orderRepository.findAll({
      where: { side, status: In([OrderStatus.OPEN, OrderStatus.PARTIAL]) },
      order: {
        price: side === OrderSide.BUY ? 'DESC' : 'ASC',
        created_at: 'ASC',
      },
      skip: offset,
      take: limit,
    });
  }

  async saveMany(orders: Order[], manager?: EntityManager): Promise<Order[]> {
    return await this.orderRepository.saveMany(orders, manager);
  }
}

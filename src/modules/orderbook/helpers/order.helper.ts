import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, FindOneOptions, In, Repository } from 'typeorm';
import { GenericCrudRepository } from '../../../database/postgres/repository/generic-crud.repository';
import { User } from '../../user/entities/user.entity';
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
    side: OrderSide,
    price: string,
    quantity: string,
    userId: number,
    manager?: EntityManager,
  ): Promise<Order> {
    return await this.orderRepository.create(
      {
        side,
        price,
        quantity,
        remaining: quantity,
        status: OrderStatus.OPEN,
        user: { id: userId } as Partial<User>,
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

import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import { GenericCrudRepository } from '../../../database/postgres/repository/generic-crud.repository';
import { OrderStatusHistory } from '../entities/order-status-history.entity';

@Injectable()
export class OrderStatusHistoryHelper {
  private readonly repository: GenericCrudRepository<OrderStatusHistory>;

  constructor(
    @InjectRepository(OrderStatusHistory)
    private readonly repo: Repository<OrderStatusHistory>,
  ) {
    this.repository = new GenericCrudRepository(repo, OrderStatusHistory.name);
  }

  /**
   * Log a new status change for an order
   */
  async logStatus(
    dto: Partial<OrderStatusHistory>,
    manager?: EntityManager,
  ): Promise<OrderStatusHistory> {
    return await this.repository.create(
      {
        order_id: dto.order_id,
        status: dto.status,
        actor: dto.actor,
      },
      manager,
    );
  }

  /**
   * Log multiple status changes in one batch (useful inside transactions)
   */
  async logMany(
    logs: Partial<OrderStatusHistory>[],
    manager?: EntityManager,
  ): Promise<OrderStatusHistory[]> {
    return await this.repository.createMany(
      logs.map((l) => ({
        order: l.order,
        status: l.status,
        actor: l.actor,
      })),
      manager,
    );
  }
}

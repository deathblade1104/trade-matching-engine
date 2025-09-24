import { Processor } from '@nestjs/bullmq';
import { Job } from 'bullmq';
import { DataSource } from 'typeorm';
import { GenericWorkerHost } from '../../../providers/infra/bullmq/generic/genericWorkerHost';
import { Order } from '../entities/order.entity';
import {
  OrderStatus,
  OrderStatusActor,
  OrderTaskQueueEnum,
} from '../enums/order.enum';
import { OrderStatusHistoryHelper } from '../helpers/order-status-history.helper';
import { OrderHelper } from '../helpers/order.helper';

export interface IOrderExpiryData {
  order_id: number;
}
@Processor(OrderTaskQueueEnum.EXPIRE_ORDER)
export class OrderExpiryProcessor extends GenericWorkerHost<
  IOrderExpiryData,
  void
> {
  constructor(
    private readonly orderHelper: OrderHelper,
    private readonly orderStatusHistoryHelper: OrderStatusHistoryHelper,
    private readonly dataSource: DataSource,
  ) {
    super(OrderTaskQueueEnum.EXPIRE_ORDER, OrderExpiryProcessor.name);
  }

  // Override getJobContext to provide ticket-specific context
  protected getJobContext(job: Job<IOrderExpiryData>): string {
    return ` for order with id: ${job.data?.order_id}`;
  }

  protected async processJob(job: Job<IOrderExpiryData>): Promise<void> {
    const { order_id: orderId } = job.data;

    if (!orderId) {
      this.logger.warn(`⚠️ No orderId for expiry in job ${job.id}`);
      return;
    }

    const order = await this.orderHelper.findById(orderId);

    if (order.status === OrderStatus.FILLED) return;
    await this.exprieOrderTransactional(order);
    return;
  }

  private async exprieOrderTransactional(order: Order): Promise<void> {
    return await this.dataSource.transaction(async (manager): Promise<void> => {
      order.status = OrderStatus.EXPIRED;
      await Promise.all([
        this.orderHelper.saveMany([order], manager),
        this.orderStatusHistoryHelper.logStatus(
          {
            status: OrderStatus.EXPIRED,
            order_id: order.id,
            actor: OrderStatusActor.SYSTEM,
          },
          manager,
        ),
      ]);
      return;
    });
  }
}

import { Processor } from '@nestjs/bullmq';
import { Job } from 'bullmq';
import { BullQueueService } from '../../../providers/infra/bullmq/bullmq.service';
import { GenericWorkerHost } from '../../../providers/infra/bullmq/generic/genericWorkerHost';
import { Order } from '../entities/order.entity';
import {
  OrderJobNameEnum,
  OrderStatus,
  OrderTaskQueueEnum,
} from '../enums/order.enum';
import { OrderMatchingHelper } from '../helpers/order-matching.helper';
import { JobOptionsMap } from '../order.constants';

export interface IOrderMatchingData {
  order_id: number;
  reprocess_count: number;
}
@Processor(OrderTaskQueueEnum.PROCESS_ORDER)
export class OrderMatchingProcessor extends GenericWorkerHost<
  IOrderMatchingData,
  void
> {
  private static MAX_REPROCESS_COUNT = 10;
  constructor(
    private readonly orderMatchingHelper: OrderMatchingHelper,
    private readonly bullQueueService: BullQueueService,
  ) {
    super(OrderTaskQueueEnum.PROCESS_ORDER, OrderMatchingProcessor.name);
  }

  // Override getJobContext to provide ticket-specific context
  protected getJobContext(job: Job<IOrderMatchingData>): string {
    return ` for order with id: ${job.data?.order_id}`;
  }

  protected async processJob(job: Job<IOrderMatchingData>): Promise<void> {
    const { order_id: orderId, reprocess_count: count } = job.data;

    if (count >= OrderMatchingProcessor.MAX_REPROCESS_COUNT) {
      this.logger.error(
        `❌ Max Reprocess Count reached for order with id : ${orderId}.
        No further procesing will happen for this.`,
      );
      return;
    }

    if (!orderId) {
      this.logger.warn(`⚠️ No orderId in job ${job.id}`);
      return;
    }

    const updatedOrder = await this.orderMatchingHelper.matchOrder(orderId);

    if (updatedOrder.status === OrderStatus.FILLED) {
      this.logger.log(`✅ Order ${orderId} fully matched.`);
      return;
    }

    if (parseFloat(updatedOrder.remaining) > 0) {
      await this.handlePartialFilledOrders(updatedOrder, count);
      return;
    }

    return;
  }

  private async handlePartialFilledOrders(updatedOrder: Order, count: number) {
    let delay = 0;

    // Adaptive delay based on what happened
    if (updatedOrder.status === OrderStatus.PARTIAL) {
      // Partial fill → short retry
      delay = Math.min(5 * 60 * 1000, 60 * 1000 * (count + 1)); // cap at 5 mins
    } else {
      // No fill at all → longer retry
      delay = Math.min(60 * 60 * 1000, 15 * 60 * 1000 * (count + 1)); // cap at 1 hour
    }

    this.logger.log(
      `♻️ Order ${updatedOrder.id} partially filled, re-enqueuing...`,
    );
    await this.bullQueueService.addJob(
      OrderTaskQueueEnum.PROCESS_ORDER,
      OrderJobNameEnum.PROCESS_ORDER,
      { orderId: updatedOrder.id, reprocess_count: count + 1 },
      {
        ...JobOptionsMap[OrderJobNameEnum.PROCESS_ORDER],
        delay: 60 * 60 * 1000, //retry after an hour
      },
    );
  }
}

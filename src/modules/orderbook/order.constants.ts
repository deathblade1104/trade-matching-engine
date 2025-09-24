import { JobsOptions } from 'bullmq';
import { OrderJobNameEnum } from './enums/order.enum';

export const JobOptionsMap: Record<OrderJobNameEnum, JobsOptions> = {
  [OrderJobNameEnum.PROCESS_ORDER]: {
    attempts: 3,
    delay: 20 * 1000, //Start after 20 seconds
    backoff: {
      type: 'exponential',
      delay: 5 * 1000, //Exponential backoff with 5 seconds delay
    },
    removeOnComplete: true,
    removeOnFail: false,
  },
};

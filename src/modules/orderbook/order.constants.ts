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
    removeOnFail: true, //DLQ not essentially required here.
  },

  [OrderJobNameEnum.EXPIRE_ORDER]: {
    attempts: 3,
    backoff: {
      type: 'exponential',
      delay: 5 * 1000,
    },
    delay: 60 * 24 * 60 * 60 * 1000, //Schedule expiry post 60 days.
    removeOnComplete: true,
    removeOnFail: false,
  },
};

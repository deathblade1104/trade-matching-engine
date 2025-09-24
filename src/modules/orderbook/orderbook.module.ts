import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { RedisCacheModule } from '../../database/redis/redis-cache.module';
import { BullMQModule } from '../../providers/infra/bullmq/bullmq.module';
import { OrdersController } from './controllers/order.controller';
import { TradesController } from './controllers/trades.controller';
import { Order } from './entities/order.entity';
import { Trade } from './entities/trades.entity';
import { OrderJobNameEnum, OrderTaskQueueEnum } from './enums/order.enum';
import { OrderMatchingHelper } from './helpers/order-matching.helper';
import { OrderHelper } from './helpers/order.helper';
import { TradesHelper } from './helpers/trades.helper';
import { JobOptionsMap } from './order.constants';
import { OrderMatchingProcessor } from './processors/order-matching.processor';
import { OrdersService } from './services/order.service';
import { TradesService } from './services/trade.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([Order, Trade]),
    RedisCacheModule,
    ConfigModule,
    BullMQModule.forFeature([
      {
        name: OrderTaskQueueEnum.PROCESS_ORDER,
        defaultJobOptions: JobOptionsMap[OrderJobNameEnum.PROCESS_ORDER],
      },
    ]),
  ],
  controllers: [OrdersController, TradesController],
  providers: [
    OrdersService,
    OrderHelper,
    TradesHelper,
    OrderMatchingHelper,
    OrderMatchingProcessor,
    TradesService,
  ],
})
export class OrderbookModule {}

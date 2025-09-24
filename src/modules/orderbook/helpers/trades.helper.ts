import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import { GenericCrudRepository } from '../../../database/postgres/repository/generic-crud.repository';
import { Trade } from '../entities/trades.entity';

@Injectable()
export class TradesHelper {
  private readonly tradeRepository: GenericCrudRepository<Trade>;

  constructor(
    @InjectRepository(Trade)
    private readonly repo: Repository<Trade>,
  ) {
    this.tradeRepository = new GenericCrudRepository(repo, Trade.name);
  }

  async createTrade(
    buyOrderId: number,
    sellOrderId: number,
    price: string,
    quantity: string,
    manager?: EntityManager,
  ): Promise<Trade> {
    return await this.tradeRepository.create(
      {
        buy_order_id: buyOrderId,
        sell_order_id: sellOrderId,
        price,
        quantity,
      },
      manager,
    );
  }

  async createMany(
    trades: {
      buy_order_id: number;
      sell_order_id: number;
      price: string;
      quantity: string;
    }[],
    manager?: EntityManager,
  ): Promise<Trade[]> {
    return await this.tradeRepository.createMany(trades, manager);
  }

  async findById(tradeId: number): Promise<Trade | null> {
    return await this.tradeRepository.findOneBy({
      where: { id: tradeId },
      relations: ['buy_order', 'sell_order'],
    });
  }

  async findTradesForOrder(orderId: number): Promise<Trade[]> {
    return await this.tradeRepository.findAll({
      where: [{ buy_order_id: orderId }, { sell_order_id: orderId }],
      relations: ['buy_order', 'sell_order'],
      order: { created_at: 'DESC' },
    });
  }
}

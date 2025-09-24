import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import {
  PaginatedResponseDto,
  PaginationQueryDto,
} from '../../../common/dtos/pagination.dto';
import { GenericCrudRepository } from '../../../database/postgres/repository/generic-crud.repository';
import { Trade } from '../entities/trades.entity';

@Injectable()
export class TradesService {
  private readonly tradeRepo: GenericCrudRepository<Trade>;

  constructor(
    @InjectRepository(Trade)
    private readonly repo: Repository<Trade>,
  ) {
    this.tradeRepo = new GenericCrudRepository(repo, Trade.name);
  }

  async getTradesByUserId(
    userId: number,
    pagination: PaginationQueryDto,
  ): Promise<PaginatedResponseDto<Trade>> {
    const { page, limit } = pagination;
    const skip = (page - 1) * limit;

    const { items, total } = await this.tradeRepo.findAllAndCount({
      where: [
        { buy_order: { user: { id: userId } } },
        { sell_order: { user: { id: userId } } },
      ],
      relations: ['buy_order', 'sell_order'],
      order: { created_at: 'DESC' },
      skip,
      take: limit,
    });

    return {
      total,
      page,
      limit,
      items,
    };
  }
}

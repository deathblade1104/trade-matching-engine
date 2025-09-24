import { Column, Entity, ManyToOne } from 'typeorm';
import { TableNames } from '../../../common/enums/entities.enums';
import { AbstractEntity } from '../../../database/postgres/abstract.entity';
import { Order } from './order.entity';

@Entity(TableNames.TRADES)
export class Trade extends AbstractEntity<Trade> {
  @ManyToOne(() => Order, { nullable: false })
  buy_order: Order;

  @ManyToOne(() => Order, { nullable: false })
  sell_order: Order;

  @Column('numeric', { precision: 18, scale: 8 })
  price: string;

  @Column('numeric', { precision: 18, scale: 8 })
  quantity: string;
}

import { Column, Entity, JoinColumn, ManyToOne } from 'typeorm';
import { TableNames } from '../../../common/enums/entities.enums';
import { AbstractEntity } from '../../../database/postgres/abstract.entity';
import { Order } from './order.entity';

@Entity(TableNames.TRADES)
export class Trade extends AbstractEntity<Trade> {
  @Column({ type: 'int' })
  buy_order_id: number;

  @Column({ type: 'int' })
  sell_order_id: number;

  @ManyToOne(() => Order, { nullable: false })
  @JoinColumn({ name: 'buy_order_id' })
  buy_order: Order;

  @ManyToOne(() => Order, { nullable: false })
  @JoinColumn({ name: 'sell_order_id' })
  sell_order: Order;

  @Column('numeric', { precision: 18, scale: 8 })
  price: string;

  @Column('numeric', { precision: 18, scale: 8 })
  quantity: string;
}

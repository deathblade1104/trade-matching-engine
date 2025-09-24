import {
  Column,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  OneToMany,
  UpdateDateColumn,
} from 'typeorm';
import { TableNames } from '../../../common/enums/entities.enums';
import { AbstractEntity } from '../../../database/postgres/abstract.entity';
import { User } from '../../user/entities/user.entity';
import { OrderSide, OrderStatus } from '../enums/order.enum';
import { OrderStatusHistory } from './order-status-history.entity';

@Entity(TableNames.ORDERS)
@Index('idx_side_status_price_createdat', [
  'side',
  'status',
  'price',
  'created_at',
])
@Index('idx_user_id', ['user'])
export class Order extends AbstractEntity<Order> {
  @Column()
  user_id: number;

  @Column({ type: 'enum', enum: OrderSide })
  side: OrderSide;

  @Column('numeric', { precision: 18, scale: 8 })
  price: string;

  @Column('numeric', { precision: 18, scale: 8 })
  quantity: string;

  @Column('numeric', { precision: 18, scale: 8 })
  remaining: string;

  @Column({ type: 'enum', enum: OrderStatus, default: OrderStatus.OPEN })
  status: OrderStatus;

  @Column({ type: 'int', default: 60 })
  validity_days: number;

  @UpdateDateColumn({ name: 'updated_at', default: () => 'CURRENT_TIMESTAMP' })
  updated_at: Date;

  @ManyToOne(() => User, (user) => user.orders, { eager: false })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @OneToMany(() => OrderStatusHistory, (history) => history.order, {
    cascade: true,
  })
  status_history: OrderStatusHistory[];
}

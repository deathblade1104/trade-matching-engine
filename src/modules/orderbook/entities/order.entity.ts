import { Column, Entity, Index, ManyToOne, UpdateDateColumn } from 'typeorm';
import { TableNames } from '../../../common/enums/entities.enums';
import { AbstractEntity } from '../../../database/postgres/abstract.entity';
import { User } from '../../user/entities/user.entity';
import { OrderSide, OrderStatus } from '../enums/order.enum';

@Entity(TableNames.ORDERS)
@Index('idx_side_status_price_createdat', [
  'side',
  'status',
  'price',
  'created_at',
])
@Index('idx_user_id', ['user'])
export class Order extends AbstractEntity<Order> {
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

  @UpdateDateColumn({ name: 'updated_at', default: () => 'CURRENT_TIMESTAMP' })
  updated_at: Date;

  @ManyToOne(() => User, (user) => user.orders, { eager: false })
  user: User;
}

import { Column, Entity, JoinColumn, ManyToOne } from 'typeorm';
import { TableNames } from '../../../common/enums/entities.enums';
import { AbstractEntity } from '../../../database/postgres/abstract.entity';
import { OrderStatus, OrderStatusActor } from '../enums/order.enum';
import { Order } from './order.entity';

@Entity(TableNames.ORDER_STATUS_HISTORY)
export class OrderStatusHistory extends AbstractEntity<OrderStatusHistory> {
  @Column({ type: 'int' })
  order_id: number;

  @Column({ type: 'enum', enum: OrderStatus })
  status: OrderStatus;

  @Column({ type: 'enum', enum: OrderStatusActor })
  actor: OrderStatusActor;

  @ManyToOne(() => Order, (order) => order.status_history, {
    nullable: false,
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'order_id' })
  order: Order;
}

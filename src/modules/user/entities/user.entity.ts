import { Column, Entity, Index, OneToMany } from 'typeorm';
import { TableNames } from '../../../common/enums/entities.enums';
import { AbstractEntity } from '../../../database/postgres/abstract.entity';
import { Order } from '../../orderbook/entities/order.entity';

@Entity(TableNames.USERS)
export class User extends AbstractEntity<User> {
  @Column()
  name: string;

  @Index('uq_users_email', { unique: true })
  @Column()
  email: string;

  @Column()
  password_hash: string;

  @OneToMany(() => Order, (order) => order.user)
  orders: Order[];
}

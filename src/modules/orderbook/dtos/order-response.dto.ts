import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { OrderSide, OrderStatus, OrderStatusActor } from '../enums/order.enum';

export class OrderStatusHistoryResponseDto {
  @ApiProperty()
  status: OrderStatus;

  @ApiProperty()
  actor: OrderStatusActor;

  @ApiProperty()
  created_at: string;
}
export class OrderResponseDto {
  @ApiProperty()
  id: number;

  @ApiProperty({ enum: OrderSide })
  side: OrderSide;

  @ApiProperty()
  price: string;

  @ApiProperty()
  quantity: string;

  @ApiProperty()
  remaining: string;

  @ApiProperty({ enum: OrderStatus })
  status: OrderStatus;

  @ApiProperty()
  created_at: string;

  @ApiProperty()
  updated_at: string;

  @ApiPropertyOptional()
  user_id: number;

  @ApiPropertyOptional()
  status_history?: OrderStatusHistoryResponseDto[];
}

export class OrderbookLevelDto {
  @ApiProperty()
  price: string;

  @ApiProperty()
  remaining: number;

  @ApiProperty()
  user_name: string;

  @ApiProperty()
  status: OrderStatus;

  @ApiProperty()
  side: OrderSide;

  @ApiProperty()
  created_at: string;
}

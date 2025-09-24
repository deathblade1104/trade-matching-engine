import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsEnum,
  IsInt,
  IsNumber,
  IsOptional,
  IsPositive,
  Max,
  Min,
} from 'class-validator';
import { OrderSide } from '../enums/order.enum';

export class CreateOrderDto {
  @ApiProperty({
    description: 'Order side - whether this is a buy or sell order',
    enum: OrderSide,
    example: OrderSide.BUY,
    enumName: 'OrderSide',
  })
  @IsEnum(OrderSide)
  side: OrderSide;

  @ApiProperty({
    description: 'Price per unit for the order',
    type: 'number',
    example: 100.5,
    minimum: 0.01,
  })
  @IsNumber()
  @IsPositive()
  price: number;

  @ApiProperty({
    description: 'Quantity of units to buy or sell',
    type: 'number',
    example: 10,
    minimum: 0.01,
  })
  @IsNumber()
  @IsPositive()
  quantity: number;

  @ApiPropertyOptional({
    description:
      'Max number of days to keep the order active before expiry (1–60). Defaults to 60 when omitted.',
    type: 'integer',
    example: 10,
    minimum: 1,
    maximum: 60,
  })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(60)
  validity_days?: number = 60;
}

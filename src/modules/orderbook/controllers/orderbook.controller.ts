import {
  Controller,
  Get,
  HttpCode,
  Param,
  Query,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { PaginationQueryDto } from '../../../common/dtos/pagination.dto';
import { JwtAuthGuard } from '../../../common/guards/jwt-auth.guard';
import { OrdersService } from '../services/order.service';

@Controller({ path: 'orderbook', version: '1' })
@ApiTags('OrderBook API')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
export class OrderBookController {
  constructor(private readonly ordersService: OrdersService) {}

  @Get(':side')
  @HttpCode(200)
  @ApiOperation({
    summary: 'Get current orderbook',
    description:
      'Retrieve the current state of the orderbook showing all active buy and sell orders grouped by price level.',
  })
  @ApiParam({
    name: 'side',
    enum: ['BUY', 'SELL'],
    description: 'Order side to retrieve from orderbook',
    example: 'BUY',
  })
  @ApiQuery({
    name: 'page',
    type: 'number',
    required: false,
    description: 'Page number for pagination',
    example: 1,
  })
  @ApiQuery({
    name: 'limit',
    type: 'number',
    required: false,
    description: 'Number of items per page',
    example: 10,
  })
  @ApiResponse({
    status: 200,
    description: 'Orderbook retrieved successfully',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Orderbook fetched successfully' },
        data: {
          type: 'object',
          properties: {
            total: { type: 'number', example: 25 },
            page: { type: 'number', example: 1 },
            limit: { type: 'number', example: 10 },
            items: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  price: { type: 'string', example: '100.50' },
                  remaining: { type: 'number', example: 15.75 },
                  status: {
                    type: 'string',
                    enum: ['OPEN', 'PARTIAL'],
                    example: 'OPEN',
                  },
                  user_name: { type: 'string', example: 'John Doe' },
                  created_at: {
                    type: 'string',
                    format: 'date-time',
                    example: '2024-01-15T10:30:00.000Z',
                  },
                  side: {
                    type: 'string',
                    enum: ['BUY', 'SELL'],
                    example: 'BUY',
                  },
                },
              },
              example: [
                {
                  price: '100.50',
                  remaining: 15.75,
                  status: 'OPEN',
                  user_name: 'John Doe',
                  created_at: '2024-01-15T10:30:00.000Z',
                  side: 'BUY',
                },
                {
                  price: '100.25',
                  remaining: 8.5,
                  status: 'PARTIAL',
                  user_name: 'Jane Smith',
                  created_at: '2024-01-15T10:25:00.000Z',
                  side: 'BUY',
                },
              ],
            },
          },
        },
      },
    },
  })
  @ApiResponse({
    status: 400,
    description: 'Invalid side parameter',
    schema: {
      type: 'object',
      properties: {
        message: {
          type: 'string',
          example: 'side param should be in [BUY,SELL].',
        },
        error: { type: 'string', example: 'Bad Request' },
        statusCode: { type: 'number', example: 400 },
      },
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Unauthorized - Invalid or missing JWT token',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Unauthorized' },
        error: { type: 'string', example: 'Unauthorized' },
        statusCode: { type: 'number', example: 401 },
      },
    },
  })
  async getOrderbook(
    @Param('side') side: string,
    @Query() query: PaginationQueryDto,
  ) {
    const orderSide = side.toUpperCase();
    const data = await this.ordersService.getOrderbook(orderSide, query);
    return {
      message: 'Orderbook fetched successfully',
      data,
    };
  }
}

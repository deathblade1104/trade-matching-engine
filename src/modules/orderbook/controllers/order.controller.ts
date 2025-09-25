import {
  Body,
  Controller,
  Get,
  HttpCode,
  Param,
  Post,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { PaginationQueryDto } from '../../../common/dtos/pagination.dto';
import { JwtAuthGuard } from '../../../common/guards/jwt-auth.guard';
import { CustomExpressRequest } from '../../../common/interfaces/express-request.interface';
import { CreateOrderDto } from '../dtos/create-order.dto';
import { OrdersService } from '../services/order.service';

@Controller({ path: 'orders', version: '1' })
@ApiTags('Orders API')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  @Post()
  @HttpCode(201)
  @ApiOperation({
    summary: 'Place a new order',
    description:
      'Create a new buy or sell order. The order will be automatically matched against existing orders in the orderbook.',
  })
  @ApiBody({
    type: CreateOrderDto,
    description: 'Order details including side (BUY/SELL), price, and quantity',
    examples: {
      buyOrder: {
        summary: 'Buy Order Example',
        value: {
          side: 'BUY',
          price: 100.5,
          quantity: 10,
        },
      },
      sellOrder: {
        summary: 'Sell Order Example',
        value: {
          side: 'SELL',
          price: 99.75,
          quantity: 5,
        },
      },
    },
  })
  @ApiResponse({
    status: 201,
    description: 'Order successfully created and enqueued for processing',
    schema: {
      type: 'object',
      properties: {
        message: {
          type: 'string',
          example:
            'Order has been received, and has been enqueued for processing.',
        },
        data: {
          type: 'object',
          properties: {
            id: { type: 'number', example: 1 },
            side: { type: 'string', enum: ['BUY', 'SELL'], example: 'BUY' },
            price: { type: 'string', example: '100.50' },
            quantity: { type: 'string', example: '10.00' },
            remaining: { type: 'string', example: '10.00' },
            status: {
              type: 'string',
              enum: ['OPEN', 'PARTIAL', 'FILLED'],
              example: 'OPEN',
            },
            validity_days: { type: 'number', example: 60 },
            created_at: { type: 'string', format: 'date-time' },
            updated_at: { type: 'string', format: 'date-time' },
          },
        },
      },
    },
  })
  @ApiResponse({
    status: 400,
    description: 'Invalid order data provided',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Validation failed' },
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
  async createOrder(
    @Body() dto: CreateOrderDto,
    @Req() req: CustomExpressRequest,
  ) {
    const userId = req.user.sub;
    const data = await this.ordersService.createOrder(dto, parseFloat(userId));
    return {
      message: 'Order has been received, and has been enqueued for processing.',
      data,
    };
  }

  @Get(':id')
  @HttpCode(200)
  @ApiOperation({
    summary: 'Get order details',
    description:
      'Retrieve detailed information about a specific order by ID. Only the order owner can access their orders.',
  })
  @ApiParam({
    name: 'id',
    type: 'number',
    description: 'Order ID',
    example: 1,
  })
  @ApiResponse({
    status: 200,
    description: 'Order details retrieved successfully',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Fetched order successfully.' },
        data: {
          type: 'object',
          properties: {
            id: { type: 'number', example: 1 },
            side: { type: 'string', enum: ['BUY', 'SELL'], example: 'BUY' },
            price: { type: 'string', example: '100.50' },
            quantity: { type: 'string', example: '10.00' },
            remaining: { type: 'string', example: '5.00' },
            status: {
              type: 'string',
              enum: ['OPEN', 'PARTIAL', 'FILLED'],
              example: 'PARTIAL',
            },
            validity_days: { type: 'number', example: 60 },
            created_at: { type: 'string', format: 'date-time' },
            updated_at: { type: 'string', format: 'date-time' },
            status_history: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  status: {
                    type: 'string',
                    enum: ['OPEN', 'PARTIAL', 'FILLED'],
                    example: 'OPEN',
                  },
                  actor: {
                    type: 'string',
                    enum: ['USER', 'SYSTEM'],
                    example: 'USER',
                  },
                  created_at: {
                    type: 'string',
                    format: 'date-time',
                    example: '2024-01-15T10:30:00.000Z',
                  },
                },
              },
            },
          },
        },
      },
    },
  })
  @ApiResponse({
    status: 403,
    description: 'Forbidden - Cannot access order belonging to another user',
    schema: {
      type: 'object',
      properties: {
        message: {
          type: 'string',
          example: 'You are not allowed to access this order',
        },
        error: { type: 'string', example: 'Forbidden' },
        statusCode: { type: 'number', example: 403 },
      },
    },
  })
  @ApiResponse({
    status: 404,
    description: 'Order not found',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Order not found' },
        error: { type: 'string', example: 'Not Found' },
        statusCode: { type: 'number', example: 404 },
      },
    },
  })
  async getOrderById(
    @Param('id') id: number,
    @Req() req: CustomExpressRequest,
  ) {
    const userId = req.user.sub;
    const data = await this.ordersService.getOrderById(id, parseFloat(userId));
    return {
      message: 'Fetched order successfully.',
      data,
    };
  }
  @Get()
  @HttpCode(200)
  @ApiOperation({
    summary: 'Get user orders',
    description:
      'Retrieve all orders belonging to the authenticated user with pagination support.',
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
    description: 'User orders retrieved successfully',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Orders fetched successfully' },
        data: {
          type: 'object',
          properties: {
            total: { type: 'number', example: 5 },
            page: { type: 'number', example: 1 },
            limit: { type: 'number', example: 10 },
            items: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  id: { type: 'number', example: 1 },
                  side: {
                    type: 'string',
                    enum: ['BUY', 'SELL'],
                    example: 'BUY',
                  },
                  price: { type: 'string', example: '100.50' },
                  quantity: { type: 'string', example: '10.00' },
                  remaining: { type: 'string', example: '5.00' },
                  status: {
                    type: 'string',
                    enum: ['OPEN', 'PARTIAL', 'FILLED'],
                    example: 'PARTIAL',
                  },
                  validity_days: { type: 'number', example: 60 },
                  created_at: { type: 'string', format: 'date-time' },
                  updated_at: { type: 'string', format: 'date-time' },
                },
              },
            },
          },
        },
      },
    },
  })
  async getOrdersPerUser(
    @Query() query: PaginationQueryDto,
    @Req() req: CustomExpressRequest,
  ) {
    const userId = req.user.sub;
    const data = await this.ordersService.getOrdersByUserId(
      parseFloat(userId),
      query,
    );
    return {
      message: 'Orders fetched successfully',
      data,
    };
  }
}

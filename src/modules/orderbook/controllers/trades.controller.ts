import {
  Controller,
  Get,
  HttpCode,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { PaginationQueryDto } from '../../../common/dtos/pagination.dto';
import { JwtAuthGuard } from '../../../common/guards/jwt-auth.guard';
import { CustomExpressRequest } from '../../../common/interfaces/express-request.interface';
import { TradesService } from '../services/trade.service';

@Controller({ path: 'trades', version: '1' })
@ApiTags('Trades API')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
export class TradesController {
  constructor(private readonly tradesService: TradesService) {}

  @Get()
  @HttpCode(200)
  @ApiOperation({
    summary: 'Get trade history',
    description:
      'Retrieve the trade history for the authenticated user. Shows all executed trades where the user was either the buyer or seller.',
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
    description: 'Trade history retrieved successfully',
    schema: {
      type: 'object',
      properties: {
        message: { type: 'string', example: 'Trades fetched successfully' },
        data: {
          type: 'object',
          properties: {
            total: { type: 'number', example: 15 },
            page: { type: 'number', example: 1 },
            limit: { type: 'number', example: 10 },
            items: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  id: { type: 'number', example: 1 },
                  price: { type: 'string', example: '100.50' },
                  quantity: { type: 'string', example: '5.00' },
                  buy_order_id: { type: 'number', example: 1 },
                  sell_order_id: { type: 'number', example: 2 },
                  created_at: { type: 'string', format: 'date-time' },
                },
              },
              example: [
                {
                  id: 1,
                  price: '100.50',
                  quantity: '5.00',
                  buy_order_id: 1,
                  sell_order_id: 2,
                  created_at: '2024-01-15T10:30:00.000Z',
                },
              ],
            },
          },
        },
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
  async getMyTrades(
    @Query() query: PaginationQueryDto,
    @Req() req: CustomExpressRequest,
  ) {
    const userId = req.user.sub;
    const data = await this.tradesService.getTradesByUserId(
      parseFloat(userId),
      query,
    );
    return {
      message: 'Trades fetched successfully',
      data,
    };
  }
}

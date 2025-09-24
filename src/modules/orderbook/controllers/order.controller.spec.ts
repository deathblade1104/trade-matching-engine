import { Test, TestingModule } from '@nestjs/testing';
import { CreateOrderDto } from '../dtos/create-order.dto';
import { OrderSide } from '../enums/order.enum';
import { OrdersService } from '../services/order.service';
import { OrdersController } from './order.controller';

describe('OrdersController', () => {
  let controller: OrdersController;
  let service: OrdersService;

  const mockOrdersService = {
    createOrder: jest.fn(),
    getOrderById: jest.fn(),
    getOrderbook: jest.fn(),
    getOrdersByUserId: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [OrdersController],
      providers: [
        {
          provide: OrdersService,
          useValue: mockOrdersService,
        },
      ],
    }).compile();

    controller = module.get<OrdersController>(OrdersController);
    service = module.get<OrdersService>(OrdersService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('createOrder', () => {
    it('should create a new order', async () => {
      const createOrderDto: CreateOrderDto = {
        side: OrderSide.BUY,
        price: 100.5,
        quantity: 10,
      };

      const mockOrder = {
        id: 1,
        side: OrderSide.BUY,
        price: '100.50',
        quantity: '10.00',
        remaining: '10.00',
        status: 'OPEN',
        created_at: new Date(),
        updated_at: new Date(),
      };

      const mockRequest = {
        user: { sub: '1' },
      };

      mockOrdersService.createOrder.mockResolvedValue(mockOrder);

      const result = await controller.createOrder(
        createOrderDto,
        mockRequest as any,
      );

      expect(service.createOrder).toHaveBeenCalledWith(createOrderDto, 1);
      expect(result).toEqual({
        message:
          'Order has been received, and has been enqueued for processing.',
        data: mockOrder,
      });
    });
  });

  describe('getOrderById', () => {
    it('should return order details', async () => {
      const orderId = 1;
      const mockOrder = {
        id: 1,
        side: OrderSide.BUY,
        price: '100.50',
        quantity: '10.00',
        remaining: '5.00',
        status: 'PARTIAL',
        created_at: new Date(),
      };

      const mockRequest = {
        user: { sub: '1' },
      };

      mockOrdersService.getOrderById.mockResolvedValue(mockOrder);

      const result = await controller.getOrderWithHistory(
        orderId,
        mockRequest as any,
      );

      expect(service.getOrderById).toHaveBeenCalledWith(orderId, 1);
      expect(result).toEqual({
        message: 'Fetched order successfully.',
        data: mockOrder,
      });
    });
  });

  describe('getOrderbook', () => {
    it('should return orderbook data', async () => {
      const mockOrderbook = {
        total: 25,
        page: 1,
        limit: 10,
        items: [
          { price: '100.50', remaining: 15.75 },
          { price: '100.25', remaining: 8.5 },
        ],
      };

      const query = { page: 1, limit: 10 };

      mockOrdersService.getOrderbook.mockResolvedValue(mockOrderbook);

      const result = await controller.getOrderbook(query);

      expect(service.getOrderbook).toHaveBeenCalledWith(query);
      expect(result).toEqual({
        message: 'Orderbook fetched successfully',
        data: mockOrderbook,
      });
    });
  });

  describe('getMyOrders', () => {
    it('should return user orders', async () => {
      const mockOrders = {
        total: 5,
        page: 1,
        limit: 10,
        items: [
          {
            id: 1,
            side: OrderSide.BUY,
            price: '100.50',
            quantity: '10.00',
            remaining: '5.00',
            status: 'PARTIAL',
            created_at: new Date(),
          },
        ],
      };

      const query = { page: 1, limit: 10 };
      const mockRequest = {
        user: { sub: '1' },
      };

      mockOrdersService.getOrdersByUserId.mockResolvedValue(mockOrders);

      const result = await controller.getMyOrders(query, mockRequest as any);

      expect(service.getOrdersByUserId).toHaveBeenCalledWith(1, query);
      expect(result).toEqual({
        message: 'Orders fetched successfully',
        data: mockOrders,
      });
    });
  });
});

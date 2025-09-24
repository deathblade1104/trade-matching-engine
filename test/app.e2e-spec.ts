import { INestApplication } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';

describe('Trade Matching Engine (e2e)', () => {
  let app: INestApplication;
  let accessToken: string;
  let userId: number;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  describe('Authentication Flow', () => {
    it('should register a new user', () => {
      return request(app.getHttpServer())
        .post('/api/v1/auth/signup')
        .send({
          name: 'Test User',
          email: 'test@example.com',
          password: 'password123',
        })
        .expect(201)
        .expect((res) => {
          expect(res.body.message).toBe('Signup Successfull');
          expect(res.body.data).toHaveProperty('id');
          expect(res.body.data).toHaveProperty('email', 'test@example.com');
          userId = res.body.data.id;
        });
    });

    it('should login user and return token', () => {
      return request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({
          email: 'test@example.com',
          password: 'password123',
        })
        .expect(200)
        .expect((res) => {
          expect(res.body.message).toBe('Login Successfull');
          expect(res.body.data).toHaveProperty('access_token');
          accessToken = res.body.data.access_token;
        });
    });
  });

  describe('Order Management', () => {
    it('should place a buy order', () => {
      return request(app.getHttpServer())
        .post('/api/v1/orders')
        .set('Authorization', `Bearer ${accessToken}`)
        .send({
          side: 'BUY',
          price: 100.5,
          quantity: 10,
        })
        .expect(201)
        .expect((res) => {
          expect(res.body.message).toBe(
            'Order has been received, and has been enqueued for processing.',
          );
          expect(res.body.data).toHaveProperty('id');
          expect(res.body.data.side).toBe('BUY');
          expect(res.body.data.price).toBe('100.50');
          expect(res.body.data.quantity).toBe('10.00');
        });
    });

    it('should place a sell order', () => {
      return request(app.getHttpServer())
        .post('/api/v1/orders')
        .set('Authorization', `Bearer ${accessToken}`)
        .send({
          side: 'SELL',
          price: 99.75,
          quantity: 5,
        })
        .expect(201)
        .expect((res) => {
          expect(res.body.message).toBe(
            'Order has been received, and has been enqueued for processing.',
          );
          expect(res.body.data).toHaveProperty('id');
          expect(res.body.data.side).toBe('SELL');
          expect(res.body.data.price).toBe('99.75');
          expect(res.body.data.quantity).toBe('5.00');
        });
    });

    it('should get user orders', () => {
      return request(app.getHttpServer())
        .get('/api/v1/orders')
        .set('Authorization', `Bearer ${accessToken}`)
        .query({ page: 1, limit: 10 })
        .expect(200)
        .expect((res) => {
          expect(res.body.message).toBe('Orders fetched successfully');
          expect(res.body.data).toHaveProperty('total');
          expect(res.body.data).toHaveProperty('page', 1);
          expect(res.body.data).toHaveProperty('limit', 10);
          expect(res.body.data).toHaveProperty('items');
          expect(Array.isArray(res.body.data.items)).toBe(true);
        });
    });

    it('should get orderbook', () => {
      return request(app.getHttpServer())
        .get('/api/v1/orders/book')
        .query({ page: 1, limit: 10 })
        .expect(200)
        .expect((res) => {
          expect(res.body.message).toBe('Orderbook fetched successfully');
          expect(res.body.data).toHaveProperty('total');
          expect(res.body.data).toHaveProperty('page', 1);
          expect(res.body.data).toHaveProperty('limit', 10);
          expect(res.body.data).toHaveProperty('items');
          expect(Array.isArray(res.body.data.items)).toBe(true);
        });
    });
  });

  describe('Trade History', () => {
    it('should get trade history', () => {
      return request(app.getHttpServer())
        .get('/api/v1/trades')
        .set('Authorization', `Bearer ${accessToken}`)
        .query({ page: 1, limit: 10 })
        .expect(200)
        .expect((res) => {
          expect(res.body.message).toBe('Trades fetched successfully');
          expect(res.body.data).toHaveProperty('total');
          expect(res.body.data).toHaveProperty('page', 1);
          expect(res.body.data).toHaveProperty('limit', 10);
          expect(res.body.data).toHaveProperty('items');
          expect(Array.isArray(res.body.data.items)).toBe(true);
        });
    });
  });

  describe('Health Check', () => {
    it('should return health status', () => {
      return request(app.getHttpServer())
        .get('/api/v1/health')
        .expect(200)
        .expect((res) => {
          expect(res.body).toHaveProperty('status');
          expect(res.body).toHaveProperty('info');
          expect(res.body).toHaveProperty('error');
          expect(res.body).toHaveProperty('details');
        });
    });
  });

  describe('Error Handling', () => {
    it('should return 401 for unauthorized access', () => {
      return request(app.getHttpServer()).get('/api/v1/orders').expect(401);
    });

    it('should return 400 for invalid order data', () => {
      return request(app.getHttpServer())
        .post('/api/v1/orders')
        .set('Authorization', `Bearer ${accessToken}`)
        .send({
          side: 'INVALID',
          price: -10,
          quantity: 0,
        })
        .expect(400);
    });

    it('should return 409 for duplicate email registration', () => {
      return request(app.getHttpServer())
        .post('/api/v1/auth/signup')
        .send({
          name: 'Another User',
          email: 'test@example.com', // Same email as before
          password: 'password123',
        })
        .expect(409);
    });
  });
});

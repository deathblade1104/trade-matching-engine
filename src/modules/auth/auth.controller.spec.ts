import { Test, TestingModule } from '@nestjs/testing';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { LoginDto } from './dtos/login.dto';
import { SignupDto } from './dtos/signup.dto';

describe('AuthController', () => {
  let controller: AuthController;
  let service: AuthService;

  const mockAuthService = {
    signup: jest.fn(),
    login: jest.fn(),
    logout: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [AuthController],
      providers: [
        {
          provide: AuthService,
          useValue: mockAuthService,
        },
      ],
    }).compile();

    controller = module.get<AuthController>(AuthController);
    service = module.get<AuthService>(AuthService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('signup', () => {
    it('should register a new user', async () => {
      const signupDto: SignupDto = {
        name: 'John Doe',
        email: 'john@example.com',
        password: 'password123',
      };

      const mockUser = {
        id: 1,
        email: 'john@example.com',
      };

      mockAuthService.signup.mockResolvedValue(mockUser);

      const result = await controller.signup(signupDto);

      expect(service.signup).toHaveBeenCalledWith(signupDto);
      expect(result).toEqual({
        message: 'Signup Successfull',
        data: mockUser,
      });
    });
  });

  describe('login', () => {
    it('should authenticate user and return token', async () => {
      const loginDto: LoginDto = {
        email: 'john@example.com',
        password: 'password123',
      };

      const mockToken = {
        access_token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
      };

      mockAuthService.login.mockResolvedValue(mockToken);

      const result = await controller.login(loginDto);

      expect(service.login).toHaveBeenCalledWith(loginDto);
      expect(result).toEqual({
        message: 'Login Successfull',
        data: mockToken,
      });
    });
  });

  describe('logout', () => {
    it('should logout user successfully', async () => {
      const mockRequest = {
        headers: {
          authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        },
        user: {
          sub: '1',
          email: 'john@example.com',
          exp: Math.floor(Date.now() / 1000) + 3600,
        },
      };

      mockAuthService.logout.mockResolvedValue(true);

      const result = await controller.logout(mockRequest as any);

      expect(service.logout).toHaveBeenCalledWith(
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        mockRequest.user,
      );
      expect(result).toEqual({
        message: 'Logout Successfull',
      });
    });
  });
});

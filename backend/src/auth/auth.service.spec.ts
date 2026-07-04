import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { ConflictException, UnauthorizedException } from '@nestjs/common';
import { AuthService } from './auth.service';
import { UsersService } from '../users/users.service';
import { User } from '../users/user.entity';
import * as bcrypt from 'bcryptjs';

describe('AuthService', () => {
  let service: AuthService;
  let usersService: jest.Mocked<UsersService>;
  let jwtService: jest.Mocked<JwtService>;

  const mockUser: User = {
    id: 'user-uuid-123',
    email: 'test@example.com',
    passwordHash: 'hashed_password_abc',
    name: 'Test Cyclist',
    googleId: null,
    createdAt: new Date(),
  };

  beforeEach(async () => {
    // Mock UsersService database queries
    const mockUsersService = {
      findOneByEmail: jest.fn(),
      findOneByGoogleId: jest.fn(),
      create: jest.fn(),
      updateGoogleId: jest.fn(),
    };

    // Mock JwtService signature routines
    const mockJwtService = {
      signAsync: jest.fn().mockResolvedValue('jwt-access-token-xyz'),
    };

    // Mock ConfigService environments
    const mockConfigService = {
      get: jest.fn((key: string) => {
        if (key === 'GOOGLE_CLIENT_ID') return 'google-client-id-123';
        if (key === 'JWT_SECRET') return 'secret-key-123';
        return null;
      }),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: UsersService, useValue: mockUsersService },
        { provide: JwtService, useValue: mockJwtService },
        { provide: ConfigService, useValue: mockConfigService },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    usersService = module.get(UsersService);
    jwtService = module.get(JwtService);
  });

  describe('signUp', () => {
    it('should successfully hash the password and create a new user profile', async () => {
      usersService.findOneByEmail.mockResolvedValue(null);
      usersService.create.mockResolvedValue(mockUser);

      const result = await service.signUp('test@example.com', 'password123', 'Test Cyclist');

      expect(usersService.findOneByEmail).toHaveBeenCalledWith('test@example.com');
      expect(usersService.create).toHaveBeenCalledWith(
        'test@example.com',
        expect.any(String), // Asserting hashed password string
        'Test Cyclist',
        null,
      );
      expect(result).toEqual({ accessToken: 'jwt-access-token-xyz' });
    });

    it('should throw ConflictException if the email is already registered', async () => {
      usersService.findOneByEmail.mockResolvedValue(mockUser);

      await expect(
        service.signUp('test@example.com', 'password123', 'Test Cyclist'),
      ).rejects.toThrow(ConflictException);
    });
  });

  describe('login', () => {
    it('should successfully validate password credentials and return a token', async () => {
      const plainPassword = 'password123';
      const salt = await bcrypt.genSalt(10);
      const hash = await bcrypt.hash(plainPassword, salt);
      const userWithRealHash = { ...mockUser, passwordHash: hash };

      usersService.findOneByEmail.mockResolvedValue(userWithRealHash);

      const result = await service.login('test@example.com', plainPassword);

      expect(usersService.findOneByEmail).toHaveBeenCalledWith('test@example.com');
      expect(result).toEqual({ accessToken: 'jwt-access-token-xyz' });
    });

    it('should throw UnauthorizedException if the user does not exist', async () => {
      usersService.findOneByEmail.mockResolvedValue(null);

      await expect(service.login('notfound@example.com', 'password')).rejects.toThrow(
        UnauthorizedException,
      );
    });

    it('should throw UnauthorizedException if the password compares incorrectly', async () => {
      usersService.findOneByEmail.mockResolvedValue(mockUser); // Holds 'hashed_password_abc' which won't match 'wrongpass'

      await expect(service.login('test@example.com', 'wrongpass')).rejects.toThrow(
        UnauthorizedException,
      );
    });
  });

  describe('googleLogin', () => {
    it('should fail token verification when verification library rejects the payload', async () => {
      // Stub the OAuth2Client validation method verifyIdToken in service instance
      jest.spyOn((service as any).googleClient, 'verifyIdToken').mockRejectedValue(
        new Error('Google verification library signature mismatch'),
      );

      await expect(service.googleLogin('invalid-google-token')).rejects.toThrow(
        UnauthorizedException,
      );
    });
  });
});

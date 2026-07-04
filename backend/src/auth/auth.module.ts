import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { UsersModule } from '../users/users.module';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';

/**
 * AuthModule coordinates dependency injections for token operations.
 */
@Module({
  imports: [
    UsersModule,
    ConfigModule,
    // Initialize JWT services asynchronously using the environment key
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.get<string>('JWT_SECRET') || 'njia_salama_default_secret_key_2026',
        signOptions: { expiresIn: '7d' }, // Token remains active for 7 days
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
  exports: [AuthService, JwtModule], // Exported for use in AuthGuard and other parts of the app
})
export class AuthModule {}

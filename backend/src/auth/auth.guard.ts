import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { Request } from 'express';

/**
 * Guard intercepting client requests to check the authorization header
 * and authenticate standard JWT bearer tokens.
 */
@Injectable()
export class AuthGuard implements CanActivate {
  constructor(
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<Request>();
    const token = this.extractTokenFromHeader(request);
    if (!token) {
      throw new UnauthorizedException('Access denied. Authentication token is missing.');
    }

    try {
      const jwtSecret = this.configService.get<string>('JWT_SECRET') || 'njia_salama_default_secret_key_2026';
      
      // Decrypt and check token constraints
      const payload = await this.jwtService.verifyAsync(token, {
        secret: jwtSecret,
      });

      // Bind decrypted user identity payload profile back into the request context
      request['user'] = payload;
    } catch (error) {
      throw new UnauthorizedException('Access denied. Invalid or expired token.');
    }

    return true;
  }

  /**
   * Helper extracting authorization token from Bearer prefix header.
   */
  private extractTokenFromHeader(request: Request): string | undefined {
    const authHeader = request.headers.authorization;
    if (!authHeader) {
      return undefined;
    }

    const [type, token] = authHeader.split(' ');
    return type === 'Bearer' ? token : undefined;
  }
}

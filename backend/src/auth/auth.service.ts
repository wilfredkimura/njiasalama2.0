import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { UsersService } from '../users/users.service';
import { User } from '../users/user.entity';
import * as bcrypt from 'bcryptjs';
import { OAuth2Client } from 'google-auth-library';

/**
 * AuthService coordinates user registration, credentials login, and Google Sign-in verification.
 */
@Injectable()
export class AuthService {
  private googleClient: OAuth2Client;

  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {
    // Retrieve the Google Client ID configured in environment variables
    const googleClientId = this.configService.get<string>('GOOGLE_CLIENT_ID');
    this.googleClient = new OAuth2Client(googleClientId);
  }

  /**
   * Helper to sign a JWT session token for a authenticated user entity.
   */
  private async generateToken(user: User): Promise<{ accessToken: string }> {
    const payload = { sub: user.id, email: user.email, name: user.name };
    const token = await this.jwtService.signAsync(payload);
    return { accessToken: token };
  }

  /**
   * Registers a user with a unique email address, hashes their password, and issues a JWT token.
   */
  async signUp(email: string, password: string, name: string): Promise<{ accessToken: string }> {
    const existingUser = await this.usersService.findOneByEmail(email);
    if (existingUser) {
      throw new ConflictException('Email address is already registered');
    }

    // Hash the password with 10 salt rounds before database storage
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    const user = await this.usersService.create(email, passwordHash, name, null);
    return this.generateToken(user);
  }

  /**
   * Validates standard login credentials and issues a JWT session token.
   */
  async login(email: string, password: string): Promise<{ accessToken: string }> {
    const user = await this.usersService.findOneByEmail(email);
    if (!user || !user.passwordHash) {
      throw new UnauthorizedException('Invalid email or password');
    }

    // Compare input credentials password with stored password hash
    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid email or password');
    }

    return this.generateToken(user);
  }

  /**
   * Validates a Google ID Token sent by Android, signs up/links user profile, and returns a JWT.
   */
  async googleLogin(idToken: string): Promise<{ accessToken: string }> {
    try {
      const googleClientId = this.configService.get<string>('GOOGLE_CLIENT_ID');
      
      // Verify ID token integrity against Google's public certificates
      const ticket = await this.googleClient.verifyIdToken({
        idToken,
        audience: googleClientId,
      });

      const payload = ticket.getPayload();
      if (!payload) {
        throw new UnauthorizedException('Invalid Google token signature');
      }

      const { sub: googleId, email, name } = payload;

      // 1. Try to find user by unique Google ID
      let user = await this.usersService.findOneByGoogleId(googleId);

      if (!user) {
        // 2. If not found, try to find user by email to link account
        if (email) {
          user = await this.usersService.findOneByEmail(email);
        }

        if (user) {
          // Link account if email matches an existing password-based profile
          user = await this.usersService.updateGoogleId(user.id, googleId);
        } else {
          // 3. Create a new user profile for Google-only login
          user = await this.usersService.create(email || null, null, name || null, googleId);
        }
      }

      return this.generateToken(user);
    } catch (error) {
      throw new UnauthorizedException('Google ID token verification failed: ' + error.message);
    }
  }
}

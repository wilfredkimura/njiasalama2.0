import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { AuthService } from './auth.service';
import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

/**
 * Validation DTO representing registration input constraints.
 */
export class SignUpDto {
  @IsEmail({}, { message: 'Please enter a valid email address' })
  email: string;

  @IsString()
  @MinLength(6, { message: 'Password must be at least 6 characters long' })
  password: string;

  @IsString()
  @IsNotEmpty({ message: 'Name cannot be empty' })
  name: string;
}

/**
 * Validation DTO representing credentials login input constraints.
 */
export class LoginDto {
  @IsEmail({}, { message: 'Please enter a valid email address' })
  email: string;

  @IsString()
  @IsNotEmpty({ message: 'Password cannot be empty' })
  password: string;
}

/**
 * Validation DTO representing Google auth input token.
 */
export class GoogleLoginDto {
  @IsString()
  @IsNotEmpty({ message: 'Google ID token is required' })
  idToken: string;
}

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  /**
   * Route to handle local user registrations.
   */
  @Post('signup')
  @HttpCode(HttpStatus.CREATED)
  async signUp(@Body() signUpDto: SignUpDto) {
    return this.authService.signUp(signUpDto.email, signUpDto.password, signUpDto.name);
  }

  /**
   * Route to handle email/password user logins.
   */
  @Post('login')
  @HttpCode(HttpStatus.OK)
  async login(@Body() loginDto: LoginDto) {
    return this.authService.login(loginDto.email, loginDto.password);
  }

  /**
   * Route to handle verified Google Sign-in ID tokens.
   */
  @Post('google')
  @HttpCode(HttpStatus.OK)
  async googleLogin(@Body() googleLoginDto: GoogleLoginDto) {
    return this.authService.googleLogin(googleLoginDto.idToken);
  }
}

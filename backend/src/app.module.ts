import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config'; // Importing Config modules to read environment (.env) files dynamically.
import { TypeOrmModule } from '@nestjs/typeorm'; // Importing TypeORM module to manage our PostgreSQL database connections.
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PinsModule } from './pins/pins.module'; // Importing the PinsModule to expose its endpoints and services.
import { UsersModule } from './users/users.module'; // Registering user identities module
import { AuthModule } from './auth/auth.module';   // Registering authentication controller module
import { RoutesModule } from './routes.module';

/**
 * Configuration factory for TypeORM module options.
 * Extracted as a standalone function to enable direct unit testing with a mock ConfigService.
 */
export function getTypeOrmModuleOptions(configService: ConfigService) {
  const dbUrl = configService.get<string>('DATABASE_URL');
  if (!dbUrl) {
    throw new Error('DATABASE_URL environment variable is missing from .env file');
  }
  
  return {
    type: 'postgres' as const, // Specify database type as a postgres string literal
    url: dbUrl,
    autoLoadEntities: true, // Tells TypeORM to find entity schema classes automatically
    synchronize: true,      // Auto-creates matching tables in the database (MVP helper)
    
    // Neon DB uses cloud serverless configurations requiring SSL connections.
    // If the DATABASE_URL contains ssl requirements, we configure SSL bypass checks.
    ssl: dbUrl.includes('sslmode=require') || dbUrl.includes('ssl=true')
      ? { rejectUnauthorized: false }
      : false,
  };
}

@Module({
  imports: [
    // Load config from .env files globally across the entire backend app
    ConfigModule.forRoot({
      isGlobal: true, // Makes configuration values accessible in all other backend modules
      envFilePath: '.env', // Path specifying where the variables are located
    }),
    
    // TypeORM Async Configuration to connect dynamically using connection string URL
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule], // Import config module so we can inject its service
      inject: [ConfigService], // Inject ConfigService to fetch DATABASE_URL
      useFactory: getTypeOrmModuleOptions, // Bind our testable options factory function
    }),
    PinsModule, // Register our newly created PinsModule containing controller routes and services
    UsersModule, // Register users operations
    AuthModule, RoutesModule,  // Register auth endpoints and JWT signature validations
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}

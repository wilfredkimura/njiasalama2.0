import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config'; // Importing Config modules to read environment (.env) files dynamically.
import { TypeOrmModule } from '@nestjs/typeorm'; // Importing TypeORM module to manage our PostgreSQL database connections.
import { AppController } from './app.controller';
import { AppService } from './app.service';

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
      useFactory: (configService: ConfigService) => {
        const dbUrl = configService.get<string>('DATABASE_URL');
        if (!dbUrl) {
          throw new Error('DATABASE_URL environment variable is missing from .env file');
        }
        
        return {
          type: 'postgres', // Database type specification
          url: dbUrl,       // Full connection string containing credentials and host
          autoLoadEntities: true, // Tells TypeORM to find entity schema classes automatically
          synchronize: true,      // Auto-creates matching tables in the database (MVP helper)
          
          // Neon DB uses cloud serverless configurations requiring SSL connections.
          // If the DATABASE_URL contains ssl requirements, we configure SSL bypass checks.
          ssl: dbUrl.includes('sslmode=require') || dbUrl.includes('ssl=true')
            ? { rejectUnauthorized: false }
            : false,
        };
      },
    }),
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}

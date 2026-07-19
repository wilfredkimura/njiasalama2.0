import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { RoutesController } from './routes.controller';
import { RoutesService } from './routes.service';
import { PinsModule } from './pins/pins.module';
import { SavedRoute } from './routes/saved-route.entity';
import { AuthModule } from './auth/auth.module';

@Module({
  imports: [
    PinsModule,
    AuthModule,
    TypeOrmModule.forFeature([SavedRoute]),
  ],
  controllers: [RoutesController],
  providers: [RoutesService],
})
export class RoutesModule {}

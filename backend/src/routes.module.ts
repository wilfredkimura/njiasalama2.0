import { Module } from '@nestjs/common';
import { RoutesController } from './routes.controller';
import { RoutesService } from './routes.service';
import { PinsModule } from './pins/pins.module';

@Module({
  imports: [PinsModule],
  controllers: [RoutesController],
  providers: [RoutesService],
})
export class RoutesModule {}

import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PinsService } from './pins.service';
import { PinsController } from './pins.controller';
import { DangerPin } from './pins.entity';

/**
 * Module definition aggregating the Pins-related features.
 * Registers the controller routes and binds dependency providers (Services/Repositories).
 */
@Module({
  // Binds the DangerPin entity to the TypeORM feature registry, injecting the repository
  imports: [TypeOrmModule.forFeature([DangerPin])],
  providers: [PinsService],
  controllers: [PinsController],
  // Export PinsService so it can be injected by other modules (e.g. WebSockets Gateway)
  exports: [PinsService],
})
export class PinsModule {}

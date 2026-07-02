import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { DangerPin } from './pins.entity';
import { CreatePinDto } from './dto/create-pin.dto';
import { PinsGateway } from './pins.gateway'; // Importing PinsGateway to trigger live broadcasts

/**
 * Service layer coordinating database read/write queries for hazard danger pins.
 * Decorating a class with @Injectable makes it managed by NestJS's dependency injection container.
 */
@Injectable()
export class PinsService {
  constructor(
    // Inject the default TypeORM repository for the DangerPin entity
    @InjectRepository(DangerPin)
    private readonly pinsRepository: Repository<DangerPin>,
    // Inject the WebSocket gateway to publish live real-time updates
    private readonly pinsGateway: PinsGateway,
  ) {}

  /**
   * Saves a new cyclist-reported road hazard pin in the database and broadcasts it in real-time.
   */
  async create(createPinDto: CreatePinDto): Promise<DangerPin> {
    const pin = this.pinsRepository.create(createPinDto);
    const savedPin = await this.pinsRepository.save(pin);
    // Broadcast the newly created danger pin to all connected WebSocket clients instantly
    this.pinsGateway.broadcastNewPin(savedPin);
    return savedPin;
  }

  /**
   * Fetches all danger pins registered in the database, ordered by latest report first.
   */
  async findAll(): Promise<DangerPin[]> {
    return await this.pinsRepository.find({
      order: { createdAt: 'DESC' },
    });
  }

  /**
   * Fetches all danger pins located within a specified radius (in meters) of a cyclist's current coordinates.
   * This method uses the standard Haversine formula written in raw SQL to calculate metric distance on the earth's surface.
   * It works on all PostgreSQL/Neon configurations without requiring the PostGIS spatial extension.
   */
  async findAllNearby(
    latitude: number,
    longitude: number,
    radiusMeters: number = 5000, // Default distance of 5 kilometers (5000 meters)
  ): Promise<DangerPin[]> {
    return await this.pinsRepository
      .createQueryBuilder('pin')
      .where(
        // The Spherical Law of Cosines (Haversine formula variant) calculates geodesic distance in meters.
        // Earth's radius is approximated as 6,371,000 meters.
        'acos(' +
          'sin(radians(pin.latitude)) * sin(radians(:latitude)) + ' +
          'cos(radians(pin.latitude)) * cos(radians(:latitude)) * ' +
          'cos(radians(:longitude) - radians(pin.longitude))' +
        ') * 6371000 <= :radiusMeters',
        { latitude, longitude, radiusMeters },
      )
      .orderBy('pin.createdAt', 'DESC') // Order search results by latest first
      .getMany();
  }
}

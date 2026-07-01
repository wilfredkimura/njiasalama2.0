import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { DangerPin } from './pins.entity';
import { CreatePinDto } from './dto/create-pin.dto';

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
  ) {}

  /**
   * Saves a new cyclist-reported road hazard pin in the database.
   */
  async create(createPinDto: CreatePinDto): Promise<DangerPin> {
    const pin = this.pinsRepository.create(createPinDto);
    return await this.pinsRepository.save(pin);
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
   * This method uses PostGIS geography query functions (ST_DWithin) to run highly performant 
   * distance calculations directly in the database.
   */
  async findAllNearby(
    latitude: number,
    longitude: number,
    radiusMeters: number = 5000, // Default distance of 5 kilometers (5000 meters)
  ): Promise<DangerPin[]> {
    return await this.pinsRepository
      .createQueryBuilder('pin')
      .where(
        // PostGIS ST_DWithin function checks if the distance between two points is within the radius.
        // We cast points to geography (::geography) so distance is calculated in meters, not flat grid degrees.
        'ST_DWithin(' +
          'ST_SetSRID(ST_MakePoint(pin.longitude, pin.latitude), 4326)::geography, ' +
          'ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, ' +
          ':radiusMeters' +
        ')',
        { latitude, longitude, radiusMeters },
      )
      .orderBy('pin.createdAt', 'DESC') // Order search results by latest first
      .getMany();
  }
}

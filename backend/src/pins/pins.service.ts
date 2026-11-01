import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { DangerPin } from './pins.entity';
import { CreatePinDto } from './dto/create-pin.dto';
import { PinsGateway } from './pins.gateway';

/**
 * Service layer coordinating database read/write queries for hazard danger pins.
 */
@Injectable()
export class PinsService {
  constructor(
    @InjectRepository(DangerPin)
    private readonly pinsRepository: Repository<DangerPin>,
    private readonly pinsGateway: PinsGateway,
  ) {}

  /**
   * Saves a new cyclist-reported road hazard pin in the database and broadcasts it in real-time.
   */
  async create(createPinDto: CreatePinDto): Promise<DangerPin> {
    const pin = this.pinsRepository.create({
      ...createPinDto,
      status: 'ACTIVE',
      upvotes: 1,
      downvotes: 0,
    });
    const savedPin = await this.pinsRepository.save(pin);
    this.pinsGateway.broadcastNewPin(savedPin);
    return savedPin;
  }

  /**
   * Fetches all active danger pins registered in the database, ordered by latest report first.
   */
  async findAll(): Promise<DangerPin[]> {
    return await this.pinsRepository.find({
      where: { status: 'ACTIVE' },
      order: { createdAt: 'DESC' },
    });
  }

  /**
   * Fetches active danger pins within a radius around the cyclist.
   */
  async findAllNearby(
    latitude: number,
    longitude: number,
    radiusMeters: number = 5000,
  ): Promise<DangerPin[]> {
    return await this.pinsRepository
      .createQueryBuilder('pin')
      .where('pin.status = :status', { status: 'ACTIVE' })
      .andWhere(
        'acos(' +
          'sin(radians(pin.latitude)) * sin(radians(:latitude)) + ' +
          'cos(radians(pin.latitude)) * cos(radians(:latitude)) * ' +
          'cos(radians(:longitude) - radians(pin.longitude))' +
        ') * 6371000 <= :radiusMeters',
        { latitude, longitude, radiusMeters },
      )
      .orderBy('pin.createdAt', 'DESC')
      .getMany();
  }

  /**
   * Allows community cyclists to vote whether a hazard is still present (confirm)
   * or cleared (resolved). 3 or more 'cleared' votes automatically resolves the pin.
   */
  async vote(pinId: string, action: 'confirm' | 'cleared'): Promise<DangerPin> {
    const pin = await this.pinsRepository.findOne({ where: { id: pinId } });
    if (!pin) {
      throw new NotFoundException(`Danger pin with ID ${pinId} not found`);
    }

    if (action === 'confirm') {
      pin.upvotes += 1;
    } else if (action === 'cleared') {
      pin.downvotes += 1;
      // Auto-decay rule: 3 or more downvotes marks hazard as resolved
      if (pin.downvotes >= 3) {
        pin.status = 'RESOLVED';
      }
    }

    return await this.pinsRepository.save(pin);
  }
}

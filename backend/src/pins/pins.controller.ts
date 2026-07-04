import { Controller, Get, Post, Body, Query, UsePipes, ValidationPipe, ParseFloatPipe, UseGuards, Request } from '@nestjs/common';
import { PinsService } from './pins.service';
import { CreatePinDto } from './dto/create-pin.dto';
import { DangerPin } from './pins.entity';
import { AuthGuard } from '../auth/auth.guard';

/**
 * Controller layer exposing HTTP REST endpoints for client applications.
 * Decorating a class with @Controller('pins') binds all its endpoints under the '/pins' path prefix.
 */
@Controller('pins')
export class PinsController {
  constructor(private readonly pinsService: PinsService) {}

  /**
   * Endpoint: POST /pins
   * Saves a new road hazard report. Requires authentication.
   * Uses NestJS ValidationPipe to enforce the constraints declared in CreatePinDto.
   */
  @Post()
  @UseGuards(AuthGuard)
  @UsePipes(new ValidationPipe({ transform: true, whitelist: true }))
  async create(
    @Body() createPinDto: CreatePinDto,
    @Request() req: any,
  ): Promise<DangerPin> {
    // Associate the reported pin with the verified user name/email from the JWT payload
    createPinDto.reportedBy = req.user.name || req.user.email || 'Authenticated User';
    return await this.pinsService.create(createPinDto);
  }

  /**
   * Endpoint: GET /pins
   * Retrieves all hazard pins registered in the system.
   */
  @Get()
  async findAll(): Promise<DangerPin[]> {
    return await this.pinsService.findAll();
  }

  /**
   * Endpoint: GET /pins/nearby
   * Retrieves all hazard pins registered within a specified geographic radius from the cyclist.
   * Queries:
   *  - latitude: decimal, required (e.g. -1.2921)
   *  - longitude: decimal, required (e.g. 36.8219)
   *  - radius: integer, optional (in meters, defaults to 5000)
   */
  @Get('nearby')
  async findNearby(
    @Query('latitude', ParseFloatPipe) latitude: number,
    @Query('longitude', ParseFloatPipe) longitude: number,
    @Query('radius') radius?: string,
  ): Promise<DangerPin[]> {
    // Convert the optional radius string query parameter to a number, defaulting to 5000 meters (5km)
    const radiusMeters = radius ? parseInt(radius, 10) : 5000;
    return await this.pinsService.findAllNearby(latitude, longitude, radiusMeters);
  }
}

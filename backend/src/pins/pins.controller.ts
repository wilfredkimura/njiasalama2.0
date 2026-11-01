import { Controller, Get, Post, Body, Param, Query, UsePipes, ValidationPipe, ParseFloatPipe, UseGuards, Request } from '@nestjs/common';
import { PinsService } from './pins.service';
import { CreatePinDto } from './dto/create-pin.dto';
import { DangerPin } from './pins.entity';
import { AuthGuard } from '../auth/auth.guard';

@Controller('pins')
export class PinsController {
  constructor(private readonly pinsService: PinsService) {}

  @Post()
  @UseGuards(AuthGuard)
  @UsePipes(new ValidationPipe({ transform: true, whitelist: true }))
  async create(
    @Body() createPinDto: CreatePinDto,
    @Request() req: any,
  ): Promise<DangerPin> {
    createPinDto.reportedBy = req.user.name || req.user.email || 'Authenticated User';
    return await this.pinsService.create(createPinDto);
  }

  @Get()
  async findAll(): Promise<DangerPin[]> {
    return await this.pinsService.findAll();
  }

  @Get('nearby')
  async findNearby(
    @Query('latitude', ParseFloatPipe) latitude: number,
    @Query('longitude', ParseFloatPipe) longitude: number,
    @Query('radius') radius?: number,
  ): Promise<DangerPin[]> {
    return await this.pinsService.findAllNearby(latitude, longitude, radius);
  }

  /**
   * Endpoint: POST /pins/:id/vote
   * Allows community verification (action: 'confirm' | 'cleared').
   */
  @Post(':id/vote')
  async vote(
    @Param('id') id: string,
    @Body('action') action: 'confirm' | 'cleared',
  ): Promise<DangerPin> {
    return await this.pinsService.vote(id, action);
  }
}

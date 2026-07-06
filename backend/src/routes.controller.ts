import { Controller, Get, Query, BadRequestException } from '@nestjs/common';
import { RoutesService } from './routes.service';
import { Route } from './route.interface';

@Controller('routes')
export class RoutesController {
  constructor(private readonly routesService: RoutesService) {}

  @Get()
  async getRoutes(
    @Query('startLat') startLat: string,
    @Query('startLng') startLng: string,
    @Query('endLat') endLat: string,
    @Query('endLng') endLng: string,
  ): Promise<Route[]> {
    const sLat = parseFloat(startLat);
    const sLng = parseFloat(startLng);
    const eLat = parseFloat(endLat);
    const eLng = parseFloat(endLng);

    if (isNaN(sLat) || isNaN(sLng) || isNaN(eLat) || isNaN(eLng)) {
      throw new BadRequestException(
        'Invalid startLat, startLng, endLat, or endLng coordinates. Must be valid float numbers.',
      );
    }

    return await this.routesService.findRoutes(sLat, sLng, eLat, eLng);
  }
}

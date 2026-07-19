import { Controller, Get, Post, Delete, Query, Body, Param, BadRequestException, UseGuards, Request, NotFoundException } from '@nestjs/common';
import { RoutesService } from './routes.service';
import { Route, GeocodeLocation } from './route.interface';
import { AuthGuard } from './auth/auth.guard';
import { SaveRouteDto } from './routes/save-route.dto';
import { SavedRoute } from './routes/saved-route.entity';

@Controller('routes')
export class RoutesController {
  constructor(private readonly routesService: RoutesService) {}

  @Get()
  async getRoutes(
    @Query('startLat') startLat: string,
    @Query('startLng') startLng: string,
    @Query('endLat') endLat: string,
    @Query('endLng') endLng: string,
    @Query('waypoints') waypoints?: string,
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

    return await this.routesService.findRoutes(sLat, sLng, eLat, eLng, waypoints);
  }

  @Get('geocode')
  async searchLocations(@Query('query') query: string): Promise<GeocodeLocation[]> {
    if (!query || query.trim().length === 0) {
      throw new BadRequestException('Search query string is required.');
    }
    return await this.routesService.searchLocations(query);
  }

  @UseGuards(AuthGuard)
  @Post('save')
  async saveRoute(
    @Request() req: any,
    @Body() dto: SaveRouteDto,
  ): Promise<SavedRoute> {
    const userId = req.user.sub;
    return await this.routesService.saveRoute(
      userId,
      dto.name,
      dto.startLat,
      dto.startLng,
      dto.endLat,
      dto.endLng,
      dto.points,
      dto.surfaceType,
      dto.distanceKm,
    );
  }

  @UseGuards(AuthGuard)
  @Get('saved')
  async getSavedRoutes(@Request() req: any): Promise<SavedRoute[]> {
    const userId = req.user.sub;
    return await this.routesService.getSavedRoutes(userId);
  }

  @UseGuards(AuthGuard)
  @Delete('saved/:id')
  async deleteSavedRoute(
    @Request() req: any,
    @Param('id') id: string,
  ): Promise<{ success: boolean }> {
    const userId = req.user.sub;
    try {
      await this.routesService.deleteSavedRoute(userId, id);
      return { success: true };
    } catch (error) {
      throw new NotFoundException(error.message);
    }
  }
}

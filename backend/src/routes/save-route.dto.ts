import { IsString, IsNotEmpty, IsNumber, IsArray, IsEnum } from 'class-validator';
import { RoutePoint, SurfaceType } from '../route.interface';

export class SaveRouteDto {
  @IsString()
  @IsNotEmpty()
  name: string;

  @IsNumber()
  startLat: number;

  @IsNumber()
  startLng: number;

  @IsNumber()
  endLat: number;

  @IsNumber()
  endLng: number;

  @IsArray()
  points: RoutePoint[];

  @IsEnum(SurfaceType)
  surfaceType: SurfaceType;

  @IsNumber()
  distanceKm: number;
}

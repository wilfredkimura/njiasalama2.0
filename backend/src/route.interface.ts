import { DangerPin } from './pins/pins.entity';

export enum SurfaceType {
  ROAD = 'ROAD',
  GRAVEL = 'GRAVEL',
}

export interface RoutePoint {
  latitude: number;
  longitude: number;
}

export interface Route {
  id: string;
  name: string;
  points: RoutePoint[];
  surfaceType: SurfaceType;
  distanceKm: number;
  dangerPins: DangerPin[];
}

export interface GeocodeLocation {
  name: string;
  latitude: number;
  longitude: number;
}

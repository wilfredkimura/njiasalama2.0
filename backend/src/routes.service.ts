import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PinsService } from './pins/pins.service';
import { Route, RoutePoint, SurfaceType } from './route.interface';
import { DangerPin } from './pins/pins.entity';

@Injectable()
export class RoutesService {
  private readonly logger = new Logger(RoutesService.name);

  constructor(
    private readonly configService: ConfigService,
    private readonly pinsService: PinsService,
  ) {}

  /**
   * Main entry point to query routes from A to B.
   * Fetches real directions from OpenRouteService and segments surface tags,
   * falling back gracefully to simulated route options if the key is missing or fails.
   */
  async findRoutes(
    startLat: number,
    startLng: number,
    endLat: number,
    endLng: number,
  ): Promise<Route[]> {
    const apiKey = this.configService.get<string>('ORS_API_KEY');
    let routes: Route[] = [];

    if (apiKey && apiKey.trim().length > 0) {
      try {
        routes = await this.fetchFromOpenRouteService(
          apiKey,
          startLat,
          startLng,
          endLat,
          endLng,
        );
      } catch (error) {
        this.logger.error(
          `Failed to fetch routes from OpenRouteService: ${error.message}. Falling back to simulation.`,
        );
        routes = this.generateSimulatedRoutes(startLat, startLng, endLat, endLng);
      }
    } else {
      this.logger.warn('ORS_API_KEY is not configured in .env file. Using simulated routes.');
      routes = this.generateSimulatedRoutes(startLat, startLng, endLat, endLng);
    }

    // Associate all danger pins from the database that are within 100m of each route
    const allPins = await this.pinsService.findAll();
    for (const route of routes) {
      route.dangerPins = this.filterPinsNearRoute(route.points, allPins, 100);
    }

    return routes;
  }

  /**
   * Invokes OpenRouteService directions endpoint and maps GeoJSON structures into Route domain objects.
   */
  private async fetchFromOpenRouteService(
    apiKey: string,
    startLat: number,
    startLng: number,
    endLat: number,
    endLng: number,
  ): Promise<Route[]> {
    const url = 'https://api.openrouteservice.org/v2/directions/cycling-regular/geojson';
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': apiKey,
      },
      body: JSON.stringify({
        coordinates: [
          [startLng, startLat], // GeoJSON expects [longitude, latitude]
          [endLng, endLat],
        ],
        extra_info: ['surface'],
        alternative_routes: {
          target_count: 2,
        },
      }),
    });

    if (!response.ok) {
      throw new Error(`OpenRouteService responded with status code ${response.status}`);
    }

    const geojson: any = await response.json();
    const features = geojson.features || [];
    const routes: Route[] = [];

    for (let index = 0; index < features.length; index++) {
      const feature = features[index];
      const coordinates: [number, number][] = feature.geometry.coordinates;
      const points: RoutePoint[] = coordinates.map(([lng, lat]) => ({
        latitude: lat,
        longitude: lng,
      }));

      const summary = feature.properties.summary || { distance: 0 };
      const distanceKm = Number((summary.distance / 1000).toFixed(2));

      // Calculate surface classification based on ORS extra_info metrics
      let unpavedDistance = 0;
      const surfaceSummary = feature.properties.extras?.surface?.summary || [];
      for (const item of surfaceSummary) {
        // ORS Surface values: 1=asphalt, 2=concrete, 4=cobblestone, 10=paving_stones are paved.
        // Others (gravel, dirt, ground, sand, grass) are unpaved.
        const isPaved = [1, 2, 4, 10].includes(item.value);
        if (!isPaved) {
          unpavedDistance += item.distance;
        }
      }

      const totalDistance = summary.distance || 1;
      const surfaceType = (unpavedDistance / totalDistance > 0.3)
        ? SurfaceType.GRAVEL
        : SurfaceType.ROAD;

      routes.push({
        id: `ors-route-${index}-${Date.now()}`,
        name: index === 0 ? 'Primary Cycling Route' : `Alternative Route ${index}`,
        points,
        surfaceType,
        distanceKm,
        dangerPins: [],
      });
    }

    // Ensure we always provide at least one ROAD and one GRAVEL option by copying and adapting if needed
    if (routes.length === 1) {
      routes.push(this.createAlternativeOption(routes[0]));
    } else if (routes.length > 1) {
      const hasRoad = routes.some(r => r.surfaceType === SurfaceType.ROAD);
      const hasGravel = routes.some(r => r.surfaceType === SurfaceType.GRAVEL);
      if (!hasRoad) {
        routes.push(this.createAlternativeOption(routes[0], SurfaceType.ROAD));
      } else if (!hasGravel) {
        routes.push(this.createAlternativeOption(routes[0], SurfaceType.GRAVEL));
      }
    }

    return routes;
  }

  /**
   * Helper that copies a route and offsets its points slightly to simulate a surface alternative.
   */
  private createAlternativeOption(baseRoute: Route, forceType?: SurfaceType): Route {
    const targetType = forceType || (baseRoute.surfaceType === SurfaceType.ROAD ? SurfaceType.GRAVEL : SurfaceType.ROAD);
    const offsetPoints = baseRoute.points.map((p, idx) => {
      // Keep start and endpoints locked, offset intermediate points
      if (idx === 0 || idx === baseRoute.points.length - 1) return p;
      return {
        latitude: p.latitude + (idx % 2 === 0 ? 0.0015 : -0.0015),
        longitude: p.longitude + (idx % 2 === 0 ? -0.0015 : 0.0015),
      };
    });

    return {
      id: `${baseRoute.id}-alt-surface`,
      name: targetType === SurfaceType.GRAVEL ? 'Scenic Gravel Trail' : 'Direct Street Route',
      points: offsetPoints,
      surfaceType: targetType,
      distanceKm: Number((baseRoute.distanceKm * 1.15).toFixed(2)),
      dangerPins: [],
    };
  }

  /**
   * Generates simulated paved and unpaved routes when ORS key is not available.
   */
  private generateSimulatedRoutes(
    startLat: number,
    startLng: number,
    endLat: number,
    endLng: number,
  ): Route[] {
    const directPoints: RoutePoint[] = this.interpolatePoints(
      startLat,
      startLng,
      endLat,
      endLng,
      false,
    );
    const scenicPoints: RoutePoint[] = this.interpolatePoints(
      startLat,
      startLng,
      endLat,
      endLng,
      true,
    );

    const baseDistance = this.calculateHaversineDistance(
      startLat,
      startLng,
      endLat,
      endLng,
    ) / 1000;

    return [
      {
        id: 'sim-route-road',
        name: 'Direct Street Route (Paved)',
        points: directPoints,
        surfaceType: SurfaceType.ROAD,
        distanceKm: Number(baseDistance.toFixed(2)),
        dangerPins: [],
      },
      {
        id: 'sim-route-gravel',
        name: 'Scenic Gravel Trail (Unpaved)',
        points: scenicPoints,
        surfaceType: SurfaceType.GRAVEL,
        distanceKm: Number((baseDistance * 1.25).toFixed(2)),
        dangerPins: [],
      },
    ];
  }

  /**
   * Generates a coordinates polyline path between start and end.
   */
  private interpolatePoints(
    startLat: number,
    startLng: number,
    endLat: number,
    endLng: number,
    curve: boolean,
  ): RoutePoint[] {
    const segments = 10;
    const points: RoutePoint[] = [];

    for (let i = 0; i <= segments; i++) {
      const fraction = i / segments;
      const lat = startLat + (endLat - startLat) * fraction;
      const lng = startLng + (endLng - startLng) * fraction;

      if (curve && i > 0 && i < segments) {
        // Apply sinusoidal wave curve offset for scenic trails
        const offset = Math.sin(fraction * Math.PI) * 0.003;
        points.push({ latitude: lat + offset, longitude: lng - offset });
      } else {
        points.push({ latitude: lat, longitude: lng });
      }
    }
    return points;
  }

  /**
   * Scans danger pins and filters those that sit within a threshold distance of any route segment.
   */
  private filterPinsNearRoute(
    routePoints: RoutePoint[],
    pins: DangerPin[],
    thresholdMeters: number,
  ): DangerPin[] {
    const dangerPins: DangerPin[] = [];

    for (const pin of pins) {
      let isClose = false;
      for (let i = 0; i < routePoints.length - 1; i++) {
        const dist = this.getDistanceToSegment(
          pin.latitude,
          pin.longitude,
          routePoints[i].latitude,
          routePoints[i].longitude,
          routePoints[i + 1].latitude,
          routePoints[i + 1].longitude,
        );
        if (dist <= thresholdMeters) {
          isClose = true;
          break;
        }
      }
      if (isClose) {
        dangerPins.push(pin);
      }
    }
    return dangerPins;
  }

  /**
   * Calculates perpendicular distance from a point to a 2D line segment.
   */
  private getDistanceToSegment(
    latP: number,
    lngP: number,
    latA: number,
    lngA: number,
    latB: number,
    lngB: number,
  ): number {
    const dLng = lngB - lngA;
    const dLat = latB - latA;

    if (dLng === 0 && dLat === 0) {
      return this.calculateHaversineDistance(latP, lngP, latA, lngA);
    }

    // Flat projection factor t
    let t = ((lngP - lngA) * dLng + (latP - latA) * dLat) / (dLng * dLng + dLat * dLat);
    t = Math.max(0, Math.min(1, t)); // Clamp to segment boundaries

    const latC = latA + t * dLat;
    const lngC = lngA + t * dLng;

    return this.calculateHaversineDistance(latP, lngP, latC, lngC);
  }

  /**
   * Basic Haversine geodesic distance helper in meters.
   */
  private calculateHaversineDistance(
    lat1: number,
    lon1: number,
    lat2: number,
    lon2: number,
  ): number {
    const R = 6371e3; // Earth's radius in meters
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

    const a =
      Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
      Math.cos(phi1) *
        Math.cos(phi2) *
        Math.sin(deltaLambda / 2) *
        Math.sin(deltaLambda / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }
}

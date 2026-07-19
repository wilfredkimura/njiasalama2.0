import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { RoutesService } from './routes.service';
import { PinsService } from './pins/pins.service';
import { SurfaceType } from './route.interface';
import { DangerPin } from './pins/pins.entity';

describe('RoutesService', () => {
  let service: RoutesService;
  let mockPinsService: Partial<PinsService>;
  let mockConfigService: Partial<ConfigService>;
  let originalFetch: typeof global.fetch;

  const fakePins: DangerPin[] = [
    {
      id: 'pin-1',
      title: 'Pothole',
      description: 'Big pothole',
      latitude: 1.002, // Close to interpolation segment (1.000 to 1.010)
      longitude: 1.002,
      type: 'POTHOLE' as any,
      reportedBy: 'Cyclist1',
      createdAt: new Date(),
    } as any,
  ];

  beforeAll(() => {
    originalFetch = global.fetch;
  });

  afterAll(() => {
    global.fetch = originalFetch;
  });

  beforeEach(async () => {
    mockPinsService = {
      findAll: jest.fn().mockResolvedValue(fakePins),
    };
    mockConfigService = {
      get: jest.fn().mockReturnValue(''), // Return empty ORS API key to trigger simulation fallback by default
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RoutesService,
        { provide: ConfigService, useValue: mockConfigService },
        { provide: PinsService, useValue: mockPinsService },
      ],
    }).compile();

    service = module.get<RoutesService>(RoutesService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('generateSimulatedRoutes (fallback mode)', () => {
    it('should generate simulated routes and perform proximity hazard scanning correctly', async () => {
      const routes = await service.findRoutes(1.0, 1.0, 1.01, 1.01);

      expect(routes).toHaveLength(2);

      const roadRoute = routes.find(r => r.surfaceType === SurfaceType.ROAD);
      const gravelRoute = routes.find(r => r.surfaceType === SurfaceType.GRAVEL);

      expect(roadRoute).toBeDefined();
      expect(gravelRoute).toBeDefined();

      expect(roadRoute.dangerPins.length).toBeGreaterThanOrEqual(1);
      expect(roadRoute.dangerPins.some(p => p.id === 'pin-1')).toBe(true);
    });
  });

  describe('searchLocations (Geocoding Proxy)', () => {
    it('should return simulated fallback search results when API key is missing', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('');

      const results = await service.searchLocations('Nairobi');
      expect(results).toHaveLength(2);
      expect(results[0].name).toContain('Nairobi');
      expect(results[0].name).toContain('Simulated Location');
    });

    it('should query ORS geocoding API and return mapped locations when API key is present', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('fake-ors-key');

      const mockResponse = {
        features: [
          {
            geometry: { coordinates: [36.8219, -1.2921] },
            properties: { label: 'Nairobi, Kenya', name: 'Nairobi' },
          },
          {
            geometry: { coordinates: [36.8500, -1.3000] },
            properties: { label: 'Nairobi East, Kenya' },
          },
        ],
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as any);

      const results = await service.searchLocations('Nairobi');

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('https://api.openrouteservice.org/geocode/search?api_key=fake-ors-key&text=Nairobi'),
      );
      expect(results).toHaveLength(2);
      expect(results[0]).toEqual({
        name: 'Nairobi, Kenya',
        latitude: -1.2921,
        longitude: 36.8219,
      });
      expect(results[1]).toEqual({
        name: 'Nairobi East, Kenya',
        latitude: -1.3000,
        longitude: 36.8500,
      });
    });

    it('should fall back to simulated results if the Geocoding API fetch throws an error', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('fake-ors-key');
      global.fetch = jest.fn().mockRejectedValue(new Error('Network failure'));

      const results = await service.searchLocations('Nairobi');
      expect(results).toHaveLength(2);
      expect(results[0].name).toContain('Nairobi');
      expect(results[0].name).toContain('Simulated Fallback');
    });
  });

  describe('findRoutes with waypoints', () => {
    it('should inject waypoints between start and end coordinates in ORS payload', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('fake-ors-key');

      const mockGeoJson = {
        features: [
          {
            geometry: {
              coordinates: [
                [36.8, -1.2],
                [36.85, -1.25],
                [36.9, -1.3],
              ],
            },
            properties: {
              summary: { distance: 15000 },
              extras: {
                surface: {
                  summary: [{ value: 1, distance: 15000 }], // asphalt (paved)
                },
              },
            },
          },
        ],
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockGeoJson),
      } as any);

      // Call routes service with start, end and a semicolon-separated waypoint
      const routes = await service.findRoutes(
        -1.2, 36.8, // Start
        -1.3, 36.9, // End
        '-1.25,36.85', // Waypoint
      );

      // Verify that fetch was called twice (once for driving-car, once for cycling-regular)
      expect(global.fetch).toHaveBeenCalledTimes(2);

      // Verify the POST body of coordinates contains the waypoint in the middle
      const firstCallArgs = (global.fetch as jest.Mock).mock.calls[0];
      const postBody = JSON.parse(firstCallArgs[1].body);
      expect(postBody.coordinates).toEqual([
        [36.8, -1.2],   // Start
        [36.85, -1.25], // Waypoint
        [36.9, -1.3],   // End
      ]);

      expect(routes).toHaveLength(2); // Since driving-car and cycling-regular both resolve
      expect(routes[0].surfaceType).toBe(SurfaceType.ROAD);
    });
  });
});

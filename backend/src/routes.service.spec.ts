import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { getRepositoryToken } from '@nestjs/typeorm';
import { RoutesService } from './routes.service';
import { PinsService } from './pins/pins.service';
import { SurfaceType } from './route.interface';
import { DangerPin } from './pins/pins.entity';
import { SavedRoute } from './routes/saved-route.entity';

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

  let mockSavedRouteRepository: any;

  beforeEach(async () => {
    mockPinsService = {
      findAll: jest.fn().mockResolvedValue(fakePins),
    };
    mockConfigService = {
      get: jest.fn().mockReturnValue(''), // Return empty ORS API key to trigger simulation fallback by default
    };
    mockSavedRouteRepository = {
      create: jest.fn().mockImplementation(dto => dto),
      save: jest.fn().mockImplementation(entity => Promise.resolve({ id: 'saved-route-id-123', ...entity })),
      find: jest.fn(),
      findOne: jest.fn(),
      remove: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RoutesService,
        { provide: ConfigService, useValue: mockConfigService },
        { provide: PinsService, useValue: mockPinsService },
        { provide: getRepositoryToken(SavedRoute), useValue: mockSavedRouteRepository },
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

      expect(roadRoute!.dangerPins.length).toBeGreaterThanOrEqual(1);
      expect(roadRoute!.dangerPins.some(p => p.id === 'pin-1')).toBe(true);
    });
  });

  describe('searchLocations (Geocoding Proxy)', () => {
    it('should return simulated fallback search results when API key is missing', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('');

      const results = await service.searchLocations('Nairobi');
      expect(results).toHaveLength(7);
      expect(results[0].name).toContain('Nairobi');
      expect(results[0].name).toContain('Simulated');
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
        expect.stringContaining('https://api.openrouteservice.org/geocode/search?api_key=fake-ors-key&text=Nairobi&size=10&boundary.country=KEN'),
      );
      expect(results).toHaveLength(2);
      expect(results[0]).toEqual({
        name: 'Nairobi, Kenya',
        latitude: -1.2921,
        longitude: 36.8219,
      });
    });

    it('should query ORS geocoding API with focus coordinates when focus bias is provided', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('fake-ors-key');

      const mockResponse = {
        features: [
          {
            geometry: { coordinates: [36.8219, -1.2921] },
            properties: { label: 'Nairobi, Kenya', name: 'Nairobi' },
          },
        ],
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as any);

      await service.searchLocations('Nairobi', -1.2921, 36.8219);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('focus.point.lat=-1.2921&focus.point.lon=36.8219'),
      );
    });

    it('should fall back to simulated results if the Geocoding API fetch throws an error', async () => {
      (mockConfigService.get as jest.Mock).mockReturnValue('fake-ors-key');
      global.fetch = jest.fn().mockRejectedValue(new Error('Network failure'));

      const results = await service.searchLocations('Nairobi');
      expect(results).toHaveLength(7);
      expect(results[0].name).toContain('Nairobi');
      expect(results[0].name).toContain('Simulated');
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

  describe('SavedRoute operations', () => {
    it('should save a route to the database successfully', async () => {
      const mockPoints = [{ latitude: 1.0, longitude: 2.0 }];
      const result = await service.saveRoute(
        'user-uuid-1',
        'Home to Work',
        1.0, 2.0, 1.1, 2.1,
        mockPoints,
        SurfaceType.ROAD,
        1.5,
      );

      expect(mockSavedRouteRepository.create).toHaveBeenCalledWith(expect.objectContaining({
        userId: 'user-uuid-1',
        name: 'Home to Work',
        startLat: 1.0,
        startLng: 2.0,
        endLat: 1.1,
        endLng: 2.1,
        points: mockPoints,
        surfaceType: SurfaceType.ROAD,
        distanceKm: 1.5,
      }));
      expect(mockSavedRouteRepository.save).toHaveBeenCalled();
      expect(result.id).toBe('saved-route-id-123');
    });

    it('should fetch saved routes for a given user', async () => {
      const mockSavedList = [{ id: '1', name: 'Route 1', userId: 'user-uuid-1' }];
      mockSavedRouteRepository.find.mockResolvedValue(mockSavedList);

      const result = await service.getSavedRoutes('user-uuid-1');

      expect(mockSavedRouteRepository.find).toHaveBeenCalledWith({
        where: { userId: 'user-uuid-1' },
        order: { createdAt: 'DESC' },
      });
      expect(result).toEqual(mockSavedList);
    });

    it('should delete a saved route if it exists and belongs to the user', async () => {
      const mockRoute = { id: 'route-1', userId: 'user-uuid-1' };
      mockSavedRouteRepository.findOne.mockResolvedValue(mockRoute);
      mockSavedRouteRepository.remove.mockResolvedValue(undefined);

      await service.deleteSavedRoute('user-uuid-1', 'route-1');

      expect(mockSavedRouteRepository.findOne).toHaveBeenCalledWith({
        where: { id: 'route-1', userId: 'user-uuid-1' },
      });
      expect(mockSavedRouteRepository.remove).toHaveBeenCalledWith(mockRoute);
    });

    it('should throw an error if the route to delete is not found or belongs to another user', async () => {
      mockSavedRouteRepository.findOne.mockResolvedValue(null);

      await expect(service.deleteSavedRoute('user-uuid-1', 'route-1')).rejects.toThrow(
        'Saved route not found or access denied',
      );
    });
  });
});

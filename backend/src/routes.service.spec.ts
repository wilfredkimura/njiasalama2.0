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
    {
      id: 'pin-2',
      title: 'Construction',
      description: 'Road works',
      latitude: 5.0, // Far away
      longitude: 5.0,
      type: 'CONSTRUCTION' as any,
      reportedBy: 'Cyclist2',
      createdAt: new Date(),
    } as any,
  ];

  beforeEach(async () => {
    mockPinsService = {
      findAll: jest.fn().mockResolvedValue(fakePins),
    };
    mockConfigService = {
      get: jest.fn().mockReturnValue(''), // Return empty ORS API key to trigger simulation fallback
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

  it('should generate simulated routes and perform proximity hazard scanning correctly', async () => {
    // Coordinate range from (1.000, 1.000) to (1.010, 1.010)
    const routes = await service.findRoutes(1.0, 1.0, 1.01, 1.01);

    expect(routes).toHaveLength(2);

    const roadRoute = routes.find(r => r.surfaceType === SurfaceType.ROAD);
    const gravelRoute = routes.find(r => r.surfaceType === SurfaceType.GRAVEL);

    expect(roadRoute).toBeDefined();
    expect(gravelRoute).toBeDefined();

    // Verify that the close pin (pin-1) was attached to the road route, and the far pin (pin-2) was not
    expect(roadRoute.dangerPins.length).toBeGreaterThanOrEqual(1);
    expect(roadRoute.dangerPins.some(p => p.id === 'pin-1')).toBe(true);
    expect(roadRoute.dangerPins.some(p => p.id === 'pin-2')).toBe(false);
  });
});

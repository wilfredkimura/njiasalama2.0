import { Test, TestingModule } from '@nestjs/testing';
import { RoutesController } from './routes.controller';
import { RoutesService } from './routes.service';
import { SurfaceType } from './route.interface';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';

describe('RoutesController', () => {
  let controller: RoutesController;
  let mockRoutesService: Partial<RoutesService>;
  let mockJwtService: Partial<JwtService>;
  let mockConfigService: Partial<ConfigService>;

  beforeEach(async () => {
    mockRoutesService = {
      findRoutes: jest.fn(),
      searchLocations: jest.fn(),
      saveRoute: jest.fn(),
      getSavedRoutes: jest.fn(),
      deleteSavedRoute: jest.fn(),
    };

    mockJwtService = {
      verifyAsync: jest.fn(),
    };

    mockConfigService = {
      get: jest.fn().mockReturnValue('mock-jwt-secret'),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [RoutesController],
      providers: [
        { provide: RoutesService, useValue: mockRoutesService },
        { provide: JwtService, useValue: mockJwtService },
        { provide: ConfigService, useValue: mockConfigService },
      ],
    }).compile();

    controller = module.get<RoutesController>(RoutesController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('getRoutes', () => {
    it('should parse coordinate strings and forward query params to routesService', async () => {
      mockRoutesService.findRoutes = jest.fn().mockResolvedValue([]);

      const result = await controller.getRoutes('-1.29', '36.82', '-1.30', '36.85', '1.02,36.8');
      expect(mockRoutesService.findRoutes).toHaveBeenCalledWith(-1.29, 36.82, -1.3, 36.85, '1.02,36.8');
      expect(result).toEqual([]);
    });
  });

  describe('searchLocations', () => {
    it('should forward search query to routesService', async () => {
      const mockLocations = [{ name: 'Nairobi', latitude: -1.2, longitude: 36.8 }];
      mockRoutesService.searchLocations = jest.fn().mockResolvedValue(mockLocations);

      const result = await controller.searchLocations('Nairobi');
      expect(mockRoutesService.searchLocations).toHaveBeenCalledWith('Nairobi');
      expect(result).toEqual(mockLocations);
    });
  });

  describe('saveRoute', () => {
    it('should forward route dto to routesService using userId from request', async () => {
      const mockRoute = { id: 'saved-id', name: 'Home Route' } as any;
      mockRoutesService.saveRoute = jest.fn().mockResolvedValue(mockRoute);

      const dto = {
        name: 'Home Route',
        startLat: 1.0,
        startLng: 2.0,
        endLat: 1.1,
        endLng: 2.1,
        points: [],
        surfaceType: SurfaceType.ROAD,
        distanceKm: 2.5,
      };

      const mockReq = { user: { sub: 'user-uuid-xyz' } };

      const result = await controller.saveRoute(mockReq, dto);
      expect(mockRoutesService.saveRoute).toHaveBeenCalledWith(
        'user-uuid-xyz',
        dto.name,
        dto.startLat,
        dto.startLng,
        dto.endLat,
        dto.endLng,
        dto.points,
        dto.surfaceType,
        dto.distanceKm,
      );
      expect(result).toEqual(mockRoute);
    });
  });

  describe('getSavedRoutes', () => {
    it('should call getSavedRoutes with the correct user id', async () => {
      const mockSavedList = [{ id: '1', name: 'Route 1' }] as any[];
      mockRoutesService.getSavedRoutes = jest.fn().mockResolvedValue(mockSavedList);

      const mockReq = { user: { sub: 'user-uuid-xyz' } };

      const result = await controller.getSavedRoutes(mockReq);
      expect(mockRoutesService.getSavedRoutes).toHaveBeenCalledWith('user-uuid-xyz');
      expect(result).toEqual(mockSavedList);
    });
  });

  describe('deleteSavedRoute', () => {
    it('should call deleteSavedRoute and return success status', async () => {
      mockRoutesService.deleteSavedRoute = jest.fn().mockResolvedValue(undefined);
      const mockReq = { user: { sub: 'user-uuid-xyz' } };

      const result = await controller.deleteSavedRoute(mockReq, 'route-1');
      expect(mockRoutesService.deleteSavedRoute).toHaveBeenCalledWith('user-uuid-xyz', 'route-1');
      expect(result).toEqual({ success: true });
    });
  });
});

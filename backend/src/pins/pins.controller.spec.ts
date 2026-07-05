import { Test, TestingModule } from '@nestjs/testing';
import { PinsController } from './pins.controller';
import { PinsService } from './pins.service';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';

describe('PinsController', () => {
  let controller: PinsController;
  let service: PinsService;

  // Mocking the PinsService business logic methods
  const mockPinsService = {
    create: jest.fn().mockImplementation((dto) => Promise.resolve({ id: 'uuid-123', ...dto })),
    findAll: jest.fn().mockResolvedValue([]),
    findAllNearby: jest.fn().mockResolvedValue([]),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [PinsController],
      providers: [
        {
          provide: PinsService,
          useValue: mockPinsService,
        },
        {
          provide: JwtService,
          useValue: {
            verifyAsync: jest.fn().mockResolvedValue({ sub: 'user-id', email: 'test@example.com' }),
          },
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn().mockReturnValue('secret'),
          },
        },
      ],
    }).compile();

    controller = module.get<PinsController>(PinsController);
    service = module.get<PinsService>(PinsService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('create', () => {
    it('should invoke service.create and return the new pin details with reporter name', async () => {
      const dto = {
        title: 'Broken Streetlight',
        description: 'Dark corner',
        type: 'UNLIT_STREET' as const,
        latitude: -1.2910,
        longitude: 36.8200,
        reportedBy: undefined as string | undefined,
        imageUrl: 'data:image/jpeg;base64,mockdata',
      };

      const mockReq = { user: { name: 'Test Cyclist', email: 'test@example.com' } };
      const result = await controller.create(dto, mockReq);

      expect(service.create).toHaveBeenCalledWith({ ...dto, reportedBy: 'Test Cyclist' });
      expect(result).toEqual({ id: 'uuid-123', ...dto, reportedBy: 'Test Cyclist' });
    });
  });

  describe('findNearby', () => {
    it('should parse query parameters and call service.findAllNearby with converted types', async () => {
      const result = await controller.findNearby(-1.2921, 36.8219, '2000');

      // Verify that query parameters were properly parsed from strings into numbers
      expect(service.findAllNearby).toHaveBeenCalledWith(-1.2921, 36.8219, 2000);
      expect(result).toEqual([]);
    });
  });
});

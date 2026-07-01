import { Test, TestingModule } from '@nestjs/testing';
import { PinsController } from './pins.controller';
import { PinsService } from './pins.service';

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
      ],
    }).compile();

    controller = module.get<PinsController>(PinsController);
    service = module.get<PinsService>(PinsService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('create', () => {
    it('should invoke service.create and return the new pin details', async () => {
      const dto = {
        title: 'Broken Streetlight',
        description: 'Dark corner',
        type: 'UNLIT_STREET' as const,
        latitude: -1.2910,
        longitude: 36.8200,
      };

      const result = await controller.create(dto);

      expect(service.create).toHaveBeenCalledWith(dto);
      expect(result).toEqual({ id: 'uuid-123', ...dto });
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

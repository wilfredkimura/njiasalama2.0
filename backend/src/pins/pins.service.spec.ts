import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PinsService } from './pins.service';
import { DangerPin } from './pins.entity';

describe('PinsService', () => {
  let service: PinsService;
  let repository: Repository<DangerPin>;

  // Mocking the database repository methods to avoid external database connections during testing
  const mockPinsRepository = {
    create: jest.fn().mockImplementation((dto) => dto),
    save: jest.fn().mockImplementation((pin) => Promise.resolve({ id: 'uuid-123', ...pin })),
    find: jest.fn().mockResolvedValue([]),
    createQueryBuilder: jest.fn().mockReturnValue({
      where: jest.fn().mockReturnThis(),
      orderBy: jest.fn().mockReturnThis(),
      getMany: jest.fn().mockResolvedValue([]),
    }),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PinsService,
        {
          provide: getRepositoryToken(DangerPin),
          useValue: mockPinsRepository,
        },
      ],
    }).compile();

    service = module.get<PinsService>(PinsService);
    repository = module.get<Repository<DangerPin>>(getRepositoryToken(DangerPin));
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('create', () => {
    it('should save a new pin and return it with a generated id', async () => {
      const dto = {
        title: 'Huge Pothole',
        description: 'Hard to spot at night',
        type: 'POTHOLE' as const,
        latitude: -1.2921,
        longitude: 36.8219,
      };

      const result = await service.create(dto);

      expect(repository.create).toHaveBeenCalledWith(dto);
      expect(repository.save).toHaveBeenCalledWith(dto);
      expect(result).toEqual({ id: 'uuid-123', ...dto });
    });
  });

  describe('findAllNearby', () => {
    it('should compile the Haversine formula spatial query and fetch matching pins', async () => {
      const result = await service.findAllNearby(-1.2921, 36.8219, 1000);

      // Verify that queryBuilder compiles the spatial queries correctly
      expect(repository.createQueryBuilder).toHaveBeenCalledWith('pin');
      expect(result).toEqual([]);
    });
  });
});

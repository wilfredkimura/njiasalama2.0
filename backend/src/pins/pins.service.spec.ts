import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PinsService } from './pins.service';
import { DangerPin } from './pins.entity';
import { PinsGateway } from './pins.gateway';

describe('PinsService', () => {
  let service: PinsService;
  let repository: Repository<DangerPin>;

  const mockQueryBuilder = {
    where: jest.fn().mockReturnThis(),
    andWhere: jest.fn().mockReturnThis(),
    orderBy: jest.fn().mockReturnThis(),
    getMany: jest.fn().mockResolvedValue([]),
  };

  const mockPinsRepository = {
    create: jest.fn().mockImplementation((dto) => dto),
    save: jest.fn().mockImplementation((pin) => Promise.resolve({ id: 'uuid-123', ...pin })),
    find: jest.fn().mockResolvedValue([]),
    findOne: jest.fn(),
    createQueryBuilder: jest.fn().mockReturnValue(mockQueryBuilder),
  };

  const mockPinsGateway = {
    broadcastNewPin: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PinsService,
        {
          provide: getRepositoryToken(DangerPin),
          useValue: mockPinsRepository,
        },
        {
          provide: PinsGateway,
          useValue: mockPinsGateway,
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
    it('should save a new pin with default active status, broadcast it, and return it', async () => {
      const dto = {
        title: 'Huge Pothole',
        description: 'Hard to spot at night',
        type: 'POTHOLE' as const,
        latitude: -1.2921,
        longitude: 36.8219,
        imageUrl: 'data:image/jpeg;base64,mockdata',
      };

      const result = await service.create(dto);

      expect(repository.create).toHaveBeenCalledWith(
        expect.objectContaining({
          ...dto,
          status: 'ACTIVE',
          upvotes: 1,
          downvotes: 0,
        })
      );
      expect(mockPinsGateway.broadcastNewPin).toHaveBeenCalled();
      expect(result.id).toBe('uuid-123');
    });
  });

  describe('findAllNearby', () => {
    it('should compile the Haversine formula spatial query and fetch matching pins', async () => {
      const result = await service.findAllNearby(-1.2921, 36.8219, 1000);

      expect(repository.createQueryBuilder).toHaveBeenCalledWith('pin');
      expect(mockQueryBuilder.where).toHaveBeenCalledWith('pin.status = :status', { status: 'ACTIVE' });
      expect(mockQueryBuilder.andWhere).toHaveBeenCalled();
      expect(result).toEqual([]);
    });
  });

  describe('vote', () => {
    it('should increment upvotes on confirm action', async () => {
      const mockPin = {
        id: 'uuid-123',
        upvotes: 1,
        downvotes: 0,
        status: 'ACTIVE',
      };
      mockPinsRepository.findOne.mockResolvedValue(mockPin);

      const result = await service.vote('uuid-123', 'confirm');
      expect(result.upvotes).toBe(2);
    });

    it('should mark hazard as RESOLVED when receiving 3 or more cleared votes', async () => {
      const mockPin = {
        id: 'uuid-123',
        upvotes: 1,
        downvotes: 2,
        status: 'ACTIVE',
      };
      mockPinsRepository.findOne.mockResolvedValue(mockPin);

      const result = await service.vote('uuid-123', 'cleared');
      expect(result.downvotes).toBe(3);
      expect(result.status).toBe('RESOLVED');
    });
  });
});

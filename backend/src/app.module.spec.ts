import { ConfigService } from '@nestjs/config';
import { getTypeOrmModuleOptions } from './app.module';

describe('getTypeOrmModuleOptions', () => {
  it('should return config successfully when DATABASE_URL is present in the environment', () => {
    // Arrange: Mock the ConfigService to return a valid connection string
    const mockConfig = {
      get: jest.fn().mockReturnValue('postgresql://postgres:postgres@localhost:5432/test_db?sslmode=require'),
    } as unknown as ConfigService;

    // Act: Resolve TypeORM config options
    const options = getTypeOrmModuleOptions(mockConfig);

    // Assert: Verify that the configurations returned are mapped correctly
    expect(options).toBeDefined();
    expect(options.type).toBe('postgres');
    expect(options.url).toBe('postgresql://postgres:postgres@localhost:5432/test_db?sslmode=require');
    expect(options.autoLoadEntities).toBe(true);
    expect(options.synchronize).toBe(true);
    expect(options.ssl).toEqual({ rejectUnauthorized: false }); // SSL should be true when sslmode is require
  });

  it('should throw an error during compilation if DATABASE_URL is missing', () => {
    // Arrange: Mock ConfigService to return undefined (missing variable)
    const mockConfig = {
      get: jest.fn().mockReturnValue(undefined),
    } as unknown as ConfigService;

    // Act & Assert: Should throw our custom database config error message
    expect(() => getTypeOrmModuleOptions(mockConfig)).toThrow(
      'DATABASE_URL environment variable is missing from .env file',
    );
  });
});

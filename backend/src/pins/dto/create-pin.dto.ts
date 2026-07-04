import { IsNotEmpty, IsString, IsEnum, IsNumber, Min, Max, IsOptional } from 'class-validator';

/**
 * Data Transfer Object (DTO) defining the schema and validation constraints
 * for incoming REST API requests to create a new road hazard pin.
 */
export class CreatePinDto {
  
  // Ensures the title is not empty, is a string, and has no trailing whitespaces
  @IsNotEmpty({ message: 'Title is required' })
  @IsString({ message: 'Title must be a valid text string' })
  title: string;

  // The description is a string providing context about the road hazard
  @IsNotEmpty({ message: 'Description is required' })
  @IsString({ message: 'Description must be a valid text string' })
  description: string;

  // Enforces that the incoming hazard category matches exactly our database enum values
  @IsEnum(['POTHOLE', 'UNLIT_STREET', 'DANGEROUS_TRAFFIC', 'OTHER'], {
    message: 'Type must be one of: POTHOLE, UNLIT_STREET, DANGEROUS_TRAFFIC, OTHER',
  })
  type: 'POTHOLE' | 'UNLIT_STREET' | 'DANGEROUS_TRAFFIC' | 'OTHER';

  // Validates that latitude is a number, ranging between -90.0 and 90.0 degrees
  @IsNumber({}, { message: 'Latitude must be a decimal number' })
  @Min(-90, { message: 'Latitude cannot be less than -90 degrees' })
  @Max(90, { message: 'Latitude cannot be greater than 90 degrees' })
  latitude: number;

  // Validates that longitude is a number, ranging between -180.0 and 180.0 degrees
  @IsNumber({}, { message: 'Longitude must be a decimal number' })
  @Min(-180, { message: 'Longitude cannot be less than -180 degrees' })
  @Max(180, { message: 'Longitude cannot be greater than 180 degrees' })
  longitude: number;

  // The reporter username is optional (e.g. if the user reports anonymously)
  @IsOptional()
  @IsString({ message: 'Reported by must be a valid text string' })
  reportedBy?: string;

  // Optional image data URI / URL
  @IsOptional()
  @IsString({ message: 'Image URL must be a valid text string' })
  imageUrl?: string;
}

import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from './user.entity';

/**
 * Service class handling database operations for User entities.
 */
@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
  ) {}

  /**
   * Creates a new user record in the database.
   */
  async create(
    email: string | null,
    passwordHash: string | null,
    name: string | null,
    googleId: string | null,
  ): Promise<User> {
    const user = this.userRepository.create({
      email: email ? email.toLowerCase() : undefined,
      passwordHash,
      name,
      googleId,
    });
    return this.userRepository.save(user);
  }

  /**
   * Finds a user by their unique primary key ID.
   */
  async findOneById(id: string): Promise<User | null> {
    return this.userRepository.findOneBy({ id });
  }

  /**
   * Finds a user by email address (case-insensitive check).
   */
  async findOneByEmail(email: string): Promise<User | null> {
    return this.userRepository.findOneBy({ email: email.toLowerCase() });
  }

  /**
   * Finds a user by their Google account ID.
   */
  async findOneByGoogleId(googleId: string): Promise<User | null> {
    return this.userRepository.findOneBy({ googleId });
  }

  /**
   * Links a Google ID to an existing user profile.
   */
  async updateGoogleId(id: string, googleId: string): Promise<User> {
    const user = await this.findOneById(id);
    if (!user) {
      throw new Error('User not found');
    }
    user.googleId = googleId;
    return this.userRepository.save(user);
  }
}

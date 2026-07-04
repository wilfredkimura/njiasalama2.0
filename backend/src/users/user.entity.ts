import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

/**
 * User Entity maps user credential profiles directly to a Neon PostgreSQL table.
 * Supports standard local accounts (email/password) and Google Sign-In.
 */
@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  // Email is unique. It is nullable because a user signing in via Google might not provide
  // an email or we might want to register them solely by Google ID first (though Google usually provides email).
  @Column({ unique: true, nullable: true })
  email: string;

  // passwordHash is nullable to accommodate users registering exclusively via Google Sign-In.
  @Column({ nullable: true })
  passwordHash: string;

  @Column({ nullable: true })
  name: string;

  // googleId holds the unique identifier returned by Google's API to associate verified tokens.
  @Column({ unique: true, nullable: true })
  googleId: string;

  @CreateDateColumn()
  createdAt: Date;
}

import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

/**
 * TypeORM Entity class mapping to the 'danger_pins' table in our PostgreSQL database.
 * Decorating a class with @Entity tells TypeORM to automatically create and manage this table.
 */
@Entity('danger_pins')
export class DangerPin {
  
  // Generates a unique UUID (Universally Unique Identifier) string automatically for every new hazard report
  @PrimaryGeneratedColumn('uuid')
  id: string;

  // Title of the hazard (e.g. "Deep Pothole")
  @Column()
  title: string;

  // Detailed description of the hazard (e.g. "Avoid left lane")
  @Column()
  description: string;

  // Type categorizations mapped directly to a database-level ENUM.
  // Enforcing enums prevents invalid category strings from entering the database.
  @Column({
    type: 'enum',
    enum: ['POTHOLE', 'UNLIT_STREET', 'DANGEROUS_TRAFFIC', 'OTHER'],
  })
  type: 'POTHOLE' | 'UNLIT_STREET' | 'DANGEROUS_TRAFFIC' | 'OTHER';

  // High-precision geographic latitude. Stored as double precision (64-bit float) for GPS accuracy.
  @Column('double precision')
  latitude: number;

  // High-precision geographic longitude. Stored as double precision (64-bit float) for GPS accuracy.
  @Column('double precision')
  longitude: number;

  // The author or reporter username. Defaults to "Anonymous" if not logged in.
  @Column({ name: 'reported_by', default: 'Anonymous' })
  reportedBy: string;

  // Optional hazard image stored as a Base64 data URI string or URL
  @Column({ name: 'image_url', nullable: true })
  imageUrl?: string;

  // Automatically captures the timestamp when the row is first inserted
  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

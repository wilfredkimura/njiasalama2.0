import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

/**
 * TypeORM Entity class mapping to the 'danger_pins' table in our PostgreSQL database.
 */
@Entity('danger_pins')
export class DangerPin {
  
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  title: string;

  @Column()
  description: string;

  @Column({
    type: 'enum',
    enum: ['POTHOLE', 'UNLIT_STREET', 'DANGEROUS_TRAFFIC', 'OTHER'],
  })
  type: 'POTHOLE' | 'UNLIT_STREET' | 'DANGEROUS_TRAFFIC' | 'OTHER';

  @Column('double precision')
  latitude: number;

  @Column('double precision')
  longitude: number;

  @Column({ name: 'reported_by', default: 'Anonymous' })
  reportedBy: string;

  @Column({ name: 'image_url', nullable: true })
  imageUrl?: string;

  // Community verification counters
  @Column({ default: 1 })
  upvotes: number;

  @Column({ default: 0 })
  downvotes: number;

  // Lifecycle status: active hazards vs. community-cleared obstacles
  @Column({
    type: 'enum',
    enum: ['ACTIVE', 'RESOLVED'],
    default: 'ACTIVE',
  })
  status: 'ACTIVE' | 'RESOLVED';

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

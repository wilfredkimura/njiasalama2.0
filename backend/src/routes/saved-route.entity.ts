import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, ManyToOne, JoinColumn } from 'typeorm';
import { User } from '../users/user.entity';
import { RoutePoint, SurfaceType } from '../route.interface';

@Entity('saved_routes')
export class SavedRoute {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ name: 'user_id' })
  userId: string;

  @Column()
  name: string;

  @Column('double precision', { name: 'start_lat' })
  startLat: number;

  @Column('double precision', { name: 'start_lng' })
  startLng: number;

  @Column('double precision', { name: 'end_lat' })
  endLat: number;

  @Column('double precision', { name: 'end_lng' })
  endLng: number;

  @Column('jsonb')
  points: RoutePoint[];

  @Column({
    type: 'enum',
    enum: SurfaceType,
    name: 'surface_type',
  })
  surfaceType: SurfaceType;

  @Column('double precision', { name: 'distance_km' })
  distanceKm: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

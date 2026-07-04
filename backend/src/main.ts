import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors(); // Enable Cross-Origin Resource Sharing for production network requests
  await app.listen(process.env.PORT ?? 3000, '0.0.0.0');
}
bootstrap();

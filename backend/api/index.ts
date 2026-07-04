import { NestFactory } from '@nestjs/core';
import { AppModule } from '../src/app.module';
import { ExpressAdapter } from '@nestjs/platform-express';
import express from 'express';

// Create a local Express server instance
const server = express();
let isInitialized = false;

/**
 * Bootstraps the NestJS app inside the Express server.
 * Runs once and caches the configuration state.
 */
async function bootstrap() {
  const app = await NestFactory.create(
    AppModule,
    new ExpressAdapter(server)
  );
  app.enableCors(); // Enable CORS for remote client requests
  await app.init(); // Wait for NestJS initialization hooks to finish
}

/**
 * Vercel Serverless Function entry point handler.
 */
export default async (req: any, res: any) => {
  if (!isInitialized) {
    await bootstrap();
    isInitialized = true;
  }
  // Delegate request/response processing back to NestJS-managed routes
  server(req, res);
};

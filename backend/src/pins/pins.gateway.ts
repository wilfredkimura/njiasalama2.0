import {
  WebSocketGateway,
  WebSocketServer,
  OnGatewayConnection,
  OnGatewayDisconnect,
  OnGatewayInit,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger } from '@nestjs/common';
import { DangerPin } from './pins.entity';

/**
 * NestJS Gateway class acting as our real-time Socket.io communication channel.
 * Decorating a class with @WebSocketGateway binds it to standard socket connections.
 * CORS is enabled with origin '*' to allow incoming connections from Android emulators and web clients.
 */
@WebSocketGateway({
  cors: {
    origin: '*',
  },
})
export class PinsGateway
  implements OnGatewayInit, OnGatewayConnection, OnGatewayDisconnect
{
  // A clean Logger utility to print structural diagnostic logs in the terminal console
  private readonly logger = new Logger(PinsGateway.name);

  // Inject the parent Socket.io Server instance dynamically
  @WebSocketServer()
  server: Server;

  /**
   * Automatically triggered when the WebSocket Gateway finishes loading and initializes.
   */
  afterInit(server: Server) {
    this.logger.log('Real-Time WebSockets Gateway initialized successfully!');
  }

  /**
   * Automatically triggered when a new client (e.g. Android App or Web Dashboard) connects.
   */
  handleConnection(client: Socket) {
    this.logger.log(`Cyclist client connected: ${client.id}`);
  }

  /**
   * Automatically triggered when a client disconnects (e.g. app minimized or closed).
   */
  handleDisconnect(client: Socket) {
    this.logger.log(`Cyclist client disconnected: ${client.id}`);
  }

  /**
   * Broadcasts newly reported road hazard pins in real-time to all currently connected socket clients.
   * Emits a 'newPin' event containing the DangerPin payload.
   */
  broadcastNewPin(pin: DangerPin) {
    this.logger.log(`Broadcasting new pin report to all connected clients: ${pin.title}`);
    // emit sends a message name ('newPin') and payload (pin details) to all active connections
    this.server.emit('newPin', pin);
  }
}

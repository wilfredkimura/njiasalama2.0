import { Test, TestingModule } from '@nestjs/testing';
import { PinsGateway } from './pins.gateway';
import { Server, Socket } from 'socket.io';
import { DangerPin } from './pins.entity';

describe('PinsGateway', () => {
  let gateway: PinsGateway;

  // Mocking the parent Socket.io Server instance
  const mockServer = {
    emit: jest.fn(),
  } as unknown as Server;

  // Mocking an individual client socket connection
  const mockSocket = {
    id: 'socket-client-999',
  } as unknown as Socket;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [PinsGateway],
    }).compile();

    gateway = module.get<PinsGateway>(PinsGateway);
    gateway.server = mockServer; // Inject the mocked server instance
  });

  it('should be defined', () => {
    expect(gateway).toBeDefined();
  });

  describe('broadcastNewPin', () => {
    it('should emit a "newPin" event containing the DangerPin payload to all listeners', () => {
      const mockPin: DangerPin = {
        id: 'pin-uuid-xyz',
        title: 'Dangerous Pothole',
        description: 'Deep road hazard',
        type: 'POTHOLE',
        latitude: -1.2921,
        longitude: 36.8219,
        reportedBy: 'Kimura',
        createdAt: new Date(),
      };

      gateway.broadcastNewPin(mockPin);

      // Verify that server.emit was invoked with event name 'newPin' and our pin payload
      expect(mockServer.emit).toHaveBeenCalledWith('newPin', mockPin);
    });
  });

  describe('lifecycle hooks', () => {
    it('should handle connections and disconnections without throwing exceptions', () => {
      // Confirm that executing lifecycle logging triggers doesn't raise errors
      expect(() => gateway.afterInit(mockServer)).not.toThrow();
      expect(() => gateway.handleConnection(mockSocket)).not.toThrow();
      expect(() => gateway.handleDisconnect(mockSocket)).not.toThrow();
    });
  });
});

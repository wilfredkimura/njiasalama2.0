package com.njiasalama.data.websocket

import com.njiasalama.domain.model.DangerPin
import kotlinx.coroutines.flow.Flow

/**
 * Interface managing Socket.io server connection handshakes and updates emission.
 * Decoupling websocket clients behind interfaces makes testing and codebase changes easy.
 */
interface SocketManager {

    /**
     * Establishes a WebSocket connection with the backend server.
     */
    fun connect()

    /**
     * Closes the active WebSocket connection.
     */
    fun disconnect()

    /**
     * Exposes a Flow emitting DangerPin elements in real-time as they are broadcast by the backend.
     */
    fun getNewPinFlow(): Flow<DangerPin>
}

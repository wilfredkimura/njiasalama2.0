package com.njiasalama.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.njiasalama.domain.model.DangerPin
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.URISyntaxException

/**
 * Concrete implementation of SocketManager managing socket handshakes and mapping events to Kotlin Flows.
 * Uses callbackFlow to transform callback-based Socket.IO events into Coroutines Streams.
 */
class SocketManagerImpl(
    private val socketUrl: String = com.njiasalama.BuildConfig.BACKEND_URL,
    private val gson: Gson = Gson()
) : SocketManager {

    private var socket: Socket? = null

    override fun connect() {
        if (socket != null && socket!!.connected()) {
            return
        }
        try {
            // IO.socket() builds the socket client instance matching our base path
            socket = IO.socket(socketUrl)
            
            // Initiate handshake connection
            socket?.connect()
            Log.d("SocketManager", "Initiating WebSocket connection to: $socketUrl")
        } catch (e: URISyntaxException) {
            Log.e("SocketManager", "Invalid WebSocket server URL syntax: $socketUrl", e)
        }
    }

    override fun disconnect() {
        socket?.disconnect()
        socket = null
        Log.d("SocketManager", "Disconnected from WebSocket server")
    }

    override fun getNewPinFlow(): Flow<DangerPin> = callbackFlow {
        // Ensure connection is established before listening
        val currentSocket = socket ?: run {
            connect()
            socket
        }

        if (currentSocket == null) {
            close(IllegalStateException("Socket client could not be initialized"))
            return@callbackFlow
        }

        // Event listener mapping incoming JSON objects to domain entities
        val listener = { args: Array<Any> ->
            if (args.isNotEmpty()) {
                try {
                    // Socket.io client returns payload arguments. We extract the raw string representation
                    // and map it into our DangerPin data class using Gson parser
                    val rawJson = args[0].toString()
                    val pin = gson.fromJson(rawJson, DangerPin::class.java)
                    
                    // Offer/send the parsed DangerPin downstream to active Flow collectors
                    trySend(pin)
                } catch (e: Exception) {
                    Log.e("SocketManager", "Failed to deserialize broadcasted DangerPin payload", e)
                }
            }
        }

        // Attach listener to catch the real-time 'newPin' broadcasts
        currentSocket.on("newPin", listener)

        // The awaitClose block is triggered automatically when the flow collector unsubscribes/cancels.
        // We remove our event callback listener here to prevent memory leaks in the application.
        awaitClose {
            currentSocket.off("newPin", listener)
        }
    }
}

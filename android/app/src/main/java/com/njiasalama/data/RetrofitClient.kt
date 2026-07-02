package com.njiasalama.data

import com.njiasalama.data.websocket.SocketManager
import com.njiasalama.data.websocket.SocketManagerImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton network provider coordinating Retrofit and Repository instances.
 * Acts as a simple, lightweight dependency container.
 */
object RetrofitClient {
    
    // '192.168.1.5' is the local Wi-Fi IP address of the host machine running the NestJS server.
    private const val BASE_URL = "http://192.168.1.5:3000/"

    // Lazily builds the Retrofit API instance on first access
    val api: NjiaSalamaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NjiaSalamaApi::class.java)
    }

    // Lazily builds the PinRepository implementation mapping our API client
    val pinRepository: PinRepositoryImpl by lazy {
        PinRepositoryImpl(api)
    }

    // Lazily builds the SocketManager client mapping our WebSockets server connection
    val socketManager: SocketManager by lazy {
        SocketManagerImpl(socketUrl = BASE_URL)
    }
}

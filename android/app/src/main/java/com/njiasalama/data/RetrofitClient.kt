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
    
    // Dynamically retrieve the backend server URL generated at build time from local.properties config
    private val BASE_URL = com.njiasalama.BuildConfig.BACKEND_URL

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

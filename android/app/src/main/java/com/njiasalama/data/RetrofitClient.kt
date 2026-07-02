package com.njiasalama.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton network provider coordinating Retrofit and Repository instances.
 * Acts as a simple, lightweight dependency container.
 */
object RetrofitClient {
    
    // '10.0.2.2' is the standard loopback IP address mapping the Android emulator to the host machine's localhost (Port 3000)
    private const val BASE_URL = "http://10.0.2.2:3000/"

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
}

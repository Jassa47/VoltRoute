package com.example.voltroute.di

import com.example.voltroute.BuildConfig
import com.example.voltroute.data.remote.api.DirectionsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt dependency injection module for application-wide dependencies
 *
 * Provides:
 * - Network clients (OkHttp, Retrofit)
 * - API interfaces (DirectionsApi)
 * - Configuration values (API key)
 *
 * All dependencies are singletons since they're stateless and expensive to create
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provide Google Maps API key from BuildConfig
     *
     * The key is loaded from local.properties by the secrets-gradle-plugin
     * and exposed via BuildConfig.MAPS_API_KEY
     *
     * @Named qualifier allows injecting this specific String value
     */
    @Provides
    @Singleton
    @Named("maps_api_key")
    fun provideMapsApiKey(): String {
        return BuildConfig.MAPS_API_KEY
    }

    /**
     * Provide configured OkHttpClient
     *
     * Features:
     * - HTTP logging in debug builds (helps with development/debugging)
     * - 30 second timeouts (Google APIs can be slow on poor connections)
     * - Automatic retry handling
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Log full request/response bodies in debug, nothing in release
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provide configured Retrofit instance
     *
     * Retrofit converts HTTP API into a Java/Kotlin interface
     *
     * @param okHttpClient Provided OkHttpClient for network calls
     * @return Configured Retrofit instance for Google Maps APIs
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provide DirectionsApi implementation
     *
     * Retrofit generates the implementation from the interface definition
     *
     * @param retrofit Configured Retrofit instance
     * @return DirectionsApi implementation ready for use
     */
    @Provides
    @Singleton
    fun provideDirectionsApi(retrofit: Retrofit): DirectionsApi {
        return retrofit.create(DirectionsApi::class.java)
    }
}


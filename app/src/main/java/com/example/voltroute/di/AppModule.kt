package com.example.voltroute.di

import android.content.Context
import androidx.room.Room
import com.example.voltroute.BuildConfig
import com.example.voltroute.data.local.dao.ChargerDao
import com.example.voltroute.data.local.dao.ChargingPlanDao
import com.example.voltroute.data.local.dao.RouteDao
import com.example.voltroute.data.local.database.VoltRouteDatabase
import com.example.voltroute.data.remote.api.DirectionsApi
import com.example.voltroute.data.remote.api.OpenChargeMapApi
import com.google.gson.Gson
import dagger.Module
import dagger.hilt.android.qualifiers.ApplicationContext
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
     * Provide configured Retrofit instance for Google Maps APIs
     *
     * Retrofit converts HTTP API into a Java/Kotlin interface
     *
     * @param okHttpClient Provided OkHttpClient for network calls
     * @return Configured Retrofit instance for Google Maps APIs
     */
    @Provides
    @Singleton
    @Named("google_retrofit")
    fun provideGoogleRetrofit(okHttpClient: OkHttpClient): Retrofit {
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
     * @param retrofit Configured Retrofit instance for Google Maps
     * @return DirectionsApi implementation ready for use
     */
    @Provides
    @Singleton
    fun provideDirectionsApi(
        @Named("google_retrofit") retrofit: Retrofit
    ): DirectionsApi {
        return retrofit.create(DirectionsApi::class.java)
    }

    /**
     * Provide Open Charge Map API key from BuildConfig
     *
     * The key is loaded from local.properties by the secrets-gradle-plugin
     * and exposed via BuildConfig.OPEN_CHARGE_MAP_KEY
     */
    @Provides
    @Singleton
    @Named("ocm_api_key")
    fun provideOcmApiKey(): String {
        return BuildConfig.OPEN_CHARGE_MAP_KEY
    }

    /**
     * Provide configured Retrofit instance for Open Charge Map API
     *
     * @param okHttpClient Provided OkHttpClient for network calls
     * @return Configured Retrofit instance for Open Charge Map
     */
    @Provides
    @Singleton
    @Named("ocm_retrofit")
    fun provideOcmRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openchargemap.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provide OpenChargeMapApi implementation
     *
     * Retrofit generates the implementation from the interface definition
     *
     * @param retrofit Configured Retrofit instance for Open Charge Map
     * @return OpenChargeMapApi implementation ready for use
     */
    @Provides
    @Singleton
    fun provideOpenChargeMapApi(
        @Named("ocm_retrofit") retrofit: Retrofit
    ): OpenChargeMapApi {
        return retrofit.create(OpenChargeMapApi::class.java)
    }

    /**
     * Provide Gson instance for JSON serialization/deserialization
     *
     * Used by CacheRepository to serialize/deserialize complex objects:
     * - List<String> connectorTypes in ChargerEntity
     * - Complete ChargingPlan with nested objects in ChargingPlanEntity
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * Provide Room database instance
     *
     * Creates/opens the local SQLite database for offline caching.
     * Database name: "voltroute_database"
     *
     * @param context Application context
     * @return VoltRouteDatabase instance
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VoltRouteDatabase {
        return Room.databaseBuilder(
            context,
            VoltRouteDatabase::class.java,
            "voltroute_database"
        ).build()
    }

    /**
     * Provide RouteDao for route cache operations
     *
     * Note: No @Singleton annotation - Room manages DAO lifecycle internally
     *
     * @param database VoltRouteDatabase instance
     * @return RouteDao implementation
     */
    @Provides
    fun provideRouteDao(database: VoltRouteDatabase): RouteDao {
        return database.routeDao()
    }

    /**
     * Provide ChargerDao for charger cache operations
     *
     * Note: No @Singleton annotation - Room manages DAO lifecycle internally
     *
     * @param database VoltRouteDatabase instance
     * @return ChargerDao implementation
     */
    @Provides
    fun provideChargerDao(database: VoltRouteDatabase): ChargerDao {
        return database.chargerDao()
    }

    /**
     * Provide ChargingPlanDao for charging plan cache operations
     *
     * Note: No @Singleton annotation - Room manages DAO lifecycle internally
     *
     * @param database VoltRouteDatabase instance
     * @return ChargingPlanDao implementation
     */
    @Provides
    fun provideChargingPlanDao(database: VoltRouteDatabase): ChargingPlanDao {
        return database.chargingPlanDao()
    }
}


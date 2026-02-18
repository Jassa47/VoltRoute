package com.example.voltroute.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.voltroute.BuildConfig
import com.example.voltroute.data.local.dao.ChargerDao
import com.example.voltroute.data.local.dao.ChargingPlanDao
import com.example.voltroute.data.local.dao.RouteDao
import com.example.voltroute.data.local.dao.TripHistoryDao
import com.example.voltroute.data.local.database.VoltRouteDatabase
import com.example.voltroute.data.remote.api.DirectionsApi
import com.example.voltroute.data.remote.api.OpenChargeMapApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
 * Room database migration from version 1 to version 2
 *
 * Version 1: Cache tables only (routes, chargers, charging plans)
 * Version 2: Added trip_history table
 *
 * CRITICAL: SQL must exactly match TripHistoryEntity field types:
 * - Kotlin Long/Int → SQL INTEGER
 * - Kotlin Double/Float → SQL REAL
 * - Kotlin String → SQL TEXT
 * - Kotlin Boolean → SQL INTEGER (Room stores as 0/1)
 *
 * This migration is applied when:
 * - User has existing database at version 1
 * - App is updated with version 2 schema
 * - Room detects version mismatch
 *
 * Without this migration, Room will throw:
 * "Migration didn't properly handle: trip_history"
 * and the app will crash on database open.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS trip_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                destinationAddress TEXT NOT NULL,
                distanceKm REAL NOT NULL,
                durationMinutes INTEGER NOT NULL,
                startBatteryPercent REAL NOT NULL,
                vehicleName TEXT NOT NULL,
                chargingStopsCount INTEGER NOT NULL,
                totalChargingTimeMinutes INTEGER NOT NULL,
                estimatedCostDollars REAL NOT NULL,
                energyUsedKWh REAL NOT NULL,
                tripDate INTEGER NOT NULL
            )
        """)
    }
}

/**
 * Hilt dependency injection module for application-wide dependencies
 *
 * Provides:
 * - Network clients (OkHttp, Retrofit)
 * - API interfaces (DirectionsApi, OpenChargeMapApi)
 * - Database (Room with migration support)
 * - DAOs (RouteDao, ChargerDao, ChargingPlanDao, TripHistoryDao)
 * - Configuration values (API keys)
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
     * Creates/opens the local SQLite database for offline caching and trip history.
     * Database name: "voltroute_database"
     *
     * Includes migration from version 1 to 2 (adds trip_history table).
     * Without the migration, Room would throw an error on schema mismatch.
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
        )
        .addMigrations(MIGRATION_1_2)
        .build()
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

    /**
     * Provide TripHistoryDao for trip history operations
     *
     * Note: No @Singleton annotation - Room manages DAO lifecycle internally
     *
     * @param database VoltRouteDatabase instance
     * @return TripHistoryDao implementation
     */
    @Provides
    fun provideTripHistoryDao(database: VoltRouteDatabase): TripHistoryDao {
        return database.tripHistoryDao()
    }

    /**
     * Provide Firebase Authentication instance
     *
     * FirebaseAuth.getInstance() returns a singleton managed by Firebase SDK.
     * Safe to call multiple times - always returns the same instance.
     *
     * Used for:
     * - Email/Password authentication
     * - Phone number authentication
     * - Auth state monitoring
     * - User session management
     *
     * @return FirebaseAuth singleton instance
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provide Firebase Firestore instance
     *
     * FirebaseFirestore.getInstance() returns a singleton managed by Firebase SDK.
     * Safe to call multiple times - always returns the same instance.
     *
     * Used for:
     * - User profile storage
     * - Real-time data sync
     * - Cloud database operations
     *
     * @return FirebaseFirestore singleton instance
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}


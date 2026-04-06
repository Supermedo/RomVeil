package com.glyph.launcher.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.glyph.launcher.BuildConfig
import com.glyph.launcher.data.local.GameDao
import com.glyph.launcher.data.local.GlyphDatabase
import com.glyph.launcher.data.remote.MobyGamesApi
import com.glyph.launcher.data.remote.RawgApi
import com.glyph.launcher.data.remote.ScreenScraperApi
import com.glyph.launcher.data.remote.TheGamesDbApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TheGamesDbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RawgRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MobyGamesRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ScreenScraperRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
    }

    @Provides
    @Singleton
    @TheGamesDbRetrofit
    fun provideTheGamesDbRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.thegamesdb.net/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideTheGamesDbApi(@TheGamesDbRetrofit retrofit: Retrofit): TheGamesDbApi {
        return retrofit.create(TheGamesDbApi::class.java)
    }

    @Provides
    @Singleton
    @RawgRetrofit
    fun provideRawgRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.rawg.io/api/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideRawgApi(@RawgRetrofit retrofit: Retrofit): RawgApi {
        return retrofit.create(RawgApi::class.java)
    }

    @Provides
    @Singleton
    @MobyGamesRetrofit
    fun provideMobyGamesRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.mobygames.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideMobyGamesApi(@MobyGamesRetrofit retrofit: Retrofit): MobyGamesApi {
        return retrofit.create(MobyGamesApi::class.java)
    }

    @Provides
    @Singleton
    @ScreenScraperRetrofit
    fun provideScreenScraperRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.screenscraper.fr/api2/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideScreenScraperApi(@ScreenScraperRetrofit retrofit: Retrofit): ScreenScraperApi {
        return retrofit.create(ScreenScraperApi::class.java)
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN rating REAL")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Room stores boolean as INTEGER (0 = false, 1 = true)
            db.execSQL("ALTER TABLE games ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GlyphDatabase {
        return Room.databaseBuilder(
            context,
            GlyphDatabase::class.java,
            "glyph_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideGameDao(database: GlyphDatabase): GameDao {
        return database.gameDao()
    }
}

package com.shamsalmaarif.reader.di

import android.content.Context
import androidx.room.Room
import com.shamsalmaarif.reader.data.database.AppDatabase
import com.shamsalmaarif.reader.data.database.dao.ReadsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "shams_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideReadsDao(db: AppDatabase): ReadsDao = db.readsDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}

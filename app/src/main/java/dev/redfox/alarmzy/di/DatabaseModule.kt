package dev.redfox.alarmzy.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.redfox.alarmzy.data.local.AlarmzyDatabase
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import dev.redfox.alarmzy.data.local.dao.GroupDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AlarmzyDatabase =
        Room.databaseBuilder(
            context,
            AlarmzyDatabase::class.java,
            "alarmzy.db"
        ).build()

    @Provides
    fun provideAlarmDao(database: AlarmzyDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun provideGroupDao(database: AlarmzyDatabase): GroupDao = database.groupDao()
}

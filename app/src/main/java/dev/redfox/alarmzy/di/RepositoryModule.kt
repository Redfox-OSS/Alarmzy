package dev.redfox.alarmzy.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.redfox.alarmzy.data.repository.AlarmRepositoryImpl
import dev.redfox.alarmzy.data.repository.GroupRepositoryImpl
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import dev.redfox.alarmzy.domain.repository.GroupRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository
}

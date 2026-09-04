package com.aalaya.pg.di

import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.core.network.SessionInvalidator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {
    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {
    /** The authenticator needs to signal sign-out; SessionManager provides it. */
    @Binds
    @Singleton
    abstract fun sessionInvalidator(impl: SessionManager): SessionInvalidator
}

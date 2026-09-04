package com.aalaya.pg.di

import javax.inject.Qualifier

/** Qualifies the application-lifetime CoroutineScope (survives all screens). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

package com.aalaya.pg.di

import javax.inject.Qualifier

/** The main authenticated OkHttp client (default, unqualified) vs the bare one below. */

/** A client with NO authenticator, used only for the token-refresh call. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BareClient

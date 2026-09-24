package com.samuelbaldasso.financeflow.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.samuelbaldasso.financeflow.data.datastore.SecurityPreferencesDataSource
import com.samuelbaldasso.financeflow.data.datastore.securityDataStore
import com.samuelbaldasso.financeflow.domain.security.AppLockManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.securityDataStore
    }

    @Provides
    @Singleton
    fun provideSecurityPreferencesDataSource(dataStore: DataStore<Preferences>): SecurityPreferencesDataSource {
        return SecurityPreferencesDataSource(dataStore)
    }

    @Provides
    @Singleton
    fun provideAppLockManager(): AppLockManager {
        return AppLockManager()
    }
}

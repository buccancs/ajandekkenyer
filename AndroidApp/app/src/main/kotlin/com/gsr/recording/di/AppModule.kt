package com.gsr.recording.di

import android.content.Context
import com.gsr.recording.shimmer.ShimmerManager
import com.gsr.recording.thermal.ThermalCameraManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideShimmerManager(@ApplicationContext context: Context): ShimmerManager {
        return ShimmerManager(context)
    }
    
    @Provides
    @Singleton
    fun provideThermalCameraManager(@ApplicationContext context: Context): ThermalCameraManager {
        return ThermalCameraManager(context)
    }
}
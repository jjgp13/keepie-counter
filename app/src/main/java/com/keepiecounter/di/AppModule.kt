package com.keepiecounter.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Dependencies will be provided here as we build each phase:
    // - Phase 3: BallDetector, BallTracker
    // - Phase 4: PoseDetector, KickDetector
    // - Phase 5: KeepieCounter
    // - Phase 7: AppDatabase, SessionDao, SessionRepository
}

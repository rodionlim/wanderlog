package com.wanderlog.android.core.di

import com.wanderlog.android.data.repository.AiRepositoryImpl
import com.wanderlog.android.data.repository.AttachmentRepositoryImpl
import com.wanderlog.android.data.repository.ExpenseRepositoryImpl
import com.wanderlog.android.data.repository.ItineraryRepositoryImpl
import com.wanderlog.android.data.repository.PackingRepositoryImpl
import com.wanderlog.android.data.repository.PlacesRepositoryImpl
import com.wanderlog.android.data.repository.TripRepositoryImpl
import com.wanderlog.android.domain.repository.AiRepository
import com.wanderlog.android.domain.repository.AttachmentRepository
import com.wanderlog.android.domain.repository.ExpenseRepository
import com.wanderlog.android.domain.repository.ItineraryRepository
import com.wanderlog.android.domain.repository.PackingRepository
import com.wanderlog.android.domain.repository.PlacesRepository
import com.wanderlog.android.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository
    @Binds @Singleton abstract fun bindItineraryRepository(impl: ItineraryRepositoryImpl): ItineraryRepository
    @Binds @Singleton abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds @Singleton abstract fun bindPackingRepository(impl: PackingRepositoryImpl): PackingRepository
    @Binds @Singleton abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
    @Binds @Singleton abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository
    @Binds @Singleton abstract fun bindAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository
}

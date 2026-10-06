package com.iocl.knowledgehub.di

import com.iocl.knowledgehub.data.repository.*
import com.iocl.knowledgehub.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMagazineRepository(impl: MagazineRepositoryImpl): MagazineRepository

    @Binds
    @Singleton
    abstract fun bindNewspaperRepository(impl: NewspaperRepositoryImpl): NewspaperRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindContentIntelligenceRepository(
        impl: ContentIntelligenceRepositoryImpl
    ): ContentIntelligenceRepository
}

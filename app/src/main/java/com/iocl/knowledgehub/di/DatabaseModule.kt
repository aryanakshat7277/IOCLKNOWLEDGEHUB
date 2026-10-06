package com.iocl.knowledgehub.di

import android.content.Context
import androidx.room.Room
import com.iocl.knowledgehub.data.local.IOCLDatabase
import com.iocl.knowledgehub.data.local.dao.BookmarkDao
import com.iocl.knowledgehub.data.local.dao.MagazineDao
import com.iocl.knowledgehub.data.local.dao.NewspaperDao
import com.iocl.knowledgehub.data.local.dao.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IOCLDatabase {
        return Room.databaseBuilder(
            context,
            IOCLDatabase::class.java,
            "iocl_knowledge_hub.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMagazineDao(database: IOCLDatabase): MagazineDao = database.magazineDao()

    @Provides
    fun provideNewspaperDao(database: IOCLDatabase): NewspaperDao = database.newspaperDao()

    @Provides
    fun provideBookmarkDao(database: IOCLDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideNotificationDao(database: IOCLDatabase): NotificationDao = database.notificationDao()
}

package com.iocl.knowledgehub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iocl.knowledgehub.data.local.dao.BookmarkDao
import com.iocl.knowledgehub.data.local.dao.MagazineDao
import com.iocl.knowledgehub.data.local.dao.NewspaperDao
import com.iocl.knowledgehub.data.local.dao.NotificationDao
import com.iocl.knowledgehub.data.local.entity.BookmarkEntity
import com.iocl.knowledgehub.data.local.entity.MagazineEntity
import com.iocl.knowledgehub.data.local.entity.NewspaperEntity
import com.iocl.knowledgehub.data.local.entity.NotificationEntity

@Database(
    entities = [
        MagazineEntity::class,
        NewspaperEntity::class,
        BookmarkEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IOCLDatabase : RoomDatabase() {
    abstract fun magazineDao(): MagazineDao
    abstract fun newspaperDao(): NewspaperDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "iocl_knowledge_hub_db"
    }
}

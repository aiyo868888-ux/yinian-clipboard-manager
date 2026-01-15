package com.yinian.clipboard.di

import android.content.Context
import com.yinian.clipboard.data.ClipboardDao
import com.yinian.clipboard.data.ClipboardDatabase
import com.yinian.clipboard.data.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideClipboardDatabase(
        @ApplicationContext context: Context
    ): ClipboardDatabase {
        return ClipboardDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideClipboardDao(database: ClipboardDatabase): ClipboardDao {
        return database.clipboardDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: ClipboardDatabase): TagDao {
        return database.tagDao()
    }
}

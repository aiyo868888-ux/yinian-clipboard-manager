package com.yinian.clipboard.data

import android.content.Context
import com.yinian.clipboard.BuildConfig
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 剪贴板数据库
 */
@Database(
    entities = [ClipboardEntity::class, TagEntity::class, ClipboardTagEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ClipboardDatabase : RoomDatabase() {

    abstract fun clipboardDao(): ClipboardDao
    abstract fun tagDao(): TagDao

    companion object {
        private const val DATABASE_NAME = "clipboard_database"

        // 数据库迁移：从版本1到版本2（添加标签表）
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建 tags 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_tag_name ON tags (name)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_name_unique ON tags (name)")

                // 创建 clipboard_tags 关联表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS clipboard_tags (
                        clipboard_id INTEGER NOT NULL,
                        tag_id INTEGER NOT NULL,
                        PRIMARY KEY(clipboard_id, tag_id),
                        FOREIGN KEY(clipboard_id) REFERENCES clipboard_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_clipboard_id ON clipboard_tags (clipboard_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_tag_id ON clipboard_tags (tag_id)")
            }
        }

        @Volatile
        private var INSTANCE: ClipboardDatabase? = null

        fun getInstance(context: Context): ClipboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    ClipboardDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())

                // 仅在DEBUG模式下允许破坏性迁移（开发阶段）
                if (BuildConfig.DEBUG_MODE) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库回调，用于初始化数据
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 数据库创建时的初始化操作
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // 数据库打开时的操作
            }
        }
    }
}

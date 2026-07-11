package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TopicEntity::class,
        StudyPlanEntity::class,
        ModuleEntity::class,
        ChatMessageEntity::class,
        ForumQuestionEntity::class,
        ForumAnswerEntity::class,
        ProjectEntity::class,
        GamificationProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun moduleDao(): ModuleDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun forumDao(): ForumDao
    abstract fun projectDao(): ProjectDao
    abstract fun gamificationDao(): GamificationDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learn_anything_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,
    val title: String,
    val targetAudience: String,
    val duration: String,
    val totalModules: Int,
    val completedModules: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studyPlanId: Int,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val isCompleted: Boolean = false,
    val lessonContent: String? = null,
    val quizJson: String? = null,
    val quizScore: Int? = null,
    val totalQuizQuestions: Int? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,
    val role: String, // "user" or "model"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forum_questions")
data class ForumQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val authorName: String,
    val votes: Int = 0,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forum_answers")
data class ForumAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionId: Int,
    val content: String,
    val authorName: String,
    val votes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,
    val topicTitle: String,
    val title: String,
    val description: String,
    val stepsJson: String, // JSON array of project steps
    val currentStepIndex: Int = 0,
    val isCompleted: Boolean = false,
    val assistantChatJson: String = "[]", // JSON array of chat messages
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "gamification_profiles")
data class GamificationProfileEntity(
    @PrimaryKey val id: Int = 1,
    val points: Int = 0,
    val completedLessonsCount: Int = 0,
    val completedProjectsCount: Int = 0,
    val badgesJson: String = "[]", // JSON array of earned badge titles
    val gmail: String? = null,
    val age: Int? = null,
    val streakCount: Int = 12,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val dailyVideosJson: String = "[]", // JSON containing daily video tasks and quizzes
    val claimedPrizesJson: String = "[]" // JSON array of unlocked/claimed rewards
)


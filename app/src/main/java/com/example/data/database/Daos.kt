package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY timestamp DESC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Int)
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun getAllStudyPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun getStudyPlansByTopicId(topicId: Int): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE id = :id")
    suspend fun getStudyPlanById(id: Int): StudyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlan(studyPlan: StudyPlanEntity): Long

    @Update
    suspend fun updateStudyPlan(studyPlan: StudyPlanEntity)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deleteStudyPlanById(id: Int)
}

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules WHERE studyPlanId = :studyPlanId ORDER BY orderIndex ASC")
    fun getModulesByStudyPlanId(studyPlanId: Int): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModuleById(id: Int): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModuleEntity): Long

    @Update
    suspend fun updateModule(module: ModuleEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE topicId = :topicId ORDER BY timestamp ASC")
    fun getChatMessagesByTopicId(topicId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE topicId = :topicId")
    suspend fun deleteChatMessagesByTopicId(topicId: Int)
}

@Dao
interface ForumDao {
    @Query("SELECT * FROM forum_questions ORDER BY timestamp DESC")
    fun getAllQuestions(): Flow<List<ForumQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: ForumQuestionEntity): Long

    @Query("UPDATE forum_questions SET votes = votes + 1 WHERE id = :id")
    suspend fun upvoteQuestion(id: Int)

    @Query("SELECT * FROM forum_answers WHERE questionId = :questionId ORDER BY votes DESC, timestamp ASC")
    fun getAnswersForQuestion(questionId: Int): Flow<List<ForumAnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: ForumAnswerEntity): Long

    @Query("UPDATE forum_answers SET votes = votes + 1 WHERE id = :id")
    suspend fun upvoteAnswer(id: Int)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE topicId = :topicId LIMIT 1")
    suspend fun getProjectByTopicId(topicId: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_profiles WHERE id = 1")
    fun getProfile(): Flow<GamificationProfileEntity?>

    @Query("SELECT * FROM gamification_profiles WHERE id = 1")
    suspend fun getProfileSync(): GamificationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GamificationProfileEntity)
}


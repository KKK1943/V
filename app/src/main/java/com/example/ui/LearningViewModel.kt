package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChatMessageEntity
import com.example.data.database.ModuleEntity
import com.example.data.database.StudyPlanEntity
import com.example.data.database.TopicEntity
import com.example.data.database.ForumQuestionEntity
import com.example.data.database.ForumAnswerEntity
import com.example.data.database.ProjectEntity
import com.example.data.database.GamificationProfileEntity
import com.example.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    DISCOVER,
    STUDY_PLANS,
    CLASSROOM,
    PROJECTS,
    FORUM,
    GAMIFICATION
}

data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class LeaderboardEntry(
    val name: String,
    val points: Int,
    val isUser: Boolean = false
)

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = LearningRepository(
        topicDao = database.topicDao(),
        studyPlanDao = database.studyPlanDao(),
        moduleDao = database.moduleDao(),
        chatMessageDao = database.chatMessageDao(),
        forumDao = database.forumDao(),
        projectDao = database.projectDao(),
        gamificationDao = database.gamificationDao()
    )

    // UI Tab State
    var selectedTab by mutableStateOf(AppTab.DISCOVER)
        private set

    fun setTab(tab: AppTab) {
        selectedTab = tab
    }

    // List of all topics
    val topics: StateFlow<List<TopicEntity>> = repository.allTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all study plans
    val studyPlans: StateFlow<List<StudyPlanEntity>> = repository.allStudyPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Active Topic
    private val _activeTopic = MutableStateFlow<TopicEntity?>(null)
    val activeTopic: StateFlow<TopicEntity?> = _activeTopic.asStateFlow()

    // Active Chat history for the selected topic
    val activeTopicChat: StateFlow<List<ChatMessageEntity>> = _activeTopic
        .flatMapLatest { topic ->
            if (topic != null) repository.getChatMessages(topic.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Study Plan
    private val _activeStudyPlan = MutableStateFlow<StudyPlanEntity?>(null)
    val activeStudyPlan: StateFlow<StudyPlanEntity?> = _activeStudyPlan.asStateFlow()

    // Modules for active study plan
    val activePlanModules: StateFlow<List<ModuleEntity>> = _activeStudyPlan
        .flatMapLatest { plan ->
            if (plan != null) repository.getModulesForPlan(plan.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Active Lesson / Module
    private val _activeModule = MutableStateFlow<ModuleEntity?>(null)
    val activeModule: StateFlow<ModuleEntity?> = _activeModule.asStateFlow()

    // --- NEW FORUM STATES ---
    val forumQuestions: StateFlow<List<ForumQuestionEntity>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeQuestion = MutableStateFlow<ForumQuestionEntity?>(null)
    val activeQuestion: StateFlow<ForumQuestionEntity?> = _activeQuestion.asStateFlow()

    val activeQuestionAnswers: StateFlow<List<ForumAnswerEntity>> = _activeQuestion
        .flatMapLatest { q ->
            if (q != null) repository.getAnswersForQuestion(q.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NEW PROJECTS STATES ---
    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // --- NEW GAMIFICATION STATES ---
    val gamificationProfile: StateFlow<GamificationProfileEntity> = repository.gamificationProfile
        .flatMapLatest { profile ->
            flowOf(profile ?: GamificationProfileEntity(id = 1, points = 0))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GamificationProfileEntity(id = 1))

    val leaderboard: StateFlow<List<LeaderboardEntry>> = gamificationProfile
        .flatMapLatest { profile ->
            val peers = listOf(
                LeaderboardEntry("AdaLovelace", 850),
                LeaderboardEntry("Hyperion", 620),
                LeaderboardEntry("ZettaByte", 480),
                LeaderboardEntry("PixelCurious", 310),
                LeaderboardEntry("CodeNinja", 150)
            )
            val combined = (peers + LeaderboardEntry("You (Learner)", profile.points, isUser = true))
                .sortedByDescending { it.points }
            flowOf(combined)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Loading & Error states
    var isTopicCreating by mutableStateOf(false)
        private set
    var isPlanGenerating by mutableStateOf(false)
        private set
    var isLessonGenerating by mutableStateOf(false)
        private set
    var isChatSending by mutableStateOf(false)
        private set
    var isProjectGenerating by mutableStateOf(false)
        private set
    var isProjectChatSending by mutableStateOf(false)
        private set
    var isDailyVideosGenerating by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Active Quiz Play State
    var quizQuestions by mutableStateOf<List<QuizQuestion>>(emptyList())
        private set
    var currentQuestionIndex by mutableStateOf(0)
        private set
    var selectedOptionIndex by mutableStateOf<Int?>(null)
        private set
    var isAnswerSubmitted by mutableStateOf(false)
        private set
    var correctAnswersCount by mutableStateOf(0)
        private set
    var isQuizCompleted by mutableStateOf(false)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun selectTopic(topic: TopicEntity) {
        _activeTopic.value = topic
    }

    fun selectStudyPlan(plan: StudyPlanEntity) {
        _activeStudyPlan.value = plan
        // Automatically set active topic based on study plan if possible
        viewModelScope.launch {
            val topic = topics.value.find { it.id == plan.topicId }
            if (topic != null) {
                _activeTopic.value = topic
            }
        }
    }

    fun selectModule(module: ModuleEntity) {
        _activeModule.value = module
        // Reset quiz playing state
        resetQuiz()
        if (!module.quizJson.isNullOrEmpty()) {
            quizQuestions = parseQuiz(module.quizJson)
        }
    }

    // --- Actions ---

    // 1. Create Topic / Add new topic to learn
    fun addNewTopic(title: String, onSuccess: () -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            isTopicCreating = true
            clearError()
            val result = repository.createTopic(title)
            isTopicCreating = false
            result.onSuccess { newTopic ->
                selectTopic(newTopic)
                onSuccess()
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to generate topic overview."
            }
        }
    }

    // 2. Generate personalized roadmap
    fun generateRoadmap(targetAudience: String) {
        val topic = _activeTopic.value ?: return
        viewModelScope.launch {
            isPlanGenerating = true
            clearError()
            val result = repository.generateStudyPlan(topic, targetAudience)
            isPlanGenerating = false
            result.onSuccess { newPlan ->
                selectStudyPlan(newPlan)
                setTab(AppTab.STUDY_PLANS)
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to generate your personalized roadmap."
            }
        }
    }

    // Generate study plan directly from a typed topic
    fun generateStudyPlanForTopic(title: String, targetAudience: String = "Beginner", onSuccess: () -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            isPlanGenerating = true
            clearError()
            val topicResult = repository.createTopic(title)
            topicResult.onSuccess { newTopic ->
                selectTopic(newTopic)
                val planResult = repository.generateStudyPlan(newTopic, targetAudience)
                isPlanGenerating = false
                planResult.onSuccess { newPlan ->
                    selectStudyPlan(newPlan)
                    setTab(AppTab.STUDY_PLANS)
                    onSuccess()
                }.onFailure { exception ->
                    errorMessage = exception.message ?: "Failed to generate your personalized study plan."
                }
            }.onFailure { exception ->
                isPlanGenerating = false
                errorMessage = exception.message ?: "Failed to generate topic overview."
            }
        }
    }

    // 3. Load or generate Lesson and Quiz for a Module
    fun loadOrCreateLesson(module: ModuleEntity) {
        if (module.lessonContent != null) {
            selectModule(module)
            setTab(AppTab.CLASSROOM)
            return
        }

        val plan = _activeStudyPlan.value ?: return
        val topic = _activeTopic.value ?: return

        viewModelScope.launch {
            isLessonGenerating = true
            clearError()
            val result = repository.generateInteractiveLesson(topic.title, plan.title, module)
            isLessonGenerating = false
            result.onSuccess { updatedModule ->
                selectModule(updatedModule)
                setTab(AppTab.CLASSROOM)
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to generate interactive lesson content."
            }
        }
    }

    // 4. Send a follow up question in Chat
    fun askQuestion(text: String) {
        val topic = _activeTopic.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            isChatSending = true
            clearError()
            val result = repository.askTopicQuestion(
                topicId = topic.id,
                topicTitle = topic.title,
                question = text,
                chatHistory = activeTopicChat.value
            )
            isChatSending = false
            result.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to connect to personal tutor."
            }
        }
    }

    // 5. Delete a topic
    fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            repository.deleteTopic(topic.id)
            if (_activeTopic.value?.id == topic.id) {
                _activeTopic.value = null
            }
        }
    }

    // 6. Delete study plan
    fun deleteStudyPlan(plan: StudyPlanEntity) {
        viewModelScope.launch {
            repository.deleteStudyPlan(plan.id)
            if (_activeStudyPlan.value?.id == plan.id) {
                _activeStudyPlan.value = null
            }
            if (_activeModule.value?.studyPlanId == plan.id) {
                _activeModule.value = null
            }
        }
    }

    // --- Quiz Engine ---

    private fun resetQuiz() {
        quizQuestions = emptyList()
        currentQuestionIndex = 0
        selectedOptionIndex = null
        isAnswerSubmitted = false
        correctAnswersCount = 0
        isQuizCompleted = false
    }

    fun selectQuizOption(index: Int) {
        if (isAnswerSubmitted) return
        selectedOptionIndex = index
    }

    fun submitQuizAnswer() {
        val currentQuestion = quizQuestions.getOrNull(currentQuestionIndex) ?: return
        val selected = selectedOptionIndex ?: return

        isAnswerSubmitted = true
        if (selected == currentQuestion.correctOptionIndex) {
            correctAnswersCount++
        }
    }

    fun nextQuizQuestion() {
        if (currentQuestionIndex < quizQuestions.size - 1) {
            currentQuestionIndex++
            selectedOptionIndex = null
            isAnswerSubmitted = false
        } else {
            isQuizCompleted = true
            // Save results to database
            val module = _activeModule.value ?: return
            viewModelScope.launch {
                repository.saveQuizScore(module, correctAnswersCount)
                // Update active module reference so progress is reflected
                _activeModule.value = module.copy(quizScore = correctAnswersCount, isCompleted = true)
                // Reward points and update lessons milestones
                repository.incrementCompletedLessons()
            }
        }
    }

    // --- FORUM ACTIONS ---
    fun selectQuestion(question: ForumQuestionEntity?) {
        _activeQuestion.value = question
    }

    fun postQuestion(title: String, content: String, author: String, category: String) {
        viewModelScope.launch {
            repository.createForumQuestion(title, content, author, category)
        }
    }

    fun postAnswer(questionId: Int, content: String, author: String) {
        viewModelScope.launch {
            repository.createForumAnswer(questionId, content, author)
        }
    }

    fun upvoteQuestion(id: Int) {
        viewModelScope.launch {
            repository.upvoteQuestion(id)
            // Refresh current active question if it matches
            if (_activeQuestion.value?.id == id) {
                _activeQuestion.value = _activeQuestion.value?.copy(votes = (_activeQuestion.value?.votes ?: 0) + 1)
            }
        }
    }

    fun upvoteAnswer(id: Int) {
        viewModelScope.launch {
            repository.upvoteAnswer(id)
        }
    }

    // --- PROJECTS ACTIONS ---
    fun selectProject(project: ProjectEntity?) {
        _activeProject.value = project
    }

    fun generateProject(topic: TopicEntity) {
        viewModelScope.launch {
            isProjectGenerating = true
            clearError()
            val result = repository.generateProject(topic)
            isProjectGenerating = false
            result.onSuccess { proj ->
                selectProject(proj)
                setTab(AppTab.PROJECTS)
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to generate project guide."
            }
        }
    }

    fun completeProjectStep(project: ProjectEntity, stepIndex: Int, totalSteps: Int) {
        viewModelScope.launch {
            val isLast = stepIndex >= totalSteps - 1
            val updated = project.copy(
                currentStepIndex = if (isLast) stepIndex else stepIndex + 1,
                isCompleted = isLast || project.isCompleted
            )
            repository.updateProject(updated)
            _activeProject.value = updated
            if (isLast && !project.isCompleted) {
                repository.incrementCompletedProjects()
            } else {
                repository.awardPoints(50)
            }
        }
    }

    fun askProjectQuestion(project: ProjectEntity, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            isProjectChatSending = true
            clearError()
            val result = repository.askProjectQuestion(project, question)
            isProjectChatSending = false
            result.onSuccess { updatedProj ->
                _activeProject.value = updatedProj
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to connect to AI project assistant."
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
            }
        }
    }

    // --- Profile & Custom Gamification Methods ---
    fun updateProfile(gmail: String?, age: Int?) {
        viewModelScope.launch {
            repository.updateProfile(gmail, age)
        }
    }

    fun incrementStreak() {
        viewModelScope.launch {
            repository.incrementStreakDirectly()
        }
    }

    fun claimPrize(prizeId: String) {
        viewModelScope.launch {
            repository.claimPrize(prizeId)
        }
    }

    fun generateDailyVideos(topicTitle: String) {
        val age = gamificationProfile.value.age ?: 18
        viewModelScope.launch {
            isDailyVideosGenerating = true
            clearError()
            val result = repository.generateDailyVideos(topicTitle, age)
            isDailyVideosGenerating = false
            result.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to generate daily video quest."
            }
        }
    }

    private fun parseQuiz(jsonStr: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optArray = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optArray.length()) {
                    opts.add(optArray.getString(j))
                }
                list.add(
                    QuizQuestion(
                        questionText = obj.optString("questionText", ""),
                        options = opts,
                        correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("LearningViewModel", "Error parsing quiz JSON", e)
        }
        return list
    }
}

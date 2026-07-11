package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRequest
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.ThinkingConfig
import com.example.data.api.ThinkingLevel
import com.example.data.api.RetrofitInstance
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LearningRepository(
    private val topicDao: TopicDao,
    private val studyPlanDao: StudyPlanDao,
    private val moduleDao: ModuleDao,
    private val chatMessageDao: ChatMessageDao,
    private val forumDao: ForumDao,
    private val projectDao: ProjectDao,
    private val gamificationDao: GamificationDao
) {
    val allTopics: Flow<List<TopicEntity>> = topicDao.getAllTopics()
    val allStudyPlans: Flow<List<StudyPlanEntity>> = studyPlanDao.getAllStudyPlans()

    fun getStudyPlansByTopic(topicId: Int): Flow<List<StudyPlanEntity>> =
        studyPlanDao.getStudyPlansByTopicId(topicId)

    fun getModulesForPlan(studyPlanId: Int): Flow<List<ModuleEntity>> =
        moduleDao.getModulesByStudyPlanId(studyPlanId)

    fun getChatMessages(topicId: Int): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getChatMessagesByTopicId(topicId)

    // Helper to get the API Key safely
    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            // Return empty so calling code can show a clean warning/error UI
            ""
        } else {
            key
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }

    // 1. Generate overview and save a new Topic
    suspend fun createTopic(title: String): Result<TopicEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured. Please add GEMINI_API_KEY to your Secrets Panel."))
        }

        val prompt = "You are an expert tutor. Provide a concise, highly engaging introduction to the topic '$title'. " +
                "Include: 1) A brief summary, 2) Why it matters, and 3) A quick 'Did you know?' fun fact. " +
                "Keep it within 180 words, styled beautifully with simple markdown bullets where appropriate."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            val overviewText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No overview could be generated. Tap to learn and ask details!"

            val topic = TopicEntity(title = title, description = overviewText)
            val id = topicDao.insertTopic(topic)
            Result.success(topic.copy(id = id.toInt()))
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error creating topic", e)
            Result.failure(e)
        }
    }

    // 2. Generate and save a personalized Study Plan with Modules
    suspend fun generateStudyPlan(topic: TopicEntity, targetAudience: String): Result<StudyPlanEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val ageText = if (profile.age != null) {
            "for a student of age ${profile.age} years old"
        } else {
            "for a student at experience level: $targetAudience"
        }

        val prompt = "You are an expert curriculum designer. Generate a personalized, highly structured learning roadmap " +
                "for the topic: '${topic.title}', specifically tailored $ageText (selected level: $targetAudience). " +
                "Return the roadmap as a single valid JSON object. Do NOT wrap the JSON in markdown code blocks, " +
                "or add any comments or text outside the JSON. The JSON structure MUST be exactly:\n" +
                "{\n" +
                "  \"title\": \"Study Plan Title\",\n" +
                "  \"targetAudience\": \"Audience level\",\n" +
                "  \"duration\": \"Estimated duration (e.g. 4 Weeks)\",\n" +
                "  \"modules\": [\n" +
                "    {\n" +
                "      \"title\": \"Module Title\",\n" +
                "      \"description\": \"Short explanation of what the user will learn in this module\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Generate between 3 to 6 logical, progressive modules. Ensure the JSON is completely valid."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH)
            )
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No study plan generated.")

            val cleanJson = cleanJsonResponse(rawJson)
            val jsonObject = JSONObject(cleanJson)

            val title = jsonObject.optString("title", "Study Roadmap for ${topic.title}")
            val audience = jsonObject.optString("targetAudience", targetAudience)
            val duration = jsonObject.optString("duration", "4 Weeks")
            val modulesArray = jsonObject.getJSONArray("modules")

            val totalModules = modulesArray.length()

            val studyPlan = StudyPlanEntity(
                topicId = topic.id,
                title = title,
                targetAudience = audience,
                duration = duration,
                totalModules = totalModules
            )

            val planId = studyPlanDao.insertStudyPlan(studyPlan).toInt()

            val modules = mutableListOf<ModuleEntity>()
            for (i in 0 until modulesArray.length()) {
                val modObj = modulesArray.getJSONObject(i)
                modules.add(
                    ModuleEntity(
                        studyPlanId = planId,
                        title = modObj.optString("title", "Module ${i + 1}"),
                        description = modObj.optString("description", ""),
                        orderIndex = i
                    )
                )
            }

            moduleDao.insertModules(modules)
            Result.success(studyPlan.copy(id = planId))
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error generating study plan", e)
            Result.failure(e)
        }
    }

    // 3. Generate interactive lesson and quiz content for a Module
    suspend fun generateInteractiveLesson(topicTitle: String, planTitle: String, module: ModuleEntity): Result<ModuleEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        val prompt = "You are an expert interactive tutor. For the module '${module.title}' (part of the curriculum '$planTitle' for topic '$topicTitle'), " +
                "generate an engaging interactive lesson. The lesson should contain:\n" +
                "1. Comprehensive, clear, step-by-step educational content with practical examples (styled nicely with Markdown headings, bold keywords, bullet points, and neat spacing).\n" +
                "2. A mini-quiz of 3 multiple-choice questions to test the user's understanding of the lesson.\n\n" +
                "Return the response as a single valid JSON object. Do NOT wrap the JSON in markdown code blocks or add any comments or text outside the JSON. The JSON structure MUST be exactly:\n" +
                "{\n" +
                "  \"lessonText\": \"The educational content text here... Use markdown formatting for headings and lists.\",\n" +
                "  \"quiz\": [\n" +
                "    {\n" +
                "      \"questionText\": \"Question text?\",\n" +
                "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n" +
                "      \"correctOptionIndex\": 0,\n" +
                "      \"explanation\": \"Explanation for why the correct answer is correct...\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Ensure the JSON is completely valid, with properly escaped quotes and standard JSON compliance."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH)
            )
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No lesson content generated.")

            val cleanJson = cleanJsonResponse(rawJson)
            val jsonObject = JSONObject(cleanJson)

            val lessonText = jsonObject.optString("lessonText", "No lesson text found.")
            val quizArray = jsonObject.optJSONArray("quiz")?.toString() ?: "[]"
            val totalQuizQuestions = jsonObject.optJSONArray("quiz")?.length() ?: 0

            val updatedModule = module.copy(
                lessonContent = lessonText,
                quizJson = quizArray,
                totalQuizQuestions = totalQuizQuestions,
                quizScore = null // Reset previous score
            )

            moduleDao.updateModule(updatedModule)
            Result.success(updatedModule)
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error generating lesson", e)
            Result.failure(e)
        }
    }

    // 4. Save a completed Quiz Score for a Module
    suspend fun saveQuizScore(module: ModuleEntity, score: Int): Unit = withContext(Dispatchers.IO) {
        val updatedModule = module.copy(quizScore = score, isCompleted = true)
        moduleDao.updateModule(updatedModule)

        // Check if all modules in the study plan are completed to update plan completed count
        val plan = studyPlanDao.getStudyPlanById(module.studyPlanId)
        if (plan != null) {
            getModulesForPlan(plan.id).collect { modules ->
                val completedCount = modules.count { it.isCompleted }
                studyPlanDao.updateStudyPlan(plan.copy(completedModules = completedCount))
            }
        }
    }

    // 5. Ask a follow-up question in the topic chat
    suspend fun askTopicQuestion(topicId: Int, topicTitle: String, question: String, chatHistory: List<ChatMessageEntity>): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        // Save User Message
        val userMsg = ChatMessageEntity(topicId = topicId, role = "user", messageText = question)
        chatMessageDao.insertChatMessage(userMsg)

        // Compile context history
        val contentsList = mutableListOf<Content>()
        // Add a friendly tutor context
        val systemInstruction = Content(parts = listOf(Part(text = "You are a friendly, encouraging AI personal tutor for the topic: '$topicTitle'. Provide clear, step-by-step explanations, answer questions, and end with a helpful guiding question to prompt further thinking.")))

        // Add history
        chatHistory.forEach { msg ->
            contentsList.add(Content(role = msg.role, parts = listOf(Part(text = msg.messageText))))
        }
        // Add current query
        contentsList.add(Content(role = "user", parts = listOf(Part(text = question))))

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH)
            )
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            val aiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I couldn't generate a response. Please ask again!"

            val modelMsg = ChatMessageEntity(topicId = topicId, role = "model", messageText = aiResponse)
            chatMessageDao.insertChatMessage(modelMsg)

            Result.success(modelMsg)
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error in chat", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTopic(id: Int) = withContext(Dispatchers.IO) {
        topicDao.deleteTopicById(id)
    }

    suspend fun deleteStudyPlan(id: Int) = withContext(Dispatchers.IO) {
        studyPlanDao.deleteStudyPlanById(id)
    }

    // --- Forum Methods ---
    val allQuestions: Flow<List<ForumQuestionEntity>> = forumDao.getAllQuestions()

    fun getAnswersForQuestion(questionId: Int): Flow<List<ForumAnswerEntity>> =
        forumDao.getAnswersForQuestion(questionId)

    suspend fun createForumQuestion(title: String, content: String, authorName: String, category: String) = withContext(Dispatchers.IO) {
        val question = ForumQuestionEntity(title = title, content = content, authorName = authorName, category = category)
        forumDao.insertQuestion(question)
        awardPoints(15)
    }

    suspend fun createForumAnswer(questionId: Int, content: String, authorName: String) = withContext(Dispatchers.IO) {
        val answer = ForumAnswerEntity(questionId = questionId, content = content, authorName = authorName)
        forumDao.insertAnswer(answer)
        awardPoints(50)
    }

    suspend fun upvoteQuestion(id: Int) = withContext(Dispatchers.IO) {
        forumDao.upvoteQuestion(id)
        awardPoints(5)
    }

    suspend fun upvoteAnswer(id: Int) = withContext(Dispatchers.IO) {
        forumDao.upvoteAnswer(id)
        awardPoints(5)
    }

    // --- Project Methods ---
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectForTopic(topicId: Int): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectByTopicId(topicId)
    }

    suspend fun deleteProject(id: Int) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project)
    }

    suspend fun generateProject(topic: TopicEntity): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        val existing = projectDao.getProjectByTopicId(topic.id)
        if (existing != null) {
            return@withContext Result.success(existing)
        }

        val prompt = "You are an expert project advisor. For the topic '${topic.title}', generate a highly engaging, " +
                "educational, real-world practical project that applies these concepts. " +
                "Return the project details as a single valid JSON object. Do NOT wrap in markdown code blocks or add any other text outside the JSON. " +
                "The JSON structure MUST be exactly:\n" +
                "{\n" +
                "  \"title\": \"Project Name\",\n" +
                "  \"description\": \"Envisioned project description explaining real-world utility and what they will build.\",\n" +
                "  \"steps\": [\n" +
                "    {\n" +
                "      \"title\": \"Step Title\",\n" +
                "      \"explanation\": \"Detailed guidance, links, tips, or simple code snips explaining exactly what to do here.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Provide between 3 to 5 clear, structured steps. Ensure the JSON is completely valid."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH)
            )
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No project generated.")

            val cleanJson = cleanJsonResponse(rawJson)
            val jsonObject = JSONObject(cleanJson)

            val title = jsonObject.optString("title", "Practical Project for ${topic.title}")
            val description = jsonObject.optString("description", "A practical project to apply what you have learned.")
            val stepsArray = jsonObject.getJSONArray("steps")

            val stepsJson = stepsArray.toString()

            val project = ProjectEntity(
                topicId = topic.id,
                topicTitle = topic.title,
                title = title,
                description = description,
                stepsJson = stepsJson
            )

            val projectId = projectDao.insertProject(project).toInt()
            Result.success(project.copy(id = projectId))
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error generating project", e)
            Result.failure(e)
        }
    }

    suspend fun askProjectQuestion(project: ProjectEntity, question: String): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        val chatArray = try {
            JSONArray(project.assistantChatJson)
        } catch (e: Exception) {
            JSONArray()
        }

        val userObj = JSONObject().apply {
            put("role", "user")
            put("message", question)
            put("timestamp", System.currentTimeMillis())
        }
        chatArray.put(userObj)

        val contentsList = mutableListOf<Content>()
        val systemInstruction = Content(parts = listOf(Part(text = "You are a professional project advisor helping a student with their real-world project: '${project.title}' under the topic '${project.topicTitle}'. Offer clear code reviews, design patterns, debugging tips, and step-by-step solutions.")))

        for (i in 0 until chatArray.length()) {
            val item = chatArray.getJSONObject(i)
            contentsList.add(Content(role = item.getString("role"), parts = listOf(Part(text = item.getString("message")))))
        }

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH)
            )
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            val aiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I am here to support you! Let me know which step you are working on."

            val modelObj = JSONObject().apply {
                put("role", "model")
                put("message", aiResponse)
                put("timestamp", System.currentTimeMillis())
            }
            chatArray.put(modelObj)

            val updatedProject = project.copy(assistantChatJson = chatArray.toString())
            projectDao.updateProject(updatedProject)

            Result.success(updatedProject)
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error in project assistant chat", e)
            Result.failure(e)
        }
    }

    // --- Gamification Methods ---
    val gamificationProfile: Flow<GamificationProfileEntity?> = gamificationDao.getProfile()

    suspend fun awardPoints(amount: Int) = withContext(Dispatchers.IO) {
        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val newPoints = profile.points + amount
        
        val currentBadges = try {
            JSONArray(profile.badgesJson).let { arr ->
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                list
            }
        } catch (e: Exception) {
            mutableListOf<String>()
        }

        val newBadges = mutableListOf<String>().apply { addAll(currentBadges) }

        if (newPoints >= 100 && !newBadges.contains("First Step")) {
            newBadges.add("First Step")
        }
        if (newPoints >= 500 && !newBadges.contains("Elite Scholar")) {
            newBadges.add("Elite Scholar")
        }
        if (newPoints >= 1000 && !newBadges.contains("Grandmaster")) {
            newBadges.add("Grandmaster")
        }

        val badgesJsonString = JSONArray(newBadges).toString()
        val updatedProfile = profile.copy(
            points = newPoints,
            badgesJson = badgesJsonString
        )
        gamificationDao.insertProfile(updatedProfile)
    }

    suspend fun incrementCompletedLessons() = withContext(Dispatchers.IO) {
        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val newCount = profile.completedLessonsCount + 1
        
        val currentBadges = try {
            JSONArray(profile.badgesJson).let { arr ->
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                list
            }
        } catch (e: Exception) {
            mutableListOf<String>()
        }
        val newBadges = mutableListOf<String>().apply { addAll(currentBadges) }
        if (newCount >= 1 && !newBadges.contains("Curriculum Starter")) {
            newBadges.add("Curriculum Starter")
        }
        if (newCount >= 3 && !newBadges.contains("Roadmap Explorer")) {
            newBadges.add("Roadmap Explorer")
        }
        
        val updated = profile.copy(
            completedLessonsCount = newCount,
            badgesJson = JSONArray(newBadges).toString()
        )
        gamificationDao.insertProfile(updated)
        awardPoints(100)
    }

    suspend fun incrementCompletedProjects() = withContext(Dispatchers.IO) {
        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val newCount = profile.completedProjectsCount + 1
        
        val currentBadges = try {
            JSONArray(profile.badgesJson).let { arr ->
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                list
            }
        } catch (e: Exception) {
            mutableListOf<String>()
        }
        val newBadges = mutableListOf<String>().apply { addAll(currentBadges) }
        if (newCount >= 1 && !newBadges.contains("Builder")) {
            newBadges.add("Builder")
        }
        if (newCount >= 3 && !newBadges.contains("Grand Creator")) {
            newBadges.add("Grand Creator")
        }
        
        val updated = profile.copy(
            completedProjectsCount = newCount,
            badgesJson = JSONArray(newBadges).toString()
        )
        gamificationDao.insertProfile(updated)
        awardPoints(500)
    }

    suspend fun updateProfile(gmail: String?, age: Int?) = withContext(Dispatchers.IO) {
        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val updated = profile.copy(
            gmail = gmail,
            age = age
        )
        gamificationDao.insertProfile(updated)
    }

    suspend fun incrementStreakDirectly() = withContext(Dispatchers.IO) {
        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val updated = profile.copy(
            streakCount = profile.streakCount + 1,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        gamificationDao.insertProfile(updated)
    }

    suspend fun claimPrize(prizeId: String) = withContext(Dispatchers.IO) {
        val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
        val prizes = try {
            JSONArray(profile.claimedPrizesJson).let { arr ->
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                list
            }
        } catch (e: Exception) {
            mutableListOf<String>()
        }
        
        if (!prizes.contains(prizeId)) {
            prizes.add(prizeId)
            val updated = profile.copy(
                claimedPrizesJson = JSONArray(prizes).toString(),
                points = profile.points + 250 // award points for claiming!
            )
            gamificationDao.insertProfile(updated)
        }
    }

    suspend fun generateDailyVideos(topicTitle: String, age: Int): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        val prompt = "You are an expert tutor. Generate a customized daily video learning quest for a student aged $age. " +
                "The topic is '$topicTitle'. Provide a group of 3 educational YouTube videos about this subject (with titles, " +
                "search links to search on YouTube, and duration), and " +
                "then generate 3 quiz questions based on those videos. " +
                "Return as a single valid JSON object. Do NOT wrap in markdown blocks. " +
                "The JSON structure MUST be exactly:\n" +
                "{\n" +
                "  \"subject\": \"$topicTitle\",\n" +
                "  \"videos\": [\n" +
                "    {\n" +
                "      \"title\": \"Video Title (e.g. Master React in 10 mins)\",\n" +
                "      \"url\": \"https://www.youtube.com/results?search_query=React+crash+course\",\n" +
                "      \"duration\": \"12 mins\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"questionText\": \"Question text?\",\n" +
                "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n" +
                "      \"correctOptionIndex\": 0,\n" +
                "      \"explanation\": \"Why this is correct\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH)
            )
        )

        try {
            val response = RetrofitInstance.apiService.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No daily videos generated.")

            val cleanJson = cleanJsonResponse(rawJson)
            
            // Save to profile
            val profile = gamificationDao.getProfileSync() ?: GamificationProfileEntity(id = 1)
            val updated = profile.copy(dailyVideosJson = cleanJson)
            gamificationDao.insertProfile(updated)

            Result.success(cleanJson)
        } catch (e: Exception) {
            Log.e("LearningRepository", "Error generating daily videos", e)
            Result.failure(e)
        }
    }
}

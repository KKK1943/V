package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.GamificationProfileEntity
import com.example.ui.LearningViewModel
import org.json.JSONArray
import org.json.JSONObject

data class BadgeTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

const val DEFAULT_DAILY_VIDEOS_JSON = """
{
  "subject": "Introduction to Programming & Artificial Intelligence",
  "videos": [
    {
      "title": "Harvard CS50 - Introduction to Computer Science (Official)",
      "url": "https://www.youtube.com/results?search_query=Harvard+CS50+Introduction+to+Computer+Science",
      "duration": "18 mins excerpt"
    },
    {
      "title": "What is Artificial Intelligence? (AI for Complete Beginners)",
      "url": "https://www.youtube.com/results?search_query=What+is+Artificial+Intelligence+AI+for+Beginners",
      "duration": "12 mins"
    },
    {
      "title": "Learn Kotlin & Jetpack Compose in 10 Minutes",
      "url": "https://www.youtube.com/results?search_query=Learn+Kotlin+Jetpack+Compose+in+10+Minutes",
      "duration": "10 mins"
    }
  ],
  "questions": [
    {
      "questionText": "What is the core focus of Harvard's introductory computer science courses?",
      "options": ["To build video game graphics only", "To understand computational thinking and problem-solving", "To master proprietary enterprise databases", "To repair old hardware parts"],
      "correctOptionIndex": 1,
      "explanation": "Computer Science is primarily about computational thinking and systematic problem-solving."
    },
    {
      "questionText": "Which framework is used for modern native Android UI development?",
      "options": ["XML layouts only", "HTML and CSS", "Jetpack Compose", "Assembly Swing"],
      "correctOptionIndex": 2,
      "explanation": "Jetpack Compose is Google's modern declarative toolkit for building native Android UI."
    },
    {
      "questionText": "How does Artificial Intelligence (AI) model human cognitive capabilities?",
      "options": ["By physically copying brain cells", "By writing deterministic IF/ELSE statements for everything", "By processing large datasets to identify patterns and make predictions", "By hard-coding instructions into physical chips"],
      "correctOptionIndex": 2,
      "explanation": "Modern AI relies on data-driven patterns, machine learning models, and training rather than manual rules."
    }
  ]
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.gamificationProfile.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val activeStudyPlan by viewModel.activeStudyPlan.collectAsStateWithLifecycle()

    // Setup input fields state
    var editMode by remember { mutableStateOf(false) }
    var gmailInput by remember(profile.gmail) { mutableStateOf(profile.gmail ?: "") }
    var ageInput by remember(profile.age) { mutableStateOf(profile.age?.toString() ?: "") }

    // Celebration/Prize Modal state
    var prizeNotification by remember { mutableStateOf<String?>(null) }

    // Parsing Daily Video Quest JSON
    val dailyQuestObj = remember(profile.dailyVideosJson) {
        try {
            if (profile.dailyVideosJson.isNotEmpty() && profile.dailyVideosJson != "[]") {
                JSONObject(profile.dailyVideosJson)
            } else {
                JSONObject(DEFAULT_DAILY_VIDEOS_JSON)
            }
        } catch (e: Exception) {
            try {
                JSONObject(DEFAULT_DAILY_VIDEOS_JSON)
            } catch (ex: Exception) {
                null
            }
        }
    }

    val dailySubject = dailyQuestObj?.optString("subject", "General AI and Coding") ?: "General AI and Coding"
    
    val dailyVideosList = remember(dailyQuestObj) {
        val list = mutableListOf<Triple<String, String, String>>() // title, url, duration
        try {
            val arr = dailyQuestObj?.getJSONArray("videos")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Triple(
                            o.optString("title", "Video ${i + 1}"),
                            o.optString("url", "https://youtube.com"),
                            o.optString("duration", "10m")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // empty
        }
        list
    }

    // Solve state for daily quiz questions
    val dailyQuestions = remember(dailyQuestObj) {
        val list = mutableListOf<JSONObject>()
        try {
            val arr = dailyQuestObj?.getJSONArray("questions")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    list.add(arr.getJSONObject(i))
                }
            }
        } catch (e: Exception) {
            // empty
        }
        list
    }

    var selectedDailyAnswers = remember(dailyQuestObj) { mutableStateListOf<Int?>().apply { 
        repeat(3) { add(null) } 
    } }
    var dailyQuizSubmitted by remember(dailyQuestObj) { mutableStateOf(false) }
    var dailyQuizResultScore by remember(dailyQuestObj) { mutableStateOf(0) }
    var dailyLessonRating by remember(dailyQuestObj) { mutableStateOf(0) }
    var isLessonEvaluated by remember(dailyQuestObj) { mutableStateOf(false) }

    // Badges
    val earnedBadgesList = remember(profile.badgesJson) {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(profile.badgesJson)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // empty
        }
        list
    }

    val claimedPrizesList = remember(profile.claimedPrizesJson) {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(profile.claimedPrizesJson)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // empty
        }
        list
    }

    val badgesCatalog = listOf(
        BadgeTemplate(
            id = "First Step",
            title = "First Step",
            description = "Unlocked first 100 XP points.",
            icon = Icons.Default.DirectionsRun,
            color = Color(0xFF4CAF50)
        ),
        BadgeTemplate(
            id = "Curriculum Starter",
            title = "Lesson Starter",
            description = "Completed your first interactive lesson.",
            icon = Icons.Default.PlayArrow,
            color = Color(0xFF2196F3)
        ),
        BadgeTemplate(
            id = "Roadmap Explorer",
            title = "Roadmap Explorer",
            description = "Completed 3 interactive lessons.",
            icon = Icons.Default.Map,
            color = Color(0xFFFF9800)
        ),
        BadgeTemplate(
            id = "Builder",
            title = "Code Architect",
            description = "Completed your first real-world project.",
            icon = Icons.Default.Construction,
            color = Color(0xFF9C27B0)
        )
    )

    // Metrics
    val totalPoints = profile.points
    val level = 1 + (totalPoints / 500)
    val pointsInCurrentLevel = totalPoints % 500
    val progress = pointsInCurrentLevel.toFloat() / 500f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Google Account Connection and Onboarding Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile Setup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personal Learner Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (profile.gmail != null && !editMode) {
                            TextButton(onClick = { editMode = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (profile.gmail == null || editMode) {
                        Text(
                            text = "Connect your Gmail account and specify your age. Lumina's generative AI uses your age to structure the perfect difficulty and style of curriculum lessons!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = gmailInput,
                            onValueChange = { gmailInput = it },
                            label = { Text("Gmail Address") },
                            placeholder = { Text("example@gmail.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Your Age") },
                            placeholder = { Text("e.g. 18") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (gmailInput.isNotBlank() && ageInput.isNotBlank()) {
                                    val ageInt = ageInput.toIntOrNull()
                                    if (ageInt != null && ageInt > 0) {
                                        viewModel.updateProfile(gmailInput, ageInt)
                                        editMode = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Google Account & Set Age")
                        }
                    } else {
                        // Profile display mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Connected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Connected Google Account",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = profile.gmail ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Curriculum Age target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${profile.age} Years Old", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Account Sync Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Active Cloud Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Daily Streak Indicator with MANDATORY Text Requirement
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFFFE0B2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Daily learning streak: ${profile.streakCount} days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // REQUIRED PHRASE
                        Text(
                            text = "You have used the application on a daily basis to keep this streak alive!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Hero Card - XP Progress
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LEARNER STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Level $level Scholar",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFF176),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$totalPoints XP Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${500 - pointsInCurrentLevel} XP to Level ${level + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .testTag("xp_progress_bar"),
                        color = Color(0xFFFFF176),
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // 3. AI DAILY VIDEO QUESTS (AI gives every single day a group of videos to watch and then questions to solve, user evaluates)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFFFF0000),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's AI Video Quest",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Daily Quest",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your daily AI lesson has arrived! Watch these curated videos on '$dailySubject' then solve the quick comprehension quiz below to unlock bonus learning prizes and boost your daily streak!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (viewModel.isDailyVideosGenerating) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Lumina AI is constructing your custom video lesson and quiz...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        // Daily videos are generated!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎥 Watch Today's Recommended Videos:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(
                                onClick = {
                                    val subjectTitle = activeStudyPlan?.title ?: "Web Development Fundamentals"
                                    viewModel.generateDailyVideos(subjectTitle)
                                }
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Regenerate", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        dailyVideosList.forEachIndexed { index, video ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(video.second)
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFFFF0000),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.first, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("Duration: ${video.third}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Daily quiz questions
                        Text(
                            text = "✏️ Comprehension Video Quiz:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!dailyQuizSubmitted) {
                            dailyQuestions.forEachIndexed { qIndex, question ->
                                val questText = question.optString("questionText", "Question?")
                                val optsArray = question.optJSONArray("options")
                                val optionsList = mutableListOf<String>()
                                if (optsArray != null) {
                                    for (j in 0 until optsArray.length()) {
                                        optionsList.add(optsArray.getString(j))
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "${qIndex + 1}. $questText", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        optionsList.forEachIndexed { oIndex, opt ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedDailyAnswers[qIndex] = oIndex }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selectedDailyAnswers[qIndex] == oIndex,
                                                    onClick = { selectedDailyAnswers[qIndex] = oIndex }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = opt, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    // Evaluate score
                                    var score = 0
                                    dailyQuestions.forEachIndexed { qIndex, question ->
                                        val correctIdx = question.optInt("correctOptionIndex", 0)
                                        if (selectedDailyAnswers[qIndex] == correctIdx) {
                                            score++
                                        }
                                    }
                                    dailyQuizResultScore = score
                                    dailyQuizSubmitted = true
                                    viewModel.updateProfile(profile.gmail, profile.age) // trigger re-cache
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Submit Video Quiz Answers")
                            }
                        } else {
                            // Quiz completed, let's let them evaluate the lesson!
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Quiz Completed!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("You answered $dailyQuizResultScore/3 questions correctly!", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    if (!isLessonEvaluated) {
                                        Text(
                                            text = "Evaluate Today's AI Lesson:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            for (star in 1..5) {
                                                Icon(
                                                    imageVector = if (star <= dailyLessonRating) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "Star $star",
                                                    tint = Color(0xFFFFD54F),
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clickable { dailyLessonRating = star }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Button(
                                            onClick = {
                                                if (dailyLessonRating > 0) {
                                                    isLessonEvaluated = true
                                                    viewModel.incrementStreak()
                                                    viewModel.updateProfile(profile.gmail, profile.age)
                                                    prizeNotification = "Lesson Evaluated successfully! Daily streak incremented to ${profile.streakCount + 1}! Claim rewards now!"
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = dailyLessonRating > 0
                                        ) {
                                            Text("Submit Lesson Evaluation")
                                        }
                                    } else {
                                        Text("Evaluation submitted: $dailyLessonRating/5 Stars!", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                        Text("Streak Updated! Claim your learning prize below!", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. FACILITATIVE PRIZES (excites user daily)
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lumina Learning Prizes Store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            val prizes = listOf(
                Triple("Streak Master Shield", "Streak of 13+ days to claim.", "shield"),
                Triple("Double XP Elixir", "Complete and evaluate today's daily video quest.", "elixir"),
                Triple("Knowledge Crown", "Unlock 1000+ total XP points to claim.", "crown")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                prizes.forEach { prize ->
                    val isClaimed = claimedPrizesList.contains(prize.third)
                    val isEligible = when(prize.third) {
                        "shield" -> profile.streakCount >= 13
                        "elixir" -> isLessonEvaluated
                        "crown" -> totalPoints >= 1000
                        else -> false
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isClaimed) {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.5f)
                            } else if (isEligible) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isClaimed) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            else if (isEligible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (isClaimed) Color.LightGray.copy(alpha = 0.2f)
                                        else if (isEligible) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(prize.third) {
                                        "shield" -> Icons.Default.Shield
                                        "elixir" -> Icons.Default.OfflineBolt
                                        else -> Icons.Default.WorkspacePremium
                                    },
                                    contentDescription = null,
                                    tint = if (isClaimed) Color.Gray
                                           else if (isEligible) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prize.first, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(prize.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (isClaimed) {
                                Text(
                                    text = "Claimed",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.claimPrize(prize.third)
                                        prizeNotification = "Congratulations! You claimed the '${prize.first}' and earned +250 Bonus XP!"
                                    },
                                    enabled = isEligible,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Claim", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Leaderboard title
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Top Learners",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Leaderboard List
        itemsIndexed(leaderboard) { index, entry ->
            val rank = index + 1
            val isUser = entry.isUser

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("leaderboard_item_$rank"),
                shape = RoundedCornerShape(12.dp),
                border = if (isUser) {
                    androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary
                    )
                } else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (rank) {
                                    1 -> Color(0xFFFFF176)
                                    2 -> Color(0xFFCFD8DC)
                                    3 -> Color(0xFFFFCC80)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$rank",
                            fontWeight = FontWeight.Bold,
                            color = if (rank in 1..3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = if (isUser) "You (Learner)" else entry.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium,
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${entry.points} XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Badges Grid section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lumina Achievement Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Display catalog of badges
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                badgesCatalog.forEach { badge ->
                    val isEarned = earnedBadgesList.contains(badge.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("badge_card_${badge.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEarned) {
                                badge.color.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.5f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEarned) badge.color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEarned) badge.color.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = badge.icon,
                                    contentDescription = badge.title,
                                    tint = if (isEarned) badge.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEarned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEarned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }

                            if (isEarned) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unlocked",
                                    tint = badge.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Celebratory Reward Claim Modal
    if (prizeNotification != null) {
        AlertDialog(
            onDismissRequest = { prizeNotification = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lumina Milestone Reward!")
                }
            },
            text = {
                Text(prizeNotification!!, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(onClick = { prizeNotification = null }) {
                    Text("Hooray!")
                }
            }
        )
    }
}

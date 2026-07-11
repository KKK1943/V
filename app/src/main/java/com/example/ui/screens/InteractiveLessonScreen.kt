package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.LearningViewModel
import com.example.ui.QuizQuestion

@Composable
fun InteractiveLessonScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val activeModule by viewModel.activeModule.collectAsStateWithLifecycle()
    val activeStudyPlan by viewModel.activeStudyPlan.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (activeModule == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "No Active Lesson",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Interactive Classroom",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select any module inside your 'Study Plans' curriculum and tap 'Start' to begin your AI-powered interactive lesson and test your knowledge here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            val module = activeModule!!

            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = activeStudyPlan?.title ?: "AI Personal Curriculum",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Module ${module.orderIndex + 1} • Interactive Lesson & Quiz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Overall Study Plan Progress Bar Component
                    if (activeStudyPlan != null && activeStudyPlan!!.totalModules > 0) {
                        val plan = activeStudyPlan!!
                        val overallPercentage = plan.completedModules.toFloat() / plan.totalModules.toFloat()
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Plan Progress",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Overall Roadmap Progress",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${(overallPercentage * 100).toInt()}% Done (${plan.completedModules}/${plan.totalModules})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = overallPercentage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // If the quiz has not been finished or started, render the lesson text first
            if (viewModel.quizQuestions.isEmpty() || viewModel.isQuizCompleted) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = "Lesson Text",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lesson Text",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Basic Markdown Renderer
                            val paragraphs = module.lessonContent?.split("\n\n") ?: emptyList()
                            paragraphs.forEach { paragraph ->
                                val cleanText = paragraph.trim()
                                if (cleanText.startsWith("###")) {
                                    Text(
                                        text = cleanText.removePrefix("###").trim(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                } else if (cleanText.startsWith("##")) {
                                    Text(
                                        text = cleanText.removePrefix("##").trim(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                    )
                                } else if (cleanText.startsWith("#")) {
                                    Text(
                                        text = cleanText.removePrefix("#").trim(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                                    )
                                } else if (cleanText.startsWith("*") || cleanText.startsWith("-")) {
                                    val bulletPoint = cleanText.substring(1).trim()
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("• ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = bulletPoint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                    }
                                } else {
                                    // Normal body paragraph
                                    Text(
                                        text = cleanText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Videos",
                                    tint = Color(0xFFFF0000)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Subject YouTube Resources",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enhance your understanding of '${module.title}' with these structured YouTube resources:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val encodedTitle = java.net.URLEncoder.encode(module.title, "UTF-8")
                            val videosList = listOf(
                                Pair("Introduction to " + module.title, "https://www.youtube.com/results?search_query=Introduction+to+" + encodedTitle),
                                Pair(module.title + " Tutorial for Beginners", "https://www.youtube.com/results?search_query=" + encodedTitle + "+tutorial+beginners"),
                                Pair(module.title + " Crash Course", "https://www.youtube.com/results?search_query=" + encodedTitle + "+crash+course")
                            )

                            videosList.forEach { video ->
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
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color(0xFFFF0000)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = video.first,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Search YouTube • FREE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Open link",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quiz Summary if Completed, else Start Quiz Button
                item {
                    if (viewModel.isQuizCompleted) {
                        QuizSummaryCard(
                            score = viewModel.correctAnswersCount,
                            totalQuestions = viewModel.quizQuestions.size,
                            onClose = {
                                viewModel.setTab(AppTab.STUDY_PLANS)
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (!module.quizJson.isNullOrEmpty()) {
                                    viewModel.loadOrCreateLesson(module) // This triggers quiz loading if present
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("start_quiz_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = "Take Quiz")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Take Module Quiz")
                        }
                    }
                }
            } else {
                // Render the interactive live Quiz screen!
                item {
                    QuizRunnerCard(
                        viewModel = viewModel,
                        currentQuestionIndex = viewModel.currentQuestionIndex,
                        questions = viewModel.quizQuestions,
                        selectedOptionIndex = viewModel.selectedOptionIndex,
                        isAnswerSubmitted = viewModel.isAnswerSubmitted
                    )
                }
            }
        }
    }
}

@Composable
fun QuizRunnerCard(
    viewModel: LearningViewModel,
    currentQuestionIndex: Int,
    questions: List<QuizQuestion>,
    selectedOptionIndex: Int?,
    isAnswerSubmitted: Boolean,
    modifier: Modifier = Modifier
) {
    val currentQuestion = questions.getOrNull(currentQuestionIndex) ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Checkpoint",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = (currentQuestionIndex + 1).toFloat() / questions.size.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Question Text
            Text(
                text = currentQuestion.questionText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedOptionIndex == index
                    val optionBorderColor = when {
                        isAnswerSubmitted && index == currentQuestion.correctOptionIndex -> Color(0xFF4CAF50) // Correct option green
                        isAnswerSubmitted && isSelected -> Color(0xFFF44336) // Incorrect option red
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    }
                    val optionBgColor = when {
                        isAnswerSubmitted && index == currentQuestion.correctOptionIndex -> Color(0xFFE8F5E9) // Correct green Tint
                        isAnswerSubmitted && isSelected -> Color(0xFFFFEBEE) // Incorrect red Tint
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(optionBgColor)
                            .border(1.5.dp, optionBorderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isAnswerSubmitted) {
                                viewModel.selectQuizOption(index)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (index) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // Status Icon
                        if (isAnswerSubmitted) {
                            if (index == currentQuestion.correctOptionIndex) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = Color(0xFF4CAF50)
                                )
                            } else if (isSelected) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "Incorrect",
                                    tint = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submitted Explanation Box
            if (isAnswerSubmitted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Explanation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tutor's Explanation",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = {
                    if (isAnswerSubmitted) {
                        viewModel.nextQuizQuestion()
                    } else {
                        viewModel.submitQuizAnswer()
                    }
                },
                enabled = selectedOptionIndex != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_action_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                val buttonText = if (isAnswerSubmitted) {
                    if (currentQuestionIndex == questions.size - 1) "Finish Quiz" else "Next Question"
                } else {
                    "Submit Answer"
                }
                Text(buttonText)
            }
        }
    }
}

@Composable
fun QuizSummaryCard(
    score: Int,
    totalQuestions: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Celebration,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Congratulations!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You've successfully completed this interactive checkpoints check!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Score",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "$score out of $totalQuestions",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            val percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions.toFloat() * 100).toInt() else 0
            Text(
                text = "You got $percentage% of answers correct!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (percentage >= 70) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Return to Study Plans")
            }
        }
    }
}

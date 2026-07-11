package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChatMessageEntity
import com.example.data.database.TopicEntity
import com.example.ui.AppTab
import com.example.ui.LearningViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val activeTopic by viewModel.activeTopic.collectAsStateWithLifecycle()
    val chatMessages by viewModel.activeTopicChat.collectAsStateWithLifecycle()

    var newTopicTitle by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("Beginner") }
    var userQuestion by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val chatListState = rememberLazyListState()

    // Auto scroll chat to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Learn Anything.",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter any topic, question, or skill. Get interactive customized plans and instant AI tutoring.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    )
                }
            }
        }

        // Search Input Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What would you like to master today?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newTopicTitle,
                        onValueChange = { newTopicTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("topic_input"),
                        placeholder = { Text("e.g. Sourdough baking, Quantum theory, French basics...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newTopicTitle.isNotBlank()) {
                                viewModel.addNewTopic(newTopicTitle.trim()) {
                                    newTopicTitle = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_topic_button"),
                        enabled = !viewModel.isTopicCreating && newTopicTitle.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (viewModel.isTopicCreating) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exploring topic...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Explore")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Topic Overview")
                        }
                    }
                }
            }
        }

        // Topics Horizontal List/Chips if active topic is not selected or for quick switching
        if (topics.isNotEmpty()) {
            item {
                Text(
                    text = "My Learning Topics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topics.take(4).forEach { topic ->
                        val isSelected = activeTopic?.id == topic.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTopic(topic) },
                            label = { Text(topic.title) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = "Topic",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Active Topic Details
        if (activeTopic != null) {
            val topic = activeTopic!!

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
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { viewModel.deleteTopic(topic) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Topic",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = topic.description,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        // Study Plan Generator Block
                        Text(
                            text = "Get Personalized Study Roadmap",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate a structured learning path with custom interactive modules tailored to your level.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Difficulty Level Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                                val isSelected = selectedDifficulty == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .clickable { selectedDifficulty = level }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = level,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.generateRoadmap(selectedDifficulty) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_roadmap_button"),
                            enabled = !viewModel.isPlanGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            if (viewModel.isPlanGenerating) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Designing Study Plan...")
                            } else {
                                Icon(Icons.Default.Map, contentDescription = "Roadmap")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Study Plan")
                            }
                        }
                    }
                }
            }

            // AI Personal Tutor Chat
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = "Tutor Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ask your AI Tutor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chat messages viewport (Elegant Dark styled viewport)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.background) // `#1A1C1E`
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)) // `#44474E`
                                .padding(12.dp)
                        ) {
                            if (chatMessages.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.QuestionAnswer,
                                        contentDescription = "No Chat",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Ask questions to clarify difficult concepts, ask for summaries, or get code examples!",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = chatListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(chatMessages) { message ->
                                        val isUser = message.role == "user"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            if (!isUser) {
                                                // Tutor Icon Avatar
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.SmartToy,
                                                        contentDescription = "Tutor",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Column(
                                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(
                                                            RoundedCornerShape(
                                                                topStart = 16.dp,
                                                                topEnd = 16.dp,
                                                                bottomStart = if (isUser) 16.dp else 0.dp,
                                                                bottomEnd = if (isUser) 0.dp else 16.dp
                                                            )
                                                        )
                                                        .background(
                                                            if (isUser) MaterialTheme.colorScheme.primaryContainer // `#4F378B`
                                                            else MaterialTheme.colorScheme.surface // `#2D2F31`
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outline,
                                                            shape = RoundedCornerShape(
                                                                topStart = 16.dp,
                                                                topEnd = 16.dp,
                                                                bottomStart = if (isUser) 16.dp else 0.dp,
                                                                bottomEnd = if (isUser) 0.dp else 16.dp
                                                            )
                                                        )
                                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                                        .widthIn(max = 260.dp)
                                                ) {
                                                    Text(
                                                        text = message.messageText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer // `#EADDFF`
                                                        else MaterialTheme.colorScheme.onSurface // `#E2E2E6`
                                                    )
                                                }
                                                Text(
                                                    text = if (isUser) "You" else "Tutor",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input message row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = userQuestion,
                                onValueChange = { userQuestion = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ask_tutor_input"),
                                placeholder = { Text("Ask a follow-up question...") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (userQuestion.isNotBlank() && !viewModel.isChatSending) {
                                        viewModel.askQuestion(userQuestion.trim())
                                        userQuestion = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (userQuestion.isNotBlank()) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("send_question_button"),
                                enabled = userQuestion.isNotBlank() && !viewModel.isChatSending
                            ) {
                                if (viewModel.isChatSending) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Send",
                                        tint = if (userQuestion.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (topics.isEmpty() && !viewModel.isTopicCreating) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Graduation cap",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your school is empty!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a topic above to begin generating your personalized curriculum and learning overviews.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}

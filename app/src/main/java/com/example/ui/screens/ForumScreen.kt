package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ForumQuestionEntity
import com.example.data.database.ForumAnswerEntity
import com.example.data.database.TopicEntity
import com.example.ui.LearningViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val questions by viewModel.forumQuestions.collectAsStateWithLifecycle()
    val activeQuestion by viewModel.activeQuestion.collectAsStateWithLifecycle()
    val answers by viewModel.activeQuestionAnswers.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showPostDialog by remember { mutableStateOf(false) }

    // Dialog inputs
    var newQuestionTitle by remember { mutableStateOf("") }
    var newQuestionCategory by remember { mutableStateOf("Quantum Physics") }
    var newQuestionContent by remember { mutableStateOf("") }
    var newQuestionAuthor by remember { mutableStateOf("GuestLearner") }

    // Answer input
    var newAnswerContent by remember { mutableStateOf("") }
    var newAnswerAuthor by remember { mutableStateOf("GuestSage") }

    val categories = listOf("All", "Quantum Physics", "Machine Learning", "Software Engineering", "Mathematics")

    // Pre-seed some standard Q&As if empty, to make the app feel alive and let users upvote instantly!
    LaunchedEffect(questions.size) {
        if (questions.isEmpty()) {
            viewModel.postQuestion(
                title = "How do I mathematically prove superposition?",
                content = "I'm learning quantum superposition but struggling with why qubits can represent |0> and |1> simultaneously. Is it just a linear combination of state vectors? How does measurement collapse it?",
                author = "QuantumNoob",
                category = "Quantum Physics"
            )
            viewModel.postQuestion(
                title = "Is Python really the best language for Machine Learning?",
                content = "Everyone recommends Python but I come from a Rust/C++ background. Why is Python dominant? Is it just because of library support like TensorFlow and PyTorch, or is there an execution speed advantage?",
                author = "RustDev",
                category = "Machine Learning"
            )
            viewModel.postQuestion(
                title = "Why does my Gradient Descent keep diverging?",
                content = "I am writing gradient descent from scratch in Kotlin. Whenever I run it, my cost function value grows exponentially and overflows. What am I doing wrong?",
                author = "MathGeek",
                category = "Mathematics"
            )
        }
    }

    // Auto-seed answers for pre-seeded questions when selected
    LaunchedEffect(activeQuestion) {
        if (activeQuestion != null && answers.isEmpty()) {
            val q = activeQuestion!!
            if (q.title.contains("superposition")) {
                viewModel.postAnswer(q.id, "Yes! It is represented as c0|0> + c1|1> where c0 and c1 are complex probability amplitudes. The sum of their absolute squares must be 1. Measurement collapses it because of Born's rule, forcing the state vector into one of the basis states.", "Dr_Dirac")
                viewModel.postAnswer(q.id, "Check out the Bloch Sphere representation. It helps visualize these state vectors as coordinates on a 3D sphere. Superposition is just a vector pointing anywhere on the surface!", "QuantumEinstein")
            } else if (q.title.contains("Python")) {
                viewModel.postAnswer(q.id, "Python is not chosen for speed, but for its syntax elegance and robust ecosystem. The computationally heavy lifting of TensorFlow/PyTorch is actually compiled in C++ under the hood!", "C_plus_plus_Master")
                viewModel.postAnswer(q.id, "Actually, Mojo and Rust are getting some traction, but the community, Jupyter Notebooks, and documentation for Python are so mature that it's very hard to replace it.", "ML_Dev")
            } else if (q.title.contains("Gradient")) {
                viewModel.postAnswer(q.id, "Divergence typically happens when your learning rate (alpha) is too high. Try scaling it down by a factor of 10 (e.g., from 0.1 to 0.01 or 0.001) and check if it stabilizes!", "DataSage")
                viewModel.postAnswer(q.id, "Also, make sure you are normalizing/scaling your input features. If feature values differ by orders of magnitude, your gradient steps will overshoot. Use standard scaling!", "MatrixLord")
            }
        }
    }

    val filteredQuestions = remember(questions, searchQuery, selectedCategory) {
        questions.filter { q ->
            val matchSearch = q.title.contains(searchQuery, ignoreCase = true) || q.content.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "All" || q.category == selectedCategory
            matchSearch && matchCategory
        }
    }

    if (activeQuestion != null) {
        val question = activeQuestion!!

        Column(modifier = modifier.fillMaxSize()) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.selectQuestion(null) },
                    modifier = Modifier.testTag("btn_back_to_forum")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Discussion details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // The main question card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(question.category, fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        labelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "by ${question.authorName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = question.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = question.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.upvoteQuestion(question.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_upvote_question_${question.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upvote (${question.votes})")
                                }

                                Text(
                                    text = "${answers.size} replies",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Replies",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // Replies List
                if (answers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Be the first to share your expertise and answer this query! (+50 XP)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(answers) { ans ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ans.authorName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    IconButton(
                                        onClick = { viewModel.upvoteAnswer(ans.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ThumbUp,
                                                contentDescription = "Upvote answer",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${ans.votes}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = ans.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Reply input box
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newAnswerAuthor,
                            onValueChange = { newAnswerAuthor = it },
                            label = { Text("Posting as...") },
                            modifier = Modifier
                                .width(130.dp)
                                .testTag("input_answer_author"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newAnswerContent,
                            onValueChange = { newAnswerContent = it },
                            placeholder = { Text("Write your expert reply...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_answer_content"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newAnswerContent.isNotBlank() && newAnswerAuthor.isNotBlank()) {
                                    viewModel.postAnswer(question.id, newAnswerContent, newAnswerAuthor)
                                    newAnswerContent = ""
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("btn_send_answer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Post Answer",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    } else {
        // LIST OF DISCUSSIONS VIEW
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showPostDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_question")
                ) {
                    Icon(imageVector = Icons.Default.AddComment, contentDescription = "Ask Question")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
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
                                text = "Lumina Forum",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Collaborate with fellow learners, pose engineering queries, provide feedback, and grow your peer rank.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search discussions...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("forum_search_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                }

                // Horizontal Categories Scroll
                item {
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        modifier = Modifier.padding(vertical = 8.dp),
                        divider = {}
                    ) {
                        categories.forEach { cat ->
                            Tab(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                text = { Text(cat) }
                            )
                        }
                    }
                }

                // Question List
                if (filteredQuestions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "No Discussions Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                } else {
                    items(filteredQuestions) { q ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.selectQuestion(q) }
                                .testTag("question_card_${q.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = q.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "by ${q.authorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = q.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.upvoteQuestion(q.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ThumbUp,
                                                contentDescription = "Upvote",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${q.votes}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Comment,
                                            contentDescription = "Replies",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Open Details",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ASK QUESTION DIALOG
    if (showPostDialog) {
        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text("Ask the Community") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newQuestionAuthor,
                        onValueChange = { newQuestionAuthor = it },
                        label = { Text("Your Username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_post_author"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newQuestionTitle,
                        onValueChange = { newQuestionTitle = it },
                        label = { Text("Question Title") },
                        placeholder = { Text("What are you trying to solve?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_post_title")
                    )

                    // Simple category drop down selection simulation
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Quantum Physics", "Machine Learning", "Mathematics").forEach { cat ->
                            FilterChip(
                                selected = newQuestionCategory == cat,
                                onClick = { newQuestionCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newQuestionContent,
                        onValueChange = { newQuestionContent = it },
                        label = { Text("Context Details") },
                        placeholder = { Text("Describe your query or problem statement so peers can assist...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("input_post_content")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newQuestionTitle.isNotBlank() && newQuestionContent.isNotBlank()) {
                            viewModel.postQuestion(
                                newQuestionTitle,
                                newQuestionContent,
                                newQuestionAuthor,
                                newQuestionCategory
                            )
                            newQuestionTitle = ""
                            newQuestionContent = ""
                            showPostDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_post_question")
                ) {
                    Text("Post Question (+15 XP)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

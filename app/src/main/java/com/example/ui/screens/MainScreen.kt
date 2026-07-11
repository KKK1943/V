package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.LearningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val activeTopic by viewModel.activeTopic.collectAsStateWithLifecycle()
    val gamificationProfile by viewModel.gamificationProfile.collectAsStateWithLifecycle()
    val topic = activeTopic

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing, // Edge-to-edge safe area handling
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Lumina Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Lumina AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (topic != null) {
                                Text(
                                    text = "Topic: ${topic.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        // Engagement Streak Counter
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.setTab(AppTab.GAMIFICATION) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFFF9800), // Amber
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = gamificationProfile.streakCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Avatar icon
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { viewModel.setTab(AppTab.GAMIFICATION) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceVariant, // #211F26 dark navigation container
                tonalElevation = 8.dp
            ) {
                // Tab 1: Discover
                NavigationBarItem(
                    selected = viewModel.selectedTab == AppTab.DISCOVER,
                    onClick = { viewModel.setTab(AppTab.DISCOVER) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == AppTab.DISCOVER) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Discover Tab"
                        )
                    },
                    label = { Text("Discover", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_discover")
                )

                // Tab 2: Study Plans
                NavigationBarItem(
                    selected = viewModel.selectedTab == AppTab.STUDY_PLANS,
                    onClick = { viewModel.setTab(AppTab.STUDY_PLANS) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == AppTab.STUDY_PLANS) Icons.Filled.AssignmentTurnedIn else Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = "Plans Tab"
                        )
                    },
                    label = { Text("Roadmaps", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_study_plans")
                )

                // Tab 3: Classroom
                NavigationBarItem(
                    selected = viewModel.selectedTab == AppTab.CLASSROOM,
                    onClick = { viewModel.setTab(AppTab.CLASSROOM) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == AppTab.CLASSROOM) Icons.Filled.School else Icons.Outlined.School,
                            contentDescription = "Classroom Tab"
                        )
                    },
                    label = { Text("Classroom", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_classroom")
                )

                // Tab 4: Projects
                NavigationBarItem(
                    selected = viewModel.selectedTab == AppTab.PROJECTS,
                    onClick = { viewModel.setTab(AppTab.PROJECTS) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == AppTab.PROJECTS) Icons.Filled.Code else Icons.Outlined.Code,
                            contentDescription = "Projects Tab"
                        )
                    },
                    label = { Text("Projects", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_projects")
                )

                // Tab 5: Forum
                NavigationBarItem(
                    selected = viewModel.selectedTab == AppTab.FORUM,
                    onClick = { viewModel.setTab(AppTab.FORUM) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == AppTab.FORUM) Icons.Filled.Forum else Icons.Outlined.Forum,
                            contentDescription = "Forum Tab"
                        )
                    },
                    label = { Text("Forum", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_forum")
                )

                // Tab 6: Gamification
                NavigationBarItem(
                    selected = viewModel.selectedTab == AppTab.GAMIFICATION,
                    onClick = { viewModel.setTab(AppTab.GAMIFICATION) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == AppTab.GAMIFICATION) Icons.Filled.Leaderboard else Icons.Outlined.Leaderboard,
                            contentDescription = "Gamification Tab"
                        )
                    },
                    label = { Text("Rank", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_gamification")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (viewModel.selectedTab) {
                AppTab.DISCOVER -> DiscoverScreen(viewModel = viewModel)
                AppTab.STUDY_PLANS -> StudyPlansScreen(viewModel = viewModel)
                AppTab.CLASSROOM -> InteractiveLessonScreen(viewModel = viewModel)
                AppTab.PROJECTS -> ProjectsScreen(viewModel = viewModel)
                AppTab.FORUM -> ForumScreen(viewModel = viewModel)
                AppTab.GAMIFICATION -> GamificationScreen(viewModel = viewModel)
            }
        }
    }

    // Beautiful Overlay Alert Dialog for API Key configuration errors or other errors
    if (viewModel.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Notice")
                }
            },
            text = {
                Text(
                    text = viewModel.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

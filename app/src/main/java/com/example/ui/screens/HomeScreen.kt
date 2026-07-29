package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEffectManager
import com.example.data.StoryProjectEntity
import com.example.ui.components.AmbientGlowBackground
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StoryViewModel,
    onNavigateToSettings: () -> Unit
) {
    val lang = viewModel.language
    val projects by viewModel.projects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf("MANGA_WORLDBUILDING") }

    // Seed default sample project ONLY ONCE on clean install if none exists
    LaunchedEffect(projects, lang) {
        if (projects.isEmpty() && !viewModel.hasSeededInitialProject) {
            viewModel.hasSeededInitialProject = true
            viewModel.createProject(
                title = Strings.get("sample_project_title", lang),
                description = Strings.get("sample_project_desc", lang),
                templatePreset = "MANGA_WORLDBUILDING"
            )
        }
    }

    val filteredProjects = projects.filter {
        it.title.contains(viewModel.searchQuery, ignoreCase = true) ||
                it.description.contains(viewModel.searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = Strings.get("app_title", lang),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Settings Button
                    IconButton(
                        onClick = {
                            SoundEffectManager.playClick()
                            onNavigateToSettings()
                        },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = Strings.get("settings", lang)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    SoundEffectManager.playAddNode()
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_canvas_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = Strings.get("new_canvas", lang))
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ambient Glowing Background
            AmbientGlowBackground(
                glowColorHex = viewModel.ambientGlowColorHex,
                isDarkMode = viewModel.isDarkMode,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text(Strings.get("search_projects", lang)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_projects_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Hero Action Card for "Create New Canvas"
                Card(
                    onClick = {
                        SoundEffectManager.playAddNode()
                        showCreateDialog = true
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("new_canvas_hero_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.get("new_canvas", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Strings.get("hero_subtitle", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = Strings.get("my_projects", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.get("no_projects_msg", lang),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProjects, key = { it.id }) { project ->
                            ProjectItemCard(
                                project = project,
                                lang = lang,
                                onClick = {
                                    SoundEffectManager.playClick()
                                    viewModel.openProject(project.id, project.title)
                                },
                                onDelete = {
                                    SoundEffectManager.playDelete()
                                    viewModel.deleteProject(project)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create New Project Dialog with Story Architecture Presets
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(Strings.get("new_canvas", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProjectTitle,
                        onValueChange = { newProjectTitle = it },
                        label = { Text(Strings.get("project_title", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProjectDesc,
                        onValueChange = { newProjectDesc = it },
                        label = { Text(Strings.get("project_desc", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(Strings.get("preset_architecture", lang), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "MANGA_WORLDBUILDING" to Strings.get("preset_manga", lang),
                            "THREE_ACT" to Strings.get("preset_3act", lang),
                            "CHARACTER_MATRIX" to Strings.get("preset_character", lang),
                            "CHAPTER_TIMELINE" to Strings.get("preset_chapter", lang)
                        ).forEach { (presetKey, presetLabel) ->
                            FilterChip(
                                selected = selectedPreset == presetKey,
                                onClick = { selectedPreset = presetKey; SoundEffectManager.playClick() },
                                label = { Text(presetLabel) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectTitle.isNotBlank()) {
                            SoundEffectManager.playSave()
                            viewModel.createProject(
                                title = newProjectTitle,
                                description = newProjectDesc,
                                templatePreset = selectedPreset
                            ) { pid ->
                                viewModel.openProject(pid, newProjectTitle)
                            }
                            newProjectTitle = ""
                            newProjectDesc = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text(Strings.get("create", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectItemCard(
    project: StoryProjectEntity,
    lang: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (project.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = {
                SoundEffectManager.playClick()
                showDeleteConfirm = true
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = Strings.get("delete", lang),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(Strings.get("delete_project_title", lang)) },
            text = { Text("${Strings.get("delete_project_confirm", lang)} '${project.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(Strings.get("delete", lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }
}

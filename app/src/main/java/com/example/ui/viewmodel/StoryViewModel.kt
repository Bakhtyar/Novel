package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.SoundEffectManager
import com.example.data.AppDatabase
import com.example.data.CanvasNodeEntity
import com.example.data.NodeConnectionEntity
import com.example.data.StoryProjectEntity
import com.example.data.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StoryRepository
    private val prefs = application.getSharedPreferences("story_canvas_prefs", android.content.Context.MODE_PRIVATE)

    val audioPlayerManager = AudioPlayerManager(application)

    var language by mutableStateOf(prefs.getString("language", "ar") ?: "ar") // Default "ar"
    var isDarkMode by mutableStateOf(prefs.getBoolean("is_dark_mode", true)) // Dark mode by default

    // Tilemap & Grid settings
    var isGridVisible by mutableStateOf(prefs.getBoolean("is_grid_visible", true))
    fun updateGridVisible(visible: Boolean) {
        isGridVisible = visible
        prefs.edit().putBoolean("is_grid_visible", visible).apply()
    }

    var isSnapToGrid by mutableStateOf(prefs.getBoolean("is_snap_to_grid", false))
    fun updateSnapToGrid(snap: Boolean) {
        isSnapToGrid = snap
        prefs.edit().putBoolean("is_snap_to_grid", snap).apply()
    }

    var gridSize by mutableStateOf(prefs.getFloat("grid_size", 40f))
    fun updateGridSize(size: Float) {
        gridSize = size
        prefs.edit().putFloat("grid_size", size).apply()
    }

    // Ambient Glow settings
    var ambientGlowColorHex by mutableStateOf(prefs.getString("ambient_glow_color", "#8B5CF6") ?: "#8B5CF6")
    fun updateAmbientGlowColor(colorHex: String) {
        ambientGlowColorHex = colorHex
        prefs.edit().putString("ambient_glow_color", colorHex).apply()
    }

    // SFX setting
    var isSfxEnabled by mutableStateOf(prefs.getBoolean("is_sfx_enabled", true))

    fun toggleSfx(enabled: Boolean) {
        isSfxEnabled = enabled
        SoundEffectManager.isSfxEnabled = enabled
        prefs.edit().putBoolean("is_sfx_enabled", enabled).apply()
    }

    var hasSeededInitialProject: Boolean
        get() = prefs.getBoolean("has_seeded_initial_project", false)
        set(value) = prefs.edit().putBoolean("has_seeded_initial_project", value).apply()

    var currentProjectId by mutableStateOf<Long?>(null)
    var currentParentNodeId by mutableStateOf<Long>(0L) // 0 for root canvas, >0 for nested sub-canvas
    var currentSubNodeTitle by mutableStateOf("")

    var selectedNodeForDetail by mutableStateOf<CanvasNodeEntity?>(null)
    var selectedDocumentNode by mutableStateOf<CanvasNodeEntity?>(null) // Full Document Editor
    val isNumberingEnabled = MutableStateFlow(true)
    val nodeNumbers = MutableStateFlow<Map<Long, String>>(emptyMap())
    var isPresentationMode by mutableStateOf(false)

    var isSketchModalOpen by mutableStateOf(false)
    var isMinimapOpen by mutableStateOf(false)
    var isMusicPlayerOpen by mutableStateOf(false)
    var connectingSourceNodeId by mutableStateOf<Long?>(null)

    var searchQuery by mutableStateOf("")
    var nodeFilterType by mutableStateOf("ALL") // "ALL", "Main Topic", "Subtopic", "Character", "Chapter", "Kingdom", "Event"
    var filterPriority by mutableStateOf(0) // 0 for All, 1..5 for specific priority

    var themePreset by mutableStateOf(prefs.getString("theme_preset", "MIDNIGHT_CYBER") ?: "MIDNIGHT_CYBER")
    fun updateThemePreset(preset: String) {
        themePreset = preset
        prefs.edit().putString("theme_preset", preset).apply()
    }

    var branchStyle by mutableStateOf(prefs.getString("branch_style", "CURVED") ?: "CURVED")
    fun updateBranchStyle(style: String) {
        branchStyle = style
        prefs.edit().putString("branch_style", style).apply()
    }

    var focusNodeId by mutableStateOf<Long?>(null)
    val selectedNodeIds = androidx.compose.runtime.mutableStateListOf<Long>()

    fun toggleNodeSelection(id: Long) {
        if (selectedNodeIds.contains(id)) {
            selectedNodeIds.remove(id)
        } else {
            selectedNodeIds.add(id)
        }
    }

    fun clearSelection() {
        selectedNodeIds.clear()
    }

    val projects: StateFlow<List<StoryProjectEntity>>

    private val _currentProjectIdFlow = MutableStateFlow<Long?>(null)
    private val _currentParentNodeIdFlow = MutableStateFlow<Long>(0L)

    val nodes: StateFlow<List<CanvasNodeEntity>> = _currentProjectIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList())
        else {
            _currentParentNodeIdFlow.flatMapLatest { parentId ->
                repository.getNodes(pid, parentId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjectNodes: StateFlow<List<CanvasNodeEntity>> = _currentProjectIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList())
        else repository.getAllNodes(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connections: StateFlow<List<NodeConnectionEntity>> = _currentProjectIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList())
        else repository.getConnections(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val dao = AppDatabase.getDatabase(application).storyDao()
        repository = StoryRepository(dao)
        projects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(allProjectNodes, isNumberingEnabled) { allNodes, enabled ->
                if (enabled) {
                    calculateNodeNumbers(allNodes)
                } else {
                    emptyMap()
                }
            }.collect { numbers ->
                nodeNumbers.value = numbers
            }
        }
    }

    private fun calculateNodeNumbers(nodes: List<CanvasNodeEntity>): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val childrenMap = nodes.groupBy { it.parentNodeId }
        
        fun traverse(parentId: Long, prefix: String) {
            val children = childrenMap[parentId]?.sortedBy { it.orderIndex } ?: return
            for ((index, child) in children.withIndex()) {
                val num = index + 1
                val label = if (prefix.isEmpty()) "$num" else "$prefix.$num"
                result[child.id] = label
                traverse(child.id, label)
            }
        }
        traverse(0L, "")
        return result
    }

    fun changeLanguage(lang: String) {
        language = lang
        prefs.edit().putString("language", lang).apply()
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
    }

    fun openProject(projectId: Long, title: String = "") {
        currentProjectId = projectId
        currentParentNodeId = 0L
        currentSubNodeTitle = title
        _currentProjectIdFlow.value = projectId
        _currentParentNodeIdFlow.value = 0L
    }

    fun openSubCanvas(nodeId: Long, nodeTitle: String) {
        currentParentNodeId = nodeId
        currentSubNodeTitle = nodeTitle
        _currentParentNodeIdFlow.value = nodeId
    }

    fun closeSubCanvas() {
        currentParentNodeId = 0L
        _currentParentNodeIdFlow.value = 0L
    }

    fun closeProject() {
        currentProjectId = null
        currentParentNodeId = 0L
        selectedDocumentNode = null
        isPresentationMode = false
        _currentProjectIdFlow.value = null
        _currentParentNodeIdFlow.value = 0L
    }

    fun openDocumentForNode(node: CanvasNodeEntity) {
        selectedDocumentNode = node
        SoundEffectManager.playOpenNote()
    }

    fun closeDocumentEditor() {
        selectedDocumentNode = null
        SoundEffectManager.playCloseNote()
    }

    fun toggleCollapseNode(node: CanvasNodeEntity) {
        viewModelScope.launch {
            repository.updateNode(node.copy(isCollapsed = !node.isCollapsed))
            SoundEffectManager.playClick()
        }
    }

    fun quickAddNode(sourceNode: CanvasNodeEntity, direction: String) {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val offset = when(direction) {
                "RIGHT" -> Pair(300f, 0f)
                "LEFT" -> Pair(-300f, 0f)
                "BOTTOM" -> Pair(0f, 180f)
                "TOP" -> Pair(0f, -180f)
                else -> Pair(300f, 0f)
            }
            val newX = sourceNode.xPos + offset.first
            val newY = sourceNode.yPos + offset.second
            val newNode = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = currentParentNodeId,
                title = "New Node",
                nodeType = "Subtopic",
                colorHex = sourceNode.colorHex,
                xPos = newX,
                yPos = newY
            )
            val newId = repository.insertNode(newNode)
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = sourceNode.id, toNodeId = newId, label = "Child"))
            SoundEffectManager.playAddNode()
        }
    }

    fun createProject(title: String, description: String, templatePreset: String = "MANGA_WORLDBUILDING", onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val proj = StoryProjectEntity(title = title, description = description, isPinned = true, templatePreset = templatePreset)
            val pid = repository.insertProject(proj)

            when (templatePreset) {
                "MANGA_WORLDBUILDING" -> seedMangaWorldbuilding(pid)
                "THREE_ACT" -> seedThreeActPlot(pid)
                "CHARACTER_MATRIX" -> seedCharacterMatrix(pid)
                "CHAPTER_TIMELINE" -> seedChapterTimeline(pid)
                else -> seedMangaWorldbuilding(pid)
            }

            onCreated(pid)
        }
    }

    private suspend fun seedMangaWorldbuilding(pid: Long) {
        val root = CanvasNodeEntity(projectId = pid, title = "World: Astraea Realm", content = "Main Worldbuilding Hub", nodeType = "Main Topic", colorHex = "#8B5CF6", xPos = 400f, yPos = 100f, markerIcon = "kingdom", documentNote = "# Astraea Realm Codex\nWelcome to the official codex of Astraea.")
        val rootId = repository.insertNode(root)

        val k1 = CanvasNodeEntity(projectId = pid, title = "Solaris Empire", content = "Dominant sun-magic kingdom", nodeType = "Kingdom", colorHex = "#F59E0B", xPos = 100f, yPos = 300f, markerIcon = "kingdom")
        val k2 = CanvasNodeEntity(projectId = pid, title = "Shadow Guild", content = "Underground rebellion", nodeType = "Kingdom", colorHex = "#EC4899", xPos = 700f, yPos = 300f, markerIcon = "secret")
        val k1Id = repository.insertNode(k1)
        val k2Id = repository.insertNode(k2)

        val c1 = CanvasNodeEntity(projectId = pid, title = "Kaelen Vane", content = "Exiled Sun Knight protagonist", nodeType = "Character", colorHex = "#3B82F6", xPos = 100f, yPos = 550f, markerIcon = "person", documentNote = "# Kaelen Vane\n- **Age**: 22\n- **Weapon**: Sol Blade\n- **Goal**: Restore honour.")
        val c2 = CanvasNodeEntity(projectId = pid, title = "Lyra Shadowsong", content = "Guild assassin & confidante", nodeType = "Character", colorHex = "#10B981", xPos = 700f, yPos = 550f, markerIcon = "person")
        val c1Id = repository.insertNode(c1)
        val c2Id = repository.insertNode(c2)

        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = rootId, toNodeId = k1Id, label = "Capital Realm"))
        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = rootId, toNodeId = k2Id, label = "Underworld"))
        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = k1Id, toNodeId = c1Id, label = "Native Knight"))
        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = k2Id, toNodeId = c2Id, label = "Operative"))
        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = c1Id, toNodeId = c2Id, label = "Secret Allies"))
    }

    private suspend fun seedThreeActPlot(pid: Long) {
        val n1 = CanvasNodeEntity(projectId = pid, title = "Act 1: Inciting Event", content = "The quiet village is raided by mystery riders.", nodeType = "Event", colorHex = "#3B82F6", xPos = 100f, yPos = 200f)
        val n2 = CanvasNodeEntity(projectId = pid, title = "Act 2: Midpoint Climax", content = "Discovery of the ancient dragon seal.", nodeType = "Event", colorHex = "#F59E0B", xPos = 450f, yPos = 200f)
        val n3 = CanvasNodeEntity(projectId = pid, title = "Act 3: Final Battle", content = "Confrontation at the Eclipse Citadel.", nodeType = "Conclusion", colorHex = "#EF4444", xPos = 800f, yPos = 200f)

        val id1 = repository.insertNode(n1)
        val id2 = repository.insertNode(n2)
        val id3 = repository.insertNode(n3)

        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id1, toNodeId = id2, label = "Escalates to"))
        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id2, toNodeId = id3, label = "Culminates in"))
    }

    private suspend fun seedCharacterMatrix(pid: Long) {
        val hero = CanvasNodeEntity(projectId = pid, title = "Protagonist", content = "Driven, passionate hero.", nodeType = "Character", colorHex = "#3B82F6", xPos = 200f, yPos = 200f, markerIcon = "person")
        val rival = CanvasNodeEntity(projectId = pid, title = "Rival / Antagonist", content = "Cold, calculating strategist.", nodeType = "Character", colorHex = "#EF4444", xPos = 600f, yPos = 200f, markerIcon = "person")

        val hId = repository.insertNode(hero)
        val rId = repository.insertNode(rival)

        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = hId, toNodeId = rId, label = "Arch Rivals"))
    }

    private suspend fun seedChapterTimeline(pid: Long) {
        val ch1 = CanvasNodeEntity(projectId = pid, title = "Chapter 1: The Call", content = "Hero leaves home village.", nodeType = "Chapter", colorHex = "#8B5CF6", xPos = 100f, yPos = 250f, dateLabel = "Day 1")
        val ch2 = CanvasNodeEntity(projectId = pid, title = "Chapter 2: The Forest", content = "Encounter with wild spirits.", nodeType = "Chapter", colorHex = "#10B981", xPos = 400f, yPos = 250f, dateLabel = "Day 3")
        val ch3 = CanvasNodeEntity(projectId = pid, title = "Chapter 3: Royal Gate", content = "Entry into the capital city.", nodeType = "Chapter", colorHex = "#F59E0B", xPos = 700f, yPos = 250f, dateLabel = "Day 7")

        val id1 = repository.insertNode(ch1)
        val id2 = repository.insertNode(ch2)
        val id3 = repository.insertNode(ch3)

        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id1, toNodeId = id2, label = "Next"))
        repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id2, toNodeId = id3, label = "Next"))
    }

    fun deleteProject(project: StoryProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (currentProjectId == project.id) {
                closeProject()
            }
        }
    }

    fun addNode(
        title: String,
        content: String,
        nodeType: String,
        colorHex: String,
        markerIcon: String = "star",
        tags: String = "",
        x: Float = 200f,
        y: Float = 200f
    ) {
        val pid = currentProjectId ?: return
        val finalX = if (isSnapToGrid) (kotlin.math.round(x / gridSize) * gridSize) else x
        val finalY = if (isSnapToGrid) (kotlin.math.round(y / gridSize) * gridSize) else y
        viewModelScope.launch {
            val siblingsCount = allProjectNodes.value.count { it.parentNodeId == currentParentNodeId }
            val node = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = currentParentNodeId,
                title = title,
                content = content,
                nodeType = nodeType,
                colorHex = colorHex,
                markerIcon = markerIcon,
                tags = tags,
                xPos = finalX,
                yPos = finalY,
                orderIndex = siblingsCount
            )
            repository.insertNode(node)
            SoundEffectManager.playAddNode()
        }
    }

    val optimisticNodePositions = mutableStateMapOf<Long, Offset>()

    fun updateNodePosition(nodeId: Long, x: Float, y: Float) {
        val finalX = if (isSnapToGrid) (kotlin.math.round(x / gridSize) * gridSize) else x
        val finalY = if (isSnapToGrid) (kotlin.math.round(y / gridSize) * gridSize) else y
        
        optimisticNodePositions[nodeId] = Offset(finalX, finalY)

        viewModelScope.launch {
            val currentNodes = allProjectNodes.value
            val node = currentNodes.find { it.id == nodeId }
            if (node != null) {
                repository.updateNode(node.copy(xPos = finalX, yPos = finalY))
            }
        }
    }

    fun updateNodeDetails(node: CanvasNodeEntity) {
        viewModelScope.launch {
            repository.updateNode(node.copy(updatedAt = System.currentTimeMillis()))
            SoundEffectManager.playClick()
        }
    }

    fun deleteNode(node: CanvasNodeEntity) {
        viewModelScope.launch {
            repository.deleteNode(node)
            if (selectedNodeForDetail?.id == node.id) {
                selectedNodeForDetail = null
            }
            if (selectedDocumentNode?.id == node.id) {
                selectedDocumentNode = null
            }
            SoundEffectManager.playDelete()
        }
    }

    fun duplicateNode(node: CanvasNodeEntity) {
        val finalX = if (isSnapToGrid) (kotlin.math.round((node.xPos + gridSize) / gridSize) * gridSize) else node.xPos + 40f
        val finalY = if (isSnapToGrid) (kotlin.math.round((node.yPos + gridSize) / gridSize) * gridSize) else node.yPos + 40f
        viewModelScope.launch {
            val dup = node.copy(
                id = 0L,
                title = "${node.title} (Copy)",
                xPos = finalX,
                yPos = finalY
            )
            repository.insertNode(dup)
            SoundEffectManager.playAddNode()
        }
    }

    fun addConnection(fromId: Long, toId: Long, label: String = "") {
        val pid = currentProjectId ?: return
        if (fromId == toId) return
        viewModelScope.launch {
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = fromId, toNodeId = toId, label = label))
            SoundEffectManager.playConnect()
        }
    }

    fun deleteConnection(conn: NodeConnectionEntity) {
        viewModelScope.launch {
            repository.deleteConnection(conn)
        }
    }

    fun addSubtopic(parent: CanvasNodeEntity, title: String = "Subtopic") {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val siblings = allProjectNodes.value.filter { it.parentNodeId == parent.id }
            val newY = parent.yPos + (siblings.size * 120f) - 40f
            val newX = parent.xPos + 280f
            val child = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = parent.id,
                title = title,
                nodeType = "Subtopic",
                colorHex = parent.colorHex,
                xPos = newX,
                yPos = newY,
                orderIndex = siblings.size
            )
            val childId = repository.insertNode(child)
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = parent.id, toNodeId = childId, label = ""))
            SoundEffectManager.playAddNode()
        }
    }

    fun addParentTopic(targetNode: CanvasNodeEntity, title: String = "Parent Topic") {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val newParentX = targetNode.xPos - 260f
            val newParentY = targetNode.yPos
            val newParent = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = targetNode.parentNodeId,
                title = title,
                nodeType = "Parent Topic",
                colorHex = targetNode.colorHex,
                xPos = newParentX,
                yPos = newParentY
            )
            val newParentId = repository.insertNode(newParent)
            // Re-parent target node under the new parent
            repository.updateNode(targetNode.copy(parentNodeId = newParentId))
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = newParentId, toNodeId = targetNode.id, label = ""))
            SoundEffectManager.playAddNode()
        }
    }

    fun addSiblingTopic(targetNode: CanvasNodeEntity, title: String = "Sibling Topic") {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val siblings = allProjectNodes.value.filter { it.parentNodeId == targetNode.parentNodeId }
            val newX = targetNode.xPos
            val newY = targetNode.yPos + 140f
            val sibling = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = targetNode.parentNodeId,
                title = title,
                nodeType = "Subtopic",
                colorHex = targetNode.colorHex,
                xPos = newX,
                yPos = newY,
                orderIndex = siblings.size
            )
            val sibId = repository.insertNode(sibling)
            if (targetNode.parentNodeId > 0L) {
                repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = targetNode.parentNodeId, toNodeId = sibId, label = ""))
            }
            SoundEffectManager.playAddNode()
        }
    }

    fun addFloatingTopic(x: Float = 200f, y: Float = 200f, title: String = "Floating Idea") {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val floating = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = 0L,
                title = title,
                nodeType = "Floating",
                colorHex = "#F59E0B",
                xPos = x,
                yPos = y
            )
            repository.insertNode(floating)
            SoundEffectManager.playAddNode()
        }
    }

    fun addCallout(targetNode: CanvasNodeEntity, text: String = "Callout Note") {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val callout = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = targetNode.id,
                title = text,
                nodeType = "Callout",
                shape = "CALLOUT",
                colorHex = "#FBBF24",
                xPos = targetNode.xPos + 220f,
                yPos = targetNode.yPos - 80f
            )
            val cId = repository.insertNode(callout)
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = targetNode.id, toNodeId = cId, label = "Note", style = "DASHED"))
            SoundEffectManager.playAddNode()
        }
    }

    fun addSummary(selectedNodesList: List<CanvasNodeEntity>, title: String = "Summary") {
        val pid = currentProjectId ?: return
        if (selectedNodesList.isEmpty()) return
        viewModelScope.launch {
            val avgX = selectedNodesList.map { it.xPos }.average().toFloat() + 280f
            val avgY = selectedNodesList.map { it.yPos }.average().toFloat()
            val summaryNode = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = 0L,
                title = title,
                nodeType = "Summary",
                shape = "SUMMARY",
                colorHex = "#EC4899",
                xPos = avgX,
                yPos = avgY,
                boundaryGroup = "SummaryGroup_${System.currentTimeMillis()}"
            )
            val summaryId = repository.insertNode(summaryNode)
            for (node in selectedNodesList) {
                repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = node.id, toNodeId = summaryId, label = "Summarizes", style = "DASHED"))
            }
            SoundEffectManager.playAddNode()
        }
    }

    fun bulkDeleteSelected() {
        if (selectedNodeIds.isEmpty()) return
        viewModelScope.launch {
            val all = allProjectNodes.value.filter { selectedNodeIds.contains(it.id) }
            for (n in all) {
                repository.deleteNode(n)
            }
            selectedNodeIds.clear()
            SoundEffectManager.playDelete()
        }
    }

    fun bulkColorSelected(colorHex: String) {
        if (selectedNodeIds.isEmpty()) return
        viewModelScope.launch {
            val all = allProjectNodes.value.filter { selectedNodeIds.contains(it.id) }
            for (n in all) {
                repository.updateNode(n.copy(colorHex = colorHex))
            }
            SoundEffectManager.playClick()
        }
    }

    fun bulkSetBoundary(groupName: String) {
        if (selectedNodeIds.isEmpty()) return
        viewModelScope.launch {
            val all = allProjectNodes.value.filter { selectedNodeIds.contains(it.id) }
            for (n in all) {
                repository.updateNode(n.copy(boundaryGroup = groupName))
            }
            SoundEffectManager.playClick()
        }
    }

    // Auto Layout Structures (MindMap, Tree, Logic, Org, Timeline, Fishbone, Matrix, TreeTable)
    fun arrangeStructure(structureType: String) {
        val currentNodes = nodes.value
        if (currentNodes.isEmpty()) return
        viewModelScope.launch {
            when (structureType) {
                "TIMELINE" -> {
                    var x = 100f
                    for (node in currentNodes) {
                        repository.updateNode(node.copy(xPos = x, yPos = 300f))
                        x += 320f
                    }
                }
                "TREE", "ORG" -> {
                    val root = currentNodes.firstOrNull { it.nodeType == "Main Topic" } ?: currentNodes.first()
                    var x = 100f
                    var y = 100f
                    for ((index, node) in currentNodes.withIndex()) {
                        x = 100f + (index % 4) * 300f
                        y = 100f + (index / 4) * 220f
                        repository.updateNode(node.copy(xPos = x, yPos = y))
                    }
                }
                "LOGIC" -> {
                    var x = 150f
                    var y = 150f
                    for ((index, node) in currentNodes.withIndex()) {
                        x = 150f + (index % 2) * 350f
                        y = 150f + (index / 2) * 200f
                        repository.updateNode(node.copy(xPos = x, yPos = y))
                    }
                }
                "FISHBONE" -> {
                    var x = 100f
                    for ((index, node) in currentNodes.withIndex()) {
                        val yOffset = if (index % 2 == 0) -160f else 160f
                        repository.updateNode(node.copy(xPos = x, yPos = 300f + yOffset))
                        x += 240f
                    }
                }
                "MATRIX", "TREE_TABLE" -> {
                    for ((index, node) in currentNodes.withIndex()) {
                        val col = index % 3
                        val row = index / 3
                        repository.updateNode(node.copy(xPos = 120f + col * 310f, yPos = 120f + row * 220f))
                    }
                }
                else -> { // MINDMAP (Bilateral / Radial layout)
                    val root = currentNodes.firstOrNull { it.nodeType == "Main Topic" } ?: currentNodes.first()
                    val children = currentNodes.filter { it.id != root.id }
                    repository.updateNode(root.copy(xPos = 500f, yPos = 400f))
                    
                    val half = (children.size + 1) / 2
                    for ((index, node) in children.withIndex()) {
                        val isRight = index < half
                        val sideIndex = if (isRight) index else (index - half)
                        val x = if (isRight) 820f else 180f
                        val y = 200f + (sideIndex * 150f)
                        repository.updateNode(node.copy(xPos = x, yPos = y))
                    }
                }
            }
            SoundEffectManager.playClick()
        }
    }

    // Export Story Project as Markdown String
    fun generateMarkdownExport(): String {
        val currentNodes = allProjectNodes.value
        val sb = StringBuilder()
        sb.append("# Story Outline Export\n\n")

        for (node in currentNodes) {
            sb.append("## [${node.nodeType}] ${node.title}\n")
            if (node.content.isNotBlank()) {
                sb.append("**Summary**: ${node.content}\n\n")
            }
            if (node.documentNote.isNotBlank()) {
                sb.append("${node.documentNote}\n\n")
            }
            sb.append("---\n\n")
        }
        return sb.toString()
    }

    fun generateJsonExport(): String {
        val currentNodes = allProjectNodes.value
        val currentConns = connections.value
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"project_id\": ${currentProjectId ?: 0},\n")
        sb.append("  \"nodes_count\": ${currentNodes.size},\n")
        sb.append("  \"nodes\": [\n")
        for ((idx, node) in currentNodes.withIndex()) {
            val comma = if (idx < currentNodes.size - 1) "," else ""
            sb.append("    {\"id\": ${node.id}, \"title\": \"${node.title.replace("\"", "\\\"")}\", \"type\": \"${node.nodeType}\", \"x\": ${node.xPos}, \"y\": ${node.yPos}}$comma\n")
        }
        sb.append("  ],\n")
        sb.append("  \"connections\": [\n")
        for ((idx, conn) in currentConns.withIndex()) {
            val comma = if (idx < currentConns.size - 1) "," else ""
            sb.append("    {\"from\": ${conn.fromNodeId}, \"to\": ${conn.toNodeId}, \"label\": \"${conn.label}\"}$comma\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }
}

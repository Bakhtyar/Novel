package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    var language by mutableStateOf("en") // "en" or "ar"
    var isDarkMode by mutableStateOf(true) // Dark mode by default

    var currentProjectId by mutableStateOf<Long?>(null)
    var currentParentNodeId by mutableStateOf<Long>(0L) // 0 for root canvas, >0 for nested sub-canvas
    var currentSubNodeTitle by mutableStateOf("")

    var selectedNodeForDetail by mutableStateOf<CanvasNodeEntity?>(null)
    var isSketchModalOpen by mutableStateOf(false)
    var isMinimapOpen by mutableStateOf(false)
    var connectingSourceNodeId by mutableStateOf<Long?>(null)

    var searchQuery by mutableStateOf("")

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
        else repository.getNodes(pid, 0L) // For minimap & connections across root
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connections: StateFlow<List<NodeConnectionEntity>> = _currentProjectIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList())
        else repository.getConnections(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val dao = AppDatabase.getDatabase(application).storyDao()
        repository = StoryRepository(dao)
        projects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Seed sample project if database is empty
        viewModelScope.launch {
            // We can check if projects are empty and create sample project
        }
    }

    fun changeLanguage(lang: String) {
        language = lang
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
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
        _currentProjectIdFlow.value = null
        _currentParentNodeIdFlow.value = 0L
    }

    fun createProject(title: String, description: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val proj = StoryProjectEntity(title = title, description = description, isPinned = true)
            val pid = repository.insertProject(proj)
            // Seed sample nodes for new project
            val n1 = CanvasNodeEntity(projectId = pid, title = "The Inciting Incident", content = "The protagonist discovers a mysterious glowing artifact in the ancient ruins.", nodeType = "Event", colorHex = "#3B82F6", xPos = 120f, yPos = 150f)
            val n2 = CanvasNodeEntity(projectId = pid, title = "Hero: Elian", content = "A young cartographer with a hidden royal lineage and a thirst for adventure.", nodeType = "Character", colorHex = "#10B981", xPos = 450f, yPos = 150f)
            val n3 = CanvasNodeEntity(projectId = pid, title = "Chapter 1: Whispers", content = "Elian packs his map bag and leaves the village before dawn.", nodeType = "Chapter", colorHex = "#8B5CF6", xPos = 120f, yPos = 400f)
            val n4 = CanvasNodeEntity(projectId = pid, title = "The Climax & Conclusion", content = "The artifact is sealed, but the realm is forever altered.", nodeType = "Conclusion", colorHex = "#EF4444", xPos = 450f, yPos = 400f)
            
            val id1 = repository.insertNode(n1)
            val id2 = repository.insertNode(n2)
            val id3 = repository.insertNode(n3)
            val id4 = repository.insertNode(n4)

            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id1, toNodeId = id3, label = "Leads to"))
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id2, toNodeId = id3, label = "Involves"))
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = id3, toNodeId = id4, label = "Progresses to"))

            onCreated(pid)
        }
    }

    fun deleteProject(project: StoryProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (currentProjectId == project.id) {
                closeProject()
            }
        }
    }

    fun addNode(title: String, content: String, nodeType: String, colorHex: String, x: Float = 200f, y: Float = 200f) {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val node = CanvasNodeEntity(
                projectId = pid,
                parentNodeId = currentParentNodeId,
                title = title,
                content = content,
                nodeType = nodeType,
                colorHex = colorHex,
                xPos = x,
                yPos = y
            )
            repository.insertNode(node)
        }
    }

    fun updateNodePosition(nodeId: Long, x: Float, y: Float) {
        val currentNodes = nodes.value
        val node = currentNodes.find { it.id == nodeId } ?: return
        viewModelScope.launch {
            repository.updateNode(node.copy(xPos = x, yPos = y))
        }
    }

    fun updateNodeDetails(node: CanvasNodeEntity) {
        viewModelScope.launch {
            repository.updateNode(node.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNode(node: CanvasNodeEntity) {
        viewModelScope.launch {
            repository.deleteNode(node)
            if (selectedNodeForDetail?.id == node.id) {
                selectedNodeForDetail = null
            }
        }
    }

    fun duplicateNode(node: CanvasNodeEntity) {
        viewModelScope.launch {
            val dup = node.copy(
                id = 0L,
                title = "${node.title} (Copy)",
                xPos = node.xPos + 40f,
                yPos = node.yPos + 40f
            )
            repository.insertNode(dup)
        }
    }

    fun addConnection(fromId: Long, toId: Long, label: String = "") {
        val pid = currentProjectId ?: return
        if (fromId == toId) return
        viewModelScope.launch {
            repository.insertConnection(NodeConnectionEntity(projectId = pid, fromNodeId = fromId, toNodeId = toId, label = label))
        }
    }

    fun deleteConnection(conn: NodeConnectionEntity) {
        viewModelScope.launch {
            repository.deleteConnection(conn)
        }
    }
}

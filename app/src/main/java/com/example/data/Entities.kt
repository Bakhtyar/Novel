package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_projects")
data class StoryProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "canvas_nodes")
data class CanvasNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val parentNodeId: Long = 0L, // 0 means root canvas, >0 means nested inside another node
    val title: String,
    val content: String,
    val nodeType: String = "Event", // Chapter, Event, Character, Idea, Conclusion
    val colorHex: String = "#3B82F6", // Blue default
    val xPos: Float = 100f,
    val yPos: Float = 100f,
    val width: Float = 220f,
    val height: Float = 140f,
    val tags: String = "",
    val imageUri: String = "",
    val sketchData: String = "",
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "node_connections")
data class NodeConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val label: String = ""
)

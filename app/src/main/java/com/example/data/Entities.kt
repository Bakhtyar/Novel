package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_projects")
data class StoryProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val structureType: String = "MINDMAP", // MINDMAP, TREE, LOGIC, ORG, TIMELINE, FISHBONE
    val templatePreset: String = "CUSTOM"
)

@Entity(tableName = "canvas_nodes")
data class CanvasNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val parentNodeId: Long = 0L, // 0 for root, >0 for sub-canvas
    val title: String,
    val content: String = "",
    val nodeType: String = "Event", // Main Topic, Subtopic, Character, Chapter, Event, Kingdom, Ability, Secret, Timeline, Idea
    val colorHex: String = "#3B82F6",
    val xPos: Float = 100f,
    val yPos: Float = 100f,
    val width: Float = 220f,
    val height: Float = 140f,
    val tags: String = "",
    val imageUri: String = "",
    val sketchData: String = "",
    val isPinned: Boolean = false,
    val isCollapsed: Boolean = false,
    val isCompleted: Boolean = false,
    val progress: Int = 0,
    val dateLabel: String = "",
    val markerIcon: String = "star",
    val boundaryGroup: String = "",
    val linkUrl: String = "",
    val documentNote: String = "", // Dedicated full-page document note content
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "node_connections")
data class NodeConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val label: String = "",
    val style: String = "SOLID"
)

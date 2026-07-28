package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM story_projects ORDER BY isPinned DESC, createdAt DESC")
    fun getAllProjects(): Flow<List<StoryProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: StoryProjectEntity): Long

    @Update
    suspend fun updateProject(project: StoryProjectEntity)

    @Delete
    suspend fun deleteProject(project: StoryProjectEntity)

    @Query("SELECT * FROM canvas_nodes WHERE projectId = :projectId AND parentNodeId = :parentNodeId")
    fun getNodesForProject(projectId: Long, parentNodeId: Long = 0L): Flow<List<CanvasNodeEntity>>

    @Query("SELECT * FROM canvas_nodes WHERE projectId = :projectId")
    suspend fun getAllNodesForProjectSync(projectId: Long): List<CanvasNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: CanvasNodeEntity): Long

    @Update
    suspend fun updateNode(node: CanvasNodeEntity)

    @Delete
    suspend fun deleteNode(node: CanvasNodeEntity)

    @Query("SELECT * FROM node_connections WHERE projectId = :projectId")
    fun getConnectionsForProject(projectId: Long): Flow<List<NodeConnectionEntity>>

    @Query("SELECT * FROM node_connections WHERE projectId = :projectId")
    suspend fun getAllConnectionsForProjectSync(projectId: Long): List<NodeConnectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: NodeConnectionEntity): Long

    @Delete
    suspend fun deleteConnection(connection: NodeConnectionEntity)
    
    @Query("DELETE FROM node_connections WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteConnectionsForNode(nodeId: Long)
}

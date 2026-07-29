package com.example.data

import kotlinx.coroutines.flow.Flow

class StoryRepository(private val dao: StoryDao) {
    val allProjects: Flow<List<StoryProjectEntity>> = dao.getAllProjects()

    suspend fun insertProject(project: StoryProjectEntity): Long = dao.insertProject(project)
    suspend fun updateProject(project: StoryProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(project: StoryProjectEntity) = dao.deleteProject(project)

    fun getNodes(projectId: Long, parentNodeId: Long = 0L): Flow<List<CanvasNodeEntity>> =
        dao.getNodesForProject(projectId, parentNodeId)

    fun getAllNodes(projectId: Long): Flow<List<CanvasNodeEntity>> =
        dao.getAllNodesForProject(projectId)

    suspend fun insertNode(node: CanvasNodeEntity): Long = dao.insertNode(node)
    suspend fun updateNode(node: CanvasNodeEntity) = dao.updateNode(node)
    suspend fun deleteNode(node: CanvasNodeEntity) {
        dao.deleteConnectionsForNode(node.id)
        dao.deleteNode(node)
    }

    fun getConnections(projectId: Long): Flow<List<NodeConnectionEntity>> =
        dao.getConnectionsForProject(projectId)

    suspend fun insertConnection(connection: NodeConnectionEntity): Long = dao.insertConnection(connection)
    suspend fun deleteConnection(connection: NodeConnectionEntity) = dao.deleteConnection(connection)

    suspend fun seedSampleDataIfEmpty() {
        // We can check if projects exist; if not, create a sample project with nodes & connections
    }
}

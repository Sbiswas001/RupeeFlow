package sayan.apps.rupeeflow.core.ai.tools

sealed class ToolResult {
    data class Success(
        val summary: String,
        val structuredData: Map<String, Any?> = emptyMap()
    ) : ToolResult()
    
    data class Error(val message: String) : ToolResult()
}

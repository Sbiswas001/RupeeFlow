# RupeeFlow AI: Financial Operating System (Phases 4-6)

This plan evolves RupeeFlow AI from a passive chatbot into a proactive financial operating system with memory, insights, and a financial timeline.

## User Review Required

> [!NOTE]
> This phase focuses on "Intelligence" rather than "Plumbing". We are building the logic for the AI to understand trends and provide proactive alerts.

## Proposed Changes

### 1. Insights & Timeline (Phase 6)
- **[NEW] [InsightsEngine.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/domain/ai/InsightsEngine.kt)**: Logic for Spending Velocity and Weekly Trends.
- **[NEW] [FinancialTimelineTool.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/tools/FinancialTimelineTool.kt)**: Summarizes "What happened this week?".

### 2. Proactive Intelligence (Phase 5)
- **[NEW] [ProactiveInsightsTool.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/tools/ProactiveInsightsTool.kt)**: Checks for budget alerts and velocity warnings.
- **[MODIFY] [ToolPlanner.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/planner/ToolPlanner.kt)**: Automatically bundles proactive alerts with summary queries.

### 3. Memory & Personalization (Phase 4)
- **[MODIFY] [AddExpenseTool.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/tools/AddExpenseTool.kt)**: Hooks into `KnowledgeGraph` to learn user categorization preferences.
- **[MODIFY] [KnowledgeGraph.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/engine/KnowledgeGraph.kt)**: Adds persistence (in-memory for now) for learned merchant mappings.

## Verification Plan

### Automated Tests
- `InsightsEngineTest`: Verify velocity calculation.
- `ToolPlannerTest`: Verify proactive alerts are bundled correctly.

### Manual Verification
- Ask "What happened this week?" and verify the timeline summary.
- Add an expense for a new merchant and verify the AI "remembers" the category next time.

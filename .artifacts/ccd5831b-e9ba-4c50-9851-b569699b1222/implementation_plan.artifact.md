# Local AI Integration for RupeeFlow

Integrate a local LLM (Gemma 3 1B) using MediaPipe LLM Inference to provide natural language financial insights, spending summaries, and smart categorization, all while remaining completely offline.

## User Review Required

> [!IMPORTANT]
> This feature requires the user to download a ~800MB model file (`gemma-3-1b-it-int4.bin`) on first use. We should provide a clear UI for this download process and explain the privacy benefits (offline processing).

> [!NOTE]
> GPU acceleration is recommended for the best experience. MediaPipe will automatically use the GPU if available.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/gradle/libs.versions.toml)
- Add MediaPipe GenAI dependency.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/build.gradle.kts)
- Include the MediaPipe library.

---

### Core Data & Engine

#### [MODIFY] [TransactionDao.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/dao/TransactionDao.kt)
- Add queries for top merchants, spending anomalies, and recurring pattern detection.

#### [NEW] [FinanceEngine.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/FinanceEngine.kt)
- A bridge between the LLM intents and the Room database. It executes the structured queries requested by the AI.

---

### AI Layer

#### [NEW] [AiManager.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/AiManager.kt)
- Handles LLM initialization, inference, and resource management using MediaPipe.

#### [NEW] [IntentExtractor.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/IntentExtractor.kt)
- Contains the system prompts and logic to convert natural language into `FinanceIntent` objects.

---

### UI Integration

#### [NEW] [AiChatScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/ai/AiChatScreen.kt)
- A dedicated chat interface for interacting with the RupeeFlow Assistant.

#### [NEW] [AiViewModel.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/ai/AiViewModel.kt)
- Manages the state of the AI assistant, model download status, and conversation history.

## Verification Plan

### Automated Tests
- Unit tests for `IntentExtractor` to verify it correctly parses various user queries into intents.
- Integration tests for `FinanceEngine` to ensure it returns correct data from the Room DB.

### Manual Verification
- Deploy to a physical device.
- Trigger model download.
- Test queries:
    - "How much did I spend on food this month?"
    - "Who are my top 5 merchants?"
    - "Compare my shopping expenses between June and July."
    - "Do I have any recurring subscriptions?"

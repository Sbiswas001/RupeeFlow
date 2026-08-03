package sayan.apps.rupeeflow.feature.ai

enum class AiStatus {
    UNKNOWN,
    CHECKING_DEVICE,
    INCOMPATIBLE,
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFYING,
    INITIALIZING,
    READY,
    FAILED
}

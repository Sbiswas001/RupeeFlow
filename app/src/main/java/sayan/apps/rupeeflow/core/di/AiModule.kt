package sayan.apps.rupeeflow.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.hilt.android.qualifiers.ApplicationContext
import sayan.apps.rupeeflow.core.ai.backend.AiBackend
import sayan.apps.rupeeflow.core.ai.backend.MediaPipeAiBackend
import sayan.apps.rupeeflow.core.ai.backend.MockAiBackend
import sayan.apps.rupeeflow.core.ai.engine.*
import sayan.apps.rupeeflow.core.ai.metrics.AiMetricsCollector
import sayan.apps.rupeeflow.core.ai.model.ModelManager
import sayan.apps.rupeeflow.core.ai.model.ModelManagerImpl
import sayan.apps.rupeeflow.core.ai.session.AiSession
import sayan.apps.rupeeflow.core.ai.tools.*
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindModelManager(impl: ModelManagerImpl): ModelManager

    @Binds
    @IntoSet
    abstract fun bindMonthlySummaryTool(tool: MonthlySummaryTool): FinanceTool

    @Binds
    @IntoSet
    abstract fun bindCategoryExpenseTool(tool: CategoryExpenseTool): FinanceTool

    @Binds
    @IntoSet
    abstract fun bindBudgetStatusTool(tool: BudgetStatusTool): FinanceTool

    companion object {
        @Provides
        @Singleton
        fun provideAiBackend(
            @ApplicationContext context: android.content.Context,
            mediaPipeBackend: MediaPipeAiBackend,
            mockBackend: MockAiBackend
        ): AiBackend {
            val modelFile = File(context.filesDir, "models/gemma-3-1b-it-int4.task")
            return if (modelFile.exists() && modelFile.length() > 1024 * 1024) {
                mediaPipeBackend
            } else {
                mockBackend
            }
        }

        @Provides
        @Singleton
        fun provideAiSession(): AiSession = AiSession()

        @Provides
        @Singleton
        fun provideFinanceEngine(
            analyticsEngine: AnalyticsEngine,
            categoryAnalyzer: CategoryAnalyzer,
            merchantAnalyzer: MerchantAnalyzer,
            budgetAnalyzer: BudgetAnalyzer,
            subscriptionAnalyzer: SubscriptionAnalyzer,
            metricsCollector: AiMetricsCollector
        ): FinanceEngine = FinanceEngine(
            analyticsEngine,
            categoryAnalyzer,
            merchantAnalyzer,
            budgetAnalyzer,
            subscriptionAnalyzer,
            metricsCollector
        )

        @Provides
        @Singleton
        fun provideToolRegistry(
            tools: Set<@JvmSuppressWildcards FinanceTool>
        ): ToolRegistry = ToolRegistry(tools.toList())

        @Provides
        @Singleton
        fun provideToolRouter(
            toolRegistry: ToolRegistry
        ): ToolRouter = ToolRouter(toolRegistry)
    }
}

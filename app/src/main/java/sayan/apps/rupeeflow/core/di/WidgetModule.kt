package sayan.apps.rupeeflow.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sayan.apps.rupeeflow.core.widget.WidgetUpdateCoordinator
import sayan.apps.rupeeflow.core.widget.WidgetUpdateCoordinatorImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    @Singleton
    abstract fun bindWidgetUpdateCoordinator(
        impl: WidgetUpdateCoordinatorImpl
    ): WidgetUpdateCoordinator
}

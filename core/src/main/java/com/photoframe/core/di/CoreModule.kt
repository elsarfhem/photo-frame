package com.photoframe.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.WorkManager
import com.photoframe.core.database.PhotoDao
import com.photoframe.core.database.PhotoDatabase
import com.photoframe.core.data.IncrementalPhotoLoader
import com.photoframe.core.data.LocalPhotoDataSource
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.data.SmbPhotoDataSource
import com.photoframe.core.logging.AppLogger
import com.photoframe.core.logging.LogExporter
import com.photoframe.core.image.ImageCache
import com.photoframe.core.observer.MediaStoreObserver
import com.photoframe.core.network.NetworkMonitor
import com.photoframe.core.reliability.CrashHandler
import com.photoframe.core.reliability.MemoryMonitor
import com.photoframe.core.telemetry.TelemetryLogger
import com.photoframe.core.repository.MultiSourcePhotoRepository
import com.photoframe.core.repository.MultiSourcePhotoRepositoryImpl
import com.photoframe.core.repository.PhotoRotationStore
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.repository.SettingsRepositoryImpl
import com.photoframe.core.repository.SlideshowRepository
import com.photoframe.core.security.CredentialStore
import com.photoframe.core.security.KeystoreCredentialStore
import com.photoframe.core.slideshow.PhotoBufferManager
import com.photoframe.core.smb.JcifsSmbClient
import com.photoframe.core.smb.SmbClient
import com.photoframe.core.source.PhotoSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): CredentialStore {
        return KeystoreCredentialStore(context)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun providePhotoDatabase(@ApplicationContext context: Context): PhotoDatabase {
        return Room.databaseBuilder(
            context,
            PhotoDatabase::class.java,
            "photo_database"
        ).build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: PhotoDatabase): PhotoDao {
        return database.photoDao()
    }

    @Provides
    @Singleton
    fun provideSmbClient(@IoDispatcher ioDispatcher: CoroutineDispatcher): SmbClient {
        return JcifsSmbClient(ioDispatcher = ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSmbPhotoDataSource(
        smbClient: SmbClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SmbPhotoDataSource {
        return SmbPhotoDataSource(smbClient, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideLocalPhotoDataSource(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): LocalPhotoDataSource {
        return LocalPhotoDataSource(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun providePhotoSourcesManager(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PhotoSourcesManager {
        return PhotoSourcesManager(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun providePhotoSourceFactory(
        smbClient: SmbClient,
        smbPhotoDataSource: SmbPhotoDataSource,
        localPhotoDataSource: LocalPhotoDataSource,
        credentialStore: CredentialStore
    ): PhotoSourceFactory {
        return PhotoSourceFactory(smbClient, smbPhotoDataSource, localPhotoDataSource, credentialStore)
    }

    @Provides
    @Singleton
    fun provideMediaStoreObserver(@ApplicationContext context: Context): MediaStoreObserver {
        return MediaStoreObserver(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideIncrementalPhotoLoader(
        smbPhotoDataSource: SmbPhotoDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): IncrementalPhotoLoader {
        return IncrementalPhotoLoader(smbPhotoDataSource, ioDispatcher)
    }

    @Provides
    @Singleton
    fun providePhotoRotationStore(
        dataStore: DataStore<Preferences>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PhotoRotationStore {
        return PhotoRotationStore(dataStore, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        credentialStore: CredentialStore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SettingsRepository {
        return SettingsRepositoryImpl(dataStore, credentialStore, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideImageCache(
        @ApplicationContext context: Context,
        smbClient: SmbClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        appLogger: AppLogger
    ): ImageCache {
        return ImageCache(context, smbClient, ioDispatcher, appLogger)
    }

    @Provides
    @Singleton
    fun providePhotoBufferManager(
        imageCache: ImageCache,
        networkMonitor: com.photoframe.core.network.NetworkMonitor,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PhotoBufferManager {
        return PhotoBufferManager(imageCache, networkMonitor, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideMultiSourcePhotoRepository(
        photoSourcesManager: PhotoSourcesManager,
        photoSourceFactory: PhotoSourceFactory,
        photoBufferManager: PhotoBufferManager,
        photoDao: PhotoDao,
        smbClient: SmbClient,
        credentialStore: CredentialStore,
        networkMonitor: com.photoframe.core.network.NetworkMonitor,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MultiSourcePhotoRepositoryImpl {
        return MultiSourcePhotoRepositoryImpl(photoSourcesManager, photoSourceFactory, photoBufferManager, photoDao, smbClient, credentialStore, networkMonitor, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSlideshowRepository(multiSourceRepo: MultiSourcePhotoRepositoryImpl): SlideshowRepository {
        return multiSourceRepo
    }

    @Provides
    @Singleton
    fun provideMultiSourceRepository(multiSourceRepo: MultiSourcePhotoRepositoryImpl): MultiSourcePhotoRepository {
        return multiSourceRepo
    }

    @Provides
    @Singleton
    fun provideMemoryMonitor(
        imageCache: ImageCache,
        photoBufferManager: PhotoBufferManager,
        telemetryLogger: TelemetryLogger,
        @DefaultDispatcher dispatcher: CoroutineDispatcher
    ): MemoryMonitor {
        return MemoryMonitor(imageCache, photoBufferManager, telemetryLogger, dispatcher)
    }

    @Provides
    @Singleton
    fun provideCrashHandler(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>,
        telemetryLogger: TelemetryLogger,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CrashHandler {
        return CrashHandler(context, dataStore, telemetryLogger, dispatcher)
    }

    @Provides
    @Singleton
    fun provideAppLogger(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AppLogger {
        return AppLogger(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideLogExporter(
        @ApplicationContext context: Context,
        appLogger: AppLogger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): LogExporter {
        return LogExporter(context, appLogger, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideTelemetryLogger(): TelemetryLogger {
        return TelemetryLogger()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

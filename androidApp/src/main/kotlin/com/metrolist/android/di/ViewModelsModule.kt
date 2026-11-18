package com.metrolist.android.di

import android.app.Application
import com.metrolist.android.R
import com.metrolist.android.data.PlayHistoryRepository
import com.metrolist.android.playback.AndroidPlayerPlatformServices
import com.metrolist.android.playback.PlayerConnection
import com.metrolist.android.viewmodels.HomeViewModel
import com.metrolist.android.viewmodels.LibraryViewModel
import com.metrolist.android.viewmodels.PlaybackViewModel
import com.metrolist.android.viewmodels.SearchViewModel
import com.metrolist.shared.api.artwork.AnimatedArtworkService
import com.metrolist.shared.api.artwork.DirectCanvasProvider
import com.metrolist.shared.viewmodels.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelsModule =
    module {
        singleOf(::PlayHistoryRepository)

        singleOf(::AnimatedArtworkService)

        viewModel {
            PlaybackViewModel(
                androidContext().applicationContext as Application,
                get(),
                get(),
            )
        }

        viewModelOf(::LibraryViewModel)
        viewModel {
            HomeViewModel(
                database = get(),
                likedSongsName = androidContext().getString(R.string.liked_songs),
            )
        }
        viewModelOf(::SearchViewModel)
        single { DirectCanvasProvider(get()) }

        single {
            PlayerViewModel(
                playerConnection = get<PlayerConnection>(),
                imageLoader = get(),
                platformContext = androidContext().applicationContext,
                platform = AndroidPlayerPlatformServices(androidContext().applicationContext, get(), get()),
                lyricsRepository = get(),
                animatedArtworkService = get(),
                musicVideoResolver = get(),
                viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            )
        }
    }

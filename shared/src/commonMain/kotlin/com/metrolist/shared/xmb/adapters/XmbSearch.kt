package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.api.YouTubeRepository
import com.metrolist.shared.model.SearchSummaryPage
import com.metrolist.shared.settings.BlockedContent
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.viewmodels.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Search for the XMB. Recent searches and suggestions come from the shared [SearchViewModel] (the
 * stock screens' controller, created on first use; history is kept in `Settings.shared`). Results
 * come straight from the repository so each opened level gets the results of its own query.
 */
class XmbSearch(
    scope: CoroutineScope,
    private val repository: () -> YouTubeRepository = { GlobalYouTubeRepository.instance },
    controllerFactory: (CoroutineScope) -> SearchViewModel = { SearchViewModel(it) },
) {
    private val controller by lazy { controllerFactory(scope) }
    private var last: Pair<String, SearchSummaryPage>? = null

    val history: Flow<List<String>> get() = controller.uiState.map { it.searchHistory }.distinctUntilChanged()

    val suggestions: Flow<List<String>>
        get() =
            controller.uiState
                .map { state ->
                    state.searchSuggestions
                        ?.queries
                        .orEmpty()
                        .take(MAX_SUGGESTIONS)
                }.distinctUntilChanged()

    /** Updates suggestions for the text typed so far. */
    fun type(text: String) = controller.onQueryChange(text)

    /** Results for [query], one folder per shelf. Records the query in the history unless paused. */
    fun results(query: String): Flow<XmbListState> =
        flow {
            val cached = last?.takeIf { it.first == query }?.second
            if (cached != null) {
                emit(cached.toListState(query))
                return@flow
            }
            emit(XmbListState())
            val record = !Settings.shared.pauseSearchHistory
            if (record) controller.addSearchHistory(query)
            val summary = repository().getSearchSummary(query, recordSearchHistory = record).withoutBlockedItems()
            last = query to summary
            emit(summary.toListState(query))
        }

    private fun SearchSummaryPage.toListState(query: String): XmbListState {
        val entries = toXmbEntries(query)
        return XmbListState(entries, isLoading = false, isError = entries.isEmpty() && onlineSearchFailed)
    }

    private companion object {
        const val MAX_SUGGESTIONS = 8
    }
}

/** A search's shelves as folders, each opening its own list. */
fun SearchSummaryPage.toXmbEntries(query: String): List<XmbEntry> =
    summaries
        .withIndex()
        .filter { (_, summary) -> summary.items.isNotEmpty() }
        .map { (index, summary) ->
            val first = summary.items.first()
            XmbEntry(
                key = "search:$query:$index",
                title = summary.title.ifBlank { first.title },
                subtitle = summary.items.take(3).joinToString(", ") { it.title },
                artwork = first.toXmbEntry().artwork,
                kind = XmbEntryKind.SECTION,
                open = XmbList.SearchSection(query, index, summary),
            )
        }

private fun SearchSummaryPage.withoutBlockedItems(): SearchSummaryPage =
    copy(
        summaries =
            summaries.mapNotNull { summary ->
                val items = BlockedContent.filterItems(summary.items)
                if (items.isEmpty()) null else summary.copy(items = items)
            },
    )

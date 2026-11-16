/*
 * Episteme Reader - A native Android document reader.
 * Copyright (C) 2026 Episteme
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * mail: epistemereader@gmail.com
 */
// LibraryScreen.kt
@file:Suppress("KotlinConstantConditions")

package com.aryan.reader

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.data.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun getBookCountString(count: Int): String {
    return pluralStringResource(id = R.plurals.book_count, count, count)
}

@Composable
private fun shelfDisplayName(shelf: Shelf): String =
    if (shelf.id == "unshelved") stringResource(R.string.group_ungrouped) else shelf.name

@UnstableApi
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
) {
    val compStart = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        ReaderPerfLog.d("LibraryScreen initial composition ${System.currentTimeMillis() - compStart}ms")
    }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenModel = remember(uiState) { uiState.toLibraryScreenModel() }
    val selectedItems = screenModel.selectedItems
    val isContextualModeActive = screenModel.isContextualModeActive
    val selectedShelves = screenModel.selectedShelves
    val isShelfContextualModeActive = screenModel.isShelfContextualModeActive
    val sortOrder = screenModel.sortOrder
    val shelves = screenModel.shelves.filter { it.type == ShelfType.MANUAL }
    val rawLibraryFiles = screenModel.rawLibraryFiles
    val tabTitles = remember {
        buildList {
            add(context.getString(R.string.tab_all_books))
            add(context.getString(R.string.tab_shelves))
            add(context.getString(R.string.tab_folders))
        }
    }
    val pagerState = rememberPagerState(
        initialPage = uiState.libraryScreenStartPage.coerceIn(0, tabTitles.lastIndex),
        pageCount = { tabTitles.size }
    )

    val containsFolderItems = screenModel.containsFolderItemsInSelection

    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    val isSearchActive = screenModel.isSearchActive
    val searchQuery = screenModel.searchQuery

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.addSyncedFolder(it)
        }
    }

    val onSelectSyncFolderClick = {
        try {
            pickFolderLauncher.launch(null)
        } catch (_: android.content.ActivityNotFoundException) {
            viewModel.showBanner(context.getString(R.string.error_folder_selection_unsupported), isError = true)
        }
    }

    val pickFileLauncher = rememberFilePickerLauncher { uris ->
        if (isContextualModeActive) {
            viewModel.clearContextualAction()
        }
        viewModel.onFilesSelected(uris)
    }

    val fallbackFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (isContextualModeActive) {
            viewModel.clearContextualAction()
        }
        viewModel.onFilesSelected(uris)
    }

    val onSelectFileClick = {
        if (isContextualModeActive) {
            viewModel.clearContextualAction()
        }
        val mimeTypes = if (uiState.useStrictFileFilter) MainViewModel.SUPPORTED_MIME_TYPES else arrayOf("*/*")
        try {
            pickFileLauncher.launch(mimeTypes)
        } catch (_: android.content.ActivityNotFoundException) {
            Timber.w("OpenDocument picker failed. Falling back to GetMultipleContents.")
            try {
                fallbackFilePickerLauncher.launch("*/*")
            } catch (_: android.content.ActivityNotFoundException) {
                viewModel.showBanner(context.getString(R.string.error_no_file_manager), isError = true)
            }
        }
    }

    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }
            .drop(1)
            .distinctUntilChanged()
            .collect { page ->
                viewModel.setLibraryScreenPage(page)
            }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteShelvesDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var itemForInfoDialog by remember { mutableStateOf<RecentFileItem?>(null) }

    BackHandler(enabled = isContextualModeActive) {
        viewModel.clearContextualAction()
    }

    BackHandler(enabled = isShelfContextualModeActive) {
        viewModel.clearShelfContextualAction()
    }

    BackHandler(enabled = isSearchActive) {
        viewModel.setSearchActive(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LibraryScreenContent(
            tabTitles = tabTitles,
            recentFiles = uiState.allRecentFiles,
            rawLibraryFiles = rawLibraryFiles,
            shelves = shelves,
            selectedItems = selectedItems,
            selectedShelves = selectedShelves,
            sortOrder = sortOrder,
            libraryFilters = uiState.libraryFilters,
            allTags = uiState.allTags,
            pinnedLibraryBookIds = uiState.pinnedLibraryBookIds,
            pagerState = pagerState,
            scope = scope,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearchActiveChange = viewModel::setSearchActive,
            onSortOrderChange = viewModel::setSortOrder,
            onFilterClick = { showFilterSheet = true },
            onClearFilters = { viewModel.updateLibraryFilters(LibraryFilters()) },
            onRemoveFilter = { viewModel.updateLibraryFilters(it) },
            onTagClick = { viewModel.openTagSelection(selectedItems.map { it.bookId }.toSet()) },
            onGroupClick = { viewModel.openGroupSelection(selectedItems.map { it.bookId }.toSet()) },
            onPinClick = { viewModel.togglePinForContextualItems(isHome = false) },
            onClearSelection = { viewModel.clearContextualAction() },
            onItemClick = viewModel::onRecentFileClicked,
            onItemLongClick = viewModel::onRecentItemLongPress,
            onInfoClick = {
                if (selectedItems.size == 1) {
                    itemForInfoDialog = selectedItems.first()
                    showInfoDialog = true
                }
            },
            onDeleteClick = { showDeleteConfirmDialog = true },
            onSelectAllClick = { viewModel.selectAllLibraryFiles() },
            onShelfClick = viewModel::onShelfClick,
            onShelfLongClick = viewModel::onShelfLongPress,
            onClearShelfSelection = viewModel::clearShelfContextualAction,
            onDeleteShelves = { showDeleteShelvesDialog = true },
            onNewShelfClick = viewModel::showCreateShelfDialog,
            onSelectFileClick = onSelectFileClick,
            onScanNowClick = viewModel::scanSyncedFolder,
            onSyncMetadataClick = viewModel::syncFolderMetadata,
            onSelectSyncFolderClick = onSelectSyncFolderClick,
            onEditFolderFiltersClick = { folder, filters -> viewModel.updateFolderFilters(folder, filters) },
            syncedFolders = uiState.syncedFolders,
            onRemoveFolderClick = { folder -> viewModel.removeSyncedFolder(folder) },
            onDisconnectSyncFolderClick = viewModel::disconnectAllSyncedFolders,
            downloadingBookIds = uiState.downloadingBookIds,
            lastFolderScanTime = uiState.lastFolderScanTime,
            isLoading = uiState.isLoading,
            isRefreshing = uiState.isRefreshing
        )


        if (uiState.showCreateShelfDialog) {
            CreateShelfDialog(
                onConfirm = viewModel::createShelf,
                onDismiss = viewModel::dismissCreateShelfDialog,
                existingNames = shelves.filter { it.id != "unshelved" }.map { it.name } + stringResource(R.string.group_ungrouped),
                isSaving = uiState.isSavingGroup
            )
        }

        if (showDeleteConfirmDialog) {
            DeleteConfirmationDialog(
                count = selectedItems.size,
                onConfirm = {
                    viewModel.deleteContextualItemsPermanently()
                    showDeleteConfirmDialog = false
                },
                onDismiss = { showDeleteConfirmDialog = false },
                isPermanentDelete = true,
                containsFolderItems = containsFolderItems
            )
        }

        if (showFilterSheet) {
            LibraryFilterSheet(
                filters = uiState.libraryFilters,
                allTags = uiState.allTags,
                syncedFolders = uiState.syncedFolders,
                onApply = { viewModel.updateLibraryFilters(it) },
                onDismiss = { showFilterSheet = false }
            )
        }

        if (showDeleteShelvesDialog) {
            DeleteShelvesConfirmationDialog(
                count = selectedShelves.size,
                onConfirm = {
                    viewModel.deleteSelectedShelves()
                    showDeleteShelvesDialog = false
                },
                onDismiss = { showDeleteShelvesDialog = false }
            )
        }

        itemForInfoDialog?.let { item ->
            if (showInfoDialog) {
                FileInfoDialog(
                    item = item,
                    onDismiss = {
                        showInfoDialog = false
                        itemForInfoDialog = null
                    },
                    onUpdateName = { newName ->
                        viewModel.updateCustomName(item.bookId, newName)
                    },
                    onOpenTags = { viewModel.openTagSelection(setOf(item.bookId)) }
                )
            }
        }
        CustomTopBanner(bannerMessage = uiState.bannerMessage)
    }
}

@UnstableApi
@Composable
fun ShelfScreen(
    viewModel: MainViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedItems = uiState.contextualActionItems
    val viewingShelfId = uiState.viewingShelfId
    val isAddingBooks = uiState.isAddingBooksToShelf
    val shelves = uiState.shelves
    val sortOrder = uiState.sortOrder
    val showRenameDialogFor = uiState.showRenameShelfDialogFor
    val showDeleteDialogFor = uiState.showDeleteShelfDialogFor

    var showRemoveFromShelfDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var itemForInfoDialog by remember { mutableStateOf<RecentFileItem?>(null) }

    BackHandler(enabled = true) {
        when {
            selectedItems.isNotEmpty() -> viewModel.clearContextualAction()
            isAddingBooks -> viewModel.dismissAddBooksToShelf()
            else -> viewModel.navigateBackFromShelf()
        }
    }

    val currentShelf = shelves.find { it.id == viewingShelfId }
    val childShelves = remember(shelves, currentShelf) {
        currentShelf?.childShelfIds?.mapNotNull { childId -> shelves.find { it.id == childId } } ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewingShelfId != null && currentShelf != null) {
            if (isAddingBooks) {
                AddBooksModeScreen(
                    shelfName = currentShelf.name,
                    availableBooks = uiState.booksAvailableForAdding,
                    selectedBookUris = uiState.booksSelectedForAdding,
                    currentSource = uiState.addBooksSource,
                    sortOrder = sortOrder,
                    onSortOrderChange = viewModel::setSortOrder,
                    onSourceChange = viewModel::setAddBooksSource,
                    onBookClick = { item -> viewModel.toggleBookSelectionForAdding(item.bookId) },
                    onBack = viewModel::dismissAddBooksToShelf,
                    onAddSelectedBooks = { viewModel.addBooksToShelf(viewingShelfId) },
                    downloadingBookIds = uiState.downloadingBookIds
                )
            } else {
                ShelfDetailScreen(
                    shelf = currentShelf,
                    childShelves = childShelves,
                    selectedItems = selectedItems,
                    sortOrder = sortOrder,
                    onSortOrderChange = viewModel::setSortOrder,
                    onBack = viewModel::navigateBackFromShelf,
                    onAddBooksClick = viewModel::showAddBooksToShelf,
                    onChildShelfClick = viewModel::onShelfClick,
                    onBookClick = viewModel::onRecentFileClicked,
                    onBookLongClick = viewModel::onRecentItemLongPress,
                    onClearSelection = viewModel::clearContextualAction,
                    onTagClick = { viewModel.openTagSelection(selectedItems.map { it.bookId }.toSet()) },
                    onGroupClick = { viewModel.openGroupSelection(selectedItems.map { it.bookId }.toSet()) },
                    onInfoClick = {
                        if (selectedItems.size == 1) {
                            itemForInfoDialog = selectedItems.first()
                            showInfoDialog = true
                        }
                    },
                    onDeleteClick = { showRemoveFromShelfDialog = true },
                    onRenameShelf = { viewModel.showRenameShelfDialog(currentShelf.id) },
                    onDeleteShelf = { viewModel.showDeleteShelfDialog(currentShelf.id) },
                    downloadingBookIds = uiState.downloadingBookIds
                )
            }
        }

        if (showRenameDialogFor != null) {
            val shelfToRename = shelves.find { it.id == showRenameDialogFor }
            if (shelfToRename != null) {
                RenameShelfDialog(
                    initialName = shelfToRename.name,
                    onConfirm = { newName -> viewModel.renameShelf(showRenameDialogFor, newName) },
                    onDismiss = viewModel::dismissRenameShelfDialog
                )
            }
        }

        if (showDeleteDialogFor != null) {
            DeleteShelfConfirmationDialog(
                shelfName = shelves.find { it.id == showDeleteDialogFor }?.name ?: "",
                onConfirm = { viewModel.deleteShelf(showDeleteDialogFor) },
                onDismiss = viewModel::dismissDeleteShelfDialog
            )
        }

        if (showRemoveFromShelfDialog) {
            RemoveFromShelfConfirmationDialog(
                count = selectedItems.size,
                shelfName = currentShelf?.name ?: "",
                onConfirm = {
                    viewModel.removeContextualItemsFromShelf()
                    showRemoveFromShelfDialog = false
                },
                onDismiss = { showRemoveFromShelfDialog = false }
            )
        }

        itemForInfoDialog?.let { item ->
            if (showInfoDialog) {
                FileInfoDialog(
                    item = item,
                    onDismiss = { showInfoDialog = false; itemForInfoDialog = null },
                    onUpdateName = { newName -> viewModel.updateCustomName(item.bookId, newName) },
                    onOpenTags = { viewModel.openTagSelection(setOf(item.bookId)) }
                )
            }
        }
        CustomTopBanner(bannerMessage = uiState.bannerMessage)
    }
}

@Suppress("unused")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreenContent(
    tabTitles: List<String>,
    recentFiles: List<RecentFileItem>,
    rawLibraryFiles: List<RecentFileItem>,
    shelves: List<Shelf>,
    selectedItems: Set<RecentFileItem>,
    selectedShelves: Set<String>,
    sortOrder: SortOrder,
    libraryFilters: LibraryFilters,
    allTags: List<TagEntity>,
    pinnedLibraryBookIds: Set<String>,
    pagerState: PagerState,
    scope: CoroutineScope,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onFilterClick: () -> Unit,
    onClearFilters: () -> Unit,
    onRemoveFilter: (LibraryFilters) -> Unit,
    onTagClick: () -> Unit,
    onGroupClick: () -> Unit,
    onPinClick: () -> Unit,
    onClearSelection: () -> Unit,
    onItemClick: (RecentFileItem) -> Unit,
    onItemLongClick: (RecentFileItem) -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onShelfClick: (Shelf) -> Unit,
    onShelfLongClick: (Shelf) -> Unit,
    onClearShelfSelection: () -> Unit,
    onDeleteShelves: () -> Unit,
    onNewShelfClick: () -> Unit,
    onSelectFileClick: () -> Unit,
    onScanNowClick: () -> Unit,
    onSyncMetadataClick: () -> Unit,
    onSelectSyncFolderClick: () -> Unit,
    onEditFolderFiltersClick: (SyncedFolder, Set<FileType>) -> Unit,
    onDisconnectSyncFolderClick: () -> Unit,
    downloadingBookIds: Set<String>,
    lastFolderScanTime: Long?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    syncedFolders: List<SyncedFolder>,
    onRemoveFolderClick: (SyncedFolder) -> Unit,

) {
    val isBookContextualModeActive = selectedItems.isNotEmpty()
    val isShelfContextualModeActive = selectedShelves.isNotEmpty()
    var showSortMenu by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val selectedBookIds = remember(selectedItems) { selectedItems.mapTo(mutableSetOf()) { it.bookId } }

    var textFieldValue by remember(isSearchActive) {
        mutableStateOf(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
    }

    LaunchedEffect(searchQuery) {
        if (textFieldValue.text != searchQuery) {
            textFieldValue = textFieldValue.copy(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                if (isBookContextualModeActive) {
                    ContextualTopAppBar(
                        selectedItemCount = selectedItems.size,
                        onNavIconClick = onClearSelection,
                        onTagClick = onTagClick,
                        onGroupClick = onGroupClick,
                        onPinClick = onPinClick,
                        onInfoClick = onInfoClick,
                        onDeleteClick = onDeleteClick,
                        onSelectAllClick = onSelectAllClick
                    )
                } else if (isShelfContextualModeActive && pagerState.currentPage == 1) {
                    ContextualTopAppBar(
                        selectedItemCount = selectedShelves.size,
                        onNavIconClick = onClearShelfSelection,
                        onDeleteClick = onDeleteShelves
                    )
                } else if (isSearchActive) {
                    Surface(
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onSearchActiveChange(false) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_close_search))
                            }
                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = {
                                    textFieldValue = it
                                    onSearchQueryChange(it.text)
                                },
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                                    .focusRequester(searchFocusRequester),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_clear_query))
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    CustomTopAppBar(title = { Text(stringResource(R.string.library_title)) },
                        actions = {
                            if (pagerState.currentPage == 0) {
                                IconButton(onClick = onFilterClick) {
                                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.content_desc_filter))
                                }
                                Box {
                                    TextButton(onClick = { showSortMenu = true }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.sort),
                                            contentDescription = stringResource(R.string.content_desc_sort),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(sortOrder.labelRes))
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        SortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(order.labelRes)) },
                                                onClick = {
                                                    onSortOrderChange(order)
                                                    showSortMenu = false
                                                },
                                                trailingIcon = {
                                                    if (order == sortOrder) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = stringResource(R.string.content_desc_selected)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { onSearchActiveChange(true) }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                                }
                            }
                        }
                    )
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    ReaderPerfLog.d("LibraryPager click page=$index title=$title")
                                    if (pagerState.currentPage != index) {
                                        scope.launch {
                                            val start = ReaderPerfLog.nowNanos()
                                            pagerState.animateScrollToPage(index)
                                            ReaderPerfLog.d(
                                                "LibraryPager settled page=$index elapsed=${ReaderPerfLog.elapsedMs(start)}ms"
                                            )
                                        }
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = libraryFilters.isActive && pagerState.currentPage == 0
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (libraryFilters.fileTypes.isNotEmpty()) {
                                AssistChip(
                                    onClick = { onRemoveFilter(libraryFilters.copy(fileTypes = emptySet())) },
                                    label = { Text(stringResource(R.string.filter_types, libraryFilters.fileTypes.joinToString { it.name })) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), modifier = Modifier.size(16.dp)) }
                                )
                            }
                            if (libraryFilters.sourceFolders.isNotEmpty()) {
                                AssistChip(
                                    onClick = { onRemoveFilter(libraryFilters.copy(sourceFolders = emptySet())) },
                                    label = { Text(stringResource(R.string.filter_folders, libraryFilters.sourceFolders.size)) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), modifier = Modifier.size(16.dp)) }
                                )
                            }
                            if (libraryFilters.readStatus != ReadStatusFilter.ALL) {
                                AssistChip(
                                    onClick = { onRemoveFilter(libraryFilters.copy(readStatus = ReadStatusFilter.ALL)) },
                                    label = { Text(stringResource(R.string.filter_status, stringResource(libraryFilters.readStatus.labelRes))) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), modifier = Modifier.size(16.dp)) }
                                )
                            }
                            if (libraryFilters.tagIds.isNotEmpty()) {
                                val selectedTags = allTags.filter { it.id in libraryFilters.tagIds }
                                val tagLabel = when {
                                    selectedTags.isEmpty() -> pluralStringResource(R.plurals.tag_count, libraryFilters.tagIds.size, libraryFilters.tagIds.size)
                                    selectedTags.size <= 2 -> selectedTags.joinToString { it.name }
                                    else -> pluralStringResource(R.plurals.tag_count, selectedTags.size, selectedTags.size)
                                }
                                AssistChip(
                                    onClick = { onRemoveFilter(libraryFilters.copy(tagIds = emptySet())) },
                                    label = { Text(stringResource(R.string.filter_tags, tagLabel)) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isBookContextualModeActive && !isShelfContextualModeActive) {
                when (pagerState.currentPage) {
                    0 -> {
                        if (recentFiles.isNotEmpty()) {
                            ExtendedFloatingActionButton(
                                text = { Text(stringResource(R.string.fab_add_file)) },
                                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_add_file)) },
                                onClick = onSelectFileClick,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    1 -> {
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(R.string.fab_new_shelf)) },
                            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_new_shelf)) },
                            onClick = onNewShelfClick,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.25f
            ),
            beyondViewportPageCount = 0,
            key = { it }
        ) { page ->
            when (page) {
                0 -> {
                    if (recentFiles.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_results_found, searchQuery))
                        }
                    } else if (recentFiles.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.your_library_empty),
                            message = stringResource(R.string.library_empty_desc),
                            onSelectFileClick = onSelectFileClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentFiles, key = { it.bookId }) { item ->
                                LibraryListItem(
                                    item = item,
                                    isSelected = item.bookId in selectedBookIds,
                                    isPinned = item.bookId in pinnedLibraryBookIds,
                                    onItemClick = { onItemClick(item) },
                                    onItemLongClick = { onItemLongClick(item) },
                                    isDownloading = item.bookId in downloadingBookIds
                                )
                            }
                        }
                    }
                }
                1 -> {
                    ShelvesScreen(
                        shelves = shelves,
                        onShelfClick = onShelfClick,
                        onShelfLongClick = onShelfLongClick,
                        selectedShelves = selectedShelves
                    )
                }
                2 -> {
                    FolderSyncScreen(
                        syncedFolders = syncedFolders,
                        allRecentFiles = rawLibraryFiles,
                        onAddFolderClick = onSelectSyncFolderClick,
                        onRemoveFolderClick = onRemoveFolderClick,
                        onEditFolderFiltersClick = onEditFolderFiltersClick,
                        onScanNowClick = onScanNowClick,
                        onSyncMetadataClick = onSyncMetadataClick,
                        isLoading = isLoading || isRefreshing
                    )
                }

            }
        }
    }
}

@Composable
private fun ShelvesScreen(
    shelves: List<Shelf>,
    onShelfClick: (Shelf) -> Unit,
    onShelfLongClick: (Shelf) -> Unit,
    selectedShelves: Set<String>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shelves, key = { it.id }) { shelf ->
            ShelfListItem(
                shelf = shelf,
                isSelected = shelf.id in selectedShelves,
                onItemClick = { onShelfClick(shelf) },
                onItemLongClick = { onShelfLongClick(shelf) }
            )
        }
    }
}

@Composable
private fun CreateShelfDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    existingNames: List<String>,
    isSaving: Boolean
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val duplicate = existingNames.any { it.equals(text.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.create_new_shelf)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.shelf_name_hint)) },
                singleLine = true,
                enabled = !isSaving,
                isError = duplicate,
                supportingText = { if (duplicate) Text(stringResource(R.string.error_shelf_exists)) },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank() && !duplicate && !isSaving
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
}

@Composable
private fun ShelfDetailScreen(
    shelf: Shelf,
    childShelves: List<Shelf>,
    selectedItems: Set<RecentFileItem>,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onBack: () -> Unit,
    onAddBooksClick: () -> Unit,
    onChildShelfClick: (Shelf) -> Unit,
    onBookClick: (RecentFileItem) -> Unit,
    onBookLongClick: (RecentFileItem) -> Unit,
    onClearSelection: () -> Unit,
    onTagClick: () -> Unit,
    onGroupClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameShelf: () -> Unit,
    onDeleteShelf: () -> Unit,
    downloadingBookIds: Set<String>,
) {
    val isContextualModeActive = selectedItems.isNotEmpty()
    val isFolderShelf = shelf.type == ShelfType.FOLDER
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember(shelf.id) { mutableStateOf(false) }
    var searchQuery by remember(shelf.id) { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var searchFieldValue by remember(isSearchActive, shelf.id) {
        mutableStateOf(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
    }
    val normalizedQuery = searchQuery.trim()
    val filteredChildShelves = remember(childShelves, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            childShelves
        } else {
            childShelves.filter { childShelf ->
                childShelf.name.contains(normalizedQuery, ignoreCase = true) ||
                    childShelf.books.any { item ->
                        item.displayName.contains(normalizedQuery, ignoreCase = true) ||
                            item.title?.contains(normalizedQuery, ignoreCase = true) == true ||
                            item.author?.contains(normalizedQuery, ignoreCase = true) == true
                    }
            }
        }
    }
    val filteredDirectBooks = remember(shelf.directBooks, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            shelf.directBooks
        } else {
            shelf.directBooks.filter { item ->
                item.displayName.contains(normalizedQuery, ignoreCase = true) ||
                    item.title?.contains(normalizedQuery, ignoreCase = true) == true ||
                    item.author?.contains(normalizedQuery, ignoreCase = true) == true ||
                    item.tags.any { tag -> tag.name.contains(normalizedQuery, ignoreCase = true) }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchFieldValue.text != searchQuery) {
            searchFieldValue = searchFieldValue.copy(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    fun clearShelfSearchQuery() {
        searchQuery = ""
        searchFieldValue = TextFieldValue("", TextRange.Zero)
    }

    fun closeShelfSearch() {
        isSearchActive = false
        clearShelfSearchQuery()
    }

    BackHandler(enabled = isSearchActive) {
        closeShelfSearch()
    }

    Scaffold(
        modifier = Modifier,
        containerColor = Color.Transparent,
        topBar = {
            if (isContextualModeActive) {
                ContextualTopAppBar(
                    selectedItemCount = selectedItems.size,
                    onNavIconClick = onClearSelection,
                    onTagClick = onTagClick,
                    onGroupClick = onGroupClick,
                    onInfoClick = onInfoClick,
                    onDeleteClick = onDeleteClick.takeIf { shelf.type == ShelfType.MANUAL && shelf.id != "unshelved" }
                )
            } else if (isSearchActive) {
                Surface(
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { closeShelfSearch() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_close_search))
                        }
                        OutlinedTextField(
                            value = searchFieldValue,
                            onValueChange = {
                                searchFieldValue = it
                                searchQuery = it.text
                            },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .focusRequester(searchFocusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { clearShelfSearchQuery() }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_clear_query))
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                CustomTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = shelfDisplayName(shelf),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isFolderShelf && shelf.childShelfCount > 0 && shelf.directBookCount > 0 ->
                                        "${pluralStringResource(R.plurals.folder_count, shelf.childShelfCount, shelf.childShelfCount)} • ${getBookCountString(shelf.directBookCount)}"
                                    isFolderShelf && shelf.childShelfCount > 0 ->
                                        pluralStringResource(R.plurals.folder_count, shelf.childShelfCount, shelf.childShelfCount)
                                    isFolderShelf -> getBookCountString(shelf.directBookCount)
                                    else -> getBookCountString(shelf.bookCount)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.sort),
                                    contentDescription = stringResource(R.string.content_desc_sort),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(sortOrder.labelRes))
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(order.labelRes)) },
                                        onClick = {
                                            onSortOrderChange(order)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (order == sortOrder) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.content_desc_selected)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.content_desc_search_shelf)
                            )
                        }

                        if (shelf.type == ShelfType.MANUAL && shelf.id != "unshelved") {
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.content_desc_more_options)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_rename_shelf)) },
                                        onClick = {
                                            onRenameShelf()
                                            showMoreMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_delete_shelf)) },
                                        onClick = {
                                            onDeleteShelf()
                                            showMoreMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (shelf.type == ShelfType.MANUAL && shelf.id != "unshelved" && !isContextualModeActive) {
                ExtendedFloatingActionButton(
                    onClick = onAddBooksClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_add_books)) }
                )
            }
        },
        content = { paddingValues ->
            if (filteredChildShelves.isEmpty() && filteredDirectBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (normalizedQuery.isBlank()) stringResource(R.string.shelf_empty) else stringResource(
                            R.string.no_results_found,
                            normalizedQuery
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredChildShelves.isNotEmpty()) {
                        if (isFolderShelf) {
                            item {
                                Text(
                                    text = stringResource(R.string.section_folders),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(filteredChildShelves, key = { it.id }) { childShelf ->
                            ShelfListItem(
                                shelf = childShelf,
                                isSelected = false,
                                onItemClick = { onChildShelfClick(childShelf) },
                                onItemLongClick = {},
                                showHierarchyIndent = false
                            )
                        }
                    }
                    if (filteredDirectBooks.isNotEmpty() && isFolderShelf && filteredChildShelves.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        item {
                            Text(
                                text = stringResource(R.string.section_files),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(filteredDirectBooks, key = { it.bookId }) { item ->
                        LibraryListItem(
                            item = item,
                            isSelected = selectedItems.any { it.bookId == item.bookId },
                            onItemClick = { onBookClick(item) },
                            onItemLongClick = { onBookLongClick(item) },
                            isDownloading = item.bookId in downloadingBookIds
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AddBooksModeScreen(
    shelfName: String,
    availableBooks: List<RecentFileItem>,
    selectedBookUris: Set<String>,
    currentSource: AddBooksSource,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onSourceChange: (AddBooksSource) -> Unit,
    onBookClick: (RecentFileItem) -> Unit,
    onBack: () -> Unit,
    onAddSelectedBooks: () -> Unit,
    downloadingBookIds: Set<String>,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier,
        containerColor = Color.Transparent,
        topBar = {
            Column {
                CustomTopAppBar(
                    title = { Text(stringResource(R.string.add_to_shelf, shelfName)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.sort),
                                    contentDescription = stringResource(R.string.content_desc_sort),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(sortOrder.labelRes))
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(order.labelRes)) },
                                        onClick = {
                                            onSortOrderChange(order)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (order == sortOrder) {
                                                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.content_desc_selected))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AddBooksSource.entries.forEach { source ->
                        FilterChip(
                            selected = source == currentSource,
                            onClick = { onSourceChange(source) },
                            label = { Text(stringResource(source.labelRes)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedBookUris.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.fab_add_count, selectedBookUris.size)) },
                    icon = { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.fab_add_books)) },
                    onClick = onAddSelectedBooks
                )
            }
        },
        content = { paddingValues ->
            if (availableBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentSource == AddBooksSource.UNSHELVED) {
                            stringResource(R.string.no_unshelved_books)
                        } else {
                            stringResource(R.string.all_books_in_shelf)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableBooks, key = { it.bookId }) { item ->
                        val isSelected = item.bookId in selectedBookUris
                        LibraryListItem(
                            item = item,
                            isSelected = isSelected,
                            onItemClick = { onBookClick(item) },
                            onItemLongClick = { onBookClick(item) },
                            isDownloading = item.bookId in downloadingBookIds
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ShelfCover(shelf: Shelf) {
    val context = LocalContext.current
    val placeholder = R.drawable.epub_placeholder
    val booksForCovers = shelf.books.take(4).reversed()
    val coverWidth = 52.dp
    val coverHeight = 75.dp
    val horizontalOffset = 12.dp
    val maxWidth = coverWidth + (horizontalOffset * (4 - 1))

    Box(
        modifier = Modifier
            .width(maxWidth)
            .height(coverHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        if (booksForCovers.size <= 1) {
            val imageModel = remember(shelf.topBook?.coverImagePath) {
                shelf.topBook?.coverImagePath?.let { File(it) } ?: placeholder
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .error(placeholder)
                    .fallback(placeholder)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.content_desc_shelf_cover, shelf.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = coverWidth, height = coverHeight)
                    .clip(MaterialTheme.shapes.small)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(coverWidth + (horizontalOffset * (booksForCovers.size - 1)))
                    .height(coverHeight)
            ) {
                booksForCovers.forEachIndexed { index, book ->
                    val imageModel = remember(book.coverImagePath) {
                        book.coverImagePath?.let { File(it) } ?: placeholder
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(width = coverWidth, height = coverHeight)
                            .align(Alignment.CenterEnd)
                            .offset(x = -horizontalOffset * index)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .error(placeholder)
                                .fallback(placeholder)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfListItem(
    shelf: Shelf,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    showHierarchyIndent: Boolean = true,
) {
    val folderIndent = if (showHierarchyIndent && shelf.type == ShelfType.FOLDER) (shelf.depth * 14).dp else 0.dp

    androidx.compose.material3.ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                else Modifier
            )
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = {
                    if (shelf.id != "unshelved") {
                        onItemLongClick()
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp + folderIndent, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShelfCover(shelf = shelf)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (shelf.type) {
                        ShelfType.SMART -> Icons.Default.Star
                        ShelfType.TAG -> Icons.AutoMirrored.Filled.LibraryBooks
                        ShelfType.FOLDER -> Icons.Default.Folder
                        ShelfType.SERIES -> Icons.AutoMirrored.Filled.LibraryBooks
                        ShelfType.MANUAL -> Icons.Default.Folder
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shelfDisplayName(shelf),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getBookCountString(shelf.bookCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryListItem(
    item: RecentFileItem,
    isSelected: Boolean,
    isPinned: Boolean = false,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    isDownloading: Boolean,
) {
    val context = LocalContext.current
    val placeholder = when (item.type) {
        FileType.PDF -> R.drawable.pdf_placeholder
        FileType.EPUB, FileType.MOBI, FileType.FB2, FileType.MD, FileType.TXT, FileType.HTML, FileType.CBZ, FileType.CBR, FileType.CB7, FileType.DOCX, FileType.ODT, FileType.FODT -> R.drawable.epub_placeholder
    }
    val imageModel = remember(item.coverImagePath) {
        item.coverImagePath?.let { File(it) } ?: placeholder
    }

    androidx.compose.material3.ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (item.isAvailable) 1.0f else 0.8f }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                else Modifier
            )
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .height(132.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.7f)
                    .clip(MaterialTheme.shapes.medium)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .error(placeholder)
                        .fallback(placeholder)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.content_desc_selected), modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(6.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.cardTitle(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.cardAuthor(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            minLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.sourceFolderUri != null || item.isOpdsStream() || isPinned) {
                        FileStatusBadges(
                            item = item,
                            isPinned = isPinned
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileTypeBadge(type = item.type, overlay = false)

                    if (item.tags.isNotEmpty()) {
                        BookTagChipsRow(
                            tags = item.tags,
                            compact = true,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (!item.isAvailable) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isDownloading) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            contentColor = if (isDownloading) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = stringResource(R.string.not_available_locally),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (isDownloading) {
                                        stringResource(R.string.status_downloading)
                                    } else {
                                        stringResource(R.string.not_available_locally)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                ReadingProgressSection(
                    progressPercentage = item.progressPercentage,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RenameShelfDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(initialName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_rename_shelf)) },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                placeholder = { Text(stringResource(R.string.shelf_name_hint)) },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(textFieldValue.text) },
                enabled = textFieldValue.text.isNotBlank() && textFieldValue.text != initialName
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
}

@Composable
private fun DeleteShelfConfirmationDialog(
    shelfName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_shelf)) },
        text = { Text(stringResource(R.string.dialog_delete_shelf_desc, shelfName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun RemoveFromShelfConfirmationDialog(
    count: Int,
    shelfName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_remove_from_shelf)) },
        text = { Text(pluralStringResource(R.plurals.dialog_remove_from_shelf_desc, count, count, shelfName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_remove)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun DeleteShelvesConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val shelfStr = pluralStringResource(id = R.plurals.shelf_count, count, count)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_shelves, shelfStr)) },
        text = { Text(stringResource(R.string.dialog_delete_shelves_desc, count, shelfStr)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun FolderSyncScreen(
    syncedFolders: List<SyncedFolder>,
    allRecentFiles: List<RecentFileItem>,
    onAddFolderClick: () -> Unit,
    onRemoveFolderClick: (SyncedFolder) -> Unit,
    onEditFolderFiltersClick: (SyncedFolder, Set<FileType>) -> Unit,
    onScanNowClick: () -> Unit,
    onSyncMetadataClick: () -> Unit,
    isLoading: Boolean
) {
    var editingFolder by remember { mutableStateOf<SyncedFolder?>(null) }
    val folderStatsByUri = remember(allRecentFiles) {
        allRecentFiles
            .asSequence()
            .filter { it.sourceFolderUri != null }
            .groupBy { it.sourceFolderUri!! }
            .mapValues { (_, files) ->
                FolderFileStats(
                    totalBooks = files.size,
                    countsByType = files.groupingBy { it.type }.eachCount()
                )
            }
    }

    Scaffold(
        floatingActionButton = {
            if (syncedFolders.size < 10) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.fab_add_folder)) },
                    icon = { Icon(Icons.Default.Add, "Add") },
                    onClick = onAddFolderClick
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (syncedFolders.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onScanNowClick,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLoading) stringResource(R.string.scanning) else stringResource(R.string.scan_all))
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = onSyncMetadataClick,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(painterResource(id = R.drawable.sync), null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sync_meta))
                    }
                }
            } else {
                EmptyState(
                    title = stringResource(R.string.sync_local_folders),
                    message = stringResource(R.string.sync_folders_desc),
                    onSelectFileClick = onAddFolderClick,
                    primaryButtonText = stringResource(R.string.action_select_folder),
                    modifier = Modifier.fillMaxSize()
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(syncedFolders, key = { it.uriString }) { folder ->
                    FolderCard(
                        folder = folder,
                        stats = folderStatsByUri[folder.uriString] ?: FolderFileStats.Empty,
                        onRemoveClick = onRemoveFolderClick,
                        onEditFiltersClick = { editingFolder = folder }
                    )
                }
            }
        }
    }

    editingFolder?.let { folder ->
        EditFolderFiltersDialog(
            folder = folder,
            onConfirm = { newFilters ->
                onEditFolderFiltersClick(folder, newFilters)
                editingFolder = null
            },
            onDismiss = { editingFolder = null }
        )
    }
}

private data class FolderFileStats(
    val totalBooks: Int,
    val countsByType: Map<FileType, Int>
) {
    companion object {
        val Empty = FolderFileStats(totalBooks = 0, countsByType = emptyMap())
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FolderCard(
    folder: SyncedFolder,
    stats: FolderFileStats,
    onRemoveClick: (SyncedFolder) -> Unit,
    onEditFiltersClick: (SyncedFolder) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val lastScanText = if (folder.lastScanTime == 0L) stringResource(R.string.never) else dateFormat.format(Date(folder.lastScanTime))

    androidx.compose.material3.ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit_filters)) },
                            onClick = {
                                showMenu = false
                                onEditFiltersClick(folder)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_remove_folder)) },
                            onClick = {
                                showMenu = false
                                onRemoveClick(folder)
                            },
                            colors = androidx.compose.material3.MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.last_sync),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = lastScanText, style = MaterialTheme.typography.bodySmall)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.books_count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = stats.totalBooks.toString(), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (stats.countsByType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stats.countsByType.forEach { (type, count) ->
                        AssistChip(
                            onClick = { },
                            label = { Text(stringResource(R.string.folder_filter_count, type.name, count)) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditFolderFiltersDialog(
    folder: SyncedFolder,
    onConfirm: (Set<FileType>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTypes by remember { mutableStateOf(folder.allowedFileTypes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.filter_file_types),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.filter_file_types_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileType.entries.forEach { type ->
                        val isSelected = type in selectedTypes
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTypes = if (isSelected) {
                                    selectedTypes - type
                                } else {
                                    selectedTypes + type
                                }
                            },
                            label = {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(selectedTypes) },
                enabled = selectedTypes.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterSheet(
    filters: LibraryFilters,
    allTags: List<TagEntity>,
    syncedFolders: List<SyncedFolder>,
    onApply: (LibraryFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentFilters by remember { mutableStateOf(filters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.filter_library), style = MaterialTheme.typography.titleLarge)

            Text(stringResource(R.string.filter_file_type), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileType.entries.forEach { type ->
                    FilterChip(
                        selected = type in currentFilters.fileTypes,
                        onClick = {
                            val newSet = if (type in currentFilters.fileTypes) currentFilters.fileTypes - type else currentFilters.fileTypes + type
                            currentFilters = currentFilters.copy(fileTypes = newSet)
                        },
                        label = { Text(type.name) }
                    )
                }
            }

            Text(stringResource(R.string.filter_source_folder), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = "IN_APP_STORAGE" in currentFilters.sourceFolders,
                    onClick = {
                        val newSet = if ("IN_APP_STORAGE" in currentFilters.sourceFolders) currentFilters.sourceFolders - "IN_APP_STORAGE" else currentFilters.sourceFolders + "IN_APP_STORAGE"
                        currentFilters = currentFilters.copy(sourceFolders = newSet)
                              },
                    label = { Text(stringResource(R.string.filter_in_app_storage)) }
                )
                syncedFolders.forEach { folder ->
                    FilterChip(
                        selected = folder.uriString in currentFilters.sourceFolders,
                        onClick = {
                            val newSet = if (folder.uriString in currentFilters.sourceFolders) currentFilters.sourceFolders - folder.uriString else currentFilters.sourceFolders + folder.uriString
                            currentFilters = currentFilters.copy(sourceFolders = newSet)
                                  },
                        label = { Text(folder.name) }
                    )
                }
            }

            Text(stringResource(R.string.filter_read_status), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadStatusFilter.entries.forEach { status ->
                    FilterChip(
                        selected = currentFilters.readStatus == status,
                        onClick = { currentFilters = currentFilters.copy(readStatus = status) },
                        label = { Text(stringResource(status.labelRes)) }
                    )
                }
            }

            if (allTags.isNotEmpty()) {
                Text(stringResource(R.string.section_tags), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        val selected = tag.id in currentFilters.tagIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val newSet = if (selected) {
                                    currentFilters.tagIds - tag.id
                                } else {
                                    currentFilters.tagIds + tag.id
                                }
                                currentFilters = currentFilters.copy(tagIds = newSet)
                            },
                            label = { Text(tag.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            Color(tag.color ?: 0xFF64B5F6.toInt()),
                                            CircleShape
                                        )
                                )
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { currentFilters = LibraryFilters() }) {
                    Text(stringResource(R.string.clear_all))
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Button(onClick = { onApply(currentFilters); onDismiss() }) {
                    Text(stringResource(R.string.action_apply))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

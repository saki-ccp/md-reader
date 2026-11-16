package com.aryan.reader

import com.aryan.reader.data.BookShelfCrossRef
import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.data.ShelfEntity
import com.aryan.reader.data.SmartCollectionDefinition
import com.aryan.reader.data.SmartCollectionEngine
import com.aryan.reader.data.SmartField
import com.aryan.reader.data.SmartOperator
import com.aryan.reader.data.SmartRule
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryStateProjectorTest {
    private val note = RecentFileItem("math", "file:///math.pdf", FileType.PDF, "Math notes", 1L,
        lastPage = 7, progressPercentage = 0.5f, highlightsJson = "[]")
    private val group = ShelfEntity("course", "Calculus", createdAt = 1L, updatedAt = 1L)

    private fun project(
        books: List<RecentFileItem> = listOf(note),
        groups: List<ShelfEntity> = emptyList(),
        refs: List<BookShelfCrossRef> = emptyList(),
        folders: List<SyncedFolder> = emptyList()
    ) = LibraryStateProjector().project(LibraryProjectionInput(
        ReaderScreenState(syncedFolders = folders), books, groups, refs, emptyList(), emptyList()
    ))

    private fun ReaderScreenState.ungroupedIds() = shelves.single { it.id == "unshelved" }.books.map { it.bookId }.toSet()

    @Test
    fun `a document can belong to two courses without losing reading data`() {
        val result = project(groups = listOf(group, group.copy(id = "review")), refs = listOf(
            BookShelfCrossRef(note.bookId, "course", 1L), BookShelfCrossRef(note.bookId, "review", 1L)
        ))
        assertEquals(emptySet<String>(), result.ungroupedIds())
        assertEquals(note, result.shelves.single { it.id == "course" }.books.single())
        assertEquals(note, result.shelves.single { it.id == "review" }.books.single())
        assertEquals(listOf(note), result.rawLibraryFiles)
    }

    @Test
    fun `deleting the last course returns document to ungrouped with progress intact`() {
        val result = project(groups = listOf(group.copy(isDeleted = true)),
            refs = listOf(BookShelfCrossRef(note.bookId, group.id, 1L)))
        assertEquals(setOf(note.bookId), result.ungroupedIds())
        assertEquals(note, result.rawLibraryFiles.single())
    }

    @Test
    fun `removing one course preserves membership in another`() {
        val result = project(groups = listOf(group, group.copy(id = "review")),
            refs = listOf(BookShelfCrossRef(note.bookId, "review", 1L)))
        assertEquals(emptySet<String>(), result.ungroupedIds())
        assertEquals(emptyList<RecentFileItem>(), result.shelves.single { it.id == "course" }.books)
        assertEquals(listOf(note), result.shelves.single { it.id == "review" }.books)
    }

    @Test
    fun `automatic series and folder shelves do not count as course groups`() {
        val folder = "content://notes"
        val books = listOf(note.copy(seriesName = "Series", sourceFolderUri = folder),
            note.copy(bookId = "second", seriesName = "Series"))
        val result = project(books = books, folders = listOf(SyncedFolder(folder, "Notes", 0L)))
        assertEquals(setOf("math", "second"), result.ungroupedIds())
    }

    @Test
    fun `automatic smart shelves do not hide ungrouped documents`() {
        val rules = SmartCollectionDefinition(rules = listOf(SmartRule(SmartField.TITLE, SmartOperator.CONTAINS, "Math")))
        val result = project(groups = listOf(group.copy(isSmart = true,
            smartRulesJson = SmartCollectionEngine.toJson(rules))))
        assertEquals(setOf(note.bookId), result.ungroupedIds())
    }
}

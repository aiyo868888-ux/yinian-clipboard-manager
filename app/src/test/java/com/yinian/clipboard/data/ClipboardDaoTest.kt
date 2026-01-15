package com.yinian.clipboard.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ClipboardDao 单元测试
 */
@RunWith(AndroidJUnit4::class)
class ClipboardDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: ClipboardDatabase
    private lateinit var dao: ClipboardDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClipboardDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.clipboardDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_and_retrieve_by_id() = runTest {
        // Given
        val entity = ClipboardEntity(
            type = ClipboardType.TEXT,
            textContent = "Hello World"
        )

        // When
        val id = dao.insert(entity)
        val retrieved = dao.getById(id)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.textContent).isEqualTo("Hello World")
        assertThat(retrieved?.id).isEqualTo(id)
    }

    @Test
    fun insert_multiple_and_get_all() = runTest {
        // Given
        val entities = listOf(
            ClipboardEntity(type = ClipboardType.TEXT, textContent = "First"),
            ClipboardEntity(type = ClipboardType.TEXT, textContent = "Second"),
            ClipboardEntity(type = ClipboardType.IMAGE, imageUri = "content://image")
        )

        // When
        dao.insertAll(entities)
        val all = dao.getAllByCreatedAt().first()

        // Then
        assertThat(all).hasSize(3)
        assertThat(all[0].textContent).isEqualTo("First")
        assertThat(all[1].textContent).isEqualTo("Second")
    }

    @Test
    fun get_latest_returns_most_recent() = runTest {
        // Given
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Old", createdAt = 1000))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "New", createdAt = 2000))

        // When
        val latest = dao.getLatest()

        // Then
        assertThat(latest).isNotNull()
        assertThat(latest?.textContent).isEqualTo("New")
    }

    @Test
    fun update_favorite() = runTest {
        // Given
        val id = dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Test"))

        // When
        dao.updateFavorite(id, true)
        val updated = dao.getById(id)

        // Then
        assertThat(updated?.isFavorite).isTrue()
    }

    @Test
    fun delete_by_id() = runTest {
        // Given
        val id = dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Delete me"))
        assertThat(dao.getById(id)).isNotNull()

        // When
        dao.deleteById(id)
        val deleted = dao.getById(id)

        // Then
        assertThat(deleted).isNull()
    }

    @Test
    fun search_by_text() = runTest {
        // Given
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Hello World"))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Goodbye World"))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "No match"))

        // When
        val results = dao.searchByText("World").first()

        // Then
        assertThat(results).hasSize(2)
    }

    @Test
    fun filter_by_type() = runTest {
        // Given
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Text"))
        dao.insert(ClipboardEntity(type = ClipboardType.IMAGE, imageUri = "content://image"))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Another text"))

        // When
        val textItems = dao.getByType(ClipboardType.TEXT).first()
        val imageItems = dao.getByType(ClipboardType.IMAGE).first()

        // Then
        assertThat(textItems).hasSize(2)
        assertThat(imageItems).hasSize(1)
    }

    @Test
    fun get_favorites() = runTest {
        // Given
        val id1 = dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Favorite 1"))
        val id2 = dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Not favorite"))
        val id3 = dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Favorite 2"))

        dao.updateFavorite(id1, true)
        dao.updateFavorite(id3, true)

        // When
        val favorites = dao.getFavorites().first()

        // Then
        assertThat(favorites).hasSize(2)
        assertThat(favorites.map { it.id }).containsExactly(id1, id3)
    }

    @Test
    fun get_count() = runTest {
        // Given
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "1"))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "2"))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "3"))

        // When
        val count = dao.getCount()

        // Then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun delete_old_records() = runTest {
        // Given
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "1", createdAt = 1000))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "2", createdAt = 2000))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "3", createdAt = 3000))
        dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "4", createdAt = 4000))

        // When - keep only latest 2
        val deleted = dao.deleteOldRecords(2)
        val remaining = dao.getAllByCreatedAt().first()

        // Then
        assertThat(deleted).isEqualTo(2) // 删除了2条
        assertThat(remaining).hasSize(2)
        assertThat(remaining[0].textContent).isEqualTo("4")
        assertThat(remaining[1].textContent).isEqualTo("3")
    }

    @Test
    fun update_entity() = runTest {
        // Given
        val id = dao.insert(ClipboardEntity(type = ClipboardType.TEXT, textContent = "Original"))
        val entity = dao.getById(id)!!

        // When
        val updated = entity.copy(textContent = "Updated")
        dao.update(updated)
        val retrieved = dao.getById(id)

        // Then
        assertThat(retrieved?.textContent).isEqualTo("Updated")
    }
}

package com.dsi.sticky_header_list

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

class StickyHeaderGroupItemsTest {

    data class TestDataModel(
        val id: Int,
        val name: String,
        val date: String
    )

    @Test
    fun `test stickyHeaderGroupItems groups by date correctly`() {
        // Arrange
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)

        val items = listOf(
            TestDataModel(1, "Item 1", today),
            TestDataModel(2, "Item 2", today),
            TestDataModel(3, "Item 3", yesterday),
            TestDataModel(4, "Item 4", yesterday)
        )

        // Act
        val result = stickyHeaderGroupItems(
            items = items,
            headerExtractor = { item -> DateUtils.getDateSortKey(item.date) },
            itemSorter = { it.id },
            isHeaderAscending = true,
            isItemAscending = true
        )

        // Assert
        // Should have 2 headers + 4 items = 6 total
        assertEquals(6, result.size)

        // Check structure: Header -> Items -> Header -> Items
        assertTrue(result[0] is StickyHeaderListItem.Header)
        assertTrue(result[1] is StickyHeaderListItem.Item)
        assertTrue(result[2] is StickyHeaderListItem.Item)
        assertTrue(result[3] is StickyHeaderListItem.Header)
        assertTrue(result[4] is StickyHeaderListItem.Item)
        assertTrue(result[5] is StickyHeaderListItem.Item)
    }

    @Test
    fun `test stickyHeaderGroupItems sorts headers in descending order when isHeaderAscending is true with negative sort key`() {
        // Arrange
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgo = dateFormat.format(calendar.time)

        val items = listOf(
            TestDataModel(1, "Old Item", fiveDaysAgo),
            TestDataModel(2, "New Item", today)
        )

        // Act
        val result = stickyHeaderGroupItems(
            items = items,
            headerExtractor = { item -> DateUtils.getDateSortKey(item.date) },
            itemSorter = { it.id },
            isHeaderAscending = true,
            isItemAscending = true
        )

        // Assert
        // Because getDateSortKey returns negative values, ascending sort will show newest first
        val firstHeader = result[0] as StickyHeaderListItem.Header
        val firstItem = result[1] as StickyHeaderListItem.Item
        val secondHeader = result[2] as StickyHeaderListItem.Header
        val secondItem = result[3] as StickyHeaderListItem.Item

        // First header should be today (newer date)
        assertEquals(DateUtils.getDateSortKey(today), firstHeader.data)
        assertEquals("New Item", (firstItem.data as TestDataModel).name)

        // Second header should be 5 days ago (older date)
        assertEquals(DateUtils.getDateSortKey(fiveDaysAgo), secondHeader.data)
        assertEquals("Old Item", (secondItem.data as TestDataModel).name)
    }

    @Test
    fun `test stickyHeaderGroupItems sorts items within groups`() {
        // Arrange
        val date = "2025-01-01"
        val items = listOf(
            TestDataModel(3, "Item 3", date),
            TestDataModel(1, "Item 1", date),
            TestDataModel(2, "Item 2", date)
        )

        // Act - ascending order
        val resultAsc = stickyHeaderGroupItems(
            items = items,
            headerExtractor = { item -> DateUtils.getDateSortKey(item.date) },
            itemSorter = { it.id },
            isHeaderAscending = true,
            isItemAscending = true
        )

        // Assert
        val item1 = resultAsc[1] as StickyHeaderListItem.Item
        val item2 = resultAsc[2] as StickyHeaderListItem.Item
        val item3 = resultAsc[3] as StickyHeaderListItem.Item

        assertEquals(1, (item1.data as TestDataModel).id)
        assertEquals(2, (item2.data as TestDataModel).id)
        assertEquals(3, (item3.data as TestDataModel).id)

        // Act - descending order
        val resultDesc = stickyHeaderGroupItems(
            items = items,
            headerExtractor = { item -> DateUtils.getDateSortKey(item.date) },
            itemSorter = { it.id },
            isHeaderAscending = true,
            isItemAscending = false
        )

        // Assert
        val item1Desc = resultDesc[1] as StickyHeaderListItem.Item
        val item2Desc = resultDesc[2] as StickyHeaderListItem.Item
        val item3Desc = resultDesc[3] as StickyHeaderListItem.Item

        assertEquals(3, (item1Desc.data as TestDataModel).id)
        assertEquals(2, (item2Desc.data as TestDataModel).id)
        assertEquals(1, (item3Desc.data as TestDataModel).id)
    }

    @Test
    fun `test stickyHeaderGroupItems handles empty list`() {
        // Arrange
        val items = emptyList<TestDataModel>()

        // Act
        val result = stickyHeaderGroupItems(
            items = items,
            headerExtractor = { item -> DateUtils.getDateSortKey(item.date) },
            itemSorter = { it.id },
            isHeaderAscending = true,
            isItemAscending = true
        )

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test stickyHeaderGroupItems handles single item`() {
        // Arrange
        val items = listOf(TestDataModel(1, "Single Item", "2025-01-01"))

        // Act
        val result = stickyHeaderGroupItems(
            items = items,
            headerExtractor = { item -> DateUtils.getDateSortKey(item.date) },
            itemSorter = { it.id },
            isHeaderAscending = true,
            isItemAscending = true
        )

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is StickyHeaderListItem.Header)
        assertTrue(result[1] is StickyHeaderListItem.Item)
    }

    @Test
    fun `test DateUtils formatDateForHeader returns correct labels`() {
        // Arrange
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgo = dateFormat.format(calendar.time)

        // Act & Assert
        assertEquals("Today", DateUtils.formatDateForHeader(today))
        assertEquals("Yesterday", DateUtils.formatDateForHeader(yesterday))
        assertTrue(DateUtils.formatDateForHeader(twoDaysAgo).matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun `test DateUtils getDateSortKey returns negative values for chronological order`() {
        // Arrange
        val newerDate = "2025-10-07"
        val olderDate = "2025-10-01"

        // Act
        val newerKey = DateUtils.getDateSortKey(newerDate)
        val olderKey = DateUtils.getDateSortKey(olderDate)

        // Assert
        assertTrue(newerKey < 0) // Should be negative
        assertTrue(olderKey < 0) // Should be negative
        assertTrue(newerKey < olderKey) // Newer date has smaller (more negative) value
    }
}


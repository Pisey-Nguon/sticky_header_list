package com.dsi.sticky_header_list

import java.util.Date

sealed class StickyHeaderListItem<out H, out I> {
    data class Header<H>(val data: H) : StickyHeaderListItem<H, Nothing>()
    data class Item<I>(val data: I) : StickyHeaderListItem<Nothing, I>()
    object Loading : StickyHeaderListItem<Nothing, Nothing>()
}

/**
 * Simple version: Groups items by date and automatically formats headers.
 * This is the easiest way to create date-based sticky headers.
 *
 * Uses Date objects for type safety - no format confusion!
 *
 * @param items List of items to group
 * @param dateExtractor Function that returns a Date object from each item
 * @param newestFirst If true, shows newest dates first. Default is true.
 *
 * Example usage:
 * ```
 * data class Task(val id: Int, val name: String, val createdAt: Date)
 *
 * val grouped = stickyHeaderGroupItems(
 *     items = myTasks,
 *     dateExtractor = { it.createdAt }
 * )
 * ```
 */
fun <T : Any> stickyHeaderGroupItems(
    items: List<T>,
    dateExtractor: (T) -> Date,
    newestFirst: Boolean = true
): List<StickyHeaderListItem<String, T>> {
    if (items.isEmpty()) return emptyList()

    // Group by normalized date (same day = same group)
    val groupedMap = items.groupBy { item ->
        DateUtils.normalizeToDay(dateExtractor(item))
    }
    val result = mutableListOf<StickyHeaderListItem<String, T>>()

    // Sort dates chronologically
    val sortedDates = if (newestFirst) {
        groupedMap.keys.sortedDescending()
    } else {
        groupedMap.keys.sorted()
    }

    // Build result with formatted headers
    sortedDates.forEach { date ->
        val formattedHeader = DateUtils.formatDateForHeader(date)
        result.add(StickyHeaderListItem.Header(formattedHeader))
        groupedMap[date]?.forEach {
            result.add(StickyHeaderListItem.Item(it))
        }
    }

    return result
}

/**
 * Advanced version with custom sorting for both headers and items.
 * Use this when you need more control over sorting.
 */
fun <T : Any, H : Comparable<H>, I : Comparable<I>> stickyHeaderGroupItems(
    items: List<T>,
    headerExtractor: (T) -> H,
    itemSorter: (T) -> I,
    isHeaderAscending: Boolean = true,
    isItemAscending: Boolean = true
): List<StickyHeaderListItem<H, T>> {
    val groupedMap = items.groupBy(headerExtractor)
    val result = mutableListOf<StickyHeaderListItem<H, T>>()

    val sortedHeaders = if (isHeaderAscending) {
        groupedMap.keys.sortedBy { it }
    } else {
        groupedMap.keys.sortedByDescending { it }
    }

    sortedHeaders.forEach { key ->
        result.add(StickyHeaderListItem.Header(key))
        val sortedItems = if (isItemAscending) {
            groupedMap[key]?.sortedBy(itemSorter)
        } else {
            groupedMap[key]?.sortedByDescending(itemSorter)
        }
        sortedItems?.forEach { result.add(StickyHeaderListItem.Item(it)) }
    }

    return result
}
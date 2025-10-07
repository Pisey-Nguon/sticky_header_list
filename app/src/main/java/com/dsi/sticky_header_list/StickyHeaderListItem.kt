package com.dsi.sticky_header_list

import java.util.Date

sealed class StickyHeaderListItem<out H, out I> {
    data class Header<H>(val data: H) : StickyHeaderListItem<H, Nothing>()
    data class Item<I>(val data: I) : StickyHeaderListItem<Nothing, I>()
    object Loading : StickyHeaderListItem<Nothing, Nothing>()
}

/**
 * Most flexible version: Use custom header objects with custom comparison logic.
 * Perfect when you need headers as objects (not just comparable primitives or strings).
 *
 * @param items List of items to group
 * @param headerExtractor Function that returns a header object for each item
 * @param headerComparator Comparator to define how headers should be sorted (optional)
 * @param itemComparator Comparator to define how items should be sorted within groups (optional)
 *
 * Example usage with custom header object:
 * ```
 * data class DateHeader(
 *     val date: Date,
 *     val label: String,
 *     val itemCount: Int
 * )
 *
 * data class Task(val id: Int, val name: String, val createdAt: Date)
 *
 * val grouped = stickyHeaderGroupItems(
 *     items = myTasks,
 *     headerExtractor = { task ->
 *         val normalizedDate = DateUtils.normalizeToDay(task.createdAt)
 *         DateHeader(
 *             date = normalizedDate,
 *             label = DateUtils.formatDateForHeader(normalizedDate),
 *             itemCount = myTasks.count { DateUtils.normalizeToDay(it.createdAt) == normalizedDate }
 *         )
 *     },
 *     headerComparator = compareByDescending { it.date }, // Sort by date, newest first
 *     itemComparator = compareByDescending { it.createdAt } // Sort items by creation date
 * )
 * ```
 */
fun <T : Any, H : Any> stickyHeaderGroupItems(
    items: List<T>,
    headerExtractor: (T) -> H,
    headerComparator: Comparator<H>? = null,
    itemComparator: Comparator<T>? = null
): List<StickyHeaderListItem<H, T>> {
    if (items.isEmpty()) return emptyList()

    val groupedMap = items.groupBy(headerExtractor)
    val result = mutableListOf<StickyHeaderListItem<H, T>>()

    val sortedHeaders = if (headerComparator != null) {
        groupedMap.keys.sortedWith(headerComparator)
    } else {
        groupedMap.keys.toList()
    }

    sortedHeaders.forEach { header ->
        result.add(StickyHeaderListItem.Header(header))

        val groupItems = groupedMap[header] ?: emptyList()
        val sortedItems = if (itemComparator != null) {
            groupItems.sortedWith(itemComparator)
        } else {
            groupItems
        }

        sortedItems.forEach {
            result.add(StickyHeaderListItem.Item(it))
        }
    }

    return result
}

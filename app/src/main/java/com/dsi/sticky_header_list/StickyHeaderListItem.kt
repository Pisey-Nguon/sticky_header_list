package com.dsi.sticky_header_list

sealed class StickyHeaderListItem {
    data class Header(val data: Any) : StickyHeaderListItem()
    data class Item(val data: Any) : StickyHeaderListItem()
    object Loading : StickyHeaderListItem()
}

fun <T : Any> stickyHeaderGroupItems(
    items: List<T>,
    headerExtractor: (T) -> Any
): List<StickyHeaderListItem> {
    val groupedMap = items.groupBy(headerExtractor)
    val result = mutableListOf<StickyHeaderListItem>()
    // Optionally sort headers if needed
    groupedMap.keys.sortedBy { it.toString() }.forEach { key ->
        result.add(StickyHeaderListItem.Header(key))
        groupedMap[key]?.forEach { result.add(StickyHeaderListItem.Item(it)) }
    }
    return result
}
fun <T : Any, H : Comparable<H>, I : Comparable<I>> stickyHeaderGroupItems(
    items: List<T>,
    headerExtractor: (T) -> H,
    itemSorter: (T) -> I,
    isHeaderAscending: Boolean = true,
    isItemAscending: Boolean = true
): List<StickyHeaderListItem> {
    val groupedMap = items.groupBy(headerExtractor)
    val result = mutableListOf<StickyHeaderListItem>()

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
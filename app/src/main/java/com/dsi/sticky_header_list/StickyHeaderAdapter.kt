package com.dsi.sticky_header_list

import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.dsi.sticky_header_list.databinding.ItemStickyLoadingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class StickyHeaderAdapter<HB : ViewBinding, IB : ViewBinding, H, I>(
    val items: ArrayList<StickyHeaderListItem<H, I>>,
    val headerInflater: (LayoutInflater, ViewGroup, Boolean) -> HB,
    private val itemInflater: (LayoutInflater, ViewGroup, Boolean) -> IB,
    val bindHeader: (HB, H) -> Unit,
    private val bindItem: (IB, I) -> Unit,
    private val onLoadMore: suspend (offset: Int) -> List<StickyHeaderListItem<H, I>>,
    private val lifecycleScope: LifecycleCoroutineScope,

    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        const val TYPE_LOADING = 2
    }

    private var isLoading = false
    private var hasMore = true

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMore &&
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                    firstVisibleItemPosition >= 0
                ) {
                    // Post to next frame to avoid modifying RecyclerView during scroll callback
                    rv.post {
                        if (!isLoading && hasMore) { // Double-check since we're posting
                            showLoading()
                            lifecycleScope.launch(Dispatchers.IO) {
                                isLoading = true
                                val offset = items.count { it is StickyHeaderListItem.Item<*> }
                                val newItems = onLoadMore.invoke(offset)
                                hasMore = newItems.isNotEmpty()
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                    if (newItems.isNotEmpty()) {
                                        Log.d("StickyHeaderAdapter", "Received ${newItems.size} new items to add")
                                        val updated = ArrayList(items)
                                        val sizeBefore = updated.size

                                        // Check if we need to merge items under an existing header
                                        val firstNewItem = newItems.firstOrNull()
                                        if (firstNewItem is StickyHeaderListItem.Header<*>) {
                                            @Suppress("UNCHECKED_CAST")
                                            val firstHeader = firstNewItem as StickyHeaderListItem.Header<H>
                                            val existingHeaderIndex = updated.indexOfFirst {
                                                it is StickyHeaderListItem.Header<*> && it.data == firstHeader.data
                                            }

                                            if (existingHeaderIndex != -1) {
                                                // Header already exists, skip it and add only the items
                                                val itemsToAdd = newItems.drop(1) // Skip the duplicate header

                                                // Find the position to insert (after all items of this header)
                                                var insertPosition = existingHeaderIndex + 1
                                                while (insertPosition < updated.size &&
                                                    updated[insertPosition] is StickyHeaderListItem.Item<*>
                                                ) {
                                                    insertPosition++
                                                }

                                                updated.addAll(insertPosition, itemsToAdd)
                                                Log.d("StickyHeaderAdapter", "Merged ${itemsToAdd.size} items under existing header '${firstHeader.data}' at position $insertPosition")
                                            } else {
                                                // New header, add everything
                                                updated.addAll(newItems)
                                                Log.d("StickyHeaderAdapter", "Added new header '${firstHeader.data}' and ${newItems.size - 1} items")
                                            }
                                        } else {
                                            // No header in new items, just add everything
                                            updated.addAll(newItems)
                                            Log.d("StickyHeaderAdapter", "Added ${newItems.size} items without header")
                                        }

                                        val sizeAfter = updated.size
                                        Log.d("StickyHeaderAdapter", "List size: $sizeBefore -> $sizeAfter (added ${sizeAfter - sizeBefore} items)")
                                        updateData(updated)
                                    } else {
                                        Log.d("StickyHeaderAdapter", "No more items to load")
                                    }

                                    isLoading = false
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun getItemViewType(position: Int): Int {
        if (position < 0 || position >= items.size) {
            return TYPE_ITEM // Return a safe default type
        }
        return when (items[position]) {
            is StickyHeaderListItem.Header -> TYPE_HEADER
            is StickyHeaderListItem.Item -> TYPE_ITEM
            is StickyHeaderListItem.Loading -> TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = headerInflater.invoke(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }

            TYPE_ITEM -> {
                val binding = itemInflater.invoke(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ItemViewHolder(binding)
            }

            TYPE_LOADING -> {
                val binding = ItemStickyLoadingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                LoadingViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val listItem = items[position]) {
            is StickyHeaderListItem.Header<*> -> {
                val headerHolder = holder as StickyHeaderAdapter<HB, IB, H, I>.HeaderViewHolder
                headerHolder.bindHeader(listItem.data as H)
            }
            is StickyHeaderListItem.Item<*> -> {
                val itemHolder = holder as StickyHeaderAdapter<HB, IB, H, I>.ItemViewHolder
                itemHolder.bindItem(listItem.data as I)
            }
            is StickyHeaderListItem.Loading -> { /* No binding needed */ }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<StickyHeaderListItem<H, I>>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun showLoading() {
        if (!isLoadingItemShown()) {
            val updated = ArrayList(items)
            updated.add(StickyHeaderListItem.Loading)
            updateData(updated)
        }
    }

    private fun hideLoading() {
        if (isLoadingItemShown()) {
            val updated = ArrayList(items)
            updated.removeAt(updated.lastIndex)
            updateData(updated)
        }
    }

    private fun isLoadingItemShown(): Boolean {
        return items.isNotEmpty() && items.last() is StickyHeaderListItem.Loading
    }

    fun filterStickyHeaderList(
        items: List<StickyHeaderListItem<H, I>>,
        predicate: (I) -> Boolean
    ): List<StickyHeaderListItem<H, I>> {
        val result = mutableListOf<StickyHeaderListItem<H, I>>()
        var currentHeader: StickyHeaderListItem.Header<H>? = null
        val currentGroupItems = mutableListOf<StickyHeaderListItem.Item<I>>()

        items.forEach { item ->
            when (item) {
                is StickyHeaderListItem.Header<*> -> {
                    // Add previous header and its filtered items
                    currentHeader?.let { header ->
                        if (currentGroupItems.isNotEmpty()) {
                            result.add(header)
                            result.addAll(currentGroupItems)
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    currentHeader = item as StickyHeaderListItem.Header<H>
                    currentGroupItems.clear()
                }
                is StickyHeaderListItem.Item<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val typedItem = item as StickyHeaderListItem.Item<I>
                    if (predicate(typedItem.data)) {
                        currentGroupItems.add(typedItem)
                    }
                }
                is StickyHeaderListItem.Loading -> { /* Skip loading items */ }
            }
        }

        // Add last header and its items
        currentHeader?.let { header ->
            if (currentGroupItems.isNotEmpty()) {
                result.add(header)
                result.addAll(currentGroupItems)
            }
        }

        return result
    }


    inner class HeaderViewHolder(val binding: HB) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindHeader(data: H) {
            bindHeader(binding, data)
        }
    }

    inner class ItemViewHolder(val binding: IB) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindItem(data: I) {
            this@StickyHeaderAdapter.bindItem(binding, data)
        }
    }

    inner class LoadingViewHolder(binding: ItemStickyLoadingBinding) :
        RecyclerView.ViewHolder(binding.root)
}
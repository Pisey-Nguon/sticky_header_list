package com.dsi.sticky_header_list

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsi.sticky_header_list.databinding.ItemDataBinding
import com.dsi.sticky_header_list.databinding.ItemHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.isNotEmpty

class MainActivity : AppCompatActivity() {

    // Data model with Date object instead of string
    data class MyDataModel(
        val id: Int,
        val name: String,
        val date: Date
    )

    private lateinit var adapter: StickyHeaderAdapter<ItemHeaderBinding, ItemDataBinding, String, MyDataModel>
    private lateinit var stickyDecoration: StickyHeaderDecoration<ItemHeaderBinding, ItemDataBinding, String, MyDataModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupWindowInsets()
        setupRecyclerView()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Create adapter with empty initial list
        adapter = StickyHeaderAdapter(
            items = ArrayList(),
            headerInflater = { inflater, parent, attach ->
                ItemHeaderBinding.inflate(inflater, parent, attach)
            },
            itemInflater = { inflater, parent, attach ->
                ItemDataBinding.inflate(inflater, parent, attach)
            },
            bindHeader = { binding, data ->
                binding.tvHeader.text = data
            },
            bindItem = { binding, data ->
                binding.tvItemName.text = data.name
                binding.root.setOnClickListener {
                    Log.d("StickyHeaderList", "Item clicked: ${data.name}")
                    onItemClicked(data)
                }
            },
            onLoadMore = { offset -> loadMoreData(offset) }
        )

        // Setup decoration with sticky header enabled by default
        stickyDecoration = StickyHeaderDecoration(adapter, isStickyEnabled = true)

        // Configure RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            addItemDecoration(stickyDecoration)

            // Optional: Add item animations
            itemAnimator?.changeDuration = 200
        }

        // Trigger initial load since we start with empty list
        triggerInitialLoad()
    }

    private fun triggerInitialLoad() {
        // Manually trigger the first load since the list starts empty
        lifecycleScope.launch {
            try {
                val initialItems = loadMoreData(offset = 0)
                if (initialItems.isNotEmpty()) {
                    adapter.updateData(initialItems)
                }
            } catch (e: Exception) {
                Log.e("StickyHeaderList", "Error loading initial data", e)
            }
        }
    }

    private suspend fun loadMoreData(offset: Int): List<StickyHeaderListItem<String, MyDataModel>> {
        try {
            Log.d("MainActivity", "loadMoreData called with offset: $offset")
            val newItems = fetchDataFromApi(offset = offset, pageSize = 10)
            Log.d("MainActivity", "fetchDataFromApi returned ${newItems.size} items")

            // Simple and easy! Just pass your items and the Date field
            return stickyHeaderGroupItems(
                items = newItems,
                dateExtractor = { it.date },
                newestFirst = true
            )
        } catch (e: Exception) {
            Log.e("StickyHeaderList", "Error loading more data", e)
            return emptyList()
        }
    }

    // Simulated API call with better error handling
    private suspend fun fetchDataFromApi(offset: Int, pageSize: Int): List<MyDataModel> {
        Log.d("checkStatus ", "fetchDataFromApi: offset $offset")
        delay(1500) // Simulate network delay

        val startId = offset + 1
        val calendar = Calendar.getInstance()

        // Set different dates based on offset to demonstrate Today, Yesterday, and formatted dates
        when (offset) {
            0 -> {
                // Today's date - no change needed
            }
            10 -> {
                // Yesterday
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            20 -> {
                // 2 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -2)
            }
            30 -> {
                // 5 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -5)
            }
            else -> {
                // 7 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
        }

        val date = calendar.time
        Log.d("checkStatus ", "fetchDataFromApi: offset done $offset with date $date")

        return (1..pageSize).map { index ->
            MyDataModel(
                id = startId + index,
                name = "Remote Item ${offset + index}",
                date = date  // Direct Date object - no string conversion!
            )
        }
    }

    private fun onItemClicked(item: MyDataModel) {
        // Handle item click - could navigate, show details, etc.
        Log.d("StickyHeaderList", "Handling click for item: ${item.name}")

        // Example: Toggle sticky header on/off when clicking an item
        // Uncomment to test the toggle feature
        // val isEnabled = stickyDecoration.isStickyEnabled()
        // stickyDecoration.setStickyEnabled(!isEnabled)
        // findViewById<RecyclerView>(R.id.recyclerView).invalidateItemDecorations()
        // Log.d("StickyHeaderList", "Sticky header ${if (!isEnabled) "enabled" else "disabled"}")
    }

    override fun onDestroy() {
        // Clear cached views to prevent memory leaks
        if (::stickyDecoration.isInitialized) {
            stickyDecoration.clearCache()
        }
        super.onDestroy()
    }
}

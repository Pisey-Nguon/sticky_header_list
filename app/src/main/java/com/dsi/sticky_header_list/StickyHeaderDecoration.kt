package com.dsi.sticky_header_list

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class StickyHeaderDecoration<HB : ViewBinding, IB : ViewBinding>(
    private val adapter: StickyHeaderAdapter<HB, IB>,
    private var isStickyEnabled: Boolean = true
) : RecyclerView.ItemDecoration() {

    private var stickyHeaderView: View? = null
    private var stickyHeaderPosition = RecyclerView.NO_POSITION

    /**
     * Enable or disable sticky header display
     */
    fun setStickyEnabled(enabled: Boolean) {
        isStickyEnabled = enabled
    }

    /**
     * Check if sticky header is currently enabled
     */
    fun isStickyEnabled(): Boolean = isStickyEnabled

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)

        // If sticky header is disabled, don't draw anything
        if (!isStickyEnabled) {
            return
        }

        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) return

        val headerPos = getHeaderPositionForItem(topChildPosition)
        if (headerPos == RecyclerView.NO_POSITION) return

        val currentHeader = getHeaderViewForItem(headerPos, parent)
        fixLayoutSize(parent, currentHeader)

        val contactPoint = currentHeader.bottom
        val childInContact = getChildInContact(parent, contactPoint)

        if (childInContact != null && adapter.getItemViewType(parent.getChildAdapterPosition(childInContact)) == StickyHeaderAdapter.TYPE_HEADER) {
            moveHeader(canvas, currentHeader, childInContact)
        } else {
            drawHeader(canvas, currentHeader)
        }
    }

    private fun getHeaderViewForItem(headerPosition: Int, parent: RecyclerView): View {
        if (stickyHeaderPosition != headerPosition) {
            stickyHeaderView?.let {
                // Clean up old view if it exists
                if (it.parent != null) {
                    (it.parent as? ViewGroup)?.removeView(it)
                }
            }

            val headerBinding = adapter.headerInflater(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            val headerItem = adapter.items[headerPosition] as StickyHeaderListItem.Header
            adapter.bindHeader(headerBinding, headerItem.data)

            stickyHeaderView = headerBinding.root
            stickyHeaderPosition = headerPosition
        }
        return stickyHeaderView!!
    }

    private fun drawHeader(canvas: Canvas, header: View) {
        canvas.save()
        canvas.translate(0f, 0f)
        header.draw(canvas)
        canvas.restore()
    }

    private fun moveHeader(canvas: Canvas, currentHeader: View, nextHeader: View) {
        canvas.save()
        canvas.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
        currentHeader.draw(canvas)
        canvas.restore()
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.bottom > contactPoint && child.top <= contactPoint) {
                return child
            }
        }
        return null
    }

    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        val childWidth = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.paddingLeft + parent.paddingRight,
            view.layoutParams.width
        )
        val childHeight = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom,
            view.layoutParams.height
        )

        view.measure(childWidth, childHeight)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private fun getHeaderPositionForItem(itemPosition: Int): Int {
        var headerPosition = RecyclerView.NO_POSITION
        var currentPosition = itemPosition

        do {
            if (adapter.getItemViewType(currentPosition) == StickyHeaderAdapter.TYPE_HEADER) {
                headerPosition = currentPosition
                break
            }
            currentPosition -= 1
        } while (currentPosition >= 0)

        return headerPosition
    }

    fun clearCache() {
        stickyHeaderView = null
        stickyHeaderPosition = RecyclerView.NO_POSITION
    }
}

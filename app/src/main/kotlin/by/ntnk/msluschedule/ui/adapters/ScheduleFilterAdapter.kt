package by.ntnk.msluschedule.ui.adapters

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import by.ntnk.msluschedule.network.data.ScheduleFilter

class ScheduleFilterAdapter(
        context: Context,
        @param:LayoutRes private val resource: Int,
        private val unfilteredData: ScheduleFilter
) : BaseAdapter(), Filterable {
    private var filteredData: ScheduleFilter = unfilteredData
    private val filter: DataFilter by lazy { DataFilter() }
    private val dummyFilter: DummyFilter by lazy { DummyFilter() }
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    var isStartsWithFilter = false
    var isIgnoreCaseFilter = false
    var isFilteringEnabled = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(layoutInflater, position, convertView, parent, resource)
    }

    private fun createViewFromResource(
            inflater: LayoutInflater,
            position: Int,
            convertView: View?,
            parent: ViewGroup,
            resource: Int
    ): View {
        val text: TextView
        val view: View = convertView ?: inflater.inflate(resource, parent, false)
        try {
            text = view as TextView
        } catch (exception: ClassCastException) {
            throw IllegalStateException(
                    this.javaClass.simpleName + " requires the resource ID to be a TextView",
                    exception
            )
        }

        val item = getItem(position)
        text.text = item
        return view
    }

    override fun getCount(): Int {
        return filteredData.size
    }

    override fun getItem(position: Int): String {
        return filteredData.valueAt(position)
    }

    override fun getItemId(position: Int): Long {
        return filteredData.keyAt(position).toLong()
    }

    override fun getFilter(): Filter {
        return if (isFilteringEnabled) filter else dummyFilter
    }

    private inner class DummyFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val filterResults = Filter.FilterResults()
            filterResults.values = unfilteredData
            filterResults.count = unfilteredData.size
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults) {
        }
    }

    private inner class DataFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val filterResults = Filter.FilterResults()
            if (constraint == null || constraint.isEmpty()) {
                filterResults.values = unfilteredData
                filterResults.count = unfilteredData.size
            } else {
                val constraintString = constraint.toString()
                val filteredData = ScheduleFilter()
                for (entry in unfilteredData.data.entries) {
                    val key = entry.key
                    val value = entry.value
                    if (isStartsWithFilter) {
                        if (value.startsWith(constraintString, ignoreCase = isIgnoreCaseFilter)) {
                            filteredData.data[key] = value
                        }
                    } else {
                        if (value.contains(constraintString, ignoreCase = isIgnoreCaseFilter)) {
                            filteredData.data[key] = value
                        }
                    }
                }
                filterResults.values = filteredData
                filterResults.count = filteredData.size
            }
            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults) {
            filteredData = results.values as ScheduleFilter
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}


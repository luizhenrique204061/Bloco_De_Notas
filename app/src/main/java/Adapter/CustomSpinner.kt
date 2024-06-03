package Adapter

import Modelo.SpinnerItem
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.olamundo.blocodenotas.R

class CustomSpinnerAdapter(
    context: Context,
    private val items: List<SpinnerItem>
) : ArrayAdapter<SpinnerItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
        val item = getItem(position)

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.textView)

        item?.let {
            imageView.setImageResource(it.imageResId)
            textView.text = it.text
        }

        return view
    }
}
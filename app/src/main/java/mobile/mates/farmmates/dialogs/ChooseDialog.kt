package mobile.mates.farmmates.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment

class ChooseDialog(
    private val loadFromGallery: () -> Unit,
    private val loadFromCamera: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options: ArrayList<String> = arrayListOf("Gallery", "Camera")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_expandable_list_item_1, options)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select from: ")
        builder.setAdapter(
            adapter
        ) { _, which ->
            if (which == 0)
                loadFromGallery()
            else
                loadFromCamera()
        }

        return builder.create()
    }
}
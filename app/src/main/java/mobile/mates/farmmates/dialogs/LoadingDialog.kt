package mobile.mates.farmmates.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import mobile.mates.farmmates.databinding.LoadingDialogBinding

class LoadingDialog : DialogFragment() {

    private lateinit var binding: LoadingDialogBinding
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        binding = LoadingDialogBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(context)
        builder.setView(binding.root)
        builder.setCancelable(false)

        return builder.create()
    }
}
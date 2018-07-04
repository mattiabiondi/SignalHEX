package biondi.mattia.signalhex

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle

class MapType: DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.map_type)
                .setItems(R.array.map_type, DialogInterface.OnClickListener { dialog, which ->

                })
        return builder.create()
    }
}
package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class UndoRequestedDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder
    private lateinit var onClick: (DialogResult) -> Unit

    fun create(context: Context, onClick: (DialogResult) -> Unit) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Undo requested!")
        this.onClick = onClick
    }

    fun show(opponentUsername: String) {
        dialogBuilder.setMessage("$opponentUsername is requesting to undo their last move")
        dialogBuilder.setPositiveButton("Accept") { _, _ ->
            onClick(DialogResult.ACCEPT)
        }
        dialogBuilder.setNegativeButton("Reject") { _, _ ->
            onClick(DialogResult.DECLINE)
        }

        dialog = dialogBuilder.show()
    }

    class UndoRequestData(val gameId: String, val opponentName: String) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(gameId)
            parcel.writeString(opponentName)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<UndoRequestData> {
            override fun createFromParcel(parcel: Parcel): UndoRequestData {
                return UndoRequestData(parcel)
            }

            override fun newArray(size: Int): Array<UndoRequestData?> {
                return arrayOfNulls(size)
            }
        }

    }
}
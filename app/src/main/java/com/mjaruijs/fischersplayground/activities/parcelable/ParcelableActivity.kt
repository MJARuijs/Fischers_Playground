package com.mjaruijs.fischersplayground.activities.parcelable

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.services.StoreDataWorker
import com.mjaruijs.fischersplayground.util.Logger
import java.util.LinkedList

class ParcelableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parcelable)

        val worker = OneTimeWorkRequestBuilder<Worker>()

            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(worker)

        workManager.getWorkInfoByIdLiveData(worker.id)
            .observe(this) {
                if (it != null && it.state.isFinished) {
                    val result = it.outputData.getParcelable(ParcelableArrayList, "output") ?: return@observe
                    onResult(result)
                }
            }
    }

    fun onResult(parcelable: Parcelable) {
        val list = parcelable as ParcelableArrayList
        for (e in list.list.entries) {
            Logger.debug("client_activity", "${e.key} ${e.value.size}")
        }
    }

    private fun Data.getParcelable(type: Parcelable.Creator<*>?, key: String): Parcelable? {
        val parcel = Parcel.obtain()
        try {
            val bytes = getByteArray(key) ?: return null
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)

            return type?.createFromParcel(parcel) as Parcelable?
        } finally {
            parcel.recycle()
        }
    }

}
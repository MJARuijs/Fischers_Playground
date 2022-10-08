package com.mjaruijs.fischersplayground.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper
import com.mjaruijs.fischersplayground.activities.ClientActivity

class BackupAgent : BackupAgentHelper() {

    companion object {
        private const val BACKUP_KEY = "FISCHERS_BACKUP_AGENT"
    }

    override fun onCreate() {
        val helper = SharedPreferencesBackupHelper(this, ClientActivity.USER_PREFERENCE_FILE)
        addHelper(BACKUP_KEY, helper)
    }

}
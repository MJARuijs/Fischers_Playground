package com.mjaruijs.fischersplayground.services

import android.content.Context
import com.mjaruijs.fischersplayground.activities.opening.PracticeSession
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.util.concurrent.atomic.AtomicBoolean

class DataManager(context: Context) {

    private val savedPracticeSessions = ArrayList<PracticeSession>()
    private val savedOpenings = ArrayList<Opening>()

    private val practiceLock = AtomicBoolean(false)
    private val openingLock = AtomicBoolean(false)

    init {
        try {
            loadData(context)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun arePracticeSessionsLocked() = practiceLock.get()

    private fun areOpeningsLocked() = openingLock.get()

    private fun obtainPracticeSessionLock() {
        while (arePracticeSessionsLocked()) {
            Thread.sleep(1)
        }

        lockPracticeSessions()
    }

    private fun obtainOpeningLock() {
        while (areOpeningsLocked()) {
            Thread.sleep(1)
        }

        lockOpenings()
    }

    private fun lockPracticeSessions() {
        practiceLock.set(true)
    }

    private fun unlockPracticeSessions() {
        practiceLock.set(false)
    }

    private fun lockOpenings() {
        openingLock.set(true)
    }

    private fun unlockOpenings() {
        openingLock.set(false)
    }

    fun getSavedOpenings(): ArrayList<Opening> {
        obtainOpeningLock()

        val openings = savedOpenings
        unlockOpenings()

        return openings
    }

    fun deleteOpening(name: String, team: Team, context: Context) {
        obtainOpeningLock()

        savedOpenings.removeIf { opening -> opening.name == name && opening.team == team }

        unlockOpenings()
        FileManager.delete("opening_${name}_${team}.txt")

        saveOpenings(context)
    }

    fun setPracticeSession(name: String, practiceSession: PracticeSession, context: Context) {
        obtainPracticeSessionLock()

        savedPracticeSessions.removeIf { session ->
            session.openingName == name
        }

        savedPracticeSessions += practiceSession
        unlockPracticeSessions()

        savePracticeSessions(context)
    }

    fun removePracticeSession(name: String, team: Team, context: Context) {
        obtainPracticeSessionLock()
        savedPracticeSessions.removeIf { session ->
            session.openingName == name && session.team == team
        }
        FileManager.delete("practice_session_${name}_$team.txt")
        unlockPracticeSessions()

        savePracticeSessions(context)
    }

    fun setOpening(name: String, team: Team, opening: Opening, context: Context) {
        obtainOpeningLock()

        savedOpenings.removeIf { storedOpening ->
            storedOpening.name == name && storedOpening.team == team
        }

        savedOpenings += opening
        unlockOpenings()

        saveOpenings(context)
    }

    fun getOpening(name: String, team: Team): Opening {
        obtainOpeningLock()

        val opening = savedOpenings.find { opening -> opening.name == name && opening.team == team }
        if (opening == null) {
            val newOpening = Opening(name, team)

            savedOpenings.add(newOpening)
            unlockOpenings()
            return newOpening
        }

        unlockOpenings()
        return opening
    }

    fun getPracticeSession(name: String, team: Team): PracticeSession? {
        obtainPracticeSessionLock()

        val session = savedPracticeSessions.find { session -> session.openingName == name && session.team == team }
        unlockPracticeSessions()
        return session
    }

    fun addOpening(opening: Opening, context: Context) {
        obtainOpeningLock()
        savedOpenings += opening
        unlockOpenings()

        saveOpenings(context)
    }

    fun isLocked() = areOpeningsLocked() || arePracticeSessionsLocked()

    fun loadData(context: Context) {
        Logger.warn(TAG, "LOADING DATA")

        loadPracticeSessions(context)
        loadSavedOpenings(context)
    }

    fun saveData(context: Context) {
        saveOpenings(context)
        savePracticeSessions(context)
    }

    private fun loadPracticeSessions(context: Context) {
        obtainPracticeSessionLock()
        Thread {
            try {
                val files = FileManager.listFilesInDirectory()

                val practiceFiles = files.filter { fileName -> fileName.startsWith("practice_session_") }

                savedPracticeSessions.clear()
                for (practiceFileName in practiceFiles) {
                    val fileContent = FileManager.readText(context, practiceFileName) ?: continue

                    val practiceSession = PracticeSession.fromString(fileContent)
                    savedPracticeSessions += practiceSession
                }
            } catch (e: Exception) {
                throw e
            } finally {
                unlockPracticeSessions()
            }
        }.start()
    }

    private fun loadSavedOpenings(context: Context) {
        obtainOpeningLock()
        Thread {
            try {
                savedOpenings.clear()
                val files = FileManager.listFilesInDirectory()

                Logger.debug(TAG, "${files.size}")

                val openingFiles = files.filter { fileName -> fileName.startsWith("opening_") }

                for (openingFileName in openingFiles) {
                    Logger.debug(TAG, "Found openingFile: $openingFileName")
                    val fileContent = FileManager.readText(context, openingFileName) ?: continue

//                    val openingInfo = openingFileName.removePrefix("opening_").removeSuffix(".txt").split("_")
//                    val openingName = openingInfo[0]
//                    val openingTeam = Team.fromString(openingInfo[1])
//                    val opening = Opening(openingName, openingTeam)
//                    opening.addFromString(fileContent)

                    val opening = Opening.fromString(fileContent)
                    savedOpenings += opening
                    Logger.debug(TAG, "Restoring savedOpening: ${opening.name}")
                }
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_loading_opening.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpenings()
            }
        }.start()
    }

//    fun saveOpening(name: String, team: Team, opening: Opening, context: Context) {
//        obtainOpeningLock()
//
//        savedOpenings.removeIf { storedOpening ->
//            storedOpening.name == name && storedOpening.team == team
//        }
//
//        savedOpenings += opening
//        unlockOpenings()
//    }

    fun savePracticeSessions(context: Context) {
        obtainPracticeSessionLock()
        Thread {
            try {
                for (practiceSession in savedPracticeSessions) {
                    Logger.debug(TAG, "Saving session: $practiceSession")
                    FileManager.write(context, "practice_session_${practiceSession.openingName}_${practiceSession.team}.txt", practiceSession.toString())
                }
            } catch (e: Exception) {

//                NetworkManager.getInstance().sendCrashReport("crash_practice_sessions_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockPracticeSessions()
            }
        }.start()
    }

    fun saveOpenings(context: Context) {
        obtainOpeningLock()
        Thread {
            try {
                for (opening in savedOpenings) {
                    Logger.debug(TAG, "Saving opening with name: opening_${opening.name}_${opening.team}.txt")
                    FileManager.write(context, "opening_${opening.name}_${opening.team}.txt", opening.toString())
                }
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_openings_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpenings()
            }

        }.start()

    }

    companion object {

        private const val TAG = "DataManager"

        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            if (instance == null) {
                instance = DataManager(context)
            }

            return instance!!
        }

    }

}
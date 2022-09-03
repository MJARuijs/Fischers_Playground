package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.util.FileManager

class StoreDataWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

//    private val savedGames = HashMap<String, MultiPlayerGame>()
    private lateinit var id: String
    private lateinit var dataManager: DataManager

    override fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences("user_data", Service.MODE_PRIVATE)
        id = preferences.getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!

        dataManager = DataManager.getInstance(applicationContext)

//        loadSavedGames()

        val topic = inputData.getString("topic")
        val data = inputData.getStringArray("data")!!

//        val dataList = data.split('|')

        var output: Any? = null
        if (topic == "move") {
            output = onOpponentMoved(data)
        }

//        val result = workDataOf(Pair("result", output))

        return Result.success()
    }

    private fun onOpponentMoved(data: Array<String>) {
        val gameId = data[0]
        val moveNotation = data[1]
        val move = Move.fromChessNotation(moveNotation)

        try {
            val game = dataManager.savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId..")
            game.moveOpponent(move, false)
            dataManager.savedGames[gameId] = game
            dataManager.saveGames()
        } catch (e: Exception) {
            FileManager.write(applicationContext, "crash_log.txt", e.stackTraceToString())
        }
    }

    private fun loadSavedGames() {
        val lines = FileManager.read(applicationContext, DataManagerService.MULTIPLAYER_GAME_FILE) ?: ArrayList()

        for (gameData in lines) {
            if (gameData.isBlank()) {
                continue
            }

            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
            val gameId = data[0]
            val lastUpdated = data[1].toLong()
            val opponentName = data[2]
            val isPlayerWhite = data[3].toBoolean()
            val gameStatus = GameStatus.fromString(data[4])
            val moveList = data[5].removePrefix("[").removeSuffix("]").split('\\')
            val chatMessages = data[6].removePrefix("[").removeSuffix("]").split('\\')
            val newsData = data[7].removePrefix("[").removeSuffix("]").split("\\")

//            val winner = data[7]

            val moves = ArrayList<Move>()

            for (move in moveList) {
                if (move.isNotBlank()) {
                    moves += Move.fromChessNotation(move)
                }
            }

            val messages = ArrayList<ChatMessage>()
            for (message in chatMessages) {
                if (message.isNotBlank()) {
                    val messageData = message.split(',')
                    val timeStamp = messageData[0]
                    val messageContent = messageData[1]
                    val type = MessageType.fromString(messageData[2])

                    messages += ChatMessage(timeStamp, messageContent, type)
                }
            }

            val newsUpdates = ArrayList<News>()
            for (news in newsData) {
                if (news.isBlank()) {
                    continue
                }

                newsUpdates += News.fromString(news)
            }

            val newGame = MultiPlayerGame(gameId, id, opponentName, isPlayerWhite, moves, messages, newsUpdates)
            newGame.status = gameStatus

            dataManager.savedGames[gameId] = newGame
        }
    }

    private fun saveGames() {
        var content = ""

        for ((gameId, game) in dataManager.savedGames) {
            var moveData = "["

            for ((i, move) in game.moves.withIndex()) {
                moveData += move.toChessNotation()
                if (i != game.moves.size - 1) {
                    moveData += "\\"
                }
            }
            moveData += "]"

            var chatData = "["

            for ((i, message) in game.chatMessages.withIndex()) {
                chatData += message.toString()
                if (i != game.chatMessages.size - 1) {
                    chatData += "\\"
                }
            }
            chatData += "]"

            var newsContent = "["

            for ((i, news) in game.newsUpdates.withIndex()) {
                newsContent += news.toString()
                if (i != game.newsUpdates.size - 1) {
                    newsContent += "\\"
                }
            }
            newsContent += "]"

            content += "$gameId|${game.lastUpdated}|${game.opponentName}|${game.isPlayingWhite}|${game.status}|$moveData|$chatData|$newsContent\n"
        }

        FileManager.write(applicationContext, DataManagerService.MULTIPLAYER_GAME_FILE, content)
    }

}
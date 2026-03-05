package com.chinesechess.game.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chinesechess.game.ai.ChessAI
import com.chinesechess.game.ai.Difficulty
import com.chinesechess.game.audio.SoundManager
import com.chinesechess.game.databinding.ActivityGameBinding
import com.chinesechess.game.engine.ChessBoard
import com.chinesechess.game.engine.ChessConstants
import com.chinesechess.game.engine.Move
import kotlinx.coroutines.*

class GameActivity : AppCompatActivity(), ChessBoardView.OnMoveListener {

    private lateinit var binding: ActivityGameBinding
    private lateinit var chessBoard: ChessBoard
    private lateinit var ai: ChessAI
    private lateinit var soundManager: SoundManager
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var vsAI = true
    private var aiThinking = false
    private var gameOver = false
    private var soundOn = true
    private var voiceOn = true
    private var undoCount = 0
    private val maxUndoPerGame = 5

    private val humorousLines = listOf(
        "哈哈，这步棋下得妙啊！",
        "看我这招，你可要小心了！",
        "嘿嘿，老夫出手，必有深意！",
        "别急，让我想想...好了，就这步！",
        "这步棋，我已经想了三秒钟！",
        "你以为我会走这步？没错！",
        "好棋好棋，虽然是我自己下的",
        "让你见识见识AI的厉害！",
        "我的炮可不是吃素的！",
        "这车开过来，请注意安全！",
        "马踏八方，谁与争锋！",
        "将军！惊不惊喜？",
        "这象走的田，比谁都大！",
        "兵过河就回不去了，跟时间一样",
        "吃你一子，不客气啦！",
        "我不是在下棋，是在表演！"
    )

    private val checkLines = listOf(
        "将军！你的老大危险了！",
        "将军！快来护驾！",
        "哎呀，你的将要凉了！",
        "将！防线有漏洞啊！"
    )

    private val captureLines = listOf(
        "吃掉一个，真香！",
        "这个子我收下了！",
        "嘿嘿，又吃了你一个！",
        "谢谢，这个子归我了！"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableFullScreen()
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vsAI = intent.getBooleanExtra("vs_ai", true)
        val diffName = intent.getStringExtra("difficulty") ?: Difficulty.MEDIUM.name
        val difficulty = try { Difficulty.valueOf(diffName) } catch (e: Exception) { Difficulty.MEDIUM }

        chessBoard = ChessBoard()
        chessBoard.init()
        ai = ChessAI(difficulty)

        soundManager = SoundManager(this)
        soundManager.init()

        binding.chessBoardView.listener = this
        binding.chessBoardView.updateBoard(chessBoard.board)

        if (!vsAI) {
            binding.tvBlackName.text = "玩家二 (黑方)"
        }

        setupButtons()
        updateStatus()
    }

    private fun enableFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupButtons() {
        binding.btnUndo.setOnClickListener { onUndo() }
        binding.btnHint.setOnClickListener { onHint() }
        binding.btnNewGame.setOnClickListener { onNewGame() }
        binding.btnSound.setOnClickListener { toggleSound() }
    }

    override fun onPieceSelected(row: Int, col: Int) {
        if (gameOver || aiThinking) return
        val piece = chessBoard.getPiece(row, col)
        if (piece == ChessConstants.EMPTY) return

        val isCurrentPlayerPiece = if (chessBoard.isRedTurn) {
            ChessConstants.isRed(piece)
        } else {
            ChessConstants.isBlack(piece)
        }

        if (!isCurrentPlayerPiece) return

        // In AI mode, only allow red pieces
        if (vsAI && !ChessConstants.isRed(piece)) return

        soundManager.playSelectSound()
        val moves = chessBoard.generateAllMoves(chessBoard.isRedTurn)
            .filter { it.fromRow == row && it.fromCol == col }
        binding.chessBoardView.setSelection(row, col, moves)
    }

    override fun onMoveMade(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        if (gameOver || aiThinking) return

        val piece = chessBoard.getPiece(fromRow, fromCol)
        val target = chessBoard.getPiece(toRow, toCol)

        // If clicking on own piece, switch selection
        if (target != ChessConstants.EMPTY && ChessConstants.sameSide(piece, target)) {
            onPieceSelected(toRow, toCol)
            return
        }

        // Validate move
        val validMoves = chessBoard.generateAllMoves(chessBoard.isRedTurn)
        val move = validMoves.find {
            it.fromRow == fromRow && it.fromCol == fromCol && it.toRow == toRow && it.toCol == toCol
        } ?: return

        executeMove(move)

        if (!gameOver && vsAI && !chessBoard.isRedTurn) {
            aiThinking = true
            updateStatus()
            scope.launch {
                delay(300) // Small delay for natural feel
                val aiMove = withContext(Dispatchers.Default) {
                    ai.findBestMove(chessBoard, false)
                }
                aiThinking = false
                if (aiMove != null) {
                    executeAIMove(aiMove)
                }
            }
        }
    }

    private fun executeMove(move: Move) {
        chessBoard.makeMove(move)
        binding.chessBoardView.updateBoard(chessBoard.board)
        binding.chessBoardView.lastMove = move
        binding.chessBoardView.clearSelection()

        // Play sound
        if (move.captured != ChessConstants.EMPTY) {
            soundManager.playCaptureSound()
        } else {
            soundManager.playMoveSound()
        }

        // Check for check/checkmate
        val isRedTurn = chessBoard.isRedTurn
        if (chessBoard.isCheckmate(isRedTurn)) {
            gameOver = true
            soundManager.playWinSound()
            val winner = if (isRedTurn) "黑方" else "红方"
            showGameOverDialog("$winner 胜！")
        } else if (chessBoard.hasNoMoves(isRedTurn)) {
            gameOver = true
            showGameOverDialog("和棋！")
        } else if (chessBoard.isInCheck(isRedTurn)) {
            soundManager.playCheckSound()
        }

        updateStatus()
    }

    private fun executeAIMove(move: Move) {
        chessBoard.makeMove(move)
        binding.chessBoardView.updateBoard(chessBoard.board)
        binding.chessBoardView.lastMove = move
        binding.chessBoardView.clearSelection()

        // Play sound
        if (move.captured != ChessConstants.EMPTY) {
            soundManager.playCaptureSound()
        } else {
            soundManager.playMoveSound()
        }

        // Show AI speech bubble
        val isCheck = chessBoard.isInCheck(true)
        val speechLine = when {
            isCheck -> checkLines.random()
            move.captured != ChessConstants.EMPTY -> captureLines.random()
            else -> humorousLines.random()
        }
        showAISpeech(speechLine)
        soundManager.speakAIMove(move, isCheck)

        // Check game state
        if (chessBoard.isCheckmate(true)) {
            gameOver = true
            soundManager.playWinSound()
            showGameOverDialog("黑方胜！电脑赢了！")
        } else if (chessBoard.hasNoMoves(true)) {
            gameOver = true
            showGameOverDialog("和棋！")
        } else if (isCheck) {
            soundManager.playCheckSound()
        }

        updateStatus()
    }

    private fun showAISpeech(text: String) {
        binding.tvAiSpeech.text = "🤖 $text"
        binding.tvAiSpeech.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages("speech")
        handler.postDelayed({
            binding.tvAiSpeech.visibility = View.GONE
        }, 4000)
    }

    private fun updateStatus() {
        val status = when {
            gameOver -> "游戏结束"
            aiThinking -> "电脑思考中..."
            chessBoard.isRedTurn -> {
                if (chessBoard.isInCheck(true)) "红方被将军！" else "红方走棋"
            }
            else -> {
                if (vsAI) "电脑思考中..." else {
                    if (chessBoard.isInCheck(false)) "黑方被将军！" else "黑方走棋"
                }
            }
        }
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(
            if (chessBoard.isRedTurn || gameOver) 0xFFCC3333.toInt() else 0xFF1A1A1A.toInt()
        )
    }

    private fun onUndo() {
        if (gameOver || aiThinking) return
        if (chessBoard.moveHistory.isEmpty()) {
            Toast.makeText(this, "没有可以悔的棋", Toast.LENGTH_SHORT).show()
            return
        }
        if (undoCount >= maxUndoPerGame) {
            Toast.makeText(this, "悔棋次数已用完（最多${maxUndoPerGame}次）", Toast.LENGTH_SHORT).show()
            return
        }

        if (vsAI) {
            // Undo both AI and player move
            if (chessBoard.moveHistory.size >= 2) {
                chessBoard.undoMove() // AI's move
                chessBoard.undoMove() // Player's move
            } else {
                chessBoard.undoMove()
            }
        } else {
            chessBoard.undoMove()
        }

        undoCount++
        binding.chessBoardView.updateBoard(chessBoard.board)
        binding.chessBoardView.clearSelection()
        binding.chessBoardView.lastMove = chessBoard.moveHistory.lastOrNull()
        updateStatus()
        Toast.makeText(this, "已悔棋（剩余${maxUndoPerGame - undoCount}次）", Toast.LENGTH_SHORT).show()
    }

    private fun onHint() {
        if (gameOver || aiThinking) return
        if (!chessBoard.isRedTurn && vsAI) return

        Toast.makeText(this, "正在分析最佳走法...", Toast.LENGTH_SHORT).show()
        scope.launch {
            val hintAI = ChessAI(Difficulty.MEDIUM)
            val hintMove = withContext(Dispatchers.Default) {
                hintAI.findBestMove(chessBoard, chessBoard.isRedTurn)
            }
            if (hintMove != null) {
                binding.chessBoardView.hintMove = hintMove
                binding.chessBoardView.invalidate()
                handler.postDelayed({
                    binding.chessBoardView.hintMove = null
                    binding.chessBoardView.invalidate()
                }, 3000)
            } else {
                Toast.makeText(this@GameActivity, "无法找到提示", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onNewGame() {
        AlertDialog.Builder(this)
            .setTitle("新局")
            .setMessage("确定要开始新的一局吗？")
            .setPositiveButton("确定") { _, _ ->
                chessBoard.init()
                gameOver = false
                aiThinking = false
                undoCount = 0
                binding.chessBoardView.updateBoard(chessBoard.board)
                binding.chessBoardView.clearSelection()
                binding.chessBoardView.lastMove = null
                binding.tvAiSpeech.visibility = View.GONE
                updateStatus()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleSound() {
        soundOn = !soundOn
        soundManager.soundEnabled = soundOn
        soundManager.voiceEnabled = soundOn
        binding.btnSound.text = if (soundOn) "音效" else "静音"
        Toast.makeText(this, if (soundOn) "音效已开启" else "音效已关闭", Toast.LENGTH_SHORT).show()
    }

    private fun showGameOverDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("游戏结束")
            .setMessage(message)
            .setPositiveButton("再来一局") { _, _ ->
                chessBoard.init()
                gameOver = false
                undoCount = 0
                binding.chessBoardView.updateBoard(chessBoard.board)
                binding.chessBoardView.clearSelection()
                binding.chessBoardView.lastMove = null
                binding.tvAiSpeech.visibility = View.GONE
                updateStatus()
            }
            .setNegativeButton("返回主菜单") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        ai.cancel()
        soundManager.release()
    }

    override fun onPause() {
        super.onPause()
        ai.cancel()
    }

    override fun onResume() {
        super.onResume()
        enableFullScreen()
    }
}

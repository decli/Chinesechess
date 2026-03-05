package com.chinesechess.game.ai

import com.chinesechess.game.engine.ChessBoard
import com.chinesechess.game.engine.ChessConstants
import com.chinesechess.game.engine.ChessConstants.BLACK_ADVISOR
import com.chinesechess.game.engine.ChessConstants.BLACK_BISHOP
import com.chinesechess.game.engine.ChessConstants.BLACK_CANNON
import com.chinesechess.game.engine.ChessConstants.BLACK_KING
import com.chinesechess.game.engine.ChessConstants.BLACK_KNIGHT
import com.chinesechess.game.engine.ChessConstants.BLACK_PAWN
import com.chinesechess.game.engine.ChessConstants.BLACK_ROOK
import com.chinesechess.game.engine.ChessConstants.BOARD_COLS
import com.chinesechess.game.engine.ChessConstants.BOARD_ROWS
import com.chinesechess.game.engine.ChessConstants.EMPTY
import com.chinesechess.game.engine.ChessConstants.RED_ADVISOR
import com.chinesechess.game.engine.ChessConstants.RED_BISHOP
import com.chinesechess.game.engine.ChessConstants.RED_CANNON
import com.chinesechess.game.engine.ChessConstants.RED_KING
import com.chinesechess.game.engine.ChessConstants.RED_KNIGHT
import com.chinesechess.game.engine.ChessConstants.RED_PAWN
import com.chinesechess.game.engine.ChessConstants.RED_ROOK
import com.chinesechess.game.engine.Move

enum class Difficulty(val searchDepth: Int, val timeLimit: Long) {
    EASY(2, 1000L),
    MEDIUM(4, 3000L),
    HARD(6, 5000L)
}

class ChessAI(var difficulty: Difficulty = Difficulty.MEDIUM) {

    @Volatile
    private var searchCancelled = false
    private var bestMoveFound: Move? = null
    private var nodesSearched = 0L
    private var searchStartTime = 0L

    // Piece-square tables for positional evaluation
    private val pawnPositionRed = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(90, 90, 110, 120, 120, 120, 110, 90, 90),
        intArrayOf(90, 90, 110, 120, 120, 120, 110, 90, 90),
        intArrayOf(70, 90, 110, 110, 110, 110, 110, 90, 70),
        intArrayOf(70, 70, 70, 70, 70, 70, 70, 70, 70),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val knightPosition = arrayOf(
        intArrayOf(220, 240, 280, 280, 280, 280, 280, 240, 220),
        intArrayOf(240, 300, 320, 320, 340, 320, 320, 300, 240),
        intArrayOf(280, 320, 340, 340, 340, 340, 340, 320, 280),
        intArrayOf(280, 320, 340, 360, 360, 360, 340, 320, 280),
        intArrayOf(280, 320, 340, 360, 360, 360, 340, 320, 280),
        intArrayOf(280, 320, 340, 360, 360, 360, 340, 320, 280),
        intArrayOf(280, 320, 340, 340, 340, 340, 340, 320, 280),
        intArrayOf(240, 300, 320, 320, 340, 320, 320, 300, 240),
        intArrayOf(220, 240, 280, 280, 280, 280, 280, 240, 220),
        intArrayOf(200, 220, 260, 260, 260, 260, 260, 220, 200)
    )

    private val rookPosition = arrayOf(
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600),
        intArrayOf(600, 620, 620, 630, 630, 630, 620, 620, 600)
    )

    private val cannonPosition = arrayOf(
        intArrayOf(280, 300, 300, 300, 300, 300, 300, 300, 280),
        intArrayOf(280, 300, 300, 310, 310, 310, 300, 300, 280),
        intArrayOf(280, 300, 310, 310, 310, 310, 310, 300, 280),
        intArrayOf(280, 300, 310, 320, 320, 320, 310, 300, 280),
        intArrayOf(280, 300, 310, 320, 320, 320, 310, 300, 280),
        intArrayOf(280, 300, 310, 320, 320, 320, 310, 300, 280),
        intArrayOf(280, 300, 310, 310, 310, 310, 310, 300, 280),
        intArrayOf(280, 300, 300, 310, 310, 310, 300, 300, 280),
        intArrayOf(280, 300, 300, 300, 300, 300, 300, 300, 280),
        intArrayOf(280, 300, 300, 300, 300, 300, 300, 300, 280)
    )

    fun cancel() {
        searchCancelled = true
    }

    fun findBestMove(board: ChessBoard, isRedSide: Boolean): Move? {
        searchCancelled = false
        bestMoveFound = null
        nodesSearched = 0
        searchStartTime = System.currentTimeMillis()

        // Iterative deepening with time limit
        val maxDepth = difficulty.searchDepth
        var lastBest: Move? = null

        for (depth in 1..maxDepth) {
            if (searchCancelled) break
            if (System.currentTimeMillis() - searchStartTime > difficulty.timeLimit) break

            val moves = board.generateAllMoves(isRedSide)
            if (moves.isEmpty()) return null
            if (moves.size == 1) return moves[0]

            // Sort moves for better pruning (captures first, then by history)
            val sortedMoves = sortMoves(moves, board, isRedSide)

            var bestScore = Int.MIN_VALUE
            var bestMove = sortedMoves[0]

            for (move in sortedMoves) {
                if (searchCancelled) break
                if (System.currentTimeMillis() - searchStartTime > difficulty.timeLimit) break

                board.makeMove(move)
                val score = -alphaBeta(board, depth - 1, Int.MIN_VALUE + 1, -bestScore, !isRedSide)
                board.undoMove()

                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }

            if (!searchCancelled && System.currentTimeMillis() - searchStartTime <= difficulty.timeLimit) {
                lastBest = bestMove
                bestMoveFound = bestMove
            }
        }

        return lastBest ?: bestMoveFound
    }

    private fun alphaBeta(board: ChessBoard, depth: Int, alpha: Int, beta: Int, isRedSide: Boolean): Int {
        nodesSearched++

        // Time check every 4096 nodes
        if (nodesSearched and 4095 == 0L) {
            if (System.currentTimeMillis() - searchStartTime > difficulty.timeLimit) {
                searchCancelled = true
                return 0
            }
        }
        if (searchCancelled) return 0

        if (depth <= 0) {
            return evaluate(board, isRedSide)
        }

        val moves = board.generateAllMoves(isRedSide)
        if (moves.isEmpty()) {
            return if (board.isInCheck(isRedSide)) -99999 + (difficulty.searchDepth - depth) else 0
        }

        val sortedMoves = sortMoves(moves, board, isRedSide)
        var currentAlpha = alpha

        for (move in sortedMoves) {
            if (searchCancelled) return 0

            board.makeMove(move)
            val score = -alphaBeta(board, depth - 1, -beta, -currentAlpha, !isRedSide)
            board.undoMove()

            if (score >= beta) return beta
            if (score > currentAlpha) currentAlpha = score
        }

        return currentAlpha
    }

    private fun sortMoves(moves: List<Move>, board: ChessBoard, isRedSide: Boolean): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            // Captures are prioritized by MVV-LVA
            if (move.captured != EMPTY) {
                score += ChessConstants.pieceValue(move.captured) * 10 - ChessConstants.pieceValue(move.piece)
            }
            // Check moves are good
            board.makeMove(move)
            if (board.isInCheck(!isRedSide)) score += 500
            board.undoMove()
            score
        }
    }

    private fun evaluate(board: ChessBoard, isRedSide: Boolean): Int {
        var score = 0
        for (r in 0 until BOARD_ROWS) {
            for (c in 0 until BOARD_COLS) {
                val piece = board.board[r][c]
                if (piece == EMPTY) continue
                val posScore = getPositionScore(piece, r, c)
                if (ChessConstants.isRed(piece)) {
                    score += posScore
                } else {
                    score -= posScore
                }
            }
        }

        // Mobility bonus
        val redMoves = board.generateAllMoves(true).size
        val blackMoves = board.generateAllMoves(false).size
        score += (redMoves - blackMoves) * 5

        // King safety
        if (board.kingsOpposing()) {
            score += if (isRedSide) -9000 else 9000
        }

        return if (isRedSide) score else -score
    }

    private fun getPositionScore(piece: Int, row: Int, col: Int): Int {
        return when (piece) {
            RED_KING -> 10000
            BLACK_KING -> 10000
            RED_PAWN -> pawnPositionRed[row][col]
            BLACK_PAWN -> pawnPositionRed[9 - row][8 - col]
            RED_KNIGHT -> knightPosition[row][col]
            BLACK_KNIGHT -> knightPosition[9 - row][8 - col]
            RED_ROOK -> rookPosition[row][col]
            BLACK_ROOK -> rookPosition[9 - row][8 - col]
            RED_CANNON -> cannonPosition[row][col]
            BLACK_CANNON -> cannonPosition[9 - row][8 - col]
            RED_ADVISOR -> 120
            BLACK_ADVISOR -> 120
            RED_BISHOP -> 120
            BLACK_BISHOP -> 120
            else -> 0
        }
    }
}

package com.chinesechess.game.engine

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
import kotlin.math.abs

object MoveValidator {

    fun isValidMove(board: Array<IntArray>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (fromRow !in 0 until BOARD_ROWS || fromCol !in 0 until BOARD_COLS) return false
        if (toRow !in 0 until BOARD_ROWS || toCol !in 0 until BOARD_COLS) return false
        if (fromRow == toRow && fromCol == toCol) return false

        val piece = board[fromRow][fromCol]
        val target = board[toRow][toCol]
        if (piece == EMPTY) return false
        if (target != EMPTY && ChessConstants.sameSide(piece, target)) return false

        return when (piece) {
            RED_KING, BLACK_KING -> isValidKingMove(board, piece, fromRow, fromCol, toRow, toCol)
            RED_ADVISOR, BLACK_ADVISOR -> isValidAdvisorMove(piece, fromRow, fromCol, toRow, toCol)
            RED_BISHOP, BLACK_BISHOP -> isValidBishopMove(board, piece, fromRow, fromCol, toRow, toCol)
            RED_KNIGHT, BLACK_KNIGHT -> isValidKnightMove(board, fromRow, fromCol, toRow, toCol)
            RED_ROOK, BLACK_ROOK -> isValidRookMove(board, fromRow, fromCol, toRow, toCol)
            RED_CANNON, BLACK_CANNON -> isValidCannonMove(board, fromRow, fromCol, toRow, toCol)
            RED_PAWN, BLACK_PAWN -> isValidPawnMove(piece, fromRow, fromCol, toRow, toCol)
            else -> false
        }
    }

    private fun isValidKingMove(board: Array<IntArray>, piece: Int, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val dr = abs(toRow - fromRow)
        val dc = abs(toCol - fromCol)

        // King face-to-face capture
        if (dc == 0 && dr > 1) {
            val otherKing = board[toRow][toCol]
            if ((piece == RED_KING && otherKing == BLACK_KING) || (piece == BLACK_KING && otherKing == RED_KING)) {
                val minR = minOf(fromRow, toRow)
                val maxR = maxOf(fromRow, toRow)
                for (r in (minR + 1) until maxR) {
                    if (board[r][fromCol] != EMPTY) return false
                }
                return true
            }
        }

        if (dr + dc != 1) return false
        if (toCol !in 3..5) return false
        return if (ChessConstants.isRed(piece)) toRow in 7..9 else toRow in 0..2
    }

    private fun isValidAdvisorMove(piece: Int, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (abs(toRow - fromRow) != 1 || abs(toCol - fromCol) != 1) return false
        if (toCol !in 3..5) return false
        return if (ChessConstants.isRed(piece)) toRow in 7..9 else toRow in 0..2
    }

    private fun isValidBishopMove(board: Array<IntArray>, piece: Int, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (abs(toRow - fromRow) != 2 || abs(toCol - fromCol) != 2) return false
        // Cannot cross river
        if (ChessConstants.isRed(piece) && toRow < 5) return false
        if (ChessConstants.isBlack(piece) && toRow > 4) return false
        // Check blocking piece (elephant eye)
        val midRow = (fromRow + toRow) / 2
        val midCol = (fromCol + toCol) / 2
        return board[midRow][midCol] == EMPTY
    }

    private fun isValidKnightMove(board: Array<IntArray>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val dr = abs(toRow - fromRow)
        val dc = abs(toCol - fromCol)
        if (!((dr == 2 && dc == 1) || (dr == 1 && dc == 2))) return false
        // Check blocking piece (horse leg)
        return if (dr == 2) {
            val blockRow = fromRow + (toRow - fromRow) / 2
            board[blockRow][fromCol] == EMPTY
        } else {
            val blockCol = fromCol + (toCol - fromCol) / 2
            board[fromRow][blockCol] == EMPTY
        }
    }

    private fun isValidRookMove(board: Array<IntArray>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (fromRow != toRow && fromCol != toCol) return false
        return countBetween(board, fromRow, fromCol, toRow, toCol) == 0
    }

    private fun isValidCannonMove(board: Array<IntArray>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (fromRow != toRow && fromCol != toCol) return false
        val between = countBetween(board, fromRow, fromCol, toRow, toCol)
        return if (board[toRow][toCol] == EMPTY) between == 0 else between == 1
    }

    private fun isValidPawnMove(piece: Int, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val dr = toRow - fromRow
        val dc = abs(toCol - fromCol)
        if (ChessConstants.isRed(piece)) {
            // Red pawn moves upward (decreasing row)
            if (fromRow > 4) {
                // Before crossing river: only forward
                return dr == -1 && dc == 0
            } else {
                // After crossing river: forward or sideways
                return (dr == -1 && dc == 0) || (dr == 0 && dc == 1)
            }
        } else {
            // Black pawn moves downward (increasing row)
            if (fromRow < 5) {
                return dr == 1 && dc == 0
            } else {
                return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
            }
        }
    }

    private fun countBetween(board: Array<IntArray>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Int {
        var count = 0
        if (fromRow == toRow) {
            val minC = minOf(fromCol, toCol) + 1
            val maxC = maxOf(fromCol, toCol)
            for (c in minC until maxC) {
                if (board[fromRow][c] != EMPTY) count++
            }
        } else {
            val minR = minOf(fromRow, toRow) + 1
            val maxR = maxOf(fromRow, toRow)
            for (r in minR until maxR) {
                if (board[r][fromCol] != EMPTY) count++
            }
        }
        return count
    }
}

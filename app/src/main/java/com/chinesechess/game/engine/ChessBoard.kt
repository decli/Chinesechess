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

class ChessBoard {
    val board = Array(BOARD_ROWS) { IntArray(BOARD_COLS) }
    var isRedTurn = true
    val moveHistory = mutableListOf<Move>()

    fun init() {
        for (r in 0 until BOARD_ROWS) board[r].fill(EMPTY)
        // Black pieces (top, rows 0-4)
        board[0][0] = BLACK_ROOK;   board[0][8] = BLACK_ROOK
        board[0][1] = BLACK_KNIGHT; board[0][7] = BLACK_KNIGHT
        board[0][2] = BLACK_BISHOP; board[0][6] = BLACK_BISHOP
        board[0][3] = BLACK_ADVISOR; board[0][5] = BLACK_ADVISOR
        board[0][4] = BLACK_KING
        board[2][1] = BLACK_CANNON; board[2][7] = BLACK_CANNON
        board[3][0] = BLACK_PAWN; board[3][2] = BLACK_PAWN
        board[3][4] = BLACK_PAWN; board[3][6] = BLACK_PAWN; board[3][8] = BLACK_PAWN

        // Red pieces (bottom, rows 5-9)
        board[9][0] = RED_ROOK;   board[9][8] = RED_ROOK
        board[9][1] = RED_KNIGHT; board[9][7] = RED_KNIGHT
        board[9][2] = RED_BISHOP; board[9][6] = RED_BISHOP
        board[9][3] = RED_ADVISOR; board[9][5] = RED_ADVISOR
        board[9][4] = RED_KING
        board[7][1] = RED_CANNON; board[7][7] = RED_CANNON
        board[6][0] = RED_PAWN; board[6][2] = RED_PAWN
        board[6][4] = RED_PAWN; board[6][6] = RED_PAWN; board[6][8] = RED_PAWN

        isRedTurn = true
        moveHistory.clear()
    }

    fun getPiece(row: Int, col: Int): Int {
        if (row !in 0 until BOARD_ROWS || col !in 0 until BOARD_COLS) return -1
        return board[row][col]
    }

    fun makeMove(move: Move): Boolean {
        board[move.toRow][move.toCol] = board[move.fromRow][move.fromCol]
        board[move.fromRow][move.fromCol] = EMPTY
        moveHistory.add(move)
        isRedTurn = !isRedTurn
        return true
    }

    fun undoMove(): Move? {
        if (moveHistory.isEmpty()) return null
        val move = moveHistory.removeAt(moveHistory.size - 1)
        board[move.fromRow][move.fromCol] = move.piece
        board[move.toRow][move.toCol] = move.captured
        isRedTurn = !isRedTurn
        return move
    }

    fun copyBoard(): Array<IntArray> {
        return Array(BOARD_ROWS) { board[it].copyOf() }
    }

    fun isInCheck(redSide: Boolean): Boolean {
        var kingRow = -1; var kingCol = -1
        val kingPiece = if (redSide) RED_KING else BLACK_KING
        for (r in 0 until BOARD_ROWS) {
            for (c in 0 until BOARD_COLS) {
                if (board[r][c] == kingPiece) {
                    kingRow = r; kingCol = c; break
                }
            }
            if (kingRow >= 0) break
        }
        if (kingRow < 0) return true

        // Check if any opponent piece can capture the king
        for (r in 0 until BOARD_ROWS) {
            for (c in 0 until BOARD_COLS) {
                val p = board[r][c]
                if (p == EMPTY) continue
                if (redSide && ChessConstants.isRed(p)) continue
                if (!redSide && ChessConstants.isBlack(p)) continue
                if (MoveValidator.isValidMove(board, r, c, kingRow, kingCol)) return true
            }
        }
        return false
    }

    fun isCheckmate(redSide: Boolean): Boolean {
        if (!isInCheck(redSide)) return false
        return generateAllMoves(redSide).isEmpty()
    }

    fun hasNoMoves(redSide: Boolean): Boolean {
        return generateAllMoves(redSide).isEmpty()
    }

    fun generateAllMoves(redSide: Boolean): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0 until BOARD_ROWS) {
            for (c in 0 until BOARD_COLS) {
                val piece = board[r][c]
                if (piece == EMPTY) continue
                if (redSide && !ChessConstants.isRed(piece)) continue
                if (!redSide && !ChessConstants.isBlack(piece)) continue
                val pieceMoves = MoveGenerator.generateMoves(board, r, c)
                for (m in pieceMoves) {
                    // Verify move doesn't leave own king in check
                    val captured = board[m.toRow][m.toCol]
                    board[m.toRow][m.toCol] = piece
                    board[r][c] = EMPTY
                    val inCheck = isInCheck(redSide)
                    board[r][c] = piece
                    board[m.toRow][m.toCol] = captured
                    if (!inCheck) {
                        moves.add(Move(r, c, m.toRow, m.toCol, captured, piece))
                    }
                }
            }
        }
        return moves
    }

    fun kingsOpposing(): Boolean {
        var redKingRow = -1; var redKingCol = -1
        var blackKingRow = -1; var blackKingCol = -1
        for (r in 0 until BOARD_ROWS) {
            for (c in 0 until BOARD_COLS) {
                when (board[r][c]) {
                    RED_KING -> { redKingRow = r; redKingCol = c }
                    BLACK_KING -> { blackKingRow = r; blackKingCol = c }
                }
            }
        }
        if (redKingCol != blackKingCol) return false
        for (r in (blackKingRow + 1) until redKingRow) {
            if (board[r][redKingCol] != EMPTY) return false
        }
        return true
    }
}

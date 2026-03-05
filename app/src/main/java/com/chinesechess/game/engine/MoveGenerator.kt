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

object MoveGenerator {

    fun generateMoves(board: Array<IntArray>, row: Int, col: Int): List<Move> {
        val piece = board[row][col]
        if (piece == EMPTY) return emptyList()
        return when (piece) {
            RED_KING, BLACK_KING -> generateKingMoves(board, row, col, piece)
            RED_ADVISOR, BLACK_ADVISOR -> generateAdvisorMoves(board, row, col, piece)
            RED_BISHOP, BLACK_BISHOP -> generateBishopMoves(board, row, col, piece)
            RED_KNIGHT, BLACK_KNIGHT -> generateKnightMoves(board, row, col, piece)
            RED_ROOK, BLACK_ROOK -> generateRookMoves(board, row, col, piece)
            RED_CANNON, BLACK_CANNON -> generateCannonMoves(board, row, col, piece)
            RED_PAWN, BLACK_PAWN -> generatePawnMoves(board, row, col, piece)
            else -> emptyList()
        }
    }

    private fun generateKingMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        val dirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (d in dirs) {
            val nr = row + d[0]; val nc = col + d[1]
            if (nc !in 3..5) continue
            val rowOk = if (ChessConstants.isRed(piece)) nr in 7..9 else nr in 0..2
            if (!rowOk) continue
            if (board[nr][nc] != EMPTY && ChessConstants.sameSide(piece, board[nr][nc])) continue
            moves.add(Move(row, col, nr, nc, board[nr][nc], piece))
        }
        // King face-to-face
        val otherKing = if (ChessConstants.isRed(piece)) BLACK_KING else RED_KING
        val dir = if (ChessConstants.isRed(piece)) -1 else 1
        var r = row + dir
        while (r in 0 until BOARD_ROWS) {
            if (board[r][col] != EMPTY) {
                if (board[r][col] == otherKing) {
                    moves.add(Move(row, col, r, col, otherKing, piece))
                }
                break
            }
            r += dir
        }
        return moves
    }

    private fun generateAdvisorMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        val dirs = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        for (d in dirs) {
            val nr = row + d[0]; val nc = col + d[1]
            if (nc !in 3..5) continue
            val rowOk = if (ChessConstants.isRed(piece)) nr in 7..9 else nr in 0..2
            if (!rowOk) continue
            if (board[nr][nc] != EMPTY && ChessConstants.sameSide(piece, board[nr][nc])) continue
            moves.add(Move(row, col, nr, nc, board[nr][nc], piece))
        }
        return moves
    }

    private fun generateBishopMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        val dirs = arrayOf(intArrayOf(-2, -2), intArrayOf(-2, 2), intArrayOf(2, -2), intArrayOf(2, 2))
        for (d in dirs) {
            val nr = row + d[0]; val nc = col + d[1]
            if (nr !in 0 until BOARD_ROWS || nc !in 0 until BOARD_COLS) continue
            if (ChessConstants.isRed(piece) && nr < 5) continue
            if (ChessConstants.isBlack(piece) && nr > 4) continue
            val mr = (row + nr) / 2; val mc = (col + nc) / 2
            if (board[mr][mc] != EMPTY) continue
            if (board[nr][nc] != EMPTY && ChessConstants.sameSide(piece, board[nr][nc])) continue
            moves.add(Move(row, col, nr, nc, board[nr][nc], piece))
        }
        return moves
    }

    private fun generateKnightMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        val jumps = arrayOf(
            intArrayOf(-2, -1, -1, 0), intArrayOf(-2, 1, -1, 0),
            intArrayOf(2, -1, 1, 0), intArrayOf(2, 1, 1, 0),
            intArrayOf(-1, -2, 0, -1), intArrayOf(-1, 2, 0, 1),
            intArrayOf(1, -2, 0, -1), intArrayOf(1, 2, 0, 1)
        )
        for (j in jumps) {
            val nr = row + j[0]; val nc = col + j[1]
            if (nr !in 0 until BOARD_ROWS || nc !in 0 until BOARD_COLS) continue
            val br = row + j[2]; val bc = col + j[3]
            if (board[br][bc] != EMPTY) continue
            if (board[nr][nc] != EMPTY && ChessConstants.sameSide(piece, board[nr][nc])) continue
            moves.add(Move(row, col, nr, nc, board[nr][nc], piece))
        }
        return moves
    }

    private fun generateRookMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        val dirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (d in dirs) {
            var nr = row + d[0]; var nc = col + d[1]
            while (nr in 0 until BOARD_ROWS && nc in 0 until BOARD_COLS) {
                if (board[nr][nc] == EMPTY) {
                    moves.add(Move(row, col, nr, nc, EMPTY, piece))
                } else {
                    if (!ChessConstants.sameSide(piece, board[nr][nc])) {
                        moves.add(Move(row, col, nr, nc, board[nr][nc], piece))
                    }
                    break
                }
                nr += d[0]; nc += d[1]
            }
        }
        return moves
    }

    private fun generateCannonMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        val dirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (d in dirs) {
            var nr = row + d[0]; var nc = col + d[1]
            var jumped = false
            while (nr in 0 until BOARD_ROWS && nc in 0 until BOARD_COLS) {
                if (!jumped) {
                    if (board[nr][nc] == EMPTY) {
                        moves.add(Move(row, col, nr, nc, EMPTY, piece))
                    } else {
                        jumped = true
                    }
                } else {
                    if (board[nr][nc] != EMPTY) {
                        if (!ChessConstants.sameSide(piece, board[nr][nc])) {
                            moves.add(Move(row, col, nr, nc, board[nr][nc], piece))
                        }
                        break
                    }
                }
                nr += d[0]; nc += d[1]
            }
        }
        return moves
    }

    private fun generatePawnMoves(board: Array<IntArray>, row: Int, col: Int, piece: Int): List<Move> {
        val moves = mutableListOf<Move>()
        if (ChessConstants.isRed(piece)) {
            // Forward (upward)
            if (row - 1 >= 0) tryAdd(board, moves, row, col, row - 1, col, piece)
            // After crossing river, can move sideways
            if (row <= 4) {
                if (col - 1 >= 0) tryAdd(board, moves, row, col, row, col - 1, piece)
                if (col + 1 < BOARD_COLS) tryAdd(board, moves, row, col, row, col + 1, piece)
            }
        } else {
            if (row + 1 < BOARD_ROWS) tryAdd(board, moves, row, col, row + 1, col, piece)
            if (row >= 5) {
                if (col - 1 >= 0) tryAdd(board, moves, row, col, row, col - 1, piece)
                if (col + 1 < BOARD_COLS) tryAdd(board, moves, row, col, row, col + 1, piece)
            }
        }
        return moves
    }

    private fun tryAdd(board: Array<IntArray>, moves: MutableList<Move>, fr: Int, fc: Int, tr: Int, tc: Int, piece: Int) {
        if (board[tr][tc] != EMPTY && ChessConstants.sameSide(piece, board[tr][tc])) return
        moves.add(Move(fr, fc, tr, tc, board[tr][tc], piece))
    }
}

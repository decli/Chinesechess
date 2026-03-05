package com.chinesechess.game.engine

/**
 * 中国象棋常量定义
 */
object ChessConstants {
    const val BOARD_ROWS = 10
    const val BOARD_COLS = 9

    // 棋子类型
    const val EMPTY = 0
    const val RED_KING = 1      // 帅
    const val RED_ADVISOR = 2   // 仕
    const val RED_BISHOP = 3    // 相
    const val RED_KNIGHT = 4    // 马
    const val RED_ROOK = 5      // 车
    const val RED_CANNON = 6    // 炮
    const val RED_PAWN = 7      // 兵

    const val BLACK_KING = 8    // 将
    const val BLACK_ADVISOR = 9 // 士
    const val BLACK_BISHOP = 10 // 象
    const val BLACK_KNIGHT = 11 // 马
    const val BLACK_ROOK = 12   // 车
    const val BLACK_CANNON = 13 // 炮
    const val BLACK_PAWN = 14   // 卒

    fun isRed(piece: Int): Boolean = piece in RED_KING..RED_PAWN
    fun isBlack(piece: Int): Boolean = piece in BLACK_KING..BLACK_PAWN
    fun isEmpty(piece: Int): Boolean = piece == EMPTY

    fun sameSide(a: Int, b: Int): Boolean {
        if (a == EMPTY || b == EMPTY) return false
        return (isRed(a) && isRed(b)) || (isBlack(a) && isBlack(b))
    }

    fun pieceName(piece: Int): String = when (piece) {
        RED_KING -> "帅"
        RED_ADVISOR -> "仕"
        RED_BISHOP -> "相"
        RED_KNIGHT -> "马"
        RED_ROOK -> "车"
        RED_CANNON -> "炮"
        RED_PAWN -> "兵"
        BLACK_KING -> "将"
        BLACK_ADVISOR -> "士"
        BLACK_BISHOP -> "象"
        BLACK_KNIGHT -> "马"
        BLACK_ROOK -> "车"
        BLACK_CANNON -> "炮"
        BLACK_PAWN -> "卒"
        else -> ""
    }

    fun pieceValue(piece: Int): Int = when (piece) {
        RED_KING, BLACK_KING -> 10000
        RED_ROOK, BLACK_ROOK -> 600
        RED_CANNON, BLACK_CANNON -> 300
        RED_KNIGHT, BLACK_KNIGHT -> 300
        RED_BISHOP, BLACK_BISHOP -> 120
        RED_ADVISOR, BLACK_ADVISOR -> 120
        RED_PAWN, BLACK_PAWN -> 100
        else -> 0
    }
}

package com.chinesechess.game.engine

data class Move(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val captured: Int = ChessConstants.EMPTY,
    val piece: Int = ChessConstants.EMPTY
) {
    fun description(): String {
        val pName = ChessConstants.pieceName(piece)
        val capName = if (captured != ChessConstants.EMPTY) "吃${ChessConstants.pieceName(captured)}" else ""
        return "${pName}(${fromRow},${fromCol})->(${toRow},${toCol})$capName"
    }
}

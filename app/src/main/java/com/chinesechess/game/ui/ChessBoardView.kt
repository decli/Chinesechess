package com.chinesechess.game.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.chinesechess.game.engine.ChessConstants
import com.chinesechess.game.engine.ChessConstants.BOARD_COLS
import com.chinesechess.game.engine.ChessConstants.BOARD_ROWS
import com.chinesechess.game.engine.ChessConstants.EMPTY
import com.chinesechess.game.engine.Move

class ChessBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface OnMoveListener {
        fun onPieceSelected(row: Int, col: Int)
        fun onMoveMade(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int)
    }

    var listener: OnMoveListener? = null
    var boardData: Array<IntArray> = Array(BOARD_ROWS) { IntArray(BOARD_COLS) }
    var selectedRow = -1
    var selectedCol = -1
    var lastMove: Move? = null
    var hintMove: Move? = null
    var validMoves: List<Move> = emptyList()

    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var pieceRadius = 0f

    // Paints
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val boardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0D9A0")
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3728")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val redPiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC3333")
        style = Paint.Style.FILL
    }
    private val blackPiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }
    private val pieceBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B6914")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val pieceInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FAEBD7")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4400CC00")
        style = Paint.Style.FILL
    }
    private val selectBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00CC00")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#440088FF")
        style = Paint.Style.FILL
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FF6600")
        style = Paint.Style.FILL
    }
    private val validMoveDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8800CC00")
        style = Paint.Style.FILL
    }
    private val riverTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3728")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // Board aspect ratio: 9 cols x 10 rows, with padding
        val desiredHeight = (w * 10.5f / 9.0f).toInt()
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val finalH = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) h else minOf(desiredHeight, h)
        setMeasuredDimension(w, finalH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions(w, h)
    }

    private fun calculateDimensions(w: Int, h: Int) {
        // Calculate cell size to maximize board on screen
        val cellW = w.toFloat() / (BOARD_COLS + 1)
        val cellH = h.toFloat() / (BOARD_ROWS + 1)
        cellSize = minOf(cellW, cellH)
        pieceRadius = cellSize * 0.44f

        val boardWidth = cellSize * (BOARD_COLS - 1)
        val boardHeight = cellSize * (BOARD_ROWS - 1)
        boardLeft = (w - boardWidth) / 2f
        boardTop = (h - boardHeight) / 2f

        textPaint.textSize = pieceRadius * 1.05f
        riverTextPaint.textSize = cellSize * 0.55f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBoard(canvas)
        drawHighlights(canvas)
        drawPieces(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        // Background
        val bgRect = RectF(
            boardLeft - cellSize * 0.5f, boardTop - cellSize * 0.5f,
            boardLeft + cellSize * (BOARD_COLS - 1) + cellSize * 0.5f,
            boardTop + cellSize * (BOARD_ROWS - 1) + cellSize * 0.5f
        )
        canvas.drawRoundRect(bgRect, 8f, 8f, boardBgPaint)

        // Grid lines
        for (r in 0 until BOARD_ROWS) {
            canvas.drawLine(
                boardLeft, boardTop + r * cellSize,
                boardLeft + (BOARD_COLS - 1) * cellSize, boardTop + r * cellSize,
                gridPaint
            )
        }
        for (c in 0 until BOARD_COLS) {
            // Top half
            canvas.drawLine(
                boardLeft + c * cellSize, boardTop,
                boardLeft + c * cellSize, boardTop + 4 * cellSize,
                gridPaint
            )
            // Bottom half
            canvas.drawLine(
                boardLeft + c * cellSize, boardTop + 5 * cellSize,
                boardLeft + c * cellSize, boardTop + 9 * cellSize,
                gridPaint
            )
        }
        // Left and right border lines across river
        canvas.drawLine(boardLeft, boardTop + 4 * cellSize, boardLeft, boardTop + 5 * cellSize, gridPaint)
        canvas.drawLine(
            boardLeft + 8 * cellSize, boardTop + 4 * cellSize,
            boardLeft + 8 * cellSize, boardTop + 5 * cellSize, gridPaint
        )

        // Palace diagonals
        // Top palace
        canvas.drawLine(
            boardLeft + 3 * cellSize, boardTop, boardLeft + 5 * cellSize, boardTop + 2 * cellSize, gridPaint
        )
        canvas.drawLine(
            boardLeft + 5 * cellSize, boardTop, boardLeft + 3 * cellSize, boardTop + 2 * cellSize, gridPaint
        )
        // Bottom palace
        canvas.drawLine(
            boardLeft + 3 * cellSize, boardTop + 7 * cellSize,
            boardLeft + 5 * cellSize, boardTop + 9 * cellSize, gridPaint
        )
        canvas.drawLine(
            boardLeft + 5 * cellSize, boardTop + 7 * cellSize,
            boardLeft + 3 * cellSize, boardTop + 9 * cellSize, gridPaint
        )

        // River text
        val riverY = boardTop + 4.5f * cellSize
        val metrics = riverTextPaint.fontMetrics
        val textY = riverY - (metrics.ascent + metrics.descent) / 2
        canvas.drawText("楚 河", boardLeft + 2 * cellSize, textY, riverTextPaint)
        canvas.drawText("汉 界", boardLeft + 6 * cellSize, textY, riverTextPaint)

        // Star positions (小十字标记)
        drawStarMark(canvas, 2, 1); drawStarMark(canvas, 2, 7)
        drawStarMark(canvas, 7, 1); drawStarMark(canvas, 7, 7)
        for (c in intArrayOf(0, 2, 4, 6, 8)) {
            drawStarMark(canvas, 3, c)
            drawStarMark(canvas, 6, c)
        }
    }

    private fun drawStarMark(canvas: Canvas, row: Int, col: Int) {
        val cx = boardLeft + col * cellSize
        val cy = boardTop + row * cellSize
        val s = cellSize * 0.1f
        val gap = cellSize * 0.06f
        val len = cellSize * 0.15f
        val markPaint = Paint(gridPaint).apply { strokeWidth = 1.5f }

        // Draw marks in corners that are within the board
        if (col > 0) {
            canvas.drawLine(cx - gap, cy - gap, cx - gap - len, cy - gap, markPaint)
            canvas.drawLine(cx - gap, cy - gap, cx - gap, cy - gap - len, markPaint)
            canvas.drawLine(cx - gap, cy + gap, cx - gap - len, cy + gap, markPaint)
            canvas.drawLine(cx - gap, cy + gap, cx - gap, cy + gap + len, markPaint)
        }
        if (col < 8) {
            canvas.drawLine(cx + gap, cy - gap, cx + gap + len, cy - gap, markPaint)
            canvas.drawLine(cx + gap, cy - gap, cx + gap, cy - gap - len, markPaint)
            canvas.drawLine(cx + gap, cy + gap, cx + gap + len, cy + gap, markPaint)
            canvas.drawLine(cx + gap, cy + gap, cx + gap, cy + gap + len, markPaint)
        }
    }

    private fun drawHighlights(canvas: Canvas) {
        // Last move highlight
        lastMove?.let { m ->
            canvas.drawCircle(
                boardLeft + m.fromCol * cellSize, boardTop + m.fromRow * cellSize,
                pieceRadius, lastMovePaint
            )
            canvas.drawCircle(
                boardLeft + m.toCol * cellSize, boardTop + m.toRow * cellSize,
                pieceRadius, lastMovePaint
            )
        }

        // Selected piece highlight
        if (selectedRow >= 0 && selectedCol >= 0) {
            val cx = boardLeft + selectedCol * cellSize
            val cy = boardTop + selectedRow * cellSize
            canvas.drawCircle(cx, cy, pieceRadius + 4f, selectPaint)
            canvas.drawCircle(cx, cy, pieceRadius + 4f, selectBorderPaint)

            // Draw valid move indicators
            for (move in validMoves) {
                val mx = boardLeft + move.toCol * cellSize
                val my = boardTop + move.toRow * cellSize
                if (move.captured != EMPTY) {
                    // Draw ring for captures
                    canvas.drawCircle(mx, my, pieceRadius + 2f, Paint(selectBorderPaint).apply {
                        color = Color.parseColor("#CC3333")
                        strokeWidth = 3f
                    })
                } else {
                    canvas.drawCircle(mx, my, cellSize * 0.12f, validMoveDotPaint)
                }
            }
        }

        // Hint highlight
        hintMove?.let { h ->
            canvas.drawCircle(
                boardLeft + h.fromCol * cellSize, boardTop + h.fromRow * cellSize,
                pieceRadius + 3f, hintPaint
            )
            canvas.drawCircle(
                boardLeft + h.toCol * cellSize, boardTop + h.toRow * cellSize,
                pieceRadius + 3f, hintPaint
            )
        }
    }

    private fun drawPieces(canvas: Canvas) {
        for (r in 0 until BOARD_ROWS) {
            for (c in 0 until BOARD_COLS) {
                val piece = boardData[r][c]
                if (piece == EMPTY) continue
                drawPiece(canvas, r, c, piece)
            }
        }
    }

    private fun drawPiece(canvas: Canvas, row: Int, col: Int, piece: Int) {
        val cx = boardLeft + col * cellSize
        val cy = boardTop + row * cellSize

        // Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40000000")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx + 2f, cy + 3f, pieceRadius, shadowPaint)

        // Piece body (wood-like)
        canvas.drawCircle(cx, cy, pieceRadius, pieceInnerPaint)

        // Inner circle border
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2f
            style = Paint.Style.STROKE
            color = if (ChessConstants.isRed(piece)) Color.parseColor("#CC3333") else Color.parseColor("#1A1A1A")
        }
        canvas.drawCircle(cx, cy, pieceRadius * 0.82f, innerBorderPaint)

        // Outer border
        canvas.drawCircle(cx, cy, pieceRadius, pieceBorderPaint)

        // Text
        val name = ChessConstants.pieceName(piece)
        textPaint.color = if (ChessConstants.isRed(piece)) Color.parseColor("#CC3333") else Color.parseColor("#1A1A1A")
        val metrics = textPaint.fontMetrics
        val textY = cy - (metrics.ascent + metrics.descent) / 2
        canvas.drawText(name, cx, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val col = ((event.x - boardLeft + cellSize / 2) / cellSize).toInt()
            val row = ((event.y - boardTop + cellSize / 2) / cellSize).toInt()

            if (row in 0 until BOARD_ROWS && col in 0 until BOARD_COLS) {
                if (selectedRow >= 0 && selectedCol >= 0) {
                    // Try to make a move
                    if (row != selectedRow || col != selectedCol) {
                        listener?.onMoveMade(selectedRow, selectedCol, row, col)
                    } else {
                        // Deselect
                        selectedRow = -1
                        selectedCol = -1
                        validMoves = emptyList()
                        invalidate()
                    }
                } else {
                    listener?.onPieceSelected(row, col)
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    fun updateBoard(data: Array<IntArray>) {
        for (r in 0 until BOARD_ROWS) {
            boardData[r] = data[r].copyOf()
        }
        invalidate()
    }

    fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        validMoves = emptyList()
        hintMove = null
        invalidate()
    }

    fun setSelection(row: Int, col: Int, moves: List<Move>) {
        selectedRow = row
        selectedCol = col
        validMoves = moves
        invalidate()
    }
}

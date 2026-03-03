/**
 * 中国象棋游戏状态管理
 * 管理棋局状态、走法历史、悔棋操作
 */

class Game {
    constructor() {
        this.reset();
    }

    /**
     * 重置为初始状态
     */
    reset() {
        this.board = getInitialBoard();
        this.currentSide = RED; // 红先
        this.moveHistory = [];  // 走法历史: { fr, fc, tr, tc, captured, boardSnapshot }
        this.status = 'playing'; // playing | red_win | black_win | draw
        this.redTime = 0;
        this.blackTime = 0;
    }

    /**
     * 获取当前棋盘副本
     */
    getBoard() {
        return this.board.map(row => [...row]);
    }

    /**
     * 执行走法
     * @returns {object|null} 走法记录, 或 null 如果不合法
     */
    makeMove(fr, fc, tr, tc) {
        // 检查是否是己方棋子
        if (getSide(this.board[fr][fc]) !== this.currentSide) return null;

        // 检查走法合法性
        const legalMoves = generatePieceMoves(this.board, fr, fc);
        const isLegalPseudo = legalMoves.some(m => m.tr === tr && m.tc === tc);
        if (!isLegalPseudo) return null;

        // 执行走法
        const captured = this.board[tr][tc];
        const boardSnapshot = this.board.map(row => [...row]);

        this.board[tr][tc] = this.board[fr][fc];
        this.board[fr][fc] = PIECE.EMPTY;

        // 检查走后是否被将 (不合法)
        if (isInCheck(this.board, this.currentSide)) {
            // 还原
            this.board = boardSnapshot;
            return null;
        }

        // 记录走法
        const moveRecord = {
            fr, fc, tr, tc,
            captured,
            piece: this.board[tr][tc],
            boardSnapshot
        };
        this.moveHistory.push(moveRecord);

        // 切换方
        this.currentSide = -this.currentSide;

        // 检查游戏结束
        this._checkGameEnd();

        return moveRecord;
    }

    /**
     * 悔棋 - 回退一步
     * @returns {boolean} 是否成功
     */
    undoMove() {
        if (this.moveHistory.length === 0) return false;

        const lastMove = this.moveHistory.pop();
        this.board = lastMove.boardSnapshot;
        this.currentSide = -this.currentSide;
        this.status = 'playing';

        return true;
    }

    /**
     * 悔棋两步 (人机对战中一次悔一个回合)
     * @returns {boolean}
     */
    undoTwoMoves() {
        if (this.moveHistory.length < 2) return this.undoMove();
        this.undoMove();
        this.undoMove();
        return true;
    }

    /**
     * 获取某个位置的合法走法
     */
    getLegalMoves(row, col) {
        if (getSide(this.board[row][col]) !== this.currentSide) return [];
        const pseudoMoves = generatePieceMoves(this.board, row, col);
        return pseudoMoves.filter(move => {
            const { board: newBoard } = applyMove(this.board, move);
            return !isInCheck(newBoard, this.currentSide);
        });
    }

    /**
     * 检查游戏结束条件
     */
    _checkGameEnd() {
        if (isCheckmate(this.board, this.currentSide)) {
            this.status = this.currentSide === RED ? 'black_win' : 'red_win';
        } else if (isStalemate(this.board, this.currentSide)) {
            this.status = 'draw';
        }
    }

    /**
     * 当前方是否被将
     */
    isCurrentInCheck() {
        return isInCheck(this.board, this.currentSide);
    }

    /**
     * 获取最后一步走法
     */
    getLastMove() {
        if (this.moveHistory.length === 0) return null;
        const m = this.moveHistory[this.moveHistory.length - 1];
        return { fr: m.fr, fc: m.fc, tr: m.tr, tc: m.tc };
    }

    /**
     * 序列化为 JSON (用于保存)
     */
    serialize() {
        return {
            board: this.board,
            currentSide: this.currentSide,
            moveHistory: this.moveHistory.map(m => ({
                fr: m.fr, fc: m.fc, tr: m.tr, tc: m.tc,
                captured: m.captured, piece: m.piece,
                boardSnapshot: m.boardSnapshot
            })),
            status: this.status
        };
    }

    /**
     * 从 JSON 恢复
     */
    deserialize(data) {
        this.board = data.board;
        this.currentSide = data.currentSide;
        this.moveHistory = data.moveHistory;
        this.status = data.status;
    }

    /**
     * 获取走法描述文本
     */
    getMoveText(moveRecord) {
        const name = PIECE_NAMES[moveRecord.piece] || '?';
        const colNames = getSide(moveRecord.piece) === RED
            ? ['九', '八', '七', '六', '五', '四', '三', '二', '一']
            : ['1', '2', '3', '4', '5', '6', '7', '8', '9'];

        const fromCol = colNames[moveRecord.fc];
        const toCol = colNames[moveRecord.tc];

        let action;
        const side = getSide(moveRecord.piece);
        if (moveRecord.fr === moveRecord.tr) {
            action = '平';
        } else if ((side === RED && moveRecord.tr < moveRecord.fr) ||
            (side === BLACK && moveRecord.tr > moveRecord.fr)) {
            action = '进';
        } else {
            action = '退';
        }

        if (action === '平') {
            return `${name}${fromCol}${action}${toCol}`;
        } else {
            const diff = Math.abs(moveRecord.tr - moveRecord.fr);
            const diffStr = side === RED
                ? ['一', '二', '三', '四', '五', '六', '七', '八', '九'][diff - 1]
                : String(diff);
            return `${name}${fromCol}${action}${diffStr}`;
        }
    }
}

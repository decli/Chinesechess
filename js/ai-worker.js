/**
 * 中国象棋 AI 引擎 - Web Worker
 * Minimax + Alpha-Beta 剪枝
 */

// 导入规则 (Worker 中需要自包含)
// 以下是 rules.js 中关键函数的副本

const PIECE = {
    EMPTY: 0,
    R_KING: 1, R_ADVISOR: 2, R_ELEPHANT: 3, R_HORSE: 4, R_ROOK: 5, R_CANNON: 6, R_PAWN: 7,
    B_KING: -1, B_ADVISOR: -2, B_ELEPHANT: -3, B_HORSE: -4, B_ROOK: -5, B_CANNON: -6, B_PAWN: -7
};

const RED = 1, BLACK = -1;

function getSide(p) { return p > 0 ? RED : p < 0 ? BLACK : 0; }
function getPieceType(p) { return Math.abs(p); }
function inBoard(r, c) { return r >= 0 && r <= 9 && c >= 0 && c <= 8; }
function inPalace(r, c, side) {
    if (c < 3 || c > 5) return false;
    return side === RED ? (r >= 7 && r <= 9) : (r >= 0 && r <= 2);
}
function inOwnHalf(r, side) { return side === RED ? r >= 5 : r <= 4; }

function findKing(board, side) {
    const king = side === RED ? PIECE.R_KING : PIECE.B_KING;
    for (let r = 0; r < 10; r++)
        for (let c = 0; c < 9; c++)
            if (board[r][c] === king) return { r, c };
    return null;
}

function kingsOpposing(board) {
    const rk = findKing(board, RED);
    const bk = findKing(board, BLACK);
    if (!rk || !bk || rk.c !== bk.c) return false;
    for (let r = Math.min(rk.r, bk.r) + 1; r < Math.max(rk.r, bk.r); r++)
        if (board[r][rk.c] !== 0) return false;
    return true;
}

function generatePieceMoves(board, row, col) {
    const piece = board[row][col];
    if (piece === 0) return [];
    const side = getSide(piece);
    const type = getPieceType(piece);
    const moves = [];

    switch (type) {
        case 1: {
            for (const [dr, dc] of [[-1, 0], [1, 0], [0, -1], [0, 1]]) {
                const nr = row + dr, nc = col + dc;
                if (inPalace(nr, nc, side) && getSide(board[nr][nc]) !== side)
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
            }
            break;
        }
        case 2: {
            for (const [dr, dc] of [[-1, -1], [-1, 1], [1, -1], [1, 1]]) {
                const nr = row + dr, nc = col + dc;
                if (inPalace(nr, nc, side) && getSide(board[nr][nc]) !== side)
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
            }
            break;
        }
        case 3: {
            const dirs = [[-2, -2], [-2, 2], [2, -2], [2, 2]];
            const eyes = [[-1, -1], [-1, 1], [1, -1], [1, 1]];
            for (let i = 0; i < 4; i++) {
                const nr = row + dirs[i][0], nc = col + dirs[i][1];
                const er = row + eyes[i][0], ec = col + eyes[i][1];
                if (inBoard(nr, nc) && inOwnHalf(nr, side) && board[er][ec] === 0 && getSide(board[nr][nc]) !== side)
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
            }
            break;
        }
        case 4: {
            const hm = [[-2, -1, -1, 0], [-2, 1, -1, 0], [2, -1, 1, 0], [2, 1, 1, 0], [-1, -2, 0, -1], [-1, 2, 0, 1], [1, -2, 0, -1], [1, 2, 0, 1]];
            for (const [dr, dc, lr, lc] of hm) {
                const nr = row + dr, nc = col + dc;
                if (inBoard(nr, nc) && board[row + lr][col + lc] === 0 && getSide(board[nr][nc]) !== side)
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
            }
            break;
        }
        case 5: {
            for (const [dr, dc] of [[-1, 0], [1, 0], [0, -1], [0, 1]]) {
                let nr = row + dr, nc = col + dc;
                while (inBoard(nr, nc)) {
                    if (board[nr][nc] === 0) { moves.push({ fr: row, fc: col, tr: nr, tc: nc }); }
                    else { if (getSide(board[nr][nc]) !== side) moves.push({ fr: row, fc: col, tr: nr, tc: nc }); break; }
                    nr += dr; nc += dc;
                }
            }
            break;
        }
        case 6: {
            for (const [dr, dc] of [[-1, 0], [1, 0], [0, -1], [0, 1]]) {
                let nr = row + dr, nc = col + dc;
                let jumped = false;
                while (inBoard(nr, nc)) {
                    if (!jumped) {
                        if (board[nr][nc] === 0) moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                        else jumped = true;
                    } else {
                        if (board[nr][nc] !== 0) { if (getSide(board[nr][nc]) !== side) moves.push({ fr: row, fc: col, tr: nr, tc: nc }); break; }
                    }
                    nr += dr; nc += dc;
                }
            }
            break;
        }
        case 7: {
            const fwd = side === RED ? -1 : 1;
            const crossed = !inOwnHalf(row, side);
            const fr = row + fwd;
            if (inBoard(fr, col) && getSide(board[fr][col]) !== side)
                moves.push({ fr: row, fc: col, tr: fr, tc: col });
            if (crossed) {
                for (const dc of [-1, 1]) {
                    const nc = col + dc;
                    if (inBoard(row, nc) && getSide(board[row][nc]) !== side)
                        moves.push({ fr: row, fc: col, tr: row, tc: nc });
                }
            }
            break;
        }
    }
    return moves;
}

function isInCheck(board, side) {
    const king = findKing(board, side);
    if (!king) return true;
    const opp = -side;
    for (let r = 0; r < 10; r++)
        for (let c = 0; c < 9; c++)
            if (getSide(board[r][c]) === opp) {
                const moves = generatePieceMoves(board, r, c);
                for (const m of moves)
                    if (m.tr === king.r && m.tc === king.c) return true;
            }
    if (kingsOpposing(board)) return true;
    return false;
}

function generateAllLegalMoves(board, side) {
    const legal = [];
    for (let r = 0; r < 10; r++)
        for (let c = 0; c < 9; c++)
            if (getSide(board[r][c]) === side) {
                const moves = generatePieceMoves(board, r, c);
                for (const m of moves) {
                    const nb = board.map(row => [...row]);
                    nb[m.tr][m.tc] = nb[m.fr][m.fc];
                    nb[m.fr][m.fc] = 0;
                    if (!isInCheck(nb, side)) legal.push(m);
                }
            }
    return legal;
}

// ============== 评估函数 ==============

// 子力价值
const PIECE_VALUE = {
    1: 10000,  // 帅/将
    2: 20,     // 仕/士
    3: 20,     // 相/象
    4: 90,     // 马
    5: 200,    // 车
    6: 100,    // 炮
    7: 30      // 兵/卒
};

// 位置价值表 (红方视角, 黑方需翻转)
const POSITION_VALUES = {
    // 帅
    1: [
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 1, 1, 1, 0, 0, 0], [0, 0, 0, 2, 2, 2, 0, 0, 0], [0, 0, 0, 11, 15, 11, 0, 0, 0]
    ],
    // 仕
    2: [
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 20, 0, 20, 0, 0, 0], [0, 0, 0, 0, 23, 0, 0, 0, 0], [0, 0, 0, 20, 0, 20, 0, 0, 0]
    ],
    // 相
    3: [
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 20, 0, 0, 0, 20, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [18, 0, 0, 0, 23, 0, 0, 0, 18], [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 20, 0, 0, 0, 20, 0, 0]
    ],
    // 马
    4: [
        [88, 85, 90, 88, 90, 88, 90, 85, 88], [85, 90, 92, 93, 78, 93, 92, 90, 85],
        [93, 92, 94, 95, 92, 95, 94, 92, 93], [92, 94, 98, 95, 98, 95, 98, 94, 92],
        [90, 98, 101, 102, 103, 102, 101, 98, 90], [90, 100, 99, 103, 104, 103, 99, 100, 90],
        [93, 108, 100, 107, 100, 107, 100, 108, 93], [92, 98, 98, 98, 96, 98, 98, 98, 92],
        [90, 96, 103, 97, 94, 97, 103, 96, 90], [90, 90, 90, 96, 91, 96, 90, 90, 90]
    ],
    // 车
    5: [
        [206, 208, 207, 213, 214, 213, 207, 208, 206], [206, 212, 209, 216, 233, 216, 209, 212, 206],
        [206, 208, 207, 214, 216, 214, 207, 208, 206], [206, 213, 213, 216, 216, 216, 213, 213, 206],
        [208, 211, 211, 214, 215, 214, 211, 211, 208], [208, 212, 212, 214, 215, 214, 212, 212, 208],
        [204, 209, 204, 212, 214, 212, 204, 209, 204], [198, 208, 204, 212, 212, 212, 204, 208, 198],
        [200, 208, 206, 212, 200, 212, 206, 208, 200], [194, 206, 204, 212, 200, 212, 204, 206, 194]
    ],
    // 炮
    6: [
        [100, 100, 96, 91, 90, 91, 96, 100, 100], [98, 98, 96, 92, 89, 92, 96, 98, 98],
        [97, 97, 96, 91, 92, 91, 96, 97, 97], [101, 99, 98, 103, 101, 103, 98, 99, 101],
        [101, 98, 99, 103, 100, 103, 99, 98, 101], [100, 98, 97, 103, 100, 103, 97, 98, 100],
        [99, 99, 99, 101, 100, 101, 99, 99, 99], [99, 98, 100, 99, 99, 99, 100, 98, 99],
        [97, 100, 100, 100, 95, 100, 100, 100, 97], [96, 100, 100, 100, 96, 100, 100, 100, 96]
    ],
    // 兵
    7: [
        [9, 9, 9, 11, 13, 11, 9, 9, 9], [19, 24, 34, 42, 44, 42, 34, 24, 19],
        [19, 24, 32, 37, 37, 37, 32, 24, 19], [19, 23, 27, 29, 30, 29, 27, 23, 19],
        [14, 18, 20, 27, 29, 27, 20, 18, 14], [7, 0, 13, 0, 16, 0, 13, 0, 7],
        [7, 0, 7, 0, 15, 0, 7, 0, 7], [0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0, 0, 0]
    ]
};

/**
 * 评估棋盘局面 (相对于指定方)
 */
function evaluate(board, side) {
    let score = 0;
    for (let r = 0; r < 10; r++) {
        for (let c = 0; c < 9; c++) {
            const piece = board[r][c];
            if (piece === 0) continue;
            const pSide = getSide(piece);
            const type = getPieceType(piece);
            const baseVal = PIECE_VALUE[type] || 0;
            const posTable = POSITION_VALUES[type];
            let posVal = 0;
            if (posTable) {
                if (pSide === RED) {
                    posVal = posTable[r][c];
                } else {
                    // 黑方翻转
                    posVal = posTable[9 - r][8 - c];
                }
            }
            const val = baseVal + posVal;
            score += (pSide === side) ? val : -val;
        }
    }
    return score;
}

/**
 * 走法排序 - 提高剪枝效率
 */
function sortMoves(board, moves, side) {
    return moves.map(m => {
        let score = 0;
        const captured = board[m.tr][m.tc];
        if (captured !== 0) {
            // 吃子优先, MVV-LVA
            score += PIECE_VALUE[getPieceType(captured)] * 10 - PIECE_VALUE[getPieceType(board[m.fr][m.fc])];
        }
        return { move: m, score };
    }).sort((a, b) => b.score - a.score).map(x => x.move);
}

/**
 * Alpha-Beta 搜索
 */
function alphaBeta(board, depth, alpha, beta, side, maximizing) {
    if (depth === 0) {
        return { score: evaluate(board, side) };
    }

    const currentSide = maximizing ? side : -side;
    const moves = generateAllLegalMoves(board, currentSide);

    if (moves.length === 0) {
        if (isInCheck(board, currentSide)) {
            return { score: maximizing ? -99999 + (10 - depth) : 99999 - (10 - depth) };
        }
        return { score: maximizing ? -99998 : 99998 };
    }

    const sorted = sortMoves(board, moves, currentSide);
    let bestMove = sorted[0];

    if (maximizing) {
        let maxScore = -Infinity;
        for (const m of sorted) {
            const nb = board.map(row => [...row]);
            nb[m.tr][m.tc] = nb[m.fr][m.fc];
            nb[m.fr][m.fc] = 0;
            const result = alphaBeta(nb, depth - 1, alpha, beta, side, false);
            if (result.score > maxScore) {
                maxScore = result.score;
                bestMove = m;
            }
            alpha = Math.max(alpha, maxScore);
            if (beta <= alpha) break;
        }
        return { score: maxScore, move: bestMove };
    } else {
        let minScore = Infinity;
        for (const m of sorted) {
            const nb = board.map(row => [...row]);
            nb[m.tr][m.tc] = nb[m.fr][m.fc];
            nb[m.fr][m.fc] = 0;
            const result = alphaBeta(nb, depth - 1, alpha, beta, side, true);
            if (result.score < minScore) {
                minScore = result.score;
                bestMove = m;
            }
            beta = Math.min(beta, minScore);
            if (beta <= alpha) break;
        }
        return { score: minScore, move: bestMove };
    }
}

/**
 * 获取 AI 走法
 */
function getAIMove(board, side, level) {
    // 难度对应搜索深度
    const depthMap = { 1: 1, 2: 2, 3: 3, 4: 4, 5: 4 };
    const depth = depthMap[level] || 3;

    // Level 1 添加随机性
    if (level === 1) {
        const moves = generateAllLegalMoves(board, side);
        if (moves.length === 0) return null;

        // 70% 概率随机走
        if (Math.random() < 0.7) {
            return moves[Math.floor(Math.random() * moves.length)];
        }
    }

    const result = alphaBeta(board, depth, -Infinity, Infinity, side, true);

    // Level 5 迭代加深
    if (level === 5) {
        const result5 = alphaBeta(board, 5, -Infinity, Infinity, side, true);
        if (result5.move) return result5.move;
    }

    return result.move || null;
}

// Worker 消息处理
self.onmessage = function (e) {
    const { type, board, side, level } = e.data;

    if (type === 'ai_move') {
        const move = getAIMove(board, side, level);
        self.postMessage({ type: 'ai_move', move });
    } else if (type === 'hint') {
        // 提示用较高深度搜索
        const hintLevel = Math.max(level, 3);
        const move = getAIMove(board, side, hintLevel);
        self.postMessage({ type: 'hint', move });
    }
};

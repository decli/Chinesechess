/**
 * 中国象棋规则引擎
 * 处理走法生成、合法性验证、将军/将杀判断
 */

// 棋子类型常量
const PIECE = {
    EMPTY: 0,
    // 红方 (正数)
    R_KING: 1,    // 帅
    R_ADVISOR: 2, // 仕
    R_ELEPHANT: 3,// 相
    R_HORSE: 4,   // 马
    R_ROOK: 5,    // 车
    R_CANNON: 6,  // 炮
    R_PAWN: 7,    // 兵
    // 黑方 (负数)
    B_KING: -1,   // 将
    B_ADVISOR: -2,// 士
    B_ELEPHANT: -3,// 象
    B_HORSE: -4,  // 马
    B_ROOK: -5,   // 车
    B_CANNON: -6, // 炮
    B_PAWN: -7    // 卒
};

// 棋子名称映射
const PIECE_NAMES = {
    [PIECE.R_KING]: '帅', [PIECE.R_ADVISOR]: '仕', [PIECE.R_ELEPHANT]: '相',
    [PIECE.R_HORSE]: '马', [PIECE.R_ROOK]: '车', [PIECE.R_CANNON]: '炮', [PIECE.R_PAWN]: '兵',
    [PIECE.B_KING]: '将', [PIECE.B_ADVISOR]: '士', [PIECE.B_ELEPHANT]: '象',
    [PIECE.B_HORSE]: '馬', [PIECE.B_ROOK]: '車', [PIECE.B_CANNON]: '砲', [PIECE.B_PAWN]: '卒'
};

const RED = 1;
const BLACK = -1;

/**
 * 获取棋子所属方
 */
function getSide(piece) {
    if (piece > 0) return RED;
    if (piece < 0) return BLACK;
    return 0;
}

/**
 * 获取棋子类型 (绝对值)
 */
function getPieceType(piece) {
    return Math.abs(piece);
}

/**
 * 判断位置是否在棋盘内
 */
function inBoard(row, col) {
    return row >= 0 && row <= 9 && col >= 0 && col <= 8;
}

/**
 * 判断位置是否在九宫格内
 */
function inPalace(row, col, side) {
    if (col < 3 || col > 5) return false;
    if (side === RED) return row >= 7 && row <= 9;
    if (side === BLACK) return row >= 0 && row <= 2;
    return false;
}

/**
 * 判断位置是否在己方半场
 */
function inOwnHalf(row, side) {
    if (side === RED) return row >= 5;
    return row <= 4;
}

/**
 * 生成某个棋子的所有伪合法走法 (不考虑走后是否被将)
 */
function generatePieceMoves(board, row, col) {
    const piece = board[row][col];
    if (piece === PIECE.EMPTY) return [];

    const side = getSide(piece);
    const type = getPieceType(piece);
    const moves = [];

    switch (type) {
        case 1: // 帅/将 - 九宫格内上下左右各一步
            const kingDirs = [[-1,0],[1,0],[0,-1],[0,1]];
            for (const [dr, dc] of kingDirs) {
                const nr = row + dr, nc = col + dc;
                if (inPalace(nr, nc, side) && getSide(board[nr][nc]) !== side) {
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                }
            }
            break;

        case 2: // 仕/士 - 九宫格内斜走一步
            const advDirs = [[-1,-1],[-1,1],[1,-1],[1,1]];
            for (const [dr, dc] of advDirs) {
                const nr = row + dr, nc = col + dc;
                if (inPalace(nr, nc, side) && getSide(board[nr][nc]) !== side) {
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                }
            }
            break;

        case 3: // 相/象 - 斜走两步,不能过河,不能塞象眼
            const eleDirs = [[-2,-2],[-2,2],[2,-2],[2,2]];
            const eleEyes = [[-1,-1],[-1,1],[1,-1],[1,1]];
            for (let i = 0; i < 4; i++) {
                const nr = row + eleDirs[i][0], nc = col + eleDirs[i][1];
                const er = row + eleEyes[i][0], ec = col + eleEyes[i][1];
                if (inBoard(nr, nc) && inOwnHalf(nr, side) &&
                    board[er][ec] === PIECE.EMPTY &&
                    getSide(board[nr][nc]) !== side) {
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                }
            }
            break;

        case 4: // 马 - 日字形走法,蹩马腿
            const horseMoves = [
                [-2,-1, -1,0], [-2,1, -1,0],
                [2,-1, 1,0],  [2,1, 1,0],
                [-1,-2, 0,-1], [-1,2, 0,1],
                [1,-2, 0,-1],  [1,2, 0,1]
            ];
            for (const [dr, dc, lr, lc] of horseMoves) {
                const nr = row + dr, nc = col + dc;
                const legR = row + lr, legC = col + lc;
                if (inBoard(nr, nc) &&
                    board[legR][legC] === PIECE.EMPTY &&
                    getSide(board[nr][nc]) !== side) {
                    moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                }
            }
            break;

        case 5: // 车 - 直线走,遇子停
            const rookDirs = [[-1,0],[1,0],[0,-1],[0,1]];
            for (const [dr, dc] of rookDirs) {
                let nr = row + dr, nc = col + dc;
                while (inBoard(nr, nc)) {
                    if (board[nr][nc] === PIECE.EMPTY) {
                        moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                    } else {
                        if (getSide(board[nr][nc]) !== side) {
                            moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                        }
                        break;
                    }
                    nr += dr; nc += dc;
                }
            }
            break;

        case 6: // 炮 - 直线走,隔一子打
            const cannonDirs = [[-1,0],[1,0],[0,-1],[0,1]];
            for (const [dr, dc] of cannonDirs) {
                let nr = row + dr, nc = col + dc;
                let jumped = false;
                while (inBoard(nr, nc)) {
                    if (!jumped) {
                        if (board[nr][nc] === PIECE.EMPTY) {
                            moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                        } else {
                            jumped = true;
                        }
                    } else {
                        if (board[nr][nc] !== PIECE.EMPTY) {
                            if (getSide(board[nr][nc]) !== side) {
                                moves.push({ fr: row, fc: col, tr: nr, tc: nc });
                            }
                            break;
                        }
                    }
                    nr += dr; nc += dc;
                }
            }
            break;

        case 7: // 兵/卒 - 未过河只能前进,过河可左右
            const forward = side === RED ? -1 : 1;
            const crossed = !inOwnHalf(row, side);
            // 前进
            const fr = row + forward;
            if (inBoard(fr, col) && getSide(board[fr][col]) !== side) {
                moves.push({ fr: row, fc: col, tr: fr, tc: col });
            }
            // 过河后可左右
            if (crossed) {
                for (const dc of [-1, 1]) {
                    const nc = col + dc;
                    if (inBoard(row, nc) && getSide(board[row][nc]) !== side) {
                        moves.push({ fr: row, fc: col, tr: row, tc: nc });
                    }
                }
            }
            break;
    }

    return moves;
}

/**
 * 找到某方的将/帅位置
 */
function findKing(board, side) {
    const king = side === RED ? PIECE.R_KING : PIECE.B_KING;
    for (let r = 0; r < 10; r++) {
        for (let c = 0; c < 9; c++) {
            if (board[r][c] === king) return { r, c };
        }
    }
    return null;
}

/**
 * 检测将帅是否面对面 (会面)
 */
function kingsOpposing(board) {
    const rk = findKing(board, RED);
    const bk = findKing(board, BLACK);
    if (!rk || !bk) return false;
    if (rk.c !== bk.c) return false;
    // 检查中间是否有棋子
    const minR = Math.min(rk.r, bk.r);
    const maxR = Math.max(rk.r, bk.r);
    for (let r = minR + 1; r < maxR; r++) {
        if (board[r][rk.c] !== PIECE.EMPTY) return false;
    }
    return true;
}

/**
 * 检测某方是否被将军
 */
function isInCheck(board, side) {
    const king = findKing(board, side);
    if (!king) return true; // 将被吃了
    const opponentSide = -side;

    // 检查对方所有棋子是否能攻击到将/帅
    for (let r = 0; r < 10; r++) {
        for (let c = 0; c < 9; c++) {
            if (getSide(board[r][c]) === opponentSide) {
                const moves = generatePieceMoves(board, r, c);
                for (const m of moves) {
                    if (m.tr === king.r && m.tc === king.c) return true;
                }
            }
        }
    }

    // 检查将帅面对面
    if (kingsOpposing(board)) return true;

    return false;
}

/**
 * 执行走法 (在副本上)
 */
function applyMove(board, move) {
    const newBoard = board.map(row => [...row]);
    const captured = newBoard[move.tr][move.tc];
    newBoard[move.tr][move.tc] = newBoard[move.fr][move.fc];
    newBoard[move.fr][move.fc] = PIECE.EMPTY;
    return { board: newBoard, captured };
}

/**
 * 生成某方所有合法走法
 */
function generateAllLegalMoves(board, side) {
    const legalMoves = [];
    for (let r = 0; r < 10; r++) {
        for (let c = 0; c < 9; c++) {
            if (getSide(board[r][c]) === side) {
                const moves = generatePieceMoves(board, r, c);
                for (const move of moves) {
                    // 走后不能被将
                    const { board: newBoard } = applyMove(board, move);
                    if (!isInCheck(newBoard, side)) {
                        // 走后不能造成将帅面对面 (对方视角的非法)
                        legalMoves.push(move);
                    }
                }
            }
        }
    }
    return legalMoves;
}

/**
 * 检测是否将杀 (某方无合法走法且被将)
 */
function isCheckmate(board, side) {
    if (!isInCheck(board, side)) return false;
    return generateAllLegalMoves(board, side).length === 0;
}

/**
 * 检测是否困毙 (某方无合法走法但未被将)
 */
function isStalemate(board, side) {
    if (isInCheck(board, side)) return false;
    return generateAllLegalMoves(board, side).length === 0;
}

/**
 * 初始棋盘布局
 * 红方在下 (行7-9), 黑方在上 (行0-2)
 */
function getInitialBoard() {
    return [
        // 行0: 黑方底线
        [PIECE.B_ROOK, PIECE.B_HORSE, PIECE.B_ELEPHANT, PIECE.B_ADVISOR, PIECE.B_KING, PIECE.B_ADVISOR, PIECE.B_ELEPHANT, PIECE.B_HORSE, PIECE.B_ROOK],
        // 行1: 空
        [0,0,0,0,0,0,0,0,0],
        // 行2: 黑炮
        [0, PIECE.B_CANNON, 0,0,0,0,0, PIECE.B_CANNON, 0],
        // 行3: 黑卒
        [PIECE.B_PAWN, 0, PIECE.B_PAWN, 0, PIECE.B_PAWN, 0, PIECE.B_PAWN, 0, PIECE.B_PAWN],
        // 行4: 楚河汉界
        [0,0,0,0,0,0,0,0,0],
        // 行5: 楚河汉界
        [0,0,0,0,0,0,0,0,0],
        // 行6: 红兵
        [PIECE.R_PAWN, 0, PIECE.R_PAWN, 0, PIECE.R_PAWN, 0, PIECE.R_PAWN, 0, PIECE.R_PAWN],
        // 行7: 红炮
        [0, PIECE.R_CANNON, 0,0,0,0,0, PIECE.R_CANNON, 0],
        // 行8: 空
        [0,0,0,0,0,0,0,0,0],
        // 行9: 红方底线
        [PIECE.R_ROOK, PIECE.R_HORSE, PIECE.R_ELEPHANT, PIECE.R_ADVISOR, PIECE.R_KING, PIECE.R_ADVISOR, PIECE.R_ELEPHANT, PIECE.R_HORSE, PIECE.R_ROOK]
    ];
}

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { PIECE, PIECE_NAMES, RED, BLACK, getSide, getPieceType, inBoard, generatePieceMoves, generateAllLegalMoves, isInCheck, isCheckmate, isStalemate, applyMove, findKing, getInitialBoard, kingsOpposing };
}

/**
 * AI 接口封装 - Web Worker 通信
 */

class AIEngine {
    constructor() {
        this.worker = null;
        this.pendingResolve = null;
        this.isThinking = false;
        this._initWorker();
    }

    _initWorker() {
        try {
            this.worker = new Worker('js/ai-worker.js');
            this.worker.onmessage = (e) => {
                const { type, move } = e.data;
                this.isThinking = false;
                if (this.pendingResolve) {
                    this.pendingResolve(move);
                    this.pendingResolve = null;
                }
            };
            this.worker.onerror = (err) => {
                console.error('AI Worker error:', err);
                this.isThinking = false;
                if (this.pendingResolve) {
                    this.pendingResolve(null);
                    this.pendingResolve = null;
                }
            };
        } catch (e) {
            console.warn('Web Worker not available, AI will run on main thread');
            this.worker = null;
        }
    }

    /**
     * 获取 AI 走法
     * @returns {Promise<{fr,fc,tr,tc}|null>}
     */
    getMove(board, side, level) {
        return new Promise((resolve) => {
            this.isThinking = true;
            this.pendingResolve = resolve;

            if (this.worker) {
                this.worker.postMessage({ type: 'ai_move', board, side, level });
            } else {
                // Fallback: 主线程 (延迟执行避免阻塞)
                setTimeout(() => {
                    const moves = generateAllLegalMoves(board, side);
                    this.isThinking = false;
                    resolve(moves.length > 0 ? moves[Math.floor(Math.random() * moves.length)] : null);
                }, 100);
            }

            // 超时保护 (15秒)
            setTimeout(() => {
                if (this.isThinking) {
                    this.isThinking = false;
                    if (this.pendingResolve) {
                        this.pendingResolve(null);
                        this.pendingResolve = null;
                    }
                }
            }, 15000);
        });
    }

    /**
     * 获取提示走法
     */
    getHint(board, side, level) {
        return new Promise((resolve) => {
            this.isThinking = true;
            this.pendingResolve = resolve;

            if (this.worker) {
                this.worker.postMessage({ type: 'hint', board, side, level });
            } else {
                setTimeout(() => {
                    const moves = generateAllLegalMoves(board, side);
                    this.isThinking = false;
                    resolve(moves.length > 0 ? moves[0] : null);
                }, 100);
            }

            setTimeout(() => {
                if (this.isThinking) {
                    this.isThinking = false;
                    if (this.pendingResolve) {
                        this.pendingResolve(null);
                        this.pendingResolve = null;
                    }
                }
            }, 15000);
        });
    }

    /**
     * 取消当前运算
     */
    cancel() {
        if (this.worker) {
            this.worker.terminate();
            this._initWorker();
        }
        this.isThinking = false;
        if (this.pendingResolve) {
            this.pendingResolve(null);
            this.pendingResolve = null;
        }
    }
}

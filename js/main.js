/**
 * 中国象棋主控制器
 * 协调 UI 交互、游戏逻辑、AI 引擎
 */

class XiangqiApp {
    constructor() {
        this.game = new Game();
        this.storage = new Storage();
        this.ai = new AIEngine();

        // 设置
        this.aiLevel = 3;
        this.playerSide = RED; // 玩家执红
        this.aiEnabled = true;
        this.soundEnabled = true; // 语音播报开关

        // UI 元素引用
        this.canvas = document.getElementById('board-canvas');
        this.board = new BoardRenderer(this.canvas);

        // UI 状态
        this.selectedPos = null;
        this.isAIThinking = false;

        this._bindEvents();
        this._loadSettings();
        this._checkAutoSave();
        this._render();
    }

    // ============== 事件绑定 ==============

    _bindEvents() {
        // Canvas 点击
        this.canvas.addEventListener('click', (e) => this._onCanvasClick(e));
        this.canvas.addEventListener('touchstart', (e) => {
            e.preventDefault();
            const touch = e.touches[0];
            const rect = this.canvas.getBoundingClientRect();
            this._handleBoardClick(touch.clientX - rect.left, touch.clientY - rect.top);
        }, { passive: false });

        // 按钮
        document.getElementById('btn-new-game').addEventListener('click', () => this._newGame());
        document.getElementById('btn-undo').addEventListener('click', () => this._undo());
        document.getElementById('btn-hint').addEventListener('click', () => this._hint());
        document.getElementById('btn-save').addEventListener('click', () => this._openSaveDialog());
        document.getElementById('btn-load').addEventListener('click', () => this._openLoadDialog());

        // 难度选择
        document.getElementById('level-select').addEventListener('change', (e) => {
            this.aiLevel = parseInt(e.target.value);
            this._saveSettings();
            this._updateStatus();
        });

        // 执子选择
        document.getElementById('side-select').addEventListener('change', (e) => {
            this.playerSide = parseInt(e.target.value);
            this.board.flipped = this.playerSide === BLACK;
            this._saveSettings();
            this._newGameConfirmed();
        });

        // 语音播报开关
        document.getElementById('sound-toggle').addEventListener('change', (e) => {
            this.soundEnabled = e.target.checked;
            this._saveSettings();
        });

        // 窗口缩放
        window.addEventListener('resize', () => {
            this.board.resize();
            this._render();
        });

        // 对话框关闭
        document.querySelectorAll('.dialog-overlay').forEach(overlay => {
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) this._closeAllDialogs();
            });
        });

        // 新游戏确认
        document.getElementById('confirm-new-game').addEventListener('click', () => {
            this._newGameConfirmed();
            this._closeAllDialogs();
        });
        document.getElementById('cancel-new-game').addEventListener('click', () => this._closeAllDialogs());

        // 存档槽位
        for (let i = 0; i < 3; i++) {
            document.getElementById(`save-slot-${i}`).addEventListener('click', () => this._saveToSlot(i));
            document.getElementById(`load-slot-${i}`).addEventListener('click', () => this._loadFromSlot(i));
        }

        // 关闭按钮
        document.querySelectorAll('.dialog-close').forEach(btn => {
            btn.addEventListener('click', () => this._closeAllDialogs());
        });

        // 游戏结束对话框
        document.getElementById('gameover-new').addEventListener('click', () => {
            this._closeAllDialogs();
            this._newGameConfirmed();
        });
        document.getElementById('gameover-close').addEventListener('click', () => this._closeAllDialogs());
    }

    // ============== 棋盘交互 ==============

    _onCanvasClick(e) {
        const rect = this.canvas.getBoundingClientRect();
        this._handleBoardClick(e.clientX - rect.left, e.clientY - rect.top);
    }

    _handleBoardClick(px, py) {
        if (this.isAIThinking || this.game.status !== 'playing') return;
        if (this.game.currentSide !== this.playerSide) return;

        const pos = this.board.pixelToBoard(px, py);
        if (!pos) return;

        const { row, col } = pos;
        const piece = this.game.board[row][col];

        // 清除提示
        this.board.clearHint();

        if (this.selectedPos) {
            // 已有选中 - 尝试走子
            const legalMoves = this.game.getLegalMoves(this.selectedPos.row, this.selectedPos.col);
            const targetMove = legalMoves.find(m => m.tr === row && m.tc === col);

            if (targetMove) {
                this._executeMove(targetMove.fr, targetMove.fc, targetMove.tr, targetMove.tc);
                return;
            }

            // 点击的是己方其他棋子 - 切换选中
            if (getSide(piece) === this.playerSide) {
                this._selectPiece(row, col);
                return;
            }

            // 点击空处 - 取消选中
            this._clearSelection();
            this._render();
            return;
        }

        // 没有选中 - 选择己方棋子
        if (getSide(piece) === this.playerSide) {
            this._selectPiece(row, col);
        }
    }

    _selectPiece(row, col) {
        this.selectedPos = { row, col };
        const legalMoves = this.game.getLegalMoves(row, col);
        this.board.setSelection(this.selectedPos, legalMoves, this.game.board);
        this._render();
    }

    _clearSelection() {
        this.selectedPos = null;
        this.board.clearSelection();
    }

    // ============== 走子 ==============

    _executeMove(fr, fc, tr, tc) {
        const moveRecord = this.game.makeMove(fr, fc, tr, tc);
        if (!moveRecord) return;

        this._clearSelection();
        this.board.setLastMove({ fr, fc, tr, tc });
        this._addMoveToHistory(moveRecord);
        this._announceMove(moveRecord);
        this._updateCheckHighlight();
        this._render();
        this._autoSave();

        // 检查游戏结束
        if (this.game.status !== 'playing') {
            setTimeout(() => this._showGameOver(), 500);
            return;
        }

        // AI 走棋
        if (this.aiEnabled && this.game.currentSide !== this.playerSide) {
            this._aiMove();
        }
    }

    async _aiMove() {
        this.isAIThinking = true;
        this._updateStatus();

        // 延迟一下让 UI 更新
        await new Promise(r => setTimeout(r, 300));

        const move = await this.ai.getMove(this.game.getBoard(), this.game.currentSide, this.aiLevel);

        this.isAIThinking = false;

        if (move) {
            const moveRecord = this.game.makeMove(move.fr, move.fc, move.tr, move.tc);
            if (moveRecord) {
                this.board.setLastMove({ fr: move.fr, fc: move.fc, tr: move.tr, tc: move.tc });
                this._addMoveToHistory(moveRecord);
                this._announceMove(moveRecord);
                this._updateCheckHighlight();
                this._render();
                this._autoSave();

                if (this.game.status !== 'playing') {
                    setTimeout(() => this._showGameOver(), 500);
                }
            }
        }

        this._updateStatus();
    }

    // ============== 操作按钮 ==============

    _newGame() {
        if (this.game.moveHistory.length > 0) {
            document.getElementById('new-game-dialog').classList.add('active');
        } else {
            this._newGameConfirmed();
        }
    }

    _newGameConfirmed() {
        this.game.reset();
        this.ai.cancel();
        this.isAIThinking = false;
        this._clearSelection();
        this.board.setLastMove(null);
        this.board.setCheckPos(null);
        this.board.clearHint();
        this.board.flipped = this.playerSide === BLACK;
        document.getElementById('move-list').innerHTML = '<li class="move-list-empty">等待开局...</li>';
        this._render();
        this._updateStatus();
        this.storage.clearAutoSave();

        // 如果玩家执黑, AI 先走
        if (this.playerSide === BLACK) {
            setTimeout(() => this._aiMove(), 500);
        }
    }

    _undo() {
        if (this.isAIThinking || this.game.moveHistory.length === 0) return;
        this.board.clearHint();

        if (this.aiEnabled) {
            // AI模式: 撤销两步
            this.game.undoTwoMoves();
        } else {
            this.game.undoMove();
        }

        this._clearSelection();
        this.board.setLastMove(this.game.getLastMove());
        this._updateCheckHighlight();
        this._rebuildMoveList();
        this._render();
        this._updateStatus();
        this._autoSave();
    }

    async _hint() {
        if (this.isAIThinking || this.game.status !== 'playing') return;
        if (this.game.currentSide !== this.playerSide) return;

        this.isAIThinking = true;
        this._updateStatus('提示计算中...');

        const move = await this.ai.getHint(this.game.getBoard(), this.game.currentSide, this.aiLevel);

        this.isAIThinking = false;

        if (move) {
            this.board.setHint(move);
            this._render();
            // 3秒后自动清除提示
            setTimeout(() => {
                this.board.clearHint();
                this._render();
            }, 3000);
        }

        this._updateStatus();
    }

    // ============== 保存/加载 ==============

    _openSaveDialog() {
        this._updateSaveSlotDisplay('save');
        document.getElementById('save-dialog').classList.add('active');
    }

    _openLoadDialog() {
        this._updateSaveSlotDisplay('load');
        document.getElementById('load-dialog').classList.add('active');
    }

    _updateSaveSlotDisplay(mode) {
        const slots = this.storage.getSaveSlots();
        for (const slotInfo of slots) {
            const elId = mode === 'save' ? `save-slot-${slotInfo.slot}` : `load-slot-${slotInfo.slot}`;
            const el = document.getElementById(elId);
            if (slotInfo.empty) {
                el.innerHTML = `<span class="slot-title">存档 ${slotInfo.slot + 1}</span><span class="slot-info">空</span>`;
                if (mode === 'load') el.disabled = true;
                else el.disabled = false;
            } else {
                el.innerHTML = `<span class="slot-title">存档 ${slotInfo.slot + 1}</span><span class="slot-info">${slotInfo.date} · 第${slotInfo.moveCount}手 · 难度${slotInfo.aiLevel}</span>`;
                el.disabled = false;
            }
        }
    }

    _saveToSlot(slot) {
        const success = this.storage.saveGame(slot, this.game.serialize(), this.aiLevel, this.playerSide);
        if (success) {
            this._showToast('保存成功！');
        } else {
            this._showToast('保存失败');
        }
        this._closeAllDialogs();
    }

    _loadFromSlot(slot) {
        const data = this.storage.loadGame(slot);
        if (!data) return;

        this.ai.cancel();
        this.isAIThinking = false;
        this.game.deserialize(data.gameData);
        this.aiLevel = data.aiLevel;
        this.playerSide = data.playerSide || RED;
        this.board.flipped = this.playerSide === BLACK;

        // 更新 UI 控件
        document.getElementById('level-select').value = this.aiLevel;
        document.getElementById('side-select').value = this.playerSide;

        this._clearSelection();
        this.board.setLastMove(this.game.getLastMove());
        this._updateCheckHighlight();
        this._rebuildMoveList();
        this._render();
        this._updateStatus();
        this._closeAllDialogs();
        this._showToast('加载成功！');

        // 如果当前是 AI 的回合
        if (this.game.status === 'playing' && this.game.currentSide !== this.playerSide) {
            setTimeout(() => this._aiMove(), 500);
        }
    }

    _autoSave() {
        this.storage.autoSave(this.game.serialize(), this.aiLevel, this.playerSide);
    }

    _checkAutoSave() {
        const data = this.storage.loadAutoSave();
        if (data && data.gameData && data.gameData.moveHistory && data.gameData.moveHistory.length > 0) {
            // 恢复上次的棋局
            this.game.deserialize(data.gameData);
            this.aiLevel = data.aiLevel || 3;
            this.playerSide = data.playerSide || RED;
            this.board.flipped = this.playerSide === BLACK;

            document.getElementById('level-select').value = this.aiLevel;
            document.getElementById('side-select').value = this.playerSide;

            this.board.setLastMove(this.game.getLastMove());
            this._updateCheckHighlight();
            this._rebuildMoveList();
            this._updateStatus();

            // 如果是 AI 的回合
            if (this.game.status === 'playing' && this.game.currentSide !== this.playerSide) {
                setTimeout(() => this._aiMove(), 800);
            }
        }
    }

    // ============== UI 更新 ==============

    _render() {
        this.board.render(this.game.board);
    }

    _updateStatus(customMsg) {
        const statusEl = document.getElementById('turn-indicator');
        const turnDot = document.getElementById('turn-dot');

        if (customMsg) {
            statusEl.textContent = customMsg;
            return;
        }

        if (this.game.status !== 'playing') {
            const msgs = {
                'red_win': '🎉 红方获胜！',
                'black_win': '🎉 黑方获胜！',
                'draw': '🤝 和棋！'
            };
            statusEl.textContent = msgs[this.game.status] || '游戏结束';
            turnDot.className = 'turn-dot';
            return;
        }

        if (this.isAIThinking) {
            statusEl.textContent = '⏳ AI 思考中...';
            turnDot.className = 'turn-dot thinking';
            return;
        }

        const isRedTurn = this.game.currentSide === RED;
        statusEl.textContent = isRedTurn ? '红方走棋' : '黑方走棋';
        turnDot.className = `turn-dot ${isRedTurn ? 'red' : 'black'}`;

        if (this.game.isCurrentInCheck()) {
            statusEl.textContent += ' ⚠️ 将军！';
        }
    }

    _updateCheckHighlight() {
        if (this.game.isCurrentInCheck()) {
            const king = findKing(this.game.board, this.game.currentSide);
            this.board.setCheckPos(king);
        } else {
            this.board.setCheckPos(null);
        }
    }

    _addMoveToHistory(moveRecord) {
        const moveList = document.getElementById('move-list');

        // 清除空状态占位
        const emptyMsg = moveList.querySelector('.move-list-empty');
        if (emptyMsg) emptyMsg.remove();

        const moveNum = this.game.moveHistory.length;
        const text = this.game.getMoveText(moveRecord);
        const side = getSide(moveRecord.piece);

        const li = document.createElement('li');
        li.className = `move-item ${side === RED ? 'red-move' : 'black-move'}`;
        li.innerHTML = `<span class="move-num">${moveNum}.</span> <span class="move-text">${text}</span>`;
        moveList.appendChild(li);
        moveList.scrollTop = moveList.scrollHeight;
    }

    _rebuildMoveList() {
        const moveList = document.getElementById('move-list');
        moveList.innerHTML = '';
        if (this.game.moveHistory.length === 0) {
            moveList.innerHTML = '<li class="move-list-empty">等待开局...</li>';
            return;
        }
        for (const record of this.game.moveHistory) {
            const moveNum = this.game.moveHistory.indexOf(record) + 1;
            const text = this.game.getMoveText(record);
            const side = getSide(record.piece);
            const li = document.createElement('li');
            li.className = `move-item ${side === RED ? 'red-move' : 'black-move'}`;
            li.innerHTML = `<span class="move-num">${moveNum}.</span> <span class="move-text">${text}</span>`;
            moveList.appendChild(li);
        }
        moveList.scrollTop = moveList.scrollHeight;
    }

    _showGameOver() {
        const dialog = document.getElementById('gameover-dialog');
        const msgEl = document.getElementById('gameover-message');
        const msgs = {
            'red_win': this.playerSide === RED ? '🎉 恭喜你，红方获胜！' : '😔 黑方败北，红方获胜！',
            'black_win': this.playerSide === BLACK ? '🎉 恭喜你，黑方获胜！' : '😔 红方败北，黑方获胜！',
            'draw': '🤝 双方和棋！'
        };
        msgEl.textContent = msgs[this.game.status] || '游戏结束';
        dialog.classList.add('active');
    }

    _showToast(message) {
        let toast = document.getElementById('toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'toast';
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 2000);
    }

    _closeAllDialogs() {
        document.querySelectorAll('.dialog-overlay').forEach(d => d.classList.remove('active'));
    }

    // ============== 语音播报 ==============

    _announceMove(moveRecord) {
        if (!this.soundEnabled) return;
        if (!('speechSynthesis' in window)) return;

        const side = getSide(moveRecord.piece);
        const sideText = side === RED ? '红方' : '黑方';
        const moveText = this.game.getMoveText(moveRecord);

        let text = `${sideText}，${moveText}`;

        // 附加将军/结束提示
        if (this.game.status === 'red_win') {
            text += '，红方获胜！';
        } else if (this.game.status === 'black_win') {
            text += '，黑方获胜！';
        } else if (this.game.status === 'draw') {
            text += '，和棋！';
        } else if (this.game.isCurrentInCheck()) {
            text += '，将军！';
        }

        // 取消之前的语音
        window.speechSynthesis.cancel();

        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'zh-CN';
        utterance.rate = 0.95;
        utterance.pitch = 1.0;
        utterance.volume = 1.0;

        // 尝试选择中文语音
        const voices = window.speechSynthesis.getVoices();
        const zhVoice = voices.find(v => v.lang.startsWith('zh'));
        if (zhVoice) utterance.voice = zhVoice;

        window.speechSynthesis.speak(utterance);
    }

    // ============== 设置持久化 ==============

    _saveSettings() {
        this.storage.saveSettings({
            aiLevel: this.aiLevel,
            playerSide: this.playerSide,
            soundEnabled: this.soundEnabled
        });
    }

    _loadSettings() {
        const settings = this.storage.loadSettings();
        if (settings) {
            this.aiLevel = settings.aiLevel || 3;
            this.playerSide = settings.playerSide || RED;
            this.soundEnabled = settings.soundEnabled !== false;
            this.board.flipped = this.playerSide === BLACK;
            document.getElementById('level-select').value = this.aiLevel;
            document.getElementById('side-select').value = this.playerSide;
            document.getElementById('sound-toggle').checked = this.soundEnabled;
        }
    }
}

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    window.app = new XiangqiApp();
});

/**
 * 本地存储管理
 */

class Storage {
    constructor() {
        this.SAVE_KEY = 'xiangqi_saves';
        this.SETTINGS_KEY = 'xiangqi_settings';
        this.MAX_SLOTS = 3;
    }

    /**
     * 保存棋局到指定槽位
     */
    saveGame(slot, gameData, aiLevel, playerSide) {
        const saves = this._getSaves();
        saves[slot] = {
            gameData,
            aiLevel,
            playerSide,
            timestamp: Date.now(),
            date: new Date().toLocaleString('zh-CN')
        };
        try {
            localStorage.setItem(this.SAVE_KEY, JSON.stringify(saves));
            return true;
        } catch (e) {
            console.error('Save failed:', e);
            return false;
        }
    }

    /**
     * 加载指定槽位的棋局
     */
    loadGame(slot) {
        const saves = this._getSaves();
        return saves[slot] || null;
    }

    /**
     * 获取所有存档信息
     */
    getSaveSlots() {
        const saves = this._getSaves();
        const slots = [];
        for (let i = 0; i < this.MAX_SLOTS; i++) {
            if (saves[i]) {
                slots.push({
                    slot: i,
                    date: saves[i].date,
                    aiLevel: saves[i].aiLevel,
                    moveCount: saves[i].gameData.moveHistory ? saves[i].gameData.moveHistory.length : 0
                });
            } else {
                slots.push({ slot: i, empty: true });
            }
        }
        return slots;
    }

    /**
     * 删除指定槽位
     */
    deleteSave(slot) {
        const saves = this._getSaves();
        delete saves[slot];
        localStorage.setItem(this.SAVE_KEY, JSON.stringify(saves));
    }

    /**
     * 保存设置
     */
    saveSettings(settings) {
        try {
            localStorage.setItem(this.SETTINGS_KEY, JSON.stringify(settings));
        } catch (e) {
            console.error('Settings save failed:', e);
        }
    }

    /**
     * 加载设置
     */
    loadSettings() {
        try {
            const data = localStorage.getItem(this.SETTINGS_KEY);
            return data ? JSON.parse(data) : null;
        } catch (e) {
            return null;
        }
    }

    /**
     * 自动保存当前棋局 (特殊槽位)
     */
    autoSave(gameData, aiLevel, playerSide) {
        try {
            localStorage.setItem('xiangqi_autosave', JSON.stringify({
                gameData, aiLevel, playerSide, timestamp: Date.now()
            }));
        } catch (e) { }
    }

    /**
     * 加载自动保存
     */
    loadAutoSave() {
        try {
            const data = localStorage.getItem('xiangqi_autosave');
            return data ? JSON.parse(data) : null;
        } catch (e) {
            return null;
        }
    }

    clearAutoSave() {
        localStorage.removeItem('xiangqi_autosave');
    }

    _getSaves() {
        try {
            const data = localStorage.getItem(this.SAVE_KEY);
            return data ? JSON.parse(data) : {};
        } catch (e) {
            return {};
        }
    }
}

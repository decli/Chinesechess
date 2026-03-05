package com.chinesechess.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import com.chinesechess.game.R
import com.chinesechess.game.engine.ChessConstants
import com.chinesechess.game.engine.Move
import java.util.*

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var moveSound = 0
    private var captureSound = 0
    private var checkSound = 0
    private var winSound = 0
    private var selectSound = 0

    var soundEnabled = true
    var voiceEnabled = true

    private val humorousLines = listOf(
        "哈哈，这步棋下得妙啊！",
        "看我这招，你可要小心了！",
        "嘿嘿，老夫出手，必有深意！",
        "别急别急，让我想想...好了，就这么走！",
        "这步棋，我已经想了三秒钟！",
        "你以为我会走这步？没错，我就走这步！",
        "好棋好棋，虽然是我自己下的",
        "让你见识见识什么叫人工智能！",
        "我的炮可不是吃素的！",
        "这车开过来了，请注意安全！",
        "马踏八方，谁与争锋！",
        "将军！惊不惊喜，意不意外？",
        "我这象走的田，比你家田地还大！",
        "士可杀不可辱，但可以挡一挡",
        "兵过河就回不去了，跟我的发际线一样",
        "吃你一个子，不客气了！",
        "这步棋价值连城！打折后也就值两毛",
        "我不是在下棋，我是在表演艺术！"
    )

    private val checkLines = listOf(
        "将军！你的老大危险了！",
        "将军！快来护驾！",
        "哎呀，你的将要凉了！",
        "围魏救赵？不，我直接将军！",
        "将！你的防线有漏洞啊！"
    )

    private val captureLines = listOf(
        "吃掉你一个子，真香！",
        "这个子我收下了，谢谢！",
        "来来来，尝尝我的大招！",
        "嘿嘿，又吃了你一个！",
        "这顿饭不错，再来一个？"
    )

    fun init() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.let { pool ->
            moveSound = pool.load(context, R.raw.move, 1)
            captureSound = pool.load(context, R.raw.capture, 1)
            checkSound = pool.load(context, R.raw.check, 1)
            winSound = pool.load(context, R.raw.win, 1)
            selectSound = pool.load(context, R.raw.select, 1)
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(1.1f)
                tts?.setPitch(1.0f)
                ttsReady = true
            }
        }
    }

    fun playMoveSound() {
        if (soundEnabled) soundPool?.play(moveSound, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun playCaptureSound() {
        if (soundEnabled) soundPool?.play(captureSound, 1f, 1f, 1, 0, 1f)
    }

    fun playCheckSound() {
        if (soundEnabled) soundPool?.play(checkSound, 1f, 1f, 1, 0, 1f)
    }

    fun playWinSound() {
        if (soundEnabled) soundPool?.play(winSound, 1f, 1f, 1, 0, 1f)
    }

    fun playSelectSound() {
        if (soundEnabled) soundPool?.play(selectSound, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun speakAIMove(move: Move, isCheck: Boolean) {
        if (!voiceEnabled || !ttsReady) return

        val line = when {
            isCheck -> checkLines.random()
            move.captured != ChessConstants.EMPTY -> captureLines.random()
            else -> humorousLines.random()
        }

        tts?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "ai_move_${System.currentTimeMillis()}")
    }

    fun speakText(text: String) {
        if (!voiceEnabled || !ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "text_${System.currentTimeMillis()}")
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

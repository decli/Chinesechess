package com.chinesechess.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chinesechess.game.ai.Difficulty
import com.chinesechess.game.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentDifficulty = Difficulty.MEDIUM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableFullScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateDifficultyButton()

        binding.btnStartGame.setOnClickListener {
            startGame(vsAI = true)
        }

        binding.btnTwoPlayer.setOnClickListener {
            startGame(vsAI = false)
        }

        binding.btnDifficulty.setOnClickListener {
            showDifficultyDialog()
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun enableFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startGame(vsAI: Boolean) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("vs_ai", vsAI)
            putExtra("difficulty", currentDifficulty.name)
        }
        startActivity(intent)
    }

    private fun showDifficultyDialog() {
        val options = arrayOf("初级 - 休闲娱乐", "中级 - 有一定挑战", "高级 - 棋力较强")
        val current = currentDifficulty.ordinal

        AlertDialog.Builder(this)
            .setTitle("选择难度")
            .setSingleChoiceItems(options, current) { dialog, which ->
                currentDifficulty = Difficulty.entries[which]
                updateDifficultyButton()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage("中国象棋 v1.0\n\n经典中国象棋游戏\n支持人机对战和双人对战\n\n祝您棋艺精进！")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun updateDifficultyButton() {
        val label = when (currentDifficulty) {
            Difficulty.EASY -> "难度：初级"
            Difficulty.MEDIUM -> "难度：中级"
            Difficulty.HARD -> "难度：高级"
        }
        binding.btnDifficulty.text = label
    }
}

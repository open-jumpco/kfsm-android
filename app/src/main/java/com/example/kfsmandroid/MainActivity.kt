package com.example.kfsmandroid

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), Turnstile {
    private var _locked: Boolean = true
    private val fsm: TurnstileFSM
    private lateinit var coinButton: Button
    private lateinit var passButton: Button
    private lateinit var turnstileState: TextView
    private lateinit var messageText: TextView

    init {
        fsm = TurnstileFSM(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coinButton = findViewById(R.id.coinButton)
        coinButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                fsm.coin()
            }
        }
        passButton = findViewById(R.id.passButton)
        passButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                fsm.pass()
            }
        }
        turnstileState = findViewById(R.id.turnstileState)
        messageText = findViewById(R.id.message)
        runBlocking {
            updateViewState()
        }
    }

    suspend fun updateViewState() {
        val textId = when (fsm.currentState()) {
            TurnstileState.LOCKED -> R.string.locked_state
            TurnstileState.UNLOCKED -> R.string.unlocked_state
        }
        runOnUiThread {
            turnstileState.setText(textId)
            TurnstileEvent.values().forEach { event ->
                when (event) {
                    TurnstileEvent.PASS -> passButton.isEnabled = fsm.allowed(event)
                    TurnstileEvent.COIN -> coinButton.isEnabled = fsm.allowed(event)
                }
            }
        }
    }

    suspend fun updateMessage(id: Int, error: Boolean) {

        val color = if (error) {
            Color.RED
        } else {
            Color.BLUE
        }
        runOnUiThread {
            messageText.setTextColor(color)
            messageText.setText(id)
        }
        Timer("ClearMessage", false).schedule(if (error) 5000L else 2000L) {
            this@MainActivity.runOnUiThread {
                messageText.setText("");
            }
        }
    }

    override val locked: Boolean
        get() = _locked

    override suspend fun lock() {
        require(!locked) { "Expected to be unlocked" }
        _locked = true
        updateViewState()
    }

    override suspend fun unlock() {
        require(locked) { "Expected to be locked" }
        _locked = false
        updateViewState()
    }

    override suspend fun returnCoin() {
        updateMessage(R.string.return_coin_message, false)
    }

    override suspend fun alarm() {
        updateMessage(R.string.alarm_message, true)
    }

    override suspend fun lockOnTimeout() {
        _locked = true
        updateMessage(R.string.timeout_message, false)
        updateViewState()
    }
}

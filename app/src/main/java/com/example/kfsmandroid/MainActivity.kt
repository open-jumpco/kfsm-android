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
import java.lang.RuntimeException
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), Turnstile {
    private var _locked: Boolean = true
    private val fsm: TurnstileFSM
    private lateinit var coinButton: Button
    private lateinit var passButton: Button
    private lateinit var turnstileState: TextView
    private lateinit var message1Text: TextView
    private lateinit var message2Text: TextView

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
        message1Text = findViewById(R.id.message1)
        message2Text = findViewById(R.id.message2)
        runBlocking {
            updateViewState()
        }
    }

    suspend fun updateViewState() {
        runOnUiThread {
            val textId = when (fsm.currentState()) {
                TurnstileState.LOCKED -> R.string.locked_state
                TurnstileState.UNLOCKED -> R.string.unlocked_state
            }
            turnstileState.setText(textId)
            TurnstileEvent.values().forEach { event ->
                when (event) {
                    TurnstileEvent.PASS -> passButton.isEnabled = fsm.allowed(event)
                    TurnstileEvent.COIN -> coinButton.isEnabled = fsm.allowed(event)
                }
            }
        }
    }

    suspend fun updateMessage(id: Int, error: Boolean, msgId: Int = 1) {

        val color = if (error) {
            Color.RED
        } else {
            Color.BLUE
        }
        runOnUiThread {
            val mt = when {
                msgId == 1 -> message1Text
                msgId == 2 -> message2Text
                else -> throw RuntimeException("Cannot find message $msgId");
            }
            mt.setTextColor(color)
            mt.setText(id)
        }
        Timer("ClearMessage", false).schedule(if (error) 5000L else 2000L) {
            this@MainActivity.runOnUiThread {
                message1Text.setText("");
                message2Text.setText("");
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
        updateMessage(R.string.return_coin_message, false, 2)
        updateViewState()
    }

    override suspend fun alarm() {
        updateMessage(R.string.alarm_message, true)
        updateViewState()
    }

    override suspend fun lockOnTimeout() {
        _locked = true
        updateMessage(R.string.timeout_message, true)
        updateViewState()
    }
}

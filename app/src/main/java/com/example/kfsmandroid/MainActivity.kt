package com.example.kfsmandroid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), Turnstile {
    private var _locked: Boolean = true
    private val fsm = TurnstileFSM(this)
    private lateinit var coinButton: Button
    private lateinit var passButton: Button
    private lateinit var turnstileState: TextView
    private lateinit var messageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coinButton = findViewById(R.id.coinButton)
        coinButton.setOnClickListener {
            fsm.coin()
            updateViewState()
        }
        passButton = findViewById(R.id.passButton)
        passButton.setOnClickListener {
            fsm.pass()
            updateViewState()
        }
        turnstileState = findViewById(R.id.turnstileState)
        messageText = findViewById(R.id.message)
        updateViewState()
    }

    fun updateViewState() {
        val textId = when (fsm.currentState()) {
            TurnstileState.LOCKED -> R.string.locked_state
            TurnstileState.UNLOCED -> R.string.unlocked_state
        }
        turnstileState.setText(textId)
        TurnstileEvent.values().forEach { event ->
            when (event) {
                TurnstileEvent.PASS -> passButton.isEnabled = fsm.allowed(event)
                TurnstileEvent.COIN -> coinButton.isEnabled = fsm.allowed(event)
            }
        }
    }

    fun updateMessage(id: Int) {
        messageText.setText(id)
    }

    override val locked: Boolean
        get() = _locked

    override fun lock() {
        require(!locked) { "Expected to be unlocked" }
        _locked = true
    }

    override fun unlock() {
        require(locked) { "Expected to be locked" }
        _locked = false
    }

    override fun returnCoin() {
        updateMessage(R.string.return_coin_message)
    }

    override fun alarm() {
        updateMessage(R.string.alarm_message)
    }
}

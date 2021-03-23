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
import mu.KotlinLogging
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), Turnstile {
    private var _locked: Boolean = true
    private val fsm: TurnstileFSM = TurnstileFSM(this)
    private lateinit var coinButton: Button
    private lateinit var passButton: Button
    private lateinit var turnstileState: TextView
    private lateinit var messageText: TextView

    companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coinButton = findViewById(R.id.coinButton)
        coinButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                fsm.coin()
                logger.info { "click:coin" }
            }
        }
        passButton = findViewById(R.id.passButton)
        passButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                fsm.pass()
                logger.info { "click:coin" }
            }
        }
        turnstileState = findViewById(R.id.turnstileState)
        messageText = findViewById(R.id.message)
        runBlocking {
            updateViewState(fsm.currentState())
        }
    }

    override suspend fun updateViewState(currentState: TurnstileState) {
        logger.debug { "updateViewState:$currentState" }
        val textId = when (currentState) {
            TurnstileState.LOCKED -> R.string.locked_state
            TurnstileState.UNLOCKED -> R.string.unlocked_state
        }
        runOnUiThread {
            turnstileState.setText(textId)
            val text = turnstileState.text
            logger.debug { "updateViewState:$text" }
            TurnstileEvent.values().forEach { event ->
                val allowed = fsm.allowed(event)
                logger.info { "allowed:$event:$allowed" }
                when (event) {
                    TurnstileEvent.PASS -> passButton.isEnabled = allowed
                    TurnstileEvent.COIN -> coinButton.isEnabled = allowed
                }
            }
        }
    }

    private fun updateMessage(id: Int, error: Boolean) {
        val color = if (error) {
            Color.RED
        } else {
            Color.BLUE
        }
        runOnUiThread {
            messageText.setTextColor(color)
            messageText.setText(id)
            val text = messageText.text
            logger.info { "updateMessage:$text:$error" }
        }
        Timer("ClearMessage", false).schedule(if (error) 5000L else 2000L) {
            this@MainActivity.runOnUiThread { messageText.text = "" }
        }
    }

    override val locked: Boolean
        get() = _locked

    override suspend fun lock() {
        logger.debug("lock")
        require(!locked) { "Expected to be unlocked" }
        _locked = true
    }

    override suspend fun unlock() {
        logger.debug("unlock")
        require(locked) { "Expected to be locked" }
        _locked = false
    }

    override suspend fun returnCoin() {
        logger.debug("returnCoin")
        updateMessage(R.string.return_coin_message, false)
    }

    override suspend fun alarm() {
        logger.debug("alarm")
        updateMessage(R.string.alarm_message, true)
    }

    override suspend fun lockOnTimeout() {
        logger.debug("lockOnTimeout")
        _locked = true
        updateMessage(R.string.timeout_message, false)
    }
}

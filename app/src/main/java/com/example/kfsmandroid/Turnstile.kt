package com.example.kfsmandroid

import io.jumpco.open.kfsm.async.asyncStateMachine

interface Turnstile {
    val locked: Boolean

    suspend fun lock()
    suspend fun unlock()
    suspend fun returnCoin()
    suspend fun alarm()
    suspend fun lockOnTimeout()
}

enum class TurnstileEvent {
    COIN,
    PASS
}

enum class TurnstileState {
    LOCKED,
    UNLOCKED
}

class TurnstileFSM(val turnstile: Turnstile) {
    companion object {
        val definition = asyncStateMachine(
            TurnstileState.values().toSet(),
            TurnstileEvent.values().toSet(),
            Turnstile::class
        ) {
            initialState { if (locked) TurnstileState.LOCKED else TurnstileState.UNLOCKED }
            default {
                action { _, _, _ ->
                    alarm()
                }
            }
            whenState(TurnstileState.LOCKED) {
                onEvent(TurnstileEvent.COIN to TurnstileState.UNLOCKED) {
                    unlock()
                }
            }
            whenState(TurnstileState.UNLOCKED) {
                timeout(TurnstileState.LOCKED, 3000) {
                    lockOnTimeout()
                }
                onEvent(TurnstileEvent.PASS to TurnstileState.LOCKED) {
                    lock()
                }
                onEvent(TurnstileEvent.COIN) {
                    returnCoin()
                }
            }
        }.build()
    }

    private val fsm = definition.create(turnstile)
    fun currentState() = fsm.currentState
    suspend fun coin() = fsm.sendEvent(TurnstileEvent.COIN)
    suspend fun pass() = fsm.sendEvent(TurnstileEvent.PASS)
    fun allowed(event: TurnstileEvent) = fsm.allowed().contains(event)
}

package com.lambao.animike.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base cho mọi ViewModel theo MVI (State/Event/Effect) — xem docs/ROADMAP.md
 * mục 1. Mỗi screen chỉ cần khai `XxxContract.kt` (State/Event/Effect) và kế
 * thừa lớp này thay vì tự viết lại state/effect boilerplate.
 */
abstract class BaseViewModel<State, Event, Effect>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    abstract fun onEvent(event: Event)

    protected fun currentState(): State = _state.value

    protected fun setState(reducer: State.() -> State) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}

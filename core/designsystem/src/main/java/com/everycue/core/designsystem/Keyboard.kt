package com.everycue.core.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Clears retained field focus and closes the IME without changing form values. */
@Composable
fun rememberKeyboardDismissAction(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboard) {
        {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
}

/** Mobile lists should yield the whole screen to content as soon as the user scrolls. */
@Composable
fun DismissKeyboardOnScroll(listState: LazyListState) {
    val dismissKeyboard = rememberKeyboardDismissAction()
    LaunchedEffect(listState, dismissKeyboard) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling -> if (isScrolling) dismissKeyboard() }
    }
}

@Composable
fun DismissKeyboardOnScroll(scrollState: ScrollState) {
    val dismissKeyboard = rememberKeyboardDismissAction()
    LaunchedEffect(scrollState, dismissKeyboard) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling -> if (isScrolling) dismissKeyboard() }
    }
}

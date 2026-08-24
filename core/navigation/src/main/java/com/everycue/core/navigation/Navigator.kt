package com.everycue.core.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(
    private val state: NavigationState,
) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks) {
            state.topLevelRoute = route
        } else {
            state.backStacks.getValue(state.topLevelRoute).add(route)
        }
    }

    fun selectTopLevel(route: NavKey) {
        require(route in state.backStacks) { "Unknown top-level route: $route" }
        if (state.topLevelRoute == route) {
            popToRoot(route)
        } else {
            state.topLevelRoute = route
        }
    }

    fun openInTopLevel(topLevelRoute: NavKey, destination: NavKey) {
        require(topLevelRoute in state.backStacks) { "Unknown top-level route: $topLevelRoute" }
        val stack = state.backStacks.getValue(topLevelRoute)
        while (stack.size > 1) stack.removeLastOrNull()
        stack.add(destination)
        state.topLevelRoute = topLevelRoute
    }

    /**
     * @return true when app navigation consumed back; false at the start root so the Activity can exit.
     */
    fun goBack(): Boolean {
        val currentStack = state.backStacks.getValue(state.topLevelRoute)
        return when {
            currentStack.size > 1 -> {
                currentStack.removeLastOrNull()
                true
            }
            state.topLevelRoute != state.startRoute -> {
                state.topLevelRoute = state.startRoute
                true
            }
            else -> false
        }
    }

    private fun popToRoot(route: NavKey) {
        val stack = state.backStacks.getValue(route)
        while (stack.size > 1) stack.removeLastOrNull()
    }
}


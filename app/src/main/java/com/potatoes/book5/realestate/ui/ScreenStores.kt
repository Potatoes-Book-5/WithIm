package com.potatoes.book5.realestate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * 화면 스택 항목마다 ViewModelStore를 따로 둔다. 스택에서 빠진 항목의 ViewModel은 정리한다.
 * 같은 단지를 다시 열면 새 ViewModel로 다시 불러온다. 주행 화면으로 잠시 바뀌어도 스택에 남아 있으면 유지된다.
 */
class ScreenStores(private val parent: ViewModelStoreOwner) {
    private val owners = mutableMapOf<String, ViewModelStoreOwner>()

    fun ownerFor(key: String): ViewModelStoreOwner = owners.getOrPut(key) { ScreenOwner(parent) }

    fun retainOnly(keys: Collection<String>) {
        (owners.keys - keys.toSet()).forEach { owners.remove(it)?.viewModelStore?.clear() }
    }

    fun clear() = retainOnly(emptyList())

    private class ScreenOwner(private val parent: ViewModelStoreOwner) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = ViewModelStore()
        private val defaults = parent as? HasDefaultViewModelProviderFactory
        override val defaultViewModelProviderFactory: ViewModelProvider.Factory
            get() = defaults?.defaultViewModelProviderFactory ?: ViewModelProvider.NewInstanceFactory()
        override val defaultViewModelCreationExtras: CreationExtras
            get() = defaults?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    }
}

/** 스택 위치와 화면으로 항목을 구분한다. 같은 단지가 스택에 두 번 있어도 따로 관리된다. */
fun screenKey(index: Int, screen: Screen): String = "$index:$screen"

@Composable
fun rememberScreenStores(stack: List<Screen>): ScreenStores {
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val stores = remember(parent) { ScreenStores(parent) }
    val keys = stack.mapIndexed(::screenKey)
    DisposableEffect(keys) {
        stores.retainOnly(keys)
        onDispose { }
    }
    DisposableEffect(stores) { onDispose { stores.clear() } }
    return stores
}

@Composable
fun ProvideScreenStore(stores: ScreenStores, key: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides stores.ownerFor(key), content = content)
}

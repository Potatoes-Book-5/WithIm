package com.potatoes.book5.realestate.ui.scrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle

/** SCR-007 스크랩 목록. ViewModel과 Effect를 연결한다. */
@Composable
fun ScrapListRoute(
    changedOnly: Boolean,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ScrapListViewModel = viewModel(
        factory = AppViewModels.Factory,
        extras = AppViewModels.extrasWith(AppViewModels.SCRAP_FILTER_KEY, if (changedOnly) ScrapFilter.CHANGED else ScrapFilter.ALL),
    ),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScrapListEffect.OpenDetail -> onOpenDetail(effect.id)
                ScrapListEffect.Close -> onBack()
            }
        }
    }
    ScrapListScreen(state = state, onIntent = viewModel::onIntent)
}

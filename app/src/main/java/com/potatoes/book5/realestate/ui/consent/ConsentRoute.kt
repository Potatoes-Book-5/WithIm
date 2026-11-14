package com.potatoes.book5.realestate.ui.consent

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.common.LargeSnackbarHost
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-006 동의와 설정. ViewModel과 Effect를 연결한다. [editing]이면 홈의 설정에서 연 수정 모드다. */
@Composable
fun ConsentRoute(
    onOpenInterestConditions: () -> Unit,
    onOpenHome: () -> Unit,
    editing: Boolean = false,
    onClose: () -> Unit = {},
    viewModel: ConsentViewModel = viewModel(
        factory = AppViewModels.Factory,
        extras = AppViewModels.extrasWith(AppViewModels.SETTINGS_EDITING_KEY, editing),
    ),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    // 결과는 MainActivity.onResume에서 PleosFusedLocationBridge가 다시 확인한다. 거부하면 위치 입력은 PERMISSION_REQUIRED다(재요청 흐름은 T-029 범위 밖).
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ConsentEffect.OpenInterestConditions -> onOpenInterestConditions()
                ConsentEffect.OpenHome -> onOpenHome()
                ConsentEffect.Close -> onClose()
                ConsentEffect.RequestLocationPermission ->
                    if (ContextCompat.checkSelfPermission(context, FUSED_LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                        locationPermission.launch(FUSED_LOCATION_PERMISSION)
                    }
                ConsentEffect.ShowDataDeleted -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(context.getString(R.string.settings_data_deleted))
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        ConsentScreen(state = state, onIntent = viewModel::onIntent)
        LargeSnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.wpx))
    }
}

/** FusedLocation 위치 API 권한(protection level dangerous). 매니페스트 선언과 같은 값이다. */
private const val FUSED_LOCATION_PERMISSION = "pleos.car.permission.FUSED_LOCATION"

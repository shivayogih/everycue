package com.everycue.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @DrawableRes val image: Int,
    @StringRes val imageDescription: Int,
    @StringRes val eyebrow: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

private val onboardingPages = listOf(
    OnboardingPage(R.drawable.onboarding_welcome, R.string.onboarding_welcome_image, R.string.onboarding_welcome_eyebrow, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
    OnboardingPage(R.drawable.onboarding_track, R.string.onboarding_track_image, R.string.onboarding_track_eyebrow, R.string.onboarding_track_title, R.string.onboarding_track_body),
    OnboardingPage(R.drawable.onboarding_pack, R.string.onboarding_pack_image, R.string.onboarding_pack_eyebrow, R.string.onboarding_pack_title, R.string.onboarding_pack_body),
    OnboardingPage(R.drawable.onboarding_renew, R.string.onboarding_renew_image, R.string.onboarding_renew_eyebrow, R.string.onboarding_renew_title, R.string.onboarding_renew_body),
)

@Composable
fun FirstRunFlow(state: ProfileUiState, onIntent: (ProfileIntent) -> Unit) {
    var showProfile by rememberSaveable { mutableStateOf(false) }
    if (showProfile) {
        ProfileForm(
            existing = null,
            busy = state.isBusy,
            error = state.errorMessage,
            submitLabel = stringResource(R.string.start_everycue),
            onSave = { onIntent(ProfileIntent.Save(it, finishAfterSave = false)) },
        )
    } else {
        TutorialPager(onFinished = { showProfile = true })
    }
}

@Composable
fun ProfileLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun TutorialPager(onFinished: () -> Unit) {
    val pager = rememberPagerState(pageCount = onboardingPages::size)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 20.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = onboardingPages[index]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(page.image),
                    contentDescription = stringResource(page.imageDescription),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentScale = ContentScale.Fit,
                )
                Text(stringResource(page.eyebrow), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            onboardingPages.indices.forEach { index ->
                Spacer(
                    Modifier.padding(4.dp).size(if (pager.currentPage == index) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (pager.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (pager.currentPage == onboardingPages.lastIndex) onFinished()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
        ) { Text(stringResource(if (pager.currentPage == onboardingPages.lastIndex) R.string.create_local_profile else R.string.next)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(state: ProfileUiState, onIntent: (ProfileIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        ProfileForm(
            existing = state.profile,
            busy = state.isBusy,
            error = state.errorMessage,
            submitLabel = stringResource(R.string.save_profile),
            onSave = { onIntent(ProfileIntent.Save(it, finishAfterSave = true)) },
            onShare = state.profile?.let { { onIntent(ProfileIntent.Share) } },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun ProfileForm(
    existing: LocalProfile?,
    busy: Boolean,
    @StringRes error: Int?,
    submitLabel: String,
    onSave: (LocalProfile) -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
) {
    var firstName by rememberSaveable(existing) { mutableStateOf(existing?.firstName.orEmpty()) }
    var lastName by rememberSaveable(existing) { mutableStateOf(existing?.lastName.orEmpty()) }
    var countryCode by rememberSaveable(existing) { mutableStateOf(existing?.countryCode ?: "+91") }
    var mobile by rememberSaveable(existing) { mutableStateOf(existing?.mobileNumber.orEmpty()) }
    var email by rememberSaveable(existing) { mutableStateOf(existing?.email.orEmpty()) }
    var address by rememberSaveable(existing) { mutableStateOf(existing?.address.orEmpty()) }
    var pincode by rememberSaveable(existing) { mutableStateOf(existing?.pincode.orEmpty()) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(if (existing == null) R.string.profile_create_title else R.string.profile_edit_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.profile_privacy_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(firstName, { firstName = it }, label = { Text(stringResource(R.string.first_name)) }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(lastName, { lastName = it }, label = { Text(stringResource(R.string.last_name)) }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(countryCode, { countryCode = it.filter { char -> char == '+' || char.isDigit() }.take(5) }, label = { Text(stringResource(R.string.country_code)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.width(104.dp))
            OutlinedTextField(mobile, { mobile = it.filter(Char::isDigit).take(15) }, label = { Text(stringResource(R.string.mobile_number)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
        }
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email_id)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            address,
            { value -> if (value.lines().size <= 5) address = value },
            label = { Text(stringResource(R.string.address_five_lines)) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(pincode, { pincode = it.take(12) }, label = { Text(stringResource(R.string.pincode)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { onSave(LocalProfile(firstName, lastName, countryCode, mobile, email, address, pincode)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(submitLabel) }
        onShare?.let {
            OutlinedButton(onClick = it, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Share, null); Text(stringResource(R.string.share_profile))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}


package com.japl.repzi.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.japl.repzi.core.domain.model.ExerciseDetail
import com.japl.repzi.core.ui.component.EmptyState

@Composable
fun ExerciseDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    ExerciseDetailScreen(
        uiState = uiState,
        onBack = onBack,
        // A phone without a browser cannot open links; the link then simply does nothing.
        onOpenUrl = { runCatching { uriHandler.openUri(it) } },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseDetailScreen(
    uiState: ExerciseDetailUiState,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exercise_detail_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val detail = uiState.detail
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            detail == null -> EmptyState(stringResource(R.string.exercise_detail_not_found), Modifier.padding(padding))
            else -> DetailContent(detail, onOpenUrl, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailContent(detail: ExerciseDetail, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    val linkStyles = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    // wger descriptions are HTML from contributors. fromHtml keeps text, emphasis and links only; nothing is executed.
    val description = remember(detail.description, linkStyles, onOpenUrl) {
        detail.description?.let {
            AnnotatedString.fromHtml(it.trim(), linkStyles = linkStyles, linkInteractionListener = webLinkListener(onOpenUrl))
        }
    }
    val hasAttribution = detail.author != null || detail.licenseName != null || detail.sourceUrl != null

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = stringResource(detail.muscleGroup.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (description != null) {
            Text(text = description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
        }
        if (hasAttribution) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            detail.author?.let { Text(stringResource(R.string.exercise_detail_author, it)) }
            detail.licenseName?.let { name ->
                val label = stringResource(R.string.exercise_detail_license, name)
                val url = detail.licenseUrl
                if (url != null) LinkText(label) { onOpenUrl(url) } else Text(label, modifier = Modifier.padding(top = 8.dp))
            }
            detail.sourceUrl?.let { url ->
                LinkText(stringResource(R.string.exercise_detail_source)) { onOpenUrl(url) }
                Text(
                    text = stringResource(R.string.exercise_detail_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Opens only web links found in a description. The text is written by contributors, so a link with another scheme
 * (`tel:`, `market:`, an app deep link) is ignored instead of launching another app.
 */
internal fun webLinkListener(onOpenUrl: (String) -> Unit) = LinkInteractionListener { link ->
    val url = (link as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
    if (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) onOpenUrl(url)
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

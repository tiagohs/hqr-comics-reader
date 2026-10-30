package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.presentation.library.CategoriesViewModel
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_move_down
import com.tiagohs.hqr.ui.resources.action_move_up
import com.tiagohs.hqr.ui.resources.action_save
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.categories_delete
import com.tiagohs.hqr.ui.resources.categories_deleted
import com.tiagohs.hqr.ui.resources.categories_empty
import com.tiagohs.hqr.ui.resources.categories_hint
import com.tiagohs.hqr.ui.resources.categories_name
import com.tiagohs.hqr.ui.resources.categories_name_empty
import com.tiagohs.hqr.ui.resources.categories_name_taken
import com.tiagohs.hqr.ui.resources.categories_new
import com.tiagohs.hqr.ui.resources.categories_rename
import com.tiagohs.hqr.ui.resources.categories_uncategorized_hint
import com.tiagohs.hqr.ui.resources.categories_works
import com.tiagohs.hqr.ui.resources.library_categories
import com.tiagohs.hqr.ui.theme.HqrIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-LIB-003: create, rename, order and delete categories, with "Desfazer" after deleting (RF-53). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel(),
    library: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val libraryState by library.state.collectAsStateWithLifecycle()
    val counts = libraryState.tabs.associate { it.id to it.count }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.library_categories), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
            )
        },
        floatingActionButton = {
            val label = stringResource(Res.string.categories_new)
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                // The extended FAB does not expose its text to screen readers on every platform.
                modifier = Modifier.semantics { contentDescription = label },
                icon = { Icon(HqrIcons.Add, null) },
                text = { Text(stringResource(Res.string.categories_new)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Text(
                    stringResource(
                        if (state.categories.isEmpty() &&
                            !state.loading
                        ) {
                            Res.string.categories_empty
                        } else {
                            Res.string.categories_hint
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            itemsIndexed(state.categories, key = { _, c -> c.id }) { index, category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    supportingContent = { Text(stringResource(Res.string.categories_works, counts[category.id] ?: 0)) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(onClick = { viewModel.move(category.id, -1) }, enabled = index > 0) {
                                Icon(HqrIcons.ArrowUp, stringResource(Res.string.action_move_up))
                            }
                            IconButton(onClick = { viewModel.move(category.id, 1) }, enabled = index < state.categories.lastIndex) {
                                Icon(HqrIcons.ArrowDown, stringResource(Res.string.action_move_down))
                            }
                            IconButton(
                                onClick = { editing = category },
                            ) { Icon(HqrIcons.EditNote, stringResource(Res.string.categories_rename)) }
                            IconButton(onClick = {
                                viewModel.delete(category.id) { deleted ->
                                    scope.launch {
                                        val result =
                                            snackbar.showSnackbar(
                                                getString(Res.string.categories_deleted, deleted.category.name, deleted.mangaIds.size),
                                                getString(Res.string.action_undo),
                                            )
                                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
                                    }
                                }
                            }) { Icon(HqrIcons.Delete, stringResource(Res.string.categories_delete)) }
                        }
                    },
                )
            }
            if (state.categories.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.categories_uncategorized_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
    if (creating) {
        NameDialog(Res.string.categories_new, "", { viewModel.problem(it) }, { creating = false }) {
            viewModel.create(it)
            creating = false
        }
    }
    editing?.let { category ->
        NameDialog(Res.string.categories_rename, category.name, { viewModel.problem(it, except = category.id) }, { editing = null }) {
            viewModel.rename(category.id, it)
            editing = null
        }
    }
}

@Composable
private fun NameDialog(
    title: org.jetbrains.compose.resources.StringResource,
    initial: String,
    problem: (String) -> CategoriesViewModel.NameProblem?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val issue = problem(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.categories_name)) },
                singleLine = true,
                isError = issue == CategoriesViewModel.NameProblem.TAKEN,
                supportingText = {
                    when (issue) {
                        CategoriesViewModel.NameProblem.TAKEN -> Text(stringResource(Res.string.categories_name_taken))
                        CategoriesViewModel.NameProblem.EMPTY -> Text(stringResource(Res.string.categories_name_empty))
                        null -> Unit
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = issue == null,
            ) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

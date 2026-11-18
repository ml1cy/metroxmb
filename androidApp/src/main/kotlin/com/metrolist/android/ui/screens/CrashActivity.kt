package com.metrolist.android.ui.screens

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.metrolist.android.MainActivity
import com.metrolist.android.R
import com.metrolist.android.utils.CrashHandler
import com.metrolist.android.utils.CrashLogUploader
import com.metrolist.android.utils.CrashPaste
import com.metrolist.android.utils.redactCrashLog
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.crash_close
import com.metrolist.shared.generated.resources.crash_copy_logs
import com.metrolist.shared.generated.resources.crash_description
import com.metrolist.shared.generated.resources.crash_logs
import com.metrolist.shared.generated.resources.crash_no_log
import com.metrolist.shared.generated.resources.crash_paste_id
import com.metrolist.shared.generated.resources.crash_paste_url
import com.metrolist.shared.generated.resources.crash_restart
import com.metrolist.shared.generated.resources.crash_retry
import com.metrolist.shared.generated.resources.crash_share_logs
import com.metrolist.shared.generated.resources.crash_title
import com.metrolist.shared.generated.resources.crash_upload
import com.metrolist.shared.generated.resources.crash_upload_disclosure
import com.metrolist.shared.generated.resources.crash_upload_failed
import com.metrolist.shared.generated.resources.crash_uploaded
import com.metrolist.shared.generated.resources.crash_uploading
import com.metrolist.shared.generated.resources.xmb_crash_open_paste
import com.metrolist.shared.ui.utils.setText
import com.metrolist.shared.xmb.screens.XmbFrameButton
import com.metrolist.shared.xmb.screens.XmbNoticeScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The crash screen the XMB way: a notice over the XMB's sky with the crash, and Restart, Share,
 * Copy, Upload (to the public paste service, after the disclosure) and Close. With
 * [CrashHandler.ACTION_LOGS] it lists the saved crash reports, each shared when chosen.
 */
class CrashActivity : ComponentActivity() {
    private val crashLog by lazy {
        CrashHandler
            .store(this)
            .list()
            .firstOrNull { it.id == intent.getStringExtra(CrashHandler.EXTRA_CRASH_ID) }
            ?.text
            ?: getString(R.string.crash_no_log)
    }
    private val crashViewModel: CrashViewModel by viewModels {
        CrashViewModel.Factory(crashLog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(CrashHandler.EXTRA_CRASH_ID)?.let { CrashHandler.store(this).acknowledge(it) }
        val failedProcess = intent.getIntExtra(CrashHandler.EXTRA_FAILED_PROCESS, 0)
        intent.removeExtra(CrashHandler.EXTRA_FAILED_PROCESS)
        if (failedProcess > 0 && failedProcess != Process.myPid()) Process.killProcess(failedProcess)
        WindowCompat.enableEdgeToEdge(window)

        setContent {
            var showLogs by remember { mutableStateOf(intent.action == CrashHandler.ACTION_LOGS) }
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            if (showLogs) {
                CrashLogs(
                    onClose = { if (intent.action == CrashHandler.ACTION_LOGS) finish() else showLogs = false },
                )
                return@setContent
            }
            val state = crashViewModel.uploadState
            val paste = (state as? CrashUploadState.Uploaded)?.paste
            val pasteIdLine = stringResource(Res.string.crash_paste_id, paste?.id.orEmpty())
            val pasteUrlLine = stringResource(Res.string.crash_paste_url, paste?.url.orEmpty())
            val status =
                when (state) {
                    CrashUploadState.Idle -> stringResource(Res.string.crash_upload_disclosure)
                    CrashUploadState.Uploading -> stringResource(Res.string.crash_uploading)
                    CrashUploadState.Failed -> stringResource(Res.string.crash_upload_failed)
                    is CrashUploadState.Uploaded -> "${stringResource(Res.string.crash_uploaded)}\n$pasteIdLine\n${state.paste.url}"
                }
            val buttons =
                buildList {
                    add(
                        XmbFrameButton(stringResource(Res.string.crash_restart)) {
                            startActivity(
                                Intent(this@CrashActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            finishAffinity()
                        },
                    )
                    add(
                        XmbFrameButton(stringResource(Res.string.crash_share_logs)) {
                            shareCrashLog(crashLogWithPaste(crashLog, paste, pasteIdLine, pasteUrlLine))
                        },
                    )
                    add(XmbFrameButton(stringResource(Res.string.crash_copy_logs)) { scope.launch { clipboard.setText(crashLog) } })
                    when (state) {
                        CrashUploadState.Idle -> {
                            add(XmbFrameButton(stringResource(Res.string.crash_upload), crashViewModel::upload))
                        }

                        CrashUploadState.Failed -> {
                            add(XmbFrameButton(stringResource(Res.string.crash_retry), crashViewModel::upload))
                        }

                        is CrashUploadState.Uploaded -> {
                            add(XmbFrameButton(stringResource(Res.string.xmb_crash_open_paste)) { openPaste(state.paste.url) })
                        }

                        CrashUploadState.Uploading -> {}
                    }
                    add(XmbFrameButton(stringResource(Res.string.crash_logs)) { showLogs = true })
                    add(XmbFrameButton(stringResource(Res.string.crash_close)) { finishAffinity() })
                }
            XmbNoticeScreen(
                id = "crash",
                title = stringResource(Res.string.crash_title),
                message = stringResource(Res.string.crash_description),
                details = "$status\n\n${redactCrashLog(crashLog).lineSequence().take(CRASH_PREVIEW_LINES).joinToString("\n")}",
                buttons = buttons,
                onBack = { finishAffinity() },
            )
        }
    }

    @Composable
    private fun CrashLogs(onClose: () -> Unit) {
        val reports = remember { CrashHandler.store(this).list().take(MAX_LISTED_REPORTS) }
        val format = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
        val close = stringResource(Res.string.crash_close)
        XmbNoticeScreen(
            id = "crash_logs",
            title = stringResource(Res.string.crash_logs),
            message = if (reports.isEmpty()) stringResource(Res.string.crash_no_log) else stringResource(Res.string.crash_share_logs),
            buttons =
                reports.map { report ->
                    XmbFrameButton(format.format(Date(report.timestamp))) { shareCrashLog(redactCrashLog(report.text)) }
                } +
                    XmbFrameButton(close, onClose),
            onBack = onClose,
        )
    }

    private fun shareCrashLog(crashLog: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val crashDir = File(cacheDir, "crashes").apply { mkdirs() }
            val crashFile = File(crashDir, "metrolist_crash_$timestamp.txt")
            crashFile.writeText(crashLog)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", crashFile)
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, crashLog)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_subject))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.crash_share_title)))
        } catch (_: Exception) {
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, crashLog)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_subject))
                }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.crash_share_title)))
        }
    }

    private fun openPaste(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: Exception) {
            Timber.w(e, "Could not open crash paste")
        }
    }

    private companion object {
        const val CRASH_PREVIEW_LINES = 6
        const val MAX_LISTED_REPORTS = 6
    }
}

internal fun crashLogWithPaste(
    crashLog: String,
    paste: CrashPaste?,
    pasteIdLine: String,
    pasteUrlLine: String,
): String =
    if (paste == null) {
        redactCrashLog(crashLog)
    } else {
        buildString {
            appendLine(pasteIdLine)
            appendLine(pasteUrlLine)
            appendLine()
            append(redactCrashLog(crashLog))
        }
    }

private sealed interface CrashUploadState {
    data object Idle : CrashUploadState

    data object Uploading : CrashUploadState

    data object Failed : CrashUploadState

    data class Uploaded(
        val paste: CrashPaste,
    ) : CrashUploadState
}

private class CrashViewModel(
    private val crashLog: String,
    private val uploader: CrashLogUploader = CrashLogUploader(),
) : ViewModel() {
    var uploadState by mutableStateOf<CrashUploadState>(CrashUploadState.Idle)
        private set

    private var uploadJob: Job? = null

    fun upload() {
        if (uploadJob?.isActive == true) return
        uploadState = CrashUploadState.Uploading
        uploadJob =
            viewModelScope.launch {
                uploadState =
                    try {
                        CrashUploadState.Uploaded(uploader.upload(crashLog))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.w(e, "Could not upload crash log")
                        CrashUploadState.Failed
                    }
            }
    }

    class Factory(
        private val crashLog: String,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CrashViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return CrashViewModel(crashLog) as T
        }
    }
}

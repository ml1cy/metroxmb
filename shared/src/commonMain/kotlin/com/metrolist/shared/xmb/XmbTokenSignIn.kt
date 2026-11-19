package com.metrolist.shared.xmb

import com.metrolist.innertubex.models.AccountInfo
import com.metrolist.shared.auth.AuthManager
import com.metrolist.shared.state.GlobalInnerTube
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.ui.TokenLoginCredentials
import com.metrolist.shared.ui.hasAuthCookie
import com.metrolist.shared.ui.parseToken
import com.metrolist.shared.ui.resolveCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** How a token sign-in ended. */
sealed interface XmbTokenResult {
    /** The text has no YouTube sign-in cookie. */
    data object Invalid : XmbTokenResult

    /** YouTube didn't accept it, or the network failed. */
    data object Failed : XmbTokenResult

    data class Saved(
        val credentials: TokenLoginCredentials,
    ) : XmbTokenResult
}

/** Whether [text] (a pasted token or cookie header) carries a YouTube sign-in cookie. */
fun looksLikeSignInToken(text: String): Boolean = text.isNotBlank() && hasAuthCookie(parseToken(text).cookie)

/**
 * Signs in with a token (the stock "Token login": the cookie header, or the app's exported token
 * text with visitor data and channel): checks it with YouTube and saves it, as the stock screen did.
 */
suspend fun xmbTokenSignIn(
    text: String,
    authManager: AuthManager = AuthManager(),
): XmbTokenResult {
    val parsed = parseToken(text)
    if (!hasAuthCookie(parsed.cookie)) return XmbTokenResult.Invalid
    return try {
        var credentials =
            parsed.resolveCredentials(
                existingCookie = authManager.innerTubeCookie,
                existingVisitorData = authManager.visitorData,
                existingDataSyncId = authManager.dataSyncId,
                existingAuthUser = authManager.authUser,
                existingAccountInfo =
                    AccountInfo(
                        authManager.accountName,
                        authManager.accountEmail,
                        authManager.accountChannelHandle,
                        authManager.accountThumbnail,
                    ),
            )
        if (credentials.visitorData.isBlank()) {
            GlobalInnerTube.instance.fetchFreshVisitorData()?.takeIf(String::isNotBlank)?.let {
                credentials =
                    credentials.copy(visitorData = it)
            }
        }
        val validated =
            withTimeoutOrNull(10_000L) {
                authManager.validateAndSaveCredentials(
                    cookie = credentials.cookie,
                    visitorData = credentials.visitorData,
                    dataSyncId = credentials.dataSyncId,
                    authUser = credentials.authUser,
                )
            } == true
        if (!validated) return XmbTokenResult.Failed
        SongLikeState.refreshLikesFromRemoteAsync()
        XmbTokenResult.Saved(
            TokenLoginCredentials(
                cookie = authManager.innerTubeCookie,
                visitorData = authManager.visitorData,
                dataSyncId = authManager.dataSyncId,
                authUser = authManager.authUser,
                accountInfo =
                    AccountInfo(
                        authManager.accountName,
                        authManager.accountEmail,
                        authManager.accountChannelHandle,
                        authManager.accountThumbnail,
                    ),
            ),
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        XmbTokenResult.Failed
    }
}

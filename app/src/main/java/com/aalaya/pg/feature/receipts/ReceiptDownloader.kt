package com.aalaya.pg.feature.receipts

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams a receipt PDF from the backend into the app cache and returns a
 * content:// Uri (via FileProvider) that any PDF viewer can open. The bytes are
 * written off the main thread.
 */
@Singleton
class ReceiptDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    /**
     * @param mine true for the tenant self-service endpoint (`/me/receipts/...`),
     * false for the staff endpoint. Tenants are 403'd on the staff route.
     */
    suspend fun download(receiptId: String, receiptNumber: String, mine: Boolean = false): ApiResult<Uri> {
        val cached = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val safeName = receiptNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(dir, "$safeName.pdf")
            if (file.exists() && file.length() > 0) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else null
        }
        if (cached != null) return ApiResult.Success(cached)
        return when (
            val r = executor.execute {
                if (mine) api.downloadMyReceipt(receiptId) else api.downloadReceipt(receiptId)
            }
        ) {
            is ApiResult.Success -> withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
                val safeName = receiptNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val file = File(dir, "$safeName.pdf")
                r.data.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                ApiResult.Success(uri)
            }
            is ApiResult.Failure -> r
        }
    }
}

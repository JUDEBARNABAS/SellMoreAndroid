package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.local.TransactionEntity
import com.example.model.KioskConstants
import com.example.model.OrderLineItem
import com.example.model.formatTimestamp
import com.example.model.formatUgx
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    fun generateQrBitmap(content: String, sizePx: Int = 512): ImageBitmap? {
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap.asImageBitmap()
        }.getOrNull()
    }
}

object ReceiptPrinter {
    fun buildShareText(txn: TransactionEntity, items: List<OrderLineItem>): String {
        val sb = StringBuilder()
        sb.appendLine("=================================")
        sb.appendLine("SELLMORE AUTOMATED YOGURT DISPENSER")
        sb.appendLine("Machine: ${KioskConstants.MACHINE_ID}")
        sb.appendLine(KioskConstants.LOCATION)
        sb.appendLine("=================================")
        sb.appendLine("Receipt: ${txn.orderRef}")
        sb.appendLine("Date:    ${formatTimestamp(txn.timestamp, "yyyy-MM-dd HH:mm:ss")}")
        sb.appendLine("Method:  ${txn.paymentMethod}")
        sb.appendLine("Port:    ${txn.transportUsed}")
        sb.appendLine("---------------------------------")
        items.forEach { item ->
            sb.appendLine("${item.quantity}x ${item.name} [${item.slotCode}]")
            sb.appendLine("   @ ${formatUgx(item.unitPriceUgx)} = ${formatUgx(item.lineTotalUgx)}")
        }
        sb.appendLine("---------------------------------")
        sb.appendLine("Subtotal (VAT Incl): ${formatUgx(txn.subtotalUgx)}")
        sb.appendLine("VAT (18% Included):  ${formatUgx(txn.vatUgx)}")
        sb.appendLine("TOTAL PAID:          ${formatUgx(txn.totalUgx)}")
        if (txn.remainingBalanceUgx != null) {
            sb.appendLine("Remaining Balance:   ${formatUgx(txn.remainingBalanceUgx)}")
        }
        sb.appendLine("=================================")
        sb.appendLine("Enjoy your chilled 330ml probiotic yogurt!")
        return sb.toString()
    }

    fun shareReceipt(context: Context, txn: TransactionEntity, items: List<OrderLineItem>) {
        val text = buildShareText(txn, items)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SellMore Receipt ${txn.orderRef}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share SellMore Receipt"))
    }

    fun printReceipt(context: Context, txn: TransactionEntity, items: List<OrderLineItem>) {
        val rowsHtml = items.joinToString("") { item ->
            "<tr><td>${item.quantity}x ${item.name} (${item.slotCode})</td><td style='text-align:right'>${formatUgx(item.lineTotalUgx)}</td></tr>"
        }
        val balanceRow = if (txn.remainingBalanceUgx != null) {
            "<p><strong>Remaining Account Balance:</strong> ${formatUgx(txn.remainingBalanceUgx)}</p>"
        } else ""

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { font-family: monospace; padding: 20px; color: #111; }
                h2 { margin-bottom: 4px; }
                table { width: 100%; border-collapse: collapse; margin: 12px 0; }
                td { padding: 4px 0; border-bottom: 1px dashed #ccc; }
                .tot { font-size: 16px; font-weight: bold; }
              </style>
            </head>
            <body>
              <h2>SellMore Yogurt Dispenser</h2>
              <p>${KioskConstants.MACHINE_ID} • ${KioskConstants.LOCATION}</p>
              <p>Order Ref: <strong>${txn.orderRef}</strong><br/>
                 Date: ${formatTimestamp(txn.timestamp, "yyyy-MM-dd HH:mm:ss")}<br/>
                 Payment: ${txn.paymentMethod}<br/>
                 Hardware: ${txn.transportUsed}</p>
              <table>$rowsHtml</table>
              <p>Subtotal: ${formatUgx(txn.subtotalUgx)}<br/>
                 VAT (18% incl.): ${formatUgx(txn.vatUgx)}<br/>
                 <span class='tot'>TOTAL: ${formatUgx(txn.totalUgx)}</span></p>
              $balanceRow
              <p>Thank you for choosing SellMore!</p>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val adapter = view.createPrintDocumentAdapter("SellMore_${txn.orderRef}")
                printManager?.print(
                    "SellMore Receipt ${txn.orderRef}",
                    adapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }
}

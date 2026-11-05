package fr.carnetdefaerun.feature.builds

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QR codes de partage : dessin, et lecture d'un QR code dans une image, sur l'appareil (ZXing) : le repli toujours disponible quand le lecteur de
 * Google Play services manque (D-021). L'image est réduite avant décodage ; un QR code de build n'a pas besoin de
 * plus de 1 600 pixels de côté.
 */
object QrCodes {
    private const val MAX_SIDE = 1_600

    /** Matrice du QR code d'un texte (correction d'erreur M, sans marge : l'écran dessine la sienne), ou null. */
    fun encode(text: String): BitMatrix? = runCatching {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    }.getOrNull()

    /** Texte du QR code trouvé dans ces pixels (ARGB), ou null. */
    fun decode(width: Int, height: Int, pixels: IntArray): String? = runCatching {
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)
        QRCodeReader().decode(bitmap, hints).text
    }.getOrNull()

    /** Texte du QR code de l'image choisie, ou null si elle est illisible ou n'en contient pas. */
    suspend fun decode(context: Context, uri: Uri): String? = withContext(Dispatchers.Default) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val image = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val side = maxOf(info.size.width, info.size.height)
                if (side > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / side
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                }
            }
            val pixels = IntArray(image.width * image.height)
            image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            decode(image.width, image.height, pixels)
        }.getOrNull()
    }
}

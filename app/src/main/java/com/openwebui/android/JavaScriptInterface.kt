package com.openwebui.android

import android.Manifest
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * JavaScript ve Android arasında köprü sağlayan sınıf.
 * Web sayfasından Android API'lerine erişim sağlar.
 */
class JavaScriptInterface(private val context: Context) {

    companion object {
        private const val TAG = "JavaScriptInterface"
    }

    /**
     * Web sayfasından erişilebilir bir metot.
     * Kısa bir mesaj gösterir.
     */
    @JavascriptInterface
    fun showToast(message: String) {
        // Ensure Toast is shown on the main thread
        if (context is MainActivity) {
            context.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } else {
             // Fallback for other contexts, though unlikely in this setup
             Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Kamera izninin durumunu kontrol eder.
     * @return İzin durumu (true: izin verildi, false: izin verilmedi)
     */
    @JavascriptInterface
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Mikrofon izninin durumunu kontrol eder.
     * @return İzin durumu (true: izin verildi, false: izin verilmedi)
     */
    @JavascriptInterface
    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Depolama izninin durumunu kontrol eder.
     * @return İzin durumu (true: izin verildi, false: izin verilmedi)
     */
    @JavascriptInterface
    fun hasStoragePermission(): Boolean {
        // For Android 10+, WRITE permission is not needed for Downloads dir via MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE // Check WRITE for older versions
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Kamera izni ister.
     * Bu metot, MainActivity'deki requestPermission metodunu çağırır.
     */
    @JavascriptInterface
    fun requestCameraPermission() {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.requestPermission(
                    Manifest.permission.CAMERA,
                    MainActivity.REQUEST_CAMERA_PERMISSION
                )
            }
        }
    }

    /**
     * Mikrofon izni ister.
     * Bu metot, MainActivity'deki requestPermission metodunu çağırır.
     */
    @JavascriptInterface
    fun requestMicrophonePermission() {
        if (context is MainActivity) {
            context.runOnUiThread {
                context.requestPermission(
                    Manifest.permission.RECORD_AUDIO,
                    MainActivity.REQUEST_MICROPHONE_PERMISSION
                )
            }
        }
    }

    /**
     * Depolama izni ister.
     * Bu metot, MainActivity'deki requestPermission metodunu çağırır.
     */
    @JavascriptInterface
    fun requestStoragePermission() {
        // Only request if needed (Android 9 and below)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (context is MainActivity) {
                context.runOnUiThread {
                    context.requestPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        MainActivity.REQUEST_STORAGE_PERMISSION
                    )
                }
            }
        }
    }

    /**
     * Cihaz bilgilerini döndürür.
     * @return Cihaz bilgileri (JSON formatında)
     */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
            {
                "model": "${Build.MODEL}",
                "manufacturer": "${Build.MANUFACTURER}",
                "version": "${Build.VERSION.RELEASE}",
                "sdkVersion": ${Build.VERSION.SDK_INT},
                "darkMode": ${isDarkModeEnabled()}
            }
        """.trimIndent()
    }
    
    /**
     * Sistemin karanlık tema modunda olup olmadığını kontrol eder.
     * @return Karanlık tema durumu (true: karanlık tema etkin, false: açık tema etkin)
     */
    @JavascriptInterface
    fun isDarkModeEnabled(): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

     /**
     * Base64 kodlanmış veriyi dosya olarak kaydeder.
     *
     * @param base64Data Base64 kodlanmış veri (ön eksiz)
     * @param filename Kaydedilecek dosya adı
      * @param mimeType Dosya MIME türü
      */
     @Suppress("DEPRECATION") // addCompletedDownload eski API'ler için kullanılıyor
     @JavascriptInterface
     fun downloadFromBase64(base64Data: String, filename: String, mimeType: String) {
         Log.i(TAG, "Processing Base64 data for: $filename, MimeType: $mimeType")
         try {
            // Depolama izni kontrolü (Android 9 ve altı için)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Storage permission not granted for Base64 download (Android 9-)")
                showToast("Dosya kaydetmek için depolama izni gerekli.")
                requestStoragePermission() // İzin iste
                return
            }

            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            Log.d(TAG, "Decoded ${decodedBytes.size} bytes.")

            // Android 10 (Q) ve üzeri için MediaStore API kullan
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Using MediaStore API (Android 10+)")
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(decodedBytes)
                            Log.d(TAG, "File saved successfully via MediaStore: $uri")
                            showToast("Dosya indirildi: $filename")
                        } ?: run {
                            Log.e(TAG, "Failed to open output stream for URI: $uri")
                            showToast("Dosya kaydedilemedi (output stream açılamadı).")
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "MediaStore file write error: ${e.message}", e)
                        showToast("Dosya kaydedilemedi: ${e.message}")
                        // Hata durumunda oluşturulan URI'yi silmeyi dene
                        try { resolver.delete(uri, null, null) } catch (deleteEx: Exception) { Log.e(TAG, "Error deleting MediaStore entry after failed write", deleteEx) }
                    }
                } else {
                    Log.e(TAG, "MediaStore.Downloads.EXTERNAL_CONTENT_URI returned null.")
                    showToast("Dosya kaydedilemedi (MediaStore URI null).")
                }
            } else {
                // Android 9 (Pie) ve altı için eski yöntemi kullan
                Log.d(TAG, "Using legacy file saving method (Android 9-)")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    if (!downloadsDir.mkdirs()) {
                         Log.e(TAG, "Failed to create Downloads directory.")
                         showToast("İndirilenler klasörü oluşturulamadı.")
                         return
                    }
                }
                
                val file = File(downloadsDir, filename)
                try {
                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(decodedBytes)
                        Log.d(TAG, "File saved successfully (legacy): ${file.absolutePath}")
                    }

                    // DownloadManager'a bildir (isteğe bağlı, sadece bildirim için)
                    try {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.addCompletedDownload(
                            file.name, // Sadece dosya adı
                            file.name, // Açıklama
                            true,      // Medya tarayıcı tarasın mı?
                            mimeType,
                            file.absolutePath,
                            file.length(),
                            true       // Bildirim gösterilsin mi?
                        )
                         showToast("Dosya indirildi: $filename")
                    } catch (dmEx: Exception) {
                         Log.e(TAG, "Failed to notify DownloadManager", dmEx)
                         // Bildirim başarısız olsa bile dosya kaydedildi, yine de toast göster
                         showToast("Dosya indirildi (bildirim hatası): $filename")
                    }

                } catch (e: IOException) {
                    Log.e(TAG, "Legacy file write error: ${e.message}", e)
                    showToast("Dosya kaydedilemedi: ${e.message}")
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decode error: ${e.message}", e)
            showToast("Dosya verisi çözümlenemedi.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in downloadFromBase64: ${e.message}", e)
            showToast("Beklenmeyen indirme hatası: ${e.message}")
        }
    }

    /**
     * Verilen URL'den dosya indirmek için DownloadManager'ı kullanır.
     *
     * @param url İndirilecek dosyanın URL'si.
     * @param filename Kaydedilecek dosya adı.
     * @param mimeType Dosyanın MIME türü.
     */
    @Suppress("DEPRECATION") // WRITE_EXTERNAL_STORAGE izni ve setDestinationInExternalPublicDir eski API'ler için
    @JavascriptInterface
    fun downloadFromUrl(url: String, filename: String, mimeType: String) {
        Log.i(TAG, "Download requested via URL: $url, Filename: $filename, MimeType: $mimeType")

        // İndirme izni kontrolü (Android 9 ve altı için)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Storage permission not granted for URL download (Android 9-)")
            showToast("Dosya indirmek için depolama izni gerekli.")
            requestStoragePermission() // İzin iste
            return
        }

        if (!url.startsWith("http:") && !url.startsWith("https:")) {
            Log.w(TAG, "Unsupported URL scheme for downloadFromUrl: $url")
            showToast("Desteklenmeyen URL şeması: $url")
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            // User-Agent eklemek genellikle iyi bir fikirdir, ancak WebView'dan doğrudan alamayız.
            // request.addRequestHeader("User-Agent", userAgent) // Gerekirse manuel olarak eklenebilir.
            request.setTitle(filename)
            request.setDescription("Dosya indiriliyor...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // İndirme hedefini belirle
            // Android 10+ için MediaStore daha modern olsa da, DownloadManager hala public dizinleri kullanabilir.
            // DIRECTORY_DOWNLOADS public olduğu için genellikle sorun olmaz.
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Log.d(TAG, "Download enqueued via downloadFromUrl for $filename")
            showToast("İndirme başladı: $filename")

        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager enqueue failed in downloadFromUrl: ${e.message}", e)
            showToast("İndirme başlatılamadı: ${e.message}")
        }
    }


    /**
     * Method to convert blobUrl to Blob, then process Base64 data on native side
     *
     * @param blobUrl The blob URL to process.
     * @param filename The suggested filename.
     * @param mimeType The suggested mime type.
     * @return A JavaScript string to be evaluated by WebView.
     */
    fun getBase64StringFromBlobUrl(blobUrl: String, filename: String, mimeType: String): String {
        // Escape filename and mimeType for JavaScript string literals
        val escapedFilename = filename.replace("'", "\\'").replace("\n", "\\n")
        val escapedMimeType = mimeType.replace("'", "\\'").replace("\n", "\\n")
        // Blob URL should generally be safe, but basic escaping can be added if needed
        val escapedBlobUrl = blobUrl.replace("'", "\\'")

        Log.i(TAG, "Creating JS script for Blob URL: $escapedBlobUrl, Filename: $escapedFilename, MimeType: $escapedMimeType")

        // Script to convert blob URL to Base64 data in Web layer, then process it in Android layer
        // Passes filename and mimetype along
        // Escape ALL '$' intended for JS with '\$' for Kotlin
        // Use escaped Kotlin variables in the JS string
        val script = """
            javascript:(async () => {
                const blobUrl = '$escapedBlobUrl';
                let finalFilename = '$escapedFilename'; // Use escaped filename passed from Kotlin
                let finalMimeType = '$escapedMimeType'; // Use escaped mimetype passed from Kotlin
                console.log('[JS] Starting Base64 conversion for:', blobUrl);
                console.log('[JS] Initial Filename:', finalFilename);
                console.log('[JS] Initial MimeType:', finalMimeType);
                try {
                    const response = await fetch(blobUrl);
                    if (!response.ok) {
                        // Escape '$' in JS template literal for Kotlin: \${'$'}{...}
                        throw new Error(`HTTP error! status: \${'$'}{response.status}`);
                    }
                    const blob = await response.blob();
                    finalMimeType = blob.type || finalMimeType; // Update mimetype if blob has one
                    console.log('[JS] Blob fetched successfully. MimeType:', finalMimeType);

                    // --- Refine Filename ---
                    let originalFilenameWithoutExt = finalFilename.includes('.') ? finalFilename.substring(0, finalFilename.lastIndexOf('.')) : finalFilename;
                    // Regex needs double escaping: once for Kotlin string, once for JS string
                    let isGenericFilename = !originalFilenameWithoutExt || /^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}\$/.test(originalFilenameWithoutExt) || originalFilenameWithoutExt === 'file' || originalFilenameWithoutExt === 'download'; // Escaped $ in regex

                    let fileExtension = '';
                    if (finalMimeType && finalMimeType !== 'application/octet-stream') {
                        const mimeParts = finalMimeType.split('/');
                        if (mimeParts.length > 1 && mimeParts[1] !== 'octet-stream') {
                            let extCandidate = mimeParts[1].split('+')[0].split(';')[0].trim();
                            if (extCandidate) {
                                fileExtension = '.' + (extCandidate === 'jpeg' ? 'jpg' : extCandidate);
                            }
                        }
                    }

                    // If filename is generic or has no extension, generate a default one
                    if (isGenericFilename || !finalFilename.includes('.')) {
                        const date = new Date();
                        const timestamp = date.getFullYear().toString() +
                                          (date.getMonth() + 1).toString().padStart(2, '0') +
                                          date.getDate().toString().padStart(2, '0') + '_' +
                                          date.getHours().toString().padStart(2, '0') +
                                          date.getMinutes().toString().padStart(2, '0') +
                                          date.getSeconds().toString().padStart(2, '0');

                        let baseType = 'file'; // Default base type
                        if (finalMimeType) {
                            const typePart = finalMimeType.split('/')[0];
                            if (['image', 'video', 'audio', 'text'].includes(typePart)) {
                                baseType = typePart;
                            } else if (finalMimeType.includes('pdf')) {
                                baseType = 'document';
                            } else if (finalMimeType.includes('zip') || finalMimeType.includes('compressed')) {
                                baseType = 'archive';
                            }
                        }

                        // Ensure we have an extension, default to .bin if unknown
                        if (!fileExtension) {
                            fileExtension = '.bin';
                            console.log('[JS] Unknown MIME type, using default extension .bin');
                        }

                        // Use standard JS concatenation
                        finalFilename = baseType + '_' + timestamp + fileExtension;
                        console.log('[JS] Generated default filename:', finalFilename);
                    } else {
                         // Ensure existing filename has the correct extension if possible
                         if (fileExtension && !finalFilename.toLowerCase().endsWith(fileExtension.toLowerCase())) {
                             // Remove existing wrong extension if present
                             if (finalFilename.includes('.')) {
                                 finalFilename = finalFilename.substring(0, finalFilename.lastIndexOf('.'));
                             }
                             finalFilename += fileExtension;
                             console.log('[JS] Corrected extension for existing filename:', finalFilename);
                         } else if (!finalFilename.includes('.')) {
                             // Add extension if missing (should be rare after previous checks)
                             finalFilename += (fileExtension || '.bin');
                             console.log('[JS] Added missing extension to existing filename:', finalFilename);
                         }
                    }

                    // Clean filename again after potential changes
                    // Regex needs double escaping for backslash
                    finalFilename = finalFilename.replace(/[/\\\\?%*:|"<>]/g, '_');
                    console.log('[JS] Final filename for Base64:', finalFilename);

                    // --- Convert to Base64 ---
                    const reader = new FileReader();
                    reader.onloadend = () => {
                        const base64data = reader.result;
                        if (base64data && base64data.includes(',')) {
                            console.log('[JS] Base64 conversion successful. Calling Android.downloadFromBase64...');
                            // Remove incorrect '\$' escape before Android object
                            Android.downloadFromBase64(base64data.split(',')[1], finalFilename, finalMimeType);
                            try { window.URL.revokeObjectURL(blobUrl); console.log('[JS] Blob URL revoked after Base64.'); } catch(e){ console.warn('[JS] RevokeObjectURL error:', e); }
                        } else {
                             console.error('[JS] FileReader result empty or invalid.');
                             // Remove incorrect '\$' escape before Android object
                             Android.showToast('Dosya okuma hatası (Base64)');
                        }
                    };
                    reader.onerror = (e) => {
                        console.error('[JS] FileReader error:', e);
                        // Remove incorrect '\$' escape before Android object
                        Android.showToast('Dosya okuma hatası (Base64)');
                    };
                    reader.readAsDataURL(blob);
                } catch (error) {
                    console.error('[JS] Error fetching or processing blob:', error);
                    // Remove incorrect '\$' escape before Android object
                    Android.showToast('Blob işlenirken hata: ' + error.message);
                }
            })();
        """.trimIndent() // Use trimIndent to remove leading whitespace
        return script
    }
}

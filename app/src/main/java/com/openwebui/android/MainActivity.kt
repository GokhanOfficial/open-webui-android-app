package com.openwebui.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.LinearLayout
// import android.content.Intent // Tarayıcı Intent'i için eklendi - Çakışma nedeniyle kaldırıldı
// import android.net.Uri // Uri işlemleri için eklendi - Çakışma nedeniyle kaldırıldı
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        // private const val OPEN_WEB_UI_URL = "http://192.168.1.105:8080" // Yerel test URL'si
         private const val OPEN_WEB_UI_URL = "https://URL" // Kullanıcının Open Web UI URL'si
        const val REQUEST_CAMERA_PERMISSION = 1001
        const val REQUEST_MICROPHONE_PERMISSION = 1002
        const val REQUEST_STORAGE_PERMISSION = 1003
        private const val FILE_CHOOSER_RESULT_CODE = 2001
        private const val CAMERA_CAPTURE_RESULT_CODE = 2002
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var retryButton: Button
    private lateinit var jsInterface: JavaScriptInterface // JavaScriptInterface örneğini tutmak için

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoPath: String? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Başlık çubuğunu gizle
        supportActionBar?.hide()
        
        // Ses yöneticisini ayarla
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL

        // View'ları başlat
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayout)
        retryButton = findViewById(R.id.retryButton)

        // JavaScriptInterface örneğini oluştur
        jsInterface = JavaScriptInterface(this)

        // Retry butonuna tıklama işleyicisi
        retryButton.setOnClickListener {
            loadWebView()
        }

        // WebView'ı yapılandır
        setupWebView()

        // WebView'ı yükle
        loadWebView()

        // Activity Result Launcher'ı başlat
        setupActivityResultLaunchers()
    }

    private fun setupActivityResultLaunchers() {
        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            var results: Array<Uri>? = null
            // Kamera veya dosya seçici sonucunu işle
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data == null || data.data == null) {
                    // Kamera ile çekilen fotoğraf
                    if (cameraPhotoPath != null) {
                        results = arrayOf(Uri.parse(cameraPhotoPath))
                    }
                } else {
                    // Dosya seçici sonucu
                    val dataString = data.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
            cameraPhotoPath = null // Temizle
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Dosya indirme işleyicisi ekle
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ -> // contentLength -> _ olarak değiştirildi
            try {
                Log.d(TAG, "Download requested: URL=$url, MimeType=$mimetype, ContentDisposition=$contentDisposition")

                // İndirme izni kontrolü
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q
                ) {
                    Log.d(TAG, "Storage permission not granted. Requesting...")
                    requestPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        REQUEST_STORAGE_PERMISSION
                    )
                    return@setDownloadListener
                }
                
                // Dosya adını al
                var filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                Log.d(TAG, "Guessed filename: $filename")
                
                // Blob URL'lerini işle (Yeni JavaScriptInterface yöntemini kullanarak)
                if (url.startsWith("blob:")) {
                    Log.d(TAG, "Blob URL detected. Evaluating JS script via getBase64StringFromBlobUrl.")
                    // Dosya adını ve mimetype'ı JavaScript'e gönder
                    val script = jsInterface.getBase64StringFromBlobUrl(url, filename, mimetype)
                    webView.evaluateJavascript(script, null)
                    return@setDownloadListener 
                }
                
                // Normal HTTP/HTTPS URL'lerini işle
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    Log.d(TAG, "Normal HTTP/HTTPS URL detected. Using DownloadManager.")
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setMimeType(mimetype)
                    request.addRequestHeader("User-Agent", userAgent)
                    request.setTitle(filename)
                    request.setDescription("Dosya indiriliyor...")
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    
                    // İndirme hedefini belirle
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                    
                    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    try {
                        dm.enqueue(request)
                        Log.d(TAG, "Download enqueued for $filename")
                        Toast.makeText(
                            applicationContext,
                            "İndirme başladı: $filename",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                         Log.e(TAG, "DownloadManager enqueue failed: ${e.message}", e)
                         Toast.makeText(
                            applicationContext,
                            "İndirme başlatılamadı (DownloadManager): ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    // Desteklenmeyen URL şeması
                    Log.w(TAG, "Unsupported URL scheme: $url")
                    Toast.makeText(
                        applicationContext,
                        "Desteklenmeyen URL şeması",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download listener error: ${e.message}", e)
                Toast.makeText(
                    applicationContext,
                    "İndirme hatası: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        // WebView ayarlarını yapılandır
        @Suppress("DEPRECATION") // Eski dosya erişim ayarları için uyarıyı bastır
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = true // Değiştirildi: Kullanıcı etkileşimi gerektir
            databaseEnabled = true
            setGeolocationEnabled(true)
            
            // Mikrofon ve kamera erişimi için kullanıcı etkileşimi gerektirme
            javaScriptCanOpenWindowsAutomatically = true
            
            // CORS sorunlarını çözmek için ek ayarlar
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            
            // Önbelleği etkinleştir (varsayılan davranış)
            cacheMode = WebSettings.LOAD_DEFAULT // LOAD_NO_CACHE yerine LOAD_DEFAULT kullanıldı
        }
        
        // WebView'ın JavaScript konsolunu logcat'e yönlendir
        WebView.setWebContentsDebuggingEnabled(true)

        // JavaScript arayüzünü ekle
        webView.addJavascriptInterface(jsInterface, "Android") // Önceden oluşturulan örneği kullan

        // WebViewClient'ı ayarla
        webView.webViewClient = object : WebViewClient() {

            // URL yüklemelerini yönet: Ana URL dışındaki linkleri tarayıcıda aç
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false // URL alınamazsa WebView'da devam et
                val targetHost = url.host
                val baseHost = Uri.parse(OPEN_WEB_UI_URL).host

                Log.d(TAG, "shouldOverrideUrlLoading: URL=$url, TargetHost=$targetHost, BaseHost=$baseHost")

                // Eğer hostlar farklıysa veya URL şeması http/https değilse (örn. mailto:, tel:) tarayıcıda aç
                if (targetHost != baseHost || !(url.scheme == "http" || url.scheme == "https")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, url)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Yeni görevde başlat
                        startActivity(intent)
                        Log.d(TAG, "Opening URL in external browser: $url")
                        return true // WebView'ın URL'yi yüklemesini engelle
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not open URL in external browser: $url", e)
                        Toast.makeText(this@MainActivity, "Bu bağlantı açılamadı.", Toast.LENGTH_SHORT).show()
                        return true // Hata durumunda da WebView yüklemesini engelle
                    }
                }

                // Hostlar aynıysa WebView içinde yükle
                Log.d(TAG, "Loading URL in WebView: $url")
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                webView.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                
                // Çerezleri kaydet
                CookieManager.getInstance().flush()
                
                // Karanlık tema bilgisini web sayfasına aktar
                val isDarkMode = jsInterface.isDarkModeEnabled()
                webView.evaluateJavascript(
                    """
                    (function() {
                        // Karanlık tema durumunu global değişkene kaydet
                        window.isAndroidDarkMode = $isDarkMode;
                        
                        // Eğer sayfa karanlık tema desteği varsa ve sistem karanlık temadaysa
                        if ($isDarkMode) {
                            // HTML elementine dark-theme sınıfı ekle (yaygın bir yaklaşım)
                            document.documentElement.classList.add('dark-theme', 'dark');
                            
                            // Özel bir olay tetikle
                            const darkModeEvent = new CustomEvent('androidDarkModeChanged', { 
                                detail: { darkMode: true } 
                            });
                            window.dispatchEvent(darkModeEvent);
                            
                            console.log('Android karanlık tema algılandı ve web sayfasına bildirildi');
                        }
                    })();
                    """.trimIndent(), null
                )
                
                // Sayfa yüklendiğinde izinleri kontrol et (Mikrofon testi kaldırıldı)
                // if (ContextCompat.checkSelfPermission(
                //         this@MainActivity,
                //         Manifest.permission.RECORD_AUDIO
                //     ) == PackageManager.PERMISSION_GRANTED
                // ) {
                //     // Mikrofon izni zaten verilmiş, JavaScript ile test et (KALDIRILDI)
                // }
                
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Kamera izni zaten verilmiş, JavaScript ile test et
                    webView.evaluateJavascript(
                        "navigator.mediaDevices.getUserMedia({video: true}).then(function(stream) {" +
                        "  console.log('Kamera erişimi başarılı');" +
                        "  stream.getTracks().forEach(track => track.stop());" +
                        "}).catch(function(err) {" +
                        "  console.error('Kamera erişimi hatası:', err);" +
                        "});", null
                    )
                }
                
                // --- getUserMedia Sarmalayıcı Enjeksiyonu ---
                webView.evaluateJavascript("""
                    (function() {
                        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia && !navigator.mediaDevices.getUserMedia.isWrapped) {
                            const originalGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
                            console.log('[Android Wrapper] Wrapping navigator.mediaDevices.getUserMedia');

                            navigator.mediaDevices.getUserMedia = function(constraints) {
                                console.log('[Android Wrapper] getUserMedia called with constraints:', JSON.stringify(constraints));

                                // Sadece ses isteniyorsa basit kısıtlamaları dene
                                if (constraints && constraints.audio && !constraints.video) {
                                     console.log('[Android Wrapper] Attempting with simplified audio constraints: { audio: true }');
                                     return originalGetUserMedia({ audio: true })
                                         .then(stream => {
                                             console.log('[Android Wrapper] getUserMedia (simplified) successful.');
                                             return stream;
                                         })
                                         .catch(errSimple => {
                                             console.error('[Android Wrapper] getUserMedia (simplified) failed:', errSimple.name, errSimple.message);
                                             console.log('[Android Wrapper] Retrying with original constraints...');
                                             // Basit deneme başarısız olursa orijinal kısıtlamalarla tekrar dene
                                             return originalGetUserMedia(constraints)
                                                 .then(stream => {
                                                     console.log('[Android Wrapper] getUserMedia (original) successful on retry.');
                                                     return stream;
                                                 })
                                                 .catch(errOriginal => {
                                                      console.error('[Android Wrapper] getUserMedia (original) failed on retry:', errOriginal.name, errOriginal.message);
                                                      // Orijinal hatayı fırlat
                                                      throw errOriginal;
                                                 });
                                         });
                                } else {
                                    // Video veya bilinmeyen kısıtlamalar varsa doğrudan orijinal fonksiyonu çağır
                                    console.log('[Android Wrapper] Calling original getUserMedia directly.');
                                    return originalGetUserMedia(constraints)
                                         .then(stream => {
                                             console.log('[Android Wrapper] getUserMedia (original) successful.');
                                             return stream;
                                         })
                                         .catch(err => {
                                              console.error('[Android Wrapper] getUserMedia (original) failed:', err.name, err.message);
                                              throw err;
                                         });
                                }
                            };
                            navigator.mediaDevices.getUserMedia.isWrapped = true; // Tekrar sarmalamayı önle
                            console.log('[Android Wrapper] getUserMedia wrapped successfully.');
                        } else {
                             if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                                 console.warn('[Android Wrapper] navigator.mediaDevices.getUserMedia not found.');
                             } else {
                                 console.log('[Android Wrapper] getUserMedia already wrapped.');
                             }
                        }
                    })();
                """.trimIndent(), null)
                // --- Enjeksiyon Sonu ---
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    webView.visibility = View.GONE
                    errorLayout.visibility = View.VISIBLE
                }
            }
        }

        // WebChromeClient'ı ayarla
        webView.webChromeClient = object : WebChromeClient() {
            // İzin isteklerini yönet
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val permissions = request.resources
                    val requestedPermissions = mutableListOf<String>()
                    
                    for (permission in permissions) {
                        when (permission) {
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    requestedPermissions.add(permission)
                                } else {
                                    requestPermission(
                                        Manifest.permission.RECORD_AUDIO,
                                        REQUEST_MICROPHONE_PERMISSION
                                    )
                                }
                            }
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    requestedPermissions.add(permission)
                                } else {
                                    requestPermission(
                                        Manifest.permission.CAMERA,
                                        REQUEST_CAMERA_PERMISSION
                                    )
                                }
                            }
                        }
                    }
                    
                    if (requestedPermissions.isNotEmpty()) {
                        request.grant(requestedPermissions.toTypedArray())
                    } else {
                        request.deny()
                    }
                }
            }
            
            // Dosya yükleme işleyicisi
            @Suppress("DEPRECATION") // WebChromeClient'daki eski metodu override ettiği için
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null) // Önceki callback'i iptal et
                this@MainActivity.filePathCallback = filePathCallback
                this@MainActivity.cameraPhotoPath = null // Önceki kamera yolunu temizle

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent?.resolveActivity(packageManager) != null) {
                    // Kamera için geçici dosya oluştur
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", cameraPhotoPath)
                    } catch (ex: IOException) {
                        Log.e(TAG, "Kamera için dosya oluşturulamadı", ex)
                    }

                    // Dosya başarıyla oluşturulduysa devam et
                    if (photoFile != null) {
                        cameraPhotoPath = "file:" + photoFile.absolutePath
                        val photoURI = FileProvider.getUriForFile(
                            this@MainActivity,
                            "${packageName}.fileprovider",
                            photoFile
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    } else {
                        takePictureIntent = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"

                val intentArray: Array<Intent?> = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Dosya Seçin")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                // startActivityForResult yerine yeni launcher'ı kullan
                fileChooserLauncher.launch(chooserIntent)
                return true
            }

            // Geolocation izni
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            // İlerleme durumunu göster
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun loadWebView() {
        webView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
        
        // Çerezleri etkinleştir
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        
        // Web sayfasını yükle
        webView.loadUrl(OPEN_WEB_UI_URL)
    }

    // Kamera için geçici dosya oluştur
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    // İzin isteme
    fun requestPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // İzin daha önce reddedildiyse açıklama göster
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                val message = when (permission) {
                    Manifest.permission.CAMERA -> getString(R.string.permission_camera_rationale)
                    Manifest.permission.RECORD_AUDIO -> getString(R.string.permission_mic_rationale)
                    else -> getString(R.string.permission_storage_rationale)
                }
                
                AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        }
    }

    // İzin sonuçlarını işle
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Kamera izni verildi
                    Log.d(TAG, "Kamera izni verildi")
                    // Kamera erişimini JavaScript ile test et
                    webView.evaluateJavascript(
                        "navigator.mediaDevices.getUserMedia({video: true}).then(function(stream) {" +
                        "  console.log('Kamera erişimi başarılı');" +
                        "  stream.getTracks().forEach(track => track.stop());" +
                        "}).catch(function(err) {" +
                        "  console.error('Kamera erişimi hatası:', err);" +
                        "});", null
                    )
                }
            }
            REQUEST_MICROPHONE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Mikrofon izni verildi
                    Log.d(TAG, "Mikrofon izni verildi")
                    
                    // Ses yöneticisini ayarla
                    // Mikrofon izni verildi, web sayfasının bunu işlemesine izin ver.
                    // Test betiği ve sayfa yenileme kaldırıldı.
                    Log.d(TAG, "Mikrofon izni verildi. Web sayfası erişimi yönetecek.")
                    
                    // İsteğe bağlı: İzin değişikliğini JavaScript'e bildirmek için basit bir olay tetiklenebilir
                    // webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('androidPermissionGranted', { detail: { permission: 'microphone' } }));", null)
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Depolama izni verildi
                    Log.d(TAG, "Depolama izni verildi")
                    webView.reload()
                }
            }
        }
    }

    // Aktivite sonuçlarını işle (Artık ActivityResultLauncher kullanıldığı için bu metoda gerek yok)
    /*
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Bu kod artık setupActivityResultLaunchers içindeki callback'te işleniyor
    }
    */

    // Geri tuşu davranışı
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    // Yapılandırma değişikliklerini algıla (tema değişiklikleri dahil)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Tema değişikliğini kontrol et
        val isDarkMode = jsInterface.isDarkModeEnabled()
        Log.d(TAG, "Yapılandırma değişti. Karanlık tema: $isDarkMode")
        
        // WebView'a tema değişikliğini bildir
        if (::webView.isInitialized && webView.visibility == View.VISIBLE) {
            webView.evaluateJavascript(
                """
                (function() {
                    // Karanlık tema durumunu güncelle
                    window.isAndroidDarkMode = $isDarkMode;
                    
                    // HTML elementinden mevcut tema sınıflarını kaldır
                    document.documentElement.classList.remove('dark-theme', 'dark');
                    
                    // Eğer karanlık tema etkinse sınıfları ekle
                    if ($isDarkMode) {
                        document.documentElement.classList.add('dark-theme', 'dark');
                    }
                    
                    // Özel bir olay tetikle
                    const darkModeEvent = new CustomEvent('androidDarkModeChanged', { 
                        detail: { darkMode: $isDarkMode } 
                    });
                    window.dispatchEvent(darkModeEvent);
                    
                    console.log('Android tema değişikliği algılandı ve web sayfasına bildirildi. Karanlık tema: $isDarkMode');
                })();
                """.trimIndent(), null
            )
        }
    }
}

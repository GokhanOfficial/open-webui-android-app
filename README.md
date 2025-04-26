# Open WebUI Android App

This project is a WebView-based application designed to provide a more integrated experience for using the [Open WebUI](https://github.com/open-webui/open-webui) web interface on Android devices. It aims to bring the core functionalities of Open WebUI to the mobile platform.

## ✨ Features

*   **Full Web Interface Access:** Provides access to all panels and functions available on the Open WebUI website through the app.
*   **Chat Export:** Download your active chat sessions in various formats like JSON, PDF, and TXT directly to your device.
*   **File Downloads:** Handles standard file downloads initiated from the web interface and supports downloading client-side generated files (Blob URLs).
*   **File Uploads:** Allows uploading files from device storage or capturing images directly using the camera within the WebView.
*   **Hardware Permissions:** Manages Android permissions for Camera, Microphone, and Storage access when required by the web interface.
*   **Audio Playback:** Enables playback of audio responses and other sounds from the Open WebUI interface within the app.
*   **System Dark Mode Sync:** Detects the Android system's dark mode setting and attempts to signal the web page to adjust its theme accordingly.
*   **External Link Handling:** Automatically opens links pointing outside the main Open WebUI URL in the device's default browser.
*   **Geolocation Support:** Supports geolocation requests originating from the web page.
*   **Improved Media Compatibility:** Includes enhancements for better compatibility with microphone and camera access within the WebView.
*   **JavaScript Bridge:** Allows the web interface to interact with native Android features like checking permissions, requesting permissions, showing toasts, and getting device info.

## ⚠️ Current Limitations & Known Issues

*   **No Offline Access:** Cannot access past chat history without an internet connection. This is due to the web-based nature of Open WebUI itself.
*   **Image Download in Chat:** Downloading image files directly from within the chat stream is currently not functional.

## 🚀 Installation & Usage

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/GokhanOfficial/open-webui-android-app.git 
    cd open-webui-android-app
    ```
2.  **Configure the URL:** Open the file `app/src/main/java/com/openwebui/android/MainActivity.kt`. Find the line containing `OPEN_WEB_UI_URL` and replace `"https://URL"` with the actual URL of your Open WebUI instance (e.g., `"http://192.168.1.100:8080"`).
3.  **Build the application:** Open a terminal in the project's root directory and run the appropriate Gradle command:
    *   For a debug build: `./gradlew assembleDebug` (Linux/macOS) or `gradlew.bat assembleDebug` (Windows)
    *   For a release build (requires signing configuration): `./gradlew assembleRelease` (Linux/macOS) or `gradlew.bat assembleRelease` (Windows)
4.  **Install the APK:** The generated APK file will be located in the `app/build/outputs/apk/` directory. Transfer this file to your Android device and install it.

## 🤝 Contributing

Contributions are welcome! If you have suggestions for improvements or find any bugs, please feel free to open an issue or submit a pull request.

---

# Open WebUI Android Uygulaması

Bu proje, [Open WebUI](https://github.com/open-webui/open-webui) web arayüzünü Android cihazlarda daha entegre bir deneyimle kullanmak için geliştirilmiş bir WebView tabanlı uygulamadır. Uygulama, Open WebUI'nin temel özelliklerini mobil platforma taşımayı hedefler.

## ✨ Öne Çıkan Özellikler

*   **Tam Web Arayüzü Erişimi:** Open WebUI web sitesinde bulunan tüm panellere ve işlevlere uygulama üzerinden erişim sağlar.
*   **Sohbet Dışa Aktarma:** Aktif sohbet oturumlarınızı JSON, PDF ve TXT gibi farklı formatlarda doğrudan cihazınıza indirebilirsiniz.
*   **Dosya İndirme:** Web arayüzünden başlatılan standart dosya indirmelerini yönetir ve istemci tarafında oluşturulan dosyaların (Blob URL'ler) indirilmesini destekler.
*   **Dosya Yükleme:** Cihaz deposundan dosya yüklemeye veya WebView içinden doğrudan kamera kullanarak resim çekmeye olanak tanır.
*   **Donanım İzinleri:** Web arayüzü tarafından gerektiğinde Kamera, Mikrofon ve Depolama erişimi için Android izinlerini yönetir.
*   **Ses Desteği:** Open WebUI arayüzündeki sesli yanıtları ve diğer sesleri uygulama içinde oynatır.
*   **Sistem Koyu Tema Senkronizasyonu:** Android sisteminin koyu tema ayarını algılar ve web sayfasının temasını buna göre ayarlaması için sinyal göndermeye çalışır.
*   **Harici Bağlantı Yönetimi:** Ana Open WebUI URL'si dışına işaret eden bağlantıları otomatik olarak cihazın varsayılan tarayıcısında açar.
*   **Coğrafi Konum Desteği:** Web sayfasından kaynaklanan coğrafi konum isteklerini destekler.
*   **Geliştirilmiş Medya Uyumluluğu:** WebView içinde mikrofon ve kamera erişimiyle daha iyi uyumluluk için iyileştirmeler içerir.
*   **JavaScript Köprüsü:** Web arayüzünün izinleri kontrol etme, izin isteme, toast mesajları gösterme ve cihaz bilgisi alma gibi yerel Android özellikleriyle etkileşim kurmasını sağlar.

## ⚠️ Mevcut Sınırlamalar ve Bilinen Sorunlar

*   **Çevrimdışı Erişim Yok:** İnternet bağlantısı olmadan geçmiş sohbet geçmişine erişilemez. Bu durum, Open WebUI'nin web tabanlı yapısından kaynaklanmaktadır.
*   **Sohbette Resim İndirme:** Sohbet akışı içindeki resim dosyalarını doğrudan indirme özelliği şu anda işlevsel değildir.

## 🚀 Kurulum ve Kullanım

1.  **Projeyi klonlayın:**
    ```bash
    git clone https://github.com/GokhanOfficial/open-webui-android-app.git
    cd open-webui-android-app
    ```
    *(`KULLANICI_ADINIZ` kısmını gerçek depo sahibinin kullanıcı adıyla değiştirin)*
2.  **URL'yi yapılandırın:** `app/src/main/java/com/openwebui/android/MainActivity.kt` dosyasını açın. `OPEN_WEB_UI_URL` içeren satırı bulun ve `"https://URL"` kısmını kendi Open WebUI örneğinizin gerçek URL'si ile değiştirin (örneğin, `"http://192.168.1.100:8080"`).
3.  **Uygulamayı derleyin:** Projenin kök dizininde bir terminal açın ve uygun Gradle komutunu çalıştırın:
    *   Hata ayıklama (debug) derlemesi için: `./gradlew assembleDebug` (Linux/macOS) veya `gradlew.bat assembleDebug` (Windows)
    *   Sürüm (release) derlemesi için (imzalama yapılandırması gerektirir): `./gradlew assembleRelease` (Linux/macOS) veya `gradlew.bat assembleRelease` (Windows)
4.  **APK'yı yükleyin:** Oluşturulan APK dosyası `app/build/outputs/apk/` dizininde bulunacaktır. Bu dosyayı Android cihazınıza aktarın ve yükleyin.

## 🤝 Katkıda Bulunma

Katkılarınız memnuniyetle karşılanır! İyileştirme önerileriniz veya bulduğunuz hatalar varsa, lütfen bir issue açmaktan veya pull request göndermekten çekinmeyin.

# Uğur TV - Android TV IPTV Uygulaması

Vestel 65" 4K Android TV 14 (ve tüm modern Android TV cihazları) için özel olarak geliştirilmiş, ultra düşük gecikmeli, yüksek performanslı ve akıcı IPTV uygulaması.

---

## 🚀 Öne Çıkan Özellikler

- **TiViMate Tarzı Çok Pencereli Rehber:**
  - **Sol Sütun:** Kategoriler (Tüm Kanallar, Favorilerim, Ulusal, Spor, Belgesel, vb.).
  - **Orta Sütun:** Anlık kanal arama, kanal logosu ve 60/120 FPS akıcı kaydırma.
  - **Sağ Sütun & Önizleme:** Seçilen kanalı duraksamadan canlı önizlemede oynatma, kumanda orta tuşuyla (OK) anında tam ekrana geçiş.
- **Ultra Düşük Gecikme (Low-Latency ExoPlayer / Media3):**
  - Donanımsal MediaCodec hızlandırmasıyla 300 ms gibi rekor bir sürede anında oynatma başlangıcı.
  - OkHttp HTTP/2 bağlantı havuzlama ve otomatik hata telafisi (.m3u8 -> .ts akıllı geçiş).
- **Vestel TV Kumandası (D-Pad) Optimizasyonu:**
  - **▲ / ▼ Tuşları:** Tam ekranda anlık kanal değiştirme (Zapping).
  - **◀ Tuşu:** Yayın durmadan soldan yarı saydam Hızlı Kanal Listesi çekmecesini açma.
  - **0 - 9 Sayı Tuşları:** Doğrudan kanal numarası tuşlayarak anında kanala zıplama.
  - **OK (Orta Tuş):** Kanal bilgi paneli (OSD), saat ve görüntü oranı kontrolü.
  - **Aspect Ratio Butonu:** 16:9, Fit, Fill (Ekranı Kapla), 4:3 formatları arasında geçiş.
- **Xtream Codes API Entegrasyonu & Test Modu:**
  - Sunucu URL'si, kullanıcı adı ve şifre ile güvenli giriş.
  - Oturum hatırlama ve otomatik bağlanma.
  - Abonelik olmadan da denenebilen hazır **Test / Demo Modu**.

---

## 📲 APK İndirme ve Kurulum

APK derlenmiş, imzalanmış ve yerel ağınızda dağıtıma hazır durumdadır:

- **Doğrudan İndirme Bağlantısı (Telefon / Tarayıcı):**
  `http://192.168.1.52:8888/UgurTV-Vestel-v1.0.0.apk`
- **Mobil İndirme Arayüzü:**
  `http://192.168.1.52:8888/`

### Vestel TV'ye Kurulum Adımları
1. Telefonunuzun tarayıcısından yukarıdaki bağlantıyı açarak APK dosyasını indirin.
2. Google Play Store'dan telefonunuza ve Vestel TV'nize **"Send Files to TV"** uygulamasını yükleyin.
3. Telefondan indirdiğiniz APK dosyasını televizyona gönderin.
4. TV'deki dosya yöneticisi (örn: FX File Explorer veya AnExplorer) üzerinden APK'ya tıklayarak kurulumu tamamlayın.

---

## 🛠 Teknik Detaylar

- **Target SDK:** 34 (Android TV OS 14)
- **Min SDK:** 26 (Android 8.0+)
- **Paket Adı:** `com.ugur.iptv`
- **Mimari:** Kotlin, AndroidX Media3 (ExoPlayer 1.4.0), Retrofit 2, OkHttp 4, Glide, Coroutines
- **Derleme Çıktısı:** `release/UgurTV-Vestel-v1.0.0.apk` (6.9 MB, İmzalı)

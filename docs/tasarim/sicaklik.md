# Sıcaklık sistemi — tasarım notu (madde 7, N1–N4, N6, N8)

## Gerçek dünya referansı

- **Isı dengesi.** Vücut sürekli metabolik ısı üretir (dinlenmede ~80–100 W, yürürken ~2×, koşarken 4–6×) ve deri ile
  ortam arasındaki farkla ısı kaybeder (Newton soğuma yasası: kayıp hızı farkla orantılı). Giysi bu iletkenliği böler
  ("clo" birimi): soğukta kaybı azaltır, ılık ortamda üretilen ısının atılmasını engeller. Islak giysi ve suya girmek
  iletkenliği 2–25 kat artırır; soğuk suda hipotermi havadakinden çok daha hızlıdır. İletkenlik iki yönlüdür: sıcak
  ortamda ıslak oyuncu ortamın dengesine de daha hızlı varır (çöl göletinde serin su dengeyi düşürür, oraya hızlı varılır).
- **Hipotermi evreleri** (çekirdek ısısı): hafif 35–32 °C titreme, el becerisinin azalması, yavaşlama; orta 32–28 °C
  titremenin durması, bilinç bulanıklığı; ağır < 28 °C organ yetmezliği. Uyarı işaretleri önce gelir, hasar sonra.
- **Hipertermi evreleri**: sıcak bitkinliği (terleme, baş dönmesi), sıcak çarpması (> 40 °C, bilinç kaybı). Çöl
  havası gündüz 45 °C'ye çıkar, gece 5 °C'ye iner (kuru hava ısıyı tutmaz); dağda her 1 km'de ~6,5 °C düşer; mağara
  yıl boyu yöre ortalamasında sabittir.
- **Cold Sweat HUD'unun okunurluğu:** küçük, ortada, rengiyle okunur (mavi/kırmızı), uç değerde titrer; sayı yerine
  yön duygusu verir. Bizim barımız aynı ilkeyle ama XP barının tam genişliğinde, ince ve yerel görünür.

## Minecraft'ta teknik

- **Ölçek:** vücut sıcaklığı −100…+100 "vücut birimi" (0 = nötr; yiyecek yüzdeleri bu ölçeğin bir yanına göredir).
  Ortam sıcaklığı okunur olsun diye °C; ısı alışverişinde `(T_ortam − T_konfor) × birim/°C` ile vücut birimine çevrilir.
- **Isı alışverişi (N4):** `B' = k·ıslaklık/(1+I) · [(E − B) + U·(a·(1+I) − 1)]`. `E` ortamın vücut birimi karşılığı,
  `I` toplam yalıtım (zırh + yün + tüy), `U` konfor ortamında metabolik ısının karşılığı, `a` etkinlik (durma 0,8,
  yürüme 1, koşma 1,6). Fark büyükse değişim hızlı, küçükse yavaş; yalıtım hem yavaşlatır hem dengeyi ısıya iter
  (kalın giysi soğukta kurtarır, sıcakta pişirir). Yün soğuma yönündeki hızı parça başına %25 keser (N3); aşırı soğuk
  eşiğinin altında yalnız yavaşlatır, metabolik ısı tutma kalkar ve vücut çok yavaş da olsa donar.
- **Ortam:** biyom (`getBaseTemperature`), gün eğrisi (öğleden 1 saat sonra tepe; kurak biyomda 3,5 kat genlik),
  yükseklik (deniz üstünde blok başına −0,06 °C), yeraltı (gökyüzü görmeyen derin noktada 12 °C'ye çekilme), kapalı
  alan, yağmur/kar/fırtına, su, güneş altı, yakın ısı/soğuk blokları (config listesi), geçici kaynaklar (yangın ve
  afet işçileri `ortamKaynagiEkle`), Nether/End, güneş evresi, donma ilerlemesi, kızıl cüce ofseti ve kış bölgesi.
- **Durum:** oyuncunun `PersistentData`'sında; 1 Hz sunucuda güncellenir; paket yalnız o oyuncuya, değişince (en çok
  1 Hz; kademe değişiminde ve `set`'te anında).
- **Etkiler:** soğukta önce yavaşlama (hareket hızı değiştiricisi), sonra buz kenarı, sonra `dogalafetler:donma`
  hasarı; sıcakta önce titreme (kamera yalpası + sıcak hava dalgası dokusu), sonra `dogalafetler:asiri_isinma`
  hasarı ve küçük alev parçacıkları. Hasar aralığı uç değere doğru kısalır (okunaklı, kademeli). Sunucu alev ve kar
  parçacıklarını yalnız çevredeki diğer oyunculara gönderir: oyuncunun kendi göğsünde doğan 8×8 sprite birinci şahısta
  ekranı kaplayan bloklu bir leke olurdu. Oyuncunun kendisi için istemci, durum paketindeki kademe hasarlıyken ve kamera
  birinci şahıs değilken saniyede bir aynı sayı ve saçılımda üretir (üçüncü şahısta kendi üstündeki alevi görür; pulsar-2
  PL-44, PL-45).

## HUD

- XP barının hemen üstünde, onunla aynı genişlikte (182 px) 5 px yüksekliğinde bar; kalpler ve açlık barı Forge'un
  `leftHeight/rightHeight`'ı ile yukarı kayar (başka modların barları da aynı yolla dizilir).
- Renk geçişi koyu buz mavisi → açık mavi → nötr kırık beyaz → turuncu → kor kırmızısı; nötr bant (±15) ince açık
  çizgilerle işaretli. Üstte beyaz kenarlı **vücut işaretçisi** (aşağı bakan üçgen + dikey çizgi), altta küçük **ortam
  oku** (yukarı bakan, yarı saydam): ok işaretçinin solundaysa vücut soğuyor, sağındaysa ısınıyor.
- **Vitrin anı:** süpernova sonrası dondurucu soğukta işaretçi barın sol ucuna dayanır, bar 1 px titrer, işaretçi
  buz mavisi ile beyaz arasında yanıp söner; ekran kenarlarından buz kristalleri içeri doğru büyür, kenarlar
  kırağıyla kaplanır. Sıcakta aynı anda turuncu-kırmızı vinyet dalgalanır ve kamera hafifçe yalpalar.
- **Yerini aldı (K-E69, N6 rev.):** vücut °C sayısı HUD'da varsayılan görünür (`sayiGoster`); bkz. "Güncelleme turu 3".
- Konum ve görünürlük CLIENT config'te (`sicaklik.hudGorunur`, `hudXOfset`, `hudYOfset`, `ekranEfektleri`, `sayiGoster`,
  `hissedilenGoster`, `durumIkonlari`, `animasyonlar`; `ortamOkuGoster` kalktı).

## Dokular

`tools/assets/sicaklik_dokulari.py` (Pillow) üretir: `textures/gui/sicaklik/bar.png` (bar, çerçeve, işaretçiler),
`textures/misc/sicaklik_buz.png` (256×256 buz kenarı, alfa), `textures/misc/sicaklik_sicak.png` (256×256 sıcak hava
vinyeti, dikey dalga deseni).

## Görsel cila (2026-11-07)

- Kalpler ve açlık barı işaretçinin tepesinden 2 px yukarıda (`leftHeight/rightHeight` 54 → 56): vanilla düşük canda
  kalpleri 1 px aşağı titrettiği için boşluksuz düzende kalpler işaretçiye biniyordu.
- Bar ve kaydırma yalnız sunucunun sıcaklık kapısı açıkken: Cold Sweat sunucuda yüklüyken (OTOMATIK) bar çizilmez ve vanilla
  barlar kaydırılmaz; sistem bizimse bar görünür. Cold Sweat'in vücut sıcaklığı göstergesinin konumu kendi işidir.

## Güncelleme turu 3 (K-E69, `.vibe/kararlar/sicaklik-3.md` S3-01…S3-11)

- **HUD:** yukarıdaki barın yerini band aldı (`textures/gui/sicaklik/hud.png` 256×64; `bar.png` kalktı): ortada vücut °C hapı (durum
  rengi, değişimde parlama, sayı durum renginin %65 beyaza açılmışı + koyu kontur), merkezden dışa dolan soğuk/sıcak tüpler,
  eşik çentikleri (hasar eşikleri kırmızı), hissedilen üçgeni, solda durum ikonları, sağda eğilim şevronları ve hissedilen °C.
  Kalpler/açlık `leftHeight/rightHeight` 58. Her atlas parçası karışımı açarak çizilir (S3-10).
- **Ölçek:** oyuncuya vücut °C (normal 37,0), `VucutOlcegi` °C = 37 + 0,05·B; iç saklama ±100 birim (eski kayıtlar aynı).
- **Hissedilen:** mekan + ıslaklık (−5·W °C) + rüzgâr soğuğu (NWS, T ≤ 10 °C, V > 4,8 km/h) + sol el; vücut hissedilenle ısı
  alışverişi yapar (iletkenlik `1 + 0,5·W`, suda 3).
- **Islaklık:** %0–100; suda 4 sn'de dolar, açık havada yağmurda ~83 sn, fırtınada ~50 sn; çatı/cam altında kurur (10 °C ~150 sn,
  sıcakta ve ateş yanında hızlı), yanan oyuncu anında kurur, ölümde sıfır; gösterge `dogalafetler:islak`.
- **Dört durum:** Üşüme ≤ 36,0 °C (ağız buharı `nefes_buhari`, ~50 tick'te bir nefes, dünya donarken yok), Donma ≤ 35,0 (hasar
  ≤ 33,5; buz kenarı), Bunalma ≥ 38,0 (ter damlası, kırmızı kenar `sicaklik_bunalma.png`, ek açlık), Yanma ≥ 39,5 (hasar ≥ 40,5).
  Kendi parçacığı yalnız üçüncü şahısta istemcide (S-08 rev.).
- **Termometre menüsü:** tuş K (kategori "Doğal Afetler"), 320×214 pano: en solda fareyi izleyen oyuncu modeli, vücut °C + eğilim +
  durum + 32–42 °C ölçeği, hissedilen/mekan/rüzgâr, mekan termometresi (−30…+50 °C), katkı dökümü; veri `sicaklik/dokum`.
- **Sol el:** Buz Kovası −12 °C (mekan > 25 °C'de `(T − 25)/10`/sn erir, 300'de su kovası), lav kovası +12 °C; ana elde etki yok.
- **Kaynaklar:** kömürleşmiş kütük 2:2, kömürleşmiş tahta 1,5:2 (kısık ama sürekli; `sicaklik.ayarSurumu` 1 liste göçü).
- **Görsel kabul:** kullanıcının oyun içi doğrulaması (2026-11-08).

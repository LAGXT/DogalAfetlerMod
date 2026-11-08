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
- Yazı yok (N6): sayı yalnız `/dogalafet sicaklik bilgi` çıktısında.
- Konum ve görünürlük CLIENT config'te (`sicaklik.hudGorunur`, `hudXOfset`, `hudYOfset`, `ortamOkuGoster`,
  `ekranEfektleri`).

## Dokular

`tools/assets/sicaklik_dokulari.py` (Pillow) üretir: `textures/gui/sicaklik/bar.png` (bar, çerçeve, işaretçiler),
`textures/misc/sicaklik_buz.png` (256×256 buz kenarı, alfa), `textures/misc/sicaklik_sicak.png` (256×256 sıcak hava
vinyeti, dikey dalga deseni).

## Görsel cila (2026-11-07)

- Kalpler ve açlık barı işaretçinin tepesinden 2 px yukarıda (`leftHeight/rightHeight` 54 → 56): vanilla düşük canda
  kalpleri 1 px aşağı titrettiği için boşluksuz düzende kalpler işaretçiye biniyordu.
- Bar ve kaydırma yalnız sunucunun sıcaklık kapısı açıkken: Cold Sweat sunucuda yüklüyken (OTOMATIK) bar çizilmez ve vanilla
  barlar kaydırılmaz; sistem bizimse bar görünür. Cold Sweat'in vücut sıcaklığı göstergesinin konumu kendi işidir.

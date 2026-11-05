# Ay Çarpması — tasarım notu (bölüm H, madde 4, 6, 8, 16; N10, N18)

Seviyesiz, tek seferlik. Gökyüzü `gokyuzu.md`, deniz yükselmesi suyu `sanal_su.md`, meteor ve krater `gunes_patlamasi.md` ile ortaktır.

## 1. Gerçek olay

- **Roche sınırı:** bir uydu gezegene çok yaklaşınca gelgit kuvveti onu kendi kütleçekiminden güçlü çeker; Ay Dünya'ya çarpmadan önce **parçalanır**: yüzeyde uzun kırıklar, sonra yörünge boyunca uzayan parça bulutu ve zamanla bir **halka**. Ordovisyen döneminde Dünya'nın bir halkası olduğuna dair kanıtlar, düşen parçaların bir kuşak boyunca krater bıraktığını gösterir.
- **Parçalanmış ay görünümü (New Scientist modellemesi):** parçaların gökyüzünde parlak bir yay çizmesi; atmosfere giren parçaların sürekli meteor yağmuru yaratması.
- **Büyük çarpma:** ufkun ötesinde bile görülen parlama, yükselen kızgın toz/buhar sütunu, dakikalar sonra gelen sarsıntı ve hava şok dalgası; okyanusta dev dalgalar. Ay'ın kaybı gelgitleri zayıflatır ama oyun bunu "deniz yükselmesi" olarak şartnameyle sabitler (gerçekçilik değil, kurgu).

Kaynaklar:
- https://www.skyatnightmagazine.com/space-science/roche-limit (Roche sınırı ve parçalanma)
- https://www.newscientist.com/article/2396204-slicing-the-moon-in-half-would-be-disastrous-for-earth-but-beautiful (parçalanmış ayın gökyüzü görünümü, meteor yağmuru)
- https://www.sciencedirect.com/science/article/pii/S0012821X24004230 (Ordovisyen halkası, düşen parçalar)

## 2. Minecraft'ta gösterim

**Zaman çizelgesi.**

| An | Olay | Görünüm |
|---|---|---|
| 0–60 sn | Siren | Ay, gündüz ya da gece fark etmeksizin gökyüzünde sabit bir yaklaşma yönünde (çarpma noktasına dönük, ufkun 35° üstü) zorla çizilir ve 1×'ten 14×'e büyür (`s = 1 + 13·t³`) |
| 20–60 sn | Parçalanma | Ay dokusunda parlayan turuncu-beyaz kırık ağı büyür (önceden çizilmiş 3 aşamalı çatlak katmanı), kenarlardan kopan 20–40 küçük parça ayın önünde ve arkasında yörünge yayı boyunca uzar, ay ince toz haresiyle çevrelenir |
| 50–60 sn | Son yaklaşma | Ay ufka iner, alt kenarı atmosfere girince kızarır, gökyüzü ve sis alttan turuncuya boyanır |
| 60 sn | **Çarpma** | Siren fade-out; ufkun ötesinde (oyunculardan ≥ 2000 blok, gerçek blok değişmez) beyaz parlama, ekran 0,5 sn %70 beyaz; 2 sn sonra ufuktan yükselen kızgın mantar sütunu (gökyüzü renderer'ında ufuk çizgisine yapışan dev sprite yığını, 45 sn'de ufkun 25° üstüne büyür) |
| 63–75 sn | **Sarsıntı** | Tüm oyuncularda 12 sn sarsıntı (deprem S4 genliği, mesafeden bağımsız), haritada yapraklar, cam, fenerler ve desteksiz bloklar düşer (deprem "sağlam olmayan" listesi, yoğunluk %30), toz perdesi yükselir |
| 70 sn → +1–2 dk | **Meteorlar** | Ay parçaları: güneş patlaması meteorlarıyla aynı varlık, ama gri-beyaz ay kayası dokusu (bazalt + kalsit karışımı görünümü) ve mavi-beyaz plazma izi; aynı krater, yangın ve ödül kuralları |
| + 10 dk | **Deniz yükselmesi** | Su seviyesi her yerde 0 → 4 blok, 5 dk boyunca (bkz. `sanal_su.md`) |
| sonra | **Aysız gökyüzü** | Gece ay yok; yıldız, galaksi, nebula. Gökyüzünde ince **enkaz halkası** (aşağıda) |

**Enkaz halkası.** Çarpmadan sonra 3 Minecraft günü boyunca gökyüzünde, ekvator düzleminde (doğu-batı ekseni boyunca, ufuktan ufka) ince, soluk gri-beyaz bir yay; içinde yavaş kayan parçacık parlamaları. Alfa 3 günde 0,6 → 0'a iner; son durum şartnamedeki "yalnız yıldız, galaksi, nebula" olur. Bu sürede ara sıra (oyuncu başına 1 / 3 dk) zararsız kayan yıldız: gökyüzünde 1 sn'lik parlak çizgi. Halka config ile kapatılabilir.

**Tsunami çarpanı dönemi.** Bu dönemde (varsayılan 4 gün) okyanus yüzeyi görsel olarak daha "huzursuz": sanal su olmayan gerçek denizde ek dalga yok (vanilla su), ama tsunami çekilmesi ×1,5 mesafe ve kıyıda gelgit çizgisi hissi için plajlarda ıslak kum parlaması (istemci taraflı, kum blokları üstünde ince parlak örtü).

**Afet sonrası iz.** Kraterler ve yanmış alanlar, ufukta günlerce yavaşça dağılan toz sütunu kalıntısı (1 günde söner), su altındaki alçak kıyılar, aysız ve çok yıldızlı geceler.

## 3. Vitrin anları

- **Parçalanan ayın son 8 saniyesi (ana vitrin, gece):** kamera bir tepede, ufka bakar (pitch +15°); ay gökyüzünün üçte birini kaplar, yüzeyinde parlayan turuncu kırıklar, çevresinde yörünge yayı boyunca uzayan parça bulutu, alt kenarı atmosferde kızarmış; ön planda silüet ağaçlar ve bir köy, zemin ay ışığıyla soluk mavi-gri.
- **Çarpma + mantar sütunu (8 sn):** aynı kamera; ufukta beyaz parlama, ardından yükselen turuncu-kahverengi sütun, sütunun altından sola-sağa yayılan toz duvarı, gökyüzünde ilk ay parçası meteorlarının mavi-beyaz izleri.
- **İlk aysız gece:** enkaz halkasının soluk yayı Samanyolu'nu çaprazlar; deniz yükselmesiyle sular altında kalmış bir iskele.

## 4. Ses tasarımı

- **Siren:** standart siren karakteri (`ses.md`), 60 sn boyunca döngü, çarpmada fade-out (bkz. Açık noktalar).

### Spike sonrası güncelleme, 2026-11-06

- **Siren kesinleşti (K0-19):** standart siren dosyası (`siren_standart.ogg`, 10 sn döngü birimi) 60 sn boyunca 6 tur çalar; çarpma anında sunucunun durdurma paketiyle 2 sn üstel fade-out (perde %20 iner). Güneş patlaması sireni kullanılmaz: o ses güneş patlamasına ayrılmış tanınır işarettir.
- **Ses yolu (N5):** `MASTER` kaynağı × "Sirenler" kaydırıcısı (varsayılan %28); açılış spin-up'ı 140 Hz'den 280 Hz'e (perde 0,5 → 1, ses motoru perdeyi 0,5'te kırpar).
- **Global siren:** paket bütün boyutlardaki oyunculara aynı anda gider (madde 3, N11). Siren sürerken Nether/End'e geçen oyuncuda siren boyut değişiminde aynı zarfla devam eder; `/stopsound` sireni susturmaz (K0-27). Siren sırasında giren oyuncu kalan süreyle sireni alır, böylece çarpma anı herkeste aynı kalan süreye denk gelir.
- Yaklaşma uğultusu siren çalarken (40–60 sn) modun ortam sesi kuralına uyar: siren boyunca −3 dB.
- **Yaklaşma (40–60 sn):** çok alçak, yükselen uğultu (sentez 18–45 Hz + 90 Hz harmonik, düşük düzey) — hissedilen ama konuşmayı bastırmayan; son 5 sn'de çatırtı katmanı (ayın parçalanması, uzak buz kırılması benzeri).
- **Çarpma:** parlama anında tam sessizlik 1,5 sn (ışık sesten önce gelir), sonra derin, uzun güm ve 10 sn süren gerilen gürleme; ardından deprem katmanı (`deprem.md`).
- **Meteorlar:** güneş patlaması meteoru ile aynı sesler, perde %10 yüksek (daha küçük, daha soluk parçalar).
- **Deniz yükselmesi:** sessiz ve yavaş; yalnız kıyıda hafif dalga çarpması döngüsü (sanal su kenarında).

## 5. Performans planı

| Kalite | Ay parça sprite | Mantar sütunu sprite | Sarsıntıda düşen blok (oyuncu başına) | Enkaz halkası | Toz perdesi |
|---|---|---|---|---|---|
| Düşük | 15 | 30 | 10 | tek doku | 60 |
| Orta | 25 | 60 | 20 | + parıltı | 150 |
| Yüksek | 40 | 100 | 35 | + parıltı | 300 |
| Ultra | 70 | 180 | 50 | + parıltı + 2 katman | 600 |

- Ay yaklaşması, parçalanma, mantar sütunu ve halka tamamen gökyüzü renderer'ında (dünya geometrisi yok, sabit maliyet).
- Sunucu: sarsıntıdaki blok düşmeleri oyuncu başına 48 blok yarıçapta, 12 sn'ye yayılmış kuyruk; meteorlar ve yangın ortak tavanlarla.

## 6. Açık noktalar — önerilen varsayılanlar

| Değer | Varsayılan |
|---|---|
| Gelişim kriteri | dünya günü ≥ 60 ve demir zırh/alet |
| Meteor süresi | 90 sn (şartname 1–2 dk) |
| Meteor sıklığı | oyuncu başına 1 / 8 sn, güneş patlaması meteorlarından %20 küçük |
| Tsunami çarpanı | ×3,5, 4 gün tam, sonra 2 günde doğrusal olarak ×1'e |
| Deniz yükselmesi | 4 blok (üst sınır config 6), 10 dk bekleme, 5 dk'da doğrusal, kalıcı |
| Sarsıntı | 12 sn, kamera 2,5°, blok düşüşü yoğunluğu %30 |
| Enkaz halkası | 3 gün, açık (config) |

### Spike sonrası güncelleme, 2026-11-06

- **Kapandı — siren süresi:** madde 5 ("güneş patlaması hariç 10 sn") ile bölüm H ("siren çaldıktan 60 sn sonra çarpma, siren o anda fade-out") arasında daha özel kural kazandı: ay çarpması sireni 60 sn, standart dosya döngüsü (K0-19; Berk değiştirebilir). "10 sn siren + 50 sn sessiz uğultu" alternatifi reddedildi.
- Gökyüzü durumu için bool yetmez: `AyDurumu {VAR, YAKLASIYOR, YOK}` + ay olay tick'i (yaklaşma 0–60 sn, parçalanma, 3 günlük enkaz halkası sönümü istemcide bu tick'ten hesaplanır; `gokyuzu.md` §2).
- Çarpma noktası görseldir; gerçek dünyada dev krater açılmaz (harita korunur, oyun devam eder).

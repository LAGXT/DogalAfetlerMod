# Güneş Patlaması — tasarım notu (bölüm D, madde 6, 7, 9, 16; N7, N9, N10)

Seviyesiz, en nadir ve en uzun afet. Gökyüzü çizimi `gokyuzu.md`'de, donma `donma.md`'de, siren `ses.md`'de; bu not uçtan uca koreografiyi ve her aşamanın görüntüsünü tanımlar.

## 1. Gerçek olay

- **Güneş patlaması (flare) ve koronal kütle atımı:** gerçekte birkaç dakikada parlaklık artışı, ardından saatler içinde gelen yüklü parçacık fırtınası; Dünya'da kutup ışıkları ekvatora yakın enlemlere iner (Carrington 1859: Karayipler'de kızıl aurora). Oyundaki "güneşin büyümesi" bilimsel değil sinematiktir; gözlemsel dayanağı kırmızı dev evresinde güneş diskinin gökyüzünün büyük bölümünü kaplaması ve rengin turuncuya kaymasıdır.
- **Süpernova görünümü:** tarihteki en parlak süpernova SN 1006 (görünür kadir ~−7,5), gündüz görülebilir ve gece gölge düşürecek parlaklıktaydı; aylarca görünür kaldı. SN 1054 (Yengeç) 23 gün gündüz görüldü. Patlama: önce noktasal, keskin beyaz-mavi bir parlama, sonra haftalarca yavaş sönüm.
- **Kalıntı nebulası (Yengeç Nebulası):** dışta kırmızı (hidrojen-alfa) ve turuncu iplikler, içte mavi-beyaz yaygın ışıma (sinkrotron), yeşil-mavi oksijen bölgeleri; düzensiz, ipliksi ve genişleyen bir kabuk.
- **Kızıl cüce ışığı:** ~3000 K yüzey sıcaklığı; disk gözle turuncu-kırmızı görünür, ışık gün batımı tonlarına yakındır, gökyüzü daha soluk ve sıcak renklidir.

Kaynaklar:
- https://science.nasa.gov/asset/hubble/peering-into-the-heart-of-the-crab-nebula/ (SN 1054, Yengeç iplikleri ve renkleri)
- https://science.nasa.gov/asset/webb/crab-nebula-webb-and-hubble-comparison/
- https://noirlab.edu/public/news/noao0305 (SN 1006 parlaklığı)
- https://ar5iv.labs.arxiv.org/html/astro-ph/0301603 (tarihî süpernova kayıtları, gündüz görünürlük)
- https://worldbuilding.stackexchange.com/questions/242396/colour-of-the-sky-on-a-planet-near-a-red-dwarf (kızıl cüce altında gökyüzü rengi; destekleyici, birincil değil)

## 2. Minecraft'ta gösterim — aşama aşama

| Aşama | Süre (şartname) | Gökyüzü (`gokyuzu.md` evresi) | Dünya |
|---|---|---|---|
| 0. Siren | 0–60 sn | `BUYUYEN` başlar: güneş 60 sn'de normalin 1,0→1,6 katına, renk beyazdan sarı-turuncuya | Hafif sıcak hava dalgası, aurora perdesi başlar |
| 1. Meteor yağmuru | 60 sn → 5. gün sonu | Güneş 5 gün boyunca 1,6→3,2 kat, turuncu `#FF9A3C`, korona halesi ve yavaşça dönen "güneş lekesi" dokusu | Meteorlar, ortak yangın, ısınma, buharlaşma derinliği 0→6 blok |
| 2. Süpernova | 1–2 gün (varsayılan 1,5) | `SUPERNOVA`: çökme + parlama + yavaş sönüm | Buharlaşan suyun geri gelişi, donma ilerlemesi başlar |
| 3. Güneşsiz dönem | donma %100 + 7 gün | `GUNESSIZ`: yıldız, galaksi, kalıntı nebulası | Donma %0→%100 (`donma.md`) |
| 4. Kızıl cüce doğuşu | ilk şafak | `KIZIL_CUCE` | Çözülme %100→%40 taban |

**Aşama 0 – Siren (60 sn).** Güneş büyümesi gözle fark edilsin diye eğri `s(t) = 1 + 0,6·t²` (sona doğru hızlanır). Gündüzse ekran 20 sn'de bir hafifçe sıcak renge kayar (sis rengine +%8 turuncu). Gece ise ufuktan yükselen **kızıl-yeşil aurora perdeleri**: gökyüzü küresinde 3 katman dikey şerit, U yönünde kayan gürültü dokusu, alt kenar yeşil `#3CFF8A`, üst kenar kızıl `#FF3C5A`, additive. Bu, sirenin "neden"ini yazı olmadan gösterir.

**Aşama 1 – Meteorlar ve kavrulma (5 Minecraft günü).**
- *Meteor* (`meteor` paketinin ortak varlığı): 40–70° açıyla gelen, çapı 0,6–3 blok sıcak kaya; gövde: lav ve magma dokulu küre benzeri blok kümesi, önünde beyaz-sarı plazma kabuğu (additive sprite), arkasında 30–80 blok uzunluğunda iki katmanlı iz: içte turuncu ateş, dışta yavaş dağılan gri duman şeridi (her 2 tick bir duman noktası, 8 sn ömür). Gece meteor çevresini turuncuya boyayan dinamik ışık yok (ışık motoruna dokunmuyoruz); bunun yerine yer yüzünde meteor altında büyüyen turuncu "parlama diski" (zemine yansıtılmış additive quad) — yaklaşan meteorun nereye düşeceğini oyuncuya 1,5 sn önceden gösterir.
- *Çarpma:* beyaz parlama (ekranda 4 tick, uzaklıkla azalan beyaz örtü), yere yayılan halka şok dalgası (yerden 0,3 blok yukarıda genişleyen toz halkası, 0,8 sn'de 4×çap), havaya fışkıran blok enkazı ve erimiş kaya kıvılcımları, sonra krater (meteor paketi), krater kenarında ortak yangın odakları, kraterde 20 sn magma bloğu parıltısı ("sıcak" ödül süresiyle aynı).
- *Yoğunluk eğrisi:* 1. gün oyuncu başına 1 meteor / 40 sn, 5. gün 1 / 12 sn (doğrusal artış); her oyuncunun 32–160 blok çevresinde dengeli (madde 3, bölüm D).
- *Kavrulma:* güneş altındaki zeminde hafif ısı dalgalanması (orman yangınındaki gibi şader'siz), çimenler 3. günden sonra rastgele kaba toprağa dönüşür (yangın sistemi dışı, `afetBlokHasari` ile), yapraklar kahverengiye döner (biyom rengi istemcide sarı-kahveye karıştırılır, `ColorResolver` çarpanı — blok değişmez). Gün ilerledikçe gökyüzü rengi soluk beyaz-sarıya ağarır.
- *Buharlaşma (N9):* su yüzeylerinden yükselen beyaz buhar (sütun başına %3 olasılıkla, 2–4 blok yükselip dağılır), okyanus üstünde alçak sis tabakası; derinlik arttıkça kıyıda açığa çıkan ıslak kum ve çakıl. Ay çarpmasından sonra geldiyse (N10) önce sanal deniz yükselmesi kademeli olarak sıfıra iner.

**Aşama 2 – Süpernova (1–2 gün).** Koreografi 40 sn sürer, tüm oyunculara aynı anda:
1. *Çökme (0–6 sn):* güneş diski 3,2 kattan 0,3 kata büzülür, rengi kızıldan mavi-beyaza döner, korona söner; dünya ışığı değişmez ama sis rengi koyulaşır (−%40). Ses kesilir (aşağıda).
2. *Parlama (6–8 sn):* noktadan patlayan mavi-beyaz ışık: ekran 0,4 sn boyunca %85 beyaz örtü, sonra 6 sn'de sönen; gökyüzünde 12 uzun ışın (yıldız saçağı) ve hızla genişleyen halka şok kabuğu.
3. *Genişleme (8–40 sn):* kabuk gökyüzünde 40°'lik açıya büyür, renk mavi-beyazdan beyaz-sarıya; yıldızlar arkasında görünmeye başlar.
4. *Sönüm (1–2 gün):* süpernova parlaklığı `e^(-t/τ)` ile azalır, gündüz bile gökyüzünde nokta + hale olarak görünür; sönüm boyunca nebulanın kırmızı iplikleri yavaşça belirir (`gokyuzu.md` nebula katmanının alfası 0→1).

**Aşama 3 – Güneşsiz dönem.** Gündüz de gece de karanlık gökyüzü: Samanyolu bandı, 2 uzak galaksi, kalıntı nebulası. Donma `donma.md`'ye göre ilerler; kar yağışı kendi renderer'ımızla (yıldız ışığında mavi-gri tonlu kar). Dünya ışık motoru değişmediği için Minecraft "gündüzü" hâlâ aydınlıktır; görsel karanlık sis ve gökyüzü rengiyle verilir (madde 16, bkz. Açık noktalar).

**Aşama 4 – Kızıl cüce doğuşu.** İlk şafakta (gün zamanı 23 000) ufukta 60 sn boyunca soluk kızıl ışık yayı belirir, güneş normal yörüngede yükselir; boyutu eski güneşin %60'ı, renk `#FF6A3A`, yumuşak kızıl hale. Gündüz gökyüzü `#B07A68`'e, sis `#C08870`'e kayar; yıldızlar ve nebula gündüz %45 alfa ile görünmeye devam eder (spike sonrası güncelleme, 2026-11-06: %25 parlak kızıl gökte görünmüyordu; `gokyuzu.md` §2).

**Afet sonrası iz.** Kraterler, yanmış alanlar, kurumuş göl yatakları (sonra yeniden dolup donar), kızıl cüce döneminde kalıcı kış bölgeleri.

## 3. Vitrin anları

- **Süpernova parlaması (8 sn, ana vitrin):** kamera donmaya başlamamış bir tepede, ufka bakar (pitch +25°), kadrajın altında silüet hâlinde ağaçlar ve bir ev. Görüntüde: gökyüzünün ortasında 12 ışınlı mavi-beyaz patlama, genişleyen halka kabuk, arka planda ilk kez beliren yıldızlar, zemin soğuk mavi tonda. Küçük resim: patlamanın 1,5 sn sonrası.
- **Meteor yağmurunun 4. günü (8 sn):** kamera bir dağ yamacında, ovaya bakar; turuncu dev güneş kadrajın üst köşesinde, gökyüzünde 4–6 meteor izi, ovada üç krater ve yayılan yangınlar, buhar tüten kuru göl yatağı.
- **Kızıl cüce doğuşu (10 sn):** donmuş okyanus kıyısı, kamera doğuya bakar; yıldızlı nebulalı gökyüzünün altında ufukta kızıl ışık yayı ve yükselen küçük kızıl disk, buz yüzeyinde turuncu yansıma şeridi.

## 4. Ses tasarımı

- **Siren:** uzun/ürkütücü varyant, 60 sn, meteorlar başlayınca 4 sn üstel fade-out (spike sonrası güncelleme, 2026-11-06; bkz. `ses.md`).
- **Meteor:** yaklaşırken yükselen ıslık + yanma hışırtısı (Doppler: perde 1,2→0,8), çarpmada derin güm (sentez 30–80 Hz + çakıl dağılması), 64 blok ötede yalnız boğuk güm.
- **Kavrulma ortamı:** düşük düzey sıcak rüzgâr (kahverengi gürültü, 0,2 Hz dalgalanma), uzak yangın uğultusu.
- **Süpernova:** çökme sırasında bütün ortam sesleri 3 sn'de susturulur (sessizlik vurgusu), parlamada tek, uzun, alçak "basınç" sesi (sentez: 25 Hz'den 60 Hz'e çıkan sinüs + yavaşça açılan gürültü, 8 sn), sonra 20 sn süren uzak kristal uğultu.
- **Güneşsiz dönem:** rüzgâr ve buz çatırtıları (`donma.md`); kızıl cüce doğuşunda tek sefer yumuşak, sıcak bir akor (sentez, 4 sn, düşük düzey).

## 5. Performans planı

| Kalite | Eşzamanlı meteor (oyuncu başına) | Meteor izi noktası | Çarpma enkazı | Aurora katmanı | Buhar olasılığı |
|---|---|---|---|---|---|
| Düşük | 2 | 20 | 8 | 1 | %1 |
| Orta | 3 | 40 | 16 | 2 | %2 |
| Yüksek | 4 | 80 | 30 | 3 | %3 |
| Ultra | 6 | 140 | 50 | 3 | %5 |

- Meteor sayısı sunucuda kaliteden bağımsızdır (oynanış); kalite yalnız istemci görselini değiştirir. 192 blok ötesindeki meteor yalnız iz çizgisi (tek uzun quad).
- Sunucu: krater ve yangın kuyrukları ortak; buharlaşma chunk başına batch (N9), tick başına blok tavanı donmayla ortak.
- Süpernova ve büyüme tamamen istemcide, sunucu yalnız evre + başlangıç tick'ini gönderir.

## 6. Açık noktalar — önerilen varsayılanlar

| Değer | Varsayılan |
|---|---|
| Gelişim kriteri | dünya günü ≥ 120 **ve** (ortalama ya da en gelişmiş, config) oyuncunun elmas zırh/alet sahibi olması |
| Güneş boyutu sonu | eski boyutun 3,2 katı |
| Meteor sıklığı | oyuncu başına 1/40 sn → 1/12 sn |
| Meteor çapı | %70 küçük (1 blok), %25 orta (2), %5 büyük (3; Kozmik Çekirdek yalnız burada) |
| Ortam sıcaklığı artışı | 1. gün +6 birim → 5. gün +18, gölgede yarısı |
| Süpernova süresi | 1,5 gün |
| Buharlaşma derinliği | 0 → 6 blok, 5 gün boyunca doğrusal |

- **Teknik risk:** "gündüz de karanlık" yalnız görsel olmalı (madde 16). Işık motoru değişmediğinden blok ışığı gündüz aydınlık kalır; karanlık hissi sis rengi + gökyüzü + `adjustLightmapColors` ile ışık haritası renk tonlaması verilebilir. `adjustLightmapColors` yalnız renk çıktısını değiştirir, ışık değerlerini, mob spawn'u ve vanilla mantığını değiştirmez; bu yorum KARARLAR'a yazılmalı.

## 7. Uygulama notları (Faz 4, 2026-11-06; kararlar `.vibe/kararlar/gunes-patlamasi-kizil-cuce.md` §B)

- **Siren büyümesi evre ilerlemesiyle (GP1):** güneş SIREN başında BÜYÜYEN olur; ilerleme siren boyunca `0,273·τ²`
  (gökyüzü ölçeği `1 + 2,2·p` → 1,0 → 1,6 kat), AKTIF'in 5 gününde 0,273 → 1 doğrusal (1,6 → 3,2 kat). Sıcaklık aynı
  ilerlemeyi okur: artış 18 iken 1. gün sonu +7,6, 5. gün +18 (§6'daki "1. gün +6 → 5. gün +18"e yakın).
- **Meteor boyu (GP3):** meteor API'sinde iki boy var; %5 büyük, kalanı küçük (§6'daki "orta" küçük sayılır). Yoğunluk
  dakikalık ardışık yağmurlarla 1/40 sn → 1/12 sn.
- **Kavrulma (GP6, GP15):** güneşe açık (yaprak/çatı altı değil) çimen 3. günden sonra kaba toprağa döner; denemeler ölçekli
  saate bağlı (`/dogalafet hiz` ile sıkışır). Yaprak ve çimen rengi 2. günün sonundan itibaren istemcide samana/kahverengiye
  karışır (blok renk işleyicisi, 8 basamak, GP13).
- **İstemci ekleri (GP11–GP14):** aurora perdesi `AFTER_SKY`'da (BUYUYEN gecelerinde); süpernova beyaz parlaması, ısı dalgalanması
  ve kızıl cüce şafak tonu görünüm uzayında (F1'de de); sesler kavurucu rüzgâr (`OrtamSesi`), süpernova basıncı + kristal
  uğultu ve kızıl cüce akoru (`tools/assets/gunes_sesleri.py`). Yapılmayanlar: çarpma anının ayrı beyaz örtüsü, süpernova
  çökmesinde öteki ortam seslerinin susturulması.

## 8. Meteor yağmuru güncellemesi (meteor-2, 2026-11-07; kararlar `.vibe/kararlar/meteor-2.md`)

Kullanıcı: patlamadan önce ve patlama sırasında meteor az. Yoğunluk `mantik.MeteorYagmuruEgrisi`: ön-uyarının son
`meteorYagmuruOncuSaniye` (90) saniyesinde öncü yağmur oyuncu başına 6/dk başlar, siren (güneş büyürken) boyunca doğrusal
24/dk'ya çıkar; AKTIF'in ve her günün başında `meteorYagmuruSaniye` (180, ölçekli) 24/dk gerçek meteor yağmuru, arasında §2'nin
1/40 sn → 1/12 sn arka planı. Afetin tek yağmuru eğriyi her tick izler (`MeteorAPI.yagmurYogunluguAyarla`). Dağılım, tavan ve
krater bütçesi değişmedi. Not: öncü meteorlar N7'nin gizli ön-uyarısını bilerek bozar (kullanıcı isteği; radar yine göstermez).

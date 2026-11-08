# Asit Yağmuru — tasarım notu (bölüm E, madde 10)

## 1. Gerçek olay

- Asit yağmuru (kükürt ve azot oksitlerinden oluşan sülfürik/nitrik asit) görsel olarak normal yağmurdan ayırt edilemez; izleri **etkilerinde** görülür. Oyunda okunabilirlik için rengi abartıyoruz (şartname: yeşilimsi/sarımsı).
- **Taş üzerinde (USGS):** mermer ve kireçtaşında kalsit çözünür; açıkta kalan yüzeyler **pürüzlenir, keskin kenarlar ve oymalar yuvarlanır**, malzeme kaybolur. Yağmur görmeyen korunaklı yüzeylerde **kara alçıtaşı kabuğu** (gypsum) oluşur, kabarcık yapıp dökülür, altında ufalanan taş kalır.
- **Bitki örtüsü (USGS, EPA):** iğne yapraklı ağaçlar iğnelerini kaybeder; Kara Orman'da yeşil iğneler dökülüp yalnız kara gövde ve dallar kalmıştır. Toprağa alüminyum salınır, bitkiler sararır.
- **Metal:** bakır ve bronzda hızlanan yeşil patina.

Kaynaklar:
- https://pubs.usgs.gov/gip/acidrain/5.html (çözünme, yuvarlanan kenarlar, kara alçıtaşı kabuğu)
- https://www.usgs.gov/water-science-school/science/acid-rain-and-water (Kara Orman, dökülen iğneler)
- https://www.epa.gov/acidrain/effects-acid-rain (ekosistem etkileri, alüminyum)

## 2. Minecraft'ta gösterim

**Bulut ve gökyüzü.** Afet alanının merkezinden 1,5R içinde sis ve gökyüzü rengi hastalıklı sarı-yeşile kayar (gündüz `#9AA35A`, gece `#2F3420`); alanın kenarında 16 blokluk yumuşak geçiş (`ViewportEvent.ComputeFogColor`). Alanın üstünde, Y=bulut seviyesinde, 0,02 blok/tick sürüklenen alçak, koyu zeytin rengi bulut tavanı (büyük bulut sprite'ları, alttan sarımsı aydınlanmış kenar). Uzaktan bakıldığında alan, ufukta sarı-yeşil bir **yağmur perdesi** (yağmur sütunları bulut tabanından yere uzanan dikey çizgili yarı saydam bant) olarak okunur.

**Yağmur.** Kendi yağış renderer'ımız (gokyuzu ile ortak `renderSnowAndRain` kancası): vanilla yağmur sütunlarının geometrisi, kendi dokumuz `asit_yagmuru.png` (damlalar sarımsı yeşil `#C8E04A`, hafif ışıltılı çekirdek), yağmur gerçek yağmurdan %15 daha yoğun ve damlalar %20 daha kalın. Vanilla yağmur o bölgede çizilmez. Gerçek vanilla hava durumu değişmez (sunucu `rain` açmaz; yalnız görsel bölge).

**Bloğa çarpma.** Damla bloğa çarptığında: küçük yeşil sıçrama (kendi parçacık) + %25 olasılıkla **cızlama dumancığı** — beyaz-sarımsı, 0,6 sn içinde yükselip dağılan ince duman. Katı yüzeylerde (çatı, taş) sürekli hafif buhar tabakası (zemin üstünde 0,3 blok yüksekte sürünen düşük yoğunluklu duman).

**Aşınma (sunucu, gerçek bloklar).** Aşınacak bloğa önce çatlak örtüsü uygulanır (`level.destroyBlockProgress(id, pos, aşama)`; aşama 3–5 sn'de 0→9 ilerler) ve üstünde yoğun cızlama dumanları çıkar, sonra dönüşüm uygulanır. Böylece bloğun "eridiği" okunur. Dönüşüm zinciri (yalnız gökyüzünü gören yüzey blokları, `MOTION_BLOCKING` yükseklik haritası):

| Blok | Dönüşüm | Not |
|---|---|---|
| yaprak | yok olur | önce iğne yapraklılar (Kara Orman) |
| çimen, çiçek, eğrelti | ölü çalı ya da yok | |
| çimen bloğu | toprak → kaba toprak | |
| ekin | kırılır (ürün düşmez) | |
| ahşap kalas/çit/kapı | çatlak → kırılır | |
| yün, halı | kırılır | |
| kırıktaş, taş tuğla | yosunlu/çatlak türe (taş tuğla → çatlak taş tuğla) | yalnız S3+ |
| kireçtaşı benzeri (kalsit, diyorit, mermer etiketi) | çatlak → çakıl | USGS kalsit çözünmesi |
| bakır bloklar | bir oksidasyon aşaması ilerler | patina |
| `dogalafetler:asit_dayanikli` (taş, deepslate, obsidyen, cam, demir, beton…) | etkilenmez | |

Cam asit dayanıklıdır; bu, "çatı altında korun" fikrini cam serayla da mümkün kılar.

**Oyuncu.** Başının üstünde gökyüzüne açık sütun varsa (yükseklik haritası kontrolü, 4 tick'te bir) hasar; çatı altında hiç hasar yok. Hasar anında ekranda kenarlardan içe akan küçük sarı-yeşil damla izleri (ekran efekti, yazı değil) ve 0,3 sn hafif sarı vinyet. Zarhın dayanıklılığı 4 sn'de 1 azalır (kask önce).

**Afet sonrası iz.** Yapraksız kara gövdeler (yaprak kaybolmuş kütükler), kaba toprak lekeleri, ölü çalılar, çatlak taş tuğlalar, yeşillenmiş bakır; 30 sn boyunca zeminden süzülen buhar.

**Seviye 1 ile 5 farkı.** S1: 32 blokluk alanda seyrek sarımsı çise; yalnız çiçekler ve birkaç yaprak kaybolur. S5: 128 blokluk alanı kaplayan yoğun sarı-yeşil sağanak, her yüzeyden duman, orman 4 dakikada yapraksız kara gövdelere döner, ahşap çatılar delinir, taş tuğla çatlar.

## 3. Vitrin anı

**Bir ormanın yapraklarını kaybettiği 8 saniye.** Kamera orman kenarında, cam bir sera ya da taş sundurmanın altında (çerçeve için), dışa ve hafif yukarı bakar (pitch −8°). Görüntüde: sarı-yeşil sis ve eğik sarımsı yağmur çizgileri, ağaçların tepesinden yükselen beyaz cızlama dumanları, çatlak örtüsü taşıyan ve tek tek kaybolan yapraklar, ön planda kaba toprağa dönen çimen ve ölü çalılar, camın üstünde süzülen yeşil damlalar. Küçük resim için: yarısı yeşil, yarısı kara gövdeye dönmüş orman, üstünde zeytin rengi bulut tavanı.

## 4. Ses tasarımı

- **Yağış:** vanilla `weather.rain` (pitch 0,9) + kendi "cızlama" döngümüz (yüksek geçiren beyaz gürültü 3–8 kHz, düşük düzey, -10 dB); çatı altında `weather.rain.above` mantığıyla boğuk.
- **Aşınma:** blok çözülürken `block.fire.extinguish` (pitch 1,4–1,8, düzey 0,25) — asidin "tıss" sesi; aynı anda en çok 4.
- **Hasar:** `entity.player.hurt` yerine kısa cızlama + vanilla hasar.
- Uzak gök gürültüsü yok (şimşek yok; yağmurun "zehirli sessizliği"). Cızlamanın yüksek frekanslı enerjisi konuşma bandının üstünde, düşük düzeyde tutulur.

## 5. Performans planı

| Kalite | Yağmur yarıçapı (vanilla 5/10) | Çarpma sıçraması/tick | Cızlama dumancığı olasılığı | Bulut tavanı sprite | Yüzey buharı |
|---|---|---|---|---|---|
| Düşük | 5 | 20 | %8 | 30 | yok |
| Orta | 10 | 50 | %15 | 60 | 8 blok içinde |
| Yüksek | 10 | 100 | %25 | 120 | 16 blok |
| Ultra | 15 | 180 | %35 | 200 | 24 blok |

- Yağmur görseli vanilla ile aynı maliyette (aynı sütun geometrisi); fark yalnız doku ve sıçrama sayısı.
- Sunucu: her tick rastgele 3–16 yüzey sütunu örneklenir (vanilla rastgele tick gibi), dönüşüm adayları kuyruğa girer; eşzamanlı `destroyBlockProgress` en çok 32 (vanilla kırılma örtüsü paketleri az yer kaplar). TPS < 18'de örnekleme yarıya.
- Oyuncu hasarı 4 tick'te bir yükseklik haritası sorgusu, O(oyuncu).

## 6. Açık noktalar — önerilen varsayılanlar

| Seviye | Alan yarıçapı | Süre | Aşınma (blok/sn) | Oyuncu hasarı | Taş tuğla aşınması |
|---|---|---|---|---|---|
| 1 | 32 | 60 sn | 2 | 0,5 can / 4 sn | yok |
| 2 | 48 | 90 sn | 4 | 0,5 can / 3 sn | yok |
| 3 | 64 | 120 sn | 7 | 0,5 can / 2 sn | var |
| 4 | 96 | 180 sn | 11 | 1 can / 2,5 sn | var |
| 5 | 128 | 240 sn | 16 | 1 can / 2 sn | var |

- **Öldürücü olmama garantisi:** asit hasarı oyuncunun canını 4'ün (2 kalp) altına indirmez (config `asitMinimumCan`, varsayılan 4). Böylece S5'te bile şartnamedeki "ortalama hasar, öldürmesin" koşulu kesin sağlanır.
- Alan hedef oyuncunun çevresinde doğar, 0,3 blok/sn ile rüzgâr yönüne kayar; başlangıçta 10 sn'de yoğunlaşır, sonda 15 sn'de seyrelir.
- "Çatı Altı" başarımı için alan içindeki oyuncunun asit hasarı sayacı tutulur.

## Faz 4 güncellemesi (afet, `is/asit-orman-yangini`)

Uygulama `afet.asit_yagmuru` + `client.asit_yagmuru`; kararlar `.vibe/kararlar/asit-orman-yangini.md` (AO-01…AO-03, AO-10, AO-11).
Notun üzerinden değişenler:

- **Yağış kancası:** gokyuzu ile ortak `renderSnowAndRain` kancası yerine `RenderLevelStageEvent.AFTER_WEATHER`; aynı vanilla
  sütun geometrisi, kendi doku, %20 kalın damla, yağmur yarıçapı 5/10/10/15. Vanilla yağmur bu bölgede zaten yağmadığı için
  bastırma gerekmedi; gökyüzünün sarmalayıcısına ikinci katman eklenmedi.
- **Bulut tavanı:** büyük koyu zeytin renkli parçacık quad'ları (12–22 blok), bulut yüksekliğinde ama kameraya en çok 60 blok
  yukarıda, günün rüzgârıyla 0,03 blok/tick sürüklenir; tavan kademe tablosundaki sayılar.
- **Taş dönüşümü:** `asit_dayanikli` tablonun önündedir ve `base_stone_overworld` (diyorit dahil) içerir; "kireçtaşı benzeri →
  çakıl" yalnız kalsitte. Etikete cam, cam panel, demir, tuğla, deepslate tuğla/karo, beton ve pişmiş kil eklendi.
- **Aşınma hızı (§5):** eşzamanlı çatlak örtüsü tavanı 64 (32, 3–5 sn'lik örtüyle saniyede ancak 6–11 bloğa izin
  veriyor, §6'daki S5 16 blok/sn'yi kesiyordu); alanda oyuncu varsa örneklerin yarısı bir oyuncunun 24 blok çevresinden
  seçilir (değişim görenin önünde, vitrin anı mümkün olur).
- **Zırh:** ayrı "4 sn'de 1 dayanıklılık" sayacı yok; `dogalafetler:asit` hasar tipi zırhı delmez, vanilla zırh hasarı azaltır ve
  her vuruşta yıpranır.
- **Ses (§4):** yeni ses dosyası yok. Yağış vanilla `weather.rain` (perde 0,9; çatı altında `weather.rain.above`), "cızlama
  döngüsü" yerine seyrek, kısık, tiz `block.fire.extinguish`; aşınma anında `block.fire.extinguish` (en çok 4). Hasar anında
  vanilla hasar sesi.
- **Afet sonrası 30 sn buhar:** yapılmadı (afet bitince istemci verisi gelmez); buhar ve cızlama yağış süresince.
- **Çatı Altı:** başarım için hasar sayacı tutulmaz; asit hasar verdiği an `BasarimIpucuEvent(HASAR_ALDI)` yayınlanır.

## Görsel cila (2026-11-07)

- **Maruziyet:** yağmur sürdükçe (AKTIF, yoğunluk > 0; giriş/çıkış çisesi dahil) alan içinde başı gökyüzüne açık
  oyuncu seviyeden bağımsız saniyede `asit_yagmuru.hasarSaniye` (1 = yarım kalp) hasar alır; seviye başına `hasar` /
  `hasarAraligiTick` listeleri kalktı. `asitMinimumCan` (4) korunur: madde 10 "öldürücü değil". Çatı altı 0; Çatı Altı
  başarımı aynı `HASAR_ALDI` ipucuyla bozulur.
- **Ekran:** maruz kaldıkça sürekli sarı-yeşil kenar vinyeti ve kenarlardan döngüyle akan damla izleri (0,5 sn'de belirir ve
  söner, vuruşun %45'i); her hasarda 0,3 sn'lik vuruş üstüne biner. Üçüncü kişi kamerada oyuncunun başından seyrek cızlama
  dumanı. Koşul istemcide sunucuyla aynı kuralı okur (ham yoğunluk, alan yarıçapı, `MOTION_BLOCKING`), yeni paket yok.

## afet-fizik-2 (2026-11-08): bütün dünyada

Kullanıcı isteği: "Asit yağmuru her yerde yağmıyor; bir kullanıcıya targetlandıysa o kullanıcının olduğu yerde yağıyor. Her
kullanıcıda yağsın, herkes görebilsin; asit yağmuru tüm dünyada yağsın, normal yağmur gibi, herkese etki etsin." Kararlar
`.vibe/kararlar/afet-fizik-2.md` (AF2-01…AF2-09). Notun üzerinden değişenler:

- **Alan yok, her yer:** etki alanı `EtkiAlani.herYer()`; afet kaymaz (`kaymaHizi` kalktı). "Her yer" afetin boyutudur (üst
  dünya): siren, durum paketi, hasar ve başarım sayımı o boyuttaki herkese; Nether/End'deki oyuncu asitten etkilenmez (AF2-08).
  §2'deki "merkezden 1,5R içinde sis", "alan kenarında 16 blokluk geçiş" ve §6'daki "alan hedef oyuncunun çevresinde doğar"
  uygulanmaz: sis, bulut tavanı, yağış, sıçrama ve buhar kameranın bulunduğu her yerde. Seviye tablosundaki yarıçap artık
  aşınmanın her oyuncunun çevresindeki örnekleme yarıçapıdır.
- **Gerçek hava değişir (§2 "sunucu rain açmaz" yerine):** AKTIF'e geçerken dünyanın havası saklanır, vanilla yağmuru açılır ve
  AKTIF boyunca tutulur (vanilla döngüsünün kesmesi, gök gürültüsü ve `/weather clear` bir tick'te geri alınır; asidin
  "zehirli sessizliği" §4 korunur). Bitişte saklanan hava aynen geri gelir; `doWeatherCycle` kuralına dokunulmaz. Yağmur
  normal yağmur gibi ateşi söndürür, oyuncuyu ıslatır ve ortamı serinletir (sıcaklık sistemi).
- **Çizim:** vanilla yağmur istemcide AKTIF boyunca ve bitişten sonra 5 sn (sunucu yağmuru 0,01/tick sönerken) çizilmez
  (`ClientLevel.setRainLevel(0)`, donma istemcisinin kar kipiyle aynı yol); asit yağışı biyomun yağışına bakmadan çizilir: çölde
  ve kar biyomunda da asit yağar (kar yerine asit).
- **Hasar her oyuncuya:** başı göğe açık her oyuncu (yaratıcı, izleyici, ölü ve chunk'ı yüklenmemiş olan hariç) saniyede
  `hasarSaniye`; `asitMinimumCan` (4) aynen. Hedefe 500 blok uzaktaki oyuncu da hasar alır (GameTest).
- **Aşınma oyuncuların çevresinde:** her örnek rastgele bir oyuncu seçer (izleyici ve yaratıcı hariç; yarısı 24 blok, yarısı
  seviye yarıçapı); hız oyuncu başına tablo değeri, tick örnekleme bütçesi `asit_yagmuru.blokButcesi` (32) × TPS kısması;
  yüklenmemiş chunk okunmaz.
- **Bilinen sınır (AF2-07):** gerçek yağmur soğuk biyomda vanilla kar katmanı biriktirir ve kazanları doldurur (istemci orada
  asit çizer); bu `afetBlokHasari=false` iken de olur, mixin'siz engellenemez.
- **Radar:** asit "gökyüzü anomalisi" (N7, yalnız güneş ve ay) değildir: normal ön-uyarı penceresinden itibaren radar "Asit
  Yağmuru, her yer" ve geri sayım gösterir, yön oku yoktur.
- **Vitrin anı (§3) çöl eki:** çölde, yağmur görmeyen bir yerde sarı-yeşil sağanak; görsel kanıt `asit_uzak` karesi.

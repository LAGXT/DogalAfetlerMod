# Özel eşyalar — tasarım notları

Her eşya işçisi bu dosyaya kendi bölümünü ekler.

## Sıcaklık yiyecekleri (madde 14, 7b, 15; parça `yiyecek`)

### Gerçek dünya

- **Kış içecekleri.** Salep, orkide yumrusu unuyla koyulaştırılmış, tarçınlı sıcak süttür; Türkiye'de kışın sokağa çıkmadan
  içilir. Sıcak çikolata ve tavşan/tavuk çorbası da "içten ısıtan" yiyeceklerdir: sıcak sıvı mideye inince çekirdeğe doğrudan
  ısı verir (anında his), sindirim ve kan dolaşımı bir süre daha ısı üretir (giderek azalan etki). Acılı, yoğun güveç
  terletir: soğukta çok iyi, sıcakta bunaltır.
- **Yaz içecekleri.** Ayran (tuzlu, soğuk yoğurt içeceği) ve şerbet (Osmanlı mutfağının kar/buzla soğutulmuş meyve şurubu)
  sıcak çöl ve yaz günlerinin içeceğidir; buzlu karpuz en hafif serinletici. Soğuk içecek donan birini daha çabuk dondurur.
- **Tekrar etkisi.** Aynı sıcak içeceği üst üste içmek ilk bardak kadar ısıtmaz (mide dolu, fark azalır); farklı bir yiyecek
  ayrı bir kaynaktır. Madde 14'ün azalan verimi bunu oyunlaştırır.

### Minecraft'ta teknik

- **Dokuz eşya** (`yiyecek.mantik.YiyecekTablosu`): sıcak — Sıcak Salep, Sıcak Çikolata (orta, 90 sn), Sıcak Tavuk Çorbası
  (büyük, 150 sn), Ateşli Güveç (çok büyük, 180 sn, yan etki), Isıtıcı Çay (hafif, 180 sn), Kutup Balığı Çorbası (büyük,
  150 sn); soğuk — Ayran (orta, 90 sn), Buzlu Karpuz Dilimi (hafif, 60 sn), Meyve Şerbeti (büyük, 120 sn). Yeme 32 tick,
  yığın 16, tok da yenir; şişe/kase boş kap olarak geri gelir. Tarifler madde 14'teki gibi şekilsiz.
- **Etki** (`YiyecekAPI.uygula`): kademe miktarı (config: hafif 8, orta 20, büyük 35, çok büyük 45 vücut birimi = ölçeğin
  yüzdesi) %25 anında, kalanı süre boyunca doğrusal azalan hızla `VucutSicakligi.etkiEkle` ile eklenir. Etki toplanır:
  sıcakken içilen salep aşırı ısınmaya, donarken içilen ayran donmaya iter ("zamanında yenmesi gerekir"). Ateşli Güveç
  vücut ya da ortam sıcakken ×1,5 ısıtır.
- **Azalan verim:** aynı eşya son yemeden 60 sn içinde tekrar yenirse anında ve süreli kısım yarıya iner; farklı eşyalar
  bağımsız; oyuncu başına, oyun saatiyle, oyuncu verisinde.
- **Durum efekti** "İçten Isınma" (turuncu alev ikonu) / "Serinlik" (buz mavisi kar tanesi): yalnız gösterge — seviye
  büyüklük kademesi (I–IV), süre etkinin süresi; sıcaklığı efekt değil sıcaklık sistemi uygular. Parçacık yok (ekranı
  kirletmez), süt silmez.
- **Vanilla pişmiş yiyecekler** (kızarmış patates, pişmiş etler/balıklar 30 sn; çorbalar 45 sn) çok küçük (3 birim)
  ısınma ve "İçten Isınma I" verir; config ile kapanır, listeye başka modların yiyecekleri eklenir.
- **Balıkçılık köprüsü:** pişmiş buz balığı orta ısınma, çiğ hafif serinlik (`YiyecekAPI.pismisBalik/cigBalik`); Kutup
  Balığı Çorbası `#dogalafetler:pismis_kutup_baligi` etiketindeki balıkla pişer.

### Görünüş ve ses

- **Dokular** (`tools/assets/yiyecek_dokular.py`, 16×16, vanilla iksir şişesi ve kase silueti, koyu kenar, sol üstten ışık):
  salep krem rengi sıvı ve yüzeyde tarçın, çikolata koyu kahve ve krema, çay kehribar kızılı ve kuru meyve; sıcakların
  şişe boynunun iki yanında kıvrılan buhar. Ayran köpüklü beyaz, şerbet pembe-kırmızı ve buz küpü; soğukların camda kırağı ve
  köşede buz parlaması. Kaseler: tavuk çorbası altın suyu, tavuk, havuç, maydanoz; güveç koyu kızıl, kor sarısı alev tozu
  ve kasenin üstünde alev dilleri; kutup balığı çorbası soluk buz mavisi suyu, somon tonlu ve beyaz balık, dereotu.
  Buzlu karpuz vanilla dilim siluetinde, kabuğun üstü ve kesik yüzü buz kristalli. Envanterde bir bakışta ısıtan (buhar,
  alev) ve serinleten (kırağı) ayrılır.
- **Efekt ikonları** 18×18: iç içe katmanlı alev (kızıl kenar → turuncu → sarı → açık çekirdek, kıvılcımlar) ve altı kollu,
  yan dallı kar tanesi (koyu mavi kenar, buz mavisi kollar, beyaz merkez).
- **Tooltip:** "İçten Isınma: orta, 90 sn" (altın) / "Serinlik: hafif, 60 sn" (su mavisi); Güveç'te kırmızı "Sıcakta hızla
  aşırı ısıtır".
- **Ses:** şişeler vanilla içme sesi ve animasyonu, kaseler ve karpuz yeme sesi; yeni ses dosyası yok.

### Vitrin anı

Kar fırtınasında donma kenarına gelmiş oyuncu buharlı bir Ateşli Güveç yer: XP barının üstündeki sıcaklık işaretçisi
anında sağa kayar, sağ üstte turuncu alev ikonu (İçten Isınma IV, 3:00) belirir, buz kenarı sonraki saniyelerde çözülür.
Aynı güveci çöl öğleninde yiyen arkadaşının barı kırmızıya taşar ve titrer.

## Hava Radarı ve Sismograf (madde 13a–b, N6, N7)

### Gerçek dünyadan

- **Hava radarı ekranı (PPI):** koyu yeşil fosforlu, merkezde verici, dönen tarama çizgisi ve arkasında sönen iz;
  hedef tarama geçerken parlayıp yavaşça sönen bir nokta. Mesafe halkaları ve kuzey referansı.
- **Sismograf (davul kayıtçı / helikorder):** dönen tambura sarılı krem kâğıt, ince mürekkep çizgisi, zaman işaretleri.
  Sakin zemin bile "mikrosismik" kıpırtı çizer; öncü depremler seyrek kısa iğneler, ana sarsıntı kâğıdı dolduran geniş
  salanımdır. Öncü sarsıntılar aralıklıdır, ana şoka yaklaştıkça sıklaşır.

### Oyunda

- **Radar paneli (elde tutulurken, sağ alt köşe):** küçük CRT kutusu. Solda 33 px'lik dairesel kap: iki mesafe halkası,
  artı ışını, saniyede bir tur atan tarama çizgisi ve 6 kademede sönen izi. Afet, oyuncunun bakışına göre (yukarı = ön)
  menzile oranlı uzaklıkta kırmızı bir nokta; tarama üstünden geçerken beyaz parlar. Sağda üç satır: afet adı, dönen ok +
  mesafe (m), "Varış m:ss" (ON_UYARI/SIREN) ya da "Kalan m:ss" (AKTIF). Afet yoksa "Gökyüzü sakin · Menzil 300 m".
- **Gökyüzü anomalisi (N7):** güneş patlaması/ay çarpması yalnız sirenden sonra; kap mora döner, içinde nabız gibi
  parlayan bir dolgu ve dışa yayılan halka, "Gökyüzü anomalisi · <afet> · Başlamasına m:ss". Yön ve mesafe yoktur.
- **Gelişmiş radar:** çerçeve mor, üst kenarda mavi ışık çizgisi; üst bölüm normal radarla aynı boyda. Altında 9 px'lik kısa
  satırlar: seviye adı (Hafif…Felaket, seviye rengiyle) ile hareket oku + hız (hareketli afette) ya da "Durağan" aynı satırda,
  "Son afetler" ve tek sütunda 5 kısa geçmiş satırı (`Kasırga S2 3dk`). Panel en çok 108 GUI px yüksekliğindedir: GUI
  ölçeği 2'de ekranın %30'unu geçmez.
- **Sismograf:** eşya hotbar'da/yan eldeyken öncü sarsıntıda her darbede kısık bir zemin gürlemesi (2,4 sn, 25–110 Hz,
  −27 LUFS, oyunda 0,08–0,30 hacım) ve 2 sn süren, en çok 0,3°'lik kamera titremesi; action bar'da (yalnız taşıyan)
  altun "Öncü sarsıntı algılandı · %45 · KD 240 m" (Hassas'ta "· Seviye: Şiddetli" kırmızı). AKTIF'te "Ana sarsıntı".
- **Sismograf ekranı (sağ tık):** koyu metal kasa, krem kayıt kâğıdı; ızgara ve dalga aynı hızla sola akar, kırmızı saniye
  işaretleri kenarlarda; sağda tamburdan uzanan kalem kolu şimdiki örneği lacivert mürekkeple yazar (Hassas'ta mor eşya
  dokusuyla uyumlu). Algılama varken kırmızı lamba yanıp söner. "60 sn önce" … "şimdi".
- **Dokular (16×16):** radar demir gövdeli, yeşil ekranlı el cihazı (tarama çizgisi, kırmızı nokta, bakır tuşlar, anten);
  sismograf taş kaide üstünde kâğıt sarılı tambur, çapraz tüy kalem ve mürekkep izi. Gelişmiş/Hassas sürümler mor-mavi
  çerçeve, cam göbeği ekran/mor mürekkep ve mor bir "çekirdek taşı".

### Vitrin anı

Deprem S3 ON_UYARI'nın erken bölümü: Hassas Sismograf taşıyan oyuncunun ekranı çok hafif kayıyor, altın action bar
"Öncü sarsıntı algılandı · %58 · GD 110 m · Seviye: Şiddetli" yazıyor, eldeki normal radar hâlâ "Gökyüzü sakin" diyor;
yarım dakika sonra radarın taraması kırmızı noktayı yakalıyor ve geri sayım başlıyor. Siren henüz yok.

### Ses

Tek dosya `esya_oncu_sarsinti.ogg` (`tools/assets/esya_radar_ses.py`): taban gürlemesi, 3–7 Hz kayan titreşim zarfı, çok
düşük çakıl çıtırtısı; konuşma bandı −40 dB (sesli sohbeti bastırmaz). Kategori `AMBIENT`, yalnız taşıyanın istemcisinde.

## Meteor, meteor ödülleri ve Termostat (madde 13c, N3, N4; parça `meteor-termostat`)

### Gerçek olay

- **Atmosfere giriş:** göktaşı saniyede 11–72 km hızla girer; önündeki hava sıkışıp ısınır ve iyonlaşır (plazma
  kabuğu), arkada ışıklı bir iz ve kalan duman/toz şeridi bırakır. Büyük cisimler (bolid) parlak beyaz-sarı, kırmızı-turuncu
  çekirdekli; magnezyum/demir ağırlıklı olanlar mavi-yeşile çalan izler verir.
- **Çarpma:** basit krater çanak biçimlidir, derinliği çapın ~1/5–1/3'ü; kenarda yükselmiş ve dışa atılmış enkaz (ejecta)
  halkası, tabanda eriyik ve şokça değişmiş kaya; çarpmayı parlama, yere yayılan şok/toz halkası ve sarsıntı izler.
- **Meteorit:** yere ulaşan parça dıştan koyu, erimiş bir kabukla (füzyon kabuğu) kaplı, içi gözenekli/metal damarlıdır; çarpmadan
  hemen sonra sıcaktır ama dakikalar içinde soğur.

Kaynaklar: https://www.amnh.org/exhibitions/permanent/meteorites (meteorit türleri, füzyon kabuğu),
https://www.lpi.usra.edu/exploration/training/illustrations/craterLandforms/ (basit krater, ejecta, derinlik/çap),
https://www.amsmeteors.org/fireballs/faqf/ (bolid renkleri, iz, ses).

### Minecraft'ta gösterim

- **Meteor varlığı** (`MeteorVarligi`, sunucu otoriteli): 40–70° açıyla, hedefin 200 blok üstünden ~9 sn'de (MT2-25) düz
  inen dönen kaya kümesi (güneş: magma + karataş/bazalt; ay: kalsit + pürüzsüz bazalt/tüf), önünde additive plazma
  kabuğu ve ışık halesi, arkasında iki katmanlı additive iz şeridi (80–140 blok, MT2-32; güneş: beyaz-sarı çekirdek + turuncu,
  ay: mavi-beyaz çekirdek + mavi) ve 8 sn'de dağılan gri duman, düşen kıvılcımlar. İnişe 1,5 sn kala zeminde büyüyen
  turuncu/mavi parlama diski oyuncuya düşüş yerini gösterir. İz, kabuk ve disk sissiz çizilir (gece görüş sisi
  uzaktan gelen izi söndürmesin); 96 blok ötesinde blok kümesi gövde yerine yalnız plazma. Işık motoruna dokunulmaz. 192 blok ötesinde parçacık yok,
  yalnız şerit.
- **Çarpma:** beyaz parlama (uzaklıkla azalan, 5 tick; dünyanın üstünde, elin ve GUI'nin altında tam ekran, F1 karelerinde de görünür), mesafeyle karesel azalan ekran sarsıntısı (oyuncuya özgü paket),
  0,8 sn'de 4×çapa yayılan toz halkası, zeminin blok parçacıklarıyla fışkıran enkaz, eriyik kaya kıvılcımları, duman,
  derin güm. Suya düşen meteor sıçrama + buhar, krater/ateş/ödül yok.
- **Krater** (`KraterProfili`): paraboloit çanak (küçük r 2,5 / derinlik 2, büyük r 5,5 / 4), üstte 2 blok bitki/kar
  temizliği; tabanın ortası magma (20 sn "sıcak" süresi boyunca parlar, sonra karataş/bazalta soğur), dışı koyu kaya;
  kenarda %55 yoğunlukla enkaz halkası (karataş, bazalt, kaba toprak; ayda pürüzsüz bazalt, tüf, kalsit). Sert bloklar
  (`dogalafetler:deprem_dayanikli`, `yanmaz`, kırılmaz, blok varlıklı) ve sıvılar korunur; damla yok. `afetBlokHasari=false`
  iken krater yok. Krater kenarı + 3 blok ortak yangınla tutuşur (yoğunluk 0,45 / 0,85), krater 30 sn ortamı ısıtır.
- **Ödüller:** Meteorit Parçası (koyu gözenekli kaya + turuncu kor damarları, altın parıltı; sıcakken alev ve duman),
  Kozmik Çekirdek (mor-mavi kristal, büyü parlaması, mor-mavi yıldız parıltısı ve büyü harfleri). Meteorit Külçesi
  (koyu demir-mavi, kıvılcım benekli), Termostat Plakası (bakır-altın plaka, altın devre izleri, kızıltaş çip).
- **Termostatlı zırh:** sıcaklığın `zirh_eki` tarif tipiyle (N3); her parça vücudu nötre 0,04 birim/sn çeker, |E| > 40'tan
  sonra azalır, 120'de %25 tabanda kalır (N4). İpucu satırı "Termostatlı".

### Vitrin anı

Gece, orman kıyısında ağaçların üstünden doğuya bakan kamera: sağ üstten dalan dört meteorun mavi-beyaz ve turuncu
izleri gökyüzünü çaprazlar, ilk çarpmanın parlaması ve toz halkası, ardından ağaçlar arasında magma tabanlı krater ve
yayılan yangın. Küçük resim: ilk çarpmadan 6 tick sonra (izler hâlâ gökyüzünde, kıvılcım ve enkaz havada).

### Ses

Uçuşta rüzgâr ıslığı döngüsü meteoru izler, perde yaklaşırken 1,2, uzaklaşırken 0,8 (Doppler), ay parçaları %10 tiz;
çarpmada alçak perdeli derin patlama (büyükte 160 blok, küçükte 80 blok duyulur), suda sıçrama + cızgırtı; sıcak parça
suya/kara değince cız. Kendi sentez dosyası yok: vanilla sesler `SoundEvents` ile (kural: tek seferlik darbe
`level.playSound`).

### Ses ve yağmur güncellemesi (meteor-2, 2026-11-07; kararlar `.vibe/kararlar/meteor-2.md`)

Kullanıcı isteği: uçuş sesi her yerden duyuluyor ve vanilla gibi. Artık iki sentez dosyası var (`tools/assets/ses_uret.py`,
ölçüm `docs/kanit/ses/meteor_*_ebur128.txt`): **uçuş** `meteor_ucus.ogg` (8 sn döngü; 35–90 / 90–220 Hz yanma kükremesi
0,25 Hz'de çapraz kayar, 70→95 Hz süzülen kısmi ton, 6 Hz plazma çalkantısı, konuşma çentiği; −26 LUFS) yalnız meteor 48 blok
yakınındayken çalar (52 blokta söner), kazanç `(1 − d/48)²`, perde dinleyiciye yaklaşma hızından Doppler `5,2 / (5,2 − v)` (tipik 1,3 blok/tick'te
1,33 → 0,8; MT2-25 öncesi `12 / (12 − v)`, 3 blok/tick), ay parçaları %10 tiz; **patlama** `meteor_patlama.ogg` (7 sn tek seferlik; kırılma darbesi, 90 → 32 Hz göğüs vuruşu, 2,2 sn
zaman sabitli 20–120 Hz uğultu kuyruğu, çakıl; −23 LUFS, ≤ −6 dBTP) büyükte 128, küçükte 96 blok içinde (küçük %70 düzey,
büyük perde 0,85), kazanç karesel. İkisini de istemci vanilla zayıflaması kapalı çalar (konum yalnız yön için); her türden aynı anda
en çok 6 (ses.md §4 kural 3): uçuşta en yakın altı, çarpmada en uzaktaki düşer. Sunucunun `entity.generic.explode`'u kalktı; suda
sıçrama + cız vanilla kalır. Ay parçası görünümü mevcut `AY` varyantıdır (kalsit + pürüzsüz bazalt/tüf gövde, mavi-beyaz
plazma); yeni doku gerekmedi.

**Yağmur yoğunluğu (istek 27):** güneş patlaması ön-uyarının son 90 sn'sinde öncü yağmur başlatır (oyuncu başına 6/dk) ve
siren boyunca 24/dk'ya çıkar; AKTIF'in başında 10 dk, sonraki günlerin başında 3 dk 90/dk, arasında bölüm D'nin 1,5 → 5/dk
arka planı. Ay parçası yağmuru oyuncu başına 90/dk, 10 dk. Dağılım (32–160 blok halka) ve krater bütçesi (512 × TPS
kısması) aynı; uçan meteor tavanı 48 → 96 (doluyken atılamayan meteor düşmez, kuyruk yok).

**Boyut değişkenliği (istek 27):** meteorun sürekli ölçeği (`meteor.mantik.MeteorOlcegi`, senkron varlık verisi ve çarpma
paketinde): %60 küçük 0,5–0,9×, %30 orta 0,9–1,6×, %10 büyük 1,6–2,5×, bant içinde düzgün. Eski küçük/büyük config değerleri
tipik ölçek 0,7 ve 2,0'daki referanslardır; arası ve uçları doğrusal: gövde çapı 1,1 → 2,6 (ölçek 2,5'te 3,2), iz 86 → 125 blok (uçlarda 80–140),
uçuş süresi ÷0,97 → ÷1,10 (uçlarda ÷0,95–1,12), krater yarıçapı 2,5 → 5,5 (2,5×'te 6,7), sarsıntı menzili 48 → 112, yangın yoğunluğu 0,45 → 0,85, patlama sesi
96 → 128 blok ve %70 → %100 düzey, perde 1,0 → 0,85. Ödül sınıfı büyük banttır (Kozmik Çekirdek yalnız 1,6× ve üstünde).

**Ödül sınırı (tur 2, şef kararı):** yağmur meteorları ödülü yalnız büyük bantta bırakır ve afet başına, hedef oyuncu başına
en çok `meteor.odulTavaniAfetBasina` (8) eşya (`meteor.mantik.OdulTavani`); 10 dk'lık 180/dk yağmurda oyuncu başına ~58 eşyalık
enflasyon en çok 8'e iner (büyük bantın beklenen ~9,5'i; tavan afetlerin ~%70'inde dolar). Tek düşürülen meteor (komut)
madde 13c şanslarıyla sınırsızdır; komut yağmurları ortak `komut` sayacını paylaşır (`meteor durdur` sıfırlar).

**Uçuş süresi (tur 3, şef kararı MT2-25):** gerçek hızda gökte aynı anda ~5 meteor "aşırı fazla" isteğine yetmedi;
sorun uçuşun ~3 sn sürmesiydi. Meteor hedefin 200 blok üstünden doğar (40–70°) ve hedefe `meteor.ucusSuresi`'nde (180 tick;
küçük 9,5 sn, büyük 8,0 sn) varır: hız yoldan çıkar (~1,0–1,9 blok/tick). Doppler (yukarıdaki 1,2/0,8 kayma) tipik hızda
1,33/0,8 kalır (`SES_HIZI` 5,2). **Tur 4 (MT2-28):** afet yağmurları 180/dk: oyuncu başına gökte ~27 meteor, uçan meteor
tavanı 448 (16 oyuncu); yağmur meteorunun çarpma noktası bakış yönüne ağırlıklı (2/3 ön yarı, 1/3 arka;
`meteor.bakisYonuOrani`), komut meteoru yönsüz.

**İz ve config göçü (tur 5, şef kararları MT2-31, MT2-32):** iz şeridi 80–140 blok, genişliği ve parlaklığı ×1,5, uzakta en
az 2 piksel kalın (200 blok yukarıdaki küçük meteor da seçilir); plazma ve kıvılcım parçacıkları ×1,5 büyük, sayıları
aynı. Var olan dünyanın SERVER config'i açılışta göç eder (`meteor.ayarSurumu`): eski varsayılanda kalan meteor ve
yağmur değerleri (tavan, doğuş yüksekliği, yağmur yoğunluğu ve süresi, büyük meteor oranları) yeniye çekilir, elle
değiştirilenler kalır.

## Buz altı balıkçılık (madde 15)

### Gerçek olay

Buz balıkçılığında (ice fishing) balıkçı göl buzunu burgu ya da keski ile deler; delik genellikle 15–25 cm çaplıdır,
kenarı keskin ve yarı saydam buzdur, içinde koyu, durgun su görünür. Delik açık bırakılırsa birkaç dakikada yüzeyde
ince bir buz zarı ("skim ice") oluşur: önce kenarlardan iğne kristaller uzanır, zar ortaya doğru kapanır, balıkçı
kepçeyle kırar. Soğuk suda balık yavaştır, ısırma seyrektir; balıkçı uzun süre hareketsiz bekler ve asıl tehlike
hareketsizlikle gelen soğumadır (rüzgârlıkta çadır, ısıtıcı, kalın giysi). Göller ve kıyı sularında alabalık, morina,
kefal, ringa tutulur; ringa sürüleri kışın kıyıya yaklaşır.

### Minecraft'ta

- **Delik bloğu `buz_deligi`:** 3 piksel kalın buz çerçeve (kendi dokusu, ~%85 opak, beyaz çatlaklı), ortadaki 10×10
  açıklıkta su. Su modelde çizilmez: blok kaynak su sıvı durumu taşır, vanilla sıvı çizicisi biyom renkli, hareketli
  su yüzeyini delikte çizer. Böylece delik her biyomda göldeki suyla aynı renkte.
- **Yavaş yeniden donma:** ömrün son 45 saniyesinde (3 × 15 sn) açıklığın üstünde kenardan içeri büyüyen yarı saydam buz
  zarı (üç doku: kenar şeridi, yarısı, neredeyse kapalı), her aşamada hafif cam tıkırtısı ve kar tanesi parçacıkları;
  sonunda buz geri gelir (`glass_place`, buz kırıntısı parçacıkları). Açıkken arada bir kabarcık patlaması.
- **Açılış anı:** eğilerek kazma/balta ile sağ tık → cam kırılma + hafif sıçrama sesi, buz kırıntısı, su sıçraması ve kar
  tanesi parçacıkları; kol sallanır, alet 5 dayanıklılık kaybeder.
- **Bekleyiş:** olta deliğin suyunda vanilla gibi yüzer. Isırma 30–60 sn (Lure seviye başına 5 sn kısaltır); son 2 sn
  vanilla'nın "balık yaklaşıyor" evresi, sonra şapırtı ve şamandıranın batması. Normal buzda olta buzun üstünde yatar.
- **Balıklar:** beş tür 16×16, vanilla balıklarıyla aynı çapraz duruş; renk, desen ve siluetle ayrılır (alabalık pembe
  yan şerit + benekler, morina zeytin alacalı + çene bıyığı, somon koyu lacivert sırt + gümüş yan, iri ve derin çatal
  kuyruk, kefal boyuna çizgili gri + yuvarlak kuyruk, ringa ince, parlak mavi + beyaz kırağı kenarı ve parıltı).
  Pişmişler türün kendi et rengiyle ızgara izli (alabalık pembe-turuncu, morina krem beyaz, somon koyu turuncu + yağ
  çizgileri, kefal gri-kahve çizgili, ringa altın). Tooltip: çiğ "serinletir (küçük)" (camgöbeği), pişmiş "ısıtır
  (orta)" (altın), pişmiş ringa "güçlü ısıtır (büyük)" (kırmızı); sayılar yiyecek kademelerinden (−8, +20, +35).
- **Risk:** delikte oltası olan ve buz üstünde hareketsiz duran oyuncunun ortamına 20 sn'de 8 °C'ye çıkan soğuk eklenir;
  sıcaklık barı gözle görülür biçimde mavi tarafa kayar. Ateş, sıcak yiyecek ya da kalın giysi bunu dengeler.

### Vitrin anı

Donmuş gölün ortasında, gün batımında, kazmayla açılmış deliğin yakın planı: buz çerçevenin ortasında koyu, hafif
dalgalı su, yüzen kırmızı-beyaz şamandıra; balık ısırınca şapırtı ve kabarcıklar, oyuncunun elinde Donmuş Ringa.

### Ses

Yalnız vanilla sesler: açılış `glass_break` (perde 1,25) + `generic_splash` (kısık); donma aşamaları `glass_hit`
(perde 0,5→0,8); kapanış `glass_place` (perde 0,6); ısırma vanilla şamandıra şapırtısı. Yeni ses dosyası yok.

## Başarımlar (madde 12, N16)

**Hedef his.** Sekme bir "hayatta kalma günlüğü" gibi okunmalı: kökten dallanan her kol bir felaket hikâyesi (bölgesel afetler,
gökten gelenler, güneşin ölümü ve buzul çağı, ayın düşüşü, hazırlık, şiddet). Toast ve sohbet duyurusu vanilla'nın kendisidir
(N6 istisnası); ödül küçük ama elle tutulur: XP, hedeflerde bir iki sıcak yiyecek, meydan okumalarda adın önünde bir ünvan.

**Sekme.** "Doğal Afetler", kök "Felaket Başlıyor" (çan ikonu: siren). Arka plan `textures/gui/advancements/backgrounds/felaket.png`,
16×16 döşenebilir: koyu bazalt/kül zemin, kenardan kenara akan fay çatlakları, çatlak içinde kor parıltısı (deprem yarığı ile
meteor/yangın korunun ortak dili). Kontrast düşük tutuldu ki ikonlar ve bağlantı çizgileri okunsun. Üretim:
`tools/assets/basarim_ikon.py` (Pillow, sabit tohum; aynı baytlar). Değiştirmek için betikteki `ZEMIN`, `CATLAK`, `KOR` renkleri
ya da `YOLLAR` çatlak yolları düzenlenir ve betik yeniden koşulur; dosya adı ve 16×16 boyutu korunmalı.

**Ağaç (yerleşim, ön koşul değil).**

| Kol | Başarımlar |
|---|---|
| Bölgesel afetler | 1 İlk Rüzgâr, 2 Dalga Avcısı, 3 Sallanan Zemin, 4 Delikten Dönüş, 5 Yanmadan Çıktım, 6 Çatı Altı |
| Gökten gelen | 7 Gökten Gelen Misafir → 22 Kozmik Şans |
| Güneşin ölümü | 8 Güneşi Atlattım → 9 Süpernova Tanığı → 10 Buzul Çağı → 11 Kutup Sığınağı; 10 → 20 Buz Balıkçısı → 21 Kutup Ustası |
| Ay | 12 Ay Düştü → 13 Yıldızlı Gece, 14 Yükselen Sular |
| Aletler | 16 Radar Altında → 15 Yer Dinleyici |
| Giyim ve sıcaklık | 17 Katmanlı Giyim → 18 Yünlü Kahraman → 19 Çorba İçer Isınır |
| Şiddet | 23 Fırtınaya Meydan Oku → 24 Felaket Ustası → 25 Hepsini Atlattım (gizli); 23 → 26 Birlikte Hayatta (gizli) |

Vanilla görünürlüğü korunur: kazanılmış bir atanın en çok iki kat altı görünür, böylece sekme ilk sirenle açılır ve oyuncu
ilerledikçe derinleşir (buzul çağı kolu süpernovadan önce sır kalır). #25 ve #26 açılana kadar hiç görünmez.

**İkonlar.** Vanilla eşyalar anlamı taşır: çan (siren), elitra (rüzgâr), trident (dalga), çatlak taş tuğla (deprem), merdiven
(çukurdan dönüş), kamp ateşi (yangın), tuğla basamak (çatı), ayçiçeği (güneş), nether yıldızı (süpernova), sıkıştırılmış ve mavi
buz (buzul, kutup), olta ve morina (buz balıkçılığı), end taşı (ay), dürbün (yıldızlı gece), kanal (yükselen sular), tüy, beyaz
yün, paratoner (fırtına), ölümsüzlük totemi (felaket ustası), fener (hepsi), pasta (birlikte). Mod eşyası ikonları (Meteorit
Parçası, Kozmik Çekirdek, Hava Radarı, Sismograf, Sıcak Tavuk Çorbası) kardeş dallar birleşince `runData` ile gerçek eşyaya döner;
o zamana kadar vanilla yedeği (ateş topu, End kristali, pusula, sculk sensörü, mantar çorbası).

**Çerçeveler ve ödüller.** Görev (köşeli) 15 XP; hedef (yuvarlak) 40 XP + 1–2 Sıcak Salep (yiyecek dalı gelene kadar fırınlanmış
patates); meydan okuma (dikenli, mor duyuru) 100 XP + ünvan. XP'ler `basarim.odul.*` config'iyle değişir.

**Ünvan.** Altın renkli `[Ünvan] ` öneki sohbet adında ve tab listesinde (N16). Her meydan okumanın kendi ünvanı: Kutup Sakini,
Kozmik Kâşif, Felaket Ustası, Her Şeyi Atlatan; en son kazanılan geçerli. Ad çevrilebilir bileşen olduğu için her oyuncu kendi
dilinde görür. `basarim.unvanGoster=false` öneki kapatır; `/dogalafet unvan <oyuncu> [ad|yok]` OP'nin elidir.

**Vitrin anı.** Seviye 5 bir afetin doğal bitişinde üç toast'ın üst üste akması (İlk Rüzgâr, Fırtınaya Meydan Oku, mor
çerçeveli Felaket Ustası) ve sohbetteki mor duyurunun kendisinde oyuncunun adının önünde beliren altın `[Felaket Ustası]`
(ünvan duyurudan önce verilir). Üçü aynı tick'te aynı tetikleyiciden gelir; oyunda sıraları garanti değildir. Görsel kanıt
koşusu sırayı sabitler: tür ve seviye 3 ayrı tick'lerde verilir, doğal bitiş Felaket Ustası'nı açar.

## Görsel cila (2026-11-07)

Beş buz balığı ve pişmişleri vanilla balık ikonu kalitesinde yeniden çizildi (`tools/assets/balikcilik_dokular.py`, rastgelelik
yok): çapraz duruş, 1 px seçici dış çizgi (üst açık, karın/alt-sağ en koyu), sırttan karına 4+3 tonluk ters gölgeleme, sıralı pul
gölgesi, ışınlı açık yüzgeçler, 2 px göz, solungaç ve göğüs yüzgeci. Tür bir bakışta ayrılır: alabalık pembe şerit ve benek,
morina alacalı sırt, yan çizgi, iki sırt yüzgeci ve bıyık; somon iri, lacivert-gümüş, derin çatal kuyruk; kefal gri boyuna
çizgili, yuvarlak kuyruk; ringa ince parlak mavi, kırağı kenar ve parıltı. Pişmişler aynı siluette altın-kahve rampa, kıtır
kenar, iki ızgara izi ve bulanık beyaz göz taşır. Kutup Balığı Çorbası kendi kasesini alır (parlayan ön ağız, damarlı gölgeli
ahşap, buz mavisi krem çorbada somon/beyaz balık ve dereotu); kullanıcı isteğiyle buharsızdır, ısıtan olduğu adı ve tarifiyle okunur.

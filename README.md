# Doğal Afetler

Minecraft 1.20.1 (Forge) için gerçekçi doğal afetler, vücut sıcaklığı sistemi ve dünya dönemleri modu. Kasırga, tsunami,
deprem, sinkhole, asit yağmuru, orman yangını, güneş patlaması ve ay çarpması; afetlerin bıraktığı izler (donan dünya, yükselen
deniz, aysız gökyüzü, kızıl cüce güneş) dünyada kalıcıdır.

- Sürüm: **0.1.0** · Minecraft **1.20.1** · Forge **47.4.28** ve üstü (47.x)
- Jar: `dogalafetler-0.1.0.jar` · Mod kimliği: `dogalafetler`
- Yazar: Kadir Berk Polat · Lisans: MIT

## İçindekiler

1. [Kurulum (Windows'ta oynamak)](#kurulum-windowsta-oynamak)
2. [Arkadaşlarla oynamak](#arkadaşlarla-oynamak)
3. [Derlemek (Windows ve Linux)](#derlemek-windows-ve-linux)
4. [Afetler](#afetler)
5. [Ortak sistemler](#ortak-sistemler)
6. [Komutlar](#komutlar)
7. [Gamerule'lar](#gamerulelar)
8. [Config dosyaları ve önemli ayarlar](#config-dosyaları-ve-önemli-ayarlar)
9. [Başarımlar](#başarımlar)
10. [Cold Sweat ile birlikte kullanmak](#cold-sweat-ile-birlikte-kullanmak)
11. [Ses ve doku dosyaları](#ses-ve-doku-dosyaları)
12. [Bilinen sınırlar](#bilinen-sınırlar)
13. [Belgeler](#belgeler)
14. [Lisans](#lisans)

## Kurulum (Windows'ta oynamak)

1. Minecraft Launcher'da **1.20.1** sürümünü bir kez açıp kapatın (dosyalar insin).
2. [Forge indirme sayfasından](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1 için
   **Installer**'ı indirin (47.4.28 ya da daha yeni bir 47.x), çalıştırın, **Install client** ile kurun.
3. `Win + R` → `%APPDATA%\.minecraft` → `mods` klasörü yoksa oluşturun ve `dogalafetler-0.1.0.jar`'ı içine kopyalayın.
4. Launcher'da **forge-1.20.1** profilini seçip oynayın. Ana menüde **Mods** listesinde "Doğal Afetler" görünmelidir.

Java ayrıca kurulmaz; Launcher kendi Java'sını kullanır.

## Arkadaşlarla oynamak

Mod hem sunucuda hem **her oyuncunun bilgisayarında** aynı sürümle kurulu olmalıdır. Sürümler farklıysa sunucu listesinde
uyarı görünür ve bağlantı reddedilir (`displayTest=MATCH_VERSION`). Bütün afet mantığı sunucuda çalışır; istemciye yalnız
görüntü ve ses için gereken durum gönderilir. Hedef grup 5–10 oyuncudur; afet sıklığı oyuncu sayısıyla artmaz.

- **e4mc (dünyayı internete açmak):** dünyayı açan kişi Doğal Afetler'in yanına [e4mc](https://modrinth.com/mod/e4mc)'nin
  Forge 1.20.1 sürümünü kurar, dünyada **LAN'a Aç**'a basar; e4mc'nin verdiği adresi arkadaşlarına iletir. Katılan oyuncuların
  yalnız Doğal Afetler'e (ve Forge'a) ihtiyacı vardır.
- **LAN:** aynı ağdaysanız Minecraft'ın kendi **LAN'a Aç** özelliği yeterlidir.
- **Dedicated sunucu:** Forge installer'da **Install server** ile boş bir klasöre kurun, `mods/` klasörüne jar'ı koyun,
  `run.bat` (Windows) ya da `run.sh` (Linux) ile başlatın; ilk açılışta `eula.txt`'de `eula=true` yapın. Komutlar sunucu
  konsolundan da çalışır.

## Derlemek (Windows ve Linux)

Gerekenler: **Java 17** (ör. Eclipse Temurin 17 JDK) ve ilk derleme için internet (Gradle, Forge ve Minecraft dosyaları
indirilir; sonraki derlemeler `--offline` ile de çalışır).

```
# Windows (cmd ya da PowerShell)
gradlew.bat build

# Linux
./gradlew build
```

Çıktı: `build/libs/dogalafetler-0.1.0.jar` (oyuna kurulacak, reobf'lu jar). Faydalı görevler:

| Görev | Ne yapar |
|---|---|
| `gradlew test` | Birim testleri (JUnit) |
| `gradlew runGameTestServer` | Oyun içi testler (GameTest) |
| `gradlew runData` | Dil dosyaları, tarifler, başarımlar, loot tabloları, `sounds.json` ve parçacık tanımlarını `src/generated/resources/` altına üretir |
| `gradlew runClient` / `runServer` | Geliştirme istemcisi / sunucusu |

İsteğe bağlı uyumluluk modlarıyla geliştirme istemcisi: `gradlew runClient -Puyum=coldsweat` (ya da `e4mc`, `ikisi`).

## Afetler

Her afet rastgele zamanda, çevrim içi oyunculardan birinin çevresinde başlar (aynı oyuncu art arda hedef seçilmez). Afetten
önce ön-uyarı penceresi (45–90 sn), ardından siren gelir; afetin etki alanındaki ve yakınındaki (+64 blok) oyuncular sireni
duyar, güneş patlaması ve ay çarpmasında herkes. Ekranda
afet yazısı çıkmaz: afetin türünü ve seviyesini yalnız Hava Radarı ve Sismograf gösterir. Aynı anda tek afet olur.

Seviyeli afetler 1–5 arasıdır: **1 Hafif, 2 Orta, 3 Şiddetli, 4 Yıkıcı, 5 Felaket**. Yeni dünyada yalnız seviye 1 vardır;
seviye 2/3/4/5 sırasıyla oyunun 10./25./50./80. gününde açılır ve yüksek seviye her zaman daha nadirdir. Şiddetli afetten
sonraki bekleme biraz uzar.

| Afet | Komut adı | Seviye | Kısaca |
|---|---|---|---|
| Kasırga | `kasirga` | 1–5 | Dönen huni, toz/enkaz bulutu; blokları, mobları ve oyuncuları havaya çeker, yolunda iz bırakır, ilerleyip dağılır. |
| Tsunami | `tsunami` | 1–5 | Yalnız geniş deniz kıyısında başlar. Önce sular çekilir, sonra dalga karaya ilerler: oyuncuları ve mobları sürükler, önce cam ve yaprakları, sonra ahşabı (yüksek seviyede taş yapıları) yıkar, dalganın girdiği mağaraları geçici olarak doldurur. Su, modun kendi çizdiği "sanal su"dur; haritaya gerçek su bloğu dökülmez. |
| Deprem | `deprem` | 1–5 | Haritayı yaran fay hattı; yaprak ve ahşap gibi sağlam olmayan bloklar yıkılıp düşer, sağlam bloklar daha az etkilenir. Ekran sarsıntısı mesafeyle azalır; seviye 3 ve üstünde artçı sarsıntılar olur. |
| Sinkhole | `sinkhole` | 1–5 | Çatlama ve titremeden sonra dairesel çöküş; bloklar çukura dolar. |
| Asit yağmuru | `asit_yagmuru` | 1–5 | Yeşilimsi yağış ve dumanlanma; sert olmayan blokları yavaş yavaş aşındırır, açıktaki oyuncuya orta düzeyde hasar verir; çatı altı korur. |
| Orman yangını | `orman_yangini` | 1–5 | Ortak yangın sistemiyle yanıcı olan her şeye yayılır, yağmurda söner; yakınındaki ortam ısınır. |
| Güneş patlaması | `gunes_patlamasi` | yok | En nadir afet, tek seferlik. Uzun siren; güneş büyür, meteorlar yağar, dünya 5 gün boyunca kavrulur, sonunda **süpernova**. Ardından güneşsiz dönem, dünyanın donması ve **kızıl cüce** güneşin doğuşu gelir (aşağıda "Dünya dönemleri"). |
| Ay çarpması | `ay_carpmasi` | yok | Güneş patlamasından biraz daha sık, tek seferlik. Ay yaklaşır ve çarpar (ekran ve harita sallanır), 1–2 dakika ay parçaları yağar, gökte enkaz halkası kalır. 10 dakika sonra deniz seviyesi kalıcı olarak yükselmeye başlar ve tsunamiler bir süre daha sık olur. Ay bir daha oluşmaz. |

Güneş patlaması ve ay çarpması yalnız dünya yeterince yaşlanınca seçilir (varsayılan: güneş için 40., ay için 25. oyun günü;
ölçüt config'ten oyuncuların gelişimine çevrilebilir).

## Ortak sistemler

- **Siren ve ses:** sirenler ve afet sesleri kısık tutulur; siren ses düzeyi istemci ayarıdır (varsayılan %28).
- **Dünya dönemleri:** güneş (normal → büyüyen → süpernova → güneşsiz → kızıl cüce ya da düşük olasılıkla pulsar) ve ay
  (var/yok) birbirinden bağımsızdır ve dünyaya kaydedilir; sonradan giren oyuncu aynı gökyüzünü görür. Güneşsiz dönemde dünya
  birkaç günde donar (çimenin üstüne kar katmanı, buz, kırağı rengi; donma uzaktaki chunk'lara oyuncu geldikçe yetişir). Kızıl
  cüce doğunca dünya kısmen çözülür, kalıcı kış bölgeleri donuk kalır. **Pulsar** (düşük olasılık) güneşten kat kat büyük, sağa
  sola hızla titreşen 3B bir nötron yıldızıdır (mavi-beyaz çekirdek, manyetik alan çizgileri, saniyede ~11 kez nabızlanan iki
  ışın konisi ve aynı vuruşta tıklayan bir ses; ses yer altında ve üstü kapalı yerlerde azalır, boğuk ve yankılı duyulur):
  15 oyun günü boyunca dünya cehenneme döner (çimen kavrulur ve küle döner,
  ağaçlar yanıp kömürleşir, kül yağar, seyrek lav gözeleri açılır, dakikada 12 meteor düşer, sıcaklık çok yükselir). Sonra uzak
  bir kara delik pulsarı 90 saniyede yutar (gündüz başlar); dünya 10 gün buzul kıyamete sürüklenir (küller kar altında kalır),
  ardından kızıl cüce doğar ve doğuş bitince eski güneş geri gelir.
- **Vücut sıcaklığı:** biyom, gün/gece, yükseklik, ısı kaynakları, yağmur ve dünya dönemi ortam sıcaklığını belirler; HUD'daki
  gösterge vücut sıcaklığını gösterir. Aşırı soğuk ve sıcak hasar verir. Zırha yün eklemek (yünlü zırh), tüy işlemek (parça
  başına en çok 30) ve Termostat Plakası eklemek (çok sıcakta aşırı ısınmayı azaltır) sıcaklığı dengeler.
- **Sıcaklık yiyecekleri:** Sıcak Salep, Sıcak Çikolata, Isıtıcı Çay, Sıcak Tavuk Çorbası, Ateşli Güveç ısıtır; Ayran, Meyve Şerbeti,
  Buzlu Karpuz Dilimi serinletir. Aynı yiyecek kısa sürede tekrar yenirse etkisi azalır.
- **Hava Radarı:** elde tutulunca küçük bir gösterge menzildeki (300 blok) yaklaşan ya da süren afetin adını, yönünü,
  uzaklığını ve süresini gösterir; güneş patlaması ve ay çarpması "gökyüzü anomalisi" olarak görünür. **Gelişmiş Hava Radarı**
  (Hava Radarı + Kozmik Çekirdek): 800 blok, seviye, hareket yönü ve son 5 afet.
- **Sismograf:** envanterde taşınınca deprem ve sinkhole'un öncü sarsıntısını hafif ses ve action bar notuyla bildirir; sağ tıkla
  sismik dalga grafiği açılır. **Hassas Sismograf** (Sismograf + Kozmik Çekirdek) daha erken uyarır ve seviyeyi de gösterir.
- **Meteorlar:** güneş patlaması ve ay çarpması meteor yağdırır; kraterde nadiren **Meteorit Parçası**, çok nadiren **Kozmik
  Çekirdek** bulunur (yaklaşık 10 dakika sonra kaybolur). 3 Meteorit Parçası + 1 demir külçesi → Meteorit Külçesi.
- **Ortak yangın:** orman yangını ve meteor ateşleri modun kendi "Afet Ateşi" bloğuyla yayılır (vanilla ateşten bağımsız).
- **Sanal su:** tsunami dalgası, mağara suyu ve deniz yükselmesi gerçek su bloğu koymadan çizilir; içinde yüzülür, boğulunur ve
  sürüklenilir. Deniz yükselmesi (varsayılan +4 blok) ay çarpmasından sonra kalıcıdır.
- **Buz altı balıkçılığı:** donmuş dünyada eğilip kazma/balta ile sağ tıklayınca buzda delik açılır, delik birkaç dakikada
  yeniden donar. Delikten beş yeni soğuk su balığı (Buz Alabalığı, Buz Kefali, Derin Soğuk Somonu, Donmuş Ringa, Kutup Morinası)
  ve nadiren hazine çıkar; Kutup Balığı Çorbası bunlardan yapılır.

## Komutlar

Bütün komutlar `/dogalafet` altındadır ve **yalnız OP** (yetki seviyesi 2) kullanabilir. Afet adları: `kasirga`, `tsunami`,
`deprem`, `gunes_patlamasi`, `asit_yagmuru`, `sinkhole`, `orman_yangini`, `ay_carpmasi`.

| Komut | Ne yapar |
|---|---|
| `/dogalafet start <afet> [seviye] [oyuncu]` | Afeti hemen başlatır (beklemeyi, gamerule'ü ve gün saati penceresini atlar; aktif afet varken reddeder). Doğal güneş patlaması yalnız sabah (gün saati 0–6000), ay çarpması yalnız gece (13000–23000) patlar; komutla başlatma bu pencereye bakmaz. |
| `/dogalafet stop` | Aktif afeti temizce durdurur; afet yoksa süren dünya dönemi süreçlerini durdurur. |
| `/dogalafet durum` | Aktif afet, aşama, seviye, kalan süre, beklemeler, çarpanlar, deniz seviyesi, ay/güneş, donma, buharlaşma ve süren dönem süreçlerinin aşaması (pulsar zincirinde kalan gün). |
| `/dogalafet sicaklik set <oyuncu> <değer>` | Oyuncunun vücut sıcaklığını ayarlar. |
| `/dogalafet sicaklik bilgi <oyuncu>` | Vücut ve ortam sıcaklığı, kalemler, yalıtım. |
| `/dogalafet denizseviyesi <değer> [kademeli]` | Deniz seviyesi artışını ayarlar/sıfırlar (0–6 blok; `kademeli` yavaşça). |
| `/dogalafet aysonrasi atla` | Ay çarpmasından sonraki 10 dakikalık beklemeyi atlayıp deniz yükselmesini başlatır. |
| `/dogalafet donma <yüzde> [donma\|cozulme]` | Global donma ilerlemesini ayarlar/sıfırlar. |
| `/dogalafet donma bilgi` | Donma ilerlemesi, dönem, buharlaşma, işlenen chunk kuyruğu. |
| `/dogalafet kizilcuce baslat` | Beklemeyi atlayıp kızıl cüce doğuşunu başlatır. |
| `/dogalafet kizilcuce bilgi` | Gökyüzü dönemi, çözülme ilerlemesi, bulunduğunuz chunk kalıcı kış mı. |
| `/dogalafet pulsar baslat` | Kızıl cüce yerine pulsarı hemen doğurur (olasılık, donma ve şafak beklenmez; zaten pulsarsa ya da güneş patlaması sürüyorsa reddeder). Pulsar zinciri: pulsar (15 gün) → kara delik yutması (90 sn) → buzul kıyamet (10 gün) → kızıl cüce doğuşu → eski güneş. |
| `/dogalafet gun <sayı>` | Oyun gününü ayarlar ve seviye olasılıklarını gösterir. |
| `/dogalafet odul meteor [parca\|cekirdek\|hepsi] [adet] [oyuncu]` | Test için Meteorit Parçası / Kozmik Çekirdek verir. |
| `/dogalafet asama ileri [süreç]` | Aktif afeti ya da bir dünya dönemi sürecini sonraki aşamaya geçirir. Pulsar zinciri için `pulsar` (her aşamada; pulsarda ilk adım ısınmayı tamamlar) ya da o aşamadayken `karadelik`, `gunessiz`, `kizilcuce`. |
| `/dogalafet hiz <çarpan>` | Afet ve dönem zamanlayıcılarını hızlandırır (1 = normal; yeniden başlatınca 1'e döner). |
| `/dogalafet sifirla` | Dünyanın afet ve dönem durumunu ilk hâline döndürür (ay, güneş, deniz, donma). |

### Yönetim ve hata ayıklama komutları

| Komut | Ne yapar |
|---|---|
| `/dogalafet buharlasma <derinlik>` | Güneş patlamasındaki buharlaşma derinliğini ayarlar. |
| `/dogalafet yangin tutustur \| sondur \| bilgi` | Ortak yangın sistemini elle yönetir. |
| `/dogalafet meteor dusur [buyuk] [ay] …`, `meteor yagmur <saniye> [dakikaBasina]`, `meteor durdur`, `meteor bilgi` | Meteor düşürür / yağmuru yönetir. |
| `/dogalafet kasirga bilgi` | Aktif kasırganın durumu. |
| `/dogalafet su bilgi \| dalga \| magara \| cekilme \| temizle \| kiyi` | Sanal su durumu ve elle dalga/mağara/çekilme. |
| `/dogalafet yiyecek bilgi [oyuncu]` | Yiyecek etkileri ve oyuncunun azalan verim pencereleri. |
| `/dogalafet basarim bilgi [oyuncu]` | Başarım ilerlemesi. |
| `/dogalafet unvan <oyuncu> [ad]` | Meydan okuma ünvanını gösterir/ayarlar. |

## Gamerule'lar

Hepsi `/gamerule <ad> [değer]` ile okunur/değiştirilir (tab tamamlamalı) ve dünyaya kaydedilir.

| Gamerule | Tür | Varsayılan | Ne yapar |
|---|---|---|---|
| `dogalAfetlerAcik` | boolean | `true` | Bütün afet sistemini açar/kapatır. |
| `kasirgaAfeti`, `tsunamiAfeti`, `depremAfeti`, `asitYagmuruAfeti`, `sinkholeAfeti`, `ormanYanginiAfeti`, `gunesPatlamasiAfeti`, `ayCarpmasiAfeti` | boolean | `true` | Afetleri tek tek açar/kapatır. |
| `afetSikligi` | int | `100` | Afet sıklığı yüzdesi (50 = yarı, 200 = iki katı). |
| `afetMaksSiddet` | int | `5` | Oluşabilecek en yüksek seviye (1–5). |
| `afetBlokHasari` | boolean | `true` | Kapalıyken afetler blok yıkmaz, yakmaz, dondurmaz, çözmez; oyuncu ve mob hasarı sürer. |
| `sicaklikSistemi` | boolean | `true` | Vücut sıcaklığı sistemini ve HUD'u açar/kapatır. |

- Kapatılan afet bir daha seçilmez. Henüz duyurulmamış (ön-uyarıdaki) afet sessizce iptal olur; başlamış afet güvenle biter.
  Süren güneş patlaması ya da ay çarpması yumuşakça durur (tamamlanmamış güneş büyümesi geri alınır); güneş zinciri gibi dönem
  süreçleri duraklar ve kural açılınca kaldıkları yerden sürer. Ulaşılmış dönemler (aysız gök, donma, kızıl cüce) geri alınmaz.
- `/dogalafet start` ile elle başlatılan afet gamerule'lerden etkilenmez.
- **Haritayı korumak için:** afet ateşi vanilla `doFireTick`'ten bağımsızdır; `doFireTick false` afet yangınlarını durdurmaz.
  Haritayı afetlerden korumanın tek yolu `afetBlokHasari false`'tur.

## Config dosyaları ve önemli ayarlar

- **Sunucu ayarları:** `dogalafetler-server.toml`, dünya başınadır: tek oyunculuda `saves/<dünya>/serverconfig/`, dedicated
  sunucuda `world/serverconfig/`. Bütün dünyalar için varsayılan vermek isterseniz dosyayı `defaultconfigs/` klasörüne koyun.
- **İstemci ayarları:** `config/dogalafetler-client.toml` (her oyuncunun kendi bilgisayarında).

Dosya ilk açılışta bütün anahtarlar ve açıklamalarıyla yazılır. Her sayısal varsayılanın gerekçesi
[`docs/KARARLAR.md`](docs/KARARLAR.md) sonundaki "N17 — Sayısal varsayılanlar" tablosundadır.

### İstemci (görüntü ve ses)

| Anahtar | Varsayılan | Ne yapar |
|---|---|---|
| `gorsel.efektKalitesi` | `YUKSEK` | `DUSUK` / `ORTA` / `YUKSEK` / `ULTRA`. Düşük, zayıf bilgisayarlar içindir; bütün afetlerin parçacık, gökyüzü ve su ayrıntısını ölçekler. |
| `uyari.sirenSes` | `28` | Siren ses düzeyi (0–100). |
| `yangin.parcacikYogunlugu`, `su.parcacikYogunlugu`, `donma.parcacikYogunlugu`, `meteor.izYogunlugu`, `gokyuzu.yildizYogunlugu` | `1,0` | Efekt kalitesinin üstüne parçacık ince ayarı (0 = kapalı). |
| `deprem.sarsintiCarpani` | `100` | Deprem ekran sarsıntısı (% 0–200). |
| `ay_carpmasi.ekranSarsintisi`, `ay_carpmasi.parlama` | `1,0` | Ay çarpmasının ekran sarsıntısı ve parlaması (0 = kapalı; harekete/ışığa duyarlı oyuncular için). |
| `gokyuzu.pulsarTitresim` | `1,0` | Pulsarın hızlı titreşimi, ışın konilerinin strobu ve kısa beyaz parlamaları (0 = kapalı: koniler sabit yanar, yalnız yavaş sarsıntı kalır). **Fotosensitivite:** pulsar dönemi saniyede ~16 kez titreşen ışık, saniyede ~11 kez nabızlanan büyük ışın konileri (parlaklık %45 ile %100 arasında; tam kararmaz) ve saniyede ~2 kısa parlama içerir; ışığa duyarlı oyuncular 0 yapmalı. Kara delik yutmasının 55. saniyesindeki parlama ve sarsıntı `ay_carpmasi.parlama` gibi kapatılamaz, 1,5 sn sürer. |

### Sunucu: yıkım hızı ve afet başına çarpanlar

Tek bir genel "yıkım hızı" yerine her afetin kendi ayarı vardır, çünkü afetlerin yıkım birimi farklıdır (blok/tick, sütun, yangın
hücresi). Sunucu TPS'i düşünce bu bütçeler kendiliğinden kısılır.

| Afet | Yıkım hızı | Diğer önemli ayarlar |
|---|---|---|
| Kasırga | `kasirga.blokCarpani` (1,0; 0–4) | `boyutCarpani`, `hizCarpani`, `sureCarpani`, `cekimCarpani`, `hasarCarpani` |
| Tsunami | `tsunami.yikimCarpani` (1,0), `tsunami.yikimBolumTick` (1) | `yikimEsyaOrani` (0,3), `ayCekilmeCarpani` (1,5) |
| Deprem | `deprem.blokButcesi` (seviye başına 32 … 128 blok/tick) | `yikimYaricapi` (16 … 100 blok) |
| Sinkhole | `sinkhole.blokButcesi` (400 blok/tick) | |
| Asit yağmuru | `asit_yagmuru.eszamanliAsindirma` (64 blok) | `kaymaHizi` (0,3 blok/sn) |
| Orman yangını / ortak yangın | `yangin.yayilmaHizCarpani` (1,0; 0 yayılmayı kapatır), `yangin.tickButcesi` (64 hücre/tick) | `yangin.aktifTavan` (1500 hücre), `orman_yangini.yanikIziButcesi` (8) |
| Ay çarpması | `ay_carpmasi.sarsintiYogunlugu` (0,30) | |
| Meteorlar | `meteor.kraterBlokButcesi` (512 blok işlemi/tick) | `meteor.kucukYanginYogunlugu` / `buyukYanginYogunlugu` |
| Donma | `donma.donmaSuresi` (6 MC günü) | `donma.buharlasmaEnCok` (6 blok) |

### Sunucu: afet sıklığı ve dönemler

| Anahtar | Varsayılan | Ne yapar |
|---|---|---|
| `cekirdek.zamanlayici.gunlukAfetOrani` | `0,8` | Bekleme dışında MC günü başına afet oranı (2–3 saatlik oturumda 3–4 afet). |
| `cekirdek.onUyari.enAzTick` / `enCokTick` | `900` / `1800` | Ön-uyarı penceresi (45–90 sn). |
| `cekirdek.siddet.seviye2Gunu` … `seviye5Gunu` | `10` / `25` / `50` / `80` | Seviyelerin açıldığı oyun günü. |
| `cekirdek.tekSeferlik.gunesPatlamasiMinGun`, `ayCarpmasiMinGun` | `40`, `25` | Tek seferlik afetlerin en erken günü; `…Kriteri` ile oyuncu gelişimine (XP + zırh) bağlanabilir. |
| `cekirdek.tekSeferlik.gunesPatlamasiTekrarlanabilir` | `false` | Güneş patlaması kızıl cücede tekrar gelebilsin mi. |
| `su.aysonrasiBeklemeDk`, `su.denizHedefArtis` | `10`, `4` | Ay çarpmasından sonra deniz yükselmesinin beklemesi (gerçek dakika) ve hedefi (blok). |
| `gokyuzu.kizilCuceGecikmeGun` | `7` | Donma %100'e ulaştıktan sonra kızıl cücenin doğmasına kadar gün. |
| `gokyuzu.pulsarOlasiligi` | `0,15` | Süpernova sonrası doğal doğuşta kızıl cüce yerine pulsar doğma olasılığı (dünya tohumundan belirlenir). |
| `gokyuzu.pulsarGun` | `15` | Pulsar döneminin süresi (oyun günü; uyku ilerletir, `/time set` ilerletmez). Bitince kara delik yutması gündüz penceresinde (gün saati 3000–7200) başlar. |
| `gokyuzu.pulsarIsinmaGun` | `1` | Pulsarın dünya etkilerinin tam güce çıkma süresi, MC günü (eski varsayılan 2 göçle 1'e çekilir). |
| `gokyuzu.pulsarYanginDakikaBasina` | `24` | Tam güçte oyuncu başına (gerçek) dakikada yeni yangın odağı (`afetBlokHasari false` iken yok; eski 20 göçle 24'e çekilir). |
| `gokyuzu.pulsarKavrulmaOlasiligi` | `0,6` | Tam güçte bir kavurma denemesinin isabet olasılığı. Deneme sayısı alanla orantılı: varsayılanla 3 oyun gününde 96 blok yarıçapta çimen kalmaz, ağaçlar kömür ve kül olur (çimen → kaba toprak → kül bloğu, yaprak → kül katmanı, kütük → kömürleşmiş kütük). |
| `gokyuzu.pulsarMeteorDakikaBasina` | `12` | Pulsar boyunca oyuncu başına dakikada meteor (güneş patlaması ve ay çarpması 180). |
| `gokyuzu.pulsarBlokButcesi` | `256` | Pulsarın tick başına en çok blok değişimi (kavurma, kül, lav; TPS düşünce kısılır). |
| `gokyuzu.pulsarLav` | `true` | Kavrulan yüzeyde seyrek tek bloklu lav gözeleri. |
| `gokyuzu.pulsarSonrasiGunessizGun` | `10` | Kara delik yutmasından sonra buzul kıyametin süresi (oyun günü); bitince kızıl cüce ilk şafakta doğar. |
| `sicaklik.sicaklikSistemiModu` | `OTOMATIK` | `OTOMATIK`: Cold Sweat yüklüyse kendi sıcaklık sistemi kapalı; `ACIK` / `KAPALI`. |
| `basarim.etkiAlaniPayi` | `0` | "Sağ çıkma" için afet yarıçapına eklenen pay (blok). |

## Başarımlar

Modun kendi başarım sekmesi **Doğal Afetler**'dir; 27 başarım vardır. "Sağ çıkmak": afetin etki alanındayken afet bitene kadar
ölmemek. Çok oyunculuda her oyuncu kendi başarımını kazanır.

**Ödüller:** görev 15 XP; hedef 40 XP ve 1–2 Sıcak Salep; meydan okuma 100 XP ve bir ünvan. Ünvanlar: Kozmik Kâşif, Kutup
Sakini, Felaket Ustası, Her Şeyi Atlatan. Modun başarımları kazanılınca sohbette duyurulur (`announceAdvancements` açıkken);
ünvan yalnız bu duyurularda oyuncu adının önünde görünür (vanilla duyurular, sohbet, tab listesi ve ölüm mesajlarında yoktur;
`basarim.unvanGoster` ile kapatılır). Eşya başarımları (#7, #22) eşya envantere girince, yaratıcı kipte de gelir; #15 ve #16
hayatta kalmada üretimle, yaratıcı kipte eşya envantere girince. XP miktarları `basarim.odul.*Xp` ile değişir.

| # | Başarım | Tür | Nasıl kazanılır |
|---|---|---|---|
| — | Felaket Başlıyor | görev (kök) | İlk afet sirenini duymak. |
| 1 | İlk Rüzgâr | görev | Bir kasırgadan sağ çık. |
| 2 | Dalga Avcısı | görev | Tsunami dalgası tarafından sürüklenip sağ çık. |
| 3 | Sallanan Zemin | görev | Bir depremden sağ çık. |
| 4 | Delikten Dönüş | görev | Sinkhole çökerken çukura düşüp sağ çık. |
| 5 | Yanmadan Çıktım | görev | Orman yangını alanında en az 30 saniye kalıp sağ çık. |
| 6 | Çatı Altı | görev | Bir asit yağmuru boyunca hiç hasar almadan hayatta kal. |
| 7 | Gökten Gelen Misafir | görev | İlk Meteorit Parçası'nı topla. |
| 8 | Güneşi Atlattım | hedef | Güneş patlamasının 5 günlük sürecinden sağ çık. |
| 9 | Süpernova Tanığı | hedef | Süpernovayı gökyüzünde gör. |
| 10 | Buzul Çağı | hedef | Donma ilerlemesi %50'yi geçsin ve hayatta ol. |
| 11 | Kutup Sığınağı | meydan okuma | Donma %100'e ulaştıktan sonra 3 Minecraft günü hayatta kal. |
| 12 | Ay Düştü | hedef | Ay çarpmasından sağ çık. |
| 13 | Yıldızlı Gece | görev | Ay çarpmasından sonra yıldızlı geceyi 10 saniye izle. |
| 14 | Yükselen Sular | görev | Deniz seviyesinin yükselmesini yaşa ve sağ kal. |
| 15 | Yer Dinleyici | görev | İlk Sismograf'ı yap. |
| 16 | Radar Altında | görev | İlk Hava Radarı'nı yap. |
| 17 | Katmanlı Giyim | görev | Bir zırh parçasına 5 tüy işle. |
| 18 | Yünlü Kahraman | hedef | 4 parça yünlü zırhı aynı anda giy. |
| 19 | Çorba İçer Isınır | görev | Donarken sıcak bir yiyecekle vücut sıcaklığını normale çıkar. |
| 20 | Buz Balıkçısı | görev | Buz deliğinden ilk balığı tut. |
| 21 | Kutup Ustası | hedef | Buz deliğinden toplam 25 balık tut. |
| 22 | Kozmik Şans | meydan okuma | İlk Kozmik Çekirdek'i bul. |
| 23 | Fırtınaya Meydan Oku | hedef | Seviye 3 ya da üstü bir afetten sağ çık. |
| 24 | Felaket Ustası | meydan okuma | Seviye 5 bir afetten sağ çık. |
| 25 | Hepsini Atlattım | meydan okuma, gizli | Sekiz afetin hepsinden en az bir kez sağ çık. |
| 26 | Birlikte Hayatta | hedef, gizli | En az 3 oyuncu aynı afeti aynı anda atlatsın. |

## Cold Sweat ile birlikte kullanmak

[Cold Sweat](https://modrinth.com/mod/cold-sweat) (2.4 ve üstü) isteğe bağlıdır. `sicaklik.sicaklikSistemiModu = OTOMATIK`
(varsayılan) iken Cold Sweat yüklüyse modun kendi vücut sıcaklığı ve HUD'u kapanır, oyuncu yalnız Cold Sweat'ten sıcaklık
hasarı alır. Afet dönemleri yine hissedilir:

- Dünya döneminin sıcaklık etkisi (güneş patlaması, süpernova, donma, kızıl cüce, kalıcı kış bölgesi) Cold Sweat'in dünya
  sıcaklığına `dogalafetler:donem` değiştiricisiyle eklenir.
- Sıcaklık yiyeceklerinin etkisi Cold Sweat'in çekirdek sıcaklığına aktarılır; azalan verim ve yiyecek efekti sürer.

Varsayılan kipte Cold Sweat yüklüyken modun kendi sıcaklık hasarı verilmediği ölçüldü (çok soğukta ve tam donmada can
değişmedi); oyun içi testlerin hepsi Cold Sweat yüklüyken de geçiyor. `ACIK` seçilirse iki sistem aynı anda çalışır ve
oyuncu ikisinden de hasar alabilir; önerilmez.

## Ses ve doku dosyaları

Bütün sesler ve dokular bu proje için üretildi; hepsi `tools/assets/` altındaki Python betikleriyle aynı bayt olarak yeniden
üretilebilir (Mojang dosyası kopyalanmadı, dışarıdan lisanslı dosya yok). Orman yangını ve asit yağmurunun ateş/yağmur sesleri
vanilla sesleridir.

**Değiştirmek için iki yol:**

1. **Kaynak paketiyle (kodsuz):** aynı adlı dosyayı bir kaynak paketinde `assets/dogalafetler/<klasör>/<ad>` yoluna koyun
   (ör. `assets/dogalafetler/sounds/siren_standart.ogg`). Sesler mono Ogg Vorbis olmalıdır; dokuların boyutu tablodakiyle aynı
   kalmalıdır (animasyonlu dokuların `.mcmeta`'sı kare sayısını belirtir).
2. **Betikle:** `tools/assets/<betik>.py`'deki parametreleri değiştirip proje kökünden çalıştırın; betik dosyayı
   `src/main/resources/assets/dogalafetler/` altına yazar, sonra jar'ı yeniden derleyin.

```
python -m venv .venv
.venv/bin/pip install -r tools/requirements.txt      # Windows: .venv\Scripts\pip
.venv/bin/python tools/assets/ses_uret.py            # örnek: siren ve kasırga sesleri
```

Ses betikleri `ffmpeg`'i (libvorbis) kullanır ve ölçüm raporlarını `docs/kanit/ses/` altına yazar. Bayt düzeyinde aynılık araç
sürümlerine bağlıdır (doğrulanan: ffmpeg n9.0.2, numpy 2.5.4, Pillow 13.0.0); başka sürümde dosya farklı ama eşdeğer çıkabilir.
GameTest şablonları (`data/dogalafetler/structures/*.nbt`) `tools/yapilar/` ve `tools/assets/*_sahne.py` ile üretilir; oyunda
kullanılmaz.

### Dosya listesi

Yollar `src/main/resources/` altındadır (jar'da `assets/dogalafetler/…`).

#### `assets/dogalafetler/sounds/`

Sesler (mono Ogg Vorbis, 48 kHz). Ölçü: entegre ses yüksekliği / gerçek tepe / süre; ölçüm raporu ve spektrogram `docs/kanit/ses/<ad>_ebur128.txt`, `<ad>_spektrogram.png`.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `ay_carpmasi_carpma.ogg` | -25.9 LUFS / -8.2 dBTP / 12.0 sn | 82,9 KB | `tools/assets/ay_carpmasi_ses.py` |
| `ay_carpmasi_citirti.ogg` | -30.0 LUFS / -11.4 dBTP / 6.0 sn | 47,6 KB | `tools/assets/ay_carpmasi_ses.py` |
| `ay_carpmasi_yaklasma.ogg` | -29.9 LUFS / -11.6 dBTP / 12.0 sn | 71,9 KB | `tools/assets/ay_carpmasi_ses.py` |
| `deprem_gurleme.ogg` | -26.0 LUFS / -7.6 dBTP / 10.0 sn | 65,5 KB | `tools/assets/deprem_ses.py` |
| `esya_oncu_sarsinti.ogg` | -26.9 LUFS / -6.7 dBTP / 2.4 sn | 21,5 KB | `tools/assets/esya_radar_ses.py` |
| `gunes_kavrulma_ruzgar.ogg` | -26.1 LUFS / -9.0 dBTP / 20.0 sn | 144,7 KB | `tools/assets/gunes_sesleri.py` |
| `gunes_kizil_cuce_akor.ogg` | -27.0 LUFS / -14.5 dBTP / 4.5 sn | 33,6 KB | `tools/assets/gunes_sesleri.py` |
| `gunes_supernova_basinc.ogg` | -25.0 LUFS / -9.3 dBTP / 8.0 sn | 38,3 KB | `tools/assets/gunes_sesleri.py` |
| `gunes_supernova_kristal.ogg` | -29.9 LUFS / -18.2 dBTP / 20.0 sn | 151,7 KB | `tools/assets/gunes_sesleri.py` |
| `karadelik_yutma.ogg` | -22.9 LUFS / -13.0 dBTP / 8.0 sn | 40,0 KB | `tools/assets/ses_uret.py` |
| `karadelik_yutma_yankili.ogg` | -23.0 LUFS / -10.0 dBTP / 8.0 sn | 45,1 KB | `tools/assets/ses_uret.py` |
| `kasirga_ruzgar.ogg` | -26.1 LUFS / -9.9 dBTP / 12.0 sn | 87,4 KB | `tools/assets/ses_uret.py` |
| `kasirga_ugultu.ogg` | -25.9 LUFS / -9.6 dBTP / 16.0 sn | 99,9 KB | `tools/assets/ses_uret.py` |
| `meteor_patlama.ogg` | -23.0 LUFS / -6.5 dBTP / 7.0 sn | 49,3 KB | `tools/assets/ses_uret.py` |
| `meteor_ucus.ogg` | -26.0 LUFS / -12.0 dBTP / 8.0 sn | 51,3 KB | `tools/assets/ses_uret.py` |
| `ortam_test.ogg` | -26.0 LUFS / -11.0 dBTP / 12.0 sn | 74,0 KB | `tools/assets/ses_uret.py` |
| `pulsar_vurus.ogg` | -26.0 LUFS / -13.7 dBTP / 8.0 sn | 79,7 KB | `tools/assets/ses_uret.py` |
| `pulsar_vurus_yankili.ogg` | -26.0 LUFS / -14.8 dBTP / 8.0 sn | 67,1 KB | `tools/assets/ses_uret.py` |
| `sinkhole_cokus.ogg` | -22.0 LUFS / -7.4 dBTP / 6.0 sn | 41,8 KB | `tools/assets/deprem_ses.py` |
| `siren_gunes.ogg` | -20.9 LUFS / -9.3 dBTP / 20.0 sn | 120,7 KB | `tools/assets/ses_uret.py` |
| `siren_standart.ogg` | -20.0 LUFS / -12.9 dBTP / 10.0 sn | 66,9 KB | `tools/assets/ses_uret.py` |
| `tsunami_carpma.ogg` | -22.9 LUFS / -7.6 dBTP / 3.0 sn | 30,6 KB | `tools/assets/tsunami_ses.py` |
| `tsunami_ugultu.ogg` | -26.0 LUFS / -11.5 dBTP / 12.0 sn | 76,8 KB | `tools/assets/tsunami_ses.py` |

#### `assets/dogalafetler/textures/item/`

Eşya simgeleri (16×16).

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `atesli_guvec.png` | 16x16 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `ayran.png` | 16x16 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `buz_alabaligi.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `buz_kefali.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `buz_parcasi.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `buzlu_karpuz.png` | 16x16 | 0,4 KB | `tools/assets/yiyecek_dokular.py` |
| `derin_soguk_somonu.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `donmus_ringa.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `gelismis_hava_radari.png` | 16x16 | 0,3 KB | `tools/assets/esya_radar_dokular.py` |
| `hassas_sismograf.png` | 16x16 | 0,3 KB | `tools/assets/esya_radar_dokular.py` |
| `hava_radari.png` | 16x16 | 0,3 KB | `tools/assets/esya_radar_dokular.py` |
| `isitici_cay.png` | 16x16 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `kozmik_cekirdek.png` | 16x16 | 0,4 KB | `tools/assets/meteor_dokular.py` |
| `kutup_baligi_corbasi.png` | 16x16 | 0,4 KB | `tools/assets/yiyecek_dokular.py` |
| `kutup_morinasi.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `meteorit_kulcesi.png` | 16x16 | 0,3 KB | `tools/assets/meteor_dokular.py` |
| `meteorit_parcasi.png` | 16x16 | 0,4 KB | `tools/assets/meteor_dokular.py` |
| `meyve_serbeti.png` | 16x16 | 0,4 KB | `tools/assets/yiyecek_dokular.py` |
| `pismis_buz_alabaligi.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `pismis_buz_kefali.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `pismis_derin_soguk_somonu.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `pismis_donmus_ringa.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `pismis_kutup_morinasi.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `sicak_cikolata.png` | 16x16 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `sicak_salep.png` | 16x16 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `sicak_tavuk_corbasi.png` | 16x16 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `sismograf.png` | 16x16 | 0,3 KB | `tools/assets/esya_radar_dokular.py` |
| `termostat_plakasi.png` | 16x16 | 0,4 KB | `tools/assets/meteor_dokular.py` |

#### `assets/dogalafetler/textures/block/`

Blok dokuları (16×16; `.mcmeta` animasyon tanımıdır).

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `buz_deligi_buzlanma_1.png` | 16x16 | 0,3 KB | `tools/assets/balikcilik_dokular.py` |
| `buz_deligi_buzlanma_2.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `buz_deligi_buzlanma_3.png` | 16x16 | 0,4 KB | `tools/assets/balikcilik_dokular.py` |
| `buz_deligi_kenar.png` | 16x16 | 0,8 KB | `tools/assets/balikcilik_dokular.py` |
| `ince_buz.png` | 16x16 | 0,8 KB | `tools/assets/donma_dokular.py` |
| `kiragili_cimen_ust.png` | 16x16 | 0,7 KB | `tools/assets/donma_dokular.py` |
| `komurlesmis_kutuk.png` | 16x16 | 0,7 KB | `tools/assets/pulsar_dokular.py` |
| `komurlesmis_kutuk_top.png` | 16x16 | 0,7 KB | `tools/assets/pulsar_dokular.py` |
| `kul.png` | 16x16 | 0,7 KB | `tools/assets/pulsar_dokular.py` |
| `su_kopuk.png` | 16x256 | 8,0 KB | `tools/assets/su_kopuk.py` |
| `su_kopuk.png.mcmeta` | — | 0,1 KB | `tools/assets/su_kopuk.py` |

#### `assets/dogalafetler/textures/particle/`

Parçacık dokuları (yangın, asit, meteor, pulsar külü ve koru). Parçacık tanımları (`particles/*.json`) `runData` ile `src/generated/` altına yazılır.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `asit_bulutu_0.png` | 64x64 | 2,6 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_bulutu_1.png` | 64x64 | 2,9 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_bulutu_2.png` | 64x64 | 2,8 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_bulutu_3.png` | 64x64 | 2,2 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_dumani_0.png` | 16x16 | 0,5 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_dumani_1.png` | 16x16 | 0,5 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_dumani_2.png` | 16x16 | 0,5 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_dumani_3.png` | 16x16 | 0,5 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_sicramasi_0.png` | 8x8 | 0,1 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_sicramasi_1.png` | 8x8 | 0,1 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_sicramasi_2.png` | 8x8 | 0,1 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `asit_sicramasi_3.png` | 8x8 | 0,2 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `kar_tanesi_0.png` | 16x16 | 0,3 KB | `tools/assets/donma_dokular.py` |
| `kar_tanesi_1.png` | 16x16 | 0,3 KB | `tools/assets/donma_dokular.py` |
| `kar_tanesi_2.png` | 16x16 | 0,5 KB | `tools/assets/donma_dokular.py` |
| `kar_tanesi_3.png` | 16x16 | 0,3 KB | `tools/assets/donma_dokular.py` |
| `kor_0.png` | 16x16 | 0,3 KB | `tools/assets/pulsar_dokular.py` |
| `kor_1.png` | 16x16 | 0,3 KB | `tools/assets/pulsar_dokular.py` |
| `kor_2.png` | 16x16 | 0,2 KB | `tools/assets/pulsar_dokular.py` |
| `kor_3.png` | 16x16 | 0,2 KB | `tools/assets/pulsar_dokular.py` |
| `kul_tozu_0.png` | 16x16 | 0,3 KB | `tools/assets/pulsar_dokular.py` |
| `kul_tozu_1.png` | 16x16 | 0,3 KB | `tools/assets/pulsar_dokular.py` |
| `kul_tozu_2.png` | 16x16 | 0,2 KB | `tools/assets/pulsar_dokular.py` |
| `kul_tozu_3.png` | 16x16 | 0,3 KB | `tools/assets/pulsar_dokular.py` |
| `kul_yagisi_0.png` | 16x16 | 0,2 KB | `tools/assets/pulsar_dokular.py` |
| `kul_yagisi_1.png` | 16x16 | 0,2 KB | `tools/assets/pulsar_dokular.py` |
| `kul_yagisi_2.png` | 16x16 | 0,3 KB | `tools/assets/pulsar_dokular.py` |
| `kul_yagisi_3.png` | 16x16 | 0,4 KB | `tools/assets/pulsar_dokular.py` |
| `meteor_duman_0.png` | 16x16 | 0,8 KB | `tools/assets/meteor_dokular.py` |
| `meteor_duman_1.png` | 16x16 | 0,8 KB | `tools/assets/meteor_dokular.py` |
| `meteor_duman_2.png` | 16x16 | 0,8 KB | `tools/assets/meteor_dokular.py` |
| `meteor_duman_3.png` | 16x16 | 0,8 KB | `tools/assets/meteor_dokular.py` |
| `meteor_isik_0.png` | 8x8 | 0,2 KB | `tools/assets/meteor_dokular.py` |
| `meteor_isik_1.png` | 8x8 | 0,2 KB | `tools/assets/meteor_dokular.py` |
| `meteor_isik_2.png` | 8x8 | 0,1 KB | `tools/assets/meteor_dokular.py` |
| `meteor_isik_3.png` | 8x8 | 0,1 KB | `tools/assets/meteor_dokular.py` |
| `meteor_yildiz_0.png` | 8x8 | 0,1 KB | `tools/assets/meteor_dokular.py` |
| `meteor_yildiz_1.png` | 8x8 | 0,1 KB | `tools/assets/meteor_dokular.py` |
| `yangin_alevi_0.png` | 16x16 | 0,5 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_alevi_1.png` | 16x16 | 0,5 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_alevi_2.png` | 16x16 | 0,4 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_alevi_3.png` | 16x16 | 0,4 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_dumani_0.png` | 32x32 | 1,5 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_dumani_1.png` | 32x32 | 1,5 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_dumani_2.png` | 32x32 | 1,4 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_dumani_3.png` | 32x32 | 1,3 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_kivilcimi_0.png` | 8x8 | 0,1 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_kivilcimi_1.png` | 8x8 | 0,1 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_kumesi_0.png` | 64x64 | 3,5 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_kumesi_1.png` | 64x64 | 3,9 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_kumesi_2.png` | 64x64 | 3,5 KB | `tools/assets/yangin_dokulari.py` |
| `yangin_kumesi_3.png` | 64x64 | 3,4 KB | `tools/assets/yangin_dokulari.py` |

#### `assets/dogalafetler/textures/environment/`

Asit yağmuru yağış sütunu.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `asit_yagmuru.png` | 64x256 | 5,3 KB | `tools/assets/asit_yagmuru_dokulari.py` |

#### `assets/dogalafetler/textures/environment/gokyuzu/`

Gökyüzü: güneş evreleri, süpernova kabuğu, nebula, kızıl cüce, pulsar ve kara delik yutması, yıldız ve galaksi katmanları.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `galaksi_kucuk_128.png` | 128x128 | 17,6 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_kucuk_128.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_kucuk_256.png` | 256x256 | 49,2 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_kucuk_256.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_kucuk_64.png` | 64x64 | 5,5 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_kucuk_64.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_sarmal_128.png` | 128x128 | 8,4 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_sarmal_128.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_sarmal_256.png` | 256x256 | 26,2 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_sarmal_256.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_sarmal_512.png` | 512x512 | 68,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `galaksi_sarmal_512.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `gunes.png` | 512x512 | 65,5 KB | `tools/assets/gokyuzu_dokulari.py` |
| `gunes.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `hale.png` | 256x256 | 7,5 KB | `tools/assets/gokyuzu_dokulari.py` |
| `hale.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `isi_dalgasi.png` | 256x64 | 6,2 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `isi_dalgasi.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `kalinti_1024.png` | 1024x1024 | 1027,4 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kalinti_1024.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kalinti_2048.png` | 2048x2048 | 2996,3 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kalinti_2048.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kalinti_512.png` | 512x512 | 321,3 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kalinti_512.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kara_delik.png` | 256x256 | 28,2 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kara_delik.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kara_delik_akis.png` | 256x256 | 10,5 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kara_delik_akis.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kara_delik_jet.png` | 64x512 | 3,9 KB | `tools/assets/gokyuzu_dokulari.py` |
| `kara_delik_jet.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `karadelik_3b_disk.png` | 1024x128 | 42,3 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_disk.png.mcmeta` | — | 0,1 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_disk_sicak.png` | 1024x128 | 31,9 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_disk_sicak.png.mcmeta` | — | 0,1 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_halka.png` | 16x64 | 0,2 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_halka.png.mcmeta` | — | 0,1 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_parilti.png` | 256x64 | 1,8 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_parilti.png.mcmeta` | — | 0,1 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_tup.png` | 256x64 | 8,3 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_tup.png.mcmeta` | — | 0,1 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_yildiz.png` | 32x32 | 0,5 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_3b_yildiz.png.mcmeta` | — | 0,1 KB | `tools/assets/karadelik_3b_dokulari.py` |
| `karadelik_buyuk_256.png` | 256x256 | 31,3 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_buyuk_256.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_buyuk_512.png` | 512x512 | 77,0 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_buyuk_512.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_disk_akis.png` | 512x512 | 95,5 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_disk_akis.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_foton.png` | 256x256 | 16,0 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_foton.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_mercek.png` | 512x512 | 109,8 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `karadelik_mercek.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `nebula_karisik_128.png` | 128x128 | 16,5 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_karisik_128.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_karisik_256.png` | 256x256 | 55,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_karisik_256.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_karisik_512.png` | 512x512 | 153,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_karisik_512.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_kirmizi_128.png` | 128x128 | 18,7 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_kirmizi_128.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_kirmizi_256.png` | 256x256 | 63,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_kirmizi_256.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_kirmizi_512.png` | 512x512 | 128,4 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_kirmizi_512.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_mavi_128.png` | 128x128 | 15,8 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_mavi_128.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_mavi_256.png` | 256x256 | 51,9 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_mavi_256.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_mavi_512.png` | 512x512 | 117,0 KB | `tools/assets/gokyuzu_dokulari.py` |
| `nebula_mavi_512.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `pulsar3b_alan.png` | 256x32 | 2,4 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_alan.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_hale_katman.png` | 256x256 | 19,5 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_hale_katman.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_koni.png` | 512x64 | 6,6 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_koni.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_parcacik.png` | 64x64 | 2,2 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_parcacik.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_yuzey.png` | 256x256 | 58,7 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar3b_yuzey.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_3b_dokulari.py` |
| `pulsar_cekirdek.png` | 256x256 | 20,0 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_cekirdek.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_hale.png` | 256x256 | 17,5 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_hale.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_isin.png` | 512x64 | 2,0 KB | `tools/assets/gokyuzu_dokulari.py` |
| `pulsar_isin.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `pulsar_isin_keskin.png` | 512x32 | 1,5 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_isin_keskin.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_korona.png` | 512x512 | 61,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `pulsar_korona.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `samanyolu.png` | 2048x256 | 422,8 KB | `tools/assets/gokyuzu_dokulari.py` |
| `samanyolu.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `spagetti.png` | 256x64 | 5,3 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `spagetti.png.mcmeta` | — | 0,1 KB | `tools/assets/pulsar_gok_dokulari.py` |
| `supernova_halka.png` | 512x512 | 99,6 KB | `tools/assets/gokyuzu_dokulari.py` |
| `supernova_halka.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `supernova_isin.png` | 512x512 | 35,9 KB | `tools/assets/gokyuzu_dokulari.py` |
| `supernova_isin.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |
| `yildiz_isin.png` | 64x64 | 0,7 KB | `tools/assets/gokyuzu_dokulari.py` |
| `yildiz_isin.png.mcmeta` | — | 0,1 KB | `tools/assets/gokyuzu_dokulari.py` |

#### `assets/dogalafetler/textures/environment/ay_carpmasi/`

Ay çarpması gök dokuları: yaklaşan ay, parlama, enkaz halkası.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `ay_catlak_1.png` | 256x256 | 23,3 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_catlak_1.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_catlak_2.png` | 256x256 | 56,2 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_catlak_2.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_catlak_3.png` | 256x256 | 82,2 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_catlak_3.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_hare.png` | 128x128 | 4,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_hare.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_kizil.png` | 256x256 | 5,4 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_kizil.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_parca.png` | 64x64 | 3,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_parca.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_yuzey.png` | 256x256 | 44,6 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `ay_yuzey.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `enkaz_halkasi.png` | 512x32 | 7,5 KB | `tools/assets/ay_carpmasi_dokular.py` |
| `enkaz_halkasi.png.mcmeta` | — | 0,1 KB | `tools/assets/ay_carpmasi_dokular.py` |

#### `assets/dogalafetler/textures/misc/`

Ekran örtüleri.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `isi_dalgasi.png` | 64x64 | 7,4 KB | `tools/assets/asit_yagmuru_dokulari.py` |
| `sicaklik_buz.png` | 256x256 | 132,1 KB | `tools/assets/sicaklik_dokulari.py` |
| `sicaklik_sicak.png` | 256x256 | 41,1 KB | `tools/assets/sicaklik_dokulari.py` |

#### `assets/dogalafetler/textures/entity/`

Varlık dokuları.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `meteor_izi.png` | 64x16 | 0,3 KB | `tools/assets/meteor_dokular.py` |
| `meteor_plazmasi.png` | 64x64 | 1,6 KB | `tools/assets/meteor_dokular.py` |

#### `assets/dogalafetler/textures/mob_effect/`

Durum efekti simgeleri.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `icten_isinma.png` | 18x18 | 0,3 KB | `tools/assets/yiyecek_dokular.py` |
| `serinlik.png` | 18x18 | 0,2 KB | `tools/assets/yiyecek_dokular.py` |

#### `assets/dogalafetler/textures/gui/sicaklik/`

Sıcaklık göstergesi (HUD).

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `bar.png` | 256x32 | 1,5 KB | `tools/assets/sicaklik_dokulari.py` |

#### `assets/dogalafetler/textures/gui/advancements/backgrounds/`

Başarım sekmesi arka planı.

| Dosya | Boyut / ölçü | Dosya boyutu | Üreten betik |
|---|---|---|---|
| `felaket.png` | 16x16 | 0,7 KB | `tools/assets/basarim_ikon.py` |

## Bilinen sınırlar

- **FPS ölçümü:** geliştirme ortamında oyun penceresi gizli çalışma alanında çizildiği için (pencere yöneticisi kare hızını
  kısıyor) ekranda görülen FPS ölçülemedi. Onun yerine kare başına işlemci süresi ölçüldü: asit yağmuru, orman yangını,
  kasırga, tsunami ve sanal suyun en yoğun (seviye 5) sahnesinde Yüksek kalitede sınırın altında kaldı; ay çarpması ve güneş
  patlamasının afetsiz karşılaştırma sahnesi olmadığından onların değerleri ön değerdir. Sunucu tarafı (TPS, tick süresi) ölçüldü.
  Kendi bilgisayarınızda yavaşlık görürseniz önce `gorsel.efektKalitesi`'ni `ORTA` ya da `DUSUK` yapın. Görünür pencerede
  Düşük/Yüksek/Ultra FPS karşılaştırması yapılmadı.
- **Çok oyunculu:** dedicated sunucuya (Cold Sweat'li dahil) tek gerçek istemciyle bağlanıldı; 2–10 oyunculu durumlar oyun içi
  testlerde sahte oyuncularla sınandı. Birden çok gerçek istemcinin aynı anda bağlı olduğu bir oturum denenmedi. Sirenin ayrı
  bir bilgisayardaki dedicated sunucudan gerçek bir istemciye çaldığı koşu yapılmadı (siren paketi oyun içi testlerde, sesi tek
  oyunculu dünyada doğrulandı).
- **Sonradan giren oyuncu:** afet ya da dönem sürerken sunucuya giren oyuncunun doğru gökyüzünü aldığı oyun içi testle
  doğrulandı; ekran görüntüsüyle denetlenmedi.
- **Gamerule tab tamamlaması:** kurallar okunup yazıldı ve yeniden açılışta korundu; `/gamerule` öneri listesi ekranda denetlenmedi.
- **Üretim sunucusu ve e4mc:** Forge installer'ın **Install server** ile kurduğu temiz bir sunucuda ve e4mc üzerinden internetten
  gerçek bir arkadaş oturumunda geliştirme sırasında denenmedi (ikisi de internet bağlantısı ister). Mod Forge geliştirme
  sunucusunda ve dedicated sunucu koşularında sınandı; "Kurulum" ve "Arkadaşlarla oynamak" adımlarını Windows'ta ilk kez
  denerken sorun görürseniz `logs/latest.log`'u saklayın.
- **Kasırga tabanının rengi:** seviye 5 kasırganın tabanındaki toz katmanının sıcak kahverengi göründüğü ekran görüntülerinde
  gözle denetlendi; otomatik renk ölçüsü zemin rengine (çimen, kum, su kenarı) bağlı kaldığı için kabul ölçüsü sayılmadı.
- **Cold Sweat:** varsayılan `OTOMATIK` kipte Cold Sweat yüklüyken çift hasar olmadığı ölçüldü (çok soğukta ve tam
  donmada can değişmedi); `sicaklikSistemiModu = ACIK` iken iki sistemin birlikte verdiği hasar ölçülmedi.
- **Gamerule ve elle başlatılan güneş patlaması:** `/dogalafet start` ile başlatılan güneş patlaması gamerule'den etkilenmez;
  ama ardından gelen dönem süreci `gunesPatlamasiAfeti false` iken durur.
- **Güneş zinciri:** `gunesPatlamasiAfeti` süpernovanın başladığı anda kapatılırsa güneş zinciri durmadan önce en çok 1 saniye
  ilerleyebilir.
- **Güneş patlaması görselleri:** çarpma anında ayrı beyaz ekran örtüsü yok, süpernova çökerken öteki ortam sesleri susmaz,
  kavrulmada ayrı uzak yangın sesi yok; kabak/karpuz sapları kavrulma rengine geçmez. Aynı bloklara renk veren başka bir mod
  varsa hangisinin rengi görüneceğini mod yükleme sırası belirler.
- **Orman yangını:** duman güneşi turuncuya boyamaz.
- **Deprem ve sinkhole:** çöken zemin tek bir büyük varlık olarak değil düşen bloklar olarak çizilir; devrilen ağaç, ayrı toz
  dokusu, çukur dibinde nem buharı ve ayrı öncü gürleme/çatlama sesleri yok. Yarık ve çukur yalnız yüklü chunk'larda açılır
  (seviye 5 yarığının yüklü alanın dışındaki ucu açılmaz).
- **Tsunami:** ekran kenarında su perdesi ve suda yüzen görsel enkaz yok. Tavanı delik mağaralarda ve vadilerde mağara suyu az
  dolar.
- **Donma:** yaprak çıkıntısının altındaki su donmaz; oyuncunun koyduğu kar ve buz kalınlaştırılmaz. Buharlaşmada kuruyan
  suyun içindeki yosun/deniz çimi ilk komşu güncellemesine kadar havada kalabilir. Uçurum kenarında kış bölgesi sınırı keskin
  olabilir. Eski dünyalardaki ve yaratıcı modda konan Kırağılı Çimen, sütunu bir sonraki donma/çözülme eşiğinde işlenince
  çimene döner (varsayılan tablo artık çimeni kırağılı çimene çevirmez).
- **`/dogalafet sifirla`:** kalıcı kış bölgelerini o anki config ile yeniden hesaplar; config dünya içinde değiştiyse eski kış
  bölgesi kayabilir.
- **Başka gökyüzü modları:** gökyüzünü tamamen değiştiren bir mod varsa yaklaşan ay ve güneş evreleri vanilla ay/güneşin üstüne
  çizilebilir (log'da uyarı).
- **Başarımlar:** sıcaklık sistemi kapalıyken "Çorba İçer Isınır" alınamaz. Bir veri paketi başarım XP'sini değiştirirse config
  farkı kod varsayılanına göre eklenir.
- **Balıkçılık:** bir buz deliğinin yanında duran ikinci oyuncu deliğin ısısından küçük bir pay alır.

## Belgeler

- [`docs/SARTNAME.md`](docs/SARTNAME.md): şartname; [`docs/SARTNAME-IZLEME.md`](docs/SARTNAME-IZLEME.md): her maddenin uygulayan
  kodu, kanıtı ve durumu.
- [`docs/KARARLAR.md`](docs/KARARLAR.md): belirsizlikte seçilen yollar, şef kararları, N17 sayısal varsayılanlar tablosu.
- [`docs/TEST-RAPORU.md`](docs/TEST-RAPORU.md): doğrulama matrisi ve kanıtlar; [`docs/tasarim/`](docs/tasarim/): tasarım notları.

## Lisans

[MIT](LICENSE) © 2026 Kadir Berk Polat. Üretilen sesler ve dokular projenin parçasıdır ve aynı lisansla dağıtılır.

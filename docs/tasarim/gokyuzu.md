# Ortak Gökyüzü — tasarım notu (madde 16, bölüm D ve H; N11)

Tek renderer, tek sunucu durumu: `ayVar` (bool) × `gunesEvresi` (`NORMAL`, `BUYUYEN`, `SUPERNOVA`, `GUNESSIZ`, `KIZIL_CUCE`) + evre başlangıç tick'i ve ilerleme. Durum SavedData'da, değişimde ve girişte pakette; geçişler istemcide aynı tick'ten deterministik hesaplanır, böylece herkes aynı anı görür.

## 1. Gerçek olay (görünür gökyüzü)

- **Yıldız renkleri:** çıplak gözle yıldızların çoğu beyaz görünür; parlakların bir kısmı belirgin mavi-beyaz (O/B/A türü) ya da turuncu-kırmızı (K/M türü). Parlaklık dağılımı üsteldir: sönükler çok, parlaklar az. Ufka yakın yıldızlar atmosfer yüzünden sönük ve titrektir.
- **Samanyolu:** karanlık gökyüzünde gökyüzünü boydan boya kesen, ortasında koyu toz şeritleri (Great Rift) olan, merkezine doğru (Yay takımyıldızı) kalınlaşan süt beyazı bant.
- **Galaksiler:** çıplak gözle Andromeda soluk, uzun bir oval leke; fotoğrafta parlak çekirdek, mavimsi sarmal kollar.
- **Emisyon nebulaları ve süpernova kalıntısı:** hidrojen-alfa kırmızı/pembe, oksijen-III mavi-yeşil; Yengeç Nebulası'nda dışta kırmızı-turuncu iplikler, içte mavi-beyaz yaygın ışıma.
- **Süpernova:** gündüz görülebilen nokta ışık (SN 1006, ~−7,5 kadir).
- **Kızıl cüce:** ~3000 K, turuncu-kırmızı disk; gökyüzü daha sıcak ve loş tonlu.

Kaynaklar:
- https://science.nasa.gov/asset/hubble/peering-into-the-heart-of-the-crab-nebula/
- https://noirlab.edu/public/news/noao0305
- https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.1/src/main/java/net/minecraftforge/client/extensions/IForgeDimensionSpecialEffects.java (`renderSky`, `renderClouds`, `renderSnowAndRain`, `adjustLightmapColors` kancaları)
- https://lexxie.dev/forge/1.20.1/net/minecraftforge/client/event/RegisterDimensionSpecialEffectsEvent.html

## 2. Minecraft'ta gösterim

**Kanca.** `RegisterDimensionSpecialEffectsEvent` ile `minecraft:overworld` efekt kimliği için o ana kadar kayıtlı efekti saran bir sarmalayıcı kaydedilir; her çağrıyı iç nesneye delege eder. Kendi çizimini yalnız Overworld boyutunda ve durum vanilla değilken yapar (`renderSky` → `true`); normal durumda `false` döner, vanilla ya da başka bir modun gökyüzü aynen kalır. Sis rengi `ViewportEvent.ComputeFogColor`, gündüz gökyüzü rengi kendi kubbe çizimimizde. Işık motoru, `getSkyDarken`, gün/gece döngüsü ve spawn kurallarına dokunulmaz.

### Spike sonrası güncelleme, 2026-11-06

Kaynak: `.vibe/kararlar/spike-gokyuzu.md` G1…G6, `docs/KARARLAR.md` K0-23, K0-26.

**Kanca kararı: saran `DimensionSpecialEffects`, üç kademe (G1).**
1. *Sar:* kayıt olayında olayın `effects` haritası yansımayla okunur, o anki overworld efekti sarılır. Okuma başarısızsa kayıt yapılmaz (bizden önceki modun efekti silinmesin), iş 2. kademeye kalır.
2. *Kazananı sar:* `FMLLoadCompleteEvent` → `enqueueWork` içinde `DimensionSpecialEffectsManager.getForType` bizim sarmalayıcımız değilse (bizden sonra kayıt yapan mod bizi ezdi) Forge'un `EFFECTS` haritası yansımayla yeniden kurulup kazanan sarılır ve geri okunarak doğrulanır. Alan Forge sınıfında olduğu için üretimde de adı `EFFECTS`'tir; `ClientLevel` efekti dünyaya girişte aldığı için yükleme sonu yeterince erkendir.
3. *Son çare:* yansıma da başarısızsa `RenderLevelStageEvent.AFTER_SKY` yedeği + WARN "BİLİNEN SINIR: ay/güneş gizlenemez" (kazanan mod kendi ayını/güneşini çizer; aysız gece ve güneşsiz dönem bozulur). `AFTER_SKY` birincil yol olarak reddedildi: o anda güneş, ay ve yıldızlar çizilmiştir; ay örtüsü ufuk gradyanını tutturamaz ve arkasındaki yıldızları siler.

- Ezilme senaryosu sahte ikinci efektle koşuldu: log `ezildi` → `EFFECTS yeniden kuruldu: ... sarıldı`, normal durumda kazananın kendi çizimi, aysız + güneşsiz durumda ay/güneş yok; yansıma bilerek bozulunca 3. kademe ve WARN. Bu senaryo Faz 2 kabulüne girer.
- **Normal güneş + ay yok** iken tam gündüzde de iç nesneye devredilir; ay çarpmasından sonra başka modun/şader paketinin gündüz göğü korunur, biz yalnız aysız geceyi çizeriz.
- Sarmalayıcı ve sis olayı yalnız `Level.OVERWORLD`'de etkin; overworld efekt kimliğini kullanan mod boyutlarının göğü değişmez (N11).
- Forge 1.20.1'de `ISkyRenderHandler` yok; kanca `IForgeDimensionSpecialEffects.renderSky/renderClouds/renderSnowAndRain/adjustLightmapColors`. Bilinen bir gökyüzü moduyla uyum için `mods.toml`'a `mandatory=false, ordering="AFTER"` bağımlılığı.

**Kabul edilen sapmalar (G2).**
- *Büyüyen güneş:* disk **normal alfa karışımı + %30 toplamalı** çizilir (tamamen toplamalı turuncu parlak gökte beyaza doyuyordu: piksellerin %39'u > 0,85). Ölçek `1 + 2,2·ilerleme` (vanilla diskten 3,2×'e), renk beyaz → `#FF9A3C`, hale `disk·(3 + 3·p)` toplamalı.
- *Kızıl cüce:* gündüz uzay katmanı **%45** (tasarımdaki %25 parlak kızıl gökte görünmüyordu), gece %100; disk eski güneşin %60'ı (vanilla `sun.png`'nin parlak diski 32 pikselin 8'i), normal karışım + kızıl hale; doğuşta gün doğumu yelpazesi `1,1 + 0,5·(1 − p)` kat (daha güçlüsü bütün ufku doğmuş kızıla boyayıp diski kaybettiriyordu).
- *Süpernova:* gök aydınlatması orta tona çekildi (ilk denemede tepe anı görüntüsünün %67'si beyazdı; ışın ve halka okunsun diye).
- Spike'ta yapılmayanlar: ufka yakın yıldız alfasının yarıya inmesi (şader ister; Faz 2 ya yıldız VBO'sunu yükseklik bandına böler ya da vazgeçer), aurora, bulutların kızıl tonlanması (`getCloudColor` mixin ister; vazgeçildi, bulut vanilla rengi kalır), çökme/parlama koreografisi.

**Durum verisi: evre başlangıç tick'i gerekir (G3, G6).** Süpernovanın 40 sn'lik çökme → parlama → genişleme koreografisi ve 20 sn'lik renk geçişleri ilerleme kesriyle değil gerçek saniyeyle tanımlandığı için pakette **evre başlangıç tick'i** taşınır; istemci zamanı bu tick'ten deterministik hesaplar (sonradan giren oyuncu da aynı kareyi görür). Ay için bool yetmez: `AyDurumu {VAR, YAKLASIYOR, YOK}` + ay olay tick'i (yaklaşma, parçalanma, enkaz halkasının sönümü; `ay_carpmasi.md`). Durum ayrı bir gökyüzü SavedData'sında değil, çekirdeğin tek dünya dönemi SavedData'sında; paket yalnız değişimde ve girişte, her boyuttaki oyuncuya. Ay/güneş bağımsızlığı (madde 16) korunur.

**Çizim sırası (kendi `renderSky`'imiz).**
1. Gökyüzü kubbesi: vanilla'daki üst disk + ufuk gün doğumu/batımı yelpazesi, renkleri evre tablosundan.
2. Derin uzay katmanı (görünürlük `uzayAlfa`): Samanyolu bandı → galaksiler → nebulalar → yıldızlar (additive, derinlik yazımı kapalı), gökyüzü dönüşüyle (`getTimeOfDay`) birlikte döner.
3. Güneş / süpernova / kızıl cüce (evreye göre).
4. Ay (yalnız `ayVar`), ay çarpması sırasında yaklaşan ay ve parçaları, sonra enkaz halkası.
5. Ufuk efektleri: ay çarpmasının havadaki 3B patlaması (7 sn; gökyüzü-2'de mantar sütununun yerine), kızıl cüce doğuş ışığı, aurora.
6. Vanilla'nın ufuk altı koyu diski.

**Yıldızlar.** Açılışta tek seferlik `VertexBuffer` (vanilla 1500 yıldız yerine Yüksek'te 6000). Yön: birim kürede düzgün rastgele, %35'i Samanyolu düzlemine ±12° içinde yoğunlaştırılmış. Parlaklık: `m = -ln(U)` ile üstel dağılım, boyut 0,08–0,45 (en parlak %1'i 4 kollu küçük ışın dokusu). Renk: %70 beyaz `#F4F6FF`, %12 mavi-beyaz `#AFC8FF`, %10 sarı `#FFE7B0`, %8 turuncu-kırmızı `#FFB08A`. Titreme: yıldızlar 4 gruba bölünür, her grubun alfası farklı fazlı 1,1–2,3 Hz sinüsle ±%15 değişir (VBO yeniden yazılmaz, grup başına tek uniform renk); ufka yakın 15°'de alfa yarıya iner.

**Samanyolu.** Gökyüzü küresinde 30° eğik bir büyük çember boyunca 64 dilimli şerit, 24° geniş; doku 2048×256 (`tools/assets/` Pillow betiği: çok ölçekli gürültü + ortada koyu toz damarları + merkez şişkinliği, sarımsı merkez → mavimsi beyaz uçlar). Alfa 0,55.

**Galaksiler.** 2 sprite: eğik sarmal (Andromeda benzeri, 256², 9° açısal boyut) ve küçük karşıdan sarmal (128², 4°); prosedürel: logaritmik sarmal kollar + sarı çekirdek + mavi kol düğümleri.

**Nebulalar.** Gece nebula seti (aysız normal gecede): 3 emisyon nebulası (kırmızı-pembe Orion benzeri 6°, mavi-yeşil 4°, karışık 5°). Süpernova kalıntısı (yalnız güneş yokken): eski güneşin yörünge yerinde, 40° açısal boyut, 1024² doku; dışta kırmızı-turuncu iplik ağı (yönlü gürültüden ince çizgiler), içte mavi-beyaz yaygın ışıma, ortada soluk mavi pulsar noktası (0,5 Hz hafif yanıp sönme). Kalıntı çok yavaş genişler (günde %1, tavan %130) — zaman geçtiği hissedilir.

**Güneş evreleri.** `NORMAL`: vanilla güneş dokusu, ölçek 1. `BUYUYEN`: kendi güneş dokumuz (yumuşak kenarlı disk + 3 halkalı korona + yavaş dönen leke dokusu), ölçek ve renk `gunes_patlamasi.md`'deki eğrilerle; disk normal karışım + %30 toplamalı, etrafında toplamalı hale, büyüdükçe hale gökyüzünün üst kubbesine kadar taşar. `SUPERNOVA`: çökme, parlama (12 ışın), genişleyen kabuk, üstel sönüm. `GUNESSIZ`: güneş yok. `KIZIL_CUCE`: %60 ölçek, `#FF6A3A` disk (normal karışım), kızıl hale; doğuş gününde ufukta 60 sn ışık yayı.

**Evre × ay matrisi.**

| Güneş \ Ay | Ay var | Ay yok |
|---|---|---|
| NORMAL | vanilla (renderer devre dışı) | gündüz iç nesneye devredilir (vanilla ya da başka modun göğü); gece ay yok, `uzayAlfa` 1 (6000 yıldız + Samanyolu + galaksiler + gece nebulaları), gökyüzü daha koyu `#05070F` |
| BUYUYEN | büyüyen güneş + vanilla ay; gece aurora | büyüyen güneş; gece yıldızlı + aurora |
| SUPERNOVA | süpernova + ay (ay süpernova ışığında mavi-beyaz tonlanır) | süpernova + yıldızlar |
| GUNESSIZ | uzay gündüz-gece açık, ay hilal yerine soluk gri disk (güneş ışığı yok: yalnız kalıntı nebulasının ışığıyla `#5A6070`), evre döngüsü durur | yalnız uzay; en karanlık ve en zengin gökyüzü |
| KIZIL_CUCE | kızıl cüce + turuncu tonlu ay; gündüz uzay %45 | kızıl cüce; gündüz uzay %45, gece %100 |

**Renk tablosu (gündüz üst kubbe / sis).** NORMAL vanilla; BUYUYEN sonu `#E8D9A8` / `#F0D8A0`; SUPERNOVA parlaması `#DDEBFF` / `#C8DAF5`, sönüm sonrası `#0A0E1C` / `#1A2030`; GUNESSIZ `#03050C` / `#0D1220` (sis biraz açık, ufuk okunur kalsın); KIZIL_CUCE `#B07A68` / `#C08870`, gece `#120A10`. Geçişler 20 sn lineer karışım.

**Bulutlar.** NORMAL/BUYUYEN vanilla; GUNESSIZ'de `renderClouds` `true` döner ve bulutlar çizilmez (karanlık gökyüzünde parlak beyaz bulut tutarsız olurdu); KIZIL_CUCE'de vanilla bulutlar, sis rengiyle tonlanır.

## 3. Vitrin anı

**Güneşsiz dönem, donmuş göl üzerinde (10 sn).** Kamera gölün kıyısında, Y+2, pitch +30°; kadrajın alt dörtte biri karla kaplı ağaç silüetleri ve buz. Görüntüde: gökyüzünü çapraz kesen toz damarlı Samanyolu, sol üstte kırmızı iplikli dev kalıntı nebulası ve ortasında yanıp sönen mavi pulsar, sağda eğik sarmal galaksi, titreyen renkli yıldızlar. Bu kare aynı zamanda "Yıldızlı Gece" başarımının görselidir.

## 4. Ses tasarımı

Gökyüzü kendi başına ses çıkarmaz; evre geçiş sesleri ilgili afet notlarındadır (süpernova basıncı, kızıl cüce akoru, ay çarpması gümü). GUNESSIZ döneminde vanilla kuş/ortam sesi yoktur, `donma.md`'deki rüzgâr katmanı kullanılır.

## 5. Performans planı

| Kalite | Yıldız | Samanyolu dilim | Galaksi/nebula doku boyu | Kalıntı dokusu | Titreme |
|---|---|---|---|---|---|
| Düşük | 2000 | 24 | 128 | 512 | yok |
| Orta | 4000 | 48 | 256 | 512 | 2 grup |
| Yüksek | 6000 | 64 | 256 | 1024 | 4 grup |
| Ultra | 10000 | 96 | 512 | 2048 | 4 grup |

- Her şey açılışta (ya da kalite değişince) bir kez derlenen VBO'lardan; karede ~15 çizim çağrısı (spike ölçümü), CPU maliyeti sabit. Dokular oyun içinde değil, `tools/assets/` betikleriyle üretilmiş PNG.
- Sunucu maliyeti sıfır; paket yalnız evre değişiminde ve oyuncu girişinde.

## 6. Açık noktalar

### Spike sonrası güncelleme, 2026-11-06

- **Kapandı — uyumluluk:** üç kademeli kanca (§2; K0-26). Bizden önce yüklenen gökyüzü modu 1. kademede, sonra yüklenen 2. kademede sarılır; normal durumda o modun göğü çalışır. Gerçek üçüncü taraf modla denenmedi (sahte ikinci efektle koşuldu). Kapsam dışı: efekt nesnesini `ClientLevel` oluştuktan sonra değiştiren ya da `LevelRenderer.renderSky`'yi mixin ile tamamen değiştiren modlar; vanilla `renderSky` içine enjekte eden modların çizimi bizim dönemlerimizde çalışmaz.
- **Açık — Iris/Oculus** [doğrulanmadı, kurulmadı]: çizimimiz `CUSTOM_SKY` fazında kalır, yalnız vanilla çekirdek şaderleri (`position`, `position_color`, `position_tex_color`) kullanılır; paketin composite'te çizdiği gök bizimkini örtebilir. Faz 2/5'te Oculus + bir paketle görsel deneme.
- **Gündüz karanlığı (G5):** ışık motoru değişmez; karanlık his kubbe rengi + sis/temizleme rengi + sarmalayıcının `adjustLightmapColors`'ıyla verilir: ışık haritası dokusunda yalnız gök ışığının renk katkısı ölçeklenir (GUNESSIZ ×0,35 mavimsi, KIZIL_CUCE ×0,75 sıcak, SUPERNOVA parlaklığa göre 0,4–1,1); sarmalayıcı önce iç nesnenin `adjustLightmapColors`'ını çağırır. Işık seviyeleri, mob spawn ve vanilla mantığı bu dokuyu okumaz. Sonuç: güneşsiz dönemde oyun mantığı gündüzdür (ölümsüz moblar gündüz yanar, yatak gündüz uyutmaz); bu, madde 16'nın isteğidir ve Berk'e açıkça söylenmeli. KARARLAR'a işleme entegratörün işi.
- **Performans (G4):** gökyüzü bölümünün GPU süresi (en küçük değer) vanilla ~0,16 ms, nebula gecesi ~0,23–0,30, süpernova ~0,36–0,41, kızıl cüce gündüz ~0,36–0,38 ms; FPS hedefi tutuyor. Kalite farkı süre değil doku belleğidir (kalıntı 2048² = 16 MiB, 1024² = 4 MiB). Düşük'te süpernova hale/ışın kareleri küçültülür (maliyet büyük toplamalı karelerin doldurma hızı).
- Nether/End'deki oyuncu durumu alır ama oradaki gökyüzü değişmez (N11).

## Faz 2 güncellemesi, 2026-11-06

Kaynak: `.vibe/kararlar/gokyuzu.md` G7…G18, rapor `.vibe/rapor/gokyuzu.md`. Kareler 1870×969, kalite Yüksek, gözlemci Y=150,
doğuya bakış; koşu 4 (`KOK/.vibe/kanit/gokyuzu/kareler-kosu4/`, önizleme `onizleme-kosu4.png`).

**Uygulananlar (tasarımdan sapmalar dahil).**
- Tek durum: çekirdeğin `DunyaDonemi` + `DonemPaketi` (ay, evre, ilerleme, evre başlangıcı); ayrı gökyüzü verisi ve paketi yok.
  Geçişler istemcide ölçekli saatten (20 sn renk geçişi; süpernova koreografisi gerçek saniyeyle), ilerleme monoton.
- Süpernova koreografisi: 0–8 sn şişmiş güneş (3,2×) turuncudan mavi-beyaza dönerek noktaya çöker → 8 sn'de parlama
  (0,4 sn tepe, ~6 sn sönüm) → 12 ışın ve ~10 sn'de açılan kabuk → 1,5 günlük üstel sönüm, kalıntı nebulası belirir.
- Kızıl cüce: donma %100 + 7 gün sonra ilk şafakta (uyku şafağı atlasa da) doğar; doğuş 6000 tick, önce ufukta kızıl
  ışık, disk ilk %20'de belirir; boy ve gök/sis tonu SERVER config (%60, ton gücü 1).
- Yapılmayan: ufka yakın yıldız sönükleşmesi, BÜYÜYEN gecesinde aurora, bulutların kızıl tonlanması, ay YAKLASIYOR durumu
  (çekirdek veri modeli; Faz 4 ay çarpmasına öneri).

**Kareler (ay × güneş, 10 kombinasyonun hepsi).**

| Kare | Durum | Ölçüm (gök bölgesi) |
|---|---|---|
| `gokyuzu-01-normal-gunduz.png` | NORMAL × ay var, gündüz | vanilla (sarmalayıcı devreder) |
| `gokyuzu-12-normal-gunduz-aysiz.png` | NORMAL × ay yok, gündüz | vanilla (iç nesneye devredildi) |
| `gokyuzu-10-normal-gece-aysiz.png` | NORMAL × ay yok, gece | ay yok, ~4600 yıldız + Samanyolu |
| `gokyuzu-11-normal-gece-ayli.png` | NORMAL × ay var, gece | vanilla, ~1140 yıldız |
| `gokyuzu-02a-buyuyen-baslangic.png`, `02b-buyuyen-son.png` | BÜYÜYEN × ay var | p=0,85 disk (255,190,99), hale ~120 px, gök sarımsı (203,204,193) |
| `gokyuzu-02c-buyuyen-gece-aysiz.png` | BÜYÜYEN × ay yok, gece | yıldızlı |
| `gokyuzu-03a-supernova-cokme.png` | SUPERNOVA, 4 sn | çöken turuncu-beyaz çekirdek (255,214,159) |
| `gokyuzu-03b-supernova-parlama.png` | SUPERNOVA, 11 sn | parlama, gökün üst bölümünün %4,5'i doymuş |
| `gokyuzu-03c-supernova-vitrin.png` (vitrin), `03d-supernova-ogle-tepe.png` | SUPERNOVA × ay var | gök (70,77,94), ışınlar 1100×570 px, gündüz ~5100 yıldız |
| `gokyuzu-03e-supernova-gece-aysiz.png` | SUPERNOVA × ay yok, gece | yıldızlı, sönümde |
| `gokyuzu-04a-gunessiz-ogle-tepe.png`, `04b-gunessiz-sabah-ufuk.png` | GÜNEŞSİZ × ay var, gündüz | gök ≈ #03050C, ~6200 yıldız, kalıntı nebulası |
| `gokyuzu-05-gunessiz-gece-ayli.png` | GÜNEŞSİZ × ay var, gece | soluk gri ay diski |
| `gokyuzu-06a-gunessiz-gece-aysiz-vitrin.png`, `06b-...-ufuk.png` | GÜNEŞSİZ × ay yok | ay yok, ~4600 yıldız (vitrin: "Yıldızlı Gece") |
| `gokyuzu-07a-kizilcuce-dogus-ilk-isik.png`, `07b-kizilcuce-yukselis.png` | KIZIL CÜCE doğuşu × ay yok | ufukta kızıl yay, gök (82,61,50) → (136,93,80) |
| `gokyuzu-08a-kizilcuce-sabah-aysiz.png`, `08b-...-ogle-tepe-aysiz.png` | KIZIL CÜCE × ay yok, gündüz | gök (177,123,105) ≈ #B07A68, disk (255,136,76), soluk yıldızlar |
| `gokyuzu-09a-kizilcuce-gece-aysiz.png` | KIZIL CÜCE × ay yok, gece | koyu kızıl gece |
| `gokyuzu-09b-kizilcuce-gece-ayli.png`, `09c-...-sabah-ayli.png` | KIZIL CÜCE × ay var | turuncu tonlu ay |

Ezilme denemesi (K0-26): `kareler-ezilme/` (ikinci kademe: normal durumda sahte modun göğü, aysız ve güneşsiz durumda
bizim gök, sahte ay/güneş %0), `kareler-ezilme-yansimasiz/` (üçüncü kademe: sahte ay ve güneş görünür, BILİNEN SINIR).

## Gökyüzü-2 güncellemesi, 2026-11-07 (canlı uzay, kara delikler, pulsar, gün saati)

Kaynak: `.vibe/kararlar/gokyuzu-2.md` G2-1, G2-5…G2-8.

**Gerçek olay.** EHT'nin M87* (2019) ve Sgr A* (2022) görüntüleri: karanlık gölge, ince foton halkası, turuncu-sarı
akresyon halkası; relativistik Doppler yüzünden halkanın bir yanı (M87'de güney) belirgin parlak. M87 jeti kutuptan çıkan ince,
düğümlü mavi-beyaz ışın. Pulsar (Yengeç pulsarı): süpernova kalıntısının ortasında dönen nötron yıldızı; manyetik kutup
ışınları bakıştan geçtikçe nabız.
- https://www.eso.org/public/news/eso1907/
- https://science.nasa.gov/asset/hubble/crab-nebula-pulsar/

**Canlı uzay.** Derin uzay gün dönüşüne ek olarak eğik eksende 600 sn'de bir tur döner (oyun zamanından; gün döngüsü
kapalıyken de). Kırpışma grubu Düşük 1 (sabit) / Orta 3 / Yüksek 6 / Ultra 8; iki frekanslı ±%32 parlaklık ve ±%7 kırmızı-mavi
kayma. Yıldız renkleri yedi tayf sınıfı (O/B `#9BB0FF` %6, A `#CAD7FF` %12, F `#F8F7FF` %22, G `#FFF4EA` %25, K `#FFD2A1` %20,
M `#FFB56C` %12, kızıl dev `#FF8A5C` %3). Galaksiler saatte bir tur, sırayla zıt yönde. Kalıntı nebulası güneş yolunda kalır.

**Kara delikler** (yalnız zengin uzayda, `nebulaKatmani` açıkken).

| Kademe | Sayı | Jet | Akış dokusu |
|---|---|---|---|
| Düşük | 1 | yok | yok |
| Orta | 2 | var | var |
| Yüksek | 3 | var | var |
| Ultra | 4 | var | var |

Açısal çap 1,0–1,6°; gölge 0,34, foton halkası 0,395, halka 0,47 (doku yarıçapı oranı), Doppler alt yan %100 / üst %30; gölge
yıldızları örter (normal karışım), akış 12–22°/sn döner, jet 6,5 yarıçap boyunda, üç düğümlü, %50 alfa ve hafif titreşim.
Maliyet kara delik başına 3 çizim çağrısı.

**Pulsar** (`GunesEvresi.PULSAR`, kızıl cücenin yerine %15). Güneş yolunda, kalıntının ortasında. Görünüm ve dünya etkileri
pulsar-2'de yenilendi (aşağıdaki bölüm); dönen ışın ve nabız (G2-7) kalktı.

| Evre \ Ay | Ay var | Ay yok |
|---|---|---|
| PULSAR | pulsar + mavimsi ay; gündüz uzay %55 | pulsar + kara delikli zengin uzay |

**Gün saati (G2-1, G2-9).** Güneş patlaması doğal yolla yalnız sabah patlar (AKTIF anı gün saati 0–6000; ON_UYARI
gerekirse ertesi sabaha kadar sessizce bekler) ve AKTIF (5 gün) boyunca gün saati o sabah saatinde donar: büyüyen güneş
doğu göğünde kalır, gece olmaz. Komutla pencere dışında başlatılan patlama gelecek öğleye alınıp orada donar. Siren
gecesi (aurora) ve süpernova sonrası dönemler vanilla döngüyle sürer. `doDaylightCycle`'a dokunulmaz.

## Pulsar-2 güncellemesi, 2026-11-08 (korkunç pulsar, cehennem dünya, kara delik yutması, eski güneşin dönüşü)

Kaynak: `.vibe/kararlar/pulsar-2.md` (PL-1…). Kullanıcı isteği: pulsar "yetersiz"; daha büyük, korkutucu, dönmeyen ama sağa sola
çok hızlı titreşen bir nötron yıldızı; dünya cehenneme döner; 15 gün sonra uzak bir kara delik pulsarı yutar; 10 gün buzul
kıyamet; kızıl cüce doğuşu bitince eski güneş geri gelir. Astronomik doğruluk değil kullanıcının tasarım niyeti esas.

**Zincir.** PULSAR (15 oyun günü, ilk gün ısınma) → KARADELIK_YUTMA (90 sn, doğal geçişte gündüz penceresinde başlar: gün
saati 3000–7200) → GÜNEŞSİZ (pulsar yolu, 10 oyun günü, buzul kıyamet, kalıntı nebulası yok) → KIZIL CÜCE doğuşu → NORMAL
(vanilla güneş, 20 sn geçişle kızıl cüce diski söner; donma doğal çözülür). Normal süpernova → kızıl cüce yolu değişmedi.

**Pulsar görünümü (dönme yok).** Çekirdek diski 10°, hale ~34°, korona ~54° (vanilla güneş diski ~8,6°; görünür ışıma
6 katın üstü), bakışta büyümez. Renk mavi-beyaz → ısınmayla mor-beyaz/çelik. Titreşim iki bileşen: 16 Hz yatay kare dalga
(her yarım periyodun genliği zamandan karmayla 0,6–1,0°, makine gibi düzgün görünmesin; dikey testere, genliğin 0,45'i) +
1–2 Hz iki sinüslü büyük sarsıntı (1,5° + güç); şiddet ısınma gücüyle artar, gerçek zamandan hesaplanır (kare süresinden
bağımsız). Hale iki kopya, zıt ofset, %50 alfa (bulanık çift görüntü). Keskin iki yönlü ışınlar gök kubbesini aşar (uç
köşeleri kamera yönünde 100 birime çekilir: kısa görüş mesafesinde kırpılmaz). Nabız yerine düzensiz "çatırtı": 0,25 sn'lik
yuvalarda %50 olasılıkla 0,10–0,15 sn beyaz patlama (~2/sn). Gök gündüz kavrulmuş turuncu-kızıl `#C0401A`, gece koyu kan
kırmızısı `#2A0608`, sis `#A83A1A` / `#240608` ve yakın (görüş ×(1 − 0,5·güç)), ışık haritası kızıl-sıcak; ufukta titreyen
ısı dalgası şeridi (gök aşaması, UV kayması); kül yağışı parçacığı (kamera çevresi 24 blok, göğe açık yerde, yalnız blok
hasarı açıkken). **Erişilebilirlik:** CLIENT `gokyuzu.pulsarTitresim` (0 = hızlı titreşim ve çatırtı kapalı, yavaş sarsıntı kalır).

| Kademe | Titreşim | Hale kopyası | Işın çifti | Kül yağışı (tick başına × güç) |
|---|---|---|---|---|
| Düşük | 8 Hz | 1 | 1 | 2 |
| Orta | 16 Hz | 2 | 2 | 4 |
| Yüksek | 16 Hz | 2 | 2 | 7 |
| Ultra | 16 Hz | 2 | 2 | 10 |

**Kara delik yutması (90 sn, evre başlangıcından deterministik).** Yeni kara delik güneş yolunda pulsardan 40° ötede.
0–20 sn hızlanarak yaklaşır (1,5° → 20°; açısal boyut uzaklıkla ters orantılı), çevresinde yerel koyu mercek halkası ve Einstein
halkası (göğün geri kalanının yıldızları sönmez). 20–60 sn pulsar kara deliğe çekilir ve spagettileşir (kara deliğe uzanan eğik
kuyruk, mor-mavi-beyaz Doppler'li akresyon diski, titreşim zirvede); 55. sn'de aradaki yolun %80'i, 56,5. sn'de pulsar yok.
55–60 sn kör edici mor-beyaz parlama (0,1 sn'de tepe, 1,5 sn tutar, τ 0,9 sn; ekran örtüsü, ışık haritası, sis) + sunucudan
tüm oyunculara konumsuz kütleçekim dalgası sesi (`karadelik_yutma.ogg`, 8 sn düşen bas 90 → 25 Hz + çatırtı, −23 LUFS) ve 3°,
100 tick ekran sarsıntısı (hızlı bileşeni `pulsarTitresim`'le). 60–90 sn kara delik küçülüp uzaklaşır ve söner, gök simsiyah, uzay
%100, kalıntı nebulası yok. Gök koyu mor `#1C0A2E` → siyah; parlamada `#E8DAFF`'ye %85. Yutmada en çok 16 kuad.

**Cehennem dünya (sunucu, `afetBlokHasari` kapısı arkasında, tek bütçe).** Çimen → kaba toprak → kül bloğu; yaprak yanar,
altına kül katmanı düşer; kütük → kömürleşmiş kütük (siyah çatlaklı, içi kızıl parlayan, ışık 3); çiçek/ot/kaktüs/mantar yanar;
kar ve buz buharlaşır; kum seyrek cama döner; seyrek tek bloklu lav gözeleri; yangın odakları, kor parçacığı; oyuncu başına dakikada
12 meteor (kızıl-beyaz iz). Buzul kıyamette kül katmanı karın altında kalır (kar katmanına döner), çözülünce kül geri gelmez:
toprak ve kül bloğu kalır.

| Evre \ Ay | Ay var | Ay yok |
|---|---|---|
| PULSAR | titreşen pulsar + mavimsi ay; gündüz uzay %55 | titreşen pulsar + kara delikli zengin uzay |
| KARADELIK_YUTMA | yutma koreografisi + mor tonlu ay | yutma koreografisi |
| GÜNEŞSİZ (pulsar yolu) | uzay, soluk gri ay, kalıntı yok | yalnız uzay, kalıntı yok |


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
5. Ufuk efektleri: ay çarpması mantar sütunu, kızıl cüce doğuş ışığı, aurora.
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

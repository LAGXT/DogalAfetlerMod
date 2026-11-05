# Ortak Gökyüzü — tasarım notu (madde 16, bölüm D ve H; N11)

Tek renderer, tek sunucu durumu: `ayVar` (bool) × `gunesEvresi` (`NORMAL`, `BUYUYEN`, `SUPERNOVA`, `GUNESSIZ`, `KIZIL_CUCE`) + evre başlangıç tick'i ve ilerleme. Durum SavedData'da, değişimde ve girişte pakette; geçişler istemcide aynı tick'ten deterministik hesaplanır, böylece herkes aynı anı görür.

## 1. Gerçek olay (görünür gökyüzü)

- **Yıldız renkleri:** çıplak gözle yıldızların çoğu beyaz görünür; parlakların bir kısmı belirgin mavi-beyaz (O/B/A türü) ya da turuncu-kırmızı (K/M türü). Parlaklık dağılımı üsteldir: sönükler çok, parlaklar az. Ufka yakın yıldızlar atmosfer yüzünden sönük ve titrektir.
- **Samanyolu:** karanlık gökyüzünde gökyüzünü boydan boya kesen, ortasında koyu toz şeritleri (Great Rift) olan, merkezine doğru (Yay takımyıldızı) kalınlaşan süt beyazı bant.
- **Galaksiler:** çıplak gözle Andromeda soluk, uzun bir oval leke; fotoyrafta parlak çekirdek, mavimsi sarmal kollar.
- **Emisyon nebulaları ve süpernova kalıntısı:** hidrojen-alfa kırmızı/pembe, oksijen-III mavi-yeşil; Yengeç Nebulası'nda dışta kırmızı-turuncu iplikler, içte mavi-beyaz yaygın ışıma.
- **Süpernova:** gündüz görülebilen nokta ışık (SN 1006, ~−7,5 kadir).
- **Kızıl cüce:** ~3000 K, turuncu-kırmızı disk; gökyüzü daha sıcak ve loş tonlu.

Kaynaklar:
- https://science.nasa.gov/asset/hubble/peering-into-the-heart-of-the-crab-nebula/
- https://noirlab.edu/public/news/noao0305
- https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.1/src/main/java/net/minecraftforge/client/extensions/IForgeDimensionSpecialEffects.java (`renderSky`, `renderClouds`, `renderSnowAndRain`, `adjustLightmapColors` kancaları)
- https://lexxie.dev/forge/1.20.1/net/minecraftforge/client/event/RegisterDimensionSpecialEffectsEvent.html

## 2. Minecraft'ta gösterim

**Kanca.** `RegisterDimensionSpecialEffectsEvent` ile `minecraft:overworld` efekt kimliği için vanilla `OverworldEffects`'i saran bir `DogalAfetGokyuzu` kaydedilir; içindeki vanilla nesneye delege eder. `renderSky` yalnız durum `NORMAL + ayVar` değilse `true` döner ve kendi çizimimizi yapar; normal durumda `false` döner, vanilla ya da başka bir modın gökyüzü aynen kalır (en az çakışma). Sis rengi `ViewportEvent.ComputeFogColor`, gündüz gökyüzü rengi kendi kubbe çizimimizde. Işık motoru, `getSkyDarken`, gün/gece döngüsü ve spawn kurallarına dokunulmaz.

**Çizim sırası (kendi `renderSky`'imiz).**
1. Gökyüzü kubbesi: vanilla'daki üst disk + ufuk gün doğumu/batımı yelpazesi, renkleri evre tablosundan.
2. Derin uzay katmanı (görünürlük `uzayAlfa`): Samanyolu bandı → galaksiler → nebulalar → yıldızlar (additive, derinlik yazımı kapalı), gökyüzü dönüşüyle (`getTimeOfDay`) birlikte döner.
3. Güneş / süpernova / kızıl cüce (evreye göre).
4. Ay (yalnız `ayVar`), ay çarpması sırasında yaklaşan ay ve parçaları, sonra enkaz halkası.
5. Ufuk efektleri: ay çarpması mantar sütunu, kızıl cüce doğuş ışığı, aurora.
6. Vanilla'nın ufuk altı koyu diski.

**Yıldızlar.** Açılışta tek seferlik `VertexBuffer` (vanilla 1500 yıldız yerine Yüksek'te 6000). Yön: birim kürede düzgün rastgele, %35'i Samanyolu düzlemine ±12° içinde yoğunlaştırılmış. Parlaklık: `m = -ln(U)` ile üstel dağılım, boyut 0,08–0,45 (en parlak %1'i 4 kollu küçük ışın dokusu). Renk: %70 beyaz `#F4F6FF`, %12 mavi-beyaz `#AFC8FF`, %10 sarı `#FFE7B0`, %8 turuncu-kırmızı `#FFB08A`. Titreme: yıldızlar 4 gruba bölünür, her grubun alfası farklı fazlı 1,1–2,3 Hz sinüsle ±%15 değişir (VBO yeniden yazılmaz, grup başına tek uniform renk); ufka yakın 15°'de alfa yarıya iner.

**Samanyolu.** Gökyüzü küresinde 30° eğik bir büyük çember boyunca 64 dilimli şerit, 24° geniş; doku 2048×256 (`tools/assets/` Pillow betiyi: çok ölçekli gürültü + ortada koyu toz damarları + merkez şişkinliği, sarımsı merkez → mavimsi beyaz uçlar). Alfa 0,55.

**Galaksiler.** 2 sprite: eğik sarmal (Andromeda benzeri, 256², 9° açısal boyut) ve küçük karşıdan sarmal (128², 4°); prosedurel: logaritmik sarmal kollar + sarı çekirdek + mavi kol düğümleri.

**Nebulalar.** Gece nebula seti (aysız normal gecede): 3 emisyon nebulası (kırmızı-pembe Orion benzeri 6°, mavi-yeşil 4°, karışık 5°). Süpernova kalıntısı (yalnız güneş yokken): eski güneşin yörünge yerinde, 40° açısal boyut, 1024² doku; dışta kırmızı-turuncu iplik ağı (yönlü gürültüden ince çizgiler), içte mavi-beyaz yaygın ışıma, ortada soluk mavi pulsar noktası (0,5 Hz hafif yanıp sönme). Kalıntı çok yavaş genişler (günde %1, tavan %130) — zaman geçtiği hissedilir.

**Güneş evreleri.** `NORMAL`: vanilla güneş dokusu, ölçek 1. `BUYUYEN`: kendi güneş dokumuz (yumuşak kenarlı disk + 3 halkalı korona + yavaş dönen leke dokusu), ölçek ve renk `gunes_patlamasi.md`'deki eğrilerle; güneş etrafında additive hale, büyüdükçe hale gökyüzünün üst kubbesine kadar taşar. `SUPERNOVA`: çökme, parlama (12 ışın), genişleyen kabuk, üstel sönüm. `GUNESSIZ`: güneş yok. `KIZIL_CUCE`: %60 ölçek, `#FF6A3A` disk, kızıl hale; doğuş gününde ufukta 60 sn ışık yayı.

**Evre × ay matrisi.**

| Güneş \ Ay | Ay var | Ay yok |
|---|---|---|
| NORMAL | vanilla (renderer devre dışı) | gündüz vanilla görünüm; gece ay yok, `uzayAlfa` 1 (6000 yıldız + Samanyolu + galaksiler + gece nebulaları), gökyüzü daha koyu `#05070F` |
| BUYUYEN | büyüyen güneş + vanilla ay; gece aurora | büyüyen güneş; gece yıldızlı + aurora |
| SUPERNOVA | süpernova + ay (ay süpernova ışığında mavi-beyaz tonlanır) | süpernova + yıldızlar |
| GUNESSIZ | uzay gündüz-gece açık, ay hilal yerine soluk gri disk (güneş ışığı yok: yalnız kalıntı nebulasının ışığıyla `#5A6070`), evre döngüsü durur | yalnız uzay; en karanlık ve en zengin gökyüzü |
| KIZIL_CUCE | kızıl cüce + turuncu tonlu ay; gündüz uzay %25 | kızıl cüce; gündüz uzay %25, gece %100 |

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

- Her şey açılışta (ya da kalite değişince) bir kez derlenen VBO'lardan; karede ~8 çizim çağrısı, CPU maliyeti sabit. Dokular oyun içinde değil, `tools/assets/` betikleriyle üretilmiş PNG.
- Sunucu maliyeti sıfır; paket yalnız evre değişiminde ve oyuncu girişinde.

## 6. Açık noktalar

- **Uyumluluk riski:** `minecraft:overworld` efektini sarmak, aynı kimliği kaydeden başka bir gökyüzü moduyla çakışır; kayıt sırasında var olan nesne sarılırsa (delege) çoğu durumda ikisi birlikte çalışır [spike 2'de doğrulanacak]. Iris/Oculus şader paketleri kendi gökyüzünü çizebilir; o durumda en azından ayın gizlenmesi için yedek: ay dokusunu kaynak paketiyle değil, `RenderLevelStageEvent.AFTER_SKY`'de ay konumuna gökyüzü renginde örtü çizmek.
- **Gündüz karanlığı:** ışık motoru değişmediği için arazi gündüz aydınlıktır; karanlık his yalnız gökyüzü + sis + `adjustLightmapColors` renk tonlamasıyla (ışık değerini değil rengi değiştirir; GUNESSIZ'de gökyüzü ışığı renk çıktısı ×0,35, mavimsi). Bunun madde 16'daki "ışık motoruna dokunma" ile uyumlu sayılması KARARLAR'a yazılmalı.
- Nether/End'deki oyuncu durumu alır ama oradaki gökyüzü değişmez (N11).

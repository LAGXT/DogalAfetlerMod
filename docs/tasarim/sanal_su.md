# Ortak Sanal Su — tasarım notu (madde 8, bölüm B ve H, N9, N10)

Tsunami dalgası, mağaraya dolan su ve deniz seviyesi yükselmesi tek sistemi kullanır: gerçek su bloğu konmaz, harita değişmez; su sütun başına **dikey aralıklar** olarak tanımlanır, istemcide çizilir, sunucuda varlıklara uygulanır. Spike kararları: `.vibe/kararlar/spike-sanal-su.md` (SU-01…SU-08), `docs/KARARLAR.md` K0-25.

## 1. Gerçek olay ve referans teknikler

- **Su yüzeyi:** sığ suda dip görünür ve su renksizdir, derinleştikçe turkuazdan koyu maviye geçer; kıyı çizgisinde ince köpük şeridi ve ıslak kumun parlak bandı oluşur; dalga tepeleri sivri, çukurları yayvandır.
- **Gerstner dalgaları (GPU Gems, böl. 1):** her köşe yalnız dikey değil yatay da yer değiştirir, köşeler tepeye doğru toplanır; sivri tepe ve yayvan çukur bu yüzden doğar. Diklik parametresi aşılınca yüzey kendi üstüne katlanır; bu eşik köpük için de işaret olarak kullanılır.
- **LOD (Distant Horizons fikri):** uzak araziyi çok daha kaba, önbelleğe alınmış ayrı bir boru hattıyla çizmek; yakın detay vanilla kalitesinde kalır.
- **Mağara suyu:** dolan su en alçak noktalardan başlayarak düz bir yüzeyle yükselir; buharlaşıp çekilirken duvarlarda ıslak şerit ve tortu çizgisi bırakır.

Kaynaklar:
- https://developer.nvidia.com/gpugems/gpugems/part-i-natural-effects/chapter-1-effective-water-simulation-physical-models
- https://github.com/armansito/water-surface (Gerstner uygulaması)
- https://blog.curseforge.com/distant-horizons-frequently-asked-questions (LOD yaklaşımı)
- https://umbc1ok.github.io/posts/water (Gerstner + normal haritası + derinliğe göre renk)

## 2. Minecraft'ta gösterim

### Spike sonrası güncelleme, 2026-11-06

**Veri modeli: tek sorgu, sütun başına dikey aralıklar (SU-01).** Bütün sanal su tek soruya cevap verir: "`(x, z)` sütununda `t` anında hangi `[alt, üst)` aralıkları su?" Render, fizik (batıklık, su yüksekliği, göz suda mı) ve ıslaklık bu sorguyu kullanır; kaynaklar kendi aralığını ekler, kesişenler birleştirilir (tahsissiz). Chunk başına tek yükseklik reddedildi: deniz altındaki mağara cebini ya da denizin üstünden geçen dalgayı aynı sütunda ifade edemez.

| Kaynak | Bellek | Aralık |
|---|---|---|
| Deniz yükselmesi | yalnız yüklü chunk'lar için 16×16 zemin, su, kaynak yüzeyi ve BFS mesafesi | `[zemin, kaynak + artış(t))` |
| Tsunami dalgası | 11 sayı (köken, yön, başlangıç, hız, yükseklik, genişlik, uzunluk, taban, menzil); yüzey analitik `y(x, z, t)` | `[zemin, y)` |
| Mağara suyu | sütun başına dolu `[y0, y1)` çiftleri (seyrek harita) + deterministik seviye–zaman | dolu aralıklar seviyeye kırpılır |
| Çekilme | 6 sayı (merkez, yarıçap, derinlik, başlangıç, süre) | gerçek su sütununda `[taban, zemin − düşüş)` |

- *Deniz yükselmesi:* gerçek su yüzeyi olan her sütun kaynaktır; çok kaynaklı BFS komşuya `zemin < kaynak + maksArtış` ve `zemin ≥ kaynak − 1` ise yayılır, en çok 96 adım (göl, nehir ve deniz her biri kendi yüzeyine göre yükselir; denize bağlı olmayan çukur dolmaz; chunk okuma sırasından bağımsız). BFS tek iplikli arka plan yürütücüsünde, anlık görüntü üzerinde; sonuç tek atamayla yayımlanır (eşzamanlı BFS istemcide 20–170 ms kare takılması yapıyordu). Geçersiz kılma: sunucuda `BlockEvent.NeighborNotifyEvent` (yüzeye 2 blok yakın değişiklik), istemcide `ChunkEvent.Load`; yalnız o chunk yeniden okunur. Diskte yalnız global artış.
- *Mağara suyu (SU-04):* tohumdan öncelik kuyruğuyla en alçak hücre önce keşif (en çok 20 000 hücre; sınıra takılırsa bekleyen en alçak hücre sızıntı seviyesidir), sonra en alçaktan düz yüzeyle kat kat dolum (cep başına en çok 4 000; katı aşacaksa durur, yüzey düz kalır). Cep = "boş + gökyüzünü görmeyen" hücreler. Harita bütünlüğü: cebin kutusu (+2 blok) başlangıçta ve bitişte hash'lenir, eşitlik log'a yazılır.

**Render (SU-02).** Şader'siz, `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS` (vanilla gerçek su dahil yarı saydam chunk katmanından hemen sonra, aynı derinlik tampon ve Fabulous hedefiyle). İki `VertexBuffer` (`DefaultVertexFormat.BLOCK`): *yakın halka* (kamera çevresi ±40 blok, 1 blok hücre, üç sinüs dalgacık, yan yüzler, eğime göre köpük, derinliğe göre alfa; her tick kurulur) ve *uzak LOD* (4×4 blok düz hücre, görüş mesafesine kadar; 10 tick'te bir ya da kamera 8 bloktan çok kayınca). Vanilla `rendertype_translucent` şaderi, blok atlasındaki `block/water_still`, biyom su rengi ve ışık haritası (sütun başına önbellek) → renk ve parlaklık vanilla suyla eşleşir. Öteleme `CHUNK_OFFSET` uniform'unda (model-görünüme konunca silindirik sis mutlak Y ile hesaplanıp su grileşiyordu). `AFTER_PARTICLES` / `AFTER_WEATHER` reddedildi (Fabulous'ta farklı hedef, yağmur suyun arkasında kalır).

**Fizik (SU-03, K0-25).** `Entity` mixin'i, **iki enjeksiyon**: `updateInWaterStateAndDoFluidPushing` `RETURN`'de sanal su varsa `fluidHeight[WATER]`, Forge `forgeFluidTypeHeight[WATER_TYPE]`, `wasTouchingWater` atanır (sıçrama efekti, düşme mesafesi sıfırlama, ateş söndürme, dalga sürüklemesi); `updateFluidOnEyes` `TAIL`'de göz sanal sudaysa `forgeFluidTypeOnEyes = WATER_TYPE`. Vanilla hesabı ezilmez (gerçek suda vanilla değeri korunur, `max`). Olay yolu (`LivingBreatheEvent` + elle ivme) denendi: yalnız boğulmayı karşıladı; yüzme pozu, vanilla su hareketi, su altı bindirmesi ve mob yapay zekâsı (`isInWater`, `getFluidHeight`) yoktu — reddedildi. Ölçülen: sanal deniz +4'te `isInWater`/`isUnderWater` true, `isSwimming` true, SWIMMING pozu, vanilla hava kaybı ve boğulma.

**Ağ.** Sunucu otoriterdir; istemciye parametre gider, hücre gitmez. Paket tam durumdur: deniz 28 bayt, bir dalga 116 bayt, çekilme 76 bayt; yalnız mağara cepleri sütun aralıklarıyla (sütun başına ~17 bayt, bir kez; Faz 2'de varint/delta ile küçültülebilir). Dalga ve mağara seviyesi oyun zamanından deterministik olduğu için tick başına paket yok.

**Yüzey ağı (hedef görünüm).** Yakın halkadaki üç sinüs dalgacık spike'ta yeterli oldu; Gerstner yatay yer değiştirmesi yalnız tsunami cephesinin dikleşen tepesinde gerekir (`tsunami.md`). Kalite yükseldikçe en ince hücre 1 → 0,5 → 0,25 blok ve LOD halka sayısı artar (§5); halka sınırlarında T-köşe çatlaklarını kapatan eğik şerit.

**Görünüm.**
- *Renk:* biyom su rengi, derinliğe göre: `d < 1` blok neredeyse saydam (alfa 0,25, zemin görünür), `d = 4` alfa 0,65, `d ≥ 10` alfa 0,85 ve koyu ton; köşe renginde önceden hesaplanır.
- *Doku:* vanilla `block/water_still` (animasyonu ve paket dokularıyla uyum bedava gelir).
- *Parlama:* güneş/ay yönüne göre köşe başına sahte yansıma (normal·yarım vektör)^32 ile beyaza karıştırma; gece yıldız/ay parlaması soluk.
- *Köpük:* (1) kıyı şeridi: `d < 0,6` olan köşelerde köpük karışımı, 2 sn periyotla ileri-geri "yalayan" bant; (2) dalga tepesi: eğim eşiği geçildiğinde; (3) engel köpüğü: blok ya da varlık çevresinde 1 blok halka.
- *Yan yüzler:* sanal su havayla dikey sınır yaptığında (dalga cephesi, mağara cebi kenarı, alçalmış su ile gerçek su arası) dikey yüzey çizilir, üst kenarı köpüklü; gerçek suyla birleştiği yerde yüz çizilmez, yüzey gerçek suyun üstüne 0,02 blok bindirilir.
- *Su altı görünümü:* kamera sanal suyun içindeyse `ViewportEvent.RenderFog` (sis −8..40, küre; değişiklik yalnız `setCanceled(true)` ile uygulanır) ve `ComputeFogColor` (biyom su sis rengi); vanilla su altı bindirmesi ve kabarcıklar mixin sayesinde kendiliğinden gelir. Mağarada aynı kural.

**Etkileşim (madde 8).** Batıklık oranına göre vanilla yüzme, kaldırma, zıplayıp yükselme, hava kaybı ve boğulma, ateşin sönmesi (mixin). Dalga sürüklemesi batıklık × dalga ivmesi: oyuncuyu kendi istemcisi (hareketin sahibi), diğer varlıkları sunucu iter; parametreler deterministik olduğu için iki taraf aynı değeri bulur. Islaklık API'si: `SanalSu.icindeMi(Entity)`, `SanalSu.batiklikOrani(Entity)` (0..1), sunucu ve istemcide; sıcaklık sistemi bunu okur. Su mob'ları için sanal su spawn yeri sayılmaz.

**Mağara suyu yaşamı.** Dolma (tsunami sırasında; yüzey cebin dibinden yukarı 2 blok/sn) → bekleme (config, varsayılan 10 dk) → buharlaşma: yüzey her 6 sn 0,25 blok iner; yüzeyden beyaz buhar (`CLOUD`, yüzey alanı başına %4), çekilen suyun bıraktığı duvarlarda 30 sn kalan koyu "ıslak şerit" (istemci decal, blok değişmez). Bitişte cep silinir; dünya bayt bayt eski hali (hash kanıtı).

**Deniz yükselmesi görünümü.** 5 dk boyunca seviye doğrusal artar (sarı kum → ıslak koyu kum → su altı); kıyı köpüğü ilerleyen çizgiyi izler, ilk kez su altında kalan çimen üzerinde 10 sn küçük kabarcık. Sanal su ile kaplı alanda çimen, çiçek ve ekin görsel olarak su altındadır (gerçekte değişmez).

## 3. Vitrin anı

**Deniz yükselmesinin bir kıyı köyünü yaladığı 10 saniye, gün batımı (ay yok, yıldızlar çıkıyor).** Kamera iskelenin ucunda, Y+3, kıyıya dönük (pitch −8°). Görüntüde: suyun altında kalmış plaj ve yarısına kadar suya gömülmüş ahşap iskele, ilerleyen köpük çizgisi ve arkasındaki ıslak koyu kum bandı, sığ suyun altında görünen çimen ve çiçekler, yüzeyde gün batımı parlaması şeridi. Tsunami vitrini `tsunami.md`'dedir.

## 4. Ses tasarımı

- Kıyı çizgisinde yalama döngüsü (sentez: 0,5 Hz zarflı pembe gürültü + kısa köpük cızırtısı), en yakın 3 kıyı noktasından konumsal; düzey çok düşük.
- Sanal suya girme/çıkma, yüzme ve su altı sesleri vanilla (mixin sayesinde vanilla kendisi çalar).
- Mağara suyu: yankılı damlalar (`block.pointed_dripstone.drip_water_into_cauldron`), buharlaşmada hafif tıslama.

## 5. Performans planı

| Kalite | En ince hücre | LOD halkaları | Dalgacık bileşeni | Köpük | Sanal su görüş mesafesi |
|---|---|---|---|---|---|
| Düşük | 1 | 1 / 4 | 1 | yalnız kıyı | 8 chunk |
| Orta | 1 | 1 / 4 | 2 | kıyı + tepe | 12 |
| Yüksek | 1 (±40 blok) | 1 / 4 (spike) | 3 | hepsi | görüş mesafesi |
| Ultra | 0,5 | 0,5 / 1 / 4 | 4 | hepsi | görüş mesafesi |

### Spike sonrası güncelleme, 2026-11-06

- Yüksek satırı spike'ın ölçtüğü düzene çekildi (yakın 1 blok ±40 + 4×4 LOD). Ölçülen (SU-08, aynı kamerada afetsiz/afet dönüşümlü): deniz +4 afetsizin %83–89'u, dalga %94–104, mağara %88–111, çekilme (derleme bittikten sonra) %96–115. Hedef (≥ %50) her çiftte tuttu.
- CPU: su render'ı kare başına 0,4–0,6 ms (deniz), yakın halka kurulumu tick başına ~3 ms, LOD 10 tick'te bir ~3 ms. Renk/ışık önbelleği olmadan LOD kurulumu ~150 ms'ye çıkıyordu (vanilla renk önbelleği taşıyor).
- Sunucu deniz alanı: oyuncu çevresi ±6 chunk, 10 tick'te bir 32 chunk okuma bütçesi; BFS 13×13 chunk 1,5–8,5 ms (arka planda). İstemci görüş mesafesi (en çok 12) + 1 chunk, 64 chunk bütçe; 27×27 chunk BFS 3–50 ms (arka planda).
- Frustum dışı karolar ne hesaplanır ne çizilir; gerçek araziyle örtüşen ve tamamen görünmeyen hücreler atlanır.

## 6. Açık noktalar

| Değer | Varsayılan |
|---|---|
| Deniz yükselmesi | 4 blok, üst sınır 6, 5 dk'da, kalıcı (geri dönüş seçeneği: 3 günde) |
| Yükselmenin karaya yayılma sınırı | gerçek suya bağlı olmayan çukurlar dolmaz; gerçek su yüzeyinden en çok 96 blok |
| Mağara suyu hacim tavanı | keşif 20 000 hücre, cep başına 4 000 |
| Mağara bekleme / kuruma | 10 dk / 0,25 blok her 6 sn |
| Boğulma | vanilla hava süresi ve hasarı |

### Spike sonrası güncelleme, 2026-11-06

- **Kapandı — yüzme/boğulma:** `Entity` mixin'i (2 enjeksiyon, K0-25); `LivingBreatheEvent` Forge 1.20.1'de var ama tek başına yetmedi.
- **Kapandı — çekilme:** `LiquidBlockRenderer.tesselate` `HEAD` mixin'i (istemci; ayrıntı `tsunami.md`). Forge 47.4.28'de sıvı tesselasyonunu atlatan olay yok.
- **Kapandı — bindirme:** gerçek suyla birleşimde 0,02 blok bindirme ve `AFTER_TRANSLUCENT_BLOCKS` ile aynı derinlik tamponu yeterli oldu; sis için `CHUNK_OFFSET` düzeltmesi.
- Mixin yapılandırması: `dogalafetler.su.mixins.json` (ortak) ve `dogalafetler.client.su.mixins.json` (yalnız istemci); üretim jar'ında refmap ve `MixinConfigs` manifest girdisi.
- **Açık:** Embeddium/Rubidium gibi sıvı render'ını değiştiren modlar yüklüyken tesselasyon mixin'i çalışmayabilir; yedek "ıslak dip" yüzeyi bu durum için saklanır.

## Faz 2 güncellemesi, 2026-11-06

Kaynak: `.vibe/kararlar/sanal-su.md` (§A sözleşme, SU-11…SU-37), rapor `.vibe/rapor/sanal-su.md`.

- **Deniz alanı su kütlesi başına (SU-11):** spike'ın "kaynak + artış" BFS'i yerine her durgun su kütlesinden öncelikli
  taşkın; sütun, kütleden ona giden en alçak yolun en yüksek zemini (eşik) yükselen seviyenin altındaysa dolar. Deniz
  (yüzey ≤ deniz seviyesi) sınırsız yükselir, göl taşma ağzında durur, kıyı kumulunun arkasındaki çukur ancak su kumulu
  aşınca dolar, 96 adım sınırı yok. Havada asılı su duvarı oluşmaz (rastgele arazi testi). §6'daki "gerçek su yüzeyinden en
  çok 96 blok" satırı bununla geçersiz: sınır yüklü alandır.
- **Deniz yükselmesi dönem süreci:** ay yok olunca 10 gerçek dakika (config) bekler, 5 dk'da +4 bloka (üst sınır 6, en
  çok 8) doğrusal yükselir, kalıcıdır; `/dogalafet denizseviyesi`, `/dogalafet aysonrasi atla`; buharlaşma (N10) artışı
  kademeli sıfıra indirir. Artış çekirdeğin `DunyaDonemi.deniz()` alanında kalıcı ve senkron.
- **Mağara suyu:** `SanalSu.magaraDoldur(merkez, hacim, kalış)`; kalış 600 sn, kuruma 0,25 blok / 6 sn, yüzeyden buhar
  (kalite çarpanlı), bitişte cep kutusu hash'i eşit. `asama ileri` kurumayı beklemeden ilerletir.
- **Görünüm:** kıyı şeridinde 2 sn periyotla yalayan köpük bandı, sığda alfa 0,25 → derinde 0,85; dalgacık bileşeni ve
  LOD mesafesi efekt kalitesine bağlı (Düşük 1 bileşen / 8 chunk … Ultra 4 bileşen).
- **Güneş yansıması (SU-21):** dalgacık normaliyle sahte speküler, ufukta söner, gün batımında turuncu.
- **Görünüm, tur 2 (SU-28):** köşe başına Fresnel ile gök/ufuk yansıması ve çok oktavlı eğim; kendi köpük dokumuz
  (`su_kopuk.png`, `tools/assets/su_kopuk.py`) kıyı şeridinde, dalga tepesinde ve kırılan dudakta; tepe boyunca serpinti;
  çekilen suyun ardında ıslak dip; su altında yüzeyden inen ışık demetleri ve ışığa bağlı turkuaz sis; mağarada
  dalgacık, tavan ışığı parlaması ve duvar kenarında ıslak şerit.
- **Kalıcılık (SU-24):** dalga, çekilme ve mağara cebi parametreleri dönemin `ekVeri("su")`'sunda; yeniden başlatmada
  mağara suyu kaldığı yerden sürer (hücreler dünyadan yeniden hesaplanır).
- **Görünüm, tur 3 (SU-35, SU-37):** mağara suyunda şader'siz tavan yansıması (dalgacıkla titreyen sıcak ışıltaşı/fener
  parlaması) ve belirgin dalgacık; su altı ışık demetleri yumuşak kenarlı üç bant, mağarada sıcak renk ve amber sis; uzak LOD'da
  dalga gövdesinin dikey cephesi (uzaktan da duvar okunur).
- **Vitrin kareleri (tur 3, K0-21 rev. 2 protokolüyle):** koşu 14 → `KOK/.vibe/kanit/sanal-su/sanal-su-deniz-vitrin.png`
  (akşam, iskelenin ucundan kıyı köyü, deniz +4; öncesi `sanal-su-deniz-once.png`), su altı `sanal-su-sualti.png`, mağara
  `sanal-su-magara-once.png` → `sanal-su-magara.png` → `sanal-su-magara-kuruyor.png` (seviye ~1,8 blok inmiş), su altı
  `sanal-su-magara-sualti.png`; koşu 17 (yalnız dalga bölümü) → `sanal-su-dalga-once.png` → `sanal-su-dalga.png` (16 blok,
  iskele ekseninde ev sırasının arasından alçaktan, duvar kadraj ortasında). Gün batımı karesi yalnız tur 1'de
  (`kosu5/sanal-su-deniz-gunbatimi.png`, eski görünüm).
- **Ölçülen FPS, tur 3 (koşu 14, afet / afetsiz):** deniz +4 256,1 / 260,6 (%98), dalga 281,3 / 273,0 (%103), mağara 387,2 /
  421,7 (%92); su CPU'su kare başına 0,13–0,25 ms, yakın halka kurulumu tick başına en çok 2,0–2,1 ms; sunucu TPS 20.
- **Ölçülen FPS (ortanca kare, aynı kamera, afet / afetsiz):** koşu 12: dalga 212,9 / 221,1 (%96), mağara 120,0 / 231,0
  (%52), deniz çifti geçersiz (afetsiz ölçüm dünya ısınırken: 95 / 225). Su CPU'su kare başına 0,26–0,48 ms; aynı makinede
  başka işçilerin Gradle/istemci süreçleri açıktı, mutlak FPS koşudan koşuya 30–380 arasında oynadı. Sunucu TPS 20
  (mspt 9,7–15,4). Tur 1 (koşu 4/5): deniz +4 %87/%91, dalga %101/%106, mağara %100/%56.

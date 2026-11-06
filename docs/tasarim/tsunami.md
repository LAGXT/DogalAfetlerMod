# Tsunami — tasarım notu (bölüm B, madde 8, 10; N10, N14)

Ortak su görseli ve fizik ayrıntısı `sanal_su.md`'dedir; bu not tsunaminin koreografisini anlatır.

## 1. Gerçek olay

- **Doğal uyarılar (IOC-UNESCO "Feel / See / Hear"):** uzun ve güçlü sarsıntı; denizin olağandışı hızla çekilmesi ve **resiflerin, kayaların ve dipteki balıkların açığa çıkması**; kıyıya **su duvarı** olarak gelip hızla içeri taşması; tren ya da jet uçağına benzeyen **gürültülü uğultu**.
- Tsunami tek dalga değil, uzun ve geniş **dalga dizisidir**; ilki her zaman en büyüğü değildir. Kıyıya yaklaştıkça sığlaşan dipte yavaşlar ve dikleşir; çoğu zaman kırılan bir duvar ya da türbülanslı bir **bore** (köpüklü basamak) olarak gelir, düzgün bir sörf dalgası gibi kıvrılmaz.
- Karada su bir **enkaz hattı** iter: önünde köpük, kırık tahta, salınan nesneler; arkasında kahverengi, çamurlu akıntı. Geri çekilen su (backwash) da yıkıcıdır ve enkazı denize taşır.
- İz: en yüksek su çizgisinde enkaz şeridi, yaş ve kararmış zemin, devrik ağaçlar.

Kaynaklar:
- https://legacy2.ctic.ioc-unesco.org/tsunami-info/natural-tsunami-warning-signs
- https://www.ses.nsw.gov.au/plan-and-prepare/tsunami/what-is-a-tsunami (dalga dizisi, uzun ve geniş dalgalar)
- https://developer.nvidia.com/gpugems/gpugems/part-i-natural-effects/chapter-1-effective-water-simulation-physical-models (Gerstner dalgası: tepe dikleşmesi, köpük için diklik ölçütü)

## 2. Minecraft'ta gösterim

**Zaman çizelgesi (S3 örneği):**

| An | Olay |
|---|---|
| −10 sn | Siren (10 sn), fade-out ile biter |
| 0–18 sn | **Çekilme:** kıyı çizgisi 20 blok denize doğru kayar; gerçek su gizlenir, alttaki kum/çakıl/yosun ve sığ su bitkileri açığa çıkar; açıkta kalan zeminde çırpınan balık parçacıkları (vanilla `cod`/`salmon` model küçük zıplama) ve damlayan su |
| 10–18 sn | Ufukta beyaz bir çizgi belirir: dalga cephesi, açıkta alçak ve geniş |
| 18–30 sn | Dalga kıyıya yaklaştıkça yükselir (sığlaşma: yükseklik `H·(1 - d/d0)^0.25` ile artar), tepe köpüklenir, ön yüz dikleşir |
| 30 sn → | Dalga karaya vurur, içeri ilerler, yıkar, sürükler; arkasında taşkın alan |
| +20–40 sn | Taşkın duruklar, **geri çekilme** akıntısı denize doğru akar; S3+ ikinci, daha küçük dalga (%60 yükseklik) |
| son | Mağaralara giren su kalır (`sanal_su.md` §mağara), yüzey suyu 30 sn'de çekilir |

**Çekilme tekniği.** Gerçek su blokları değişmez; istemcide "su gizleme maskesi" (sütun başına gizlenen derinlik) gerçek suyun bu sütunlarda çizilmemesini sağlar, yerine indirilmiş seviyede sanal su yüzeyi çizilir. Kanca: `LiquidBlockRenderer.tesselate` mixin'i (aşağıdaki spike güncellemesi). Ay çarpmasından sonraki çarpan döneminde çekilme mesafesi ×1,5 (bölüm H: "daha belirgin").

**Dalga gövdesi.** Dalga, ilerleme yönüne dik bir cephe boyunca yükseklik profili `h(s, t)` olan sanal su hacmidir (s = cepheye uzaklık):
- Ön yüz: sığ suda diklik artınca tepe noktası ileri kaydırılır (Gerstner yatay yer değiştirme) ve **kırılan dudak** oluşur: tepenin önündeki 2–4 blokluk ters eğim (sarkıntı) ağın kendi üzerine katlanmasıyla çizilir.
- Renk: dipte koyu turkuaz (`#1E5F6E`), üst kısımda daha açık ve saydam (ışığın geçtiği ince tepe, `#5FB7B0`, alfa 0,55); karaya girdikten sonra 3 sn içinde çamurlu kahverengiye (`#6B5A3E`) kayar.
- **Köpük:** diklik ölçütü (Jacobian < eşik) geçilen yerlerde köpük dokusu karışır; tepe boyunca beyaz şerit, ön yüzde aşağı akan köpük çizgileri, cephe tabanında yuvarlanan beyaz bore.
- **Sıçrantı:** tepe boyunca her blokta 0–2 su damlası + büyük beyaz sıçrantı sprite'ı (kendi dokumuz, 8 kare); engele (blok, varlık) çarpan cephede 6–10 blok yukarı fışkıran beyaz duvar.
- **Enkaz hattı:** yıkılan bloklar `SuruklenenBlokVarligi` (yalnız görsel, istemcide) olarak dalga önünde yüzer, döner, dalga durunca en uzak noktaya bırakılır; sürüklenen eşyalar gerçek `ItemEntity`.

**Özel yağmur.** Afet boyunca 96 blok içinde kendi yağmur renderer'ımız: rüzgârla dalga yönüne 25° eğik, uzun ve ince çizgiler (vanilla'dan 1,5 kat uzun), iki katman (yakın hızlı, uzak yavaş ve soluk); zemine çarpan damlada küçük halka sıçrantısı; sanal su yüzeyinde halka dalgacıkları (normal haritası bozulması). Gri-mavi sis, görüş %70.

**Yıkım koreografisi.** Cephe her ilerlediğinde önündeki 3 blok derinlikteki yıkılabilir bloklar sırayla kırılır: önce yaprak, çiçek, çit, cam (anında), sonra ahşap (0,3 sn gecikme), sonra kırıktaş (yalnız S4–5). Kırılan bloğun %30'u sürüklenen enkaz olarak dalgaya katılır. Bu sıralama binanın "önce camlar, sonra duvar" diye dağıldığını gösterir.

**Sürükleme (sunucu).** Her oyuncu/mob için: suya batıklık oranı × dalga hızı × seviye katsayısı kadar ivme; oyuncu girdisi %35'e düşer; yatay hız 0,2 blok/tick'i geçip bir bloğa çarparsa `(hız - 0,2) × 10` hasar. Oyuncunun arkasında 3+ sıra dayanıklı blok varsa (rüzgâr gölgesi gibi "su gölgesi") kuvvet %40 azalır; dalga tepesinin üstündeki oyuncu etkilenmez.

**Kamera.** Cephe 24 blok içindeyken yerden gelen düşük frekanslı titreme (±0,3°·seviye); içine düşülünce kamera yuvarlanması ±8° salınım, ekran kenarında su perdesi.

**Seviye 1 ile 5 farkı.** S1: dizden yüksek, köpüklü bir bore, plajı ve iskeleyi geçer, 30 blok içeri girer. S5: 20 blok yüksekliğinde, tepesi kırılarak katlanan turkuaz bir duvar, çekilmede 40 blok dip açığa çıkar, köyü ve ormanları yıkarak 220 blok içeri girer, ardından ikinci dalga gelir.

### Spike sonrası güncelleme, 2026-11-06

Kaynak: `.vibe/kararlar/spike-sanal-su.md` SU-01…SU-05, `docs/KARARLAR.md` K0-25.

- **Çekilme (SU-05):** istemci mixin'i `client.su.mixin.LiquidBlockRendererMixin`, `tesselate` `HEAD`'de: çekilme bölgesinde ve üstündeki su katmanı gizlenen katmandan azsa gerçek su bloku çizilmez. Bölge başlarken ve biterken bir kez `LevelRenderer.setBlocksDirty` ile chunk'lar yeniden derlenir; düşüşü sanal yüzey gösterir, alçalmış yüzey ile gizlenmemiş gerçek su arasındaki dikey duvar sanal yan yüzle çizilir. Sunucu ve fizik gerçek suyu görmeye devam eder (kabul edildi). Ölçülen: ~210 chunk bölümünü işaretleme 1–15 ms, yeniden derleme süresince en uzun kare 36–286 ms (makine yüküne bağlı; çekilme başlangıcında bir kez), derleme bitince FPS afetsizle aynı bantta. Embeddium/Rubidium gibi sıvı render'ını değiştiren modlar yüklüyken yedek: sığ kıyı şeridinde (≤ 3 blok derin) gerçek suyun üstüne zemin renginde opak "ıslak dip" yüzeyi.
- **Dalga verisi:** dalga 11 sayılık parametre kümesidir (köken, yön, başlangıç, hız, yükseklik, genişlik, uzunluk, taban, menzil), yüzey analitik `y(x, z, t)`; sütun aralığı sorgusu `[zemin, y)` döner. Kırılan dudak (sarkıntı) yalnız görsel ağdır; fizik sütun aralığıyla yetinir.
- **Render:** `AFTER_TRANSLUCENT_BLOCKS`, vanilla `rendertype_translucent` şaderi ve `block/water_still` dokusu; dalga cephesinin yan yüzleri spike'ta çalıştı; köpük tepe şeridi Faz 2 kabul ölçütüdür (beyaz piksel oranı ölçülebilir). Bu nottaki renk tonları (`#1E5F6E` → `#5FB7B0`, karada `#6B5A3E`) köşe rengiyle biyom su renginin üstüne karıştırılır.
- **Sürükleme ve yüzme:** `Entity` mixin'i (2 enjeksiyon) dalgaya giren oyuncu ve mob'a vanilla yüzme, hava kaybı ve boğulmayı verir; dalga itmesi batıklık × dalga ivmesi (`SanalSuDunyasi.itme`): oyuncuyu kendi istemcisi, diğer varlıkları sunucu iter, ikisi aynı deterministik değeri bulur. Çarpma hasarı ve "su gölgesi" sunucuda kalır.
- Ölçülen FPS: dalga afetsizin %94–104'ü, çekilme (derleme sonrası) %96–115.

## 3. Vitrin anı

**Dalganın kıyıya vurmadan önceki son 6 saniyesi.** Kamera kıyıdaki bir tepede ya da kulede, dalgaya 40–60 blok uzakta, cephenin yanından çapraz bakar (yön açısı 30–45°, pitch −5°). Görüntüde: ön planda açığa çıkmış, ıslak parlayan dip ve çırpınan balıklar; ortada ufku kaplayan turkuaz duvar, tepesinden öne katlanan beyaz köpüklü dudak ve havaya savrulan sıçrantı; dalganın ince tepesinden sızan ışık; eğik yağmur çizgileri; dalganın önünde çatısı havalanan bir ahşap ev. Küçük resimde dalga yüksekliği bir evden belirgin şekilde büyük görünmeli.

## 4. Ses tasarımı

- **Çekilme:** artan bir "emme" ve çakıl hışırtısı (pembe gürültü, 200–1500 Hz bant, 15 sn crescendo), damlama (`block.pointed_dripstone.drip_water`), çırpınan balık (`entity.cod.flop`). Martılar sessizleşir: normal ortam sesleri bu sürede %50 bastırılır — sessizlik gerilim yaratır.
- **Yaklaşan dalga:** jet/tren uğultusu, 50–150 Hz, mesafe azaldıkça yükselir; konumsal, mono, cephe boyunca 3 kaynak (sol-orta-sağ) ile geniş duyulur.
- **Çarpma:** kırılan dalga çıtırtısı + derin güm (`entity.generic.splash` üç pitch, kendi sentez basımız), yıkılan ahşap ve cam vanilla kırılma sesleri (aynı anda en çok 6).
- **İçinde:** `ambient.underwater.loop` + boğuk alt geçiren filtre.
- Kategori `WEATHER`/`BLOCKS`; konuşma bandı oyuğu ve tüm katmanların toplamı için tavan `ses.md`'deki gibi.

## 5. Performans planı

| Kalite | Dalga ağı (yakın hücre) | Köpük/sıçrantı sprite | Sürüklenen görsel enkaz | Yağmur damla/sn | İkinci dalga |
|---|---|---|---|---|---|
| Düşük | 1 blok / 4 blok uzak | 300 | 10 | 400 | yok |
| Orta | 0,5 / 2 | 800 | 25 | 900 | var |
| Yüksek | 0,5 / 1 | 1600 | 50 | 1600 | var |
| Ultra | 0,25 / 1 | 3000 | 90 | 2600 | var |

- LOD ve hücre ağı `sanal_su.md`'deki ortak kurallarla; dalga cephesi 48 blok içinde her zaman en ince ağla.
- Sunucu: yıkım kuyruğu tick başına en çok 48 blok (S5), TPS kuralıyla kısılır; sürükleme yalnız dalgaya 4 blok yakın varlıklar için (AABB sorgusu cephe şeridinde).
- Ağ (spike sonrası güncelleme, 2026-11-06): dalga parametreleri (116 bayt) ve çekilme (76 bayt) tam durum paketiyle bir kez gider; istemci aynı deterministik formülle dalgayı oyun zamanından kendisi hesaplar. 2 Hz düzeltme paketi gerekmedi (SU-01).

## 6. Açık noktalar — önerilen varsayılanlar

| Seviye | Dalga yüksekliği | Hız (blok/sn) | Karaya giriş | Cephe genişliği | Çekilme mesafesi | Sürükleme ivmesi (blok/tick²) | Su hasarı | Yıktığı sertlik |
|---|---|---|---|---|---|---|---|---|
| 1 | 4 | 4 | 30 | 60 | 10 | 0,04 | 0,5 can/sn | ≤1,0 |
| 2 | 7 | 5 | 60 | 100 | 15 | 0,06 | 1 | ≤2,0 |
| 3 | 10 | 6 | 100 | 140 | 20 | 0,08 | 1,5 | ≤3,0 |
| 4 | 14 | 7 | 150 | 200 | 30 | 0,10 | 2 | ≤3,5 |
| 5 | 20 | 8 | 220 | 260 | 40 | 0,12 | 3 | ≤4,0 |

### Spike sonrası güncelleme, 2026-11-06

- **Kapandı — çekilme:** Forge 47.4.28 kaynaklarında sıvı tesselasyonunu atlatan ya da chunk geometrisine müdahale eden olay yok (`IClientFluidTypeExtensions` yalnız doku, renk, bindirme ve sis verir); `LiquidBlockRenderer.tesselate` `HEAD` mixin'i seçildi ve çalıştı (K0-25). Gerçek su için alfa 0 renk reddedildi (yine mixin ister, alfa 0 yüzey derinlik yazar ve dibi gizler). "Islak dip" yalnız sıvı render'ını değiştiren modlar için yedek. Gizlenen bölgede sunucu hâlâ su görür; oyuncu orada yürürse yüzme fiziği sürer (kısa süreli, kabul edildi).
- Hedef oyuncunun 96 blok içinde en az 400 sütunluk deniz/okyanus suyu yoksa N14 uygulanır.

## 7. Faz 4 uygulama güncellemesi, 2026-11-06

Ayrıntı ve gerekçeler `.vibe/kararlar/tsunami.md` TS-01…TS-14.

- **Zaman:** dalga her seviyede kıyıdan `clamp(hız·18 sn, 60, 150)` blok açıkta doğar ve ~18 sn'de kıyıya varır (çarpma ~28. sn); S3+ ikinci dalga birincisi menzile vardığı an başlar. Sığlaşma formülü açıkta %35 pay ile (sıfır yükseklik yerine ufukta alçak çizgi).
- **Yıkım:** yalnız yapı ve ağaç blokları (yaprak, cam, çit → ahşap → S4–5 taş yapı); doğal arazi, çimen/çiçek, su dolu ve blok varlıklı bloklar (sandık) yıkılmaz. Tick başına değişen chunk bölümü sınırlı (istemci yeniden derlemesi, TS-12). Enkaz kırılan ahşap/taş blokların bir kısmının gerçek eşyasıdır ve dalgada sürüklenir.
- **Mağara:** doğal tavanlı, göğü görmeyen ve yanında açık hava olan hücre dalgaya açılan ağız sayılır; seviye başına `1 + seviye/2` cep.
- **Ses:** kendi sentez seslerimiz (`tools/assets/tsunami_ses.py`): yaklaşan dalganın 40–150 Hz uğultusu cephenin ortasında döngülü ortam katmanı (`uyari.OrtamSesi`, açıklığı geçerken %35 → %100), kıyıya vurmada derin güm + sıçrama darbesi cephe boyunca üç noktada; çekilmede vanilla çırpınan balık ve damlama.
- **Köpük:** ortak su renderer'ının üstüne tsunaminin kendi katmanı (`client.tsunami.TsunamiKopugu`): tepe boyunca beyaz şerit, ön yüzde kesik köpük çizgileri, tabanda yuvarlanan bore.
- **Yağmur:** kendi damla havuzu (çizgi + zemin/sanal su halkası), iki katman, 25° eğik; sanal su yüzeyindeki halka normal haritası bozulması yerine çizilen halkadır.
- **Faz 5 cilası (şef kararı, Faz 4'te yapılmaz):** ekran kenarındaki su perdesi ve istemcide yüzen görsel enkaz (`SuruklenenBlokVarligi`).

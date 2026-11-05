# Ortak Sanal Su — tasarım notu (madde 8, bölüm B ve H, N9, N10)

Tsunami dalgası, mağaraya dolan su ve deniz seviyesi yükselmesi tek sistemi kullanır: gerçek su bloğu konmaz, harita değişmez; su bir **yükseklik alanı** (yüzey) ya da **hacim cebi** (mağara) olarak tutulur, istemcide çizilir, sunucuda varlıklara uygulanır.

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

**Veri modeli (sunucu ve istemci aynı kodla hesaplar).**
- *Yüzey suyu (deniz yükselmesi, tsunami):* chunk başına 16×16 `float` su seviyesi ve `short` zemin yüksekliği tablosu. Deniz yükselmesinde değer gerçek dünyadan türetilir: gerçek su yüzeyi `s` olan sütunlardan başlayan BFS, zemini `s + artış`'tan alçak ve bağlı komşulara yayılır (göl, nehir ve deniz her biri kendi yüzeyine göre yükselir). Sonuç chunk yüklenince tembel hesaplanır, chunk değişene kadar önbellekte kalır; diskte yalnız global `artis` sayısı tutulur. Tsunamide alan, dalga profilinden zamanla hesaplanır.
- *Mağara suyu:* tsunami suyunun gökyüzünü görmeyen boşluğa girdiği noktadan başlayan sınırlı taşkın dolgusu (hava blokları, en alçaktan yukarı), hacim tavanlı. Her **cep** = bağlı hava hacmi + yüzey Y + bitiş zamanı; chunk bölümü başına bit kümesi olarak SavedData'da (sunucu yeniden başlasa da kalır).

**Yüzey ağı.** Kamera çevresindeki yüzey suyu çok çözünürlüklü ızgarayla çizilir: 0–32 blok 0,5 blok hücre, 32–96 blok 1 blok, 96–192 blok 2 blok, ötesi 4 blok; halka sınırlarında T-köşe çatlaklarını kapatan eğik şerit. Köşe yüksekliği = su seviyesi + dalga: deniz yükselmesinde 3 sakin Gerstner bileşeni (genlik toplamı 0,15 blok, dalga boyu 6–18 blok, rüzgâr yönünde), tsunamide dalga profili + aynı küçük dalgalar.

**Görünüm (şader'siz, `RenderType.translucent` benzeri kendi render tipimiz).**
- *Renk:* biyom su rengi (vanilla ile uyumlu), derinliğe göre: `d < 1` blok neredeyse saydam (alfa 0,25, zemin görünür), `d = 4` alfa 0,65, `d ≥ 10` alfa 0,85 ve koyu ton; köşe renginde önceden hesaplanır.
- *Doku:* vanilla su dokusunu kullanmayız; kendi 32×32 animasyonlu dokumuz (16 kare, yumuşak dalgacık), iki katman farklı yönlerde kayar — vanilla'dan ayırt edilemesin diye renk ve parlaklık vanilla suya eşlenir.
- *Parlama:* güneş/ay yönüne göre köşe başına sahte yansıma (normal·yarım vektör)^32 ile beyaza karıştırma; gece yıldız/ay parlaması soluk.
- *Köpük:* (1) kıyı şeridi: `d < 0,6` olan köşelerde köpük dokusu karışımı, 2 sn periyotla ileri-geri "yalayan" bant; (2) dalga tepesi: Gerstner diklik ölçütü eşiği geçtiğinde; (3) engel köpüğü: blok ya da varlık çevresinde 1 blok halka.
- *Yan yüzler:* sanal su havayla dikey sınır yaptığında (dalga cephesi, mağara cebi kenarı) dikey yüzey çizilir, üst kenarı köpüklü; gerçek suyla birleştiği yerde yüz çizilmez, yüzey gerçek suyun üstüne 0,02 blok bindirilir (titreme önlenir).
- *Su altı görünümü:* kamera sanal suyun içindeyse sis vanilla su altı sisine eşlenir (`ViewportEvent.RenderFog` ve `ComputeFogColor`), yükselen kabarcıklar (`BUBBLE`), yüzeyin altı aşağıdan da çizilir (çift yüz).

**Etkileşim (sunucu, madde 8).** Her tick yakın varlıklar için: varlığın sütunundaki seviyeye göre batıklık oranı → kaldırma (yukarı ivme), sürükleme (hız ×0,8), zıplama tuşuyla yükselme, baş su altındaysa hava azalır ve boğulma hasarı, ateş söner, ıslaklık bayrağı (sıcaklık sistemi okur). Mob'lara aynı kurallar; su mob'ları için sanal su yaşanabilir sayılmaz (boğulmaz ama spawn etmez). İstemci tahmini aynı kodla çalışır, sunucu otoritedir.

**Mağara suyu yaşamı.** Dolma (tsunami sırasında; yüzey cebin dibinden yukarı 2 blok/sn) → bekleme (config, varsayılan 10 dk) → buharlaşma: yüzey her 6 sn 0,25 blok iner; yüzeyden beyaz buhar (yüzey alanı başına %4), çekilen suyun bıraktığı duvarlarda 30 sn kalan koyu "ıslak şerit" (istemci decal, blok değişmez). Bitişte cep silinir; dünya bayt bayt eski hali.

**Deniz yükselmesi görünümü.** 5 dk boyunca seviye doğrusal artar (sarı kum → ıslak koyu kum → su altı); kıyı köpüğü ilerleyen çizgiyi izler, ilk kez su altında kalan çimen üzerinde 10 sn küçük kabarcık. Sanal su ile kaplı alanda çimen, çiçek ve ekin görsel olarak su altındadır (gerçekte değişmez).

## 3. Vitrin anı

**Deniz yükselmesinin bir kıyı köyünü yaladığı 10 saniye, gün batımı (ay yok, yıldızlar çıkıyor).** Kamera iskelenin ucunda, Y+3, kıyıya dönük (pitch −8°). Görüntüde: suyun altında kalmış plaj ve yarısına kadar suya gömülmüş ahşap iskele, ilerleyen köpük çizgisi ve arkasındaki ıslak koyu kum bandı, sığ suyun altında görünen çimen ve çiçekler, yüzeyde gün batımı parlaması şeridi. Tsunami vitrini `tsunami.md`'dedir.

## 4. Ses tasarımı

- Kıyı çizgisinde yalama döngüsü (sentez: 0,5 Hz zarflı pembe gürültü + kısa köpük cızırtısı), en yakın 3 kıyı noktasından konumsal; düzey çok düşük.
- Sanal suya girme/çıkma: vanilla `entity.player.splash`, `entity.generic.swim`; su altında `ambient.underwater.loop` + alt geçiren.
- Mağara suyu: yankılı damlalar (`block.pointed_dripstone.drip_water_into_cauldron`), buharlaşmada hafif tıslama.

## 5. Performans planı

| Kalite | En ince hücre | LOD halkaları | Gerstner bileşen | Köpük | Su altı kabarcık | Sanal su görüş mesafesi |
|---|---|---|---|---|---|---|
| Düşük | 1 | 1 / 4 | 1 | yalnız kıyı | az | 8 chunk |
| Orta | 1 | 1 / 2 / 4 | 2 | kıyı + tepe | orta | 12 |
| Yüksek | 0,5 | 0,5 / 1 / 2 / 4 | 3 | hepsi | orta | görüş mesafesi |
| Ultra | 0,25 | 0,25 / 0,5 / 1 / 2 / 4 | 4 | hepsi | çok | görüş mesafesi |

- **Batch:** her 4×4 chunk karosu için bir `VertexBuffer`; deniz yükselmesinde dalgalar küçük olduğundan karolar 4 karede bir yeniden doldurulur, yalnız kameraya yakın 2 karo her karede. Frustum dışı karolar ne hesaplanır ne çizilir; gerçek araziyle örtüşen ve tamamen görünmeyen (altı kapalı) hücreler atlanır.
- **Sunucu:** yalnız oyuncu çevresindeki (±4 chunk) yüklü chunk'ların su alanı hesaplanır; BFS chunk başına bir kez, sonra önbellek. Varlık kontrolü O(varlık), sütun başına dizi okuması. Blok değişimi (oyuncu kazarsa) yalnız o chunk'ın önbelleğini geçersiz kılar.
- **Ağ:** deniz yükselmesi için tek sayı (`artis`, başlangıç tick'i, süre); istemci alanı kendisi türetir. Mağara cepleri chunk takibine göre (oyuncu chunk'ı görünce) sıkıştırılmış bit kümesi olarak bir kez.

## 6. Açık noktalar

| Değer | Varsayılan |
|---|---|
| Deniz yükselmesi | 4 blok, üst sınır 6, 5 dk'da, kalıcı (geri dönüş seçeneği: 3 günde) |
| Yükselmenin karaya yayılma sınırı | gerçek suya bağlı olmayan çukurlar dolmaz; gerçek su yüzeyinden en çok 96 blok |
| Mağara suyu hacim tavanı | tsunami başına 20 000 blok, cep başına 4 000 |
| Mağara bekleme / kuruma | 10 dk / 0,25 blok her 6 sn |
| Boğulma | vanilla hava süresi ve hasarı |

- **Teknik risk:** vanilla yüzme animasyonu, `isInWater()`, `isSwimming()` ve hava kaybı gerçek sıvı bloklarından hesaplanır. Olay ile yetinilebilen kısımlar: hareket (kendi ivmemiz), hava (`LivingBreatheEvent` [Forge 1.20.1'de varlığı doğrulanmalı]), ıslaklık (kendi bayrağımız). Yüzme pozu ve mob yapay zekâsının suyu tanıması için `Entity#updateFluidHeightAndDoFluidPushing`'e küçük bir mixin gerekebilir (spike 1, gerekçe KARARLAR'a).
- Sanal suyun gerçek suyla birleştiği yüzeyde ışık ve sis çakışması olabilir; bindirme ofseti spike'ta ayarlanır.

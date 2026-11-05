# Deprem — tasarım notu (bölüm C, madde 10, 13b)

## 1. Gerçek olay

- **Yüzey kırığı.** 6 Şubat 2023 Kahramanmaraş depremlerinde (Mw 7,8 ve 7,5) yüzey kırığı toplam ~460 km boyunca izlendi; sol yanal doğrultu atımlı fayın tipik izleri: **kademeli (en échelon) çatlaklar** — ana hatta çapraz, birbirine paralel, kısa yarıklar; **basmış sırtlar (mole track)** — fay üzerinde kabarmış, parçalanmış toprak şeridi; **ayrılma havzaları (pull-apart)** — fayın sıçradığı yerde çökmüş çukurlar; yolları, çitleri, ağaç sıralarını metrelerce yana kaydıran atım; yer yer dikey **fay basamağı (scarp)**.
- Kırık bir noktadan başlayıp fay boyunca saniyede kilometrelerce **ilerler**; yarık bir anda değil, bir yöne yayılarak açılır.
- **Ses:** büyük depremlerden önce/sırasında derin bir gürleme ve güm sesi duyulur; P dalgası sarsıntıdan kısa süre önce gelir, çoğu enerji insan duyma sınırının altındadır.
- **Hasar deseni:** ahşap çatılar, bacalar, gevşek dış kaplamalar ve ağaç dalları önce düşer; yamalarda heyelan, su kenarlarında sıvılaşma (kum kaynaması); sarsıntı sonrası havada aslı toz bulutu.

Kaynaklar:
- https://www.usgs.gov/media/images/preliminary-observations-surface-rupture-2023-turkey-earthquake-sequence
- https://geerassociation.org/components/com_geer_reports/geerfiles/GEER_2023_Turkey_Earthquake_FullReport_ReducedSize.pdf (460 km kırık, tipik doğrultu atımlı morfoloji)
- https://www.usgs.gov/data/field-observations-surface-rupture-and-fault-displacement-2023-mw-78-pazarcik-turkiye
- http://news.berkeley.edu/2018/01/12/transcript-berkeley-seismologists-capture-earthquakes-rumble (deprem sesi)

## 2. Minecraft'ta gösterim

**Fay hattının geometrisi (sunucu).** Merkez etrafında bir doğrultu açısı seçilir; hat, 8–16 blokluk **segmentlerden** oluşan bir çoklu çizgidir; segmentler arasında 1–3 blok yanal sıçrama (kademeli görünüm), sıçrama noktalarında küçük ayrılma çukuru. Yarık profili: genişlik ve derinlik merkezde tam, uçlara doğru `sin(π·u)^0.7` ile incelir; duvarlar düz değil, her derinlik katmanında ±1 blok rastgele çıkıntı (pürüzlü kaya yüzü), dipte kırık taş ve çakıl molozu. Hattın iki yanında 2–4 blok uzaklıkta 45° açılı kısa yan çatlaklar (1 blok geniş, 1–2 derin). S4–5'te hattın bir yakası 1–2 blok alçalır (fay basamağı).

**İlerleyiş.** Yarık merkezden iki uç yönüne 6–12 blok/sn hızla açılır. Her blok sütunu üç karede değişir: (1) yüzeyde çatlak dokusu (kırılma aşaması örtüsü, `destroyBlockProgress` ile 0→9), (2) üst 2 blok `FallingBlockEntity` olarak yarığa düşer, (3) alt kısım katman katman 2 tick arayla boşaltılır. Boşaltılan bloğun %20'si yarığın dibine moloz olarak geri bırakılır (toplam hacim korunuyormuş hissi).

**Toz ve parçacıklar.** Açılan her sütunda yarıktan yukarı fışkıran bloğun rengini taşıyan toz (`BlockParticleOption` ile `BLOCK` parçacığı, 6–10 adet) ve 8–14 blok yükselen gri-bej toz bulutu sprite'ları (kendi dokumuz, 20–40 sn asılı kalır, yavaşça rüzgâr yönüne kayar). Etki alanında zeminden sürekli küçük toz ve çakıl sıçramaları.

**"Sağlam olmayan" blokların yıkılması.** Etki yarıçapında rastgele sırayla: yapraklar düşer (yaprak kırılma parçacığı + `FallingBlockEntity` olarak kısa düşüş, yere çarpınca eşya/yok olur), ahşap kalas/merdiven/çit, cam, fener, saksı gibi bloklar yerinden kopar ve düşer; desteği kalmayan çatı parçaları çöker. `dogalafetler:deprem_dayanikli` (taş, deepslate, obsidyen, tuğla) yalnız yarık hattında kırılır. Yamalarda S4–5 küçük heyelan: eğimi 45°'den dik toprak/çakıl/kum yüzeyleri kayar.

**Ekran sarsıntısı (istemci, oyuncu başına).** `ViewportEvent.ComputeCameraAngles` ile iki bileşen: hızlı titreme (8–14 Hz, iki bağımsız Perlin kanalı yaw/pitch) + yavaş yuvarlanma salınımı (1,2–2 Hz, roll). Genlik `A = A_seviye · clamp(1 - d/R, 0.15, 1)` — uzaktaki oyuncu hafif titrer, yakındaki sallanır. Zarf: 1,5 sn öncü titreme (%20), sonra ana sarsıntı, sonda 3 sn sönüm. El eşyası (`RenderHandEvent`) ayrıca ±2 piksel sallanır. Erişilebilirlik için CLIENT config'te sarsıntı çarpanı (0–200 %).

**Afet sonrası iz.** Kalıcı yarık, iki yanında çatlak ve moloz, devrik yapraksız ağaçlar, 2 dk boyunca yarıktan süzülen ince toz, artan küçük artçı sarsıntılar (S3+: 1–3 adet, ana sarsıntının %15'i, 20–60 sn arayla; blok yıkmaz).

**Seviye 1 ile 5 farkı.** S1 (4,0): 24 blokluk, tek blok geniş sığ bir çatlak, birkaç yaprak düşer, kamera hafifçe titrer. S5 (9,0): 256 blok boyunca araziyi ikiye bölen, 6 blok geniş, 32 blok derin pürüzlü kanyon; bir yakası basamak oluşturup alçalmış, iki yanında kademeli çatlaklar, ahşap köy çökmüş, yamalar kaymış, ufku kaplayan toz perdesi.

## 3. Vitrin anı

**Yarığın oyuncuya doğru koştuğu 6 saniye.** Kamera yarık hattının uzantısında, 30 blok ileride, Y+6'dan hatta bakar (pitch −20°). Görüntüde: zemin uzaktan yaklaşan bir fermuar gibi açılıyor, yarığın ağzından bloğun renginde toz fışkırıyor, yarığa düşen çimen-toprak blokları havada, sağda bir ahşap evin çatısı çöküyor, solda ağaçlardan yaprak yağıyor, kamera titrediği için kadraj hafif eğik. Küçük resim için: yarık kadrajın ortasından ufka uzanan karanlık bir çizgi, iki yanında toz sütunları.

## 4. Ses tasarımı

- **Öncü gürleme (sarsıntıdan 1,5 sn önce):** 25–60 Hz, yükselen "tren yaklaşıyor" gürlemesi. Oyuncunun duyduğu ilk şey budur (P dalgası hissi).
- **Ana katman:** derin gürleme döngüsü (sentez, 20–80 Hz + 200 Hz'e kadar kırık kaya çıtırtısı), mesafeyle ses düzeyi düşer, alt geçiren filtre uzakta daha boğuk.
- **Nokta sesler:** yarık açılırken sütun başına kaya kırılması (`block.stone.break`, `block.deepslate.break`, pitch 0,6–0,8), yıkılan ahşap (`block.wood.break`), cam (`block.glass.break`), ahşap gıcırtısı (`block.wooden_door.close` pitch 0,5, uzatılmış). Aynı anda en çok 8 nokta ses.
- **Sonrası:** yarığa düşen çakıl sızması (`block.gravel.fall`), uzak köpek havlaması yok (vanilla dışı içerik eklemiyoruz).
- Gürleme enerjisinin çoğu 100 Hz altında olduğu için konuşmayı bastırmaz; 300–3400 Hz bandı 6 dB oyuk.

## 5. Performans planı

| Kalite | Toz sprite (asılı) | Blok parçacığı/sütun | `FallingBlockEntity` tavanı | Heyelan | Artçı toz |
|---|---|---|---|---|---|
| Düşük | 120 | 2 | 20 | yok | yok |
| Orta | 300 | 4 | 40 | S5 | var |
| Yüksek | 600 | 8 | 60 | S4–5 | var |
| Ultra | 1200 | 12 | 100 | S4–5 | var |

- Sunucu: yarık kuyruğu sütun sütun; tick başına en çok 64 blok değişimi (S5), TPS < 18'de yarı hız. Blok değişimleri `Block.UPDATE_CLIENTS` ile, komşu fizik güncellemesi yalnız yarık kenarında. `FallingBlockEntity` tavanı aşılırsa blok doğrudan kaldırılır ve yalnız parçacık gönderilir.
- İstemci: toz sprite'ları tek toplu çizim; 96 blok ötesinde blok parçacığı üretilmez, yalnız toz bulutu.
- Sarsıntı tamamen istemcide hesaplanır; sunucu yalnız başlangıç anı, merkez, seviye ve süreyi bir kez gönderir.

## 6. Açık noktalar — önerilen varsayılanlar

| Seviye | Richter hissi | Yarık uzunluğu | Genişlik | Derinlik | Sarsıntı süresi | Kamera genliği (yakın) | Yıkım yarıçapı | Hasar |
|---|---|---|---|---|---|---|---|---|
| 1 | 4,0 | 24 | 1 | 4 | 8 sn | 0,3° | 16 | yok |
| 2 | 5,5 | 48 | 2 | 8 | 15 sn | 0,8° | 32 | düşen blok |
| 3 | 6,5 | 96 | 3 | 14 | 25 sn | 1,5° | 48 | + yarığa düşme |
| 4 | 7,5 | 160 | 4 | 22 | 40 sn | 2,5° | 72 | + 0,5 can/2 sn yakın alanda |
| 5 | 9,0 | 256 | 6 | 32 | 60 sn | 4,0° | 100 | + 1 can/2 sn yakın alanda |

- Yarık derinliği yüzeyden ölçülür, en alt Y = −50 (ana kaya ve deepslate tabanı korunur).
- Taşıyıcı blokların yıkılması yalnız `minecraft:planks`, `logs` dışı ahşap, `glass`, `leaves`, `fences` ve `dogalafetler:deprem_kirilgan` etiketiyle.
- Öncü sarsıntı (sismograf) çok hafif titreme: 0,1°, 0,5 sn, 6–10 sn arayla; yalnız eşyayı taşıyanda.

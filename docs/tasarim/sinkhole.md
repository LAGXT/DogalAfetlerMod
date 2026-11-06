# Sinkhole (Çöken Çukur) — tasarım notu (bölüm F, madde 10, 13b)

## 1. Gerçek olay

- **İki tür.** Örtü-çökme (cover-collapse) sinkhole'ları, kireçtaşı içinde oluşmuş boşluğun üstündeki kum-kil örtüsünün **aniden** çökmesiyle oluşur; örtü-oturma (cover-subsidence) türü yavaşça çanak biçiminde oturur. Oyundaki afet ilk türdür, ama çökmeden önceki saniyelerde ikincinin belirtilerini taşır.
- **Öncü belirtiler (Florida Jeoloji Kurumu):** toprakta çatlaklar, zeminin sarkması ve oturması, normalde görünmeyen kayanın açığa çıkması, su birikintisi, kuruyan bitki örtüsü, **daha önce dik duran ağaçların, çitlerin ve yapıların eğilmesi**, kapı ve pencerelerin sıkışması, gıcırtı ve çıtırtı sesleri, kaldırımların ayrılması.
- **Çökme geometrisi:** ilk çökme dik duvarlı silindir/huni biçimindedir; ardından kenarlar desteksiz kalıp **iç içe halkalar hâlinde** geriye doğru köşelenir ("kenarlar çökebilir, yaklaşmayın"), basamaklı bir çanak kalır; zemin yüzeyinde çukura eş merkezli çatlak halkaları görülür. Dipte çoğunlukla yeraltı suyu birikir.

Kaynaklar:
- http://publicfiles.dep.state.fl.us/FGS/FGS_Publications/L/L20.pdf (FGS Leaflet 20: türler, belirtiler, "kenarlar çökebilir")
- https://www.usgs.gov/media/images/cover-collapse-sinkholes-can-open-suddenly
- https://www.usgs.gov/water-science-school/science/sinkholes
- https://linkinghub.elsevier.com/retrieve/pii/S0169555X18302071 (örtü-çökme morfolojisi ve evrim modeli)

## 2. Minecraft'ta gösterim

**Zaman çizelgesi (S3: çap 16, derinlik 16):**

| An | Aşama | Görünüm |
|---|---|---|
| −10–0 sn | Siren | — |
| 0–4 sn | **Titreme ve çatlama** | Merkezden dışa 3 eş merkezli halka (r = 0,35R, 0,7R, 1,05R) üzerinde çatlak örtüsü (`destroyBlockProgress` aşama 0→6), halkaları radyal çatlaklar birleştirir; hafif kamera titremesi; çimen üzerinde toz fışkırması |
| 4–7 sn | **Sarkma** | Merkez çapın %40'ı 1 blok oturur (üst blok bir altına taşınır), ağaçlar ve çit direkleri merkeze doğru 10–20° eğilir (`BlockDisplay` benzeri eğik görsel kopya, aşağıda), su varsa merkezde çıkar |
| 7–10 sn | **Çekirdek çöküşü** | İç halka (r < 0,5R) tek parça hâlinde düşer: sütunlar 0,6 sn içinde aşağı kayan blok kütleleri olarak görünür, üstündeki ağaç, çit, hayvan, oyuncuyla birlikte; boşluktan yukarı toz patlaması |
| 10–16 sn | **Basamaklı geri çekilme** | Dış halkalar sırayla (0,7R sonra R) 1,5 sn arayla köşelenir: halkanın üst katmanı içe devrilir, çukura kayan bloklar dibe yığılır; her halka bir basamak bırakır |
| 16–30 sn | **Durulma** | Kenarlardan tek tek düşen bloklar ve çakıl sızması, dipten yükselen toz, kenarda asılı kalan çimen saçakları (kökleri olan çimen blokları bilerek 1 blok içe taşan bırakılır) |

**Çöküşün görselliği.** Tek tek `FallingBlockEntity` yerine çekirdek çöküşü **tek bir "çöken kütle" varlığı** ile çizilir: sunucu çökecek bölgenin blok durumlarını (en üst 4 katman) bir kez paketler, blokları hemen kaldırır; istemci bu kütleyi önbellekli tek ağ olarak (sahte blok varlığı, chunk mesh'i gibi derlenmiş `VertexBuffer`) yerçekimiyle düşürür, düşerken dış kenarları 2–4° içe yatırır ve parçalara ayırır (kütle 4–8 dilime bölünüp hafif farklı hızla düşer). Dibe ulaşınca sunucu dipte moloz katmanını (toprak, çakıl, kırıktaş, devrik kütük) gerçek blok olarak yerleştirir — "bloklar çukura dolsun" bu yığındır. Bu yöntem 16 çaplı çukurda ~800 varlık yerine 1 varlık çizer.

**Eğilen ağaçlar.** Sarkma aşamasında kenar halkasındaki ağaçların blokları istemcide eğik görsel kopya olarak çizilir (gerçek bloklar o sürede istemci tarafında gizlenir), çekirdek çökerken çukura devrilirler. Gerçek bloklar çöküş sonunda yeni konumda (dipte, yatay kütük) yerleşir.

**Kenar ve dip.** Kenar duvarları dik ama pürüzlü (±1 blok); üst 1–2 katman toprak/kum, altı taş; S4–5'te dipte açığa çıkan mağara boşluğuna açılan karanlık bir ağız (dip merkezinde 2–3 blokluk ek çukur, gerçek kireçtaşı boşluğunu çağrıştırır). Dipte su: gerçek su koymayız (harita), yalnız dipte 20 sn süren nem buharı/damla parçacığı.

**Kamera.** Çatlama aşamasında 0,2° titreme, çekirdek çöküşünde 32 blok içinde tek sert sarsma (0,6 sn, 1,5°·seviye/3) ve aşağı doğru kısa pitch çekimi (−1,5°), çukura düşen oyuncuda FOV 6° genişler.

**Afet sonrası iz.** Kalıcı basamaklı çukur, kenarın 1–3 blok dışında çatlak halkası (çimen → kaba toprak), dipte moloz ve devrik ağaç, 60 sn boyunca aralıklı kenar dökülmesi.

**Seviye 1 ile 5 farkı.** S1: 6 blok çaplı, 6 derin, tek adımlı dik bir delik; çatlaklar yalnız bir halka. S5: 34 blok çaplı, 32 derin, üç basamaklı dev bir çanak; ağaçlar ve bir ev parçası içine devrilmiş, dipte mağaraya açılan karanlık ağız, kenarda asılı çimen saçakları, çukurdan yükselen toz sütunu.

## 3. Vitrin anı

**Çekirdek çöküşü ve ilk halka (8 saniye).** Kamera çukurun dış kenarından 6–10 blok geride, Y+10'da, merkeze 35–45° aşağı bakar. Görüntüde: çukurun tam ortası tek parça hâlinde aşağı kayıyor, üstündeki ağaç dik hâlde birlikte iniyor, bir inek havada, dış halka içe devrilmeye başlamış; ön planda zeminde kameraya kadar uzanan eş merkezli çatlaklar; boşluktan yukarı toprak renginde toz patlıyor. Küçük resim için: tam yukarıdan (pitch −80°) çekilmiş ikinci kare — kusursuz dairesel karanlık çukur, çevresinde çatlak halkaları.

## 4. Ses tasarımı

- **Çatlama:** kuru çıtırtılar ve kaya "pop" sesleri (sentez: kısa band geçiren darbeler 800–2500 Hz, rastgele aralık), altında çok alçak gürleme (30–60 Hz). Ahşap yapı varsa gıcırtı (`block.wooden_door.close` pitch 0,5 uzatılmış).
- **Çöküş:** derin, boğuk güm + uzun kum/çakıl akması (`block.gravel.fall`, `block.sand.fall` katmanlı), çukurun içinden gelen boşluk yankısı (yankılı alt geçiren, `ambient.cave` kısa). Konumsal; çukur merkezinden.
- **Durulma:** seyrek tek blok düşüşleri, çakıl sızması.
- Tepe anı yalnız 1–2 sn sürer; ortam katmanı konuşma bandında oyuklu.

## 5. Performans planı

| Kalite | Çöken kütle dilimi | Toz sprite | Kenar dökülmesi (`FallingBlockEntity`) | Eğilen ağaç görseli |
|---|---|---|---|---|
| Düşük | 1 (tek parça) | 100 | 8 | yok (doğrudan devrilir) |
| Orta | 4 | 250 | 16 | var |
| Yüksek | 6 | 500 | 30 | var |
| Ultra | 8 | 900 | 50 | var |

- Çöken kütle ağı bir kez derlenir (istemcide 1–2 kare), sonra yalnız dönüşüm matrisi değişir.
- Sunucu blok değişimleri: halka halka kuyrukta, tick başına en çok 400 blok kaldırma (`setBlock` bayrağı ışık güncellemesini tek seferde toplar), TPS < 18'de aşama süreleri %50 uzar ama sıra korunur.
- S5 çukur ~29 000 blok; 10 sn'lik çöküş aşamasına yayılınca tick başına ~145 blok.

## 6. Açık noktalar — önerilen varsayılanlar

| Seviye | Çap | Derinlik | Çatlama süresi | Çöküş süresi | Basamak | Düşme hasarı dışı hasar |
|---|---|---|---|---|---|---|
| 1 | 6 | 6 | 4 sn | 3 sn | 1 | yok |
| 2 | 10 | 10 | 4 sn | 4 sn | 1 | yok |
| 3 | 16 | 16 | 5 sn | 6 sn | 2 | düşen blok 2 can |
| 4 | 24 | 24 | 6 sn | 8 sn | 2 | düşen blok 3 can |
| 5 | 34 | 32 | 7 sn | 10 sn | 3 | düşen blok 4 can |

- Yer seçimi: hedef oyuncudan 12–40 blok uzakta, yüzeyi çoğunlukla doğal blok olan, yüksekliği çap boyunca ±4 blok düz bir alan; 8 deneme başarısızsa başka oyuncu.
- Derinlik yüzeyden ölçülür, Y = −50 altına inmez; çukur gerçek bir mağaraya denk gelirse o boşluğa bağlanır (ek kazı yapılmaz).
- İlk 4 sn'lik çatlama aşaması oyuncuya kaçma fırsatı verir; "Delikten Dönüş" başarımı için çekirdek alanındaki oyuncu işaretlenir.

## 7. Uygulama notu (Faz 4)

Uygulanan: beş alt aşama (çatlama, sarkma, çekirdek çöküşü, basamaklı geri çekilme, durulma; `afet.sinkhole.mantik`),
eş merkezli + radyal çatlak örtüleri, merkezin %40'ının bir blok oturması, pürüzlü duvarlı halkalı basamaklı çukur (S4–5 dipte
mağara ağzı), dibe moloz, kenar çatlak halkası (çimen → kaba toprak), aralıklı kenar dökülmesi, çöküş gümü
(`sinkhole_cokus.ogg`) ve toz patlaması, istemcide 0,2° titreme + tek sert sarsma + aşağı bakış çekimi + düşen oyuncuda 6° FOV,
"Delikten Dönüş" ipucu. Sapma (`.vibe/kararlar/deprem-sinkhole.md` DS-02): tek "çöken kütle" varlığı yerine halkanın içi önce
görünmeden boşaltılır, sonra yüzey ve üstündekiler aynı tick'te tavanlı `FallingBlockEntity` olarak düşer (tavanı aşan
bloklar parçacıkla kalkar); eğilen ağaç görseli yok, ağaçlar dik düşer. **Çöken kütle varlığı ve eğilen ağaç: Faz 5 cilası,
performans gerekçesi (şef kararı, tur 2).** Derlenmiş `VertexBuffer`'lı özel varlık, istemcide blokları gizleyen eğik kopya ve
dilimli düşüş, S5 çukurunda (~29 000 blok) ağ ve bellek maliyetini ölçülmeden taşıyamaz; tavanlı `FallingBlockEntity` yolu
TPS ve FPS ölçüsüyle sınanmıştır (rapor). Bu turda yapılmayanlar (DS-14): dipte nem buharı, ayrı `sinkhole_catlama.ogg`
(vanilla çıtırtı + ortak gürleme döngüsü kullanılıyor).

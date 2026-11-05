# Orman Yangını — tasarım notu (bölüm G, madde 6, 7, 10)

Yangın mantığı ortak `yangin` yöneticisindedir (meteor ateşleri de aynı sistem). Bu not hem ortak yangının görsel dilini hem orman yangını afetinin koreografisini tanımlar; meteor ateşleri aynı görselleri kullanır.

## 1. Gerçek olay

- **Yangın türleri:** yüzey yangını (çimen, çalı, ölü örtü; alçak, hızlı ilerleyen alev şeridi), **taç (crown) yangını** (alevler ağaç tepelerine sıçrar, tepeden tepeye koşar; en yüksek ve en görünür alevler), **kıvılcım sıçraması (spotting)**: yanan parçalar rüzgârla cephenin önüne taşınıp yeni odaklar açar.
- **Cephe biçimi:** rüzgâr yönünde uzamış elips; baş (head) hızlı ve yoğun, kanatlar yavaş, sırt (heel) zayıf.
- **Duman sütunu:** duraysal olmayan havada dikey yükselen sütun; tepesinde beyaz, karnabahar biçimli **pirokümülüs** bulutu (yoğuşma). Duraysal havada duman yatay yayılır ve vadilerde birikir. Duman rüzgâr altında güneşi turuncuya boyar.
- **Sonrası:** siyah kül, kömürleşmiş düz gövdeler, beyaz kül lekeleri, saatlerce tüten kütükler.

Kaynaklar:
- https://fs-prod-nwcg.s3.us-gov-west-1.amazonaws.com/s3fs-public/publication/pms-437.pdf (NWCG PMS 437: duman sütunu ve duraysal/duraysal olmayan hava, yangın davranışı gözlemi)
- https://fs-prod-nwcg.s3.us-gov-west-1.amazonaws.com/s3fs-public/publication/pms437-1.pdf (alev boyu, yangın biçimi)
- https://semspub.epa.gov/work/08/1772063.pdf (konveksiyon sütunu, pirokümülüs)

## 2. Minecraft'ta gösterim

**Ortak yangın hücresi.** Yangın yöneticisi yanan konumları kendi listesinde tutar ve vanilla ateş bloğunu (`minecraft:fire`) yerleştirir — vanilla ateş görseli, ışığı ve hasarı bedava gelir; vanilla'nın kendi yayılması bizim yöneticimizin yerine geçmesin diye ateşler `doFireTick`'ten bağımsız olarak bizim kuyruğumuzla söndürülür/yayılır [spike'ta doğrulanacak: vanilla ateş bloğunun kendi rastgele tick'i de yayılır; gerekirse `dogalafetler:afet_atesi` adlı vanilla görünümlü ama kendi tick'i olmayan ateş bloğu]. Yanıcılık vanilla `FireBlock` yanıcılık değerinden gelir (N12).

**Alev katmanları (istemci, yanan hücre başına).**
1. Vanilla ateş bloğu.
2. **Büyük alev dilleri:** yaprak ve kütük yanarken hücre üstünde 1,5–3 blok yüksekliğinde, 8 karelik animasyonlu alev sprite'ı (kendi dokumuz, turuncu-sarı çekirdek, kızıl uç, additive karışım); taç yangınında ağacın üstünde birleşik 4–6 blokluk alev perdesi. Alevler rüzgâr yönüne 15–30° yatık.
3. **Kıvılcımlar:** yukarı sarmal çizerek yükselen parlak turuncu noktalar (`LAVA` benzeri ama küçük, 2–4 sn ömür); rüzgâr altı yönde 3–12 blok uzağa taşınan "ateş kırıntıları" (spotting) — bunların bir kısmı sunucuda gerçek yeni odak olur, istemci aynı yörüngeyi çizer.
4. **Duman:** hücre başına `CAMPFIRE_SIGNAL_SMOKE` benzeri koyu gri duman, yangın kenarında daha açık (taze yanma), merkezde koyu.
5. **Duman sütunu + pirokümülüs (afet başına tek):** yangın alanının ağırlık merkezinde, 40–90 blok yükselen, tepeye doğru genişleyen sütun (büyük yarı saydam duman sprite'ları yığını, alttan turuncu aydınlatılmış); tepesinde beyaz, karnabahar biçimli bulut kapağı (S3+). Sütun rüzgâr yönüne 10° eğilir; kilometrelerce uzaktan yangının yerini gösterir.

**Ortam.** Yangına 48 blok içinde sis rengi turuncu-kahverengi (`#7A5236`), görüş %55; güneş diski turuncuya boyanır (gokyuzu renderer'ına "duman örtüsü" parametresi). 24 blok içinde ekranda hafif ısı dalgalanması (şader yok: kamera yaw/pitch'ine 0,05°'lik 3 Hz sinus + alt kenarda yukarı kayan yarı saydam ısı dokusu). Ortam sıcaklığı yanan hücre sayısı ve uzaklıkla artar (madde 7).

**Afet koreografisi.** Başlangıç: hedef oyuncunun 40–90 blok uzağında orman/çayır biyomunda bir başlangıç alanı (S'ye göre yarıçap). Rüzgâr yönü afet başında seçilir ve tüm afet boyunca sabit kalır (±20° sapma); yayılma olasılığı rüzgâr yönünde ×2,5, karşı yönde ×0,3 → elips cephe kendiliğinden oluşur. Yağmur yağınca yayılma durur, hücreler 10–30 sn içinde söner (yoğun buhar + `block.fire.extinguish`).

**Afet sonrası iz.** Yanan kütüklerin %30'u yok olur, kalanları vanilla "soyulmuş koyu meşe"/"soyulmuş mangrov" gibi en koyu soyulmuş kütüğe (kömürleşme görünümü) döner; çimen → kaba toprak, çiçek/çimen kaybolur; yer yer kül izi için `ASH` ve `WHITE_ASH` parçacıkları 3 dk boyunca süzülür; 60 sn boyunca bazı kütüklerden tüten ince duman. Yanmış alan uzaktan kahverengi-siyah bir yara olarak okunur.

**Seviye 1 ile 5 farkı.** S1: birkaç ağaç ve çimenlikte yerel, alçak alevli yüzey yangını, ince gri duman. S5: 24 blok yarıçapla başlayan, rüzgâr altı yönde 12 blok kıvılcım sıçratan taç yangını; ağaç tepelerinde birleşik alev perdeleri, 90 blok yükselen duman sütunu ve tepesinde beyaz pirokümülüs, turuncuya boyanmış gökyüzü.

## 3. Vitrin anı

**Taç yangınının bir tepeden diğerine sıçradığı 8 saniye, alacakaranlıkta.** Kamera yangın cephesinin rüzgâr üstü yanında, bir tepede, 30–50 blok uzakta, cepheyi yandan görerek (pitch −5°). Görüntüde: ön planda kararmış gövdeler ve kül, ortada ağaç tepelerinde yatık turuncu alev perdeleri, havada rüzgârla taşınan kıvılcım yayları, sağda yeni tutuşan bir ağaç, arka planda alttan turuncu aydınlanmış dev duman sütunu ve tepesinde beyaz pirokümülüs; gökyüzü kahverengi-turuncu. Gece çekimi alevlerin ışığını (vanilla ateş ışığı 15) en iyi gösterir.

## 4. Ses tasarımı

- **Yakın (<12 blok):** vanilla `block.fire.ambient` (yanan hücre başına değil, en yakın 4 hücreden), + kendi "çıtırtı" katmanımız (rastgele kısa darbeler, 1–4 kHz, düşük düzey) ve kütük patlamaları (seyrek "pop", `block.wood.break` pitch 1,3).
- **Orta (12–64 blok):** yangın uğultusu — alevlerin "kişneme/uğuldama" sesi (40–200 Hz pembe gürültü, 0,5 Hz dalgalanma), düzeyi yanan hücre sayısının kare köküyle artar, tavanı sabit.
- **Rüzgâr:** yangının çektiği hava; `item.elytra.flying` çok düşük düzey.
- **Yağmurla sönme:** `block.fire.extinguish` + buhar tıslaması.
- Kalabalık yangında ses tavanı: en çok 6 konumsal kaynak (kümelenmiş hücrelerin merkezlerinden), konuşma bandı oyuklu.

## 5. Performans planı

| Kalite | Büyük alev sprite (en yakın hücre) | Kıvılcım/sn | Hücre dumanı | Duman sütunu sprite | Isı dalgalanması |
|---|---|---|---|---|---|
| Düşük | 40 hücre | 30 | her 4 hücrede 1 | 40 | yok |
| Orta | 120 | 80 | her 2 | 90 | yok |
| Yüksek | 250 | 160 | hepsi | 160 | var |
| Ultra | 500 | 300 | hepsi + yoğun | 260 | var |

- **LOD:** 48 blok ötesinde hücre dumanı yerine 4×4 hücre kümesi başına tek büyük duman sprite'ı; 96 blok ötesinde yalnız vanilla ateş + duman sütunu. Kamera arkasındaki hücreler (frustum dışı) parçacık üretmez.
- **Sunucu (ortak yönetici, madde 6):** aktif yangın tavanı config (varsayılan 1500 hücre), tick başına yayılma denemesi tavanı (varsayılan 64), TPS < 18'de yarı, < 15'te dörtte bir; tavan dolunca en eski hücreler söndürülür. Yangın hücreleri chunk başına kovalarda; yalnız yüklü chunk'larda işlenir.
- Ağ: vanilla ateş blok değişimleri dışında paket yok; duman sütunu konumu ve rüzgâr 1 Hz.

## 6. Açık noktalar — önerilen varsayılanlar

| Seviye | Başlangıç yarıçapı | Yayılma çarpanı | Süre | Kıvılcım sıçraması | Taç yangını | Ortam sıcaklığı artışı (merkez) |
|---|---|---|---|---|---|---|
| 1 | 3 | ×0,5 | 2 dk | yok | yok | +4 birim |
| 2 | 6 | ×0,75 | 3 dk | 3 blok | yok | +6 |
| 3 | 10 | ×1,0 | 4 dk | 5 blok | var | +8 |
| 4 | 16 | ×1,4 | 6 dk | 8 blok | var | +11 |
| 5 | 24 | ×1,8 | 8 dk | 12 blok | var | +14 |

- Süre dolunca yeni tutuşma durur, mevcut hücreler 20–40 sn içinde söner (afet temiz biter).
- Sıcaklık birimi `sicaklik` sisteminin ölçeğine göre KARARLAR'da ayarlanır; buradaki sayılar göreli büyüklüktür.
- "Yanmadan Çıktım" için yangın alanı = yanan hücrelerin dışbükeyinin 8 blok genişletilmişi.

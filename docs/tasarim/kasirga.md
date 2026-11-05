# Kasırga — tasarım notu (bölüm A, madde 10)

## 1. Gerçek olay

Gözlemlenebilir özellikler (NWS Spotter Guide ve saha çalışmaları):

- **Yaşam döngüsü üç evre.** Gelişme: önce yerde bir **toz girdabı**, üstte bulut tabanından inen kısa bir huni; ikisi arasında görünmeyen ama dönen hava var. Olgun evre: huni neredeyse dikey, en tehlikeli an; büyük olanlar **kama (wedge)** görünümündedir. Sönme: arka kanat aşağı akımı (RFD) tabanı keser, huni eğilir, incelir ve yılan gibi kıvrılır (**ip / rope evresi**, bazen "akrep kuyruğu").
- **Kama ne demek (spike sonrası düzeltme, 2026-11-06).** Kama, genişliği yüksekliğine yaklaşan kalın bir gövdedir; tabanı tepesinden geniş bir koni değildir. Yoğuşma hunisi yukarı doğru genişleyip duvar bulutuyla birleşir; tabanı geniş gösteren şey yerde dönen enkaz bulutudur (etek).
- **Huni = yoğuşma.** Görünen huni basınç düşüşüyle yoğuşan sudur; nem azsa huni yere değmez, ama yerde dönen toz/enkaz bulutu yine kasırgadır. Yani taban halkası huniden bağımsız çizilmeli.
- **Duvar bulutu (wall cloud).** Huninin üstünde, ana bulut tabanından alçalmış, yavaşça dönen koyu bir disk; arkasında RFD'nin açtığı at nalı biçimli açık "temiz yarık".
- **Alt girdaplar (suction vortices).** Büyük kasırgalarda ana dolaşımın çevresinde dönen küçük, hızlı girdaplar; yerde **sikloidal izler** (iç içe halka biçimli hasar çizgileri) bırakırlar.
- **Dönüş yönü.** Kuzey yarımkürede kasırgaların büyük çoğunluğu saat yönünün tersine (siklonik) döner.
- **Enkaz bulutu.** Olgun evrede taban, huniyi aşağıdan saran geniş, kahverengi-gri bir enkaz kılıfıyla örtülür; büyük parçalar huninin dışına savrulur.

Kaynaklar:
- https://www.weather.gov/spotterguide/tor_life (gelişme / olgun / ip evresi, kama, uydu girdaplar)
- https://www.weather.gov/spotterguide/supercell2 (duvar bulutu, RFD)
- https://www.weather.gov/media/pah/Skywarn/EliteSpotterWorkshopSlidesSection3.pdf (alt girdaplar)
- https://par.nsf.gov/servlets/purl/10384234 (sikloidal izler, enkaz bulutunun evrimi)
- https://github.com/Corosauce/weather2/tree/1.20 (Weather2: huniyi eksen çevresinde halkalar hâlinde dönen parçacık sprite'larıyla çizer, kalkan blokları ayrı varlıklar olarak taşır)

## 2. Minecraft'ta gösterim

**Sunucu:** tek `KasirgaVarligi` (görünmez, `noPhysics`) konum, hız, yön, seviye, evre (`DOGUS`, `OLGUN`, `IP`, `DAGILMA`) ve evre ilerlemesini taşır; istemciye `SynchedEntityData` ile 4 Hz gider. Blok koparma, çekim ve hasar sunucuda.

**İstemci katmanları (aşağıdan yukarı):**

1. **Taban enkaz eteği.** Çap = huni gövdesinin taban çapının 1,8 katı, yükseklik 6–14 blok, kahverengi-gri, alfa 0,6–0,8 (tabanda yüksek, üst kenarda sönen); huninin tabanını sarar ve onunla birleşir, böylece silüet tabanda genişler. 4 karelik animasyonlu toz sprite'ı (kendi dokumuz), halka üzerinde açısal hız `ω(r) = ω0·(r0/r)` (dışa doğru yavaşlayan Rankine benzeri profil). Vitrin karesinde etek zorunludur.
2. **Huni gövdesi.** Prosedürel ağ: 24 dilim × 32 halka; her halkanın yarıçapı `r(h) = r_taban + (r_tepe - r_taban)·h^0.6`, `r_tepe` tabandan geniştir (S5'te ~1,6×) ve tepe duvar bulutuyla birleşir; ekseni gürültüyle hafifçe kıvrılır (tabanda en çok). S4–5'te gövdenin orta yükseklikteki genişliği yüksekliğin ~%40–50'sidir (kama görünümü). Doku yatayda periyodik bulut gürültüsü; U koordinatı zamanla kayar (dönüş hissi), iki katman farklı hızda ve ters fazda üst üste (derinlik). Yarı saydam, kenarlara doğru alfa düşer (Fresnel taklidi: normal·bakış açısı). Şader gerekmez.
3. **Alt girdaplar (Seviye 4–5).** 2–4 ince ikincil huni, ana eksen çevresinde 0,5 tur/sn döner; yerde sikloidal iz bırakır (aşağıda).
4. **Duvar bulutu.** Huni tepesinde, çap = 6 × taban çapı, 8 blok kalın, koyu gri-mavi disk; 120 büyük bulut sprite'ı, 0,05 tur/sn döner. Arka tarafında açık renkli at nalı boşluk (RFD yarığı) — bu, kasırganın yönünü uzaktan okutur.
5. **Kalkan bloklar.** Sunucu koparılan bloğun bir kısmını `UcanBlokVarligi` olarak gönderir (gerçek fizik, oyuncuya çarpabilir); istemci her gerçek varlık için kalite kadar **yalnız görsel** enkaz (blok modelinin 0,25–0,5 ölçekli kopyası) üretir. Enkaz sarmal yörüngede yükselir (`y += v_y`, `θ += ω(r)`), 25–60 blok yukarıda dışa savrulup düşer.
6. **Ortam.** Kasırga 64 blok yakındayken sis rengi yeşilimsi griye (`#5E6B5A`) kayar, sis mesafesi %60'a iner (`ViewportEvent.RenderFog`, `ComputeFogColor`); yağmur yoğunluğu artar; 20–40 sn'de bir uzakta şimşek (vanilla `LightningBolt` görseli, hasarsız `visualOnly`).

**Kamera.** 40 blok içinde düşük frekanslı titreme (`ViewportEvent.ComputeCameraAngles`, yuvarlanma ±0,4°·seviye, 6–9 Hz gürültü) ve oyuncu huniye bakarken hafif FOV daralması (−4°).

**Afet sonrası iz.** Geçiş hattı boyunca 1–3 blok genişliğinde "sıyrılmış" şerit: çimen → toprak, kırık yapraklar, devrik kütükler (yere yatay bırakılan kütük blokları). Seviye 4–5'te alt girdapların bıraktığı sikloidal halka izleri (toprak/kaba toprak halkaları). Yere düşen enkaz blokları gerçek blok olarak kalır (`afetBlokHasari=true` ise).

**Seviye 1 ile 5 farkı (görüntüde):** S1 ince, yere tam değmeyen, açık gri bir halat; yukarı doğru hafifçe genişler, etek seyrek, havada yaprak ve çimen parçaları. S5 duvar bulutundan yere inen, orta yükseklikte yüksekliğinin ~%40–50'si kadar geniş koyu bir kama gövde; tabanını 60 blok çaplı kahverengi-gri enkaz eteği sarar, çevresinde 3–4 alt girdap döner, havada kütük ve taş blokları, ufku kaplayan duvar bulutu.

### Spike sonrası güncelleme, 2026-11-06

Kaynak: `.vibe/kararlar/spike-kasirga.md` (SK-01…SK-06), `.vibe/rapor/inceleme-spike-kasirga.md` B2.

- **Biçim düzeltmesi (B2).** Spike, notun eski sayılarını (S5 taban 34, yükseklik 110 = %31, "tabanı tepesinden geniş") izledi; ölçülen silüet yukarı doğru genişleyen bir sütun + geniş duvar bulutuydu ve etek vitrin karesinde kontrol karesinden ayrılmadı. Yukarı genişleyen gövde fiziksel olarak doğrudur; eksik olan kama kalınlığı ve etektir. Bu yüzden gövde tepe çapı ve etek çapı §6 tablosuna ayrı sütun olarak girdi, etek alfası yükseltildi ve vitrinde zorunlu oldu.
- **Spike kararı — huni (SK-01):** prosedürel ağ seçildi (iki iç içe katman: dış ve %80 yarıçaplı iç; kalite tablosundaki dilim × halka). Doku bellekte üretilen 256×256 yatay periyodik fBm atlası; render tipi vanilla `entity_translucent` şaderi üstünde kendi `RenderType`'ımız (bulanık doku, arka yüz kesmesi yok, derinlik yazmaz, dörtgenler kameraya göre sıralı). Weather2 tarzı sprite halkaları (taşma, kare kenar, S1 halatı boncuk dizisine dönüyor) ve karışım reddedildi. Faz 4: Ultra'da çokgen kenarı daha çok dilim ya da daha yumuşak kenar alfasıyla gider.
- **Spike kararı — sprite'lar (SK-02):** etek, duvar bulutu ve alt girdaplar renderer'ın kendi kameraya dönük dörtgen listesine, huniyle aynı tampona ve aynı render tipine yazılır (kare başına tek çizim çağrısı, nesne ve tick yok). Vanilla `ParticleEngine` reddedildi: 16384'lük ortak kuyruk başka efektleri siler, derinlik sıralaması huniyle tutmaz.
- **Spike kararı — kalkan bloklar (SK-03):** `UcanBlokVarligi extends Entity` (FallingBlockEntity türevi değil), kasırga başına **en çok 40**, konum 10 Hz; yere değince gerçek blok. Görsel kopya: Düşük 1, Yüksek 4, Ultra 6 kopya / gerçek blok, kasırga başına en çok 240, 160 blok ötesinde 0; üretim `onSyncedDataUpdated` kancasıyla. Faz 4: kopyalar önbellekli `BakedModel` dörtgenleriyle.
- **Spike kararı — hareket ve ortam (SK-04):** kasırga konumu vanilla izleyiciyle 4 Hz, istemci 5 tick'e enterpole eder; `RenderFog` değişikliği yalnız olay iptal edilince uygulanır (`setCanceled(true)`).

## 3. Vitrin anı

**Olgun evrenin ilk 8 saniyesi**, huni bir köy ya da orman kenarına girerken. Kamera kasırganın 45–60 blok önünde, yere yakın (Y+3), hafif yukarı bakar (pitch −12°); kadrajın üçte biri yer, üçte ikisi huni ve duvar bulutu. Görüntüde: ön planda kopan yapraklar ve havaya kalkan bir ahşap çatı parçası, ortada kahverengi enkaz kılıfıyla sarılı kama huni, sağda/solda dönen alt girdaplardan biri, üstte koyu duvar bulutu ve arkasındaki açık RFD yarığından sızan ışık, sis yeşilimsi gri. Küçük resim için: huni kadrajın merkezinde, tabanda enkaz halkasının yaydığı "etek" net görünmeli.

### Spike sonrası güncelleme, 2026-11-06

- Arazi ve ağaçlar eteği örtmesin diye kamera Y+8–12'ye çıkar (pitch −5…−10°); etek kadrajda gövdenin iki yanından taşmalıdır.
- Kabul ölçütü (sayısal): vitrin karesinde taban bandı (ufuk ± etek yüksekliği), aynı kameranın kasırgasız kontrol karesinden huni genişliğinin en az 1,5 katı genişlikte ayrılır (≥ 20 L koyuluk farkı ya da kahverengi-gri renk kümesi).

## 4. Ses tasarımı

- **Uzak (64–160 blok):** "yük treni" uğultusu — 40–120 Hz, yavaş genlik dalgalanmalı gürültü döngüsü, mono, konumsal.
- **Orta (16–64 blok):** + rüzgâr ıslığı (vanilla `item.elytra.flying` düşük pitch 0,6–0,8, ses düzeyi hıza bağlı) + rastgele enkaz çarpmaları (`block.wood.break`, `block.gravel.break`, `block.glass.break`, 0,5–2 sn aralıkla).
- **Yakın (<16 blok):** + derin uğultu katmanı ve basınç "pompası" (2 Hz'lik genlik modülasyonu); orta frekans (300–3400 Hz) bilinçli olarak 6 dB oyuk (bkz. `ses.md`), konuşma bandı boş kalır.
- Toplam kasırga ortam sesi kategori: `WEATHER`; en yakında bile kısa süreli ses yüksekliği siren tepe değerinin altında.

## 5. Performans planı

| Kalite | Huni ağı | Taban sprite | Görsel enkaz / gerçek blok | Duvar bulutu | Alt girdap |
|---|---|---|---|---|---|
| Düşük | 12×16 | 150 | 1 / 1 | 40 sprite | yok |
| Orta | 16×24 | 350 | 2 / 1 | 80 | 2 |
| Yüksek (varsayılan) | 24×32 | 700 | 4 / 1 | 120 | 2–4 |
| Ultra | 32×48 | 1400 | 6 / 1 | 200 | 4 |

- **LOD:** oyuncu ile mesafe 96 bloğu geçince taban sprite sayısı yarıya, 160'ta enkaz durur, 256'da yalnız huni + duvar bulutu (impostor değil, düşük ağ). Yakındaki oyuncu (<48 blok) her zaman seçili kalitenin tamamını görür.
- **Batch:** bütün sprite'lar tek `BufferBuilder` ile tek çizim çağrısı; enkaz blok modelleri önbellekli `BakedModel` quad listelerinden.
- **Sunucu:** blok koparma kuyruğu tick başına seviye tablosundaki sayıyla sınırlı; TPS < 18 ise yarıya, < 15 ise dörtte bire iner. Aynı anda en çok 40 `UcanBlokVarligi`; fazlası doğrudan yere düşen eşya/blok olarak sonuçlanır.
- Hedef: tepe anda FPS ≥ afetsizin %50'si, TPS > 18.

### Spike sonrası güncelleme, 2026-11-06

- Ölçülen (SK-05, S5, 52 blok): Yüksek %72–77, Düşük %87–91, Ultra %46–65 (sınırda); `/forge tps` her ölçümde 20,0. Hedef Yüksek'te tutuyor.
- En pahalı katmanlar duvar bulutu ve etek; maliyet köşe sayısı değil GPU doldurma oranı (üst üste binen büyük yarı saydam sprite'lar). Faz 4 yöntemi (efekt kırpmadan, madde 1): duvar bulutunu birkaç halkalı yarı saydam ağa çevirmek ya da sprite boyut/alfa dağılımını üst üste binmeyi azaltacak biçimde düzenlemek; sprite boyutunu ekranda kapladığı alana göre sınırlamak; huni ağını tick başına bir kez kurup karede yalnız U kaymasını güncellemek. Etek alfasının yükselmesi (0,6–0,8) bu bütçeye dahil ölçülmeli.

## 6. Açık noktalar — önerilen varsayılanlar

| Seviye | Gövde taban çapı | Gövde tepe çapı | Etek çapı (1,8×) | Yükseklik | Hız (blok/sn) | Aktif süre | Çekim yarıçapı | Blok kaldırma/tick | Kırabildiği sertlik | Çekirdek hasarı |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 Hafif | 6 | 10 | 11 | 50 | 2,0 | 60 sn | 9 | 2 | ≤0,6 (yaprak, çimen, çiçek) | 1 can/sn |
| 2 Orta | 10 | 17 | 18 | 65 | 2,6 | 90 sn | 15 | 4 | ≤2,0 (ahşap, cam) | 1,5 |
| 3 Şiddetli | 16 | 27 | 29 | 80 | 3,2 | 120 sn | 24 | 8 | ≤3,0 | 2 |
| 4 Yıkıcı | 24 | 46 | 43 | 95 | 3,8 | 150 sn | 36 | 14 | ≤4,0 | 3 |
| 5 Felaket | 34 | 55 | 60 | 110 | 4,4 | 180 sn | 51 | 20 | ≤5,0 (taş, kırıktaş) | 4 |

- Spike sonrası güncelleme, 2026-11-06: "Gövde tepe çapı" ve "Etek çapı" sütunları eklendi; gövdenin orta yükseklikteki genişliği (h = 0,5; `h^0.6` = 0,66) S4 ~38,5 / 95 = %41, S5 ~48 / 110 = %44 (kama). Etek yüksekliği S1 6 → S5 14 blok, doğrusal.
- Yönelim: hedef oyuncudan 80–140 blok uzakta doğar, oyuncu çevresine doğru ±35° sapmalı rastgele yürüyüşle ilerler; doğuş 8 sn (toz girdabı → huni iner), dağılma 10 sn (ip evresi).
- Bloklar `dogalafetler:kasirga_dayanikli` etiketiyle korunur (obsidyen, deepslate, ana kaya varsayılan).
- Kaldırılan oyuncu en çok 25 blok yükselir; düşme hasarı vanilla.

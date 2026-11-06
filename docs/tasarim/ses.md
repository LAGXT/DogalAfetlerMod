# Siren ve Afet Sesleri — tasarım notu (madde 5, N5, N6, N19)

## 1. Gerçek olay

- **Dış mekan uyarı sirenleri (Federal Signal 2001-130 / Equinox, Model 2):** üç temel sinyal — **sabit (steady)**, **inip çıkan (wail)** ve **hızlı inip çıkan (fast wail)**. Elektromekanik sirende perde, rotor devri × rotor-stator port sayısıyla belirlenir; motor hızlanıp yavaşladıkça perde yumuşakça kayar ("wail" karakteri), ani perde sıçraması yoktur. Mekanik sirenin açılışında motor hızlanırken uzun, alçaktan yükselen bir ıslak (spin-up), kapanışında yavaşça inen bir kuyruk (wind-down) vardır.
- **Halkın tepkisi (NIST TN 1950):** siren sesi tanınırlık için tutarlı olmalı; aynı sinyal farklı anlamlar taşıyınca karışıklık doğar. Bu yüzden bizde: tüm afetler için tek tanınır siren, güneş patlaması için açıkça farklı bir varyant.
- **Afet sesleri:** tsunami "tren ya da jet" uğultusu (IOC-UNESCO), kasırga "yük treni", deprem alçak frekanslı gürleme ve güm (P dalgası sarsıntıdan önce), göl buzunun dispersif "şarkısı".

Kaynaklar:
- https://pdf.directindustry.com/pdf/federal-signal/2001-130-equinox-sirens/9256-699894.html (steady / wail / fast wail)
- https://www.fedsig.com/sites/default/files/resource_library_document/2001-130%20Equinox%20508-128%20Sirens%20Manual%20%7C%2025500289.pdf (perde = rotor devri × port sayısı)
- https://nvlpubs.nist.gov/nistpubs/TechnicalNotes/NIST.TN.1950.pdf (siren sistemleri ve kamu tepkisi)
- https://legacy2.ctic.ioc-unesco.org/tsunami-info/natural-tsunami-warning-signs
- http://news.berkeley.edu/2018/01/12/transcript-berkeley-seismologists-capture-earthquakes-rumble

## 2. Minecraft'ta gösterim (çalışma yolu)

**Siren kategorisi (N5).** Siren `SoundSource.MASTER` ile çalınır (böylece Hava, Bloklar gibi vanilla kaydırıcıları etkilemez, yalnız Ana Ses etkiler) ve ses düzeyi `CLIENT config sirenSes / 100` ile çarpılır. "Müzik ve Sesler" ekranına `ScreenEvent.Init.Post` ile "Sirenler" kaydırıcısı eklenir, varsayılan **%28**. `SoundSource` enum'una gerçek kategori eklemek mixin/enum hack ister (aşağıdaki spike güncellemesi: denendi, reddedildi).

**Siren çalma.** Konumsuz (`SoundInstance.Attenuation.NONE`, `relative=true`), sunucunun gönderdiği siren paketiyle tüm etkilenebilecek oyuncularda aynı anda başlar. `AbstractTickableSoundInstance`: her tick ses düzeyi zarfı kodla yönetilir → afet aktif aşamaya geçince 2,0 sn'lik üstel fade-out (madde 5; güneş sireninde 4 sn), dosya döngülüdür, böylece süre kodla kesin tutulur (10 sn ya da 60 sn).

**Sentez yolu.** `tools/assets/ses_uret.py` (KÖK/.venv, numpy): her ses deterministik seed'le 48 kHz `float32` olarak üretilir → normalize → `wav` → `ffmpeg -i x.wav -ac 1 -c:a libvorbis -q:a 5 x.ogg` (konumlu sesler mono olmalı, Minecraft stereo sesi uzaklıkla kısmaz; siren de mono). Kontrol: `ffmpeg -af ebur128` ile LUFS ve gerçek tepe, `showspectrumpic` ile spektrogram PNG (`docs/kanit/`). Döngülü dosyaların başı ve sonu sıfır geçişinde birleşir (50 ms çapraz geçiş).

### Spike sonrası güncelleme, 2026-11-06

Kaynak: `.vibe/kararlar/spike-siren.md` S-01…S-06, `docs/KARARLAR.md` K0-19, K0-23, K0-27.

- **N5 yolu seçildi (S-01):** siren `SoundSource.MASTER` ile çalar, düzey `zarf × sirenSes / 100`; CLIENT config `ses.sirenSes` (0–100, varsayılan **28**, `dogalafetler-client.toml`). "Sirenler" kaydırıcısı "Müzik ve Sesler" ekranında Metin Okuyucu/Konuşma satırının boş sağına oturur (bulunamazsa listenin sonuna), değer bellekte anında değişir, ekran kapanırken bir kez kaydedilir; 0 = "KAPALI". Faz 2'de ikinci giriş noktası: "Modlar → Doğal Afetler → Ayarlar" (`ConfigScreenFactory`). Gerçek `SoundSource` kategorisi (enum mixin'i) denendi ve çalıştı, ama reddedildi: `SoundSource` Forge 47.4.28'de `IExtensibleEnum` değil, vanilla varsayılanı %100 olur, ses paketleri kategoriyi sıra numarasıyla taşır (başka modun sabitiyle sıra kayabilir), başka ses modları `values().length`'e güvenebilir.
- **Spin-up 140 Hz (S-02):** `SoundEngine` perdeyi 0,5–2,0'a kırptığı ve dosya döngülü olduğu için spin-up dosyaya gömülmez; zarf perdeyi 0,5 → 1 çeker (280 Hz'in yarısı = 140 Hz). Zarf: standart açılış 24 tick, fade-out 40 tick (2 sn, perde %20 iner); güneş açılış 60 tick, süre boyunca +3 dB, fade-out 80 tick (4 sn). Güneş sireninin 1,5 → 2,5 kHz parlaması dosyaya gömülmedi (20 sn döngü 3 kez çalar); yerine kodda +3 dB.
- **Ay çarpması sireni 60 sn (K0-19):** standart siren dosyası 60 sn döngüyle çalar, çarpma anında 2 sn fade-out; diğer altı afette 10 sn, güneş patlamasında 60 sn güneş sireni.
- **Siren paketi** afet türünü değil siren türünü taşır (`SirenPaketi(tür, süreTick)`, `SirenDurdurPaketi`); paketi dinleyen değiştirilmiş istemci afet türünü öğrenemez (radar avantajı korunur). Faz 2'de sireni bitiren sunucunun durdurma paketidir; istemci süresi yalnız güvenlik tavanı (`süreTick + 3 sn`). Sonradan giren oyuncuya kalan süreyle siren paketi.
- **Siren sürer (K0-27):** boyut değişimi, `/stopsound` ve kaynak yeniden yüklemesi `SoundManager.stop()` ile bütün sesleri keser; zarf ses örneğinden ayrı yaşadığı için siren aynı zarfla yeniden çalar (açılış tekrarlanmaz, düzey sıçramaz). **`/stopsound` sireni susturmaz** (uyarı sesi, kasıtlı); oyuncunun yolu "Sirenler" kaydırıcısıdır. Akışlı dosya baştan çalar (düzey sürekli). Sunucudan çıkışta siren fade beklemeden durur; ikinci siren gelirse eskisi 0,5 sn'de söner.
- **Ölçülen dosyalar (S-03):** `siren_standart.ogg` −20,0 LUFS, gerçek tepe −12,9 dBTP, baskın 284→420→284 Hz, >2,5 kHz −45 dB, 68 553 bayt; `siren_gunes.ogg` −20,9 LUFS, −9,3 dBTP, 182→300→240→182 Hz, 123 565 bayt. Kanıt `docs/kanit/ses/`. Bayt determinizmi için ffmpeg `-serial_offset 20261105 -fflags +bitexact -flags:a +bitexact -map_metadata -1`; ara WAV 16 bit TPDF dither. Standart sirene +0,3 Hz, −10 dB ikinci osilatör eklendi (tonu canlandırır).
- **Alt yazı:** iki siren olayının da `subtitle` = `subtitles.dogalafetler.siren` ("Siren çalıyor"). Açık nokta: konumsuz (`relative=true`, 0,0,0) seste vanilla alt yazı oku dünya başlangıcını gösterebilir; Faz 2'de kulakla/görüntüyle seçilecek.

## 3. Vitrin anı (ses)

**Güneş patlaması sireninin ilk 10 saniyesi:** oyuncular bir üste sohbet ederken uzak, alçak bir uğultu karışır; yavaşça yükselen iki ses birbirine sürttükçe "vuruş" yapar, aynı anda güneş gözle görülür şekilde büyümeye başlar. Konuşma kesilmez, ama herkes "bu ne?" diye susar. Kabul ölçütü: sesli sohbette konuşan arkadaşın sözleri sirenle birlikte anlaşılır kalır (kulakla değerlendirilecek, `GOREV` §7).

## 4. Ses tasarımı

**Standart siren (`siren_standart.ogg`, 10 sn döngü birimi) — "sakin ama fark edilir".**
- Kaynak: iki osilatör; ana sinüs + 2. harmonik −12 dB + 3. harmonik −22 dB (mekanik sirenin yumuşak "üfleme" tonu, kare dalga sertliği yok) + −30 dB pembe gürültü "hava" katmanı.
- Perde: **wail** örüntüsü, 280 Hz ↔ 420 Hz (yaklaşık C#4 ↔ G#4, tam beşli aralık), periyot 5 sn: 2,0 sn yükseliş, 0,5 sn tepe, 2,5 sn iniş (yavaş, nefes alan ritim). İlk 1,2 sn spin-up 140 Hz'den başlar (spike sonrası: kodda perde 0,5 → 1; §2 sonundaki güncelleme).
- Filtre: 2,5 kHz üstü −12 dB/oktav alçak geçiren (tizlik yok), 8 kHz üstü pratikte boş.
- Hedef: dosya içi entegre ses yüksekliği −20 LUFS, gerçek tepe ≤ −3 dBTP; varsayılan %28 kaydırıcıda oyuncunun duyduğu düzey vanilla yağmurdan biraz yüksek, kılıç vuruşu sesinden düşük. Baskın frekans 280–420 Hz (spektrogramda tek, yumuşak kayan çizgi).

**Güneş patlaması sireni (`siren_gunes.ogg`, 20 sn döngü birimi, 60 sn çalınır) — uzun, ürkütücü.**
- Daha alçak ve yavaş: 180 Hz → 300 Hz → 240 Hz, periyot 10 sn; iniş küçük üçlü aralığa (minor) oturur, tekin olmayan bir "inleme" hissi.
- İkinci osilatör 0,7 Hz sapmalı (yavaş vuruş / dalgalanma), altında 55 Hz sinüs drone (−18 dB) ve 2,5 sn'lik sentetik yankı kuyruğu (uzaktan, vadiden gelen siren).
- 60 sn boyunca düzey ve parlaklık çok yavaş artar (+3 dB, alçak geçiren 1,5 → 2,5 kHz) — "yaklaşıyor" hissi; 60. saniyede meteorlarla 4 sn fade-out.
- Hedef −21 LUFS, tepe ≤ −3 dBTP; standart sirenle karıştırılamaz (farklı perde bandı ve tempo).

**Afet ortam katmanları (özet; ayrıntı her afet notunda).**

| Afet | Sentez katmanı | Vanilla yeniden kullanım |
|---|---|---|
| Kasırga | tren uğultusu 40–120 Hz; basınç pompası 2 Hz | `item.elytra.flying`, `block.wood/gravel/glass.break`, `entity.lightning_bolt.thunder` |
| Tsunami | çekilme emmesi, jet uğultusu 50–150 Hz, kırılma gümü | `entity.generic.splash`, `entity.cod.flop`, `block.pointed_dripstone.drip_water`, `ambient.underwater.loop` |
| Deprem | öncü gürleme 25–60 Hz, ana gürleme 20–80 Hz + çıtırtı | `block.stone/deepslate.break`, `block.gravel.fall`, `block.wooden_door.close` |
| Sinkhole | kaya "pop" darbeleri, boşluk yankısı | `block.gravel.fall`, `block.sand.fall`, `ambient.cave` |
| Asit yağmuru | cızlama 3–8 kHz (çok düşük düzey) | `weather.rain`, `block.fire.extinguish` |
| Orman yangını | yangın uğultusu 40–200 Hz, çıtırtı | `block.fire.ambient`, `block.fire.extinguish` |
| Güneş patlaması | meteor ıslığı, çarpma gümü, süpernova basıncı, kızıl cüce akoru | `entity.generic.explode` (uzak, pitch 0,5) |
| Ay çarpması | yaklaşma uğultusu 18–45 Hz, çarpma gümü | deprem katmanı |
| Donma | soğuk rüzgâr, buz şarkısı chirp'i | `block.glass.break`, damla sesleri |

**Konuşmayı bastırmama kuralları.**
1. Sürekli (döngülü) ortam seslerinde 300–3400 Hz bandına geniş −06 dB çentik (1 kHz merkezli, Q 0,5) sentez sırasında uygulanır; enerji alt bas ve çok üst tizde toplanır.
2. Döngülü ortam sesleri −26 LUFS, tek seferlik darbeler tepe ≤ −6 dBTP.
3. Kod tavanı: modun aynı anda çalan en çok 16 ses örneği; aynı türden en çok 6; fazlası en uzaktaki/en sessiz örnekten düşürülür.
4. Siren çalarken modun ortam sesleri −3 dB alçalır (siren açıkça duyulsun, toplam düzey artmasın).
5. Mesafe: konumlu seslerde vanilla doğrusal azalma + uzakta alçak geçiren hissi için iki dosya (yakın/uzak varyant), aradaki geçiş kodla çapraz karışım.
6. Kategoriler: ortam sesleri `WEATHER` (kasırga, tsunami, yağmur) ve `BLOCKS` (kırılma, yangın), donma ve gökyüzü `AMBIENT`; oyuncu bunları vanilla kaydırıcılarla ayrı ayrı kısabilir.

**Altyazı.** `sounds.json`'da `subtitle` anahtarları: "Siren çalıyor", "Uzak bir uğultu", "Yer gürlüyor" vb.; yalnız vanilla Altyazı açıkken görünür (madde 5).

## 5. Performans planı

| Kalite | Eşzamanlı mod sesi tavanı | Yakın/uzak varyant geçişi | Nokta ses sıklığı (kırılma, çıtırtı) |
|---|---|---|---|
| Düşük | 8 | yok (tek dosya) | %50 |
| Orta | 12 | var | %75 |
| Yüksek | 16 | var | %100 |
| Ultra | 20 | var | %100 |

- Ses dosyaları `stream: true` yalnız 10 sn'den uzun döngüler için (siren_gunes, ortam döngüleri); kısa darbeler bellekte.
- Toplam ses paketi hedefi < 6 MB (Vorbis q5, mono).

## 6. Açık noktalar

| Dosya | Süre | Kaynak |
|---|---|---|
| `siren_standart.ogg` | 10 sn (döngü) | sentez |
| `siren_gunes.ogg` | 20 sn (döngü, 3 tur) | sentez |
| `kasirga_ugultu_yakin/uzak.ogg` | 12 sn döngü | sentez |
| `tsunami_cekilme.ogg`, `tsunami_ugultu.ogg`, `tsunami_carpma.ogg` | 15 / 12 döngü / 4 sn | sentez |
| `deprem_oncu.ogg`, `deprem_gurleme.ogg` | 2 / 10 sn döngü | sentez |
| `sinkhole_catlama.ogg`, `sinkhole_cokus.ogg` | 4 / 6 sn | sentez |
| `asit_cizlama.ogg`, `yangin_ugultu.ogg`, `yangin_citirti.ogg` | 8 / 10 / 6 sn döngü | sentez |
| `meteor_islik.ogg`, `meteor_carpma.ogg`, `supernova.ogg`, `kizilcuce_akor.ogg`, `ay_yaklasma.ogg`, `ay_carpma.ogg` | 3 / 4 / 28 / 4 / 20 / 12 sn | sentez |
| `donma_ruzgar.ogg`, `buz_sarkisi.ogg`, `kiyi_yalama.ogg` | 12 döngü / 1 / 6 döngü | sentez |

- Varsayılan siren düzeyi %28 (şartname %25–30 aralığının ortası).
- Spike sonrası güncelleme, 2026-11-06: siren `MASTER` + "Sirenler" kaydırıcısı yolu kesinleşti (K0-23); oyuncu sireni vanilla kategori kaydırıcılarıyla değil, yalnız "Sirenler" ve Ana Ses ile kısar. Ay çarpması sireni 60 sn (K0-19, `ay_carpmasi.md` §6).

## 7. Faz 2 güncellemesi (2026-11-06, `uyari-ses`)

Kaynak: `.vibe/kararlar/uyari-ses.md` (U-01…U-16), kod `uyari/**`, `client/uyari/**`.

- **Sürenin yetkilisi sunucu (U-01).** Siren `SirenIstegiEvent` ile başlar, `SirenDurdurEvent` ile (afet AKTIF'e geçince ya da siren sürerken durdurulunca) fade-out'a girer. İstemcideki süre yalnız güvenlik tavanıdır: kalan süre + 3 sn'de durdurma gelmediyse türün kendi fade-out'u başlar. Fade süreleri SERVER config'te (`uyari.standartFadeOutTick` 40, `uyari.gunesFadeOutTick` 80) ve durdurma paketiyle gider.
- **Paketler (U-02).** `uyari/siren` = siren türü, afet türü, `afetNo` (afetin başlangıç saati), toplam süre, kalan süre (gerçek tick). Aynı `afetNo`'nun 1 Hz tekrar isteği çalan sirende yok sayılır; başka afetin sireni eskisini 0,5 sn'de keser; eski afetin geç kalan durdurması yeni sireni söndürmez. Afet türü pakette: çekirdeğin `AfetDurumPaketi` SIREN'den itibaren türü zaten aynı oyunculara gönderiyor, ON_UYARI'da siren paketi gitmez (radar avantajı korunur).
- **Sonradan giren (U-03).** Kalan süreyle çalar; açılış kısalmaz (kısa belirme), güneş sireninin +3 dB yükselişi geçen sürenin yerinden sürer.
- **Alt yazı oku (U-06, S-06 B4).** Kaynak dinleyiciye göreli (0,0,0) çalar (sağ-sol kayması yok); yalnız çalma anında (düzey 0 iken) konum dinleyicinin dünya konumudur, böylece vanilla alt yazısı dünya başlangıcını göstermez (konum ≈ kamera → ok çizilmez).
- **Ortam katmanları API'si (U-07…U-10).** `uyari.OrtamSesi.baslat(server, kimlik, afetNo, ses, kategori, Yer, HacimEgrisi)` / `konumGuncelle` / `durdur(server, kimlik, fadeTick)`. İstemci düzeyi = hacim eğrisi × doğrusal mesafe sönümü (menzil afetçe verilir; OpenAL sönümü kapalı, yön duyulur) × `ortamTavan` (CLIENT, varsayılan 40) × siren sırasında −3 dB (20 tick geçiş) × kapanış fade'i; vanilla kategori kaydırıcısı (Hava/Bloklar/Ortam) motor tarafından çarpılır. Eşzamanlı katman tavanı efekt kalitesine göre 8/12/16/20 (§5 tablosu), aynı dosyadan en çok 6; fazlası en sessizden düşer. Afet bitince unutulan katmanlar 2 sn'de kendiliğinden söner. Sonradan giren/boyut değiştiren oyuncu katmanı geçen süreyle alır.
- **Örnek katman `ortam_test.ogg` ("Uzak bir uğultu").** 45 + 67,5 Hz kısmi tonlar 0,25 Hz'de nefes alır, altında 30–180 Hz gürleme, çok düşük 4–9 kHz hışırtı; konuşma bandına (1 kHz merkezli, ~2,5 oktav) −6 dB çentik (§4 kural 1). 12 sn döngü, akışla. Ölçülen: I = −26,0 LUFS, gerçek tepe −11,0 dBTP (hedef −26 / ≤ −6), baskın 40–60 Hz, 300–3400 Hz bandı toplamın −26,4 dB altında. GameTest ve istemci otomasyonu bu katmanla sınanır; afet işçilerinin gerçek katmanları kendi notlarındadır.
- **Ölçüm (GOREV §6 #11).** `tools/assets/ses_uret.py` her dosyayı oyunun çalacağı ogg'den ölçer ve tasarım hedefiyle karşılaştırır (`docs/kanit/ses/<ad>_ebur128.txt`, `<ad>_spektrogram.png`, 20 Hz–10 kHz log ölçek): standart siren −20,0 LUFS, tepe −12,9 dBTP, baskın 420/400/380 Hz, perde izi 284→420→284 Hz, >2,5 kHz −45,1 dB → "sakin ama fark edilir" hedefine uygun (tiz yok, tepe hedefin 10 dB altında). Güneş sireni −20,9 LUFS, −9,3 dBTP, baskın 240/180/220 Hz, perde izi 182→300→240→182 Hz, >2,5 kHz −50,9 dB. Siren dosyaları spike'takiyle bayt bayt aynı (determinizm korunuyor).
- **Varsayılan düzeyler.** `uyari.sirenSes` 28 (Ana Ses ile çarpılır), `uyari.ortamTavan` 40: Ortam kaydırıcısı %100 iken örnek katman ≈ −34 LUFS, %28'lik siren ≈ −31 LUFS → ortam sirenden ~3 dB, siren sırasında ~6 dB alçak.
- **Oyun içi ölçüm (U-15).** İstemci otomasyonunun sesi hoparlöre değil oyunun kendi OpenAL Soft'unun "Wave File Writer" aygıtıyla dosyaya gitti (Ana Ses 0 iken vanilla hiçbir sesi başlatmaz). Kayıt kaynak dosyalarla eşleşik kazançla ölçüldü (`tools/senaryo/uyari_kayit_olc.py`): "Sirenler" 28 → 40 sonrası siren +3,09 dB (beklenen 20·log10(40/28) = +3,10), siren çalarken ortam katmanı −3,00 dB (bastırma hedefi −3), `/stopsound` sonrası siren dosyası baştan ve aynı düzeyle çaldı. Gizli workspace'teki istemci yavaş döndüğü için (istemci ~13 tick/sn) fade-out kayıtta ~1 sn'lik basamaklarla görünür; zarfın kendisi log'da tick tick (−60 dB / 40 tick). Kulakla değerlendirme sahibine kalır.

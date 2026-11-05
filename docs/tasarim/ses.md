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

**Siren kategorisi (N5).** Siren `SoundSource.MASTER` ile çalınır (böylece Hava, Bloklar gibi vanilla kaydırıcıları etkilemez, yalnız Ana Ses etkiler) ve ses düzeyi `CLIENT config sirenSes / 100` ile çarpılır. "Müzik ve Sesler" ekranına `ScreenEvent.Init.Post` ile "Sirenler" kaydırıcısı eklenir, varsayılan **%28**. `SoundSource` enum'una gerçek kategori eklemek mixin/enum hack ister; Forge 1.20.1'de genişletilebilir enum olmadığı varsayılır [spike 4'te doğrulanmalı].

**Siren çalma.** Konumsuz (`SoundInstance.Attenuation.NONE`, `relative=true`), sunucunun gönderdiği siren paketiyle tüm etkilenebilecek oyuncularda aynı anda başlar. `AbstractTickableSoundInstance`: her tick ses düzeyi zarfı kodla yönetilir → afet aktif aşamaya geçince 2,0 sn'lik üstel fade-out (madde 5), dosya döngülüdür, böylece süre kodla kesin tutulur (10 sn ya da 60 sn).

**Sentez yolu.** `tools/assets/ses_uret.py` (KÖK/.venv, numpy): her ses deterministik seed'le 48 kHz `float32` olarak üretilir → normalize → `wav` → `ffmpeg -i x.wav -ac 1 -c:a libvorbis -q:a 5 x.ogg` (konumlu sesler mono olmalı, Minecraft stereo sesi uzaklıkla kısmaz; siren de mono). Kontrol: `ffmpeg -af ebur128` ile LUFS ve gerçek tepe, `showspectrumpic` ile spektrogram PNG (`docs/kanit/`). Döngülü dosyaların başı ve sonu sıfır geçişinde birleşir (50 ms çapraz geçiş).

## 3. Vitrin anı (ses)

**Güneş patlaması sireninin ilk 10 saniyesi:** oyuncular bir üste sohbet ederken uzak, alçak bir uğultu karışır; yavaşça yükselen iki ses birbirine sürttükçe "vuruş" yapar, aynı anda güneş gözle görülür şekilde büyümeye başlar. Konuşma kesilmez, ama herkes "bu ne?" diye susar. Kabul ölçütü: sesli sohbette konuşan arkadaşın sözleri sirenle birlikte anlaşılır kalır (kulakla değerlendirilecek, `GOREV` §7).

## 4. Ses tasarımı

**Standart siren (`siren_standart.ogg`, 10 sn döngü birimi) — "sakin ama fark edilir".**
- Kaynak: iki osilatör; ana sinüs + 2. harmonik −12 dB + 3. harmonik −22 dB (mekanik sirenin yumuşak "üfleme" tonu, kare dalga sertliği yok) + −30 dB pembe gürültü "hava" katmanı.
- Perde: **wail** örüntüsü, 280 Hz ↔ 420 Hz (yaklaşık C#4 ↔ G#4, tam beşli aralık), periyot 5 sn: 2,0 sn yükseliş, 0,5 sn tepe, 2,5 sn iniş (yavaş, nefes alan ritim). İlk 1,2 sn spin-up 120 Hz'den başlar.
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
- Siren `MASTER` üzerinden çalındığı için oyuncu vanilla bir kaydırıcıyla değil, yalnız "Sirenler" ve Ana Ses ile kısar; bu N5'in "kendi kategorisi" isteğine yakın en güvenli yol olarak KARARLAR'a yazılmalı.
- Ay çarpması sireninin 10 sn mi 60 sn mi olduğu şartnamede çelişkili (`ay_carpmasi.md` §6).

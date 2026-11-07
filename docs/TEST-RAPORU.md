# Test raporu

`docs/GOREV.md` §6 doğrulama matrisi. Her satırın kanıtı (komut, çıktı özeti, ekran görüntüsü yolu) "Kanıt"
sütununa yazılır; ekran görüntüleri ve ölçüm dosyaları `docs/kanit/` altında durur. Durum değerleri: `yapılmadı`, `kısmi`, `yeşil`,
`bilinen sınır` (davranış GameTest/senaryoyla kanıtlı, kalan kare ya da ölçüm bu makinede alınamıyor; aşağıdaki bölüm), `bekliyor`
(kullanıcı kararı ya da dış ağ). Kanıt hücreleri tek son durum cümlesidir (izleme-inceleme-5 D13); satırların HEAD'i hücrede, hepsi
ana makinede koştu (`5-matris`, K-E43 öncesi; kasırga senaryosu pc2).

| # | Ne | Nasıl | Yeşil ölçütü | Kanıt | Durum |
|---|---|---|---|---|---|
| 1 | Derleme | `./gradlew build` | çıkış 0, `build/libs/` altında reobf'lu jar | `7d393d8`'de `gradle_kapisi.sh build --rerun-tasks` çıkış 0, `warning:` 0, `dogalafetler-0.1.0.jar` 9 940 524 B, refmap + mixins.json jar'da (`.vibe/kanit/matris-5/wt/gradle/build.log`); son `main` jar'ı reobf denetimi teslimat ön koşusunda (`00d1500`, 9 990 913 B, SRG adları, `.vibe/kanit/teslimat-onkosu/`). | yeşil |
| 2 | Birim testleri | `./gradlew test` | hepsi geçer: zamanlayıcı ve çarpan; şiddet ağırlıkları ve gün eşikleri; ısı alışverişi (fark büyükse hızlı, küçükse yavaş); yün %25/50/75/100; tüy yalıtımı; aşırı soğuk istisnası; çok yalıtımla sıcakta aşırı ısınma; termostatın uç sıcaklıkta azalması; yiyecek azalan verimi; kalıcı kış maskesinin aynı seed'de aynı çıkması; TPS kısması | `7d393d8`'de build içi `test` 548/0/0/0 (90 sınıf), madde → yöntem eşlemesi `.vibe/kanit/matris-5/statik/esleme-2-3.txt`; son `main` (`00d1500`) 551/0/0/0 (`.vibe/kanit/teslimat-onkosu/build.log`). | yeşil |
| 3 | GameTest | `./gradlew runGameTestServer` | hepsi geçer: tüy upgrade'i (1 tüy harcanır, sayaç artar, üst sınır); yün ve termostat tarifleri (büyü, dayanıklılık, diğer upgrade'ler korunur, ikinci uygulama reddedilir); donma tablosunda dondur → çöz = başlangıç; `afetBlokHasari=false` iken blok değişmez; buz deliği açılır ve yeniden donar; 10 sahte oyuncunun sıcaklığı birbirinden bağımsız (sahte oyuncular tick almayabilir; gerekirse güncelleme testte doğrudan çağrılır); respawn'da sıfırlama; SavedData kalıcılığı; yangın yanmaz bloğu, asit dayanıklı bloğu bozmaz; aynı anda iki afet başlamaz | `7d393d8`'de filtresiz `runGameTestServer` iki koşu `FILTRE=yok`, `All 216 required tests passed` ×2, `Exception` 2 = kasıtlı çekirdek hata testi (`.vibe/kanit/matris-5/wt/gradle/gametest-{1,2}.log`); F-18 testi yalnız `-Puyum=coldsweat`'te sınar (#9). | yeşil |
| 4 | Geliştirme sunucusu | `runServer`, konsoldan her komut ve her afet için start/stop | log'da istisna yok, `invalid dist` yok, sunucu çökmez | `7d393d8`'de `ek_sunucu.py` + dokuz `*_sunucu.py` + meteor-2 + gökyüzü/yangın-2 hepsi `senaryo sonucu 0`, süreç kodları 0, `Exception`/`/ERROR]`/`invalid dist`/`session.lock` 0 (`.vibe/kanit/matris-5/wt/sunucu/`); kasırga senaryosu `00d1500`'de pc2'de 0. | yeşil |
| 5 | Üretim sunucusu | Forge 1.20.1 installer `--installServer` ile ayrı klasör, `mods/`'a build jar'ı | açılır; her afet başlar ve biter; log temiz (reobf/refmap sorunu yok) | Forge installer `--installServer` koşmadı: installer jar'ı yerel önbellekte yok, kütüphane indirmesi dış ağ ister (kullanıcı sorusu 4; teslimat adım 5). | bekliyor |
| 6 | Görsel tur | `runClient` + Quick Play | her afet için ön-uyarı, siren, aktif, vitrin anı, bitiş ekran görüntüsü; ekranda hiçbir uyarı yazısı yok; siren afet başlayınca fade-out ile durur; HUD barı soğuk/nötr/sıcak hallerde ve aşırı uçta titreşir; radar ve sismograf göstergeleri yalnız eşyayı taşıyanda; şef görüntüleri açıp tasarım notlarıyla karşılaştırdı | `7d393d8`'de 20 istemci adımından 18'i geçerli (K0-21 rev. 2: ALARM 0, `no_focus=true`, sahip girdisi 0, `options.txt` aynı sha; kare sayıları matris raporunda, `.vibe/kanit/matris-5/wt/istemci/<adım>/`); kasırganın iki koşusu K-17 taban rengiyle geçersiz: S5 merkezi su/kıyı kenarına düştü (P3) → K-E45 betik düzeltmesi, yeniden koşu ve şef kare incelemesi bekliyor. | kısmi |
| 7 | Çok oyunculu | ayrı klasörlü sunucu + 2–3 istemci (`--username A`/`B`/`C`, `online-mode=false`) | istemcilerde aynı afet aynı aşamada; afet ortasında giren oyuncu senkron; sıcaklıklar bağımsız; siren hepsine gider; "Birlikte Hayatta" 3 oyuncuyla açılır. RAM yetmezse eksik istemciler GameTest sahte oyuncularıyla tamamlanır ve raporda belirtilir | `7d393d8`'de Cold Sweat'li dedicated sunucuya tek runClient (`A`) bağlandı, komutlar oyuncuya uygulandı (`.vibe/kanit/matris-5/wt/istemci/s18/kanit/`), 10 sahte oyuncu GameTest'te (#3); `uyari_sunucu.py` istemcili koşusu RAM kapısı (≥ 10 GiB) açılmadığı için koşmadı → K-E46 (sunucu pc2'de, istemci ana makinede) yazıldı, pc2 güvenlik duvarı portu açılınca koşar; çoklu gerçek istemci bilinen sınır (karar 5). | kısmi |
| 8 | Kalıcılık | deniz seviyesi, ayın yok olması, güneş evresi, donma ilerlemesi, kızıl cüce dönemi, olasılık çarpanı ve bir chunk'ın donma seviyesi ayarlanır → sunucu durdurulup başlatılır | değerler korunur; sonradan giren istemcide doğru gökyüzü görünür | `7d393d8`'de afet, dönem, çarpan ve donma kayıttan geri geliyor, 13 gamerule yeniden açılışta korundu 13/13, meteor-2 bekleyen krater kayıttan okundu (`.vibe/kanit/matris-5/wt/sunucu/`); sonradan giren istemcinin gökyüzü durumu GameTest'le kanıtlı (`sonradanGirenOyuncuGokyuzuDurumunuAlir`), ayrı karesi otomasyon olmadığı için alınmadı (bilinen sınır). | bilinen sınır |
| 9 | Uyumluluk | e4mc ile istemci: tek oyunculu dünya → Open to LAN; Cold Sweat ile istemci | çökme yok; e4mc adresi log'da; Cold Sweat yüklüyken çift sıcaklık hasarı yok (kanıt: iki modun hasar log'u ya da ölçülen can kaybı) | Cold Sweat: `-Puyum=coldsweat` filtresiz GameTest 216/216 (K-E41, `f1954dc`, `.vibe/rapor/dogrulama-entegrasyon-5-son.md`; matrisin `c646244` koşusundaki 12 hata bu kararla kapandı), S-18 çift hasar yok (`sicaklik set A -95` + `donma 100` sonrası can 20,0 → 20,0, `.vibe/kanit/matris-5/wt/istemci/s18/`); e4mc koşmadı, dış ağ ister (kullanıcı sorusu 4). | bekliyor |
| 10 | Performans | her afetin tepe anında `/forge tps` ve istemci F3 FPS; efekt kalitesi Düşük, Yüksek ve Ultra | değerler tabloda; §1'deki hedef tutuyor; TPS eşiğin altına düşünce kısmanın devreye girdiği log'da görülür | `7d393d8`'de ortak ölçüm tablosu `.vibe/kanit/matris-5/wt/fps.tsv` (158 satır): K-E29 (b) oran/mutlak ölçüsü asit-orman, kasırga 55 blok, tsunami karada, su deniz/dalga/mağara S5 Yüksek'te geçti (TS-16 rev. 3 CPU ms/kare, SU-45 kurulum CPU'su), ay ve güneşin afetsiz tabanı yok → ön değer; gizli workspace'te duvar saati FPS'i (deprem karelerinde 1 FPS) ölçülemiyor (karar 2, bilinen sınır); tepe TPS ve kısma senaryo log'larında (`forge tps`). | bilinen sınır |
| 11 | Ses | siren varsayılanı ve ayar ekranı; siren ve ortam seslerinin `ffmpeg -af ebur128` ölçümü ve spektrogram PNG'si (`docs/kanit/`) | varsayılan ~%25–30; ses ayarı ekranındaki kaydırıcının ekran görüntüsü; config değeri değişir; sirenin tepe seviyesi ve baskın frekansı tasarım notundaki "sakin ama fark edilir" hedefiyle karşılaştırıldı | 17 ogg = 17 `ebur128` = 17 spektrogram, 17/17 uygun (`.vibe/kanit/matris-5/statik/ses.txt`); uyarı-ses istemci koşusu geçerli, config önce/sonra (`.vibe/kanit/matris-5/wt/istemci/uyari/`); kareler şef incelemesinde. | yeşil |
| 12 | Uzun süreçler | `/dogalafet hiz`, `asama ileri`, `aysonrasi atla`, `donma`, `kizilcuce baslat` ile güneş patlaması (5 gün → süpernova → nebula → donma %100 → kızıl cüce → çözülme tabanı) ve ay çarpması (çarpma → meteorlar → 10 dk → deniz yükselmesi → aysız gece) uçtan uca | her aşama sırayla gelir; her aşamanın ekran görüntüsü ve log'u var | `7d393d8`'de güneş zinciri 13 kare (ön-uyarı → süpernova → güneşsiz → kızıl cüce) ve ay zinciri 18 kare (öncesi → çarpma → halka → sıfırla), her aşama sunucu log'unda (`.vibe/kanit/matris-5/wt/istemci/{gunes,ay}/`); kareler şef incelemesinde. | yeşil |
| 13 | Chunk yetişmesi | uzak bir chunk'a git, `/dogalafet donma 60`, geri dön; kızıl cüce döneminde `kizilcuce bilgi` ile kalıcı kış ve ılık bölge | dönülen chunk hedef seviyeye adım adım yetişir, TPS düşmez; kış bölgesi donmuş kalır, sınırda geçiş yumuşak | `7d393d8`'de donma istemci koşusu 12 kare (992 sn), `donma 60` + dönüş + `kizilcuce bilgi`, ilerleme kayıttan kalıcı (`.vibe/kanit/matris-5/wt/istemci/donma/`); GameTest `chunkYetismesiAdimAdimVeButceAsilmaz` (#3). | yeşil |
| 14 | Gamerule'lar | her kural `/gamerule <ad>` ile okunur ve değiştirilir | tab tamamlama çalışır; kapalı afet seçilmez, başlamış afet güvenle biter; ulaşılmış dönemler geri alınmaz | `7d393d8`'de 13 gamerule okundu, yazıldı ve yeniden açılışta korundu (`.vibe/kanit/matris-5/wt/sunucu/ek-2/ozet.txt`); kapalı afetin seçilmemesi ve başlamış afetin bitmesi GameTest'te (`kapaliKuralliAfetSecilmez`, `gercekGunesKuralKapanincaIptalKuralVeDevam`); tab tamamlama karesi otomasyon olmadığı için alınmadı (bilinen sınır). | bilinen sınır |
| 15 | Sanal su | tsunami, mağara suyu ve deniz yükselmesi öncesi ve sonrası etkilenen bölgedeki blokların sayımı ya da hash'i; GameTest ve istemcide sanal suya giren oyuncu ve mob | bloklar birebir aynı (harita bozulmadı; mağara suyu kuruduktan sonra iz yok); oyuncu sanal suda yüzer, havası azalır ve boğulur, sürüklenir, ıslanır (sıcaklık); mob'lar da etkilenir | `7d393d8`'de tsunami ve su senaryoları 0, tsunami 23 + su 9 kare (mağara önce/sonra/sualtı/kuruyor, dalga, avcı; `.vibe/kanit/matris-5/wt/istemci/{tsunami,su}/`), hash ve yüzme/boğulma GameTest'leri geçti (#3); kareler şef incelemesinde. | yeşil |
| 16 | Başarımlar | 27 başarımın her biri komut ve senaryolarla tetiklenir | her biri doğru oyuncuda açılır; toast ve sohbet duyurusu; ödüller verilir; gizli olanlar açılana kadar görünmez | 27/27 başarım GameTest'te kazanılır (#3); gerçek afetle 8/27 (#1, #3, #5, #6, #8, #12, #13, #14), başarım istemci koşusu 12 kare toast/sohbet (`.vibe/kanit/matris-5/wt/istemci/basarim/`). | yeşil |

## Faz 0 notu

Faz 0 (kurulum) iskeletin derlendiğini, JUnit ve GameTest altyapısının çalıştığını ve geliştirme sunucusunun açılıp
kapandığını gösterdi; kanıtlar `.vibe/rapor/kurulum.md`'dedir. Matris satırları şartnamedeki özellikler için tanımlıdır ve
özellikler yazılıp Faz 5'te koşturulana kadar `yapılmadı` kalır.

## Faz 0 ön koşu

#1 (derleme), #2 (birim testleri), #3 (GameTest) ve #4 (geliştirme sunucusu) satırları Faz 0 iskeleti üzerinde çalıştı
(kanıt: `.vibe/rapor/dogrulama-kurulum.md`, bağımsız doğrulayıcı 9/9). Bunlar altyapının çalıştığını gösterir; satırların
tam yeşili, özellikler yazıldıktan sonra Faz 5 koşusunu bekler.

## Faz 2a notu

Çekirdek `main`'e birleşince (`82bf1bd`) #1–#4 satırları çekirdek kanıtıyla `kısmi` oldu (yalnız çekirdek maddeleri; kanıt
hücrelerinde komut, sayı ve log yolu). Satırların tam yeşili diğer alt sistemler yazıldıktan sonra Faz 5 koşusunu bekler.

## Faz 2b notu

Altı ortak sistem `main`'e girince (birleştirmeler `a844da5` … `3e896f6`, kancalar `d62d796`, son `d303643`) #1–#4 `main`
sayılarıyla güncellendi; #6, #8, #9, #10, #11, #13, #15 ortak sistem kanıtıyla `kısmi` oldu. Hiçbir satır tam yeşil değil:
afetler (Faz 4), eşyalar (Faz 3), çok oyunculu ve üretim sunucusu koşuları (Faz 5) bekliyor. Rapor `.vibe/rapor/entegrasyon-2b.md`.


## Faz 3 notu

Beş eşya/ilerleme parçası `main`'e girince (birleştirmeler `4b725ec` … `d23653b`, bağlama `68577b1`, `1e70a46`, GameTest
yerleşimi `58251f3`) #1–#4 `main` sayılarıyla güncellendi; #6, #8, #9, #11 Faz 3 kanıtı aldı, #16 ilk kez `kısmi` oldu. Hiçbir
satır tam yeşil değil: afetler (Faz 4), görünür koşu kareleri, çok oyunculu ve üretim sunucusu koşuları (Faz 5) bekliyor. Rapor
`.vibe/rapor/entegrasyon-3.md`.

## Faz 4 notu

Altı afet parçası `main`'e girdi (birleştirmeler güneş `ded08d7`, ay `3127568`, deprem-sinkhole `455b801`, asit-orman `8048de7`,
kasırga `7224965`, tsunami `4f12577`; son kabul `main` `c55d67d`). #1–#4 her turda `main` sayılarıyla güncellendi (son: JUnit 548,
GameTest 202 ×2, altı afet senaryosu + çekirdek + sanal su senaryosu çıkış 0); #6, #8, #10, #11, #12, #15, #16 afet kanıtı aldı.
Hiçbir satır tam yeşil değil: görünür koşu kareleri ve FPS (K0-21 rev. 2 (j); kullanıcı sorusu), üretim sunucusu (#5), çok
oyunculu (#7), gamerule tab tamamlaması (#14) ve Cold Sweat/e4mc (#9) Faz 5'te (`.vibe/gorev/5-matris.md`). Rapor
`.vibe/rapor/entegrasyon-4.md`.

## Faz 5 notu

`5-entegrasyon` (`main` `553d0c2`; rapor `.vibe/rapor/entegrasyon-5.md`, kanıt `.vibe/kanit/entegrasyon-5/`): #1–#4 yukarıda.
Devralınan P2'ler ve izleme-inceleme-5 kod P2'leri GameTest'li (K-E31…K-E37). Hücrelerin "son durum" cümlesiyle yeniden
yazılması (izleme-inceleme-5 D13) ve #5–#16 `5-matris`'te.

## Faz 5 matris notu

`5-matris` (doğrulayıcı, `7d393d8`, rapor `.vibe/rapor/matris-5.md`, kanıt `.vibe/kanit/matris-5/`) ve şef kararları (KARARLAR "Şef
kararları (Faz 5)" K-E45, K-E46): **9 yeşil** (#1–#4, #11, #12, #13, #15, #16), **2 kısmi** (#6 kasırga K-17 yeniden koşusu, #7
uyarı istemcili koşu), **3 bilinen sınır** (#8, #10, #14), **2 bekliyor** (#5 installer, #9 e4mc; kullanıcı sorusu 4). #6, #11, #12,
#13 ve #15'in kareleri şef incelemesinde. Birleşik `main` (`00d1500` sonrası) istemci yeniden koşusu (#6/#10, Ts2-1 `tsunami_gorsel.py
kopuk` karesi, yeni `SuOtomasyon` kapısı) şef kararını bekliyor.

## Bilinen sınırlar

- **FPS duvar saati (#10, karar 2):** istemci gizli workspace'te koşar; compositor kısması duvar saati FPS'ini anlamsızlaştırır (deprem
  karelerinde 1 FPS). Geçerlilik ölçüsü render thread CPU ms/kare (K-E29 (b), TS-16 rev. 3, SU-45); ay ve güneş için afetsiz taban
  sahnesi olmadığından değerler ön değerdir. Duvar saati ≥ 30 FPS görünür koşu tetikleyicisidir (kullanıcı sorusu 1).
- **Sonradan giren istemcinin gökyüzü karesi (#8):** davranış GameTest `sonradanGirenOyuncuGokyuzuDurumunuAlir` ve dönem paketiyle
  kanıtlı; istemci sonradan bağlatıp kare alan otomasyon yok.
- **Gamerule tab tamamlama karesi (#14):** kurallar okunur, yazılır ve korunur; komut önerisi ekranını açan otomasyon yok.
- **Çoklu gerçek istemci (#7, karar 5):** RAM tek istemciye yeter; 2–3 oyuncu GameTest sahte oyuncularıyla.

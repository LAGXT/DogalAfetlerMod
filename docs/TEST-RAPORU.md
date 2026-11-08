# Test raporu

`docs/GOREV.md` §6 doğrulama matrisi. Her satırın kanıtı (komut, çıktı özeti, ekran görüntüsü yolu) "Kanıt"
sütununa yazılır; ekran görüntüleri ve ölçüm dosyaları `docs/kanit/` altında durur. Durum değerleri: `yapılmadı`, `kısmi`, `yeşil`,
`bilinen sınır` (davranış GameTest/senaryoyla kanıtlı, kalan kare ya da ölçüm bu makinede alınamıyor; aşağıdaki bölüm), `bekliyor`
(kullanıcı kararı ya da dış ağ). Kanıt hücreleri tek son durum cümlesidir (izleme-inceleme-5 D13); "HEAD" sütunu satırın kanıtının
alındığı commit'tir: sunucu ve GameTest `7d393d8`/`6169b46` (kabul kanıtı `6169b46` filtresiz + Cold Sweat 216/216 ×2), istemci
`matris-5-son` (`9ec1b3f`, asit-orman/yangın `6169b46`, kasırga `9d90fb7`), derleme `9d90fb7`. İstemci işleri ana makinede, ağır
derleme ve test işleri K-E44'ten sonra pc2'de koştu.

| # | Ne | Nasıl | Yeşil ölçütü | Kanıt | HEAD | Durum |
|---|---|---|---|---|---|---|
| 1 | Derleme | `./gradlew build` | çıkış 0, `build/libs/` altında reobf'lu jar | teslim derlemesi pc2'de `gradle_kapisi.sh build --rerun-tasks` çıkış 0, `warning:` 0; `cikti/dogalafetler-0.1.0.jar` 9 997 221 B, sha256 `05ffe69a…c58a51`, refmap + iki mixin config + SRG adları (aşağıda "Teslim jar'ı", `.vibe/kanit/teslimat-t32/`). | `9d90fb7` | yeşil |
| 2 | Birim testleri | `./gradlew test` | hepsi geçer: zamanlayıcı ve çarpan; şiddet ağırlıkları ve gün eşikleri; ısı alışverişi (fark büyükse hızlı, küçükse yavaş); yün %25/50/75/100; tüy yalıtımı; aşırı soğuk istisnası; çok yalıtımla sıcakta aşırı ısınma; termostatın uç sıcaklıkta azalması; yiyecek azalan verimi; kalıcı kış maskesinin aynı seed'de aynı çıkması; TPS kısması | teslim derlemesi içi `test` 551/0/0/0 (`.vibe/kanit/teslimat-t32/build.log`); madde → yöntem eşlemesi `.vibe/kanit/matris-5/statik/esleme-2-3.txt` (`7d393d8`, 548). | `9d90fb7` | yeşil |
| 3 | GameTest | `./gradlew runGameTestServer` | hepsi geçer: tüy upgrade'i (1 tüy harcanır, sayaç artar, üst sınır); yün ve termostat tarifleri (büyü, dayanıklılık, diğer upgrade'ler korunur, ikinci uygulama reddedilir); donma tablosunda dondur → çöz = başlangıç; `afetBlokHasari=false` iken blok değişmez; buz deliği açılır ve yeniden donar; 10 sahte oyuncunun sıcaklığı birbirinden bağımsız (sahte oyuncular tick almayabilir; gerekirse güncelleme testte doğrudan çağrılır); respawn'da sıfırlama; SavedData kalıcılığı; yangın yanmaz bloğu, asit dayanıklı bloğu bozmaz; aynı anda iki afet başlamaz | kabul kanıtı `6169b46` pc2 filtresiz `All 216 required tests passed` ve `-Puyum=coldsweat` 216/216, F-18 köprüsü Cold Sweat'le sınandı (`.vibe/rapor/dogrulama-entegrasyon-5-son-3.md`); `7d393d8` filtresiz 216/216 ×2 (`Exception` 2 = kasıtlı çekirdek hata testi); `9d90fb7` yalnız `client/kasirga`, `TEST_FILTRE=Kasirga` 11/11 (`.vibe/kanit/k-e49/`). | `6169b46` (+ `7d393d8`, `9d90fb7`) | yeşil |
| 4 | Geliştirme sunucusu | `runServer`, konsoldan her komut ve her afet için start/stop | log'da istisna yok, `invalid dist` yok, sunucu çökmez | `ek_sunucu.py` + dokuz `*_sunucu.py` + meteor-2 + gökyüzü/yangın-2 hepsi `senaryo sonucu 0`, `Exception`/`/ERROR]`/`invalid dist`/`session.lock` 0 (`.vibe/kanit/matris-5/wt/sunucu/`); kasırga senaryosu pc2'de 0; `yangin_sunucu` `6169b46`'da yeniden 0, koşu geçerli (`Exception` 2 eskiden beri, kasıtlı; `.vibe/kanit/matris-5-son/istemci/yangin_sunucu/`). | `7d393d8` (kasırga `00d1500`, yangın `6169b46`) | yeşil |
| 5 | Üretim sunucusu | Forge 1.20.1 installer `--installServer` ile ayrı klasör, `mods/`'a build jar'ı | açılır; her afet başlar ve biter; log temiz (reobf/refmap sorunu yok) | Forge installer `--installServer` koşmadı: installer yerel önbellekte yok, dış ağ izni yok; jar reobf'lu ve geliştirme/dedicated sunucularda sınandı, README "Kurulum" adımlarını kullanıcı Windows'ta dener (K-E50 c). | — | bilinen sınır |
| 6 | Görsel tur | `runClient` + Quick Play | her afet için ön-uyarı, siren, aktif, vitrin anı, bitiş ekran görüntüsü; ekranda hiçbir uyarı yazısı yok; siren afet başlayınca fade-out ile durur; HUD barı soğuk/nötr/sıcak hallerde ve aşırı uçta titreşir; radar ve sismograf göstergeleri yalnız eşyayı taşıyanda; şef görüntüleri açıp tasarım notlarıyla karşılaştırdı | `matris-5-son`: 15 istemci adımı çıkış 0 (K0-21 rev. 2: `no_focus=true`, ws 91, sahip girdisi 0, alarm 0), kare kapısı geçersiz 0 (su 9, tsunami 25, deprem 21, ay 23, güneş 14, radar 8, meteor 12, balıkçılık 12, başarım 12, asit-orman 19); kasırga `9d90fb7` (K-E49 `yalnizToz`) 13 kare, K-17 satırı bilgi (K-E50 b); yangın `6169b46` geçerli; kareler kullanıcı incelemesiyle kabul (K-E50 a) (`.vibe/kanit/matris-5-son/istemci/`). | `9ec1b3f`, `6169b46`, `9d90fb7` | yeşil |
| 7 | Çok oyunculu | ayrı klasörlü sunucu + 2–3 istemci (`--username A`/`B`/`C`, `online-mode=false`) | istemcilerde aynı afet aynı aşamada; afet ortasında giren oyuncu senkron; sıcaklıklar bağımsız; siren hepsine gider; "Birlikte Hayatta" 3 oyuncuyla açılır. RAM yetmezse eksik istemciler GameTest sahte oyuncularıyla tamamlanır ve raporda belirtilir | Cold Sweat'li dedicated sunucuya tek runClient (`A`) bağlandı, komutlar oyuncuya uygulandı (`.vibe/kanit/matris-5/wt/istemci/s18/kanit/`), 10 sahte oyuncu GameTest'te (#3); çoklu gerçek istemci (karar 5) ve uzak sunucudan gerçek istemciye siren (`uyari_sunucu.py --yalniz-istemci`, pc2 `ufw` portu açılmadı) bilinen sınır (K-E50 e). | `7d393d8` | bilinen sınır |
| 8 | Kalıcılık | deniz seviyesi, ayın yok olması, güneş evresi, donma ilerlemesi, kızıl cüce dönemi, olasılık çarpanı ve bir chunk'ın donma seviyesi ayarlanır → sunucu durdurulup başlatılır | değerler korunur; sonradan giren istemcide doğru gökyüzü görünür | afet, dönem, çarpan ve donma kayıttan geri geliyor, 13 gamerule yeniden açılışta korundu 13/13, meteor-2 bekleyen krater kayıttan okundu (`.vibe/kanit/matris-5/wt/sunucu/`); sonradan giren istemcinin gökyüzü durumu GameTest'le kanıtlı (`sonradanGirenOyuncuGokyuzuDurumunuAlir`), ayrı karesi otomasyon olmadığı için yok (K-E47). | `7d393d8` | bilinen sınır |
| 9 | Uyumluluk | e4mc ile istemci: tek oyunculu dünya → Open to LAN; Cold Sweat ile istemci | çökme yok; e4mc adresi log'da; Cold Sweat yüklüyken çift sıcaklık hasarı yok (kanıt: iki modun hasar log'u ya da ölçülen can kaybı) | Cold Sweat: `-Puyum=coldsweat` filtresiz GameTest 216/216 (`6169b46`, K-E41), S-18 çift hasar yok (can 20,0 → 20,0, `.vibe/kanit/matris-5/wt/istemci/s18/`); e4mc koşmadı, dış ağ izni yok (K-E50 c). | `6169b46`, `7d393d8` | bilinen sınır |
| 10 | Performans | her afetin tepe anında `/forge tps` ve istemci F3 FPS; efekt kalitesi Düşük, Yüksek ve Ultra | değerler tabloda; §1'deki hedef tutuyor; TPS eşiğin altına düşünce kısmanın devreye girdiği log'da görülür | `.vibe/kanit/matris-5-son/fps.tsv` 98 ölçüm (K-E29 b): 40 `gecti`, 30 `bilgi`, 28 `kaldi`; `kaldi` olanların hepsi duvar saati tetiği `fps<30` (kasırga S5 20, asit-orman `s5_orman` 6, tsunami S1 2). SU-45 kurulum CPU'su deniz 2,00 / dalga 2,05 / mağara 2,24 ms, bütçe aşan tick 0 (K-E48 doğrulandı). Gizli workspace'te duvar saati FPS'i anlamlı değil, görünür pencere koşusu yapılmadı (karar 2, K-E50 d); tepe TPS ve kısma senaryo log'larında. | `9ec1b3f`, `6169b46`, `9d90fb7` | bilinen sınır |
| 11 | Ses | siren varsayılanı ve ayar ekranı; siren ve ortam seslerinin `ffmpeg -af ebur128` ölçümü ve spektrogram PNG'si (`docs/kanit/`) | varsayılan ~%25–30; ses ayarı ekranındaki kaydırıcının ekran görüntüsü; config değeri değişir; sirenin tepe seviyesi ve baskın frekansı tasarım notundaki "sakin ama fark edilir" hedefiyle karşılaştırıldı | 17 ogg = 17 `ebur128` = 17 spektrogram, 17/17 uygun (`.vibe/kanit/matris-5/statik/ses.txt`); uyarı-ses istemci koşusu `9ec1b3f`'te çıkış 0, 3 kare, config önce/sonra (`.vibe/kanit/matris-5-son/istemci/uyari/`); kareler kullanıcı kabulü (K-E50 a). | `9ec1b3f` | yeşil |
| 12 | Uzun süreçler | `/dogalafet hiz`, `asama ileri`, `aysonrasi atla`, `donma`, `kizilcuce baslat` ile güneş patlaması (5 gün → süpernova → nebula → donma %100 → kızıl cüce → çözülme tabanı) ve ay çarpması (çarpma → meteorlar → 10 dk → deniz yükselmesi → aysız gece) uçtan uca | her aşama sırayla gelir; her aşamanın ekran görüntüsü ve log'u var | güneş zinciri 14 kare (ön-uyarı → süpernova → güneşsiz → kızıl cüce) ve ay zinciri 23 kare (öncesi → çarpma → halka → sıfırla), kapı 14/0 ve 23/0, her aşama sunucu log'unda (`.vibe/kanit/matris-5-son/istemci/{gunes,ay}/`); kareler kullanıcı kabulü (K-E50 a). | `9ec1b3f` | yeşil |
| 13 | Chunk yetişmesi | uzak bir chunk'a git, `/dogalafet donma 60`, geri dön; kızıl cüce döneminde `kizilcuce bilgi` ile kalıcı kış ve ılık bölge | dönülen chunk hedef seviyeye adım adım yetişir, TPS düşmez; kış bölgesi donmuş kalır, sınırda geçiş yumuşak | donma istemci koşusu 13 kare, `donma 60` uzakta + dönüş: `yetisme_60_surerken` ve `yetisme_60_bitti` kareleri (`.vibe/kanit/matris-5-son/istemci/donma/`); GameTest `chunkYetismesiAdimAdimVeButceAsilmaz` (#3); `bitti` karesinden bir saniye önce sunucuda `bekleyen=54` chunk vardı (açık nokta, KARARLAR "Açık noktalar (Faz 5)"). | `9ec1b3f` | yeşil |
| 14 | Gamerule'lar | her kural `/gamerule <ad>` ile okunur ve değiştirilir | tab tamamlama çalışır; kapalı afet seçilmez, başlamış afet güvenle biter; ulaşılmış dönemler geri alınmaz | 13 gamerule okundu, yazıldı ve yeniden açılışta korundu (`.vibe/kanit/matris-5/wt/sunucu/ek-2/ozet.txt`); kapalı afetin seçilmemesi ve başlamış afetin bitmesi GameTest'te (`kapaliKuralliAfetSecilmez`, `gercekGunesKuralKapanincaIptalKuralVeDevam`); tab tamamlama karesi otomasyon olmadığı için yok (K-E47). | `7d393d8` | bilinen sınır |
| 15 | Sanal su | tsunami, mağara suyu ve deniz yükselmesi öncesi ve sonrası etkilenen bölgedeki blokların sayımı ya da hash'i; GameTest ve istemcide sanal suya giren oyuncu ve mob | bloklar birebir aynı (harita bozulmadı; mağara suyu kuruduktan sonra iz yok); oyuncu sanal suda yüzer, havası azalır ve boğulur, sürüklenir, ıslanır (sıcaklık); mob'lar da etkilenir | `su hepsi` 9 kare ve tsunami 25 kare (köpük `kopuk` dahil, `AVCI GECERSIZ` 0, `fell out of the world` 0, `.vibe/kanit/matris-5-son/istemci/{su,tsunami}/`); hash ve yüzme/boğulma GameTest'leri geçti (#3); kareler kullanıcı kabulü (K-E50 a). | `9ec1b3f` | yeşil |
| 16 | Başarımlar | 27 başarımın her biri komut ve senaryolarla tetiklenir | her biri doğru oyuncuda açılır; toast ve sohbet duyurusu; ödüller verilir; gizli olanlar açılana kadar görünmez | 27/27 başarım GameTest'te kazanılır (#3); gerçek afetle 12/27 kanıtlı (liste `.vibe/rapor/matris-5.md` #16), başarım istemci koşusu 12 kare toast/sohbet, kapı 12/0 (`.vibe/kanit/matris-5-son/istemci/basarim/`). | `9ec1b3f` | yeşil |

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

## Faz 5 matris notu (ara sayım; son durum bir sonraki bölümde)

`5-matris` (doğrulayıcı, `7d393d8`, rapor `.vibe/rapor/matris-5.md`, kanıt `.vibe/kanit/matris-5/`) ve şef kararları (KARARLAR "Şef
kararları (Faz 5)" K-E45, K-E46): **9 yeşil** (#1–#4, #11, #12, #13, #15, #16), **2 kısmi** (#6 kasırga K-17 yeniden koşusu, #7
uyarı istemcili koşu), **3 bilinen sınır** (#8, #10, #14), **2 bekliyor** (#5 installer, #9 e4mc; kullanıcı sorusu 4). #6, #11, #12,
#13 ve #15'in kareleri şef incelemesinde. Birleşik `main` (`00d1500` sonrası) istemci yeniden koşusu (#6/#10, Ts2-1 `tsunami_gorsel.py
kopuk` karesi, yeni `SuOtomasyon` kapısı) şef kararını bekliyor.

## Faz 5 son durum (kapanış, K-E50)

Doğrulayıcı-3 `matris-5-son` (rapor `.vibe/rapor/matris-5-son.md`, kanıt `.vibe/kanit/matris-5-son/`) ve kullanıcının kapanış kararı
K-E50 ("test tekrarı yok"; kareler kullanıcı incelemesiyle kabul) sonrası §6: **10 yeşil** (#1–#4, #6, #11, #12, #13, #15, #16),
**6 bilinen sınır** (#5 ve #9 dış ağ K-E50 c, #7 karar 5 ve K-E50 e, #8 ve #14 otomasyon yok K-E47, #10 duvar saati FPS K-E50 d),
**0 kısmi**, **0 bekliyor**. `matris-5-son`'da 15 istemci adımı çıkış 0; SU-45 kapandı (2,00 / 2,05 / 2,24 ms, bütçe aşan tick 0);
kasırga K-17 `9d90fb7` koşusunda `s5_vitrin` tuttu (%53), `s5_taban_uzak` tutmadı (%10) → K-E49 yedek yolu (K-E50 b). S18 istemci
koşusu iki koşuda "girdi sayıları satırı yok" ile geçersiz sayıldı (betik; sunucu tarafı ve can ölçümü geçerli, açık nokta).

## Üretim jar'ında test afeti

`OyunTestiAfeti` sınıfları jar'da var ama yalnız `dogalafetler.oyunTestiAfeti` sistem özelliğiyle kaydedilir (`DogalAfetler.java:70`
`OyunTestiAfeti.ozelliktenKaydet()`, özellik yoksa hiçbir şey yapmaz; özelliği yalnız `build.gradle`'ın `-PoyunTestiAfeti` run ayarı verir):
`cekirdek_sunucu.py`'nin bayraklı kosu1/kosu2'sinde 4'er "Oyun testi" satırı (kasıtlı taklit denetimi), bayraksız kosu3'te ve `5-matris`'in
diğer 22 senaryo log'unda 0 (`.vibe/kanit/k-e44-dogrulama/sunucu-cekirdek/`, `.vibe/kanit/matris-5/wt/sunucu/`); jar `00d1500`
(sha256 `687a9cef…4800a`), kayıt yolu o tarihten beri değişmedi. Üretim sunucusu koşusu (#5) bilinen sınır (K-E50 (c): dış ağ yok).

## Teslim jar'ı

`cikti/dogalafetler-0.1.0.jar` (git dışı; sürüm `0.1.0`, K-E50 (f)): pc2'de `gradle_kapisi.sh build --rerun-tasks` (`main` `9d90fb7`
kaynakları, `MAKINE=LAGXT2`) çıkış 0, `warning:` 0, JUnit **551/0/0/0**; jar `uzak.sh -g build/libs/` ile geri çekildi.

| Özellik | Değer |
|---|---|
| SHA-256 | `05ffe69a26e98e55e049dfebb8869a3cbbd91764ae284f3f0789034c9bc58a51` |
| Boyut | 9 997 221 B; 1329 girdi, 794 `.class` (`jar tf \| grep -c class`) |
| `META-INF/mods.toml` | `modId=dogalafetler`, `version=0.1.0`, `displayTest=MATCH_VERSION`, `license=MIT` |
| Mixin | manifest `MixinConfigs` iki config (`dogalafetler.su.mixins.json`, `dogalafetler.client.su.mixins.json`); `dogalafetler.refmap.json` `EntityMixin` 2, `LiquidBlockRendererMixin` 1 (ör. `Entity;m_20323_()V`) |
| Reobf | `javap -p -c EntityMixin` SRG adları (`f_19798_`, `m_5841_`, …); 294 sınıfta SRG adı, Mojang adı (`getGameTime`, `getBlockState`) içeren sınıf 0 |

GameTest bu jar için yeniden koşmadı (kullanıcı kararı, K-E50): kabul kanıtı `6169b46` filtresiz 216/216 ve `-Puyum=coldsweat` 216/216
(`.vibe/rapor/dogrulama-entegrasyon-5-son-3.md`); `9d90fb7` yalnız `client/kasirga` değiştirdi, `TEST_FILTRE=Kasirga` 11/11 (K-E49).
Kanıt `.vibe/kanit/teslimat-t32/` (`build.log`, `jar.txt`).

## Güncelleme turu 3 notu (`is/entegrasyon-8`, 2026-11-08)

Dört dal (afet-zaman-3 K-E67, blok-3 K-E68, sıcaklık-3 K-E69, afet-fizik-3 K-E70) ve protokol 9 (K-E71). Son kod HEAD `02bff06`,
kuyruktan: #1 build `build @88d973b1fecd` BUILD SUCCESSFUL; #2 JUnit 854/0 `test @88d973b1fecd`; #3 filtresiz GameTest 313/313
`gametest @19a78927fd26` (protokol 9, 15 paket; filtresiz koşuyu atlama kararı ilk koşu sürerken geldi, o koşu yeni bir test
kusurunu yakaladı, düzeltmeden sonraki koşu geçti; kabul ona dayanmaz); `runData` üç kez written 0; N19 270/270 fark 0. Ara koşular:
`5513a72` GameTest 181/181, `91b7a76` GameTest 80/80. #6/#10 görsel ve ölçüm koşuları kullanıcı kararıyla yapılmadı ya da durduruldu:
sıcaklık, kasırga ve genel doğruluk kullanıcının oyun içi doğrulamasıyla kabul (KARARLAR K-E71 (f), "Bilinen sınırlar (güncelleme
turu 3)"). Tsunami yağmuru kullanıcı kararıyla varsayılan kapalı. Kanıt `.vibe/kanit/entegrasyon-8/`, rapor
`.vibe/rapor/entegrasyon-8.md`.

## Bilinen sınırlar

- **FPS duvar saati (#10, karar 2):** istemci gizli workspace'te koşar; compositor kısması duvar saati FPS'ini anlamsızlaştırır (deprem
  karelerinde 1 FPS). Geçerlilik ölçüsü render thread CPU ms/kare (K-E29 (b), TS-16 rev. 3, SU-45); ay ve güneş için afetsiz taban
  sahnesi olmadığından değerler ön değerdir. Duvar saati ≥ 30 FPS görünür koşu tetikleyicisidir; görünür pencere koşusu yapılmadı
  (K-E50 d). `matris-5-son`'daki 28 `kaldi` ölçümün hepsi bu tetikle (`fps<30`).
- **Sonradan giren istemcinin gökyüzü karesi (#8):** davranış GameTest `sonradanGirenOyuncuGokyuzuDurumunuAlir` ve dönem paketiyle
  kanıtlı; istemci sonradan bağlatıp kare alan otomasyon yok.
- **Gamerule tab tamamlama karesi (#14):** kurallar okunur, yazılır ve korunur; komut önerisi ekranını açan otomasyon yok.
- **Çoklu gerçek istemci (#7, karar 5):** RAM tek istemciye yeter; 2–3 oyuncu GameTest sahte oyuncularıyla. Uzak (pc2) sunucudan
  gerçek istemciye siren koşusu pc2 `ufw` portu açılmadığı için yapılmadı (K-E50 e).
- **Üretim sunucusu (#5) ve e4mc (#9), K-E50 (c):** Forge installer `--installServer` ve e4mc dış ağ ister; installer yerel önbellekte
  yok. README "Kurulum" ve "Arkadaşlarla oynamak" adımlarını kullanıcı Windows'ta dener.
- **Kasırga K-17 taban rengi (K-E50 b):** `kasirga_gorsel.py` `taban rengi` satırı bilgi; palet `TOZ` 0,80/0,56/0,30 sabiti ve gözle kabul.

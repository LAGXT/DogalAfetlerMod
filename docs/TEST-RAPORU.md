# Test raporu

`docs/GOREV.md` §6 doğrulama matrisi. Her satırın kanıtı (komut, çıktı özeti, ekran görüntüsü yolu) "Kanıt"
sütununa yazılır; ekran görüntüleri ve ölçüm dosyaları `docs/kanit/` altında durur. Durum değerleri: `yapılmadı`, `kısmi`, `yeşil`.

| # | Ne | Nasıl | Yeşil ölçütü | Kanıt | Durum |
|---|---|---|---|---|---|
| 1 | Derleme | `./gradlew build` | çıkış 0, `build/libs/` altında reobf'lu jar | — | yapılmadı |
| 2 | Birim testleri | `./gradlew test` | hepsi geçer: zamanlayıcı ve çarpan; şiddet ağırlıkları ve gün eşikleri; ısı alışverişi (fark büyükse hızlı, küçükse yavaş); yün %25/50/75/100; tüy yalıtımı; aşırı soğuk istisnası; çok yalıtımla sıcakta aşırı ısınma; termostatın uç sıcaklıkta azalması; yiyecek azalan verimi; kalıcı kış maskesinin aynı seed'de aynı çıkması; TPS kısması | — | yapılmadı |
| 3 | GameTest | `./gradlew runGameTestServer` | hepsi geçer: tüy upgrade'i (1 tüy harcanır, sayaç artar, üst sınır); yün ve termostat tarifleri (büyü, dayanıklılık, diğer upgrade'ler korunur, ikinci uygulama reddedilir); donma tablosunda dondur → çöz = başlangıç; `afetBlokHasari=false` iken blok değişmez; buz deliği açılır ve yeniden donar; 10 sahte oyuncunun sıcaklığı birbirinden bağımsız (sahte oyuncular tick almayabilir; gerekirse güncelleme testte doğrudan çağrılır); respawn'da sıfırlama; SavedData kalıcılığı; yangın yanmaz bloğu, asit dayanıklı bloğu bozmaz; aynı anda iki afet başlamaz | — | yapılmadı |
| 4 | Geliştirme sunucusu | `runServer`, konsoldan her komut ve her afet için start/stop | log'da istisna yok, `invalid dist` yok, sunucu çökmez | — | yapılmadı |
| 5 | Üretim sunucusu | Forge 1.20.1 installer `--installServer` ile ayrı klasör, `mods/`'a build jar'ı | açılır; her afet başlar ve biter; log temiz (reobf/refmap sorunu yok) | — | yapılmadı |
| 6 | Görsel tur | `runClient` + Quick Play | her afet için ön-uyarı, siren, aktif, vitrin anı, bitiş ekran görüntüsü; ekranda hiçbir uyarı yazısı yok; siren afet başlayınca fade-out ile durur; HUD barı soğuk/nötr/sıcak hallerde ve aşırı uçta titreşir; radar ve sismograf göstergeleri yalnız eşyayı taşıyanda; şef görüntüleri açıp tasarım notlarıyla karşılaştırdı | — | yapılmadı |
| 7 | Çok oyunculu | ayrı klasörlü sunucu + 2–3 istemci (`--username A`/`B`/`C`, `online-mode=false`) | istemcilerde aynı afet aynı aşamada; afet ortasında giren oyuncu senkron; sıcaklıklar bağımsız; siren hepsine gider; "Birlikte Hayatta" 3 oyuncuyla açılır. RAM yetmezse eksik istemciler GameTest sahte oyuncularıyla tamamlanır ve raporda belirtilir | — | yapılmadı |
| 8 | Kalıcılık | deniz seviyesi, ayın yok olması, güneş evresi, donma ilerlemesi, kızıl cüce dönemi, olasılık çarpanı ve bir chunk'ın donma seviyesi ayarlanır → sunucu durdurulup başlatılır | değerler korunur; sonradan giren istemcide doğru gökyüzü görünür | — | yapılmadı |
| 9 | Uyumluluk | e4mc ile istemci: tek oyunculu dünya → Open to LAN; Cold Sweat ile istemci | çökme yok; e4mc adresi log'da; Cold Sweat yüklüyken çift sıcaklık hasarı yok (kanıt: iki modun hasar log'u ya da ölçülen can kaybı) | — | yapılmadı |
| 10 | Performans | her afetin tepe anında `/forge tps` ve istemci F3 FPS; efekt kalitesi Düşük, Yüksek ve Ultra | değerler tabloda; §1'deki hedef tutuyor; TPS eşiğin altına düşünce kısmanın devreye girdiği log'da görülür | — | yapılmadı |
| 11 | Ses | siren varsayılanı ve ayar ekranı; siren ve ortam seslerinin `ffmpeg -af ebur128` ölçümü ve spektrogram PNG'si (`docs/kanit/`) | varsayılan ~%25–30; ses ayarı ekranındaki kaydırıcının ekran görüntüsü; config değeri değişir; sirenin tepe seviyesi ve baskın frekansı tasarım notundaki "sakin ama fark edilir" hedefiyle karşılaştırıldı | — | yapılmadı |
| 12 | Uzun süreçler | `/dogalafet hiz`, `asama ileri`, `aysonrasi atla`, `donma`, `kizilcuce baslat` ile güneş patlaması (5 gün → süpernova → nebula → donma %100 → kızıl cüce → çözülme tabanı) ve ay çarpması (çarpma → meteorlar → 10 dk → deniz yükselmesi → aysız gece) uçtan uca | her aşama sırayla gelir; her aşamanın ekran görüntüsü ve log'u var | — | yapılmadı |
| 13 | Chunk yetişmesi | uzak bir chunk'a git, `/dogalafet donma 60`, geri dön; kızıl cüce döneminde `kizilcuce bilgi` ile kalıcı kış ve ılık bölge | dönülen chunk hedef seviyeye adım adım yetişir, TPS düşmez; kış bölgesi donmuş kalır, sınırda geçiş yumuşak | — | yapılmadı |
| 14 | Gamerule'lar | her kural `/gamerule <ad>` ile okunur ve değiştirilir | tab tamamlama çalışır; kapalı afet seçilmez, başlamış afet güvenle biter; ulaşılmış dönemler geri alınmaz | — | yapılmadı |
| 15 | Sanal su | tsunami, mağara suyu ve deniz yükselmesi öncesi ve sonrası etkilenen bölgedeki blokların sayımı ya da hash'i; GameTest ve istemcide sanal suya giren oyuncu ve mob | bloklar birebir aynı (harita bozulmadı; mağara suyu kuruduktan sonra iz yok); oyuncu sanal suda yüzer, havası azalır ve boğulur, sürüklenir, ıslanır (sıcaklık); mob'lar da etkilenir | — | yapılmadı |
| 16 | Başarımlar | 27 başarımın her biri komut ve senaryolarla tetiklenir | her biri doğru oyuncuda açılır; toast ve sohbet duyurusu; ödüller verilir; gizli olanlar açılana kadar görünmez | — | yapılmadı |

## Faz 0 notu

Faz 0 (kurulum) iskeletin derlendiğini, JUnit ve GameTest altyapısının çalıştığını ve geliştirme sunucusunun açılıp
kapandığını gösterdi; kanıtlar `.vibe/rapor/kurulum.md`'dedir. Matris satırları şartnamedeki özellikler için tanımlıdır ve
özellikler yazılıp Faz 5'te koşturulana kadar `yapılmadı` kalır.

## Faz 0 ön koşu

#1 (derleme), #2 (birim testleri), #3 (GameTest) ve #4 (geliştirme sunucusu) satırları Faz 0 iskeleti üzerinde çalıştı
(kanıt: `.vibe/rapor/dogrulama-kurulum.md`, bağımsız doğrulayıcı 9/9). Bunlar altyapının çalıştığını gösterir; satırların
tam yeşili, özellikler yazıldıktan sonra Faz 5 koşusunu bekler.

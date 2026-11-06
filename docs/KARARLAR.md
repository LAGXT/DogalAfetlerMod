# Kararlar

Belirsizlikte seçilen yol ve gerekçesi (docs/GOREV.md, SARTNAME N17). Her kararın başında kimliği ve onu veren parça
vardır. Parça kararları önce `.vibe/kararlar/<parça>.md`'ye yazılır, entegratör birleştirmede buraya işler.

## Faz 0 — Kurulum

**K0-01 Lisans: MIT.** Berk serbest bıraktı. MIT kısa, yaygın ve modun arkadaşlarla paylaşılmasını, çatallanmasını ve
modpack'lere girmesini engellemez. Metin `LICENSE`'ta, `gradle.properties: mod_license=MIT`. Üretilen ses/doku
dosyaları da projenin parçası olarak aynı lisanstır; dışarıdan alınan dosya olursa lisansı `docs/ASSET-LISANS.md`'ye yazılır.

**K0-02 Kimlikler.** `mod_id=dogalafetler`, kök paket `io.github.kadirberkpolat1.dogalafetler`, yazar "Kadir Berk Polat",
görünen ad "Doğal Afetler", sürüm `0.1.0`, jar `dogalafetler-<sürüm>.jar`. Gradle `gradle.properties`'i ISO-8859-1
okuduğu için değerlerdeki Türkçe harfler `\uXXXX` kaçışıyla yazılır. `mods.toml`: `displayTest="MATCH_VERSION"`
(mod hem sunucuda hem istemcide gerekir; sürüm uyuşmazlığı bağlantı ekranında görünür).

**K0-03 Paketler ve sahipleri.** GOREV §4'teki paketler açıldı: `core`, `network`, `config`, `komut`, `kural` gerçek
iskeletle; `uyari`, `sicaklik`, `su`, `yangin`, `gokyuzu`, `donma`, `esya`, `meteor`, `yiyecek`, `balikcilik`, `basarim`, `afet`,
`client` sahipliği açıklayan `package-info.java` ile. Ölçüte ek olarak **`veri`** paketi açıldı (datagen kayıt noktası,
çekirdek işçisinin): ortak üretilen dosyaların (dil, sounds.json) tek sahibi olsun diye. GameTest sınıfları test ettikleri
paketin içinde, `<Konu>OyunTestleri` adıyla durur (ör. `kural/KuralOyunTestleri`); JUnit testleri `src/test/java`'da aynı
paket adıyla.

**K0-04 Ana sınıf kurucusu `FMLJavaModLoadingContext` parametresi alır.** Forge 47.4.28'de
`FMLJavaModLoadingContext.get()` ve `ModLoadingContext.get()` kaldırılmak üzere işaretli; MDK örneği de parametreli
kurucuyu kullanıyor. Aynı nedenle `new ResourceLocation(ns, yol)` yerine `ResourceLocation.fromNamespaceAndPath`
(`DogalAfetler.konum(yol)`) kullanılır. Derleme uyarısız.

**K0-05 Gamerule'lar: 13 kural, kategori `MISC`, kayıt ortak kurulumda.** Madde 11'de 13 kural sayılıyor
(`dogalAfetlerAcik`, sekiz afet anahtarı, `afetSikligi`, `afetMaksSiddet`, `afetBlokHasari`, `sicaklikSistemi`); kurulum görev
metnindeki "14" sayısı şartnameyle çelişiyor, şartname kazanır (GOREV girişi). `GameRules.register` thread-safe
olmadığı için `Kurallar` sınıfı `FMLCommonSetupEvent.enqueueWork` içinde ana thread'de yüklenir. Vanilla
`IntegerValue` aralık denetlemediği için değerler okunurken sıkıştırılır: `afetMaksSiddet` [1, 5] (madde 11),
`afetSikligi` [0, 1000] (üst sınır bu kararın: %1000 = on kat; daha yüksek değer bekleme kuralı nedeniyle zaten fark
yaratmaz). Okuyucular `Kurallar.maksSiddet(...)`, `Kurallar.siklikCarpani(...)`, `Kurallar.afetSecilebilir(...)` kullanır.

**K0-06 `/dogalafet` kayıt kancası.** Kök `DogalAfetKomutu`'nda, `requires(hasPermission(2))`. Diğer paketler mod
inşasında `KomutKaydi.ekle(LiteralArgumentBuilder)` ya da argümanları `CommandBuildContext` isteyen komutlar için
`KomutKaydi.ekle(baglam -> ...)` çağırır; komutlar her `RegisterCommandsEvent`'te (sunucu açılışı, `/reload`) yeniden kurulur.
Aynı adlı iki alt komut Brigadier'da sessizce birleşeceği için `IllegalStateException` ile reddedilir. Faz 0'da `durum`
"Henüz afet yok." döner; gerçek içeriği (N18) çekirdek işçisi yazar.

**K0-07 Ağ kanalı.** Tek `SimpleChannel` `dogalafetler:ana`, protokol sürümü `"1"`, istemci ve sunucu sürümü birebir
eşleşmeli (mod iki tarafta da zorunlu). Paket kimlikleri kayıt sırasına göre verilir; bu yüzden paket kayıtları
her çalışmada aynı sırayla, ortak kurulumda yapılmalıdır. Protokol değişince sürüm artırılır.

**K0-08 Config.** `SERVER` → `dogalafetler-server.toml` (dünya başına, istemciye senkron), `CLIENT` →
`dogalafetler-client.toml`. Faz 0'da `sicaklik.sicaklikSistemiModu = OTOMATIK|ACIK|KAPALI` (varsayılan `OTOMATIK`, N8) ve
`gorsel.efektKalitesi = DUSUK|ORTA|YUKSEK|ULTRA` (varsayılan `YUKSEK`, N13). Enum adları ASCII'dir; TOML'de büyük harf
yazılır.

**K0-09 Datagen kayıt noktası.** `veri.DataGenerators` tek `en_us`, tek `tr_tr` ve tek `sounds.json` provider'ı kurar;
alt sistemler bunlara katkı ekler (`ceviriEkle(y -> y.ekle(anahtar, ingilizce, turkce))`, `sesEkle(s -> s.tanimla(...))`).
Kendi dosyalarını yazan provider'lar (tarif, loot, advancement, etiket) `saglayiciEkle(olay -> ...)` ile eklenir. Aynı
dil anahtarını iki katkı yazarsa datagen hata verir (çakışma sessiz kalmaz). Hiç ses tanımı yokken Forge `sounds.json`
yazmaz; ilk ses eklendiğinde dosya oluşur. Üretilen dosyalar commit'lenir; zaman damgası taşıyan
`src/generated/resources/.cache/` her `runData`'da değiştiği için `.gitignore`'dadır.

**K0-10 Çalışma klasörleri.** `runClient` → `run/`, `runServer` → `run-server/`, `runGameTestServer` → `run-gametest/`,
`runData` → `run-data/`; aynı anda çalışabilsinler diye ayrı (GOREV §2). Hepsi `run*/` kalıbıyla `.gitignore`'da. Yeni
klon/worktree'de el işi kalmasın diye `runServer` ilk açılışta `run-server/eula.txt` (`eula=true`) ve
`server.properties`'i (`online-mode=false`, `level-name=test`, `level-seed=20261105`) yoksa yazar; `runClient`
`run/options.txt`'e `pauseOnLostFocus:false` ekler; `runGameTestServer` boş bir `run-gametest/server.properties` yazar
(GameTest sunucusu bu dosyayı okur, yoksa log'a zararsız ama yanıltıcı bir `NoSuchFileException` ERROR'u düşer). Konsol
log seviyesi `info` (MDK'nın `debug`'ı kanıt dosyalarını şişiriyordu; ayrıntı `logs/debug.log`'da kalır).

**K0-11 Test dünyası.** Seed `20261105` (görev tarihi; `gradle.properties: test_dunyasi_seed`). Sunucu dünyayı
`level-name=test` ile üretir, böylece `level.dat` içindeki `LevelName` de `test` olur; `tools/dunya/test_dunyasi_hazirla.py`
onu `run/saves/test`'e kopyalar ve `allowCommands=1` yapar (dedicated server dünyasında hileler kapalı gelir; tek
oyunculu Quick Play'de `/dogalafet` için gerekir). Quick Play isteğe bağlıdır: `./gradlew runClient -PquickPlay=test`.

**K0-12 GameTest şablonu `dogalafetler:bos`.** 1x1x1, tek hava bloğu, DataVersion 3465 (1.20.1);
`tools/yapilar/bos_yapi_uret.py` standart kütüphaneyle üretir (gzip `mtime=0`, aynı girdi → aynı bayt). Testler
`@GameTestHolder("dogalafetler")` + `@PrefixGameTestTemplate(false)` + `@GameTest(template = "bos")` kullanır.

**K0-13 JDK 17.** Sistemde yalnız Java 21 (mise) vardı. Sistem paketi kurulmadan Temurin 17.0.21+9 arşivi Adoptium
API'den `~/.jdks/jdk-17.0.21+9`'a açıldı; sistem varsayılanı değişmedi. Gradle `JAVA_HOME` ile çalışır, toolchain aynı
JDK'yı bulur.

**K0-14 Uyumluluk modları.** `-Puyum=yok|e4mc|coldsweat|ikisi` (varsayılan `yok`; geçersiz değer derlemeyi durdurur).
Modrinth maven `content { includeGroup 'maven.modrinth' }` ile yalnız bu gruba açıktır. `exclusiveContent` denendi ve
reddedildi: `fg.deobf` aynı gruptaki `*_mapped_official_1.20.1` yapıtını ForgeGradle'ın kendi deobf deposundan verir,
`exclusiveContent` o depoyu da engellediği için iki bağımlılık `FAILED` çözüldü. e4mc `6.2.3-forge` sürüm numarasıyla;
Cold Sweat `2.4.5` numarası 1.16.5–1.21.1 arası altı dosyada tekrarlandığı için Forge 1.20.1 dosyasının Modrinth sürüm
kimliğiyle (`z3qJDJxm`) bağlanır. Güncellemede `gradle.properties`'teki iki değer değişir.

**K0-15 Türkçe yerel ayar.** Makine `tr_TR.UTF-8`; Berk'in Windows'u da büyük olasılıkla Türkçe. Geliştirme ve test
bu yerelde koşar ki Türkçe "I/İ" hataları erken görülsün. Kodda `toLowerCase`/`toUpperCase` daima `Locale.ROOT` ile
yapılır (`AfetTuruTest.kimlikTurkceYereldeBuyukHarfleCozulur`).

**K0-16 Git kimliği.** Makinede global `user.name`/`user.email` yok ve global ayar değiştirilmez; commit'ler
`git -c user.name='Kadir Berk Polat' -c user.email='kadirberkpolat1@users.noreply.github.com' commit ...` ile atılır.
Varsayılan dal `main`.

**K0-17 Python.** Proje betikleri `KÖK/.venv`'deki Python'u kullanır (`tools/requirements.txt`: Pillow, numpy, nbtlib);
sistem Python paketlerine güvenilmez.

**K0-18 Sunucu konsolu.** `runServer` görevi `standardInput = System.in` ile Gradle'ın stdin'ini sunucuya iletir;
ForgeGradle bunu varsayılan olarak yapmıyor ve konsola yazılan `stop` sunucuya ulaşmıyordu. Otomasyonda sunucu
**pty'siz** ve stdin'i bir boruya bağlı açılır (ör. Python `subprocess.Popen(..., stdin=PIPE)`; `Done (` görülünce
`stop\n` yazılır). pty'li arka plan servisi olarak açılan Gradle sarmalayıcısı terminal yüzünden durduruldu (`T` durumu)
ve çıktı akmadı; bu yol kullanılmaz. Sunucu işletim sistemi sinyaliyle de (SIGTERM) dünyayı kaydederek kapanır, ama
kabul kanıtı `stop` komutudur.

## Faz 2a — Çekirdek

Parça `cekirdek` (dal `is/cekirdek`, son commit `1369ae7`; `main`'e birleştirme `82bf1bd`). Kaynak
`.vibe/kararlar/cekirdek.md` §B/§C/§D; diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır ve Faz 2b
işçileri oradan okur. Metindeki §1…§13 göndermeleri o dosyanın §A bölümlerine aittir. Şef kararları (K-C28, K-C29,
K-C30, K-C41, K-C42 ve K-C46'nın Z3 bölümü) "Şef kararları (Faz 2a)" bölümündedir; N17 satırları sondaki tablodadır.
Rapor ve kanıtlar: `.vibe/rapor/cekirdek.md`, `.vibe/kanit/cekirdek/`.

**K-C1 Yaşam döngüsü arayüz değil soyut sınıf.** Aşama, aşama süresi, alt aşama, etkilenen/ölen takibi ve NBT biçimi
her afette aynı olmalı; arayüzde bunlar ya yöneticide (afetten ayrı, kayıtta iki parça) ya da her afette tekrar
yazılırdı. Final metotlar sırayı korur (ör. `baslat` iki kez çağrılamaz, `temizle` bir kez).

**K-C2 Zamanlayıcı: tür başına bağımsız oran.** λ_t = `gunlukAfetOrani × afetSikligi × çarpan_t × ağırlık_t / Σağırlık`
(afet/gün); kontrol aralığında olay olasılığı `1 − e^(−Λ·adım/24000)`, tür λ_t/Λ ile seçilir. Kapalı, beklemedeki ya da tek
seferliği tükenmiş tür Λ'ya girmez (kalanlar sıklaşmaz). Oyuncu sayısı hiçbir yerde yok (madde 3). Kontrol saniyede bir;
olasılık adıma göre ölçeklendiği için sıklığı değiştirmez.

**K-C3 Art arda 4–5, katı okuma.** Madde 10 "Seviye 4-5 afetler ardışık gelmesin (aynı seviye art arda en fazla bir kez)".
Önceki seviyeli afet 4 ya da 5 idiyse bu seçimde 4 ve 5'in ikisi de dıştadır; parantezdeki kural bunun özel hâlidir ve
yüksek seviyeler daha da nadirleşir. Seviyesiz afetler (güneş, ay) geçmişi sıfırlamaz. Berk değiştirebilir.

**K-C4 Şiddet ağırlığı.** Seviye L'nin ağırlığı `k^(L−1) × rampa`; `k` gün 0'da 0,35'ten gün 120'de 0,6'ya doğrusal (k &lt; 1 →
yüksek seviye her zaman daha nadir); yeni açılan seviye ağırlığının %20'siyle başlar, 15 günde tama çıkar. Gün 90:
S1 %48,9, S2 %26,3, S3 %14,1, S4 %7,6, S5 %3,0. Gün 120+: %43,4 / %26,0 / %15,6 / %9,4 / %5,6 (`.vibe/kanit/cekirdek/simulasyon.txt`). `afetMaksSiddet` üstü keser. İsteğe bağlı
gelişim kriteri açıksa şiddet günü `min(gün, puan × 2)` olur.

**K-C5 Bekleme bitişten başlar.** Genel ve tür beklemesi afet bitince (her nedenle, ON_UYARI iptali hariç) kurulur;
şiddetli afette `1 + 0,25 × max(0, seviye−2)` kat uzar; seviyesiz afet 5 sayılır. N14 ertelemesi yalnız tür beklemesidir;
kısa bekleme uzunu silmez. `/dogalafet start` beklemeyi atlar ama kendisi de bitince bekleme kurar.

**K-C6 Tek seferlik ve geç oyun kapıları.** Ay çarpması: ay varsa. Güneş patlaması: güneş `NORMAL` iken (ya da
`gunesPatlamasiTekrarlanabilir` açıkken `KIZIL_CUCE`); büyüyen/süpernova/güneşsiz evrede güneş patlaması seçilmez. İkisi
de oyun günü ve gelişim puanı eşiğini bekler (bölüm D "yeterince geliştiğinde"; bölüm H "aynı mantıkta" → ay
çarpmasına da daha düşük eşik). Puan = XP seviyesi + zırh puanı; çevrim içi oyuncuların ortalaması ya da en gelişmişi.

**K-C7 Gamerule kapanınca süren afet.** Madde 11 iki cümlesi birleştirildi: (a) ON_UYARI'daki (henüz duyurulmamış)
afet `IPTAL` ile temizce kaldırılır, bekleme kurmaz; (b) sunucu geneli afetin (güneş/ay) bitmemiş süreci `KURAL` ile
yumuşak durdurulur ("yalnızca henüz tamamlanmamış süreçler durdurulsun"); ulaşılmış dönem alanlarına dokunulmaz;
(c) diğer süren afetler doğal bitişine kadar sürer ("başlamış afet güvenli şekilde bitirilsin"). Afet yuvası dışındaki
dönem süreçleri için K-C25/K-C41, durdurulan güneş sürecinin evresi için K-C26. Elle başlatılan afet gamerule'den
etkilenmez; bu muafiyet yalnız afet yuvası içindir (inceleme-2 Y5): elle başlatılmış güneş patlamasının devamı olan dönem
süreci kural kapalıysa ilk denetimde `KURAL` ile durur (bilinen sınır, kod değişmedi); güneş patlaması afeti sürerken bağlı
bir sürecin durması `BUYUYEN`'i geri almaz. `dogalAfetlerAcik=false` → hiç seçim yok; süren afet için (a)–(c) geçerli.

**K-C8 Hedef seçimi.** Adaylar: config boyutlarındaki (N11) izleyici olmayan canlı çevrim içi oyuncular. İki ve daha
çok adayda son hedef dışlanır; kalanlar karıştırılır, son afet merkezine 128 bloktan yakın olanlar sona alınır (madde 4
"bir oyuncunun çevresinde art arda afet çıkmasın"; yasak değil tercih — hep birlikte oynayan grupta afet hiç çıkmaz
olmasın). Sıradaki her aday için `merkezSec` + `yerUygunMu`; hiçbiri uygun değilse tür `ertelemeTick` ertelenir ve
beklemede olmayan türlerden ağırlıkla sıradaki afet seçilir (N14). Sunucu geneli afetin hedef oyuncusu yoktur (merkez
dünya doğma noktası).

**K-C9 Etki alanı yatay; etkilenebilecek = alan + 64.** Mağaradaki ve çatıdaki oyuncu afetin altındadır. Siren ve afet
durum paketi etki alanı + `sirenEkYaricap` (64; eski ad `etkilenmeEkMesafesi`) içindekilere gider (madde 3 "etkilenebilecek diğer oyuncular da
sireni duysun"); global afette herkese (N11: Nether/End dahil).

**K-C10 Sağ çıkan.** Takip AKTIF'te başlar (yalnız sirenden kaçan afeti yaşamamıştır). Etkilenmiş oyuncu afet bitene kadar
nerede ölürse ölsün düşer; bitişte çevrim dışı olan sayılmaz (çıkıp girerek başarım almayı engeller).

**K-C11 Ölçekli afet saati; hız kalıcı değil.** Bütün zamanlayıcılar tek saatte; `hiz` yalnız bu saati hızlandırır (dünya
zamanına dokunmaz). Hız yeniden başlatmada 1'e döner: test aracı unutulursa dünya kalıcı hızlı akmasın.

**K-C12 Oyun günü** — yerini K-C29 aldı (`getGameTime()`). `/dogalafet gun N` ofseti `N − şimdiki gün` yapar; dünya
saatine dokunmaz.

**K-C13 Senkron.** Dönem paketi tam durumdur ve toplanır (acil alanlar hemen); afet durum paketi aşama değişiminde ve
1 Hz; ON_UYARI hiçbir istemciye gitmez (madde 4: eşyası olmayan için oyun normal sürer; radar verisini esya kendi
paketiyle yalnız eşyayı tutana yollar).

**K-C14 İstemci teslimi `DistExecutor.safeCallWhenOn`.** Görev metni `safeRunWhenOn` istiyordu; `SafeRunnable`
parametresizdir ve Forge geliştirme ortamında `validateSafeReferent` yakalayan lambdayı (`() -> X.isle(paket)`)
`Unsafe Referent usage` ile reddeder (FMLEnvironment.production=false iken her çağrıda). Aynı ailedeki `safeCallWhenOn`
istemci sınıfının parametresiz statik metodunu (`IstemciCekirdekPaketleri::donemAlicisi`) başvuru olarak alır, istemci
alıcısını (`Consumer`) döndürür; ortak paylaşılan değişken yoktur. `safeCallWhenOn` 47.4.28'de kaldırılmak üzere
işaretli değil (javap: yalnız `callWhenOn`, `runWhenOn`, `runForDist` deprecated).

**K-C15 Config: tek spec, bölüm kancası.** Spec mod kurucusunun sonunda kurulur; kancalar o ana kadar toplanır.
`config.Ayarlar` kaldırıldı (yerini `AyarKaydi` aldı); `SunucuAyarlari`/`IstemciAyarlari` static final alanları erişimci
metotlara döndü (kullanan yoktu). TOML anahtar yolları değişmedi (`sicaklik.sicaklikSistemiModu`, `gorsel.efektKalitesi`).

**K-C16 GameTest sunucusunda otomatik seçim kapalı.** `GameTestServer` örneğinde yönetici rastgele afet başlatmaz
(sahte oyuncu gelince gerçek bir afet testlerin dünyasını bozardı); testler `secimYap(true)` ile kuru seçim yapar.

**K-C17 GameTest sahte oyuncusu `EmbeddedChannel`'lı.** `GameTestHelper.makeMockServerPlayerInLevel()` Forge 1.20.1'de
kanalsız `Connection` kurar; `PlayerList.placeNewPlayer` → `NetworkHooks.sendMCRegistryPackets` →
`NetworkFilters.injectIfNecessary` → `connection.channel().pipeline()` NPE verir (ilk GameTest koşusunda iki test bu
mesajla düştü; bytecode'la doğrulandı). `sahteOyuncu` bağlantıyı `new EmbeddedChannel(baglanti)`'ya bağlar: kanal aktif
olur, `packet_handler` olmadığı için filtre enjeksiyonu atlanır, paketler gömülü kanalın giden kuyruğunda kalır.
**Diğer işçiler (sıcaklık "10 sahte oyuncu" testi dahil) aynı yardımcıyı kullanmalı.**

**K-C18 `OyunTestiAfeti` ana kaynakta.** GameTest'ler ana kaynak kümesinde (K0-03); yaşam döngüsü sınaması için tam
çalışan, dünyaya dokunmayan bir afet gerekir. `runServer` senaryosu gerçek afet olmadan yaşam döngüsünü ve kalıcılığı
(aktif afetin yeniden başlatmada geri yüklenmesi) gösterebilsin diye `-PoyunTestiAfeti` özelliğiyle de kaydedilebilir.

**K-C19 Erken bildirim ön-uyarının önüne eklenir.** ON_UYARI süresi = `erkenBildirimTick` (600) + pencere (900–1800);
ilk 600 tick'i yalnız Hassas Sismograf görür (`normalOnUyariBasladi()`). Eşyası olmayan için fark yoktur (siren yine
pencerenin sonunda).

**K-C20 Protokol sürümü** — önce "2" (ilk paketler); kimlik verme kuralı değişince "3" (K-C33).

**K-C21 Seviyesiz afetin seviyesi 0.** `AfetBaglami`, olaylar ve geçmiş 0 taşır; dil anahtarı `seviye.dogalafetler.0`
("Seviyesiz").

**K-C22 Çarpan sınırları.** Tek çarpan (0, 100]; aynı türdeki çarpanlar çarpılır, toplam ≤ 1000 (1'den küçük çarpan bastırma
için kullanılabilir; max alınsaydı bastırma çalışmazdı). Üstel sönüm `1 + (tepe−1)·(e^(−5f)−e^(−5))/(1−e^(−5))`: sürenin
sonunda tam 1.

**K-C23 Afet kodu hata verirse sunucu çökmez.** `tick`/`asamaIleri`/`baslat` istisnası ERROR log'a düşer, afet `HATA`
nedeniyle yumuşak durdurulur, olay yayınlanır. Kayıttaki afet okunamazsa ya da türü artık kayıtlı değilse WARN ile atılır.

**K-C24 GameTest dünyası: düz, yapısız, sakin (şef K0-31).** Forge'un GameTest sunucusu dünyayı `server.properties`'ten
kurar ve vanilla GameTest kurallarını almaz; iki spike'ta normal arazi testleri bozdu (`MOTION_BLOCKING` yüzeyi, sahne
üstünde arazi). `build.gradle` → `runGameTestServer` her koşuda `run-gametest/server.properties`'e `level-type=flat`,
`level-name=oyuntesti-duz` (eski `world` klasörüne dokunulmaz), `generate-structures=false` ve yapısız düz
`generator-settings` (bedrock + 2 toprak + çimen, ova, `structure_overrides: []`) yazar; dosyadaki diğer satırlar korunur,
yazılan anahtarlar Gradle çıktısına `run-gametest/server.properties: level-type=flat` biçiminde düşer. `kural.OyunTestiDunyasi`
`ServerStartedEvent`'te, yalnız `GameTestServer` örneğinde `doMobSpawning`, `doWeatherCycle`, `doDaylightCycle`,
`doFireTick` = false ve `randomTickSpeed` = 0 yapar ve okunan değerleri tek satırda loglar (`GameTest dünyası
'oyuntesti-duz' (düz: true) kuralları ayarlandı: ...`). Sistem özelliği yerine `instanceof GameTestServer`: ayrı bir
bayrak unutulursa kurallar normal sunucuya sızabilirdi; sınıf türü kesindir. Reddedilen: dünyayı her koşuda silmek (testler
sahnelerini kendileri kurar; silme her koşuya dünya üretimi ekler), level-type `minecraft:flat` yazımı (şef notundaki
yazım `flat`; Minecraft ikisini de `minecraft:flat`'e çözer).

### İnceleme düzeltmeleri (inceleme-cekirdek P1/P2, şef kararları)

**K-C25 Dönem süreçleri `core.DonemSureci` (B1, madde 11).** Afet yuvası dışında süren süreçler arayüzle kaydolur
(`AfetYoneticisi.donemSureciKaydet`, mod kurucusunda; kimlik benzersiz). `/dogalafet stop` aktif afet yokken süren
bütün süreçleri durdurur ve hangilerini durdurduğunu yazar; `afetTuru`'nun gamerule'ü ya da `dogalAfetlerAcik` kapanınca
aynı yol saniyede bir çalışır (`kurallariDenetle`); `durum` süren süreçleri listeler; `asama ileri` aktif afet yokken
süreçlere sorar ve `DonemAsamaIleriEvent`'in yerini alır (tek kural: dönem süreci arayüzü). Duran süreç için kural yeniden
açılınca davranış K-C41'dedir; ilk sürümdeki "kural açılınca sürmez; yeniden başlamak yeni bir afet seçimiyle olur" gerekçesi
geçersizdi (SUPERNOVA/GUNESSIZ'de güneş patlaması bir daha seçilmez, süreç kalıcı takılırdı; inceleme-2 Y3). Kanıt: GameTest
`donemSureciStopVeGameruleIleDurur`. Reddedilen: inceleyicinin önerdiği `DonemSureciDurdurEvent` — `durum` listesi ve
gamerule bağı için kayıtlı süreçlerin bilinmesi gerekir; olay bunu vermez.

**K-C26 Tamamlanmamış güneş süreci geri alınır, ulaşılmış dönem kalır.** Güneş patlaması afeti `DOGAL` dışı bir nedenle
(komut, kural, hata) biterken ya da `afetTuru = GUNES_PATLAMASI` olan dönem süreci `KOMUT` ile durdurulurken evre `BUYUYEN`
ise çekirdek onu `NORMAL`'e döndürür: 5 günlük büyüme tamamlanmamış süreçtir ve güneş patlaması yeniden seçilebilir kalır
(tek seferlik kapı NORMAL'de açık; tür beklemesi yine kurulur). Sürecin `KURAL` duraklatması döndürmez (kural açılınca
sürer, K-C41); güneş patlaması afeti sürerken süreç yolu döndürmez (Y5). `SUPERNOVA` ve sonrası geri alınmaz, yalnız süreç
durur. Kanıt: GameTest `gunesSureciStopIleNormaleDonerUlasilmisDonemKalir`, senaryo `sun: Swelling sun` → `stop` →
`sun: Normal sun`.

**K-C27 Siren sırasında çıkıp giren oyuncu (B2).** `PlayerLoggedOutEvent` oyuncuyu siren ve paket alıcılarından düşürür;
geri girince 1 Hz denetim (`sirenIstegiGonder`) ona kalan süreyle yeni `SirenIstegiEvent` gönderir. Kanıt: GameTest
`sirenSirasindaCikipGireneKalanSureyleYenidenGider` (aynı profille `PlayerList.remove` + `placeNewPlayer`; ikinci istekte süre
ilkinden kısa).

**K-C28 Gelişim kriteri (şef kararı).** → Şef kararları (Faz 2a).

**K-C29 Oyun günü = `getGameTime()/24000 + ofset` (şef kararı; uyku için K-C42).** → Şef kararları (Faz 2a).

**K-C30 N11 siren kapsamı (şef kararı).** → Şef kararları (Faz 2a).

**K-C31 Seçim turu tek yerde (`core.mantik.SecimTuru`).** Zamanlayıcı + hedef sıralaması + N14 ertelemesi saf sınıfta;
`AfetYoneticisi` ve JUnit simülasyonu aynı kodu çağırır. Oyuncu sayısı testi bu gerçek yolla koşar (1 ve 10 hedef adayı, 10^6
tick, yoğun ayar: 9 034 / 9 012 afet). Dağılım testleri gün, puan ve tek seferlik kapılarıyla koşar: gün 0'da güneş/ay 0;
ay yokken ay 0; kızıl cücede güneş 0; PUAN kriterinde puan 10'da ikisi de 0.

**K-C32 Afet kodu koruması genişledi (K-C23).** `etkiAlani`, `istemciVerisiYaz`, `yuklendi`, `kapanirken`, `baslat`,
`merkezSec`, `yerUygunMu` da korunur; hata → ERROR log + `durdur(temiz=false)` + `BitisNedeni.HATA` (aktif afetse) + olay.
Aşama geçişi sırasında afet hata ile kalkarsa geçişin kalanı (olay, paket) atlanır.
İkinci incelemede (Y1) `aktifKalanTick` deliği kapandı: `AfetYoneticisi.kalanOku(afet)` korumalı okur (hata → `hataVerdi`,
-1); `paketleriGonder`, siren isteği, `durum` ve `start` çıktısı onu kullanır; `AfetAsamaDegistiEvent`'ten sonra afet artık aktif
değilse paket gönderilmez (hayalet afet olmaz). Kanıt: GameTest `aktifKalanTickHatasindaAfetHataylaBiter`,
`etkiAlaniHatasindaAfetHataylaBiter` (sunucu tick'i sürer, `HATA` nedenli tek `AfetBittiEvent`, aktif afet boş, alandaki
oyuncuya son giden afet paketi `YOK`).

**K-C33 Kayıt yalnız mod kurucusunda; paket kimliği ada göre.** Config bölümü, ağ katılımı, afet ve dönem süreci kaydı
açık `kaydet()` metotlarıyla, koşulsuz. Her paketin `<alt sistem>/<paket>` adı vardır; ortak kurulumda tanımlar ada göre
sıralanıp kimlikler o sırayla verilir (iki tarafta katılım sırası farklı olsa da aynı ad kümesi aynı kimlikleri verir) ve
log'a kimlik → ad listesi yazılır. Protokol "3".

**K-C34 Afet durum paketinin merkezi güncel.** `AfetDurumPaketi.merkez` etki alanının o anki merkezidir (hareketli afet);
sunucu geneli afette başlangıç merkezi.

**K-C35 İstemci afet saati.** `IstemciDonem` tahmini oyun duraklatılmışken ilerlemez (`Minecraft.isPaused()`), son paketten
en çok 60 tick ileri gider; `donemTazelemeTick` varsayılanı 20 (1 Hz hizalama), aralığı 20–60 (inceleme-2 Y2: 60'tan seyrek
tazelemede gökyüzü geçişleri donup sıçrardı; Forge aralık dışı eski değeri en yakın sınıra çeker — eski GameTest
dünyasındaki 600 `gametest-3.log`'da 60'a düzeltildi). Reddedilen: ayrı saat paketi (aynı bilgi, ikinci paket türü), saati
hiç geri götürmeyen yumuşatma (1 Hz'de geri adım en çok saniyelik kayma kadar), sınırı pakette taşımak
(`max(60, tazeleme + 20)`: protokol değişir, aynı sonucu config aralığı verir).

**K-C36 `ekVeri` referansı korunur.** `sifirla` alt sistem etiketlerinin içini boşaltır, nesneyi değiştirmez; kayıttan okuma
yeni bir `DunyaDonemi` kurduğu için referans dünya başına geçerlidir (yine de her erişimde `ekVeri(ad)` önerilir).

**K-C37 TPS kısması histerezisli.** Düşüş 18 / 15, dönüş 19 (→ 1) ve 16,5 (1/4 → 1/2); config `tps.toparlanmaEsigi`,
`tps.kritikToparlanmaEsigi`; tutarsız eşikler sıkıştırılır. JUnit: 17,8 ↔ 18,6 salınımında tek kademe değişimi.

**K-C38 Config WARN'ın kök nedeni ve düzeltmesi.** Forge yeni SERVER config dosyasını (her yeni dünyada
`serverconfig/`) boş oluşturur ve düzeltirken **her anahtar için** `Incorrect key … corrected from null to its default`
WARN yazar (Forge'un kendi `forge-server.toml`'u da aynı); bizim için 74 satırdı. `AyarKaydi` config'leri, dosya işleyicisi
(`VarsayilanYazanIsleyici`) dosya yoksa onu spec'in varsayılanları ve açıklamalarıyla (`VarsayilanConfigYazici`, JUnit:
`spec.isCorrect` yazılan dosyada true) önceden yazan bir `ModConfig` alt sınıfıyla kaydeder; Forge doğru dosyayı düzeltmez.
`defaultconfigs/` altında paket yapımcısı dosyası varsa dokunulmaz. Forge'un kendi `forge-server.toml` WARN'ları Forge'un
davranışıdır, kalmaya devam eder. Reddedilen: `defaultconfigs/`'a dosya yazmak (mod güncellenince eski varsayılanlar yeni
dünyalara taşınırdı), log4j filtresi (belirtiyi gizler).

**K-C39 Dönem okuma log'u geri yüklemeden sonra.** "Dünya dönemi okundu (dogalafetler_donem): …" satırı `DunyaDonemi.yukle`
yerine `AfetYoneticisi` aktif afeti geri yükledikten sonra yazılır (`aktifAfet=deprem/aktif`, artık `bekliyor` yok); yeni
dünyada "oluşturuldu".

**K-C40 `logs/` commit'lenmez; yarım senaryo koşusunun bıraktığı `server.properties` elle geri alındı.** Worktree kökünde
Gradle/Forge açılışı `logs/` (`debug.log`, `latest.log`) oluşturur; çalışma çıktısıdır, `.gitignore`'a `logs/` eklendi
(`run*/` ile aynı gerekçe). Kesilen senaryo koşusu `run-server/server.properties`'i `level-name=cekirdek-senaryo` hâlinde
bırakmıştı; betik özgün içeriği bellekte tutup sonda yazdığı için yeniden koşu bu yanlış hâli "özgün" sayardı. Özgün değer
(`level-name=test`, KÖK ve diğer worktree'lerle aynı; diğer satırlar zaten aynı) koşudan önce yazıldı. Reddedilen: betiğe
yedek dosyası eklemek (Faz 2a düzeltme kapsamı dışında; `run-server/` izlenmiyor, tek seferlik kesinti).

### İkinci inceleme düzeltmeleri (inceleme-cekirdek-2 Y1…Y8, şef kararları)

**K-C41 Durdurulan dönem süreci: kural ile duran sürer, stop ile duran sürmez (şef kararı; Y3 seçenek b + test yolu).** → Şef kararları (Faz 2a).

**K-C42 Oyun günü uykuyu sayar (şef kararı; Y7; `sifirla` kısmı K-C46 ile revize).** → Şef kararları (Faz 2a).

**K-C43 Durdurma kaydı çekirdekte ve döngüye bağlı (Y4, K-C41 ile birlikte).** Sahibin kendi `ekVeri("…").durdu` bayrağı
kalktı: kimlik → `DurdurmaNedeni` `DunyaDonemi`'dedir (`surecDurdurmalari`, NBT `surecDurdurmalari`), sahibi
`AfetYoneticisi.surecDurdu(this)` sorar. Kayıt dört yoldan silinir: kural açılınca (yalnız `KURAL`), süreç tamamlanınca
(`aktifMi` false; saniyelik denetimde ve `asama ileri`'den sonra; `aktifMi` hata verirse silinmez), sahibi yeni döngü
başlatınca (`donemSureciBasladi`; yalnız başlangıç koşulunda, K-C46), `sifirla`. `aktifMi` artık "başlamış ve tamamlanmamış" demektir (durmuş süreç de true);
`durdur(ust, neden)` ve `devamEt(ust)` yalnız bildirimdir (vars. boş). Reddedilen: arayüze `dongu()` kimliği (her sahip bir
anahtar daha yazar; `donemSureciBasladi` + otomatik silme aynı işi görür), sahibin bayrağı (iki kaynak; Y4 tuzağı).

**K-C44 Y5, Y6, Y8 ve küçük not.** Y5: süreç yolundaki `BUYUYEN` geri alımı güneş patlaması afeti aktifken yapılmaz (büyüme
o afetindir; afet kendi bitişinde K-C26'ya göre karar verir); elle başlatılan afetin devamı olan süreç bilinen sınır (K-C7).
Y6: aktif afet varken `stop` yalnız afeti durdurur, süren süreç varsa "Süren dönem süreçleri: …; durdurmak için yeniden
/dogalafet stop" satırını ekler (`commands.dogalafetler.stop.surecler_suruyor`); tek `stop` ikisini durdurmaz (afet testi
sırasında dönem kazara durmasın). Y8: test `secimTuruAdaySayisiniSikligaKatmaz` adını aldı, yorumdan "erteleme" iddiası çıktı
(N14 ertelemesinin oyuncu sayısına etkisi ölçülmüyor; test gerileme bekçisidir). `secimYap` Javadoc'u K-C30'a göre düzeldi.

**K-C45 Hata ve paket sınaması, senaryo süreci (test altyapısı).** Y1 GameTest'leri `OyunTestiAfeti` alt sınıfı `HataVerenAfet`
ile koşar (seçilen kanca: `etkiAlani`, `aktifKalanTick`, `istemciVerisiYaz`, `asamaTick`, `temizle`); istemci paketi sahte
oyuncunun `EmbeddedChannel` giden kuyruğundan okunur ve `AfetDurumPaketi.YOK`'un vanilla paketinin baytlarıyla karşılaştırılır
(paket kimliği ilk bayt). Reddedilen: üretim koduna paket gözlem kancası. Senaryonun K-C41 adımları için
`core.OyunTestiSureci` (`oyuntesti_gunes`) yalnız `-PoyunTestiAfeti` güneş türüyle kaydedilir (üretimde yok); §3a sahip
deseninin çalışan örneğidir.

### Üçüncü inceleme düzeltmeleri (inceleme-cekirdek-3 Z1…Z3)

**K-C46 Sözleşme düzeltmeleri (Z1, Z2) ve K-C42 revizyonu (Z3, şef kararı).** Z1: çekirdek dışındaki kod afetin alt sınıf
kancalarına inen okumaları (`etkiAlani()`, `asamaKalanTick()`/`aktifKalanTick()`) yalnız yöneticinin korumalı
`etkiAlani(afet)` ve `kalanOku(afet)` okumalarıyla yapar; kural §1'de, §3a radar paragrafında ve `Afet.etkiAlani()`,
`Afet.asamaKalanTick()` Javadoc'unda (kod değişikliği yalnız yorum). Z2: §3a "Döngü" maddesi — döngü kimliği sürecin kendi
başlangıç olayından, yalnız başlangıç koşulunda karşılaştırılır (güneş zinciri: `gunesEvresi() == SUPERNOVA` iken
`evreBaslangicSaati`; ay sonrası deniz: `deniz().baslangicSaati()`), tek döngülü süreç `donemSureciBasladi` çağırmaz; örnek
`OyunTestiSureci:72` gibi şartlı yazıldı (`GunesZinciriSureci`); saat farkıyla ilerleyen sahip `devamEt`'te son işlenen saati
şimdiye çeker (ya da ilerlemeyi `zamanAdimi()` ile biriktirir). Gerekçe: koşulsuz karşılaştırma, `stop` ile durmuş çok evreli
süreci OP `asama ileri` ile ilerletince ya da evreye bağlı olmayan süreçte güneş evresi değişince `KOMUT` kaydını
kendiliğinden siliyordu (K-C41'e aykırı). Z3 (K-C42 revizyonu, şef kararı) → Şef kararları (Faz 2a).

### Faz 2b entegrasyonunda eklenen çekirdek kararları

**K-C47 Protokol sürümünü entegratör artırır.** Alt sistemler paketlerini `AgKanali.kaydet` ile ekler; paket kimlik kümesi
değişince istemci ve sunucu sürümü uyuşmalıdır (K0-07, K-C33). Birden çok dal paket eklediği için her dalın kendi artırması
birleştirmede çakışırdı (sıcaklık S-19, uyarı, yangın §E). Kural (çekirdek sözleşmesi §9): paket ekleyen dal
`PROTOKOL_SURUMU`'nu artırmaz; entegratör birleştirmede bir kez artırır. Faz 2b'de "3" → "4" (`d62d796`): `sicaklik/durum`,
`su/durum`, `uyari/ortam`, `uyari/ortam_durdur`, `uyari/siren`, `uyari/siren_durdur`, `yangin/ates_durumu` eklendi; log
`9 ağ paketi kaydedildi (protokol 4)` (`.vibe/kanit/entegrasyon-2b/gametest-4.log` ve yedi dedicated senaryo). Faz 3: "5" (K-E17 d).
**Faz 4 notu (ay, entegrasyon 4 2/6):** "5" → "6", Faz 4'te tek sefer: paket kümesi aynı (12 paket), ama ay `DonemPaketi`'nin tel
biçimine `long ayOlaySaati` ekledi (+8 bayt, sonda); eski istemci yeni paketi okuyamaz. Diğer beş Faz 4 dalı paket eklemedi,
artırmaz.

**K-C48 `/dogalafet asama ileri [süreç]`.** Donma K-D31: donma ve buharlaşma süreçlerinin ayrık aşaması yok; argümansız
`asama ileri` ilk dolu çıktıyı veren süreci ilerlettiği için, birden çok süreç sürerken (güneş zinciri, donma, buharlaşma, deniz)
OP istediği süreci seçemiyordu. İsteğe bağlı `surec` argümanı (tab tamamlama: süren ve durdurulmuş süreçlerin kimlikleri)
verilirse aktif afete dokunulmaz, yalnız o süreç sorulur (`AfetYoneticisi.donemAsamaIleri(Optional<String>)`); süreç yoksa ya
da ilerleyemiyorsa `commands.dogalafetler.asama.surec_yok`. Argümansız davranış değişmedi (K-C41). Kanıt: GameTest
`donemSureciStopVeGameruleIleDurur` (`asama ileri oyuntesti_gunes_patlamasi` yalnız o süreci ilerletir, ay süreci ilerlemez;
`gametest-4.log`, `-5.log`).

**K-C49 Deniz yükselmesi sürecinde §3a istisnası (sanal su SU-32).** Durdurulmuş (`KURAL` ya da `KOMUT`) deniz yükselmesi
sürecinde dönemin `deniz()` alanı buharlaşmayla değişmeye devam eder; §3a'nın "durdurmada ulaşılmış alanlar korunur" kuralına
bilinçli istisnadır. Gerekçe: gamerule afetin seçilmesini kapatır, dünyanın fiziğini değil; N10 sırası (buharlaşma önce sanal
yükselmeyi tüketir) durdurmayla bozulmamalı. Çekirdek sözleşmesi §3a'ya işlendi.

**K-C50 Güneş evresi ilerlemesini evrenin sahibi yazar (sıcaklık §A.8).** Sıcaklık `evreIlerlemesi()`'ni okur, yazmaz: evrenin
sahibi (güneş patlaması afeti ya da gökyüzü dönem süreci) `BUYUYEN` boyunca 0→1'i 5 MC gününde (ölçekli afet saatiyle) doğrusal,
`SUPERNOVA` boyunca 0→1'i evre süresinde yazar; yazılmazsa güneş patlaması sıcaklığı 0 kalır ve süpernova artığı sönmez.
**Faz 4 notu (güneş GP1, entegrasyon 4):** `BUYUYEN` eğrisi güneş patlamasında iki parçalıdır: sirende 0 → 0,273 karesel
(`0,273·τ²`, gökyüzü ölçeği 1 → 1,6), AKTIF'in 5 MC gününde 0,273 → 1 doğrusal (sıcaklık artışı siren sonunda %27, 1. gün
%42, 5. gün %100). Çekirdek §6 ve sıcaklık §A.8 sözleşme metinleri buna göre değişti; okuyucu kod (sıcaklık, gökyüzü) değişmedi
(çarpan `evreIlerlemesi` olarak kaldı; inceleme-gunes-patlamasi-kizil-cuce doğruladı).
Çekirdek sözleşmesi §6 tablosunun evre ilerlemesi satırına işlendi.

### Reddedilen yollar

- **`DonemSureciDurdurEvent` (inceleme önerisi):** `durum` listesi ve gamerule bağı kayıtlı süreçleri ister (K-C25).
- **Monoton "ulaşılan gün":** oyun zamanı zaten monoton (K-C29).
- **Config WARN'ı için `defaultconfigs/` dosyası ya da log filtresi:** K-C38.
- **Ayrı saat paketi:** dönem paketi 1 Hz aynı işi görür (K-C35).
- **Duran sürecin kural açılınca hiç sürmemesi / `stop` ile durana da sürmesi:** K-C41 (şef kararı).
- **Uyku sayılmasın (yalnız `getGameTime()`):** her gece uyunan sunucuda gün ~%48 yavaş ilerlerdi (K-C42).
- **`DonemSureci.dongu()` ve sahibin durdurma bayrağı:** K-C43.
- **Tek `stop`'un afeti ve süreçleri birlikte durdurması:** afet testi sırasında dönem kazara dururdu (K-C44).
- **Paket gözlemi için üretim kancası:** K-C45.
- **`sifirla`'nın uyku birikimini silmesi:** oyun günü görünen günün gerisine düşerdi (K-C46, şef kararı).
- **Döngü kimliğinin koşulsuz karşılaştırılması:** `KOMUT` kaydı kendiliğinden silinirdi (K-C46).
- **Yaşam döngüsü arayüzü + aşama durumu yöneticide:** kayıt iki parçaya bölünürdü, her afet takip/kayıt kodunu tekrar
  ederdi (K-C1).
- **Tek global oran, kapalı türlerde yeniden normalleştirme:** yedi tür kapatılınca kalan tek tür sekiz kat sıklaşırdı.
- **Oyuncu başına zar:** madde 3'e aykırı (10 oyuncuda 10 kat afet).
- **Sayaç/takvim ("her X günde") ya da sabit aralıklı kuyruk:** madde 4'e aykırı.
- **Art arda kuralını yalnız aynı seviyeye uygulamak (4→5 serbest):** madde 10'un ilk yarısı ("4-5 ardışık gelmesin") karşılanmazdı.
- **Son merkeze yakın oyuncuyu tamamen yasaklamak:** birlikte oynayan grupta afet hiç çıkmazdı; tercih sırası yeterli.
- **Gamerule kapanınca tüm süren afetleri kesmek / hiçbirini kesmemek:** madde 11'in iki cümlesinden birini bozardı (K-C7).
- **Sağ çıkan takibini SIREN'den başlatmak:** sirenden kaçan oyuncu "afetten sağ çıktı" sayılırdı.
- **Hızı SavedData'da saklamak:** unutulan test hızı dünyayı kalıcı hızlandırırdı.
- **Her dönem alanı için ayrı SavedData (spike-gokyuzu `GokyuzuVerisi`):** GOREV §4 tek SavedData; alt sistemlere özel
  alanlar için `ekVeri`.
- **Dönem paketi her setter'da anında:** donma G'yi her tick güncellerse tick başına paket olurdu; toplama + acil alanlar.
- **İstemci teslimi için `safeRunWhenOn` + statik teslim kutusu:** gereksiz paylaşılan değişken; `unsafeRunWhenOn`: görev
  "safe" yolu istedi; spike-siren'in alıcı arayüzü yolu: görev DistExecutor istedi (K-C14).
- **`makeMockServerPlayerInLevel`:** Forge 1.20.1'de NPE (K-C17).
- **Oyuncu listesine girmeyen sahte oyuncu (`level.addNewPlayer`):** çevrim içi sayılmaz; hedef seçimi ve sağ çıkan
  hesabı sınanamazdı.

## Faz 2b — Sıcaklık

Parça `sicaklik` (dal `is/sicaklik`, son commit `8e44b16`; `main`'e birleştirme `a844da5`). Kaynak `.vibe/kararlar/sicaklik.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır; metindeki §A/§1… göndermeleri o dosyaya aittir.
Şef kararları (S-03, S-18, S-19) "Şef kararları (Faz 2b)" bölümündedir; N17 satırları sondaki tablodadır. Rapor
`.vibe/rapor/sicaklik.md`.

**S-01 Ölçek: vücut −100…+100 birim, ortam °C.** Madde 14 etkileri "ölçeğin yüzdesi" olarak tanımlıyor; simetrik yüz birimlik
yan bunu doğrudan verir. Ortamı °C tutmak `bilgi` dökümünü ve config'i okunur kılar (çöl öğleni 52 °C, mağara 12 °C).
Çeviri `E = (T − konfor) · birimPerDerece` (18 °C, 2 birim/°C).

**S-02 Isı alışverişi modeli (N4).** Newton soğuması + sabit metabolik üretim + iletkenliği bölen yalıtım:
`B' = k·w/(1+I)·[(E−B) + U·(a·(1+I)−1)]`. Bu biçim fizikteki `dT/dt = (M − C(T−T_env))/cap`, `C = C₀/(1+I)` denkleminin
vücut birimine çevrilmişidir; çıplak ve yürürken konforda nötr dengeyi, yalıtımla sıcakta aşırı ısınmayı ("kalın giysi
soukta kurtarır, sıcakta pişirir") ve fark büyükken hızlı değişimi tek formülden verir. Reddedilen: sabit hızlı yaklaşma
("fark büyükse hızlı" sağlanmaz), yalıtımı yalnız hız bölen olarak almak (sıcakta dezavantaj çıkmaz).

**S-03 Yün (N3) yalnız nötrün altında soğuma yönünde ayrı çarpan (tur 2 rev., şef kararı; T1).** → Şef kararları (Faz 2b).

**S-04 Aşırı soğuk istisnası.** Ortam ≤ `isi.asiriSogukEsigi` (−25 °C) iken yalıtımın metabolik ısıyı tutma terimi kalkar ve yün
kesintisi `asiriSogukYunVerimi` (0,75) ile çarpılır: yalıtım yalnız yavaşlatır. 4 yün + 120 tüy ile −45 °C'de donma hasarı
eşiğine ~60 dk'da (çıplakta ~2,5 dk) inilir (JUnit: &gt; 10 kat, ama sınırlı). Süpernova sonrası G=100 ova −47 °C, kızıl cücede
kış bölgesi −35 °C, 200+ zirve −25'in altı — eşik bunları kapsar, normal karlı ova gecesini (−10 °C) kapsamaz.

**S-05 Tüy.** Upgrade başına 0,04 yalıtım; 4 parça × 8 tüy normal soğukta (−10 °C) nötr tutar; 4 × 30 + yün ılık ortamda
(22 °C) ~7 dk'da yanmaya götürür (JUnit). Sağ tık hem `RightClickItem` hem `RightClickBlock`'ta, iki tarafta iptal edilir (sunucu
değiştirir; istemci yalnız kol sallar). Ana el tüy değilse ya da yardımcı elde `ArmorItem` yoksa olaya dokunulmaz. Yaratıcı
modda tüy harcanmaz (vanilla tüketim kuralı). Sınırda tüy harcanmaz, düşük perdeli bir ses çalar.

**S-06 Durum `PersistentData`'da, capability değil.** Veri yalnız oyuncuda, küçük (vücut + ≤ 16 süreli etki + sayac), ve oyuncu
dosyasıyla kendiliğinden kaydedilir (çıkış/giriş kalıcılığı). Capability kaydı/ekleme/klonlama/senkron kodu gerekmez. Ölümde
`PlayerRespawnEvent` sıfırlar; ölümsüz kopyada (End dönüşü) `Clone` etiketi taşır (GameTest ikisini de sınar). Paket
kısıtlaması için son gönderilen değerler yalnız bellekte (`GECICI`, çıkışta silinir).

**S-07 Güncelleme 1 Hz, sunucu tick sayacına göre yayılır.** Oyuncu `(tickCount_sunucu + entityId) mod 20` ile seçilir: 10 oyuncu
farklı tick'lere dağılır ve tick almayan sahte oyuncular da gerçek yoldan güncellenir (GameTest kural testi). Oyuncu
başına iş: 11³ blok taraması (~1 300 `getBlockState`, oyuncunun yüklü çevresi) + sabit hesap.

**S-08 Hasar: kendi hasar tiplerimiz, `setSecondsOnFire` yok.** Ateşe vermek vanilla ateş hasarını ikinci kez ekler ve suya girince
söner (ısı çarpması sönmez); vanilla donma (`setTicksFrozen`) tam donunca kendi hasarını ekler. Bunun yerine
`dogalafetler:donma`/`asiri_isinma` hasarı, aralık eşikte 6 sn'den uçta 2 sn'ye kısalır (yarım kalp); alev/kar tanesi
parçacıkları görsel işaret verir. Aşırı ısınma `is_fire` etiketlidir: Ateşe Direnç iksiri korur (Cold Sweat ile aynı oyuncu
beklentisi). Zırh hasarı azaltmaz (`bypasses_armor`), geri tepme yok.

**S-09 Yavaşlama hareket hızı değiştiricisi.** `MOVEMENT_SPEED` üzerinde sabit UUID'li geçici `MULTIPLY_TOTAL` değiştirici; soğuk
eşiğinden (−30) ölçek ucuna %0→%50. Slowness efekti kullanılmadı (ikon/parçacık görünür, süt ile silinir, süre yönetimi ister).

**S-10 HUD XP barı genişliğinde, kalpler kayar.** Bar 182×5 px, XP seviye sayısının hemen üstünde; kalpler ve açlık barı
Forge `leftHeight/rightHeight` ile 14 px yukarı kayar (başka modların barları da aynı mekanizmayla dizilir). Overlay
`PLAYER_HEALTH`'in altına kaydedilir ki yükseklik kalpler çizilmeden önce artmış olsun. Reddedilen: kalplerle açlık arasındaki
20 px boşluğa sığan bar (okunmaz), XP barının üstüne binen bar (seviye sayısını örter).

**S-11 Ekran efektleri overlay + kamera açısı.** Buz kenarı ve sıcak hava vinyeti `FROSTBITE` overlay'inin üstüne kayıtlı tam
ekran dokular; sıcak dokusu dikeyde kesintisizdir ve yukarı kayar (dalgalanma). "Titreme" `ViewportEvent.ComputeCameraAngles`
ile roll/pitch yalpasıdır (sıcakta yavaş, donarken hızlı küçük). Post-shader reddedildi (shader paketleriyle çakışır,
Fabulous'ta riskli). CLIENT config ile kapatılabilir.

**S-12 Kapı kapalıyken bir kez KAPALI paketi.** "Kapalıyken paket yok" şartı ile HUD'un kaybolması arasındaki tek çelişki
görünen HUD'u gizlemektir; geçişte oyuncu başına tek paket gider, sonra hiçbir şey.

**S-13 Cold Sweat köprüsü (N8) uygulandı.** Cold Sweat 2.4.5 API'si (`TempModifierRegisterEvent`, `DefaultTempModifiersEvent`,
`TempModifier`, `Temperature.convert`) javap ile doğrulandı. Cold Sweat yüklüyken `uyum.ColdSweatKoprusu` dünya döneminin
°C katkısını (güneş evresi + donma + kızıl cüce + kış bölgesi; bizim ortam formülümüzden) Cold Sweat'in `WORLD` sıcaklığına
`dogalafetler:donem` değiştiricisiyle ekler (°C → Cold Sweat MC birimi `Temperature.convert`). Bizim sistem açıkken değiştirici
etkisizdir. Bağımlılık `compileOnly fg.deobf(cold-sweat)` (önbellekte, `--offline` derlenir), `mods.toml`'da
`mandatory=false` `[2.4,)`; sınıf yalnız `ModList.isLoaded("cold_sweat")` ise yüklenir.

**S-14 Ortam katsayıları tek kaynakta.** Config anahtarları saf kayıtların bileşen adlarıdır, varsayılanlar `VARSAYILAN`'dan okunur,
kayıtlar config yüklenince yansımayla kurulur (`SicaklikAyarlari`); geçersiz kombinasyon (sırasız eşikler) ERROR log + varsayılan.
Isı/soğuk kaynakları `sicaklik.kaynaklar` listesinde `blok=güç:yarıçap` (başka modların blokları eklenebilir; LIT özelliği
olanlar yalnız yanarken).

**S-15 Ortam kalemleri.** Kurak biyom = yağışsız ve biyom sıcaklığı &gt; 1 (çöl, savan, kanyon); güneş altı yalnız gökyüzü
görülürken ve yağış yokken; yeraltı gökyüzü görmeyen noktada yüzey derinliğiyle (24 blokta tam) 12 °C'ye; sığ kapalı alan
%30 oranında 16 °C'ye; dünya dönemi etkileri tam derinlikte %80 söner (güneş patlamasında derine inen korunur, bölüm D);
güneşsiz dönemde gün eğrisi sürekli gece, kızıl cücede güneş ve gün-gece %60. Dünya dönemi yalnız normal boyutları etkiler.

**S-16 Görsel kanıt otomasyonu.** `client.sicaklik.SicaklikOtomasyonu` yalnız `-Ddogalafetler.otomasyon=sicaklik*` sistem
özelliğiyle (koşu betiği `JAVA_TOOL_OPTIONS` ile verir) çalışır; `build.gradle` istemci koşusuna `-PkullaniciAdi` ve
`-PquickPlaySunucu` (çok oyunculu Quick Play) satırları eklendi. Betiği `tools/senaryo/sicaklik_gorsel.py` (K0-21 rev. 2, ws 92).

**S-17 Uyumluluk koşusunda mixin refmap yeniden eşlemesi (build.gradle).** `-Puyum=coldsweat` ile ilk Quick Play koşusu dünyayı
açamadı: `RegistryDataLoader` → `cold_sweat:item/insulator/ad_astra/...` "Could not find item: ad_astra:jet_suit" → "Failed to load
registries". Kök neden: Cold Sweat'in `required_mods` süzgeci mixin'le çalışır, mixin refmap'i SRG adlıdır; ForgeGradle geliştirme
ortamında resmi (official) adlara çevrilmeyince hedef kaçar. MDK'nın önerdiği `mixin.env.remapRefMap=true` +
`mixin.env.refMapRemappingFile=build/createSrgToMcp/output.srg` yalnız `uyum != yok` iken bütün koşulara eklendi; sonraki koşuda
dünya açıldı. Üretim jar'ını etkilemez (yalnız `runs`).

**S-18 GOREV §6 #9 çift hasar ölçümü — Faz 5 reçetesi (şef kararı; T4, P2 kabul).** → Şef kararları (Faz 2b).

**S-19 Protokol sürümü dalda artırılmaz (şef kararı; T5).** → Şef kararları (Faz 2b).

**S-20 Görsel kanıt koşusunun geçerliliği (tur 2; T2, T3).** `tools/senaryo/sicaklik_gorsel.py` her koşuya numara verir
(`runClient-<etiket>-<n>.log`, `runServer-<n>.log`, `latest-<etiket>-<n>.log`; eski log üzerine yazılmaz), istemci bitince
`run/logs/latest.log`'daki `SICAKLIK_OTOMASYON kapanıyor; girdi sayıları` satırını kaynağı ve satır numarasıyla `protokol.log`'a
yazar; satır yoksa ya da sayılardan biri 0 değilse koşu GEÇERSİZ (çıkış 1). `run/options.txt`'e `tutorialStep:none` yazılır
(vanilla öğretici kutusu kareleri kirletmesin). Titreşim kanıtı: HUD son çizdiği barın ofsetini (`SicaklikHud.sonDx/sonDy`), yanık
satırını ve çizim tick'ini tutar; otomasyon her kare satırına yazar ve ikinci kareyi (`_b`) HUD yeni bir tick'te **farklı bir ofsetle**
çizilene kadar bekletir (en çok 160 tick; bulunamazsa WARN). Sabit tick farkı reddedildi: gizli workspace'te istemci ~1 FPS çizer, 30
tick (= `t/2`'de 15, 3'ün katı) aynı faza, 2–3 tick aynı çizime düşüyordu.

### Reddedilen yollar

- **Capability ile durum:** kayıt, ekleme, klonlama ve senkron kodu; `PersistentData` aynı kalıcılığı verir (S-06).
- **`setSecondsOnFire` / vanilla `setTicksFrozen`:** çift hasar ve suyla sönen ısı çarpması (S-08).
- **Slowness efekti:** süt siler, ikon ve süre yönetimi (S-09).
- **Post-processing shader ile ısı dalgası:** shader paketleri ve Fabulous ile çakışma riski (S-11).
- **Yünü yalnız yalıtıma katmak / yalnız oran olarak uygulamak:** biri N3 oranını, öteki N4 "sıcakta dezavantaj"ı bozar (S-03).
- **Kapalı sistemde hiç paket göndermemek:** açık iken görünen HUD kapandığında ekranda donar kalırdı (S-12).
- **Cold Sweat'in `GatherDefaultTempModifiersEvent`'i:** 2.4.5'te kaldırılmak üzere işaretli (derleme uyarısı); `DefaultTempModifiersEvent` kullanıldı.
- **Ayrı vanilla tarif sağlayıcısı (`RecipeProvider`):** özel tarif tipinin JSON'u tek satırlık; `SicaklikVerileri` hasar tipleri ve etiketlerle birlikte yazar.

## Faz 2b — Uyarı ve ses

Parça `uyari-ses` (dal `is/uyari-ses`, son commit `49d0bfc`; `main`'e birleştirme `7ca6eb6`). Kaynak `.vibe/kararlar/uyari-ses.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır; metindeki §A/§1… göndermeleri o dosyaya aittir.
Şef kararları (U-02, U-15 (K0-35), U-17) "Şef kararları (Faz 2b)" bölümündedir; N17 satırları sondaki tablodadır. Rapor
`.vibe/rapor/uyari-ses.md`.

**U-01 Sürenin yetkilisi sunucu (S-06 B3).** Sireni durdurma paketi bitirir (madde 5 "afet asıl etkisine geçtiği anda").
İstemci zarfı kalan süre dolunca kendiliğinden sönmez; kalan + 60 tick'te (`GUVENLIK_PAYI_TICK`) durdurma gelmediyse
türün varsayılan fade-out'u başlar (paket kaybı, sunucu donması). Log nedeni ayırır: "durdurma paketi" / "güvenlik tavanı".

**U-02 Paket afet türünü ve `afetNo`'yu taşır (şef kabulü).** → Şef kararları (Faz 2b).

**U-03 Geç gelen oyuncu (madde 3, S-06 B2).** Paket toplam ve kalan süreyi taşır; zarf `toplam − kalan` yerinden başlar:
kalan süre kadar çalar, açılış kısalmaz (kısa belirme), güneş sireninin +3 dB yükselişi kaldığı yerden sürer. Toplam
= `zamanOlcegi().gercekTick(SirenTuru.sureTick(tur))` (hız dahil), kalan = olayın `sureTick`'i.

**U-04 Fade süreleri SERVER config'te, paketle gider.** Fade-out aynı sunucudaki herkeste aynı olmalı (oynanış anı olan
meteor/çarpma başlangıcıyla eş); istemci profilindeki 40/80 tick yalnız güvenlik tavanında kullanılır.

**U-05 Config yolu `uyari.sirenSes`.** Spike `ses.sirenSes` kullanıyordu; çekirdeğin bölüm kuralı (bölüm adı = alt sistem,
K-C15) gereği `uyari`. Kaydırıcı `ConfigValue.set` ile bellekte değişir (çalan siren her tick okur, anında etkili),
ekran kapanırken (`ScreenEvent.Closing`) bir kez `save()`.

**U-06 Alt yazı oku (S-06 B4).** `relative=true` korunur (kaynak dinleyicide, sağ-sol zıplaması yok); yalnız çalma anında
`getX/Y/Z` dinleyicinin dünya konumudur ve ilk tick'te (0,0,0)'a döner. Vanilla `SubtitleOverlay` konumu çalma anında okur:
konum ≈ kamera olduğu için yön vektörü sıfır, ok çizilmez. Bu tick'te düzey 0'dır (zarf sessiz başlar; devam örneği bir
tick sessiz), yanlış konumdan ses duyulmaz.

**U-07 Ortam sönümü istemcide.** `Attenuation.NONE` + kendi doğrusal sönümü. Vanilla `LINEAR` menzili 16 × hacim'dir;
büyük menzil için hacim > 1 gerekir ve `ortamTavan`/bastırma aynı hacme bindiğinde menzil de değişirdi. Kendi sönümümüzde
menzil ve düzey ayrıdır; kaynak noktada durduğu için yön yine duyulur.

**U-08 Eşzamanlı katman tavanı efekt kalitesine bağlı** (ses.md §5: Düşük 8, Orta 12, Yüksek 16, Ultra 20; aynı dosyadan 6).
Seçim her tick, düzeye göre azalan, eşitlikte kimliğe göre (belirlenimci); seçilmeyen katmanın ses örneği durur, zaman
çizelgesi sürer.

**U-09 Siren sırasında ortam −3 dB** (ses.md §4 kural 4), 20 tick'te desibelde doğrusal geçiş (`Bastirma`), sıçrama yok.

**U-10 Katmanlar afete bağlı.** `afetNo`'lu katmanlar `AfetBittiEvent`'te 40 tick'te söner (afet işçisi `temizle`de
unutsa da ses asılı kalmaz, Faz 4 kabulü "geride ses kalmaz"). Sunucu kapanınca kayıt silinir; kalıcı değildir (afet
kaydından dönüşte sahibi `yuklendi`de yeniden `baslat` eder).

**U-11 Ses motorunun kestiği katman da sürer (K0-27 ile aynı yol).** Boyut değişimi `SoundManager.stop()` sireni ve ortam
katmanlarını keser; `SirenNobeti` (20 tick deneme aralığı) ikisinde de aynı zarf/zaman çizelgesiyle yeni örnek başlatır.
`/stopsound` sireni susturamaz (K0-27, kasıtlı).

**U-12 Oyuncusuz dedicated server'da siren paketi yok.** Çekirdek alıcı yoksa `SirenIstegiEvent` yayınlamaz. `runServer`
senaryosu SIREN aşamasını `[uyari] SIREN aşaması: … 0 oyuncuya` log'uyla ve `invalid dist` 0 ile gösterir; paketin
gittiği GameTest'te (sahte oyuncunun gömülü kanalı) ve istemci koşusunda (entegre sunucu log'u) kanıtlanır.

**U-13 Otomasyon komutları sohbete yazmaz.** `UyariOtomasyonu` komutları entegre sunucuda `withSuppressedOutput()` kaynakla
verir; ekran görüntüsünde OP komut çıktısı (N6'da serbest) da kalmaz, kare yalnız oyunu gösterir.

**U-14 `build.gradle` istemci koşusu.** `client` koşusuna `-PoyunTestiAfeti` (entegre sunucuda test afeti) ve sanal-su ile
birebir aynı `-Potomasyon` bloğu (1280×720) eklendi; birleştirmede tek blok kalır.

**U-15 İstemci kanıtının sesi dosyaya gider, hoparlöre gitmez (şef kabulü; entegratör K0-35 olarak işler).** → K0-35 (a), Şef kararları (Faz 2b).

**U-16 Sönen siren yeni isteği engellemez.** `afetNo` = afetin başlangıç saati; `stop` ve `start` aynı tick'te gelirse yeni
afetin numarası eskisininkiyle aynı olabilir (ölçekli saat o tick'te ilerlememiş olabilir). Durdurma almış siren aynı
afet için bir daha istenmez (çekirdek durdurmada alıcı listesini siler); bu yüzden sönen sirende gelen istek her zaman
yeni siren başlatır, eskisi kendi fade'iyle söner (`SirenKarari.istek(caliyor, sonuyor, …)`).

**U-17 Otomasyon dosyaya güvenmez; koşu `options.txt`'i geri yazar (inceleme V1 ve U2-1…U2-4, şef kuralı).** → K0-35 (b), Şef kararları (Faz 2b).

**U-18 Ana Ses 0 iken yeniden çalma denenmez (inceleme V4).** Vanilla Ana Ses 0 iken sesi başlatmaz, `isActive` false
kalır; nöbet bunu "motor kesti" sanmaz (`SirenNobeti.yenidenCalinmali(…, anaSesKapali)`), siren ve ortam katmanı için
saniyede bir INFO satırı üretilmez. Ana Ses açılınca ilk tick'te aynı zarfla çalar.

**U-19 Yeniden doğan oyuncu katmanları alır (inceleme V3).** Forge 1.20.1'de ölüm ve End çıkışı `PlayerList.respawn` yolundan
geçer ve yalnız `PlayerRespawnEvent` tetikler; `OrtamSesi` buna da abonedir. Siren için ek yol gerekmez: çekirdek
alıcıyı UUID ile tutar, istemcide boyut değişimi `SoundManager.stop()`'u U-11 yoluyla karşılanır. GameTest
`baskaBoyuttaYenidenDoganOyuncuKatmaniAlir`.

### Reddedilen yollar

- **Gerçek `SoundSource` kategorisi (enum mixin'i):** S-01'de denenip reddedildi (varsayılan %100, ağ ordinal'i, mixin);
  tekrar denenmedi.
- **İstemci süresiyle kendiliğinden fade-out (spike modeli):** sunucu durdurmasıyla iki kaynak yarışır; yüklü sunucuda
  siren afet başlamadan sönerdi. Yerine U-01.
- **`relative=false` + konumu her tick kameraya eşitleme (S-06 B4 önerisi):** kaynak 20 Hz, dinleyici kare hızında
  güncellenir; koşan oyuncuda mono ses sağa-sola zıplar. Yerine U-06.
- **Siren paketinde yalnız siren türü (S-02):** U-02.
- **Ortam için vanilla `LINEAR` sönümü:** U-07.
- **Ortam katmanını afet işçisinin kendi paketiyle göndermesi:** her afet aynı geç gelen/boyut değişimi/bastırma/tavan
  mantığını yeniden yazardı; tavan (ses.md §4 kural 3) ancak tek karışımda uygulanabilir.
- **Ana Ses %0 ile istemci kanıtı:** vanilla sesi hiç başlatmaz; yerine U-15.
- **Ölçüm için sistem ses sunucusundan (PipeWire monitörü) kayıt:** sahibin hoparlörüne ses gider ve sahibin çalan
  sesleri kayda karışır; U-15 sürecin kendi karışımını alır.
- **Paketten afet türünü çıkarmak (inceleme V6 seçeneği):** görev metni istiyor, log/teşhis kullanıyor; U-02.
- **Seçenek ekranını kapatırken `options.txt`'i sonradan dosya düzeyinde düzeltmek:** vanilla biçimine bağlı metin
  düzenlemesi; yerine U-17 (b), Ana Ses kaydetme anında bellekte 0.

## Faz 2b — Gökyüzü

Parça `gokyuzu` (dal `is/gokyuzu`, son commit `09ee1d3`; `main`'e birleştirme `456f465`). Kaynak `.vibe/kararlar/gokyuzu.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır; metindeki §A/§1… göndermeleri o dosyaya aittir.
Şef kararları (G19, G21) "Şef kararları (Faz 2b)" bölümündedir; N17 satırları sondaki tablodadır. Rapor
`.vibe/rapor/gokyuzu.md`.

**G7 Süreler.** Süpernova 1,5 gün (bölüm D "1-2 gün"ün ortası; config 1–2). GÜNEŞSİZ ilerlemesi kalıntı genişlemesidir
(tasarım: günde %1, tavan %130 → 30 gün); evreyi yalnız kızıl cüce kapısı bitirir. Kızıl cüce doğuşu 6000 tick: şafaktan
öğle öncesine kadar disk yükselir, ufuk ışığı söner.

**G8 Ay veri modeli (çekirdeğe öneri).** Spike G6'daki `AyDurumu {VAR, YAKLASIYOR, YOK}` + ay olay saati çekirdekte yok
(`DunyaDonemi.ayVar()` bool); `core/**` bu parçaya kapalı. Bu parça ayın var/yok iki durumunu çizer. Faz 4 ay çarpması için
öneri: `DunyaDonemi`'ne ay olay saati (yaklaşma başlangıcı, çarpma anı) ve `DonemPaketi`'ne o alan; yaklaşan ay 60 sn sürdüğü
ölçekli saatten hesaplanır (G9 ile aynı yol).

**G9 Geçişler ölçekli saatten, monoton.** İstemci evre başlangıcından beri geçen ölçekli saati `IstemciDonem.saat(pt)`'den
hesaplar: 20 sn renk geçişi (`GECIS_TICK = 400`), süpernova koreografisi (0–8 sn şişmiş güneş 3,2× → nokta, turuncu →
mavi-beyaz; 8 sn'de parlama, 0,4 sn tepe + 6 sn üstel sönüm; kabuk ~10 sn'de açılır, sonra ilerlemeyle 8 → 63 birim) ve
1–2 günlük sönüm ilerlemeyle (`e^(-p/0,35)`). Sonradan giren oyuncu aynı saatten aynı kareyi görür. Paketin saniyelik
ilerlemesi `MonotonIlerleme` ile 1 sn zaman sabitiyle yumuşatılır ve aynı evrede geri gitmez (K-C35 geri adımı). Eğriler
JUnit'te monoton.

**G10 Önceki evre pakette değil.** Zincir sırası sabit (NORMAL → BÜYÜYEN → SUPERNOVA → GÜNEŞSİZ → KIZIL_CUCE); istemci
`oncekiEvre` ile bilir. `sifirla`'daki NORMAL'e dönüş geçişsizdir (anında vanilla).

**G11 Renk ve boy ofsetleri sunucu otoriteli.** `kizilCuceBoyutOrani` (0,6) ve `kizilCuceRenkGucu` (1,0) SERVER config'te:
Forge sunucu config'ini istemciye eşitler, herkes aynı göğü görür (madde 3). Yalnız makineye özgü yük ayarları CLIENT'ta
(`yildizYogunlugu`, `nebulaKatmani`). Dünyaya girmeden config okunamazsa varsayılan görünüm.

**G12 Kızıl cüce şafağa hizalı.** Kapı açıldıktan sonra gün saati 23000–24000 penceresindeyse ya da önceki tick ile bu tick
arasında 23000 geçildiyse (uyku 13000 → 24000 sıçraması, `time add`) doğar; `/time set` geri almak şafak sayılmaz;
`doDaylightCycle=false` iken şafak hiç gelmediği için hemen doğar. Gün saatine dokunulmaz (madde 16). Doğuş ufukta yalnız
kızıl ışıkla başlar, disk ilerlemenin ilk %20'sinde belirir.

**G13 Vitrin ayarları (Faz 2).** Süpernova: gök aydınlatması 0,5 → 0,38, ışın boyu `30 + 70·flaş + 25·parlaklık`, alfa
`0,35 + 0,6·parlaklık` (koşu 2'de ışınlar göğe gömülüyordu; koşu 3/4'te gök ortancası (70,77,94), ışınların parlak
kutusu 1100 × 570 px). Büyüyen güneş diskinin toplamalı payı %30 → %12, hale daha turuncu (son evrede disk sarı-beyaza
doyuyordu; şimdi merkez (255,190,99)). Düşük kalitede süpernova hale/ışın kareleri 0,6× (spike G6 doldurma hızı).

**G14 Işık haritası tonu görünüm başına bir kez.** `adjustLightmapColors` 256 piksel için çağrılır; `gorunum` aynı ms + aynı
kısmi tick + aynı paket sürümünde önbellekten döner, ton `Gorunum.isik`'te hazırdır. Işık seviyeleri değişmez (G5).

**G15 Çekirdek taklidiyle çakışma (geliştirme sınırı).** `-PoyunTestiAfeti=...,gunes_patlamasi` ile çekirdeğin `oyuntesti_gunes` taklidi de
kayıtlıyken iki süreç aynı evreyi sürer (çift ilerleme; taklit GÜNEŞSİZ → KIZIL_CUCE'yi donma kapısı olmadan yapar) ve
`tools/senaryo/cekirdek_sunucu.py` ("Era processes still running: oyuntesti_gunes; run ...") kırılır. Karar: taklit
kayıtlıysa gerçek zincir dönem süreci olarak kaydedilmez (WARN), komutlar çalışır. Bu dalda çekirdek senaryosu yeniden
koşuldu: sonuç 0. Yalnız `-PoyunTestiAfeti=...,gunes_patlamasi` (geliştirme sunucusu, senaryo) ile olur; üretim jar'ında
ve normal geliştirme koşusunda taklit kayıtlı değildir, zincir her zaman kayıtlıdır. Kod değişmez. Öneri: Faz 4'te gerçek
güneş patlaması gelince çekirdek taklidi emekliye ayırabilir.

**G16 Ezilme denemesi kodda.** `SahteGokyuzuModu` (spike'tan) yalnız geliştirme ortamında ve otomasyon bayrağı `ezilme` /
`ezilme-yansimasiz` iken etkin; Faz 5 regresyonunda aynı komutla koşulur.

**G17 Görsel otomasyon.** `GokyuzuOtomasyonu` yalnız geliştirme ortamında ve `run/dogalafetler-gokyuzu-otomasyon` dosyası
varken (içerik `tam` | `ezilme` | `ezilme-yansimasiz`). Gözlemci kipi, Y=150 (ağaçlar ufku kapatmasın; ilk koşu yerdeydi
ve kareler toprak bloğuna bakıyordu), sabit bakış. Girdi sayacı açılıştan kapanışa (K0-21 rev. 2 (e)); otomasyonun
açmadığı ekran kapatılır (yükleme ekranı hariç). Süpernova çekimleri tick sayımına değil evre saniyesine bağlı: gizli
workspace'te FPS 1'e düşünce de aynı an çekilir.

**G18 Bilinçli yapılmayanlar.** Ufka yakın yıldızların sönükleşmesi (şader ister), BÜYÜYEN gecesinde aurora, bulutların
kızıl tonlanması (`getCloudColor` mixin ister), Iris/Oculus denemesi (kurulu değil), ay YAKLASIYOR durumu (G8).

**G19 Gün döngüsü kapalıyken kızıl cüce (şef kararı).** → Şef kararları (Faz 2b).

**G20 Kapı sayacının kaydı.** Donma %100'deyken sayac her tick değişir. Değişen her tick'te yalnız `setDirty()` (ucuz bayrak;
disk yazımı vanilla dünya kaydında/oto-kayıtta bir kez olduğu için ek yazma yok, çöküşte kayıp oto-kayıt aralığını aşmaz);
`ekVeriDegisti` (olay yayını) yalnız durum geçişinde (sayacın sıfırlanması, kapının açılması). Önceki "20 tick'te bir" hem
19 tick'lik kayıp penceresi bırakıyor hem saniyede bir gereksiz olay yayıyordu: en az yazma ve olay bu (inceleme G-P2-2).

**G21 runClient kilidi.** → Şef kararları (Faz 2b).

**G22 "Yıldızlı Gece" (N16) tanımı.** Ay yok + GECE + gökyüzünü gören konum. "Gece" vanilla gün saatinin 13000–23000
aralığıdır, güneşsiz dönemde de: gündüz yıldız görünse de saymaz (şart "gecede" diyor). Saf tanım
`mantik.GokyuzuOlcutleri` (JUnit `GokyuzuOlcutleriTest`), API `Gokyuzu.yildizliGeceMi(oyuncu)` (eski
`yildizliGeceyiGoruyorMu` yerine; inceleme G-P2-3).

**G23 Samanyolu bandı yumuşak kenarlı, gündüz yarı parlak (görsel değerlendirme P2).** Doku: kenar düşüşü şerit yarısının
dış %70'ine yayıldı (`smoothstep(0, 0,35)^1,5`) ve 1'de kırpma yerine yumuşak doyma (`1 − e^(−1,6·x)`): doymuş piksel %2,54 →
%0, en dik enine eğim medyanı 62 → 46, dış çeyreklerde 4,0 → 3,0. Çizim: şerit ±12° → ±14°, enine üç bant, dış %30'da köşe
alfası kenarda 0 (doku ne olursa olsun sert kenar kalmaz); gündüz çarpanı `lerp(gunIsigi, 1, 0,5)`. Betik deterministik (iki
çalıştırma aynı SHA-256 `590d8945…`; diğer 23 doku değişmedi). Kareler `kareler-samanyolu/` (s1 güneşsiz öğle, s2 sabah ufuk,
s3 aysız gece, s4 kızıl cüce öğle), ölçüm `samanyolu-olcum.txt`.

**G24 Kilit sinyalde bırakılır, çocuk öldürülür (inceleme G2-1).** §A.6 'Sinyal'. İşleyici yalnız SIGTERM gönderir ve sinyali
kaydeder; bekleme ve SIGKILL'e yükseltme ana döngüde (`wait(0.5)` yoklaması). İşleyicinin içinde beklemek, dıştaki `wait()`'in
tuttuğu `Popen` waitpid kilidi yüzünden kilitleniyordu (ilk deneme sınamada yakalandı). `Popen` öncesi sinyaller maskelenir,
çocukta maske açılır: çocuk başlarken gelen sinyal de kaybolmaz. Gradle notu: grup SIGTERM'i sarmalayıcıyı öldürür; ayrı
süreçteki Gradle daemon'u istemcisi kopunca derlemeyi iptal eder ve runClient JVM'i onunla kapanır [ÇIKARIM: runClient ile
sinyal denemesi yapılmadı].

**G25 Samanyolu ölçümünün sınırı (inceleme G2-3).** Ekran karelerinden yapılan kenar ölçümü bandı kalıntı nebulasından ve
galaksi karelerinden ayıramıyor; s1/s2'de oranın artması da bu yüzden yorumlanamaz. Kesin olan yalnız dokudaki ölçüm (dış çeyrek
eğimi 4,0 → 3,0, doymuş %2,54 → %0) ve çizimdeki kenar alfa bantları (kod). Ekrandaki sonucun **kesin kabulü şefin gözünde**:
`kareler-kosu4/gokyuzu-04a-gunessiz-ogle-tepe.png` ↔ `kareler-samanyolu/gokyuzu-samanyolu-s1-gunessiz-ogle-tepe.png`. Kesin
ekran ölçümü istenirse yolu: kalıntı nebulası kapalı bir test karesi ya da bant çemberinin kameraya izdüşümüyle yalnız bant
normali boyunca profil (bu turda yapılmadı: runClient istenmedi).

### Reddedilen yollar

- Ayrı gökyüzü SavedData'sı ve paketi (spike `GokyuzuVerisi`, `GokyuzuPaketi`): çekirdeğin tek `DunyaDonemi` + `DonemPaketi`
  yeter (G3, G6).
- Önceki evreyi pakete eklemek: çekirdek dosyası, gereksiz (G10).
- Süpernova koreografisini evre ilerlemesiyle tanımlamak: 1,5 günde 40 sn ilerlemenin %0,15'i, saniyelik paketle titrer;
  gerçek saniye seçildi (G9).
- Kızıl cüceyi kapı açılır açılmaz doğurmak: bölüm D "ilk şafakta". Gün saatini şafağa sarmak: madde 16.
- `AFTER_SKY` birincil yol ve ay örtüsü: spike G1; örtü kodu silindi.
- Gerçek zincirle çekirdek taklidini birlikte çalıştırmak: çift ilerleme, kapısız kızıl cüce (G15).
- Her tick'te yeni `GunesZinciri.Ayarlar`, her pikselde yeni ışık tonu üretmek: önbellek (G14).

## Faz 2b — Yangın

Parça `yangin` (dal `is/yangin`, son commit `31ce86a`; `main`'e birleştirme `a1df326`). Kaynak `.vibe/kararlar/yangin.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır; metindeki §A/§1… göndermeleri o dosyaya aittir.
Şef kararları (Y-05, Y-19, Y-20, Y-22, Y-23, Y-24, Y-27, Y-28) "Şef kararları (Faz 2b)" bölümündedir; N17 satırları sondaki tablodadır. Rapor
`.vibe/rapor/yangin.md`.

**Y-01 Kendi ateş bloğu (`AfetAtesiBlogu`) + kendi yayılma kuyruğu.** K0-20'deki "doğrulanmadı" riski deneyle kapandı:
GameTest `afetAtesiKendiBasinaYayilmaz` aynı sahnede `doFireTick=true` ile 200 tick boyunca bir `afet_atesi` ve bir vanilla
ateş yaktı; log (iki koşu): `Y-01 deney: doFireTick=true, 200 tick: afet ateşi 0 blok, vanilla ateş 3 blok değiştirdi`
ve aynı satır `... vanilla ateş 1 blok` (vanilla ateş rastgele tick'e bağlıdır; afet ateşi her koşuda 0).
`BaseFireBlock` alt sınıfı `FireBlock`'un rastgele tick'ini (yaş, sönme, komşu yakma, yakıtı havaya çevirme) taşımaz;
vanilla dokusu (`template_fire_floor` + `block/fire_0/1`), ışık 15, `entityInside` hasarı ve `animateTick` ses/dumanı gelir.
Böylece bütçe, tavan, `afetBlokHasari` ve yağmur sönmesi yalnız kuyruktan gelir; `doFireTick` hangi değerde olursa
olsun (GameTest dünyasında false) aynı davranır. Reddedilen: vanilla `FireBlock` + kendi kuyruğu (§D).

**Y-02 Yanıcılık vanilla/Forge değerinden (N12).** `IForgeBlockState.getFlammability` yanma süresini ve yanıp
yanmayacağını, `getFireSpreadSpeed` tutuşma olasılığını verir; başka modların `FireBlock.setFlammable` ya da Forge
geçersiz kılmaları kendiliğinden geçerlidir. `dogalafetler:yanmaz` her şeyin önündedir.

**Y-03 Yayılma modeli.** Hücre en sık 10 tick'te bir işlenir; her işlenişte yerel kıvılcım yarıçapında 3 rastgele
deneme (dy −1…+2, yukarı eğilimli). Olasılık = `getFireSpreadSpeed`/100 × `yayilmaHizCarpani` (config) ×
`yayilmaCarpani(yogunluk)` × rüzgâr × eğim × nem (üst sınır 1). Rüzgâr: o oyun gününden türeyen sabit yön, güç 0,6
(rüzgâr yönünde ×1,6, karşısında ×0,4, yanlarda ×1): yangın bir cephe halinde ilerler. Eğim: yukarı ×1,6, aşağı ×0,5
(taç yangını). Nem: yağmurdaki (açık ya da korunaklı) hücre hiç yaymaz (Y-08); kuru hücrenin yağmur altındaki hedefi
×0,3 (yağışsız biyomdan yağan biyoma). Sıçrama: her işlenişte %4 olasılıkla
rüzgâr altında (±20°) 2…`sicramaMesafesi` blok öteye düşen kıvılcım, iniş sütununun en üst yakıtını rüzgârsız
olasılıkla tutuşturur. Yoğunluk yayılan hücreye aynen geçer (sönümlenmez): seviye karakteri cephe boyunca korunur;
yangını sınırlayan tavan, yakıt ve `kaynakBitti`'dir.

**Y-04 Kuyruk, öncelik, bütçe, tavan, yüklü olmayan chunk.** Her sunucu tick'inde (boyut başına) işlenmeye hazır hücreler
puanlanır: en yakın oyuncuya uzaklık / (1 + bekleme/100 tick); en küçük `bütçe` puan O(n log k) seçilir
(`OncelikSecici`). Bütçe = `tickButcesi × TpsOlcer.kismaCarpani()` (1 / 0,5 / 0,25, en az 1); başka TPS ölçer yok. Bütçe
değişince tek INFO satırı: `yangın kısma: <boyut> bütçe A -> B hücre/tick (TPS çarpanı k, TPS t, aktif n)` (§6 #10). Tavan
(`aktifTavan`) aşılınca en eski hücreler (ekleme sırası, `LinkedHashMap`) söner. Yüklü olmayan chunk'taki hücre
işlenmez, oraya yayılma ve tutuşturma yapılmaz; chunk yüklenince kaldığı yerden sürer (oyun saati geçmişse yanma
süresi dolmuş olabilir: yokken de yanmış sayılır).

**Y-05 `kaynakBitti(kaynak, temiz)`: iki kip, vanilla'ya devir yok (şef kararı, inceleme F9).** → Şef kararları (Faz 2b).

**Y-06 Yanma sonucu: hava.** Yakıtın yanma süresi `60 + 9600/(flammability + 20)` tick (çimen ~7 sn, yaprak 9 sn, tahta
15 sn, odun ~22 sn); süre dolunca blok havaya döner (vanilla `FireBlock.checkBurnOut` gibi; Forge `onCaughtFire` çağrılır: TNT
patlar). Yaprak ve odun ikisi de hava. Kömürleşmiş blok reddedildi (§D).

**Y-07 `afetBlokHasari=false`: hiçbir blok değişmez.** `afet_atesi` de bir blok değişimi olduğu için konmaz (kural
yangın sırasında kapatılırsa konmuş ateş blokları ilk işlenişte kalkar). Hücre yalnız veri + paket parçacıkları + ısı +
varlık hasarıdır (ateş konumundaki canlıya 3 sn yanma + 1 `inFire` hasarı, her işlenişte). Yanıp biten konum 2400 tick
yeniden tutuşmaz. GameTest: sahne hash'i 320 tick boyunca aynı.

**Y-08 Yağmurda sönme.** Ateş konumu ya da yatay komşusu yağmur görüyorsa (açık; vanilla `isNearRain` gibi) her
işlenişte sönme olasılığı `yagmurSonmeOlasiligi` (0,25) + 0,3 × yaş oranı, beklenen sönme ~2–3 sn. Yağan biyomda
gökyüzüne kapalı (yaprak ya da çatı altı) ateş korunaklıdır: aynı olasılık × 0,3 (`KORUNAKLI_YAGMUR_ORANI`), ~5–8 sn.
Vanilla yalnız yağmur gören ateşi söndürür; görsel koşu 2'de taç içindeki 139 hücre 30 sn yağmurda yanmaya devam etti,
bölüm G "yağmur yağınca sönebilsin" bütün yangın için istendiğinden korunaklı sönme eklendi. Bu da yetmedi: koşu 4–5'te
yağmurda 527 hücre 3 sn'de 120'ye indi ama taç içinde yayılma sönmeyi geçti ve 15 sn'de 188'e çıktı (korunaklı hücrenin
ömürü boyunca ~5 yeni hücre; kuru taçta yayılma ×0,65 nemle sürüyordu). Son kural: yağmurdaki (açık ya da korunaklı)
hücre **hiç yaymaz** (kıvılcım sıçraması dahil), yalnız söner ya da yanıp biter; yangın yağmurda tekdüze azalır. GameTest
`yagmurdaKorunakliAtesYayilmaz` (cam çatı altında 5×5 yaprak, seviye 4 yoğunluğunda iki tohum, en çok 2 hücre) eski
kodda kırmızıydı (`en çok 5 hücre`, `gametest-kirmizi.log`). Yeraltı (Y-23): korunaklılık yüzeye yakınlıkla sınırlı; üstü
8 bloktan derin katı örtülü ateş (maden, mağara) yağmurdan etkilenmez ve normal yayılır. Yağışı olmayan biyomda
(çöl, savan) ve karlı biyomda (yağış `SNOW`) sönme yok, vanilla ile aynı. Sönen hücrenin konumu paketle gider: istemcide buhar (bulut parçacığı) + yüksek perdeli
`block.fire.extinguish`. Config ile kapatılabilir.

**Y-09 Ağ: chunk özeti, 2 Hz, yalnız yakındakilere.** `yangin/ates_durumu`: yatayda 128 blok içindeki yangın chunk'larının
özeti (hücre sayısı, ağırlık merkezi, en üst Y); 64 blok içindeki chunk'lar için en çok 48 hücre konumu (chunk içi 1 int);
64 blok içindeki buhar konumları. Yangını olmayan oyuncuya paket gitmez; menzilden çıkana bir kez boş paket (istemci
temizlenir). Vanilla ateş bloğu değişimleri ayrıca vanilla paketiyle gider. Okuyucu sayıları sınırlar (1024 özet,
256 hücre, 256 buhar).

**Y-10 İstemci efektleri ve LOD (N13).** Tasarım §5 tablosu `IstemciYangin.Kademe`'de: alevli en yakın hücre 40/120/250/500,
kıvılcım/sn 30/80/160/300, hücre dumanı her 4/2/1/1 hücrede (Ultra ×1,5), duman sütunu tavanı `yangin.sutunTavani` × 0,25 /
0,5 / 1 / 2 (Düşük/Orta/Yüksek/Ultra; Yüksek 64, Y-24). `lodMesafesi` (48) ötesinde hücre parçacığı yerine chunk başına
duman kümesi (96 bloka kadar), `sutunMesafesi` (160) içindeki yangında duman sütunu (200–280 tick ömür, büyüyerek sönen
2,5–4,5 boy, ilk dörtte birinde alttan turuncu aydınlanır) ve 12+ hücrede tepesinde pirokümülüs kümesi (Y-24). Kıvılcım
vanilla lav parçacığı değil kendi `yangin_kivilcimi`: rüzgârla sürüklenir, yükselir, sarıdan kızıla dönüp söner. Kameranın
arkasındaki (12 blok ötesi) hücreler parçacık üretmez. Bütün parçacıklarımız (buhar ve kıvılcım dahil)
`ClientLevel.addParticle(tur, true, …)` ile **force** eklenir: vanilla `LevelRenderer.addParticleInternal` force olmayan
parçacığı kameradan 32 bloktan uzaksa atar ve `addAlwaysVisibleParticle` bunu atlatmaz (bytecode: `force=false,
decreased=true`). Görsel koşu 4'te Yüksek karede 120 tick'te 6145 üretim çağrısına karşın yalnız 105 parçacık yaşıyordu:
48 blokluk LOD ve 96 blokluk küme dumanu fiilen 32 blokta kesiliyordu. Force vanilla parçacık ayarını da atladığı için
ayar (Tümü / Azaltılmış / En Az) yoğunluğu 1 / 0,5 / 0,2 ile çarpar; mesafe ve sayı yalnız LOD ve kademeyle sınırlıdır.
Kendi parçacıklarımız: tam parlak
alev (4 kare 16²), büyüyerek yükselen gri duman (4 kare 32²), dokular `tools/assets/yangin_dokulari.py` ile tohumlu üretilir.
Duman dokusunun alfası kenarda sıfıra iner (yarıçapın %60–100'ü yumuşak sönüm), alevin kenar satır/sütunları en çok %20:
ilk sürümde gürültü kenar piksellerine %40'a varan alfa veriyordu ve büyük duman quad'ları karede sert kenarlı koyu
dörtgenler olarak görünüyordu (görsel koşu 1, Yüksek kare). `AfetAtesiBlogu.animateTick` yalnız vanilla ateş sesini çalar:
vanilla `BaseFireBlock` her çağrıda üç `LARGE_SMOKE` üretiyordu (inceleme F7: karelerdeki kare lekelerin aday kaynağı); duman
kalite kademeli kendi parçacıklarımızdan gelir. Isı titremesi Faz 4'e (şef kararı).

**Y-11 Kalıcılık.** Boyut başına SavedData `dogalafetler_yangin` (biçim sürümü 2): hücre konumu, kaynak, yoğunluk,
başlangıç/bitiş/son işleniş saati, yayılabilirlik, ateş bloğu konumu; sürüm 2 bekleyen silme konumlarını (`bekleyen`, Y-19)
ekler, sürüm 1 kaydı boş kümeyle okunur. Sunucu yeniden açılınca yangın sürer (runServer koşu 2, kod `1352c99`: 234
hücreyle kapanan yangın açılışta 263 hücreyle sürüyordu). "Yanıp biten" hafızası, buhar kuyruğu ve süren alan tutuşturma
işleri kaydedilmez (yarım kalan alan yeniden açılışta sürmez; tutuşan hücreler sürer).

**Y-12 Ateş bloğu dışarıdan kalkarsa hücre söner.** Oyuncu ateşi vurursa, su akıp silerse ya da başka bir blok yerine
konursa hücre bir sonraki işlenişinde söner (vanilla'daki gibi elle söndürülebilir).

**Y-13 GameTest dünyası ve şablon.** K-C24 dünyasında (`doFireTick=false`) testler kendi sahnesini kurar; şablon
`yangin_sahne` 9×8×9 hava (`bos` 1³). Dünya geneli durumu değiştiren testler (gamerule, hava, TPS kademesi) kendi batch'inde
durur ve değeri iddialardan önce geri koyar; yağmur testleri zaman aşımında da havayı açar. GameTest dünyası koşular
arasında saklanır ve batch sırası sabittir (iki koşuda aynı 18 batch sırası), sahneler aynı yere kurulur: zaman aşımıyla
düşen kırmızı koşunun kayıtlı hücreleri sonraki koşuda aynı sahnenin yeni yapraklarını tutuşturdu (düzeltilmiş kodla bir
kez `en çok 4 hücre`, sonraki beş koşu yeşil). `YanginOyunTestleri` GameTest sunucusu açılınca (`ServerStartedEvent`,
`GameTestServer`) bütün boyutlarda kalan hücreleri söndürür; üretim sunucusunu etkilemez.

**Y-14 TPS kademesinin testte zorlanması.** `TpsOlcer`'ın zorlama yardımcısı yok ve `core/**` yangın işçisinin değil:
yönetici çarpanı `DoubleSupplier` arkasından okur (varsayılan `TpsOlcer::kismaCarpani`), test paket içi
`kismaKaynagiAyarla` ile 0,25 verir. Gerçek kademe düşüşü runServer koşu 2'de gösterilir: yangın sürerken 144 uzak chunk
`forceload` ile üretilir, `TPS 6.0 (165.4 ms/tick): bütçe çarpanı 1.00 -> 0.25` ardından
`yangın kısma: minecraft:overworld bütçe 64 -> 16 hücre/tick (TPS çarpanı 0.25, TPS 6.0, aktif 241)`; `yangin bilgi` aynı
anda `budget 16/64 per tick (TPS throttle ×0.25, TPS 6.0)`. Aynı log'da yarı kademe ve toparlanma da vardır:
`TPS 17.1 (58.3 ms/tick): bütçe çarpanı 0.25 -> 0.50`, sonra `yangın kısma: … bütçe 16 -> 64 … TPS 20.0` (kod `1352c99`). Senaryonun
ilk `bilgi` adımı bütçeyi 64/32/16'dan herhangi biri kabul eder: yeni dünyada chunk üretimi kısmayı açabiliyor (bir
koşuda TPS 6,6'da `budget 16/64` yazıldı ve katı `64/64` beklentisi düştü; mod doğru davranıyordu).

**Y-15 Görsel kanıt otomasyonu.** `client.yangin.YanginOtomasyonu` tek oyunculu dünyada entegre sunucu thread'inde en
yakın ormanı bulur (`findClosestBiome3d`), ±24 blok içinde tepesi yaprak olan sütunu en çok merkeze kayar,
alacakaranlığa (13000) alır, günün rüzgârına dik 33 blokluk bir taç cephesi (yalnız yaprak tepeli sütunlar, yoğunluk
0,7 = seviye 4) tutuşturur ve seyirci kamerayı cephenin 30 blok rüzgâr üstüne, aradaki ağaç tepelerinin 12 blok üstüne
koyar, cepheye baktırır (tasarım §3 "rüzgâr üstü"; yangın kameradan uzağa ilerler). Koşu 1–2'de kamera cephenin
batısında ve taç hizasındaydı: koşu 1'de aradaki ağaçlar ateşi örttü (turuncu piksel %0,02), koşu 2'de ateş göründü
(sıcak piksel %0,9) ama eski dokunun sert kenarlı duman quad'ları karenin üst yarısını kapattı. Koşu 3'te (34 blok,
kareler 12 sn arayla) cephe ikinci kareye kadar LOD mesafesinin (48) ötesine ilerledi: Düşük kare Yüksek'ten çok
parçacık gösterdi (399 / 313; hücre alevi yerine kalite bağımsız küme dumanu ve vanilla ateş dumanu). Son düzen:
tutuşturmada Yüksek kademe seçilip parçacık motoru boşaltılır (`ParticleEngine.setLevel`), 6 sn sonra Yüksek kare,
Düşük'e geçip yine boşaltılır, 6 sn sonra Düşük kare: iki kare aynı parçacık yaşında, yangın 6 sn ilerlemiş. Kare
satırı kendi üretimimizi de yazar (`IstemciYangin.uretileniAl`, vanilla ateş dumanu ve yağmur hariç). Ardından yağmur
başlar, 3 ve 15 sn sonra iki kare alınır. Her karede sunucu, çevredeki ateş
bloklarından kaçının görüş alanında (dikey 70°, pencere en/boy oranı) ve kameradan açık görüş hattında olduğunu
(`ClipContext.Block.COLLIDER`, yaprak dahil) log'lar: görüntü kanalı olmayan doğrulayıcı için karenin sayısal eşi.
`/forge tps`, `yangin bilgi` ve 5 sn'de bir aktif hücre sayısı da log'dadır. Protokol K0-21 rev. 2, workspace 95,
`tools/senaryo/yangin_gorsel.py` (kareler `KANIT/kosu-<n>/`); ajan kabuğu Hyprland imzasını devralmadığı için betik tek
canlı örneğin imzasını `$XDG_RUNTIME_DIR/hypr/` altından alır (birden çoksa durur).

**Y-16 Yayılma ayarı (görsel koşu 1 bulgusu).** İlk modelde (2 deneme, yoğunluk her kuşakta ×0,97, sıçrama yok, yanma
`60 + 4800/(f + 20)`) gerçek ormanda 237 hücrelik cephe 17,5 sn'de 48 hücreye, 50 sn'de 0'a indi: kare duman ve çıplak
gövde gösterdi (turuncu piksel %0,02), yangın "tutuşa tutuşa" yayılmadı. 80×80 bloklu çevrimdışı orman benzetimi
(Python, oyundaki formüllerle; ağaç sıklığı 1/26, taç doluluğu %90; 90 sn'de yanan blok): eski model 1797; yeni model
(yoğunluk çarpanı 0,5 + 1,3·y, sönümsüz yoğunluk, 3 deneme, rüzgâr altı sıçrama, yanma `60 + 9600/(f + 20)`)
S1 165, S2 831, S3 3593, S5 11 282. Seyrek ormanda (1/36, %85) S1 557, S3 1858, S5 7097. Seviyeler arasında belirgin
fark, S1 kendiliğinden sönebilir, S5 bir ormanı bir buçuk dakikada yutar; tavan (1500) S5'i sınırlar. GameTest
`yaprakKumesindeYayilir`: 7×3×7 yaprak kümesinin alt katmanında iki tohum (yoğunluk 0,1, sıçrama eşiğinin altında)
80–150 tick'te 12+ bloka yayılır. İlk sürümün tek tepe tohumu benzetimde %4–12 ihtimalle yayılmadan sönüyordu ve bir
koşuda 240 tick'te 6 blokla düştü; iki alt tohumla 20 000 benzetimde 0 başarısız, en geç 150 tick.

**Y-17 Yaygın yangın davranışı (görsel koşu 2 ölçümü, Y-16 modeli).** Gerçek ormanda yoğunluk 1 ile tutuşturulan
118 hücre 5 sn'de 1215'e çıktı (taç yangını), sonra yaprak yandıkça 30 sn boyunca 426–913 arasında kendini sürdürdü;
`/forge tps` aynı anda entegre sunucuda 7,8 ms/tick, TPS 20 (bütçe 64, kısma yok). Yağmur başlayınca 692 hücre 6 sn'de
267'ye, 26 sn'de 138'e indi; kalan korunaklı taç içi hücreler Y-08 değişikliğinin nedenidir.

**Y-18 Son görsel koşu (koşu 6, kod `4d6cae6`; `372a908` yalnız otomasyon log'u ekler).** Seviye 4 cephe (92 hücre)
5 sn'de 862 hücre; `/forge tps` entegre sunucuda 12,7 ms/tick, TPS 20, `yangin bilgi` 809/1500, bütçe 64/64. Yüksek kare:
görünür ateş bloğu 298 (37,7 blok), 120 tick'te kendi üretimimiz 5995, yaşayan parçacık 2068. Düşük kare (6 sn sonra):
218 ateş bloğu, üretim 1188, parçacık 786; iki kare arasında %20 piksel farkı. Yağmur başlayınca 498 hücre 3 sn'de 68'e
indi (görünen ateş 24); sonra toplam aktif 77 → 96 → 143 → 124'e çıktı, görünen ateş 16'da kaldı ve son karede sıcak
piksel %0,03. GameTest `yagmurdaKorunakliAtesYayilmaz` aynı kodda yeşil. Yükselişin kaynağını inceleme (inceleme-yangin
Notlar) koşu dünyasından çözdü: rüzgâr +x yönünde, yanan 1848 bloğun 294'ü yağışsız `savanna`/`savanna_plateau`'da;
orada yağmur yangını söndürmez (Y-08, vanilla ile aynı). Hata değil.

**Y-19 Bekleyen silme (inceleme F1, P1; şef kararı).** → Şef kararları (Faz 2b).

**Y-20 Ateş modeli ve dayanak (inceleme F3; şef: ikisi de).** → Şef kararları (Faz 2b).

**Y-21 `alanTutustur` bütçeli (inceleme F5).** r=24 kürede ~57 900 konum × 6 komşu = tek tick'te ~350 bin okuma oluyordu.
Artık önce konumun kendisi `yanabilir` mi bakılır (tek okuma; hava ve taş hemen elenir), komşu taraması sonra; tarama
`mantik.KureTarayici` ile tick başına `alanOkumaButcesi` = hücre bütçesi × 64 okumayla (64 → 4096, kısmada 1024) yapılır ve
kaldığı yerden sürer. JUnit: r=24 kürenin her konumu bir kez, 4096 bütçeyle ⌈57 905 / 4096⌉ = 15 çağrıda; bütçe aşılmaz.
`sondur(bolge)` bölgedeki süren işleri, `kaynakBitti` o kaynağın işlerini siler.

**Y-22 `minecraft:fire` etiketi (inceleme F2; şef kararı).** → Şef kararları (Faz 2b).

**Y-23 Yağmur yalnız gökyüzüne yakın ateşi etkiler (inceleme F6; şef kararı).** → Şef kararları (Faz 2b).

**Y-24 İstemci özeti, duman sütunu tavanı, pirokümülüs, kıvılcım (inceleme F4, F7; şef kararı).** → Şef kararları (Faz 2b).

**Y-25 Yöneticinin tick maliyeti ölçülür (inceleme F8).** Her boyutun tick'i (bekleyen silme + alan işi + işleme + özet)
`System.nanoTime` ile ölçülür, son 100 çalışan tick halkada tutulur; `yangin bilgi` ortalama/en çok ms yazar, 30 sn'de bir
INFO `yangın özeti: … yönetici o ms ort / m ms en çok`. Ölçümler: GameTest 0,11–0,13 ms ort / 1,9–3,2 ms en çok; dedicated
234 hücre 0,51 / 4,02 ms, kısma sırasında 251 hücre 0,77 ms ort; yeniden açılıştan hemen sonra 263 hücrede 10,93 ms ort /
18,41 en çok ([INFERENCE] soğuk JIT ve ilk tick'lerde birikmiş bütün hücrelerin hazır olması; ayrıştırılmadı). İstemci FPS ve
Ultra karesi bilinen sınır: gizli workspace ~1 FPS çizer, ölçüm Faz 5'te görünür pencerede (şef kararı, inceleme önerisi).
Kısma kanıtı yapay yükle (forceload chunk üretimi) alınır; log satırını kanıtlar, üretimde yeterliliği Faz 5 ölçer.

**Y-26 Tur 2 görsel koşusu (koşu 7, kod `1352c99`, tek protokol koşusu).** Kamera adayları cephe çizgisinin ucundan 24→10
blok; hiçbiri kaplama tahmini hedefine (0,35) ulaşmadı, en yüksek olan (10 blok, 0,091) seçildi. Yüksek kare: görünen
ateş bloğu 358 (koşu 6: 298), ortalama uzaklık 30,3 blok, kaplama tahmini 0,115, aktif 1003, canlı sütun 33 / küme 10,
120 tick'te kendi üretim 5125; `/forge tps` entegre sunucu 9,8 ms/tick TPS 20, yönetici 0,65 ms ort / 2,33 ms en çok.
Düşük kare (6 sn sonra): 252 ateş bloğu, üretim 1076, sütun 12 / küme 3, iki kare arası %30,8 piksel farkı. Sıcak piksel
(R > 180, 60 < G < 200, B < 90): Yüksek %1,48 (koşu 6: %0,86), Düşük %5,39; turuncu (R−B > 80) Yüksek %4,2, Düşük %8,3.
Değerlendirici model (`judge_batch`, görüntülü, `litellm/claude-haiku-5-5-1m`; `kosu-7/degerlendirme.json`): Yüksek ve
Düşük karede alev payı "%8 ve üstü" (koşu 6: "%2–8"), puan 2/4 (koşu 6 da bu modelle 2/4; inceleyicinin modeli 3/4
vermişti, ölçek farklı). Aynı model: cephe "yandan" okunmuyor (altı karenin hiçbirinde), pirokümülüs görünmüyor (sütun
karesi 1/4), yağmur karesinde buhar görünmüyor, Yüksek ve sütun karesinde kare lekeler var, yüzen alev yok. [INFERENCE]
Nedenler: kamera cephe çizgisinin uzantısında olduğundan cephe derinliğe doğru uzanıyor ve eğim ~25° aşağı (tasarım §3
−5°); küme parçacığı gökyüzü ışığıyla aydınlandığı için alacakaranlıkta koyu kalıyor; Yüksek'te hücre dumanı alevin önünde
(Düşük karede sıcak piksel 3,6 kat). Yağmur karesinde görünen 139 ateş bloğunun 123'ü yağışsız savanada (sönmez, Y-08); son
karedeki 48 hücrenin hepsi `savanna_plateau`. Bu koşu tek izinli koşuydu; düzeltmeler (dik kamera ve düşük eğim, kümenin
alttan ateşle aydınlanması, yağmur karesi orman bölümünde) görsel doğrulamasız yapılmadı, şefin ikinci koşu kararına bırakıldı.

**Y-27 Afet bitişi merkezi abonelik (inceleme-yangin-2 F2-1; şef kararı).** → Şef kararları (Faz 2b).

**Y-28 Üç görsel düzeltme, kosusuz (inceleme-yangin-2 F2-2, F2-3; şef kararı).** → Şef kararları (Faz 2b).

**Y-29 Testler güçlendi (inceleme-yangin-2 F2-4).** `kaynakBittiYanarakBiter` kısaltmayı sınar (kütük: ilk bitiş > 80 tick,
sonra ≤ 80; kütük 160 tick zaman aşımında havaya döner). `yukluOlmayanAtesChunkYuklenincekalkar` 1 Hz denetimi kapatır
(`bekleyenDenetimiAyarla(false)`, başarıda ve 95. tick'te geri açılır): bloğu yalnız `ChunkEvent.Load` yolu kaldırabilir.

### Reddedilen yollar

- **Vanilla `FireBlock` + kendi kuyruğu:** `doFireTick=true` iken vanilla ateş kendi rastgele tick'iyle bütçe ve tavan
  dışında yayılır, `afetBlokHasari=false` iken de blok yakar, yağmur sönmesi config'e uymaz (Y-01 deneyi: 200 tick'te
  1–3 blok); `doFireTick=false` iken hiç sönmez (yaşlanma rastgele tick'te). Her iki halde §6 bozulur.
- **`FireBlock` mixin'i:** bütün vanilla ateşin davranışını değiştirirdi (çakmak, lav); event ve kendi blok yeterli
  olduğu için mixin gerekmedi.
- **Kömürleşmiş blok:** yeni blok + doku + etiket ve veri paketi işi; vanilla yanmış bloğu havaya çevirir. Yanmış alan
  izi gerekirse Faz 4 (orman yangını) zemin dönüşümünü `afetBlokHasari` ile kendi yapar.
- **Hücre başına parçacık paketi / vanilla `sendParticles`:** her parçacık için sunucu paketi, 5–10 oyuncuda trafik
  patlar; chunk özeti + istemcide üretim yerine seçildi.
- **Yoğunluğun kuşaktan kuşağa sönümlenmesi** (ilk sürüm ×0,97): S5 yangını birkaç kuşakta S1'e düşürüyordu; seviye
  karakteri korunmalı.
- **Ayrı TPS ölçer:** çekirdek §8 "başka TPS ölçer yazılmaz".
- **Bekleyen silme yerine chunk'ı zorla yüklemek** (inceleme F1 alternatifi): `sondur` ya da tavan taşması bir anda onlarca
  chunk yükletir, TPS kısmasını ve "yüklü olmayan chunk bekler" kuralını bozar.
- **Buhar paketini 128 bloka çıkarmak** (inceleme F7 alternatifi): şef kararıyla yağmur karesi ≤ 40 blokla alınır; 64 blok
  ötesinde buhar zaten parçacık olarak görünmez, paket büyürdü.

## Faz 2b — Donma

Parça `donma` (dal `is/donma`, son commit `906fa00`; `main`'e birleştirme `a0292fb`). Kaynak `.vibe/kararlar/donma.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır; metindeki §A/§1… göndermeleri o dosyaya aittir.
Şef kararları (K-D11, K-D21, K-D22, K-D23, K-D25, K-D31, K-D33, K-D34, K-D37, K-D38) "Şef kararları (Faz 2b)" bölümündedir; N17 satırları sondaki tablodadır. Rapor
`.vibe/rapor/donma.md`.

**K-D6 Kod düzeni.** Saf mantık `donma.mantik` (Minecraft sınıfı yok, JUnit): `KisMaskesi`, `KisAlani`, `DonmaHedefi`,
`DonmaEsikleri`, `EsikKumesi`, `DonusumTablosu`, `SuSutunu`, `KiyiUzakligi`, `ChunkOnceligi`, `YetismeButcesi`, `DonmaIlerlemesi`,
`BuharlasmaHedefi`. Spike sınıfları bu pakete taşındı; `config.Ayarlar` yerine `DonmaAyarlari` (`AyarKaydi`), spike'ın kendi
SavedData'sı ve TPS ölçeri yerine çekirdek (`DunyaDonemi`, `TpsOlcer.kismaCarpani()`).

**K-D7 İnce buz su sütununun seviyesidir.** Sütun buz seviyesi `0` yok, `1` üst blok ince buz, `k+1` üstte `k` blok buz
(`SuSutunu.planlaSeviye`); `DonmaEsikleri.buzSeviyesi/buzSeviyesiCozulme` ilk katmanın ilk (donarken) ve son (çözülürken)
`donma.esikler.inceBuzPayi` (6) puanını ince buz yapar (kalınlık adımlarından küçük olmalı). Tablo satırında `"ince"` alanı.
Gerekçe: tasarım notu §2 (su → ince buz → buz; çürük buz) ve vitrindeki koyu halka; sütunun kökü yine sudur, tersinirlik
bozulmaz. `IceBlock`: ışıkta erime ve kırılınca su bırakma vanilla buzla aynı.

**K-D8 Eşik aralığı atlaması (spike B7).** Sütunun etkin seviyesi `(eski, yeni]` aralığında hiçbir adımın eşiği yoksa
blokları okunmaz. `EsikKumesi` eşik fonksiyonlarının kendisinden kurulur (her seviyede önceki seviyeye göre değişen fonksiyon
var mı; tüm kıyı uzaklıkları için) + tablo satırlarının eşikleri; JUnit fonksiyonlarla tutarlılığı 120×12×17 aralıkta
doğrular. **Yalnız aynı yöndeki ardışık adımda serbest** (`DonmaHedefi.atlamaSerbest`, chunk verisinde son adımın yönü
`yon`): tam bir adımdan sonra her sütun o yönün fonksiyonundadır, ama yön dönünce (donma → çözülme) öbür fonksiyondadır ve
aralıkta eşik olmasa da değişir (JUnit `yonDonusundeAtlamaGuvenliDegil`: donma 60'ta kıyıda 3 blok buz, çözülme fonksiyonu
(55, 60]'ta sabit, 55'te 2 blok olmalı). Yarım adımdan devam, dönem değişimi ve buharlaşma adımında atlama kapalı ve
yön 0 yazılır; hiç işlenmemiş chunk (G 0, donma) donarken atlayabilir. Kıyı BFS'i yalnız atlanmayan ilk su sütununda
hesaplanır. Doğrudan yol (GameTest `bolgeyiIsle`) yön değiştirdiği için atlamasız çalışır.

**K-D9 Bütçe kesin üst sınırdır.** `YetismeButcesi`: tick başına en çok `tickBasinaMaksBlok · TpsOlcer.kismaCarpani()` blok;
birimli (sütun 1, blok 8, kıyı BFS'i 256 — K-D20); bir sütun ancak kalan birim en kötü sütunu (buz seviyeleri + buharlaşma derinliği kadar
değişim) karşılıyorsa işlenir — spike'ta bir sütunluk aşım olabiliyordu. Süre tavanı (`tickBasinaMaksMs`; K-D35'ten beri her
sütundan ve her chunk işi kurulumundan önce bakılır) aşılırsa sonraki tick'in tavanı aşım kadar düşer (en az dörtte biri; spike Faz 2 notu). JUnit 2000 tick × 3 kısma;
GameTest `chunkYetismesiAdimAdimVeButceAsilmaz` her tick'i ölçer.

**K-D10 Yarım adım capability'de.** İş başlarken `yarimG/yarimCozulme` yazılır, biterken silinir. Chunk adım ortasında boşalırsa
yeni iş eski değer olarak yarımı alır: işlenmiş sütunlar doğru yerde, işlenmemişler yönlü kuralla ileri gider (spike'taki
"G tam eski değere dönerse sütunlar bir adım ileride kalır" açığı kapandı). Bilinen sınır: G yarımın içine dönerse işlenmemiş
sütunlar G bir sonraki artışta yetişir.

**K-D11 `sifirla` bizim kar/buz/kırağımızı kuyrukla 0'a çözer** → Şef kararları (Faz 2b).

**K-D12 Kış parametreleri dünyaya sabit (spike B9).** Çözülme dönemi açılınca (`COZULME` olayı) config'in oran, dalga boyu,
bant, Y geçişi ve ılık ofset değerleri `ekVeri("donma").kis`'e yazılır ve `KisBolgesi.alan` oradan okur; config sonradan
değişse de kış bölgeleri kaymaz. `sifirla` kaydı siler (yeni döngü yeni config'i alır).

**K-D13 Süreç sınırları.** Kızıl cüce kapısı (`donma ≥ 100`, gecikme, şafak) gökyüzünündür; donma yalnız G'yi ve
çözülme dönemini yazar. `DonemDegistiEvent` işleyicisi yalnız sunucunun kendi `DunyaDonemi` nesnesine tepki verir
(çekirdek GameTest'i bağımsız bir dönem nesnesinin setter'larını çağırıp olay yayınlıyor). `kalici=true` kızıl cücede
çözülmeyi tamamen kapatır; `bagimsizCozulme=true` güneş normalken G'yi 0'a indirir (ikisi de varsayılan kapalı, madde 9).

**K-D14 Buharlaşma sütun kaydıyla (N9).** Sütun başına yalnız "orijinal su yüzeyi Y + kaldırılan blok sayısı"
saklanır (kuruyan sütunlarda). Hedef: `min(etkin derinlik, orijinal su derinliği)` — sığ göl tamamen kurur, okyanus
derinlik kadar alçalır. Yalnız üstü açık su kaynağı sütunları; akıcı su ve waterlogged bloklar kurumaz. Kaldırma ve geri
dolma `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE` ile: komşu su şekil güncellemesi almaz, deliğe akmaz (aksi halde gölün
kuruyan katmanı akıcı suyla dolardı); önceden planlı komşu sıvı tick'leri de silinir (K-D37). Geri dolma alttan yukarıdır; kaynak su "zaten dolu" sayılır ve geçilir (K-D24), akıcı su
kaynakla değiştirilir, yalnız katı blokta (oyuncu doldurdu) durulur ve kayıt bırakılır. Chunk'ın son uygulanan derinliği `buhar`; aynı chunk işinde sütun başına önce buharlaşma, sonra donma
(N10: "geri dolma tamamlanmadan donma adımı başlamaz"; GameTest `buharlasmaSonrasiOnceGeriDolarSonraDonar`). Bilinen sınır:
kuruyan suyun içindeki yosun/deniz çimi şekil güncellemesi almadığı için havada kalabilir (ilk komşu güncellemesinde kırılır).

**K-D15 Yağmur → kar yalnız istemcide, mixin'siz.** Kar yağışı eşiği (`donma.esikler.karYagisiEsigi`, 30; K-D33) aşılınca ve yağmur
yağarken (`levelData.isRaining()`) istemci yerel dünyanın yağmur düzeyini 0'a çeker (vanilla yağmur, yağmur sesi ve
splash çizilmez) ve kendi iri/yavaş kar tanelerini (vanilla `SNOWFLAKE`, 32 blok yarıçap, kaliteyle) düşürür; eşik altına
inince yağmur düzeyi geri verilir. Biyom sıcaklığı, sunucu havası, mob/kar oluşumu değişmez. Reddedilenler §D. Bilinen
sınır: kar sırasında vanilla şimşek parlaması (yağmur düzeyiyle çarpılır) görünmez; istemcide yağmur düzeyini okuyan
başka mod 0 görür.

**K-D16 Kardeş API'ler kancayla.** `SanalSu`, `SicaklikKancalari`, `KizilCuceBilgiKatkisi`, `DenizYukselmesiSureci` main'de yok:
donma onları import etmez; çağırdığı üç şey `DisKancalar` (boş varsayılan), dışarıya verdiği iki sağlayıcı `DonmaApi`
(§A.1/§A.2). Entegratör birleştirmede `DogalAfetler` kurucusuna üç satır ekler.

**K-D17 GameTest kapsamı.** Zamanlayıcı testleri global G'yi değiştirdiği için her biri kendi batch'indedir
(`donma_zamanlayici_1..4`) ve `DonmaZamanlayici.kapsamAyarla(sahnenin chunk'ları)` ile yalnız kendi chunk'larını işler (spawn ve
önceki testlerin chunk'larına dokunulmaz); bitince kapsamı kaldırır ve G'yi 0'a döner. Kapsam yalnız test kancısıdır.
Chunk yeniden yüklemesi gerçek `ChunkEvent.Unload/Load` olaylarını yayınlayarak yapılır (zorlanmış test chunk'ı boşaltılamaz).

**K-D18 Bitkiler ve çiçekler.** Üstünde bitki olan zemin sütunu değişmez (spike K-D3); çiçek/ot kırağısı için blok değişimi
yok (orijinal blok kaydı olmadan geri dönüşümsüz olurdu). "Yaprak karı" `#minecraft:leaves` köklü kar katmanıdır.

**K-D19 Ortak dosya satırları.** `DogalAfetler` kurucusuna tek satır `DonmaSistemi.kaydet(modBus)`; datagen katkısı lambdayla
(model üreticisine bağlı sınıf dedicated sunucuda yüklenmez). `build.gradle`, `AgKanali`, `AyarKaydi` değişmedi.

**K-D20 Kıyı uzaklığı chunk + 15 blok hale üzerinde (madde 9 "kıyılardan içeri").** Spike BFS'i chunk'ın 1 blokluk halkasını
görüyordu ve halkadaki su hücrelerini kaynak olarak yaymıyordu: kıyısı komşu chunk'ta 2+ blok uzakta olan sütun yanlış
(fazla) uzaklık alıyor, chunk kenarında buz geç geliyordu. Hata GameTest'le bulundu (aşağıda); görsel turun üstten
karelerindeki düz şerit (koşu 1 ve koşu 2'de aynı yerde) bu düzeltmeden sonra da duruyor, yani bu hatadan değil — nedeni
incelenmedi (arazi olabilir; rapor AÇIK).
`DonmaIsleyici.kiyiUzakliklari` artık bölgenin çevresinde `kiyiUzaklikSiniri` blok hale okur: sınıra kadarki her yol hale
içinde kaldığından sonuç gerçek uzaklıktır ve chunk kesiminden bağımsızdır (ticking chunk'ın komşuları yüklüdür; yüklü
olmayan hücre kaynak sayılmaz). Maliyet chunk başına 48×48 yüzey okuma, 16 hücrede bir chunk araması; bütçede 256 birim
(bir chunk taraması), yalnız eşik atlanmayan ilk su sütununda. Kanıt: GameTest `kiyiUzakligiChunkSinirindanBagimsiz`
(32×32 sahne, chunk sınırını her zaman kesen yarıçap 12 göl; zamanlayıcı G=35'te her su sütunu sahne geneli gerçek
uzaklığa göre beklenen evrede): eski kodda kırmızı (58 sütun, ör. `c=1` sütun ince buzda kalmış;
`kanit/donma/gametest-4-kirmizi.log`), düzeltmeyle yeşil.

**K-D21 Vitrin dizisi (şef kararı).** → Şef kararları (Faz 2b).

**K-D22 1 blok derin su donar (şef kararı D1, P1).** → Şef kararları (Faz 2b).

**K-D23 İlk kar 15 (şef kararı D2, P1).** → Şef kararları (Faz 2b).

**K-D24 Geri dolma kaynak suda durmaz (D3).** Yukarıda K-D14. Zamanlayıcı yolunda sütunlar karışık sırayla ve birkaç
tick'e yayılarak dolar; erken dolan sütunun kaynağı komşu kuru sütuna akıcı su yayar, vanilla bunu kaynağa çevirebilir.
GameTest `buharlasmaCokTickliGeriDolmaBaslangicVerir` (chunk sınırını kesen göl, gerçek kuyruk, 6 → 0, 40 tick sonra
başlangıç hash'i).

**K-D25 Yapı çatılarına kar (şef kararı D8).** → Şef kararları (Faz 2b).

**K-D26 Simülasyon dışı görünen chunk'lar da donar (D5).** `isPositionTicking` simülasyon mesafesini de istiyordu. Ölçüt
artık "bilet seviyesi ≥ BLOCK_TICKING ve 8 komşu tam yüklü" (`DonmaZamanlayici.islenebilir`): kenar sütunundaki şekil
güncellemesi komşuyu senkron yüklemez (spike K-D2 tuzağı), görüş mesafesindeki dış halka da işlenir. ChunkHolder ticking
future'ı protected olduğu için kullanılmadı; future tamamlanmadan yapılan değişiklik istemciye giden ilk chunk paketinde yer
alır. Öncelik en yakın oyuncuya uzaklıkla devam eder (dış halka sona kalır). GameTest `simulasyonDisiGorunenChunkDonar`
(tur 3, D2-3): oyuncunun görüş bileti gibi yalnız yükleme bileti (`DistanceManager.addTicket`, seviye 31; ticking izleyicisine
girmez) 40 chunk ötedeki chunk'ı yükler; `isPositionTicking` false iken kuyruk onu G 60'a (konulan çimen kırağılanır) ve 0'a
(çimen geri gelir) yetiştirir. Test tarifi (K-E21): bilet + hedefle 8 komşusunun eşzamanlı `level.getChunk` ile üretimi;
üretim tick zaman aşımına girmez.

**K-D27 Vitrin karesinde sohbet yok (D6).** Otomasyon kurulumda `hideGui` açar ve sohbeti yalnız istemcide
`chatVisibility = HIDDEN` yapar (sunucuya bildirilmez: komut çıktıları yine gelir ve log'a düşer, çizilmez); çıkışta FULL.

**K-D28 Kış ağırlığının yüksekliği yapraksız zeminden (D7).** `DonmaIsleyici.zeminY` ve `DonmaApi.yuzeyY`
`MOTION_BLOCKING_NO_LEAVES` okur: ağaç tepesi yanındaki zeminden "daha kışlı" olmaz. K0-24'ün 0,05 komşu sınırı aynı zemin
eğimindeki komşular içindir; uçurumda (komşu zemin farkı ≥ 2 blok) ağırlık farkı en çok 0,047 × fark (bilinen sınır, spike
K-D4 "uçurum: doğal sınır"). GameTest `kisAgirligiYaprakSayilmadanZemindenOkunur` (tur 3, D2-3): çimen sütununa Y 130'a kadar
kalıcı yaprak; `MOTION_BLOCKING` > 120 iken `yuzeyY` ve `zeminY` çimen bloğunun Y'si (`LevelChunk.getHeight` en üst bloğu
döner, `Level.getHeight`'in bir altı), yanındaki çıplak çimenle aynı; kış ağırlığı farkı ≤ 0,05 (yaprak tepesi okunsaydı ağırlık
1 olurdu).

**K-D29 Kırağılı çimen oyuncuya geçmez (D9).** Loot ipek dokunuşla `grass_block`, değilse toprak verir (vanilla karın
çimende bıraktığı gibi); blok yalnız donmayla oluşur. Bilinen sınır: yaratıcı modda eşya menüsünden alınıp konursa
çözülmede çimene döner (yaratıcı oyuncunun dekoru).

**K-D30 İnceleme notları.** (1) `kalici` doğuştan sonra kapatılırsa çözülme dönemi `DonmaSureci.tick`'te açılır. (2) Kızıl
cüce başında ılık sütunlar ofset kadar (en çok 10 puan) tek adımda iner; 1–2 kar katmanı doğuşla kalkar — kabul (madde 9
"önce ılık bölgeler" ile uyumlu, küçük). (3) Kayıt sırası (`DonmaSistemi.kaydet` / `GokyuzuSistemi.kaydet`) artık
önemsiz: donma süreçleri `asama ileri`'ye yanıt vermez (K-D31). (4) Entegratöre: K-D15 yağmur düzeyini 0'a çektiği için gökyüzü
dalının `1 − rainLevel` alfası kar yağarken yıldızları tam gösterir; birleşince göz kontrolü.

**K-D31 Donma ve buharlaşma süreçlerinin ayrık aşaması yok (şef kararı, doğrulayıcı 6b, P1).** → Şef kararları (Faz 2b).

**K-D32 N15 grep istisnası: `*VeriSaglayicilari` datagen sınıfları (doğrulayıcı 7).** `donma.veri.DonmaVeriSaglayicilari`
Forge `net.minecraftforge.client.model.generators.*` (`BlockStateProvider`, `ModelFile`) kullanır: bunlar Forge'un datagen
sınıflarıdır, istemci kodu değil. Sınıf yalnız `GatherDataEvent` yolundan yüklenir: `DonmaSistemi.kaydet`
`DataGenerators.saglayiciEkle(olay -> DonmaVeriSaglayicilari.ekle(olay))` kaydeder; `invokestatic` lambda gövdesindedir
(`lambda$kaydet$N`), sınıf ancak gövde ilk çalıştığında (yalnız `runData`) çözülür; doğrulayıcı aynı parametre tipinde
(`GatherDataEvent`) atanabilirlik için yüklemez; `import` satırı bytecode'a girmez. Çekirdeğin `veri.DataGenerators`'ı
istemci üretici kullanmaz (çeviri/tarif katkıları + `saglayiciEkle` kancısı); aynı kanca deseni kullanıldı. Çalışma zamanı
kanıtı: dedicated `runServer` `JAVA_TOOL_OPTIONS=-Xlog:class+load` ile, sunucu JVM'i 21 292 sınıf yükledi, `DonmaSistemi`,
lambdaları, `DataGenerators` ve `DataGenerators$SaglayiciEkleyici` var, `DonmaVeriSaglayicilari` ve `client.model.generators`
**0** satır; `invalid dist` 0 (`kanit/donma/n15-verbose-class.log`, `runServer-verbose-class.log`). Entegratöre: N15 grep'i
`*/veri/*VeriSaglayicilari.java` dosyalarını bu koşulla (yalnız `saglayiciEkle` lambdasından referans) istisna sayın.

**K-D33 Kar yağışı 30 (şef kararı, inceleme-2 D2-1).** → Şef kararları (Faz 2b).

**K-D34 Yaprak altındaki su donmaz (bilinen sınır, şef kararı).** → Şef kararları (Faz 2b).

**K-D35 Süre tavanı kurulum ve BFS'i kapsıyor (inceleme-2 D2-2).** Süre artık tick'in başından (sıra yenileme dahil)
ölçülür ve her yeni chunk işi kurulmadan (kış alanı/biyom örnekleri) ve her sütundan önce denetlenir (önceden 4 sütunda
bir ve yalnız sütun döngüsünde); kıyı BFS'i bir sütunun içinde başladığı için de denetimden sonra gelir. Tek tick tavanı en
çok bir kurulum ya da bir sütun + BFS kadar aşar; aşım sonraki tick'ten düşülür. BFS birimi (256) sütundan önce ayrılır
(`YetismeButcesi.sutunRezervi(..., bfsBekliyor)`; önceden sonradan düşülüyor, birim eksiye inebiliyordu). Eşik kümesi
`ServerStartedEvent`'te kurulur (ilk işçi tick'ine girmez). 4 ms bir ortalama hedefidir, kesin üst sınır değil (soğuk JIT ve GC
duvar saatine girer). `donma bilgi` yeni satır: "En uzun işçi tick'i: ısınmadan sonra X ms (ısınma penceresi Y ms); süre
tavanı 4 ms". Isınma = istatistik sıfırlanınca ilk 100 **iş yapan** (sütun işleyen) işçi tick'i (tur 4, inceleme-3 D3-1: boş
tick'ler pencereyi dolduruyor, ilk gerçek iş — görsel koşu 5'te 102,47 ms — "ısınmadan sonra" sayılıyordu). JUnit
`YetismeButcesiTest` (BFS'li rezerv, 2000 tick × 3 kısmada birim eksiye düşmez ve blok sınırı aşılmaz, süre denetimi ve
aşım düşümü). Tur 3'ün "9,69 ms GC duraklamasıyla çakışmıyor" dayanağı `kanit/donma/gc-tur3/` (`-Xlog:gc`, sunucu
JVM'i `gc-227853.log`), tur 4'ünkü `gc-tur4/`.

**K-D36 Buharlaşma GameTest'i sahnenin sıvı tick'lerini bekliyordu (tur 3; tur 4'te K-D37 yerini aldı).**
`buharlasmaCokTickliGeriDolmaBaslangicVerir` tur 3'te iki koşuda kırmızı (`gametest-9/10.log`), tanı koşusu
(`gametest-11-tani.log`): 9 chunk'ın hepsi `buhar 6`, süren iş 0, ama göl ortası üç kat kaynak su. Test 20 tick bekleyip
buharlaşmayı başlatıyordu ve yarış bilinen sınır yazılmıştı. Şef kararıyla düzeltildi (K-D37); bekleme kalktı.

**K-D37 Kuruyan sütuna komşu sıvı tick'i akmaz (şef kararı, inceleme-3 D3-2).** → Şef kararları (Faz 2b).

**K-D38 `buharlasma_gol_yatagi` karesi sönük kalır (şef kararı, inceleme-3 görsel).** → Şef kararları (Faz 2b).

### Reddedilen yollar

- **Yağmur → kar için `Biome.warmEnoughToRain` mixin'i:** çalışır ama mixin altyapısı (`build.gradle`, mixins.json) bu dalda
  yok ve sanal-su dalıyla birleştirme çakışması yaratır; event yolu (yağmur düzeyi) yeterli. **`DimensionSpecialEffects.renderSnowAndRain`:**
  gökyüzü işçisi üst dünyanın efekt nesnesini sarıyor (K0-26); ikinci sarıcı zinciri karmaşıklaştırır. **Sunucuda havayı
  değiştirmek:** oyun durumunu (yağmur, kar birikimi, mob) değiştirir.
- **`sifirla`'da blokları bırakmak (ilk sürüm, K-D11 eski):** sıfırlanan dünyada kalan buz/kar bir daha hiç çözülmezdi;
  test aracı alanı kirletiyordu (inceleme D4, şef kararı).
- **Buharlaşan suyu orijinal blok olarak saklamak:** su dışı blok kurumaz; yalnız Y ve sayı yeter.
- **Buharlaşmayı normal bayrakla (şekil güncellemeli) yapmak:** komşu kaynaklar deliğe akıcı su akıttı (göl yüzeyi yeniden
  doldu); geri dolmada da aynı.
- **G'yi 5'lik adımlarla yayınlamak (B7 alternatifi):** çekirdek alanı değişirdi; eşik aralığı atlaması aynı kazancı verir.
- **Biyom rengi tonlaması için vanilla renk işleyicisini sarmak** ve **kayrağı örtüsü/sarkit decal'ı:** bu turda yapılmadı
  (rapor AÇIK); tonlama başka modla çakışma riski taşır, decal ayrı render katmanı ister.

## Faz 2b — Sanal su

Parça `sanal-su` (dal `is/sanal-su`, son commit `ae4d133`; `main`'e birleştirme `3e896f6`). Kaynak `.vibe/kararlar/sanal-su.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) o dosyada kalır; metindeki §A/§1… göndermeleri o dosyaya aittir.
Şef kararları (SU-31, SU-32, SU-38, SU-40) "Şef kararları (Faz 2b)" bölümündedir; N17 satırları sondaki tablodadır. Rapor
`.vibe/rapor/sanal-su.md`.

**SU-11 Deniz alanı su kütlesi başına taşkın (SU-10 B3 kapandı).** Spike'ın "kaynak + artış" BFS'i göl kenarında ve
kıyı çukurlarında havada asılı su duvarları üretiyordu (B3) ve 96 adım sınırında ovada dikey duvar bırakıyordu. Yeni hesap:
kütle = aynı yüzeydeki bitişik durgun (kaynak) su sütunları; her kütleden kova kuyruklu öncelikli taşkın her sütuna
`eşik` yazar (kütleden sütuna giden en alçak yolun en yüksek zemini). Kütle `a` kadar yükselince seviyesi
`min(yüzey + a, tavan)`; eşiği seviyenin altındaki sütunlar `[zemin, seviye)` dolar. Deniz kütlesi (yüzey ≤
`getSeaLevel()`) sınırsız; göl kütlesi taşma ağzında durur (taşkın yüzeyin 1 bloktan fazla altındaki sütuna değerse tavan
o anki eşik). Birden çok kütle aynı sütunu doldurursa tam yükselmede yüksek seviye kazanır. Sonuç: ıslak sütunun kuru
komşusunun zemini her artışta su yüzeyinin üstünde (rastgele arazi JUnit'i); kumul arkasındaki çukur ancak deniz kumulu
aşınca dolar; mesafe sınırı yok (yüklü alan kadar). Akan su kaynak sayılmaz. Maliyet: tek iplikli arka plan, kütle başına
en çok 8 kova. Kanıt: `DenizAlaniTest` (11 test).

**SU-12 Deniz artışı çekirdek döneminde, adım `DenizAdimi` ile.** Artış `DunyaDonemi.deniz()`'de kalıcı ve senkron (çekirdek
§6); sürecin hızı `ekVeri("su").hiz` (blok/ölçekli tick) — bir kez hesaplanır, böylece `/dogalafet hiz` ve yeniden
başlatma yükselmeyi doğrusal tutar. Bekleme "gerçek dakika" olarak `dakika × 1200` ölçekli tick'tir: TPS düşükse ve
oyun duraklatılmışken (tek oyunculu) bekleme de uzar; bu, `stop`/kural duraklatmasıyla tutarlı tek saat seçimidir.

**SU-13 Sunucu durumu boyut başına (SU-10 notu kapandı).** `SanalSu.BOYUTLAR` (`ResourceKey<Level>` → durum); deniz artışı
yalnız Overworld'e uygulanır (N11), dalga ve mağara her boyutta çalışır. İstemci kopyası seviye değişince yenilenir,
çıkışta silinir. ~~Dalga, mağara ve çekilme diskte tutulmaz~~ — tur 2'de SU-24 ile değişti: parametreleri dönemin
`ekVeri("su")` etiketinde kalıcı.

**SU-14 Mağara yaşamı dönem süreci.** `magara_suyu` süreci (`afetTuru = TSUNAMI`): `stop` → hemen kaldır (geçici etki,
§3a); `KURAL` → seviye o anda donar (`Magara.donma`), `devamEt` → çizelge duraklama kadar kayar (`MagaraZamani.kaydir`);
`asama ileri` → `MagaraZamani.ileri(t)` (dolu bekleme → kuruma → bitiş, N18 "mağara suyunun buharlaşmasını beklemeden").
Zaman çizelgesi deterministik olduğu için sürecin tick'i yoktur; istemci aynı çizelgeyi pakette alır.

**SU-15 Mağara kuruması.** Varsayılan kalış 600 sn, kuruma 0,25 blok / 6 sn (tasarım notu); seviye kademeli iner, buhar
parçacığı yüzey alanının %4'ü × kalite çarpanı (Düşük 0,4 … Ultra 1,5) × `su.parcacikYogunlugu`, kare başına en çok 40.
Bitince cep silinir; cep kutusu (+2 blok) başlangıç ve bitiş hash'i log'a yazılır ("eşit=true").

**SU-16 Sürükleme ayrıntısı.** SU-03'teki deterministik itme korunur; ek olarak: oyuncu girdisinin kısmi kaybı (yatay hız
×0,88/tick, istemci), su gölgesi (dalgaya bakan tarafta 3 sıra tam blok → ×0,6), dalga hasarı ve çarpma hasarı sunucuda
(`SanalSuSunucu.dalgaEtkileri`, tsunami.md §2). Eşya (`ItemEntity`) mixin üzerinden vanilla su hareketiyle birlikte itilir.

**SU-17 Render kalite bağları (N13).** Dalgacık bileşeni Düşük 1 / Orta 2 / Yüksek 3 / Ultra 4; eğim köpüğü Düşük'te kapalı;
uzak LOD mesafesi `min(görüş, Düşük 8 / Orta 12 / üst kalite sınırsız, su.lodMesafeChunk)`; kıyı şeridinde (derinlik
< 0,8) 2 sn periyotla "yalayan" köpük bandı; derinliğe göre alfa (sığda 0,25, derinde 0,85).

**SU-18 Paket kodlaması (SU-10 B9 kısmen).** Mağara sütun aralıkları varint + zigzag delta (sütunlar sıralı, x/z farkı;
y'ler önceki y'ye göre). Cep kimlikli artımlı paket ve yakın oyuncu süzgeci yapılmadı: arkadaş ölçeğinde (≤ 10 oyuncu, birkaç
cep) tam durum yeterli.

**SU-19 Görsel kanıt otomasyonu.** `-Potomasyon=su` ile `client.su.SuOtomasyon` (spike otomasyonu taşınmadı, yeniden yazıldı):
sahneler komutla kurulur, FPS ortanca kare süresiyle ölçülür, bütün girdi olayları log'lanır (K0-21 rev. 2 (e)). Koşu
betiği `tools/senaryo/su_istemci.py` protokolü uygular (exec kuralı, belirteç kapısı, pencere doğrulaması, 5 sn bekçi).

**SU-20 Deniz alanı okumaları tick'lere yayılır, oyuncu sayısıyla ölçeklenir (K0-30 Faz 2 notu kapandı).** Spike 10 tick'te
bir 32 chunk'ı tek tick'te okuyordu (1–11 ms sıçrama). Şimdi plan ve istek kümesi 10 tick'te bir güncellenir, okuma her
tick `TazelemePlani.tickButcesi(onTick, oyuncu, kısma) = ceil(onTick · max(1, ceil(oyuncu/2)) · kısma / 10)` chunk (en az
1): tek oyuncuda tick başına 4, 10 oyuncuda 16; TPS kısması (`TpsOlcer.kismaCarpani()`) çarpar. Her oyuncunun yakın
halkası (±2 chunk, 25 chunk/sn) böylece çok oyuncuda da her saniye okunur. İstemci aynı biçimde (tek odak). Kanıt:
`TazelemePlaniTest.okumaButcesiTicklereYayilirVeOyuncuyaGoreOlceklenir`, GameTest `denizAlaniBlokDegisiminiYakalar`
(bayrak-2 9 tick, bayrak-3 1 tick, `chunkDegisti` 1 tick; sınır 40).

**SU-21 Güneş yansıması (tasarım notu "parlama").** Şader'siz: köşe rengine sahte speküler karışımı; dalgacık normali
ile göz–güneş yarım vektörünün açısı ^48, güneş yönü `getSunAngle` ile gökyüzü çiziminin dönüşü ((−sin θ, cos θ, 0)),
ufukta söner, yağmurda yok; gün batımında renk turuncuya kayar; yansıyan sütunun blok ışığı yükselir (akşamı da parlak).
Uzak LOD karolarında normal sapması ×2,5 (şerit dağılır). Vitrin için `/dogalafet su kiyi` karası batıda olan, yaprak
örtüsüz kumsalı tercih eder (kamera batıya, gün batımına bakar).

**SU-22 Koşarak yüzme üçüncü enjeksiyon istemedi.** Forge 47.4.28 `Entity.updateSwimming` yüzmeye başlamayı
`isUnderWater() || canStartSwimming()` ile, sürdürmeyi `isInWater() || isInFluidType(...)` ile karar verir (userdev
`Entity.java.patch`); EntityMixin `wasTouchingWater` ve göz sıvısını doldurduğu için ikisi de sanal suyu görür. Kanıt:
GameTest `sahteOyuncuSanalSudaHavaKaybederBogulur` (koşan oyuncu `isSwimming` ve `Pose.SWIMMING`).

**SU-23 İstemcide yerel oyuncu ölçümü (§6 #15).** Otomasyon (`o:` adımı) yerel oyuncunun `isInWater`, `isUnderWater`,
hava, can, poz, `SanalSu.batiklikOrani/icindeMi` ve dalga yönündeki kaymayı log'lar: koşu 5'te kumsalda deniz +4 altında
hava 204 → 102, test dalgasında 18,3 blok kayma, mağarada su altında hava 199 (rapor). İlk iki deneme geçersizdi (oyuncu
gercek denizin dibine battı; test dalgasının tabanı gerçek su yüzeyinin altındaydı): test dalgasının tabanı artık
`min(deniz seviyesi, MOTION_BLOCKING) − 1`.

### Tur 2 (inceleme S-P2-1…3, doğrulama madde 4, görsel değerlendirme 2b)

**SU-24 Sanal su kalıcı (S-P2-2, madde 3).** `su.SuKalicilik`: dalga, çekilme ve mağara cebi **parametreleri** dönemin
`ekVeri("su")` etiketinde (`dalgalar`, `cekilmeler`, `magaralar`; tür başına en çok 40 kayıt, en yenileri) durur, sunucu
açılınca yeniden kurulur. Mağara hücreleri saklanmaz: cebin en alçak hücresi, yüzeyi, hacmi, kutusu ve zamanları
(ölçekli afet saati, kalıcı ve monoton) saklanır; hücreler `MagaraDolumu.seviyeyeKadar` ile aynı dünyadan yeniden hesaplanır
(kayıt başına ~0,3 KB; GameTest'te iki kayıt 380 karakter). Dünya değişmişse (tohum boş değil) cep boş döner, su
kurulmaz. Mağara kaldığı yerden kurumaya devam eder. Test: `magaraVeDalgaKayittanYenidenKurulur` (GameTest), `MagaraDolumuTest`
`seviyeyeKadarDolumDoldurmaylaAyniCebiKurar` / `seviyeyeKadarDolumSiniriAsarsaBos`.

**SU-25 Buharlaşma inişi durdurmaya uyar (S-P2-1). — Tur 3'te SU-32 ile geri alındı.** `DenizYukselmesiSureci`'nin buharlaşma dalı da `surecDurdu`'ya bakar:
`/dogalafet stop` sonrası deniz değişmez; süreç sürdürülünce iniş devam eder. GameTest: durmuş süreçte artış 2,5'te kalır,
sürdürülünce 0'a iner.

**SU-26 Yakın halka kurulumu tick bütçesiyle (S-P2-3).** Halka yapımı üç aşamalı (sütun sorgusu, köşeler, dörtgenler),
satır satır ilerler ve tick başına `su.yakinKurulumButcesiMs` (CLIENT, 2 ms) kadar çalışır; kalanı sonraki tick'e kalır,
eski tampon o sürede çizilmeye devam eder. Arka plan thread'i reddedildi (§D). Koşu 12: deniz +4 kurulumu toplam 9,8 ms
5,8 tick'e yayıldı, tick başına en çok 2,5 ms; dalga 3,7 ms / 2,7 tick / 2,4 ms; mağara 3,0 ms / 2,4 tick / 5,0 ms (bütçe
satır başında denetlenir, tek satır aşabilir). Yüklü makinede (koşu 11) tek tick 13,4 ms görüldü.

**SU-27 Dalga aralığının tabanı alan okumadan önce dünyadan (doğrulama madde 4, P1).** Doğrulayıcının iki koşusunda
`dalgaMobuVeEsyayiSurukler` düştü (`domuz suyu tanımadı`): domuz şablon kenarından düşüp dalga tabanının altına girmiş,
alan o sütunu henüz okumadığı için aralık dalga tabanından başlıyordu. Çözüm: alanın okumadığı sütunda
`SanalSuDunyasi.zeminYedek` → `SanalSu.yedekZemin(level)` (yaprak hariç `MOTION_BLOCKING` yüzeyi; chunk yüklü değilse
bilinmiyor), sunucu ve istemci aynı kural. Alan okuma istekleri en yakın odağa (oyuncu, sabit bölge) Chebyshev
uzaklığına göre sıralanır (`TazelemePlani.yakindanUzaga`). Test belirlenimci kılındı: domuz dalga tabanının altındaki
hendekte, her koşul gerçekleştiği ilk tick'te kilitlenir. Üç ardışık GameTest koşusu + son koşu: `domuz suda=true`,
`All 20 required tests passed`.

**SU-28 Şader'siz su görünümü (görsel değerlendirme 2b: beş kare sönüktü).** Köşe başına gölgeleme: dağınık ışık
(normal · güneş), Fresnel (Schlick) ile ufuk/gök rengi yansıması ve alfa artışı, güneş parıltısı; çok oktavlı yüzey eğimi
(köşe ızgarası). Kendi köpük dokumuz `textures/block/su_kopuk.png` (16 kare, döşenebilir; üretim
`tools/assets/su_kopuk.py`, N19): kıyı şeridi, dalga tepesi bandı ve kırılan dudak (LOD'da da tepe köpüğü). Dalga tepesi
boyunca serpinti ve sıçrama parçacıkları (kalite çarpanlı), çekilen suyun ardında ıslak dip. Su altı: yüzeyden inen ışık
demetleri, sis rengi biyom su sisi %30 + su rengi %70, kamera noktasının ışığıyla ölçekli (gündüz sığ su okunur, gece/mağara
karanlık), sis uzaklığı 56 blok. Mağara suyu: dalgacığın %55'i yükseklikte, tavan ışığı parlaması, duvara değen kenarda ince
parlak ıslak şerit. Ölçü (koşu 12): su CPU'su kare başına 0,26–0,48 ms, çizim 0,03–0,05 ms.

**SU-29 Vitrin sahnesi komutla (tasarım notu §3).** `SuVitrin` bulunan kumsala denize uzanan ahşap iskele ve kıyı köyü
(8 ev, fenerler) kurar, kamerayı hazır açılara taşır (iskele ucu, su altı, dalga için evlerin arkasından alçak), vitrin
dalgasını çekilmeyle başlatır. Belirlenimci (aynı dünya → aynı sahne). Gerçek blok koyar: yalnız test/kanıt komutu,
sanal su sisteminin parçası değil. Mağara sahnesi istemci otomasyonunda (`SuOtomasyon.magara`: damlataş tavan, dokuz
ışıltaşı yaması, sarkıtlar, asılı fenerler, iki duvarda deniz fenerleri).

**SU-30 `yakindanUzaga` paylaşılan durumsuz (koşu 8 çökmesi).** Tek oyunculuda sunucu ve istemci thread'i sıralamayı aynı
anda çağırıyordu; ortak statik `HashMap` ile karşılaştırıcı `TimSort` içinde NPE verdi ve entegre sunucuyu düşürdü. Artık
yerel diziler: üst bitler uzaklık, alt bitler sıralı anahtarın sırası, tek `long[]` sıralaması (ayrıca nesne
ayrılmaz). Test: dört thread'den 400 eş zamanlı çağrı aynı sonucu verir.

### Tur 3 (inceleme-2 X2-1…X2-8, doğrulama-2 madde 4)

**SU-31 Gerçek blok koyan geliştirme komutları üretimde yok (X2-1, P1).** → Şef kararları (Faz 2b).

**SU-32 Buharlaşma inişi durdurmadan bağımsız (X2-6, şef kararı; SU-25'in yerine).** → Şef kararları (Faz 2b).

**SU-33 Kurulum bütçesi borçlu ve parçalı (X2-2).** `su.KurulumButcesi` (saf mantık, JUnit 5 test): bir tick'te bütçeyi aşan
süre sonraki tick'lerin bütçesinden düşülür (çekirdek donma `YetismeButcesi` deseni), uzun sürede ortalama bütçeyi geçmez.
Yapım satır yerine 16 sütünluk parçalarla ilerler, bütçe her parçadan sonra denetlenir. `kurulum ms` log'u gerçek işi
(borç hariç) yazar. Koşu 14 ölçüm pencerelerinde tick başına en çok 2,10 / 2,03 / 2,03 ms (deniz / dalga / mağara; tur 2
mağarada 5,0 ms). 10 sn'lik log'da kamera ışınlandıktan hemen sonra 4,8–7,1 ms tepe görüldü (soğuk ışık/renk önbelleği;
tek parça uzun sürdü), aşım sonraki tick'lerden düşülür.

**SU-34 Kalıcılık tavanı, kirli yazım ve afet numarası (X2-3, X2-4).** §A2 "Kalıcılık ve kimlik kuralı". Tavan bütün
boyutlar için tür başına 4/4/8 (eskisi düşer), uyarı bir kez; yazım `yayinla`'da değil sunucu tick sonunda kirli bayrakla;
`sonKimlik` kalıcı (yeni kimlik kayıtlı kimlikle çakışmaz); `dalgaBaslat(…, afetNo)` tekil. GameTest kalıcılık: ekVeri 392
karakter.

**SU-35 Mağara suyu tavan yansıması ve sıcak su altı (X2-7, X2-8).** Şader'siz: köşeden kameraya bakış dalgacık normaline
göre yansıtılır, ışın yarım blok adımlarla en çok 12 blok izlenir; ışık kaynağına (ışıltaşı, fener) vuran ışın keskin sıcak
parlama, başka bloka vuran o noktanın blok ışığıyla amber; Fresnel ile yatık bakışta güçlenir; köşe başına yapım başına
bir kez. Mağara dalgacığı %55 → %90 yükseklik, eğim ×1,8. Işık demetleri enine üç bant (dış köşeler alfa 0), aşağı %60
genişler, genişlik yavaşça dalgalanır; gökyüzü ışığı düşükse renk sıcak; sayı Orta 10 / Yüksek 18 / Ultra 26. Su altı sisi
gök görmeyen ama blok ışığı alan suda amber tona kayar. `ciz` sonunda harmanlama kapatılır; seviye değişiminde eski tamponlar
çizilmez.

**SU-36 Dalga tabanı önce canlı zeminden (doğrulama-2 madde 4).** Kök neden: dalga aralığının tabanı deniz alanının okuduğu
zemindendi ve alan zemini **bayat** olabilir (kodla değişen blok olay atmaz, tazeleme periyodik). Doğrulayıcının düşen koşusunda
alan GameTest hendeği kazılmadan önce sütunu okumuştu: taban -59, domuz dipte -60 (geçen koşularda domuz zıplayıp -58,57'de
kalıyordu). SU-27'deki yedek yol yalnız "alan hiç bilmiyor" durumunu kapsıyordu. Çözüm üretim yolunda: `SanalSuDunyasi.canliZemin`
(önceki `zeminYedek`) öncelikli; dünyanın yükseklik haritası (yaprak hariç `MOTION_BLOCKING`, blok değişince hemen güncel),
chunk yüklü değilse alanın zemini, o da yoksa dalga tabanı. Sunucu ve istemci aynı kural. Test: JUnit
`dalgaTabaniAlaninEskiZemininiDegilCanliZeminiKullanir` (kırmızı: 64 ≠ 61); GameTest belirlenimci: alan taş zemini okuduktan
sonra okuması dondurulur, iki blok derin hendek kodla kazılır (domuz çıkamaz, dipte kalır), bütün koşullar aynı tick'te; eski
öncelikle kırmızı (`domuz suyu tanımadı (y=-60.0, dalga tabanı -59, alanın bayat zemini -59)`), düzeltmeyle 5 + 2 ardışık koşu yeşil.

**SU-37 Uzak LOD'da dalga cephesi (koşu 14 dalga karesi boştu).** Yakın halkanın (±40) dışındaki dalga yalnız yatay 4×4
karolarla çiziliyordu; önden bakınca duvar görünmüyordu (öncesi/sonrası farkı %3,3). Dalga karolarında komşu karodan 1 bloktan
fazla yüksek kenara dikey etek eklenir (yalnız dalga karolarında komşu sorgusu). Faz 4 tsunamisinin "ufukta yükselen duvar"
görünümü de buna dayanır. Vitrin kamerası iskele ekseninde, ilk ev sırasının arasından alçak (deniz + 5,5); kare dalga kıyıya
~8 blok kala.

### Tur 4 (inceleme-3 S3-1…S3-3)

**SU-38 Uzak LOD kurulumu da bütçeli (S3-1).** → Şef kararları (Faz 2b).

**SU-39 Dalga testi eşiği fizikten (S3-2).** Sabit `dx > 3` yerine: domuzun ilk ıslandığı konumdan kayması >
`max(1, 0,6 · beklenen(n))`, `n` suda geçen tick. Beklenen: her tick hıza `ivme` eklenir, `travel` o hızla yürütür, vanilla su
sürtünmesi 0,8 ile çarpar, dalga hızını geçmez (`SuOyunTestleri.beklenenSurukleme`). Model ölçümle tıpatıp (7 tick: beklenen
1,24763, ölçülen 1,24763; 8 tick 1,51810 = 1,51810; 9 tick 1,79948 = 1,79948). Islanmadan önceki gezinme sayılmaz (bir koşuda
toplam kayma 0,98, ıslakken 1,80). Kullanılan değerler log'da (ivme, sürtünme, hız sınırı, suda tick, beklenen, eşik). Domuzun
yapay zekâsı açık: `setNoAi(true)` denendi, NoAI mob `travel` etmediği için itilmedi ve test düştü (`tur4/`).

**SU-40 Mağara görseli: SU-35 kod yolu ve Faz 4 yeniden puanlama (S3-3, şef kararı).** → Şef kararları (Faz 2b).

### Reddedilen yollar

- **Kaynak + artış tek BFS (spike SU-06):** göl yamaçlarında ve 96 adım sınırında havada su duvarları (B3); SU-11.
- **Deniz artışını kendi SavedData'mızda tutmak:** çekirdek `DunyaDonemi.deniz()` zaten kalıcı ve senkron.
- **Mağara hücrelerini diske yazmak:** 4000 hücrelik cep ~30 KB, `ekVeri` önerisi ≤ 16 KB; parametre saklayıp dünyadan yeniden
  hesaplamak yeter (SU-24). (Tur 1'deki "hiç saklama" kararı S-P2-2 ile geri alındı.)
- **Yakın halka kurulumunu arka plan thread'ine almak (S-P2-3 önerisi):** sütun sorgusu istemci seviyesini ve deniz alanını
okur (paylaşılan geçici nesneler, §A.2); kopyalama maliyeti kazancı yer. Tick bütçesi yeterli (SU-26).
- **Testte domuzu hendekten yukarı koymak / koşulları ayrı tick'lerde kilitlemek (SU-36):** kararlılığı testten alır, üretimdeki
  bayat zemin hatasını gizlerdi.
- **Geliştirme komutları için ikinci bir `su` alt komutu (SU-31):** çekirdek aynı adlı iki alt komutu reddeder; aynı düğüme eklenir.
- **Buharlaşma inişinin durdurmaya uyması (SU-25):** N10 sırasını kural ayrımında bozuyordu (SU-32).
- **Su altı sisini biyom su sisi rengiyle bırakmak:** çoğu biyomda neredeyse siyah lacivert, kare okunmuyordu (SU-28).
- **Gerçek su bloğu koymak, blok olayı göndermek:** madde 8 yasaklar.
- **Olay yolu fizik (`LivingBreatheEvent` + elle ivme):** SU-03, K0-25.
- **Spike otomasyonu ve `spike su` komutunu taşımak:** spike'a özgü; doğrulama komutları `su` altında yeniden yazıldı.
- **Cep kimlikli artımlı mağara paketi:** SU-18.
- **Koşarak yüzme için `Entity.updateSwimming` mixin'i:** gerekmedi (SU-22).
- **Okumaları 10 tick'te bir toplu yapmak (spike):** tek tick'te 1–11 ms sıçrama; SU-20.

## Faz 2b — Entegrasyon

Entegratör oturumu `entegrator`, altı Faz 2b dalının `main`'e birleştirilmesi (`a844da5` … `3e896f6`) ve sonrası (`d62d796`,
`fe4a844`, `96b8f8a`, `6ecafb9`, `d303643`). Rapor `.vibe/rapor/entegrasyon-2b.md`, kanıtlar `.vibe/kanit/entegrasyon-2b/`.

**K-E1 Kancalar tek sınıfta, mod kurucusunda.** `io.github.kadirberkpolat1.dogalafetler.Kancalar.bagla()` alt sistem
kayıtlarından sonra, `AyarKaydi.kaydet`'ten önce bir kez çağrılır ve yalnız bir satırlık adaptörler içerir (mantık sahiplerdedir):
yangın → sıcaklık `YanginKancalari.isiKaynagiAyarla(SicaklikKancalari::ortamKaynagiEkle)`; yangın → sanal su `sanalSuAyarla`
(`SanalSu.suSeviyesi` bloğun Y'sinden yüksekse su altı); donma → sanal su `DisKancalar.ayarla` (`SanalSu.chunkDegisti`,
`suSeviyesi`, `DenizYukselmesiSureci.buharlasmaPayi`); donma → sıcaklık `kisBolgesiSaglayiciAyarla(DonmaApi::kisAgirligi)`; donma →
gökyüzü `KizilCuceBilgiKatkisi.ekle(DonmaApi::kisBolgesiSatirlari)`. Sanal su → sıcaklık ıslaklığı için bağ yoktur: sıcaklık
`isInWater()` / `isInWaterRainOrBubble()` okur, sanal su mixin'i o alanları doldurur (sanal su §A2). Sınıf ortak koddur,
istemci sınıfı import etmez. Kurucu sırası: çekirdek config'leri, `OyunTestiAfeti`, su, sıcaklık, uyarı, gökyüzü, yangın,
donma, `Kancalar.bagla()`, datagen katkıları, `AyarKaydi.kaydet` (sonda). Kanıt: GameTest
`SicaklikKancaOyunTestleri#yanginIsisiSicaklikKaynaginaGider` (yanan kütüğün yanında sahte oyuncu varken sıcaklığın geçici kaynak
toplamı artar; sıcaklığın kış ağırlığı `DonmaApi.kisAgirligi` ile aynı) ve `KancaOyunTestleri#donmaKancalariSanalSuyaVeGokyuzuneBagli`
(`buharlasmaPayi` sanal sudan gelir, `sanalSuVarMi` = `suSeviyesi` karşılaştırması, `kizilcuce bilgi` donmanın kış satırını içerir).

**K-E2 `build.gradle`: tek `client` bloğu; refmap uyuma göre.** Sıcaklık, uyarı-ses ve sanal-su aynı `client` satırlarını
(`-PkullaniciAdi`, `-PquickPlaySunucu`, `-Potomasyon` + `--width/--height`; uyarı-ses ayrıca `-PoyunTestiAfeti`) eklemişti;
birleştirmede tek kopya kaldı. Sanal su'nun MixinGradle eklentisi, `mixin {}` bloğu, `annotationProcessor` ve Sponge deposu
(`settings.gradle`) ile sıcaklığın `compileOnly` Cold Sweat'i korundu. Çakışma: sanal su `mixin.env.disableRefMap=true`'yu
koşulsuz ekliyordu; sıcaklık `-Puyum != yok` iken uyum modlarının SRG refmap'ini `mixin.env.remapRefMap` ile çeviriyordu (S-17).
Refmap kapalıyken uyum modunun refmap'i hiç okunmaz ve Cold Sweat'in mixin hedefleri kayardı; bu yüzden `disableRefMap` yalnız
`uyum == 'yok'` iken, aksi hâlde S-17 eşlemesi. Bilinen sınır: `-Puyum=coldsweat` ile sanal su mixin'leri de refmap'ten (SRG →
resmî ad çevirisiyle) çözülür; bu birleşim runClient'ta sınanmadı (Faz 5, §6 #9).

**K-E3 Yangın otomasyonu `-Potomasyon=yangin`.** Yangın betik `JAVA_TOOL_OPTIONS=-Ddogalafetler.otomasyon=yangin` kullanıyordu
(yangın §E). `-Potomasyon` aynı `dogalafetler.otomasyon` sistem özelliğini koyduğu ve `YanginOtomasyonu` onu okuduğu için Java
değişikliği gerekmedi; `tools/senaryo/yangin_gorsel.py` `-Potomasyon=yangin`'e geçti. Yan etki: `-Potomasyon` pencereyi 1280×720
yapar; yangın kareleri bundan sonra bu çözünürlüktedir. Sıcaklık ve donma betikleri aynı özelliği `JAVA_TOOL_OPTIONS` ile vermeye devam
eder (davranış aynı).

**K-E4 runClient açan betikler kilitle (K0-21 rev. 2 (k)).** `sicaklik_gorsel.py`, `uyari_istemci.py`, `su_istemci.py`,
`yangin_gorsel.py`, `donma_gorsel.py` koşu betiklerinde yalnız runClient başlatan satır `python3 tools/senaryo/runclient_kilit.py
kos <worktree> --bekle 1800 -- timeout … ./gradlew runClient …` oldu (gökyüzü §A.6, G21, G24). Betikler koşulmadı (runClient yok);
`py_compile` 6/6, kilit sınaması 8/8 (`.vibe/kanit/entegrasyon-2b/runclient-kilit-test.log`).

**K-E5 Datagen: blok etiketi sağlayıcılarına ayrı ad; lang çakışmaları yeniden üretimle.** Yangın `YanginEtiketSaglayici` ve donma
`DonmaVeriSaglayicilari.Etiketler` ikisi de `BlockTagsProvider`; varsayılan adları aynı ("Tags for minecraft:block mod id
dogalafetler"), `DataGenerator.addProvider` ikincisini `Duplicate provider` ile reddetti ve birleşik `runData` çıkış 1 verdi
(`rundata-1.log`). Her birine `getName()` ile ayrı ad verildi; etiket dosyaları zaten ayrı yollara yazıldığı için içerik
değişmedi. Lang dosyalarındaki birleştirme çakışmaları anahtar birleşimiyle (ortak anahtarda farklı değer yok denetimi) çözüldü,
ardından `runData` yeniden üretti: tek fark K-C48'in yeni anahtarı ve dosya sonu satırı (`96b8f8a`); sonraki `runData`'da `status` boş.
Ayrı ad yalnız kayıt hatasını giderir; iki sağlayıcının aynı etiket dosyasını yazmasını önlemez: sahiplik kuralı K-E12.

**K-E6 Mağara GameTest'i yalnız kendi sahnesini özetler.** Birleşik `main`'de ikinci ve üçüncü GameTest koşusunda
`magaraDolarKururDunyaDegismez` "kurumadan sonra şablon hash'i değişti" ile düştü (`gametest-2.log`, `-3.log`). Tanı koşusunda
(`gametest-tani.log`, geçici log satırı commit'lenmedi) değişen bloklar yalnız şablonun x/z 14–15 kenarında taban düzeyinde akan
gerçek suydu (`water[level=3..6]`). `oda()` yalnız 0..13'ü kurar; kenar komşu sahnelere açıktır ve K-C24 dünyası koşular arasında
saklanır: önceki koşudan kalan su oraya akıyordu (dalda testler daha az komşuyla koşuyordu; suyun kaynağı ayrıca aranmadı).
Mağara testi artık kendi kurduğu x/z 0..13 kutusunu özetler (`odaHash`); deniz testi kutunun tamamını kendisi kurduğu için
değişmedi. Üretim kodu değişmedi. Ardışık iki koşu 85/85 (`gametest-4.log`, `-5.log`; `6ecafb9`). Kapsam daralması şef
kararıyla kalır; sızan suyun kaynağı "Açık noktalar (Faz 2b)"da.

**K-E7 Gökyüzü senaryosu birleşik donmaya uyarlandı.** `gokyuzu_sunucu.py` koşu 2'de dönem kaydında `donma=100.00` bekliyordu;
dalda donma sistemi yoktu. Birleşik `main`'de donma süreci kızıl cücede çözülme dönemini açar ve G'yi %100'den çözülme tabanına
(%40) indirir (donma §A.3); ilk koşu `donma=91.73 cozulme=true` ile çıkış 1 verdi (`sunucu/gokyuzu-ilk-basarisiz/`). Beklenti
`cozulme=true` ve donma 40–100 oldu; yeniden koşu çıkış 0 (`d303643`).

**K-E8 Sıcaklık dedicated senaryosu ve var olan config.** Sıcaklık dalının sunucu betiği yok; doğrulayıcının `sunucu_kosu.py`'si
(`.vibe/kanit/sicaklik-dogrulama/`) kopyalanıp KÖK'te koşuldu (`sicaklik bilgi @a`, gamerule `sicaklikSistemi` kapat/aç, `durum`).
Betik `run-server/test` dünyasını kullanır; o dünyanın Faz 2a'dan kalan `serverconfig/dogalafetler-server.toml`'unda Faz 2b
bölümleri olmadığı için Forge 216 `Incorrect key … corrected` WARN'ı yazdı (K-C38 yazıcısı yalnız dosya yoksa çalışır; var
olan dosyayı Forge düzeltir). Yeni dünyalarda (diğer altı senaryo) bizim anahtarlar için 0; kalıcı GameTest dünyası da ilk
koşuda aynı nedenle 140 WARN yazdı, sonraki koşularda 0. Bu bir kez olur ve düzeltilmiş dosya doğrudur.

**K-E9 N15 denetiminde datagen istisnası (donma K-D32).** N15 grep'i (`import .*\.client\.` dış paketlerde) `*/veri/*VeriSaglayicilari.java`
dosyalarını, yalnız `DataGenerators.saglayiciEkle` lambdasından referans verildiği sürece istisna sayar: Forge
`client.model.generators` datagen sınıflarıdır, dedicated sunucuda yüklenmez (K-D32 `-Xlog:class+load` kanıtı; yedi dedicated
senaryoda `invalid dist` 0).

**K-E10 Kullanımdan kalkmış API notu.** `build` `warning:` 0; javac yalnız "Some input files use or override a deprecated API"
Note'u verir. `-Xlint:deprecation` ile dört yer (`xlint-deprecation.log`): `SicaklikOyunTestleri:130`
`EnchantmentHelper.getItemEnchantmentLevel`, `AfetAtesiBlogu:77,92` `canSurvive`/`updateShape` üzerine yazımı (Mojang'ın
`@Deprecated` işareti burada "doğrudan çağırma, üzerine yaz" anlamındadır), `DonmaOyunTestleri:680` `LevelChunk.writeCapsToNBT`.
Kabul edildi: ikisi test kodu, ikisi vanilla blok davranışının üzerine yazımı.

**K-E11 `/dogalafet sifirla` yangın hücrelerini söndürür (şef kararı).** N18 "dünyanın afet ve dönem durumunu ilk haline
döndürür". Önceden aktif afet yokken `sifirla` olay üretmediği için biten afetin yanan hücreleri (en çok 30 sn) ve komut ateşleri
sürüyordu (yangın §E). `YanginYoneticisi.donemDegisti` çekirdeğin `DonemDegistiEvent(SIFIRLAMA)` olayına abonedir (yalnız
sunucunun kendi dönem nesnesi, donma ile aynı koruma): her boyutta süren alan tutuşturma işleri durur ve bütün hücreler
kaynaktan bağımsız (komutla yakılanlar dahil) `temiz=true` yoluyla söner, yanıp biten konumların 2400 tick'lik "yeniden tutuşmaz"
hafızası (`yanmislar`) ve buhar listesi silinir (inceleme E4); yüklü olmayan chunk'taki ateş blokları bekleyen silmeye
girer ve chunk yüklenince kalkar (Y-19). Kanıt: GameTest `YanginOyunTestleri#sifirlaYanginlariSondurur` (`yangin tutustur 4` →
`sifirla` → `aktifSayi` 0, alan işi 0, yanık hafızası boş, `afet_atesi` 0, sahne baştaki gibi; GameTest toplamı 86), dedicated
`yangin_sunucu.py` (`tutustur 6` → `sifirla` → "yangın: dünya dönemi sıfırlandı, 252 hücre söndürüldü" → `bilgi` 0 →
`fill … replace dogalafetler:afet_atesi` "No blocks were filled"); commit'ler `c8477ec`, `7d10047`; kanıt
`.vibe/kanit/entegrasyon-2b/tur3/`, `tur3-ek/`.

**K-E12 Etiket sağlayıcı sahipliği: vanilla etiketlere tek ortak sağlayıcı (şef kararı; inceleme E2).** Etiket sağlayıcısı
dosya başına tam içerik yazar: aynı etiketi iki sağlayıcı yazarsa `DataGenerator` hata vermez, son yazan kazanır ve diğerinin
girdileri sessizce kaybolur. Kural: her `BlockTagsProvider`/`ItemTagsProvider` yalnız kendi ad alanındaki (`dogalafetler:*`)
etiket dosyalarını yazar; vanilla (`minecraft:*`) blok etiketlerine katkı yalnız çekirdek sahipliğindeki
`veri.OrtakEtiketler`'den gelir. Alt sistemler mod kurucusunda `DataGenerators.vanillaBlokEtiketiEkle(etiket, blok...)` ile katkı
ekler (bloklar `Supplier`, datagen'de çözülür; `minecraft` dışı ad alanı `IllegalArgumentException`). Taşınanlar: yangının
`minecraft:fire` katkısı (Y-22, `YanginSistemi.kaydet`) ve donmanın `minecraft:dirt`, `animals_spawnable_on`, `mineable/shovel`, `ice`,
`mineable/pickaxe` katkıları (`DonmaSistemi.kaydet`); `YanginEtiketSaglayici` ve donma `Etiketler` artık yalnız kendi etiketlerini
yazar. Bir alt sistemin kendi `dogalafetler:*` etiketini başka sistem genişletecekse sahibi yine tektir (ör. `yanmaz`,
`asit_dayanikli`, `deprem_dayanikli` yangının; yangın §A4). Kanıt: `runData` çıkış 0 ve blokların/eşyaların bulunduğu bütün etiket
dosyalarının sha256'i öncekiyle aynı (`fire.json` farkı boş; `.vibe/kanit/entegrasyon-2b/tur3-ek/etiket-sha-once.txt`,
`-sonra.txt`), `7d10047`. Bilinen sınır: sıcaklığın `minecraft:damage_type` etiketleri (`is_fire`, `is_freezing`, `bypasses_armor`,
`no_knockback`) blok/eşya etiketi değil ve bugün tek yazarı sıcaklık; ikinci bir yazar gelirse aynı desenle ortak sağlayıcıya
taşınır. Eşya etiketi kancası ilk ihtiyaçta aynı şekilde eklenir (Faz 3/4 işçileri için kural çekirdek sözleşmesi §12'de).

## Faz 3 — Yiyecek

Parça `yiyecek` (dal `is/yiyecek`, son commit `d6f74f1`; `main`'e birleştirme `4b725ec`). Kaynak `.vibe/kararlar/yiyecek.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) ve entegratöre notlar (§E) o dosyada kalır; metindeki §A/§E göndermeleri o dosyaya aittir.
Şef kararları (F-17, F-18) "Şef kararları (Faz 3)" bölümündedir; N17 satırları sondaki tablodadır. Rapor `.vibe/rapor/yiyecek.md`.

**F-01 Etki = kademe toplamı; %25 anında, kalanı süre boyunca azalan hızla.** Madde 14: "anında bir miktar etki + belirli bir
süre boyunca yavaş yavaş devam eden, giderek azalan". Kademe miktarı (ölçeğin yüzdesi) etkinin tamamıdır; `anlikOran`
(0,25) kadarı hemen eklenir, kalanı sıcaklığın `SureliEtki`'siyle (doğrusal azalan hız, toplam tam) süre boyunca. Sıcaklık
§A1 örneği (Güveç 8 + 32) ile aynı oran (%20–25). Pay config'tedir.

**F-02 Kademe varsayılanları: çok küçük 3, küçük 8, orta 20, büyük 35, çok büyük 45 birim.** Şartname aralıklarının (%5–10,
15–25, 30–40) ortasına yakın; çok büyük (yalnız Ateşli Güveç, "en güçlü soğuk savar") büyüğün üst sınırı 40'ın üstünde.
Çok küçük vanilla pişmiş yiyecekler içindir ("çok küçük ve kısa"). Doğrulama: kademeler sıralı olmalı, değilse ERROR log
+ varsayılanlar. Sıcaklık eşikleriyle ölçü (ortam soğuması hariç): Salep (anında 5, 90 sn'de +15) donma eşiğindeki (−75)
oyuncuyu çok soğuk sınırına (−55), Tavuk Çorbası + Salep birlikte nötr banda (−20) taşır; tek yiyecek ölçeği çevirmez.

**F-03 Azalan verim: tür = eşya kimliği, pencere son yemeden, oyun saati, oyuncu verisinde.** Aynı tür son yemeden
60 sn içinde (`0 ≤ geçen < 1200 tick`) tekrar yenirse anında ve süreli kısım ×0,5 (süre kısalmaz). Pencere son yemeden
sayılır: sürekli spam sürekli yarım etki verir (şartname "spam engeli"); ilk yemeden saymak her 60 sn'de bir tam etki
kapısı açardı. Saat `ServerLevel.getGameTime()` (monoton, boyutlar ortak, `/time set` değiştirmez); geri gitmiş saat
(dünya kopyası) ceza vermez. Kayıt `PersistentData` `dogalafetler_yiyecek` (tür → tick), penceresi dolan kayıtlar her
yemede atılır; çıkış/girişte kalır, ölümde yeni hayat temiz başlar, End dönüşünde (`Clone`, ölümsüz) taşınır. Kapı kapalıyken
kayıt yazılmaz (kapalı dünyada gizli durum birikmez, sıcaklık T6 ile aynı). Vanilla yiyecekler ve balıklar da aynı kurala
tabidir. JUnit `YemeGecmisiTest` (7), GameTest `ikinciSalepYariEtkiFarkliTurBagimsiz`, `gecmisKopyadaTasinirOlumdeSifirlanir`.

**F-04 Ateşli Güveç yan etkisi: sıcakta ×1,5; diğer yiyecekler toplanarak "zamanında yenmeli".** Madde 14 genel kuralı
("sıcak yiyecek, ortam zaten sıcakken/oyuncu aşırı ısınmışken ısınmayı artırır... zamanında yenmesi gerekir") toplamalı
uygulanarak sağlanır: etki vücudun durumuna bakılmadan eklenir, sınır ya da bağışıklık yoktur (sıcakken içilen salep aşırı
ısınmaya iter, donarken içilen ayran donmayı hızlandırır). Güveç'in özel "yan etki"si ("sıcak ortamda aşırı ısınmaya kolay
iter") çarpandır: vücut ya da ortam (vücut birimi) `yanEtkiEsigi` (20 birim; ortamda 28 °C) ve üstündeyse etki
`yanEtkiCarpani` (1,5) ile büyür. GameTest `guvecSicaktaAsiriIsinmayaIter`, tek kaynak `kanit/yiyecek/yiyecek-t2-gametest-1.log`
(tur 2, koşu 1): ortam 71,8 birimde vücut 50'den anında +16,88 (= 45 · 1,5 · 0,25, ortamdan bağımsız), 30 sn sonra 80,6 —
YANIYOR kademesi `esikler.yanmaHasari` 75'te başlar, pay +5,6 birim; yemeyen kontrol 52,4 (SICAK). Ortam kalıcı GameTest
dünyasının durumuyla koşudan koşuya kayar (inceleme Yi5; tur 1 koşu 1'de 66,6 / 79,8 / 51,5, pay +4,8); test bu yüzden
eşiğe değil kademeye bakar ve sahnenin yan etki eşiğini (20) geçtiğini ayrıca doğrular.

**F-05 Durum efekti yalnız gösterge; süt silmez; nötr.** "İçten Isınma"/"Serinlik" `MobEffect`'i sıcaklık uygulamaz: sayısal
etkinin tek kaynağı `VucutSicakligi`'nın süreli etkisidir (config, azalan verim, yan etki orada hesaplanır, kalıcıdır, kapıya
uyar). Efekti `applyEffectTick` ile ısıtan yapmak ikinci bir kaynak (çift etki, başka modun verdiği efekt de ısıtırdı) ve süt
ile silinebilen ısınma yaratırdı. Efekt ikon ve kalan süreyi gösterir: amplifier = kademe seviyesi (küçük/çok küçük I, orta
II, büyük III, çok büyük IV), süre = etkinin süresi, ikonlu, parçacıksız (`visible=false`). `getCurativeItems()` boş: süt
sıcaklık etkisini silmediği için ikonu da silmez. Kategori `NEUTRAL` (soğukta kurtarır, sıcakta pişirir). Ölümde vanilla gibi
silinir (sıcaklık etkisi de respawn'da silinir). **Kabul edilen ayrışma (şef kararı, inceleme Yi6):** ölümsüzlük totemi
(`removeAllEffects`) ve `/effect clear` ikonu siler, sıcaklık etkisi kalan süresince sürer; ikon aynı yöndeki en yüksek
kademeyi gösterir (vanilla `MobEffectInstance.update` düşük amplifier'ı yok sayar) ve azalan verimin yarı etkisini ayrıca
göstermez. `MobEffectEvent.Remove`'u iptal etmek reddedildi: totem ve OP komutunun vanilla anlamını değiştirir. Javadoc
`SicaklikEtkisi`, §A1.

**F-06 Sıcaklık yiyecekleri tok da yenir (`alwaysEat`).** Sıcaklık aracıdır ve "zamanında yenmesi gerekir"; tok oyuncunun donarken
salep içememesi kuralı bozardı. Vanilla pişmiş yiyecekler vanilla kuralında kalır (açlık gerekir).

**F-07 Kap.** Yeme 32 tick, yığın 16 (madde 14). Son yiyecekte el yığını boş kaba döner; yığın sürerken kap envantere,
sığmazsa yere düşer; yaratıcıda yığın azalmadığı için kap verilmez (vanilla). Şişeler içme animasyonu ve sesiyle, kaseler ve
karpuz yeme animasyonuyla. Buzlu Karpuz Dilimi'nin kabı yoktur.

**F-08 Tarifler birebir; kalanlar vanilla, bal şişesi yalnız çayda bastırılır.** Hepsi vanilla `crafting_shapeless` (tarif
kitabı açma başarımlarıyla). "Kova geri verilir": süt kovasının vanilla `craftRemainder`'ı (kova) zaten döner; GameTest
Salep/Çikolata/Ayran'da kalanın kova olduğunu doğrular. Bal şişesinin vanilla kalanı (boş şişe) Salep'te korunur: 3 cam
şişe + bal şişesi = 4 cam → 3 salep + 1 boş şişe, denge tutar. Isıtıcı Çay'da (1 cam şişe + bal şişesi → 2 çay) bal şişesi
ikinci çayın şişesidir; vanilla kalan 2 camdan 3 cam çıkarıp şişe çoğalttırırdı. Bu yüzden çay
`dogalafetler:kalan_secimli_sekilsiz` tipindedir (`kalan_birakmayan: [honey_bottle]`; GameTest: çayın kalanı boş).
Ateşli Güveç tek tarif, et malzemesi dizi malzeme `[cooked_beef | cooked_porkchop]` ("sığır veya domuz"; GameTest ikisini de
pişirir). Tarifler `data/dogalafetler/recipes/<eşya>.json`.

**F-09 Kutup Balığı Çorbası etiketle; etiket dosyası balıkçılığın.** Balıklar balıkçılık işçisinin eşyalarıdır; tarif eşya
etiketine bağlanır. Bu dal boş etiket dosyası da üretmez: balıkçılığın etiket sağlayıcısı aynı yolu yazacağı için iki sağlayıcı
çakışırdı. Etiket boşken koşulsuz tarif yüklenir ve **bariyerle eşleşir**: Forge 47 `Ingredient$TagValue.getItems()` boş
etikete "Empty Tag" adlı bir bariyer koyar (kase + bariyer + patates → çorba; JEI/tarif ekranı malzemeyi bariyer gösterir).
Bu yüzden tarif koşulludur (F-17). İlk sürümdeki "hiç eşleşmez" cümlesi yanlıştı (inceleme Yi1).

**F-10 Datagen kendi adlı sağlayıcılarla.** Vanilla `RecipeProvider.getName()` `final` ve "Recipes"tir: mod genelinde tek
`RecipeProvider` olabilirdi (K-E5); `YiyecekVerileri.Tarifler` ("Doğal Afetler yiyecek tarifleri") `ShapelessRecipeBuilder`
çıktısını (tarif + tarif kitabı başarımı) kendisi yazar. Eşya modelleri düz JSON ("Doğal Afetler yiyecek modelleri"):
`client.model.generators` import'u olmadığı için K-E9 istisnasına gerek yoktur.

**F-11 Vanilla pişmiş yiyecekler: `LivingEntityUseItemEvent.Finish`, config listesi.** Kendi eşyalarımız hariç (onlar kendi
`finishUsingItem`'larında uygular, çift etki olmaz). Liste `eşya=saniye` (başka modların yiyecekleri eklenebilir), miktar
çok küçük kademe (3 birim), efekt "İçten Isınma I". Varsayılan: kızarmış patates, pişmiş sığır/domuz/tavuk/koyun/tavşan/morina/
somon 30 sn; mantar/tavşan/pancar çorbası 45 sn. Çiğ yiyecekler listede yok. `vanillaIsinma=false` kapatır (GameTest
`ConfigValue.set` ile sınar ve geri alır). Kayıtlı olmayan kimlik WARN ile atlanır.

**F-12 Olay kapı kapalıyken de gelir.** Başarım ve ileride başka dinleyiciler "yendi"yi bilir; `uygulandi()` ayırır. Kademeler
aynıdır (sıcaklık değişmedi).

**F-13 Tooltip istemcide `ItemTooltipEvent` ile (`client.yiyecek.YiyecekIpuclari`).** Sıcaklık `SicaklikIpuclari` deseni; N6
kapsamı dışı eşya bilgisi. Satır "İçten Isınma: orta, 90 sn" (altın) / "Serinlik: hafif, 60 sn" (su mavisi); Güveç'te kırmızı
"Sıcakta hızla aşırı ısıtır". Kademe adları şartnamenin ürün dilinden: "hafif" (küçük), orta, büyük, çok büyük, çok hafif.
Vanilla yiyeceklerde satır yalnız SERVER config yüklüyse (senkronlu) ve açıksa.

**F-14 Balık süreleri config'te (N17).** Şartname pişmiş balık için "orta ısınma", çiğ için "hafif soğutur" der, süre vermez:
`yiyecek.balik.pismisSaniye` 90 (Salep gibi orta), `cigSaniye` 60 (Buzlu Karpuz gibi hafif). Balıkçılık `YiyecekAPI` fabrikalarıyla
alır.

**F-15 Görsel kanıt gerçek yeme yoluyla.** `client.yiyecek.YiyecekOtomasyonu` (`-Potomasyon=yiyecek`) kullan tuşunun *oyun içi*
durumunu (`KeyMapping.setDown`) basılı tutar: istemci → sunucu eşya kullanma paketi, 32 tick, sunucuda `finishUsingItem`;
fiziksel girdi olayı üretmez (girdi sayacı 0). Tooltip için yalnız `MouseHandler`'ın iç konumu ayarlanır (SRG `f_91507_`,
`f_91508_`), işletim sistemi imleci oynatılmaz. K0-21 rev. 2 + (k) kilit, ws 93, dil tr_tr.

**F-16 Komut `/dogalafet yiyecek bilgi [oyuncu]`.** Dedicated sunucuda config'le kurulmuş tabloyu okunur kılar (kabul
senaryosu `tools/senaryo/yiyecek_sunucu.py`); oyuncuda azalan verim penceresini gösterir.

**F-17 Kutup Balığı Çorbası koşullu tarif (şef kararı, inceleme Yi1).** → Şef kararları (Faz 3).

**F-18 Cold Sweat yüklüyken yiyecekler yalnız yemek değeri verir (bilinen sınır, şef kararı; inceleme Yi3).** → Şef kararları (Faz 3).

**F-19 Ağır süreç kapısı iki koşuda çiğnendi (inceleme Yi2).** Tur 1'de ilk `runGameTestServer` (14:20) ve son `build` + `runData`
(14:32) `pgrep -fc GradleWrapperMain` = 2 iken başladı (GOREV §2 ihlali; boş RAM 5 GB, OOM yok, sonuçlar etkilenmedi: aynı
kod sonraki kapılı koşularda aynı sonucu verdi). Tur 2'de bütün Gradle koşuları `/tmp/yiyecek_gradle.sh` kapısından geçti
(sayaç ≥ 2 ya da boş RAM < 4 GB iken 60 sn bekle, en çok 20 dk); kapı satırları (`KAPI n: gradle=… bos=…`) her log'un başında
(`kanit/yiyecek/yiyecek-t2-*.log`; ör. `compileJava runData` 8 tur, kesilen build 11 tur bekledi). İlk build bekleme sırasında oturum
yeniden başlayınca kesildi ve hiç koşmadı; yeniden kapıdan geçirildi. Dedicated senaryo ve runClient da aynı kapı döngüsünden
geçti (`protokol.log`, senaryo çıktısı).

### Reddedilen yollar

- **Efektin sıcaklığı uygulaması (`applyEffectTick`):** iki kaynak, sütle silinen ısınma, başka modun verdiği efektle ısınma (F-05).
- **Bütün sıcak yiyecekleri sıcakta güçlendirmek:** şartname çarpanı yalnız Güveç'in "yan etki"si olarak verir; genel kural
  toplamalı uygulamayla sağlanır (F-04).
- **Azalan verim penceresini ilk yemeden saymak:** her 60 sn'de bir tam etki, spam engelini zayıflatır (F-03).
- **Gerçek saat (`System.currentTimeMillis`):** tek oyunculuda duraklatmada ve sunucu gecikip yetişirken oyun süresinden kopar;
  oyun saati monoton.
- **Capability ile geçmiş:** küçük oyuncu verisi için kayıt/klonlama kodu; `PersistentData` yeter (sıcaklık S-06 ile aynı).
- **Vanilla `RecipeProvider` alt sınıfı ve `ItemModelProvider`:** sabit ad çakışması (K-E5) ve istemci paketi import'u (K-E9) (F-10).
- **Boş `#pismis_kutup_baligi` etiket dosyasını bu dalda üretmek:** balıkçılığın sağlayıcısıyla aynı yol (F-09).
- **Isıtıcı Çay'da vanilla kalan:** cam şişe çoğaltır (F-08).
- **Tok oyuncuya sıcaklık yiyeceği yasağı (vanilla `canEat`):** "zamanında yenmesi" kuralını bozar (F-06).


## Faz 3 — Radar ve sismograf

Parça `radar-sismograf` (dal `is/radar-sismograf`, son commit `ffcd8bd`; `main`'e birleştirme `e8d04f2`). Kaynak `.vibe/kararlar/radar-sismograf.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) ve entegratöre notlar (§E) o dosyada kalır; metindeki §A/§E göndermeleri o dosyaya aittir.
Şef kararları (R-17, R-18) "Şef kararları (Faz 3)" bölümündedir; N17 satırları sondaki tablodadır. Rapor `.vibe/rapor/radar-sismograf.md`.

**R-01 Görünürlük kuralı tek saf fonksiyonda, pencere tanımı çekirdekte.** `Gorunurluk.radar(asama, normalOnUyariBasladi,
global, ayniBoyut, mesafe, menzil)`: BITIS gizli; ON_UYARI'da global afet her zaman gizli (N7), bölgesel afet yalnız
`normalOnUyariBasladi()` sonrası; SIREN/AKTIF'te global afet `ANOMALI` (boyut ve menzil aranmaz: güneş ve ay her yerdedir),
bölgesel afet aynı boyut + menzil içinde `AFET`. `Gorunurluk.sismograf(tur, asama, normalOnUyariBasladi, hassas, ayniBoyut,
mesafe, menzil)`: yalnız deprem/sinkhole, aynı boyut, menzil içi, BITIS hariç; ON_UYARI'da normal sismograf
`normalOnUyariBasladi()` sonrası, Hassas ON_UYARI'nın başından (R-20).

**R-02 Sismografın menzili var.** Şartname sismograf menzili vermiyor. Menzilsiz sismograf haritanın öbür ucundaki depremi
de "öncü sarsıntı" diye gösterirdi; gerçek sismograflar uzak depremi de kaydeder ama oyunda bu, eşyayı her oyuncu için
anlamsız bir alarma çevirirdi. Radarla aynı 300/800 seçildi (normal/Hassas; config).

**R-03 Yön ve mesafe istemcide, merkez sunucudan.** Paket merkezi (radarda gerçek, sismografta tahmini) blok
koordinatı olarak taşır; ok ve mesafe istemcide her karede oyuncunun güncel konumu ve bakışıyla hesaplanır. Böylece ok
dönerken akıcıdır ve oyuncu yürüdükçe paket gerekmez (paket yalnız durum değişince gider, madde 3).

**R-04 "Değişince" = alan eşitliği.** Sunucu son gönderilen paketi oyuncu başına tutar; yeni durum `equals` ise göndermez.
Kalan süre saniye çözünürlükte olduğundan aktif afette radar paketi saniyede bir gider (üst sınır 1 Hz); sakin
radarda tek paket. Geçmişin yaşı dakika çözünürlükte, Gelişmiş radarın hareket yönü 5°'ye ve hızı 0,5 blok/sn'ye,
öncü şiddeti 0,02'ye yuvarlanır: titreşen sayılar paket üretmez. Bırakınca tek `YOK`; radarı hiç tutmamış oyuncu için
harita girdisi yoktur ve paket gitmez.

**R-05 Normal radar seviyeyi hiç almaz.** Madde 13a "normal radar seviyesini göstermesin": seviye, hareket ve geçmiş
normal radarda pakete hiç konmaz (yalnız çizimde gizlemek değil). Sismografta seviye yalnız Hassas'ta gönderilir.

**R-06 Şiddet yüzdesi ve merkez afet başına sabit hatayla tahmin.** Gerçek şiddet S1 %20 … S5 %100. Tahmin
`gerçek ± pay`, pay normalde 24 puandan başlayıp afet yaklaştıkça (ON_UYARI ilerlemesi) 7,2'ye iner; Hassas'ta 6 → 1,8.
Sapmanın yönü afet başına sabittir (tohum = `afetNo`; Hassas ayrı tohum): değer saniyeden saniyeye zıplamaz, gerçeğe doğru
kayar. Merkez de aynı tohumla en çok 40 blok (Hassas 6) sapar; Hassas'ta mesafe 1 m, normalde 10 m'ye yuvarlı yazılır.

**R-07 Öncü sarsıntı darbelidir, sürekli değil.** Gerçek öncü depremler aralıklı küçük sarsıntılardır. Şiddet
`(0,35 + 0,13·seviye)·(0,2 + 0,8·ilerleme^1,5)`; darbe aralığı 12 sn'den 3 sn'ye iner, her darbe 2 sn'lik `sin²` zarfı. Her
darbede kısık ses (0,08–0,30 hacım, `AMBIENT`, yalnız bu istemcide; konuşma bandı −40 dB) ve en çok 0,6° × sunucu
titreme çarpanı (vars. %50 → 0,3°) kamera titremesi; titreme CLIENT config ile kapatılabilir. SIREN boyunca da sürer
(öncü sarsıntı ana sarsıntıya kadar).

**R-08 Sismik grafik istemcide üretilir.** 60 sn × 20 Hz = 1200 örnekli halka tampon her istemci tick'inde, yalnız sismograf
taşınırken dolar: deterministik taşıyıcı (üç sinüs + yumuşak gürültü) × genlik (sakin zemin 0,04; öncüde darbe zarfıyla
büyüyen tepe; AKTIF'te 0,55–1). Sunucu örnek göndermez (madde 3: 20 Hz paket yerine durum paketi). Ekran 200 piksel
sütunu, sütun başına 6 örneğin min–max'ı olarak çizer (kare başına ~200 dolgu); oyunu duraklatmaz.

**R-09 Gösterge yerleşimi: Forge'un gerçek yükseklikleriyle.** Varsayılan sağ alt köşe. Alt köşelerde panel hotbar + yan el
yuvası sütunuyla (orta ±121 px) örtüşürse hotbar'ın (alttan 26), orta durum sütunuyla (±91) örtüşürse `D + 2`'nin
(`D = max(leftHeight, rightHeight)`), ortalanmış action bar satırıyla örtüşürse `max(D + 9, 68) + 8`'in üstüne kalkar.
Forge 1.20.1 `ForgeGui.renderRecordOverlay` action bar'ı vanilla'nın `h - 68`'i yerine `h - max(D + 9, 68)`'e taşır (metin
4 px yukarıdan, aksan 2 px); sıcaklık barı `leftHeight`'ı 74'e çıkardığı için satır alttan ~89 px'e kadar çıkar. Action bar
genişliği, sismograf metni son 80 tick içinde yazıldıysa onun piksel genişliği (Hassas metni seviyeyle ~274 px), değilse
±121. Katman `CHAT_PANEL`'in altında, yani durum satırları ve action bar çizildikten sonra: `leftHeight/rightHeight` o anda
kesindir. **Kanıt bulgusu (iki koşu):** ilk sürüm sabit vanilla yüksekliğini (72) kullandı — Hassas metni (x 97–371, y 154–164)
radar panelinin (x 331–464, y 77–171) altında kaldı (`gorsel-kosu1/`); ikinci sürüm `D + 11` ile panel altı y 158'de kaldı
(`gorsel-kosu2/`, metin 154'ten başlıyor). Üçüncü sürüm Forge kaynağındaki formülü kullanır; JUnit
`GostergeYerlesimiTest#uzunActionBarSatiriPaneliYukariItar` ölçülen pikselleri sabitler, kanıt `gorsel/` piksel taramasıyla
dikey ayrıklığı doğrular. Köşe ve kaydırma CLIENT config'te.

**R-10 Kozmik Çekirdek etiketle.** Eşya meteor işçisinindir; tarif `#dogalafetler:kozmik_cekirdek` ister. Etiketi bu dal
üretmez (meteor işçisinin `ItemTagsProvider`'ı yazar), aksi hâlde iki dal aynı etiket dosyasını üretip birleştirmede
çakışırdı. Boş etiketin malzemesi Forge 47'de "Empty Tag" adlı bir bariyerle eşleşir (inceleme R1); bu yüzden tarifler
koşulludur (R-17). (İlk sürümdeki "boş etiketle craft edilemez" ifadesi yanlıştı.)

**R-11 Hassas öncelikli.** Oyuncu iki radar tutuyorsa (ana + yan el) Gelişmiş olan, iki sismograf taşıyorsa Hassas olan
geçerlidir. "Envanterde" = hotbar (9 yuva) ya da yan el (madde 13b "hotbar veya yan el"); ana envanterin geri kalanı sayılmaz.

**R-12 `RadarOlaylari` yazılmadı.** Görev metninde isteğe bağlıydı; başarımlar (#15, #16) craft ile açılır ve vanilla
tetikleyicisi yeterlidir. Dinleyicisi olmayan olay ölü kod olurdu.

**R-13 Güneş/ay anomalisinde yön ve mesafe yok.** N7 "gökyüzü anomalisi" ve geri sayım diyor; global afetin merkezi
(doğma noktası) anlamsız olduğu için pakete konmaz, kapı mor nabız ve halka çizer. Geri sayım SIREN'de AKTIF'in
başlamasına ("Başlamasına m:ss"), AKTIF'te afetin kalanına ("Kalan").

**R-14 Yazı yalnız N6 istisnalarında.** Radar paneli yalnız radar elde iken (sunucudan `elde` ve istemcide eldeki eşya
birlikte doğrulanır), action bar yalnız sismografı taşıyan oyuncunun kendi istemcisinde (vanilla `displayClientMessage(…,
true)`, sunucudan sohbet/action bar paketi gitmez).

**R-15 Otomasyon gömülü sunucu komut kaynağıyla.** Görsel kanıt `RadarOtomasyonu` tek oyunculu dünyada komutları
`server.createCommandSourceStack().withSuppressedOutput()` ile verir (oyuncu tuşu, OP ya da hile ayarı gerekmez; sohbet
kirlenmez). Yalnız `-Potomasyon=radar` ile etkin.

**R-16 K-E12 ve K-E11 uyumu (main `254fe2d` kural eki).** Kendi sağlayıcım (`RadarSismografVerileri`) yalnız
`dogalafetler:*` tarif ve eşya modeli yazar; hiçbir vanilla (`minecraft:*`) blok/eşya etiketine katkı yoktur, bu yüzden
`DataGenerators.vanillaBlokEtiketiEkle` kancasına bağlanacak bir şey yoktur. `#dogalafetler:kozmik_cekirdek` etiketini de bu
dal yazmaz (R-10). K-E11: radar ve sismograf kalıcı durum tutmaz (yalnız oyuncu başına son gönderilen paket ve hareket izi,
bellekte); `/dogalafet sifirla` aktif afeti durdurduğunda sonraki 1 Hz denetim "sakin" paketi gönderir, iz yeni afet
numarasıyla sıfırlanır. `DonemDegistiEvent(SIFIRLAMA)` aboneliği gerekmez. Geçmiş (`sonAfetler`) çekirdeğindir ve `sifirla`
onu temizler; Gelişmiş radar bir sonraki pakette boş listeyi gösterir.

**R-17 Gelişmiş tarifler koşullu (inceleme R1, şef kararı).** → Şef kararları (Faz 3).

**R-18 Gelişmiş panel ekranın en çok %30'u (inceleme R2, şef kararı).** → Şef kararları (Faz 3).

**R-19 Toplu çizim, sağ üst efekt ikonları, 1 sn tarama (inceleme R3, R7).** Forge 47.4.28 `GuiGraphics.fill` yönetilmeyen
modda her çağrıdan sonra boşaltır (ayrı çizim). Radar paneli (~280 dolgu + metin) ve sismograf grafiği (~250 dolgu) artık
`g.drawManaged(...)` içinde: aynı `RenderType`'taki ardışık dolgular tek çizime toplanır, tipler arası geçiş önceki
topluğu boşalttığı için sıra (zemin → kap → yazı) korunur. FPS ölçümü Faz 5 tablosunda (radar elde ve grafik ekranı açık).
`SAG_UST` köşesinde panel vanilla efekt ikonlarının altına iner (`GostergeYerlesimi.efektAlti`: yararlı satır y 1–25, zararlı
y 27–51; `showIcon` ve Forge `isVisibleInGui` olan efektler). Tarama 18°/tick = saniyede bir tur (tasarım notu).

**R-20 Sismograf penceresi çekirdekten; ses ölçümü `docs/kanit/ses/` (inceleme R4, R5).** Sismograf kendi
`asamaGecen >= erken × (1 − oran)` formülünü bıraktı: normal sismograf radar gibi `afet.normalOnUyariBasladi()`, Hassas
ON_UYARI'nın başından (erken bölümü çekirdeğin `cekirdek.onUyari.erkenBildirimTick`'i belirler). `esya.sismograf.hassasErkenOran`
config'i kaldırıldı; pencere tanımı tek kaynaktır. Öncü sarsıntı sesinin `ebur128` ölçümü ve spektrogramı siren ve ortam
sesleriyle aynı yerde: `docs/kanit/ses/esya_oncu_sarsinti_{ebur128.txt,spektrogram.png}` (GOREV §6 #11); `.vibe/kanit/` kopyası
kalır. Oyun içi Wave kaydının ölçümü bir sonraki görsel koşuya (runClient bu turda yok).

### Reddedilen yollar

- **Sunucudan sismik örnek akışı (20 Hz):** grafik gerçek zamanlı olurdu ama oyuncu başına 20 paket/sn madde 3'e aykırı;
  dalga istemcide durum paketinden deterministik üretilir (R-08).
- **Yönü ve mesafeyi sunucuda hesaplayıp göndermek:** oyuncu her dönüşte paket ister ya da ok saniyede bir zıplar (R-03).
- **Radarı yalnız istemcide `IstemciAfetDurumu`'ndan çizmek:** `AfetDurumPaketi` ON_UYARI'da gitmez ve yalnız etki alanı +
  64 blok içindekilere gider; 300/800 menzil ve ön-uyarı için ayrı, oyuncuya özel paket gerekir.
- **Doku tabanlı HUD paneli:** pikselli CRT çerçevesi `fill` ile çizildi; boyut içeriğe (dil, geçmiş satırı) göre değişir,
  sabit dokuyla güzel uzamazdı.
- **Sismograf grafiği için `AbstractContainerMenu`:** sunucu tarafı menü gerekmez, salt istemci `Screen` yeterli.
- **Kozmik Çekirdek etiketini bu dalda üretmek:** R-10.


## Faz 3 — Meteor ve termostat

Parça `meteor-termostat` (dal `is/meteor-termostat`, son commit `1748441`; `main`'e birleştirme `6ab6357`). Kaynak `.vibe/kararlar/meteor-termostat.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) ve entegratöre notlar (§E) o dosyada kalır; metindeki §A/§E göndermeleri o dosyaya aittir.
Şef kararları (M-22) "Şef kararları (Faz 3)" bölümündedir; N17 satırları sondaki tablodadır. Rapor `.vibe/rapor/meteor-termostat.md`.

**M-01 Meteor düz çizgide sabit hızla iner; çarpışma sunucuda blok örneklemesiyle.** Varlık `noPhysics`; sunucu her tick
yolunu 0,4 blok adımla örnekler, ilk çarpışmalı bloka (yaprak ve çarpışmasız bitkiler geçilir: krater ağaç tepesinde
değil yerde açılır) ya da suya (gerçek ya da `SanalSu.suSeviyesi`) çarpar. Hız 3 blok/tick (eksen başına paket sınırı 3,9),
140 blok yükseklikten 40–70°'de ~2,5–3 sn uçuş. İstemci aynı hızla ilerletir (2 tick'te bir düzeltme), iniş noktasını
`clip` ile tahmin eder. Gerekçe: yerçekimli yayın çarpışma tahmini ve senkronu daha pahalı, görsel farkı yok.

**M-02 Suya düşen meteor buhar çıkarır; krater, ateş ve ödül yok.** Sanal su dahil (madde 8: gerçek suya
dokunulmaz). Sarsıntı yarı şiddette.

**M-03 Krater: paraboloit çanak, magma astar, enkaz halkası; sert bloklar ve sıvılar korunur.** Küçük r 2,5 / d 2,
büyük r 5,5 / d 4 (tasarım "çap 0,6–3 blok" meteor → ~5–11 blok krater; derinlik/çap ~0,35, basit krater). Yüzeyün
2 blok üstü de boşaltılır (bitki, kar). Sert = `dogalafetler:deprem_dayanikli` ∪ `dogalafetler:yanmaz` ∪ kırılmaz ∪ blok
varlıklı (sandık içeriği kaybolmasın). `UPDATE_SUPPRESS_DROPS`: krater eşya düşürmez (yüzlerce toprak eşyası ve ödül
"yalnız meteordan" kuralı). Taban ortası (r ≤ 0,4R) magma: tasarımın "kraterde 20 sn magma parıltısı" ile sıcak süresi
aynı config'ten; sonra karataş (ayda pürüzsüz bazalt) olur. İşlem tick başına ortak bütçeyle (`kraterBlokButcesi` 512 ×
`TpsOlcer.kismaCarpani()`); büyük krater ~700 işlem ≈ 2 tick.

**M-04 `afetBlokHasari=false` → krater planı boş; ateş ve ödül sürer.** Ateş ortak yangına gider, kuralı yangın kendisi
uygular (blok değişmez). Ödül blok değil eşya olduğu için verilir (zemine bırakılır). GameTest: bölge hash'i aynı.

**M-05 Meteor durumu kaydedilmez (karar: uçan meteor atılır).** Varlık `noSave`, yağmur ve krater işleri bellekte.
Yeniden açılışta uçan meteor (≤ 3 sn uçuş) ve yarım krater işi (≤ birkaç tick) kaybolur; yağmuru afet `yuklendi`'de
yeniden başlatır. Gerekçe: kaybolan oynanış birkaç saniye; varlığı kaydetmek yarım çarpma ve çift yağmur riskini getirir.
Chunk'ı boşalan meteor da atılır; hedefine 600 tick'te ulaşamayan meteor atılır.

**M-06 Dağıtım: round-robin + halka ofseti.** Oyuncular UUID sırasıyla, her meteor sıradakine; fark en çok 1 (JUnit:
1–10 oyuncu, oyuncu sayısı değişince varyans). Ofset 32–160 blok halkasında alana göre düzgün (tasarım §2). Oyuncusuz
sunucuda doğuş noktası.

**M-07 Ödül ömrü `Item#getEntityLifespan` ile 10 dk.** Forge'un eşya ömrü kancası vanilla 6000 tick yerine
`meteor.odulOmruSaniye` (600) × 20 verir; `setUnlimitedLifetime` + kendi sayacımız reddedildi (§D). Aynı kural Kozmik
Çekirdek için de geçerli.

**M-08 Sıcak parça: NBT bitiş tick'i; alan ya da yanına gelen oyuncu yanar; su/kar/buz üstünde hemen soğur.** Alınca
(`EntityItemPickupEvent`) sıcaksa `meteor.yanmaSaniye` (3) sn yanma, etiket her durumda silinir (soğuk parçalar yığılır).
Yerde 5 tick'te bir: süre dolduysa ya da suda/karın/buzun üstündeyse etiket silinir (cız sesi + buhar), değilse 0,6
blok içindeki oyuncular yanar.

**M-09 Ödül zarı tohumlu ve çarpma başına bir kez.** İki bağımsız atış (parça %3 / büyükte %5; çekirdek yalnız
büyükte %0,3). Çekirdek krater ortasına, parça 0,6R içinde rastgele bir sütuna, tabanın ilk boş noktasına bırakılır.
Loot table/GLM yok (madde 13c "yalnız meteordan").

**M-10 Görünüm kaynaktan.** `AY_CARPMASI` kaynağı ay görünümü (tasarım: gri-beyaz kaya, mavi-beyaz iz, ay sesi %10 tiz),
diğerleri güneş. Komutta `ay` dalı.

**M-11 Sarsıntı ve parlama istemcide, oyuncuya özgü.** Sunucu şiddeti mesafeyle hesaplar (`temel · (1 − d/menzil)²`;
küçük 0,35 / 48 blok, büyük 0,8 / 112 blok); istemci `ViewportEvent.ComputeCameraAngles` ile yaw/pitch/roll titreşimi
(şiddet × 3,5° × `meteor.sarsintiOlcegi`), süre 8–40 tick. Parlama GUI katmanı (`registerAboveAll`), F1 (GUI gizli)
karelerinde görünmez.

**M-12 Sesler vanilla.** Uçuş: `item.elytra.flying` döngüsü meteoru izler, Doppler perde 1,2 → 0,8; çarpma
`entity.generic.explode` alçak perde (hacim 10 / 5 → 160 / 80 blok); suda sıçrama + lav sönmesi. Kendi sentezimiz
(tasarım "30–80 Hz güm + çakıl") bu parçada yok: açık nokta (Faz 4 afet ses tasarımıyla).

**M-13 Eşya kaydı ayrı `DeferredRegister`.** Radar/sismograf işçisinin `esya/EsyaKayitlari.java`'sıyla çakışmamak için
`esya/MeteorEsyaKayitlari.java`; kayıt `Meteor.kaydet(modBus)` içinden (K-C33).

**M-14 Tarif malzemeleri Forge etiketleri.** Demir/altın/kızıltaş `#forge:ingots/iron`, `#forge:ingots/gold`,
`#forge:dusts/redstone` (madde 2 uyumluluk: başka modların eşdeğer külçeleri de kabul).

**M-15 Varlık tavanı 48/boyut; dolunca meteor atılmaz.** 10 oyuncu × tasarımın "oyuncu başına 4 eşzamanlı" (Yüksek) +
pay. Kuyruğa alınmaz: yağmur zaten süreklidir.

**M-16 Datagen tek sınıf, iki sağlayıcı adı (K-E5).** `MeteorVerileri` sunucu (tarifler, etiket) ve istemci (eşya
modelleri) için ayrı adlı iki örnek.

**M-17 GameTest yolları.** Krater/ödül/yangın testleri `MeteorCarpmasi.carp`'ı doğrudan çağırır (uçuş süresinden
bağımsız), biri gerçek uçuşla (`dusur`, 90°): meteor varlığı doğar, iner, krater açar. Zar paket içi
`dusur(..., Sanslar)` ile sabitlenir. Şablon `dogalafetler:meteor_sahne` (15x12x15 hava, `tools/assets/meteor_sahne.py`).


**M-18 Sıfırlama temizliği (K-E11).** `DonemDegistiEvent(SIFIRLAMA)` (`/dogalafet sifirla`) gelince `MeteorYoneticisi`
bütün boyutlarda yağmurları durdurur, uçan meteorları kaldırır (yeni krater yok), sıcak ödül parçalarının sıcak etiketini
siler ve bekleyen magma astarını hemen soğumuş kayaya çevirir. Açılmış kraterler ve yerdeki ödüller kalır (dünyadaki
kalıcı iz; sıfırlama dönem durumudur, harita değil). Ateşleri yangın kendisi söndürür (K-E11). GameTest
`sifirlamaUcanMeteorVeSicakParcayiTemizler` (yalnız meteor dinleyicisini çağırır; donma/su sıfırlanmaz).

**M-19 Etiketler (K-E12).** Bu parça vanilla (`minecraft:*`) blok/eşya etiketine katkı yapmaz; kendi sağlayıcısı yalnız
`#dogalafetler:kozmik_cekirdek` eşya etiketini yazar. Sert blok okuması yangının `dogalafetler:deprem_dayanikli` ve
`dogalafetler:yanmaz` etiketlerinden. Entegratörün K-E12 kancasına bağlayacağı satır yok.

**M-20 Görsel otomasyonun yavaş çekimi.** Gizli workspace'te istemci ~1–10 FPS çizer; 3 blok/tick meteor bir çizilmiş
karede ekrandan geçer (koşu 1'de iz ve çarpma kareleri aynı karede kaldı, `carpma`/`carpma_b` md5 eşit). Otomasyon
vitrinde `meteor.dusmeHizi`'ni 0,6'ya indirir (yalnız `-Potomasyon=meteor`; sonra eski değere döner) ve kareyi istenen
tick'te değil sonraki çizilmiş karenin sonunda (`RenderTickEvent` END) alır. Oyun davranışı değişmez.


**M-21 Meteor GameTest'leri kutudan taşmaz ve geçince sahnesini boşaltır.** İlk sürümde yangın testi tahta halkasını
krater merkezinden 9 bloka kadar (15x15 kutunun 2 blok dışına) koyuyordu. GameTest dünyası koşular arasında saklanır
(K-C24) ve batch'ler x boyunca dizildiği için test eklenince konumlar kayar (`meteor_sifirla` eklenince krater merkezleri
318 → 337); ikinci ve üçüncü koşuda yangının Y-01 deneyi (`afetAtesiKendiBasinaYayilmaz`, doFireTick açık) önceki
koşudan kalan tahtanın yanına düştü ve "afet ateşi 25 blok" ile düştü (`build-gametest-2.log`, `-3.log`; ilk koşu
93/93). Tahta artık kutu içinde (r 5,6–7,0, yüzeyde) ve her sahne testi geçince `sahneyiTemizle`: ateş (kutu + 4),
ödül eşyaları, kutudaki bloklar hava. Bu worktree'nin kirlenmiş GameTest dünyası kenara alındı (`/tmp`); `main`'in
dünyası eski sürümü hiç görmedi. Üretim kodu değişmedi.


### Tur 2 (inceleme-meteor-termostat M1…M6, şef kararları)

**M-22 Vitrin Faz 4'te puanlanır (şef, M2).** → Şef kararları (Faz 3).

**M-23 Çarpma parlaması dünya çiziminin sonunda (M3).** GUI katmanı (`registerAboveAll`) kaldırıldı;
`RenderLevelStageEvent.Stage.AFTER_LEVEL`'da birim izdüşümle tam ekran `POSITION_COLOR` quad (derinlik/sis yok, sıcaklığın
ekran efektleriyle aynı karışım kurulumu). F1 (`hideGui`) iken de görünür; dünyanın üstünde, elin ve GUI'nin altında
çizilir (Forge `renderLevel` sırası: `AFTER_LEVEL` → `renderItemInHand` → GUI; HUD parlamanın üstünde okunur kalır). Güç,
süre ve `parlamaOlcegi` (0 kapalı) değişmedi.

**M-24 İz, plazma kabuğu ve yer diski sissiz (M4).** `client.meteor.MeteorIsikTipi`: `RenderType.eyes` ile aynı gölgelendirici,
doku, toplamalı karışım ve yalnız renk yazımı; katman parçası çizim sırasında `setShaderFogStart(MAX)` yapıp eski değeri
geri koyar (eyes parça gölgelendiricisi rengi `linear_fog_fade` ile çarpıyordu). LOD değişmedi (parçacık 192 blok, çizim
512); ayrıca blok kümesi gövde 96 bloktan uzakta çizilmez (uzaktan kare leke gibi okunuyordu; plazma panosu kalır).
Estetik ayar (genişlik/parlaklık) Faz 4 vitrininde.

**M-25 Krater işi ve magma soğuması kalıcı ve yüklü olmayan chunk'ta bekler (M1).** Boyut başına SavedData
`dogalafetler_meteor` (`MeteorVerisi`): bitmemiş krater işleri (merkez, boyut, görünüm, kaynak, ödül sonucu, evre/sıra,
sayaçlar, tohum; plan tohumdan yeniden üretilir) ve magma soğumaları (konum, mutlak oyun tick'i, yeni blok). Sıradaki
hücrenin chunk'ı yüklü değilse iş orada durur (yangın Y-19 "bekleyen" deseni), diğer işleri tıkamaz, her tick bir
`isLoaded` ile yeniden dener; chunk yüklenince (yeniden açılıştan sonra da) kaldığı yerden tamamlanır, yangın/ödül o
zaman gelir. Zamanı gelmiş soğuma chunk yüklü değilse kayıtta kalır. Magma süresi yeni `meteor.magmaSogumaSaniye`
(20, tasarımın "20 sn magma parıltısı"; yangının `afetSonrasiSonumSaniye`'sinden ayrı, çünkü o yangının sönümüdür). `sifirla`
(M-18): yüklü magma hemen, yüklü olmayan yüklenince soğur; süren kraterler sürer. `meteor bilgi` bekleyen sayısını yazar.
M-05'in "yarım krater kaybolur" cümlesi geçersiz; uçan meteor ve yağmur yine kaydedilmez.

**M-26 Sıcak parça takibi NBT'den (M6).** `dogalafetler_sicak_bitis` zaten mutlak oyun tick'idir (`getGameTime`, kayıtla
korunur). Bellekteki liste artık yalnız bir önbellek: `EntityJoinLevelEvent` (bırakılış, yeniden açılış, chunk yüklenmesi)
etiketli eşyayı süresi sürüyorsa takibe alır, dolmuşsa hemen soğutur. Yeniden açılıştan sonra "yanına gelen yanar" ve
"suda soğur" sürer.

**M-27 Tur 2 testleri (M5).** GameTest: gerçek alma yolu (`ItemEntity.playerTouch` → olay → envanter, etiketsiz yığılır) ve
yakınlık yanması; NBT kaydet/oku taklidi (sıcak kalır ve takibe girer / süresi dolmuş kayıt soğur); suya düşen gerçek
uçuş (krater, ateş, ödül yok, bölge hash'i aynı, olay `suya`); yağmur iki oyuncuya dengeli (fark ≤ 1) ve uçan tavan 48
(paket içi yağmur aşırı yüklemesi: halka 0–2 blok, dik giriş); `afetBlokHasari=false` iken ateş yanar, yakıt ve bölge
değişmez, ödüller düşer; krater işi yüklü olmayan hücrede bekler, kayıttan aynen okunur, sonra tamamlanır, magma
soğuması bekler; termostat + yün + 30 tüy birlikte (yalıtım girdisi üçünü sayar, sıcakta çekim fizikle aynı, tam koruma yok).
Buharın kendisi görseldir (sunucu `sendParticles` + istemci); dedicated log'unda "buhar, krater yok".


**M-28 `meteor dusur` anında çarpma bildirimi (dedicated bulgusu).** Komut, çağrıdan önce hedef chunk yüklü ve tavan boş
mu bakar; API boş döndüyse ve bunlar sağlanıyorduysa çarpma hemen olmuştur ("struck ... at once"), suya düşüş de dahil.
Önce "bekleyen krater artışı" bakılıyordu: varlık işlemeyen chunk'ta suya düşen meteor "No meteor" yazıyordu.


### Tur 3 (inceleme-meteor-termostat-2 Md1…Md3)

**M-29 Krater bitişi de yüklü chunk bekler (Md1).** Kenar bitince bitiş (yangın, ödül bırakma, `MeteorCarptiEvent`) önce
ödül karesinin (merkez sütunu; parça varsa merkezden ±⌈0,6 r⌉) değdiği bütün chunk'ları `MeteorYoneticisi.yuklu` ile sorar;
biri yüklü değilse iş bekleyende kalır (`bekliyor`), chunk yüklenince biter. `tabanNoktasi`'nın `getBlockState`'i artık
senkron chunk yüklemez. Yangın alanı kendi `isLoaded` denetimini zaten yapıyor. GameTest: bekleyen krater testine bitiş
aşaması eklendi (merkez chunk'ı yüklü değilken ödül düşmez, yüklenince çekirdek + parça düşer); test sayısı değişmedi.

**M-30 GameTest'ler yalnız kendi sahnelerinin krater işlerini sayar (Md2).** Krater işleri kalıcı olduğu için (M-25,
K-C24) boyut geneli `bekleyenKraterSayisi == 0` yerine merkezi sahne kutusu (+4) içindeki işler sayılır; sahne kurulurken
(`zemin`) aynı kutudaki eski işler atılır (`MeteorYoneticisi.kraterleriUnut`). Üretim yolu değişmedi.

**M-23 metni düzeltildi (Md3):** parlama dünyanın üstünde, elin ve GUI'nin altında.


### Tur 4 (doğrulama-2: aralıklı GameTest)

**M-31 Sıcak parçanın su/kar/buz soğuması her tick denetlenir; yakınlık yakması 5 tick'te bir kalır.** Kök neden:
`sicakParcaYakarYirmiSaniyeSonraYakmaz`'da su hücresinin ortasında (y +0,5) 0,25 yukarı hızla doğan parça vanilla
`ItemEntity` fiziğiyle yalnız ilk iki tick suya değer (`wasTouchingWater` `baseTick`'te güncellenir; y 5,5 → 5,75 → 5,99,
su yüzeyi 5,889), sonra ~0,6 blok seker; rastgele yatay hızıyla (±0,04) ~%31 olasılıkla 1×1 havuzun taş kenarına düşer ve
bir daha suya değmez. Denetim `gameTime % 5 == 0` tick'lerinde çalıştığı için iki tick'lik pencereyi 5'te 3 kaçırıyordu:
başarısızlık ≈ 0,31 × 0,6 ≈ %19, evre testin başladığı oyun tick'ine (batch sırasına) bağlı (benzetim: 200 000 atış).
Test hatası değil, üretim açığıydı: suya kısa değip kıyıya seken parça sıcak kalıyordu (tasarım "su üstünde hemen
soğur"). Düzeltme: `sicaklariIsle` her tick (süre, su, kar/buz; takip listesi küçük, tick başına liste boyu kadar
`isInWater` + iki blok okuması), oyuncu taraması (`getEntitiesOfClass`) yine 5 tick'te bir. Test güçlendi: "suya düşen parça
hemen soğumalı" denetimi 40. tick'ten 5. tick'e çekildi (eski kod bu sınırda %60 düşerdi; yeni kod belirlenimci).
Reddedilen: testte parçayı ortalamak/hızı sıfırlamak ya da `succeedWhen` ile beklemek (üretim açığını gizler).

### Reddedilen yollar

- **Meteoru kaydetmek (`shouldBeSaved`)**: yarım çarpma ve afet `yuklendi`'de ikinci yağmurla çift meteor riski (M-05).
- **Fizikli (yerçekimli) düşüş**: istemci tahmini ve iniş diski hesabı karmaşıklaşır; 3 sn'lik uçuşta fark görünmez.
- **`ItemEntity.setUnlimitedLifetime` + kendi ömür sayacımız**: Forge'un `getEntityLifespan` kancası aynı işi kayıt
  biçimiyle birlikte (yeniden açılışta yaş korunur) yapıyor.
- **Özel `ItemEntity` alt sınıfı (MeteoritParcasi varlığı)**: başka modların eşya mıknatısı/sıralayıcısı vanilla
  `ItemEntity` bekler; davranış NBT + olay + yönetici listesiyle yeterli.
- **Krateri `Explosion` ile açmak**: eşya düşürür, sert blok etiketini tanımaz, şekli kontrol edilemez, tek tick'te.
- **Loot table / Global Loot Modifier ile ödül**: madde 13c ödülü meteora bağlar; tablo veri paketiyle başka kaynaklara
  sızabilir.


## Faz 3 — Balıkçılık

Parça `balikcilik` (dal `is/balikcilik`, son commit `03e30aa`; `main`'e birleştirme `5f98daf`). Kaynak `.vibe/kararlar/balikcilik.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) ve entegratöre notlar (§E) o dosyada kalır; metindeki §A/§E göndermeleri o dosyaya aittir.
Şef kararları (B-19, B-20) "Şef kararları (Faz 3)" bölümündedir; N17 satırları sondaki tablodadır. Rapor `.vibe/rapor/balikcilik.md`.

**B-01 Kod düzeni.** Saf mantık `balikcilik.mantik` (JUnit, 5 sınıf 27 test): `Aktiflik`, `DelikOmru` (ömür, aşama
 gecikmesi, chunk sınırı), `IsirmaSuresi`, `LootAgirliklari`, `HareketsizlikSogumasi`. Minecraft tarafı: blok, açma,
olta/soğuma izleyicisi, GLM, loot girdisi, eşya, config, çeviriler, datagen, GameTest.

**B-02 Aktiflik.** `buzYuzeyi && (G >= eşik || kızılCüce)` (tur 2, Ba7: önceki sürümdeki kış ağırlığı dalı sonucu hiç
 değiştirmiyordu, kaldırıldı; `DonmaApi.kisAgirligi` artık sorulmaz). `buzYuzeyi` =
`DonmaApi.buzYuzeyiMi` (buz ya da ince buz, altında su; donma §A.1). Kızıl cüce = `cozulmeDonemi()` ya da evre
`KIZIL_CUCE`. Madde 15 "kızıl cüce döneminde global ilerleme düşse bile kalıcı kış bölgelerinde ve hâlâ donmuş su
yüzeylerinde": iki sayım aynı koşula iner (kış bölgesinde de delik için su üstü buz gerekir, buz varsa yeter). Süpernovadan
önce (G < eşik, kızıl cüce yok) doğal buz bile açılmaz ("süpernova sonrası"). G global değerdir (madde 15 "donma
ilerlemesi eşiği"); sütun hedefi kullanılmaz. Delik var olduğu sürece balık tutulur (aktiflik yalnız açmada sorulur).

**B-03 Isırma süresi: vanilla `FishingHook` + olay + yansıma.** Delikte olta vanilla gibi yüzer (bloğun su sıvı durumu).
`EntityJoinLevelEvent` ile oltalar izlenir; sunucu tick sonunda olta delikteyse vanilla `timeUntilLured` her tick bizim
kalan beklememize yazılır; bekleme bitince `timeUntilLured = 0`, `timeUntilHooked = 40` (`IsirmaSuresi.YAKLASMA_TICK`) ve
balık açısı yazılır: vanilla yaklaşma, şapırtı, ısırma ve çekme aynen çalışır. Isırma kaçırılırsa (vanilla iki sayacı
0'lar) yeni bekleme çekilir. Toplam süre `uniform[30, 60] sn − Lure × 5 sn`, taban 10 sn; Lure seviyesi oltanın kendi
`lureSpeed` alanından. Alanlar Forge `ObfuscationReflectionHelper` ile SRG adlarıyla (`f_37089_` nibble, `f_37090_`
timeUntilLured, `f_37091_` timeUntilHooked, `f_37092_` fishAngle, `f_37097_` lureSpeed; `mcp_config 1.20.1-20230612`):
geliştirmede resmî ada çevrilir, üretimde SRG'dir. Görev metnindeki sıra (önce olay/tick, sonra alt sınıf, en son mixin) izlendi:
`ItemFishedEvent` ısırma süresini değiştiremez (yalnız çekmede gelir), `RightClickItem` atanı değil yeri bilir; alt sınıf
(§D) gereksiz çıktı; mixin gerekmedi. `build.gradle` (çekirdeğin) erişim dönüştürücü satırı istemedi.

**B-04 Loot: GLM + kendi tablolar + kategori girdisi.** Forge `ItemFishedEvent.getDrops()` "loot'u değiştirmek için
LootTables kullanın" der ve vanilla yalnız kendi listesini doğurur; GLM vanilla tablonun sonucunu değiştirir, vanilla
eşya varlığını, XP'yi, istatistiği ve ölçütü bizim eşyalarımızla üretir. Koşul: `loot_table_id = minecraft:gameplay/fishing`
+ kod içinde `THIS_ENTITY` olta ve `delikteMi`. "Oranlar config" (görev metni, N17) ile "veri paketiyle genişler" (N12)
bir arada olsun diye tablo tek özel girdidir (`balikcilik_kategorisi`): kategori config ağırlığından, içerik kategori
tablosundan. Şans vanilla "quality" gibi: seviye başına çöp −2, hazine +1. Varsayılan balık %92 / çöp %7 / hazine %1
(vanilla 85/10/5): madde 15 "kayıp/çöp şansı düşük, ödül iyi", "hazine çok nadir". Meteor eşyası hiçbir tabloda yok
(madde 13c); GameTest 1000 çekilişte denetler. Vanilla açık su balıkçılığı değişmez.

**B-05 Delik bloğu: sıvı durumu ve çarpışma.** Blok `getFluidState` ile kaynak su döner ama `LiquidBlockContainer` /
`BucketPickup` değildir: su yayılmaz (sıvı tick'i zamanlanmaz, komşu su bu bloka akamaz), kova ve sünger alamaz, vanilla
sıvı çizicisi biyom renkli su yüzeyini çizer, olta yüzer (vanilla "su" denetimi). Su modelde çizilmez. Waterlogged
(`SimpleWaterloggedBlock`) reddedildi: kova suyu alır ve blok buz gibi kalırdı. Çarpışma: oyuncuya tam blok (üstünde
yürünür, buzun altına düşülmez), diğer varlıklara boş (olta 1×1 açıklığa düşer ve komşu buz blokları onu sütunda
tutar; 4 piksellik çerçeve çarpışması oltanın 0,5 bloklu delikten geçmesini zorlaştırıyordu). Kar katmanı deliğin
üstünde duramaz (desteği boş). Görüntü şekli boş, gölgelendirme 1.

**B-06 Yeniden donma: zamanlanmış blok tick'i, BlockEntity yok.** Açılışta ömür `uniform[240, 300] sn`; ilk tick
`ömür − 3 × aşama`, sonra 3 aşama × 15 sn (en kısa ömrün dörtte birini aşmaz; deliğin kendi ömrüne bağlı değil, blok
ömrü saklamaz). Zamanlanmış tick chunk'la kaydedilir: sunucu kapanıp açılınca da donar; yüklü olmayan chunk'ta
bekler. Son aşamadan sonra **eski buz** (`ince` → donmanın ince buzu, yoksa vanilla buz) gelir; donma tablosuna girmez
(donma §A.5). **Donma biti korunur:** delik donmanın sütun bitine dokunmaz; donma süreci üstü su ya da buz olmayan
sütunu kara sayar ve `buz_deligi` kar zemini etiketinde olmadığı için bir şey yapmaz; delik donunca aynı blok (ve bit)
geri gelir. Delik açıkken o sütunda çözülme/donma adımı atlanır (bilinen sınır: atlama serbestken donma bir sonraki
eşik geçişinde yetişir, K-D8). Yeniden donma `afetBlokHasari`'na bağlı değildir: oyuncunun açtığı deliğin kapanmasıdır.

**B-07 Chunk sınırı: palet taraması, sayaç yok.** Açma anında chunk bölümlerinden yalnız paletinde `buz_deligi`
olabilecekler (`maybeHas`) taranır. Delik açma nadir bir oyuncu eylemi; SavedData/capability sayacı kayıt, senkron
ve kaçak (kırılan, patlayan, `/setblock`) delik muhasebesi isterdi. Varsayılan sınır 4.

**B-08 Açma koşulları.** `PlayerInteractEvent.RightClickBlock`, yalnız sunucuda: `isSecondaryUseActive` (eğilme), alet
(`ToolActions` kazma/balta: modlu aletler de), blok vanilla buz ya da donmanın ince buzu, üstü boş, `aktifMi`. İstemci
paketi her durumda yollar; kol sallama sunucunun `SUCCESS` sonucundan gelir (istemcide tahmin edilmez: eşik/su bilgisi
yalnız sunucuda). Paketlenmiş/mavi buz kabul edilmez (madde 15 "suyun üstündeki buz"; donmanın tablosu da su → buz).
Alet `hurtAndBreak(5)` (Unbreaking ve yaratıcı vanilla kuralıyla). Yalnız chunk sınırında eylem çubuğuna tek satır
("Bu bölgede çok fazla açık buz deliği var"): afet uyarısı değil, oyuncunun kendi eyleminin geri bildirimi (N6). Eşik
altında sessizce bir şey olmaz.

**B-09 Hareketsizlik soğuması: `ortamKaynagiEkle`.** Sıcaklık `Etkinlik.DURMA` zaten metabolizmayı düşürür; madde 15
"daha hızlı soğutsun" için ek: oltası delikte, buz (vanilla `#ice`, ince buz dahil) ya da delik üstünde ve saniyede
0,25 bloktan az yatay hareket eden oyuncunun sayaç saniyesi artar; güç `−min(1, s/20) × 8 °C`. Kaynak saniyede bir, gövde
merkezinde (ayak + 1; sıcaklık kaynakları orada ölçer), yarıçap 1,5, süre 21 tick. 40 tick'ten uzun ara sayacı
sıfırlar. `VucutSicakligi.etkiEkle` reddedildi: doğrudan vücuda yazar, yalıtımdan geçmez; madde 15 "kalın giyinmek
zorunda kalsın" ortam kaynağıyla sağlanır. Bilinen sınır: bir blok yanındaki başka oyuncu ~%11 pay alır. Hareketsizlik
sıcaklık §A'da ayrı bir kanca olarak yok; ek öneri gerekmedi.

**B-10 Datagen: benzersiz sağlayıcı adları, vanilla blok etiketine yazılmaz.** Vanilla `LootTableProvider` ve
`RecipeProvider`'ın `getName()`'i final ve aynı adlı ikinci sağlayıcıyı `DataGenerator` reddeder (K-E5): `Adli` sarıcısı
ad verir ("Balıkçılık loot tabloları", "Balıkçılık tarifleri"); diğerleri `getName()` üzerine yazar. İlk denemede
`mineable/pickaxe`'e deliği yazan ikinci blok etiketi sağlayıcısı donmanın aynı dosyadaki `ince_buz` satırını sildi (aynı
yola yazan son sağlayıcı kazanır); sağlayıcı kaldırıldı, dosya geri yüklendi (delik buz kadar kırılgan, etiket
gerekmez). Bu bulgu K-E12'nin (main `254fe2d`) gerekçesiyle aynıdır: kendi sağlayıcımız yalnız `dogalafetler:*` etiketlerini
(`cig_kutup_baligi`, `pismis_kutup_baligi`) yazar; `#minecraft:fishes` ilk sürümde yazılıyordu, K-E12 gelince kaldırıldı
(§A.2). `data/forge/loot_modifiers/global_loot_modifiers.json`'u yazan tek sağlayıcı bizimki (`replace: false`); başka
bir dal GLM eklerse entegratör tek sağlayıcıda birleştirmeli (aynı dosya, aynı sorun).

**B-11 Yiyecek değerleri.** Sıcaklık sayıları B-19 ile değişti (tur 1'in −18/+18/+25 config anahtarları kaldırıldı).
Doyuruculuk kod sabiti (kayıt anında config yüklü değil): çiğ 2/0,1 (ringa 3/0,2), pişmiş 5/0,6 … 7/0,9 (vanilla
pişmiş morina 5/0,6, somon 6/0,8). Yığın 16, yeme 32 tick (madde 14). Tooltip'ler `appendHoverText`.

**B-12 Buz Parçası.** Madde 15 çöpü "Buz Parçası" vanilla'da yok: `dogalafetler:buz_parcasi` eşyası; 4 tane → 1 buz
(şekilsiz) ile çöp bir işe yarar.

**B-13 Pişirme tarifleri.** Vanilla balıklarıyla aynı: ocak 200 tick, tütsüleme 100, kamp ateşi 600, XP 0,35.

**B-14 Yaratıcı sekmeler vanilla.** Balıklar "Yiyecek ve İçecekler", buz parçası "Malzemeler"; modun kendi sekmesi yok.

**B-15 Görsel.** Model: 3 pikselik buz halka (`buz_deligi_kenar`, ~%85 opak), dış yüzler komşu buza karşı cullface;
aşama 1–3'te 10×10 açıklığın üstünde (y 14,6; su yüzeyi 14,2) buz zarı. Parçacıklar sunucunun yolladığı vanilla
parçacıklar (açılış: buz kırıntısı, sıçrama, kar tanesi; aşamalar: kar tanesi; kapanış: buz kırıntısı) ve bloğun
`animateTick`'i (açıkken kabarcık patlaması, donarken kar tanesi). Yeni parçacık tipi ve istemci kaydı gerekmedi; istemci
paketi yalnız otomasyondur. Dokular `tools/assets/balikcilik_dokular.py` (Pillow, sabit tohum).

**B-16 Görsel kanıt otomasyonu.** `client.balikcilik.BalikcilikOtomasyonu` (`-Ddogalafetler.otomasyon=balikcilik`), betik
`tools/senaryo/balikcilik_gorsel.py` (K0-21 rev. 2, ws 94, (k) kilit). Sahne komutla kurulur (G 60, 7×7 donmuş gölet);
delik gerçek Forge olayıyla açılır, olta sunucuda oluşturulup deliğe bırakılır (fare/klavye yok), gerçek ısırma beklenir,
`retrieve` ile çekilir. Tooltip karesi otomasyonun çizdiği katmanda gerçek `GuiGraphics.renderTooltip` ile (imleç oynatılamaz).


**B-17 `sifirla` (K-E11).** Bağımlı kalıcı durum yok: açık delikler blok durumu + zamanlanmış tick, chunk sayacı yok
(B-07), oltaların beklemesi ve hareketsizlik sayaçları bellekte. `DonemDegistiEvent(SIFIRLAMA)` dinleyicisi
(`BuzBalikciligi.donemDegisti`) hareketsizlik sayaçlarını siler ve delikteki oltalara yeni bekleme çektirir. Açık
delikler sıfırlamada silinmez: oyuncunun dünyasındaki bir blok değişikliğidir ve en çok 5 dk içinde kendiliğinden
donar (eski buz geri gelir; donma `sifirla` sonrası kendi buzunu bitle çözer, K-D11).


**B-18 Görsel koşu 1 bulgusu: çekilen eşya delikte kalıyordu.** Koşu 1'de gerçek ısırma 46,8 sn'de geldi, çekme olta
hasarı 1 verdi ama `BalikTutulduEvent` yayınlanmadı ve eşya envantere gelmedi. Kök neden: vanilla ısırmada şamandırayı aşağı
çeker, olta deliğin altındaki su bloğuna iner (`delikteMi` yalnız oltanın bloğuna bakıyordu); ayrıca vanilla eşyayı olta
konumunda doğurup oyuncuya fırlatır, deliğin komşuları buz olduğu için eşya duvara çarpıp suda kalır. Düzeltme:
`delikKonumu` oltanın bloğundan yukarı en çok iki su bloğu boyunca deliği arar (GLM ve olay aynı sorguyu kullanır);
`ItemFishedEvent`'ten hemen sonra aynı tick'te doğan, delik sütunundaki `ItemEntity` deliğin üstüne alınır ve vanilla
formülüyle oyuncuya yeniden fırlatılır. GameTest `deligeAtilanOltaBuzDeligiLootunuVerir` iki durumu da denetler; koşu 2'de
olay `[1 buz_alabaligi] (balık 1)` ve eşya elde.

**B-19 Balık sıcaklığı = yiyecek kademeleri (tur 2, Ba5, şef kararı).** → Şef kararları (Faz 3).

**B-20 Yeniden donan delik ve donma biti (tur 2, Ba2; tur 3, Bc2, şef kararı).** → Şef kararları (Faz 3).

**B-21 Pişmiş dokular türün rengini ve siluetini korur (tur 2, Ba6).** Pişmişler artık ortak kahveye karışmaz: her
türün kendi et rengi (alabalık pembe-turuncu + koyu benek, morina krem beyaz pul pul, somon koyu turuncu + açık yağ
çizgileri, kefal gri-kahve çizgili, ringa altın), ızgara izi hafifletildi (%35, dört adımda bir). Siluet türe göre:
kuyruk (alabalık ve morina düz, kefal yuvarlak, ringa çatal, somon derin çatal), sırt yüzgeci, gövde genişliği (2,2–3,3),
morinada çene bıyığı. Ölçüm (`balikcilik_dokular.py` sonunda yazdırır; ortak opak piksellerde ortalama |dR|+|dG|+|dB|):
pişmiş çiftler 76,5–215,2 (tur 1: 16,9–47), siluet farkı 9–30 px (tur 1: 1–15). Görüntü değerlendiricisi pişmiş ayırtı
3/4 (tur 1: 2/4). Karşılaştırma kâğıdı `.vibe/kanit/balikcilik/baliklar-tur2.png`.

**B-22 Testler güçlendirildi (tur 2, Ba3, Ba4, Ba7) ve vitrin notu (Ba8).**
- Ba3: `delikGercekZamanlanmisTickleYenidenDonar` aşama 1'de konan deliği gerçek zamanlanmış tick'lerle 1 → 2 → 3 → buz
  izler; her geçişte yeniden zamanlanmış tick ve aralık = aşama süresi (300 ± 2 tick) denetlenir ve log'lanır
  (`[balikcilik] test: delik aşaması ...`). Açılıştan aşama 1'e ilk gecikme ≥ 3900 tick olduğu ve config değiştirmek kalıcı
  GameTest dünyasının config dosyasına yazılacağı için 0 → 1 adımı (aynı `tick` kodu) burada koşulmaz; doğrudan `tick`
  çağıran test de kalır.
- Ba4: `acikSudaVanillaLootDegismez`: buzsuz havuzda zorlanmış ısırma; `ItemFishedEvent` sonucunda `dogalafetler:` eşyası yok,
  `BalikTutulduEvent` yok. Kendi batch'inde (`balikcilik_acik_su`) donma 0: ilk iki koşuda `balikcilik` batch'inin donma
  %50'si gökyüzüne açık havuz yüzeyini buza çevirdi, şamandıra buzun içinde kaldı (log: `sıvı=EmptyFluid`). GLM'nin
  kararı donmaya bağlı değil (`delikteMi`), test kapsamı değişmez.
- Ba7: meteor totolojisi yerine varsayılan oranların tam değerleri ve şans 3 oranları (JUnit); `Aktiflik`'in ölü kış dalı
  kaldırıldı (B-02); GameTest bekleme alt sınırı config'ten: `kalan >= enAz − (atıştan geçen tick) − 1` (önce 200 tick pay).
- Ba8: kareler işlev kanıtı olarak kabul (şef); vitrin anı (gün batımı, alçak açı ve ısırma anı, elde Donmuş Ringa) Faz 5
  görsel turunda çekilir. Tur 2'de runClient koşulmadı.

### Reddedilen yollar

- **Kendi `FishingHook` alt sınıfı:** atarken deliğe gideceği bilinmez; tüm olta kullanımlarını (`RightClickItem`) ele
  geçirip alt sınıf doğurmak başka modların oltalarını/büyülerini ve vanilla istemci tahminini bozar.
- **Mixin (`catchingFish`):** olay + tick + yansıma yettiği için gerekmedi (GÖREV §3 "mixin yalnız event yetmiyorsa").
- **`ItemFishedEvent`'i iptal edip kendi eşyalarını doğurmak:** istatistik, XP ve `fishing_rod_hooked` ölçütünü elle
  tekrarlamak gerekirdi; Forge belgesi GLM'yi önerir.
- **Sabit ağırlıklı tek loot tablosu:** oranlar config'ten gelemezdi. **Kategori seçimini GLM içinde yapmak:** tablo veri
  paketinde görünmez olurdu.
- **Waterlogged buz bloğu:** kova suyu alır, blok buz gibi kalır, sıvı tick'leri başlar.
- **4 piksellik çerçeve çarpışması herkes için:** olta 0,5 bloklu açıklığa zor girer; 3 piksel oyuncuyu (0,6) düşürür.
- **SavedData delik sayacı:** kırılan/patlayan/`setblock` deliklerde sapar; palet taraması durumsuz.
- **BlockEntity ile ömür:** zamanlanmış tick aynı işi kayıtlı ve ucuz yapar.
- **`VucutSicakligi.etkiEkle` ile hareketsizlik soğuması:** yalıtımı atlar (B-09).
- **`mineable/pickaxe`'e delik:** donmanın satırını siliyordu (B-10).


## Faz 3 — Başarımlar

Parça `basarim` (dal `is/basarim`, son commit `85b77e2`; `main`'e birleştirme `d23653b`). Kaynak `.vibe/kararlar/basarim.md` §B/§C/§D;
diğer işçilerin kullandığı arayüz sözleşmeleri (§A) ve entegratöre notlar (§E) o dosyada kalır; metindeki §A/§E göndermeleri o dosyaya aittir.
Şef kararları (A-29, A-30, A-31, A-35) "Şef kararları (Faz 3)" bölümündedir; N17 satırları sondaki tablodadır. Rapor `.vibe/rapor/basarim.md`.

**A-01 Dört genel ölçüt sınıfı, kayıt ortak kurulumda.** 15 tetikleyici `BasitOlcut` (alansız), `EsikOlcutu` (sayısal eşik,
`kesin` bayrağı), `EsyaOlcutu` (`ResourceLocation`) ve `AfetOlcutu` ile; hepsi `SimpleCriterionTrigger` (oyuncu başına dinleyici).
Nesneler `BasarimOlcutleri`'nde `static final` (yan etkisiz); vanilla `CriteriaTriggers.register` `FMLCommonSetupEvent.enqueueWork`
içinde (kayıt haritası düz `HashMap`, mod kurucuları paralel). Datagen yalnız örneği serileştirir, kayıt gerekmez.

**A-02 Şartnamenin sayıları JSON'da.** 5 tüy, 4 yünlü, 25 balık, 3 oyuncu, %50 donma, 3 gün, 10 sn ve seviye 3/5 advancement
JSON'unun ölçüt alanlarındadır (veri paketiyle değişir, N12 ruhu). Kodda yalnız tanımı gereği sabit olanlar kaldı:
`AfetIzi.YANGIN_SANIYESI = 30` (madde 12 #5), sinkhole düşüşü 4 blok ve yangın yakınlığı 8 blok (yedek denetim, §C).

**A-03 Oyuncu verisi `getPersistentData()` + `PlayerEvent.Clone`.** `dogalafetler_basarim` bileşiği (sıcaklığın `VucutSicakligi`
deseni): atlatılan türler, balık toplamı, ünvan, kutup sayacı, deniz ölüm döngüsü, süren afetin izi. Capability yerine: veri yalnız
sunucuda okunur, istemciye gitmez, &lt; 200 bayttır, oyuncu dosyasıyla kaydedilir; ölümde ve End dönüşünde `Clone` ile kopyalanır.
Kısa ömürlü durum (yıldızlı gece sayacı, çorba takibi, yangın saniyesi tekilliği) bellekte, çıkışta silinir.

**A-04 "Sağ çıkan" türetimi.** `SagCikanTuretimi`: doğal olmayan bitişte boş; olaydaki `sagCikanlar ∩ etkilenenler`;
`basarim.etkiAlaniPayi` (vars. 0) > 0 ise başarım kendisi de izler: AKTIF aşamada saniyede bir etki alanı + pay içindeki oyuncu
izine `alanda`, afet sürerken ölünce `oldu` yazılır; bitişte `alanda ∧ ¬oldu ∧ çevrim içi` olanlar da sağ çıkandır. Madde 12'deki
"afet yarıçapında, config'den ayarlanabilir" yarıçapın kendisi afetin seviye tablosundadır (Faz 4); pay başarıma özgü ek alandır.

**A-05 XP: JSON varsayılanı + config farkı.** `rewards.experience` görev 15, hedef 40, meydan okuma 100 (madde 12 "küçük"; 100 XP
≈ 7 seviye). Config `basarim.odul.*Xp` farklıysa fark `AdvancementEarnEvent`'te `giveExperiencePoints` ile eklenir (negatifse geri
alınır; config en az 0 olduğu için net XP ≥ 0). Böylece toast/ödül vanilla kalır, config yine etkilidir. Veri paketi JSON XP'sini
değiştirirse fark kod varsayılanına göre hesaplanır (bilinen sınır).

**A-06 Hedef ödülü loot tablosu, yiyecek datagen anında çözülür.** `dogalafetler:rewards/hedef`: 1 havuz, 1–2 adet
(`set_count` uniform). Yiyecek `dogalafetler:sicak_salep` datagen anında kayıtlıysa o, değilse `minecraft:baked_potato` (vanilla
pişmiş yiyecek, madde 14'e göre hafif ısıtır). Kayıtsız eşya adı loot tablosunu yüklenemez yapar; bu dalda yiyecek dalı yok.
Yiyecek dalı birleşince `runData` Sıcak Salep'i yazar.

**A-07 Mod eşyası ikonları için vanilla yedek.** İkon kayıtsız eşyaysa advancement yüklenmez; datagen anında kayıtlı değilse
yedek yazılır (Meteorit Parçası → ateş topu, Kozmik Çekirdek → End kristali, Hava Radarı → pusula, Sismograf → sculk sensörü,
Sıcak Tavuk Çorbası → mantar çorbası). Birleşmeden sonra `runData` gerçek ikonları yazar.

**A-08 Kutup Sığınağı sayacı.** Saat oyun gününün tabanı `getGameTime() + uykuBirikimi` (K-C42: uyku sayılır, `/time set`
saymaz). Saniyede bir, oyuncu canlıyken ve dünya donma %100'ü görmüşken (G ≥ %100 ya da evre KIZIL_CUCE, "ulaştıktan sonra")
birikir; tek adımda en çok 1 gün + 200 tick (uyku atlaması). Ölüm sıfırlar; girişte son gözlem unutulur: çevrim dışı süre "hayatta
kalma" sayılmaz (yalnız oyunda geçirilen süre). Kızıl cüce gecikmesi ≥ 3 gün (gökyüzü G-kuralı) olduğundan doğuş sayacı bozmaz.
`sifirla` sonrası sıfırdan başlar (A-27).

**A-09 Kök: `SirenIstegiEvent`.** Görev metni `AfetAsamaDegistiEvent` SIREN'i öneriyordu; o olayda alıcı listesi yok. Siren isteği
tam alıcıları taşır (etki alanı + `sirenEkYaricap`, global afette herkes) ve siren sürerken alana giren/bağlanan oyuncuya yeniden
gelir: "ilk afet sirenini duymak" birebir.

**A-10 Çatı Altı "hiç hasar".** Tek kaynak asit yağmurunun `HASAR_ALDI` ipucudur (A-30, A-31, A-35): asit işçisi kayıt yaptıysa
yalnız asidin verdiği hasar bozar (zombi vuruşu bozmaz). Kayıt yoksa olay yok sayılır ve yedek
denetim: AKTIF afette etki alanındayken alınan her hasar (`LivingDamageEvent`, miktar > 0, LOWEST); bu yedekte çatı altındaki
oyuncu düşme hasarı alırsa da kaybeder (inceleme A3'ün yanlış negatifi; Faz 4 kaydıyla kapanır).

**A-11 Dalga Avcısı:** `SURUKLENDI` ipucu; yedek (A-31): AKTIF tsunamide etki alanında, her tick `SanalSu.itmeHesapla(o, …) != null`.
**A-12 Delikten Dönüş:** `CUKURA_DUSTU` ipucu; yedek: AKTIF sinkhole etki alanında `fallDistance ≥ 4` (ağaçtan atlayanı da
sayar, inceleme A3; Faz 4 kaydıyla kapanır). **A-13 Yanmadan Çıktım:** `YANGIN_ALANINDA` ipucunun her oyun saniyesi (saniyede en çok
bir) sayılır, toplam ≥ 30 (kesintisizlik şart değil: "en az 30 saniye kalıp"); yedek: AKTIF orman yangınında etki alanında ve 8
blok içinde yanan hücre olan her saniye, kendi sayacında (olay ve yedek saniyeleri toplanmaz).

**A-14 Çorba İçer Isınır (N16).** Başlangıç kademesi yemeye başlarken (`LivingEntityUseItemEvent.Start`) ölçülür: anında
ısınma `Finish`'ten önce eklenir ve oyuncuyu eşiğin üstüne çıkarabilir. "Donma durumu" = yalnız `Kademe.DONUYOR` (≤ −75, A-29;
`CorbaTakibi.donmaDurumunda`). Sıcak yiyecek = etiket `dogalafetler:basarim/sicak_yiyecek` ya da yedikten sonra taze pozitif
`SureliEtki` (`Finish`'te geçen ≤ 1 tick, saniyelik denetimde ≤ 60 tick: durum efekti ilk tick'inde ekliyorsa da yakalanır). "Etkisi
sürerken" = o etkinin süresi (etiketle tanınıp süresi görülmeyende 180 sn); bu sürede `Kademe.NOTR` görülünce bir kez tetiklenir.
Sıcaklık kapısı kapalıyken vücut hep nötr okunur: başarım alınamaz (bilinen sınır). Tur 1'deki `SOGUK` ve altı seçimi
(inceleme A1, P1) kaldırıldı: −31'de bir kızarmış patatesle alınabiliyordu.

**A-15 Buz deliği balığı.** `ItemFishedEvent` LOWEST (iptal edilmemiş): kancanın bloğundan bir aşağı ile üç yukarı arasında
`dogalafetler:buz_deligi` varsa (balıkçılık işçisi: ısırmada olta deliğin altındaki suya 1–2 blok iner, kanca bloğu o zaman su olur)
düşen eşyalardan yenebilir olanların adedi "balık" sayılır. Balıkçılığın kendi sayımı `#dogalafetler:cig_kutup_baligi` etiketidir; o
dalda yakalanan yenebilir eşyalar yalnız bu çiğ balıklardır (çöp: buz parçası, kar topu; hazine: kitap yenmez), sonuç aynıdır.
Toplam oyuncu verisinde; #20 `min 1`, #21 `min 25`. Balıkçılığın `BalikTutulduEvent`'i bu dalda yok (main'de de henüz yok),
derlenemez; birleşmede olay yolu tercih edilir (§E.3).

**A-16 Yerden alma `PlayerEvent.ItemPickupEvent`.** `EntityItemPickupEvent` envantere girmeden ve iptal edilebilir hâlde gelir; dolu
envanterde alınamayan eşya da sayılırdı. Pickup olayı eşya envantere girdikten sonra, alınan adetle gelir.

**A-17 Üretim `ItemCraftedEvent`** (sunucu tarafı, `ServerPlayer`); kimlik birebir: Gelişmiş Hava Radarı ve Hassas Sismograf ayrı
kimliktir, saymaz ("İlk Sismograf'ı craft et").

**A-18 Tüy upgrade'i:** sıcaklığın `TuyUpgrade`'i (NORMAL) sağ tık olayını iptal edip uygular; başarım dinleyicisi LOWEST ve
`receiveCanceled` ile sonra gelir ve yardımcı eldeki zırhın tüy sayısını verir. Son tüy harcanınca ana el boşalsa da sayılır.

**A-19 Buzul Çağı "geçsin" = kesin büyük** (`esik: 50`, > 50). Saniyelik denetim süreçteki küçük adımları, `DonemDegistiEvent(DONMA)`
komutla gelen ≥ 1 puanlık sıçramayı hemen yakalar (süreç her tick yazdığı için olay yolu daraltıldı). Canlı oyuncu şartı ("sen hayatta ol").

**A-20 Yükselen Sular:** tamamlanma anı `DenizSeviyesi.mevcutArtis ≥ hedefArtis − 0,001` (hedef > 0). Yükseliş sürerken
(0 < mevcut < hedef) ölen oyuncu o döngüde (`baslangicSaati`) hak kazanmaz; tamamlanma anında çevrim içi ve canlı olanlar kazanır.
Bekleme süresindeki (mevcut = 0) ölüm sayılmaz: yükseliş henüz yaşanmıyor.

**A-21 Süpernova Tanığı / Yıldızlı Gece:** tanımlar gökyüzünün (`Gokyuzu.supernovayiGoruyorMu`, `yildizliGeceMi`, N16). Yıldızlı gece
sayacı kesintisiz saniyedir (`ArdisikSayac`); koşul bozulunca, ölünce ve çıkınca sıfırlanır; eşik JSON'da `saniye: 10`.

**A-22 Kozmetik ünvan (N16).** Her meydan okumanın kendi ünvanı: Kutup Sığınağı → "Kutup Sakini", Kozmik Şans → "Kozmik Kâşif",
Felaket Ustası → "Felaket Ustası", Hepsini Atlattım → "Her Şeyi Atlatan"; en son kazanılan geçerlidir (OP komutla değiştirir).
`PlayerEvent.NameFormat` ve `TabListNameFormat` (LOW, yalnız `ServerPlayer`) altın `[ünvan] ` öneki ekler; ad bileşeni çevrilebilir,
her istemci kendi dilinde görür. Sohbet satırı, ölüm ve başarım duyuruları sunucunun görünen adını kullanır. `unvanGoster=false`
iken önek yok, veri korunur. Giriş, yeniden doğuş, ünvan değişimi ve config yeniden yüklemesinde `refreshDisplayName` +
`refreshTabListName`. Ünvan, başarımın ilerleme olayında verilir (A-32): meydan okumanın kendi sohbet duyurusu da önekli adı
taşır. İstemcideki baş üstü ad etiketi değişmez (N16 sohbet ve tab der).

**A-23 Sekme ağacı.** Ebeveynler yerleşimdir, ön koşul değildir (vanilla): bölgesel afetler (1–6), meteor (7 → 22), güneş zinciri
(8 → 9 → 10 → 11; 10 → 20 → 21), ay (12 → 13, 14), aletler (16 → 15), giyim (17 → 18 → 19), şiddet (23 → 24 → 25, 23 → 26).
Görünürlük vanilla: kazanılmış atadan en çok iki kat aşağısı görünür; #25 ve #26 `hidden=true` (açılana kadar görünmez).
Kök arka planı `textures/gui/advancements/backgrounds/felaket.png` (16×16, `tools/assets/basarim_ikon.py`).

**A-24 Tek veri sağlayıcısı.** Vanilla `AdvancementProvider.getName()` `final` "Advancements"; başka bir alt sistemin sağlayıcısıyla
`Duplicate provider` olurdu (K-E5). `BasarimVerileri` tek `DataProvider` ("Doğal Afetler başarımları"); JSON vanilla kurucularıyla
(`Advancement.Builder.serializeToJson`, `LootDataType.TABLE`) üretilir.

**A-25 Hepsini Atlattım / Birlikte Hayatta.** Küme sekiz türün hepsidir (seviyesiz güneş ve ay dahil), oyuncu başına kalıcı;
"Birlikte Hayatta" aynı afetin türetilmiş sağ çıkan sayısıdır (pay dahil), eşiği geçince bütün sağ çıkanlara.

**A-26 Doğrulama komutu `/dogalafet basarim bilgi [oyuncu]`** (N18 ruhunda, OP): yüklenen sekme başarımı sayısı (27/27), oyuncunun
kazandığı, atlattığı türler, balık toplamı, kutup günü, ünvan. Dedicated senaryo bununla advancement'ların yüklendiğini görür.

**A-27 `/dogalafet sifirla` (K-E11).** `DonemDegistiEvent(SIFIRLAMA)` geldiğinde başarım `DunyaDonemi.ekVeri("basarim")`'e yeni bir
döngü işareti yazar (`sifirlama` = `max(üst dünya getGameTime() + 1, önceki işaret + 1)`: sıfırlama ek veriyi boşaltır; işaret
sıfırdan farklı ve oturumda kesin artan, aynı tick'teki iki `sifirla` de ayrılır — ilk GameTest koşusu tam bu durumda düştü) ve
`ekVeriDegisti` çağırır. **Sıfırlanmayanlar** (oyuncu verisi, kalıcı ilerleme): kazanılmış advancement'lar,
"Hepsini Atlattım" kümesi, buz deliği balık toplamı, ünvan; Yıldızlı Gece 10 sn sayacı zaten bellektedir ve ay geri gelince
kendiliğinden kesilir. **Sıfırlananlar** (döneme bağlı ara sayaçlar): Kutup Sığınağı'nın "donma %100'den beri gün" birikimi
(oyuncu verisinde `kutupDongu` işaretle eşleşmezse 0 okunur; çevrim dışı oyuncu da ilk okumada sıfırlanır) ve `sonDonma`. Deniz
ölüm döngüsü (`DenizSeviyesi.baslangicSaati`) ve afet izi (afet numarası) zaten kendi döngü anahtarlarıyla ayrılır. GameTest
`kutupSiginagiUcGunSifirlaYalnizAraSayaciSifirlar` gerçek `AfetYoneticisi.sifirla()` ile sınar.

**A-28 Etiketler (K-E12).** Başarım vanilla (`minecraft:*`) etikete katkı vermez; tek etiketi `dogalafetler:basarim/sicak_yiyecek`
(eşya) kendi `DataProvider`'ında yazılır. `DataGenerators.vanillaBlokEtiketiEkle` kancası gerekmedi.

**A-29 Çorba başlangıcı `DONUYOR` (şef kararı, inceleme A1).** → Şef kararları (Faz 3).

**A-30 §6 #16: 27 başarımın her biri GameTest'te kazanılır (şef kararı, inceleme A2).** → Şef kararları (Faz 3).

**A-31 Tek kaynak: olay varsa yedek sayılmaz (şef kararı, inceleme A3; A-35 ile daraltıldı).** → Şef kararları (Faz 3).

**A-32 Ünvan ilerleme olayında (inceleme A6).** Forge `PlayerAdvancements.award` sırası: `AdvancementProgressEvent` → ödül →
sohbet duyurusu (`getDisplayName()`) → `AdvancementEarnEvent` (javap ile doğrulandı). Ünvan `AdvancementProgressEvent` (`GRANT`,
ilerleme tamam) dinleyicisinde verilir; meydan okumanın kendi duyurusu da `[Ünvan] Ad` gösterir. Config XP farkı
`AdvancementEarnEvent`'te kalır. GameTest seviye 5: kazanım olayındaki (duyurudan hemen sonra) ad önekli.

**A-33 Görsel otomasyon komutları (inceleme A4, A6).** Sahne komutları entegre sunucunun konsol kaynağından `withSuppressedOutput()`
ile verilir (`@s` oyuncu adına çevrilir): başarı ya da hata satırı sohbete düşmez; dördüncü `asama ileri` (afet zaten bitmişken)
kaldırıldı; kareden önce sohbet (`ChatComponent.clearMessages`, yiyecek Yi4) ve toast'lar temizlenir. Gizliler açıkken Felaket
Ustası kolu ortalanmış ek kare (`sekme_gizliler_acik_orta`): #25 üst kenarda kırpılmaz. Son `basarim bilgi` çıktısı sunucu log'una.

**A-34 Görsel otomasyonda kare kapısı ve iç imleç (koşu 2 → koşu 3; `bda69e0`, `3181496`).** Koşu 2'de iki toast karesi boş
çıktı: istemci o anlarda FPS 1 çiziyordu (görünmeyen workspace), `Screenshot.grab` bayat framebuffer'ı aldı; toast'lar sonraki
karede yarı kaymış görünüyordu. Toast üreten komuttan önce istemci en az 20 FPS çizmeli (şef alt sınırı 15; en çok 60 sn
beklenir, sonra uyarıyla devam) ve toast karesi adım başından beri en az 20 kare çizilip (`RenderTickEvent` sayacı) 25 tick geçince
alınır (toast kayması 600 ms, görünürlük 5 sn). Kurulumdaki `advancement revoke` yalnız kendi sekmemize (`from
dogalafetler:dogalafetler/root`) iner. **Koşu 3 (şefin onayladığı tek ek koşu) toast'ları doğruladı:** iki toast karesi FPS 57/60
ile alındı, kök toast'ı ve üç toast birlikte görünür. Sabit iç imleç bir widget'ın üstünde kalınca vanilla ipucu kutusu sekmeyi
karartıyordu (`sekme_erken`, `sekme_gizliler_acik_orta`); `bda69e0` `MouseHandler` konumunu (`f_91507_`/`f_91508_`,
`ObfuscationReflectionHelper`) sekme açılmadan sıfırlıyordu ve koşu 3'te ipucu kutusu kaldı: `Minecraft.setScreen` fare tutuluysa
`MouseHandler.releaseMouse` çağırır, o da konumu pencere ortasına geri yazar (javap ile doğrulandı). `3181496` konumu ekran açıldıktan
sonra sıfırlar; gerçek imlece ve sahibin oturumuna dokunulmaz, girdi sayıları etkilenmez. Üçüncü koşu yok (şef): `3181496` yalnız
`compileJava` ile denendi, ipucusuz sekme karesi Faz 5 görünür koşusuna kalır. Kare log'u artık önceki adımda çizilen kareyi yazar
(koşu 3'te adım başında sıfırlanmış sayacı yazıp hep 0 gösteriyordu).

**A-35 İnceleme-basarim-2 P2'leri (Bs1…Bs3, şef kararları).** → Şef kararları (Faz 3).

### Reddedilen yollar

- **Vanilla `inventory_changed` / `recipe_crafted` ölçütleri:** eşya yüklemi kayıtsız eşyada advancement'ı yüklenemez yapar
  (Faz 3 eşyaları paralel dallarda); kendi `esya_*` ölçütü kimliği yalnız karşılaştırır.
- **Capability:** istemci eşitlemesi ve kayıt kodu gerekmiyor; kalıcı veri küçük (A-03).
- **`ForgeAdvancementProvider` / `LootTableProvider`:** adları `final`, başka sağlayıcıyla çakışır (A-24).
- **Loot tablosunda sabit `sicak_salep`:** bu dalda eşya yok, tablo yüklenemezdi (A-06).
- **Ünvanı `rewards.function` ile vermek:** mcfunction oyuncu kalıcı verisine yazamaz, ad olaylarını tetikleyemez; olay yolu (A-22).
- **XP'yi tamamen koddan vermek (JSON 0):** advancement ekranındaki ödül bilgisini ve veri paketi düzenlenebilirliğini kaybettirir (A-05).
- **`AfetAsamaDegistiEvent` SIREN ile kök:** alıcı listesi yok, sonradan bağlanan oyuncu kaçar (A-09).
- **Çatı Altı için `dogalafetler:asit` hasar tipini beklemek:** Faz 4'e bağımlılık; "hiç hasar" harfiyen daha basit ve doğru (A-10).
- **Yünlü Kahraman için `LivingEquipmentChangeEvent`:** sahte oyuncu tick almadığı için olay gelmez, sınanamaz; saniyelik denetim
  hem gerçek hem sahte oyuncuda çalışır.

## Faz 3 — Entegrasyon

Entegratör (`entegrator` oturumu): `main` `254fe2d` üstüne beş dal `merge --no-ff` ile, sırayla yiyecek `4b725ec`, radar-sismograf
`e8d04f2`, meteor-termostat `6ab6357`, balıkçılık `5f98daf`, başarım `d23653b` (çakışmalar yalnız `DogalAfetler.java` kayıt satırı, iki
lang dosyası ve `docs/tasarim/esyalar.md` başlığı; lang `runData` ile doğrulandı). K-E13 ve K-E14 şefçe istendi ("Şef kararları
(Faz 3)"). Rapor `.vibe/rapor/entegrasyon-3.md`, kanıt `.vibe/kanit/entegrasyon-3/`.

**K-E15 Birleşik GameTest: üç test batch sırasından ve sahne yerleşiminden bağımsız.** Dört dalın birleştiği `main`'in ilk
koşusu 131'den 3 düştü (`.vibe/kanit/entegrasyon-3/gametest-1.log`); üçü de dalında geçiyordu. GameTest sunucusu batch'leri sırayla
koşar; sıra ve ızgara yerleşimi test kümesiyle değişir. Üç düzeltme denetimi koruyup testi sıradan ve yerleşimden bağımsız kılar:
(a) meteor `kraterIsiYukluDegilkenBeklerSonraTamamlanir` ("Expected Blackstone, got Magma Block"): aşama 1'in bitiş taklidi merkez
chunk'ının tamamını yüklü değil sayıyordu; sahne kökü kayınca magma hücresi merkezle aynı chunk'a düştü ve soğuması da
bloke oldu. Taklit magma hücresini dışarıda bırakır. (b) yiyecek `guvecSicaktaAsiriIsinmayaIter` ("sahne sıcak olmalı: ortam 5.79",
dalında 66–72): dönem global; iki batch önce `balikcilik_sinir` donmayı %60'ta bırakıyordu. Mutlak ortam ölçen test nötr
dönemi (donma 0, güneş normal) kendisi kurar, sonda eskisini geri koyar. (c) donma `simulasyonDisiGorunenChunkDonar` ("hedef chunk
simülasyon dışında olmalı"): hedef köke göre 40 chunk doğudaydı ve ızgara oraya uzanıyordu; hedef z = −40 chunk, ileti hedefi
yazar. Beklentiler ve üretim kodu değişmedi (`9389461`). Aynı testin "aşama 0" düşmesi için (c) yanlış teşhisti; ayrı kök
neden: z = −40 hedefi her koşuda üretilir, üretim duvar saatiyle, zaman aşımı tick'le (bkz. K-E21).

**K-E16 GameTest yerleşimi tek satırda; her koşu boş dünyayla başlar.** Başarım birleşince 156 testle iki test her koşuda düştü
(`gametest-5..7.log`): su `denizYukselmesiBolgeyiDegistirmezSifirIzBirakmaz` ("0 aralık", deniz +4) ve yangın
`blokHasariKapaliykenBlokDegismez` (sahneye doğudan akan gerçek su). Tanı iletileri ve dünya taraması
(`.vibe/kanit/entegrasyon-3/gametest-dunya-su-taramasi.txt`) iki kök neden verdi. (1) Vanilla `GameTestBatchRunner` satırı yalnız bir
batch'in 8. testinde kırar (sayaç batch'e yerel; `javap`) ve yeni satırın derinliğini yalnız o batch'in şablonlarından alır:
`balikcilik` (9 test, 1×1×1 `bos`) satırı kırınca ikinci satır z = 7'de başladı; aynı satırdaki `donma_alan` (9 derin) ve `donma_gol`
(32) sahnelerini ikinci satırın alan temizliği kesti ve donma gölünün suyu su sahnesinin kuzey duvarına aktı. `defaultBatch` (31) ve
`yangin_genel` (8) de satır kırıyordu. (2) Saklanan dünyada (K-C24) eski yerleşimlerin kutu dışında kalan donma gölleri (y −58…−56,
3113 kaynak blok) yeni yerleşimin kenar temizliğiyle açılıp yangın sahnesine akıyordu: K-E6'nın "kaynağı bulunmadı" suyu. Karar:
hiçbir batch 8 test taşımaz (balıkçılık 7 + 2, `yangin_genel` 7 + 1, `basarim_genel` 5 + 4, `donma_alan` 3 × 5, `defaultBatch` 7);
bekçi GameTest `CekirdekOyunTestleri.hicBirBatchSatirKirmaz` 8 ve üstü testli batch'i adıyla düşürür. `runGameTestServer` her
koşuda `run-gametest/oyuntesti-duz`'u siler (`build.gradle`; Gradle çıktısında satır). İki test tanı iletisi kazandı (yangın:
değişen bloklar ve köşe; su: aralık, köşe, deniz, alan kuyruğu, oyuncu); beklentiler değişmedi. Kanıt: `gametest-12/13/14.log`
"All 157 required tests passed" (`58251f3`). Reddedilen: düşen iki testi kendi kutusuna daraltmak (K-E6 gibi; sızıntıyı gizler,
kök neden kalır); GameTest yerleşimine mixin (üretim jar'ına test çatısı yaması); dünyayı saklayıp her test sonunda sahne
temizliği (satır çakışması koşu içinde de olur).

**K-E17 Faz 3 kancaları ve başarımın tek kaynakları.** (a) Balıklar `YiyecekAPI.uygula` ile (balıkçılık §E, inceleme Bc3):
azalan verim türü eşyanın kayıt kimliği (her balığın çiği ve pişmişi ayrı tür), çiğ `cigBalik` (küçük serinlik,
`yiyecek.balik.cigSaniye`), pişmiş `pismisBalik` (orta, `pismisSaniye`), pişmiş Donmuş Ringa `new YiyecekTanimi(kimlik, ISINMA,
BUYUK, 120, false)` (B-19'un 120 sn'si korunur); `mantik.BalikSicakligi` ve JUnit'i silindi. Balıklar artık durum efekti ve 60 sn
azalan verim alır. (b) K-E12'nin eşya sürümü: `DataGenerators.vanillaEsyaEtiketiEkle` + tek sağlayıcı `veri.OrtakEsyaEtiketleri`
("Ortak vanilla eşya etiketleri"); balıkçılık on balığı `#minecraft:fishes`'e ekler. (c) `#dogalafetler:kozmik_cekirdek` tek sabit
`MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI` (radarın kopyası silindi); radar tarif testi etiketin Kozmik Çekirdeği içermesini
şart koşar ve onunla gerçekten craft eder (boş etiket dalı artık regresyon). (d) Protokol `"5"` (K-C47, bir kez; radar iki, meteor
bir paket). (e) Başarım: balık sayımı tek kaynak `BalikTutulduEvent` (başarım §E.3; `ItemFishedEvent` kanca yolu, `DELIK_YUKARI`,
`BasarimKimlikleri.BUZ_DELIGI` silindi; `balikSayisi()` yalnız `#cig_kutup_baligi` sayar, A-15 ile aynı sonuç). `YiyecekYendiEvent`
dinlenmez: başarımın `Start`/`Finish` + taze ısınma etkisi yolu aynı yemeyi tanır (§E.4), ikinci abonelik aynı yemeğe ikinci
kaynak olurdu (abonelik eklenmemesi şef kabul, 2026-11-06). Zincir GameTest `corbaGercekYiyecekZinciriyleAcilir`: gerçek Ateşli Güveç `YiyecekAPI`'den yenir, yiyecek olayı bir kez
ve `oncekiKademe() == DONUYOR` gelir (yiyecek §A3 = A-29), #19 nötrde açılır. `MeteorCarptiEvent` istatistik ve afet koreografisi
içindir (meteor §A.2); #7 ve #22 eşya alımından, başarım dinlemez (şef kabul, 2026-11-06). `yayinciKaydet` Faz 3'te gerekmez: dört ipucu Faz 4
afetlerinindir (§A.3). (f) `runData`: #7, #15, #16, #19, #22 ikonları mod eşyaları, `rewards/hedef` Sıcak Salep (A-06, A-07).
(g) `DogalAfetler` kaydı `DonmaSistemi.kaydet`'ten sonra: `Yiyecek`, `EsyaKayitlari`, `Meteor`, `Balikcilik`, `Basarim`, sonra
`Kancalar.bagla()` (`68577b1`, `1e70a46`).

## Faz 4 — Güneş patlaması ve kızıl cüce

Parça `gunes-patlamasi-kizil-cuce` (dal `is/gunes-patlamasi-kizil-cuce`, son commit `84b2faf`; `main`'e birleştirme `ded08d7`, entegrasyon 4
1/6). Kaynak `.vibe/kararlar/gunes-patlamasi-kizil-cuce.md` §B/§C/§D; diğer işçilerin kullandığı arayüz sözleşmeleri (§A) ve
entegratöre notlar (§E) o dosyada kalır. Şef kararları (§E b, c; GP17 FPS) "Şef kararları (Faz 4)" bölümündedir; N17 satırları
sondaki tablodadır. Rapor `.vibe/rapor/gunes-patlamasi-kizil-cuce.md`, inceleme `.vibe/rapor/inceleme-gunes-patlamasi-kizil-cuce.md`
(kabul, P0/P1 = 0, P2 = 5), birleştirme `.vibe/rapor/entegrasyon-4.md`.

**GP1 Evre ilerlemesi: siren karesel 0 → 0,273, AKTIF doğrusal 0,273 → 1 (sıcaklık §A.8 sapması).** Gökyüzü güneşi
`1 + 2,2·p` ölçekle çizer (gokyuzu §2). Şartname "siren başladıktan sonra güneş yavaş yavaş büyüsün"; tasarım §2 siren
60 sn'de 1,0 → 1,6 kat (`1 + 0,6·τ²`). 5 günlük doğrusal 0 → 1 sirende yalnız 1,02 kat verirdi (görünmez). Bu yüzden siren
boyunca `p = (0,6/2,2)·τ²`, AKTIF'te `p = 0,273 + 0,727·t` (güneş 1,6 → 3,2 kat, sıçramasız). Sıcaklık aynı `p`'yi okur:
`gunesPatlamasiArtisi` = 18 ise siren sonu +4,9, 1. gün sonu +7,6, 5. gün +18; tasarımın "1. gün +6 → 5. gün +18" eğrisiyle
uyumlu. Sıcaklık §A.8'in "BÜYÜYEN boyunca 0 → 1 doğrusal" cümlesi buna göre güncellenmeli (§E). JUnit: monoton, sona doğru
hızlanan siren, siren sonu = AKTIF başı, 1,6 ve 3,2 kat; GameTest `sirendeGunesBuyurMeteorlarAktifteBaslarStopNormaleDondurur`.

**GP2 BÜYÜYEN'e geçiş SIREN başında.** Ön-uyarıda gök değişseydi eşyası olmayan oyuncu afeti önceden görürdü (madde 4,
N7: radarda bile görünmez). Buharlaşma (BÜYÜYEN'de başlar) bu yüzden meteorlardan 60 sn önce başlar; 5 günlük sürecin
%0,8'i, derinlik farkı ihmal edilir. Çekirdeğin taklidi BÜYÜYEN'i AKTIF'te yapıyordu; gerçek afet bundan ayrılır (taklit yalnız
yaşam döngüsü içindir).

**GP3 Meteor yoğunluğu dakikalık ardışık yağmurlarla; gerçek tick.** `MeteorAPI.yagmur` tek bir yoğunluk alır; eğri (1/40 sn →
1/12 sn, oyuncu başına) afetin (meteor §A.1). Her 1200 oyun tick'inde (gerçek tick, afet `asamaTick` sayısı) 1200 tick'lik
yeni bir yağmur o anın yoğunluğuyla başlar; yağmur dengeli dağıtımı ve giren/çıkan oyuncuyu kendisi yürütür (`oyuncular`
boş). `/dogalafet hiz` 5 günü sıkıştırır ama meteor yoğunluğu gerçek zamanda kalır (test aracı meteor yağdırmayı 1000 katına
çıkarmasın). Büyük meteor %5 (tasarım: "büyük %5, Kozmik Çekirdek yalnız burada"); API'de orta boy yok, tasarımın %25 "orta"sı
küçük sayılır. Yağmur kaydedilmez (M-05): kayıttan dönüşte `yuklendi` ilk tick'te yenisini başlatır.

**GP4 Alt aşama = gün; `asama ileri` bir gün atlar.** N18 "güneş patlamasının 5 gününü beklemeden". Gün gün atlamak
sıcaklık, kavrulma ve meteor yoğunluğunun her gününün ayrı sınanmasını sağlar; 5. günde (son alt aşama) `asama ileri` BITIS'e
geçer = doğal bitiş (süpernova, "Güneşi Atlattım"). Siren + 5 gün + bitiş = 7 komut; sonrası zincirin `asama ileri`'si.

**GP5 Başlama kapısı `yerUygunMu`'da.** Çekirdeğin tek seferlik kapısı (K-C6) rastgele seçimi korur, `/dogalafet start` korumaz.
Süpernova/güneşsiz/doğuş sürerken başlayan afet evreyi BÜYÜYEN yapıp zinciri ve donmayı bozardı; `yerUygunMu` false →
"uygun yer bulunamadı" (N14 yolu, ek çekirdek kodu yok). Tamamlanmış kızıl cücede elle başlar (test aracı; rastgele seçim
`gunesPatlamasiTekrarlanabilir`'e bağlı).

**GP6 Kavrulma: güneşe açık çimen → kaba toprak, 3. günden.** Tasarım §2 "çimenler 3. günden sonra rastgele kaba toprağa
dönüşür (yangın sistemi dışı, `afetBlokHasari` ile)". Her tick oyuncu başına bir deneme, olasılık 3. gün 0 → 5. gün 0,5 ×
TPS kısması, 64 blok yarıçap; 2 günde 128×128 alanın ~%50'si (yamalı). "Güneşe açık" = sütunun `MOTION_BLOCKING` yüksekliğinin
hemen altı (yaprak ve çatı gölgeler; yükseklik haritası eşzamanlı, gök ışığı gecikebilir). Her değişimde
`SanalSu.chunkDegisti`. GameTest `kavrulmaCimeniKabaToprakYaparBlokHasariKapaliykenDokunmaz`. Kalıcı izdir (krater gibi);
`stop` geri almaz.

**GP7 Taklitle çakışma (K-E22 ile değişti).** Tur 1'de gerçek kayıt `ozelliktenKaydet`'ten sonraydı ve taklit kayıtlıysa gerçek afet
atlanıyordu. K-E22 (şef kararı) sırayı çevirdi: gerçek kayıt önce, taklit kayıtlı türü atlar ve güneş türü atlanınca
`OyunTestiSureci` de kaydedilmez, `GunesZinciriSureci` her zaman kayıtlıdır. Bu daldaki atlama dalı ölü kod olduğu için kaldırıldı;
senaryonun alt sınıfı (`GercekAfetSunucusu`) da kalktı, betik ortak `Sunucu`'yu kullanır. Sonuç: `-PoyunTestiAfeti=...,gunes_patlamasi`
taşıyan eski senaryolar artık gerçek afeti koşar (§E'de kırılan adımlar).

**GP8 Kalıcılık.** NBT `aktifSure` (başlarken config'ten sabitlenir; süreç ortasında config değişse de aynı kalır), `aktifGecen`.
Evre ve ilerleme `DunyaDonemi`'nde. GameTest `kayitOkumaGunuVeKalaniKorurZincirDisindaBaslamaz`; senaryo koşu 2 "Aktif afet geri
yüklendi: gunes_patlamasi ... aktif" + büyüyen güneş + yağmurun yeniden başlaması; koşu 3 kızıl cüce kalıcı.

**GP9 GameTest hijyeni (K-E15, K-E16).** Beş test, beş batch (her biri 1 test). Global dönemi değiştirenler başta ve sonda
`sifirla` ("eski değer" = boş dünyanın ilk hâli; her koşu boş dünyayla başlar, gokyuzu testleriyle aynı desen); değiştirilen
gamerule (`afetBlokHasari`) eski değerine döner. Meteor yağdıran test `afetBlokHasari=false` ile koşar ve afeti aynı testte
`stop` ile temizce bitirir (uçan meteor ve yağmur 0 doğrulanır). Sahte oyuncu test sonunda listeden çıkarılır.

**GP10 İstemci ve ses kapsamı (tur 2'de güncellendi).** Güneş diski, süpernova, nebula, kızıl cüce (gokyuzu), meteor izi/çarpma
(meteor), siren (uyarı), donma ve buhar (donma) ortak çizicilerdedir. Afete özel ekler `client.gunes_patlamasi`'ndadır (GP11–GP14);
tek durum kaynağı çekirdeğin `DonemPaketi`'si (evre, ilerleme, evre başlangıcı): yeni paket, yeni mixin ve gokyuzu dosyasına yazım yok.
Yapılmayanlar: çarpma anının beyaz örtüsü (meteor parçasının çarpma parlaması var), süpernova çökmesinde öteki ortam seslerinin
susturulması (vanilla kategorilerine dokunmak gerekir), kavrulmada uzak yangın uğultusunun ayrı katmanı (rüzgâr dosyasının içinde).

**GP11 Aurora perdesi `AFTER_SKY`'da.** Tasarım §2 aşama 0: "gece ufuktan yükselen kızıl-yeşil aurora" (Carrington 1859). Forge
`RenderLevelStageEvent.Stage.AFTER_SKY`'da `position_color`, toplamalı, derinlik yazımı yok: kuzey göğünde ±100°'lik yay, 7–13°'den
22–36° yukarı üç bant (alt kenar yeşil `#3CFF8A`, üst kenar kızıl `#FF3C5A`, uçlar saydam), U yönünde kayan ışın deseni
(`GunesGorselEgrileri.isin`) ve dalgalanan yükseklik/yarıçap. Güç `aurora(p, gece, yağmur)`: BUYUYEN'in bütün gecelerinde (siren ve
5 gün), sirende %35'ten tama açılır, gündüz ve yağmurda söner. Arazi sonra çizildiği için perdenin önünde kalır. Kalite (N13,
mevcut `gorsel.efektKalitesi`): katman 1/2/3/3, dilim 48/64/96/128 (Düşük/Orta/Yüksek/Ultra); en çok 1 152 dörtgen, kare başına
ayırma yok (statik sütun dizileri, yalnız render thread'i).

**GP12 Ekran örtüleri görünüm uzayında (`AFTER_WEATHER`), arayüz katmanı değil.** F1 (`hideGui`) bütün arayüz katmanlarını gizler;
süpernova parlaması ve ısı dalgalanması arayüz değil dünyanın parlaklığıdır (vitrin kareleri de F1'le alınır). Model-görünüm birim
matrise alınır, z = −1'de projeksiyondan hesaplanan ekranın 1,2 katı dörtgenler çizilir, sonra geri yüklenir. (a) Süpernova: beyaz
%85 × `GokyuzuEgrileri.supernovaFlas(evre saniyesi)` (gökyüzünün parlama eğrisiyle aynı an), çatı altında %35. (b) Isı
dalgalanması: ekranın alt %55'inde yana kayan, titreşen sıcak şeritler (kalite 10/16/24/32 şerit), yalnız gündüz ve göğü gören
konumda, AKTIF ilerledikçe %25'ten tama (şadersiz serap; vücut sıcaklığının sıcak vinyeti sıcaklığın kendi katmanıdır). (c) Kızıl
cüce şafak tonu: doğuş boyunca `#FF6A3A`, tepe %14 (doğuşun ortası), doğuş tamam olunca 0; gündüz gök/sis tonu ve ışık haritası
gokyuzu'nundur (G11, G14).

**GP13 Yaprak/çimen kuruması blok renk işleyicisiyle.** Tasarım §2 "yapraklar kahverengiye döner (biyom rengi istemcide
karıştırılır), blok değişmez". `RegisterColorHandlersEvent.Block`'ta çimen (`grass_block`, `grass`, `fern`, `potted_fern`,
`tall_grass`, `large_fern`) ve yapraklar (meşe, orman, akasya, kara meşe, mangrov, sarmaşık, ladin, huş) için vanilla'nın
`BlockColors.createDefault` hesabının aynısı + kuruluk karışımı (çimen `#B59A55`, yaprak `#8A6A2E`). Kuruluk 2. günün sonundan (p =
0,564) sürecin sonuna yumuşak geçiş, tavan %72; 8 basamak, basamak değişince bir kez `levelRenderer.allChanged()` (5 günde en
çok 8 + dönüş). Kuruluk 0'da renk vanilla ile bire bir; eşya simgesi (`level == null`) değişmez. Bilinen sınır: aynı blokları
renk işleyicisiyle kaydeden başka bir mod varsa mod yükleme sırası kazananı belirler (önceki işleyici Forge API'sinde okunamaz).

**GP14 Sesler.** `tools/assets/gunes_sesleri.py` (ses_uret yardımcıları, deterministik), ölçüm `docs/kanit/ses/gunes_*_ebur128.txt` +
spektrogram: kavurucu rüzgâr (20 sn döngü, −26,1 LUFS, −9,0 dBTP, baskın 40 Hz), süpernova basıncı (8 sn, 25 → 60 Hz, −25,0 LUFS,
−9,3 dBTP), kristal uğultu (20 sn, −29,9 LUFS, −18,2 dBTP, 340–660 Hz), kızıl cüce akoru (4,5 sn, −27,0 LUFS, −14,5 dBTP); dördü
de "UYGUN". Rüzgâr sunucunun `OrtamSesi` katmanıdır (`gunes/kavrulma`, global, WEATHER; AKTIF'te %30 → %100, afet bitince söner,
kayıttan dönüşte yeniden). Süpernova ve akor eşitlenmiş evre saniyesinden istemcide bir kez çalar (AMBIENT, konumsuz): basınç
çökme bitince (8. sn), kristal 6 sn sonra, akor doğuşun ilk 6 sn'sinde; sonradan giren oyuncu yalnız pencere içindeyse duyar.
Siren uyarınındır (K0-19). `sounds.json` ve altyazılar datagen (`GunesPatlamasiSesleri`).

**GP15 Kavrulma ölçekli saate bağlı (tur 1'den değişti).** Tur 1'de her gerçek tick'te oyuncu başına bir deneme vardı;
`/dogalafet hiz` 5 günü sıkıştırırken kavrulmayı sıkıştırmıyordu (hızlı testte çimen hiç kurumuyordu). Şimdi ölçekli tick başına
bir deneme (`adim` kadar, oyuncu başına tick'te en çok 32; TPS kısması olasılığı çarpar). Hız 1'de davranış aynıdır.

**GP16 Görsel otomasyon.** `client.gunes_patlamasi.GunesPatlamasiOtomasyonu` (`-Potomasyon=gunes_patlamasi`) + `tools/senaryo/
gunes_patlamasi_gorsel.py` (ws 96, `runclient_kilit.py kos`, K0-21 rev. 2 (a)–(e), kare eksik/bayatsa bir tekrar). Kamera en yakın
ormanın 40 blok batısında uçan yaratıcı oyuncudur: izleyici meteor yağmurunun hedefi değildir (meteor §A.1). Gerçek afet komutlarla
yürütülür; tek vitrin eki 4. günde afetin kendi kaynağıyla kısa, yoğun bir yağmurdur (oyuncu başına dakikada 40, %15 büyük,
düşme 0,6 yavaş çekim) — tasarım vitrini "4–6 meteor izi" ister, afetin 4. gün yoğunluğu (dakikada 4,3) tek karede bunu vermez.
Kavrulma karesi hız 60 × 300 tick (4. günün %75'i) sonrasıdır. Her kare: koşul + FPS ≥ 15 (en çok 60 tick) + en az iki yeni
çizilmiş kare; satırı FPS, TPS, mspt, evre, ilerleme, evre saniyesi, uçan meteor, parçacık ve kuruluk basamağını yazar.
Toast'lar (F1'de de çizilir) her istemci tick'inde temizlenir; süpernova çökmesi ve parlaması `hiz 0.25` ile ağır çekimde alınır.

**GP17 Gizli workspace'te FPS 1: tekrar yok, ölçüm Faz 5'e.** Koşu 1'de 13 karenin hepsi FPS 1 (15 FPS kapısı her karede 60 tick'te
zaman aşımına düştü); kareler yine en az iki yeni çizilmiş karenin sonunda alındı, 13 farklı md5 ve piksel ölçümleri evrelerle
uyumlu (`gorsel/kare-olcum-1.tsv`: aurora karesinde üst yarı yeşil %8,4 / kızıl %4,3, kızıl cücede kızıl %72). Şefin tekrar koşulu
"kare boş çıkarsa" sağlanmadığı için ikinci runClient açılmadı. FPS 1 compositor kısmasıdır, modün maliyeti değildir: emsal
yangın ("İstemci FPS ve Ultra karesi bilinen sınır: gizli workspace ~1 FPS çizer, ölçüm Faz 5'te görünür pencerede", şef
kararı), spike-gokyuzu Faz 2 notu, K0-21 rev. 2 (j). Sunucu tarafı ölçüldü: TPS her karede 20,0, meteor vitrini tepesinde mspt
6,7–7,1 (10 uçan meteor, 4 482 parçacık). Gerçek FPS ve Düşük/Yüksek/Ultra karşılaştırması görünür pencere ya da başsız
kalibrasyon ister (§E).

**GP18 Başarısız GameTest de batch sonunda temizlenir (entegrasyon 4, inceleme P2).** `succeedWhen` kullanan iki test
(`sirendeGunesBuyur...`, `zincirKizilCuceye...`) başarısız bitince kendi temizliğini çalıştırmıyordu (`try/finally` asıl
kontrolü geçtiği için işlemez: koşul zaman aşımına kadar yeniden denenir). Batch'leri `@AfterBatch` ile afeti durdurur, dönemi ve
hızı `sifirla` ile sıfırlar, siren testinin kapattığı `afetBlokHasari`'nı eski değerine döndürür (K-E15; balıkçılık K-E22 c ile aynı
desen). Başarılı yol değişmedi.

### Reddedilen yollar

- **BÜYÜYEN'i AKTIF'te açmak (taklit gibi):** sirende güneş büyümezdi (bölüm D).
- **Siren büyümesini gökyüzü çizicisinde ayrı alanla vermek:** gokyuzu paketine yazmak (sahiplik dışı) ve yeni dönem alanı/paket
  (çekirdek); aynı sonuç evre ilerlemesiyle alınır (GP1).
- **Tek uzun yağmur:** yoğunluk eğrisi uygulanamazdı (GP3).
- **Kavrulmada `canSeeSky`:** gök ışığı blok değişiminden sonra gecikir; yükseklik haritası eşzamanlı (GP6).
- **Başlama kapısını komutta denetlemek:** `CekirdekKomutlari` çekirdeğindir; `yerUygunMu` aynı işi kendi sınıfında görür (GP5).

## Faz 4 — Ay çarpması

Parça `ay-carpmasi` (dal `is/ay-carpmasi`, son commit `a588720`; `main`'e birleştirme `3127568`, protokol `d0fe4b9`, P2 `1a3f80a`, entegrasyon 4 2/6). Kaynak
`.vibe/kararlar/ay-carpmasi.md` §B/§C/§D; sözleşmeler (§A) ve entegratör notları (§E) o dosyada kalır. Şef kararları (K-E24 AY-07,
protokol "6") "Şef kararları (Faz 4)" bölümündedir; N17 satırları sondaki tablodadır. Rapor `.vibe/rapor/ay-carpmasi.md`, inceleme
`.vibe/rapor/inceleme-ay-carpmasi.md` (kabul, P2 = 7) ve `-2.md` (AY-12, P2 = 1), birleştirme `.vibe/rapor/entegrasyon-4.md`.

**AY-01 Ay veri modeli: bool + olay saati, ayrı alan değil `AyDurumu` türetmesi.** `ayVar` bool'u korunur, yanına tek long
`ayOlaySaati` eklenir; durum ikisinden türetilir. Neden: `ayVar()`'ı okuyan altı alt sistem (su, başarım, tek seferlik kural,
gökyüzü, komut, testler) değişmeden kalır; NBT ve paket geriye uyumlu tek alan alır. Ayrı enum alanı iki kaynaklı durum (bool
ile tutarsız) riski getirirdi.

**AY-02 Yaklaşma = SIREN aşaması, çarpma = AKTIF başı.** Bölüm H "siren çaldıktan 60 sn sonra çarpsın (siren bu anda
fade-out)" çekirdeğin SIREN → AKTIF geçişiyle zaten aynı an (uyarı-ses §A.1). Yaklaşmanın 60 sn'si istemcide SIREN başından
ölçekli saatle çizilir; `/dogalafet hiz` hepsini birlikte hızlandırır. JUnit `yaklasmaSirenSuresiyleAyni` bağı denetler.

**AY-03 Siren sırasında durdurma ayı geri getirir; çarpmadan sonra hiçbir durdurma geri getirmez.** Ay çarpması tek
seferliktir ve "ulaşılmış dönem geri alınmaz" (madde 11). Yaklaşma henüz ulaşılmış dönem değildir (güneşin BUYUYEN →
NORMAL dönüşüyle aynı mantık, K-C26). Kural (KURAL, yumuşak) durdurmada da aynı: ay çarpmadıysa yerinde kalır.

**AY-04 Çarpma noktası görseldir; harita sarsıntısı yalnız "sağlam olmayan" düşmesidir.** Tasarım §6 (spike sonrası): dev
krater açılmaz. "Harita sallansın" (H.3) oyuncu çevresinde desteksiz yaprak/cam/fener düşmesiyle verilir (tasarım §2),
`afetBlokHasari=false` iken hiç blok düşmez (madde 11). Deprem işçisinin "sağlam olmayan" listesi bu dalda yok; kendi
etiketimiz `#dogalafetler:ay_sarsintisi_duser` veri paketiyle genişletilir (N12). Desteksiz = altı `FallingBlock.isFree`.

**AY-05 Sarsıntı mesafeden bağımsız ve her boyutta.** H.3 "tüm oyuncuların ekranı". Kamera sarsıntısı çarpma saatinden
istemcide hesaplanır (paket yok); Nether/End'deki oyuncu da sarsılır (N11 global olay), parlama yalnız Overworld'de (göğü
olmayan boyutta beyaz ekran anlamsız).

**AY-06 Ses: güm her oyuncuya kendi konumunda.** `playNotifySound` her oyuncuya ayrı ve mesafesiz (uyarı-ses §A.4 kural 2
konumlu `level.playSound`'u önerir; çarpma ufkun ötesinde, herkes aynı düzeyde duymalı, madde 3). Yaklaşma uğultusu ve
çıtırtı `OrtamSesi` global katmanı (siren sırasında −3 dB bastırma ve geç gelen oyuncu çekirdekte/uyarıda çözülü).

**AY-07 Döngülü ay sesleri −30 LUFS (tasarım tercihi, K-E24).** Uyarı-ses hedefi −26 LUFS'tur; K-E24 döngülü ortam
katmanına −26…−30 bandını tanır. −30 seçildi çünkü 18–100 Hz alçak uğultu −26'da "konuşmayı bastırmayan, hissedilen"
(tasarım §4) karaktere fazla geliyordu. Tepe sınırı gerekçe değildir: +4 dB ile tepe −7,6 / −7,4 dBTP olurdu, −6 dBTP sınırının
içinde (entegrasyon 4 düzeltmesi, inceleme P2). −30 LUFS, tepe −11,6 dBTP; çıtırtının 2,5 kHz üstü −58 dB (konuşma bandı çentikli).

**AY-08 Gökyüzü çizimi gök küresinde sabit dünya yönünde, zaman dönüşü dışında.** Yaklaşan ay gece/gündüz dönmeden "çarpma
noktasına dönük sabit yön" (tasarım §2) çizilir; azimut doğudan 20° güneye (`AyEgrileri.AZIMUT_DERECE`). Enkaz halkası
doğu-batı ekseninden geçen, tepeden güneye 30° eğik büyük çember (ekvator düzlemi hissi), ufuk altı yarısı çizilmez.

**AY-09 Gündüz de biz çizeriz (yalnız ay çarpması sürerken ve izleri sönene kadar).** Sarmalayıcı normalde NORMAL güneş + ay
yok gündüzünde iç nesneye devreder (G1/B4). Yaklaşan ay ve mantar sütunu "gündüz ya da gece fark etmeksizin" (tasarım §2)
görünmeli; `IstemciAy.gokyuzuEtkin()` true iken sarmalayıcı çizer. Halka (3 gün) da bunun içinde; halka kapalıysa ya da
söndükten sonra davranış eskisi gibidir (başka modun gündüz göğü korunur).

**AY-10 Yağmur doğuş noktasına düşer (oyuncusuz).** `MeteorAPI.yagmur(oyuncular=boş)` sözleşmesi: boyuttaki oyuncular her tick
yeniden okunur, oyuncu yoksa doğuş noktası. GameTest'lerde yağmur başladığı tick'te bitirilir ya da sıklık 0 yapılır
(`meteorDakikaBasinaAyarla`, paket içi); sahnelere meteor düşmez.

**AY-11 Kayan yıldızlar istemci yerel rastgelesi.** Tasarım "oyuncu başına 1 / 3 dk, zararsız": ağ yok, her istemci kendi
kayan yıldızını görür (oyuncuya özgü efekt, madde 3).

**AY-12 Sarsıntı yalnız varlık işleyen chunk'ta blok düşürür; GameTest sahnesi varlık işleyince koşar.** Kök neden: GameTest'in
zorladığı sahne chunk'larından biri (z < 0 yanı, chunk `[59,-1]`) ilk tick'te henüz FULL/ENTITY_TICKING görünürlüğüne çıkmamışken
`aday` yalnız `hasChunkAt` sorduğu için oradaki yaprağı seçiyor, `FallingBlockEntity.fall` bloğu kaldırıyor ama varlık
`PersistentEntitySectionManager.addEntity`'de görünürlüğü `HIDDEN` olan bölüme giriyordu; `getEntitiesOfClass` yalnız
erişilebilir bölümleri gezdiği için sayaç (`toplam`, bu sarsıntı nesnesine özel) kutudaki varlık sayısından büyük çıkıyordu.
Kanıt (geçici TANI satırı, `.vibe/kanit/ay-carpmasi/tani-1..3.log`): koşu 1 toplam 12 / görünen 7, eksik 5'in hepsi `chunk=[59, -1]
gorunur=false varlikIsler=false kutuda=true`; koşu 3 toplam 8 / görünen 3, eksik 5'in hepsi aynı; geçen koşu 2'de 15/15, bütün
varlıklar `gorunur=true` (iki tanesi `[60,-1]` TRACKED: görünür, işlemiyor). Sayaç başka sahnenin ya da vanilla'nın düşen
bloğunu saymıyor (her köken sahnenin yaprak karesi içinde, `blok=oak_leaves`), varlıklar kutu dışına düşmüyor (hepsi `kutuda=true`,
aynı tick). Başarısız koşularda TPS kısma çarpanı 0,5 (sunucu gecikmesi chunk görünürlük güncellemesini geciktiriyor).
Düzeltme: üretimde `SarsintiDusmeleri.aday` `level.isPositionEntityTicking` ister (aynı görünürlük haritasını okur: düşen blok
görünmez bölümde donup kalmaz, oyunda da varlık işlemeyen kenar chunk'ta blok kaybolmaz); test `succeedWhen` ile tarama alanının
bütün chunk'ları varlık işleyene dek bekler, sonra eşik ve doğrulamalar aynen bir kez koşar (zayıflatma yok). Reddedilen: sayımı
`havadakiler` listesinden yapmak (görünmez varlığı "düştü" sayıp asıl hatayı gizlerdi).

**AY-13 Birleştirme düzeltmeleri (entegrasyon 4, inceleme P2).** (a) Sunucu açılışında (`ServerStartedEvent`, çekirdeğin geri
yüklemesinden sonra, LOWEST) ay YAKLASIYOR ama aktif afet ay çarpması değilse ay VAR'a döner ve WARN yazılır: afet kaydı
okunamazsa ay yaklaşma hâlinde takılı kalmaz (`AyCarpmasiSistemi`). (b) Sarsıntı GameTest'i hacim boşaltma, düşen blok silme
ve sahne kurulumunu `varlikIsliyor` sağlandıktan sonra yapar (gizli bölümdeki eski düşen blok silinmeden kalmaz, AY-12 ailesi);
sahnedeki `GameTestAssertException` dışı istisna da testi düşürür, sunucu tick'ine kaçmaz. (c) Hız testi başarısız bitse de
batch sonunda (`@AfterBatch`) afet durur, dönem sıfırlanır, `meteorDakikaBasina` eski değerine döner (K-E15, GP18 deseni).
(d) AY-07 gerekçesi ölçümle hizalandı (tasarım tercihi, K-E24).

### Reddedilen yollar

- **Kendi ay meteoru varlığı:** Faz 4 kuralı; yalnız `MeteorAPI`.
- **Çarpma için ayrı istemci paketi (`ay_carpmasi/carpma`):** dönem paketindeki olay saati yeter; ayrı paket protokol
  artışı ve sonradan girenler için ikinci senkron yolu gerektirirdi.
- **Gerçek dev krater:** tasarım §6 spike sonrası kararı (harita korunur).
- **`AFTER_SKY` ile yaklaşan ay:** vanilla ay o anda çizilmiştir, gizlenemez (G1); sarmalayıcı yolu kullanıldı. 3. kademede
  (yansıma bozuk) yaklaşan ay vanilla ayın üstüne çizilir (bilinen sınır, gökyüzü G1 ile aynı).
- **Sarsıntıyı meteor sarsıntı paketiyle göndermek:** o paket mesafeyle söner (M-11); ay çarpması mesafeden bağımsız.

## Şef kararları (Faz 0–1)

**K0-19 Ay çarpması siren süresi: 60 sn.** Madde 5 "Güneş Patlaması hariç 10 sn" der; bölüm H "siren çaldıktan 60 saniye
sonra ay çarpsın (siren bu anda fade-out ile dursun)" der. Daha özel kural kazanır: ay çarpmasında standart siren dosyası
60 sn döngüyle çalar ve çarpma anında fade-out olur; diğer altı afette 10 sn. Berk değiştirebilir.

**K0-20 Tasarım notları kabul edildi.** `docs/tasarim/*.md` (12 dosya) şef tarafından okundu ve Faz 2–4 işçileri için
bağlayıcı başlangıç noktasıdır; notlardaki "doğrulanmadı" işaretli teknik riskler (sıvı render mixin'i, sanal suda
yüzme/boğulma, DimensionSpecialEffects sarmalama, özel ateş bloğu) Faz 1 spike'larında karara bağlanır.

**K0-21 Görsel kanıt yöntemi.** Klavye/fare olmadan ekran görüntüsü almak için istemci tarafında yalnız bir Gradle
özelliğiyle (`-Pspike=<ad>` / ileride `-Potomasyon=<senaryo>`) açılan otomasyon kancası kullanılır:
`ClientPlayerNetworkEvent.LoggingIn` sonrası zamanlayıcıyla komut gönderir, `Screenshot.grab` ile PNG yazar, FPS'i loglar,
`Minecraft.stop()` ile kapanır. İstemci `DISPLAY=:0` ile açılır ve `hyprctl dispatch movetoworkspacesilent` ile ajan
workspace'ine (90–99) taşınır. Aynı anda tek `runClient` (MAKINE.md).

**K0-21 revizyonu (görsel kanıt ve ekran protokolü).** spike-siren bulgusu: ajan workspace'ine taşınan Minecraft penceresi
Hyprland klavye odağını alabiliyor (sahibinin tuşları oyuna gitti, Escape duraklatma menüsünü açtı);
`hyprctl dispatch movetoworkspacesilent` bu Hyprland'de (Lua yapılandırma) sözdizimi hatası veriyor. K0-21'deki taşıma
yolu bu protokolle değişir:

- (a) İstemciyi açmadan önce `hyprctl activewindow -j` ile sahibinin aktif pencere adresi kaydedilir.
- (b) Kendi pencere YALNIZ pid ile seçilir (`hyprctl clients -j` → pid → `readlink /proc/<pid>/cwd` = kendi worktree'nin
  `run/`'ı); sınıf/başlık eşlemesi yasak.
- (c) Taşıma hypruse `hypr move_window` ile ajan workspace'ine (90–99; her işçiye ayrı numara).
- (d) Taşımadan sonra ve oyun boyunca 30 sn'de bir `activewindow` kontrolü; odak oyundaysa (a)'daki pencereye geri
  verilir ve loglanır.
- (e) Başka pencereye tıklama/odaklama/workspace değiştirme yok.
- (f) Otomasyon kendi açmadığı ekranı (PauseScreen) kapatır; adımlar duvar saatiyle değil olay/durum koşuluyla ilerler.
- (g) `EARLYDISPLAY ... trouble handing off` çökmesinde `run/config/fml.toml` → `earlyWindowControl = false` (git dışı).

Görünmeyen workspace'te `Screenshot.grab` siyah değil (oyun framebuffer'ı okunur). FPS ölçümü başka Gradle çalışırken
gürültülüdür; raporda belirtilir.

**K0-22 Git komutu `/usr/bin/git`.** Kabuktaki `git` omp sarmalayıcısına gidiyor; `git worktree add` sarmalayıcıda
gitignore'daki klasörleri (`.worktrees/` dahil, özyinelemeli) kopyalayıp 33 GB doldurdu (spike-donma). Bütün işçiler ve
entegratör git için `/usr/bin/git` kullanır. Patch'ler `--binary` ile alınır (nbt/png/ogg).

**K0-23 Faz 1 ara sonuçları (şef kabulü bekleyen).** Ayrıntı `.vibe/kararlar/spike-*.md`; nihai işleme Faz 5'te.

- spike-siren: N5 yolu (MASTER + `sirenSes` %28 + "Sirenler" kaydırıcısı). Gerçek `SoundSource` kategorisi denendi ve
  reddedildi (ordinal ağ riski, varsayılan %100, mixin). Ay çarpması sireni 60 sn (K0-19).
- spike-donma: chunk capability, 256 blok/tick + 4 ms, TPS 18/15 kısması, iki yönlü tek köklü zincir tablo + veri paketi,
  seed'li değer gürültüsü maskesi %25.
- spike-gokyuzu: kanca (b) sarmalayan `DimensionSpecialEffects`, (c) yedek.

**K0-24 Faz 1 inceleme kararları (donma).** Madde 9 tasarım notuna karşı kazanır.

- Çözülme sırası: önce kırağı/ince kar, sonra buz kenardan içeri; ılık bölge önce (yönlü histerezis eşikleri).
- Kış ağırlığı sütun düzeyindedir: maske (chunk köşelerinden çift doğrusal) ⊕ 3×3 chunk soğuk biyom oranı ⊕ yükseklik
  smoothstep bandı (spike'ta 88–120; 120 üstü w=1). Komşu sütun sıçraması ≤ 0,05.
- Chunk verisi: `sonG + dönem + kar/buz bitleri`.
- Yüzey tablosunda hedef yalnız `dogalafetler` ad alanındaki donma bloklarıdır (`kiragili_cimen` ilk örnek); yabancı hedef
  yükleyicide reddedilir. N12 genişlemesi için açık onay alanı vardır.
- Oyuncunun koyduğu kar katmanı/buz, bizim koyduğumuz bitlerle ayrılır; doğal kar/buz kalınlaştırılmaz (bilinen sınır).

Ayrıntı `.vibe/kararlar/spike-donma.md`.

**K0-25 Sanal su mixin'leri kabul.** `Entity.updateInWaterStateAndDoFluidPushing` (RETURN) ve `updateFluidOnEyes` (TAIL)
enjeksiyonları ile istemcide `LiquidBlockRenderer.tesselate` (HEAD, çekilme bölgesi). Gerekçe: olay yolu yüzme pozunu,
vanilla su hareketini ve mob yapay zekâsını karşılamadığı tabloyla gösterildi (GOREV "mixin yalnız event yetmiyorsa").
Veri modeli: sütun başına dikey aralık sorgusu; istemciye parametre gider (deniz 28 B, dalga 116 B); mağara cebi sütun
aralıklarıyla tutulur. Ayrıntı `.vibe/kararlar/spike-sanal-su.md`.

**K0-26 Gökyüzü üç kademeli kanca.** Sar → ezilirse `EFFECTS`'i yansımayla yeniden kurup kazananı sar → yansıma
başarısızsa (c) `AFTER_SKY` + "bilinen sınır: ay/güneş gizlenemez" WARN'ı. Ezilme senaryosu sahte ikinci efektle koşuldu.

**K0-27 Siren devamı.** Boyut değişimi/`SoundManager.stop()` sonrası siren zarf korunarak yeniden çalınır; `/stopsound`
sireni susturamaz (uyarı sesi, kasıtlı). Sonradan giren oyuncuya kalan süreyle siren paketi Faz 2 `uyari` işçisine.

**K0-28 Ekran odağı — açık sorun.** K0-21 rev. (d) yetersiz: gizli workspace'teki istemci 1–21 sn içinde odağı geri
alıyor; sahibin tuşları (Alt/Tab, Escape) oyuna gitti; bir koşuda sahibin etkin workspace'i boş ws 97'de kaldı. Deneme:
pid'e bağlı, oturum süreli geçici `nofocus` pencere kuralı (spike-kasirga ölçüyor); alternatif Xvfb/başsız compositor.
Sonuç gelene dek `runClient` yalnız şef onayıyla. Kullanıcıya soruldu.

**K0-29 Mağara suyu hücre modeli.** Üç durumlu hücre: BOS (hava + çarpışmasız/değiştirilebilir bloklar; su altında
kalır, bloğa dokunulmaz) / KATI / ACIK (gökyüzünü gören hava ya da yüklü olmayan chunk). Keşif ACIK komşuya değince su
eşiği (en alçak açıklık) aşılmaz. Bilinen sınır: tavanı delik mağara ve vadide az su (açık hücre = tahliye). Ayrıntı
`.vibe/kararlar/spike-sanal-su.md` SU-09.

**K0-30 Sanal su tazeleme.** Deniz alanı iki tarafta da `TazelemePlani` (kamera/oyuncu çevresi ±2 chunk, 20 tick, 16
chunk/tick) + sunucuda bayraktan bağımsız `BlockEvent`'ler + `SanalSu.chunkDegisti(ChunkPos)` API'si (donma sistemi
çağırır). Faz 2 notu: okumaları tick'lere yay, çok oyuncuda kapasite. SU-10.

**K0-31 GameTest dünyası düz ve yapısız, tek ayar çekirdekte.** Normal arazi `MOTION_BLOCKING` yüzey haritası ve sahne
üstündeki arazi yüzünden iki spike'ta GameTest'leri bozdu (donma, sanal su). `runGameTestServer` `server.properties`'e
`level-type=flat`, yapısız `generator-settings`, `level-name=oyuntesti-duz` yazar; ayrıca vanilla GameTest kuralları
(`doMobSpawning/doWeatherCycle/doDaylightCycle=false`, `randomTickSpeed=0`) aynı kurulumda. Bu ayar `cekirdek` dalında
`build.gradle`'a girer (`cekirdek` işçisine bildirildi); spike dallarındaki kopyalar birleşmez.

**K0-32 Faz 1 kabulü.** Beş spike da inceleme + doğrulama zincirinden geçti: siren 149b2c4, donma 59a2328, gökyüzü
71c3af8, sanal su 6a2cd95 (hepsi P0/P1 = 0); kasırga 54929d4 kabul, odak deneyi + vitrin düzeltmesi sürüyor. Raporlar
`.vibe/rapor/inceleme-*.md`, `dogrulama-faz1-son.md`.

**K0-21 rev. 2 — Ekran protokolü (kesin, şef kabulü; rev. (a)–(g)'nin yerine).** Ajan istemcisi Hyprland'de odak alamayacak biçimde açılır; taşıma ve "odağı geri verme" yoktur.
Dayanak: `.vibe/kararlar/spike-kasirga.md` SK-09 (Hyprland 0.57.1, Lua yöneticisi, `misc:focus_on_activate = true`).

- (a) **Açılış yalnız exec kuralıyla:** `hyprctl dispatch 'hl.dsp.exec_cmd("[workspace <9x> silent; no_initial_focus; no_focus on;
  focus_on_activate off] sh <koşu betiği>")'`; betik `./gradlew runClient --no-daemon ...` çalıştırır. `9x` işçiye özgü (90–99).
  Doğrudan `./gradlew runClient`, sonradan taşıma ve sahibin yapılandırma dosyalarına ya da global ayarlara kural yazmak yasak.
- (b) **Belirteç kapısı:** istemci JVM'i görünür görünmez (pid; `/proc/<pid>/cwd` = kendi `run/`) `/proc/<pid>/environ`'da
  `HL_EXEC_RULE_TOKEN` aranır; yoksa pencere eşlenmeden süreç öldürülür, koşu iptal.
- (c) **Pencere doğrulaması:** pid'in her penceresi eşlenince `hl.dsp.window.set_prop({ window = "pid:<pid>", prop = "no_focus",
  value = "1", lock = true })` ve `prop = "focus_on_activate", value = "0"` yazılır (`value` dizgi; yanlış tür özelliği sıfırlar),
  ardından `hyprctl getprop` ile `no_focus = true` ve pencerenin workspace'i `9x` doğrulanır. Tutmazsa süreç hemen öldürülür.
- (d) **Bekçi:** açılıştan önce sahibin etkin penceresi kaydedilir; oyun boyunca 5 sn'de bir `hyprctl activewindow -j`. Etkin pencere
  kendi pid'imizse ALARM: süreç öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz, şefe bildirilir. "Geri ver, devam et" yok.
- (e) **Oyun içi girdi günlüğü:** `InputEvent.Key`, `InputEvent.MouseButton` ve `InputEvent.MouseScrollingEvent` istemci kurulumundan
  kapanışa kadar (yalnız otomasyon adımlarında değil) log'lanır; sayılar rapora girer, sıfırdan farklıysa koşu geçersiz + şefe.
  `isWindowActive` odak göstergesi değildir (XWayland'in X düzeyi odağı; koşu 5/6'da baştan sona `true` iken tuş 0), yalnız bilgi.
- (f) **Deney pencereleri de** (a)–(d)'ye tabidir; kural doğrulanamazsa (`no_focus` ≠ true) pencere ilk odak isteğinden önce kapatılır.
- (g) Sahibin oturumunda tıklama, odaklama, workspace değiştirme, hypruse `pointer`/`keyboard`/`click_ui` yok; okuma (`hyprctl ... -j`,
  hypruse `desktop`) serbest. Pencere "urgent" işaretlenebilir (çubukta ws `9x`): zararsız.
- (h) Otomasyon kendi açmadığı ekranı kapatır; adımlar olay/durum koşuluyla ilerler. `EARLYDISPLAY ... trouble handing off`
  çökmesinde `run/config/fml.toml` → `earlyWindowControl = false` (git dışı).
- (i) Ağır süreç kapısı ve aynı anda tek `runClient` (MAKINE.md) sürer; FPS yalnız Gradle sayacı 1 iken geçerli, oranlar ortanca kare
  süresiyle.
- (j) Bu protokolle K0-28 kapanır ve `runClient` ayrıca şef onayı istemez (şef kararı). Başsız yol (weston `--backend=headless`
  + XWayland) kalibrasyon koşusunu geçerse (aynı sahne ve çözünürlük, 60 FPS tavanı yok, `Screenshot.grab` siyah değil, FPS ±%10)
  uzun otomasyon koşularında tercih edilir.
- (k) runClient açan her betik `tools/senaryo/runclient_kilit.py kos <worktree> --bekle <sn> -- <komut>` kullanır (`.vibe/runclient.lock`,
  gökyüzü §A.6, G21, G24); `pgrep` kapısı yalnız bilgi içindir, karar kilitten gelir. Faz 2b betikleri birleştirmede bağlandı (K-E4).

Kanıt: `spike-kasirga` SK-09 (test penceresi 27 istek 0 odak; Minecraft 110 yoklama 0 ALARM, 0 tuş). Kalan risk: sahip
XWayland uygulaması kullanırsa X düzeyi odak (`isWindowActive`); şu an sahibin pencereleri Wayland. Weston headless
(`/usr/bin/weston` 15, Iris Xe GL) uzun otomasyon için aday; kalibrasyon koşusu Faz 4'te.

**K0-28 kapandı** (K0-21 rev. 2 ile). `runClient` artık şef onayı gerektirmez; protokol zorunlu.

**K0-33 Kasırga spike kabulü (49d54db).** SK-01 prosedürel ağ, SK-02 kendi dörtgen listesi, SK-03 `UcanBlokVarligi` ≤ 40 +
görsel kopyalar, SK-04 4 Hz + enterpolasyon, SK-11 bulut katmanları `AFTER_PARTICLES` (Fabulous'ta derinlik riski Faz 4
notu), SK-10 enkaz eteği + `tepeCapi` tablodan. Faz 4 kabul şartları: vitrin kamerası tabanı gören yönde ve iki yandan
taşan etek (kasirga.md:57), taban rengi kahverengi (R>G>B ve sıcak ton), S1 yaprak/çimen parçaları sayısal olarak
seçilebilir; performans hedefi S5'te 52/36/20/≤5 blok ve kaldırılan oyuncu noktasında Yüksek ≥ %50 (ortanca kare süresi;
5 blokta bugün %18–32 → LOD gerekir); `kasirga_dayanikli` etiketine deepslate cevherleri, merdiven/basamak/duvar
varyantları.

**K0-34 Faz 1 kapanışı.** Beş spike kabul (K0-32 + K0-33). Spike dalları `main`'e birleşmez; Faz 2/4 işçileri
`.vibe/kararlar/spike-*.md`'yi okur.

## Şef kararları (Faz 2a)

Çekirdek parçasında şefin verdiği kararlar (kaynak `.vibe/kararlar/cekirdek.md` §B; Faz 2a bölümünde aynı kimliklerle atıf vardır).

**K-C28 Gelişim kriteri (şef kararı).** `gunesPatlamasiKriteri` ve `ayCarpmasiKriteri` = `GUN | PUAN | IKISI_DE | HERHANGI`,
varsayılan `GUN`; `gunesPatlamasiMinGun = 40`, `ayCarpmasiMinGun = 25`; puan eşikleri (35 / 20) yalnız PUAN içeren
kriterlerde, puan `gelisimOlcutu` (ORTALAMA | EN_GELISMIS) ile toplanır (madde 3). Gerekçe: anlık XP + zırh puanı büyü ve
ölümle düşer; eşik 35'te güneş patlaması pratikte gelmiyordu (inceleyici D2). JUnit simülasyonu (400 dünya × 400 gün, gerçek
kapılar, dönem geçişleri, afet 8 000 tick): ilk güneş patlaması ortanca **78,8. gün**, 400 günde görülme **%99,5**; ilk ay
çarpması ortanca **49,9. gün** (güneşten önce). Güneş ağırlığı 20 korundu.

**K-C29 Oyun günü = `getGameTime()/24000 + ofset` (şef kararı; uyku için K-C42).** Dünya yaşı geri gitmemeli: `/time set`
ve `/time add` gün saatini değiştirir, oyun zamanını değiştirmez; `doDaylightCycle=false` iken de ilerler. Saf formül
`core.mantik.OyunGunu` (JUnit), GameTest (`setDayTime` +72 000 ve 1 000 sonrası gün 42), senaryo (`time add 72000`, `time set
day` sonrası `durum` gün 42). Reddedilen: monoton "ulaşılan gün" alanı (ek durum; oyun zamanı zaten monoton). İlk sürümde
uyku da "değiştirmez" diye bir özellik sayılmıştı; uyku geri alma değil ileri gidiştir, K-C42 onu sayar.

**K-C30 N11 siren kapsamı (şef kararı).** Bölgesel afette (altı seviyeli afet) siren ve afet durumu yalnız afetin boyutunda
(Overworld) etki alanı + `sirenEkYaricap` içindekilere; sunucu geneli afette (güneş, ay) ve global dönem olaylarında her
boyuttaki oyuncuya. Herkes Nether/End'deyken güneş patlaması ve ay çarpması seçilebilir (hedefsiz), bölgesel afet seçilmez
(hedef yok); hiç çevrim içi (izleyici olmayan) oyuncu yoksa seçim yok. Kanıt: JUnit `SecimTuruTest`.

**K-C41 Durdurulan dönem süreci: kural ile duran sürer, stop ile duran sürmez (şef kararı; Y3 seçenek b + test yolu).**
Madde 11 "yalnızca henüz tamamlanmamış süreçler durdurulsun" gamerule için duraklatmadır: `KURAL` ile duran süreç kural
yeniden açılınca kaldığı yerden sürer (`DonemSureci.devamEt`). `/dogalafet stop` ile duran süreç kendiliğinden sürmez; ama
`/dogalafet asama ileri` durmuş süreci de görür ve bir aşama ilerletir (OP test aracı; süreç SUPERNOVA/GUNESSIZ'de kalıcı
takılmaz), süreç durmuş kalır. `durum` duranları "durduruldu (stop)" / "durduruldu (kural)" diye ayrı satırda gösterir. Ayırım
için durdurma nedeni süreç başına `DunyaDonemi`'de kalıcıdır (K-C43). Kural ile durmuş sürece `stop` verilirse kayıt `KOMUT`'a
çevrilir (OP'nin açık durdurması kuraldan güçlüdür). Kanıt: GameTest `kuraldanDuranSurecAcilincaSurerKomuttanDuranSurmez`
(kural kapat → `KURAL`; kural aç → `devamEt`, sürüyor; `stop` → `KOMUT`; kural kapat/aç → sürmez; `asama ileri` → ilerler,
`KOMUT` kalır; kaydet/oku sonrası kayıt korunur; `KURAL` → `stop` → `KOMUT`, kural açılınca sürmez). Sunucu senaryosuna
(`tools/senaryo/cekirdek_sunucu.py`) aynı adımlar eklendi; bu turda koşulmadı, bir sonraki doğrulayıcı koşar.

**K-C42 Oyun günü uykuyu sayar (şef kararı; Y7; `sifirla` kısmı K-C46 ile revize).** Madde 10 ve bölüm D'deki "oyun içi
gün" oyuncunun yaşadığı gündür; uyku geceyi atlar, gün ilerler. Forge `SleepFinishedTimeEvent`'te (en düşük öncelik, yalnız
üst dünya) atlanan tick (`yeniZaman − eskiZaman`, pozitifse; `OyunGunu.uykuAtlamasi`) `DunyaDonemi.uykuBirikimi`'nde toplanır
(kalıcı; `sifirla` **korur**, yalnız `gunOfseti`'ni 0 yapar — K-C46); gün = `(gameTime + uykuBirikimi) / 24000 + ofset`.
`/time set` bağışıklığı ve `doDaylightCycle=false` davranışı korunur (gün döngüsü kapalıyken uyku saati atlatmaz, olay gelmez).
N17 gerekçeleri değişmedi (artık görünen günle aynı). `BICIM_SURUMU` artmadı (yayımlanmamış); `gunOfseti`'nin anlamı
Javadoc'ta: "istenen gün − o anki gün", oyun zamanı + uyku birikimine göre. Kanıt: JUnit `OyunGunuTest` (tek uyku 11 458 tick
≈ 0,48 gün ve uyanınca yeni gün; ardışık 10 uyku → 10 gün, uykusuz 5), GameTest `sifirlaDonemiIlkHalineDondurur`
(`SleepFinishedTimeEvent` 48 000 tick → gün 42 → 44; `sifirla` sonrası birikim korunur, gün = (oyun zamanı + uyku) günü),
`gametest-4.log` "Uyku 48000 tick atladı …".

**K-C46 (Z3) K-C42 revizyonu: `sifirla` uyku birikimini korur (şef kararı).** Z3 (şef kararı, önceki tur talimatını geri alır): `sifirla` `uykuBirikimi`'ni
**korur**, yalnız `gunOfseti`'ni 0 yapar. Uyku birikimi test aracı değil, yaşanmış gecelerin payı = dünya yaşının parçası;
N18 `sifirla` listesinde gün yok. `sifirla` sonrası oyun günü = (oyun zamanı + uyku) günü = görünen gün. Kanıt: GameTest
`sifirlaDonemiIlkHalineDondurur` (birikim korunur, gün formülü), `gametest-4.log`.

## Şef kararları (Faz 2b)

Faz 2b parçalarında şefin verdiği ya da kabul ettiği kararlar (kaynak `.vibe/kararlar/<parça>.md` §B; parça bölümlerinde aynı
kimliklerle atıf vardır). Başlığında "şef kararı/kabulü/kuralı" geçen kararlar ile entegrasyon görevinde sayılanlar (gökyüzü G21,
sanal su SU-31, SU-38) buradadır. Uyarı-ses U-15 ve U-17 (aynı şef kabulünün iki yarısı) K0-35 kimliğiyle, (a) ve (b) olarak işlendi.

### Sıcaklık

**S-03 Yün (N3) yalnız nötrün altında soğuma yönünde ayrı çarpan (tur 2 rev., şef kararı; T1).** Yün kesintisi yalnız vücut
nötrün altındayken (B ≤ 0) ve vücut soğurken uygulanır: hız yünsüz yalıtımla hesaplanıp `1 − 0,25·n` ile çarpılır; böylece
"1 parça %25 … 4 parça %100" aynı zırhın yünsüzüne göre tam tutar (JUnit oran testi 1e-9) ve 4 yünlü parçayla normal soğukta
nötrdeki vücut hiç soğumaz (JUnit `yunKesintisiYalnizNotrunAltinda`: −10 °C, B0 = 0, 1 saat → 0). Vücut nötrün üstündeyken
sıcaktan nötre dönüş yünsüz yalıtımla tam hızdadır (aşırı ısınmış oyuncu yünlü zırhla serin ortamda dengeye iner: 4 yün, B0 = +90,
18 °C → 10 dk'da ≈45,6, 8 °C → ≈25,6; 3 yün 18 °C → ≈36,1; JUnit `dortYunluAsiriIsinmisVucutSerinOrtamdaDengeyeIner`,
`ucYunluAsiriIsinmisVucutDaYunsuzHizlaDoner`). Nötrü geçen adım `IsiAlisverisi.adim` içinde ikiye bölünür: 0'a kadar üst tarafın
hızıyla, kalan süre 0'daki hızla (JUnit: 4 yün, B0 = +5, −10 °C, tek 120 sn adım → tam 0). Isınma yönünde yün toplam yalıtıma
(`yunBasina` 0,25) katılır ve sıcakta dezavantaj olur (N4). Isınma/soğuma ayrımı tam yalıtımlı kuvvetin işaretiyledir; nötrün
üstündeki denge noktası çevresinde en çok bir adımlık (&lt; 0,2 birim) salınım olabilir, HUD'da görünmez (paket eşiği 0,25).
İlk sürüm kesintiyi her soğuma kuvvetine uyguluyordu: 4 yünlü oyuncu bir kez aşırı ısınınca −25 °C'nin üstündeki her ortamda yanma
kademesinde kilitli kalıyordu (inceleme T1, P1). Reddedilen: yünü yalnız yalıtıma katmak (oran %25'in üstüne çıkar, 4 parça "hiç
donmama"yı vermez); kesintiyi "ortam dengesinin altı" ile sınırlamak (şef nötrü seçti: tek, ortamdan bağımsız eşik).

**S-18 GOREV §6 #9 çift hasar ölçümü — Faz 5 reçetesi (şef kararı; T4, P2 kabul).** Kodda değişiklik yok; hasar yolu yapısal
olarak kapıya bağlı (`sunucuTick` → `SicaklikKapisi.acikMi` → `guncelle` → `etkileriUygula`; başka üretim çağıranı yok) ve
Cold Sweat'li koşuda kapı "→ KAPALI". Faz 5 doğrulayıcısı: (1) `-Puyum=coldsweat` ile `sicaklik_gorsel.py coldsweat` benzeri tek
oyunculu dünya, OP; (2) `/dogalafet sicaklik set <oyuncu> -95` (kapı kapalıyken de değer yazılır, uyarı satırı çıkar); (3) vanilla
`/effect` olmadan, Cold Sweat sıcaklığı nötr bir yerde 60 sn bekle, başta ve sonda `/data get entity <oyuncu> Health`; (4) log ve
ölüm/hasar çıktısında `dogalafetler.donma`/`dogalafetler.asiri_isinma` kaynaklı hasar 0 (can kaybı yalnız Cold Sweat'ten);
(5) köprü: `/dogalafet donma 100` (ya da güneş evresini `asama ileri` ile `GUNESSIZ`/`SUPERNOVA`'ya getirip) Cold Sweat'in dünya
sıcaklığının (`/temp` ya da Cold Sweat HUD'u) `dogalafetler:donem` değiştiricisiyle düştüğü, `NORMAL`'e dönünce eski değere çıktığı.
Yeşil ölçüt: (4) 0 satır, (5) düşüş ≈ `ortam.donmaDususu` (55 °C) × G/100, Cold Sweat birimine çevrilmiş.

**S-19 Protokol sürümü dalda artırılmaz (şef kararı; T5).** `sicaklik/durum` paketi paket kümesini değiştirir (K0-07), ama Faz 2b'de
birden çok dal paket eklediği için her dalın ayrı artırması birleştirmede çakışır. `AgKanali.PROTOKOL_SURUMU` bu dalda "3" kalır;
entegratör Faz 2b birleştirmesinde bir kez "4" yapar ve çekirdek sözleşmesi §9'a "paket ekleyen dal protokolü artırmaz;
entegratör birleştirmede artırır" kuralını yazar.

### Uyarı ve ses

**U-02 Paket afet türünü ve `afetNo`'yu taşır (şef kabulü).** Görev metni afet türü ve `afetNo` istiyor. Spike S-02 "tür gizli
kalsın (radar avantajı)" demişti; çekirdeğin `AfetDurumPaketi` SIREN'den itibaren türü aynı oyunculara zaten gönderiyor ve
ON_UYARI'da siren paketi gitmez, yani radarın ön-uyarı avantajı değişmez. `afetNo` iki kuralı mümkün kılar: aynı
afetin 1 Hz tekrar isteği çalan sirende yok sayılır (`SirenKarari.istek`), eski afetin geç kalan durdurması yeni sireni
söndürmez (`durdurUygulanir`). Afet türü pakette kalır (inceleme V6): istemcide bugün yalnız log'a yazılır (kanıt
log'ları "siren_standart (deprem)" biçiminde hangi afetin sireni olduğunu buna dayanır), ileride istemci tarafı Faz 4 kodu
okuyabilir; ses seçimi `SirenTuru`'yla, ekranda gösterilmez. Kullanılmayan `SirenSesi.afet` alanı kaldırıldı. ses.md §2'deki
"siren paketi afet türünü değil siren türünü taşır" maddesinin yerini bu karar aldı (ses.md §7 başında yazılı).

**K0-35 İstemci ses kanıtı: ses dosyaya gider, otomasyon dosyaya güvenmez (şef kabulü; uyarı-ses U-15 + U-17).** İki
yarı birlikte uygulanır: yalnız (a) uygulanırsa Ana Ses 1.0 `options.txt`'e kalabiliyordu (inceleme V1, U2-1), bu yüzden ses kanıtı
alan her işçi (b)'yi de uygular.

(a) **Ses dosyaya (U-15).** Görev "Ana Ses %0 ile başla, ölçümü dosyadan yap"
diyor; ama vanilla `SoundEngine.play` Ana Ses 0 iken hiçbir sesi başlatmaz (bytecode: `Skipped playing soundEvent: {},
master volume was zero`), siren örneği tick almaz, sönüm/yeniden çalma/kaydırıcı etkisi gözlenemez. Yol: oyunun kendi
OpenAL Soft'u (LWJGL 3.3.1, `libopenal.so` içinde "Wave File Writer" arka ucu) `ALSOFT_DRIVERS=wave` + `ALSOFT_CONF`
(`[wave] file = /tmp/uyari-ses-cikis.wav`) ile yalnız dosyaya yazan aygıtı açar. Oyun yine Ana Ses %0 ile açılır;
otomasyon açık aygıtın adını OpenAL'dan (vanilla `Library.getCurrentDeviceName` gibi `ALC_ALL_DEVICES_SPECIFIER`) okur ve yalnız "Wave File Writer" ise Ana
Ses'i %100 yapar, değilse oyunu kapatır; koşu betiği de log'daki `OpenAL initialized on device` satırını denetleyip
başka aygıtta süreci öldürür (iki kat kapı). Kayıt `tools/senaryo/uyari_kayit_olc.py` ile ölçülür (siren bandı
200–1000 Hz, ortam bandı 30–110 Hz, 100 ms pencere). Gerçek dinleme sahibine kalır.

(b) **Otomasyon kuralları (U-17; inceleme V1 ve U2-1…U2-4, şef kuralı): otomasyon dosyaya güvenmez, koşu `options.txt`'i geri
yazar.** (b1) Koşu betiği (`uyari_istemci.py`) worktree'nin `run/options.txt`'ini koşudan önce bayt bayt saklar, sonunda (`finally`; hata,
SIGINT ve SIGTERM'de de: ana thread'de iki sinyal `SystemExit`'e çevrilir, geri yazım sırasında yok sayılır; istemci açıksa
önce kapatılır) geri yazar ve önce/sonra sha256'yı log'a yazar. Tur 1'in kirlettiği dosya (`bed47f2d…`: Ana Ses 1.0,
`lang:tr_tr`, susturulmuş kategoriler) bir kez temizlendi: `lang:en_us`, bütün `soundCategory_*` 1.0 (vanilla
varsayılanı), sha256 `0bf2e0e7…`; sonraki koşular bu temiz dosyayı saklayıp geri yazar. (b2) Java kapısı (`UyariOtomasyonu`):
`FMLClientSetupEvent`'te (`enqueueWork`, ana thread) Ana Ses'i kendisi 0 yapar. Gerçek sıra: kosu4'te ortak kurulum
07:11:41, ses motoru 07:11:52 ("OpenAL initialized"), oyuncu dünyaya giriş 07:12:05, ilk `ClientTickEvent` 07:12:07; Tur 2'deki
"ilk tick müzik/oyun sesinden önce" iddiası yanlıştı (ilk tick ses motorundan 15 sn sonra). İstemci kurulumu ses
motorundan önce gelir [Tur 3'te istemci koşusu yapılmadı; sıra kosu4'teki ortak kurulum zamanına dayanır]. İlk tick kapısı
yedek kalır. Aygıt doğrulanmazsa Ana Ses 0'ı `options.save()` ile yazıp oyunu kapatır; seçenek ekranı kapanırken
(`ScreenEvent.Closing`, `OptionsSubScreen.removed()` → `options.save()`'ten önce postalanır; bytecode'la doğrulandı)
Ana Ses o an 0'dır ve bir sonraki tick'te geri açılır. Yan etkisi (yalnız otomasyonda): vanilla `SoundEngine.tickNonPaused`
kategori düzeyi 0 olan kanalı durdurur (bytecode: `getSoundSourceVolume` ≤ 0 → `Channel.stop`), MASTER'daki siren o tick
kesilir ve U-11 yoluyla aynı zarfla yeniden çalar (`kosu4/runClient-uyari.log:317`; kayıtta 0,95 sn sessizlik, sonra yeni
kaydırıcı düzeyi). Kapanışta 0 yazılıp kaydedilir. Böylece betiksiz bir `-Potomasyon=uyari` koşusu da Ana Ses 1.0'ı
dosyaya yazmaz ve yanlış aygıtta ses çıkarmaz. (b3) Senaryo son adımına ("çıkış") ulaşmayan koşu "tamamlanmadı" yazılır.
Kanıt `kosu4/pencere-uyari.log`: options.txt sha256 önce = sonra (`bed47f2d…`).

**U-17 Otomasyon dosyaya güvenmez; koşu `options.txt`'i geri yazar.** → K0-35 (b).

### Gökyüzü

**G19 Gün döngüsü kapalıyken kızıl cüce (şef kararı).** `doDaylightCycle=false` iken şafak hiç gelmez: gecikme dolunca
o anki gün saatinde doğar ve log'a "Kızıl cüce: gün döngüsü kapalı, şafak beklenmedi (gün saati N)" yazılır (çekirdek
K-C29 ile aynı ruh: kural oyuncunun kapattığı döngüye bağlanmaz). Gün saatine dokunulmaz. Kod `GunesZinciri.dogusNedeni`
(`BEKLE | SAFAK | SAFAK_GECILDI | GUN_DONGUSU_KAPALI`); `kizilcuce bilgi` bu durumda "gün döngüsü kapalı: şafak beklenmez" der.
JUnit `dogusNedeniKapiSafakVeGunDongusu`, senaryo adımı `gokyuzu_sunucu.py` (inceleme G-P2-1).

**G21 runClient kilidi.** §A.6. Koşu 3'teki çakışmanın nedeni `pgrep` kapısının yarışı ve yalnız Gradle saymasıydı (inceleme
G-P2-4). Bu turdaki tek runClient (Samanyolu) kilitle koşuldu: kilit alındı → bırakıldı, 0 alarm, 0 tuş.

### Yangın

**Y-05 `kaynakBitti(kaynak, temiz)`: iki kip, vanilla'ya devir yok (şef kararı, inceleme F9).** `temiz=true` (stop,
sıfırlama, gamerule): GOREV "stop temiz biter, asılı durum kalmaz"; hücreler hemen söndürülür (`sondur` yolu, ateş blokları
kalkar, yanıcı bloklar korunur). `temiz=false` (doğal bitiş, hata): yayılma durur, bitiş zamanı en geç
`afetSonrasiSonumSaniye` (30 sn; ikinci yarıda rastgele) içine çekilir ve hücreler **yanıp biter** (yakıt havaya döner;
yaprak 9 sn, çimen 7 sn ile çoğu bu süreden önce kendiliğinden biter). İlk sürüm tek kipti ve belgeler "söner"
diyordu; inceleme F9 bunun yanlış olduğunu gösterdi. Her iki kipte o kaynağın süren alan tutuşturmaları silinir. Vanilla
ateşe devir reddedildi: bütçe/tavan/`afetBlokHasari` dışında ve `doFireTick=false` dünyalarda hiç sönmeden kalırdı.
GameTest `kaynakBittiTemizSondurur` (yalnız o kaynak söner, sahne hash'i aynı, öteki kaynak sürer) ve
`kaynakBittiYanarakBiter` (tur 3: meşe kütüğü 444 tick yanar, sönüm penceresi testte 80 tick; bitiş 444 → ≤ 80'e kısalır ve
kütük 80 tick içinde havaya döner). Kipin hangi bitişte seçileceği tur 3'te şef kararıyla değişti: gamerule (`KURAL`) artık
yanıp biter (Y-27).

**Y-19 Bekleyen silme (inceleme F1, P1; şef kararı).** Hücre kalkarken ateş bloğu yüklü olmayan bir chunk'taysa (`isLoaded`
= chunk FULL değil) blok dünyada sahipsiz, tick'siz ve sönmeden kalıyordu (`sondur`, tavan taşması, `kaldir` yolları).
Artık `atesSil` o konumu SavedData'da kalıcı `bekleyenSilmeler` kümesine yazar (biçim sürümü 2, alan `bekleyen`); hücre
silinmesi bloktan bağımsızdır. `ChunkEvent.Load` (işçi thread'inden de gelebilir: yalnız `volatile` bayrak) bir sonraki
tick'te denetimi tetikler, ayrıca saniyede bir denetlenir; yüklenen konumda blok hâlâ `afet_atesi` ise kaldırılır, konum
kümeden çıkar. Aynı konuma yeniden ateş konursa (`atesYak`) bekleyen kaydı silinir. Chunk zorla yüklenmez. Kanıt:
GameTest `yukluOlmayanAtesChunkYuklenincekalkar` (yüklü değil taklidi `yukluKaynagiAyarla` ile; sondur → blok yerinde ve
bekleyen kümede → `ChunkEvent.Load` → blok hava, yaprak yerinde) ve dedicated senaryo gerçek boşaltma ile: 1000,1000'de
`tutustur 4`, `forceload remove`, `execute if loaded` "Test failed" → `sondur` → `yangın sondur: 112 hücre söndü; 112
ateş bloğu yüklü olmayan chunk'ta` → `forceload add` → `yangın bekleyen silme: 112 ateş bloğu kaldırıldı` → `fill …
replace dogalafetler:afet_atesi` "No blocks were filled" (`sunucu/kosu2.log`).

**Y-20 Ateş modeli ve dayanak (inceleme F3; şef: ikisi de).** `AfetAtesiBlogu` vanilla `FireBlock`'un yön özelliklerini
(`north/east/south/west/up`) ve vanilla `fire` blockstate'inin multipart'ını kendi kimliğiyle kullanır (modeller vanilla
`block/fire_floor*`, `fire_side*`, `fire_up*`'ın `cutout` çocukları). Durum `dayanagaGore`: alt yanıcı ya da üst yüzü
sağlamsa zemin alevi, değilse yanıcı komşulara dönük yan/tavan alevi (vanilla `getStateForPlacement`). `canSurvive` vanilla
kuralı (alt sağlam ya da altı komşudan biri yanıcı); `atesYak` yaşayamayacağı konuma ateş koymaz; `updateShape` dayanak
kalkınca bloğu havaya çevirir, hücre bir sonraki işlenişte Y-12 ile söner. GameTest `yanAlevYonluDayanaksizSoner`: üstü
taşla kapalı yaprağın ateşi kuzey yanına `south=true` yan alevi olarak konur; yaprak kalkınca ateş aynı tick'te hava.

**Y-22 `minecraft:fire` etiketi (inceleme F2; şef kararı).** `tag(BlockTags.FIRE).add(afet_atesi)` (datagen,
`src/generated/resources/data/minecraft/tags/blocks/fire.json`, `replace: false`): su iksiri söndürür (blok kalkar → hücre
Y-12 ile söner), yol bulucu `DAMAGE_FIRE` sayar (köylü ve hayvanlar kaçınır), ateşe etiketle tepki veren modlar görür.
Vanilla ateşle çift yönlü yayılma olmaz: afet ateşinin tick'i yok, vanilla ateş afet ateşinin konumuna yayılamaz (boş
değil); vanilla ateşin tutuşturduğu blok vanilla kalır, bizim kuyruk yalnız kendi hücrelerini bilir. GameTest
`afetAtesiAtesEtiketinde`. Not (Faz 4 meteor): `BaseFireBlock.onPlace` obsidyen çerçevenin içine konan afet ateşinde
Nether portalı açar (vanilla ateş de açar). Yazım yeri K-E12 ile ortak sağlayıcıya taşındı (`DataGenerators.vanillaBlokEtiketiEkle`;
`fire.json` içeriği aynı).

**Y-23 Yağmur yalnız gökyüzüne yakın ateşi etkiler (inceleme F6; şef kararı).** `YanginMantigi.yagisDurumu(dünyada yağmur,
yağmur görüyor, biyom yağışı RAIN, örtü)`: yağmur gören (ya da yatay komşusu gören) ateş açık; yağan biyomda üstündeki
katı örtü (`MOTION_BLOCKING_NO_LEAVES` yüksekliği − y, yaprak sayılmaz) ≤ 8 blok ise korunaklı; daha derini yağmurdan
etkilenmez ve normal yayılır. JUnit `yagmurYalnizGokyuzuneYakinAtesiEtkiler` (örtü 0 ve 8 korunaklı, 9 yok, yağışsız biyom
yok, yağmur yokken yok).

**Y-24 İstemci özeti, duman sütunu tavanı, pirokümülüs, kıvılcım (inceleme F4, F7; şef kararı).** `IstemciYangin` özeti
`mc.level` değişince (boyut değişimi, yeniden giriş) ve çıkışta siler: eski boyutun koordinatlarında parçacık üretilmez.
Duman sütunu yalnız `sutunMesafesi` (160 blok) içindeki yangın chunk'larından, hücre sayısıyla ağırlıklı seçilen yerden
çıkar; canlı sütun parçacığı `sutunTavani` × kademe (Yüksek 64; Düşük 16, Orta 32, Ultra 128) aşmaz. Pirokümülüs: 12+
hücrede ağırlık merkezinin 20–28 blok üstünde açık gri (0,72–0,88), 5–9 boy, yavaş açılıp sönen küme parçacıkları, tavanın
yarısı kadar. Canlı sayım `YanginParcacigi.SUTUNLAR/KUMELER` listeleriyle; motor parçacığı attığında (setLevel, tavan) liste
3 tick içinde ayıklanır. Kıvılcım rüzgârla sürüklenir (günün rüzgârı, sunucudaki formül). Buhar menzili (64 blok) değişmedi;
yağmur karesi kamera ≤ 40 blokla alınır (şef kararı).

**Y-27 Afet bitişi merkezi abonelik (inceleme-yangin-2 F2-1; şef kararı).** `YanginYoneticisi.afetBitti(AfetBittiEvent)`:
kaynak = `YanginKaynagi.afet(olay.tur(), olay.baslangicSaati())` (çekirdek `afetNo` = `baglam().baslangicSaati()`, aynı değer),
`temiz` = `YanginMantigi.temizBitis(neden)`: `KOMUT` (`/dogalafet stop` ve `sifirla`; çekirdekte ayrı `SIFIRLAMA` nedeni yok,
`sifirla` → `durdur()` → `KOMUT`) ve `IPTAL` (ön-uyarıda iptal; normalde ateş yok) → söndür; `DOGAL`, `KURAL`, `HATA` →
yayılma durur, hücreler `afetSonrasiSonumSaniye` içinde yanıp biter. Tur 2'de `KURAL` söndürüyordu (§A1 metni); tur 3 şef
kararı onu yanarak bitirir. Çekirdeğin `temiz` bayrağı ters anlamlıdır (doğal bitiş `true`), yangına geçirilmez. `kaynakBitti`
idempotent: `temiz=false` yalnız hâlâ yayılabilen hücrelerin bitişini kısaltır, ikinci çağrı pencereyi yeniden çekmez;
`temiz=true` ikinci çağrıda söndürecek hücre bulmaz. JUnit `afetBitisNedeniYanginKipiniBelirler`; GameTest
`afetDurunceAtesiSoner` (`OyunTestiAfeti` deprem, `elleBaslat`, `tutustur(kaynak=afetNo)`, `durdur()` → ateş 0, yaprak yerinde)
ve `afetDogalBitinceAtesYanipBiter` (aynı akış, `asamaIleri` ile doğal bitiş → hücre kalır ama yayılmaz, bitiş ≤ 80 tick
(test penceresi), meşe kütüğü havaya döner, aktif 0). Log: `yangın kaynağı bitti: YanginKaynagi[tur=DEPREM, afetNo=…] 1 hücre
yayılmadan en geç 4 sn'de yanıp bitecek` ve `… (temiz): YanginKaynagi[tur=DEPREM, …] 1 hücre söndürüldü`
(`gametest-tur3-1.log`).

**Y-28 Üç görsel düzeltme, kosusuz (inceleme-yangin-2 F2-2, F2-3; şef kararı).** (1) Alev dokusu tam parlak çizilir ve
parçacık gölgelendiricisi yalnız alfa < 0,1'i atar: dış iki piksel halkası alfa 0, üçüncü halka yarım (ölçüm: dört karede
dış iki halka alfa en çok 0). (2) Sütun ve küme için ayrı 64×64 yumuşak doku `yangin_kumesi_0..3` (iki oktav gürültü, alfa
yarıçapın %45'inden kenara yumuşak sıfır; dış iki halka 0). Quad boyu sınırı (≤ 6 blok) reddedildi: pirokümülüsün genişliği
(5–9 blok, ömür boyunca ×1,6) ve sütunun büyümesi (×3) kaybolur, aynı görünüm için daha çok parçacık (tavan) gerekirdi. 64 piksel
doku pikseli 0,2–0,45 bloktan 0,1–0,22 bloka indirir. Dokular tohumlu ve deterministik (iki üretimde md5 aynı). (3) Otomasyonun
yağmur kamerası yalnız `Biome.getPrecipitationAt == RAIN` olan ateş bloklarını sayar (savana ateşi kadraja hedeflenmez; log
`kamera yagmur: N ateş bloğunun M'i yağmurlu biyomda`). Şef kararıyla ek protokol koşusu yapılmadı; görsel kontrol Faz 4 orman
yangını afetinin koşusunda.

### Donma

**K-D11 `sifirla` bizim kar/buz/kırağımızı kuyrukla 0'a çözer** (şef kararı D4, önceki "bloklar çözülmez" sürümünün
yerine). N18 `sifirla`'yı "ilk haline döndürür" diye tanımlar ve test aracıdır. `SIFIRLAMA` olayında
`ekVeri("donma").dongu = System.currentTimeMillis()`; chunk ilk görüldüğünde döngüsü farklıysa yalnız döngüyü kaydeder
(`donguyeAktar`), son işlenen G'yi, dönemi, bitleri ve kuru kayıtlarını korur; kuyruk onu normal adımlarla (bütçe × kısma)
yeni döngünün hedefine (G 0, dönem yok, kalıcı kış yok, buharlaşma 0) çözer ve kurumuş suyu geri doldurur. Oyuncunun ve
doğanın karı/buzu (bit yok) kalır. Yüklü olmayan chunk yüklenince yetişir (yetişme zaten tembel). Bilinen sınır:
`sifirla` kalıcı kış parametrelerini sildiği için eski hedef config'teki parametrelerle hesaplanır; config döngü içinde
değiştiyse kış bölgesi kenarındaki sütunlarda çözülme adımı bir eşik kayabilir (bloklar yine 0'da başlangıca döner).
GameTest `sifirlaBizimKarVeBuzuCozer` (`donma 100` → `sifirla` → sahne başlangıçla aynı, oyuncunun 3 katlı karı dahil),
`donma_sunucu.py` (dedicated: `sifirla` sonrası `Chunk [0, 0]: last processed 0`).

**K-D21 Vitrin dizisi (şef kararı).** Madde 9 ve K0-24 sırası kazanır: kırağı (10) → ince kar (15, K-D23) → kenardan buz (20)
→ kalın buz ve yapı karı (40). Eşik tablosu değişmedi; tasarım notu vitrin yüzdesi tablodaki "buz kıyıdan içeri
başlıyor" bandına (kıyıda 20, 15 blok içeride 50) çekildi: vitrin %35. Görsel tur gerçek donma süreciyle `hiz 20` altında
%15 → %35 çeker; %25 ve %35'te (yan) ve %35'te (üst) kare alır.

**K-D22 1 blok derin su donar (şef kararı D1, P1).** "Parçanın en alt bloğu donmaz" yalnız derinlik ≥ 2 iken geçerli
(madde 15: buz altında su kalsın). 1 derin sütun (altı taramada katı) ince buz → buz olur, kıyı rafları ve sığ bataklık
yürünebilir. Donmuş 1 derin sütunda su kalmadığı için çözülme yalnız buz biti işaretliyse su sayar
(`SuSutunu.planlaSeviye(..., bizim)`); oyuncunun tek buz bloğu korunur. Altı tarama dışında kalan tek blok (bilinmeyen) donmaz.
JUnit `birDerinSuTamamenDonarBizimseCozulur`, `ikiVeDortDerinSudaEnAltSuKalir`; GameTest `sigKiyiTamamenDonarVeCozulur`
(1/2/3 derin eğimli kıyı: %100'de kıyı buz, 40'ta kıyı açık, 0'da başlangıç). Görsel turdaki x=48 chunk hizalı şerit
bu 1 derin raftı (inceleme D1); kapanışı görsel tur kontrol eder (rapor).

**K-D23 İlk kar 15 (şef kararı D2, P1).** Varsayılan sıra kırağı 10 < ilk kar 15 < kıyı buzu 20 < kalın buz 40. Config yorumunda
sıra kısıtı; `DonmaAyarlari.esikler()` ilk kar ≥ `ilkBuzEsigi` görürse onu `ilkBuzEsigi − 1`'e (titreşimin üstünde) çeker ve
WARN yazar (`DonmaEsikleri.siraliKarEsikleri`; çekirdek K-C37 `Esikler` sıkıştırmasıyla aynı desen). Kırağı tablo satırı
olduğu için onun sırası veri paketi yazarındadır. JUnit `varsayilanSiraKiragiKarKiyiBuzuKalinBuz`,
`bozukSiraIlkKarIlkBuzdanOnceyeCekilir`.

**K-D25 Yapı çatılarına kar (şef kararı D8).** Madde 9 "ilerleyen aşamalarda ağaçlar, zemin ve yapılar". Yeni etiket
`#dogalafetler:donma/yapi_kar_zemini` ve ayrı eşikler `donma.esikler.yapiKarEsigi` (40 = kalın buz aşaması) /
`yapiKarCozulmeEsigi` (55 = ilk karın çözülmesi; kıyı buzundan önce kalkar). Kar katmanı yalnız bitle, çözülmede kalkar;
alttaki blok hiç değişmez. Eşik kümesi (`EsikKumesi`) yapı karı fonksiyonunu da içerir. JUnit `yapiKariGecGelirOnceKalkar`;
GameTest `yapiCatisinaKarGecGelirVeKalkar` (30'da zeminde kar, çatıda yok; 100'de çatıda 5 kat; oyuncunun çatı karı
değişmez; 0'da başlangıç). Sahnedeki "tanınmayan blok" artık demir bloğu (kırık taş yapı bloğu oldu).

**K-D31 Donma ve buharlaşma süreçlerinin ayrık aşaması yok (şef kararı, doğrulayıcı 6b, P1).** İkisi de sürekli
ilerler; `asamaIleri` override edilmez (`DonemSureci` varsayılanı `Optional.empty()`). Gerekçe: çekirdek §3a `asama ileri`
aktif afet yokken önce SÜREN süreçlere sorar ve ilk dolu çıktıyı alır; GÜNEŞSİZ'de `donma` sürdüğü için komut
durdurulmuş güneş zincirini (`oyuntesti_gunes`) değil donmayı %100'e taşıyordu ve `cekirdek_sunucu.py` "Red dwarf era"
adımında çıkış 1 veriyordu (bu dalda iki kez, temiz HEAD kopyasında da). Boş çıktıyla çekirdek sıradaki sürece geçer.
Test araçları zaten var: `/dogalafet donma <yüzde>` ve `/dogalafet buharlasma <derinlik>`. `surec_asama` dil anahtarı
silindi. **Entegratöre öneri (K-C47 adayı):** çekirdek `asama ileri [kimlik]` isteğe bağlı hedef argümanı; sürekli
süreçler ancak adıyla istenince ilerler, argümansız komut bugünkü sırayı korur. Kabul: `cekirdek_sunucu.py` bu dalda
çıkış 0 (`kanit/donma/cekirdek-senaryo/kosu1.log:454` `oyuntesti_gunes -> Red dwarf era`, `ozet.log` "senaryo sonucu 0,
süreç kodları [0, 0]"), `donma_sunucu.py` 0 (`donma-sunucu-3.log`). Donma GameTest'lerinde `asama ileri` beklentisi yoktu.

**K-D33 Kar yağışı 30 (şef kararı, inceleme-2 D2-1).** Madde 9 sırası: kırağı (10) → kar katmanı (15) → kenardan buz (20) →
**yağmur yerine kar (30)** → kalın buz / yapı karı (40). Vitrin (%35, K-D21) böylece kar yağışında; tasarım §3 "kar yağıyor"
korunur. K-D23 sıra kısıtına eklendi: `ilkBuzEsigi < karYagisiEsigi ≤ kalinBuzEsigi` (= ilk buz + kalınlık adımı);
`DonmaAyarlari.karYagisiEsigi()` dışındaki değeri en yakın sınıra çeker ve WARN yazar (`DonmaEsikleri.siraliKarYagisi`).
Config aralığı 0–100 (önceki 101 = "hiç" sıra kısıtıyla anlamsız). JUnit `varsayilanSiraKiragiKarKiyiBuzuKalinBuz`,
`karYagisiKiyiBuzuIleKalinBuzArasinaCekilir`. Otomasyon: vitrin öğlede (`time set 6000`); %25 kar yağışı eşiğinin altında
olduğu için yağmursuz (yağmur sahneyi karartıyordu), yağmur G 30'da açılır ve istemcide kar olarak yağar; %35 kar
yağışında. Kurumuş göl yatağı karesi eski su yüzeyinin 3 blok üstünden, ~8 blok yakından.

**K-D34 Yaprak altındaki su donmaz (bilinen sınır, şef kararı).** Donma ve buharlaşma yüzeyi `MOTION_BLOCKING`'den okur
(`DonmaIsleyici.yuzey`): yaprak çıkıntısı altındaki su sütunu kara sayılır ve donmaz (bataklıkta ağaç altı su açık
kalabilir; vitrin gölünde böyle sütun yok, inceleme sayımı 0). Kod değişmez. Faz 5 adayları: yüzeyi `WORLD_SURFACE`'tan
başlayıp yaprak/hava geçerek ilk su ya da katı bloka inen sütun taraması, ya da su aramasını `MOTION_BLOCKING_NO_LEAVES`'den
yapmak (kar katmanının yaprak üstüne konması korunarak).

**K-D37 Kuruyan sütuna komşu sıvı tick'i akmaz (şef kararı, inceleme-3 D3-2).** Kök neden (tur 3'teki çıkarım artık
kırmızı testle kanıtlı): buharlaşma şekil güncellemesiz yazar, komşu yeni tick almaz; ama önceden planlanmış sıvı tick'i
(sahne kurulumu, yeni üretilen chunk'ın bekleyen tick'leri, oyuncunun kıyıda blok değiştirmesi) birkaç tick'e yayılan
kurutma sırasında henüz kurumamış komşudan kuru deliğe akıyor, vanilla iki kaynak arasındaki akıcı suyu kaynağa
çeviriyor ve kayıtlı sütun su dolu kalıyordu (chunk `sonBuhar` hedefte olduğu için yeniden kuyruğa girmiyordu). İki parça:
(1) Kuruyan her konumun 4 yatay komşusundaki suyun (kaynak, akıcı ya da waterlogged) planlı sıvı tick'i silinir
(`DonmaIsleyici.komsuSiviTikleriniSil`: `hasScheduledTick` varsa tek blokluk kutuyla `LevelTicks.clearArea`; tick yoksa tahsis
yok). Komşu yalnız o anki yayılmayı kaybeder; bir sonraki komşu güncellemesinde vanilla yeniden tick planlar. (2) Kayıtlı
sütunda kurumuş kalan aralığa (`ust … ust − min(k, kaldırılan) + 1`) sonradan gelen su (kaynak ya da akıcı; waterlogged blok
değil) her buharlaşma işinde yeniden kurur, kayıt değişmez (inceleyicinin ucuz önerisi; dış etkiyle gelen su için). Geri
dolmada önce dolum, sonra kalan aralığın denetimi; katı blokta duran dolum (kayıt silinir) yeniden kurutmaz. Bütçe rezervi
değişmez (sütun başına değişim ≤ max(buhar, sonBuhar)). Kırmızı: `gametest-14-kirmizi.log` (silme geçici olarak kapalı):
`komsuSiviTikiKurumusSutunuDoldurmaz` "kuruyan sütunun komşusundaki tick silinmeli", `buharlasmaCokTickliGeriDolmaBaslangicVerir`
"kurumuş gölde 1192 su bloğu kaldı" (9 chunk `buhar 6`, sütun (15,15) üç kat kaynak su); yeşil: `gametest-15/16.log`.
GameTest'ler: `komsuSiviTikiKurumusSutunuDoldurmaz` (doğrudan yol: üstü taşla kapalı, buharlaşmayan komşu kaynak, sıvı
tick'i planlıyken göl kurur; tick silinir, 20 tick sonra kıyı sütunu kuru; kurumuş yatağa konan kaynak ve akıcı su aynı
derinlikte yeniden kurur, 2 blok; derinlik 0'da ve 20 tick sonra sahne başlangıçla aynı); `buharlasmaCokTickliGeriDolmaBaslangicVerir`
(zamanlayıcı yolu: buharlaşma sahnenin tick'leri planlıyken başlar, kuruduktan 20 tick sonra 32×32 sahnede su 0, sonra geri
dolma ve başlangıç hash'i). Kalan sınır: son derinliğe (6) ulaşıldıktan sonra dış etkiyle (oyuncu kovası, nehir akışı)
kurumuş yatağa gelen su bir sonraki buharlaşma işine (derinlik ya da G değişimi; süpernovada geri dolma) kadar kalır.

**K-D38 `buharlasma_gol_yatagi` karesi sönük kalır (şef kararı, inceleme-3 görsel).** Alçak açı yatağı tek başına okutmadı;
yatağı `buharlasma_gol_yatagi_ust` taşıyor. Faz 4 güneş patlaması vitrininde (büyüyen güneş ışığı ve buhar efektleriyle)
yeniden puanlanır; bu fazda yeni koşu yok.

### Sanal su

**SU-31 Gerçek blok koyan geliştirme komutları üretimde yok (X2-1, P1).** `su vitrin …` ve `su ay var|yok` yalnız
`SuKomutlari.gelistirmeKomutlari()` (`!FMLEnvironment.production` ve `dogalafetler.otomasyon` özelliği, yani
`-Potomasyon`) doğruyken kaydedilir. Dallar ayrı sınıfta (`SuVitrinKomutlari.ekle(su)`) ve aynı `su` düğümüne eklenir:
çekirdek `KomutKaydi` aynı adlı ikinci alt komutu reddeder (koşu 13'te ayrı `su` düğümü dünya yüklemesini durdurdu). `SuVitrin`
ve `SuVitrinKomutlari` başka hiçbir yerden anılmaz; üretimde sınıf yüklenmez. Kanıt: dedicated senaryoda `dogalafet su vitrin`
ve `dogalafet su ay yok` → `Incorrect argument for command` (kayıtlı değil); üretim bayrağı GameTest'te taklit edilemediği için
kod okuması + bu satır.

**SU-32 Buharlaşma inişi durdurmadan bağımsız (X2-6, şef kararı; SU-25'in yerine).** Deniz yükselmesinin buharlaşmayla sıfıra
inmesi hem `KURAL` hem `KOMUT` duruşunda uygulanır. Gerekçe: gamerule afetin seçilmesini kapatır, dünyanın fiziğini değil;
`stop` yükselmeyi durdurur, buharlaşma güneş sürecinin sonucudur. Böylece N10 sırası (önce sanal artış sıfır, sonra gerçek su)
`ayCarpmasiAfeti=false` + güneş açıkken de korunur. §3a notu: deniz süreci durdurulmuşken dönemin `deniz()` alanı yalnız
buharlaşma yüzünden değişir. GameTest `denizSureciAyYokOluncaBaslarDururBuharlasmaSifirlar`: KOMUT ile durmuşken buharlaşma 3 →
artış 4 → 1, KURAL (`gamerule ayCarpmasiAfeti false` + `kurallariDenetle`) ile durmuşken buharlaşma 5 → 0 ("artışlar=[2.5, 1.0,
0.0]").

**SU-38 Uzak LOD kurulumu da bütçeli (S3-1).** Uzak LOD artık tek karede kurulmaz: `uzakBaslat` kökeni, yarıçapı ve zamanı
sabitler, `uzakParca` her çağrıda bir LOD sütunu (`gx`) kurar, son sütunda tampon yüklenir; o sürede önceki tampon çizilir.
Yakın halka ve uzak LOD aynı `KurulumButcesi`'ni (2 ms + borç, config açıklaması güncellendi) paylaşır; ikisi de bekliyorsa bu
tick'te az harcayanın parçası çalışır (`siradaki`, eşitlikte yakın). Yenileme: dalga/çekilme/mağara varken 10 tick, sakin
denizde 100 tick (güneş açısı için), kamera 8 bloktan fazla kayınca ya da deniz artışı değişince hemen; yapım sürerken yenisi
başlamaz. SU-37 etekleri aynı parçalarda yayılır. Ölçüm log'u `uzak_yayılma` ve tick başına gerçek işi (yakın + uzak) yazar.
JUnit: `ikiIsVarkenAzHarcayanOnceCalisir`, `uzakLodParcalariYakinHalkaylaButceyiPaylasir` (yakın halka sürekli 10 ms, uzak her
10 tick'te 16 ms = koşu 17'nin en kötüsü, 0,12 ms'lik sütunlar: tick başına iş ≤ bir parça + 2 ms, uzak en çok 17 tick'te
biter, iki iş de aç kalmaz). FPS ve `kurulum ms` tepe ölçümü (hedef ≤ ~3 ms, ısınma dışı) Faz 4 tsunami koşusunda (bu turda runClient yok).

**SU-40 Mağara görseli: SU-35 kod yolu ve Faz 4 yeniden puanlama (S3-3, şef kararı).** Kod okuması: koşu 14 mağara (dolu)
karesinde kamera y 12,3, dolu cebin yüzeyi `ustY` 13 − 0,11 = 12,89, yani kamera **suyun altındaydı**; `yansimaGucu` kamera
yüzeyin altındayken 0 döner, tavan yansıması hiç çalışmadı (amber %0,01 bunu açıklıyor; koşu 12'nin kamerası 14,8'deydi).
Kamera y 14'e alındı. Mağara-sualtı: amber sis rengi uygulanıyordu ama sis uzaklığı 56 blok, mağara ~17 blok; sis rengi kareye
~%30'dan az giriyordu. Gökyüzü görmeyen, blok ışığı alan suda sis yakına çekilir (56 → en yakın 24 blok, sıcak payla
ölçekli). Görsel doğrulama ve yeniden puanlama Faz 4 tsunami vitrininde (§A2 notu, hedef ≥ 3/4).

## Şef kararları (Faz 3)

Faz 3 parçalarında şefin verdiği ya da kabul ettiği kararlar (kaynak `.vibe/kararlar/<parça>.md` §B; parça bölümlerinde aynı
kimliklerle atıf vardır) ve entegrasyon görevinde şefin istediği K-E13, K-E14. Başlığında "şef kararı" ya da "(şef, …)" geçen
kararlar buradadır.

### Entegrasyon

**K-E11 `/dogalafet sifirla` yangın hücrelerini söndürür (şef kararı).** → Faz 2b — Entegrasyon. Faz 3 parçaları `SIFIRLAMA`'yı
uygular: meteor M-18, balıkçılık `BuzBalikciligi::donemDegisti`, başarım A-27; radar ve yiyecek kalıcı durum tutmaz.

**K-E12 Etiket sağlayıcı sahipliği (şef kararı).** → Faz 2b — Entegrasyon; eşya sürümü K-E17 (b).

**K-E13 Dedicated senaryolar ayrı portla koşabilir; ağır süreç kapısı portu da bekler.** Kök neden: `tools/senaryo/*_sunucu.py`
betiklerinin çoğu `run-server/server.properties`'in varsayılan 25565 portunu kullanıyordu. Her worktree'nin `run-server/`
klasörü ayrı olduğu için paralel işçilerin tek ortak kaynağı port; ikinci sunucu bağlanamayıp düşüyordu. Karar: ortak
yardımcı `cekirdek_sunucu.py`:

- `argumanlar()`: konumsal `<log klasörü>` ya da `--kanit <dizin>` (ikisi farklıysa hata), `--port <n>` (varsayılan 25565,
  davranış değişmez). Port senaryo süresince `server-port` olarak yazılır; sonda `server.properties` özgün içeriğine döner.
- `port_bos()`: `SO_REUSEADDR` ile bind denemesi; dinleyen soket (IPv6 çift yığın dahil) dolu, TIME_WAIT boş sayılır.
- `agir_surec_kapisi(port)`: Gradle < 2, boş RAM ≥ 4 GB ve port boş olana dek 60 sn'de bir, en çok 20 dk; açılmazsa çıkış 3.

Ortak yardımcıyı kullanan meteor/yangın/yiyecek/su/uyarı/gökyüzü aynı argümanları ve kapıyı alır (uyarı `--istemci`
quickPlay adresine portu geçirir; istemcili kapı RAM ≥ 10 GiB + port). Kendi portu olan donma (25593), balıkçılık (25596),
eşya (25591) varsayılanlarını ve kanıt klasörlerini korur, `--port`/`--kanit` ve ortak kapıyı alır; bu üçü portu önceki gibi
`server.properties`'te kalıcı bırakır (izlenmeyen dosya; sonraki senaryo kendi portunu yazar) (eşyanın çıktıyı işçinin
klasörüne yazması, doğrulama-radar-sismograf-2 (c), böylece kapandı). `gradle_kapisi.sh`: `KAPI_PORT` verilirse portu dinleyen
süreç kalmamasını da bekler (`ss -Hltn`). Sınır: aynı worktree'de `run-server/` (server.properties, senaryo dünyaları)
paylaşıldığı için senaryolar yine sırayla koşar. Kanıt: duman testi (`--help` 10/10; port dinlenirken `port_bos` False,
kapanınca True; `server.properties` yaz/geri al), entegrasyon 3 senaryoları 25611–25621'de (`.vibe/rapor/entegrasyon-3.md`).
Not (K-E20 birleştirmesi): `gradle_kapisi.sh` Gradle'ı `LC_ALL=C.UTF-8` ile başlatır; `tr_TR.UTF-8` altında sonuç satırı
`BUİLD SUCCESSFUL` yazılıyor ve `BUILD` grep'leri kaçırıyordu (doğrulayıcı-2 notu). Log'un ilk satırı `FILTRE=...` (K-E20).

**K-E14 Donma test sahnesi kurulumu zamanlayıcının yarım işini atar.** Belirti: `kiyiUzakligiChunkSinirindanBagimsiz` (batch
`donma_zamanlayici_5`) balıkçılık doğrulamasında 3 koşudan 1'inde "113 sütun … beklenen ice gerçek water" ile düştü
(`.vibe/kanit/balikcilik-dogrulama/gametest-1.log`). Kök neden: `DonmaZamanlayici.ISLER` (yarım chunk işleri) statik; yalnız
sunucu açılışında, chunk boşalınca ya da iş bitince temizlenir. Önceki testlerin genel G evresinde (kapsam yok: balıkçılık
`donma(y, 50)`, gökyüzü G=100) her zaman yüklü spawn chunk'larında yarım kalan işler, sonraki test kapsamı daraltınca da
`ISLER`'de kalıyordu. Test kurulumu `donguyeGec` ile chunk'ın `sonG`/bit/yarım verisini sıfırlıyor ama işi atmıyordu; kıyı
sahnesindeki chunk'ın eski işi (ör. 95→100; (95, 100] aralığında eşik yok, K-D8) bitince `sonG=100` yazılıyor, sonraki adımlar
100→35 çözülme yönünde koşuyor ve su donmuyordu. Kanıt: üç balıkçılık log'unda `donma_zamanlayici_5`'e girerken
`[donma] özet` `süren=1..2`. Aralıklılık, iş ilerlemesinin nanoTime bütçesine bağlı olmasından. Üretim tutarlı: yarım iş
bitince yazdığı `sonG` kendi işlediği bloklarla uyumlu; tutarsızlığı yalnız veriyi dışarıdan sıfırlayan test kurulumu
yaratıyordu. Düzeltme: yalnız GameTest için `DonmaZamanlayici.chunkVerisiniSifirla(chunk, dongu, g, cozulme)` veriyi
sıfırlar, yarım işi atar (log `[donma] test sahnesi: chunk x,z yarım işi atıldı (G a -> b, n/256 sütun)`), chunk yüklüyse
yeniden kuyruğa alır; `zamanlayiciHazirla` ve `simulasyonDisiGorunenChunkDonar` bunu kullanır. Test beklentileri değişmedi.
Kanıt: birleşik koşu 2'de `chunk 4,4 yarım işi atıldı (G 10 -> 5, 6/256 sütun)` (eski iş gerçekten vardı), üç koşu 131/131.
Reddedilen: testte bekleme/yeniden deneme (kök nedeni gizler); `kapsamAyarla`'da tüm işleri atmak (kapsam dışı chunk'ların
geçerli işini de silerdi).

**K-E18 `/dogalafet odul meteor` (KOM.13; şef kararı, sahibi entegratör).** Şartname KOMUTLAR: "/dogalafet odul meteor (test için
Meteorit Parçası / Kozmik Çekirdek verme)". Biçim `/dogalafet odul meteor [parca|cekirdek|hepsi] [adet 1–64] [oyuncu]`: argümansız
komutu veren oyuncuya birer Meteorit Parçası ve Kozmik Çekirdek (şartnamedeki "Parçası / Çekirdek" ikisini birden karşılar);
`parca`/`cekirdek` yalnız o eşyadan, `hepsi` ikisinden `adet`'er; oyuncu verilmezse komutu veren (konsolda oyuncu şart). Alt
komut meteor paketinde (`MeteorKomutlari.odulKomutu`, `KomutKaydi` ile `odul` kökü; yetki 2, diğer `dogalafet` alt komutlarıyla
aynı). Verme `MeteorAPI.odulVer(oyuncu, parca, cekirdek)` (meteor §A): eşyalar soğuk (sıcaklık etiketi yok; yanma krater
ödülüne özgü), envantere girer, sığmayan oyuncunun önüne düşer. Tür ayrıştırma saf `meteor.mantik.OdulIstegi` (büyük/küçük harf
duyarsız; bilinmeyen tür hata iletisi `commands.dogalafetler.odul.meteor_tur`, verme yok). Lang datagen (`MeteorCevirileri`).
Madde 13c "ödüller yalnız meteordan" oynanış kuralıdır; OP test komutu şartnamenin kendi istisnasıdır. Kanıt: JUnit
`OdulIstegiTest` (3), GameTest `odulKomutuMeteorEsyalariniVerir` (batch `meteor_odul_komut`: argümansız 1 + 1, `cekirdek 3 <oyuncu>`,
bilinmeyen tür 0, ödüller soğuk, OP olmayan reddedilir), `meteor_sunucu.py` adımı (konsolda `odul meteor cekirdek 1 Kimse` → "No
player was found": komut kayıtlı ve ayrıştırılıyor).

**K-E19 Faz 4 hızlandırma (şef kararı, 2026-11-06).** (a) **Ağır süreç tavanı ölçüme bağlı:** kullanılabilir RAM sürekli
≥ 12 GB ise üç `./gradlew`, değilse iki. Ölçüm `nproc` 8, `free -m` kullanılabilir 12 093 MiB (11,8 GiB; bir GameTest
çalışırken 8 979 MiB) → **iki** kalır (`docs/MAKINE.md` "Faz 4 öncesi ölçüm", GOREV §2). (b) **Gradle daemon değişmez:** `org.gradle.daemon=false` + `org.gradle.jvmargs=-Xmx3G`
ile istemci JVM'i (`-Xmx64m`) uyuşmadığından her çağrı tek kullanımlık daemon forklar ("single-use Daemon"). Ölçüm `cleanTest
test --offline` (472 JUnit), ikişer koşu (`.vibe/kanit/faz3-kapanis/zaman-*.log`): A bugünkü düzen 48 / 53 sn; B
`GRADLE_OPTS=-Xmx3G` (jvmargs'ı istemciyle eşleme denemesi) 37 / 33 sn ama fork sürdü (log'da yine "single-use"; A'dan kısa
oluşu forkla ilgili değil); C `--daemon` 38 sn (daemon açılışı) / 13 sn (sıcak daemon). Kazanç yalnız aynı daemon'u yeniden
kullanan ardışık koşularda (~35 sn). Bedeli: boşta 3 saat yaşayan, `-Xmx3G`'ye kadar büyüyen ve ağır süreç kapısının
`GradleWrapperMain` sayacına girmeyen JVM'ler; paralel worktree'lerdeki eş zamanlı her derleme ayrı daemon açar ve iki tavanlı,
11,8 GiB'lık makinede 4 GB payını yer. Daemon kapalı kalır (GOREV §2 "her derleme bitince RAM boşalır"), `gradle.properties`'e
dokunulmadı. (c) **Doğrulama
turları** (GOREV §6 sonu): tur 2+ yalnız değişen yolu koşar, değişmeyen kanıt önceki rapordan atıfla aktarılır; GameTest
varsayılan 2 koşu, aralıklı şüphesinde 3. (d) **Kabul kuralı** (GOREV §3 "Denetim zinciri"): inceleme P0 = P1 = 0 ise parça
kabul; P2 için ayrı işçi turu açılmaz, inceleyici "Entegratöre devredilen P2'ler" bölümü yazar (dosya:satır + tek satır
düzeltme), entegratör birleştirmede uygular ya da açık noktaya yazar. Gerekçe: Faz 3'te P2 turları parça başına 3–4 tura
çıktı (meteor 4, başarım 3, balıkçılık 3, radar 2 tur). Doğrulama iki kuyrukta: `dogrulayici` (portlar 25611–25620),
`dogrulayici-2` (25631–25640); işçi dedicated portu 25640 + (workspace − 90). (e) **Yoklama yok** (GOREV §2): işçi,
doğrulayıcı ve entegratör `sleep N` döngüsüyle yoklamaz; arka plan işini bloklayan `wait` ile bekler. Faz 4 görev taslakları
`.vibe/gorev/4-<parça>.md` bu kuralları ve GameTest filtresini (`-Ptest_filtre=<regex>`, K-E20, dal `is/gametest-filtre`) taşır.

**K-E20 GameTest filtresi: işçi yalnız kendi testlerini koşar; kabul ve entegrasyon filtresiz (şef kararı, dal
`is/gametest-filtre` `62ed7f4`).** `runGameTestServer` her koşuda bütün testleri koşuyordu. Sistem özelliği
`dogalafetler.gametest.filtre` (Java regex) verilirse bir test, test sınıfının basit adı, batch adı ya da test fonksiyonunun
(metot) adından birinde regex `find` ile uyarsa koşar; eşleme büyük/küçük harfe duyarlıdır. Özellik yoksa ya da boşsa davranış
değişmez. Geçersiz regex açık hata verir (`IllegalArgumentException`, özellik adı ve desen iletide; GameTest sunucusu
açılmaz, çıkış 1). Saf eşleme `core.mantik.OyunTestiFiltresi` (JUnit `OyunTestiFiltresiTest`), sunucu ucu
`core.OyunTestiSecimi`. Yer: Forge `@GameTestHolder` metotlarını `RegisterGameTestsEvent`'ten sonra kaydeder ve `Main` batch'leri
hemen kurar; arada kanca yoktur. Bu yüzden kayıt defteri tam kalır, süzme `ServerAboutToStartEvent`'te
`GameTestServer.testBatches`'te (SRG `f_177587_`) testler başlamadan yapılır; boşalan batch düşer, kısmen kalan batch adı ve
before/after işlevleriyle yeniden kurulur. Geçiş: `build.gradle` `gameTestServer` `-Ptest_filtre=<regex>` verilirse özelliği
geçirir; `tools/senaryo/gradle_kapisi.sh` `TEST_FILTRE` ortam değişkenini `-Ptest_filtre` olarak ekler. Açılışta tek satır:
`[dogalafetler] GameTest filtresi '<regex>': N/M test` ya da `GameTest filtresi yok: M test`.

- **Bekçi kararı:** `CekirdekOyunTestleri.hicBirBatchSatirKirmaz` filtreden bağımsız her koşuda kalır ve tam kayıt defterini
  sayar; batch ≤ 7 kuralı (K-E16) filtreli koşuda da korunur. Her koşu boş dünya kuralı değişmedi.
- **Süreler:** işçi `-Ptest_filtre=Meteor` 57 sn (16/157), filtresiz 170 sn; doğrulama-2 Meteor 75 sn (16/157), filtresiz
  157/157 ×2 169–185 sn, geçersiz regex çıkış 1 (Gf1 kapandı; `.vibe/kanit/gametest-filtre-dogrulama/`).
- **Gf2 (birleştirmede):** filtre bekçi dışında hiçbir teste uymazsa sunucu açılmaz:
  `OyunTestiFiltresi.bosSecimiReddet` `IllegalStateException` ("<özellik> '<desen>' bekçi dışında hiçbir teste uymadı") fırlatır,
  geçersiz regex'le aynı yoldan çıkış ≠ 0; yanlış yazılan regex yalnız bekçiyi koşup yeşil bitmez. JUnit
  `bekciDisindaHicTesteUymayanFiltreAcikHataVerir`.
- **Gf4 (birleştirmede):** `gradle_kapisi.sh` log'un ilk satırına `FILTRE=<regex> (kabul için geçersiz koşu)` ya da
  `FILTRE=yok` yazar; dışa aktarılı kalmış `TEST_FILTRE` kabul koşusunu sessizce süzemez. Doğrulayıcılar kabul loglarında
  `FILTRE=yok` arar (GOREV §6).
- **Aralıklı düşme (gametest-filtre AÇIK):** filtresiz ilk koşudaki `simulasyonDisiGorunenChunkDonar` "aşama 0" düşmesi
  K-E21 ile kapandı.
- **Açık (Gf3):** filtresiz yol da `f_177587_` yansımasıyla batch listesini okur (yalnız toplamı loglamak için); kabul
  koşuları gereksiz yere SRG adına bağlı. Şef kararıyla dokunulmadı.
- **Reddedilen:** `RegisterGameTestsEvent`'te süzmek (Forge metotları olaydan sonra ekler); `GameTestRegistry.TEST_FUNCTIONS`'ı
  süzmek (bekçi batch'lerin yalnız bir kısmını sayardı); vanilla `/test runall` ya da mixin.

İşçi kullanımı: `TEST_FILTRE='Meteor|meteor_' tools/senaryo/gradle_kapisi.sh <log> runGameTestServer` ya da
`JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 ./gradlew runGameTestServer --offline -Ptest_filtre='Meteor|meteor_'`.

**K-E21 `simulasyonDisiGorunenChunkDonar` hedef chunk'ı ve 8 komşusunu eşzamanlı üretir; dünya üretimi tick sayacının dışında
kalır (şef kararı, dal `is/donma-aralikli` `0bf06bb`).** Belirti: "simülasyon dışı chunk 60'a ve geri 0'a yetişmedi (aşama 0)"
(`gametest-filtre/gametest-tam.log`:790); aşama 0 = hedef hiç `DonmaZamanlayici.islenebilir` olmadı. **Kök neden:** aşama 0,
z = −40'taki hedefin ve 8 komşusunun arka plan iş parçacıklarında üretilmesini (duvar saati) bekler; `timeoutTicks = 3000` oyun
tick'i sayar ve GameTest sunucusu tick'leri uyumadan koşturur (`GameTestServer.waitUntilNextTick` yalnız `runAllTasks`; vanilla
50 ms bekler). Aynı 1–4 sn'lik üretim yüke göre 80 ile 3000+ tick arası tutar; düşen koşuda 3000 tick ~2,5 sn'de doldu.
**İstatistik (83 kayıtlı koşu):** 68'i ≤ 60 tick (saklanan dünyada bölge diskte kalıyordu); uzunlar bölgenin ilk üretildiği
koşular (`entegrasyon-3/gametest-2` 1600, `radar-sismograf/gametest-4` 2400, `donma/gametest-9` 600). K-E16'dan sonra her koşu
üretim: 260 / 80 / 120 / 300 / 660 / 600 ve bir kez 3000+ (düştü); tick hızı 160–1540 tick/sn. K-E15 (c) kendi iletisi için
doğruydu, "aşama 0" için yanlış teşhisti. **Düzeltme (yalnız test):** `addTicket(GORUS_BILETI, hedef, 31, hedef)`'ten sonra
hedef ve 8 komşusu `ServerLevel.getChunk(x, z)` ile yüklenir (ana iş parçacığında `managedBlock`; üretim tick ilerlemeden
biter). `getChunk`'ın geçici `TicketType.UNKNOWN` bileti (seviye 33, ömrü **2 tick**: zaman aşımı 1, `purgeStaleTickets` ikinci
temizlikte siler) ticking izleyicisine girmez; senaryo, `isPositionTicking` denetimleri, `timeoutTicks` ve G eşikleri aynı. Tanı
satırı `[donma] test sahnesi: chunk x,z ve 8 komşusu eşzamanlı üretildi (N ms)`. Kanıt (`.vibe/kanit/donma-aralikli/`): JUnit
469, GameTest 157/157 ×3, üretim 255 / 278 / 372 ms (doğrulama 189–235 ms), `donma_zamanlayici_7` ≤ 1 ilerleme satırı (≤ 20 tick).
Faz 4 kuralı (GOREV §6): uzak chunk `level.getChunk` ile eşzamanlı üretilir; duvar saatine bağlı arka plan işi tick zaman
aşımıyla beklenmez. **Reddedilen:** `timeoutTicks`'i büyütmek (oran yine yüke bağlı); hedefi yüklü bölgeye yaklaştırmak (K-D26
sahnesi bozulur); GameTest sunucusuna tick sınırı mixin'i (üretim jar'ına test yaması, bütün koşular yavaşlar); üretim kodunu
değiştirmek (bekleme vanilla üretimde); yeniden koşup geçeni kabul etmek (olasılık yükle artar).

**K-E22 Faz 4 engelleri: test afeti gerçek kaydı ezmez, başarım testleri geçici kayıtsızlık kullanır, balıkçılık dönemi geri
koyar (şef kararı, 2026-11-06).** Üç Faz 4 işçisi (deprem-sinkhole, asit-orman-yangını, tsunami) aynı üç engele çarptı.

- **(a) Kayıt sırası:** `-PoyunTestiAfeti=deprem[,sinkhole]` kurucuda gerçek `AfetKaydi.kaydet(DEPREM)`'den önce koşuyordu; gerçek
  kayıt "deprem zaten kayıtlı" `IllegalStateException` atıyor, mod yüklenmiyordu (`cekirdek_sunucu.py`, `esya_sunucu.py`,
  `uyari_istemci.py`, `uyari_sunucu.py`, `basarim_sunucu.py`, `radar_gorsel.py` bayrağı taşır). Karar: **gerçek kayıt önce**.
  `DogalAfetler` kurucusunda afet kayıtları (Faz 4 satırları) `OyunTestiAfeti.ozelliktenKaydet()`'in üstüne yazılır (yorum
  satırı yeri gösterir). `ozelliktenKaydet` gerçek afeti kayıtlı türü `WARN` ile atlar ("Oyun testi afeti deprem atlandı …
  gerçek afet kayıtlı (K-E22)"); güneş türü atlanırsa `OyunTestiSureci` de kaydedilmez, gerçek `GunesZinciriSureci` kalır.
  Saf seçim `core.mantik.OyunTestiAfetSecimi` (JUnit `OyunTestiAfetSecimiTest`, 3). Sıra yine ters yazılırsa (gerçek kayıt
  `ozelliktenKaydet`'ten sonra) `AfetKaydi.kaydet` hatası nedenini ve K-E22'yi söyler. Betiklere dokunulmadı: Faz 4 sonunda
  bayrak etkisiz kalır (yalnız uyarı); Faz 5 temizliğinde betiklerden kaldırılır ya da kayıtsız türle değiştirilir.
- **(b) Başarım testleri ve kayıtlı yayıncı:** `olaysizAfetteYedekTahminSayilir` ve
  `olaysizAfetteYedekKayitliYayincidaYedekKapali` SINKHOLE/ASIT_YAGMURU/ORMAN_YANGINI/TSUNAMI'yi "kayıtsız yayıncı" örneği
  sayıyordu; Faz 4 kuralı `yayinciKaydet` zorunlu olduğundan dört işçinin dalında da kırılırdı. Karar:
  `BasarimIpucuEvent.geciciKayitsiz(tur, ipucu)` (AutoCloseable; `IpucuKaynaklari.kaldir` artık önceki kaydı döndürür, `close`
  kayıtlıysa geri koyar; yalnız GameTest/otomasyon, javadoc'ta). İki test bununla sarıldı; JUnit
  `IpucuKaynaklariTest.kaldirVeGeriKoyOncekiKaydiKorur`. Başarım §A.3'e not düşüldü.
- **(c) Balıkçılık dönem hijyeni:** `BalikcilikOyunTestleri.donma()` global donmayı 50/60'ta bırakıyordu (Faz 3 açık noktası);
  `YanginOyunTestleri.afetAtesiKendiBasinaYayilmaz` komşu sahneye kar girince 25 ≠ 27 ile düşüyordu (`main`'de yerleşim
  sayesinde işlenmiyordu, Faz 4 yerleşimi kayınca düşer). Karar (K-E15 b): balıkçılığın altı batch'i `@BeforeBatch` ile
  global dönemi (donma, çözülme, güneş evresi ve ilerlemesi) saklar, `@AfterBatch` ile geri koyar. Test başına geri koyma
  reddedildi: aynı batch'in testleri eşzamanlı koşar, kısa test süren kardeşinin donmasını erken sıfırlardı. Forge
  `@GameTestHolder` sınıfının bütün metotlarını `GameTestRegistry.register(Method)`'a verir, o da `BeforeBatch`/`AfterBatch`'i
  batch adıyla kaydeder (`javap`; K-E20 filtresi batch işlevlerini korur). Yangın testine dokunulmadı. "Koşu sonunda G = 0"
  bekçisi yazılmadı: GameTest batch sırası son koşan testi garanti etmez.

Kanıt `.vibe/kanit/k-e22/` (`ozet.txt`).

**K-E23 Çekirdek senaryosu taklidi gerçek afetin yerine geçirir (`!tür`); ağır süreç kapısı zaman aşımında görevi
başlatmaz (şef kararı, 2026-11-06).** (a) **Sorun:** K-E22'den sonra gerçek `gunes_patlamasi` kayıtlıyken
`-PoyunTestiAfeti=deprem,gunes_patlamasi` bayrağı güneş türünü atlar; `cekirdek_sunucu.py` K-C41 bloğu taklidin üç
`asama ileri`'de biten AKTİF'ine ve `oyuntesti_gunes` sürecine dayanıyordu (güneş §E c; gerçek afet 5 günlük AKTİF,
`gunes_zinciri`). Deprem adımları da deprem birleşince gerçek depremi (blok hasarı, süre, geri yükleme) açardı. **Karar:**
`!` önekli tür (`-PoyunTestiAfeti=!deprem,!gunes_patlamasi`) gerçek afeti kayıtlıysa onun fabrikasını oyun testi afetiyle
değiştirir (`AfetKaydi.oyunTestiEz`, paket içi; yalnız `ozelliktenKaydet`), kayıtsızsa düz kayıt gibi davranır; log "Oyun testi
afeti <tür> gerçek afetin yerine kaydedildi (…, K-E23)". Güneş türü taklit olunca `OyunTestiSureci` kaydedilir ve
`GunesZinciriSureci`'nin mevcut koruması zinciri kaydetmez (K-C41 süreci tek). Saf seçim `OyunTestiAfetSecimi` (`ezilecek` listesi;
JUnit `unlemliTurGercekKaydinYerineGecer`). `cekirdek_sunucu.py` kendi `Sunucu`'sunu `!deprem,!gunes_patlamasi` ile açar ve iki
açılışta iki taklit satırını ister (yoksa "mod K-E23'ten eski" hatası); `start kasirga 3` kasırga kayıtsızsa "not registered
yet", kayıtlıysa başlat + `stop`. Ortak `Sunucu`'nun varsayılan bayrağı (`deprem,gunes_patlamasi`, diğer betikler) değişmedi.
**Reddedilen:** blokta 7 `asama ileri` + `gunes_zinciri` (çekirdek senaryosu güneş afetinin süresine ve donma/buharılaşma
yan etkilerine bağlanır, deprem adımları yine gerçek depreme düşer); K-C41 bloğunu `gunes_patlamasi_sunucu.py`'ye devretmek
(çekirdek kendi sözleşmesini sınamaz, başka parçanın betiklerine bağlanır); kalıcı `oyun_testi` `AfetTuru` (üretim yüzeyine
test türü sızar: N18 adları, tab tamamlama, zamanlayıcı, gamerule haritası, dil, başarım sayımları; ayrıca K-C41 tam da
`gunes_patlamasi` türünü, onun gamerule'ünü ve ona bağlı dönem süreçlerini ister, sentetik türle sınanamaz). Aynı bayrağa
dayanan diğer betikler `!tür`'e geçti (her biri taklidin anlık yaşam döngüsüne dayanıyor; gerçek afeti sınamak afetin kendi
senaryosunun işi): `esya_sunucu.py` `!deprem` (radar/sismograf kaydı + deprem 2 yaşam döngüsü; gerçek depremin blok hasarı ve
süresi kabulü bozmasın; log satırı iki taklit iletisini de tanır); `uyari_sunucu.py` `!deprem,!gunes_patlamasi` (siren sırası
afetin süresinden bağımsız; güneşin gerçek kaydı K-E22 ile zaten atlanıyordu); `uyari_istemci.py` `!deprem` (istemcili siren
koşusu, aynı gerekçe); `basarim_sunucu.py` `!deprem` (`start deprem 5` + üç `asama ileri` → doğal bitiş taklidin anlık
BITIS'ine dayanır); `basarim_gorsel.py` `!kasirga` (`BasarimOtomasyonu` "kasırga S5" adımı hızlı yaşam döngüsü bekler);
`radar_gorsel.py` `!deprem,!sinkhole,!kasirga,!gunes_patlamasi` (otomasyonun kare sırası taklidin aşamalarına göre; radar ve
sismograf göstergesi afetin türünden ve çekirdeğin `etkiAlani`/`kalanOku`'sundan okur). Kanıt (`.vibe/kanit/k-e23-betik/`, `main`
`58f2fa6`, güneş gerçek kayıtlı): `esya_sunucu.py` 25611, `uyari_sunucu.py` 25612, `basarim_sunucu.py` 25613 → üçü de çıkış 0 (esya: `12 ağ paketi … protokol 5`, `invalid dist`/`Exception`/`ERROR` 0, deprem taklidi kaydı; uyarı: `gunes_patlamasi gerçek afetin yerine kaydedildi (K-E23)`; başarım: deprem 5 doğal bitiş). Üç runClient
betiği (`uyari_istemci`, `basarim_gorsel`, `radar_gorsel`) koşulmadı: yalnız `py_compile` + bayrak satırı; Faz 5 görünür koşuda
sınanır. Bayrağı bilerek varsayılan (`deprem,gunes_patlamasi`, taklit atlanır) bırakan tek betik `gunes_patlamasi_sunucu.py`'dir
(gerçek afeti sınar).
(b) **Kapı zaman aşımı (doğrulayıcı-2):** `gradle_kapisi.sh` 20 dk dolunca kapı açılmasa da görevi başlatıyordu (GOREV §2,
K-E19 a tavanını sessizce deliyordu); artık görev başlatılmaz, log'a ve çıktıya `KAPI: açılmadı (N dk)`, çıkış 3
(`agir_surec_kapisi` ile aynı); `KAPI_DK` üst sınırı (varsayılan 20), `TEST_FILTRE`/`KAPI_PORT` davranışı aynı. Kanıt
(`.vibe/kanit/k-e23/`): `build --rerun-tasks` 0, `warning:` 0, JUnit 482/0; main'de `cekirdek_sunucu.py --port 25611` 0 (iki taklit
satırı "için kaydedildi", kasırga "not registered yet"); güneş dalı `84b2faf` + `26acbfd` geçici birleşiminde (`.worktrees/k-e23-gunes`, dal dosyalarına yazılmadı) `cekirdek_sunucu.py --port 25612` 0 (`cekirdek-gunes/kosu1.log`:56 "gunes_patlamasi gerçek afetin yerine kaydedildi", :390 `Solar Flare phase: Ending`, :455-499 `oyuntesti_gunes` zinciri; doğrulayıcı-2'nin K-E23 öncesi düşüşü `gunes-patlamasi-dogrulama/sunucu-cekirdek/kosu1.log`:328-329 kapandı). Değişmemiş güneş worktree'sinde main betiği koşmaz: `!` desteği modun Java tarafındadır, dal `main`'i (`26acbfd`) alınca geçer; kapı dolu portla `KAPI_DK=1` → 60 sn'de
`KAPI: açılmadı (1 dk)`, çıkış 3, Gradle başlamadı (`kapi-zaman-asimi.*`).

**K-E24 Döngülü ortam ses katmanı −26…−30 LUFS (şef kararı, 2026-11-06; 2026-11-07 düzeltildi).** Döngülü ortam katmanı
−26…−30 LUFS (±1) bandında kabul (izin, zorunluluk değil; `docs/tasarim/ses.md` §4 kural 2'nin −26'sı da geçerli). Tek
seferlik sesler ve sirenler ses.md §4'e tabidir (tek seferlik: tepe ≤ −6 dBTP, LUFS bağlayıcı değil; siren −20/−21 LUFS); K-E24
bunları değiştirmez. Gerekçe: ay çarpmasının döngülü sesleri (`ay_carpmasi_yaklasma`, `ay_carpmasi_citirti`; AY-07) −30 LUFS'te
uzun süre kulağı yormuyor, siren ve çarpma gibi olayların önünde kalıyor (şef kabul, 2026-11-06); kabul edilmiş tek seferlik
sesler de LUFS bakımından zaten serbest (`sinkhole_cokus` −22, güneş kristal −29,9 / akor −27,0). **Düzeltme (2026-11-07, inceleme):**
ilk metin "tek atımlı sesler ve sirenler −26 ± 1 LUFS" diyordu; ses.md §4 ile çelişiyordu, şef kararıyla ses.md §4 geçerli.
AY-07'nin "−26'da tepe sınırı aşılıyordu" gerekçesi ölçümle çelişiyordu (+4 dB ile −7,6 / −7,4 dBTP, sınır içinde); ay
birleştirmesinde (entegrasyon 4, 2/6) "tasarım tercihi, K-E24" olarak düzeltildi.

### Yiyecek

**F-17 Kutup Balığı Çorbası koşullu tarif (şef kararı, inceleme Yi1).** Tarif ve tarif kitabı başarımı Forge'un üst düzey
`conditions` dizisini taşır: `[{"type": "forge:not", "value": {"type": "forge:tag_empty", "tag":
"dogalafetler:pismis_kutup_baligi"}}]` (datagen `YiyecekVerileri.KosulluKayit`, `CraftingHelper.serialize`). Forge 47.4.28
`RecipeManager.apply` ve `ServerAdvancementManager.apply` bu diziyi yüklemede `ICondition.IContext` etiketleriyle denetler
(javap: "Skipping loading recipe {} as it's conditions were not met"); `TagEmptyCondition` aynı yeniden yüklemenin
etiketlerini okur. Etiket boşken tarif yoktur, balıkçılık etiketi doldurunca ek iş gerekmeden yüklenir: entegratörün
birleştirme sırası fark etmez. Reddedilen: Forge `ConditionalRecipe` (`forge:conditional` sarmalı; aynı sonuç, ekstra iç içe
biçim), balıkçılığın önce birleşmesini şart koşmak (sıraya bağımlı). Kanıt: GameTest `tariflerMadde14IleBirebir` (boş
etikette tarif yok, bariyer eşleşmez), dedicated log "8 … yüklendi; koşullu atlanan 1 [kutup_baligi_corbasi]".

**F-18 Cold Sweat yüklüyken yiyecekler yalnız yemek değeri verir (bilinen sınır, şef kararı; inceleme Yi3).** OTOMATIK kipte
Cold Sweat yüklüyse sıcaklık kapısı kapalıdır (sıcaklık §A4); `YiyecekAPI.uygula` kapı kapalıyken sıcaklık ve efekt vermez
(F-12), tooltip yine "İçten Isınma: …" yazar. Sıcaklığın S-13 köprüsü yalnız dünya/dönem sıcaklığını Cold Sweat'e taşır,
yiyecekleri taşımaz. Madde 2'ye aykırı değil (çift etki yok). Faz 5'e: S-18 reçetesine ek olarak Cold Sweat yüklüyken yiyecek
etkisinin Cold Sweat vücut sıcaklığına köprülenip köprülenemeyeceği (S-13 deseni, Cold Sweat API'si) araştırılacak; karar
o zamana dek kod değişmez. README "bilinen sınırlar"a girer.

### Radar ve sismograf

**R-17 Gelişmiş tarifler koşullu (inceleme R1, şef kararı).** İki şekilsiz tarif `"conditions": [{"type": "forge:not",
"value": {"type": "forge:tag_empty", "tag": "dogalafetler:kozmik_cekirdek"}}]` ile üretilir (yiyecek Yi1/F-17 deseni). Etiket
boşken `RecipeManager`'da yoktur; tarif kitabı ve JEI "Empty Tag" bariyeri göstermez, Hava Radarı + bariyer bir şey vermez.
Meteor dalı etiketi doldurunca koşul kendiliğinden geçer. GameTest iki durumu da kapsar.

**R-18 Gelişmiş panel ekranın en çok %30'u (inceleme R2, şef kararı).** Madde 13a "küçük, ekranı kaplamayan". Eski Gelişmiş
panel 150×124 GUI px'ti (468×243'te yüksekliğin %51'i). Yeni düzen: üst bölüm normal radarla aynı (yarıçap 16 kap +
üç satır, 41 px); altında 9 px'lik kısa satırlar: şiddet adı + hareket oku + hız ya da "Durağan" aynı satırda, "Son afetler",
5 geçmiş tek sütunda kısa biçimde (`Kasırga S2 3dk`, EN `Tornado L2 3m`). Yükseklik `GostergeYerlesimi.panelYuksekligi(n) =
41 + 2 + 9n + 2`; en çok 7 satırda **108 px**. Referans GUI ölçeği 2: 1280×720 → 360 px (%30,0), kanıt penceresi 1870×969 →
484 px (%22,3), 1920×1080 → 540 px (%20,0). JUnit `gelismisPanelGuiOlcegi2deEkranYuksekligininYuzdeOtuzunuGecmez` sınar;
istemci panel boyutu ve ekran yüzdesi değişince `[radar] panel WxH GUI px (...), ekran WxH: yükseklik %N` satırını log'a yazar.
GUI ölçeği 4'te (kanıt karelerinin otomatik ölçeği, 243 px) oran %44'e çıkar; oyuncu ölçeği ya da köşesini seçer.

### Meteor ve termostat

**M-22 Vitrin Faz 4'te puanlanır (şef, M2).** Meteor izi ve çarpma vitrini güneş patlaması/ay çarpması vitrininde (yoğunluk,
kamera ve koreografi afetin). Bu parça yalnız M3/M4 kod düzeltmelerini yapar; tek ek protokol koşusu yalnız ödül ve ipucu
kareleri (`tools/senaryo/meteor_gorsel.py odul` → `-PotomasyonBolum=odul`): öğlen ışığı + dört deniz feneri, kamera ~1,6 blok;
her ipucu kendi karesinde, satırlar ekran genişliğine göre sarılır (tooltip'ler üst üste binmez).

### Balıkçılık

**B-19 Balık sıcaklığı = yiyecek kademeleri (tur 2, Ba5, şef kararı).** Tek kaynak yiyeceğin kademeleri: çiğ balık küçük
serinlik (−8, 60 sn), pişmiş balık orta ısınma (+20, 90 sn), pişmiş Donmuş Ringa büyük ısınma (+35, 120 sn; madde 15
"daha çok ısınma"). Yiyecek F-01 biçimi: %25 anında, kalan süre boyunca azalan hızla (`VucutSicakligi.etkiEkle(o, toplam·0,25,
süre, toplam·0,75)`). Balıkçılığın dokuz `cig*/pismis*/ringa*` anahtarı silindi; sayılar `mantik.BalikSicakligi`'nde, dalda
`YiyecekAPI` olmadığı için yiyeceğin varsayılanlarıyla aynı sabitler (JUnit `BalikSicakligiTest`; GameTest anında −2 / +5 /
+8,75). Geçiş §E.

**B-20 Yeniden donan delik ve donma biti (tur 2, Ba2; tur 3, Bc2, şef kararı).** Delik açılırken kökeni blok durumuna
yazılır: `buz_deligi[donma=true]` yalnız sütunun donma buz biti (donma §A.6, `ChunkDonmaVerisi.buz(i)`) işaretliyse
(`BuzDeligiAcma.donmaBuzuMu`). Son aşamada delik **yalnız `donma=true` ve global donma ilerlemesi 0 ise** (`DelikOmru.suyaDonerMi(G,
donmaBuzu)`: `/dogalafet sifirla` ya da tamamen çözülmüş dünya) **su** olur; vanilla ve oyuncu buzundan açılan delik her
zaman eski buza döner (madde 15 "eski buz haline dönsün"; `aktiflikEsigi = 0` config'inde de). Aksi hâlde eski buz ve
sütunun değişmeyen donma biti. Gerekçe: donma süreci açık deliğin sütununu atlar (üst blok su/buz değil); chunk hedefe
vardıktan sonra donan delik bir daha işlenmez, `sifirla` sonrası "ilk hâl" (N18) bozulurdu. `ince` köken sayılmaz: oyuncu
ince buz bloğunu da koyabilir, tek doğru kaynak bit. **Bilinen sınır:** kızıl cüce tabanında (G 40) çözülmesi gereken bir
sütunda donan delik buz kalır (G > 0). Tam çözüm donmadan bir sütun hedefi sorgusu ister (§E). JUnit
`sifirDonmadaSuyaAksiHaldeBuzaDoner` (bit yoksa donma 0'da da buz), GameTest `sifirDonmadaYalnizDonmaBuzundakiDelikSuyaDoner`
(bitli ve bitsiz sütunda gerçek açma yolu: `donma` true/false; donma 0'da bitli → su, bitsiz → buz) ve
`kazmaylaEgilerekDelikAcilirAletBesHasarAlir` (bitsiz buzda `donma=false`). Dedicated senaryo donma 50 adımında `donma=false`
(→ buz), donma 0 adımında `buz_deligi[asama=3,donma=true]` (→ su) koyar. Senaryoda sıra önemli: donma 50 açıkken açık su
yüzeyi kalırsa donma süreci sütunu kalın buzla dondurur (doğru davranış; ilk tur 2 koşusunda alttaki su da buz oldu).

### Başarımlar

**A-29 Çorba başlangıcı `DONUYOR` (şef kararı, inceleme A1).** Madde 12 #19 "donma durumundayken", N16 "donma eşiğinin
altındayken"; sıcaklığın eşiklerinde "donma" donma hasarı kademesidir (`Esikler.donmaHasari`, vars. −75), yiyecek sözleşmesi §A3
de `oncekiKademe() == DONUYOR` der. `SOGUK` (yavaşlama) ve `COK_SOGUK` (buz kenarı) sayılmaz. Başarım zor ama alınabilir: N16
"etkisi sürerken nötre dönme" başka ısı kaynaklarını yasaklamaz (Ateşli Güveç + kamp ateşi ya da ılık iç mekan). JUnit
`CorbaTakibiTest.donmaDurumuYalnizDonmaHasariKademesi`, `soguktaKizarmisPatatesAlinmazDonarkenAlinir`; GameTest
`corbaIcerIsinirYalnizDonarkenSicakYiyecekle` (−31'de kızarmış patates alınmaz, −80'de Güveç ısınması + nötr alınır, −80'de
ısıtmayan yiyecek alınmaz).

**A-30 §6 #16: 27 başarımın her biri GameTest'te kazanılır (şef kararı, inceleme A2).** Faz 4'e bağlı olmayanlar gerçek
yollarla: #8 Güneşi Atlattım ve #9 Süpernova Tanığı sunucu geneli `OyunTestiAfeti(GUNES_PATLAMASI)` yaşam döngüsüyle (doğal bitişte
evre SUPERNOVA, göğü gören tanık; süreçte ölen ikisini de almaz); #12 Ay Düştü `OyunTestiAfeti(AY_CARPMASI)` ile; #13 Yıldızlı Gece
`ayVarAyarla(false)` + gece + kesintisiz 10 saniyelik denetim (gündüzle kesilen 9 + 9 yetmez); #14 Yükselen Sular `denizAyarla`
ile (yükselişte ölen almaz); #7/#22 `ItemPickupEvent` (başka eşya sayılmaz; eşya kardeş dalda kayıtsızken dinleyicinin kimlik yolu)
+ Kozmik Şans ünvanı; #15 Yer Dinleyici üretim yolu; #19 A-29 testi. Faz 4 iç davranışına bağlı #2, #4, #5, #6 afetin
`BasarimIpucuEvent`'i testten yayınlanarak sınanır (`ipucuOlayiTekKaynakDalgaCukurYanginAsit`); §A.3'te kayıt ve olay Faz 4 afet
işçisine **zorunludur**. Tablo: rapor §6 #16.

**A-31 Tek kaynak: olay varsa yedek sayılmaz (şef kararı, inceleme A3; A-35 ile daraltıldı).** İz her ipucunu iki kaynakta tutar
(afet olayı / yedek tahmin; `AfetIzi.olayBitleri`, `tahminBitleri`, yangın için iki ayrı saniye sayacı). Kaynak tür başına
seçilir: tür o ipucunun yayıncısı olarak kayıtlıysa (`BasarimIpucuEvent.yayinciKaydet`) yalnız olay, aksi hâlde yalnız yedek;
iki kaynak toplanmaz (çift sayım yok). Kayıtlı yayıncıda yedek denetim hiç çalışmaz (iz yazmaz). Tur 2'deki "o afette herhangi bir
oyuncu için olay geldiyse de yalnız olay" kuralı (dünya verisi `ipucuAfet`/`ipucuBitleri`) A-35 ile kaldırıldı. Saf mantık
`AfetIzi.kosullar(olayli)`, `IpucuKaynaklari` (JUnit `AfetIziTest`, `IpucuKaynaklariTest`); GameTest kayıtlı türde ağaçtan
düşüşün, 30 yedek yangın saniyesinin ve zombi hasarının sayılmadığını, kayıtsız türde yedeğin sayıldığını sınar.

**A-35 İnceleme-basarim-2 P2'leri (Bs1…Bs3, şef kararları).**
- **Bs1 tab listesi.** Vanilla `Gui` tek oyunculu dünyada tek oyuncuyla tab listesini ancak bir liste hedefi varken çizer; 42ec26f
  kurulumdan `scoreboard` satırlarını kaldırınca koşu 2/3 `tab_unvan` karesinde liste yoktu (altın önek sohbetteydi). Tab ünvanı
  kanıtı: koşu 1 `basarim_tab_unvan-1.png` + GameTest `seviyeBesFelaketUstasiUnvanVeXp` (`getTabListDisplayName()` öneki). Otomasyon
  kurulumu `scoreboard objectives add basarim_kanit dummy` + `setdisplay list` satırlarını yine çıktısız (konsol kaynağı,
  `withSuppressedOutput`, A-33) verir, kapanış hedefi siler: Faz 5 görünür koşusunda tab listesi çizilir. Ek runClient yok.
- **Bs2 yayıncı kaydı çalışma anında.** `IpucuKaynaklari.olayGeldi(tür, ipucu)`: kayıtlı yayıncıdan `KABUL`; kayıtsız kaynaktan
  tür + ipucu başına ilk olayda `UYAR_YOK_SAY` (`LOGGER.warn "[basarim] ... yayıncı olarak kayıtlı değil"`), sonra `YOK_SAY`. Kaydı
  unutan Faz 4 işçisi ilk olayında uyarı görür; olay yok sayıldığı için sonuç başka oyuncuların olaylarına bağlı değildir (tek
  kaynak tür başına: kayıtlı = olay, kayıtsız = yedek). Bu yüzden A-31'in "o afette olay geldiyse" yolu ölü kod oldu ve
  kaldırıldı (`BasarimVerisi.gelenIpuclari/ipucuGeldi`, dünya verisi `ipucuAfet`/`ipucuBitleri`; eski kayıtlardaki bu anahtarlar
  okunmaz, zararsız). JUnit `IpucuKaynaklariTest` iki durum (kayıtlı: her olay kabul, yedek kapalı; kayıtsız: ilk olay uyarır,
  tekrar uyarmaz, tür/ipucu başına, kayıttan sonra kabul). GameTest: dört ipucu testi dört türü geçici yayıncı yapar (Faz 4
  davranışı); kayıtsız sinkhole'da gelen olay ize yazılmaz (`olayBitleri == 0`). Kayıt unutulur ve hiç olay gelmezse uyarı
  yoktur; o durumda davranış yedek denetimdir (belirlenimci) ve §A.3 kaydı zorunlu tutar.
- **Bs3 vitrin sırası.** Doğal bitişte İlk Rüzgâr, Fırtınaya Meydan Oku ve Felaket Ustası aynı `afetten_sag_cikti`
  tetikleyicisinin dinleyicileri; sıraları garanti değil (koşu 2: Fırtınaya → Felaket Ustası → İlk Rüzgâr; koşu 3: Felaket
  Ustası → İlk Rüzgâr → Fırtınaya). Oyun mantığı değişmedi. Yalnız otomasyon: AKTIF aşamada İlk Rüzgâr ve Fırtınaya ayrı
  adımlarda (her biri kazanıldığı görülmeden sonraki verilmez) `advancement grant` ile verilir, doğal bitiş yalnız Felaket
  Ustası'nı (ünvanla) açar: toast ve duyuru sırası İlk Rüzgâr → Fırtınaya → Felaket Ustası. Doğal bitişin üçünü birden
  verdiği GameTest'lerde sınanır (`dogalBitenKasirga…`, `seviyeUc…`, `seviyeBes…`). Tasarım notu "Vitrin anı" sıranın oyunda garanti
  olmadığını yazar.

## Şef kararları (Faz 4)

Faz 4 parçalarında şefin verdiği ya da kabul ettiği kararlar (kaynak `.vibe/kararlar/<parça>.md` §B/§E; parça bölümlerinde aynı
kimliklerle atıf vardır). Faz 4 öncesi entegrasyon kararları (K-E18…K-E24) "Şef kararları (Faz 3)" bölümünün Entegrasyon alt
başlığındadır.

### Güneş patlaması ve kızıl cüce

- **GP1 sözleşme cümleleri (§E a):** başkasının karar dosyasına işçi yazmaz; entegratör işledi (`.vibe/kararlar/cekirdek.md` §6
  evre ilerlemesi satırı, `.vibe/kararlar/sicaklik.md` §A.8, K-C50 notu).
- **K-C26 ve tekrarlanabilir güneş patlaması (§E b): sözleşmeye uygun, değişmez.** `gunesPatlamasiTekrarlanabilir=true` iken kızıl
  cücede başlayıp sirende/AKTIF'te `stop` ile duran afetin BÜYÜYEN'i çekirdekçe `NORMAL`'e döner (K-C26 metni). Varsayılan
  config'te (tekrarlanamaz) bu yol yoktur.
- **B-20 açık kalır (§E c):** kızıl cüce tabanında yeniden donan buz deliği donma/balıkçılık kodudur, bu parçanın kapsamı dışı
  ("Açık noktalar (Faz 3)").
- **GP17 FPS ölçümü Faz 5'e:** gizli workspace 1 FPS çizer; kareler boş değilken ikinci runClient açılmaz (yangındaki şef
  kararıyla aynı). Gerçek FPS ve Düşük/Yüksek/Ultra karşılaştırması "Açık noktalar (Faz 4)".

### Ay çarpması

- **Protokol "6" (K-C47, Faz 4 tek sefer):** ay `DonemPaketi`'ne `long ayOlaySaati` ekledi (tel biçimi değişti, paket sayısı aynı);
  entegratör birleştirmede "5" → "6" yaptı (`d0fe4b9`). Diğer Faz 4 dalları paket eklemedi, artırmaz.
- **AY-07 döngülü sesler −30 LUFS (K-E24):** şef kabul, 2026-11-06; K-E24 döngülü ortam katmanına −26…−30 bandını tanır.

## Açık noktalar (Faz 2b)

Entegratöre bırakılan notlardan işlenenler: kancalar (K-E1), protokol (K-C47), `asama ileri [süreç]` (K-C48), sanal su §3a istisnası
(K-C49), güneş evresi ilerlemesi (K-C50), runClient kilidi (K0-21 rev. 2 (k), K-E4), yangın `-Potomasyon` (K-E3), donma N15
istisnası (K-E9), `sifirla` ↔ yangın (K-E11), etiket sahipliği (K-E12), `.worktrees` temizliği (tur 3; yedi birleşmiş
worktree kaldırıldı, beş spike kalır). Açık kalanlar:

- **README gamerule notu (yangın §E):** afet ateşi `doFireTick`'ten bağımsızdır (Y-01); haritayı korumak için tek denetim
  `afetBlokHasari=false`. README Faz 5 teslimatında yazılır.
- **Ay veri modeli (gökyüzü G8):** yaklaşan ay / parçalanma için `DunyaDonemi`'nde ay olay saati (ya da `AyDurumu`) ve
  `DonemPaketi` alanı yok; Faz 4 ay çarpması işçisi çekirdeğe ekler (K-C47 kuralıyla).
- **Faz 4 kuralı (sanal su §E):** kodla blok değiştiren her afet (tsunami, deprem, sinkhole, meteor) değişen chunk için
  `SanalSu.chunkDegisti(level, chunkPos)` çağırır; Faz 4 görev metinlerine girer.
- **Görsel yeniden puanlama Faz 4'te:** mağara suyu ve mağara-sualtı kareleri (SU-40), uzak LOD kurulum tepesi ve FPS (SU-38),
  buharlaşma göl yatağı karesi (K-D38).
- **Ölçümler:** FPS oranları Faz 4 başsız kalibrasyonu ve Faz 5 (K0-21 rev. 2 (j)); Cold Sweat çift hasar ölçümü Faz 5 (S-18);
  `-Puyum=coldsweat` ile sanal su mixin'lerinin refmap yolu runClient'ta sınanmadı (K-E2).
- **GameTest dünyasında sızan gerçek su (donma/su ortak bulgusu; K-E6, inceleme E3):** birleşik `main`'de `su_alani`
  şablonunun x/z 14–15 kenarına gerçek akıcı su (`water[level=3..6]`) aktı; kaynağı bulunmadı. Aday: kalıcı K-C24 dünyasında
  komuşu bir donma/buharlaşma sahnesinin (geri dolma) ya da zaman aşımıyla temizlenmeden biten bir testin bıraktığı su. Kaynak bir
  test sahnesiyse o test kendi kutusunu kapatır/temizler; üretim kodundaysa (geri dolmada akıcı su) donma işçisine gider. Faz 4
  tsunami işçisi mağara testinde (batch yerleşimi ve komşu sahneler) araştırır. **Faz 3'te bulundu (K-E16):** vanilla
  yerleşiminin satır kırması ve saklanan dünyadaki eski donma gölleri; GameTest tek satırda ve her koşu boş dünyayla.

## Açık noktalar (Faz 3)

Entegratöre bırakılan notlardan işlenenler: balık ↔ yiyecek (K-E17 a), `#minecraft:fishes` (K-E17 b), `kozmik_cekirdek` ve gelişmiş
tarifler (K-E17 c), protokol (K-E17 d), başarımın tek kaynakları (K-E17 e), `runData` ikon ve ödül (K-E17 f), `docs/tasarim/esyalar.md`
tek başlık (beş bölüm), dedicated port (K-E13), aralıklı donma testi (K-E14), GameTest yerleşimi ve Faz 2b'nin sızan suyu (K-E16),
birleşmiş beş worktree kaldırıldı (`.vibe/diff/<parça>.patch` kalır). Açık kalanlar:

- **Faz 5 görünür koşusu (runClient kareleri):** başarım sekme ipucu kutusu (A-34; `3181496` yalnız derlendi), tab listesi ünvan
  karesi (Bs1, A-33) ve vitrin anı sırası (Bs3); radar Gelişmiş panel karesi (R-18), öncü sarsıntının oyun içi Wave ölçümü (R4) ve
  radar elde / ekran açıkken FPS (R3, R-19); balıkçılık vitrini (Ba8, B-22).
- **Meteor vitrini Faz 4'te (M-22):** meteor izi ve çarpma vitrini güneş patlaması / ay çarpması vitrininde puanlanır.
- **Meteor çarpma sesi (M-12):** şimdilik vanilla patlama (alçak perde); tasarımın "30–80 Hz güm + çakıl" sentezi Faz 4 afet ses
  tasarımıyla.
- **Kızıl cüce tabanında yeniden donan delik (B-20):** delik yalnız global donma 0 iken suya döner; kızıl cüce tabanında donma buzunda
  açılmış delik eski buza döner (bilinen sınır). Donmaya `hedefDonmusMu` sorgusu önerisi (balıkçılık §E) Faz 5.
- **Cold Sweat (S-18, F-18):** Cold Sweat yüklüyken yiyecekler (balıklar dahil) yalnız yemek değeri verir; yiyecek etkisinin Cold
  Sweat vücut sıcaklığına köprüsü Faz 5'te S-18 reçetesiyle araştırılır; README "bilinen sınırlar".
- **GameTest hijyeni:** balıkçılık testleri global donmayı 50/60'ta bırakıyordu (`BalikcilikOyunTestleri.donma()`); K-E22 (c)
  ile kapandı (batch başında saklanır, sonunda geri konur).
- **Faz 4 görev metinlerine girecek kurallar:** her batch en çok 7 test (K-E16; bekçi `hicBirBatchSatirKirmaz`); dedicated
  senaryolar `cekirdek_sunucu.argumanlar` / `agir_surec_kapisi` ile `--port` / `--kanit` alır (K-E13); tsunami, sinkhole, orman
  yangını ve asit yağmuru mod kurucusunda `BasarimIpucuEvent.yayinciKaydet`, koşulda `BasarimIpucuEvent` yayınlar (başarım §A.3,
  A-30, A-31; Faz 4 log'unda "yayıncı olarak kayıtlı değil" satırı 0); kodla blok değiştiren afet `SanalSu.chunkDegisti` çağırır.

## Açık noktalar (Faz 4)

Birleşen parçalardan kalanlar (her birleştirmede güncellenir; plan `.vibe/rapor/entegrasyon-4.md`):

- **Güneş — FPS (GP17):** gerçek FPS ve Düşük/Yüksek/Ultra karşılaştırması Faz 5'te görünür pencere ya da başsız kalibrasyonla
  (K0-21 rev. 2 (j)); hazır yol `gunes_patlamasi_gorsel.py` (kare başına FPS/TPS/mspt), kalite için
  `gorsel.efektKalitesi` değiştirilip üç koşu.
- **Güneş — vitrin kareleri zayıf (inceleme P2):** kavrulma için aynı açıdan 1. gün / 4. gün sonu kare çifti (ağaçlık ve çimen
  çerçevede) yok; süpernova kabuğu karesi gökyüzü sahibinin kabuk kalibrasyonuyla yeniden alınmalı
  (`client/gunes_patlamasi/GunesPatlamasiOtomasyonu`). Faz 5 görünür koşu.
- **Güneş — şeker kamışı kurumuyor (inceleme P2):** `client/gunes_patlamasi/KavrulmaRengi.java:38-42` renk işleyicisi
  listesinde `Blocks.SUGAR_CANE` yok; nilüfer ve saplar da dışarıda (GP13 bilinen sınırına eklenmeli). Faz 5.
- **Güneş — GP10 yapılmayanlar:** çarpma anının ayrı beyaz örtüsü, süpernova çökmesinde öteki ortam seslerinin susturulması,
  kavrulmada ayrı uzak yangın katmanı.
- **Betikler (K-E22/K-E23):** `-PoyunTestiAfeti` taşıyan betikler birleştirme turlarında `!tür`'e geçer ya da gerçek afete
  uyarlanır (`entegrasyon-4.md` §3.6). Güneş birleştiğinde `uyari_sunucu.py` ve `radar_gorsel.py` güneş adımları gerçek afetle
  aynı çıktıyı verir (güneş §E); `basarim_sunucu.py`'de güneş adımı yok.

- **Ay — vitrin kamerası (inceleme P2):** çarpma vitrininde ufuk arazinin arkasında; parlama sonrası ufuk ışığı ve mantar sütunu
  09–12 karelerinde seçilmiyor. Kamera açık ufka (kıyı ya da `ust + 40`, pitch −3…−5°), `00_once` ve `17_sifirla_ay_geri` −88°'de
  (`client/ay_carpmasi/AyCarpmasiOtomasyonu.java:149-172, 208`). Faz 5 görünür koşu.
- **Ay — kayan yıldız görünmedi:** bütün karelerde kayan yıldız sayısı 0; otomasyon halka karesinden önce paket içi
  `kayanYildizZorla`'yı çağırmalı (`AyCarpmasiGokyuzu.java:313-327`). Faz 5.
- **Ay — düşen cam/yaprak eşyaya dönüşebiliyor:** `SarsintiDusmeleri.java:117-122` düşen varlıkta `dropItem = false` yok.
- **Ay — #13 Yıldızlı Gece ve #14 Yükselen Sular gerçek zincirle sınanmadı:** zincir GameTest'ine sahte oyuncuyla iki iddia
  eklenmeli (bugün başarımın kendi testleri dönem alanından sınıyor).
- **Ay — FPS:** gizli workspace 1 FPS (GP17 ile aynı), Faz 5.

## N17 — Sayısal varsayılanlar

Şartnamenin sayı vermediği her değer config'e konur ve burada gerekçesiyle listelenir. Satırları değeri koyan işçi ekler.

| Alt sistem | Config anahtarı | Varsayılan | Birim | Gerekçe |
|---|---|---|---|---|
| çekirdek | `cekirdek.zamanlayici.gunlukAfetOrani` | 0,8 | afet / MC günü (bekleme dışı) | bekleme ve afet süresiyle gerçekleşen ~0,43/gün (simülasyon, 5 dk süren afetle) ≈ 46 dk'da bir; 2–3 saatlik oturumda 3–4 afet (madde 4 "hiç olmayacak kadar nadir değil, bunaltıcı değil"); JUnit 417 günlük simülasyonda 0,25–1,0 aralığını doğrular |
| çekirdek | `cekirdek.zamanlayici.kontrolAraligiTick` | 20 | tick | saniyede bir zar; olasılık adıma göre ölçeklenir, sıklığı değiştirmez |
| çekirdek | `cekirdek.zamanlayici.genelBeklemeTick` | 12 000 | tick (10 dk, yarım gün) | madde 4 en az bekleme: afetten sonra toparlanma payı |
| çekirdek | `cekirdek.zamanlayici.siddetBeklemeCarpani` | 0,25 | oran | madde 10 "biraz uzasın": S3 ×1,25, S4 ×1,5, S5 ve seviyesiz ×1,75 |
| çekirdek | `cekirdek.zamanlayici.ertelemeTick` | 6 000 | tick (5 dk) | N14: uygun yer yoksa türün yeniden denenmesi; oyuncular bu sürede yer değiştirir |
| çekirdek | `cekirdek.zamanlayici.baslangicBeklemeTick` | 24 000 | tick (1 gün) | yeni dünyada ilk gün afetsiz; barınak kurulabilsin |
| çekirdek | `cekirdek.afetler.{kasirga,tsunami,deprem,asit_yagmuru,sinkhole,orman_yangini}.agirlik` | 100 | ağırlık | madde 4 "görece sık, birbirine yakın" |
| çekirdek | `cekirdek.afetler.ay_carpmasi.agirlik` | 32 | ağırlık | nadir: uygun olduktan sonra λ ≈ 0,04/gün (~25 günde bir) |
| çekirdek | `cekirdek.afetler.gunes_patlamasi.agirlik` | 20 | ağırlık | en nadir: uygun olduktan sonra λ ≈ 0,025/gün (~40 günde bir); oyun boyunca yaşanabilir |
| çekirdek | `cekirdek.afetler.<altı sık afet>.beklemeTick` | 48 000 | tick (2 gün) | aynı tür art arda gelmesin |
| çekirdek | `cekirdek.afetler.ay_carpmasi.beklemeTick` | 240 000 | tick (10 gün) | tek seferlik; yalnız ay geri gelirse (sifirla) anlamlı |
| çekirdek | `cekirdek.afetler.gunes_patlamasi.beklemeTick` | 480 000 | tick (20 gün) | tekrarlanabilir açıkken iki güneş patlaması arası |
| çekirdek | `cekirdek.onUyari.enAzTick` / `enCokTick` | 900 / 1 800 | tick (45 / 90 sn) | şartname madde 4 |
| çekirdek | `cekirdek.onUyari.erkenBildirimTick` | 600 | tick (30 sn) | Hassas Sismograf "daha erken": 75–120 sn uyarı (madde 13b) |
| çekirdek | `cekirdek.siddet.seviye{2,3,4,5}Gunu` | 10 / 25 / 50 / 80 | oyun günü | şartname madde 10 |
| çekirdek | `cekirdek.siddet.nadirlikBaslangic` | 0,35 | katsayı | gün 10'da S2 ≈ %7: açılan seviye hemen baskın değil |
| çekirdek | `cekirdek.siddet.nadirlikSon` | 0,6 | katsayı | geç oyunda S5 ≈ %5,6, S1 ≈ %43: kayış var, yüksek seviye hâlâ nadir |
| çekirdek | `cekirdek.siddet.nadirlikGecisGunu` | 120 | oyun günü | S5 açılışından (80) 40 gün sonra son dağılım |
| çekirdek | `cekirdek.siddet.acilisRampaGunu` | 15 | oyun günü | açılan seviyenin tam ağırlığa çıkışı |
| çekirdek | `cekirdek.siddet.acilisBaslangicOrani` | 0,2 | oran | açılış gününde ağırlığın %20'si |
| çekirdek | `cekirdek.siddet.gelisimDahil` | false | — | madde 10 "opsiyonel" |
| çekirdek | `cekirdek.siddet.puanBasinaGun` | 2,0 | gün / puan | puan 40 (XP 20 + elmas zırh) → 80. gün = S5 eşiği |
| çekirdek | `cekirdek.tekSeferlik.gelisimOlcutu` | ORTALAMA | — | arkadaş grubunda tek önde gideni cezalandırmaz (madde 3) |
| çekirdek | `cekirdek.tekSeferlik.gunesPatlamasiTekrarlanabilir` | false | — | şartname madde 4 |
| çekirdek | `cekirdek.tekSeferlik.gunesPatlamasiKriteri` | GUN | — | şef kararı (K-C28): anlık puan geri gidebilir; gün monoton |
| çekirdek | `cekirdek.tekSeferlik.gunesPatlamasiMinGun` | 40 | oyun günü | şef kararı; ilk güneş patlaması ortanca 78,8. gün, 400 günde %99,5 (simülasyon) |
| çekirdek | `cekirdek.tekSeferlik.gunesPatlamasiPuanEsigi` | 35 | puan | yalnız PUAN/IKISI_DE/HERHANGI: tam demir zırh (15) + XP 20 |
| çekirdek | `cekirdek.tekSeferlik.ayCarpmasiKriteri` | GUN | — | güneş patlamasıyla aynı mantık (bölüm H) |
| çekirdek | `cekirdek.tekSeferlik.ayCarpmasiMinGun` | 25 | oyun günü | şef kararı; ilk ay çarpması ortanca 49,9. gün (güneşten önce) |
| çekirdek | `cekirdek.tekSeferlik.ayCarpmasiPuanEsigi` | 20 | puan | yalnız PUAN içeren kriterlerde: demir zırh + XP 5 |
| çekirdek | `cekirdek.hedef.boyutlar` | ["minecraft:overworld"] | boyut | N11 |
| çekirdek | `cekirdek.hedef.sirenEkYaricap` | 64 | blok | 4 chunk: bölgesel afette alanın hemen dışındakiler de siren duyar (madde 3, N11, K-C30) |
| çekirdek | `cekirdek.hedef.ardisikAfetMesafesi` | 128 | blok | 8 chunk: aynı yerin çevresi art arda hedef olmasın |
| çekirdek | `cekirdek.tps.esik` / `kritikEsik` | 18 / 15 | TPS | spike-donma K-D2, GOREV §1 performans hedefi |
| çekirdek | `cekirdek.tps.toparlanmaEsigi` / `kritikToparlanmaEsigi` | 19 / 16,5 | TPS | histerezis (K-C37): eşik çevresinde salınan TPS kademeyi gidip getirmesin |
| çekirdek | `cekirdek.ag.afetPaketAraligiTick` | 20 | tick (1 Hz) | madde 3 "düşük sıklık" |
| çekirdek | `cekirdek.ag.donemPaketAraligiTick` | 20 | tick | sürekli değişen G/ilerleme saniyede en çok bir paket |
| çekirdek | `cekirdek.ag.donemTazelemeTick` | 20 | tick (1 Hz; aralık 20–60) | istemci afet saati sunucuya hizalansın (K-C35); istemci tahmini 60 tick'le sınırlı olduğu için daha seyrek olamaz; ~70 B/sn/oyuncu |
| çekirdek | `cekirdek.carpan.tsunamiCarpani` | 3,5 | kat | bölüm H "normalin 3-4 katı" |
| çekirdek | `cekirdek.carpan.tsunamiCarpanSuresiTick` | 72 000 | tick (3 gün) | bölüm H "birkaç Minecraft günü" |
| çekirdek | `cekirdek.carpan.tsunamiSonumTick` | 48 000 | tick (2 gün) | "yavaş yavaş normale dönsün" |
| çekirdek | `cekirdek.carpan.tsunamiSonumTipi` | DOGRUSAL | — | öngörülebilir sönüm |
| sıcaklık | `sicaklik.guncellemeAraligiTick` | 20 | tick | madde 3 "saniyede birkaç kez" sınırının altında; zaman sabiti dakikalar olduğu için 1 Hz algılanabilir fark yaratmaz |
| sıcaklık | `sicaklik.paketTazelemeTick` | 200 | tick | değer değişmese de 10 sn'de bir; kayıp paket HUD'u en çok 10 sn eski tutar |
| sıcaklık | `sicaklik.tuyUstSiniri` | 30 | upgrade/parça | N2 "20 veya 30" |
| sıcaklık | `sicaklik.kaynakTaramaYaricapi` | 5 | blok | kamp ateşi/fırın etrafında oturma mesafesi; 11³ blok/sn/oyuncu ucuz |
| sıcaklık | `sicaklik.hasarMiktari` | 1,0 | can (yarım kalp) | madde 7c "kademeli, okunaklı": aralıkla birlikte uçta 0,5 can/sn |
| sıcaklık | `sicaklik.kaynaklar` | lav 40:6, ateş 25:4, ruh ateşi 20:4, kamp ateşi 22:5, ruh kamp ateşi 16:5, fırın 10:3, yüksek fırın 12:3, tütsüleyici 8:3, magma 12:3, meşale 4:2, duvar meşalesi 4:2, fener 3:2, balkabağı feneri 2:2, buz −4:3, paketlenmiş buz −6:3, mavi buz −10:4, kar bloğu −2:2, toz kar −5:2 | °C:blok | madde 7a listesi; kamp ateşi başında gece ovada (8 °C) ~25 °C → konforlu; lav yanında 48 °C+; mavi buz odası belirgin soğuk |
| sıcaklık | `isi.esitlenmeHizi` | 0,006 | 1/sn | zaman sabiti ~170 sn: çıplak oyuncu dondurucu soğukta ~2,5 dk'da hasar eşiğinde (tepki süresi var), sıcak ortamda yine dakikalar |
| sıcaklık | `isi.konforSicakligi` | 18 | °C | hafif giyimli insanın nötr ortamı ~18–22 °C; ova öğleni 24 °C nötrde kalır |
| sıcaklık | `isi.birimPerDerece` | 2,0 | birim/°C | −32 °C ortam fark → −100'ün yarısını aşan soğuk; Nether (52 °C) → +68 |
| sıcaklık | `isi.metabolikIsi` | 38 | birim | = (37 − 18) × 2: konfor ortamında vücudun ürettiği ısının karşılığı (fiziksel türetim, S-02) |
| sıcaklık | `isi.durmaEtkinligi` / `yurumeEtkinligi` / `kosmaEtkinligi` | 0,8 / 1,0 / 1,4 | çarpan | dinlenme ~0,8, yürüme 1 MET referansı; koşma 1,4 (gerçekte 4–6 × ama oyunda sürekli koşu var; madde 7b "hafif ısıtsın") |
| sıcaklık | `isi.islakIletkenlik` / `suIletkenlik` | 1,5 / 3,0 | çarpan | ıslak giysi iletkenliği ~2 kat, su ~25 kat (oyun için 3: suda dondurucu soğukta ~1 dk) |
| sıcaklık | `isi.asiriSogukEsigi` | −25 | °C | süpernova sonrası (−47), kış bölgesi (−35), 200+ zirve; normal karlı gece (−10) dışarıda (S-04) |
| sıcaklık | `yalitim.zirhBasina` | 0,05 | I | 4 parça zırh = 0,2: tek başına konforu bozmaz (+7,6 birim) |
| sıcaklık | `yalitim.yunBasina` | 0,25 | I | 4 yünlü parça sıcakta +38 birim → ılıkta "sıcak" kademesi (N4 dezavantaj) |
| sıcaklık | `yalitim.tuyBasina` | 0,04 | I | 8 tüy/parça normal soğukta nötr; 30/parça ılıkta yanma (S-05) |
| sıcaklık | `yalitim.yunSogumaOrani` | 0,25 | oran | N3 %25/parça |
| sıcaklık | `yalitim.asiriSogukYunVerimi` | 0,75 | oran | aşırı soğukta 4 yün soğumayı %75 keser (hiç durdurmaz) |
| sıcaklık | `yalitim.termostatCekimi` | 0,04 | birim/sn/parça | 4 parça −10 °C'de soğumanın ~%40'ını karşılar: "güçlü ama sınırlı" (madde 13c) |
| sıcaklık | `yalitim.termostatIlimliSinir` / `termostatUcSinir` / `termostatTaban` | 40 / 120 / 0,25 | birim / birim / oran | ılıman ortamda tam, süpernova ve güneş patlaması uçlarında %25'e iner, hiç kaybolmaz |
| sıcaklık | `esikler.notrBant` | 15 | birim | HUD'da nötr bant; konfor ±7 °C |
| sıcaklık | `esikler.soguk` / `cokSoguk` / `donmaHasari` | −30 / −55 / −75 | birim | hafif hipotermi (yavaşlama) → orta (buz efekti) → ağır (hasar); aralar tepki süresi verir |
| sıcaklık | `esikler.sicak` / `cokSicak` / `yanmaHasari` | 30 / 55 / 75 | birim | simetrik: sıcak bitkinliği → güçlü titreme → ısı çarpması |
| sıcaklık | `esikler.enCokYavaslama` | 0,5 | oran | −100'de %50 yavaş: kaçabilir ama zorlanır |
| sıcaklık | `esikler.donmaAralikBasi/Sonu`, `yanmaAralikBasi/Sonu` | 120 / 40 | tick | eşikte 6 sn'de yarım kalp, uçta 2 sn'de; 20 can ~3–4 dk dayanır |
| sıcaklık | `ortam.biyomCarpani` / `biyomOfseti` | 20 / −2 | °C | vanilla biyom sıcaklığı (ova 0,8 → 14, karlı 0 → −2, donmuş zirve −0,7 → −16, çöl 2 → 38) |
| sıcaklık | `ortam.kurakOrtalamaDususu` / `gunGeceGenligi` / `kurakGenlikCarpani` | 10 / 6 / 3,5 | °C / °C / çarpan | ova gece 8 – öğlen 24; çöl öğlen 52, gece 8 (madde 7a "çölde gündüz çok sıcak gece çok soğuk") |
| sıcaklık | `ortam.tepeSaati` | 7000 | tick | günlük sıcaklık tepesi öğleden ~1 saat sonra |
| sıcaklık | `ortam.yukseklikBaslangici` / `yukseklikKatsayisi` | 64 / 0,06 | Y / °C/blok | gerçek 6,5 °C/km × 10 (Minecraft dikey ölçeği); Y 200'de −8 °C |
| sıcaklık | `ortam.yagmur` / `kar` / `firtina` | −4 / −7 / −7 | °C | yağış ve rüzgârın hissedilen soğutması; ayrıca güneş bonusu kalkar |
| sıcaklık | `ortam.gunes` | 4 | °C | öğlen doğrudan güneş; gölgede 0 |
| sıcaklık | `ortam.yeraltiSicakligi` / `yeraltiGecisDerinligi` | 12 / 24 | °C / blok | mağara yıl boyu yöre ortalaması (~12 °C); 24 blok derinlikte tam |
| sıcaklık | `ortam.kapaliAlanHedefi` / `kapaliAlanOrani` | 16 / 0,3 | °C / oran | iç mekân dışarıya göre dengeli ama ısıtmasız ılık değil |
| sıcaklık | `ortam.su` | −6 | °C | su havadan serin; asıl etki iletkenlikte |
| sıcaklık | `ortam.kaynakUstSiniri` / `kaynakAltSiniri` | 45 / −25 | °C | lav gölünde bile sınırlı; buz odası dondurucu değil |
| sıcaklık | `ortam.nether` / `end` | 52 / −6 | °C | Nether "çok sıcak" (çıplakta yavaş yavaş yanma), End "soğuk" |
| sıcaklık | `ortam.gunesPatlamasiArtisi` / `golgeOrani` / `yeraltiKorumasi` | 38 / 0,45 / 0,8 | °C / oran / oran | tepe öğleninde ova 62 °C (açıkta ~3 dk'da yanma); gölgede 36 °C; derin sığınakta ~15 °C (bölüm D "en alt katmanlara inip saklanmak") |
| sıcaklık | `ortam.supernovaKalintisi` | 15 | °C | süpernova başında artık ısı, evre boyunca söner |
| sıcaklık | `ortam.donmaDususu` | 55 | °C | G=100'de ova −47 °C (aşırı soğuk); G=40 tabanı −22 °C katkı |
| sıcaklık | `ortam.kizilCuceOfseti` / `kizilCuceGunesCarpani` | −8 / 0,6 | °C / oran | kızıl cücede ova öğleni ~−10 °C: eski dünyadan belirgin soğuk ama süpernovadan 35 °C sıcak |
| sıcaklık | `ortam.kisBolgesiDususu` | 25 | °C | kalıcı kış bölgesi ~−35 °C (aşırı soğuk) |
| sıcaklık (istemci) | `sicaklik.hudGorunur` / `hudXOfset` / `hudYOfset` / `ortamOkuGoster` / `ekranEfektleri` / `kameraTitremesi` | true / 0 / 0 / true / true / true | — | madde 7d "pozisyonu ve görünürlüğü config'den" |
| uyarı | `uyari.sirenSes` | 28 | — | madde 5 "%25–30"; aralığın ortası (S-01) |
| uyarı | `uyari.ortamTavan` | 40 | — | örnek katman (−26 LUFS) %40'ta ≈ −34 LUFS, %28 siren (−20 LUFS) ≈ −31 LUFS: ortam sirenden ~3 dB, siren sırasında ~6 dB alçak; konuşma bandı ayrıca −6 dB çentikli |
| uyarı | `uyari.standartFadeOutTick` | 40 (2 sn) | — | ses.md §2 "2,0 sn üstel fade-out" |
| uyarı | `uyari.gunesFadeOutTick` | 80 (4 sn) | — | ses.md §4 "60. saniyede meteorlarla 4 sn fade-out" |
| uyarı | sabit: Güvenlik payı | 60 tick | — | S-06 B3 "süreTick + 3 sn" |
| uyarı | sabit: Kesme fade | 10 tick | — | ikinci siren eskisini tıkırtısız keser (S-02) |
| uyarı | sabit: Paket süre tavanı | 12 000 tick | — | bozuk paket sonsuz siren çalmasın (S-02 B7) |
| uyarı | sabit: Açılış | standart 24, güneş 60 tick | — | S-02 (spin-up 140 → 280 Hz; güneş 3 sn'de belirir) |
| uyarı | sabit: Güneş yükselişi | +3 dB / süre | — | ses.md §4 "yaklaşıyor" |
| uyarı | sabit: Ortam bastırma | −3 dB, 20 tick | — | ses.md §4 kural 4 |
| uyarı | sabit: Katman tavanı | 8/12/16/20, dosya başı 6 | — | ses.md §4 kural 3, §5 |
| uyarı | sabit: Ortam varsayılan fade | 40 tick | — | afet bitince unutulan katman |
| gökyüzü | `gokyuzu.supernovaSuresiGun` (SERVER) | 1,5 | — | aralık 1–2; bölüm D "1-2 Minecraft günü"nün ortası |
| gökyüzü | `gokyuzu.kizilCuceGecikmeGun` (SERVER) | 7 | — | aralık 3–365; bölüm D varsayılanı; "Kutup Sığınağı"nın 3 gününden kısa olamaz |
| gökyüzü | `gokyuzu.kizilCuceDogusSuresiTick` (SERVER) | 6000 | — | aralık 1200–24000; şafaktan öğle öncesine kadar yaşanan, herkesin görebileceği doğuş |
| gökyüzü | `gokyuzu.kizilCuceBoyutOrani` (SERVER) | 0,6 | — | aralık 0,2–1; bölüm D "eski boyutun %60'ı" |
| gökyüzü | `gokyuzu.kizilCuceRenkGucu` (SERVER) | 1,0 | — | aralık 0–1; tasarım tonu tam; 0 = vanilla gök/sis rengi (yalnız görsel) |
| gökyüzü | `gokyuzu.kalintiGenislemeGun` (SERVER) | 30 | — | aralık 1–1000; tasarım: günde %1, tavan %130 |
| gökyüzü | `gokyuzu.yildizYogunlugu` (CLIENT) | 1,0 | — | aralık 0,25–2; kalite tablosunun yıldız sayısı çarpanı (Düşük 2000 … Ultra 10000) |
| gökyüzü | `gokyuzu.nebulaKatmani` (CLIENT) | true | — | Samanyolu/galaksi/gece nebulası kapatılabilir (zayıf GPU) |
| gökyüzü | sabit: renk geçişi | 400 tick | — | tasarım "20 sn doğrusal" |
| gökyüzü | sabit: süpernova çökmesi | 8 sn | — | vitrin koreografisi (G9) |
| gökyüzü | sabit: şafak | 23000 | — | vanilla gün doğumu yelpazesinin başladığı saat |
| yangın | `yangin.tickButcesi` (SERVER) | 64 | hücre/tick | tasarım §5; hücre 10 tick'te bir işlendiğinden 640 hücre tam hızda döner, ötesi yakınlık sırasıyla yavaşlar; işleniş başına ~5 blok okuma + 3 deneme |
| yangın | `yangin.aktifTavan` (SERVER) | 1500 | hücre / boyut | tasarım §5; 5–10 kişilik oyunda birkaç eşzamanlı meteor krateri + bir orman yangını; SavedData ~60 KB |
| yangın | `yangin.yayilmaHizCarpani` (SERVER) | 1,0 | çarpan | vanilla yayılma hızı ölçeği (çimen 60 → deneme başına %60); 0 yayılmayı kapatır |
| yangın | `yangin.yagmurdaSonme` (SERVER) | true | — | bölüm G "yağmur yağınca sönebilsin" |
| yangın | `yangin.yagmurSonmeOlasiligi` (SERVER) | 0,25 | olasılık / işleniş | vanilla yağmur sönmesi 0,2 + yaş; bizde +0,3 × yaş, açıkta ~2–3 sn, yaprak/çatı altında ×0,3 (~5–8 sn): yağmur başlayınca yangın bir dakika içinde biter |
| yangın | `yangin.afetSonrasiSonumSaniye` (SERVER) | 30 | sn | tasarım §6 "20–40 sn içinde söner"; ikinci yarıda rastgele (15–30 sn) |
| yangın | `yangin.parcacikYogunlugu` (CLIENT) | 1,0 | çarpan | efekt kalitesinin üstüne ince ayar; 0 kendi parçacıklarımızı kapatır (vanilla ateş kalır) |
| yangın | `yangin.lodMesafesi` (CLIENT) | 48 | blok | tasarım §5 LOD: 48 blok ötesinde 4×4 küme yerine chunk başına duman |
| yangın | `yangin.sutunTavani` (CLIENT) | 64 | canlı parçacık (Yüksek) | tasarım §5 sütun sprite tavanı (Yüksek 160) yerine daha az ama büyük parçacık: 2,5–13 blok boyunda yarı saydam quad'lar; Düşük ×0,25, Orta ×0,5, Ultra ×2; pirokümülüs tavanın yarısı |
| yangın | `yangin.sutunMesafesi` (CLIENT) | 160 | blok | uzaktan görünen sütun için yeterli, `force` parçacıklarının dünyanın öbür ucunda doğmasını önler (inceleme F4) |
| donma | `donma.donmaSuresi` | 6 | MC günü | madde 9 "birkaç gün"; tasarım notu |
| donma | `donma.cozulmeSuresi` | 10 | MC günü | şartname |
| donma | `donma.cozulmeTabani` | 40 | % | şartname |
| donma | `donma.kalici` | false | — | madde 9 "donma kalıcı mı / sonradan çözülsün mü"; kızıl cüce çözülmesi varsayılan |
| donma | `donma.bagimsizCozulme` | false | — | şartname: kızıl cüce dışı çözülme opsiyonel, kapalı |
| donma | `donma.tickBasinaMaksBlok` | 256 | blok/tick | spike: 441 chunk 0→60 ~80 sn, TPS 20 |
| donma | `donma.tickBasinaMaksMs` | 4 | ms | tick'in %8'i; ikinci güvence; ortalama hedefi, tek tick bir kurulum ya da bir sütun + BFS kadar aşabilir (K-D35) |
| donma | `donma.seviyeAdimi` | 5 | G puanı | bir geçişte en çok bir eşik; dönülen chunk ~6 tick'te yetişir |
| donma | `donma.esikler.karEsikleri` / `karCozulmeEsikleri` | 15,55,70,85,95 / 55,65,75,85,95 | % | madde 9 sırası (K-D23): ilk kar kırağıdan sonra, kıyı buzundan önce; ince kar önce kalksın |
| donma | `donma.esikler.yapiKarEsigi` / `yapiKarCozulmeEsigi` | 40 / 55 | % | yapılar ilerleyen aşamada (K-D25): kalın buzla aynı; ince karla birlikte kalkar |
| donma | `donma.esikler.ilkBuzEsigi` / `buzKalinlikAdimi` / `kiyiKatsayisi` | 20 / 20 / 2 | % / % / %/blok | kıyı buzu ilk kardan sonra (madde 9); 4 blok kalınlık kıyıda %80 |
| donma | `donma.esikler.kiyiUzaklikSiniri` / `maksBuzKalinligi` | 15 / 4 | blok | chunk + 15 blok hale içinde ölçülür; 2+ derin suyun en alt bloğu ayrıca donmaz, 1 derin su tamamen donar (K-D22) |
| donma | `donma.esikler.buzCozulmeTabani` / `buzCozulmeKiyiKatsayisi` / `buzCozulmeEnAz` / `buzCozulmeAdimi` | 45 / 3 / 10 / 10 | % | kıyı kırağıdan sonra açılır; taban %40'ta kıyı açık, orta kalın |
| donma | `donma.esikler.titresimGenligi` | 8 | % | tüm eşiklerden küçük |
| donma | `donma.esikler.inceBuzPayi` | 6 | % | ince buz evresi görülsün (kıyıda %20–26), kalınlık adımından (10/20) küçük |
| donma | `donma.esikler.karYagisiEsigi` | 30 | % | madde 9 sırası (K-D33): kıyı buzu (20) < yağmur yerine kar ≤ kalın buz (40); vitrin %35 kar yağışında |
| donma | `donma.kis.kisOrani` / `kisDalgaBoyu` / `kisGecisBandi` | 0,25 / 1200 / 0,03 | oran / blok / gürültü | şartname %25; adalar; komşu sütun farkı ≤ 0,05 |
| donma | `donma.kis.kisYGecisAlt` / `kisYGecisUst` | 88 / 120 | blok | Y≥120 tam kış; eğim ≤ 0,05/blok |
| donma | `donma.kis.ilikOfsetSicaklikTabani` / `ilikOfsetKatsayisi` / `ilikOfsetEnCok` | 0,3 / 8 / 10 | biyom sıc. / puan / puan | "önce ılık bölgeler" |
| donma | `donma.buharlasmaEnCok` | 6 | blok | N9 |
| donma | `donma.buharlasmaSuresi` | 5 | MC günü | güneş patlamasının 5 günü |
| donma (CLIENT) | `donma.parcacikYogunlugu` | 1,0 | çarpan | efekt kalitesiyle birlikte (Düşük 0,3 … Ultra 1,5) |
| su (SERVER) | `su.aysonrasiBeklemeDk` | 10 | gerçek dk | bölüm H "10 dakika" |
| su | `su.denizHedefArtis` | 4 | blok | bölüm H "3-5 blok" ortası; plaj ve nehir kenarı batar, köy çoğu yerde kuru kalır |
| su | `su.denizUstSinir` | 6 (1–8) | blok | "makul üst sınır"; 8 sistem tavanı (taşkın derinliği) |
| su | `su.denizYukselmeDk` | 5 | gerçek dk | "birkaç dakikaya yayılsın", fark edilir ama ani değil |
| su | `su.denizKalici` | true | — | bölüm H "varsayılan olarak kalıcı" |
| su | `su.denizGeriDonusBeklemeDk` / `su.denizGeriDonusDk` | 60 / 5 | gerçek dk | kalıcı kapalıysa zirvede bir saat, aynı hızla dönüş |
| su | `su.magaraKalisSn` | 600 | sn | tasarım notu (10 dk); oyuncunun mağaraya inip görmesine yeter |
| su | `su.magaraDolmaHizi` | 2 | blok/sn | dolum bir tsunami cephesinden kısa sürmeli |
| su | `su.magaraKurumaBlok` / `su.magaraKurumaAraligiSn` | 0,25 / 6 | blok / sn | 4 bloklık cep ~1,6 dk'da kurur, inişi görülür |
| su | `su.magaraMaksHacim` | 4000 | hücre | spike; ağda ~5–10 KB, render maliyeti ihmal edilir |
| su | `su.magaraMaksKesif` | 20000 | hücre | spike; büyük mağarada sızıntı seviyesi |
| su | `su.alanOkumaButcesi` | 32 | chunk / 10 tick | spike ölçümü (0,7–11 ms); tick'lere yayılır (4/tick), her 2 oyuncuda katlanır, TPS kısmasıyla çarpılır (SU-20) |
| su | `su.tazelemeUzakButce` | 16 | chunk / sn | K0-30 |
| su (CLIENT) | `su.lodMesafeChunk` | 16 | chunk | görüş mesafesi zaten sınırlar; çok uzak LOD kurulumu ~ms |
| su (CLIENT) | `su.parcacikYogunlugu` | 1,0 | çarpan | kalite çarpanının üstüne kullanıcı ayarı |
| su (CLIENT) | `su.okumaButcesi` | 64 | chunk / 10 tick | spike (istemci); tick'lere yayılır |
| su (CLIENT) | `su.yakinKurulumButcesiMs` (yakın + uzak LOD ortak, SU-38) | 2,0 (0,25–50) | ms / tick | 60 FPS kare bütçesinin ~%12'si; deniz +4 halkası ~6 tick'te biter, eski tampon arada çizilir (SU-26) |
| su (kod) | dalga hasarı / su gölgesi / girdi sönümü | Faz 4 tablosu / ×0,6 / ×0,88 | — | tsunami.md §2 |
| yiyecek | `yiyecek.kademe.cokKucuk` | 3 | vücut birimi | vanilla pişmiş yiyecek "çok küçük": küçük kademenin alt sınırı 5'in altı (F-02) |
| yiyecek | `yiyecek.kademe.kucuk` | 8 | vücut birimi | madde 14 "küçük %5–10" ortası (Isıtıcı Çay, Buzlu Karpuz, çiğ balık) |
| yiyecek | `yiyecek.kademe.orta` | 20 | vücut birimi | "orta %15–25" ortası (Salep, Çikolata, Ayran, pişmiş balık) |
| yiyecek | `yiyecek.kademe.buyuk` | 35 | vücut birimi | "büyük %30–40" ortası (Tavuk Çorbası, Kutup Balığı Çorbası, Şerbet) |
| yiyecek | `yiyecek.kademe.cokBuyuk` | 45 | vücut birimi | Ateşli Güveç "çok büyük": büyüğün üst sınırı 40'ın üstü, en güçlü soğuk savar |
| yiyecek | `yiyecek.anlikOran` | 0,25 | oran | "anında bir miktar" + giderek azalan süreli kısım; sıcaklık §A1 örneğiyle aynı oran (F-01) |
| yiyecek | `yiyecek.azalanVerimPenceresiSaniye` | 60 | sn | madde 14 "60 saniye içinde" (şartname sayısı; config) |
| yiyecek | `yiyecek.azalanVerimOrani` | 0,5 | çarpan | madde 14 "%50 azalsın" (şartname sayısı; config) |
| yiyecek | `yiyecek.yanEtkiCarpani` | 1,5 | çarpan | Ateşli Güveç sıcakta: 45 → 67,5 birim; vücut 50'de 30 sn'de yanma kademesine iter (GameTest), soğukta etkisiz (F-04) |
| yiyecek | `yiyecek.yanEtkiEsigi` | 20 | vücut birimi | 28 °C ortam ya da ılık vücut: ılıman ova (~22 °C) tetiklemez; çöl öğleni, Nether, yangın ya da lav yakını tetikler |
| yiyecek | `yiyecek.vanillaIsinma` | true | aç/kapa | madde 14 "opsiyonel, config'den kapatılabilir" |
| yiyecek | `yiyecek.vanillaYiyecekler` | 8 pişmiş yiyecek =30, 3 çorba =45 | eşya=sn | "çok küçük ve kısa"; çorbalar sıcak servis edildiği için biraz uzun (F-11) |
| yiyecek | `yiyecek.balik.pismisSaniye` | 90 | sn | pişmiş buz balığı orta ısınma: Salep/Çikolata/Ayran'la aynı orta süre (F-14) |
| yiyecek | `yiyecek.balik.cigSaniye` | 60 | sn | çiğ buz balığı hafif serinlik: Buzlu Karpuz'la aynı hafif süre (F-14) |
| yiyecek | (tablo, kod) besin/doygunluk | Salep 4/0,6, Çikolata 5/0,6, Tavuk Çorbası 10/0,9, Güveç 8/0,8, Çay 2/0,3, Ayran 4/0,5, Karpuz 3/0,4, Şerbet 4/0,4, Kutup Balığı Çorbası 9/0,9 | açlık / doygunluk çarpanı | içecekler bal şişesi/karpuz ölçeğinde; çorbalar vanilla tavşan yahnisi (10/0,6) çevresinde, Tavuk Çorbası "yüksek doyuruculuk" en yüksek |
| radar | `esya.radar.menzil` | 300 | blok | madde 13a |
| radar | `esya.radar.gelismisMenzil` | 800 | blok | madde 13a |
| radar | `esya.radar.paketAraligiTick` | 20 (20–200) | tick | madde 3 "düşük sıklık"; geri sayım saniyelik |
| sismograf | `esya.sismograf.menzil` | 300 | blok | R-02; radarla aynı ölçek |
| sismograf | `esya.sismograf.hassasMenzil` | 800 | blok | R-02; Gelişmiş radarla aynı |
| sismograf | `esya.sismograf.merkezHatasi` | 40 | blok | 300 blok menzilde yönü doğru, yeri kaba |
| sismograf | `esya.sismograf.hassasMerkezHatasi` | 6 | blok | "daha doğru merkez": bir oda boyu |
| sismograf | `esya.sismograf.titremeSiddeti` | 50 | % | 100 = 0,6°; "çok hafif" titreme 0,3° |
| sismograf | `esya.sismograf.paketAraligiTick` | 20 (20–200) | tick | madde 3 |
| sismograf (sabit) | şiddet tahmini payı | 24 → 7,2 / Hassas 6 → 1,8 | % puan | R-06 |
| sismograf (sabit) | öncü darbe aralığı / süresi | 240 → 60 / 40 | tick | R-07 |
| sismograf (sabit) | öncü ses hacmi | 0,08–0,30 | çarpan | madde 5 kısık |
| radar (sabit) | hareket izi penceresi | 6 örnek (5 sn) | — | yön kararlı, 1 bloktan az = durağan |
| radar (sabit) | tarama hızı | 18 | °/tick | saniyede bir tur (R-19) |
| radar (sabit) | Gelişmiş panel yüksekliği | ≤ 108 (7 alt satır × 9) | GUI px | GUI ölçeği 2'de ekranın ≤ %30'u (R-18) |
| CLIENT | `esya.radar.gostergeGorunur` | true | — | |
| CLIENT | `esya.radar.gostergeKosesi` | `SAG_ALT` | — | sohbet solda, efekt ikonları sağ üstte; R-09 |
| CLIENT | `esya.radar.gostergeXOfset`, `gostergeYOfset` | 0 | GUI px | |
| CLIENT | `esya.sismograf.titreme` | true | — | |
| CLIENT | `esya.sismograf.ses` | true | — | |
| CLIENT | `esya.sismograf.grafik` | true | — | madde 13b "opsiyonel" ekran |
| meteor | `meteor.parcaSansi` | 0,03 | olasılık | madde 13c |
| meteor | `meteor.buyukParcaSansi` | 0,05 | olasılık | madde 13c |
| meteor | `meteor.cekirdekSansi` | 0,003 | olasılık | madde 13c |
| meteor | `meteor.sicakSaniye` | 20 | sn | madde 13c; kraterin magma parıltısı da bu süre |
| meteor | `meteor.odulOmruSaniye` | 600 | sn | madde 13c (10 dk) |
| meteor | `meteor.yanmaSaniye` | 3 | sn | "kısa süre yanma": 3 sn ≈ 3–4 can, öldürmez |
| meteor | `meteor.kucukKraterYaricapi` / `meteor.kucukKraterDerinligi` | 2,5 / 2 | blok | 1 blokluk meteor → 5 blok çap |
| meteor | `meteor.buyukKraterYaricapi` / `meteor.buyukKraterDerinligi` | 5,5 / 4 | blok | 3 blokluk meteor → 11 blok çap, derinlik/çap 0,36 |
| meteor | `meteor.magmaSogumaSaniye` | 20 | sn | M-25; tasarım "kraterde 20 sn magma parıltısı" |
| meteor | `meteor.kraterBlokButcesi` | 512 | blok işlemi / tick | büyük krater ~2 tick; TPS kısmasıyla çarpılır |
| meteor | `meteor.varlikTavani` | 48 | varlık | M-15 |
| meteor | `meteor.dagitimIcYaricap` / `meteor.dagitimDisYaricap` | 32 / 160 | blok | tasarım §2 "32–160 blok çevresinde" |
| meteor | `meteor.girisYuksekligi` | 140 | blok | izin gökyüzünde görünmesi, uçuş ~3 sn (tepki süresi) |
| meteor | `meteor.dusmeHizi` | 3,0 | blok / tick | blok/tick; paket sınırı 3,9 |
| meteor | `meteor.girisAcisiEnAz` / `meteor.girisAcisiEnCok` | 40 / 70 | — | tasarım "40–70°" |
| meteor | `meteor.sarsintiMenziliKucuk` / `meteor.sarsintiMenziliBuyuk` | 48 / 112 | — | M-11 |
| meteor | `meteor.kucukYanginYogunlugu` / `meteor.buyukYanginYogunlugu` | 0,45 / 0,85 | alan oranı | yangın §1: alan oranı %62 / %90 |
| meteor | `meteor.yanginYaricapEki` | 3 | blok | krater kenarı + enkaz halkası ötesi |
| meteor | `meteor.kucukKraterSicakligi` / `meteor.buyukKraterSicakligi` | 14 / 24 | °C | yangın ısı kaynağı 18 °C ile aynı ölçek |
| meteor | `meteor.kraterSicaklikSaniye` | 30 | sn | magma soğuduktan bir süre sonra ortam normale döner |
| meteor | `meteor.yagmurBuyukOrani` | 0,05 | oran | tasarım §6 "%5 büyük" (yalnız komut; afetler kendi oranını verir) |
| meteor (istemci) | `meteor.izYogunlugu` | 1,0 | çarpan | parçacık çarpanı (× kalite 0,25 / 0,5 / 1 / 1,75) |
| meteor (istemci) | `meteor.sarsintiOlcegi` | 1,0 | çarpan | 0 kapalı |
| meteor (istemci) | `meteor.parlamaOlcegi` | 1,0 | çarpan | 0 kapalı (ışığa duyarlı oyuncular) |
| balıkçılık | `balikcilik.aktiflikEsigi` | 30 | % (G) | madde 15 varsayılanı |
| balıkçılık | `balikcilik.delikOmruEnAzSn` / `delikOmruEnCokSn` | 240 / 300 | sn | madde 15 "4–5 dakika"; her delik aralıktan rastgele |
| balıkçılık | `balikcilik.donmaAsamasiSn` | 15 | sn (× 3 aşama) | "yavaşça yeniden donarak": son 45 sn görünür zar; oyuncu kapanacağını görür |
| balıkçılık | `balikcilik.chunkBasinaDelik` | 4 | delik | 5–10 oyuncu bir gölde yan yana tutabilir; chunk başına zamanlanmış tick ve palet taraması küçük kalır |
| balıkçılık | `balikcilik.aletHasari` | 5 | dayanıklılık | madde 15 |
| balıkçılık | `balikcilik.isirmaEnAzSn` / `isirmaEnCokSn` | 30 / 60 | sn | madde 15 (vanilla 5–30 sn + 1–4 sn yaklaşma) |
| balıkçılık | `balikcilik.lureKisaltmaSn` | 5 | sn / seviye | vanilla Lure ile aynı adım |
| balıkçılık | `balikcilik.isirmaTabanSn` | 10 | sn | Lure III'te 15–45 sn; hızlı ama vanilla'dan yavaş kalır |
| balıkçılık | `balikcilik.agirlikYaygin` / `Orta` / `Nadir` / `Cop` / `Hazine` | 56 / 28 / 8 / 7 / 1 | ağırlık | balık %92 (vanilla 85), çöp %7 (10), hazine %1 (5): "kayıp düşük, ödül iyi, hazine çok nadir"; 30–60 sn ısırmayla saatte ~70 balık, bolluğu bekleme sınırlar |
| balıkçılık | `balikcilik.sansCopAzalmasi` / `sansHazineArtisi` | 2 / 1 | ağırlık / seviye | vanilla quality gibi (çöp −2); hazine +1 (Luck III'te %4, vanilla'nın altında) |
| balıkçılık | `balikcilik.sogutmaEnCokC` | 8 | °C | buz kaynağı büyüklüğünde; G 60 soğuğunda yalıtımsız oyuncu birkaç dakikada "soğuk" kademesine iner |
| balıkçılık | `balikcilik.sogutmaTamSureSn` | 20 | sn | kısa bekleyiş cezasız, uzun hareketsizlik tam ceza |
| balıkçılık (kod) | balık sıcaklığı (`mantik.BalikSicakligi`) | çiğ −8 / 60 sn, pişmiş +20 / 90 sn, pişmiş ringa +35 / 120 sn; %25 anında | vücut birimi / sn | yiyecek `kademe.kucuk/orta/buyuk` ve `anlikOran` ile aynı (B-19); config yiyeceğindir, balıkçılıkta anahtar yok |
| balıkçılık (kod) | hareket eşiği / kaynak yarıçapı / yaklaşma | 0,25 blok/sn / 1,5 blok / 40 tick | — | B-03, B-09 |
| balıkçılık (kod) | doyuruculuk, yığın | çiğ 2/0,1 (ringa 3/0,2), pişmiş 5/0,6–7/0,9; 16 | — | B-11 |
| başarım | `basarim.etkiAlaniPayi` | 0 | blok | madde 12 "afet yarıçapında, config'den ayarlanabilir": 0 = çekirdeğin etkilenen tanımı; kenarda duranları da saymak isteyen sunucu açar |
| başarım | `basarim.odul.gorevXp` | 15 | XP | madde 12 "XP versin", küçük (vanilla görevleri 0) |
| başarım | `basarim.odul.hedefXp` | 40 | XP | görevden büyük; ayrıca 1–2 sıcak yiyecek |
| başarım | `basarim.odul.meydanOkumaXp` | 100 | XP | "daha büyük XP", ≈ 7 seviye; oyunu bozmaz |
| başarım | `basarim.unvanGoster` | true | — | N16: ünvan config ile kapatılabilir |
| başarım (JSON) | hedef ödülü | 1–2 | adet | madde 12 "1–2 sıcak yiyecek" |
| başarım (kod) | sinkhole düşüşü | 4 | blok | ipucu gelmezse "çukura düştü": vanilla düşme hasarının başladığı 3 bloğun hemen üstü |
| başarım (kod) | yangın yakınlığı | 8 | blok | ipucu gelmezse "yangın alanında": yangın ısısının etkili olduğu yakınlık (yangın 6 + √n) |
| başarım (kod) | çorba aday penceresi | 60 | tick | yedikten sonra ısınmanın görülmesi için 3 sn |
| başarım (kod) | çorba varsayılan etki süresi | 180 | sn | madde 14'ün en uzun sıcak yiyeceği |
| güneş patlaması | `gunes_patlamasi.aktifSureGun` | 5,0 | MC günü (1–10) | bölüm D "5 Minecraft günü" |
| güneş patlaması | `gunes_patlamasi.meteorBaslangicDakikaBasina` | 1,5 | meteor/dk/oyuncu (0–30) | tasarım: oyuncu başına 1/40 sn |
| güneş patlaması | `gunes_patlamasi.meteorSonDakikaBasina` | 5,0 | meteor/dk/oyuncu (0–30) | tasarım: 5. gün 1/12 sn; arası doğrusal |
| güneş patlaması | `gunes_patlamasi.buyukMeteorOrani` | 0,05 | oran (0–1) | tasarım: büyük %5 (Kozmik Çekirdek yalnız büyükte) |
| güneş patlaması | `gunes_patlamasi.kavrulmaBaslangicGun` | 3,0 | gün (0–10) | tasarım §2 "3. günden sonra" |
| güneş patlaması | `gunes_patlamasi.kavrulmaOlasiligi` | 0,5 | olasılık (0–1) | 2 günde çevrenin ~yarısı: yamalı, çöl değil |
| güneş patlaması | `gunes_patlamasi.kavrulmaYaricap` | 64 | blok (8–160) | 4 chunk: oyuncunun gördüğü çevre, yüklü alan |
| güneş patlaması | sabit: yağmur dilimi | 1200 | tick | yoğunluk dakikada bir güncellenir (GP3) |
| güneş patlaması | sabit: siren sonu güneş | 1,6 | kat | tasarım §2 aşama 0 (GP1) |
| güneş patlaması | sabit: kavrulma denemesi tavanı | 32 | deneme/oyuncu/tick | `hiz` yüksekken TPS korunur (GP15) |
| güneş patlaması | sabit: yaprak/çimen kuruluğu | p 0,564 → 1, tavan %72, 8 basamak | — | tasarım "yapraklar kahverengiye"; basamak = yeniden örme (GP13) |
| güneş patlaması | `gorsel.efektKalitesi` (mevcut, CLIENT) | Yüksek | Düşük/Orta/Yüksek/Ultra | aurora katmanı 1/2/3/3, dilim 48–128; ısı şeridi 10–32 (N13, tasarım §5) |
| ay çarpması | `ay_carpmasi.meteorSuresiSn` | 90 | sn | bölüm H "1-2 dk" ortası (tasarım §6) |
| ay çarpması | `ay_carpmasi.meteorDakikaBasina` | 7,5 | adet/dk/oyuncu | tasarım §6 "oyuncu başına 1 / 8 sn"; güneşten hafif (H.1) |
| ay çarpması | `ay_carpmasi.meteorBuyukOrani` | 0,04 | olasılık | meteor sözleşmesi örneği; ay parçaları küçük |
| ay çarpması | `ay_carpmasi.sarsintiBlokSayisi` | 35 | blok/oyuncu | tasarım §5 Yüksek satırı |
| ay çarpması | `ay_carpmasi.sarsintiYaricapi` | 48 | blok | tasarım §5 |
| ay çarpması | `ay_carpmasi.sarsintiYogunlugu` | 0,30 | olasılık | tasarım §6 |
| ay çarpması | `ay_carpmasi.halka` | true | — | tasarım §2 "config ile kapatılabilir" |
| ay çarpması | (sabit) sarsıntı | 12 sn, 2,5° | — | tasarım §6; `AyEgrileri` |
| ay çarpması | (sabit) parlama | 0,5 sn %70 | — | tasarım §2 |
| ay çarpması | (sabit) mantar | 45 sn'de 25°, 1 günde söner | — | tasarım §2, §2 "afet sonrası iz" |
| ay çarpması | (sabit) halka | 3 gün, alfa 0,6 → 0 | — | tasarım §2 |

Config dışı sabitler (oynanış değeri değil, sınır/biçim): siren 200/1200 tick (şartname, K0-19); son afet geçmişi 5
(madde 13a); tek çarpan ≤ 100, toplam ≤ 1000 (K-C22); hız 0,1–1000; TPS ölçüm aralığı 20 tick; dönem kaydı sınırları
(donma 0–100, buharlaşma 0–64, deniz 0–64 blok).

**Yangın.** Kod sabitleri (config dışı, tasarım gereği): işleniş aralığı 10 tick, özet paketi 2 Hz / 128 blok / hücre listesi
64 blok, ısı bildirimi 1 Hz / 96 blok, yanık hafızası 2400 tick, sıçrama olasılığı %4 / işleniş, `alanTutustur` en
çok 24 blok ve tick başına hücre bütçesi × 64 okuma, bekleyen silme denetimi chunk yüklenince ya da 1 Hz, yönetici özeti
30 sn, korunaklı yağmur örtüsü 8 blok.

**Donma.** Spike tablosundaki (`spike-donma.md` "Önerilen config varsayılanları") değerler aynı; tablodaki donma satırları anahtarların son adlarıdır.
Config dışı sabitler: blok maliyeti 8, kıyı BFS'i 256 birim (K-D20); sütun sırası tohumu 0x5D0E1; kuru kaydı sütun başına en çok
127 blok.

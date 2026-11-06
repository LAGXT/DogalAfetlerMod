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

Config dışı sabitler (oynanış değeri değil, sınır/biçim): siren 200/1200 tick (şartname, K0-19); son afet geçmişi 5
(madde 13a); tek çarpan ≤ 100, toplam ≤ 1000 (K-C22); hız 0,1–1000; TPS ölçüm aralığı 20 tick; dönem kaydı sınırları
(donma 0–100, buharlaşma 0–64, deniz 0–64 blok).

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

## N17 — Sayısal varsayılanlar

Şartnamenin sayı vermediği her değer config'e konur ve burada gerekçesiyle listelenir. Satırları değeri koyan işçi ekler.

| Alt sistem | Config anahtarı | Varsayılan | Birim | Gerekçe |
|---|---|---|---|---|

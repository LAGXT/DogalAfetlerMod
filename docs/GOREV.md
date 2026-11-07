# Doğal Afetler modu — vibe şef görevi

Bu görev omp `/vibe` modunda çalışır. Sen **şefsin**: işi parçalara böler, işçi oturumlarına (`vibe_spawn`, `vibe_send`) dağıtır, sonucu dosya, log ve ekran görüntüsü **okuyarak** doğrularsın. Dosya yazmak, komut çalıştırmak ve derlemek işçilerin işidir.

- **Ne yapılacak:** `docs/SARTNAME.md` (Berk'in şartnamesi + netleştirmeler). Başlamadan baştan sona oku. Bu dosyayla çelişirse şartname kazanır.
- **Nasıl çalışılacak:** bu dosya.
- **`KÖK`:** omp'nin açıldığı, içinde `docs/` bulunan klasör. Mutlak yolunu çalışma dizininden al ve işçi metinlerine mutlak yol olarak yaz.
- **`KÖK/_eski-surum-2026-11-04/`:** bu görevin eski, geçersiz sürümü; Berk'in arşivi. Okunmaz, değiştirilmez, `.gitignore`'a girer.
- **Dil:** sohbet, belgeler ve kod yorumları Türkçe.
- **Atıflar:** `§N` bu dosyanın bölümleridir. Şartnameye Berk'in kullandığı gibi `madde N` (GENEL KURALLAR 1–16), `bölüm X` (afetler A–H) ve `NN` (netleştirmeler) ile atıf yapılır.

Kullanıcıdan aşama aşama onay bekleme. Her faz sonunda sohbete 3–5 satırlık durum yaz ve devam et. Belirsizlikte makul varsayımı seç, gerekçesiyle `docs/KARARLAR.md`'ye yazdır, devam et. Yalnız şu durumlarda dur ve kullanıcıya seçenek sun: sistem paketi kurulumu, geri alınamaz işlem, kapsamı daraltma, şartnamedeki bir isteğin teknik olarak imkânsız olması ya da ancak büyük bir uyumluluk/performans riskiyle yapılabilmesi.

## Bitti tanımı

İş, şu üçü kanıtıyla birlikte hazır olunca biter:

1. `docs/SARTNAME-IZLEME.md`: şartnamedeki her madde ve her alt madde (GENEL KURALLAR 1–16, DOĞAL AFETLER A–H, KOMUTLAR, TESLİM EDİLECEKLER, N1–N19) ayrı bir satır: madde → uygulayan sınıf/dosya → onu kanıtlayan test ya da gözlem → durum. Her satır yeşil olmalı. "Kısmi" ya da "yapılmadı" satırı ancak kullanıcı o kapsam daralmasını bu konuşmada onayladıysa kalabilir.
2. §6'daki doğrulama matrisinin her satırı yeşil; kanıtı (komut, çıktı özeti, ekran görüntüsü yolu) `docs/TEST-RAPORU.md`'de.
3. §7'deki teslimat hazır.

## 1. Yaratıcı alan

Berk modun yaratıcı ve güzel olmasını istiyor. Şartname **ne** olacağını söyler; nasıl görüneceği, duyulacağı ve hissettireceği işçilerin ustalığına kalır.

- **Sabit olan:** şartnamedeki özellikler, Berk'in verdiği sayılar, tarifler, adlar ve kurallar (listedeki afetler, yalnız siren ile uyarı, kısık siren sesi, sunucu otoriteli mantık).
- **Serbest olan:** görsel ve ses tasarımı, animasyon, parçacık, render teknikleri, kamera ve sarsıntı hissi, yıkımın koreografisi, şartnamenin sayı vermediği her değer ve uygulama yolu. Şartnamenin asgari tarifini aşan cila (enkaz, şimşek, ışık parlaması, toz perdesi, katmanlı ortam sesi, afet sonrası iz) memnuniyetle karşılanır; ölçüsü §1'in sonundaki performans hedefidir.
- **Önce tasarım:** Her afet, gökyüzü dönemi ve dünya donması için işçi koda başlamadan `docs/tasarim/<ad>.md`'ye kısa bir tasarım notu yazar: gerçek olayın araştırmayla bulunan gözlemlenebilir özellikleri (ör. kasırgada yoğuşma hunisi, tabandaki enkaz halkası, dönüş; tsunamide çekilen deniz ve açığa çıkan dip, köpüklü yıkılan tepe, enkaz hattı; sinkhole'da halka çatlaklar ve basamaklı çökme), bunları Minecraft'ta hangi tekniklerle göstereceği, **vitrin anı** (afetin en etkileyici 5–10 saniyesi) ve ses tasarımı. Şef notu okur; sönük ya da sıradan bulursa gerekçesiyle geri gönderir.
- **Kalite çıtası — vitrin:** Her afetin tepe anının ekran görüntüsü, bir mod tanıtım videosunun küçük resmi olabilecek kadar etkileyici olmalı. Şef görüntüleri kendisi açar ve tasarım notuyla karşılaştırır; "çalışıyor ama sönük" kabul değildir.
- **Performans hedefi** (değiştirilebilir varsayılan): efekt kalitesi Yüksek'te, afetin tepe anında yakındaki istemcinin FPS'i aynı yerdeki afetsiz FPS'in en az yarısı; sunucu TPS'i 18'in üstünde. Hedefe efekti kırparak değil şartname madde 1'deki yöntemlerle (batch, LOD, görünürlük) ulaşılır.

## 2. Makine ve ortam

Bu görev hangi bilgisayarda çalışacağını bilmez. Faz 0'ın ilk işi makineyi ölçmektir.

- **`docs/MAKINE.md`:** kurulum işçisi yazar: işletim sistemi, CPU, toplam ve boş RAM, GPU, ekran oturumu (Wayland/X11, masaüstü ya da compositor), kurulu Java sürümleri ve yolları, git, Python (Pillow, numpy), ffmpeg, ImageMagick, boş disk, Forge Maven ve Modrinth erişimi. Paralellik, RAM kuralı ve görsel test yolu bu dosyaya göre belirlenir.
- **Java 17:** Forge 1.20.1 Java 17 ister. Yoksa önce sistem paketi gerektirmeyen yolu dene: Gradle toolchain'in JDK 17'yi kendisi indirmesi ya da Temurin JDK 17 arşivinin kullanıcı dizinine (`~/.jdks/`) açılması. Sistem paket yöneticisi gerekirse makinenin sahibine sor. Sistemin varsayılan Java'sı olduğu gibi kalır; Gradle ve Minecraft komutları `JAVA_HOME` ile çalışır. Projenin Gradle bağımlılıkları (Forge, Modrinth Maven, JUnit) serbest.
- **Forge MDK:** 1.20.1'in güncel önerilen sürümünü `https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json`'dan al (2026-11-04'te 47.4.28 idi). MDK adresi: `https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-<sürüm>/forge-1.20.1-<sürüm>-mdk.zip`. MDK'daki `org.gradle.daemon=false` böyle kalır; her derleme bitince RAM boşalır.
- **MDK run'ları:** `client`, `server` ve `gameTestServer` aynı `run/` klasörünü kullanır (yalnız `data` → `run-data/`). Aynı anda çalışacaklarsa sunucuya ve ek istemcilere ayrı `workingDirectory` ver (ör. `run-server/`, `run-client2/`). `gameTestServer` hiç GameTest yoksa çöker. MDK'da JUnit yok, eklenir.
- **Otomatik dünyaya giriş:** Minecraft 1.20 Quick Play argümanları menüye tıklamadan dünyaya sokar: `--quickPlaySingleplayer <dünya>`, `--quickPlayMultiplayer localhost:25565`; ek oyuncu için `--username <ad>`. Tek oyunculu Quick Play dünyanın `saves/` altında var olmasını ister: test dünyası Faz 0'da sabit bir seed ile bir kez oluşturulur (ör. sunucunun ürettiği `world` klasörü `run/saves/<ad>`'a kopyalanır), seed `docs/MAKINE.md`'ye yazılır.
- **Odak ve duraklama:** İstemci odak kaybedince tek oyunculu dünya duraklar (`pauseOnLostFocus` varsayılan açık); her istemci klasörünün `options.txt` dosyasına `pauseOnLostFocus:false` yazılır.
- **Uyumluluk modları:** Modrinth'te Forge 1.20.1 için e4mc ve Cold Sweat var (2026-11-04'te `6.2.3-forge` ve `2.4.5`; güncelini kontrol et; slug `e4mc`, `cold-sweat`). Geliştirme ortamına `maven { url "https://api.modrinth.com/maven" }` ve `runtimeOnly fg.deobf("maven.modrinth:<slug>:<sürüm>")` ile, bir Gradle özelliğiyle açılıp kapanacak şekilde eklenir (ör. `-Puyum=e4mc`); varsayılan kapalı, normal testler modsuz koşar.
- **Ekran:** Minecraft istemcisi makinenin gerçek ekranında açılır ve sahibi o sırada bilgisayarı kullanıyor olabilir. Hyprland ve hypruse MCP varsa pencereyi ajan workspace'lerinde (90–99) tut (`launch`, `hypr`) ve ekran görüntüsünü hypruse `screenshot` ya da oyunun F2'siyle (`run*/screenshots/`) al. Görünmeyen workspace'teki pencere kare çizmeyebilir: FPS ölçümü (§6 #10) ve görüntü boş gelirse çekim, pencere görünür bir workspace'teyken yapılır; bunun için sahibine önceden sor. Başka bir masaüstünde, pencere sahibinin önüne çıkacaksa önce sor. Hiç ekran yoksa görsel kanıt yerine log ve GameTest kullanılır ve raporda belirtilir.
- **Python ve ses araçları:** Python paketleri (Pillow, numpy) `KÖK/.venv` içinde kurulur; bu proje kapsamıdır, sistem paketi sayılmaz. ffmpeg yoksa sor.
- **Skill'ler:** Görev metinlerinde adı geçen skill'ler (`test-driven-development`, `systematic-debugging`, `verification-before-completion`, `requesting-code-review`) kuruluysa kullanılır; değilse aynı ilke uygulanır: önce test, kök neden, bitti demeden kanıt, bağımsız inceleme. Kurulu olanlar `docs/MAKINE.md`'ye yazılır.
- **Araştırma:** makinede kurulu araçlar (ör. context7 ile Forge/Minecraft API, deepwiki, web araması); gerçek afet davranışları ve görsel fikirler için web.

### Ağır süreç kuralı (RAM)

Ağır süreç = bir `./gradlew` çağrısı (Gradle JVM'i ve başlattığı Minecraft JVM'i birlikte tek sayılır) ya da Gradle'sız çalışan bir Minecraft sunucusu. Aynı anda en fazla iki ağır süreç çalışır. İşçi ağır süreç başlatmadan önce `free -g` ve `pgrep -fc GradleWrapperMain` bakar; boş RAM 4 GB'ın altındaysa ya da zaten iki `gradlew` çalışıyorsa bekler (en çok 10 dk), sonra şefe raporlar. Tavan ölçüme bağlıdır (K-E19 a): kullanılabilir RAM sürekli ≥ 12 GB ise üç `./gradlew`, değilse iki; 2026-11-06 ölçümü 11,8 GB / 8 iş parçacığı → **iki** (`docs/MAKINE.md`). İstisna çok oyunculu test (§6 #7): sunucu §6 #5'teki üretim kurulumundan Gradle'sız (`run.sh`) çalışır, istemciler `runClient` ile açılır; başka ağır süreç yokken ve boş RAM `docs/MAKINE.md`'ye göre yetiyorsa (her `runClient` yaklaşık 5–6 GB: Gradle JVM'i + oyun) şef bu testi diğer işçiler derleme yapmazken başlatır. İşçi turunu bitirmeden başlattığı her Minecraft sürecini kapatır.

**İkinci makine pc2 (K-E43):** ekransız ağır iş (`gradle_kapisi.sh` ile `build`/`test`/`runGameTestServer` ve `*_sunucu.py` senaryoları) pc2'de koşar: komutun önüne `tools/senaryo/uzak.sh -g <log ya da kanıt klasörü/>` eklenir, ör. `tools/senaryo/uzak.sh -g <log> tools/senaryo/gradle_kapisi.sh <log> runGameTestServer`. Betik çalışma klasörünü pc2'ye eşitler, işi orada koşar, `-g` yollarını geri çeker; pc2'de aynı anda tek uzak iş olur, ikinci çağrı kilidi bekler. Çıkış 4 = pc2 kapalı: iş yerelde başlatılmaz, şefe/kullanıcıya sorulur. `runClient`, `*_gorsel.py`, `*_istemci.py` ve `runData` ana makinede kalır ve yukarıdaki kurala uyar.

**Yoklama yok (K-E19 e):** işçi, doğrulayıcı ve entegratör bir arka plan işini `sleep N` döngüsüyle ya da log kuyruğunu tekrar tekrar okuyarak beklemez; işi arka planda başlatır, beklemek zorundaysa bloklayan `wait` aracını kullanır, beklerken başka işi sürdürür. Her yoklama turu kota ve bağlam harcar.

**Senaryo ve port (K-E40):** aynı anda iki oturum aynı worktree'de `*_sunucu.py`/`runServer` koşmaz (kapı `run-server/.senaryo.lock` flock'unu tutar); her oturum kendi worktree'sinde (gerekirse `worktree add --detach <geçici> <commit>`, bitince `worktree remove`) ve kendi port aralığında koşar: `dogrulayici` 25611–25620, `dogrulayici-2` 25631–25640, işçiler 25641+ws, entegratör 25651–25660. GameTest (`run-gametest/`) ve `build` ayrı dizinde, serbest.

**GameTest filtresi (K-E20):** işçi geliştirirken yalnız kendi test sınıfını/batch'ini koşar: `TEST_FILTRE='<regex>' tools/senaryo/gradle_kapisi.sh <log> runGameTestServer` ya da `JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 ./gradlew runGameTestServer --offline -Ptest_filtre='<regex>'` (sınıf, batch ya da test adında `find`, büyük/küçük harfe duyarlı; bekçi her koşuda kalır; bekçi dışında hiçbir teste uymayan ya da geçersiz regex sunucuyu açmadan çıkış ≠ 0 verir). Kabul, doğrulama ve entegrasyon koşuları filtresizdir.

### Kota ve hız sınırı

İşçiler bir API anahtarıyla çalışır; anahtarın eşzamanlı istek ve aylık kredi sınırı olabilir.

- Aynı anda **çalışan tur** sayısı (şef dahil) sınırın altında kalır; boştaki oturumlar istek atmadığı için sayılmaz. Sınır bilinmiyorsa ya da 5 ise aynı anda en çok 4 işçi turu. İşçiler kendi içinde alt ajan (`task`) açmaz; her alt ajan da eşzamanlı bir istektir.
- İşçi 429, "rate limit" ya da "quota/usage exhausted" hatasıyla durursa iş kaybolmaz: önce diğer işçilerin turlarını bekle, sonra aynı işçiye `vibe_send` ile "önce `sleep 300` çalıştır, sonra kaldığın yerden devam et" gönder. Üç kez üst üste olursa kullanıcıya bildir.
- Mekanik işleri `fast` işçiye ver; derin düşünme gerektirmeyen turlarda kredi daha az gider.

## 3. Vibe çalışma düzeni

- **İşçi seçimi:** `good` → spike, tasarım notu, ortak sistemler, afetler, hata ayıklama, entegrasyon, kod incelemesi. `fast` → mekanik işler: ses/doku üretim betiğini çalıştırmak, test matrisini koşturup kanıt toplamak, belge düzenlemek.
- **Oturumlar:** bir iş akışı = bir oturum; takip ve düzeltmeler aynı oturuma `vibe_send` ile, birkaç düzeltme biriktirilip tek turda. İnceleyici ve doğrulayıcıyı her parça için yeniden açma; sıradaki parçayı aynı oturuma gönder.
- **Worktree:** vibe'ın kendi izolasyonu yoktur; bütün işçiler aynı dizinde başlar. Paralel kod yazan her işçi kendi git worktree'sinde, kendi dalında çalışır: `KÖK/.worktrees/<parça>` (`.gitignore`'da), dal `is/<parça>`. Dalları yalnız entegratör (`kurulum` oturumu) `main`'e birleştirir. Gradle önbelleği (`~/.gradle`) ortak olduğu için ilk worktree'den sonrakilerin kurulumu hızlıdır.
- **Sahiplik:** her işçi kendi paketine yazar (§4). Ortak dosyalar (ana mod sınıfı, `mods.toml`, `build.gradle`, ağ kanalı, config spec'leri, gamerule kayıtları) çekirdek işçisinindir; diğerleri bunlara yalnız satır ekler. Her alt sistem kendi `DeferredRegister`'ını, paket kayıt metodunu ve `/dogalafet` alt komutlarını kendi paketinde tutar; birleştirme çakışmaları küçük kalır.
- **Kaynak JSON'ları datagen ile:** lang (`en_us`, `tr_tr`), `sounds.json`, tarifler, loot table, loot modifier, advancement, etiketler ve parçacık tanımları (`DataGenerators.parcacikEkle`, K-E25) elle ortak dosyaya yazılmaz; her alt sistem kendi Forge data provider sınıfını yazar, dosyalar `runData` ile üretilir. Entegratör her birleştirmeden sonra `runData`'yı yeniden koşar; üretilmiş dosyalardaki çakışma yeniden üretimle çözülür.
- **Plan ve süreklilik:** `KÖK/.vibe/DURUM.md` planın tek kaynağıdır: parça başına bir satır (parça, işçi oturumu, denetim zinciri, kabul ölçütü, durum); sahibi entegratördür. Bağlam sıkışırsa ya da oturum koparsa durumu `vibe_list`, `.vibe/DURUM.md`, `docs/KARARLAR.md`, `.vibe/rapor/` ve `git log`'dan kur, kaldığın parçadan devam et.
- **Bağlam bütçesi:** her işçi turu sana araç izi ve cevap getirir; büyük işi az turla ver (bir parçanın tamamı + kabul ölçütü). İşçi cevabı 5 satırdır, ayrıntı rapor dosyasındadır. Doğrularken yalnız işçinin verdiği satır aralığını, `.vibe/diff/<parça>.patch`'in ilgili kısmını ya da inceleme raporunun `## Özet`'ini oku.

### Denetim zinciri

| Parça | Zincir |
|---|---|
| Küçük ve geri alınabilir (ses/doku, yiyecek tarifi, lang) | yazan → doğrulayıcı |
| Ortak sistem, dünya verisi, ağ, entegrasyon, birden çok parçayı etkileyen | yazan → inceleyici → doğrulayıcı |

- **Yazan:** kendi worktree'sinde yazar, kendi parçasının hedefli testini koşar, kendi dalına commit atar, `git diff main...is/<parça>`'yı `KÖK/.vibe/diff/<parça>.patch`'e yazar.
- **İnceleyici** (`good`, o kodu yazmamış, `requesting-code-review` skill'i): patch'i şartnameye, `docs/KARARLAR.md`'ye ve kabul ölçütüne karşı okur; bulguları `KÖK/.vibe/rapor/inceleme-<parça>.md`'ye yazar: `P0` çalışmıyor ya da dünya verisi bozuluyor, `P1` şartnameye ya da sözleşmeye aykırı, `P2` iyileştirme. Dosya `## Özet` ile başlar: sayılar ve her `P0`/`P1` için tek satır `dosya:satır — sorun`.
- **Doğrulayıcı** (`fast`, ikisinden de bağımsız): yazanın worktree'sinde kaynak dosyalara dokunmadan kabul komutlarını baştan koşar; her kabul maddesi için `komut → sonuç` satırı yazar, ham çıktı `KÖK/.vibe/kanit/<parça>.txt`'de.
- `P0`/`P1` bulgusu ya da başarısız kanıt yazana döner; düzeltmeden sonra yalnız etkilenen kanıtlar yeniden koşar. **İnceleme `P0` = `P1` = 0 ise parça kabul edilir** ve `DURUM.md`'ye işlenir (K-E19 d). `P2`'ler için ayrı işçi turu açılmaz: inceleyici raporuna "Entegratöre devredilen P2'ler" bölümünü yazar (her biri `dosya:satır` + tek satır düzeltme); entegratör birleştirmede uygular ya da `docs/KARARLAR.md`'de açık noktaya yazar. Gerekçe: Faz 3'te P2 turları parça başına 3–4 tura (yazan + inceleme + doğrulama tekrarları) çıktı ve kabulü günlerce geciktirdi; P2'ler kabulü değiştirmeyen iyileştirmelerdir.
- Doğrulama iki kuyrukta (Faz 4+): `dogrulayici` (dedicated portları 25611–25620) ve `dogrulayici-2` (25631–25640); işçinin kendi dedicated portu 25640 + (workspace − 90).
- Kabul ölçütünü sen yazarsın; işçinin "bitti"si iddiadır. Görsel işi ekran görüntülerini kendin açıp tasarım notu ve şartnameyle karşılaştırmadan kabul etme.
- Bir işçinin turu 10 dakikadır yeni araç çağrısı olmadan sürüyorsa `vibe_list` ile son etkinliğine bak; bekleyen bir süreçte takıldıysa süreci kapatmasını söyle, yanıt vermiyorsa `vibe_kill` ile kapatıp işi aynı görev metniyle yeni oturuma ver.

### İşçi görev metni

İşçiler bu konuşmayı ve kullanıcının kurallarını görmez. Her `vibe_spawn` metni bu iskelettedir: ne yapılacağı ve nasıl doğrulanacağı kesin, nasıl yapılacağı işçiye kalır.

```markdown
# Görev: <parça>
Hedef: <bitmiş hâli tek cümle>
Bağlam: docs/SARTNAME.md'nin ilgili maddeleri (numarayla), docs/GOREV.md §1 (yaratıcı alan ve vitrin çıtası), docs/KARARLAR.md'deki ilgili sözleşmeler, okuyacağı dosyalar.
Konum: KÖK=<mutlak yol>. Worktree: `git -C KÖK worktree add -b is/<parça> KÖK/.worktrees/<parça> main`; bütün iş orada. JAVA_HOME=<docs/MAKINE.md'deki JDK 17 yolu>.
Sahiplik: yazabileceğin yollar: <…>. Dokunmayacakların ve sahipleri: <yol → işçi>. Ortak dosyalara yalnız satır ekle.
Kabul ölçütü: <komut> → <beklenen>; <gözlem> → <beklenen>.
Kurallar:
- Yanıt, kod yorumu ve belge Türkçe.
- Her sınıf tam çalışır. İstemci kodu `client` paketinde ve yalnız SARTNAME N15'teki yollarla çağrılır; afet mantığı sunucuda; mixin yalnız event yetmiyorsa ve gerekçesi KARARLAR.md'de.
- Kardeş işçinin ya da kullanıcının değişikliğini olduğu gibi bırak; silme ya da üzerine yazma gerekiyorsa raporda öner.
- Git'te yalnız kendi dalına, her çalışan adımda commit. Kullandığın git komutları: add, commit, diff, log, status, worktree.
- Sistem paketi ya da global ayar gerekiyorsa raporda iste.
- Ağır süreç kuralı (GOREV §2); arka planda başlattığın her süreci `timeout` ile sınırla ve turu bitirmeden kapat.
- Alt ajan (task) açma; paralel istek sınırı var.
- Saf mantıkta test-driven-development, hata ve beklenmedik davranışta systematic-debugging, bitti demeden verification-before-completion skill'ini kullan.
- Verdiğin kararları gerekçesiyle KÖK/.vibe/kararlar/<parça>.md'ye yaz.
Rapor: ayrıntıyı (yapılanlar, kanıtlar, ekran görüntüsü yolları, yapılmayan kontroller "yapılmadı" diye) KÖK/.vibe/rapor/<parça>.md'ye yaz. Cevabın yalnız şu 5 satır:
DURUM: bitti | kısmi | takıldı
DEĞİŞEN: <dosya:satır-aralığı>, …
KANIT: <komut> → <sonuç>; …
AÇIK: <soru ya da risk> | yok
RAPOR: KÖK/.vibe/rapor/<parça>.md
```

İnceleyici ve doğrulayıcının metninde Sahiplik "kaynak dosyalara yazma; yalnız `KÖK/.vibe/`" olur.

## 4. Mimari çerçeve

- `mod_id`: `dogalafetler`; kök paket `io.github.kadirberkpolat1.dogalafetler`; yazar "Kadir Berk Polat".
- **Alt paketler ve sahipleri:**
  - Çekirdek işçisi: `core` (afet yöneticisi, ön-uyarı penceresi, şiddet seçimi, geçici olasılık çarpanı, tek seferlik olaylar, afet yaşam döngüsü, dünya dönemi, SavedData, TPS ölçer), `network`, `config`, `komut` (`/dogalafet` kökü, `start`/`stop`/`durum`/`gun`/`hiz`/`sifirla` ve diğer paketlerin alt komut ekleyeceği kayıt kancası), `kural` (gamerule'lar).
  - Sistem işçileri: `uyari`, `sicaklik` (HUD, tüy upgrade'i, yün ve zırh + malzeme özel tarif tipi), `su`, `yangin`, `gokyuzu`, `donma` (donma, çözülme, buharlaşma, kalıcı kış maskesi).
  - Eşya işçileri: `esya` (radar, sismograf, termostat plakası ve tarifi; tarif `sicaklik`'in tarif tipini kullanır), `meteor` (güneş patlaması ve ay çarpmasının ortak meteor varlığı, krater, ödüller), `yiyecek`, `balikcilik`, `basarim`.
  - Afet işçileri: `afet.<ad>`.
  - `client.<alt sistem>`: o alt sistemin işçisi (HUD, render, ses, ekran sarsıntısı, radar/sismograf göstergesi).
- **Saf mantık** Minecraft sınıflarına dokunmayan sınıflarda durur: zamanlayıcı olasılığı, bekleme, geçici çarpanın sönmesi, şiddet ağırlıkları, ısı alışverişi formülü, yalıtım hesabı, yiyecek azalan verimi, kalıcı kış maskesi, chunk hedef donma seviyesi, TPS kısması. Böylece JUnit ile saniyeler içinde test edilir.
- **Afet yaşam döngüsü:** ortak arayüz, `ON_UYARI → SIREN → AKTIF → BITIS` (uzun afetlerde alt aşamalar). Durum SavedData'da; istemciye aşama değişiminde ve düşük sıklıkta paket gider; sonradan giren oyuncu girişte tam durumu alır.
- **Dünya dönemi** afetten ayrıdır ve tek SavedData'da durur: ay (var/yok), güneş evresi (normal / büyüyen / süpernova / güneşsiz / kızıl cüce), donma ilerlemesi, buharlaşma derinliği, deniz seviyesi, geçici çarpanlar. Gökyüzü, sıcaklık, su ve donma sistemleri bunu okur.
- **Afet bitiş olayı:** çekirdek her afet bittiğinde etki alanındaki oyuncuları, seviyeyi ve sonucu (sağ çıkanlar) bir event ile yayınlar; başarımlar ve radar geçmişi bunu dinler.
- **Config:** oynanış değerleri SERVER config'te (dünya başına, istemciye senkron); görsel, ses ve HUD ayarları CLIENT config'te.

## 5. Fazlar

Fazları `.vibe/DURUM.md`'ye yazdır; her faz kabul ölçütü yeşil olunca kapanır.

### Faz 0 — Kurulum (tek `good` işçi `kurulum`, `KÖK`'te)

Makineyi ölç (`docs/MAKINE.md`); JDK 17'yi sağla (§2); MDK'yı `KÖK`'e aç (`docs/`'a dokunma); örnek modu `dogalafetler` iskeletine çevir; `git init` ve `.gitignore` (`.worktrees/`, `.vibe/`, `.venv/`, `run*/`, `build/`, `.gradle/`, `cikti/`, `_eski-surum-*/`); JUnit 5 ve bir örnek test; bir örnek GameTest; datagen (`runData`) iskeleti; sabit seed'li test dünyası ve `pauseOnLostFocus:false` (§2); `docs/KARARLAR.md`, `docs/TEST-RAPORU.md`, şartnamedeki bütün maddeleri "yapılmadı" diye listeleyen `docs/SARTNAME-IZLEME.md` ve `.vibe/DURUM.md`.

Kabul: `./gradlew build`, `./gradlew test` ve `./gradlew runGameTestServer` çıkış kodu 0; `./gradlew runServer` (`run/eula.txt` = `eula=true`) log'da `Done (` satırına kadar açılır ve `stop` ile kapanır; hepsi commit'li.

### Faz 1 — Spike'lar (paralel `good`, her biri kendi worktree'sinde)

Riskli teknik kararları gerçek kod yazılmadan önce küçük deneylerle kapat:

1. **Sanal su** (madde 8, bölüm B ve H): gerçek su bloğu koymadan render + yüzme, boğulma, sürükleme, sıcaklıkta ıslaklık; mağaraya dolan suyun hacmi ve kuruması; deniz seviyesi yükselmesinin "her yerde" nasıl hesaplanıp çizileceği (yüklü chunk'larda tembel ve önbellekli, uzakta LOD). En riskli parça budur.
2. **Gökyüzü** (madde 16, bölüm D ve H): güneşin büyümesi, süpernova → nebula, güneşsiz yıldız/galaksi/nebula gökyüzü, aysız gece, kızıl cüce doğuşu; ay ve güneş durumlarının her kombinasyonu; diğer modlarla en az çakışan kanca.
3. **Dünya donması** (madde 9, N9): chunk başına hedef seviye ve yetişme, tick başına blok bütçesi, iki yönlü tablo, kalıcı kış maskesi; uzaklaşıp dönülen chunk'ın yetişmesi ve TPS etkisi.
4. **Siren ses kategorisi** (madde 5, N5).
5. **Kasırga görseli** (bölüm A): dönen huni + kalkan bloklar; varlık sayısı ve performans.

Kabul: her spike için çalışan küçük kanıt (ekran görüntüsü ya da log) ve `KARARLAR.md`'de seçilen yol, denenip reddedilen yollar ve nedenleri. Spike dalları `main`'e birleşmez; kararı Faz 2 işçisi temiz kodla uygular.

### Faz 2 — Ortak sistemler

- **2a. Çekirdek (önce, tek `good`):** afet yöneticisi (madde 3, 4 ve 10: zamanlayıcı, olasılık, bekleme, geçici çarpan, ön-uyarı penceresi, şiddet seçimi, tek seferlik olaylar, hedef oyuncu seçimi, oyuncu sayısından bağımsız sıklık, TPS kısması), afet yaşam döngüsü arayüzü, dünya dönemi, afet bitiş olayı, SavedData, SimpleChannel ağ kanalı, gamerule'lar (madde 11), config iskeleti, `/dogalafet` kökü, çekirdeğin kendi alt komutları ve alt komut kayıt kancası (diğer komutları sahibi olan sistem işçisi ekler; afetler kayıt sistemi üzerinden gelir). Kabul: JUnit (olasılık dağılımı, bekleme, çarpanın sönmesi, gün eşiklerine göre seviye ağırlıkları, art arda 4–5 olmaması, aynı anda iki afet olmaması, aynı oyuncunun art arda hedef olmaması); GameTest (SavedData yaz/oku, kapalı gamerule'daki afetin seçilmemesi); dedicated server açılışı.
- **2b. Paralel (çekirdek `main`'e girince):** uyarı ve ses (madde 5, N5, N6), sıcaklık (madde 7, N1–N4, N8; HUD, tüy, yün), sanal su (madde 8, spike 1'e göre), yangın (madde 6), gökyüzü (madde 16, spike 2'ye göre), dünya donması ve buharlaşma (madde 9, N9, N10, spike 3'e göre). Her birinin kabulü §6 matrisindeki ilgili satırlardır.

### Faz 3 — Eşyalar ve ilerleme (paralel)

- Hava Radarı ve Sismograf, gelişmiş sürümleriyle (madde 13a–b, N7): çekirdek.
- Meteor varlığı, krater, meteor ödülleri, Termostat Plakası ve Termostatlı zırh (madde 13c, N3): yangın + sıcaklık.
- Sıcaklık yiyecekleri (madde 14): sıcaklık.
- Buz altı balıkçılık (madde 15): donma + sıcaklık.
- Başarımlar (madde 12, N16): tetikleyiciler ve advancement JSON'ları; afetlerle ilgili ölçütler afetler geldikçe bağlanır.

### Faz 4 — Afetler (paralel, bağımlılık sırasıyla)

- Kasırga (bölüm A): çekirdek + uyarı.
- Deprem (bölüm C) + Sinkhole (bölüm F): aynı işçi, arazi yıkımı ortak.
- Asit yağmuru (bölüm E) + Orman yangını (bölüm G): yangın sistemi.
- Tsunami (bölüm B): sanal su.
- Ay çarpması (bölüm H): meteor + sanal su + yangın + gökyüzü + sıcaklık.
- Güneş patlaması (bölüm D) ve kızıl cüce dönemi: meteor + yangın + gökyüzü + sıcaklık + donma; en son.

Her afetin kabulü: tasarım notu şefçe onaylı; `/dogalafet start <ad> [seviye]` ile başlar, `/dogalafet stop` ile temiz biter (geride varlık, parçacık, sanal su ya da asılı kalmış durum kalmaz); ön-uyarı (radar/sismograf), siren, aktif, vitrin anı ve bitiş ekran görüntüleri; Seviye 1 ile Seviye 5 farkı görüntüde belli; tepe anında TPS ve FPS ölçümü; şartnamedeki her alt maddesi izleme tablosunda yeşil.

### Faz 5 — Entegrasyon ve tam doğrulama

Entegratör bütün dalları `main`'e birleştirir, `.vibe/kararlar/*.md`'yi `docs/KARARLAR.md`'ye işler, tam test paketini koşar. Kodu yazmamış bir doğrulayıcı (`fast`) §6 matrisini baştan sona koşturur ve kanıtları toplar. Ayrı bir inceleyici (`good`) kodu `SARTNAME-IZLEME.md`'ye karşı okur. Bulunan her hata ilgili işçiye geri gider; düzeltmeden sonra etkilenen matris satırları yeniden koşar. Matris tamamen yeşil olunca §7.

## 6. Doğrulama matrisi

| # | Ne | Nasıl | Yeşil ölçütü |
|---|---|---|---|
| 1 | Derleme | `./gradlew build` | çıkış 0, `build/libs/` altında reobf'lu jar |
| 2 | Birim testleri | `./gradlew test` | hepsi geçer: zamanlayıcı ve çarpan; şiddet ağırlıkları ve gün eşikleri; ısı alışverişi (fark büyükse hızlı, küçükse yavaş); yün %25/50/75/100; tüy yalıtımı; aşırı soğuk istisnası; çok yalıtımla sıcakta aşırı ısınma; termostatın uç sıcaklıkta azalması; yiyecek azalan verimi; kalıcı kış maskesinin aynı seed'de aynı çıkması; TPS kısması |
| 3 | GameTest | `./gradlew runGameTestServer` | hepsi geçer: tüy upgrade'i (1 tüy harcanır, sayaç artar, üst sınır); yün ve termostat tarifleri (büyü, dayanıklılık, diğer upgrade'ler korunur, ikinci uygulama reddedilir); donma tablosunda dondur → çöz = başlangıç; `afetBlokHasari=false` iken blok değişmez; buz deliği açılır ve yeniden donar; 10 sahte oyuncunun sıcaklığı birbirinden bağımsız (sahte oyuncular tick almayabilir; gerekirse güncelleme testte doğrudan çağrılır); respawn'da sıfırlama; SavedData kalıcılığı; yangın yanmaz bloğu, asit dayanıklı bloğu bozmaz; aynı anda iki afet başlamaz |
| 4 | Geliştirme sunucusu | `runServer`, konsoldan her komut ve her afet için start/stop | log'da istisna yok, `invalid dist` yok, sunucu çökmez |
| 5 | Üretim sunucusu | Forge 1.20.1 installer `--installServer` ile ayrı klasör, `mods/`'a build jar'ı | açılır; her afet başlar ve biter; log temiz (reobf/refmap sorunu yok) |
| 6 | Görsel tur | `runClient` + Quick Play | her afet için ön-uyarı, siren, aktif, vitrin anı, bitiş ekran görüntüsü; ekranda hiçbir uyarı yazısı yok; siren afet başlayınca fade-out ile durur; HUD barı soğuk/nötr/sıcak hallerde ve aşırı uçta titreşir; radar ve sismograf göstergeleri yalnız eşyayı taşıyanda; şef görüntüleri açıp tasarım notlarıyla karşılaştırdı |
| 7 | Çok oyunculu | ayrı klasörlü sunucu + 2–3 istemci (`--username A`/`B`/`C`, `online-mode=false`) | istemcilerde aynı afet aynı aşamada; afet ortasında giren oyuncu senkron; sıcaklıklar bağımsız; siren hepsine gider; "Birlikte Hayatta" 3 oyuncuyla açılır. RAM yetmezse eksik istemciler GameTest sahte oyuncularıyla tamamlanır ve raporda belirtilir |
| 8 | Kalıcılık | deniz seviyesi, ayın yok olması, güneş evresi, donma ilerlemesi, kızıl cüce dönemi, olasılık çarpanı ve bir chunk'ın donma seviyesi ayarlanır → sunucu durdurulup başlatılır | değerler korunur; sonradan giren istemcide doğru gökyüzü görünür |
| 9 | Uyumluluk | e4mc ile istemci: tek oyunculu dünya → Open to LAN; Cold Sweat ile istemci | çökme yok; e4mc adresi log'da; Cold Sweat yüklüyken çift sıcaklık hasarı yok (kanıt: iki modun hasar log'u ya da ölçülen can kaybı) |
| 10 | Performans | her afetin tepe anında `/forge tps` ve istemci F3 FPS; efekt kalitesi Düşük, Yüksek ve Ultra | değerler tabloda; §1'deki hedef tutuyor; TPS eşiğin altına düşünce kısmanın devreye girdiği log'da görülür |
| 11 | Ses | siren varsayılanı ve ayar ekranı; siren ve ortam seslerinin `ffmpeg -af ebur128` ölçümü ve spektrogram PNG'si (`docs/kanit/`) | varsayılan ~%25–30; ses ayarı ekranındaki kaydırıcının ekran görüntüsü; config değeri değişir; sirenin tepe seviyesi ve baskın frekansı tasarım notundaki "sakin ama fark edilir" hedefiyle karşılaştırıldı |
| 12 | Uzun süreçler | `/dogalafet hiz`, `asama ileri`, `aysonrasi atla`, `donma`, `kizilcuce baslat` ile güneş patlaması (5 gün → süpernova → nebula → donma %100 → kızıl cüce → çözülme tabanı) ve ay çarpması (çarpma → meteorlar → 10 dk → deniz yükselmesi → aysız gece) uçtan uca | her aşama sırayla gelir; her aşamanın ekran görüntüsü ve log'u var |
| 13 | Chunk yetişmesi | uzak bir chunk'a git, `/dogalafet donma 60`, geri dön; kızıl cüce döneminde `kizilcuce bilgi` ile kalıcı kış ve ılık bölge | dönülen chunk hedef seviyeye adım adım yetişir, TPS düşmez; kış bölgesi donmuş kalır, sınırda geçiş yumuşak |
| 14 | Gamerule'lar | her kural `/gamerule <ad>` ile okunur ve değiştirilir | tab tamamlama çalışır; kapalı afet seçilmez, başlamış afet güvenle biter; ulaşılmış dönemler geri alınmaz |
| 15 | Sanal su | tsunami, mağara suyu ve deniz yükselmesi öncesi ve sonrası etkilenen bölgedeki blokların sayımı ya da hash'i; GameTest ve istemcide sanal suya giren oyuncu ve mob | bloklar birebir aynı (harita bozulmadı; mağara suyu kuruduktan sonra iz yok); oyuncu sanal suda yüzer, havası azalır ve boğulur, sürüklenir, ıslanır (sıcaklık); mob'lar da etkilenir |
| 16 | Başarımlar | 27 başarımın her biri komut ve senaryolarla tetiklenir | her biri doğru oyuncuda açılır; toast ve sohbet duyurusu; ödüller verilir; gizli olanlar açılana kadar görünmez |

**Doğrulama turları (K-E19 c):** ilk doğrulama kabul komutlarının hepsini koşar. Tur 2 ve sonrası yalnız değişen yolu koşar (değişen dosyaların testleri, etkilenen matris satırları, gerekiyorsa tam derleme); değişmeyen kanıt önceki rapordan satırıyla atıfla aktarılır ("değişmedi, `dogrulama-<parça>.md` madde N"). GameTest varsayılan **2** ardışık koşudur; aralıklı düşme şüphesinde (bir koşuda düşüp diğerinde geçen test, zamanlamaya bağlı ileti) 3. Kabul GameTest'i filtresizdir: `gradle_kapisi.sh` log'unun ilk satırı `FILTRE=yok` olmalı ve sonuç `All <tam sayı> required tests passed`; `FILTRE=<regex>` satırlı log kabul kanıtı sayılmaz (K-E20). Faz 4 GameTest kuralı: uzak chunk `level.getChunk` ile eşzamanlı üretilir; duvar saatine bağlı arka plan işi tick zaman aşımıyla beklenmez (K-E21).

## 7. Teslimat

- `KÖK/cikti/dogalafetler-<sürüm>.jar`: reobf'lu, üretim sunucusunda test edilmiş jar.
- `README.md` (Türkçe): Windows'ta oynamak (Forge 1.20.1 installer, jar'ı `%APPDATA%\.minecraft\mods`'a koymak); Windows ve Linux'ta derlemek (Java 17, `gradlew build`); arkadaşlarla oynamak (e4mc ve dedicated server; modun sunucuda ve her istemcide gerektiği); komutlar ve gamerule'lar; config dosyaları ve önemli ayarlar; ses ve doku dosyalarının adları, yolları, boyutları ve nasıl değiştirileceği; bilinen sınırlar.
- **Sesler ve dokular** gerçek dosyalardır ve `tools/assets/` altındaki betiklerle tekrar üretilebilir (ör. numpy ile siren sentezi → ffmpeg ile `.ogg`; Pillow ile doku ve parçacık). Uygun yerde vanilla sesler `sounds.json`'dan kullanılabilir. Dışarıdan lisanslı dosya alınırsa lisansı `docs/ASSET-LISANS.md`'ye yazılır.
- `docs/TEST-RAPORU.md` (matris, kanıtlar, `docs/kanit/` altındaki ekran görüntüleri), `docs/SARTNAME-IZLEME.md`, `docs/KARARLAR.md`, `docs/tasarim/`.
- Son sohbet mesajın: jar yolu, matris özeti, kullanıcının elle denemesi gerekenler (ör. Windows'taki gerçek başlatıcıda deneme, seslerin kulakla değerlendirilmesi).

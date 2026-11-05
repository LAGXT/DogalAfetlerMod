# Şartname izleme tablosu

`docs/SARTNAME.md`'deki her madde ve alt madde ayrı bir satırdır (GOREV §0 "Bitti tanımı" 1). Sütunlar: madde →
uygulayan sınıf/dosya → kanıtlayan test ya da gözlem → durum. Durum değerleri: `yapılmadı`, `kısmi`, `yeşil`.

- Madde kimliği: `7e.3` = madde 7, (e) bendi, 3. satır; `A.2` = bölüm A, 2. satır; `KOM` = KOMUTLAR; `TES` = TESLİM
  EDILECEKLER; `N` = NETLEŞTİRMELER. "Şartname (özet)" sütunu metnin başıdır; bağlayıcı metin `docs/SARTNAME.md`'dir.
- Faz 0 (kurulum) yalnız iskelet kurdu; iskeleti olan satırlarda "Uygulayan" sütunu dolu ama madde tümüyle
  kanıtlanana kadar durum `yapılmadı` kalır.
- Satır sayısı: genel kurallar 167, afetler 53, komutlar 17, teslim 12, netleştirmeler 19;
  toplam 268.

## GENEL KURALLAR (madde 1–16)

| Madde | Şartname (özet) | Beklenen sahip | Uygulayan sınıf/dosya | Kanıt | Durum |
|---|---|---|---|---|---|
| 1.1 | Öncelik sırası: önce görsel kalite ve gerçekçilik, sonra optimizasyon. Optimizasyon önemli ama efektleri bozacak, sulandıracak veya çirkinleştirecek kadar agresif olması… | core + client.* (efekt kalitesi: config) | — | — | yapılmadı |
| 1.2 | Afetler etkileyici, yoğun ve gerçekçi görünmeli: yeterli parçacık, detaylı dalga/kasırga animasyonu, akıcı yıkım efektleri. "Kasmasın diye" efekti kırpma. | core + client.* (efekt kalitesi: config) | — | — | yapılmadı |
| 1.3 | Orta seviye bir bilgisayarda akıcı çalışması hedeflensin. Çok düşük özellikli bilgisayarlar için config'te opsiyonel bir "Düşük Efekt" modu olsun ama VARSAYILAN mod "Kal… | core + client.* (efekt kalitesi: config) | — | — | yapılmadı |
| 1.4 | Optimizasyonu görüntüyü bozmadan sağlayan akıllı yöntemler kullan: ağır işlemleri tick'lere böl (batch/kuyruk), yalnızca oyuncuya yakın alanlarda yüksek detay göster, uz… | core + client.* (efekt kalitesi: config) | — | — | yapılmadı |
| 1.5 | Config dosyasında efekt kalitesi (Düşük / Orta / Yüksek / Ultra), parçacık yoğunluğu ve yıkım hızı ayarlanabilsin. | core + client.* (efekt kalitesi: config) | `config/IstemciAyarlari` (`efektKalitesi`, Faz 0 iskeleti); parçacık yoğunluğu ve yıkım hızı yok | — | yapılmadı |
| 2.1 | Sürüm: Minecraft 1.20.1, Forge. | core, network, config | `build.gradle`, `gradle.properties`, `META-INF/mods.toml` (Forge 47.4.28, MC 1.20.1) | `./gradlew build` (Faz 0) | yapılmadı |
| 2.2 | Diğer modlarla uyumlu çalışmalı: vanilla sistemlerini gereksiz yere ezme (overwrite) yapma, event sistemi kullan (loot için Global Loot Modifier, vb.), kendi registry na… | core, network, config | — | — | yapılmadı |
| 2.3 | e4mc modu ile uyumlu olmak ZORUNDA. Tüm afet mantığı sunucu tarafında (server-authoritative) çalışsın, istemciye sadece gerekli senkronizasyon paketleri (SimpleChannel)… | core, network, config | — | — | yapılmadı |
| 2.4 | Tek oyunculu, LAN/e4mc ve dedicated server olmak üzere hepsinde çalışmalı. | core, network, config | — | — | yapılmadı |
| 2.5 | Cold Sweat gibi başka sıcaklık modları yüklüyse çakışma olmasın: config'te kendi sıcaklık sistemimizi kapatma/açma seçeneği olsun, aynı anda iki sistem çalışıp oyuncuya… | core, network, config | `config/SunucuAyarlari` + `config/SicaklikSistemiModu` (Faz 0 iskeleti) | `SicaklikSistemiModuTest` (JUnit) | yapılmadı |
| 3.1 | Mod, arkadaş grubu ölçeğinde çok oyunculu oynanabilsin: hedef 5-10 oyuncu (maksimum 10). Hem e4mc ile açılmış LAN dünyasında hem dedicated server'da sorunsuz çalışsın. B… | core, network | — | — | yapılmadı |
| 3.2 | Tüm afet durumu (aktif afet, şiddet seviyesi, konumu, aşaması, kalan süre, deniz seviyesi artışı, dünya donma ilerlemesi, gökyüzü/güneş dönemi) sunucuda tutulsun ve tüm… | core, network | — | — | yapılmadı |
| 3.3 | Her oyuncu için ayrı hesaplama gereken şeyler (vücut sıcaklığı, donma, yanma, asit hasarı, ekran sarsıntısı, başarım ilerlemesi, sıcaklık yiyeceği etkileri) oyuncu bazın… | core, network | — | — | yapılmadı |
| 3.4 | Ağ trafiği makul olsun: paketler sadece ilgili oyunculara gitsin, her tick'te gereksiz paket gönderme, durum değişimlerinde ve düşük sıklıkta güncelleme paketleri kullan… | core, network | — | — | yapılmadı |
| 3.5 | Afet konumu seçilirken rastgele bir çevrimiçi oyuncunun çevresi seçilsin; aynı oyuncu art arda hedef alınmasın. Afet bir oyuncunun yakınında başlasa bile etkilenebilecek… | core, network | — | — | yapılmadı |
| 3.6 | Sunucu TPS'i belirli bir değerin altına düşerse yıkım, yangın yayılması, donma/çözülme yayılması ve efekt işlemleri otomatik yavaşlasın. | core, network | — | — | yapılmadı |
| 3.7 | Afet sıklığı oyuncu sayısıyla orantısız artmasın: 1 kişi de 10 kişi de aynı genel afet sıklığını yaşasın (config'den ayarlanabilir). | core, network | — | — | yapılmadı |
| 3.8 | Güneş patlaması ve ay çarpması gibi sunucu geneli afetlerde "oyuncunun geliştiği" kriteri, çevrimiçi oyuncuların ortalamasına veya en gelişmiş oyuncuya göre config'den s… | core, network | — | — | yapılmadı |
| 3.9 | Oyuncular oyundan çıkıp girebilsin: afet sırasında sunucuya giren oyuncuya mevcut afet durumu ve gökyüzü dönemi doğru şekilde senkronize edilsin. Oyuncu ölüp yeniden doğ… | core, network | — | — | yapılmadı |
| 3.10 | Thread-safe yaz: sunucu tarafı işlemleri ana sunucu thread'inde yap, istemci kodunu (@OnlyIn(Dist.CLIENT)) sunucu koduna karıştırma. Dedicated server'da istemci sınıflar… | core, network | — | — | yapılmadı |
| 3.11 | Komutlar sadece yetkili oyuncular (OP / yetki seviyesi) tarafından kullanılabilsin. | core, network | `komut/DogalAfetKomutu` (`requires(hasPermission(2))`, Faz 0) | `KomutOyunTestleri` (GameTest) | yapılmadı |
| 4.1 | Tüm afetler haritada rastgele zamanda ve rastgele yerde oluşsun. | core | — | — | yapılmadı |
| 4.2 | Afetler oyunun gidişatını bozmayacak şekilde oluşsun: iki afet aynı anda başlamasın, afetler arasında minimum bekleme süresi olsun, bir oyuncunun çevresinde art arda afe… | core | — | — | yapılmadı |
| 4.3 | Hiçbir afet "hiç olmayacak" kadar nadir olmasın, ama oyunu bunaltacak kadar sık da olmasın. "Her X günde bir kesin olur" gibi sabit bir takvim de olmasın; olasılık taban… | core | — | — | yapılmadı |
| 4.4 | Nadirlik sırası: Kasırga, Tsunami, Deprem, Asit Yağmuru, Sinkhole, Orman Yangını (görece sık, birbirine yakın) → Ay Çarpması (nadir) → Güneş Patlaması (en nadir, ama yin… | core | — | — | yapılmadı |
| 4.5 | TEK SEFERLİK OLAYLAR: Ay çarpması gerçekleşip ay yok olduktan sonra ay çarpması tekrar seçilmesin (gökyüzünde ay kalmadı). Güneş patlaması süreci kızıl cüce dönemine ula… | core | — | — | yapılmadı |
| 4.6 | Her afetin oluşma olasılığı ve bekleme süresi config dosyasında ayarlanabilir olsun. | core | — | — | yapılmadı |
| 4.7 | OLASILIK ÇARPANI SİSTEMİ: Bazı olaylar (örneğin ay çarpması sonrası) belirli afetlerin olasılığını geçici olarak artırabilsin. Bunun için afet yöneticisinde "geçici olas… | core | — | — | yapılmadı |
| 4.8 | ÖN-UYARI PENCERESİ: Afet yöneticisi, afet seçildikten sonra siren çalmadan önce gizli bir "ön-uyarı penceresi" (varsayılan 45-90 saniye, config'den ayarlanabilir) işlets… | core | — | — | yapılmadı |
| 5.1 | Her afet başlamadan önce siren sesi çalsın. Siren, afetten etkilenebilecek tüm oyunculara gitsin. | uyari, client.uyari | — | — | yapılmadı |
| 5.2 | Güneş Patlaması hariç tüm afetlerde siren 10 saniye sürsün. Güneş Patlaması sireni özel ve daha uzun/ürkütücü olsun. | uyari, client.uyari | — | — | yapılmadı |
| 5.3 | SİREN SES SEVİYESİ: Oyuncular sesli sohbette (Discord vb.) konuşurken arkadaşlarını duyabilmeli. Bu yüzden siren ve afet uyarı sesleri varsayılan olarak KISIK olsun (var… | uyari, client.uyari | — | — | yapılmadı |
| 5.4 | EKRANDA UYARI YAZISI OLMAYACAK: Afet spawn olurken, siren sırasında veya afet boyunca ekranda hiçbir uyarı yazısı, başlık, banner veya bildirim kutusu çıkmasın ("TORNADO… | uyari, client.uyari | — | — | yapılmadı |
| 5.5 | Afet asıl etkisine geçtiği anda (siren süresi bittiğinde / afet aşamasına geçildiğinde; güneş patlaması ve ay çarpmasında meteor/çarpma başladığında) siren yumuşak bir f… | uyari, client.uyari | — | — | yapılmadı |
| 5.6 | sounds.json'daki altyazı (subtitle) anahtarları yalnızca vanilla Altyazı ayarı açık olan oyuncularda vanilla altyazısı olarak görünsün (örn. "Siren çalıyor"); bu ayrı bi… | uyari, client.uyari | — | — | yapılmadı |
| 5.7 | Afetin türünü, yönünü ve seviyesini önceden görmek isteyen oyuncu Hava Radarı / Sismograf (madde 13) kullanır. | uyari, client.uyari | — | — | yapılmadı |
| 6.1 | Orman yangını, güneş patlaması meteorları ve ay çarpması meteorları AYNI yangın yayılma sistemini kullansın (tek bir ortak, optimize yangın yöneticisi yaz, kodu tekrarla… | yangin | — | — | yapılmadı |
| 6.2 | Bu sistem batch/kuyruk mantığıyla çalışsın, TPS düşünce otomatik yavaşlasın, aktif yangın sayısı ve yayılma hızı config'den sınırlandırılabilsin. | yangin | — | — | yapılmadı |
| 7.1 | Bu sistem modun temel parçası. Güneş patlamasındaki donma ve yanma da bu sistemle çalışsın. | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7a.1 | Ortam sıcaklığı ve vücut sıcaklığı AYRI değerler olsun: | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7a.2 | ORTAM SICAKLIĞI: oyuncunun bulunduğu yerin sıcaklığı. Şunlara göre hesaplansın: biyom sıcaklığı, yükseklik (dağlar soğuk, derin mağara/yeraltı dengeli), gün/gece döngüsü… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7a.3 | VÜCUT SICAKLIĞI: oyuncunun kendi iç sıcaklığı. Normal değeri dengeli bir orta noktada olsun. | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7b.1 | ISI ALIŞVERİŞİ: Vücut sıcaklığı ortam sıcaklığıyla gerçekteki gibi ısı alışverişi yapsın. Vücut sıcaklığı ortama doğru YAVAŞ YAVAŞ yaklaşsın (anında eşitlenmesin, ortam… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7c.1 | Vücut sıcaklığı çok düşerse: kademeli donma (önce yavaşlama, ekranda buz/donma efekti, sonra donma hasarı). | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7c.2 | Vücut sıcaklığı çok yükselirse: kademeli aşırı ısınma (önce ekranda sıcak titreme efekti, sonra yanma hasarı / ateşte yanma efekti). Sıcak ortamda aşırı ısınan oyuncu ya… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7c.3 | Hasarlar ani değil, kademeli ve okunaklı olsun; oyuncu tepki verebilsin. | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7d.1 | SICAKLIK BARI (HUD): Ekranda, en alttaki XP barının hemen ÜSTÜNDE, Cold Sweat modundaki gibi küçük, şık bir sıcaklık/soğukluk göstergesi olsun. Soğuktan sıcağa doğru ren… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.1 | ZIRH YALITIMI (TÜY UPGRADE'İ VE YÜN). Eski "tüylü zırh seti" fikri KALDIRILDI, yerine aşağıdaki iki sistem var: | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.2 | TÜY UPGRADE'İ (etkileşim ile): Oyuncu SOL eline herhangi bir zırh parçası (vanilla veya moddan gelen, kask/göğüslük/pantolon/bot), SAĞ eline tavuktan düşen tüyü alıp tüy… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.3 | Soğukta: upgrade sayısı arttıkça vücut daha yavaş soğur, yani donma süresi uzar. Çok sayıda upgrade ile soğukta çok uzun süre dayanılır. | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.4 | Ama belli bir seviyeden sonra vücut sıcaklığı yükseleceği için: ortam soğuk DEĞİLSE (ılık/sıcak ortam), çok upgrade'li zırh giyen oyuncu aşırı ısınır ve YANMAYA başlar.… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.5 | Her zırh parçasının upgrade'i ayrı ayrı sayılsın ve toplam yalıtım dört parçanın toplamı olsun. | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.6 | YÜN İLE CRAFTING: Crafting tezgahında, istediğimiz bir zırh parçası + koyundan düşen yün birleştirildiğinde, o zırh parçası "Yünlü" hale gelsin (veri bileşeniyle). Her y… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.7 | ÇOK AŞIRI SOĞUK İSTİSNASI: Ortam sıcaklığı olağanüstü düşükse (örneğin süpernova sonrası dondurucu soğuk, kızıl cüce dönemindeki kalıcı kış bölgelerinin en soğuk noktala… | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.8 | Yün ve tüy etkileri toplanabilsin ama dengeli olsun (hiçbir kombinasyon oyunu tamamen bozmasın, ortam aşırı sıcaksa yüksek yalıtım dezavantaja dönüşsün). | sicaklik, client.sicaklik | — | — | yapılmadı |
| 7e.9 | Tüm hesaplamalar her oyuncu için ayrı yapılsın. | sicaklik, client.sicaklik | — | — | yapılmadı |
| 8.1 | Tsunami dalgası, mağaraya dolan su ve ay çarpması sonrası yükselen deniz seviyesi AYNI özel su sistemini kullansın (tek bir ortak, optimize su yöneticisi/renderer yaz, k… | su, client.su | — | — | yapılmadı |
| 8.2 | Bu sistem gerçek su bloklarını DEĞİŞTİRMESİN; harita verisi bozulmasın. Özel render edilen su, oyuncu/mob etkileşimi (yüzme, sürükleme, boğulma, hasar) ve görsel olarak… | su, client.su | — | — | yapılmadı |
| 9.1 | Güneş patlaması / süpernova sonrası dünyanın donması ve kızıl cüce döneminde buzulun kısmen çekilmesi bu tek sistemle yapılsın. Dünya ANİDEN donmasın veya erimesin, uzun… | donma | — | — | yapılmadı |
| 9.2 | GLOBAL DONMA İLERLEMESİ: Sunucuda dünya için tek bir "donma ilerlemesi" değeri (0-100%) tutulsun. Süpernovadan sonra bu değer zamanla (config'den ayarlanabilir süre, var… | donma | — | — | yapılmadı |
| 9.3 | KADEMELİ DONMA ADIMLARI: İlerleme arttıkça dünya aşamalı değişsin, örneğin: önce hava soğur ve hafif kar/ayaz efektleri başlar → çimenler ve yapraklar kırağılanır / üzer… | donma | — | — | yapılmadı |
| 9.4 | SULARIN BUZA DÖNMESİ: Dünyadaki su blokları (deniz, okyanus, göl, nehir) git gide, yüzeyden başlayarak ve kıyılardan içeri doğru yavaş yavaş buza dönüşsün. (Not: Bu sist… | donma | — | — | yapılmadı |
| 9.5 | YENİ YÜKLENEN CHUNK'LAR: Dünyanın yeni yüklenen/keşfedilen chunk'ları da o anki duruma uygun yüklensin. Chunk yükleme olayında (ChunkEvent.Load veya benzeri) o anki glob… | donma | — | — | yapılmadı |
| 9.6 | ÖNCELİK: Donma/çözülme işlemi önce oyuncuların yüklediği/yakın chunk'larda yapılsın (oyuncunun gördüğü yerde değişim görünür olsun), uzak chunk'lar daha düşük öncelikle… | donma | — | — | yapılmadı |
| 9.7 | Donmuş blokların orijinal halini saklamak zorunda olmak performansı bozacaksa saklanmasın. Dönüşüm tablosu İKİ YÖNLÜ tanımlansın (örn. su ↔ buz, çimen ↔ kırağılı çimen,… | donma | — | — | yapılmadı |
| 9.8 | Diğer modların blokları ve yapılarla uyumlu ol: sadece su/çimen/yaprak/toprak gibi güvenli bloklar için dönüşüm tablosu kullan, tanınmayan blokları bozma, tablo config v… | donma | — | — | yapılmadı |
| 9.9 | ÇÖZÜLME (KIZIL CÜCE DÖNEMİ): Kızıl cüce güneş doğduktan sonra (bölüm D) global donma ilerlemesi %100'den yavaş yavaş bir taban değere düşsün (varsayılan taban %40, süre… | donma | — | — | yapılmadı |
| 9.10 | KALICI KIŞ BÖLGELERİ: Bazı bölgeler kalıcı olarak donmuş kalsın (%100 seviyede): soğuk/karlı biyomlar, belirli bir yüksekliğin üstü (varsayılan Y=120, config) ve dünya s… | donma | — | — | yapılmadı |
| 9.11 | Kış bölgesi dışında buzul önce ılık bölgelerden ve kıyılardan başlayarak çekilsin: ince kar katmanları ve kırağı önce kalksın, ardından su yüzeylerindeki buz kenarlardan… | donma | — | — | yapılmadı |
| 9.12 | Çözülme de batch/kuyruk, oyuncu yakınlık önceliği ve TPS kuralına uysun. | donma | — | — | yapılmadı |
| 10.1 | Kasırga, Tsunami, Deprem, Asit Yağmuru, Sinkhole ve Orman Yangını 5 şiddet seviyesine sahip olsun: Seviye 1 (Hafif), 2 (Orta), 3 (Şiddetli), 4 (Yıkıcı), 5 (Felaket). Gün… | core | — | — | yapılmadı |
| 10.2 | Seviye, afetin boyutunu, hızını, süresini, hasarını ve blok yıkım gücünü belirlesin. Her afet için seviyenin ne değiştirdiğini net tanımla ve config'den ayarlanabilir ya… | core | — | — | yapılmadı |
| 10.3 | Kasırga (F1-F5 hissi): huni genişliği/yüksekliği, hareket hızı, yaşam süresi, çekim gücü, kaldırabildiği blok sayısı ve hasar artar. | core | — | — | yapılmadı |
| 10.4 | Tsunami: dalga yüksekliği, hızı, karaya içeri ne kadar girdiği, sürükleme kuvveti ve yıkım gücü artar. | core | — | — | yapılmadı |
| 10.5 | Deprem (Richter hissi 4.0 → 9.0): yarığın uzunluğu/genişliği/derinliği, sarsıntı süresi, ekran sarsıntısı şiddeti ve yıkılan blok alanı artar. | core | — | — | yapılmadı |
| 10.6 | Asit yağmuru: yağış alanı, süresi, aşındırma hızı ve oyuncuya verilen hasar (hâlâ "ortalama" hasar sınırında kalsın, Seviye 5'te bile öldürücü olmasın) artar. | core | — | — | yapılmadı |
| 10.7 | Sinkhole: çukurun çapı ve derinliği artar. | core | — | — | yapılmadı |
| 10.8 | Orman yangını: başlangıç alanı, yayılma hızı ve süre artar (ortak yangın sistemi sınırları dahilinde). | core | — | — | yapılmadı |
| 10.9 | ŞİDDETİN OYUN İLERLEDİKÇE ARTMASI: Seviye, afet oluşurken ağırlıklı rastgele seçimle belirlensin. Oyunun başında ağırlık hafif seviyelerde olsun, oyun ilerledikçe (dünya… | core | — | — | yapılmadı |
| 10.10 | Oyunun gidişatını bozmaması için: Seviye 4-5 afetler, ardışık gelmesin (aynı seviye art arda en fazla bir kez), ve her şiddetli afetten sonra bekleme süresi biraz uzasın. | core | — | — | yapılmadı |
| 10.11 | Seviye ekranda yazı olarak GÖSTERİLMESİN (madde 5). Seviye yalnızca Gelişmiş Hava Radarı ve Hassas Sismograf'ta (madde 13) ve /dogalafet komut çıktılarında görünsün, baş… | core | — | — | yapılmadı |
| 10.12 | Test komutu: /dogalafet start <afetadı> [seviye] [oyuncu] ile seviye belirlenebilsin. | core | — | — | yapılmadı |
| 11.1 | Oyun içinde /gamerule komutuyla yönetilen özel kurallar ekle (GameRules.register ile, dünya verisinde kalıcı olsun). Varsayılanları belirt: | kural | `kural/Kurallar` (`GameRules.register`, Faz 0) | `KuralOyunTestleri` (GameTest) | yapılmadı |
| 11.2 | dogalAfetlerAcik (boolean, true): tüm afet sistemini aç/kapat. | kural | `kural/Kurallar.DOGAL_AFETLER_ACIK` (Faz 0) | `KuralOyunTestleri` | yapılmadı |
| 11.3 | kasirgaAfeti, tsunamiAfeti, depremAfeti, asitYagmuruAfeti, sinkholeAfeti, ormanYanginiAfeti, gunesPatlamasiAfeti, ayCarpmasiAfeti (hepsi boolean, true): afetleri tek tek… | kural | `kural/Kurallar` 8 afet kuralı + `afetSecilebilir` (Faz 0); güvenli bitirme yok | `KuralOyunTestleri` | yapılmadı |
| 11.4 | afetSikligi (int, 100): afet sıklığı yüzdesi (100 = normal, 50 = yarı, 200 = iki katı). | kural | `kural/Kurallar.AFET_SIKLIGI`, `KuralSinirlari.siklikCarpani` (Faz 0) | `KuralSinirlariTest`, `KuralOyunTestleri` | yapılmadı |
| 11.5 | afetMaksSiddet (int, 5): oluşabilecek maksimum şiddet seviyesi (1-5). | kural | `kural/Kurallar.AFET_MAKS_SIDDET`, `KuralSinirlari.maksSiddet` (Faz 0) | `KuralSinirlariTest`, `KuralOyunTestleri` | yapılmadı |
| 11.6 | afetBlokHasari (boolean, true): kapalıysa afetler blok yıkmaz/yakmaz/dondurmaz/çözmez (oyuncu ve mob hasarı sürer, harita korunur). | kural | `kural/Kurallar.AFET_BLOK_HASARI` (Faz 0); afetlerde uygulanmadı | `KuralOyunTestleri` | yapılmadı |
| 11.7 | sicaklikSistemi (boolean, true): vücut sıcaklığı sistemini ve HUD'u aç/kapat. | kural | `kural/Kurallar.SICAKLIK_SISTEMI` (Faz 0); sıcaklık sistemi yok | `KuralOyunTestleri` | yapılmadı |
| 11.8 | Güneş patlaması ve ay çarpması gamerule ile kapatılırsa devam eden süreçleri de güvenle durdurulabilsin (/dogalafet stop ile birlikte). Zaten ulaşılmış dünya dönemleri (… | kural | — | — | yapılmadı |
| 11.9 | Bu kuralların hepsi /gamerule <ad> ile okunabilsin/değiştirilebilsin ve tab-tamamlama desteği olsun. | kural | — | — | yapılmadı |
| 12.1 | Modun kendi başarım sekmesi olsun: "Doğal Afetler". Kök başarım: "Felaket Başlıyor" (ilk afet sirenini duyunca otomatik açılır). Başarımlar data pack içinde advancement… | basarim | — | — | yapılmadı |
| 12.2 | Kök: "Felaket Başlıyor" (görev) — modu yükleyip ilk afet sirenini duymak. | basarim | — | — | yapılmadı |
| 12.3 | "İlk Rüzgâr" (görev) — Bir kasırgadan sağ çık. | basarim | — | — | yapılmadı |
| 12.4 | "Dalga Avcısı" (görev) — Tsunami dalgası tarafından sürüklenip sağ çık. | basarim | — | — | yapılmadı |
| 12.5 | "Sallanan Zemin" (görev) — Bir depremden sağ çık. | basarim | — | — | yapılmadı |
| 12.6 | "Delikten Dönüş" (görev) — Sinkhole çökerken çukura düşüp sağ çık. | basarim | — | — | yapılmadı |
| 12.7 | "Yanmadan Çıktım" (görev) — Orman yangını alanının içinde en az 30 saniye kalıp sağ çık. | basarim | — | — | yapılmadı |
| 12.8 | "Çatı Altı" (görev) — Bir asit yağmuru boyunca hiç hasar almadan hayatta kal (çatı altında/korunakta). | basarim | — | — | yapılmadı |
| 12.9 | "Gökten Gelen Misafir" (görev) — İlk Meteorit Parçası'nı topla (madde 13). | basarim | — | — | yapılmadı |
| 12.10 | "Güneşi Atlattım" (hedef) — Güneş patlamasının 5 Minecraft günlük sürecinden sağ çık. | basarim | — | — | yapılmadı |
| 12.11 | "Süpernova Tanığı" (hedef) — Süpernovayı gökyüzünde gör. | basarim | — | — | yapılmadı |
| 12.12 | "Buzul Çağı" (hedef) — Dünya donma ilerlemesi %50'yi geçsin ve sen hayatta ol. | basarim | — | — | yapılmadı |
| 12.13 | "Kutup Sığınağı" (meydan okuma) — Donma ilerlemesi %100'e ulaştıktan sonra 3 Minecraft günü hayatta kal. | basarim | — | — | yapılmadı |
| 12.14 | "Ay Düştü" (hedef) — Ay çarpmasından sağ çık. | basarim | — | — | yapılmadı |
| 12.15 | "Yıldızlı Gece" (görev) — Ay çarpmasından sonra ilk yıldızlı/galaksili geceyi izle. | basarim | — | — | yapılmadı |
| 12.16 | "Yükselen Sular" (görev) — Deniz seviyesinin yükselmesini yaşa ve sağ kal. | basarim | — | — | yapılmadı |
| 12.17 | "Yer Dinleyici" (görev) — İlk Sismograf'ı craft et. | basarim | — | — | yapılmadı |
| 12.18 | "Radar Altında" (görev) — İlk Hava Radarı'nı craft et. | basarim | — | — | yapılmadı |
| 12.19 | "Katmanlı Giyim" (görev) — Bir zırh parçasına 5 tüy upgrade'i uygula. | basarim | — | — | yapılmadı |
| 12.20 | "Yünlü Kahraman" (hedef) — 4 parça yünlü zırhı aynı anda giy. | basarim | — | — | yapılmadı |
| 12.21 | "Çorba İçer Isınır" (görev) — Donma durumundayken sıcak bir yiyecek yiyerek vücut sıcaklığını normale çıkar. | basarim | — | — | yapılmadı |
| 12.22 | "Buz Balıkçısı" (görev) — Buz deliğinden ilk balığı tut (madde 15). | basarim | — | — | yapılmadı |
| 12.23 | "Kutup Ustası" (hedef) — Buz deliğinden toplam 25 balık tut. | basarim | — | — | yapılmadı |
| 12.24 | "Kozmik Şans" (meydan okuma) — İlk Kozmik Çekirdek'i bul (madde 13). | basarim | — | — | yapılmadı |
| 12.25 | "Fırtınaya Meydan Oku" (hedef) — Seviye 3 veya üzeri bir afetten sağ çık. | basarim | — | — | yapılmadı |
| 12.26 | "Felaket Ustası" (meydan okuma) — Seviye 5 (Felaket) bir afetten sağ çık. | basarim | — | — | yapılmadı |
| 12.27 | "Hepsini Atlattım" (meydan okuma, gizli) — Kasırga, tsunami, deprem, asit yağmuru, sinkhole, orman yangını, güneş patlaması ve ay çarpmasının hepsinden en az bir kez sağ… | basarim | — | — | yapılmadı |
| 12.28 | "Birlikte Hayatta" (hedef, gizli) — En az 3 oyuncu aynı afeti aynı anda atlatsın (aynı afetin etki alanında olup hepsi sağ çıksın). | basarim | — | — | yapılmadı |
| 12.29 | Ödüller: Başarımlar XP versin; "hedef" başarımlar ayrıca küçük bir ödül (örneğin 1-2 sıcak yiyecek), "meydan okuma" başarımları daha büyük XP ve bir kozmetik ünvan/chat… | basarim | — | — | yapılmadı |
| 13.1 | Tüm eşyaların tarifi data-driven (recipe JSON) olsun, yeni dokular için doku isimlerini ve boyutlarını bana söyle (veya basit prosedürel alternatif öner). | esya, meteor, client.esya | — | — | yapılmadı |
| 13a.1 | Tarif (3x3, şekilli): üst sıra: Demir Külçesi, Cam, Demir Külçesi; orta sıra: Kızıltaş Tozu, Pusula, Kızıltaş Tozu; alt sıra: Demir Külçesi, Bakır Külçesi, Demir Külçesi. | esya, meteor, client.esya | — | — | yapılmadı |
| 13a.2 | Elde (ana veya yan el) tutulduğunda çalışsın. Ekranda küçük, ekranı kaplamayan bir gösterge çıksın: yaklaşan veya aktif afetin adı, yönü (ok), tahmini uzaklığı (blok) ve… | esya, meteor, client.esya | — | — | yapılmadı |
| 13a.3 | Madde 4'teki ÖN-UYARI PENCERESİ boyunca (siren çalmadan önce) radar elde tutuluyorsa, yaklaşan afeti önceden gösterebilsin: oyuncu siren gelmeden hazırlık yapabilsin. Gü… | esya, meteor, client.esya | — | — | yapılmadı |
| 13a.4 | Normal radar afet seviyesini göstermesin; sadece afet türü, yön, mesafe ve süre göstersin. | esya, meteor, client.esya | — | — | yapılmadı |
| 13a.5 | GELİŞMİŞ HAVA RADARI: Hava Radarı + 1 Kozmik Çekirdek (şekilsiz tarif). Menzil 800 blok, ayrıca afet şiddet seviyesini, hareket yönünü (kasırga/tsunami gibi hareketli af… | esya, meteor, client.esya | — | — | yapılmadı |
| 13a.6 | Çok oyunculuda her oyuncu kendi radarını kullanır; sunucudan sadece ilgili oyuncuya düşük sıklıkta paket gitsin. | esya, meteor, client.esya | — | — | yapılmadı |
| 13b.1 | Tarif (3x3, şekilli): üst sıra: Demir Külçesi, Kağıt, Demir Külçesi; orta sıra: Çubuk, Kızıltaş Tozu, Çubuk; alt sıra: Taş, Bakır Külçesi, Taş. | esya, meteor, client.esya | — | — | yapılmadı |
| 13b.2 | Envanterde (hotbar veya yan el) taşınırken çalışsın. Deprem ve sinkhole için ön-uyarı penceresi boyunca (siren çalmadan önce) "öncü sarsıntı" algılasın: hafif bir zemin… | esya, meteor, client.esya | — | — | yapılmadı |
| 13b.3 | Sağ tık ile açılan küçük bir ekranda (opsiyonel, performansı etkilemesin) son 60 saniyenin sismik dalga grafiği çizilsin (tüy kalem animasyonu hissi). | esya, meteor, client.esya | — | — | yapılmadı |
| 13b.4 | Gelişmiş: Sismograf + 1 Kozmik Çekirdek → "Hassas Sismograf": ön-uyarı süresi daha erken başlar (daha uzun uyarı), merkez konumunu daha doğru gösterir, depremin seviyesi… | esya, meteor, client.esya | — | — | yapılmadı |
| 13b.5 | Eşya olmayan oyuncu için hiçbir şey değişmez; sadece normal siren gelir. | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.1 | Bu ödüller YALNIZCA güneş patlaması ve ay çarpması meteorlarından çıksın (başka afetlerden, balıktan, sandıktan, mob'dan çıkmasın). | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.2 | METEORİT PARÇASI: Her meteor çarptığında, çarpma kraterinde nadir bir şansla (varsayılan %3, config'den ayarlanabilir; büyük meteorlarda %5) bir "Meteorit Parçası" item… | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.3 | KOZMİK ÇEKİRDEK: Çok daha nadir (varsayılan %0,3, sadece büyük meteorlarda ve yalnızca bir çarpma kraterinin ortasında) bir "Kozmik Çekirdek" çıkabilsin. Mor-mavi parılt… | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.4 | METEORİT KÜLÇESİ: 3 Meteorit Parçası + 1 Demir Külçesi (şekilsiz) → 1 Meteorit Külçesi. | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.5 | TERMOSTAT PLAKASI: Meteorit Külçesi + Altın Külçesi + Kızıltaş Tozu + Demir Külçesi (şekilsiz) → 1 Termostat Plakası. | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.6 | TERMOSTATLI ZIRH: Crafting'de bir zırh parçası + Termostat Plakası → zırh "Termostatlı" olur (veri bileşeniyle, her parçaya yalnızca bir kez). Her Termostatlı parça, vüc… | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.7 | Kozmik Çekirdek kullanımı: Gelişmiş Hava Radarı ve Hassas Sismograf (yukarıda), yeni başka bir kullanım gerekmez. | esya, meteor, client.esya | — | — | yapılmadı |
| 13c.8 | Düşme şansları, süreler ve etkiler config'den ayarlanabilsin. Çok oyunculuda meteor ödülleri yere düşen eşyadır (adil kapış), oyunu bozacak kadar bol çıkmasın. | esya, meteor, client.esya | — | — | yapılmadı |
| 14.1 | Vücut sıcaklığını (madde 7) kontrollü şekilde değiştiren yiyecekler. Her yiyecek yenince hem normal yemek değeri verir hem de bir "Sıcaklık Etkisi" durum efekti (status… | yiyecek | — | — | yapılmadı |
| 14.2 | Azalan verim: Aynı türden yiyecek 60 saniye içinde tekrar yenirse etkisi %50 azalsın (spam engeli). Farklı yiyecekler kendi başına çalışsın. | yiyecek | — | — | yapılmadı |
| 14.3 | Sıcak yiyecek, ortam zaten sıcakken/oyuncu aşırı ısınmışken ısınmayı artırır (aşırı ısınmaya itebilir). Soğuk içecek, ortam zaten çok soğukken/donarken soğutmayı artırır… | yiyecek | — | — | yapılmadı |
| 14.4 | Yiyecek yeme süresi vanilla gibi (32 tick), stack boyutu 16 olsun. Container olan yemeklerde (şişe/kase) boş container geri verilsin. | yiyecek | — | — | yapılmadı |
| 14.5 | Vanilla pişmiş yiyecekler (kızarmış patates vb.) çok küçük ve kısa bir ısınma verebilsin (opsiyonel, config'den kapatılabilir). | yiyecek | — | — | yapılmadı |
| 14.6 | Sıcak Salep (orta ısınma, 90 sn): 3 Cam Şişe + Süt Kovası + Bal Şişesi (şekilsiz) → 3 Sıcak Salep (kova geri verilir). | yiyecek | — | — | yapılmadı |
| 14.7 | Sıcak Çikolata (orta ısınma, 90 sn): 3 Cam Şişe + Süt Kovası + 2 Kakao Çekirdeği + Şeker (şekilsiz) → 3 Sıcak Çikolata (kova geri verilir). | yiyecek | — | — | yapılmadı |
| 14.8 | Sıcak Tavuk Çorbası (büyük ısınma, 150 sn): Kase + Pişmiş Tavuk + Havuç + Patates (şekilsiz) → Sıcak Tavuk Çorbası (kase geri verilir). Yüksek doyuruculuk. | yiyecek | — | — | yapılmadı |
| 14.9 | Ateşli Güveç (çok büyük ısınma, 180 sn, yan etki): Kase + Pişmiş Et (sığır veya domuz) + Patates + Alev Tozu (şekilsiz) → Ateşli Güveç. Çok güçlü ısıtır ama sıcak ortamd… | yiyecek | — | — | yapılmadı |
| 14.10 | Kuru Meyveli Çay: Cam Şişe + Tatlı Çilek x2 + Bal Şişesi (şekilsiz) → 2 Isıtıcı Çay (hafif ısınma, uzun süre, 180 sn). | yiyecek | — | — | yapılmadı |
| 14.11 | Ayran (orta serinleme, 90 sn): 3 Cam Şişe + Süt Kovası + 2 Kar Topu (şekilsiz) → 3 Ayran (kova geri verilir). | yiyecek | — | — | yapılmadı |
| 14.12 | Buzlu Karpuz (hafif serinleme, 60 sn): Karpuz Dilimi + Kar Topu (şekilsiz) → Buzlu Karpuz Dilimi. | yiyecek | — | — | yapılmadı |
| 14.13 | Meyve Şerbeti (büyük serinleme, 120 sn): Cam Şişe + Tatlı Çilek x3 + Şeker + Kar Topu (şekilsiz) → Meyve Şerbeti. Sıcak çölde ve güneş patlamasında hayat kurtarır. | yiyecek | — | — | yapılmadı |
| 14.14 | Kutup Balığı Çorbası (büyük ısınma, 150 sn): Kase + Pişmiş Kutup Balığı (madde 15'teki balıklardan biri) + Patates (şekilsiz). | yiyecek | — | — | yapılmadı |
| 14.15 | Pişmiş buz balıkları (madde 15) kendi başına orta ısınma versin; çiğ olarak yenirse hafif soğutsun. | yiyecek | — | — | yapılmadı |
| 15.1 | Süpernovadan sonra dünya donunca (madde 9), oyuncular için yeni bir yiyecek ve ısınma kaynağı olarak buz deliğinden balık tutma sistemi: | balikcilik | — | — | yapılmadı |
| 15.2 | Sistem, donma ilerlemesi belirli bir eşiği (varsayılan %30, config) geçtiğinde ve suyun üstü buza dönüştüğünde aktif olsun. Kızıl cüce döneminde global ilerleme düşse bi… | balikcilik | — | — | yapılmadı |
| 15.3 | BUZ DELİĞİ AÇMA: Oyuncu bir kazma veya balta ile buz bloğuna (suyun üstündeki buza) eğilerek (shift) sağ tık yaptığında, buz bloğu "Buz Deliği" bloğuna dönüşsün (alet 5… | balikcilik | — | — | yapılmadı |
| 15.4 | BALIK TUTMA: Olta, Buz Deliği'ne atılırsa çalışsın (normal buza atılırsa tutmasın). Balık ısırma süresi normalden biraz uzun olsun (30-60 sn, Lure büyüsü hızlandırsın),… | balikcilik | — | — | yapılmadı |
| 15.5 | LOOT (balıklar, hepsinin dokusu ve adı Türkçe): Buz Alabalığı (yaygın), Kutup Morinası (yaygın), Derin Soğuk Somonu (orta), Buz Kefali (orta), Donmuş Ringa (nadir, daha… | balikcilik | — | — | yapılmadı |
| 15.6 | YİYECEK ÖZELLİKLERİ: Çiğ balıklar hafif soğutur (ama karnı doyurur), pişmiş balıklar (ocak/fırın/ateş) orta ısınma verir ve kendi başlarına değerli yiyecektir; Kutup Bal… | balikcilik | — | — | yapılmadı |
| 15.7 | RİSK/DENGE: Buz üstünde hareketsiz durarak balık tutmak vücudu daha hızlı soğutsun (hareketsizlik + soğuk). Oyuncu yanında ateş/sıcak yiyecek bulundurmak veya kalın giyi… | balikcilik | — | — | yapılmadı |
| 15.8 | Başarımlar (madde 12) ile bağlantılı çalışsın: "Buz Balıkçısı", "Kutup Ustası". | balikcilik | — | — | yapılmadı |
| 16.1 | Güneş büyümesi, süpernova, güneşsiz yıldızlı/nebulalı gökyüzü, ay çarpması sonrası aysız yıldızlı gece ve kızıl cüce güneş AYNI gökyüzü renderer'ını ve aynı sunucu taraf… | gokyuzu, client.gokyuzu | — | — | yapılmadı |
| 16.2 | Gökyüzü durumu dünya verisinde (SavedData) saklansın, durum değişince tüm oyunculara paketle gitsin, sonradan giren oyuncuya doğru senkronize edilsin. | gokyuzu, client.gokyuzu | — | — | yapılmadı |
| 16.3 | Gökyüzü değişiklikleri yalnızca görsel olsun: vanilla ışık motoruna, gün/gece döngüsüne ve mob spawn kurallarına dokunma (diğer modlarla uyumluluk için). | gokyuzu, client.gokyuzu | — | — | yapılmadı |
| 16.4 | Ay durumu (var/yok) ve güneş durumu (normal / büyüyen / süpernova / güneşsiz / kızıl cüce) birbirinden bağımsız tutulsun ve her kombinasyonda doğru görünsün. | gokyuzu, client.gokyuzu | — | — | yapılmadı |

## DOĞAL AFETLER (bölüm A–H)

| Madde | Şartname (özet) | Beklenen sahip | Uygulayan sınıf/dosya | Kanıt | Durum |
|---|---|---|---|---|---|
| A.1 | Rastgele zamanda, rastgele yerde, siren ve ses eşliğinde haritada kasırga oluşsun. | `afet.kasirga` | — | — | yapılmadı |
| A.2 | Gerçek kasırga gibi görünsün: dönen huni şekli, toz/enkaz bulutu, yerden kalkan bloklar ve parçalar. Görsel olarak etkileyici ve detaylı olsun. | `afet.kasirga` | — | — | yapılmadı |
| A.3 | Oyunculara, hayvanlara/mob'lara ve haritaya (bloklar, ağaçlar) zarar versin, nesneleri havaya çeksin. | `afet.kasirga` | — | — | yapılmadı |
| A.4 | Kasırga hareket etsin ve bir süre sonra dağılsın. Oyunun gidişatını bozmasın (sık spawn olmasın, aynı anda çok sayıda olmasın). | `afet.kasirga` | — | — | yapılmadı |
| B.1 | Tsunami, Minecraft'ın su blokları yerine kendi yazdığın özel render edilen su ile yapılsın (madde 8'deki ortak özel su sistemi). Bu su haritaya taşmasın, harita verisini… | `afet.tsunami` | — | — | yapılmadı |
| B.2 | Tsunami olmadan önce yakındaki kıyılardaki ve denizdeki sular geri çekilsin (görsel olarak), sonra toplanıp dev bir dalga oluşturup karaya doğru ilerlesin. Gerçek tsunam… | `afet.tsunami` | — | — | yapılmadı |
| B.3 | SÜRÜKLEME: Tsunami dalgası, kendisine çarpan oyuncuları, mob'ları ve düşen eşyaları dalganın ilerleme yönünde güçlü şekilde SÜRÜKLESİN (itme/velocity uygulansın). Oyuncu… | `afet.tsunami` | — | — | yapılmadı |
| B.4 | Tsunaminin suyuna giren oyuncular ve mob'lar ayrıca hasar alsın. | `afet.tsunami` | — | — | yapılmadı |
| B.5 | Tsunami haritadaki blokları, ağaçları ve evleri yıkabilsin (batch ile ama yıkım akıcı ve etkileyici görünsün). | `afet.tsunami` | — | — | yapılmadı |
| B.6 | Tsunami sırasında özel yazdığın, kaliteli bir yağmur efekti yağsın (kendi yaptığın özel yağmur parçacığı/efekti). | `afet.tsunami` | — | — | yapılmadı |
| B.7 | MAĞARALARA GİREN SU: Tsunaminin suyu mağaralara, tünellere ve yeraltı boşluklarına girerse orada kalsın (oyuncuyu da sürükleyip içeri taşıyabilsin). Su, mağarada belirli… | `afet.tsunami` | — | — | yapılmadı |
| B.8 | Oyunun gidişatını bozmayacak şekilde, normalde sık olmayan bir şekilde oluşsun. Ancak ay çarpması gerçekleştikten sonra tsunami olasılığı geçici olarak ARTSIN (bkz. madd… | `afet.tsunami` | — | — | yapılmadı |
| C.1 | Rastgele zamanda haritada deprem olsun. | `afet.deprem` | — | — | yapılmadı |
| C.2 | Klasik olarak haritayı ikiye bölen, gerçek depremdeki gibi bir yarık/fay hattı oluşsun. Yarık gerçekçi, derin ve etkileyici görünsün (batch ile ama ilerleyişi akıcı olsu… | `afet.deprem` | — | — | yapılmadı |
| C.3 | Etraftaki ağaç yaprakları, tahta evler gibi "sağlam olmayan" bloklar yıkılıp yere düşsün. Sağlam bloklar (taş, deepslate, obsidyen vb.) daha az etkilensin. | `afet.deprem` | — | — | yapılmadı |
| C.4 | Deprem sırasında oyuncuların ekranı titresin ve deprem sesi duyulsun (depremin yakınındaki oyuncularda daha şiddetli, uzaktakilerde daha hafif). | `afet.deprem` | — | — | yapılmadı |
| C.5 | Sismograf (madde 13b) ile ön-uyarı penceresinde öncü sarsıntı algılanabilsin. | `afet.deprem` | — | — | yapılmadı |
| D.1 | Sadece oyunun ilerleyen zamanlarında olsun: oyuncular yeterince geliştiğinde (örneğin oyun içi gün sayısı, ilerleme/başarım veya eşya seviyesi gibi bir kriter; kriteri c… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.2 | Gerçek hayattaki gibi uzun bir uyarı sireni çalsın (kısık ses kuralına uysun); bu, güneş patlamasının habercisi olsun. | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.3 | Siren başladıktan sonra gökyüzündeki güneş yavaş yavaş büyüsün. | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.4 | Siren başladıktan 60 saniye sonra dünyaya meteorlar düşmeye başlasın (siren bu anda fade-out ile dursun). Oyuncular haritanın en alt katmanlarına inip taş, deepslate vey… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.5 | Düşen her meteor, çarptığı yerde ateş çıkarsın ve bu ateşler orman yangınındaki gibi etrafa tutuşup yayılsın (çimen, yaprak, ağaç, odun gibi yanıcı her şeyi yaksın, sağl… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.6 | Meteorlar her oyuncunun çevresinde dengeli şekilde düşsün (tek oyuncuya yığılma olmasın, herkes risk altında olsun). | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.7 | Güneş patlamasından sonra ortam sıcaklığı yükselsin (madde 7); oyuncu gölgede/korunaklı yerde durmazsa vücut sıcaklığı artıp yanmaya başlasın, etraf git gide yanmaya baş… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.8 | Bu süreç 5 Minecraft günü boyunca devam etsin. | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.9 | 5 Minecraft günü sonunda güneş patlasın ve gökyüzünde güneşin süpernovası görünsün. Süpernova 1-2 Minecraft günü boyunca gözüksün. | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.10 | GÜNEŞSİZ DÖNEM: Süpernova parıltısı yavaşça sönerek bir süpernova kalıntısı nebulasına dönüşsün. Bundan sonra gökyüzünde güneş olmasın; gündüz de gece de gökyüzü karanlı… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.11 | Süpernovadan sonra madde 9'daki ortak dünya donma sistemi başlasın: dünya anında değil, YAVAŞ YAVAŞ donsun. Sular git gide buza dönsün, çevre kırağılansın/karlansın, yen… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.12 | Güneş patlaması sırasında (süpernovadan ÖNCE) dünyadaki sular buharlaşsın. Süpernovadan sonra donma başladığında su yeniden oluşmuş/kalmış alanlar donma sistemine dahil… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.13 | Donma ilerlemesi %100'e ulaştıktan belirli bir süre sonra (varsayılan 7 Minecraft günü, config; "Kutup Sığınağı" başarımının 3 günlük şartından kısa olamaz) yeni bir gün… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.14 | DOĞUŞ: İlk şafakta, yıldızlı ve nebulalı gökyüzünün arasından ufukta soluk bir kızıl ışık belirsin ve yeni güneş yavaşça yükselsin. Yıldızlar ve nebula, kızıl cüce gökyü… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.15 | GÖRÜNÜM: Eski güneşten daha küçük (varsayılan eski boyutun %60'ı, config), soluk turuncu-kırmızı bir güneş olsun. Gündüz gökyüzü ve sis rengi hafif kızıl/turuncu tonlu,… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.16 | ISI: Eskisinden zayıf ısıtsın. Ortam sıcaklığı kademeli olarak toparlansın ama eski dünyanın normal değerlerine ulaşmasın (config'li sıcaklık ofseti, varsayılan belirgin… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.17 | ÇÖZÜLME: Dünya tamamen erimesin; buzul yavaş yavaş çekilsin (madde 9 ÇÖZÜLME). Kalıcı "kış" bölgeleri donmuş kalsın. | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.18 | OYUN DEVAM EDER: Oyun bitmesin; yeni bir dönem başlasın. Diğer afetler (kasırga, tsunami, deprem, asit yağmuru, sinkhole, orman yangını, ayı hâlâ varsa ay çarpması) norm… | `afet.gunes_patlamasi` | — | — | yapılmadı |
| D.19 | Kızıl cüce dönemi dünya verisinde saklansın, sunucu yeniden başlayınca kaybolmasın ve sonradan giren oyuncuda doğru görünsün. | `afet.gunes_patlamasi` | — | — | yapılmadı |
| E.1 | Rastgele zamanda haritada asit yağmuru yağsın. Görsel olarak ayırt edilebilir olsun (yeşilimsi/sarımsı yağmur efekti, bloklara düşünce dumanlanma). | `afet.asit_yagmuru` | — | — | yapılmadı |
| E.2 | Sert bloklar hariç, asidin düştüğü yerdeki blokları yavaş yavaş kırsın/aşındırsın. | `afet.asit_yagmuru` | — | — | yapılmadı |
| E.3 | Oyuncuya değerse hasar versin ama çok fazla değil, orta düzeyde hasar olsun. Çatı altında/korunaklı yerde olan oyuncuya zarar vermesin. | `afet.asit_yagmuru` | — | — | yapılmadı |
| F.1 | Rastgele bir yerde, siren ve ses eşliğinde haritada büyük bir delik/çukur oluşsun. | `afet.sinkhole` | — | — | yapılmadı |
| F.2 | Gerçek sinkhole'lara benzesin (internetten fikir al): yer çökmeden önce çatlama/titreme, sonra dairesel çökme, bloklar çukura dolsun. | `afet.sinkhole` | — | — | yapılmadı |
| F.3 | Batch sistemiyle uygula ama çöküş akıcı ve etkileyici görünsün. Sismograf (madde 13b) ile ön-uyarı penceresinde öncü sarsıntı algılanabilsin. | `afet.sinkhole` | — | — | yapılmadı |
| G.1 | Rastgele zamanda başlasın ve alev tutuşa tutuşa etrafı yaksın. | `afet.orman_yangini` | — | — | yapılmadı |
| G.2 | Sadece ağaçlarla sınırlı olmasın: çimen, çiçek, yaprak, odun gibi yanıcı olan her şey yanabilsin. Sağlam (yanmaz) bloklar yanmasın. | `afet.orman_yangini` | — | — | yapılmadı |
| G.3 | Yangın yayılma hızı kontrollü olsun, oyunun gidişatını bozmasın. Yağmur yağınca sönebilsin. Alev ve duman efektleri gerçekçi ve yoğun görünsün. Yangına yakın bölgelerde… | `afet.orman_yangini` | — | — | yapılmadı |
| G.4 | Bu afetin yangın mantığı madde 6'daki ortak yangın sistemidir; güneş patlaması ve ay çarpması meteor ateşleri de aynı sistemi kullanır. | `afet.orman_yangini` | — | — | yapılmadı |
| H.1 | Güneş patlaması ile aynı mantıkta ama biraz daha az nadir (güneş patlamasından biraz daha sık) ve biraz daha hafif. Ay yok olduktan sonra tekrar oluşmasın (madde 4). | `afet.ay_carpmasi` | — | — | yapılmadı |
| H.2 | Siren çaldıktan 60 saniye sonra ay dünyaya çarpsın (siren bu anda fade-out ile dursun). | `afet.ay_carpmasi` | — | — | yapılmadı |
| H.3 | Çarpma anında tüm oyuncuların ekranı ve harita sallansın. | `afet.ay_carpmasi` | — | — | yapılmadı |
| H.4 | Ardından 1-2 dakika boyunca dünyaya meteorlar düşsün. Bu meteorların çıkardığı ateşler de orman yangınındaki gibi etrafa tutuşup yayılsın (ortak yangın sistemi). Bu mete… | `afet.ay_carpmasi` | — | — | yapılmadı |
| H.5 | TSUNAMİ OLASILIĞI ARTIŞI: Ay çarpması gerçekleştikten sonra (ay patladığı andan itibaren) tsunami oluşma olasılığı geçici olarak belirgin şekilde artsın (örneğin normal… | `afet.ay_carpmasi` | — | — | yapılmadı |
| H.6 | DENİZ SEVİYESİ YÜKSELMESİ: Ay patladıktan 10 dakika (gerçek zaman, config'den ayarlanabilir) sonra dünyadaki tüm denizlerde, okyanuslarda, göllerde ve nehirlerde su sevi… | `afet.ay_carpmasi` | — | — | yapılmadı |
| H.7 | Ay patladıktan sonra, Minecraft dünyasında gece olduğunda (güneş olmadığında) gökyüzünde ARTIK HİÇ AY GÖRÜNMESİN; onun yerine çok sayıda yıldız, galaksi ve nebula görüns… | `afet.ay_carpmasi` | — | — | yapılmadı |

## KOMUTLAR

| Madde | Şartname (özet) | Beklenen sahip | Uygulayan sınıf/dosya | Kanıt | Durum |
|---|---|---|---|---|---|
| KOM.1 | Tüm komutlar yalnız OP yetkisiyle (yetki seviyesi 2) kullanılır. | komut | `komut/DogalAfetKomutu` (Faz 0) | `KomutOyunTestleri` | yapılmadı |
| KOM.2 | /dogalafet start <afetadı> [seviye] [oyuncu] (afet adları N18). | komut + core | — | — | yapılmadı |
| KOM.3 | /dogalafet stop | komut + core | — | — | yapılmadı |
| KOM.4 | /dogalafet sicaklik set <oyuncu> <değer> | sicaklik | — | — | yapılmadı |
| KOM.5 | /dogalafet sicaklik bilgi <oyuncu> | sicaklik | — | — | yapılmadı |
| KOM.6 | /dogalafet denizseviyesi <değer> (su yükselmesini manuel ayarlama/sıfırlama) | su | — | — | yapılmadı |
| KOM.7 | Ay çarpması sonrası bekleme süresini atlayan test komutu: /dogalafet aysonrasi atla (N18) | afet.ay_carpmasi | — | — | yapılmadı |
| KOM.8 | /dogalafet donma <yüzde> (global donma ilerlemesini ayarlama/sıfırlama) | donma | — | — | yapılmadı |
| KOM.9 | /dogalafet donma bilgi (mevcut ilerleme ve işlenen chunk sayısı) | donma | — | — | yapılmadı |
| KOM.10 | /dogalafet kizilcuce baslat (bekleme süresini atlayıp doğuşu hemen başlatır) | afet.gunes_patlamasi + gokyuzu | — | — | yapılmadı |
| KOM.11 | /dogalafet kizilcuce bilgi (gökyüzü dönemi, çözülme ilerlemesi, oyuncunun chunk'ı kalıcı kış mı) | donma + gokyuzu | — | — | yapılmadı |
| KOM.12 | /dogalafet gun <sayı> (dünya yaşını ayarlayıp seviye ağırlıklarını görme) | core | — | — | yapılmadı |
| KOM.13 | /dogalafet odul meteor (test için Meteorit Parçası / Kozmik Çekirdek verme) | meteor | — | — | yapılmadı |
| KOM.14 | N18: /dogalafet durum (aktif afet, aşama, seviye, kalan süre, beklemeler, çarpanlar, deniz seviyesi, ay/güneş, donma ve buharlaşma) | core | `komut/DogalAfetKomutu.durum` (Faz 0: yalnız "Henüz afet yok.") | `KomutOyunTestleri` | yapılmadı |
| KOM.15 | N18: /dogalafet asama ileri (aktif afeti ya da dünya dönemini sonraki aşamaya geçirir) | core | — | — | yapılmadı |
| KOM.16 | N18: /dogalafet hiz <çarpan> (afet ve dönem zamanlayıcılarını hızlandırır) | core | — | — | yapılmadı |
| KOM.17 | N18: /dogalafet sifirla (dünyanın afet ve dönem durumunu ilk haline döndürür) | core | — | — | yapılmadı |

## TESLİM EDİLECEKLER

| Madde | Şartname (özet) | Beklenen sahip | Uygulayan sınıf/dosya | Kanıt | Durum |
|---|---|---|---|---|---|
| TES.1 | Kod eksiksiz: placeholder ya da "buraya kodu ekle" boşluğu yok. | hepsi | — | — | yapılmadı |
| TES.2 | Tüm sınıflar ve registry'ler yazılı. | hepsi | — | — | yapılmadı |
| TES.3 | Mixin gerekiyorsa mixin config'leri (gerekçesi KARARLAR'da). | ilgili işçi | — | — | yapılmadı |
| TES.4 | build.gradle | çekirdek | `build.gradle` (Faz 0) | `./gradlew build` | yapılmadı |
| TES.5 | mods.toml | çekirdek | `src/main/resources/META-INF/mods.toml` (Faz 0) | `./gradlew build` | yapılmadı |
| TES.6 | pack.mcmeta | çekirdek | `src/main/resources/pack.mcmeta` (Faz 0) | `./gradlew build` | yapılmadı |
| TES.7 | Lang dosyaları (en_us, tr_tr; datagen) | veri + hepsi | `veri/DataGenerators`, `veri/DilSaglayici` (Faz 0) | `./gradlew runData` | yapılmadı |
| TES.8 | Tarif (recipe) JSON'ları (datagen) | sicaklik, esya, meteor, yiyecek | — | — | yapılmadı |
| TES.9 | Loot table / loot modifier'lar (datagen) | balikcilik, meteor | — | — | yapılmadı |
| TES.10 | Advancement JSON'ları (datagen) | basarim | — | — | yapılmadı |
| TES.11 | Ses tanımları (sounds.json; datagen) | uyari + afetler | `veri/SesSaglayici` (Faz 0; henüz ses yok) | — | yapılmadı |
| TES.12 | Ses ve doku dosyalarının adları, klasörleri (README; N19: tools/assets ile üretilir) | hepsi + README | — | — | yapılmadı |

## NETLEŞTİRMELER (N1–N19)

| Madde | Şartname (özet) | Beklenen sahip | Uygulayan sınıf/dosya | Kanıt | Durum |
|---|---|---|---|---|---|
| N1 | NBT: Minecraft 1.20.1'de eşya veri bileşeni (data component) yok, 1.20.5'te geldi. "NBT/veri bileşeni" ve "veri bileşeniyle" ifadeleri (tüy upgrade'i, Yünlü, Termostatlı… | sicaklik, esya | — | — | yapılmadı |
| N2 | Tüy upgrade'i elleri: "SAĞ el" ana el (tüy), "SOL el" yardımcı el / offhand (zırh) demektir; oyuncu sol elli ayardaysa da ana/yardımcı el eşlemesi geçerlidir. Oyuncu bir… | sicaklik | — | — | yapılmadı |
| N3 | Zırh tarifleri (yün, Termostat Plakası): İkisi de vanilla ve modlu her zırh parçasını (`ArmorItem`) kabul eden özel şekilsiz tarif tipidir; NBT, büyüler, dayanıklılık ve… | sicaklik, esya | — | — | yapılmadı |
| N4 | Yalıtım modeli: Vücut sürekli metabolik ısı üretir (hareket ve koşmayla artar); yalıtım (zırh, yün, tüy) vücut ile ortam arasındaki ısı alışverişini yavaşlatır. Soğukta… | sicaklik | — | — | yapılmadı |
| N5 | Siren ses kategorisi: Vanilla `SoundSource` enum'una yeni kategori eklemek enum hack'i ya da mixin ister ve başka modlarla çakışabilir [doğrulanmadı, araştır]. Tercih ed… | uyari, client.uyari | — | — | yapılmadı |
| N6 | "Ekranda uyarı yazısı yok" kuralının kapsamı: Kural, afetin geldiğini, türünü ya da seviyesini herkese yazıyla duyuran her şeyi kapsar: başlık, banner, toast, sohbet mes… | uyari, esya, sicaklik | — | — | yapılmadı |
| N7 | Radarda güneş patlaması ve ay çarpması [yorum, Berk değiştirebilir]: Madde 13a'daki parantez harfiyen uygulanır: bu iki afet ön-uyarı penceresinde radarda görünmez; sire… | esya | — | — | yapılmadı |
| N8 | Gamerule, config ve Cold Sweat: Gamerule'lar dünya başına aç/kapa ve çarpandır; sayısal ayarlar config'te durur. `afetSikligi` config'teki olasılıkları çarpar; `afetMaks… | config, sicaklik, kural | `config/SicaklikSistemiModu`, `SunucuAyarlari.SICAKLIK_SISTEMI_MODU` (Faz 0) | `SicaklikSistemiModuTest` | yapılmadı |
| N9 | Gerçek blok değişiklikleri ve buharlaşma [yorum, Berk değiştirebilir]: Madde 8'deki "gerçek su bloklarına dokunma" kuralı yalnız ortak özel su sistemi içindir. Dünya don… | donma | — | — | yapılmadı |
| N10 | Dünya dönemleri arasındaki geçişler [yorum, Berk değiştirebilir]: Ay çarpmasından sonra güneş patlaması gelirse buharlaşma önce sanal deniz yükselmesini kademeli olarak… | donma, su, core | — | — | yapılmadı |
| N11 | Boyut: Afetler Overworld'de oluşur (config'te boyut listesi). Nether/End'deki oyuncular sireni ve global gökyüzü durumunu yine alır. | core | — | — | yapılmadı |
| N12 | Blok sınıfları etiketlerle tanımlanır: Sert / sağlam / yanmaz bloklar (ör. `dogalafetler:asit_dayanikli`, `dogalafetler:deprem_dayanikli`; varsayılanlar taş, deepslate,… | yangin, donma, afetler (etiketler) | — | — | yapılmadı |
| N13 | Efekt kalitesi: Varsayılan "Yüksek" (metindeki "Kalite" modu); "Düşük Efekt" modu "Düşük" seviyesidir; "Ultra" güçlü makineler için isteğe bağlı üst seviyedir. | config, client.* | `config/EfektKalitesi`, `IstemciAyarlari.EFEKT_KALITESI` varsayılan YUKSEK (Faz 0) | — | yapılmadı |
| N14 | Tsunami yeri: Hedef oyuncunun yakınında okyanus/geniş su yoksa uygun başka bir oyuncu seçilir; hiçbiri uygun değilse tsunami ertelenir ve sıradaki afet seçilir. | afet.tsunami, core | — | — | yapılmadı |
| N15 | İstemci/sunucu ayrımı: Madde 3'teki `@OnlyIn(Dist.CLIENT)` ifadesinin amacı ayrımdır; Forge belgesi bu anotasyonu modlarda doğrudan kullanmayı önermez ("There is NO reas… | client + hepsi | `client/package-info.java` kuralı (Faz 0) | — | yapılmadı |
| N16 | Başarım ölçütleri [yorum, Berk değiştirebilir]: "Süpernovayı gökyüzünde gör": süpernova aşamasında Overworld'de gökyüzünü gören bir konumda bulunmak. "Yıldızlı Gece": ay… | basarim | — | — | yapılmadı |
| N17 | Sayısal varsayılanlar: Metinde sayısı verilmeyen her değer (sıcaklık birimi ve eşikleri, hasar miktarları, afet boyutları ve süreleri, seviye tablosu, olasılıklar, bekle… | hepsi + docs/KARARLAR.md | — | — | yapılmadı |
| N18 | Komut ayrıntıları: Afet adları `kasirga`, `tsunami`, `deprem`, `gunes_patlamasi`, `asit_yagmuru`, `sinkhole`, `orman_yangini`, `ay_carpmasi`. Berk'in istediği "ay çarpma… | komut + core | `core/AfetTuru` kimlikleri (Faz 0) | `AfetTuruTest` | yapılmadı |
| N19 | Dokular ve sesler: Madde 13'teki ve TESLİM'deki "bana söyle / alternatif öner" isteği vibe düzeninde şöyle karşılanır: doku ve sesleri işçiler kendisi üretir ve oyunda g… | tools/assets + README | — | — | yapılmadı |

# Doğal Afetler — Ürün şartnamesi

Bu dosya **ne** yapılacağını söyler; nasıl çalışılacağı `docs/GOREV.md`'de.

- "GENEL KURALLAR", "DOĞAL AFETLER", "KOMUTLAR" ve "TESLİM EDİLECEKLER" bölümleri Berk'in 2026-11-05 tarihli metnidir, kelimesi kelimesine (yalnız terminalden gelen satır kırılmaları birleştirildi; başlıklar ve başarım listesinin girintisi Markdown'a çevrildi).
- Berk'in metnindeki "ÇIKTI VE ÇALIŞMA ŞEKLİ" maddeleri 1–4 (aşamalı çalış, her adımda "devam edeyim mi?" diye sor) ve "BİLGİSAYAR KURULUMU" bölümü (Windows masaüstü betiği, MDK adımları, kopyala-yapıştır) sohbet içindi; vibe düzeninde yerlerini `docs/GOREV.md` aldı. Windows'ta oynama ve derleme adımları README'ye girer.
- **Kim kim:** Berk (Kadir Berk Polat) şartnamenin sahibidir; bu oturumdaki kullanıcı onun adına karar verebilir. Jony, Berk'in asistanıdır ve netleştirmeleri yazmıştır.
- En sondaki NETLEŞTİRMELER Jony'nindir; Berk değiştirebilir. Berk'in metninde teknik olarak yanlış ya da açık kalan yerleri yorumlar; çelişkide netleştirme geçerlidir. Netleştirmenin dokunmadığı her yerde Berk'in metni olduğu gibi uygulanır.

---

Sen uzman bir Minecraft modu geliştiricisisin. Minecraft 1.20.1 için Forge ile çalışan, "Doğal Afetler" adında bir mod yazmanı istiyorum.
Aşağıdaki tüm özellikleri eksiksiz uygula. Kendi kafandan yeni doğal afet ekleme; sadece benim yazdıklarımı yap. Gerçekçi afet davranışları, sıcaklık sistemi ve görsel/ses fikirleri için internetten araştırma yapabilirsin.

## GENEL KURALLAR

### 1) GÖRSEL KALİTE VE OPTİMİZASYON DENGESİ
- Öncelik sırası: önce görsel kalite ve gerçekçilik, sonra optimizasyon. Optimizasyon önemli ama efektleri bozacak, sulandıracak veya çirkinleştirecek kadar agresif olmasın.
- Afetler etkileyici, yoğun ve gerçekçi görünmeli: yeterli parçacık, detaylı dalga/kasırga animasyonu, akıcı yıkım efektleri. "Kasmasın diye" efekti kırpma.
- Orta seviye bir bilgisayarda akıcı çalışması hedeflensin. Çok düşük özellikli bilgisayarlar için config'te opsiyonel bir "Düşük Efekt" modu olsun ama VARSAYILAN mod "Kalite" modu olsun.
- Optimizasyonu görüntüyü bozmadan sağlayan akıllı yöntemler kullan: ağır işlemleri tick'lere böl (batch/kuyruk), yalnızca oyuncuya yakın alanlarda yüksek detay göster, uzakta detayı kademeli azalt (LOD), görünmeyen yerleri hesaplama. Yakındaki oyuncunun gördüğü kısım her zaman en kaliteli haliyle kalsın.
- Config dosyasında efekt kalitesi (Düşük / Orta / Yüksek / Ultra), parçacık yoğunluğu ve yıkım hızı ayarlanabilsin.

### 2) UYUMLULUK
- Sürüm: Minecraft 1.20.1, Forge.
- Diğer modlarla uyumlu çalışmalı: vanilla sistemlerini gereksiz yere ezme (overwrite) yapma, event sistemi kullan (loot için Global Loot Modifier, vb.), kendi registry namespace'ini kullan, başka modların bloklarına zarar veren sert bağımlılıklar kurma.
- e4mc modu ile uyumlu olmak ZORUNDA. Tüm afet mantığı sunucu tarafında (server-authoritative) çalışsın, istemciye sadece gerekli senkronizasyon paketleri (SimpleChannel) gitsin; ses, ekran sarsıntısı, sıcaklık barı, gökyüzü durumu, radar/sismograf göstergeleri ve efektler istemcide güvenli şekilde tetiklensin. Böylece LAN'ı e4mc ile internete açan oyunlarda da sorunsuz çalışsın.
- Tek oyunculu, LAN/e4mc ve dedicated server olmak üzere hepsinde çalışmalı.
- Cold Sweat gibi başka sıcaklık modları yüklüyse çakışma olmasın: config'te kendi sıcaklık sistemimizi kapatma/açma seçeneği olsun, aynı anda iki sistem çalışıp oyuncuya çift hasar vermesin.

### 3) ÇOK OYUNCULU DESTEK
- Mod, arkadaş grubu ölçeğinde çok oyunculu oynanabilsin: hedef 5-10 oyuncu (maksimum 10). Hem e4mc ile açılmış LAN dünyasında hem dedicated server'da sorunsuz çalışsın. Büyük sunucular için ekstra optimizasyon gerekmez, ama bu 5-10 kişilik oyunda herkes için akıcı olsun.
- Tüm afet durumu (aktif afet, şiddet seviyesi, konumu, aşaması, kalan süre, deniz seviyesi artışı, dünya donma ilerlemesi, gökyüzü/güneş dönemi) sunucuda tutulsun ve tüm oyunculara tutarlı şekilde senkronize edilsin. Herkes aynı afeti aynı yerde, aynı aşamada görsün (desync olmasın).
- Her oyuncu için ayrı hesaplama gereken şeyler (vücut sıcaklığı, donma, yanma, asit hasarı, ekran sarsıntısı, başarım ilerlemesi, sıcaklık yiyeceği etkileri) oyuncu bazında çalışsın: bir oyuncunun durumu diğerini etkilemesin.
- Ağ trafiği makul olsun: paketler sadece ilgili oyunculara gitsin, her tick'te gereksiz paket gönderme, durum değişimlerinde ve düşük sıklıkta güncelleme paketleri kullan (sıcaklık değeri de saniyede birkaç kez veya değişim olunca gitsin).
- Afet konumu seçilirken rastgele bir çevrimiçi oyuncunun çevresi seçilsin; aynı oyuncu art arda hedef alınmasın. Afet bir oyuncunun yakınında başlasa bile etkilenebilecek diğer oyuncular da sireni duysun.
- Sunucu TPS'i belirli bir değerin altına düşerse yıkım, yangın yayılması, donma/çözülme yayılması ve efekt işlemleri otomatik yavaşlasın.
- Afet sıklığı oyuncu sayısıyla orantısız artmasın: 1 kişi de 10 kişi de aynı genel afet sıklığını yaşasın (config'den ayarlanabilir).
- Güneş patlaması ve ay çarpması gibi sunucu geneli afetlerde "oyuncunun geliştiği" kriteri, çevrimiçi oyuncuların ortalamasına veya en gelişmiş oyuncuya göre config'den seçilebilsin. Sirenler ve global olaylar (güneş büyümesi, süpernova, güneşsiz dönem, kızıl cüce güneşin doğuşu, donma ve çözülme, yıldızlı gökyüzü, deniz seviyesi yükselmesi) tüm oyunculara aynı anda gitsin.
- Oyuncular oyundan çıkıp girebilsin: afet sırasında sunucuya giren oyuncuya mevcut afet durumu ve gökyüzü dönemi doğru şekilde senkronize edilsin. Oyuncu ölüp yeniden doğunca (respawn) efektler/durumlar (vücut sıcaklığı dahil) doğru şekilde sıfırlansın.
- Thread-safe yaz: sunucu tarafı işlemleri ana sunucu thread'inde yap, istemci kodunu (@OnlyIn(Dist.CLIENT)) sunucu koduna karıştırma. Dedicated server'da istemci sınıfları yüklenip çökme olmasın.
- Komutlar sadece yetkili oyuncular (OP / yetki seviyesi) tarafından kullanılabilsin.

### 4) RASTGELE OLUŞMA / DENGE
- Tüm afetler haritada rastgele zamanda ve rastgele yerde oluşsun.
- Afetler oyunun gidişatını bozmayacak şekilde oluşsun: iki afet aynı anda başlamasın, afetler arasında minimum bekleme süresi olsun, bir oyuncunun çevresinde art arda afet çıkmasın.
- Hiçbir afet "hiç olmayacak" kadar nadir olmasın, ama oyunu bunaltacak kadar sık da olmasın. "Her X günde bir kesin olur" gibi sabit bir takvim de olmasın; olasılık tabanlı, hafif rastgele bir sistem olsun.
- Nadirlik sırası: Kasırga, Tsunami, Deprem, Asit Yağmuru, Sinkhole, Orman Yangını (görece sık, birbirine yakın) → Ay Çarpması (nadir) → Güneş Patlaması (en nadir, ama yine de oyun boyunca yaşanabilir).
- TEK SEFERLİK OLAYLAR: Ay çarpması gerçekleşip ay yok olduktan sonra ay çarpması tekrar seçilmesin (gökyüzünde ay kalmadı). Güneş patlaması süreci kızıl cüce dönemine ulaştıktan sonra tekrar seçilmesin (config: "güneş patlaması tekrarlanabilir", varsayılan false). Diğer afetler her dönemde normal şekilde devam etsin.
- Her afetin oluşma olasılığı ve bekleme süresi config dosyasında ayarlanabilir olsun.
- OLASILIK ÇARPANI SİSTEMİ: Bazı olaylar (örneğin ay çarpması sonrası) belirli afetlerin olasılığını geçici olarak artırabilsin. Bunun için afet yöneticisinde "geçici olasılık çarpanı" mekanizması olsun: çarpan belli bir süre aktif kalsın, sonra yavaş yavaş normale dönsün. Artış olsa bile iki afet aynı anda başlamasın ve minimum bekleme süresi kuralı bozulmasın.
- ÖN-UYARI PENCERESİ: Afet yöneticisi, afet seçildikten sonra siren çalmadan önce gizli bir "ön-uyarı penceresi" (varsayılan 45-90 saniye, config'den ayarlanabilir) işletsin. Bu pencerede afet zaten kararlaştırılmıştır ama oyunculara herhangi bir bildirim gitmez. Sadece madde 13'teki Hava Radarı ve Sismograf eşyalarına sahip oyuncular bu pencerede bilgi alabilir. Eşyası olmayan oyuncular için oyun, siren çalana kadar normal devam eder (yani eşyalar isteğe bağlı bir avantajdır, zorunlu değildir).

### 5) UYARI SİSTEMİ (SADECE SİREN, EKRAN YAZISI YOK)
- Her afet başlamadan önce siren sesi çalsın. Siren, afetten etkilenebilecek tüm oyunculara gitsin.
- Güneş Patlaması hariç tüm afetlerde siren 10 saniye sürsün. Güneş Patlaması sireni özel ve daha uzun/ürkütücü olsun.
- SİREN SES SEVİYESİ: Oyuncular sesli sohbette (Discord vb.) konuşurken arkadaşlarını duyabilmeli. Bu yüzden siren ve afet uyarı sesleri varsayılan olarak KISIK olsun (varsayılan ses seviyesi yaklaşık %25-30). Sirenler kendi ses kategorisinde olsun, oyunun ses ayarlarından ayrıca ayarlanabilsin. Ayrıca config'te "siren ses seviyesi" (0-100) seçeneği olsun. Siren çok tiz/sert olmasın, sakin ama fark edilir bir ses olsun. Afet ortam sesleri (kasırga, deprem, tsunami uğultusu) da makul seviyede kalsın ve konuşmayı bastırmasın.
- EKRANDA UYARI YAZISI OLMAYACAK: Afet spawn olurken, siren sırasında veya afet boyunca ekranda hiçbir uyarı yazısı, başlık, banner veya bildirim kutusu çıkmasın ("TORNADO WARNING", "TSUNAMI WARNING", seviye yazısı vb. HİÇ OLMAYACAK). Uyarı yalnızca sesle (siren) verilsin. Bunun için bir HUD/overlay sınıfı da yazma.
- Afet asıl etkisine geçtiği anda (siren süresi bittiğinde / afet aşamasına geçildiğinde; güneş patlaması ve ay çarpmasında meteor/çarpma başladığında) siren yumuşak bir fade-out ile dursun.
- sounds.json'daki altyazı (subtitle) anahtarları yalnızca vanilla Altyazı ayarı açık olan oyuncularda vanilla altyazısı olarak görünsün (örn. "Siren çalıyor"); bu ayrı bir uyarı yazısı değildir.
- Afetin türünü, yönünü ve seviyesini önceden görmek isteyen oyuncu Hava Radarı / Sismograf (madde 13) kullanır.

### 6) ORTAK YANGIN SİSTEMİ
- Orman yangını, güneş patlaması meteorları ve ay çarpması meteorları AYNI yangın yayılma sistemini kullansın (tek bir ortak, optimize yangın yöneticisi yaz, kodu tekrarlama).
- Bu sistem batch/kuyruk mantığıyla çalışsın, TPS düşünce otomatik yavaşlasın, aktif yangın sayısı ve yayılma hızı config'den sınırlandırılabilsin.

### 7) SICAKLIK SİSTEMİ (Cold Sweat tarzı, gerçekçi ısı alışverişi)
Bu sistem modun temel parçası. Güneş patlamasındaki donma ve yanma da bu sistemle çalışsın.

a) Ortam sıcaklığı ve vücut sıcaklığı AYRI değerler olsun:
- ORTAM SICAKLIĞI: oyuncunun bulunduğu yerin sıcaklığı. Şunlara göre hesaplansın: biyom sıcaklığı, yükseklik (dağlar soğuk, derin mağara/yeraltı dengeli), gün/gece döngüsü (gece daha soğuk, çölde gündüz çok sıcak gece çok soğuk), yağmur/kar/fırtına, su içinde olma (soğuk su vücudu hızlı soğutur), gölge/güneş altı, kapalı alan (iç mekan dışarıya göre daha dengeli), yakındaki ısı kaynakları (lav, ateş, kamp ateşi, fırın, meşale, magma bloğu; sıcak) ve soğuk kaynakları (buz, kar, paketlenmiş buz; soğuk), Nether (çok sıcak), End (soğuk), güneş patlaması sırasında artan sıcaklık, süpernova sonrası çok düşen sıcaklık (madde 9'daki dünya donma ilerlemesine bağlı olarak giderek düşsün), kızıl cüce döneminde kısmen toparlanan ama eski dünyadan belirgin şekilde soğuk kalan sıcaklık (kalıcı kış bölgelerinde çok soğuk; bkz. bölüm D ve madde 9).
- VÜCUT SICAKLIĞI: oyuncunun kendi iç sıcaklığı. Normal değeri dengeli bir orta noktada olsun.

b) ISI ALIŞVERİŞİ: Vücut sıcaklığı ortam sıcaklığıyla gerçekteki gibi ısı alışverişi yapsın. Vücut sıcaklığı ortama doğru YAVAŞ YAVAŞ yaklaşsın (anında eşitlenmesin, ortam ile vücut arasındaki farkı oran alan bir formül kullan; fark büyükse değişim hızlı, fark küçükse yavaş olsun). Yalıtım (zırh, yün, tüy upgrade) bu ısı alışverişini yavaşlatsın. Hareket etmek, koşmak vücudu hafif ısıtsın; durmak ve ıslak olmak daha çabuk soğutsun. Yemek yemek (sıcak/soğuk yiyecekler, bkz. madde 14) vücut sıcaklığını değiştirebilsin.

c) ETKİLER:
- Vücut sıcaklığı çok düşerse: kademeli donma (önce yavaşlama, ekranda buz/donma efekti, sonra donma hasarı).
- Vücut sıcaklığı çok yükselirse: kademeli aşırı ısınma (önce ekranda sıcak titreme efekti, sonra yanma hasarı / ateşte yanma efekti). Sıcak ortamda aşırı ısınan oyuncu yanmaya başlasın.
- Hasarlar ani değil, kademeli ve okunaklı olsun; oyuncu tepki verebilsin.

d) SICAKLIK BARI (HUD): Ekranda, en alttaki XP barının hemen ÜSTÜNDE, Cold Sweat modundaki gibi küçük, şık bir sıcaklık/soğukluk göstergesi olsun. Soğuktan sıcağa doğru renk geçişli (mavi → nötr → kırmızı) bir bar olsun, üzerinde oyuncunun vücut sıcaklığını gösteren bir işaretçi bulunsun. İsteğe bağlı olarak ortam sıcaklığını gösteren ikinci küçük bir işaretçi/ok olsun ki oyuncu ısınıyor mu soğuyor mu anlasın. Bar, vücut sıcaklığına göre gerçekten canlı olarak değişsin; aşırı soğukta/sıcakta bar titreşsin veya uyarı renginde yanıp sönsün. HUD pozisyonu ve görünürlüğü config'den ayarlanabilsin. HUD, sunucudan gelen sıcaklık verisiyle çalışsın (düşük sıklıkta paket).

e) ZIRH YALITIMI (TÜY UPGRADE'İ VE YÜN). Eski "tüylü zırh seti" fikri KALDIRILDI, yerine aşağıdaki iki sistem var:
- TÜY UPGRADE'İ (etkileşim ile): Oyuncu SOL eline herhangi bir zırh parçası (vanilla veya moddan gelen, kask/göğüslük/pantolon/bot), SAĞ eline tavuktan düşen tüyü alıp tüye SAĞ TIK yaptığında, sol eldeki zırh parçasına bir "Tüy Upgrade'i" eklensin (tüy 1 adet harcansın). Bu upgrade zırhın NBT/veri bileşeninde saklansın ve eşya açıklamasında (tooltip) görünsün (örn. "Tüy Upgrade'i: 3"). Oyuncu bir zırh parçasına bu upgrade'i İSTEDİĞİ KADAR uygulayabilsin (üst sınır olarak çok yüksek bir config değeri olsun, örneğin 20 veya 30, ama pratikte sınırsız hissettirsin). Her upgrade, o zırhı giyen oyuncunun vücut sıcaklığını artırsın / ısı kaybını azaltsın (yalıtım). Etkisi:
  * Soğukta: upgrade sayısı arttıkça vücut daha yavaş soğur, yani donma süresi uzar. Çok sayıda upgrade ile soğukta çok uzun süre dayanılır.
  * Ama belli bir seviyeden sonra vücut sıcaklığı yükseleceği için: ortam soğuk DEĞİLSE (ılık/sıcak ortam), çok upgrade'li zırh giyen oyuncu aşırı ısınır ve YANMAYA başlar. Soğuk ortamda ise bu ısınma donmayı yavaşlatan iyi bir şey olarak çalışır. Yani gerçek hayattaki gibi: kalın giysi soğukta kurtarır, sıcakta pişirir.
  * Her zırh parçasının upgrade'i ayrı ayrı sayılsın ve toplam yalıtım dört parçanın toplamı olsun.
- YÜN İLE CRAFTING: Crafting tezgahında, istediğimiz bir zırh parçası + koyundan düşen yün birleştirildiğinde, o zırh parçası "Yünlü" hale gelsin (veri bileşeniyle). Her yünlü zırh parçası donma süresini %25 azaltsın: 1 parça = %25, 2 parça = %50, 3 parça = %75, 4 parça = %100. 4 parça da yünlü ise normal soğuk koşullarda karakter HİÇ DONMASIN (donma hızı %100 azalsın). Her zırh parçasına yün yalnızca bir kez uygulanabilsin. Zırhın kendi özellikleri (dayanıklılık, enchant) crafting sonrası korunsun.
- ÇOK AŞIRI SOĞUK İSTİSNASI: Ortam sıcaklığı olağanüstü düşükse (örneğin süpernova sonrası dondurucu soğuk, kızıl cüce dönemindeki kalıcı kış bölgelerinin en soğuk noktaları, çok yüksek dağ zirvesi veya donmuş dünyanın en soğuk noktaları), 4 parça yünlü zırh ve yüksek tüy upgrade'i bile donmayı tamamen engellemesin; ama donma ÇOK YAVAŞ ilerlesin (yalıtım sayesinde yine de ciddi şekilde gecikmeli). Yani yalıtım korur ama aşırı soğuğa karşı sınırsız değil.
- Yün ve tüy etkileri toplanabilsin ama dengeli olsun (hiçbir kombinasyon oyunu tamamen bozmasın, ortam aşırı sıcaksa yüksek yalıtım dezavantaja dönüşsün).
- Tüm hesaplamalar her oyuncu için ayrı yapılsın.

### 8) ORTAK ÖZEL SU SİSTEMİ
- Tsunami dalgası, mağaraya dolan su ve ay çarpması sonrası yükselen deniz seviyesi AYNI özel su sistemini kullansın (tek bir ortak, optimize su yöneticisi/renderer yaz, kodu tekrarlama).
- Bu sistem gerçek su bloklarını DEĞİŞTİRMESİN; harita verisi bozulmasın. Özel render edilen su, oyuncu/mob etkileşimi (yüzme, sürükleme, boğulma, hasar) ve görsel olarak gerçek su gibi davransın.

### 9) ORTAK DÜNYA DONMA SİSTEMİ (süpernova sonrası kademeli buzul çağı ve kızıl cüce döneminde kısmi çözülme)
Güneş patlaması / süpernova sonrası dünyanın donması ve kızıl cüce döneminde buzulun kısmen çekilmesi bu tek sistemle yapılsın. Dünya ANİDEN donmasın veya erimesin, uzun bir sürece yayılarak yavaş yavaş değişsin.
- GLOBAL DONMA İLERLEMESİ: Sunucuda dünya için tek bir "donma ilerlemesi" değeri (0-100%) tutulsun. Süpernovadan sonra bu değer zamanla (config'den ayarlanabilir süre, varsayılan birkaç Minecraft günü) yavaş yavaş artsın. Kızıl cüce döneminde ise yavaşça bir taban değere kadar geri düşsün (bkz. ÇÖZÜLME). Bu değer dünya verisinde (SavedData) saklansın, sunucu kapanıp açılınca kaybolmasın ve oyunculara senkronize edilsin. Ortam sıcaklığı (madde 7) bu ilerlemeye bağlı olarak değişsin.
- KADEMELİ DONMA ADIMLARI: İlerleme arttıkça dünya aşamalı değişsin, örneğin: önce hava soğur ve hafif kar/ayaz efektleri başlar → çimenler ve yapraklar kırağılanır / üzerine ince kar katmanı gelir → sular önce kenarlardan buzlanmaya başlar → yağmur yerine kar yağar → göller, nehirler ve denizler yüzeyden başlayarak buza dönüşür → ilerleyen aşamalarda ağaçlar, zemin ve yapılar da karla/buzla kaplanır. Aşamalar ve eşik değerleri config'den ayarlanabilir olsun. Bloklar rastgele sırayla ve yavaş yavaş değişsin (yayılma hissi), bir anda tüm dünya değişmesin.
- SULARIN BUZA DÖNMESİ: Dünyadaki su blokları (deniz, okyanus, göl, nehir) git gide, yüzeyden başlayarak ve kıyılardan içeri doğru yavaş yavaş buza dönüşsün. (Not: Bu sistem tsunami gibi özel render su değil, gerçek blokları değiştirir; bu yüzden batch/kuyruk ile yavaş ve kontrollü yapılsın.) Buz kalınlığı ilerlemeyle artsın, tamamen donmuş yüzey oyuncunun üstünde yürüyebileceği şekilde olsun. Güneş patlaması sırasında buharlaşmış sular (bkz. bölüm D) ile çakışma olmasın: buharlaşma ve donma aşamaları sırayla çalışsın.
- YENİ YÜKLENEN CHUNK'LAR: Dünyanın yeni yüklenen/keşfedilen chunk'ları da o anki duruma uygun yüklensin. Chunk yükleme olayında (ChunkEvent.Load veya benzeri) o anki global donma ilerlemesine (ve kızıl cüce döneminde o chunk'ın kalıcı kış bölgesi olup olmamasına) göre o chunk'ın hedef donma seviyesi uygulansın. Oyuncunun uzaklaştığı ve sonra tekrar yüklenen eski chunk'lar da geride kalmasın, güncel seviyeye yetişsin: chunk'ın "donma seviyesi" bilgisi saklansın, yüklenince eksik kalan kısım hedefe doğru (donma veya çözülme yönünde) adım adım tamamlansın. Bunu bir seferde yapıp kasma yaratmasın; chunk başına iş küçük parçalara bölünsün.
- ÖNCELİK: Donma/çözülme işlemi önce oyuncuların yüklediği/yakın chunk'larda yapılsın (oyuncunun gördüğü yerde değişim görünür olsun), uzak chunk'lar daha düşük öncelikle işlensin. Sunucu TPS'i düşünce hız otomatik azalsın. Config'te "donma hızı", "çözülme hızı", "tick başına maksimum blok değişimi" ve "donma kalıcı mı / sonradan çözülsün mü" seçenekleri olsun.
- Donmuş blokların orijinal halini saklamak zorunda olmak performansı bozacaksa saklanmasın. Dönüşüm tablosu İKİ YÖNLÜ tanımlansın (örn. su ↔ buz, çimen ↔ kırağılı çimen, kar katmanı ekle ↔ kaldır); çözülme bu tablonun ters yönüyle yapılsın, böylece orijinal blok kaydı gerekmez. Tabloda yalnızca geri döndürülebilir dönüşümler olsun. Kızıl cüce dönemi dışındaki bağımsız çözülme opsiyonel olsun (config, varsayılan kapalı).
- Diğer modların blokları ve yapılarla uyumlu ol: sadece su/çimen/yaprak/toprak gibi güvenli bloklar için dönüşüm tablosu kullan, tanınmayan blokları bozma, tablo config veya tag ile genişletilebilsin.
- ÇÖZÜLME (KIZIL CÜCE DÖNEMİ): Kızıl cüce güneş doğduktan sonra (bölüm D) global donma ilerlemesi %100'den yavaş yavaş bir taban değere düşsün (varsayılan taban %40, süre varsayılan 10 Minecraft günü; ikisi de config). Dünya hiçbir zaman tamamen erimesin.
  * KALICI KIŞ BÖLGELERİ: Bazı bölgeler kalıcı olarak donmuş kalsın (%100 seviyede): soğuk/karlı biyomlar, belirli bir yüksekliğin üstü (varsayılan Y=120, config) ve dünya seed'ine bağlı, düşük frekanslı bir gürültü maskesiyle seçilen alanlar (varsayılan karanın yaklaşık %25'i, config). Seçim deterministik olsun: aynı dünyada her zaman aynı bölgeler kış kalsın, chunk bazında hesaplanıp ucuz olsun, sunucu yeniden başlayınca değişmesin.
  * Kış bölgesi dışında buzul önce ılık bölgelerden ve kıyılardan başlayarak çekilsin: ince kar katmanları ve kırağı önce kalksın, ardından su yüzeylerindeki buz kenarlardan içeri doğru çözülsün. Kış bölgelerinin sınırında geçiş yumuşak olsun (keskin çizgi olmasın).
  * Çözülme de batch/kuyruk, oyuncu yakınlık önceliği ve TPS kuralına uysun.

### 10) AFET ŞİDDET SEVİYELERİ (oyun ilerledikçe güçlenen afetler)
- Kasırga, Tsunami, Deprem, Asit Yağmuru, Sinkhole ve Orman Yangını 5 şiddet seviyesine sahip olsun: Seviye 1 (Hafif), 2 (Orta), 3 (Şiddetli), 4 (Yıkıcı), 5 (Felaket). Güneş Patlaması ve Ay Çarpması seviyesiz, tek ve sabit şiddette olsun.
- Seviye, afetin boyutunu, hızını, süresini, hasarını ve blok yıkım gücünü belirlesin. Her afet için seviyenin ne değiştirdiğini net tanımla ve config'den ayarlanabilir yap. Örnekler:
  * Kasırga (F1-F5 hissi): huni genişliği/yüksekliği, hareket hızı, yaşam süresi, çekim gücü, kaldırabildiği blok sayısı ve hasar artar.
  * Tsunami: dalga yüksekliği, hızı, karaya içeri ne kadar girdiği, sürükleme kuvveti ve yıkım gücü artar.
  * Deprem (Richter hissi 4.0 → 9.0): yarığın uzunluğu/genişliği/derinliği, sarsıntı süresi, ekran sarsıntısı şiddeti ve yıkılan blok alanı artar.
  * Asit yağmuru: yağış alanı, süresi, aşındırma hızı ve oyuncuya verilen hasar (hâlâ "ortalama" hasar sınırında kalsın, Seviye 5'te bile öldürücü olmasın) artar.
  * Sinkhole: çukurun çapı ve derinliği artar.
  * Orman yangını: başlangıç alanı, yayılma hızı ve süre artar (ortak yangın sistemi sınırları dahilinde).
- ŞİDDETİN OYUN İLERLEDİKÇE ARTMASI: Seviye, afet oluşurken ağırlıklı rastgele seçimle belirlensin. Oyunun başında ağırlık hafif seviyelerde olsun, oyun ilerledikçe (dünya yaşı = oyun içi gün sayısı; opsiyonel olarak madde 3'teki "oyuncu gelişimi" kriteri de hesaba katılsın) ağırlıklar yavaş yavaş yüksek seviyelere kaysın. Varsayılan eşikler (hepsi config'den ayarlanabilir): Seviye 1 → baştan, Seviye 2 → 10. gün, Seviye 3 → 25. gün, Seviye 4 → 50. gün, Seviye 5 → 80. gün. Açılan seviye hemen baskın olmasın; yüksek seviyeler her zaman daha nadir olsun. Oyuncu ilk günlerde Seviye 4-5 afetle karşılaşmasın.
- Oyunun gidişatını bozmaması için: Seviye 4-5 afetler, ardışık gelmesin (aynı seviye art arda en fazla bir kez), ve her şiddetli afetten sonra bekleme süresi biraz uzasın.
- Seviye ekranda yazı olarak GÖSTERİLMESİN (madde 5). Seviye yalnızca Gelişmiş Hava Radarı ve Hassas Sismograf'ta (madde 13) ve /dogalafet komut çıktılarında görünsün, başarımlarda (madde 12) kullanılsın.
- Test komutu: /dogalafet start <afetadı> [seviye] [oyuncu] ile seviye belirlenebilsin.

### 11) GAMERULE SİSTEMİ
Oyun içinde /gamerule komutuyla yönetilen özel kurallar ekle (GameRules.register ile, dünya verisinde kalıcı olsun). Varsayılanları belirt:
- dogalAfetlerAcik (boolean, true): tüm afet sistemini aç/kapat.
- kasirgaAfeti, tsunamiAfeti, depremAfeti, asitYagmuruAfeti, sinkholeAfeti, ormanYanginiAfeti, gunesPatlamasiAfeti, ayCarpmasiAfeti (hepsi boolean, true): afetleri tek tek aç/kapat. Kapalı afet hiç seçilmesin, ama kuyrukta olan/başlamış afet güvenli şekilde bitirilsin.
- afetSikligi (int, 100): afet sıklığı yüzdesi (100 = normal, 50 = yarı, 200 = iki katı).
- afetMaksSiddet (int, 5): oluşabilecek maksimum şiddet seviyesi (1-5).
- afetBlokHasari (boolean, true): kapalıysa afetler blok yıkmaz/yakmaz/dondurmaz/çözmez (oyuncu ve mob hasarı sürer, harita korunur).
- sicaklikSistemi (boolean, true): vücut sıcaklığı sistemini ve HUD'u aç/kapat.
- Güneş patlaması ve ay çarpması gamerule ile kapatılırsa devam eden süreçleri de güvenle durdurulabilsin (/dogalafet stop ile birlikte). Zaten ulaşılmış dünya dönemleri (aysız gökyüzü, donma ilerlemesi, kızıl cüce güneş) gamerule kapatılınca geri alınmasın; yalnızca henüz tamamlanmamış süreçler durdurulsun.

Bu kuralların hepsi /gamerule <ad> ile okunabilsin/değiştirilebilsin ve tab-tamamlama desteği olsun.

### 12) BAŞARIM SİSTEMİ (Advancement)
- Modun kendi başarım sekmesi olsun: "Doğal Afetler". Kök başarım: "Felaket Başlıyor" (ilk afet sirenini duyunca otomatik açılır). Başarımlar data pack içinde advancement JSON'larıyla tanımlansın, kendi özel kriter tetikleyicilerini (CriteriaTrigger) yaz, oyuncu bazında takip et ve çok oyunculuda her oyuncu kendi başarımını kazansın. Her başarımın Türkçe adı ve açıklaması lang dosyasında olsun. Başarımlar toast bildirimi + sohbet duyurusuyla görünsün (bunlar vanilla başarım bildirimleridir, afet uyarı yazısı sayılmaz). Altta "sağ çıkmak" şu demek: afetin etki alanındayken (afet yarıçapında, config'den ayarlanabilir) afet bitene kadar ölmemek.
- Başarım listesi (isim — tür — nasıl kazanılır):
  - Kök: "Felaket Başlıyor" (görev) — modu yükleyip ilk afet sirenini duymak.
  - Afetten Sağ Çıkma:
    1. "İlk Rüzgâr" (görev) — Bir kasırgadan sağ çık.
    2. "Dalga Avcısı" (görev) — Tsunami dalgası tarafından sürüklenip sağ çık.
    3. "Sallanan Zemin" (görev) — Bir depremden sağ çık.
    4. "Delikten Dönüş" (görev) — Sinkhole çökerken çukura düşüp sağ çık.
    5. "Yanmadan Çıktım" (görev) — Orman yangını alanının içinde en az 30 saniye kalıp sağ çık.
    6. "Çatı Altı" (görev) — Bir asit yağmuru boyunca hiç hasar almadan hayatta kal (çatı altında/korunakta).
    7. "Gökten Gelen Misafir" (görev) — İlk Meteorit Parçası'nı topla (madde 13).
    8. "Güneşi Atlattım" (hedef) — Güneş patlamasının 5 Minecraft günlük sürecinden sağ çık.
    9. "Süpernova Tanığı" (hedef) — Süpernovayı gökyüzünde gör.
    10. "Buzul Çağı" (hedef) — Dünya donma ilerlemesi %50'yi geçsin ve sen hayatta ol.
    11. "Kutup Sığınağı" (meydan okuma) — Donma ilerlemesi %100'e ulaştıktan sonra 3 Minecraft günü hayatta kal.
    12. "Ay Düştü" (hedef) — Ay çarpmasından sağ çık.
    13. "Yıldızlı Gece" (görev) — Ay çarpmasından sonra ilk yıldızlı/galaksili geceyi izle.
    14. "Yükselen Sular" (görev) — Deniz seviyesinin yükselmesini yaşa ve sağ kal.
  - Hazırlık ve Eşyalar:
    15. "Yer Dinleyici" (görev) — İlk Sismograf'ı craft et.
    16. "Radar Altında" (görev) — İlk Hava Radarı'nı craft et.
    17. "Katmanlı Giyim" (görev) — Bir zırh parçasına 5 tüy upgrade'i uygula.
    18. "Yünlü Kahraman" (hedef) — 4 parça yünlü zırhı aynı anda giy.
    19. "Çorba İçer Isınır" (görev) — Donma durumundayken sıcak bir yiyecek yiyerek vücut sıcaklığını normale çıkar.
    20. "Buz Balıkçısı" (görev) — Buz deliğinden ilk balığı tut (madde 15).
    21. "Kutup Ustası" (hedef) — Buz deliğinden toplam 25 balık tut.
    22. "Kozmik Şans" (meydan okuma) — İlk Kozmik Çekirdek'i bul (madde 13).
  - Şiddet ve Zorluk:
    23. "Fırtınaya Meydan Oku" (hedef) — Seviye 3 veya üzeri bir afetten sağ çık.
    24. "Felaket Ustası" (meydan okuma) — Seviye 5 (Felaket) bir afetten sağ çık.
    25. "Hepsini Atlattım" (meydan okuma, gizli) — Kasırga, tsunami, deprem, asit yağmuru, sinkhole, orman yangını, güneş patlaması ve ay çarpmasının hepsinden en az bir kez sağ çıkmış ol.
    26. "Birlikte Hayatta" (hedef, gizli) — En az 3 oyuncu aynı afeti aynı anda atlatsın (aynı afetin etki alanında olup hepsi sağ çıksın).
- Ödüller: Başarımlar XP versin; "hedef" başarımlar ayrıca küçük bir ödül (örneğin 1-2 sıcak yiyecek), "meydan okuma" başarımları daha büyük XP ve bir kozmetik ünvan/chat duyurusu versin. Ödüller oyunu bozmayacak kadar küçük kalsın.

### 13) ÖZEL EŞYALAR
Tüm eşyaların tarifi data-driven (recipe JSON) olsun, yeni dokular için doku isimlerini ve boyutlarını bana söyle (veya basit prosedürel alternatif öner).

a) HAVA RADARI (Afet Radarı)
- Tarif (3x3, şekilli): üst sıra: Demir Külçesi, Cam, Demir Külçesi; orta sıra: Kızıltaş Tozu, Pusula, Kızıltaş Tozu; alt sıra: Demir Külçesi, Bakır Külçesi, Demir Külçesi.
- Elde (ana veya yan el) tutulduğunda çalışsın. Ekranda küçük, ekranı kaplamayan bir gösterge çıksın: yaklaşan veya aktif afetin adı, yönü (ok), tahmini uzaklığı (blok) ve tahmini varış/başlama süresi. Menzil varsayılan 300 blok (config). Bu gösterge yalnızca radar elde tutulurken görünür; afet uyarı yazısı değildir.
- Madde 4'teki ÖN-UYARI PENCERESİ boyunca (siren çalmadan önce) radar elde tutuluyorsa, yaklaşan afeti önceden gösterebilsin: oyuncu siren gelmeden hazırlık yapabilsin. Güneş patlaması ve ay çarpması için "gökyüzü anomalisi" olarak gösterilsin (ön-uyarı penceresinde değil, sirenden sonra da geri sayım göstersin).
- Normal radar afet seviyesini göstermesin; sadece afet türü, yön, mesafe ve süre göstersin.
- GELİŞMİŞ HAVA RADARI: Hava Radarı + 1 Kozmik Çekirdek (şekilsiz tarif). Menzil 800 blok, ayrıca afet şiddet seviyesini, hareket yönünü (kasırga/tsunami gibi hareketli afetler için) ve son 5 afetin kısa geçmişini gösterir.
- Çok oyunculuda her oyuncu kendi radarını kullanır; sunucudan sadece ilgili oyuncuya düşük sıklıkta paket gitsin.

b) SİSMOGRAF
- Tarif (3x3, şekilli): üst sıra: Demir Külçesi, Kağıt, Demir Külçesi; orta sıra: Çubuk, Kızıltaş Tozu, Çubuk; alt sıra: Taş, Bakır Külçesi, Taş.
- Envanterde (hotbar veya yan el) taşınırken çalışsın. Deprem ve sinkhole için ön-uyarı penceresi boyunca (siren çalmadan önce) "öncü sarsıntı" algılasın: hafif bir zemin titremesi sesi/efekti (çok hafif ekran titremesi) ve yalnızca sismografı taşıyan oyuncunun action bar'ında "Öncü sarsıntı algılandı" + tahmini şiddet (%) + merkez yönü/mesafesi göstersin.
- Sağ tık ile açılan küçük bir ekranda (opsiyonel, performansı etkilemesin) son 60 saniyenin sismik dalga grafiği çizilsin (tüy kalem animasyonu hissi).
- Gelişmiş: Sismograf + 1 Kozmik Çekirdek → "Hassas Sismograf": ön-uyarı süresi daha erken başlar (daha uzun uyarı), merkez konumunu daha doğru gösterir, depremin seviyesini de belirtir.
- Eşya olmayan oyuncu için hiçbir şey değişmez; sadece normal siren gelir.

c) METEOR ÖDÜLLERİ (SADECE METEORLARDAN)
- Bu ödüller YALNIZCA güneş patlaması ve ay çarpması meteorlarından çıksın (başka afetlerden, balıktan, sandıktan, mob'dan çıkmasın).
- METEORİT PARÇASI: Her meteor çarptığında, çarpma kraterinde nadir bir şansla (varsayılan %3, config'den ayarlanabilir; büyük meteorlarda %5) bir "Meteorit Parçası" item entity'si oluşsun. Parıldayan/ışık saçan bir eşya olsun (hafif parçacık efekti), 10 dakika sonra yok olsun. Çarpmanın hemen ardından ilk 20 saniye "sıcak" olsun: sıcakken yanına gelen/üzerinden alan oyuncuya kısa süre yanma etkisi uygulansın (su veya kar üstünde hemen soğur). Soğuyunca güvenle alınabilsin.
- KOZMİK ÇEKİRDEK: Çok daha nadir (varsayılan %0,3, sadece büyük meteorlarda ve yalnızca bir çarpma kraterinin ortasında) bir "Kozmik Çekirdek" çıkabilsin. Mor-mavi parıltılı, enchant parlaması olan bir eşya.
- METEORİT KÜLÇESİ: 3 Meteorit Parçası + 1 Demir Külçesi (şekilsiz) → 1 Meteorit Külçesi.
- TERMOSTAT PLAKASI: Meteorit Külçesi + Altın Külçesi + Kızıltaş Tozu + Demir Külçesi (şekilsiz) → 1 Termostat Plakası.
- TERMOSTATLI ZIRH: Crafting'de bir zırh parçası + Termostat Plakası → zırh "Termostatlı" olur (veri bileşeniyle, her parçaya yalnızca bir kez). Her Termostatlı parça, vücut sıcaklığını nötr orta noktaya doğru küçük bir oranda çeker: soğukta hafifçe ısıtır, sıcakta hafifçe soğutur (4 parça birlikte güçlü ama hâlâ sınırlı; süpernova sonrası aşırı soğuk ve güneş patlaması aşırı sıcağında etkisi azalarak kalsın, hiçbir koşulda tam koruma vermesin). Zırhın diğer özellikleri ve tüy/yün upgrade'leri korunsun, hepsi birlikte dengeli çalışsın.
- Kozmik Çekirdek kullanımı: Gelişmiş Hava Radarı ve Hassas Sismograf (yukarıda), yeni başka bir kullanım gerekmez.
- Düşme şansları, süreler ve etkiler config'den ayarlanabilsin. Çok oyunculuda meteor ödülleri yere düşen eşyadır (adil kapış), oyunu bozacak kadar bol çıkmasın.

### 14) SICAKLIK YİYECEKLERİ (sıcak/soğuk yemek ve içecekler)
Vücut sıcaklığını (madde 7) kontrollü şekilde değiştiren yiyecekler. Her yiyecek yenince hem normal yemek değeri verir hem de bir "Sıcaklık Etkisi" durum efekti (status effect) uygular. Efekt anında bir miktar etki + belirli bir süre boyunca yavaş yavaş devam eden ısınma/serinleme yapsın (örneğin ısınma etkisi 60-180 sn sürsün, giderek azalsın). Etkiler vücut sıcaklığı ölçeğinin yüzdesi olarak tanımlansın (küçük %5-10, orta %15-25, büyük %30-40) ve config'den ayarlanabilsin. Efektin kendi küçük ikonu ve "İçten Isınma" / "Serinlik" adları olsun (dil dosyası).

Kurallar:
- Azalan verim: Aynı türden yiyecek 60 saniye içinde tekrar yenirse etkisi %50 azalsın (spam engeli). Farklı yiyecekler kendi başına çalışsın.
- Sıcak yiyecek, ortam zaten sıcakken/oyuncu aşırı ısınmışken ısınmayı artırır (aşırı ısınmaya itebilir). Soğuk içecek, ortam zaten çok soğukken/donarken soğutmayı artırır. Yani zamanında yenmesi gerekir.
- Yiyecek yeme süresi vanilla gibi (32 tick), stack boyutu 16 olsun. Container olan yemeklerde (şişe/kase) boş container geri verilsin.
- Vanilla pişmiş yiyecekler (kızarmış patates vb.) çok küçük ve kısa bir ısınma verebilsin (opsiyonel, config'den kapatılabilir).

Sıcak yiyecekler ve tarifleri:
- Sıcak Salep (orta ısınma, 90 sn): 3 Cam Şişe + Süt Kovası + Bal Şişesi (şekilsiz) → 3 Sıcak Salep (kova geri verilir).
- Sıcak Çikolata (orta ısınma, 90 sn): 3 Cam Şişe + Süt Kovası + 2 Kakao Çekirdeği + Şeker (şekilsiz) → 3 Sıcak Çikolata (kova geri verilir).
- Sıcak Tavuk Çorbası (büyük ısınma, 150 sn): Kase + Pişmiş Tavuk + Havuç + Patates (şekilsiz) → Sıcak Tavuk Çorbası (kase geri verilir). Yüksek doyuruculuk.
- Ateşli Güveç (çok büyük ısınma, 180 sn, yan etki): Kase + Pişmiş Et (sığır veya domuz) + Patates + Alev Tozu (şekilsiz) → Ateşli Güveç. Çok güçlü ısıtır ama sıcak ortamda aşırı ısınmaya kolay iter; en güçlü soğuk savar.
- Kuru Meyveli Çay: Cam Şişe + Tatlı Çilek x2 + Bal Şişesi (şekilsiz) → 2 Isıtıcı Çay (hafif ısınma, uzun süre, 180 sn).

Soğutucu yiyecekler ve tarifleri:
- Ayran (orta serinleme, 90 sn): 3 Cam Şişe + Süt Kovası + 2 Kar Topu (şekilsiz) → 3 Ayran (kova geri verilir).
- Buzlu Karpuz (hafif serinleme, 60 sn): Karpuz Dilimi + Kar Topu (şekilsiz) → Buzlu Karpuz Dilimi.
- Meyve Şerbeti (büyük serinleme, 120 sn): Cam Şişe + Tatlı Çilek x3 + Şeker + Kar Topu (şekilsiz) → Meyve Şerbeti. Sıcak çölde ve güneş patlamasında hayat kurtarır.

Buz balığı yemekleri (madde 15 ile bağlantılı):
- Kutup Balığı Çorbası (büyük ısınma, 150 sn): Kase + Pişmiş Kutup Balığı (madde 15'teki balıklardan biri) + Patates (şekilsiz).
- Pişmiş buz balıkları (madde 15) kendi başına orta ısınma versin; çiğ olarak yenirse hafif soğutsun.

### 15) BUZ ALTI BALIKÇILIK (süpernova sonrası)
Süpernovadan sonra dünya donunca (madde 9), oyuncular için yeni bir yiyecek ve ısınma kaynağı olarak buz deliğinden balık tutma sistemi:
- Sistem, donma ilerlemesi belirli bir eşiği (varsayılan %30, config) geçtiğinde ve suyun üstü buza dönüştüğünde aktif olsun. Kızıl cüce döneminde global ilerleme düşse bile kalıcı kış bölgelerinde ve hâlâ donmuş su yüzeylerinde çalışmaya devam etsin.
- BUZ DELİĞİ AÇMA: Oyuncu bir kazma veya balta ile buz bloğuna (suyun üstündeki buza) eğilerek (shift) sağ tık yaptığında, buz bloğu "Buz Deliği" bloğuna dönüşsün (alet 5 dayanıklılık kaybetsin). Delikten alttaki suyun görünür olduğu bir model/doku olsun. Delik belirli bir süre sonra (varsayılan 4-5 dakika, config) yavaşça yeniden donarak eski buz haline dönsün. Her chunk'ta aynı anda açık olabilecek delik sayısı sınırlı olsun (performans). Buz deliği sadece su üstündeki buzda açılsın.
- BALIK TUTMA: Olta, Buz Deliği'ne atılırsa çalışsın (normal buza atılırsa tutmasın). Balık ısırma süresi normalden biraz uzun olsun (30-60 sn, Lure büyüsü hızlandırsın), karşılığında kayıp/çöp şansı düşük ve ödül iyi olsun. Vanilla balıkçılık sistemini bozma: Global Loot Modifier / ayrı loot table kullan, sadece Buz Deliği ile yapılan balık tutmada geçerli olsun.
- LOOT (balıklar, hepsinin dokusu ve adı Türkçe): Buz Alabalığı (yaygın), Kutup Morinası (yaygın), Derin Soğuk Somonu (orta), Buz Kefali (orta), Donmuş Ringa (nadir, daha çok ısınma verir). Çöp: Buz Parçası, Kar Topu. Hazine (çok nadir): büyülü kitap (örneğin Frost Walker / Lure / Luck of the Sea). Not: meteor ödülleri buradan ÇIKMASIN.
- YİYECEK ÖZELLİKLERİ: Çiğ balıklar hafif soğutur (ama karnı doyurur), pişmiş balıklar (ocak/fırın/ateş) orta ısınma verir ve kendi başlarına değerli yiyecektir; Kutup Balığı Çorbası (madde 14) en güçlü ısınma kaynaklarından biridir.
- RİSK/DENGE: Buz üstünde hareketsiz durarak balık tutmak vücudu daha hızlı soğutsun (hareketsizlik + soğuk). Oyuncu yanında ateş/sıcak yiyecek bulundurmak veya kalın giyinmek zorunda kalsın. Böylece balıkçılık risk-ödül dengesi taşısın. Balıkçılık oyunu bozacak kadar bol yiyecek vermesin ama donmuş dünyada hayatta kalmayı mümkün kılsın.
- Başarımlar (madde 12) ile bağlantılı çalışsın: "Buz Balıkçısı", "Kutup Ustası".

### 16) ORTAK GÖKYÜZÜ SİSTEMİ
- Güneş büyümesi, süpernova, güneşsiz yıldızlı/nebulalı gökyüzü, ay çarpması sonrası aysız yıldızlı gece ve kızıl cüce güneş AYNI gökyüzü renderer'ını ve aynı sunucu tarafı "gökyüzü durumu" verisini kullansın (kodu tekrarlama).
- Gökyüzü durumu dünya verisinde (SavedData) saklansın, durum değişince tüm oyunculara paketle gitsin, sonradan giren oyuncuya doğru senkronize edilsin.
- Gökyüzü değişiklikleri yalnızca görsel olsun: vanilla ışık motoruna, gün/gece döngüsüne ve mob spawn kurallarına dokunma (diğer modlarla uyumluluk için).
- Ay durumu (var/yok) ve güneş durumu (normal / büyüyen / süpernova / güneşsiz / kızıl cüce) birbirinden bağımsız tutulsun ve her kombinasyonda doğru görünsün.

## DOĞAL AFETLER

### A) KASIRGA (Tornado) [şiddet seviyeli, madde 10]
- Rastgele zamanda, rastgele yerde, siren ve ses eşliğinde haritada kasırga oluşsun.
- Gerçek kasırga gibi görünsün: dönen huni şekli, toz/enkaz bulutu, yerden kalkan bloklar ve parçalar. Görsel olarak etkileyici ve detaylı olsun.
- Oyunculara, hayvanlara/mob'lara ve haritaya (bloklar, ağaçlar) zarar versin, nesneleri havaya çeksin.
- Kasırga hareket etsin ve bir süre sonra dağılsın. Oyunun gidişatını bozmasın (sık spawn olmasın, aynı anda çok sayıda olmasın).

### B) TSUNAMİ [şiddet seviyeli, madde 10]
- Tsunami, Minecraft'ın su blokları yerine kendi yazdığın özel render edilen su ile yapılsın (madde 8'deki ortak özel su sistemi). Bu su haritaya taşmasın, harita verisini bozmasın, ama gerçek su gibi görünsün: animasyonlu, şeffaf, dalga hareketi, köpük ve sıçrama efektleri ile. Görsel kalite öncelikli.
- Tsunami olmadan önce yakındaki kıyılardaki ve denizdeki sular geri çekilsin (görsel olarak), sonra toplanıp dev bir dalga oluşturup karaya doğru ilerlesin. Gerçek tsunami gibi olsun.
- SÜRÜKLEME: Tsunami dalgası, kendisine çarpan oyuncuları, mob'ları ve düşen eşyaları dalganın ilerleme yönünde güçlü şekilde SÜRÜKLESİN (itme/velocity uygulansın). Oyuncu dalganın içine girince kontrolü kısmen kaybetsin, dalganın akışıyla sürüklensin; dalga yüksekliği ve hızına göre sürükleme kuvveti değişsin. Sürüklenme sırasında engellere (ağaç, duvar, bina) çarparsa ekstra hasar alsın. Oyuncunun yüzerek veya yüksek bir yere çıkarak kaçabilme şansı olsun (yüksek tepe, yüksek bina, kalın duvar/sağlam yapı sürüklemeyi azaltsın). Sürükleme çok oyunculu ortamda her oyuncu için ayrı hesaplansın ve sunucu tarafında yapılsın.
- Tsunaminin suyuna giren oyuncular ve mob'lar ayrıca hasar alsın.
- Tsunami haritadaki blokları, ağaçları ve evleri yıkabilsin (batch ile ama yıkım akıcı ve etkileyici görünsün).
- Tsunami sırasında özel yazdığın, kaliteli bir yağmur efekti yağsın (kendi yaptığın özel yağmur parçacığı/efekti).
- MAĞARALARA GİREN SU: Tsunaminin suyu mağaralara, tünellere ve yeraltı boşluklarına girerse orada kalsın (oyuncuyu da sürükleyip içeri taşıyabilsin). Su, mağarada belirli bir süre (config'den ayarlanabilir) kalıp doldurduğu boşlukta durmaya devam etsin, bu sürenin sonunda yavaş yavaş buharlaşıp/kuruyup yok olsun (su seviyesi kademeli düşsün, buharlaşma parçacıkları görünsün). Bu özel su sistemiyle yapılsın, harita verisi (gerçek su blokları) bozulmasın ve sonunda dünya tamamen eski haline dönsün. Mağaradaki su, oyuncular için boğulma/hasar tehlikesi yaratsın.
- Oyunun gidişatını bozmayacak şekilde, normalde sık olmayan bir şekilde oluşsun. Ancak ay çarpması gerçekleştikten sonra tsunami olasılığı geçici olarak ARTSIN (bkz. madde 4 olasılık çarpanı sistemi ve bölüm H).

### C) DEPREM [şiddet seviyeli, madde 10]
- Rastgele zamanda haritada deprem olsun.
- Klasik olarak haritayı ikiye bölen, gerçek depremdeki gibi bir yarık/fay hattı oluşsun. Yarık gerçekçi, derin ve etkileyici görünsün (batch ile ama ilerleyişi akıcı olsun).
- Etraftaki ağaç yaprakları, tahta evler gibi "sağlam olmayan" bloklar yıkılıp yere düşsün. Sağlam bloklar (taş, deepslate, obsidyen vb.) daha az etkilensin.
- Deprem sırasında oyuncuların ekranı titresin ve deprem sesi duyulsun (depremin yakınındaki oyuncularda daha şiddetli, uzaktakilerde daha hafif).
- Sismograf (madde 13b) ile ön-uyarı penceresinde öncü sarsıntı algılanabilsin.

### D) GÜNEŞ PATLAMASI (en nadir; en etkileyici ve en çok optimize edilmesi gereken afet) [seviyesiz]
- Sadece oyunun ilerleyen zamanlarında olsun: oyuncular yeterince geliştiğinde (örneğin oyun içi gün sayısı, ilerleme/başarım veya eşya seviyesi gibi bir kriter; kriteri config'den ayarlanabilir yap).
- Gerçek hayattaki gibi uzun bir uyarı sireni çalsın (kısık ses kuralına uysun); bu, güneş patlamasının habercisi olsun.
- Siren başladıktan sonra gökyüzündeki güneş yavaş yavaş büyüsün.
- Siren başladıktan 60 saniye sonra dünyaya meteorlar düşmeye başlasın (siren bu anda fade-out ile dursun). Oyuncular haritanın en alt katmanlarına inip taş, deepslate veya obsidyen gibi güçlü bloklarla saklanmak zorunda kalsın.
- Düşen her meteor, çarptığı yerde ateş çıkarsın ve bu ateşler orman yangınındaki gibi etrafa tutuşup yayılsın (çimen, yaprak, ağaç, odun gibi yanıcı her şeyi yaksın, sağlam bloklar yanmasın). Bunun için madde 6'daki ortak yangın sistemini kullan. Meteorlar nadir şansla Meteorit Parçası / Kozmik Çekirdek bırakabilsin (madde 13c).
- Meteorlar her oyuncunun çevresinde dengeli şekilde düşsün (tek oyuncuya yığılma olmasın, herkes risk altında olsun).
- Güneş patlamasından sonra ortam sıcaklığı yükselsin (madde 7); oyuncu gölgede/korunaklı yerde durmazsa vücut sıcaklığı artıp yanmaya başlasın, etraf git gide yanmaya başlasın ve meteorlar düşsün.
- Bu süreç 5 Minecraft günü boyunca devam etsin.
- 5 Minecraft günü sonunda güneş patlasın ve gökyüzünde güneşin süpernovası görünsün. Süpernova 1-2 Minecraft günü boyunca gözüksün.
- GÜNEŞSİZ DÖNEM: Süpernova parıltısı yavaşça sönerek bir süpernova kalıntısı nebulasına dönüşsün. Bundan sonra gökyüzünde güneş olmasın; gündüz de gece de gökyüzü karanlık, çok sayıda yıldız, galaksi ve nebulayla dolu olsun (madde 16, ay çarpmasındaki yıldızlı gökyüzüyle aynı renderer).
- Süpernovadan sonra madde 9'daki ortak dünya donma sistemi başlasın: dünya anında değil, YAVAŞ YAVAŞ donsun. Sular git gide buza dönsün, çevre kırağılansın/karlansın, yeni yüklenen chunk'lar da o anki donma seviyesine uygun donmuş olarak yüklensin. Ortam sıcaklığı donma ilerlemesiyle birlikte giderek çok düşsün ve oyuncular madde 7'deki sıcaklık sistemiyle yavaş yavaş donsun. Hayatta kalmak için yünlü zırh, tüy upgrade'i (madde 7e), Termostatlı zırh (madde 13c), sıcak yiyecekler (madde 14) ve buz altı balıkçılık (madde 15) kullanılsın; 4 parça yünlü zırh + upgrade'lerle donma çok yavaşlasın ama süpernova sonrası aşırı soğuk istisnasına göre tamamen yok olmasın.
- Güneş patlaması sırasında (süpernovadan ÖNCE) dünyadaki sular buharlaşsın. Süpernovadan sonra donma başladığında su yeniden oluşmuş/kalmış alanlar donma sistemine dahil olsun.
- KIZIL CÜCE GÜNEŞ (yeni dönem; afet değil, siren yok):
  * Donma ilerlemesi %100'e ulaştıktan belirli bir süre sonra (varsayılan 7 Minecraft günü, config; "Kutup Sığınağı" başarımının 3 günlük şartından kısa olamaz) yeni bir güneş doğsun.
  * DOĞUŞ: İlk şafakta, yıldızlı ve nebulalı gökyüzünün arasından ufukta soluk bir kızıl ışık belirsin ve yeni güneş yavaşça yükselsin. Yıldızlar ve nebula, kızıl cüce gökyüzündeyken bile gündüz soluk şekilde görünmeye devam etsin. Doğuş tüm oyunculara aynı anda senkronize olsun.
  * GÖRÜNÜM: Eski güneşten daha küçük (varsayılan eski boyutun %60'ı, config), soluk turuncu-kırmızı bir güneş olsun. Gündüz gökyüzü ve sis rengi hafif kızıl/turuncu tonlu, eski dünyadan daha loş görünsün (yalnızca görsel; ışık motoru değişmesin, madde 16).
  * ISI: Eskisinden zayıf ısıtsın. Ortam sıcaklığı kademeli olarak toparlansın ama eski dünyanın normal değerlerine ulaşmasın (config'li sıcaklık ofseti, varsayılan belirgin şekilde serin). Kalıcı kış bölgeleri çok soğuk kalsın.
  * ÇÖZÜLME: Dünya tamamen erimesin; buzul yavaş yavaş çekilsin (madde 9 ÇÖZÜLME). Kalıcı "kış" bölgeleri donmuş kalsın.
  * OYUN DEVAM EDER: Oyun bitmesin; yeni bir dönem başlasın. Diğer afetler (kasırga, tsunami, deprem, asit yağmuru, sinkhole, orman yangını, ayı hâlâ varsa ay çarpması) normal şekilde devam etsin. Güneş patlaması bu dönemde tekrar seçilmesin (madde 4).
  * Kızıl cüce dönemi dünya verisinde saklansın, sunucu yeniden başlayınca kaybolmasın ve sonradan giren oyuncuda doğru görünsün.

### E) ASİT YAĞMURU [şiddet seviyeli, madde 10]
- Rastgele zamanda haritada asit yağmuru yağsın. Görsel olarak ayırt edilebilir olsun (yeşilimsi/sarımsı yağmur efekti, bloklara düşünce dumanlanma).
- Sert bloklar hariç, asidin düştüğü yerdeki blokları yavaş yavaş kırsın/aşındırsın.
- Oyuncuya değerse hasar versin ama çok fazla değil, orta düzeyde hasar olsun. Çatı altında/korunaklı yerde olan oyuncuya zarar vermesin.

### F) SİNKHOLE (Çöken Çukur) [şiddet seviyeli, madde 10]
- Rastgele bir yerde, siren ve ses eşliğinde haritada büyük bir delik/çukur oluşsun.
- Gerçek sinkhole'lara benzesin (internetten fikir al): yer çökmeden önce çatlama/titreme, sonra dairesel çökme, bloklar çukura dolsun.
- Batch sistemiyle uygula ama çöküş akıcı ve etkileyici görünsün. Sismograf (madde 13b) ile ön-uyarı penceresinde öncü sarsıntı algılanabilsin.

### G) ORMAN YANGINI [şiddet seviyeli, madde 10]
- Rastgele zamanda başlasın ve alev tutuşa tutuşa etrafı yaksın.
- Sadece ağaçlarla sınırlı olmasın: çimen, çiçek, yaprak, odun gibi yanıcı olan her şey yanabilsin. Sağlam (yanmaz) bloklar yanmasın.
- Yangın yayılma hızı kontrollü olsun, oyunun gidişatını bozmasın. Yağmur yağınca sönebilsin. Alev ve duman efektleri gerçekçi ve yoğun görünsün. Yangına yakın bölgelerde ortam sıcaklığı yükselsin (madde 7).
- Bu afetin yangın mantığı madde 6'daki ortak yangın sistemidir; güneş patlaması ve ay çarpması meteor ateşleri de aynı sistemi kullanır.

### H) AY ÇARPMASI [seviyesiz]
- Güneş patlaması ile aynı mantıkta ama biraz daha az nadir (güneş patlamasından biraz daha sık) ve biraz daha hafif. Ay yok olduktan sonra tekrar oluşmasın (madde 4).
- Siren çaldıktan 60 saniye sonra ay dünyaya çarpsın (siren bu anda fade-out ile dursun).
- Çarpma anında tüm oyuncuların ekranı ve harita sallansın.
- Ardından 1-2 dakika boyunca dünyaya meteorlar düşsün. Bu meteorların çıkardığı ateşler de orman yangınındaki gibi etrafa tutuşup yayılsın (ortak yangın sistemi). Bu meteorlar da nadir şansla Meteorit Parçası / Kozmik Çekirdek bırakabilsin (madde 13c).
- TSUNAMİ OLASILIĞI ARTIŞI: Ay çarpması gerçekleştikten sonra (ay patladığı andan itibaren) tsunami oluşma olasılığı geçici olarak belirgin şekilde artsın (örneğin normal olasılığın 3-4 katı; çarpan ve süre config'den ayarlanabilir, örneğin birkaç Minecraft günü boyunca). Bu süre sonunda olasılık yavaş yavaş normale dönsün. Madde 4'teki olasılık çarpanı sistemini kullan. Artışa rağmen iki tsunami aynı anda olmasın, minimum bekleme süresi kuralı bozulmasın ve oyun bunaltıcı hale gelmesin. Ayın yarattığı çekim/gelgit etkisi hissi verecek şekilde, tsunami öncesi sular geri çekilme efekti de bu dönemde biraz daha belirgin olabilir.
- DENİZ SEVİYESİ YÜKSELMESİ: Ay patladıktan 10 dakika (gerçek zaman, config'den ayarlanabilir) sonra dünyadaki tüm denizlerde, okyanuslarda, göllerde ve nehirlerde su seviyesi HER YERDE yavaş yavaş yükselsin. Yükselme ılımlı olsun: toplamda sadece birkaç blok (varsayılan yaklaşık 3-5 blok, maksimum sınır config'den ayarlanabilir, mutlaka makul bir üst sınır olsun). Tüm dünya sular altında kalmasın, ultra seviyede yükselme OLMASIN; sadece alçak kıyı bölgeleri, plajlar, nehir kenarları ve düz arazilerin alçak kısımları kısmen su altında kalsın, yüksek arazi, dağlar ve çoğu yapı etkilenmesin. Yükselme ani değil, kademeli ve fark edilir şekilde yavaş olsun (birkaç dakikaya yayılsın). Bu yükselmiş seviye varsayılan olarak kalıcı olsun (config'den, belirli bir süre sonra yavaşça eski haline dönme seçeneği de eklenebilsin). Bu işlem madde 8'deki ortak özel su sistemiyle yapılsın: gerçek su bloklarını değiştirme, harita verisini bozma, çok sayıda blok güncellemesi yapıp sunucuyu kastırma. Yükselen suyun içinde kalan oyuncular normal su kurallarına göre yüzsün/boğulma kurallarına tabi olsun, ve yükselen su seviyesi sıcaklık sisteminde de (soğuk su, ıslaklık) doğru çalışsın. Su seviyesi bilgisi dünya verisinde saklansın, sunucuya sonradan giren oyuncuya da doğru senkronize edilsin ve sunucu yeniden başlayınca kaybolmasın.
- Ay patladıktan sonra, Minecraft dünyasında gece olduğunda (güneş olmadığında) gökyüzünde ARTIK HİÇ AY GÖRÜNMESİN; onun yerine çok sayıda yıldız, galaksi ve nebula görünsün. Bu gökyüzü görsel olarak çok kaliteli ve etkileyici olsun (hafif ama detaylı bir render ile; madde 16'daki ortak gökyüzü sistemi). Bu değişiklik dünya verisinde saklansın ve sunucuya sonradan giren her oyuncuda da doğru görünsün.

## KOMUTLAR

Test için: afetleri manuel tetikleyen komutlar ekle (örnek: /dogalafet start <afetadı> [seviye] [oyuncu], /dogalafet stop). Komutlar sadece OP yetkisi olanlar tarafından kullanılabilsin. Sıcaklık sistemini test etmek için /dogalafet sicaklik set <oyuncu> <değer> ve /dogalafet sicaklik bilgi <oyuncu> komutları da olsun. Ay çarpması sonrası etkileri hızlı test edebilmek için /dogalafet denizseviyesi <değer> (su yükselmesini manuel ayarlama/sıfırlama) komutu ve ay çarpması sonrası bekleme süresini atlayan bir test komutu da olsun. Dünya donmasını test etmek için /dogalafet donma <yüzde> (global donma ilerlemesini ayarlama/sıfırlama) ve /dogalafet donma bilgi (mevcut ilerleme ve işlenen chunk sayısı) komutları da olsun. Kızıl cüce dönemini test etmek için /dogalafet kizilcuce baslat (bekleme süresini atlayıp doğuşu hemen başlatır) ve /dogalafet kizilcuce bilgi (mevcut gökyüzü dönemi, çözülme ilerlemesi, oyuncunun bulunduğu chunk'ın kalıcı kış bölgesi olup olmadığı) komutları da olsun. Şiddet sistemini test etmek için /dogalafet gun <sayı> (dünya yaşını/oyun gününü ayarlayıp seviye ağırlıklarını görme) komutu ve /dogalafet odul meteor (test için Meteorit Parçası / Kozmik Çekirdek verme) komutu da olsun.

## TESLİM EDİLECEKLER

- Kod eksiksiz olsun: placeholder, "buraya kodu ekle" gibi boşluklar bırakma. Tüm sınıfları, registry'leri, mixin gerekiyorsa mixin config'lerini, build.gradle, mods.toml, pack.mcmeta, lang dosyalarını, tarif (recipe) JSON'larını, loot table/loot modifier'ları, advancement JSON'larını ve ses tanımlarını (sounds.json) yaz.
- Kullanacağım ses ve doku dosyaları için hangi isimlerle, hangi klasörlere koyacağımı net söyle (ya da kod içinde üretilebilen alternatifler öner).

---

## NETLEŞTİRMELER

Burada olmayan bir belirsizlikte makul olanı seç ve `docs/KARARLAR.md`'ye gerekçesiyle yaz.

N1. **NBT:** Minecraft 1.20.1'de eşya veri bileşeni (data component) yok, 1.20.5'te geldi. "NBT/veri bileşeni" ve "veri bileşeniyle" ifadeleri (tüy upgrade'i, Yünlü, Termostatlı) ItemStack NBT'si demektir. Tooltip satırları `ItemTooltipEvent` ile eklenir; böylece başka modların zırhlarında da görünür.

N2. **Tüy upgrade'i elleri:** "SAĞ el" ana el (tüy), "SOL el" yardımcı el / offhand (zırh) demektir; oyuncu sol elli ayardaysa da ana/yardımcı el eşlemesi geçerlidir. Oyuncu bir bloğa bakarken sağ tıklasa da çalışır. Üst sınır config'te, varsayılan 30.

N3. **Zırh tarifleri (yün, Termostat Plakası):** İkisi de vanilla ve modlu her zırh parçasını (`ArmorItem`) kabul eden özel şekilsiz tarif tipidir; NBT, büyüler, dayanıklılık ve diğer upgrade'ler korunur; parça zaten yünlü/termostatlıysa tarif sonuç vermez. Yün olarak `#minecraft:wool` etiketindeki her renk kabul edilir. "Donma süresini %25 azaltsın" ifadesi parça başına soğuma/donma **hızını** %25 azaltmak demektir (4 parça = normal soğukta hiç donmama; metindeki "donma hızı %100 azalsın" ile tutarlı).

N4. **Yalıtım modeli:** Vücut sürekli metabolik ısı üretir (hareket ve koşmayla artar); yalıtım (zırh, yün, tüy) vücut ile ortam arasındaki ısı alışverişini yavaşlatır. Soğukta bu, soğumayı yavaşlatır. Ilık/sıcak ortamda üretilen ısı atılamadığı için vücut ısınır; bu yüzden çok yalıtımlı oyuncu aşırı ısınır ve yanar ("kalın giysi soğukta kurtarır, sıcakta pişirir"). Yünün N3'teki oranı soğuma yönüne uygulanır; yün de toplam yalıtıma katıldığı için aşırı sıcakta dezavantaja döner. Aşırı soğuk eşiği config'tedir; altında yün ve tüy donmayı yalnız yavaşlatır. Termostat vücudu nötre çeker ve etkisi uç sıcaklıklara doğru azalır.

N5. **Siren ses kategorisi:** Vanilla `SoundSource` enum'una yeni kategori eklemek enum hack'i ya da mixin ister ve başka modlarla çakışabilir [doğrulanmadı, araştır]. Tercih edilen yol: vanilla "Müzik ve Sesler" ekranına `ScreenEvent.Init.Post` ile bir "Sirenler" kaydırıcısı eklemek; kaydırıcı CLIENT config'teki 0–100 siren ses seviyesini değiştirir. Gerçek kategori eklemenin güvenli ve uyumlu bir yolu bulunursa o kullanılabilir. Varsayılan seviye %25–30.

N6. **"Ekranda uyarı yazısı yok" kuralının kapsamı:** Kural, afetin geldiğini, türünü ya da seviyesini herkese yazıyla duyuran her şeyi kapsar: başlık, banner, toast, sohbet mesajı, action bar. Berk'in metninde ayrıca istenen şu göstergeler kuralın dışındadır ve yazılır: sıcaklık barı (madde 7d), donma ve aşırı ısınma ekran efektleri (madde 7c), Hava Radarı göstergesi (yalnız elde tutulurken, madde 13a), Sismograf action bar'ı (yalnız taşıyan oyuncuda, madde 13b), vanilla başarım bildirimleri (madde 12), vanilla altyazılar (madde 5) ve OP komut çıktıları. "Bunun için bir HUD/overlay sınıfı da yazma" cümlesi afet uyarısı overlay'i içindir; bu göstergelerin overlay'leri yazılır.

N7. **Radarda güneş patlaması ve ay çarpması** [yorum, Berk değiştirebilir]: Madde 13a'daki parantez harfiyen uygulanır: bu iki afet ön-uyarı penceresinde radarda görünmez; siren başladığında radar "gökyüzü anomalisi" gösterir ve meteorların/çarpmanın başlamasına geri sayım yapar.

N8. **Gamerule, config ve Cold Sweat:** Gamerule'lar dünya başına aç/kapa ve çarpandır; sayısal ayarlar config'te durur. `afetSikligi` config'teki olasılıkları çarpar ve afetler arası beklemeleri böler (N25); `afetMaksSiddet` madde 10'daki seviye seçimini keser. Sıcaklık sistemi ancak `sicaklikSistemi` gamerule'ü açıksa **ve** SERVER config'teki `sicaklikSistemiModu = OTOMATIK | ACIK | KAPALI` (varsayılan `OTOMATIK`: Cold Sweat yüklüyse kapalı) izin veriyorsa çalışır; böylece iki sistem aynı anda hasar vermez. Bizim sistem kapalıyken güneş patlaması, süpernova ve kızıl cüce sıcaklık etkisinin Cold Sweat'in API'si üzerinden nasıl uygulanacağını araştır; API kullanılırsa bağımlılık `compileOnly` ve `mods.toml`'da isteğe bağlıdır (`mandatory=false`), çağrılar `ModList.get().isLoaded(...)` korumalı ayrı bir sınıftadır. Uygun yol yoksa yalnız çevresel etkiler (yangın, buz, gökyüzü) kalır ve bu `docs/KARARLAR.md`'ye yazılır.

N9. **Gerçek blok değişiklikleri ve buharlaşma** [yorum, Berk değiştirebilir]: Madde 8'deki "gerçek su bloklarına dokunma" kuralı yalnız ortak özel su sistemi içindir. Dünya donması (madde 9) ve güneş patlamasındaki buharlaşma gerçek bloklarla yapılır. Buharlaşma madde 9'daki iki yönlü tabloya sığmaz (su → hava kendiliğinden geri dönmez), bu yüzden şöyle çalışır: 5 günlük süreç boyunca global bir "buharlaşma derinliği" artar (config, varsayılan en çok 6 blok); her chunk bu derinliğe kadar yüzeyden aşağı kurur, sığ göller ve nehirler tamamen kuruyabilir, okyanuslar birkaç blok alçalır ve buhar/sis efekti alır. Chunk'lar madde 9'daki donma seviyesiyle aynı mekanizmayla hedefe yetişir (yalnız yüklü chunk'lar, oyuncu yakınlık önceliği, batch, TPS kısması). Kuruyan su konumları chunk başına sıkıştırılmış biçimde saklanır; süpernovadan sonra bu sular yeniden oluşur ve donma sistemine girer (bölüm D: "su yeniden oluşmuş/kalmış alanlar").

N10. **Dünya dönemleri arasındaki geçişler** [yorum, Berk değiştirebilir]: Ay çarpmasından sonra güneş patlaması gelirse buharlaşma önce sanal deniz yükselmesini kademeli olarak sıfıra indirir, sonra gerçek suya geçer; yükselme sonraki dönemlerde geri gelmez. Donmuş dünyada tsunami, hedef bölgede yeterli açık (donmamış) su yoksa N14'teki gibi ertelenir.

N11. **Boyut:** Afetler Overworld'de oluşur (config'te boyut listesi). Nether/End'deki oyuncular sireni ve global gökyüzü durumunu yine alır.

N12. **Blok sınıfları etiketlerle tanımlanır:** Sert / sağlam / yanmaz bloklar (ör. `dogalafetler:asit_dayanikli`, `dogalafetler:deprem_dayanikli`; varsayılanlar taş, deepslate, obsidyen vb.) ve madde 9'daki donma dönüşüm tablosu veri paketiyle genişletilebilir (etiket ya da JSON). Yanıcılık vanilla yanıcılık değerinden gelir. Böylece başka modların blokları veri paketiyle ayarlanabilir.

N13. **Efekt kalitesi:** Varsayılan "Yüksek" (metindeki "Kalite" modu); "Düşük Efekt" modu "Düşük" seviyesidir; "Ultra" güçlü makineler için isteğe bağlı üst seviyedir.

N14. **Tsunami yeri:** Hedef oyuncunun yakınında okyanus/geniş su yoksa uygun başka bir oyuncu seçilir; hiçbiri uygun değilse tsunami ertelenir ve sıradaki afet seçilir.

N15. **İstemci/sunucu ayrımı:** Madde 3'teki `@OnlyIn(Dist.CLIENT)` ifadesinin amacı ayrımdır; Forge belgesi bu anotasyonu modlarda doğrudan kullanmayı önermez ("There is NO reason for using this annotation directly", docs.minecraftforge.net 1.20.x, Sides). İstemci sınıfları `client` paketinde durur ve yalnız `@Mod.EventBusSubscriber(value = Dist.CLIENT)` aboneleri, `DistExecutor` ya da `FMLEnvironment.dist` kontrolü üzerinden çağrılır. Ortak/sunucu kodu istemci sınıfını doğrudan import etmez.

N16. **Başarım ölçütleri** [yorum, Berk değiştirebilir]: "Süpernovayı gökyüzünde gör": süpernova aşamasında Overworld'de gökyüzünü gören bir konumda bulunmak. "Yıldızlı Gece": ay yok olduktan sonraki bir gecede gökyüzünü gören konumda en az 10 saniye kalmak. "Çorba İçer Isınır": donma eşiğinin altındayken sıcak bir yiyecek yemek ve etkisi sürerken vücudun nötr bölgeye dönmesi. "Birlikte Hayatta": aynı afetin etki alanında afet bitene kadar en az 3 oyuncunun ölmeden kalması. "Kozmetik ünvan": oyuncu verisinde saklanan ünvan; yalnız modun kendi başarım duyurusunda adın önünde görünür, config ile kapatılabilir. Görünen ad, tab listesi, oyuncu sohbeti, ölüm mesajları ve vanilla başarım duyuruları öneksiz kalır (kullanıcı kararı 2026-11-07; eski "sohbet adının ve tab listesindeki adın önünde, `PlayerEvent.NameFormat`, `TabListNameFormat`" cümlesinin yerine; `docs/KARARLAR.md` K-E57, A-37). "Topla/bul" ölçütleri (Gökten Gelen Misafir, Kozmik Şans) eşyaya ilk kez sahip olmaktır (yerden, sandıktan, `/give` ya da yaratıcı envanterden); "craft et" ölçütleri (Yer Dinleyici, Radar Altında) yaratıcı kipte eşyanın envantere girmesini de sayar (K-E57, A-36).

N17. **Sayısal varsayılanlar:** Metinde sayısı verilmeyen her değer (sıcaklık birimi ve eşikleri, hasar miktarları, afet boyutları ve süreleri, seviye tablosu, olasılıklar, bekleme süreleri, TPS kısma eşiği, paket sıklığı, güneş patlaması gelişim kriteri) gerçekçi ve oynanabilir bir varsayılanla config'e konur ve `docs/KARARLAR.md`'de tek bir tabloda gerekçesiyle listelenir.

N18. **Komut ayrıntıları:** Afet adları `kasirga`, `tsunami`, `deprem`, `gunes_patlamasi`, `asit_yagmuru`, `sinkhole`, `orman_yangini`, `ay_carpmasi`. Berk'in istediği "ay çarpması sonrası bekleme süresini atlayan test komutu": `/dogalafet aysonrasi atla` (10 dakika beklenmeden deniz seviyesi yükselmesi başlar). Doğrulama için ek komutlar (yine yalnız OP):
- `/dogalafet durum`: aktif afet, aşaması, seviyesi, kalan süre, bekleme süreleri, geçici çarpanlar, deniz seviyesi, ay/güneş durumu, donma ve buharlaşma ilerlemesi.
- `/dogalafet asama ileri`: aktif afeti ya da dünya dönemini bir sonraki aşamaya geçirir (güneş patlamasının 5 gününü, süpernovayı, mağara suyunun buharlaşmasını beklemeden test etmek için).
- `/dogalafet hiz <çarpan>`: afet ve dönem zamanlayıcılarını hızlandırır (1 = normal).
- `/dogalafet sifirla`: dünyanın afet ve dönem durumunu ilk haline döndürür (ay, güneş, deniz seviyesi, donma, çarpanlar); testleri tekrarlamak için.
- `/dogalafet siklik [yüzde]`: afet sıklığını (`afetSikligi` gamerule'ü) gösterir ya da ayarlar ve sonraki afetin ne zaman beklendiğini yazar (N25).

N19. **Dokular ve sesler:** Madde 13'teki ve TESLİM'deki "bana söyle / alternatif öner" isteği vibe düzeninde şöyle karşılanır: doku ve sesleri işçiler kendisi üretir ve oyunda gerçekten kullanılır; üretim betikleri `tools/assets/` altında durur, tekrar çalıştırılabilir. README her dosyanın adını, klasörünü, boyutunu ve nasıl değiştirileceğini yazar. Dışarıdan lisanslı dosya alınırsa lisansı `docs/ASSET-LISANS.md`'ye yazılır.

N20. **Asit yağmuru hasarı** [yorum, Berk değiştirebilir]: Madde 10'daki "oyuncuya verilen hasar … artar" cümlesinin yerine (kullanıcı kararı 2026-11-07): başı açık oyuncu, yağmur sürdükçe her seviyede saniyede yarım kalp (1 can puanı, config `asit_yagmuru.hasarSaniye`) hasar alır; seviye yağış alanını, süresini ve aşındırma hızını büyütür, hasarı değil. "Seviye 5'te bile öldürücü olmasın" kuralı aynen geçerlidir (`asitMinimumCan`, 2 kalbin altına inmez). `docs/KARARLAR.md` K-E58.

N21. **Meteor yağmurlarının süresi ve yoğunluğu** [yorum, Berk değiştirebilir]: Bölüm H'deki "1-2 dakika boyunca" ifadesi kullanıcı isteğiyle aşılır: ay parçası yağmuru 10 dakika sürer, oyuncu başına dakikada 180 parça düşer (config `ay_carpmasi.meteorSuresiSn`, `meteorDakikaBasina`); ay çarpması afeti yağmurla biter, bu süre boyunca başka afet başlamaz. Güneş patlamasında meteorlar ön-uyarının son 90 saniyesinde seyrek başlar ve sirende artar (bölüm D'deki "siren başladıktan 60 saniye sonra" ve N7'deki gizli ön-uyarının bilinçli istisnası; `gunes_patlamasi.meteorYagmuruOncuSaniye=0` eski davranışı verir); AKTIF'in başında 10 dakika, sonraki her günün başında 3 dakika oyuncu başına dakikada 180 meteorluk yağmur, arada şartnamenin seyrek arka planı sürer. Meteorların boyu süreklidir (küçük %60, orta %30, büyük %10; 0,5–2,5 kat); krater, yangın, sarsıntı ve ses boyla büyür. Ödül şansları meteor başına aynı kalır (madde 13c); toplam ödül meteor sayısıyla artar. Bir meteorun uçuşu 8–10 saniyedir; yağmurun üçte ikisi oyuncunun baktığı yarıya, üçte biri arkasına düşer (oyuncu başına sayı eşit kalır; `meteor.ucusSuresi`, `meteor.bakisYonuOrani`). `docs/KARARLAR.md` K-E56 (rev.: 90 → 180/dk, MT2-25, MT2-28).

N22. **Patlama anı ve gün saati** [yorum, Berk değiştirebilir]: Doğal güneş patlaması yalnız sabah (gün saati 0–6000), doğal ay çarpması yalnız gece (13000–23000) asıl etkisine geçer; afet ön-uyarının sonunda bu pencereyi oyuncuya görünmeden bekler (en çok bir gün). `/dogalafet start` pencereye bakmaz (operatör niyeti); gün döngüsü kapalıyken beklenmez. Güneş patlaması AKTIF olduğu sürece Overworld'ün gün saati sabittir (doğal patlamada sabah saatinde, komutla pencere dışında başlayanda gelecek öğlede); bu, madde 16'daki "gün/gece döngüsüne dokunma" kuralının bilinçli istisnasıdır ve `doDaylightCycle` gamerule'üne dokunulmaz (kullanıcı kararı 2026-11-07; `docs/KARARLAR.md` K-E55, G2-1, G2-9).

N23. **Pulsar** [yorum, Berk değiştirebilir]: Süpernovadan sonraki doğuşta yeni yıldız düşük olasılıkla (config `gokyuzu.pulsarOlasiligi`, varsayılan %15) kızıl cüce yerine pulsardır: güneş yolunda dönen ışınlı mavi-beyaz yıldız, kızıl-sıcak gökyüzü; ortam ısınır, çimen kavrulur, yangınlar çıkar, donmuş dünya çözülür. Kalıcı dönemdir; `/dogalafet pulsar baslat` ile de başlar (kullanıcı isteği 2026-11-07; `docs/KARARLAR.md` gökyüzü G2 bölümü).

N24. **Deprem artçıları ve "iki afet aynı anda"** [yorum, Berk değiştirebilir]: Madde 4'teki "iki afet aynı anda başlamasın" kuralı yıkıcı afetler içindir. Depremin artçıları (blok değiştirmez, hasar vermez; bölüm C'nin "afet sonrası izi") ana sarsıntı bitince afet yuvasının dışında sürer; bu sırada yeni afet başlayabilir. `docs/KARARLAR.md` AF-30 (DS-14'ün yerine).

N25. **Afet sıklığı** [yorum, Berk değiştirebilir]: Madde 11'deki `afetSikligi` (100 = normal, 50 = yarı, 200 = iki katı) yalnız zar olasılığını değil, gerçekleşen afet sayısını ölçekler: yeni dünyadaki ilk gün beklemesi, iki afet arasındaki bekleme ve aynı türün tekrar beklemesi de bu çarpana bölünür (200'de yarısı, 50'de iki katı). Değer 0–1000 aralığındadır (1000 üstü 1000 sayılır); 0 afet seçimini durdurur. Değişiklik süren beklemelere hemen yansır ve OP'lere bildirilir. Afetin kendi süresi ve komutla başlatılan afet etkilenmez. Kolaylık komutu `/dogalafet siklik [yüzde]` aynı gamerule'ü okur/yazar ve sonraki afet için tahmin verir. `docs/KARARLAR.md` K-E62.

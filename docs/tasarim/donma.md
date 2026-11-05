# Dünya Donması ve Çözülme — tasarım notu (madde 9, bölüm D, N9, N10, N12)

Bu not görsel dili ve adımların sırasını tanımlar; chunk hedef seviyesi, kuyruk ve maske mantığı spike 3 / `donma` paketindedir.

## 1. Gerçek olay

- **Kırağı (hoar frost) ve kayrağı (rime):** kırağı durgun, açık ve soğuk gecelerde su buharının doğrudan yüzeye kristalleşmesiyle oluşur; tüy, iğne ya da eğrelti biçimli kristaller, özellikle çimen uçlarında ve kenarlarda. Kayrağı sisli ve rüzgârlı havada aşırı soğumuş damlacıkların donmasıyla oluşur; **rüzgâra bakan tarafta** kalın, beyaz, opak kabuk büyütür.
- **Göl ve nehir buzu:** buz kıyıdan (sığ, durgun su) başlar — **kıyı buzu / kenar buzu**; akıntılı suda önce frazil (yüzen buz kristali) ve buz çorbası, sonra levhalar; durgun su ince, kara ve şeffaf bir "skim" buzla örtülür, kalktıkça beyazlaşır. Buz kalınlığı soğuk birikimiyle (donma derece-günü) yaklaşık kareköküyle artar: ilk santimler hızlı, sonrası yavaş.
- **Çözülme:** buz kenarlardan ve koyu yüzeylere yakın yerlerden açılır; çürümüş (candled) buz grileşir, dikey kristal sütunlara ayrılır; kar önce güneye bakan yamalardan, ağaç diplerinden kalkar.

Kaynaklar:
- https://www.rmets.org/metmatters/how-does-hoar-frost-form
- https://www.rmets.org/metmatters/types-frost
- https://westvalleynaturalists.org/geography/hoarfrost-or-rime-ice (kırağı ve kayrağı farkı, rüzgâr yönü)
- https://seagrant.umn.edu/news-info/featured-stories/lake-river-ice-formation-classification (kıyıdan büyüyen buz, frazil)
- https://nrc-publications.canada.ca/fra/voir/td?id=05e9e078-6f46-4fa4-bbd7-2a608aa77b3c (göl buzunun oluşumu, kalınlaşması ve erimesi)

## 2. Minecraft'ta gösterim

**Adımlar (global ilerleme eşikleri, config).**

| İlerleme | Adım | Gerçek blok değişimi (iki yönlü tablo) | Yalnız görsel |
|---|---|---|---|
| %0–10 | Ayaz | — | Oyuncu ağzından nefes buharı (2 sn'de bir), havada seyrek süzülen buz kristali parıltısı, sis mavimsi |
| %10–25 | Kırağı | çimen bloğu ↔ `kiragili_cimen`; uzun ot/eğrelti ↔ `kiragili_ot` | Yaprak ve çimen biyom rengi beyaz-maviye kayar (renk çarpanı, ilerlemeyle %0→%60) |
| %25–40 | Kenar buzu, ilk kar | su (kıyıya bitişik) ↔ `ince_buz`; hava (zemin üstü) ↔ kar katmanı 1 | İnce buz koyu ve yarı saydam, altındaki su görünür |
| %40 | Kar yağışı | — | Yağmur yerine kar çizilir (`renderSnowAndRain`), kar taneleri daha iri ve yavaş |
| %40–70 | Yüzey donması | su yüzeyi ↔ `ince_buz` ↔ buz; buz kalınlığı 1→2 blok; kar katmanı 1→3 | Buz yüzeyinde parıltı noktaları |
| %70–100 | Buzul | buz kalınlığı 2→4 blok, en üst buz ↔ sıkıştırılmış buz (deniz ortası); kar katmanı 3→5 (çatılar, yapraklar dahil); hava (yaprak/çatı altı) ↔ `buz_sarkiti`; çimen ↔ kar bloğu yalnız %95+ | Rüzgâra bakan blok yüzlerinde kayrağı örtüsü (aşağıda) |

**Kendi bloklarımız (hepsi iki yönlü tabloda, `dogalafetler:` ad alanı).**
- `kiragili_cimen`: çimen dokusu, üst yüzde ve yan üst kenarda beyaz iğne kristalleri (16×16, 6 renkli kristal deseni), biyom rengi alt katmanda korunur.
- `kiragili_ot`: uzun otun uçları beyaz.
- `ince_buz`: neredeyse kara, %70 saydam, içinde ince beyaz çatlak ağı; üzerinde yürünür (şartname: "tamamen donmuş yüzey yürünebilir"), koşarken çıtırtı sesi.
- `buz_sarkiti`: tavana asılı 1–3 piksel kalın buz sarkıtı (çift doğa çapraz model), çatı saçaklarında ve yaprak altında; çözülmede damla parçacığı bırakıp kaybolur.

**Kenardan içeri.** Chunk işlenirken her su sütunu için "kıyıya uzaklık" (chunk ve komşu kenarları içinde çok kaynaklı BFS, 0–16, chunk başına önbellek) hesaplanır; sütunun donma eşiği `eşik + uzaklık · 0,02`. Böylece buz kıyıdan göl ortasına doğru halka halka ilerler ve çözülmede tam tersi sırayla açılır. Kalınlık da aynı kuralı izler: kıyıda kalın, ortada ince.

**Rastgele sıra.** Aynı eşikteki bloklar sabit karma ile sıralanır (`hash(x,z,seed)`); değişim "yayılıyor" gibi görünür ama aynı chunk her yüklemede aynı görünür (deterministik).

**Kayrağı örtüsü (yalnız istemci).** %70 üstünde, çit, duvar, ağaç gövdesi gibi blokların hakim rüzgâr yönüne bakan yüzüne beyaz, tüy biçimli kristal dokusu (decal) bindirilir: yalnız oyuncunun 24 blok çevresinde, görünen yüzlerde, `RenderLevelStageEvent.AFTER_SOLID_BLOCKS` ile toplu çizim. Blok değişmez, harita etkilenmez.

**Kalıcı kış sınırı (kızıl cüce dönemi).** Chunk hedefi `hedef = max(global, maske)`; maske değeri seed'li düşük frekanslı gürültüden (dalga boyu ~1200 blok) gelir ve sınırda 64 bloklu `smoothstep` ile 1'den globale iner. Ayrıca blok düzeyinde eşik titreşimi (±%8 karma): sınır çizgi değil, kırağı ve kar adalarının seyrekleştiği geniş bir kuşak olarak okunur. Y > 120 ve soğuk biyomlar daima %100.

**Çözülme görünümü.** Ters sıra: kar katmanları ve kırağı önce, buz sarkıtları damlayarak, sonra su yüzeyi kenardan içeri. Geçiş anı görülsün diye: buz → `ince_buz` (koyulaşma, "çürük buz" evresi) → su; çözülen blokta 1 sn damla ve buhar parçacığı, yapraklardan süzülen damlalar (%2 olasılıkla, oyuncu yakınında). Biyom rengi tonlaması yerel hedef seviyeyi izler.

**Buharlaşmayla sıra (N9, N10).** Güneş patlamasında kuruyan su konumları süpernovadan sonra önce su olarak geri gelir (kıyıdan dolan, 0,5 sn'lik dalgacık parçacığıyla) ve aynı chunk işinde sonra donma tablosuna girer; bir chunk'ta "geri dolma" tamamlanmadan donma adımı başlamaz.

## 3. Vitrin anı

**Donmanın %55 anında bir gölün kıyıdan içeri donması (hızlandırılmış, `/dogalafet hiz 20`, 10 sn).** Kamera gölü çevreleyen bir tepede, Y+12, pitch −30°; gölün yarısı beyaz buz, kıyıdan merkeze ilerleyen koyu `ince_buz` halkası, ortada son kalan açık su ve üstünden tüten buhar; kıyıda kırağılı beyaz uçlu çimenler, yapraklar mavi-beyaz, kar yağıyor, gökyüzünde yıldızlar ve nebula. Küçük resim için tam yukarıdan ikinci kare: eş merkezli buz halkaları.

## 4. Ses tasarımı

- **Rüzgâr katmanı:** ilerlemeyle artan soğuk rüzgâr (sentez: bant geçiren gürültü 300–1200 Hz, yavaş ses düzeyi dalgalanması); %0'da sessiz, %100'de düşük düzey sürekli. Kapalı alanda (üstü kapalı) %30.
- **Buz:** donan göl yüzeyinden seyrek, derin "buz şarkısı" — göl buzunun genişlerken çıkardığı dispersif "piyuv" sesi (sentez: 2 kHz → 200 Hz'e 0,3 sn'de inen chirp, yankılı), oyuncu buz üstündeyken 20–60 sn'de bir.
- **Çözülme:** damlama (`block.pointed_dripstone.drip_water`), buz kırılması (`block.glass.break` pitch 1,5, düşük düzey).
- Kategori `AMBIENT`, konuşma bandında düşük enerji.

## 5. Performans planı

| Kalite | Kayrağı örtüsü yarıçapı | Nefes buharı | Buz parıltısı | Çözülme damlası | Biyom rengi tonlama |
|---|---|---|---|---|---|
| Düşük | yok | yalnız kendi | yok | %0,5 | var |
| Orta | 12 | tüm oyuncular | 8 blok | %1 | var |
| Yüksek | 24 | tüm oyuncular | 16 blok | %2 | var |
| Ultra | 32 | tüm oyuncular | 24 blok | %3 | var |

- Sunucu blok bütçesi kaliteden bağımsız (config: tick başına en çok 256 blok, TPS < 18'de yarı, < 15'te dörtte bir); öncelik oyuncunun bulunduğu chunk → görüş mesafesinin yarısı → kalan yüklü chunk'lar. Blok değişimi `UPDATE_CLIENTS` bayrağıyla, komşu fizik güncellemesi yok; aynı chunk'taki değişimler aynı tick'te toplandığı için vanilla tek chunk bölümü paketi gönderir.
- Biyom rengi tonlaması chunk yeniden derlemesi ister; seviye her %5 değiştiğinde ve yalnız görünen chunk'larda tetiklenir (sürekli değil).

## 6. Açık noktalar — önerilen varsayılanlar

| Değer | Varsayılan |
|---|---|
| Donma süresi %0→%100 | 6 Minecraft günü |
| Çözülme %100→%40 | 10 gün (şartname) |
| Buz kalınlığı | %40'ta 1, %60'ta 2, %80'de 3, %100'de 4 blok |
| Kar katmanı | %25'te 1, %55'te 2, %70'te 3, %85'te 4, %95'te 5 |
| Kış sınırı geçiş genişliği | 64 blok + ±%8 blok titreşimi |
| Tonlama üst sınırı | %60 beyaz-mavi `#DCE8F5` |

- **Teknik risk:** vanilla çimen ve yaprak renklerini tonlamak için `RegisterColorHandlersEvent.Block`'ta vanilla blokların renk işleyicisini sarmak gerekir; başka bir mod da sararsa son kayıt kazanır. Sarıcı önceki işleyiciyi çağırıp yalnız çıktıyı karıştırır (zincirleme), böylece çakışma en aza iner.
- Yeni bloklar `#dogalafetler:donma_tablosu` JSON'unda; tanınmayan bloklara dokunulmaz (N12).

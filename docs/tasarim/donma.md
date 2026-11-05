# Dünya Donması ve Çözülme — tasarım notu (madde 9, bölüm D, N9, N10, N12)

Bu not görsel dili ve adımların sırasını tanımlar; chunk durumu, kuyruk ve maske mantığı `.vibe/kararlar/spike-donma.md` (K-D1…K-D5) ve `docs/KARARLAR.md` K0-24'tedir. Madde 9 bu nota karşı kazanır.

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

### Spike sonrası güncelleme, 2026-11-06

Bu bölüm spike-donma kararlarına ve inceleme B2'ye (çözülme sırası) göre yeniden yazıldı. Eşikler sütunun **etkin seviyesine** uygulanır: sütun hedefi + sabit titreşim (±8, `hash(x, z, seed)`). Donarken donma eşikleri, çözülürken (daha yüksek ya da eşit) çözülme eşikleri kullanılır (histerezis).

**Adımlar (config).**

| Öğe | Donma: geldiği seviye | Çözülme: kalktığı seviye | Gerçek blok değişimi | Yalnız görsel (istemci) |
|---|---|---|---|---|
| Ayaz | %0 | — | — | Nefes buharı (2 sn'de bir), süzülen buz kristali parıltısı, sis mavimsi |
| Kırağı | 10 | 50 | `grass_block` ↔ `dogalafetler:kiragili_cimen` | Yaprak ve çimen biyom rengi beyaz-maviye kayar (%0→%60) |
| İlk buz (kıyıya uzaklık `c` = 0–15) | `20 + 2c` (kıyıda 20, ortada 50) | `max(10, 45 − 3c)` (kıyıda 45, 12+ blok içeride 10) | su sütununun üst bloğu ↔ buz (ilk evre `ince_buz`, aşağıda) | İnce buz koyu ve yarı saydam, altındaki su görünür |
| Buz katmanı 2 / 3 / 4 | ilk buz + 20 / 40 / 60 | ilk buz çözülmesi + 10 / 20 / 30 | buz sütunu aşağı kalınlaşır; en alt su bloğu hiç donmaz | Buz yüzeyinde parıltı noktaları |
| Kar katmanı 1 / 2 / 3 / 4 / 5 | 25 / 55 / 70 / 85 / 95 | 55 / 65 / 75 / 85 / 95 | `#dogalafetler:donma/kar_zemini` köklü zeminin üstü; çimen/misel üstünde en çok 1 katman | — |
| Kar yağışı | 40 | 40 | — | Yağmur yerine kar (`renderSnowAndRain`), taneler iri ve yavaş |
| Buz sarkıtı ve kayrağı | 70 | 70 | — | Çatı saçağı ve yaprak altında sarkıt; rüzgâra bakan yüzlerde kayrağı örtüsü (aşağıda) |

Donma sırası: kırağı → kıyı buzu ("sular önce kenarlardan", madde 9) → kar → kar yağışı → göl ortasının donması ve kalınlaşma → kalın kar ve buzul görünümü.

**Kendi bloklarımız (`dogalafetler:` ad alanı).**
- `kiragili_cimen` (spike'ta yapıldı): `SnowyDirtBlock`; yayılmaz ve toprağa dönmez, yalnız donma/çözülmeyle değişir; `snowy` özelliği kaynağından taşınır. Üst doku `tools/assets/kiragili_cimen_doku.py` ile prosedürel (beyaz iğne kristalleri, biyom rengi alt katmanda), yan yüz vanilla karlı çimen, alt toprak; kırılınca toprak düşer.
- `ince_buz` (Faz 2): neredeyse kara, %70 saydam, içinde ince beyaz çatlak ağı; su sütunu planında üst bloğun ilk ve son evresi (su → `ince_buz` → buz; çözülmede buz → `ince_buz` "çürük buz" → su). Üzerinde yürünür, koşarken çıtırtı sesi. Spike yalnız vanilla buz koydu.
- Üstünde bitki olan zemine dokunulmaz (kısa ot kırağılı çimende yaşayamaz); bitkilerin kırağısı yalnız biyom rengi tonlamasıyla görünür. Eski plandaki `kiragili_ot`, `sikistirilmis buz` adımı ve çimen ↔ kar bloğu kaldırıldı (spike: kök belirsizliği, buz su sütununun hedefi). Buz sarkıtı gerçek blok değil, istemci çizimi (hava → sarkıt dönüşümünün kökü belirsiz olurdu).

**Spike kararı — tablo (K-D3).** Yüzey tablosu tek köklü zincirdir: her blok en çok bir dönüşümün kaynağı ve en çok birinin hedefi, döngü yok, eşikler zincirde artar; çözülme orijinal bloğu köke yürüyerek bulur (orijinal blok kaydı yok). **Hedef yalnız `dogalafetler` ad alanındaki donma bloklarıdır**; yabancı hedef yükleyicide uyarıyla reddedilir (N12 genişlemesi için açık `"yabanciHedefOnayi": true`). Özellik kümesi farklı satırlar ve çift bloklar (`half`, `part`) reddedilir. Su sütunu (`su → N blok buz`) ve kar (köke bağlı katman) tablodan ayrı planlardır. Oyuncunun ya da doğanın karı/buzu chunk başına "bizim koyduğumuz" bitleriyle ayrılır ve hiç değişmez (doğal buz kalınlaştırılmaz: bilinen sınır). `afetBlokHasari=false` → hiç blok değişmez. Kanıt: `dondur(100) → çöz(0)` = başlangıç (486 blok durumu birebir).

**Spike kararı — chunk durumu ve bütçe (K-D1, K-D2).** Chunk başına Forge capability (`ChunkDonmaSeviyesi`, chunk NBT'sinde): son işlenen global ilerleme G, dönem (donma / çözülme), kar ve buz bitleri (~70 bayt). Bir iş chunk'ı en çok 5 G puanı ilerletir, 256 sütun sabit karışık sırayla. Bütçe **tick başına 256 blok + 4 ms tavanı**; **TPS < 18 → 1/2, < 15 → 1/4**. Yalnız ticking chunk'lar işlenir (komşusu yüklenmemiş chunk'ın kenarındaki değişim komşuyu senkron yükletiyordu). Kuyruk en yakın oyuncuya uzaklığa göre sıralı (madde 9 öncelik). Ölçüm: 441 chunk G 0→60 ~80 sn, `/forge tps` 20,0, işçi ortalaması ≤ 0,8 ms/tick; uzak chunk dönülünce 12 adımda yetişir.

**Kenardan içeri.** Chunk işlenirken her su sütunu için "kıyıya uzaklık" `c` (chunk ve komşu halkası içinde çok kaynaklı BFS, 0–15) hesaplanır. Donmada buz kıyıdan göl ortasına halka halka ilerler (eşik `20 + 2c`); **çözülmede de kıyıdan içeri açılır** (eşik `45 − 3c`): önce kıyı şeridi su olur, göl ortası en son. Kalınlık da aynı kuralı izler: kıyıda kalın, ortada ince.

**Rastgele sıra.** Aynı eşikteki bloklar sabit karma ile sıralanır; değişim "yayılıyor" gibi görünür ama aynı chunk her yüklemede aynı görünür (deterministik).

**Kayrağı örtüsü ve sarkıtlar (yalnız istemci).** %70 üstünde çit, duvar, ağaç gövdesi gibi blokların hakim rüzgâr yönüne bakan yüzüne beyaz, tüy biçimli kristal dokusu (decal); çatı saçaklarında ve yaprak altında 1–3 piksel kalın sarkıt sprite'ları. Yalnız oyuncunun 24 blok çevresinde, görünen yüzlerde, `RenderLevelStageEvent.AFTER_SOLID_BLOCKS` ile toplu çizim. Blok değişmez.

**Kalıcı kış sınırı (kızıl cüce dönemi, K-D4).** Kış ağırlığı `w` **sütun düzeyindedir**: `w = max(maske, biyom oranı, yükseklik bandı)`. Maske seed'li iki oktavlı değer gürültüsü (dalga boyu 1200 blok, kara %25, ±0,03 bantta `smoothstep`), chunk köşelerinde hesaplanıp sütunda çift doğrusal ara değerlenir; biyom oranı köşeye değen 2×2 chunk'ın soğuk/karlı biyom payı (gürültü biyomu, chunk yüklemez); yükseklik bandı `smoothstep(88, 120, yüzeyY)`, Y ≥ 120 tam kış. Komşu sütunlar arasındaki sıçrama **≤ 0,05** (ölçülen en büyük 0,0468): sınır çizgi değil, kırağı ve kar adalarının seyrekleştiği geniş bir kuşak olarak okunur. Çözülme döneminde sütun hedefi `T = G + (100 − G)·w − ofset·(1 − w)`; donma döneminde maske uygulanmaz.

**Çözülme görünümü (madde 9).** Sıra: **kırağı ve ince kar önce** (son kar katmanı 55'te, kırağı 50'de kalkar), **sonra buz kenardan içeri** (kıyı 45'te, göl ortası 10'a kadar). **Ilık bölgeler önce:** biyom sıcaklığından ofset `clamp(8·(sıcaklık − 0,3), 0, 10)` puan (çöl 10, ova 4, orman 3,2, tayga 0) kış dışı sütunları erken indirir. Taban %40'ta: kar ve kırağı yok, kıyı ve 1 blok içerisi açık, 2+ blok içeride buz, göl ortası kalın. Geçiş anı görülsün diye buz → `ince_buz` → su; çözülen blokta 1 sn damla ve buhar parçacığı, yapraklardan süzülen damlalar (%2 olasılıkla, oyuncu yakınında). Biyom rengi tonlaması yerel sütun hedefini izler.

**Buharlaşmayla sıra (N9, N10).** Güneş patlamasında kuruyan su konumları süpernovadan sonra önce su olarak geri gelir (kıyıdan dolan, 0,5 sn'lik dalgacık parçacığıyla) ve aynı chunk işinde sonra donma planına girer; bir chunk'ta "geri dolma" tamamlanmadan donma adımı başlamaz.

## 3. Vitrin anı

**Donmanın %55 anında bir gölün kıyıdan içeri donması (hızlandırılmış, `/dogalafet hiz 20`, 10 sn).** Kamera gölü çevreleyen bir tepede, Y+12, pitch −30°; gölün yarısı beyaz buz, kıyıdan merkeze ilerleyen koyu `ince_buz` halkası, ortada son kalan açık su ve üstünden tüten buhar; kıyıda kırağılı beyaz uçlu çimenler, yapraklar mavi-beyaz, kar yağıyor, gökyüzünde yıldızlar ve nebula. Küçük resim için tam yukarıdan ikinci kare: eş merkezli buz halkaları. Test dünyasının spawn'ı kalıcı kış bölgesinde (`w = 1`); kızıl cüce çözülme vitrini için kış dışı bir göl seçilir.

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

- Sunucu blok bütçesi kaliteden bağımsızdır (§2 spike kararı: 256 blok + 4 ms, TPS 18/15 kademeleri, yalnız ticking chunk, en yakın oyuncu önce). Blok değişimi `UPDATE_CLIENTS` bayrağıyla; aynı chunk'taki değişimler aynı tick'te toplandığı için vanilla tek chunk bölümü paketi gönderir.
- Spike sonrası güncelleme, 2026-11-06 (Faz 2 notları): her G adımında eşik aşmayan sütunlar blok okunmadan atlanır (10 oyuncuda bütçenin ~%46'sı boş taramaya gidiyordu); TPS ölçer çekirdekte tek kaynaktır (`MinecraftServer.tickTimes`); maske parametreleri çözülme başlarken dünya dönemi SavedData'sına yazılır (config değişse de kış bölgeleri kaymaz).
- Biyom rengi tonlaması chunk yeniden derlemesi ister; seviye her %5 değiştiğinde ve yalnız görünen chunk'larda tetiklenir (sürekli değil).

## 6. Açık noktalar — önerilen varsayılanlar

### Spike sonrası güncelleme, 2026-11-06

Tablo `.vibe/kararlar/spike-donma.md` "Önerilen config varsayılanları (N17)" ile hizalıdır.

| Config anahtarı | Varsayılan | Birim |
|---|---|---|
| `donma.tickBasinaMaksBlok` / `donma.tickBasinaMaksMs` | 256 / 4 | blok/tick / ms |
| `donma.seviyeAdimi` | 5 | G puanı |
| `donma.tpsEsigi` / `donma.tpsKritikEsigi` | 18 / 15 | TPS (bütçe 1/2 / 1/4) |
| `donma.donmaSuresi` / `donma.cozulmeSuresi` | 6 / 10 | Minecraft günü |
| `donma.cozulmeTabani` | 40 | % |
| `donma.bagimsizCozulme` | false | — |
| `donma.kiragi` (tablo JSON) | 10 / 50 | % (donma / çözülme) |
| `donma.karEsikleri` / `donma.karCozulmeEsikleri` | 25, 55, 70, 85, 95 / 55, 65, 75, 85, 95 | % |
| `donma.ilkBuzEsigi` / `donma.kiyiKatsayisi` / `donma.buzKalinlikAdimi` | 20 / 2 / 20 | % / %/blok / % |
| `donma.buzCozulmeTabani` / `donma.buzCozulmeKiyiKatsayisi` / `donma.buzCozulmeEnAz` / `donma.buzCozulmeAdimi` | 45 / 3 / 10 / 10 | % / %/blok / % / % |
| `donma.kiyiUzaklikSiniri` / `donma.maksBuzKalinligi` | 15 / 4 | blok |
| `donma.titresimGenligi` | 8 | % (bütün eşiklerden küçük) |
| `donma.kisOrani` / `donma.kisDalgaBoyu` / `donma.kisGecisBandi` | 0,25 / 1200 / 0,03 | oran / blok / gürültü birimi |
| `donma.kisYGecisAlt` / `donma.kisYGecisUst` | 88 / 120 | blok |
| `donma.ilikOfsetSicaklikTabani` / `donma.ilikOfsetKatsayisi` / `donma.ilikOfsetEnCok` | 0,3 / 8 / 10 | biyom sıcaklığı / puan / G puanı |
| Tonlama üst sınırı (CLIENT) | %60 beyaz-mavi `#DCE8F5` | — |

- **Teknik risk:** vanilla çimen ve yaprak renklerini tonlamak için `RegisterColorHandlersEvent.Block`'ta vanilla blokların renk işleyicisini sarmak gerekir; başka bir mod da sararsa son kayıt kazanır. Sarıcı önceki işleyiciyi çağırıp yalnız çıktıyı karıştırır (zincirleme), böylece çakışma en aza iner. `kiragili_cimen`'in biyom rengi de bu işte.
- Tablo veri paketinde (`dogalafetler:donma` JSON + `#dogalafetler:donma/kar_zemini` etiketi); tanınmayan bloklara dokunulmaz (N12). `/reload` ile kaldırılan satırın dönüştürdüğü bloklar donuk kalır (bilinen sınır).
- `/dogalafet sifirla` blokları çözmeden yalnız veriyi sıfırlar.

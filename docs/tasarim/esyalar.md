# Özel eşyalar — tasarım notları

Her eşya işçisi bu dosyaya kendi bölümünü ekler.

## Başarımlar (madde 12, N16)

**Hedef his.** Sekme bir "hayatta kalma günlüğü" gibi okunmalı: kökten dallanan her kol bir felaket hikâyesi (bölgesel afetler,
gökten gelenler, güneşin ölümü ve buzul çağı, ayın düşüşü, hazırlık, şiddet). Toast ve sohbet duyurusu vanilla'nın kendisidir
(N6 istisnası); ödül küçük ama elle tutulur: XP, hedeflerde bir iki sıcak yiyecek, meydan okumalarda adın önünde bir ünvan.

**Sekme.** "Doğal Afetler", kök "Felaket Başlıyor" (çan ikonu: siren). Arka plan `textures/gui/advancements/backgrounds/felaket.png`,
16×16 döşenebilir: koyu bazalt/kül zemin, kenardan kenara akan fay çatlakları, çatlak içinde kor parıltısı (deprem yarığı ile
meteor/yangın korunun ortak dili). Kontrast düşük tutuldu ki ikonlar ve bağlantı çizgileri okunsun. Üretim:
`tools/assets/basarim_ikon.py` (Pillow, sabit tohum; aynı baytlar). Değiştirmek için betikteki `ZEMIN`, `CATLAK`, `KOR` renkleri
ya da `YOLLAR` çatlak yolları düzenlenir ve betik yeniden koşulur; dosya adı ve 16×16 boyutu korunmalı.

**Ağaç (yerleşim, ön koşul değil).**

| Kol | Başarımlar |
|---|---|
| Bölgesel afetler | 1 İlk Rüzgâr, 2 Dalga Avcısı, 3 Sallanan Zemin, 4 Delikten Dönüş, 5 Yanmadan Çıktım, 6 Çatı Altı |
| Gökten gelen | 7 Gökten Gelen Misafir → 22 Kozmik Şans |
| Güneşin ölümü | 8 Güneşi Atlattım → 9 Süpernova Tanığı → 10 Buzul Çağı → 11 Kutup Sığınağı; 10 → 20 Buz Balıkçısı → 21 Kutup Ustası |
| Ay | 12 Ay Düştü → 13 Yıldızlı Gece, 14 Yükselen Sular |
| Aletler | 16 Radar Altında → 15 Yer Dinleyici |
| Giyim ve sıcaklık | 17 Katmanlı Giyim → 18 Yünlü Kahraman → 19 Çorba İçer Isınır |
| Şiddet | 23 Fırtınaya Meydan Oku → 24 Felaket Ustası → 25 Hepsini Atlattım (gizli); 23 → 26 Birlikte Hayatta (gizli) |

Vanilla görünürlüğü korunur: kazanılmış bir atanın en çok iki kat altı görünür, böylece sekme ilk sirenle açılır ve oyuncu
ilerledikçe derinleşir (buzul çağı kolu süpernovadan önce sır kalır). #25 ve #26 açılana kadar hiç görünmez.

**İkonlar.** Vanilla eşyalar anlamı taşır: çan (siren), elitra (rüzgâr), trident (dalga), çatlak taş tuğla (deprem), merdiven
(çukurdan dönüş), kamp ateşi (yangın), tuğla basamak (çatı), ayçiçeği (güneş), nether yıldızı (süpernova), sıkıştırılmış ve mavi
buz (buzul, kutup), olta ve morina (buz balıkçılığı), end taşı (ay), dürbün (yıldızlı gece), kanal (yükselen sular), tüy, beyaz
yün, paratoner (fırtına), ölümsüzlük totemi (felaket ustası), fener (hepsi), pasta (birlikte). Mod eşyası ikonları (Meteorit
Parçası, Kozmik Çekirdek, Hava Radarı, Sismograf, Sıcak Tavuk Çorbası) kardeş dallar birleşince `runData` ile gerçek eşyaya döner;
o zamana kadar vanilla yedeği (ateş topu, End kristali, pusula, sculk sensörü, mantar çorbası).

**Çerçeveler ve ödüller.** Görev (köşeli) 15 XP; hedef (yuvarlak) 40 XP + 1–2 Sıcak Salep (yiyecek dalı gelene kadar fırınlanmış
patates); meydan okuma (dikenli, mor duyuru) 100 XP + ünvan. XP'ler `basarim.odul.*` config'iyle değişir.

**Ünvan.** Altın renkli `[Ünvan] ` öneki sohbet adında ve tab listesinde (N16). Her meydan okumanın kendi ünvanı: Kutup Sakini,
Kozmik Kâşif, Felaket Ustası, Her Şeyi Atlatan; en son kazanılan geçerli. Ad çevrilebilir bileşen olduğu için her oyuncu kendi
dilinde görür. `basarim.unvanGoster=false` öneki kapatır; `/dogalafet unvan <oyuncu> [ad|yok]` OP'nin elidir.

**Vitrin anı.** Seviye 5 bir afetin doğal bitişinde üç toast'ın üst üste akması (İlk Rüzgâr, Fırtınaya Meydan Oku, mor
çerçeveli Felaket Ustası) ve sohbetteki mor duyurunun kendisinde oyuncunun adının önünde beliren altın `[Felaket Ustası]`
(ünvan duyurudan önce verilir). Üçü aynı tick'te aynı tetikleyiciden gelir; oyunda sıraları garanti değildir. Görsel kanıt
koşusu sırayı sabitler: tür ve seviye 3 ayrı tick'lerde verilir, doğal bitiş Felaket Ustası'nı açar.

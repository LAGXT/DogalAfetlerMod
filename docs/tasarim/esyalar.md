# Eşyalar ve ilerleme — tasarım notları

Her Faz 3 parçası bu dosyaya kendi bölümünü ekler.

## Buz altı balıkçılık (madde 15)

### Gerçek olay

Buz balıkçılığında (ice fishing) balıkçı göl buzunu burgu ya da keski ile deler; delik genellikle 15–25 cm çaplıdır,
kenarı keskin ve yarı saydam buzdur, içinde koyu, durgun su görünür. Delik açık bırakılırsa birkaç dakikada yüzeyde
ince bir buz zarı ("skim ice") oluşur: önce kenarlardan iğne kristaller uzanır, zar ortaya doğru kapanır, balıkçı
kepçeyle kırar. Soğuk suda balık yavaştır, ısırma seyrektir; balıkçı uzun süre hareketsiz bekler ve asıl tehlike
hareketsizlikle gelen soğumadır (rüzgârlıkta çadır, ısıtıcı, kalın giysi). Göller ve kıyı sularında alabalık, morina,
kefal, ringa tutulur; ringa sürüleri kışın kıyıya yaklaşır.

### Minecraft'ta

- **Delik bloğu `buz_deligi`:** 3 piksel kalın buz çerçeve (kendi dokusu, ~%85 opak, beyaz çatlaklı), ortadaki 10×10
  açıklıkta su. Su modelde çizilmez: blok kaynak su sıvı durumu taşır, vanilla sıvı çizicisi biyom renkli, hareketli
  su yüzeyini delikte çizer. Böylece delik her biyomda göldeki suyla aynı renkte.
- **Yavaş yeniden donma:** ömrün son 45 saniyesinde (3 × 15 sn) açıklığın üstünde kenardan içeri büyüyen yarı saydam buz
  zarı (üç doku: kenar şeridi, yarısı, neredeyse kapalı), her aşamada hafif cam tıkırtısı ve kar tanesi parçacıkları;
  sonunda buz geri gelir (`glass_place`, buz kırıntısı parçacıkları). Açıkken arada bir kabarcık patlaması.
- **Açılış anı:** eğilerek kazma/balta ile sağ tık → cam kırılma + hafif sıçrama sesi, buz kırıntısı, su sıçraması ve kar
  tanesi parçacıkları; kol sallanır, alet 5 dayanıklılık kaybeder.
- **Bekleyiş:** olta deliğin suyunda vanilla gibi yüzer. Isırma 30–60 sn (Lure seviye başına 5 sn kısaltır); son 2 sn
  vanilla'nın "balık yaklaşıyor" evresi, sonra şapırtı ve şamandıranın batması. Normal buzda olta buzun üstünde yatar.
- **Balıklar:** beş tür 16×16, vanilla balıklarıyla aynı çapraz duruş; renk ve desenle ayrılır (alabalık pembe yan
  şerit + benekler, morina zeytin alacalı, somon koyu lacivert sırt + gümüş yan ve daha iri, kefal boyuna çizgili gri,
  ringa parlak mavi + beyaz kırağı kenarı ve parıltı). Pişmişler kızarmış kahve-altın, ızgara izli. Tooltip: çiğ "hafifçe
  serinletir" (camgöbeği), pişmiş "ısıtır (orta)" (altın), pişmiş ringa "daha çok ısıtır" (kırmızı).
- **Risk:** delikte oltası olan ve buz üstünde hareketsiz duran oyuncunun ortamına 20 sn'de 8 °C'ye çıkan soğuk eklenir;
  sıcaklık barı gözle görülür biçimde mavi tarafa kayar. Ateş, sıcak yiyecek ya da kalın giysi bunu dengeler.

### Vitrin anı

Donmuş gölün ortasında, gün batımında, kazmayla açılmış deliğin yakın planı: buz çerçevenin ortasında koyu, hafif
dalgalı su, yüzen kırmızı-beyaz şamandıra; balık ısırınca şapırtı ve kabarcıklar, oyuncunun elinde Donmuş Ringa.

### Ses

Yalnız vanilla sesler: açılış `glass_break` (perde 1,25) + `generic_splash` (kısık); donma aşamaları `glass_hit`
(perde 0,5→0,8); kapanış `glass_place` (perde 0,6); ısırma vanilla şamandıra şapırtısı. Yeni ses dosyası yok.

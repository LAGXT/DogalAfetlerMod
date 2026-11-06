# Özel eşyalar — tasarım notları

Her eşya işçisi bu dosyaya kendi bölümünü ekler.

## Sıcaklık yiyecekleri (madde 14, 7b, 15; parça `yiyecek`)

### Gerçek dünya

- **Kış içecekleri.** Salep, orkide yumrusu unuyla koyulaştırılmış, tarçınlı sıcak süttür; Türkiye'de kışın sokağa çıkmadan
  içilir. Sıcak çikolata ve tavşan/tavuk çorbası da "içten ısıtan" yiyeceklerdir: sıcak sıvı mideye inince çekirdeğe doğrudan
  ısı verir (anında his), sindirim ve kan dolaşımı bir süre daha ısı üretir (giderek azalan etki). Acılı, yoğun güveç
  terletir: soğukta çok iyi, sıcakta bunaltır.
- **Yaz içecekleri.** Ayran (tuzlu, soğuk yoğurt içeceği) ve şerbet (Osmanlı mutfağının kar/buzla soğutulmuş meyve şurubu)
  sıcak çöl ve yaz günlerinin içeceğidir; buzlu karpuz en hafif serinletici. Soğuk içecek donan birini daha çabuk dondurur.
- **Tekrar etkisi.** Aynı sıcak içeceği üst üste içmek ilk bardak kadar ısıtmaz (mide dolu, fark azalır); farklı bir yiyecek
  ayrı bir kaynaktır. Madde 14'ün azalan verimi bunu oyunlaştırır.

### Minecraft'ta teknik

- **Dokuz eşya** (`yiyecek.mantik.YiyecekTablosu`): sıcak — Sıcak Salep, Sıcak Çikolata (orta, 90 sn), Sıcak Tavuk Çorbası
  (büyük, 150 sn), Ateşli Güveç (çok büyük, 180 sn, yan etki), Isıtıcı Çay (hafif, 180 sn), Kutup Balığı Çorbası (büyük,
  150 sn); soğuk — Ayran (orta, 90 sn), Buzlu Karpuz Dilimi (hafif, 60 sn), Meyve Şerbeti (büyük, 120 sn). Yeme 32 tick,
  yığın 16, tok da yenir; şişe/kase boş kap olarak geri gelir. Tarifler madde 14'teki gibi şekilsiz.
- **Etki** (`YiyecekAPI.uygula`): kademe miktarı (config: hafif 8, orta 20, büyük 35, çok büyük 45 vücut birimi = ölçeğin
  yüzdesi) %25 anında, kalanı süre boyunca doğrusal azalan hızla `VucutSicakligi.etkiEkle` ile eklenir. Etki toplanır:
  sıcakken içilen salep aşırı ısınmaya, donarken içilen ayran donmaya iter ("zamanında yenmesi gerekir"). Ateşli Güveç
  vücut ya da ortam sıcakken ×1,5 ısıtır.
- **Azalan verim:** aynı eşya son yemeden 60 sn içinde tekrar yenirse anında ve süreli kısım yarıya iner; farklı eşyalar
  bağımsız; oyuncu başına, oyun saatiyle, oyuncu verisinde.
- **Durum efekti** "İçten Isınma" (turuncu alev ikonu) / "Serinlik" (buz mavisi kar tanesi): yalnız gösterge — seviye
  büyüklük kademesi (I–IV), süre etkinin süresi; sıcaklığı efekt değil sıcaklık sistemi uygular. Parçacık yok (ekranı
  kirletmez), süt silmez.
- **Vanilla pişmiş yiyecekler** (kızarmış patates, pişmiş etler/balıklar 30 sn; çorbalar 45 sn) çok küçük (3 birim)
  ısınma ve "İçten Isınma I" verir; config ile kapanır, listeye başka modların yiyecekleri eklenir.
- **Balıkçılık köprüsü:** pişmiş buz balığı orta ısınma, çiğ hafif serinlik (`YiyecekAPI.pismisBalik/cigBalik`); Kutup
  Balığı Çorbası `#dogalafetler:pismis_kutup_baligi` etiketindeki balıkla pişer.

### Görünüş ve ses

- **Dokular** (`tools/assets/yiyecek_dokular.py`, 16×16, vanilla iksir şişesi ve kase silueti, koyu kenar, sol üstten ışık):
  salep krem rengi sıvı ve yüzeyde tarçın, çikolata koyu kahve ve krema, çay kehribar kızılı ve kuru meyve; sıcakların
  şişe boynunun iki yanında kıvrılan buhar. Ayran köpüklü beyaz, şerbet pembe-kırmızı ve buz küpü; soğukların camda kırağı ve
  köşede buz parlaması. Kaseler: tavuk çorbası altın suyu, tavuk, havuç, maydanoz; güveç koyu kızıl, kor sarısı alev tozu
  ve kasenin üstünde alev dilleri; kutup balığı çorbası soluk buz mavisi suyu, somon tonlu ve beyaz balık, dereotu.
  Buzlu karpuz vanilla dilim siluetinde, kabuğun üstü ve kesik yüzü buz kristalli. Envanterde bir bakışta ısıtan (buhar,
  alev) ve serinleten (kırağı) ayrılır.
- **Efekt ikonları** 18×18: iç içe katmanlı alev (kızıl kenar → turuncu → sarı → açık çekirdek, kıvılcımlar) ve altı kollu,
  yan dallı kar tanesi (koyu mavi kenar, buz mavisi kollar, beyaz merkez).
- **Tooltip:** "İçten Isınma: orta, 90 sn" (altın) / "Serinlik: hafif, 60 sn" (su mavisi); Güveç'te kırmızı "Sıcakta hızla
  aşırı ısıtır".
- **Ses:** şişeler vanilla içme sesi ve animasyonu, kaseler ve karpuz yeme sesi; yeni ses dosyası yok.

### Vitrin anı

Kar fırtınasında donma kenarına gelmiş oyuncu buharlı bir Ateşli Güveç yer: XP barının üstündeki sıcaklık işaretçisi
anında sağa kayar, sağ üstte turuncu alev ikonu (İçten Isınma IV, 3:00) belirir, buz kenarı sonraki saniyelerde çözülür.
Aynı güveci çöl öğleninde yiyen arkadaşının barı kırmızıya taşar ve titrer.

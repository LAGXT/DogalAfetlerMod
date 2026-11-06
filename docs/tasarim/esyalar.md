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

## Hava Radarı ve Sismograf (madde 13a–b, N6, N7)

### Gerçek dünyadan

- **Hava radarı ekranı (PPI):** koyu yeşil fosforlu, merkezde verici, dönen tarama çizgisi ve arkasında sönen iz;
  hedef tarama geçerken parlayıp yavaşça sönen bir nokta. Mesafe halkaları ve kuzey referansı.
- **Sismograf (davul kayıtçı / helikorder):** dönen tambura sarılı krem kâğıt, ince mürekkep çizgisi, zaman işaretleri.
  Sakin zemin bile "mikrosismik" kıpırtı çizer; öncü depremler seyrek kısa iğneler, ana sarsıntı kâğıdı dolduran geniş
  salanımdır. Öncü sarsıntılar aralıklıdır, ana şoka yaklaştıkça sıklaşır.

### Oyunda

- **Radar paneli (elde tutulurken, sağ alt köşe):** küçük CRT kutusu. Solda 33 px'lik dairesel kap: iki mesafe halkası,
  artı ışını, saniyede bir tur atan tarama çizgisi ve 6 kademede sönen izi. Afet, oyuncunun bakışına göre (yukarı = ön)
  menzile oranlı uzaklıkta kırmızı bir nokta; tarama üstünden geçerken beyaz parlar. Sağda üç satır: afet adı, dönen ok +
  mesafe (m), "Varış m:ss" (ON_UYARI/SIREN) ya da "Kalan m:ss" (AKTIF). Afet yoksa "Gökyüzü sakin · Menzil 300 m".
- **Gökyüzü anomalisi (N7):** güneş patlaması/ay çarpması yalnız sirenden sonra; kap mora döner, içinde nabız gibi
  parlayan bir dolgu ve dışa yayılan halka, "Gökyüzü anomalisi · <afet> · Başlamasına m:ss". Yön ve mesafe yoktur.
- **Gelişmiş radar:** çerçeve mor, üst kenarda mavi ışık çizgisi; üst bölüm normal radarla aynı boyda. Altında 9 px'lik kısa
  satırlar: seviye adı (Hafif…Felaket, seviye rengiyle) ile hareket oku + hız (hareketli afette) ya da "Durağan" aynı satırda,
  "Son afetler" ve tek sütunda 5 kısa geçmiş satırı (`Kasırga S2 3dk`). Panel en çok 108 GUI px yüksekliğindedir: GUI
  ölçeği 2'de ekranın %30'unu geçmez.
- **Sismograf:** eşya hotbar'da/yan eldeyken öncü sarsıntıda her darbede kısık bir zemin gürlemesi (2,4 sn, 25–110 Hz,
  −27 LUFS, oyunda 0,08–0,30 hacım) ve 2 sn süren, en çok 0,3°'lik kamera titremesi; action bar'da (yalnız taşıyan)
  altun "Öncü sarsıntı algılandı · %45 · KD 240 m" (Hassas'ta "· Seviye: Şiddetli" kırmızı). AKTIF'te "Ana sarsıntı".
- **Sismograf ekranı (sağ tık):** koyu metal kasa, krem kayıt kâğıdı; ızgara ve dalga aynı hızla sola akar, kırmızı saniye
  işaretleri kenarlarda; sağda tamburdan uzanan kalem kolu şimdiki örneği lacivert mürekkeple yazar (Hassas'ta mor eşya
  dokusuyla uyumlu). Algılama varken kırmızı lamba yanıp söner. "60 sn önce" … "şimdi".
- **Dokular (16×16):** radar demir gövdeli, yeşil ekranlı el cihazı (tarama çizgisi, kırmızı nokta, bakır tuşlar, anten);
  sismograf taş kaide üstünde kâğıt sarılı tambur, çapraz tüy kalem ve mürekkep izi. Gelişmiş/Hassas sürümler mor-mavi
  çerçeve, cam göbeği ekran/mor mürekkep ve mor bir "çekirdek taşı".

### Vitrin anı

Deprem S3 ON_UYARI'nın erken bölümü: Hassas Sismograf taşıyan oyuncunun ekranı çok hafif kayıyor, altın action bar
"Öncü sarsıntı algılandı · %58 · GD 110 m · Seviye: Şiddetli" yazıyor, eldeki normal radar hâlâ "Gökyüzü sakin" diyor;
yarım dakika sonra radarın taraması kırmızı noktayı yakalıyor ve geri sayım başlıyor. Siren henüz yok.

### Ses

Tek dosya `esya_oncu_sarsinti.ogg` (`tools/assets/esya_radar_ses.py`): taban gürlemesi, 3–7 Hz kayan titreşim zarfı, çok
düşük çakıl çıtırtısı; konuşma bandı −40 dB (sesli sohbeti bastırmaz). Kategori `AMBIENT`, yalnız taşıyanın istemcisinde.

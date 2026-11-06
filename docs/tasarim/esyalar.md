# Eşyalar — tasarım notu

## Meteor, meteor ödülleri ve Termostat (madde 13c, N3, N4; parça `meteor-termostat`)

### Gerçek olay

- **Atmosfere giriş:** göktaşı saniyede 11–72 km hızla girer; önündeki hava sıkışıp ısınır ve iyonlaşır (plazma
  kabuğu), arkada ışıklı bir iz ve kalan duman/toz şeridi bırakır. Büyük cisimler (bolid) parlak beyaz-sarı, kırmızı-turuncu
  çekirdekli; magnezyum/demir ağırlıklı olanlar mavi-yeşile çalan izler verir.
- **Çarpma:** basit krater çanak biçimlidir, derinliği çapın ~1/5–1/3'ü; kenarda yükselmiş ve dışa atılmış enkaz (ejecta)
  halkası, tabanda eriyik ve şokça değişmiş kaya; çarpmayı parlama, yere yayılan şok/toz halkası ve sarsıntı izler.
- **Meteorit:** yere ulaşan parça dıştan koyu, erimiş bir kabukla (füzyon kabuğu) kaplı, içi gözenekli/metal damarlıdır; çarpmadan
  hemen sonra sıcaktır ama dakikalar içinde soğur.

Kaynaklar: https://www.amnh.org/exhibitions/permanent/meteorites (meteorit türleri, füzyon kabuğu),
https://www.lpi.usra.edu/exploration/training/illustrations/craterLandforms/ (basit krater, ejecta, derinlik/çap),
https://www.amsmeteors.org/fireballs/faqf/ (bolid renkleri, iz, ses).

### Minecraft'ta gösterim

- **Meteor varlığı** (`MeteorVarligi`, sunucu otoriteli): 40–70° açıyla, hedefin 140 blok üstünden 3 blok/tick düz
  inen dönen kaya kümesi (güneş: magma + karataş/bazalt; ay: kalsit + pürüzsüz bazalt/tüf), önünde additive plazma
  kabuğu ve ışık halesi, arkasında iki katmanlı additive iz şeridi (40 / 72 blok; güneş: beyaz-sarı çekirdek + turuncu,
  ay: mavi-beyaz çekirdek + mavi) ve 8 sn'de dağılan gri duman, düşen kıvılcımlar. İnişe 1,5 sn kala zeminde büyüyen
  turuncu/mavi parlama diski oyuncuya düşüş yerini gösterir. İz, kabuk ve disk sissiz çizilir (gece görüş sisi
  uzaktan gelen izi söndürmesin); 96 blok ötesinde blok kümesi gövde yerine yalnız plazma. Işık motoruna dokunulmaz. 192 blok ötesinde parçacık yok,
  yalnız şerit.
- **Çarpma:** beyaz parlama (uzaklıkla azalan, 5 tick; dünyanın üstünde, elin ve GUI'nin altında tam ekran, F1 karelerinde de görünür), mesafeyle karesel azalan ekran sarsıntısı (oyuncuya özgü paket),
  0,8 sn'de 4×çapa yayılan toz halkası, zeminin blok parçacıklarıyla fışkıran enkaz, eriyik kaya kıvılcımları, duman,
  derin güm. Suya düşen meteor sıçrama + buhar, krater/ateş/ödül yok.
- **Krater** (`KraterProfili`): paraboloit çanak (küçük r 2,5 / derinlik 2, büyük r 5,5 / 4), üstte 2 blok bitki/kar
  temizliği; tabanın ortası magma (20 sn "sıcak" süresi boyunca parlar, sonra karataş/bazalta soğur), dışı koyu kaya;
  kenarda %55 yoğunlukla enkaz halkası (karataş, bazalt, kaba toprak; ayda pürüzsüz bazalt, tüf, kalsit). Sert bloklar
  (`dogalafetler:deprem_dayanikli`, `yanmaz`, kırılmaz, blok varlıklı) ve sıvılar korunur; damla yok. `afetBlokHasari=false`
  iken krater yok. Krater kenarı + 3 blok ortak yangınla tutuşur (yoğunluk 0,45 / 0,85), krater 30 sn ortamı ısıtır.
- **Ödüller:** Meteorit Parçası (koyu gözenekli kaya + turuncu kor damarları, altın parıltı; sıcakken alev ve duman),
  Kozmik Çekirdek (mor-mavi kristal, büyü parlaması, mor-mavi yıldız parıltısı ve büyü harfleri). Meteorit Külçesi
  (koyu demir-mavi, kıvılcım benekli), Termostat Plakası (bakır-altın plaka, altın devre izleri, kızıltaş çip).
- **Termostatlı zırh:** sıcaklığın `zirh_eki` tarif tipiyle (N3); her parça vücudu nötre 0,04 birim/sn çeker, |E| > 40'tan
  sonra azalır, 120'de %25 tabanda kalır (N4). İpucu satırı "Termostatlı".

### Vitrin anı

Gece, orman kıyısında ağaçların üstünden doğuya bakan kamera: sağ üstten dalan dört meteorun mavi-beyaz ve turuncu
izleri gökyüzünü çaprazlar, ilk çarpmanın parlaması ve toz halkası, ardından ağaçlar arasında magma tabanlı krater ve
yayılan yangın. Küçük resim: ilk çarpmadan 6 tick sonra (izler hâlâ gökyüzünde, kıvılcım ve enkaz havada).

### Ses

Uçuşta rüzgâr ıslığı döngüsü meteoru izler, perde yaklaşırken 1,2, uzaklaşırken 0,8 (Doppler), ay parçaları %10 tiz;
çarpmada alçak perdeli derin patlama (büyükte 160 blok, küçükte 80 blok duyulur), suda sıçrama + cızgırtı; sıcak parça
suya/kara değince cız. Kendi sentez dosyası yok: vanilla sesler `SoundEvents` ile (kural: tek seferlik darbe
`level.playSound`).

# Özel eşyalar — tasarım notları

Her eşya işçisi bu dosyaya kendi bölümünü ekler.

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

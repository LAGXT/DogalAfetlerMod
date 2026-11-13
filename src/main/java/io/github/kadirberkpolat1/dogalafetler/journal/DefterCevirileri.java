package io.github.kadirberkpolat1.dogalafetler.journal;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/**
 * Afet Defteri'nin dil metinleri (datagen, İngilizce + Türkçe): eşya adı ve ipucu, ekran başlıkları, istatistik etiketleri ve
 * rehber sayfaları. Anahtarlar {@value #ON} önekiyle başlar; afet adları ve seviye adları çekirdeğin mevcut anahtarlarıdır
 * ({@code afet.dogalafetler.*}, {@code seviye.dogalafetler.*}). Rehberin sıcaklık eşikleri sunucudan gelir ve metne
 * {@code %s} olarak girer.
 */
public final class DefterCevirileri {
    public static final String ON = "defter.dogalafetler.";
    /** Afet türü ipucu anahtarı: {@code defter.dogalafetler.rehber.ipucu.<kimlik>}. */
    public static final String IPUCU_ON = ON + "rehber.ipucu.";
    /** Seviye açıklaması anahtarı: {@code defter.dogalafetler.rehber.seviye.<1-5>}. */
    public static final String SEVIYE_ON = ON + "rehber.seviye.";

    private DefterCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        y.ekle("item.dogalafetler.afet_defteri", "Disaster Journal", "Afet Defteri");
        y.ekle("item.dogalafetler.afet_defteri.ipucu",
                "Right-click to open. Records the disasters you survived, your catches, distance travelled and days survived.",
                "Sağ tık: açar. Atlattığın afetleri, tuttuklarını, kat ettiğin mesafeyi ve hayatta kaldığın günleri kaydeder.");

        y.ekle(ON + "baslik", "Disaster Journal", "Afet Defteri");
        y.ekle(ON + "sekme.genel", "General", "Genel");
        y.ekle(ON + "sekme.afetler", "Disasters", "Afetler");
        y.ekle(ON + "sekme.esyalar", "Catches", "Balık/Eşya");
        y.ekle(ON + "sekme.rehber", "Guide", "Rehber");
        y.ekle(ON + "sayfa", "Page %s/%s", "Sayfa %s/%s");
        y.ekle(ON + "onceki", "Previous page", "Önceki sayfa");
        y.ekle(ON + "sonraki", "Next page", "Sonraki sayfa");

        y.ekle(ON + "gun", "%s days", "%s gün");
        y.ekle(ON + "gun.bir", "1 day", "1 gün");
        y.ekle(ON + "gun.no", "Day %s", "Gün %s");
        y.ekle(ON + "mesafe.m", "%s m", "%s m");
        y.ekle(ON + "mesafe.km", "%s km", "%s km");
        y.ekle(ON + "derece", "%s °C", "%s °C");
        y.ekle(ON + "yok", "-", "-");
        y.ekle(ON + "adet", "×%s", "×%s");

        y.ekle(ON + "genel.yasam", "Survival", "Hayatta Kalma");
        y.ekle(ON + "genel.gun_simdi", "This life", "Bu yaşam");
        y.ekle(ON + "genel.gun_en", "Longest life", "En uzun yaşam");
        y.ekle(ON + "genel.dunya", "World day", "Dünya günü");
        y.ekle(ON + "genel.olum", "Deaths", "Ölüm sayısı");
        y.ekle(ON + "genel.mesafe", "Distance Travelled", "Kat Edilen Mesafe");
        y.ekle(ON + "mesafe.toplam", "Total", "Toplam");
        y.ekle(ON + "mesafe.kara", "On land", "Karada");
        y.ekle(ON + "mesafe.su", "In water", "Suda");
        y.ekle(ON + "mesafe.hava", "In the air", "Havada");
        y.ekle(ON + "mesafe.tasit", "Vehicles", "Taşıt ve binek");
        y.ekle(ON + "genel.afetler", "Disasters", "Afetler");
        y.ekle(ON + "afet.atlatilan", "Survived", "Atlatılan");
        y.ekle(ON + "afet.karsilasilan", "Faced", "Karşılaşılan");
        y.ekle(ON + "afet.en_yuksek", "Peak level", "En yüksek");
        y.ekle(ON + "afet.en_yuksek_tur", "Peak disaster", "Afet türü");
        y.ekle(ON + "afet.olum", "Deaths in disasters", "Afette ölüm");
        y.ekle(ON + "afet.dalga", "Tsunami waves", "Tsunami dalgası");
        y.ekle(ON + "genel.sicaklik", "Body Temperature", "Vücut Sıcaklığı");
        y.ekle(ON + "sicaklik.soguk", "Coldest", "En soğuk");
        y.ekle(ON + "sicaklik.sicak", "Hottest", "En sıcak");
        y.ekle(ON + "genel.meteor", "Meteors and Ice", "Meteor ve Buz");
        y.ekle(ON + "meteor.parca", "Meteorite shards", "Meteorit parçası");
        y.ekle(ON + "meteor.cekirdek", "Cosmic cores", "Kozmik çekirdek");
        y.ekle(ON + "buz.delik", "Ice holes", "Açılan buz deliği");

        y.ekle(ON + "afetler.seviye", "Survived by level", "Seviyeye göre atlatılan");
        y.ekle(ON + "afetler.ilk", "First encounter", "İlk karşılaşma");
        y.ekle(ON + "afetler.ilk_gun", "Game day", "Oyun günü");
        y.ekle(ON + "afetler.ilk_tarih", "Date", "Tarih");
        y.ekle(ON + "afetler.ilk_yok", "Not encountered yet", "Henüz karşılaşılmadı");
        y.ekle(ON + "afetler.son", "Last 5 Disasters", "Son 5 Afet");
        y.ekle(ON + "afetler.son_bos", "You have not survived a disaster yet.", "Henüz bir afet atlatmadın.");
        y.ekle(ON + "afetler.son_seviyeli", "%s · Level %s", "%s · Seviye %s");

        y.ekle(ON + "esyalar.baslik", "Fishing", "Balıkçılık");
        y.ekle(ON + "esyalar.olta", "Rod catches", "Olta tutuşu");
        y.ekle(ON + "esyalar.buz_tutus", "Ice-hole pulls", "Buz deliği çekişi");
        y.ekle(ON + "esyalar.buz_balik", "Ice fish", "Buz balığı");
        y.ekle(ON + "esyalar.balik", "Fish caught", "Tutulan balık");
        y.ekle(ON + "esyalar.esya", "Other items", "Diğer eşya");
        y.ekle(ON + "esyalar.balik_liste", "Fish", "Balıklar");
        y.ekle(ON + "esyalar.esya_liste", "Treasure and junk", "Hazine ve çöp");
        y.ekle(ON + "esyalar.bos", "You have not caught anything yet.", "Henüz bir şey tutmadın.");

        y.ekle(ON + "rehber.seviyeler", "Disaster Levels", "Afet Seviyeleri");
        y.ekle(SEVIYE_ON + "1", "A small area and a short time. Most players get through it unharmed.",
                "Dar alan, kısa süre. Çoğu oyuncu hasarsız atlatır.");
        y.ekle(SEVIYE_ON + "2", "A wider area; weak blocks get damaged.", "Daha geniş alan; zayıf bloklar zarar görür.");
        y.ekle(SEVIYE_ON + "3", "Even sturdy blocks suffer. Keep a shelter and an escape plan.",
                "Sağlam bloklar da zarar görür. Sığınağın ve kaçış planın olsun.");
        y.ekle(SEVIYE_ON + "4", "A huge area and heavy destruction. Move away early.",
                "Çok geniş alan ve ağır yıkım. Erkenden uzaklaş.");
        y.ekle(SEVIYE_ON + "5", "Reshapes the landscape. Surviving means getting out of its way.",
                "Araziyi yeniden biçimlendirir. Hayatta kalmak, yolundan çekilmekle olur.");
        y.ekle(ON + "rehber.seviye_not",
                "Higher levels unlock as game days pass and are rarer. Solar flare and moon impact have no levels.",
                "Yüksek seviyeler oyun günü ilerledikçe açılır ve daha nadirdir. Güneş patlaması ile ay çarpmasının seviyesi yoktur.");

        y.ekle(ON + "rehber.ne_yapmali", "What to Do", "Ne Yapmalı");
        y.ekle(IPUCU_ON + AfetTuru.KASIRGA.kimlik(),
                "Run sideways to the funnel's path, never toward it. Get underground or behind solid blocks; the pull grows stronger the closer you are.",
                "Huninin yoluna dik yönde uzaklaş, asla ona doğru koşma. Yer altına ya da sağlam blokların arkasına geç; yaklaştıkça çekim güçlenir.");
        y.ekle(IPUCU_ON + AfetTuru.TSUNAMI.kimlik(),
                "Leave the coast at the first siren and climb to high ground. The sea pulls back before the wave arrives; do not follow it.",
                "İlk sirende kıyıdan uzaklaş, yüksek zemine çık. Dalgadan önce deniz çekilir; çekilen denizin peşine düşme.");
        y.ekle(IPUCU_ON + AfetTuru.DEPREM.kimlik(),
                "Get into the open, away from tall blocks and trees. Stay clear of cracks; aftershocks follow at higher levels. A Seismograph warns of foreshocks.",
                "Açık alana çık; yüksek yapılardan ve ağaçlardan uzak dur. Yarıklara yaklaşma; yüksek seviyede artçı sarsıntılar gelir. Sismograf öncü sarsıntıyı haber verir.");
        y.ekle(IPUCU_ON + AfetTuru.SINKHOLE.kimlik(),
                "Cracking and trembling come before the collapse: leave the area at once and keep away from the edge. A Seismograph shows the foreshocks.",
                "Çökmeden önce çatlama ve titreme gelir: alanı hemen terk et, çukurun kenarından uzak dur. Sismograf öncü sarsıntıyı gösterir.");
        y.ekle(IPUCU_ON + AfetTuru.ASIT_YAGMURU.kimlik(),
                "Take cover under a roof: acid hurts anyone with an open sky above and slowly eats soft blocks.",
                "Çatı altına gir: asit, başı açık herkese hasar verir ve yumuşak blokları yavaş yavaş aşındırır.");
        y.ekle(IPUCU_ON + AfetTuru.ORMAN_YANGINI.kimlik(),
                "Move away from forests and anything flammable. Water and rain stop the spread.",
                "Ormandan ve yanıcı her şeyden uzaklaş. Su ve yağmur yangının yayılmasını durdurur.");
        y.ekle(IPUCU_ON + AfetTuru.GUNES_PATLAMASI.kimlik(),
                "The world scorches and temperatures soar: go underground, carry cooling food and an Ice Bucket, and stock up on supplies.",
                "Dünya kavrulur ve sıcaklık çok yükselir: yer altına in, serinletici yiyecek ve Buz Kovası taşı, erzak biriktir.");
        y.ekle(IPUCU_ON + AfetTuru.AY_CARPMASI.kimlik(),
                "Moon fragments rain for a minute or two: find a roof. Afterwards the sea rises for good and tsunamis grow frequent: stay away from the coast.",
                "1–2 dakika ay parçaları yağar: çatı altına gir. Sonrasında deniz kalıcı olarak yükselir ve tsunamiler sıklaşır: kıyıdan uzak dur.");
        y.ekle(IPUCU_ON + AfetTuru.SEL.kimlik(),
                "Seas and lakes rise and drown low ground; rain, storms, tsunamis and the moon impact make floods likelier. Climb to high ground and keep clear of the water; fire, lava, campfires, furnaces and torches evaporate flood water around them.",
                "Deniz ve göller yükselip alçak yerleri sular altında bırakır; yağmur, fırtına, tsunami ve ay çarpması seli olası kılar. Yüksek zemine çık, sudan uzak dur; ateş, lav, kamp ateşi, fırın ve meşale çevrelerindeki sel suyunu buharlaştırır.");

        y.ekle(ON + "rehber.sicaklik", "Temperature Guide", "Sıcaklık Rehberi");
        y.ekle(ON + "rehber.sicaklik_giris",
                "Your body temperature is shown in °C and is 37.0 °C when normal. The thermometer menu (K by default) shows the details.",
                "Vücut sıcaklığın °C ile gösterilir; normal değer 37,0 °C. Termometre menüsü (varsayılan K) ayrıntıları gösterir.");
        y.ekle(ON + "rehber.sicaklik_usume", "Chill: %s °C and below", "Üşüme: %s °C ve altı");
        y.ekle(ON + "rehber.sicaklik_donma", "Freezing: %s °C and below, damage below %s °C",
                "Donma: %s °C ve altı, %s °C altında hasar");
        y.ekle(ON + "rehber.sicaklik_bunalma", "Overheating: %s °C and above, extra hunger",
                "Bunalma: %s °C ve üstü, ek açlık");
        y.ekle(ON + "rehber.sicaklik_yanma", "Burning: %s °C and above, damage above %s °C",
                "Yanma: %s °C ve üstü, %s °C üstünde hasar");
        y.ekle(ON + "rehber.sicaklik_ipucu1", "Water makes you wet and cools you faster; dry off under a roof.",
                "Su seni ıslatır ve daha hızlı üşütür; çatı altında kurursun.");
        y.ekle(ON + "rehber.sicaklik_ipucu2",
                "Hot soups and drinks warm you, cold ones cool you. An Ice Bucket in the offhand cools; a lava bucket warms.",
                "Sıcak çorba ve içecekler ısıtır, soğuk olanlar serinletir. Sol eldeki Buz Kovası serinletir, lav kovası ısıtır.");
        y.ekle(ON + "rehber.sicaklik_ipucu3",
                "Wool armor insulates, feathers help balance, and a Thermostat Plate reduces overheating.",
                "Yünlü zırh yalıtır, tüy işlemek dengeler, Termostat Plakası aşırı ısınmayı azaltır.");
    }
}

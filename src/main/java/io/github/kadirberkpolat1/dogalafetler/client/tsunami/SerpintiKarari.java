package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

/**
 * Dalga serpintisi ve köpüğünün bir noktada doğuş kararı (saf; dünya sorguları {@link SerpintiNoktasi}'nda). Yükseklik
 * yalnız bu karede ekranda çizilen su yüzeyinden alınır (AF3-10, denetim P1-1): yakın tampon noktayı kapsıyorsa onun su
 * üstü (orada su çizilmiyorsa NaN), kapsamıyorsa uzak LOD'un o noktada çizdiği karo (çizmiyorsa NaN). Analitik dalga
 * ({@code Dalga.yuzey}: düz zemin varsayar) ve sanal su seviyesi ({@code SanalSu.suSeviyesi}: ağın henüz çizmediği suyu da
 * bilir) girdi değildir: yakın ağ boş ya da gecikmişken (yavaş makinede dalga oyuncuya yeni gelirken) kameranın
 * çevresinde hiçbir ağ su çizmez ve serpinti doğmaz; bilinçli sonuç: yavaş makinede az serpinti.
 */
final class SerpintiKarari {
    /**
     * Parçacık görünen su üstünden en çok bu kadar yüksekte doğar: çizilen ağ dalgacıksız ham yüzeyin çevresinde ±0,1 blok
     * dalgalanır, parçacık ve köpük yüzeyden en çok 0,2 blok kalkar; yarım bloğun altındaki fark gözle "havada" değildir.
     */
    static final double HAVA_PAYI = 0.5;

    /** Karar: doğar ya da neden doğmadığı. */
    enum Sonuc {
        DOGAR,
        /** Noktada bu karede çizilen su yüzeyi yok (ağ boş, gecikmiş ya da sütun kuru). */
        CIZIMSIZ,
        /** Nokta çarpışma şekli olan bir bloğun hücresinde. */
        BLOK_ICINDE,
        /** Nokta sütunun {@code MOTION_BLOCKING} tepesinin altında: çatı, saçak, ağaç ya da mağara tavanı altı. */
        CATI_ALTI,
        /** Nokta görünen su üstünden {@link #HAVA_PAYI}'ndan yüksekte. */
        HAVADA
    }

    private SerpintiKarari() {
    }

    /**
     * Noktanın altındaki, bu karede çizilen su üstü: yakın tampon kapsıyorsa yalnız onun su üstü (uzak LOD'a düşmez),
     * değilse uzak LOD'un çizdiği karo; ikisi de çizmiyorsa NaN.
     */
    static double yuzey(boolean yakinKapsar, double yakinSuUstu, double uzakSuUstu) {
        return yakinKapsar ? yakinSuUstu : uzakSuUstu;
    }

    /**
     * {@code y} yüksekliğindeki aday nokta için karar. {@code cizilen} {@link #yuzey}'in sonucu, {@code tepe} sütunun
     * {@code MOTION_BLOCKING} yükseklik haritası (ilk hareket engellemeyen hücre), {@code kati} noktanın hücresindeki bloğun
     * çarpışma şeklinin boş olmaması. Sıra: çizilen su yoksa, sonra blok, çatı altı ve havada.
     */
    static Sonuc karar(double y, double cizilen, int tepe, boolean kati) {
        if (Double.isNaN(cizilen)) {
            return Sonuc.CIZIMSIZ;
        }
        if (kati) {
            return Sonuc.BLOK_ICINDE;
        }
        if (y < tepe) {
            return Sonuc.CATI_ALTI;
        }
        return y - cizilen > HAVA_PAYI ? Sonuc.HAVADA : Sonuc.DOGAR;
    }
}

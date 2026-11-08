package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

/**
 * Dalga serpintisi ve köpüğünün bir noktada doğuş kararı (saf; dünya sorguları {@link SerpintiNoktasi}'nda). Yükseklik
 * analitik dalga yüzeyinden ({@code Dalga.yuzey}: düz zemin varsayar, blok ve çizilen ağ bilmez; yavaş makinede ağ dalgayı
 * 7–14 sn geriden çizerken serpinti kuru arazide patlıyordu) alınmaz, görünen sudan alınır: yakın tampon noktayı kapsıyorsa
 * bu karede çizilen su üstü ({@code cizilen}; orada su çizilmiyorsa NaN), kapsamıyorsa araziyi ve seli bilen sanal su
 * seviyesi ({@code seviye}, {@code SanalSu.suSeviyesi}; su yoksa NaN).
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
        /** Yakın tampon noktayı kapsıyor ama orada su çizilmiyor (ağ dalgayı henüz çizmedi ya da sütun kuru). */
        CIZIMSIZ,
        /** Yakın tampon dışında ve sütunda sanal su yok. */
        SUSUZ,
        /** Nokta çarpışma şekli olan bir bloğun hücresinde. */
        BLOK_ICINDE,
        /** Nokta sütunun {@code MOTION_BLOCKING} tepesinin altında: çatı, saçak, ağaç ya da mağara tavanı altı. */
        CATI_ALTI,
        /** Nokta görünen su üstünden {@link #HAVA_PAYI}'ndan yüksekte. */
        HAVADA
    }

    private SerpintiKarari() {
    }

    /** Noktanın altındaki görünen su üstü: kapsanan noktada yalnız çizilen su (analitik seviyeye düşmez), değilse seviye. */
    static double yuzey(boolean kapsanan, double cizilen, double seviye) {
        return kapsanan ? cizilen : seviye;
    }

    /**
     * {@code y} yüksekliğindeki aday nokta için karar. {@code tepe} sütunun {@code MOTION_BLOCKING} yükseklik haritası (ilk
     * hareket engellemeyen hücre), {@code kati} noktanın hücresindeki bloğun çarpışma şeklinin boş olmaması. Sıra: su yoksa
     * (çizimsiz ya da susuz), sonra blok, çatı altı ve havada.
     */
    static Sonuc karar(double y, double cizilen, boolean kapsanan, double seviye, int tepe, boolean kati) {
        double ust = yuzey(kapsanan, cizilen, seviye);
        if (Double.isNaN(ust)) {
            return kapsanan ? Sonuc.CIZIMSIZ : Sonuc.SUSUZ;
        }
        if (kati) {
            return Sonuc.BLOK_ICINDE;
        }
        if (y < tepe) {
            return Sonuc.CATI_ALTI;
        }
        return y - ust > HAVA_PAYI ? Sonuc.HAVADA : Sonuc.DOGAR;
    }
}

package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

/**
 * Saniyede bir gelen sunucu değerini akıcı ve gecikmesiz gösteren istemci yumuşatıcısı (K-E74). Gecikmenin kökü iki
 * şeydi: sunucu değeri 1 Hz hesaplayıp yalnız belirli eşiklerin üstündeki değişimi yolluyordu ve istemci gösterdiği
 * değeri bir sonraki pakete kadar sabit hedefe yaklaştırıyordu; yani ekran hep en az bir paket aralığı geriden gelirdi.
 * Burada her paket bir <em>hedef</em> ve bir <em>hız</em> getirir: hedef paketteki değer, hız ya sunucunun bildirdiği eğilim
 * ya da ardışık iki paketin farkıdır. Paketten bu yana geçen süre kadar ileri tahmin edilen değere ({@link #ONGORU_TICK}
 * ile sınırlı) gösterilen değer her tick üstel olarak yaklaşır, kare arasında {@code partialTick} ile ara değerlenir.
 * Ek paket ya da bant genişliği gerekmez. Minecraft'a bağlı değildir.
 */
final class AkanDeger {
    /** İleri tahminin en çok süresi (tick): sunucunun varsayılan 20 tick'lik güncelleme aralığı ve biraz pay. */
    static final int ONGORU_TICK = 25;
    /** Hızı iki paketin farkından kestirmek için paketler arası en az ve en çok süre (tick). */
    static final int EN_AZ_ARALIK = 10;
    static final int EN_COK_ARALIK = 45;

    /** İleri tahminin değer birimindeki üst sınırı: ani sıçramada (ışınlanma, boyut) fazla savrulmasın. */
    private final float enCokOngoru;
    private float onceki;
    private float simdi;
    private float hedef;
    /** Hedefe doğru tick başına hız. */
    private float hiz;
    private long hedefTick;
    private float oncekiPaket;
    private long oncekiPaketTick = Long.MIN_VALUE;

    AkanDeger(float enCokOngoru) {
        this.enCokOngoru = enCokOngoru;
    }

    /** Gösterilen değeri doğrudan yerleştirir (ilk paket, kapanış, çıkış). */
    void sifirla(float deger, long tick) {
        onceki = simdi = hedef = oncekiPaket = deger;
        hiz = 0f;
        hedefTick = oncekiPaketTick = tick;
    }

    /** Yeni sunucu değeri; hız ardışık paketlerin farkından kestirilir. */
    void paket(float deger, long tick) {
        long aralik = tick - oncekiPaketTick;
        if (oncekiPaketTick == Long.MIN_VALUE) {
            hiz = 0f;
        } else if (aralik >= EN_AZ_ARALIK) {
            hiz = aralik <= EN_COK_ARALIK ? (deger - oncekiPaket) / aralik : 0f;
        } else {
            // Hemen ardından gelen ek paket (komut, yiyecek): hız tahmini bozulmasın, yalnız hedef değişir.
            hedef = deger;
            hedefTick = tick;
            return;
        }
        konumla(deger, tick);
        oncekiPaket = deger;
        oncekiPaketTick = tick;
    }

    /** Yeni sunucu değeri ve sunucunun bildirdiği hız (tick başına değer). */
    void paket(float deger, long tick, float tickBasinaHiz) {
        hiz = tickBasinaHiz;
        konumla(deger, tick);
    }

    private void konumla(float deger, long tick) {
        hedef = deger;
        hedefTick = tick;
    }

    /** O tick için ileri tahminli hedef. */
    float hedef(long tick) {
        float sure = Math.min(Math.max(tick - hedefTick, 0L), ONGORU_TICK);
        float ileri = Math.max(-enCokOngoru, Math.min(enCokOngoru, hiz * sure));
        return hedef + ileri;
    }

    /** İstemci tick'i: gösterilen değer hedefe {@code yaklasma} oranında yaklaşır. */
    void adim(long tick, float yaklasma) {
        onceki = simdi;
        simdi += (hedef(tick) - simdi) * yaklasma;
    }

    float goster(float partialTick) {
        return onceki + (simdi - onceki) * partialTick;
    }
}

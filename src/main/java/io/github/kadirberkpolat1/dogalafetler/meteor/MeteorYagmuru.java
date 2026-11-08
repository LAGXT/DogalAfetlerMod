package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorDagitimi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Süreli meteor yağmuru: oyuncu başına dakikada {@code dakikaBasina} meteor, sırayla oyunculara dağıtılır ({@link
 * MeteorDagitimi}), her biri oyuncunun {@code dagitimIcYaricap}–{@code dagitimDisYaricap} halkasına, bakış yönüne
 * ağırlıklı düşer ({@code meteor.bakisYonuOrani}; MT2-28: yağmur göründüğü yerde yoğundur, arkada da düşer). Oyuncu
 * kümesi boşsa boyuttaki bütün (izleyici olmayan) oyuncular her tick yeniden okunur; hiç oyuncu yoksa dünya doğuş
 * noktası tek hedeftir (oyuncusuz sunucu, yönsüz). Yoğunluk çalışırken değişebilir ({@link #yogunlukAyarla}; birikim korunur,
 * düşük yoğunlukta da kesir kaybolmaz). Kaydedilmez: afet yüklenince yeniden başlatır.
 */
final class MeteorYagmuru {
    /** Oyuncusuz sunucuda doğuş noktası hedefinin ödül tavanı kimliği. */
    static final UUID DOGUS_HEDEFI = new UUID(0L, 0L);
    final int kimlik;
    final YanginKaynagi kaynak;
    private final Set<UUID> oyuncular;
    private double dakikaBasina;
    private final double buyukOran;
    private final MeteorGorunumu gorunum;
    private final MeteorDagitimi dagitim;
    private final Random rastgele;
    private int kalanTick;
    private double birikim;
    /** Atmaya kalkışılan meteor (tavan ya da yüklü olmayan chunk yüzünden uçmayan dahil). */
    private int atilan;
    /** Test: sabit dağıtım halkası (− 1: config), giriş açısı (NaN: rastgele) ve ödül şansları (null: config). */
    private int icYaricap = -1;
    private int disYaricap = -1;
    private double aci = Double.NaN;
    private OdulZari.Sanslar sanslar;

    MeteorYagmuru(int kimlik, Set<UUID> oyuncular, int sureTick, double dakikaBasina, double buyukOran,
            MeteorGorunumu gorunum, YanginKaynagi kaynak, long tohum) {
        this.kimlik = kimlik;
        this.oyuncular = Set.copyOf(oyuncular);
        this.kalanTick = sureTick;
        this.dakikaBasina = Math.max(0, dakikaBasina);
        this.buyukOran = Math.max(0, Math.min(1, buyukOran));
        this.kaynak = kaynak;
        this.gorunum = gorunum;
        this.rastgele = new Random(tohum);
        this.dagitim = new MeteorDagitimi(rastgele.nextInt(1024));
    }

    int kalanTick() {
        return kalanTick;
    }

    double yogunluk() {
        return dakikaBasina;
    }

    int atilan() {
        return atilan;
    }

    void yogunlukAyarla(double yeni) {
        dakikaBasina = Math.max(0, yeni);
    }

    MeteorYagmuru testAyari(int ic, int dis, double girisAcisi, OdulZari.Sanslar odulSanslari) {
        icYaricap = ic;
        disYaricap = dis;
        aci = girisAcisi;
        sanslar = odulSanslari;
        return this;
    }

    /** Bir tick ilerler; bitince {@code false}. */
    boolean tick(ServerLevel level) {
        if (kalanTick-- <= 0) {
            return false;
        }
        List<ServerPlayer> hedefler = hedefler(level);
        int n = Math.max(1, hedefler.size());
        birikim += n * dakikaBasina / 1200.0;
        int ic = icYaricap >= 0 ? icYaricap : MeteorAyarlari.dagitimIcYaricap();
        int dis = disYaricap >= 0 ? disYaricap : MeteorAyarlari.dagitimDisYaricap();
        while (birikim >= 1) {
            birikim -= 1;
            atilan++;
            ServerPlayer o = hedefler.isEmpty() ? null : hedefler.get(dagitim.siradaki(n));
            BlockPos merkez = o == null ? level.getSharedSpawnPos() : o.blockPosition();
            double[] ofset = o == null ? MeteorDagitimi.ofset(rastgele, ic, dis) : MeteorDagitimi.ofset(rastgele, ic, dis,
                    MeteorDagitimi.bakisAcisi(o.getYRot()), MeteorAyarlari.bakisYonuOrani());
            BlockPos hedef = merkez.offset((int) Math.round(ofset[0]), 0, (int) Math.round(ofset[1]));
            double olcek = MeteorOlcegi.ornekle(rastgele, buyukOran, MeteorAyarlari.ortaMeteorOrani());
            double a = Double.isNaN(aci) ? MeteorAyarlari.girisAcisiEnAz()
                    + rastgele.nextDouble() * Math.max(0, MeteorAyarlari.girisAcisiEnCok() - MeteorAyarlari.girisAcisiEnAz())
                    : aci;
            MeteorAPI.dusur(level, hedef, olcek, gorunum, kaynak, true, a, rastgele.nextDouble() * 2 * Math.PI, sanslar,
                    o == null ? DOGUS_HEDEFI : o.getUUID());
        }
        return true;
    }

    /** Hedef oyuncular UUID sırasıyla (boşsa doğuş noktası tek hedeftir, ödül tavanı {@link #DOGUS_HEDEFI}'ne sayılır). */
    private List<ServerPlayer> hedefler(ServerLevel level) {
        List<ServerPlayer> secilen = new ArrayList<>();
        for (ServerPlayer o : level.players()) {
            if (!o.isSpectator() && o.isAlive() && (oyuncular.isEmpty() || oyuncular.contains(o.getUUID()))) {
                secilen.add(o);
            }
        }
        secilen.sort(Comparator.comparing(ServerPlayer::getUUID));
        return secilen;
    }
}

package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorDagitimi;
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
 * MeteorDagitimi}), her biri oyuncunun {@code dagitimIcYaricap}–{@code dagitimDisYaricap} halkasına düşer. Oyuncu
 * kümesi boşsa boyuttaki bütün (izleyici olmayan) oyuncular her tick yeniden okunur; hiç oyuncu yoksa dünya doğuş
 * noktası tek hedeftir (oyuncusuz sunucu). Kaydedilmez: afet yüklenince yeniden başlatır.
 */
final class MeteorYagmuru {
    final int kimlik;
    final YanginKaynagi kaynak;
    private final Set<UUID> oyuncular;
    private final double dakikaBasina;
    private final double buyukOran;
    private final MeteorGorunumu gorunum;
    private final MeteorDagitimi dagitim;
    private final Random rastgele;
    private int kalanTick;
    private double birikim;

    MeteorYagmuru(int kimlik, Set<UUID> oyuncular, int sureTick, double dakikaBasina, double buyukOran,
            YanginKaynagi kaynak, long tohum) {
        this.kimlik = kimlik;
        this.oyuncular = Set.copyOf(oyuncular);
        this.kalanTick = sureTick;
        this.dakikaBasina = Math.max(0, dakikaBasina);
        this.buyukOran = Math.max(0, Math.min(1, buyukOran));
        this.kaynak = kaynak;
        this.gorunum = MeteorGorunumu.kaynaktan(kaynak);
        this.rastgele = new Random(tohum);
        this.dagitim = new MeteorDagitimi(rastgele.nextInt(1024));
    }

    int kalanTick() {
        return kalanTick;
    }

    /** Bir tick ilerler; bitince {@code false}. */
    boolean tick(ServerLevel level) {
        if (kalanTick-- <= 0) {
            return false;
        }
        List<BlockPos> merkezler = merkezler(level);
        birikim += merkezler.size() * dakikaBasina / 1200.0;
        int ic = MeteorAyarlari.dagitimIcYaricap();
        int dis = MeteorAyarlari.dagitimDisYaricap();
        while (birikim >= 1) {
            birikim -= 1;
            BlockPos merkez = merkezler.get(dagitim.siradaki(merkezler.size()));
            double[] o = MeteorDagitimi.ofset(rastgele, ic, dis);
            BlockPos hedef = merkez.offset((int) Math.round(o[0]), 0, (int) Math.round(o[1]));
            MeteorBoyutu boyut = rastgele.nextDouble() < buyukOran ? MeteorBoyutu.BUYUK : MeteorBoyutu.KUCUK;
            MeteorAPI.dusur(level, hedef, boyut, gorunum, kaynak, true);
        }
        return true;
    }

    private List<BlockPos> merkezler(ServerLevel level) {
        List<ServerPlayer> secilen = new ArrayList<>();
        for (ServerPlayer o : level.players()) {
            if (!o.isSpectator() && o.isAlive() && (oyuncular.isEmpty() || oyuncular.contains(o.getUUID()))) {
                secilen.add(o);
            }
        }
        secilen.sort(Comparator.comparing(ServerPlayer::getUUID));
        List<BlockPos> sonuc = new ArrayList<>(secilen.size());
        for (ServerPlayer o : secilen) {
            sonuc.add(o.blockPosition());
        }
        if (sonuc.isEmpty()) {
            sonuc.add(level.getSharedSpawnPos());
        }
        return sonuc;
    }
}

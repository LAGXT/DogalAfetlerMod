package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Cekim;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kasırga çekiminin bir varlığa uygulanması (AF3-01): hareketin sahibi uygular. Oyuncuyu kendi istemcisi her tick iter
 * ({@code client.kasirga.KasirgaOyuncuCekimi}, senkronlanan {@link KasirgaVarligi} verisiyle); mob, eşya ve aracı sunucu
 * ({@link Kasirga}). İki taraf da aynı {@link Cekim} hesabını ve aynı sürtünme tahminini kullanır.
 */
public final class KasirgaCekimi {
    /**
     * Bir kasırganın çekim girdisi.
     *
     * @param merkez kasırga ekseninin zemin noktası
     * @param carpan evre gücü × {@code cekimCarpani}
     * @param tavan  kaldırma tavanı (blok, kasırga zemininden)
     */
    public record Kaynak(Vec3 merkez, KasirgaSeviyesi seviye, double carpan, double tavan) {
    }

    private KasirgaCekimi() {
    }

    /** Yaratıcı/izleyici oyuncu, binekteki, asılı (tablo, çerçeve), fiziksiz ve kasırganın kendi varlıkları çekilmez. */
    public static boolean cekilebilir(Entity e) {
        if (e instanceof KasirgaVarligi || e instanceof UcanBlokVarligi || e instanceof HangingEntity || !e.isAlive()
                || e.isSpectator() || e.isPassenger() || e.noPhysics) {
            return false;
        }
        return !(e instanceof Player p) || !p.isCreative() && !p.getAbilities().flying;
    }

    /**
     * Hareketinin sahibi bir oyuncunun istemcisi: oyuncunun kendisi ya da oyuncunun sürdüğü araç (tekne, at). Bunların çekimini
     * istemci uygular ({@code client.kasirga.KasirgaOyuncuCekimi}); sunucunun hızı istemcinin bir sonraki hareket paketiyle
     * ezilirdi (AF3-01, denetim P2-4).
     */
    public static boolean istemciYonetir(Entity e) {
        return e instanceof Player || e.getControllingPassenger() instanceof Player;
    }

    /** Varlığa bu tick eklenecek hız {vx, vy, vz}; {@code vy > 0} kaldırmanın etkidiği anlamına gelir. */
    public static double[] ivme(Entity e, Kaynak k) {
        KasirgaSeviyesi s = k.seviye();
        Vec3 v = e.getDeltaMovement();
        return Cekim.ivme(e.getX() - k.merkez().x, e.getZ() - k.merkez().z, e.getY() - k.merkez().y, v.x, v.z,
                s.tabanYaricapi(), s.cekimYaricapi(), s.guc(), k.carpan(), k.tavan(), surtunme(e));
    }

    /**
     * Vanilla'nın bu tick hıza uygulayacağı yatay sürtünme: sıvıda 0,8; havada canlı 0,91, eşya 0,98; yerde altındaki
     * bloğun sürtünmesi × (canlı 0,91, eşya 0,98).
     */
    public static double surtunme(Entity e) {
        boolean esya = e instanceof ItemEntity;
        if (e.isInWater() || e.isInLava()) {
            return 0.8;
        }
        if (!e.onGround()) {
            return esya ? 0.98 : Cekim.HAVA_SURTUNMESI;
        }
        BlockPos alt = BlockPos.containing(e.getX(), e.getBoundingBox().minY - 0.5000001, e.getZ());
        return e.level().getBlockState(alt).getFriction(e.level(), alt, e) * (esya ? 0.98 : 0.91);
    }

    /**
     * {@code ivme}'yi varlığın hızına ekler ve tavanlara kırpar ({@link Cekim#kirp(double[], double[])}: tavan yalnız çekimin
     * eklediğini sınırlar, zaten daha hızlı olan elytra ya da itilen varlığı yavaşlatmaz); kaldırma etkiliyken düşme mesafesi
     * sıfırlanır (huni varlığı taşırken alçaltması düşme sayılmaz; kaldırma bitince vanilla düşme hasarı işler). Sonuç
     * sıfırsa dokunmaz.
     *
     * @return hız değişti mi
     */
    public static boolean uygula(Entity e, double[] ivme) {
        if (ivme[0] == 0 && ivme[1] == 0 && ivme[2] == 0) {
            return false;
        }
        Vec3 v = e.getDeltaMovement();
        double[] yeni = Cekim.kirp(new double[] {v.x + ivme[0], v.y + ivme[1], v.z + ivme[2]}, new double[] {v.x, v.y, v.z});
        e.setDeltaMovement(yeni[0], yeni[1], yeni[2]);
        if (ivme[1] > 0) {
            e.resetFallDistance();
        }
        return true;
    }
}

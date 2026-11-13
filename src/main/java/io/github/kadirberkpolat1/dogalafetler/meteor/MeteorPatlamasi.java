package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Çarpma anının varlık etkisi: {@code meteor.patlamaYaricapi}'ndaki canlılar merkezden uzaklığa göre doğrusal azalan hasar
 * ({@code meteor.patlamaHasari}, patlama hasar türü: zirh, büyü ve Kozmik zirh bunu azaltır), merkezden dışarı itme ve
 * yanıcılık alır; büyük bantta ayrıca kısa bir baş dönmesi. Blok yıkımı {@link KraterIsi}'nindir ve {@code
 * afetBlokHasari}'na bağlıdır; bu sınıf blok değiştirmez, canlı hasarı kuralından bağımsızdır. En çok {@value #EN_COK_VARLIK}
 * canlı işlenir (en yakın sırası gerekmez; tek çarpmada sınırlı iş).
 */
final class MeteorPatlamasi {
    static final int EN_COK_VARLIK = 128;
    /** Bu orandan (1 − mesafe/yarıçap) uzak canlılar yanmaz. */
    private static final double YANMA_ESIGI = 0.25;
    private static final double BASDONMESI_ESIGI = 0.35;

    private MeteorPatlamasi() {
    }

    /** Etkilenen canlı sayısını döndürür. */
    static int uygula(ServerLevel level, Vec3 nokta, double olcek) {
        double yaricap = MeteorAyarlari.patlamaYaricapi(olcek);
        double enCokHasar = MeteorAyarlari.patlamaHasari(olcek);
        if (yaricap <= 0) {
            return 0;
        }
        double itme = MeteorAyarlari.patlamaItmesi();
        boolean buyuk = MeteorOlcegi.buyuk(olcek);
        List<LivingEntity> hedefler = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(nokta, nokta).inflate(yaricap), e -> e.isAlive() && !e.isSpectator());
        int sayi = 0;
        for (LivingEntity e : hedefler) {
            if (sayi >= EN_COK_VARLIK) {
                break;
            }
            Vec3 merkez = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double mesafe = merkez.distanceTo(nokta);
            if (mesafe >= yaricap) {
                continue;
            }
            double f = 1 - mesafe / yaricap;
            sayi++;
            if (f > YANMA_ESIGI && !e.fireImmune()) {
                e.setSecondsOnFire(Mth.ceil(2 + 8 * f));
            }
            if (buyuk && f > BASDONMESI_ESIGI) {
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) (60 + 100 * f), 0, false, false));
            }
            itele(e, merkez, nokta, f, itme);
            float hasar = (float) (enCokHasar * f);
            if (hasar > 0.01f) {
                e.hurt(level.damageSources().explosion(null, null), hasar);
            }
        }
        return sayi;
    }

    private static void itele(LivingEntity e, Vec3 merkez, Vec3 nokta, double f, double itme) {
        if (itme <= 0) {
            return;
        }
        double dx = merkez.x - nokta.x;
        double dz = merkez.z - nokta.z;
        double uzunluk = Math.sqrt(dx * dx + dz * dz);
        if (uzunluk < 1e-3) {
            double aci = e.getRandom().nextDouble() * 2 * Math.PI;
            dx = Math.cos(aci);
            dz = Math.sin(aci);
            uzunluk = 1;
        }
        double direnc = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
        double guc = itme * f * direnc;
        e.push(dx / uzunluk * guc * 1.2, (0.25 + 0.55 * f) * Math.min(1.0, itme) * direnc, dz / uzunluk * guc * 1.2);
        e.hurtMarked = true;
    }
}

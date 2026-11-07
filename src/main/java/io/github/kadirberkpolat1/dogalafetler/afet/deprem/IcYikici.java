package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.IcYikimSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.ortak.AfetBloklari;
import io.github.kadirberkpolat1.dogalafetler.afet.ortak.mantik.BlokRolu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Depremin iç yıkımı (AF-D2…AF-D5): ana sarsıntı boyunca tick bütçesiyle sınırlı sütun taraması. Sütunların yarısı etki
 * alanındaki bir oyuncunun çevresinden (evin içi: oyuncunun {@value #OYUNCU_UST} blok üstünden {@value #OYUNCU_ALT} blok
 * altına), yarısı alanın merkeze yoğunlaşan rastgele sütunlarından (yüzeyden aşağı {@value IcYikimSeviyesi#SUTUN_DERINLIGI}
 * blok) seçilir; sütun başına en çok {@value #SUTUN_ISABETI} etki. S2+: {@code #afet_dekor} blokları (kapı, kapak, çit ve
 * çit kapısı hariç) eşya olarak düşer, saniyede bir oyuncuların çevresindeki eşya çerçevesi ve tablolar düşer; S3+: yaprak
 * dökülür (kırılma parçacığı, {@link IcYikimSeviyesi#YAPRAK_ESYA_ORANI} olasılıkla eşya); S4+: altı hava olan zayıf blok
 * ({@link #zayifTavan}) düşen blok olur. {@code afetBlokHasari} kuralını çağıran denetler; blok değişimleri
 * {@link BlokIsleri} üzerinden sanal suya bildirilir.
 */
final class IcYikici {
    private static final int OYUNCU_YARICAPI = 12;
    private static final int OYUNCU_UST = 10;
    private static final int OYUNCU_ALT = 6;
    private static final int SUTUN_ISABETI = 2;
    private static final int ASILI_ARALIGI = 20;

    private final BlokIsleri isler;
    private final RandomSource rastgele;
    private final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos alti = new BlockPos.MutableBlockPos();
    private int asiliSayac;
    private int tavanKalan;
    private int dekor;
    private int yaprak;
    private int tavan;
    private int asili;

    IcYikici(BlokIsleri isler, RandomSource rastgele) {
        this.isler = isler;
        this.rastgele = rastgele;
    }

    /** Özet (log): düşen dekor, dökülen yaprak, düşen tavan, düşen çerçeve/tablo. */
    String ozet() {
        return "dekor " + dekor + ", yaprak " + yaprak + ", tavan " + tavan + ", asılı " + asili;
    }

    /** Bir tick; {@code kisma}: TPS kademesi × adım (ana yıkımla aynı). */
    void tick(IcYikimSeviyesi s, BlockPos merkez, double yaricap, double kisma, int adim) {
        if (s.bos() || adim <= 0) {
            return;
        }
        int ayar = DepremAyarlari.icYikimButcesi();
        double beklenen = s.beklenenSutun(ayar, kisma);
        int n = (int) beklenen + (rastgele.nextDouble() < beklenen - (int) beklenen ? 1 : 0);
        tavanKalan = s.tavanSiniri(ayar);
        int alandaki = alandakiOyuncu(merkez, yaricap);
        for (int k = 0; k < n; k++) {
            ServerPlayer o = alandaki > 0 && k % 2 == 0 ? oyuncu(merkez, yaricap, rastgele.nextInt(alandaki)) : null;
            if (o != null) {
                int x = o.getBlockX() + rastgele.nextInt(2 * OYUNCU_YARICAPI + 1) - OYUNCU_YARICAPI;
                int z = o.getBlockZ() + rastgele.nextInt(2 * OYUNCU_YARICAPI + 1) - OYUNCU_YARICAPI;
                int ust = isler.ust(x, z);
                if (ust != Integer.MIN_VALUE) {
                    sutun(s, x, z, Math.min(ust, o.getBlockY() + OYUNCU_UST), o.getBlockY() - OYUNCU_ALT, false);
                }
                continue;
            }
            double d = yaricap * Math.sqrt(rastgele.nextDouble());
            double a = rastgele.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(merkez.getX() + 0.5 + Math.cos(a) * d);
            int z = (int) Math.floor(merkez.getZ() + 0.5 + Math.sin(a) * d);
            int ust = isler.ust(x, z);
            if (ust != Integer.MIN_VALUE) {
                sutun(s, x, z, ust, ust - IcYikimSeviyesi.SUTUN_DERINLIGI + 1, false);
            }
        }
        asiliSayac += adim;
        if (s.dekor() && alandaki > 0 && asiliSayac >= ASILI_ARALIGI) {
            asiliSayac = 0;
            int butce = Math.max(1, (int) Math.ceil(s.beklenenSutun(ayar, kisma)));
            for (int i = 0; i < alandaki; i++) {
                ServerPlayer o = oyuncu(merkez, yaricap, i);
                if (o != null) {
                    asililar(s, o.getBoundingBox().inflate(OYUNCU_YARICAPI, OYUNCU_ALT, OYUNCU_YARICAPI), butce, false);
                }
            }
        }
    }

    /**
     * Tek sütun, {@code ust}'ten {@code alt}'a (dahil) yukarıdan aşağı; etki sayısını döner. {@code kesin}: olasılıklar atlanır
     * ve tavan sınırı bu çağrı için yenilenir (yalnız GameTest kancası); seviye eşikleri her zaman geçerlidir.
     */
    int sutun(IcYikimSeviyesi s, int x, int z, int ust, int alt, boolean kesin) {
        if (kesin) {
            tavanKalan = s.tavanSiniri(DepremAyarlari.icYikimButcesi());
        }
        ServerLevel level = isler.level();
        int etki = 0;
        for (int y = ust; y >= alt && etki < SUTUN_ISABETI; y--) {
            BlockState st = level.getBlockState(p.set(x, y, z));
            if (st.isAir()) {
                continue;
            }
            if (AfetBloklari.rol(level, p, st) == BlokRolu.DEKOR) {
                if (s.dekor() && !AfetBloklari.yapiDekoru(st) && sans(s.dekorOlasiligi(), kesin)
                        && isler.dok(p.immutable(), true)) {
                    dekor++;
                    etki++;
                }
            } else if (st.is(BlockTags.LEAVES)) {
                if (s.yaprak() && sans(s.yaprakOlasiligi(), kesin)
                        && isler.dok(p.immutable(), rastgele.nextDouble() < IcYikimSeviyesi.YAPRAK_ESYA_ORANI)) {
                    yaprak++;
                    etki++;
                }
            } else if (tavanKalan > 0 && zayifTavan(level, st, s) && sans(s.tavanOlasiligi(), kesin)) {
                Vec3 hiz = new Vec3((rastgele.nextDouble() - 0.5) * 0.04, 0, (rastgele.nextDouble() - 0.5) * 0.04);
                if (isler.dusur(p.immutable(), hiz, DepremAyarlari.dusenBlokTavani())) {
                    tavanKalan--;
                    tavan++;
                    etki++;
                }
            }
        }
        return etki;
    }

    /** Kutudaki eşya çerçeveleri ve tablolar (tasma düğümü değil): en çok {@code butce} tanesi eşyalarıyla düşer. */
    int asililar(IcYikimSeviyesi s, AABB kutu, int butce, boolean kesin) {
        if (!s.dekor()) {
            return 0;
        }
        int n = 0;
        for (HangingEntity e : isler.level().getEntitiesOfClass(HangingEntity.class, kutu,
                h -> (h instanceof ItemFrame || h instanceof Painting) && h.isAlive() && !h.isInvulnerable())) {
            if (n >= butce) {
                break;
            }
            if (sans(s.dekorOlasiligi(), kesin)) {
                e.kill();
                e.dropItem(null);
                n++;
                asili++;
            }
        }
        return n;
    }

    /**
     * AF-D3 zayıf tavan: altı hava; blok varlığı, iki parçalı blok (kapı, yatak, uzun bitki), {@code deprem_dayanikli}, yaprak,
     * cam ve cam panel (düşen blok olarak sağlam inerdi; fay hattında kırılgan olarak kırılır) ve çarpışmasız blok (sarmaşık,
     * sarkan kök, spor çiçeği: tavan değil) değil; {@code deprem_kirilgan} etiketinde ya da sertliği seviye eşiğinin altında ve
     * kazma/kürek/çapa ile kazılmayan (yün, ışık taşı, mantar bloğu; doğal zemin — taş, toprak, kum, buz, kar, yosun, sculk,
     * pişmiş kil — hep bu aletlerle kazılır, mağara tavanı çökmez).
     */
    private boolean zayifTavan(ServerLevel level, BlockState st, IcYikimSeviyesi s) {
        if (!s.tavan() || st.hasBlockEntity() || st.is(DepremEtiketleri.DAYANIKLI)
                || st.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || st.hasProperty(BlockStateProperties.BED_PART)
                || !st.getFluidState().isEmpty() || st.is(BlockTags.IMPERMEABLE) || st.is(Tags.Blocks.GLASS_PANES)
                || st.getCollisionShape(level, p).isEmpty()) {
            return false;
        }
        boolean zayif = st.is(DepremEtiketleri.KIRILGAN) || (s.zayifSertlik(st.getDestroySpeed(level, p))
                && !st.is(BlockTags.MINEABLE_WITH_PICKAXE) && !st.is(BlockTags.MINEABLE_WITH_SHOVEL)
                && !st.is(BlockTags.MINEABLE_WITH_HOE));
        return zayif && level.getBlockState(alti.setWithOffset(p, Direction.DOWN)).isAir();
    }

    private boolean sans(double olasilik, boolean kesin) {
        return kesin || rastgele.nextDouble() < olasilik;
    }

    /** Etki alanındaki (yatay) izleyici olmayan oyuncu sayısı; sıradaki oyuncu {@link #oyuncu} ile alınır (liste kurulmaz). */
    private int alandakiOyuncu(BlockPos merkez, double yaricap) {
        int n = 0;
        for (ServerPlayer o : isler.level().players()) {
            if (alanda(o, merkez, yaricap)) {
                n++;
            }
        }
        return n;
    }

    private ServerPlayer oyuncu(BlockPos merkez, double yaricap, int sira) {
        int n = 0;
        for (ServerPlayer o : isler.level().players()) {
            if (alanda(o, merkez, yaricap) && n++ == sira) {
                return o;
            }
        }
        return null;
    }

    private static boolean alanda(ServerPlayer o, BlockPos merkez, double yaricap) {
        double dx = o.getX() - merkez.getX() - 0.5;
        double dz = o.getZ() - merkez.getZ() - 0.5;
        return !o.isSpectator() && dx * dx + dz * dz <= yaricap * yaricap;
    }
}

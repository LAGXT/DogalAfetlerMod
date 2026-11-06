package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.ArtciPlani;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.DepremSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.FayHatti;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Deprem (bölüm C): AKTIF aşamada fay hattı ({@link FayHatti}) merkezden iki uca 7–12 blok/sn hızla açılır. Cephenin önünde
 * yüzeyde çatlak örtüsü büyür; açılan her sütunda toz fışkırır, kaya kırılır, sütun tick bütçesiyle yukarıdan aşağı
 * boşaltılır ve sütunların beşte birinde yüzey bloğu yarığa düşer (dipteki moloz). Etki yarıçapında {@code deprem_kirilgan}
 * bloklar yerinden kopar (yaprak kırılır, diğerleri düşer), S4–5'te dik toprak/kum/çakıl yamaları kayar ve yarığa yakın
 * canlılar hasar alır. {@code deprem_dayanikli} ve diğer bloklar yalnız yarık hattında kırılır. {@code afetBlokHasari=false} iken
 * hiçbir blok değişmez (örtü, toz, ses ve sarsıntı sürer). Ana sarsıntı bitince yıkım ve hasar durur; S3+'te AKTIF aşama
 * artçı sarsıntılar ({@link ArtciPlani}) bitene kadar sürer: süren artçının sırası {@code altAsama()}'dır (değişimi durum paketini
 * hemen gönderir), artçı blok yıkmaz; kısa gürleme ve yarıktan toz. Ekran sarsıntısı istemcidedir ({@code client.deprem}).
 */
public final class Deprem extends Afet {
    static final String SES_KATMANI = "deprem/gurleme";
    static final String ARTCI_KATMANI = "deprem/artci";
    private static final int ORTU_TABANI = -1_000_000;
    private static final double ORTU_ONU = 3;
    private static final int TUR_YARIK = 0;
    private static final int TUR_YAN = 1;
    private static final int TUR_BASAMAK = 2;

    private record Is(FayHatti.Hucre h, int tur) {
    }

    private static final class Sutun {
        final int sira;
        final int x;
        final int z;
        final int zemin;
        final int taban;
        final boolean dusur;
        int y;

        Sutun(int sira, int x, int z, int ust, int zemin, int taban, boolean dusur) {
            this.sira = sira;
            this.x = x;
            this.z = z;
            this.zemin = zemin;
            this.taban = taban;
            this.dusur = dusur;
            this.y = ust;
        }
    }

    private final BlockPos sabitMerkez;
    private final RandomSource rastgele = RandomSource.create();
    private long tohum;
    private double aci;
    private long gecen;
    private int acilan;
    private int hasarSayaci;
    private boolean tamamYazildi;
    private boolean anaSesBitti;
    private List<ArtciPlani.Artci> artcilar;
    private final BitSet tamam = new BitSet();
    private final List<Sutun> aktif = new ArrayList<>();
    private int[] kayitliAktif = new int[0];
    private DepremSeviyesi seviyeBilgisi;
    private FayHatti.Plan plan;
    private List<Is> sira;
    private BlokIsleri isler;

    public Deprem() {
        this(null);
    }

    private Deprem(BlockPos sabitMerkez) {
        super(AfetTuru.DEPREM);
        this.sabitMerkez = sabitMerkez;
    }

    /** Yalnız GameTest: merkezi hedef oyuncudan bağımsız sabit deprem. */
    static Deprem sabitMerkezli(BlockPos merkez) {
        return new Deprem(merkez.immutable());
    }

    DepremSeviyesi seviyeBilgisi() {
        if (seviyeBilgisi == null) {
            seviyeBilgisi = DepremAyarlari.seviye(seviye());
        }
        return seviyeBilgisi;
    }

    /** Yarık planı (AKTIF'te kurulur; test). */
    FayHatti.Plan plan() {
        return plan;
    }

    BlokIsleri isler() {
        return isler;
    }

    /** Artçı planı (tohum ve seviyeden; kayıttan dönüşte aynısı). */
    List<ArtciPlani.Artci> artcilar() {
        if (artcilar == null) {
            artcilar = DepremAyarlari.artciPlani(seviyeBilgisi(), tohum);
        }
        return artcilar;
    }

    /** Fay doğrultusu (radyan, yatay XZ düzleminde); görsel otomasyon kamerayı hatta göre koyar. */
    public double fayAcisi() {
        return aci;
    }

    boolean kazmaBitti() {
        return sira != null && acilan >= sira.size() && aktif.isEmpty();
    }

    @Override
    public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, RandomSource r) {
        if (sabitMerkez != null) {
            return sabitMerkez;
        }
        double a = r.nextDouble() * Math.PI * 2;
        double d = 6 + 2 * seviye;
        int x = (int) Math.floor(hedef.getX() + Math.cos(a) * d);
        int z = (int) Math.floor(hedef.getZ() + Math.sin(a) * d);
        int y = level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) : hedef.getBlockY();
        return new BlockPos(x, y, z);
    }

    @Override
    public EtkiAlani etkiAlani() {
        BlockPos m = baglam().merkez();
        DepremSeviyesi s = seviyeBilgisi();
        return EtkiAlani.daire(m.getX() + 0.5, m.getY(), m.getZ() + 0.5,
                Math.max(s.yikimYaricapi(), s.uzunluk() / 2.0 + 8));
    }

    @Override
    protected void baslarken(ServerLevel level) {
        tohum = level.random.nextLong();
        aci = level.random.nextDouble() * Math.PI;
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF) {
            sesiBaslat(level);
        } else if (yeni == AfetAsamasi.BITIS) {
            OrtamSesi.durdur(level.getServer(), SES_KATMANI, 60);
            OrtamSesi.durdur(level.getServer(), ARTCI_KATMANI, 30);
            if (isler != null) {
                isler.ortuleriKaldir();
            }
        }
    }

    @Override
    protected void yuklendi(ServerLevel level) {
        if (asama() != AfetAsamasi.AKTIF) {
            return;
        }
        if (gecen < seviyeBilgisi().sureTick()) {
            sesiBaslat(level);
        } else {
            anaSesBitti = true;
        }
        if (altAsama() > 0 && altAsama() <= artcilar().size()) {
            artciSesi(level, artcilar().get(altAsama() - 1));
        }
    }

    private void sesiBaslat(ServerLevel level) {
        BlockPos m = baglam().merkez();
        OrtamSesi.baslat(level.getServer(), SES_KATMANI, baglam().baslangicSaati(), DepremSistemi.GURLEME_ADI,
                SoundSource.BLOCKS, OrtamSesi.Yer.nokta(level.dimension(), Vec3.atCenterOf(m),
                        (float) (etkiAlani().yaricap() + 64)), HacimEgrisi.giris(30, 1f));
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        if (isler == null) {
            hazirla(level);
        }
        gecen += adim;
        DepremSeviyesi s = seviyeBilgisi();
        boolean anaSarsinti = gecen < s.sureTick();
        if (!anaSarsinti && !anaSesBitti) {
            anaSesBitti = true;
            OrtamSesi.durdur(level.getServer(), SES_KATMANI, 60);
        }
        int artci = ArtciPlani.suren(artcilar(), gecen);
        if (artci != altAsama()) {
            artciDegisti(level, artci);
        }
        double cephe = s.acilmaHizi() * gecen / 20.0;
        boolean hasar = Kurallar.blokHasariAcik(level.getGameRules());
        int sesler = 0;
        while (acilan < sira.size() && sira.get(acilan).h().yol() <= cephe) {
            int i = acilan++;
            isler.ortuKaldir(i);
            Is is = sira.get(i);
            if (is.tur() == TUR_YARIK && sesler < 2 && i % 3 == 0) {
                sesler++;
                FayHatti.Hucre h = is.h();
                int y = isler.ust(h.x(), h.z());
                level.playSound(null, h.x() + 0.5, y, h.z() + 0.5,
                        y < 0 ? SoundEvents.DEEPSLATE_BREAK : SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0f,
                        0.6f + rastgele.nextFloat() * 0.2f);
            }
            toz(level, is);
            if (hasar && !tamam.get(i)) {
                ac(i, is);
            }
        }
        for (int i = acilan; i < sira.size() && i < acilan + 96; i++) {
            FayHatti.Hucre h = sira.get(i).h();
            if (h.yol() > cephe + ORTU_ONU) {
                break;
            }
            int y = isler.ust(h.x(), h.z());
            if (y != Integer.MIN_VALUE) {
                isler.ortu(i, new BlockPos(h.x(), y, h.z()), (int) Math.max(0, Math.min(9, 9 * (1 - (h.yol() - cephe) / ORTU_ONU))));
            }
        }
        if (hasar && adim > 0) {
            double kisma = TpsOlcer.kismaCarpani() * Math.min(4, adim);
            kaz((int) Math.max(1, DepremAyarlari.blokButcesi(s.seviye()) * kisma));
            if (anaSarsinti) {
                yik(s, kisma);
                if (s.heyelan()) {
                    heyelan(s, kisma);
                }
            }
        }
        if (!tamamYazildi && kazmaBitti()) {
            tamamYazildi = true;
            DogalAfetler.LOGGER.info("[deprem] yarık tamam: seviye {}, {} sütun, değişen blok {}, merkez {}", s.seviye(),
                    sira.size(), isler.degisenBlok(), baglam().merkez().toShortString());
        }
        hasarSayaci += adim;
        if (hasarSayaci >= 40) {
            hasarSayaci -= 40;
            if (s.hasar() > 0 && anaSarsinti) {
                hasarVer(level, s);
            }
        }
        isler.bildir();
    }

    /** Süren artçı değişti (0: artçı yok): kısa gürleme katmanı ve yarık boyunca toz; blok değişmez. */
    private void artciDegisti(ServerLevel level, int yeni) {
        if (altAsama() > 0) {
            OrtamSesi.durdur(level.getServer(), ARTCI_KATMANI, 30);
        }
        altAsamaAyarla(yeni);
        if (yeni == 0) {
            return;
        }
        ArtciPlani.Artci a = artcilar().get(yeni - 1);
        artciSesi(level, a);
        DogalAfetler.LOGGER.info("[deprem] artçı sarsıntı {}/{}: şiddet %{}, süre {} tick, merkez {}", yeni, artcilar().size(),
                Math.round(a.kesir() * 100), a.sure(), baglam().merkez().toShortString());
        List<FayHatti.Hucre> yarik = plan.yarik();
        for (int k = 0; k < 16 && !yarik.isEmpty(); k++) {
            FayHatti.Hucre h = yarik.get(rastgele.nextInt(yarik.size()));
            int y = isler.ust(h.x(), h.z());
            if (y != Integer.MIN_VALUE) {
                isler.parcacik(ParticleTypes.CAMPFIRE_COSY_SMOKE, h.x() + 0.5, y + 1, h.z() + 0.5, 2, 0.4, 0.02);
                isler.parcacik(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState()), h.x() + 0.5,
                        y + 1, h.z() + 0.5, 4, 0.3, 0.15);
            }
        }
    }

    private void artciSesi(ServerLevel level, ArtciPlani.Artci a) {
        int sure = Math.max(4, a.sure());
        float hacim = (float) Math.max(0.2, Math.min(1, 0.4 + a.kesir()));
        OrtamSesi.baslat(level.getServer(), ARTCI_KATMANI, baglam().baslangicSaati(), DepremSistemi.GURLEME_ADI,
                SoundSource.BLOCKS, OrtamSesi.Yer.nokta(level.dimension(), Vec3.atCenterOf(baglam().merkez()),
                        (float) (etkiAlani().yaricap() + 32)),
                HacimEgrisi.of(new HacimEgrisi.Nokta(0, 0f), new HacimEgrisi.Nokta(sure / 4, hacim),
                        new HacimEgrisi.Nokta(sure / 2, hacim), new HacimEgrisi.Nokta(sure, 0f)));
    }

    private void hazirla(ServerLevel level) {
        DepremSeviyesi s = seviyeBilgisi();
        BlockPos m = baglam().merkez();
        plan = FayHatti.olustur(m.getX(), m.getZ(), aci, s, tohum);
        List<Is> liste = new ArrayList<>(plan.toplam());
        plan.yarik().forEach(h -> liste.add(new Is(h, TUR_YARIK)));
        plan.yan().forEach(h -> liste.add(new Is(h, TUR_YAN)));
        plan.basamak().forEach(h -> liste.add(new Is(h, TUR_BASAMAK)));
        liste.sort(Comparator.comparingDouble((Is is) -> is.h().yol()).thenComparingInt(Is::tur));
        sira = List.copyOf(liste);
        isler = new BlokIsleri(level, ORTU_TABANI);
        for (int k = 0; k + 3 < kayitliAktif.length; k += 4) {
            int i = kayitliAktif[k];
            if (i >= 0 && i < sira.size()) {
                FayHatti.Hucre h = sira.get(i).h();
                aktif.add(new Sutun(i, h.x(), h.z(), isler.ust(h.x(), h.z()), kayitliAktif[k + 2], kayitliAktif[k + 1],
                        kayitliAktif[k + 3] != 0));
            }
        }
        kayitliAktif = new int[0];
    }

    private void toz(ServerLevel level, Is is) {
        FayHatti.Hucre h = is.h();
        int y = isler.ust(h.x(), h.z());
        if (y == Integer.MIN_VALUE) {
            return;
        }
        BlockState yuzey = level.getBlockState(new BlockPos(h.x(), y, h.z()));
        if (yuzey.isAir()) {
            yuzey = Blocks.DIRT.defaultBlockState();
        }
        int adet = is.tur() == TUR_YARIK ? 8 : 3;
        isler.parcacik(new BlockParticleOption(ParticleTypes.BLOCK, yuzey), h.x() + 0.5, y + 1.1, h.z() + 0.5, adet, 0.3,
                0.25);
        if (is.tur() == TUR_YARIK && rastgele.nextInt(4) == 0) {
            isler.parcacik(ParticleTypes.CAMPFIRE_COSY_SMOKE, h.x() + 0.5, y + 1.5, h.z() + 0.5, 1, 0.4, 0.015);
        }
    }

    private void ac(int i, Is is) {
        FayHatti.Hucre h = is.h();
        int zemin = isler.zemin(h.x(), h.z());
        if (zemin == Integer.MIN_VALUE) {
            tamam.set(i);
            return;
        }
        int taban = Math.max(isler.enAltY(), zemin - h.derinlik() + 1);
        // Tek blok derin sütunda yüzeyin altı boşaltılmaz: düşen blok hemen yerine konar, yarık açılmamış görünür.
        boolean dusur = is.tur() == TUR_YARIK && taban < zemin && rastgele.nextInt(5) == 0;
        aktif.add(new Sutun(i, h.x(), h.z(), isler.ust(h.x(), h.z()), zemin, taban, dusur));
    }

    /** Sütunlar arasında sırayla birer blok: açık yarığın tamamı birlikte derinleşir. */
    private void kaz(int butce) {
        int kalan = butce;
        while (kalan > 0 && !aktif.isEmpty()) {
            Iterator<Sutun> it = aktif.iterator();
            while (it.hasNext() && kalan > 0) {
                Sutun c = it.next();
                if (adimAt(c)) {
                    kalan--;
                }
                if (c.y < c.taban) {
                    if (c.dusur) {
                        isler.dusur(new BlockPos(c.x, c.zemin, c.z), new Vec3(0, -0.05, 0),
                                DepremAyarlari.dusenBlokTavani());
                    }
                    tamam.set(c.sira);
                    it.remove();
                }
            }
        }
    }

    /** Üstten aşağı bir blok kaldırır; düşecek sütunun yüzey bloğu iç boşalana dek yerinde kalır. */
    private boolean adimAt(Sutun c) {
        int y = c.y--;
        if (c.dusur && y == c.zemin) {
            return false;
        }
        return isler.kaldir(new BlockPos(c.x, y, c.z), y == c.zemin || y == c.taban);
    }

    /** Etki yarıçapında rastgele sütunlarda sağlam olmayan bloklar: yaprak kırılır, altı boş olan düşer, gerisi kırılır. */
    private void yik(DepremSeviyesi s, double kisma) {
        ServerLevel level = isler.level();
        BlockPos m = baglam().merkez();
        double yaricap = s.yikimYaricapi();
        double beklenen = 12 * s.yikimYogunlugu() * kisma;
        int n = (int) beklenen + (rastgele.nextDouble() < beklenen - (int) beklenen ? 1 : 0);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int k = 0; k < n; k++) {
            double d = yaricap * Math.sqrt(rastgele.nextDouble());
            double a = rastgele.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(m.getX() + 0.5 + Math.cos(a) * d);
            int z = (int) Math.floor(m.getZ() + 0.5 + Math.sin(a) * d);
            if (rastgele.nextDouble() > Math.max(0.2, 1 - d / yaricap)) {
                continue;
            }
            int ust = isler.ust(x, z);
            if (ust == Integer.MIN_VALUE) {
                continue;
            }
            int kirilan = 0;
            for (int y = ust; y > ust - 16 && kirilan < 3; y--) {
                BlockState st = level.getBlockState(p.set(x, y, z));
                if (st.is(DepremEtiketleri.DAYANIKLI) || st.is(BlockTags.DIRT) || st.is(BlockTags.SAND)) {
                    break;
                }
                if (!st.is(DepremEtiketleri.KIRILGAN)) {
                    continue;
                }
                BlockPos konum = p.immutable();
                boolean oldu;
                if (st.is(BlockTags.LEAVES) || !level.getBlockState(konum.below()).isAir()) {
                    oldu = isler.kir(konum);
                } else {
                    oldu = isler.dusur(konum, new Vec3((rastgele.nextDouble() - 0.5) * 0.08, 0,
                            (rastgele.nextDouble() - 0.5) * 0.08), DepremAyarlari.dusenBlokTavani());
                }
                if (oldu) {
                    kirilan++;
                }
            }
        }
    }

    /** S4–5: komşusundan en az 2 blok yüksek toprak/kum/çakıl yüzeyi aşağı kayar. */
    private void heyelan(DepremSeviyesi s, double kisma) {
        ServerLevel level = isler.level();
        BlockPos m = baglam().merkez();
        int n = (int) Math.ceil(4 * kisma);
        for (int k = 0; k < n; k++) {
            double d = s.yikimYaricapi() * Math.sqrt(rastgele.nextDouble());
            double a = rastgele.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(m.getX() + 0.5 + Math.cos(a) * d);
            int z = (int) Math.floor(m.getZ() + 0.5 + Math.sin(a) * d);
            int ust = isler.ust(x, z);
            if (ust == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos konum = new BlockPos(x, ust, z);
            BlockState st = level.getBlockState(konum);
            if (!(st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(Blocks.GRAVEL))) {
                continue;
            }
            for (Direction yon : Direction.Plane.HORIZONTAL) {
                int komsu = isler.ust(x + yon.getStepX(), z + yon.getStepZ());
                if (komsu != Integer.MIN_VALUE && komsu <= ust - 2) {
                    isler.dusur(konum, new Vec3(yon.getStepX() * 0.15, 0.05, yon.getStepZ() * 0.15),
                            DepremAyarlari.dusenBlokTavani());
                    break;
                }
            }
        }
    }

    /** S4–5: yarık hattına 8 blok içindeki canlılar. */
    private void hasarVer(ServerLevel level, DepremSeviyesi s) {
        BlockPos m = baglam().merkez();
        double yarim = s.uzunluk() / 2.0;
        double cx = m.getX() + 0.5;
        double cz = m.getZ() + 0.5;
        double dx = Math.cos(aci);
        double dz = Math.sin(aci);
        AABB kutu = new AABB(cx - yarim - 8, m.getY() - 24, cz - yarim - 8, cx + yarim + 8, m.getY() + 24, cz + yarim + 8);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, kutu, LivingEntity::isAlive)) {
            double ex = e.getX() - cx;
            double ez = e.getZ() - cz;
            double t = Math.max(-yarim, Math.min(yarim, ex * dx + ez * dz));
            double fx = ex - t * dx;
            double fz = ez - t * dz;
            if (fx * fx + fz * fz <= 64) {
                e.hurt(level.damageSources().generic(), s.hasar());
            }
        }
    }

    @Override
    protected boolean aktifBitti() {
        DepremSeviyesi s = seviyeBilgisi();
        return gecen >= ArtciPlani.son(artcilar(), s.sureTick()) && (kazmaBitti() || gecen >= s.sureTick() + 600L);
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, ArtciPlani.son(artcilar(), seviyeBilgisi().sureTick()) - gecen);
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        OrtamSesi.durdur(level.getServer(), SES_KATMANI, temiz ? 20 : 40);
        OrtamSesi.durdur(level.getServer(), ARTCI_KATMANI, temiz ? 20 : 40);
        if (isler != null) {
            isler.ortuleriKaldir();
            if (temiz) {
                isler.dusenleriKaldir();
            }
            isler.bildir();
        }
        aktif.clear();
    }

    @Override
    protected void istemciVerisiYaz(CompoundTag t) {
        DepremSeviyesi s = seviyeBilgisi();
        double yarim = s.uzunluk() / 2.0;
        t.putInt("g", (int) Math.min(Integer.MAX_VALUE, gecen));
        t.putInt("s", s.sureTick());
        t.putFloat("a", (float) s.kameraGenligi());
        t.putFloat("r", (float) etkiAlani().yaricap());
        t.putFloat("aci", (float) aci);
        t.putFloat("L", (float) yarim);
        t.putFloat("f", (float) Math.min(yarim, s.acilmaHizi() * gecen / 20.0));
        int k = altAsama();
        if (k > 0 && k <= artcilar().size()) {
            ArtciPlani.Artci a = artcilar().get(k - 1);
            t.putInt("ab", (int) a.baslangic());
            t.putInt("ae", (int) a.bitis());
            t.putFloat("af", (float) a.kesir());
        }
    }

    @Override
    protected void ozelDurumYaz(CompoundTag t) {
        t.putLong("tohum", tohum);
        t.putDouble("aci", aci);
        t.putLong("gecen", gecen);
        t.putInt("acilan", acilan);
        t.putLongArray("tamam", tamam.toLongArray());
        int[] dizi = new int[aktif.size() * 4];
        for (int k = 0; k < aktif.size(); k++) {
            Sutun c = aktif.get(k);
            dizi[4 * k] = c.sira;
            dizi[4 * k + 1] = c.taban;
            dizi[4 * k + 2] = c.zemin;
            dizi[4 * k + 3] = c.dusur ? 1 : 0;
        }
        t.putIntArray("aktif", dizi.length == 0 ? kayitliAktif : dizi);
    }

    @Override
    protected void ozelDurumOku(CompoundTag t) {
        tohum = t.getLong("tohum");
        aci = t.getDouble("aci");
        gecen = Math.max(0, t.getLong("gecen"));
        acilan = Math.max(0, t.getInt("acilan"));
        tamam.clear();
        tamam.or(BitSet.valueOf(t.getLongArray("tamam")));
        kayitliAktif = t.getIntArray("aktif");
    }
}

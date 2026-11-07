package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.ArtciPlani;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.ArtciSarsintisi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Artçı kuyruğu (AF-D1; DS-14'ün yerine): deprem AKTIF'i ana sarsıntı ve yarık kazısı bitince kapanır, afet yuvası boşalır ve
 * yeni afet hemen başlayabilir; artçı planının kalanı ({@link ArtciPlani#devret}) yuva dışında, çekirdeğin dönem süreci
 * sözleşmesiyle sürer (çekirdek §3a): {@code /dogalafet stop} afet yokken ve {@code depremAfeti}/{@code dogalAfetlerAcik}
 * kapanınca durur, {@code /dogalafet asama ileri} sıradaki sınıra atlar. Durum {@code ekVeri("deprem_artci")}'dadır (sunucu
 * yeniden açılınca sürer), ilerleme {@code zamanAdimi()} ile birikir ({@link #devamEt} boş). Artçı blok değiştirmez: afete bağlı
 * olmayan ({@link OrtamSesi#AFETSIZ}) kısa gürleme katmanı ve yarık boyunca toz/çakıl. Yeni deprem kuyruğu devralınca eskisi
 * yerini bırakır (yeni döngü, önceki durdurma kaydı silinir).
 */
public final class ArtciKuyrugu implements DonemSureci {
    public static final String KIMLIK = "deprem_artci";
    static final String AD_ANAHTARI = "donem_sureci.dogalafetler." + KIMLIK;
    static final String ASAMA_ARTCI = "commands.dogalafetler.deprem_artci.artci";
    static final String ASAMA_ARA = "commands.dogalafetler.deprem_artci.ara";
    static final String ASAMA_BITTI = "commands.dogalafetler.deprem_artci.bitti";
    /** Artçı gürleme katmanı; istemci sarsıntısı genliği ve yarıçapı bundan okur ({@link ArtciSarsintisi}). */
    public static final String KATMAN = "deprem/artci";
    static final ArtciKuyrugu ORNEK = new ArtciKuyrugu();
    static final int TOZ_NOKTASI = 16;

    private static final String NO = "no";
    private static final String SEVIYE = "seviye";
    private static final String BOYUT = "boyut";
    private static final String MERKEZ = "merkez";
    private static final String MENZIL = "menzil";
    private static final String GECEN = "gecen";
    private static final String SUREN = "suren";
    private static final String PLAN = "plan";
    private static final String TOZ = "toz";

    /** Ekteki planın çözülmüş kopyası (her tick NBT okunmasın); etiket nesnesi ve deprem numarasıyla geçerli. */
    private CompoundTag onbellekEtiketi;
    private long onbellekNo;
    private List<ArtciPlani.Artci> onbellek = List.of();

    private ArtciKuyrugu() {
    }

    /** Mod kurucusundan ({@link DepremSistemi#kaydet}): dönem süreci ve tick dinleyicisi. */
    static void kaydet() {
        AfetYoneticisi.donemSureciKaydet(ORNEK);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, ORNEK::tick);
    }

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Component ad() {
        return Component.translatable(AD_ANAHTARI);
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.DEPREM);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        return DunyaDonemi.al(ustDunya).ekVeri(KIMLIK).contains(PLAN);
    }

    /** Süren artçının gürlemesi söner; kural açılınca henüz sürüyorsa yeniden başlar. */
    @Override
    public void durdur(ServerLevel ustDunya, DurdurmaNedeni neden) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        CompoundTag v = donem.ekVeri(KIMLIK);
        OrtamSesi.durdur(ustDunya.getServer(), KATMAN, 20);
        if (v.getInt(SUREN) != 0) {
            v.putInt(SUREN, 0);
            donem.setDirty();
        }
    }

    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        MinecraftServer server = ustDunya.getServer();
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        CompoundTag v = donem.ekVeri(KIMLIK);
        if (!v.contains(PLAN)) {
            return Optional.empty();
        }
        List<ArtciPlani.Artci> plan = plan(v);
        long gecen = ArtciPlani.sonrakiSinir(plan, v.getLong(GECEN));
        v.putLong(GECEN, gecen);
        donem.setDirty();
        if (!guncelle(server, donem, v, plan, !AfetYoneticisi.al(server).surecDurdu(this))) {
            return Optional.of(Component.translatable(ASAMA_BITTI, ad()));
        }
        int suren = ArtciPlani.suren(plan, gecen);
        return Optional.of(suren > 0 ? Component.translatable(ASAMA_ARTCI, ad(), suren, plan.size())
                : Component.translatable(ASAMA_ARA, ad(), siradaki(plan, gecen), plan.size()));
    }

    /**
     * Biten depremin kalan artçılarını devralır ({@code kalan} zamanları devir anından); boş planda bir şey yapmaz. Süren eski
     * kuyruk yerini bırakır.
     *
     * @param toz yarık boyunca toz noktaları ({@code BlockPos.asLong}; yükseklik artçı anında yüzeyden okunur)
     */
    static void devral(ServerLevel level, long afetNo, int seviye, BlockPos merkez, float menzil,
            List<ArtciPlani.Artci> kalan, long[] toz) {
        if (kalan.isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        CompoundTag v = donem.ekVeri(KIMLIK);
        if (v.getInt(SUREN) > 0) {
            OrtamSesi.durdur(server, KATMAN, 30);
        }
        bosalt(v);
        v.putLong(NO, afetNo);
        v.putInt(SEVIYE, seviye);
        v.putString(BOYUT, level.dimension().location().toString());
        v.putLong(MERKEZ, merkez.asLong());
        v.putFloat(MENZIL, menzil);
        v.putLong(GECEN, 0L);
        v.putInt(SUREN, 0);
        ListTag liste = new ListTag();
        for (ArtciPlani.Artci a : kalan) {
            CompoundTag t = new CompoundTag();
            t.putLong("b", a.baslangic());
            t.putLong("e", a.bitis());
            t.putDouble("k", a.kesir());
            liste.add(t);
        }
        v.put(PLAN, liste);
        v.putLongArray(TOZ, toz);
        donem.ekVeriDegisti(KIMLIK);
        yonetici.donemSureciBasladi(ORNEK);
        DogalAfetler.LOGGER.info("[deprem] artçı kuyruğu: seviye {}, {} artçı afet yuvası dışında, ilki {} tick sonra, merkez {}",
                seviye, kalan.size(), kalan.get(0).baslangic(), merkez.toShortString());
    }

    /** Kuyrukta artçı bekliyor ya da sürüyor mu (test ve görsel otomasyon okuması). */
    public static boolean suruyor(MinecraftServer server) {
        return AfetYoneticisi.al(server).donem().ekVeri(KIMLIK).contains(PLAN);
    }

    /** Süren artçının sırası (1'den), yoksa 0. */
    static int suren(MinecraftServer server) {
        return AfetYoneticisi.al(server).donem().ekVeri(KIMLIK).getInt(SUREN);
    }

    /** Yalnız GameTest: kuyruk sesiyle birlikte hemen boşalır (artçı sonraki testlere sızmasın). */
    static void sifirla(MinecraftServer server) {
        DunyaDonemi donem = AfetYoneticisi.al(server).donem();
        CompoundTag v = donem.ekVeri(KIMLIK);
        OrtamSesi.durdur(server, KATMAN, 1);
        if (!v.isEmpty()) {
            bosalt(v);
            donem.ekVeriDegisti(KIMLIK);
        }
    }

    private void tick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        CompoundTag v = donem.ekVeri(KIMLIK);
        if (!v.contains(PLAN)) {
            return;
        }
        int adim = yonetici.zamanAdimi();
        if (adim <= 0 || yonetici.surecDurdu(this)) {
            return;
        }
        v.putLong(GECEN, v.getLong(GECEN) + adim);
        donem.setDirty();
        guncelle(server, donem, v, plan(v), true);
    }

    /**
     * Süren artçı değiştiyse sesi ve tozu işler ({@code sesli} false: durdurulmuş süreç, yalnız zaman ilerler); kuyruk bittiyse
     * boşalır ve false döner.
     */
    private boolean guncelle(MinecraftServer server, DunyaDonemi donem, CompoundTag v, List<ArtciPlani.Artci> plan,
            boolean sesli) {
        long gecen = v.getLong(GECEN);
        int eski = v.getInt(SUREN);
        int yeni = sesli ? ArtciPlani.suren(plan, gecen) : 0;
        // Ortam katmanları kalıcı değil: artçı sürerken sunucu yeniden açıldıysa ses yeniden başlar.
        if (yeni != eski || yeni > 0 && !OrtamSesi.aktifMi(KATMAN)) {
            if (eski > 0) {
                OrtamSesi.durdur(server, KATMAN, 30);
            }
            v.putInt(SUREN, yeni);
            if (yeni > 0) {
                artciBasladi(server, v, plan, yeni, yeni != eski);
            }
            donem.ekVeriDegisti(KIMLIK);
        }
        if (!ArtciPlani.bitti(plan, gecen)) {
            return true;
        }
        DogalAfetler.LOGGER.info("[deprem] artçı kuyruğu bitti: seviye {}, {} artçı, merkez {}", v.getInt(SEVIYE), plan.size(),
                BlockPos.of(v.getLong(MERKEZ)).toShortString());
        if (yeni > 0) {
            OrtamSesi.durdur(server, KATMAN, 30);
        }
        bosalt(v);
        donem.ekVeriDegisti(KIMLIK);
        return false;
    }

    /** {@code ilkKez} false: sunucu yeniden açılınca yalnız ses yeniden başlar, artçı satırı ikinci kez yazılmaz. */
    private static void artciBasladi(MinecraftServer server, CompoundTag v, List<ArtciPlani.Artci> plan, int sira,
            boolean ilkKez) {
        ArtciPlani.Artci a = plan.get(sira - 1);
        BlockPos merkez = BlockPos.of(v.getLong(MERKEZ));
        if (ilkKez) {
            DogalAfetler.LOGGER.info("[deprem] artçı sarsıntı {}/{}: şiddet %{}, süre {} tick, merkez {}", sira, plan.size(),
                    Math.round(a.kesir() * 100), a.sure(), merkez.toShortString());
        }
        ResourceLocation boyutAdi = ResourceLocation.tryParse(v.getString(BOYUT));
        if (boyutAdi == null) {
            return;
        }
        ResourceKey<Level> boyut = ResourceKey.create(Registries.DIMENSION, boyutAdi);
        int sure = Math.max(4, a.sure());
        float hacim = ArtciSarsintisi.hacim(
                ArtciSarsintisi.genlik(DepremAyarlari.seviye(v.getInt(SEVIYE)).kameraGenligi(), a.kesir()));
        OrtamSesi.baslat(server, KATMAN, OrtamSesi.AFETSIZ, DepremSistemi.GURLEME_ADI, SoundSource.BLOCKS,
                OrtamSesi.Yer.nokta(boyut, Vec3.atCenterOf(merkez), Math.max(1f, v.getFloat(MENZIL))),
                HacimEgrisi.of(new HacimEgrisi.Nokta(0, 0f), new HacimEgrisi.Nokta(sure / 4, hacim),
                        new HacimEgrisi.Nokta(sure / 2, hacim), new HacimEgrisi.Nokta(sure, 0f)));
        ServerLevel level = server.getLevel(boyut);
        if (level == null) {
            return;
        }
        BlockParticleOption cakil = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState());
        for (long nokta : v.getLongArray(TOZ)) {
            int x = BlockPos.getX(nokta);
            int z = BlockPos.getZ(nokta);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlokIsleri.parcacik(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y, z + 0.5, 2, 0.4, 0.02);
            BlokIsleri.parcacik(level, cakil, x + 0.5, y, z + 0.5, 4, 0.3, 0.15);
        }
    }

    private List<ArtciPlani.Artci> plan(CompoundTag v) {
        long no = v.getLong(NO);
        if (v != onbellekEtiketi || no != onbellekNo) {
            ListTag liste = v.getList(PLAN, Tag.TAG_COMPOUND);
            List<ArtciPlani.Artci> plan = new ArrayList<>(liste.size());
            for (int i = 0; i < liste.size(); i++) {
                CompoundTag t = liste.getCompound(i);
                plan.add(new ArtciPlani.Artci(t.getLong("b"), t.getLong("e"), t.getDouble("k")));
            }
            onbellek = List.copyOf(plan);
            onbellekEtiketi = v;
            onbellekNo = no;
        }
        return onbellek;
    }

    /** {@code gecen} anından sonra başlayacak ilk artçının sırası (1'den); yoksa plan boyu. */
    private static int siradaki(List<ArtciPlani.Artci> plan, long gecen) {
        for (int i = 0; i < plan.size(); i++) {
            if (gecen < plan.get(i).baslangic()) {
                return i + 1;
            }
        }
        return plan.size();
    }

    private static void bosalt(CompoundTag v) {
        for (String anahtar : List.copyOf(v.getAllKeys())) {
            v.remove(anahtar);
        }
    }
}

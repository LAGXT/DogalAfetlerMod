package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Carpanlar;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Cekim;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Yonelim;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Kasırga afeti (bölüm A, madde 10; tasarım notu {@code docs/tasarim/kasirga.md}). Durum (konum, yön, hedef, AKTIF'te geçen
 * süre) bu afetin NBT'sindedir; {@link KasirgaVarligi} yalnız görüntü taşıyıcısıdır ve konumu entity-ticking olduğunda var
 * olur (spike B8: yüklü alan dışında kasırga soyut ilerler).
 *
 * <ul>
 * <li>ON_UYARI: gizli; hiçbir şey görünmez (madde 4).</li>
 * <li>SIREN: {@link KasirgaEvresi#OLUSUM}: duvar bulutu alçalıp döner, uzak uğultu 10 sn'de açılır.</li>
 * <li>AKTIF: doğuş 8 sn → olgun (seviye süresi) → ip 10 sn → dağılma 2 sn; alt aşama = evre sırası. Hareket, blok
 * kaldırma, çekim ve hasar sunucuda; bütçeler {@link TpsOlcer#kismaCarpani()} ile çarpılır.</li>
 * </ul>
 *
 * Kodla değişen her blok için chunk {@link SanalSu#chunkDegisti} ile bildirilir; {@code afetBlokHasari=false} iken hiçbir
 * blok değişmez.
 */
public final class Kasirga extends Afet {
    static final String UGULTU_KIMLIK = "kasirga/ugultu";
    static final String RUZGAR_KIMLIK = "kasirga/ruzgar";
    /** Uçan blok tavanı doluyken parçalanan bloğun eşya düşürme olasılığı (patlamadaki gibi kayıp; eşya varlığı TPS'i yer). */
    static final double PARCA_ESYA_PAYI = 1.0 / 16;
    private static final SoundEvent[] ENKAZ_SESLERI = {SoundEvents.WOOD_BREAK, SoundEvents.GRAVEL_BREAK,
        SoundEvents.GLASS_BREAK, SoundEvents.GRASS_BREAK, SoundEvents.WOOD_BREAK};
    private static final double TAM_TUR = Math.PI * 2;

    private Vec3 konum;
    private double yon;
    private double anaYon;
    private double hedefYon;
    private double hedefX;
    private double hedefZ;
    private boolean hedefVar;
    private boolean hedefGecildi;
    private long aktifGecen;
    private long toplamKaldirilan;
    private boolean hareketli = true;

    private Carpanlar carpanlar;
    private KasirgaSeviyesi seviyeBilgisi;
    private KasirgaVarligi varlik;
    private final List<UcanBlokVarligi> ucanlar = new ArrayList<>();
    private KasirgaEvresi.Durum durum = new KasirgaEvresi.Durum(KasirgaEvresi.OLUSUM, 0);
    private final RandomSource rastgele = RandomSource.create();
    private int tick;
    private int sonrakiEnkazSesi;
    private int sonrakiSimsek = 200;
    private boolean ugultuCaliyor;
    private boolean ruzgarCaliyor;

    public Kasirga() {
        super(AfetTuru.KASIRGA);
    }

    // ---------------------------------------------------------------- okuma

    /** Kasırga ekseninin zemin noktası (güncel). */
    public Vec3 konum() {
        return konum != null ? konum : Vec3.atBottomCenterOf(baglam().merkez());
    }

    /** Seviye tablosu (config çarpanları afet başlarken okunur). */
    public KasirgaSeviyesi seviyeBilgisi() {
        if (seviyeBilgisi == null) {
            carpanlar = KasirgaAyarlari.carpanlar();
            seviyeBilgisi = KasirgaSeviyesi.icin(seviye(), carpanlar);
        }
        return seviyeBilgisi;
    }

    private Carpanlar carpanlar() {
        seviyeBilgisi();
        return carpanlar;
    }

    public KasirgaEvresi.Durum durum() {
        return durum;
    }

    /** Hareket yönü (radyan; x doğu, z güney). */
    public double yon() {
        return yon;
    }

    public KasirgaVarligi varlik() {
        return varlik;
    }

    public int ucanBlokSayisi() {
        return ucanlar.size();
    }

    public long toplamKaldirilan() {
        return toplamKaldirilan;
    }

    long aktifGecen() {
        return aktifGecen;
    }

    /** Huni blokları henüz sarmal yörüngede taşıyor mu (ip evresinden itibaren bırakır). */
    boolean bloklariTasiyor() {
        return durum.evre() == KasirgaEvresi.DOGUS || durum.evre() == KasirgaEvresi.OLGUN;
    }

    /** GameTest: kasırga yerinde kalır (sahne dışına çıkmasın). */
    void hareketiKapat() {
        hareketli = false;
    }

    /** GameTest: kasırgayı sahnenin ortasına taşır. */
    void konumAyarla(Vec3 yeni) {
        konum = yeni;
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    @Override
    public EtkiAlani etkiAlani() {
        Vec3 k = konum();
        return EtkiAlani.daire(k.x, k.y, k.z, seviyeBilgisi().cekimYaricapi());
    }

    /**
     * Hedefin {@link Yonelim#dogusMesafesi} kadar uzağında doğar; hedef oyuncunun konumu yürüyüşün hedefidir. Doğuş chunk'ı
     * yüklü değilse yükseklik hedefin Y'sinden alınır: yükseklik okuması chunk'ı eşzamanlı üretmesin (inceleme Kn3).
     */
    @Override
    public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, RandomSource r) {
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(seviye, KasirgaAyarlari.carpanlar());
        hedefX = hedef.getX();
        hedefZ = hedef.getZ();
        hedefVar = true;
        double[] ofset = Yonelim.dogusOfseti(s.cekimYaricapi(), r.nextDouble(), r.nextDouble() * TAM_TUR);
        int x = Mth.floor(hedefX + ofset[0]);
        int z = Mth.floor(hedefZ + ofset[1]);
        int yukseklik = level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                : Mth.floor(hedef.getY());
        return new BlockPos(x, yukseklik, z);
    }

    @Override
    protected void baslarken(ServerLevel level) {
        konum = Vec3.atBottomCenterOf(baglam().merkez());
        if (!hedefVar) {
            // Konuma başlatılan kasırga (hedef oyuncu yok): rastgele bir yöne yürür.
            double aci = rastgele.nextDouble() * TAM_TUR;
            hedefX = konum.x + Math.cos(aci) * 96;
            hedefZ = konum.z + Math.sin(aci) * 96;
            hedefVar = true;
        }
        anaYon = Yonelim.yon(konum.x, konum.z, hedefX, hedefZ);
        yon = anaYon;
        hedefYon = anaYon;
        KasirgaSeviyesi s = seviyeBilgisi();
        DogalAfetler.LOGGER.info("[kasirga] başladı: seviye {} merkez {} hedef ({}, {}), taban çapı {}, çekim yarıçapı {}",
                seviye(), baglam().merkez().toShortString(), Math.round(hedefX), Math.round(hedefZ), s.tabanCapi(),
                s.cekimYaricapi());
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        tick++;
        switch (asama) {
            case SIREN -> {
                double p = (double) asamaGecenTick() / Math.max(1, SirenTuru.sureTick(tur()));
                durumAyarla(level, new KasirgaEvresi.Durum(KasirgaEvresi.OLUSUM, Math.min(1, p)));
                varlikGuncelle(level);
                sesleriBaslat(level, false);
                sesleriTasi(level);
            }
            case AKTIF -> {
                aktifGecen += Math.max(0, adim);
                durumAyarla(level, KasirgaEvresi.aktifte(aktifGecen, seviyeBilgisi().aktifSureTick()));
                hareketEt(level);
                varlikGuncelle(level);
                sesleriBaslat(level, true);
                sesleriTasi(level);
                ucanlar.removeIf(Entity::isRemoved);
                if (Kurallar.blokHasariAcik(level.getGameRules())) {
                    bloklariKaldir(level);
                    izBirak(level);
                }
                varliklariCek(level);
                enkazSesi(level);
                simsek(level);
            }
            default -> {
            }
        }
    }

    /** Evre aşamayla birlikte değişir (ilk tick beklenmez): {@code asama ileri} AKTIF'e geçince doğuşla başlar. */
    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.SIREN) {
            durumAyarla(level, new KasirgaEvresi.Durum(KasirgaEvresi.OLUSUM, 0));
        } else if (yeni == AfetAsamasi.AKTIF) {
            durumAyarla(level, KasirgaEvresi.aktifte(aktifGecen, seviyeBilgisi().aktifSureTick()));
        }
    }

    @Override
    protected boolean aktifBitti() {
        return aktifGecen >= KasirgaEvresi.toplamAktif(seviyeBilgisi().aktifSureTick());
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, KasirgaEvresi.toplamAktif(seviyeBilgisi().aktifSureTick()) - aktifGecen);
    }

    /** {@code /dogalafet asama ileri} AKTIF'te: bir sonraki evre (doğuş → olgun → ip → dağılma); dağılmadan sonra BITIS. */
    @Override
    protected boolean altAsamaIleri(ServerLevel level) {
        long olgun = seviyeBilgisi().aktifSureTick();
        KasirgaEvresi simdiki = KasirgaEvresi.aktifte(aktifGecen, olgun).evre();
        if (simdiki == KasirgaEvresi.DAGILMA) {
            return false;
        }
        aktifGecen = KasirgaEvresi.baslangic(KasirgaEvresi.values()[simdiki.ordinal() + 1], olgun);
        durumAyarla(level, KasirgaEvresi.aktifte(aktifGecen, olgun));
        return true;
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        MinecraftServer server = level.getServer();
        sesleriDurdur(server, 40);
        int birakilan = 0;
        for (UcanBlokVarligi blok : new ArrayList<>(ucanlar)) {
            blok.yereBirak();
            birakilan++;
        }
        ucanlar.clear();
        // Yeniden yüklemeden sonra liste boş olabilir: bu afete bağlı (ya da sahipsiz kalmış) her varlık da kaldırılır.
        int kasirgaVarligi = 0;
        for (ServerLevel l : server.getAllLevels()) {
            for (UcanBlokVarligi blok : l.getEntities(KasirgaKayitlari.UCAN_BLOK.get(),
                    b -> b.sahip() == this || b.sahip() == null || b.sahip().sonlandi())) {
                blok.yereBirak();
                birakilan++;
            }
            for (KasirgaVarligi k : l.getEntities(KasirgaKayitlari.KASIRGA.get(), k -> true)) {
                k.discard();
                kasirgaVarligi++;
            }
        }
        varlik = null;
        DogalAfetler.LOGGER.info("[kasirga] temizlendi (temiz={}): kaldırılan blok {}, yere bırakılan uçan blok {}, "
                + "atılan kasırga varlığı {}", temiz, toplamKaldirilan, birakilan, kasirgaVarligi);
    }

    /** Sunucu kapanırken havadaki bloklar yere konur (varlıklar kaydedilmez; blok kaybolmasın). */
    @Override
    protected void kapanirken(ServerLevel level) {
        for (UcanBlokVarligi blok : new ArrayList<>(ucanlar)) {
            blok.yereBirak();
        }
        ucanlar.clear();
        if (varlik != null) {
            varlik.discard();
            varlik = null;
        }
    }

    @Override
    protected void ozelDurumYaz(CompoundTag t) {
        Vec3 k = konum();
        t.putDouble("x", k.x);
        t.putDouble("y", k.y);
        t.putDouble("z", k.z);
        t.putDouble("yon", yon);
        t.putDouble("anaYon", anaYon);
        t.putDouble("hedefYon", hedefYon);
        t.putDouble("hedefX", hedefX);
        t.putDouble("hedefZ", hedefZ);
        t.putBoolean("hedefVar", hedefVar);
        t.putBoolean("hedefGecildi", hedefGecildi);
        t.putLong("aktifGecen", aktifGecen);
        t.putLong("kaldirilan", toplamKaldirilan);
    }

    @Override
    protected void ozelDurumOku(CompoundTag t) {
        if (t.contains("x")) {
            konum = new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
        }
        yon = t.getDouble("yon");
        anaYon = t.getDouble("anaYon");
        hedefYon = t.getDouble("hedefYon");
        hedefX = t.getDouble("hedefX");
        hedefZ = t.getDouble("hedefZ");
        hedefVar = t.getBoolean("hedefVar");
        hedefGecildi = t.getBoolean("hedefGecildi");
        aktifGecen = Math.max(0, t.getLong("aktifGecen"));
        toplamKaldirilan = Math.max(0, t.getLong("kaldirilan"));
    }

    /** Kayıttan dönüş: evre ilk tick beklenmeden {@code aktifGecen}'den kurulur, görüntü varlığı hemen doğar (inceleme Kn5). */
    @Override
    protected void yuklendi(ServerLevel level) {
        if (asama() == AfetAsamasi.AKTIF) {
            durum = KasirgaEvresi.aktifte(aktifGecen, seviyeBilgisi().aktifSureTick());
        } else if (asama() == AfetAsamasi.SIREN) {
            durum = new KasirgaEvresi.Durum(KasirgaEvresi.OLUSUM,
                    Math.min(1, (double) asamaGecenTick() / Math.max(1, SirenTuru.sureTick(tur()))));
        }
        altAsamaAyarla(durum.evre().ordinal());
        if (asama() == AfetAsamasi.SIREN || asama() == AfetAsamasi.AKTIF) {
            varlikGuncelle(level);
        }
        DogalAfetler.LOGGER.info("[kasirga] kayıttan dönüş: evre {} ({}), konum {}, kaldırılan {}", durum.evre(),
                String.format(Locale.ROOT, "%.2f", durum.ilerleme()), BlockPos.containing(konum()).toShortString(),
                toplamKaldirilan);
    }

    // ---------------------------------------------------------------- davranış

    private void durumAyarla(ServerLevel level, KasirgaEvresi.Durum yeni) {
        KasirgaEvresi eski = durum.evre();
        durum = yeni;
        if (yeni.evre() != eski) {
            altAsamaAyarla(yeni.evre().ordinal());
            DogalAfetler.LOGGER.info("[kasirga] evre {} → {} (konum {}, uçan blok {}, kaldırılan {})", eski, yeni.evre(),
                    BlockPos.containing(konum()).toShortString(), ucanlar.size(), toplamKaldirilan);
            if (yeni.evre() == KasirgaEvresi.IP) {
                // İp evresi 10 sn: uğultu ve rüzgâr huniyle birlikte söner.
                sesleriDurdur(level.getServer(), KasirgaEvresi.IP_TICK);
            }
        }
    }

    private void hareketEt(ServerLevel level) {
        if (!hareketli) {
            return;
        }
        Vec3 k = konum();
        if (tick % Yonelim.SAPMA_ARALIGI_TICK == 0) {
            if (!hedefGecildi) {
                if (Math.hypot(hedefX - k.x, hedefZ - k.z) < Yonelim.GECIS_YARICAPI) {
                    hedefGecildi = true;
                } else {
                    anaYon = Yonelim.yon(k.x, k.z, hedefX, hedefZ);
                }
            }
            hedefYon = Yonelim.sapmaliYon(anaYon, rastgele.nextDouble());
        }
        yon = Yonelim.don(yon, hedefYon);
        double adim = seviyeBilgisi().hiz() / 20 * KasirgaEvresi.hizPayi(durum.evre());
        double nx = k.x + Math.cos(yon) * adim;
        double nz = k.z + Math.sin(yon) * adim;
        double y = k.y;
        if (level.isLoaded(BlockPos.containing(nx, y, nz))) {
            int zemin = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz));
            y += (zemin - y) * 0.2;
        }
        konum = new Vec3(nx, y, nz);
    }

    /** Görüntü varlığı: konum entity-ticking iken var, değilken yok (kasırga soyut ilerler). */
    private void varlikGuncelle(ServerLevel level) {
        if (varlik != null && varlik.isRemoved()) {
            varlik = null;
        }
        Vec3 k = konum();
        boolean isler = level.isPositionEntityTicking(BlockPos.containing(k));
        if (varlik == null) {
            if (!isler) {
                return;
            }
            KasirgaVarligi yeni = KasirgaKayitlari.KASIRGA.get().create(level);
            if (yeni == null) {
                return;
            }
            yeni.moveTo(k.x, k.y, k.z, 0, 0);
            yeni.bagla(this, seviye(), (float) carpanlar().boyut());
            yeni.guncelle(durum, (float) yon);
            varlik = yeni;
            level.addFreshEntity(yeni);
        } else if (!isler) {
            varlik.discard();
            varlik = null;
        } else {
            varlik.setPos(k.x, k.y, k.z);
            varlik.guncelle(durum, (float) yon);
        }
    }

    /**
     * Tick başına seviye tablosundaki sayı kadar deneme (evre payı × TPS kısması): çekim yarıçapının %60'ı içinde, merkeze
     * yoğun rastgele sütunun en üst bloğu. Uçan blok tavanı doluysa blok parçalanır (kırılma parçacığı ve sesi): tick başına
     * en çok denemenin dörtte biri, yalnız {@link #PARCA_ESYA_PAYI}'nda eşya düşer. Her kırılan blok eşya düşürseydi S5'te
     * 15 sn'de ~3800 eşya varlığı oluşuyor ve tick 40 ms'ye çıkıyordu (K-04).
     */
    private void bloklariKaldir(ServerLevel level) {
        double pay = KasirgaEvresi.kaldirmaPayi(durum);
        KasirgaSeviyesi s = seviyeBilgisi();
        if (pay <= 0 || s.blokKaldirmaTick() <= 0) {
            return;
        }
        int deneme = Math.max(1, (int) Math.round(s.blokKaldirmaTick() * pay * TpsOlcer.kismaCarpani()));
        int tavan = KasirgaAyarlari.ucanBlokTavani();
        int parcalanabilir = Math.max(1, deneme / 4);
        int parcalanan = 0;
        Vec3 k = konum();
        LongSet degisen = new LongOpenHashSet();
        for (int i = 0; i < deneme; i++) {
            double aci = rastgele.nextDouble() * TAM_TUR;
            double r = s.cekimYaricapi() * 0.6 * Math.sqrt(rastgele.nextDouble());
            int x = Mth.floor(k.x + Math.cos(aci) * r);
            int z = Mth.floor(k.z + Math.sin(aci) * r);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos yer = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            BlockState blok = level.getBlockState(yer);
            if (!kaldirilabilir(level, yer, blok, s)) {
                continue;
            }
            boolean ucabilir = ucanlar.size() < tavan && level.isPositionEntityTicking(yer);
            if (!ucabilir && parcalanan >= parcalanabilir) {
                continue;
            }
            degisen.add(ChunkPos.asLong(x >> 4, z >> 4));
            toplamKaldirilan++;
            if (ucabilir) {
                level.removeBlock(yer, false);
                double yaricap = Math.hypot(x + 0.5 - k.x, z + 0.5 - k.z);
                double baslangic = Math.atan2(z + 0.5 - k.z, x + 0.5 - k.x);
                UcanBlokVarligi ucan = UcanBlokVarligi.olustur(level, blok, this, varlik != null ? varlik.getId() : -1,
                        baslangic, yaricap, yer.getY() - k.y);
                level.addFreshEntity(ucan);
                ucanlar.add(ucan);
            } else {
                parcalanan++;
                level.destroyBlock(yer, false);
                if (rastgele.nextDouble() < PARCA_ESYA_PAYI) {
                    Block.dropResources(blok, level, yer);
                }
            }
        }
        degisen.forEach((long c) -> SanalSu.chunkDegisti(level, new ChunkPos(c)));
    }

    /**
     * Kaldırılabilir blok: hava, sıvı, blok varlıklı (sandık vb.), {@code #dogalafetler:kasirga_dayanikli}, çift blok (uzun ot,
     * kapı, yatak; tek yarısı taşınırsa geçersiz yarım blok kalır) değil; sertliği seviye sınırında. Toprak/kum/taş zemin yalnız
     * S4-5'te kalkar; S1-3 zemini sıyırır ({@link #izBirak}).
     */
    static boolean kaldirilabilir(ServerLevel level, BlockPos yer, BlockState blok, KasirgaSeviyesi s) {
        if (blok.isAir() || !blok.getFluidState().isEmpty() || blok.hasBlockEntity() || blok.is(KasirgaKayitlari.DAYANIKLI)
                || blok.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || blok.hasProperty(BlockStateProperties.BED_PART)) {
            return false;
        }
        if (!s.zeminKazar() && zeminMi(blok)) {
            return false;
        }
        float sertlik = blok.getDestroySpeed(level, yer);
        return sertlik >= 0 && sertlik <= s.sertlikSiniri();
    }

    private static boolean zeminMi(BlockState blok) {
        return blok.is(BlockTags.DIRT) || blok.is(BlockTags.SAND) || blok.is(Blocks.GRAVEL) || blok.is(Blocks.FARMLAND)
                || blok.is(Blocks.DIRT_PATH) || blok.is(BlockTags.BASE_STONE_OVERWORLD) || blok.is(Blocks.SNOW_BLOCK)
                || blok.is(Blocks.CLAY);
    }

    /**
     * Afet sonrası iz (tasarım notu §2): huni tabanının altında bitkiler kopar, çimen toprağa döner (sıyırılmış şerit);
     * S4-5'te alt girdapların yer izi kaba toprak (sikloidal halkalar).
     */
    private void izBirak(ServerLevel level) {
        if (durum.evre() != KasirgaEvresi.OLGUN && durum.evre() != KasirgaEvresi.IP) {
            return;
        }
        KasirgaSeviyesi s = seviyeBilgisi();
        Vec3 k = konum();
        LongSet degisen = new LongOpenHashSet();
        int adet = Math.max(1, (int) Math.round(s.seviye() * TpsOlcer.kismaCarpani()));
        for (int i = 0; i < adet; i++) {
            double aci = rastgele.nextDouble() * TAM_TUR;
            double r = s.hasarYaricapi() * Math.sqrt(rastgele.nextDouble());
            siyir(level, Mth.floor(k.x + Math.cos(aci) * r), Mth.floor(k.z + Math.sin(aci) * r), Blocks.DIRT, degisen);
        }
        int girdap = s.seviye() >= 5 ? 4 : s.seviye() == 4 ? 3 : 0;
        double t = tick / 20.0;
        for (int g = 0; g < girdap; g++) {
            double faz = g * TAM_TUR / girdap - t * Math.PI;
            double yorunge = s.tabanYaricapi() * 1.2;
            siyir(level, Mth.floor(k.x + Math.cos(faz) * yorunge), Mth.floor(k.z + Math.sin(faz) * yorunge), Blocks.COARSE_DIRT,
                    degisen);
        }
        degisen.forEach((long c) -> SanalSu.chunkDegisti(level, new ChunkPos(c)));
    }

    private static void siyir(ServerLevel level, int x, int z, Block toprak, LongSet degisen) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        BlockPos ust = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        BlockState blok = level.getBlockState(ust);
        if (!blok.isAir() && blok.canBeReplaced() && blok.getFluidState().isEmpty()
                && !blok.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            level.destroyBlock(ust, false);
            degisen.add(ChunkPos.asLong(x >> 4, z >> 4));
            ust = ust.below();
            blok = level.getBlockState(ust);
        }
        if (blok.is(Blocks.GRASS_BLOCK) || blok.is(Blocks.PODZOL) || blok.is(Blocks.MYCELIUM)
                || (toprak == Blocks.COARSE_DIRT && blok.is(Blocks.DIRT))) {
            level.setBlock(ust, toprak.defaultBlockState(), Block.UPDATE_ALL);
            degisen.add(ChunkPos.asLong(x >> 4, z >> 4));
        }
    }

    /**
     * Çekim, kaldırma ve çekirdek hasarı ({@link Cekim}): çekim yarıçapındaki oyuncu, mob, eşya ve araçlar. Oyuncunun hızı
     * istemcide işlediği için iki tick'te bir {@code hurtMarked} ile gönderilir. Hasar saniyede bir, huni tabanında.
     */
    private void varliklariCek(ServerLevel level) {
        double guc = KasirgaEvresi.guc(durum) * carpanlar().cekim();
        KasirgaSeviyesi s = seviyeBilgisi();
        if (guc <= 0 && KasirgaEvresi.guc(durum) <= 0) {
            return;
        }
        Vec3 k = konum();
        double r = s.cekimYaricapi();
        AABB kutu = new AABB(k.x - r, k.y - 6, k.z - r, k.x + r, k.y + s.yukseklik(), k.z + r);
        double tavan = KasirgaAyarlari.kaldirmaTavani();
        boolean hasarTiki = tick % 20 == 0;
        double hasar = s.cekirdekHasari() * KasirgaEvresi.guc(durum);
        for (Entity e : level.getEntities((Entity) null, kutu, Kasirga::cekilebilir)) {
            double dx = e.getX() - k.x;
            double dz = e.getZ() - k.z;
            double h = e.getY() - k.y;
            double[] ivme = Cekim.ivme(dx, dz, h, s.tabanYaricapi(), r, s.seviye(), guc, tavan);
            if (ivme[0] != 0 || ivme[1] != 0 || ivme[2] != 0) {
                if (e instanceof ServerPlayer oyuncu) {
                    // Oyuncunun hareketini istemci işler; sunucudaki hız güncel değildir. Son tick'in yer değiştirmesi hız
                    // tahminidir; iki tick'lik ivme eklenip hız paketiyle gönderilir.
                    if (tick % 2 == 0) {
                        double[] yeni = Cekim.kirp(new double[] {e.getX() - e.xo + 2 * ivme[0], e.getY() - e.yo + 2 * ivme[1],
                            e.getZ() - e.zo + 2 * ivme[2]});
                        oyuncu.setDeltaMovement(yeni[0], yeni[1], yeni[2]);
                        oyuncu.hurtMarked = true;
                    }
                } else {
                    Vec3 v = e.getDeltaMovement();
                    double[] yeni = Cekim.kirp(new double[] {v.x + ivme[0], v.y + ivme[1], v.z + ivme[2]});
                    e.setDeltaMovement(yeni[0], yeni[1], yeni[2]);
                    e.hasImpulse = true;
                    if (yeni[1] > 0) {
                        e.fallDistance = 0;
                    }
                }
            }
            if (hasarTiki && hasar > 0 && e instanceof LivingEntity canli && Math.hypot(dx, dz) < s.hasarYaricapi()
                    && h < s.yukseklik() * 0.5) {
                canli.hurt(KasirgaKayitlari.hasar(level), (float) Math.max(0.5, hasar));
            }
        }
    }

    private static boolean cekilebilir(Entity e) {
        if (e instanceof KasirgaVarligi || e instanceof UcanBlokVarligi || e instanceof HangingEntity || !e.isAlive()
                || e.isSpectator() || e.isPassenger() || e.noPhysics) {
            return false;
        }
        return !(e instanceof Player p) || !p.isCreative();
    }

    private void sesleriBaslat(ServerLevel level, boolean aktif) {
        if (durum.evre() == KasirgaEvresi.IP || durum.evre() == KasirgaEvresi.DAGILMA) {
            return;
        }
        MinecraftServer server = level.getServer();
        long afetNo = baglam().baslangicSaati();
        if (!ugultuCaliyor) {
            OrtamSesi.baslat(server, UGULTU_KIMLIK, afetNo, KasirgaKayitlari.UGULTU, SoundSource.WEATHER,
                    OrtamSesi.Yer.nokta(level.dimension(), konum(), 160f), HacimEgrisi.giris(200, 1f));
            ugultuCaliyor = true;
        }
        if (aktif && !ruzgarCaliyor) {
            OrtamSesi.baslat(server, RUZGAR_KIMLIK, afetNo, KasirgaKayitlari.RUZGAR, SoundSource.WEATHER,
                    OrtamSesi.Yer.nokta(level.dimension(), konum(), 64f), HacimEgrisi.giris(KasirgaEvresi.DOGUS_TICK, 0.9f));
            ruzgarCaliyor = true;
        }
    }

    /** Hareketli kaynak: en çok 4 Hz (OrtamSesi sözleşmesi). */
    private void sesleriTasi(ServerLevel level) {
        if (tick % 5 != 0) {
            return;
        }
        if (ugultuCaliyor) {
            OrtamSesi.konumGuncelle(level.getServer(), UGULTU_KIMLIK, konum());
        }
        if (ruzgarCaliyor) {
            OrtamSesi.konumGuncelle(level.getServer(), RUZGAR_KIMLIK, konum());
        }
    }

    private void sesleriDurdur(MinecraftServer server, int fade) {
        OrtamSesi.durdur(server, UGULTU_KIMLIK, fade);
        OrtamSesi.durdur(server, RUZGAR_KIMLIK, fade);
        ugultuCaliyor = false;
        ruzgarCaliyor = false;
    }

    /**
     * Olgun evrede 20-40 sn'de bir kasırganın 40-90 blok uzağına hasarsız şimşek (tasarım notu §2 katman 6; vanilla
     * {@code LightningBolt#setVisualOnly}: ateş yakmaz, can almaz, gök gürültüsü vanilla WEATHER sesi).
     */
    private void simsek(ServerLevel level) {
        if (durum.evre() != KasirgaEvresi.OLGUN || tick < sonrakiSimsek) {
            return;
        }
        sonrakiSimsek = tick + 400 + rastgele.nextInt(400);
        Vec3 k = konum();
        double aci = rastgele.nextDouble() * TAM_TUR;
        double r = 40 + rastgele.nextDouble() * 50;
        int x = Mth.floor(k.x + Math.cos(aci) * r);
        int z = Mth.floor(k.z + Math.sin(aci) * r);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        LightningBolt simsek = EntityType.LIGHTNING_BOLT.create(level);
        if (simsek != null) {
            simsek.moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5);
            simsek.setVisualOnly(true);
            level.addFreshEntity(simsek);
        }
    }

    /** Rastgele enkaz çarpmaları (tasarım notu §4 orta mesafe): 0,5-2 sn aralıkla, kasırganın içinde, BLOCKS kategorisi. */
    private void enkazSesi(ServerLevel level) {
        if (KasirgaEvresi.kaldirmaPayi(durum) <= 0 || tick < sonrakiEnkazSesi) {
            return;
        }
        sonrakiEnkazSesi = tick + 10 + rastgele.nextInt(30);
        KasirgaSeviyesi s = seviyeBilgisi();
        Vec3 k = konum();
        double aci = rastgele.nextDouble() * TAM_TUR;
        double r = s.cekimYaricapi() * 0.6 * rastgele.nextDouble();
        level.playSound(null, k.x + Math.cos(aci) * r, k.y + rastgele.nextDouble() * 15, k.z + Math.sin(aci) * r,
                ENKAZ_SESLERI[rastgele.nextInt(ENKAZ_SESLERI.length)], SoundSource.BLOCKS, 1.5f + 0.3f * s.seviye(),
                0.6f + rastgele.nextFloat() * 0.3f);
    }
}

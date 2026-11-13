package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Carpanlar;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaRotasi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaYikimi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Yonelim;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetMerkezi;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Kasırga afeti (bölüm A, madde 10; tasarım notu {@code docs/tasarim/kasirga.md}). Durum (konum, yön, rota, AKTIF'te geçen
 * süre) bu afetin NBT'sindedir; {@link KasirgaVarligi} yalnız görüntü taşıyıcısıdır ve konumu entity-ticking olduğunda var
 * olur. Kasırga kimseyi izlemez: hedef oyuncu yalnız doğuş noktasını verir, yürüyüş {@link KasirgaRotasi}'nın ara
 * hedeflerinedir ve yalnız entity-ticking chunk'lara ilerler (K-01: görüntü ve çekim yalnız oralarda çalışır).
 *
 * <ul>
 * <li>ON_UYARI: gizli; hiçbir şey görünmez (madde 4).</li>
 * <li>SIREN: {@link KasirgaEvresi#OLUSUM}: duvar bulutu alçalıp döner, uzak uğultu 10 sn'de açılır.</li>
 * <li>AKTIF: doğuş 8 sn → olgun (seviye süresi) → ip 10 sn → dağılma 2 sn; alt aşama = evre sırası. Hareket, blok
 * kaldırma, mob/eşya çekimi ve hasar sunucuda (oyuncu çekimi oyuncunun istemcisinde, {@link KasirgaCekimi}); bütçeler
 * {@link TpsOlcer#kismaCarpani()} ile çarpılır.</li>
 * </ul>
 *
 * Kodla değişen her blok için chunk {@link SanalSu#chunkDegisti} ile bildirilir; {@code afetBlokHasari=false} iken hiçbir
 * blok değişmez.
 */
public final class Kasirga extends Afet {
    static final String UGULTU_KIMLIK = "kasirga/ugultu";
    static final String RUZGAR_KIMLIK = "kasirga/ruzgar";
    /** Devrilen gövdede yukarıdan aşağı en çok bu kadar kesintisiz dikey kütük aranır (dev ağaçlar dahil). */
    private static final int GOVDE_TARAMA = 32;
    /** Devrik kütüğün konduğu zemin, gövde dibinden en çok bu kadar blok yüksek ya da alçakta olabilir. */
    private static final int YATIS_YUKSEKLIK_FARKI = 2;
    /** Zemin yüzeyi yok (boşluk, ör. Uç): {@link #zeminYuzeyi}. */
    static final int ZEMIN_YOK = Integer.MIN_VALUE;
    /** Zemin taramasında yüksek blok üstünden (ağaç, bina) aşağı en çok bu kadar kat atlanır. */
    private static final int ZEMIN_TARAMA = 64;
    /** Zeminsiz (boşluk üstü) kalan kasırga bu kadar tick sonra sona erer. */
    private static final int ZEMINSIZ_SINIR = 100;
    /** Önündeki engelin denetim ve temizleme aralığı (tick). */
    private static final int ENGEL_ARALIGI = 4;
    /** Temizlenemeyen engelde yan yön sapması (radyan), süresi (tick) ve bu sırada tick başına en çok dönüş. */
    private static final double KACIS_ACISI = 1.0;
    private static final int KACIS_TICK = 80;
    private static final double KACIS_DONUS = Math.toRadians(5);
    private static final SoundEvent[] ENKAZ_SESLERI = {SoundEvents.WOOD_BREAK, SoundEvents.GRAVEL_BREAK,
        SoundEvents.GLASS_BREAK, SoundEvents.GRASS_BREAK, SoundEvents.WOOD_BREAK};
    private static final double TAM_TUR = Math.PI * 2;
    /** K-E42: ara hedef satırı yalnız görsel otomasyonda ({@code -Potomasyon}); üretimde ve GameTest'te yazılmaz. */
    private static final boolean OTOMASYON = !System.getProperty("dogalafetler.otomasyon", "").isEmpty();
    /**
     * Vanilla "uçma" denetiminin sayacı ({@code ServerGamePacketListenerImpl.aboveGroundTickCount}, SRG {@code f_9737_}) ve
     * sürülen aracın eşi ({@code aboveGroundVehicleTickCount}, {@code f_9739_}): uçma izni kapalı sunucuda havada yüzen oyuncu
     * 80 tick sonra atılır. Olay yok; Forge yansıması (projede {@code BuzBalikciligi} kalıbı), mixin ya da access transformer
     * gerekmez (AF3-08).
     */
    private static final Field UCMA_SAYACI = ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class, "f_9737_");
    private static final Field ARAC_UCMA_SAYACI = ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class,
            "f_9739_");

    private Vec3 konum;
    private double yon;
    private double hedefYon;
    /** Doğuşta hedeften kasırgaya yön (ilk bacağın yönü); hedefsiz başlatılan kasırgada NaN. */
    private double ilkYon = Double.NaN;
    private KasirgaRotasi rota;
    private long aktifGecen;
    private long toplamKaldirilan;
    private boolean hareketli = true;
    /** Temizlenemeyen engelden kaçış: kalan tick ve hedef yön. */
    private int kacisKalan;
    private double kacisYonu;
    /** Zemin yokken geçen tick; sınırı aşınca kasırga sona erer. */
    private int zeminsizTick;
    private boolean zeminsizBitis;

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

    /** Şu anki ara hedef {x, z}; rota henüz yoksa {@code null}. */
    public double[] araHedef() {
        return rota == null ? null : new double[] {rota.hedefX(), rota.hedefZ()};
    }

    /** Şu anki ara hedefin numarası (seçim sayacı; 0 = henüz seçilmedi). */
    public int araHedefNo() {
        return rota == null ? 0 : rota.sayac();
    }

    KasirgaRotasi rota() {
        return rota;
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

    /** GameTest: kasırgayı sahnenin ortasına taşır; rota yeni konumdan yeniden başlar (ilk hareket tick'inde ara hedef seçer). */
    void konumAyarla(Vec3 yeni) {
        konum = yeni;
        if (rota != null) {
            rota = KasirgaRotasi.yeni(yeni.x, yeni.z, yon, rota.tohum());
        }
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    @Override
    public EtkiAlani etkiAlani() {
        Vec3 k = konum();
        return EtkiAlani.daire(k.x, k.y, k.z, seviyeBilgisi().cekimYaricapi());
    }

    /**
     * Hedefin {@link Yonelim#dogusMesafesi} kadar uzağında doğar (K-03); hedef yürüyüşte izlenmez, ilk bacağın yönü hedeften
     * uzaklaşır. Doğuş chunk'ı yüklü değilse yükseklik hedefin Y'sinden alınır: yükseklik okuması chunk'ı eşzamanlı
     * üretmesin (inceleme Kn3).
     */
    @Override
    public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, BlockPos merkezNoktasi,
            RandomSource r) {
        if (!AfetMerkezi.komutNoktasi(hedef, merkezNoktasi)) {
            // Doğal başlatma: nokta oyuncunun 3–10 chunk uzağındaki halkada; ilk bacak hedeften uzaklaşır.
            ilkYon = Math.atan2(merkezNoktasi.getZ() - hedef.getZ(), merkezNoktasi.getX() - hedef.getX());
            return merkezNoktasi;
        }
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(seviye, KasirgaAyarlari.carpanlar());
        double aci = r.nextDouble() * TAM_TUR;
        double[] ofset = Yonelim.dogusOfseti(s.cekimYaricapi(), r.nextDouble(), aci);
        ilkYon = aci;
        int x = Mth.floor(hedef.getX() + ofset[0]);
        int z = Mth.floor(hedef.getZ() + ofset[1]);
        int yukseklik = level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                : Mth.floor(hedef.getY());
        return new BlockPos(x, yukseklik, z);
    }

    /**
     * Kasırga yalnız zemini olan boyutta ve gerçek zemin üstünde doğar: gökyüzü ışığı olmayan (Uç) ya da tavanlı (Nether)
     * boyut ve boşluk üstündeki sütun reddedilir. Chunk yüklü değilse (yükseklik okuması chunk üretmesin) kabul edilir; zemin
     * ilk hareket tick'lerinde yeniden denetlenir ({@link #zeminDenetimi}).
     */
    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        if (!zeminliBoyut(level)) {
            return false;
        }
        return !level.hasChunk(merkez.getX() >> 4, merkez.getZ() >> 4)
                || zeminYuzeyi(level, merkez.getX(), merkez.getZ()) != ZEMIN_YOK;
    }

    /** Gökyüzü ışıklı ve tavansız boyut (Overworld ve benzerleri); Uç ve Nether değil. */
    static boolean zeminliBoyut(ServerLevel level) {
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }

    /**
     * {@code (x, z)} sütununun gerçek zemin yüzeyi (zemin bloğunun ya da su yüzeyinin üst Y'si; kasırga tabanı buraya oturur).
     * Bina, ağaç gövdesi ve benzeri yapılar zemin sayılmaz: yapraksız en üst bloktan aşağı, toprak/kum/taş/kar/kil/buz gibi
     * doğal zemin ya da sıvı bulunana dek inilir ({@link #zeminBlogu}); kasırga engelin üstüne çıkmaz. Chunk yüklü değilse ya da
     * zemin yoksa (boşluk) {@link #ZEMIN_YOK}.
     */
    static int zeminYuzeyi(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return ZEMIN_YOK;
        }
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int alt = Math.max(level.getMinBuildHeight(), ust - ZEMIN_TARAMA);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = ust - 1; y >= alt; y--) {
            if (zeminBlogu(level.getBlockState(p.set(x, y, z)))) {
                return y + 1;
            }
        }
        return ZEMIN_YOK;
    }

    /** Doğal zemin: toprak ailesi, kürek bloğu (kar, kil, çamur, kum, çakıl), ana kaya, kil, buz ya da sıvı. */
    private static boolean zeminBlogu(BlockState blok) {
        if (blok.isAir()) {
            return false;
        }
        return !blok.getFluidState().isEmpty() || zeminMi(blok) || blok.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || blok.is(BlockTags.BASE_STONE_OVERWORLD) || blok.is(BlockTags.TERRACOTTA) || blok.is(BlockTags.ICE);
    }

    /** Başlangıç noktasının Y'si gerçek zemine oturtulur (doğuş noktası ağaç ya da çatı üstünde olabilir). */
    private static Vec3 zemineOturtulmus(ServerLevel level, Vec3 k) {
        int zemin = zeminYuzeyi(level, Mth.floor(k.x), Mth.floor(k.z));
        return zemin == ZEMIN_YOK ? k : new Vec3(k.x, zemin, k.z);
    }

    @Override
    protected void baslarken(ServerLevel level) {
        konum = zemineOturtulmus(level, Vec3.atBottomCenterOf(baglam().merkez()));
        // Konuma başlatılan kasırga (hedef oyuncu yok) ilk bacağı rastgele bir yöne yürür.
        yon = Double.isNaN(ilkYon) ? rastgele.nextDouble() * TAM_TUR : ilkYon;
        hedefYon = yon;
        rota = KasirgaRotasi.yeni(konum.x, konum.z, yon, rastgele.nextLong());
        KasirgaSeviyesi s = seviyeBilgisi();
        DogalAfetler.LOGGER.info("[kasirga] başladı: seviye {} merkez {} rota tohumu {}, taban çapı {}, çekim yarıçapı {}",
                seviye(), baglam().merkez().toShortString(), rota.tohum(), s.tabanCapi(), s.cekimYaricapi());
        araHedefSec(level, konum);
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        tick++;
        isleyemeyenUcanlariBirak(level);
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
                hareketEt(level, adim);
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
        return zeminsizBitis || aktifGecen >= KasirgaEvresi.toplamAktif(seviyeBilgisi().aktifSureTick());
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
        t.putDouble("hedefYon", hedefYon);
        if (rota != null) {
            KasirgaRotasi.Durum d = rota.yaz();
            CompoundTag r = new CompoundTag();
            r.putDouble("bx", d.baslangicX());
            r.putDouble("bz", d.baslangicZ());
            r.putLong("tohum", d.tohum());
            r.putInt("sayac", d.sayac());
            r.putDouble("hx", d.hedefX());
            r.putDouble("hz", d.hedefZ());
            r.putDouble("sonYon", d.sonYon());
            r.putInt("kalan", d.kalanTick());
            r.putString("tur", d.sonTur().name());
            r.putBoolean("donus", d.sonDonus());
            t.put("rota", r);
        }
        t.putLong("aktifGecen", aktifGecen);
        t.putLong("kaldirilan", toplamKaldirilan);
    }

    @Override
    protected void ozelDurumOku(CompoundTag t) {
        if (t.contains("x")) {
            konum = new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
        }
        yon = t.getDouble("yon");
        hedefYon = t.getDouble("hedefYon");
        if (t.contains("rota", Tag.TAG_COMPOUND)) {
            CompoundTag r = t.getCompound("rota");
            KasirgaRotasi.Tur tur = KasirgaRotasi.Tur.RASTGELE;
            for (KasirgaRotasi.Tur aday : KasirgaRotasi.Tur.values()) {
                if (aday.name().equals(r.getString("tur"))) {
                    tur = aday;
                }
            }
            rota = KasirgaRotasi.oku(new KasirgaRotasi.Durum(r.getDouble("bx"), r.getDouble("bz"), r.getLong("tohum"),
                    r.getInt("sayac"), r.getDouble("hx"), r.getDouble("hz"), r.getDouble("sonYon"), r.getInt("kalan"), tur,
                    r.getBoolean("donus")));
        } else {
            // Rotasız eski kayıt: rota bulunduğu konumdan yeniden başlar.
            Vec3 k = konum();
            rota = KasirgaRotasi.yeni(k.x, k.z, yon, rastgele.nextLong());
        }
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
        if (rota.sayac() == 0) {
            araHedefSec(level, konum());
        } else {
            yolHedefiAyarla(rota.hedefX(), rota.hedefZ());
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

    /**
     * Ara hedefe yürür: zamanı gelince ({@link KasirgaRotasi#secimZamani}) yeni ara hedef, her {@link Yonelim#SAPMA_ARALIGI_TICK}
     * tick'te ara hedefin yönü çevresinde sapmalı yön. Entity-ticking alandan çıkacak adımda kasırga bu tick yerinde kalır ve
     * zorunlu yeni ara hedef ister ({@link #zorunluSecim}; yüklü adaylar önce gelir, kasırga içeri döner). Zaten yüklü alanın
     * dışındaysa (düşük simülasyon mesafesinde doğuş, kayıttan dönüş) K-01'deki gibi soyut ilerler; yükseklik yalnız yüklü
     * sütundan okunur. Rota başlangıcından {@link KasirgaRotasi#EN_COK_UZAKLIK} bloktan uzağa çıkan adım sınıra kırpılır ve
     * zorunlu yeni ara hedef istenir.
     *
     * <p>Zemin: huni tabanı her tick sütunun gerçek zeminine ({@link #zeminYuzeyi}) sıkıca oturur, kopmaz; bina, ağaç ve
     * benzeri engellerin üstüne çıkmaz. Önündeki engel seviyenin gücüyle yok edilir ({@link #onuTemizle}); yok edilemiyorsa (sert
     * ya da dayanıklı blok, aşırı dik yamaç, boşluk) kasırga yön değiştirir ({@link #kacisBaslat}).
     */
    private void hareketEt(ServerLevel level, int adim) {
        if (!hareketli || rota == null) {
            return;
        }
        Vec3 k = konum();
        zeminDenetimi(level, k);
        if (rota.secimZamani(adim, k.x, k.z)) {
            araHedefSec(level, k);
        } else if (tick % Yonelim.SAPMA_ARALIGI_TICK == 0) {
            hedefYon = Yonelim.sapmaliYon(Yonelim.yon(k.x, k.z, rota.hedefX(), rota.hedefZ()), rastgele.nextDouble());
        }
        double hizPayi = 1;
        if (kacisKalan > 0) {
            kacisKalan--;
            hedefYon = kacisYonu;
            yon += Math.max(-KACIS_DONUS, Math.min(KACIS_DONUS, Yonelim.aciFarki(hedefYon, yon)));
            if (Math.abs(Yonelim.aciFarki(hedefYon, yon)) > 0.25) {
                hizPayi = 0.15;
            }
        } else {
            yon = Yonelim.don(yon, hedefYon);
        }
        double hiz = seviyeBilgisi().hiz() / 20 * KasirgaEvresi.hizPayi(durum.evre()) * hizPayi;
        double nx = k.x + Math.cos(yon) * hiz;
        double nz = k.z + Math.sin(yon) * hiz;
        double uzaklik = rota.baslangicaUzaklik(nx, nz);
        boolean sinirda = uzaklik > KasirgaRotasi.EN_COK_UZAKLIK;
        if (sinirda) {
            double oran = KasirgaRotasi.EN_COK_UZAKLIK / uzaklik;
            nx = rota.baslangicX() + (nx - rota.baslangicX()) * oran;
            nz = rota.baslangicZ() + (nz - rota.baslangicZ()) * oran;
        }
        double y = k.y;
        BlockPos sonraki = BlockPos.containing(nx, y, nz);
        if (!level.isPositionEntityTicking(sonraki) && level.isPositionEntityTicking(BlockPos.containing(k))) {
            zorunluSecim(level, k);
            return;
        }
        int bx = Mth.floor(nx);
        int bz = Mth.floor(nz);
        if (level.hasChunk(bx >> 4, bz >> 4)) {
            KasirgaSeviyesi s = seviyeBilgisi();
            int zemin = zeminYuzeyi(level, bx, bz);
            if (zemin == ZEMIN_YOK || zemin - y > engelYuksekligi(s)) {
                kacisBaslat(level, k);
                return;
            }
            if (tick % ENGEL_ARALIGI == 0 && Kurallar.blokHasariAcik(level.getGameRules())
                    && level.isPositionEntityTicking(sonraki)) {
                int engel = onuTemizle(level, k, y);
                if (engel == ENGEL_KALDI) {
                    kacisBaslat(level, k);
                    return;
                } else if (engel == ENGEL_BEKLE) {
                    return;
                }
            }
            y = zemineOturt(y, zemin);
        }
        konum = new Vec3(nx, y, nz);
        if (sinirda) {
            zorunluSecim(level, konum);
        }
    }

    private static final int ENGEL_YOK = 0;
    /** Bu tick'in yıkım bütçesi bitti: engel yok edilebilir, kasırga bir sonraki denetime dek bekler. */
    private static final int ENGEL_BEKLE = 1;
    /** Yok edilemeyen engel (sert, dayanıklı, blok varlıklı): yön değişir. */
    private static final int ENGEL_KALDI = 2;

    /** Tabanın bir adımda çıkabileceği en yüksek zemin farkı (dik yamaç/kayalık engel sayılır): S1 4 → S5 8 blok. */
    private static double engelYuksekligi(KasirgaSeviyesi s) {
        return 3 + s.seviye();
    }

    /**
     * Huni tabanını zemine yumuşak ama sıkı oturtur: fark yarıya iner, tick başına en çok 1,5 blok yükselir ve 3 blok alçalır
     * (zemin çökerse taban havada asılı kalmaz); 0,05 blokta tam oturur.
     */
    private static double zemineOturt(double y, int zemin) {
        double fark = zemin - y;
        if (Math.abs(fark) < 0.05) {
            return zemin;
        }
        return y + Math.max(-3.0, Math.min(1.5, fark * 0.6));
    }

    /**
     * 10 tick'te bir bulunduğu sütunun zeminini denetler: {@link #ZEMINSIZ_SINIR} tick boyunca zemin yoksa (boşluk üstü, Uç)
     * kasırga sona erer ({@link #aktifBitti}). Yüklü olmayan chunk'ta karar verilmez.
     */
    private void zeminDenetimi(ServerLevel level, Vec3 k) {
        if (tick % 10 != 0) {
            return;
        }
        int x = Mth.floor(k.x);
        int z = Mth.floor(k.z);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        if (zeminYuzeyi(level, x, z) != ZEMIN_YOK) {
            zeminsizTick = 0;
            return;
        }
        zeminsizTick += 10;
        if (zeminsizTick >= ZEMINSIZ_SINIR && !zeminsizBitis) {
            zeminsizBitis = true;
            DogalAfetler.LOGGER.info("[kasirga] zemin yok ({}, boyut {}): kasırga sona eriyor",
                    BlockPos.containing(k).toShortString(), level.dimension().location());
        }
    }

    /**
     * Kasırganın önündeki (taban yarıçapı + 1,5 blok) üç sütunda, zeminden {@code 3 + 2·seviye} blok yukarıya kadar engel
     * bloklarını yok eder: seviyenin sertlik sınırı ve yıkım bütçesi içinde uçurur ya da parçalar ({@link #kaldir}); hiç
     * değişmeyen blok varsa ({@code #dogalafetler:kasirga_dayanikli}, blok varlıklı, sertlik sınırının üstü) {@link #ENGEL_KALDI}.
     * Çarpışması olmayan (bitki) bloklar engel değildir; zemin bloğu da değildir (taban onun üstüne oturur).
     */
    private int onuTemizle(ServerLevel level, Vec3 k, double y) {
        KasirgaSeviyesi s = seviyeBilgisi();
        KasirgaYikimi.Butce butce = KasirgaYikimi.butce(s.seviye(), s.blokKaldirmaTick(), 1, TpsOlcer.kismaCarpani());
        if (butce.deneme() <= 0) {
            return ENGEL_YOK;
        }
        YikimTuru tur = new YikimTuru(butce, KasirgaAyarlari.ucanBlokTavani());
        double mesafe = s.tabanYaricapi() + 1.5;
        int yukseklik = 3 + 2 * s.seviye();
        BlockPos.MutableBlockPos yer = new BlockPos.MutableBlockPos();
        int islem = Math.max(8, butce.deneme());
        int sonuc = ENGEL_YOK;
        try {
            for (int i = -1; i <= 1 && sonuc != ENGEL_KALDI; i++) {
                double aci = yon + i * 0.45;
                int x = Mth.floor(k.x + Math.cos(aci) * mesafe);
                int z = Mth.floor(k.z + Math.sin(aci) * mesafe);
                int zemin = zeminYuzeyi(level, x, z);
                if (zemin == ZEMIN_YOK) {
                    continue;
                }
                int ust = Mth.ceil(Math.max(y, zemin)) + yukseklik;
                for (int yy = zemin; yy <= ust; yy++) {
                    yer.set(x, yy, z);
                    BlockState blok = level.getBlockState(yer);
                    if (blok.isAir() || blok.getCollisionShape(level, yer).isEmpty()) {
                        continue;
                    }
                    if (!kaldirilabilir(level, yer, blok, s)) {
                        if (!ciftBlokKirilabilir(level, yer, blok, s)) {
                            sonuc = ENGEL_KALDI;
                            break;
                        }
                        if (islem <= 0 || !parcala(level, yer, blok, tur)) {
                            sonuc = ENGEL_BEKLE;
                            break;
                        }
                        tur.degisti(yer);
                        toplamKaldirilan++;
                        islem--;
                        continue;
                    }
                    if (islem <= 0 || tur.tukendi(ucanlar.size()) || !kaldir(level, yer, blok, tur, true)) {
                        sonuc = ENGEL_BEKLE;
                        break;
                    }
                    islem--;
                }
            }
        } finally {
            tur.bildir(level);
        }
        return sonuc;
    }

    /** Çift blok (kapı, uzun ot) uçurulmaz ama kırılır: blok varlığı ve dayanıklı etiketi dışında, sertlik sınırı içinde. */
    private static boolean ciftBlokKirilabilir(ServerLevel level, BlockPos yer, BlockState blok, KasirgaSeviyesi s) {
        if (blok.hasBlockEntity() || blok.is(KasirgaKayitlari.DAYANIKLI) || !blok.getFluidState().isEmpty()
                || !blok.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return false;
        }
        float sertlik = blok.getDestroySpeed(level, yer);
        return sertlik >= 0 && sertlik <= s.sertlikSiniri();
    }

    /**
     * Temizlenemeyen engel ya da boşluk önünde: solda ve sağda 57° açıdaki zemin farkına bakıp daha engelsiz yana döner;
     * {@link #KACIS_TICK} boyunca rota hedefi yerine bu yön izlenir, dönerken kasırga neredeyse durur. Kaçış sürerken yeniden
     * seçilmez.
     */
    private void kacisBaslat(ServerLevel level, Vec3 k) {
        if (kacisKalan > 0) {
            return;
        }
        double sol = engelSkoru(level, k, yon - KACIS_ACISI);
        double sag = engelSkoru(level, k, yon + KACIS_ACISI);
        int yan = sol < sag ? -1 : sol > sag ? 1 : rastgele.nextBoolean() ? 1 : -1;
        kacisYonu = yon + yan * KACIS_ACISI;
        kacisKalan = KACIS_TICK;
    }

    /** Yön için engel puanı: boşluk 1000, aksi halde taban üstündeki zemin farkı (küçük iyi). */
    private double engelSkoru(ServerLevel level, Vec3 k, double aci) {
        double mesafe = seviyeBilgisi().tabanYaricapi() + 4;
        int x = Mth.floor(k.x + Math.cos(aci) * mesafe);
        int z = Mth.floor(k.z + Math.sin(aci) * mesafe);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return 0;
        }
        int zemin = zeminYuzeyi(level, x, z);
        return zemin == ZEMIN_YOK ? 1000 : Math.max(0, zemin - k.y);
    }

    /**
     * Sınırda ya da yüklü alan kenarında (AF2-25): son seçimden {@link KasirgaRotasi#ZORUNLU_ARALIK} geçtiyse yeni ara hedef;
     * geçmediyse sapmasız eldeki ara hedefe döner (yön dışarıyı göstermeye devam etmesin, tick başına seçim olmasın).
     */
    private void zorunluSecim(ServerLevel level, Vec3 k) {
        if (rota.zorunluSecimSerbest()) {
            araHedefSec(level, k);
        } else {
            hedefYon = Yonelim.yon(k.x, k.z, rota.hedefX(), rota.hedefZ());
        }
    }

    /**
     * Yeni ara hedef: oyuncular (izleyici olmayan, canlı) ve entity-ticking yüklülük rotaya verilir; yön hemen yeni ara hedefe
     * çevrilir (sapmalı), radara bildirilir.
     */
    private void araHedefSec(ServerLevel level, Vec3 k) {
        List<double[]> oyuncular = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.isAlive()) {
                oyuncular.add(new double[] {p.getX(), p.getZ()});
            }
        }
        KasirgaRotasi.Secim s = rota.yeniAraHedef(k.x, k.z, oyuncular,
                (x, z) -> level.isPositionEntityTicking(BlockPos.containing(x, k.y, z)));
        hedefYon = Yonelim.sapmaliYon(Yonelim.yon(k.x, k.z, s.x(), s.z()), rastgele.nextDouble());
        yolHedefiAyarla(s.x(), s.z());
        if (OTOMASYON) {
            DogalAfetler.LOGGER.info("[kasirga] ara hedef {}: ({}, {}) tür {}{}, uzaklık {}, başlangıca {}", s.no(),
                    Math.round(s.x()), Math.round(s.z()), s.tur(), s.donus() ? " dönüş" : "", Math.round(s.uzaklik()),
                    Math.round(s.baslangicaUzaklik()));
        }
    }

    /**
     * Kn4: varlık işlemeyen konumdaki uçan blok donar ve chunk boşalınca kaydedilmediği ({@code noSave}) için bloğuyla silinir;
     * o konumda hemen yere bırakılır (chunk yüklüyse blok olarak, değilse eşyası da kaybolmadan önce).
     */
    void isleyemeyenUcanlariBirak(ServerLevel level) {
        for (UcanBlokVarligi blok : ucanlar) {
            if (!blok.isRemoved() && !level.isPositionEntityTicking(blok.blockPosition())) {
                blok.yereBirak();
            }
        }
        ucanlar.removeIf(Entity::isRemoved);
    }

    /** GameTest: uçan bloklar listesine elle ekler. */
    void ucanEkle(UcanBlokVarligi blok) {
        ucanlar.add(blok);
    }

    /** Tepeden fırlatma rampası 0-1 (ömrün son %13'ü; öncesinde ve siren aşamasında 0). */
    private double firlatmaRampi() {
        return KasirgaEvresi.firlatma(aktifGecen, seviyeBilgisi().aktifSureTick());
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
            yeni.bagla(this, seviye(), (float) carpanlar().boyut(), (float) carpanlar().cekim(), (float) kaldirmaTavani());
            yeni.guncelle(durum, (float) yon, (float) firlatmaRampi());
            varlik = yeni;
            level.addFreshEntity(yeni);
        } else if (!isler) {
            varlik.discard();
            varlik = null;
        } else {
            varlik.setPos(k.x, k.y, k.z);
            varlik.guncelle(durum, (float) yon, (float) firlatmaRampi());
        }
    }

    /**
     * Tick başına {@link KasirgaYikimi#butce} kadar sütun denemesi (seviye × evre payı × TPS kısması): yıkım yarıçapında
     * (çekim yarıçapının %60'ı) rastgele sütun, seviyeye ve merkeze uzaklığa göre {@link KasirgaYikimi#cekimOlasiligi} ile
     * çekilir ve {@link #sutunuIsle} ile yukarıdan aşağı taranır. Uçan blok tavanı doluysa blok parçalanır (kırılma
     * parçacığı ve sesi), tick başına en çok bütçenin parça sayısı kadar; eşya payı K-04'ün tick başına eşya hızını aşmaz
     * (her kırılan blok eşya düşürseydi S5'te 15 sn'de ~3800 eşya varlığı oluşuyor ve tick 40 ms'ye çıkıyordu).
     */
    private void bloklariKaldir(ServerLevel level) {
        KasirgaSeviyesi s = seviyeBilgisi();
        KasirgaYikimi.Butce butce = KasirgaYikimi.butce(s.seviye(), s.blokKaldirmaTick(), KasirgaEvresi.kaldirmaPayi(durum),
                TpsOlcer.kismaCarpani());
        if (butce.deneme() <= 0) {
            return;
        }
        YikimTuru tur = new YikimTuru(butce, KasirgaAyarlari.ucanBlokTavani());
        Vec3 k = konum();
        double yikim = KasirgaYikimi.yikimYaricapi(s.cekimYaricapi());
        for (int i = 0; i < butce.deneme() && !tur.tukendi(ucanlar.size()); i++) {
            double aci = rastgele.nextDouble() * TAM_TUR;
            double r = yikim * Math.sqrt(rastgele.nextDouble());
            if (rastgele.nextDouble() >= KasirgaYikimi.cekimOlasiligi(s.seviye(), r, s.tabanYaricapi(), yikim)) {
                continue;
            }
            int x = Mth.floor(k.x + Math.cos(aci) * r);
            int z = Mth.floor(k.z + Math.sin(aci) * r);
            if (level.hasChunk(x >> 4, z >> 4)) {
                sutunuIsle(level, x, z, s, tur, true);
            }
        }
        tur.bildir(level);
    }

    /**
     * GameTest: {@code (x, z)} sütununu tam evre payı ve kısmasız bir tick bütçesiyle işler; {@code ucurabilir=false} uçan
     * blok tavanı doluymuş gibidir (bloklar parçalanır, gövde yatırılır). {@code afetBlokHasari=false} iken hiçbir şey yapmaz.
     */
    int sutunuIsleTest(ServerLevel level, int x, int z, boolean ucurabilir) {
        if (!Kurallar.blokHasariAcik(level.getGameRules())) {
            return 0;
        }
        KasirgaSeviyesi s = seviyeBilgisi();
        YikimTuru tur = new YikimTuru(KasirgaYikimi.butce(s.seviye(), s.blokKaldirmaTick(), 1, 1),
                KasirgaAyarlari.ucanBlokTavani());
        int kalkan = sutunuIsle(level, x, z, s, tur, ucurabilir);
        tur.bildir(level);
        return kalkan;
    }

    /**
     * Sütunu en üst bloktan aşağı {@link KasirgaYikimi#derinlik} kadar tarar (hava atlanır ama sayılır). Kaldırılamayan ya da
     * seviyenin bu derinlikte almadığı ilk blokta durur: sığınağın altı kazılmaz. S4-5'te dikey kütüğe varınca gövde
     * devrilir ({@link #govdeyiDevir}) ve tarama biter. Döner: sütundan kalkan blok sayısı.
     */
    private int sutunuIsle(ServerLevel level, int x, int z, KasirgaSeviyesi s, YikimTuru tur, boolean ucurabilir) {
        int ust = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int alt = Math.max(level.getMinBuildHeight(), ust - KasirgaYikimi.derinlik(s.seviye()) + 1);
        BlockPos.MutableBlockPos yer = new BlockPos.MutableBlockPos();
        int kalkan = 0;
        for (int y = ust; y >= alt; y--) {
            yer.set(x, y, z);
            BlockState blok = level.getBlockState(yer);
            if (blok.isAir()) {
                continue;
            }
            if (!kaldirilabilir(level, yer, blok, s) || !KasirgaYikimi.alinir(s.seviye(), sinif(level, yer, blok), ust - y)) {
                break;
            }
            if (KasirgaYikimi.devirir(s.seviye()) && dikeyKutuk(blok)) {
                kalkan += govdeyiDevir(level, yer.immutable(), s, tur, ucurabilir);
                break;
            }
            if (!kaldir(level, yer, blok, tur, ucurabilir)) {
                break;
            }
            kalkan++;
        }
        return kalkan;
    }

    /** Bloğu uçurur ya da parçalar; ikisine de bütçe yoksa {@code false} (blok yerinde kalır). */
    private boolean kaldir(ServerLevel level, BlockPos yer, BlockState blok, YikimTuru tur, boolean ucurabilir) {
        if (ucabilir(level, yer, tur, ucurabilir)) {
            ucur(level, yer, blok);
        } else if (!parcala(level, yer, blok, tur)) {
            return false;
        }
        tur.degisti(yer);
        toplamKaldirilan++;
        return true;
    }

    private boolean ucabilir(ServerLevel level, BlockPos yer, YikimTuru tur, boolean ucurabilir) {
        return ucurabilir && ucanlar.size() < tur.tavan && level.isPositionEntityTicking(yer);
    }

    private void ucur(ServerLevel level, BlockPos yer, BlockState blok) {
        level.removeBlock(yer, false);
        Vec3 k = konum();
        double dx = yer.getX() + 0.5 - k.x;
        double dz = yer.getZ() + 0.5 - k.z;
        UcanBlokVarligi ucan = UcanBlokVarligi.olustur(level, blok, this, varlik != null ? varlik.getId() : -1,
                Math.atan2(dz, dx), Math.hypot(dx, dz), yer.getY() - k.y);
        level.addFreshEntity(ucan);
        ucanlar.add(ucan);
    }

    /** Parça bütçesi varsa blok kırılır (vanilla parçacık ve ses), yalnız tick'in eşya payında eşya düşer. */
    private boolean parcala(ServerLevel level, BlockPos yer, BlockState blok, YikimTuru tur) {
        if (tur.parca <= 0) {
            return false;
        }
        tur.parca--;
        level.destroyBlock(yer, false);
        if (rastgele.nextDouble() < tur.esyaPayi) {
            Block.dropResources(blok, level, yer);
        }
        return true;
    }

    /**
     * S4-5 ağaç devrilmesi (K-05 devrik kütük): {@code tepe}'den aşağı kesintisiz dikey kütükler tepeden başlayarak tick'in
     * kütük bütçesi kadar kalkar. Tavanda yer varsa kütük uçar; yoksa rüzgârın teğet yönüne ({@link
     * KasirgaYikimi#devrilmeYonu}) yatar: tepeden {@code j}. kütük gövde dibinden {@code boy - j} blok ötedeki zemine yatay
     * konur. Yer yoksa parçalanır. Yatırılan kütük de parça bütçesinden düşer: tavan doluyken tick başına değişen blok
     * K-04'ün parça tavanını aşmaz (AF-23 rev.); bütçe bitince gövdenin kalanı kütük kalır, sonraki ziyaret sürdürür.
     */
    private int govdeyiDevir(ServerLevel level, BlockPos tepe, KasirgaSeviyesi s, YikimTuru tur, boolean ucurabilir) {
        int x = tepe.getX();
        int z = tepe.getZ();
        BlockPos.MutableBlockPos m = tepe.mutable();
        int boy = 0;
        while (boy < GOVDE_TARAMA && m.getY() >= level.getMinBuildHeight()) {
            BlockState b = level.getBlockState(m);
            if (!dikeyKutuk(b) || !kaldirilabilir(level, m, b, s)) {
                break;
            }
            boy++;
            m.move(Direction.DOWN);
        }
        int dipY = tepe.getY() - boy + 1;
        Vec3 k = konum();
        int[] yon = KasirgaYikimi.devrilmeYonu(x + 0.5 - k.x, z + 0.5 - k.z);
        Direction.Axis eksen = yon[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
        int devrilen = 0;
        for (int j = 0; j < boy && tur.kutuk > 0; j++) {
            m.set(x, tepe.getY() - j, z);
            BlockState kutuk = level.getBlockState(m);
            if (ucabilir(level, m, tur, ucurabilir)) {
                ucur(level, m, kutuk);
            } else {
                if (tur.parca <= 0) {
                    break;
                }
                BlockPos yatis = yatisYeri(level, x + yon[0] * (boy - j), z + yon[1] * (boy - j), dipY);
                if (yatis != null) {
                    tur.parca--;
                    level.removeBlock(m, false);
                    level.setBlock(yatis, kutuk.hasProperty(BlockStateProperties.AXIS)
                            ? kutuk.setValue(BlockStateProperties.AXIS, eksen) : kutuk, Block.UPDATE_ALL);
                    tur.degisti(yatis);
                } else {
                    parcala(level, m, kutuk, tur);
                }
            }
            tur.degisti(m);
            tur.kutuk--;
            toplamKaldirilan++;
            devrilen++;
        }
        return devrilen;
    }

    /**
     * Devrik kütüğün yeri: {@code (x, z)} sütununda yaprak saymayan en üst katı bloğun üstü; gövde dibinin ±2 bloğunda,
     * boş (değiştirilebilir: hava, ot) ve suyun üstü değil. Uygun değilse {@code null}.
     */
    private static BlockPos yatisYeri(ServerLevel level, int x, int z, int dipY) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - dipY) > YATIS_YUKSEKLIK_FARKI) {
            return null;
        }
        BlockPos yer = new BlockPos(x, y, z);
        BlockState mevcut = level.getBlockState(yer);
        return mevcut.canBeReplaced() && mevcut.getFluidState().isEmpty() && level.getFluidState(yer.below()).isEmpty()
                ? yer : null;
    }

    private static boolean dikeyKutuk(BlockState blok) {
        return blok.is(BlockTags.LOGS) && (!blok.hasProperty(BlockStateProperties.AXIS)
                || blok.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y);
    }

    /** Bloğun yıkım sınıfı: yaprak, kütük ve ahşap yapı etiketlerden (N12), zemin {@link #zeminMi}, anında kırılan bitki. */
    private static KasirgaYikimi.Sinif sinif(ServerLevel level, BlockPos yer, BlockState blok) {
        if (blok.is(BlockTags.LEAVES)) {
            return KasirgaYikimi.Sinif.YAPRAK;
        }
        if (blok.is(BlockTags.LOGS)) {
            return KasirgaYikimi.Sinif.KUTUK;
        }
        if (blok.is(BlockTags.PLANKS) || blok.is(BlockTags.WOODEN_STAIRS) || blok.is(BlockTags.WOODEN_SLABS)
                || blok.is(BlockTags.WOODEN_FENCES) || blok.is(BlockTags.FENCE_GATES) || blok.is(BlockTags.WOODEN_TRAPDOORS)) {
            return KasirgaYikimi.Sinif.AHSAP_YAPI;
        }
        if (zeminMi(blok)) {
            return KasirgaYikimi.Sinif.ZEMIN;
        }
        return blok.getDestroySpeed(level, yer) == 0 ? KasirgaYikimi.Sinif.BITKI : KasirgaYikimi.Sinif.DIGER;
    }

    /** Bir tick'in kalan yıkım bütçesi ve değişen chunk'ları ({@link SanalSu#chunkDegisti} bir kez, tick sonunda). */
    private static final class YikimTuru {
        final int tavan;
        final double esyaPayi;
        final LongSet degisen = new LongOpenHashSet();
        int parca;
        int kutuk;

        YikimTuru(KasirgaYikimi.Butce butce, int tavan) {
            this.tavan = tavan;
            this.esyaPayi = butce.esyaPayi();
            this.parca = butce.parca();
            this.kutuk = butce.kutuk();
        }

        /** Tavan dolu ve parça bütçesi bitti (yatırılan kütük de ondan düşer): bu tick başka blok kalkamaz. */
        boolean tukendi(int ucan) {
            return parca <= 0 && ucan >= tavan;
        }

        void degisti(BlockPos p) {
            degisen.add(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
        }

        void bildir(ServerLevel level) {
            degisen.forEach((long c) -> SanalSu.chunkDegisti(level, new ChunkPos(c)));
        }
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

    /** Kaldırma tavanı (blok): config'in S5 değeri bu kasırganın yüksekliğiyle orantılı ({@link KasirgaSeviyesi#kaldirmaTavani}). */
    private double kaldirmaTavani() {
        return seviyeBilgisi().kaldirmaTavani(KasirgaAyarlari.kaldirmaTavani());
    }

    /**
     * Çekim, yörünge, kaldırma, tepeden fırlatma ve çekirdek hasarı ({@link KasirgaCekimi}): çekim yarıçapındaki
     * mob, eşya ve sahipsiz araçlar huninin çevresinde yükselerek döner, ömrün son %13'ünde huninin tepesinden dışa ve yukarı
     * fırlatılır; hızları burada değişir. Oyuncunun ve sürdüğü aracın hareketinin sahibi oyuncunun istemcisidir, çekim orada uygulanır (AF3-01): sunucu
     * onların hızını ezmez (END tick'inde {@code getX() - xo} her zaman 0'dır, eski hız tahmini çekimi biriktirmiyordu). Kaldırma
     * etkiliyken sunucu yalnız düşme mesafesini (düşme hasarı sunucuda) ve oyuncunun uçma sayacını sıfırlar (AF3-08: huni oyuncuyu
     * tavanda asılı tutar; kaldırma bölgesinden çıkan oyuncuda vanilla denetimi aynen sürer). Hasar saniyede bir, huni tabanda.
     */
    private void varliklariCek(ServerLevel level) {
        double firlatma = firlatmaRampi();
        double guc = Math.max(KasirgaEvresi.guc(durum), firlatma);
        if (guc <= 0) {
            return;
        }
        KasirgaSeviyesi s = seviyeBilgisi();
        KasirgaCekimi.Kaynak kaynak = new KasirgaCekimi.Kaynak(konum(), s, guc * carpanlar().cekim(), kaldirmaTavani(),
                firlatma * carpanlar().cekim());
        Vec3 k = kaynak.merkez();
        double r = s.cekimYaricapi();
        AABB kutu = new AABB(k.x - r, k.y - 6, k.z - r, k.x + r, k.y + s.yukseklik(), k.z + r);
        boolean hasarTiki = tick % 20 == 0;
        double hasar = s.cekirdekHasari() * KasirgaEvresi.guc(durum);
        for (Entity e : level.getEntities((Entity) null, kutu, KasirgaCekimi::cekilebilir)) {
            double[] ivme = KasirgaCekimi.ivme(e, kaynak);
            if (KasirgaCekimi.istemciYonetir(e)) {
                if (ivme[1] > 0) {
                    e.resetFallDistance();
                    if ((e instanceof ServerPlayer p ? p : e.getControllingPassenger()) instanceof ServerPlayer surucu) {
                        ucmaDenetiminiSifirla(surucu);
                    }
                }
            } else if (KasirgaCekimi.uygula(e, ivme, kaynak.firlatma())) {
                e.hasImpulse = true;
            }
            double h = e.getY() - k.y;
            if (hasarTiki && hasar > 0 && e instanceof LivingEntity canli && Math.hypot(e.getX() - k.x, e.getZ() - k.z)
                    < s.hasarYaricapi() && h < s.yukseklik() * 0.5) {
                canli.hurt(KasirgaKayitlari.hasar(level), (float) Math.max(0.5, hasar));
            }
        }
    }

    /** Kaldırma etkiliyken oyuncunun (ve sürdüğü aracın) uçma sayacı sıfırlanır; bağlantısı olmayan oyuncu (çıkış anı) atlanır. */
    private static void ucmaDenetiminiSifirla(ServerPlayer oyuncu) {
        io.github.kadirberkpolat1.dogalafetler.basarim.BasarimOlaylari.kasirgaKaldirdi(oyuncu); // K-E74 "Bir Tur Attım"
        if (oyuncu.connection == null) {
            return;
        }
        try {
            UCMA_SAYACI.setInt(oyuncu.connection, 0);
            ARAC_UCMA_SAYACI.setInt(oyuncu.connection, 0);
        } catch (IllegalAccessException hata) {
            throw new IllegalStateException("uçma sayacına erişilemedi", hata);
        }
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

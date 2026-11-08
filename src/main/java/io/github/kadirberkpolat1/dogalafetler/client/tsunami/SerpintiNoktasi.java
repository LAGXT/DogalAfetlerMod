package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuGizleme;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuRenderer;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dalga serpintisi ({@code IstemciSu.serpinti}) ve köpüğünün ({@link TsunamiKopugu}) doğuş noktasının dünya sorgusu (karar
 * {@link SerpintiKarari}'nda) ve otomasyon ölçümü. Nokta başına en çok bir çizilen ağ okuması (yakın tampon ya da uzak LOD
 * karosu), bir yükseklik haritası ve bir blok okuması (TS-16 rev. 3 bütçesi); çizilen su yoksa harita ve blok okunmaz.
 * İstemci tick'i ve render (aynı thread).
 */
public final class SerpintiNoktasi {
    private static final BlockPos.MutableBlockPos P = new BlockPos.MutableBlockPos();
    /** Vanilla {@code LevelRenderer.addParticle} kameradan 32 bloktan uzaktaki (zorlanmamış) parçacığı doğurmaz. */
    private static final double PARCACIK_MENZILI = 32;
    private static final int IYI = 0;
    private static final int HAVADA = 1;
    private static final int BLOK = 2;
    private static final int CIZIMSIZ = 3;
    /** Otomasyon: son KARE satırından bu yana doğan serpinti parçacıkları, doğuş konumunun sınıfına göre ({@link #sinif}). */
    private static final int[] SERPINTI = new int[4];
    private static final int KOPUK_EN_COK = 8192;
    /** Otomasyon: son çizilen karenin köpük noktaları (x, y, z); üretimde boş dizi. */
    private static final double[] KOPUK = new double[GorselOtomasyon.AKTIF ? 3 * KOPUK_EN_COK : 0];
    private static int kopuk;

    private SerpintiNoktasi() {
    }

    /**
     * {@code (x, z)}'de bu karede çizilen su üstünün {@code kalkis} blok üstü (parçacığın ya da köpüğün doğduğu Y);
     * {@link SerpintiKarari#karar} doğmaz diyorsa NaN: çizilen su yok (yakın ağ boş ya da gecikmiş dahil), nokta blok içinde,
     * çatı altında ya da havada.
     */
    public static double dogusYuksekligi(ClientLevel l, double x, double z, double kalkis) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        boolean kapsanan = SuRenderer.yakinKapsar(bx, bz);
        double ust = SerpintiKarari.yuzey(kapsanan, kapsanan ? SuRenderer.yakinSuUstu(bx, bz) : Double.NaN,
                kapsanan ? Double.NaN : SuRenderer.uzakSuUstu(bx, bz));
        if (Double.isNaN(ust)) {
            return Double.NaN;
        }
        double y = ust + kalkis;
        int tepe = l.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        P.set(bx, Mth.floor(y), bz);
        boolean kati = !l.getBlockState(P).getCollisionShape(l, P).isEmpty();
        return SerpintiKarari.karar(y, ust, tepe, kati) == SerpintiKarari.Sonuc.DOGAR ? y : Double.NaN;
    }

    /**
     * Köpük bore'unun tabanı: {@code (x, z)}'de görünen su (sanal su: {@link #dogusYuksekligi}; yoksa sütunun en üstündeki,
     * çekilmenin gizlemediği gerçek su); ikisi de yoksa (kuru arazi, açığa çıkan dip) NaN.
     */
    static double gorunenSu(ClientLevel l, double x, double z) {
        double y = dogusYuksekligi(l, x, z, 0);
        if (!Double.isNaN(y)) {
            return y;
        }
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        P.set(bx, l.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) - 1, bz);
        FluidState f = l.getFluidState(P);
        return f.isEmpty() || SuGizleme.gizliMi(l, P, f) ? Double.NaN : P.getY() + f.getHeight(l, P);
    }

    /** Otomasyon ölçümü: {@code (x, y, z)}'de doğurulan serpinti parçacığı (vanilla'nın doğurmadığı uzak parçacık sayılmaz). */
    public static void dogdu(ClientLevel l, double x, double y, double z) {
        if (!GorselOtomasyon.AKTIF) {
            return;
        }
        Vec3 kam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (kam.distanceToSqr(x, y, z) <= PARCACIK_MENZILI * PARCACIK_MENZILI) {
            SERPINTI[sinif(l, x, y, z)]++;
        }
    }

    /** Otomasyon: yeni köpük karesi başlıyor (ölçüt yalnız son çizilen karenin köpüğüne bakar). */
    static void kopukKaresi() {
        kopuk = 0;
    }

    /** Otomasyon: bu karede çizilen bir köpük noktası. */
    static void kopukNoktasi(double x, double y, double z) {
        if (!GorselOtomasyon.AKTIF || kopuk >= KOPUK_EN_COK) {
            return;
        }
        KOPUK[3 * kopuk] = x;
        KOPUK[3 * kopuk + 1] = y;
        KOPUK[3 * kopuk + 2] = z;
        kopuk++;
    }

    /**
     * Otomasyon kabul ölçütü (KARE satırı), doğuran kararın girdisiyle değil noktanın son konumunda SuRenderer'ın bu karede
     * herhangi bir ağla gerçekten çizdiği su üstüyle ({@link SuRenderer#cizilenUst}) ve bloğun tam çarpışma kutularıyla:
     * {@code serpinti_dogan} son KARE satırından bu yana doğan serpinti parçacığı, {@code kopuk_nokta} son çizilen karenin
     * köpük noktası (tepe ve ön yüz örnekleri; bore gerçek suya da oturduğu için sayılmaz); serpinti ve köpük toplamı olarak
     * {@code serpinti_havada} (çizilen su üstünden {@link SerpintiKarari#HAVA_PAYI}'ndan yüksekte), {@code serpinti_blok_icinde}
     * (bir bloğun çarpışma şeklinin içinde), {@code serpinti_cizimsiz_su} (noktada hiçbir ağ su çizmiyor: yakın ağ boş ya da
     * gecikmiş, kuru sütun; analitik ya da sanal su seviyesi sorulmaz). Kabul: üçü de 0. Serpinti sayıları sıfırlanır.
     */
    static String olcut() {
        int[] say = SERPINTI.clone();
        ClientLevel l = Minecraft.getInstance().level;
        for (int i = 0; i < kopuk && l != null; i++) {
            say[sinif(l, KOPUK[3 * i], KOPUK[3 * i + 1], KOPUK[3 * i + 2])]++;
        }
        String s = String.format(Locale.ROOT,
                "serpinti_dogan=%d kopuk_nokta=%d serpinti_havada=%d serpinti_blok_icinde=%d serpinti_cizimsiz_su=%d",
                Arrays.stream(SERPINTI).sum(), kopuk, say[HAVADA], say[BLOK], say[CIZIMSIZ]);
        Arrays.fill(SERPINTI, 0);
        return s;
    }

    /** Noktanın sınıfı: blok şeklinin içinde, çizimsiz su üstü, havada ya da iyi. */
    private static int sinif(ClientLevel l, double x, double y, double z) {
        int bx = Mth.floor(x);
        int by = Mth.floor(y);
        int bz = Mth.floor(z);
        P.set(bx, by, bz);
        for (AABB k : l.getBlockState(P).getCollisionShape(l, P).toAabbs()) {
            if (k.contains(x - bx, y - by, z - bz)) {
                return BLOK;
            }
        }
        double ust = SuRenderer.cizilenUst(bx, bz);
        return Double.isNaN(ust) ? CIZIMSIZ : y - ust > SerpintiKarari.HAVA_PAYI ? HAVADA : IYI;
    }
}

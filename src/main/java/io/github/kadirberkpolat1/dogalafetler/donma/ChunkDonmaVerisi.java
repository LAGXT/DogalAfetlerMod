package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Chunk başına donma durumu (K-D1, K0-24): son işlenen global ilerleme ({@code sonG}), o işlemin dönemi (donma / çözülme),
 * süren adımın bitişi (yarım adım: chunk adım ortasında boşalırsa işlenmiş sütunlar ileride kalır), sıfırlama döngüsü,
 * sütun başına "bizim koyduğumuz" kar ve buz bitleri ve buharlaşma kayıtları (N9: kuruyan sütunun orijinal su yüzeyi ve
 * kaldırılan blok sayısı, yalnız kuruyan sütunlar). Bit işaretli olmayan kar ve buza (doğal ya da oyuncunun) dokunulmaz.
 * Yalnız sunucuda, yalnız üst dünyada bağlanır; chunk NBT'sinin {@code ForgeCaps} alanına chunk'la birlikte yazılır.
 */
public final class ChunkDonmaVerisi {
    public static final Capability<ChunkDonmaVerisi> YETENEK = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation KIMLIK = DogalAfetler.konum("donma");
    private static final int YARIM_YOK = -1;

    private byte sonG;
    private boolean cozulme;
    private long dongu;
    private byte sonBuhar;
    private int yarimG = YARIM_YOK;
    private boolean yarimCozulme;
    private final long[] kar = new long[4];
    private final long[] buz = new long[4];
    /** Kuruyan sütunun orijinal su yüzeyi (Y); kayıt yoksa {@code null}. */
    private short[] kuruUstY;
    private byte[] kuruSayi;

    public int sonG() {
        return sonG;
    }

    public boolean cozulme() {
        return cozulme;
    }

    public long dongu() {
        return dongu;
    }

    /** Chunk'a son uygulanan buharlaşma derinliği (blok). */
    public int sonBuhar() {
        return sonBuhar;
    }

    public void sonBuhar(int derinlik) {
        sonBuhar = (byte) Math.max(0, Math.min(127, derinlik));
    }

    public void durum(int g, boolean cozulmeDonemi) {
        sonG = (byte) Math.max(0, Math.min(100, g));
        cozulme = cozulmeDonemi;
        yarimG = YARIM_YOK;
    }

    /** Süren adımın bitişi; adım bitince {@link #durum} siler. */
    public void yarimAdim(int g, boolean cozulmeDonemi) {
        yarimG = Math.max(0, Math.min(100, g));
        yarimCozulme = cozulmeDonemi;
    }

    public boolean yarimVar() {
        return yarimG != YARIM_YOK;
    }

    public int yarimG() {
        return yarimG;
    }

    public boolean yarimCozulme() {
        return yarimCozulme;
    }

    /**
     * {@code /dogalafet sifirla} sonrası ilk görülüş: bloklar çözülmeden yalnız veri sıfırlanır. Chunk yeni dönemin
 * ilerlemesini benimser; bitler ve buharlaşma kayıtları silinir (kalan kar/buz artık yabancıdır).
     */
    public void donguyeGec(long yeniDongu, int g, boolean cozulmeDonemi) {
        dongu = yeniDongu;
        durum(g, cozulmeDonemi);
        Arrays.fill(kar, 0L);
        Arrays.fill(buz, 0L);
        kuruUstY = null;
        kuruSayi = null;
        sonBuhar = 0;
    }

    /** Chunk içi sütun indeksi, 0–255. */
    public static int indeks(int x, int z) {
        return (z & 15) << 4 | (x & 15);
    }

    public boolean kar(int i) {
        return bit(kar, i);
    }

    public void kar(int i, boolean deger) {
        bit(kar, i, deger);
    }

    public boolean buz(int i) {
        return bit(buz, i);
    }

    public void buz(int i, boolean deger) {
        bit(buz, i, deger);
    }

    /** Sütunda kurumuş blok sayısı (yoksa 0). */
    public int kuruSayi(int i) {
        return kuruSayi == null ? 0 : kuruSayi[i];
    }

    public int kuruUstY(int i) {
        return kuruUstY[i];
    }

    public boolean kuruVar() {
        return kuruSayi != null;
    }

    /** Buharlaşma kaydını yazar; {@code sayi == 0} kaydı siler. */
    public void kuru(int i, int ustY, int sayi) {
        if (sayi <= 0) {
            if (kuruSayi != null) {
                kuruSayi[i] = 0;
                for (byte b : kuruSayi) {
                    if (b != 0) {
                        return;
                    }
                }
                kuruSayi = null;
                kuruUstY = null;
            }
            return;
        }
        if (kuruSayi == null) {
            kuruSayi = new byte[256];
            kuruUstY = new short[256];
        }
        kuruSayi[i] = (byte) Math.min(127, sayi);
        kuruUstY[i] = (short) ustY;
    }

    private static boolean bit(long[] k, int i) {
        return (k[i >>> 6] >>> (i & 63) & 1L) != 0;
    }

    private static void bit(long[] k, int i, boolean deger) {
        if (deger) {
            k[i >>> 6] |= 1L << (i & 63);
        } else {
            k[i >>> 6] &= ~(1L << (i & 63));
        }
    }

    static void kaydet(RegisterCapabilitiesEvent olay) {
        olay.register(ChunkDonmaVerisi.class);
    }

    static void ekle(AttachCapabilitiesEvent<LevelChunk> olay) {
        Level level = olay.getObject().getLevel();
        if (level == null || level.isClientSide() || level.dimension() != Level.OVERWORLD) {
            return;
        }
        olay.addCapability(KIMLIK, new Saglayici());
    }

    @Nullable
    public static ChunkDonmaVerisi al(LevelChunk chunk) {
        return chunk.getCapability(YETENEK).orElse(null);
    }

    CompoundTag yaz() {
        CompoundTag etiket = new CompoundTag();
        etiket.putByte("g", sonG);
        etiket.putBoolean("cozulme", cozulme);
        etiket.putLong("dongu", dongu);
        etiket.putByte("buhar", sonBuhar);
        if (yarimVar()) {
            etiket.putByte("yarimG", (byte) yarimG);
            etiket.putBoolean("yarimCozulme", yarimCozulme);
        }
        etiket.putLongArray("kar", kar);
        etiket.putLongArray("buz", buz);
        if (kuruSayi != null) {
            int n = 0;
            int[] paket = new int[256];
            for (int i = 0; i < 256; i++) {
                if (kuruSayi[i] != 0) {
                    paket[n++] = i | (kuruSayi[i] & 0x7F) << 8 | (kuruUstY[i] & 0xFFFF) << 16;
                }
            }
            etiket.putIntArray("kuru", Arrays.copyOf(paket, n));
        }
        return etiket;
    }

    void oku(CompoundTag c) {
        sonG = (byte) Math.max(0, Math.min(100, c.getByte("g")));
        cozulme = c.getBoolean("cozulme");
        dongu = c.getLong("dongu");
        sonBuhar = (byte) Math.max(0, c.getByte("buhar"));
        yarimG = c.contains("yarimG", Tag.TAG_BYTE) ? Math.max(0, Math.min(100, c.getByte("yarimG"))) : YARIM_YOK;
        yarimCozulme = c.getBoolean("yarimCozulme");
        kopyala(c.getLongArray("kar"), kar);
        kopyala(c.getLongArray("buz"), buz);
        kuruSayi = null;
        kuruUstY = null;
        for (int p : c.getIntArray("kuru")) {
            kuru(p & 0xFF, (short) (p >>> 16), p >>> 8 & 0x7F);
        }
    }

    private static void kopyala(long[] kaynak, long[] hedef) {
        Arrays.fill(hedef, 0L);
        System.arraycopy(kaynak, 0, hedef, 0, Math.min(kaynak.length, hedef.length));
    }

    static final class Saglayici implements ICapabilitySerializable<CompoundTag> {
        final ChunkDonmaVerisi veri = new ChunkDonmaVerisi();
        private final LazyOptional<ChunkDonmaVerisi> secenek = LazyOptional.of(() -> veri);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction yon) {
            return YETENEK.orEmpty(cap, secenek);
        }

        @Override
        public CompoundTag serializeNBT() {
            return veri.yaz();
        }

        @Override
        public void deserializeNBT(CompoundTag etiket) {
            veri.oku(etiket);
        }
    }
}

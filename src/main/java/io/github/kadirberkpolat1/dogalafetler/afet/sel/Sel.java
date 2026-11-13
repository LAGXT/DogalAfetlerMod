package io.github.kadirberkpolat1.dogalafetler.afet.sel;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelKurallari;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SelSuyu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Sel (taşkın): deniz, nehir ve göl yüzeyi {@code artisBlok} kadar yükselir ve altındaki alçak arazi (suyla bağlantılı
 * her çukur) sel suyuna gömülür; yüksek arazi kuru kalır. Su, tsunami ve ay çarpması sonrası deniz yükselmesiyle aynı sanal sudur
 * ({@link SanalSu}): gerçek blok hiç değişmez, su yüzme, batırma, ıslatma ve boğulma kurallarını ortak sistemden alır.
 * Yapılanma: AKTIF başlangıcında parametrik bir {@link SelSuyu} kaydedilir (yükselme, tepede kalış, geri çekilme); sunucu ve
 * istemci aynı parametrelerden aynı yükselmeyi hesaplar. İsı yayan bloklar (lav, ateş, kamp ateşi, yanan fırın, meşale…) sel
 * suyunu çevrelerinde buharlaştırır ({@link SelIsiTarayici}); denizin kendi seviyesi buharlaşmaz. Etki alanı sanal suyun
 * olduğu boyutun her yeridir (siren ve durum paketi o boyuttaki herkese gider); üst dünyada, yakında su varsa seçilir
 * ({@link #yerUygunMu}). Doğal seçim olasılığını yağmur, fırtına, tsunami ve ay çarpması artırır
 * ({@code SelTetikleyicileri}).
 */
public final class Sel extends Afet {
    /** Seli tutan su kimliği yok. */
    private static final int KIMLIK_YOK = -1;
    /** {@link #yerUygunMu}: merkezden bu yarıçapta (blok) ve bu sıklıkta (blok) su aranır. */
    private static final int ARAMA_YARICAP = 64;
    private static final int ARAMA_ADIM = 4;
    private static final int EN_AZ_SU_ORNEGI = 3;

    private int selKimlik = KIMLIK_YOK;
    /** Sel suyunun başladığı ölçekli afet saati (kalıcı; yeniden açılışta aynı su yeniden kurulur); NaN = henüz yok. */
    private double selBaslangic = Double.NaN;
    private final SelIsiTarayici tarayici = new SelIsiTarayici();

    public Sel() {
        super(AfetTuru.SEL);
    }

    private SelKurallari.Seviye ayar() {
        return SelAyarlari.seviye(seviye());
    }

    @Override
    public EtkiAlani etkiAlani() {
        return EtkiAlani.herYer();
    }

    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        return level.dimension() == Level.OVERWORLD && suVarMi(level, merkez);
    }

    /** Merkez çevresinde yüklü chunk'larda yeterince su yüzeyi var mı (sel kaplayacak bir ey olmalı). */
    static boolean suVarMi(ServerLevel level, BlockPos merkez) {
        int su = 0;
        for (int dx = -ARAMA_YARICAP; dx <= ARAMA_YARICAP; dx += ARAMA_ADIM) {
            for (int dz = -ARAMA_YARICAP; dz <= ARAMA_YARICAP; dz += ARAMA_ADIM) {
                int x = merkez.getX() + dx;
                int z = merkez.getZ() + dz;
                if (!level.hasChunkAt(new BlockPos(x, level.getSeaLevel(), z))) {
                    continue;
                }
                int yuzey = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (level.getFluidState(new BlockPos(x, yuzey - 1, z)).is(FluidTags.WATER) && ++su >= EN_AZ_SU_ORNEGI) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF) {
            SelKurallari.Seviye s = ayar();
            DunyaDonemi donem = DunyaDonemi.al(level);
            selBaslangic = donem.saat();
            DogalAfetler.LOGGER.info("sel: seviye {} +{} blok, {} sn yükselir, {} sn tepede kalır, {} sn çekilir", seviye(),
                    s.artisBlok(), s.yukselmeTick() / SelKurallari.SANIYE_TICK, s.zirveTick() / SelKurallari.SANIYE_TICK,
                    s.inmeTick() / SelKurallari.SANIYE_TICK);
            suyuKur(level);
        }
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        if (selKimlik == KIMLIK_YOK || !SanalSu.selVarMi(level, selKimlik)) {
            // Sel suyu sürerken bir sebepten düştüyse (boyut sıfırlandı, test) kaldığı yerden yeniden kurulur.
            if (!Double.isNaN(selBaslangic) && DunyaDonemi.al(level).saat() < selBaslangic + ayar().toplamTick()) {
                suyuKur(level);
            }
        }
        if (SelAyarlari.isiBuharlastirir()) {
            if (tarayici.tick(level)) {
                SanalSu.isiBolgeleriAyarla(level, tarayici.bolgeler());
            }
        }
    }

    @Override
    protected boolean aktifBitti() {
        return asamaGecenTick() >= ayar().toplamTick();
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, ayar().toplamTick() - asamaGecenTick());
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        suyuBirak(level);
        DogalAfetler.LOGGER.info("sel bitti: seviye {}", seviye());
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        if (!Double.isNaN(selBaslangic)) {
            etiket.putDouble("selBaslangic", selBaslangic);
        }
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        selBaslangic = etiket.contains("selBaslangic") ? etiket.getDouble("selBaslangic") : Double.NaN;
    }

    @Override
    protected void yuklendi(ServerLevel level) {
        if (asama() == AfetAsamasi.AKTIF && !Double.isNaN(selBaslangic)) {
            suyuKur(level);
        }
    }

    /** {@code a}: şu anki sel yükselmesi (blok); istemci göstergeleri ve radar için. */
    @Override
    protected void istemciVerisiYaz(CompoundTag etiket) {
        if (asama() == AfetAsamasi.AKTIF && !Double.isNaN(selBaslangic)) {
            etiket.putFloat("a", (float) suyu().artis(SanalSu.suankiSunucuSaati()));
        }
    }

    private SelSuyu suyu() {
        return ayar().suyu(selBaslangic);
    }

    private void suyuKur(ServerLevel level) {
        if (selKimlik != KIMLIK_YOK && SanalSu.selVarMi(level, selKimlik)) {
            return;
        }
        selKimlik = SanalSu.selBaslat(level, suyu());
    }

    private void suyuBirak(ServerLevel level) {
        if (selKimlik != KIMLIK_YOK) {
            SanalSu.selBitir(level, selKimlik);
            selKimlik = KIMLIK_YOK;
        }
        SanalSu.isiBolgeleriAyarla(level, List.of());
    }

    /** GameTest: sel suyunun kimliği; yoksa -1. */
    int selKimlik() {
        return selKimlik;
    }

    /** GameTest: ısı tarayıcının son bölge listesi. */
    int isiBolgesiSayisi() {
        return tarayici.bolgeler().size();
    }
}

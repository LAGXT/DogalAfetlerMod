package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * Asit yağmurunun vanilla havası (AF2-02): AKTIF'e geçerken dünyanın hava durumu saklanır ve yağmur açılır; AKTIF boyunca
 * vanilla'nın yağmuru kesmesi, gök gürültüsü ve {@code /weather} bir tick'te geri alınır; bitişte saklanan hava olduğu gibi
 * geri yüklenir (açık hava, yağmur ve gök gürültüsü sayacı dahil). {@code doWeatherCycle} kuralına dokunulmaz. Üst dünyadan
 * türeyen boyutların hava verisi yazılamaz; orada hiçbir şey değişmez.
 */
final class AsitHavasi {
    /** {@link #tut} sonucu: dokunulmadı, yağmur süresi tazelendi, vanilla havası yağmuru kesmişti ve geri alındı. */
    enum Sonuc { AYNI, TAZELENDI, GERI_ALINDI }

    private AsitHavasi() {
    }

    /** Dünyanın şu anki hava durumu; sunucu hava verisi yoksa {@code null}. */
    static CompoundTag sakla(ServerLevel level) {
        if (!(level.getLevelData() instanceof ServerLevelData veri)) {
            return null;
        }
        CompoundTag t = new CompoundTag();
        t.putInt("acik", veri.getClearWeatherTime());
        t.putInt("yagmur", veri.getRainTime());
        t.putInt("gok", veri.getThunderTime());
        t.putBoolean("yagiyor", veri.isRaining());
        t.putBoolean("gurultu", veri.isThundering());
        return t;
    }

    /** Yağmur kesildiyse (yağmıyor, gök gürülüyor, {@code /weather clear}) ya da süresi bitmek üzereyse yeniden açar. */
    static Sonuc tut(ServerLevel level) {
        if (!(level.getLevelData() instanceof ServerLevelData veri)) {
            return Sonuc.AYNI;
        }
        boolean kesildi = AsitKurallari.yagmurKesildi(veri.isRaining(), veri.isThundering(), veri.getClearWeatherTime());
        if (!AsitKurallari.yagmurYenilenmeli(veri.isRaining(), veri.isThundering(), veri.getRainTime(),
                veri.getClearWeatherTime())) {
            return Sonuc.AYNI;
        }
        level.setWeatherParameters(0, AsitKurallari.YAGMUR_SURESI_TICK, true, false);
        return kesildi ? Sonuc.GERI_ALINDI : Sonuc.TAZELENDI;
    }

    /** {@link #sakla} ile alınan hava durumunu değiştirmeden geri yazar. */
    static void geriYukle(ServerLevel level, CompoundTag t) {
        if (!(level.getLevelData() instanceof ServerLevelData veri)) {
            return;
        }
        veri.setClearWeatherTime(t.getInt("acik"));
        veri.setRainTime(t.getInt("yagmur"));
        veri.setThunderTime(t.getInt("gok"));
        veri.setRaining(t.getBoolean("yagiyor"));
        veri.setThundering(t.getBoolean("gurultu"));
    }

    /** Log için kısa özet: {@code yağmur=false açık=12345 yağmurSüresi=0 gök=false}. */
    static String ozet(CompoundTag t) {
        return String.format(Locale.ROOT, "yağmur=%b açık=%d yağmurSüresi=%d gök=%b", t.getBoolean("yagiyor"),
                t.getInt("acik"), t.getInt("yagmur"), t.getBoolean("gurultu"));
    }
}

package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sıcaklık bantının saf hesapları (§A8, S3-07): yerleşim, dolum, çentik ve üçgen konumları, şevron sayısı, metinler ve
 * animasyon eğrileri. Minecraft'a bağlı değildir; {@link SicaklikHud} çizer. Koordinatlar bantın sol kenarına göredir:
 * {@code [0,74)} soğuk tüp, {@code [74,108)} vücut hapı, {@code [108,182)} sıcak tüp. Tüplerin 1 px çerçevesi vardır; dolum
 * hapın kenarından dışa doğru |B|/100 kadar ilerler.
 */
public final class HudCizimi {
    /** XP barıyla aynı genişlik. */
    public static final int GENISLIK = 182;
    public static final int YUKSEKLIK = 11;
    public static final int HAP_G = 34;
    /** Bir yarım tüpün dış genişliği (74) ve dolum pikseli (72). */
    public static final int YARIM = (GENISLIK - HAP_G) / 2;
    public static final int YARIM_IC = YARIM - 2;
    public static final int TUP_Y = 3;
    public static final int TUP_YUKSEKLIK = 5;
    public static final int IKON = 9;
    public static final int IKON_ARALIGI = 10;
    /** Durum değişince hapın beyaz parladığı süre (tick, TaN fikri). */
    public static final int PARLAMA_TICK = 6;
    /** Hapın sol kenarı (74) ve sıcak tüpün sol kenarı (108). */
    public static final int HAP_X = YARIM;
    public static final int SICAK_X = YARIM + HAP_G;
    /** Hap sayısının dört yönlü koyu konturu (ARGB): nabız ve parlama sırasında da rakamları zeminden ayırır. */
    public static final int KONTUR = 0xFF120E0C;

    /** Bantın sol kenarındaki etkin durum ikonları, çizim sırasıyla; {@code u} atlas sütunu. */
    public enum Ikon {
        ISLAK(0),
        USUME(18),
        DONMA(27),
        BUNALMA(36),
        YANMA(45);

        public final int u;

        Ikon(int u) {
            this.u = u;
        }
    }

    private HudCizimi() {
    }

    /** Yarım tüpte dolu piksel sayısı (0–72). */
    public static int dolum(float b) {
        return Math.round(Math.min(100f, Math.abs(b)) / 100f * YARIM_IC);
    }

    /**
     * Dolu piksel aralığı {@code {bas, son}} (son hariç): soğukta hapın solundan sola, sıcakta hapın sağından sağa. Boşsa
     * {@code bas == son}.
     */
    public static int[] dolguAraligi(float b) {
        int n = dolum(b);
        if (b < 0) {
            return new int[] {HAP_X - 1 - n, HAP_X - 1};
        }
        return new int[] {SICAK_X + 1, SICAK_X + 1 + n};
    }

    /**
     * Eşik çentiğinin x'i: vücut tam eşikteyken dolumun en dıştaki pikseli. Bu yüzden dolum çentiğe vardığında kademe de
     * değişmiş olur (kademe eşikte dahil değişir); iki yarı merkeze göre ayna simetriktir.
     */
    public static int centik(float esik) {
        int n = Math.max(1, dolum(esik));
        return esik < 0 ? HAP_X - 1 - n : SICAK_X + n;
    }

    /** Hissedilen eşdeğerinin (vücut birimi) üçgen merkezi; nötre çok yakınsa hapın ortası. */
    public static int ucgen(float ortamBirim) {
        if (Math.abs(ortamBirim) < 100f / YARIM_IC / 2f) {
            return GENISLIK / 2;
        }
        return centik(ortamBirim);
    }

    /** Eğilim şevronu: işaret yön (+ ısınma), büyüklük 0–3 (|eğilim| 0,05 / 0,2 / 0,5 B/sn). */
    public static int sevron(float egilim) {
        float a = Math.abs(egilim);
        int n = a >= 0.5f ? 3 : a >= 0.2f ? 2 : a >= 0.05f ? 1 : 0;
        return egilim < 0 ? -n : n;
    }

    /** Kayan şevronun dikey kayması 0–2 px; her şevron bir öncekinden bir faz geride. */
    public static int sevronKaymasi(int sira, float zaman) {
        return Math.floorMod((int) (zaman / 4f) - sira, 3);
    }

    /** Hapın vücut sayısı: {@code "36.4°"}. */
    public static String sayiMetni(float vucut) {
        return VucutOlcegi.metin(VucutOlcegi.santigrat(vucut)) + "°";
    }

    /** Barın sağındaki hissedilen sıcaklık: tam sayı °C, {@code "-6°"}. */
    public static String hissedilenMetni(float santigrat) {
        return String.format(Locale.ROOT, "%d°", Math.round(santigrat));
    }

    /** Etkin ikonlar soldan sağa: ıslaklık görünürse damla, sonra normal dışı vücut durumu. */
    public static List<Ikon> ikonlar(SicaklikDurumu durum, float islaklik) {
        List<Ikon> l = new ArrayList<>(2);
        if (islaklik >= 0.05f) {
            l.add(Ikon.ISLAK);
        }
        Ikon d = durumIkonu(durum);
        if (d != null) {
            l.add(d);
        }
        return l;
    }

    /** Hissedilen °C yazısının rengi: dondurucu, soğuk, konfor (12–28 °C), sıcak, kavurucu. */
    public static int hissedilenRengi(float santigrat) {
        if (santigrat < 0f) {
            return 0xFF6CB8FF;
        }
        if (santigrat < 12f) {
            return 0xFFA8DCFF;
        }
        if (santigrat <= 28f) {
            return 0xFFE0DFE0;
        }
        return santigrat <= 40f ? 0xFFF8AF35 : 0xFFFF4A24;
    }

    /** Durumun ikonu; normalde yok. */
    public static Ikon durumIkonu(SicaklikDurumu durum) {
        return switch (durum) {
            case USUME -> Ikon.USUME;
            case DONMA -> Ikon.DONMA;
            case BUNALMA -> Ikon.BUNALMA;
            case YANMA -> Ikon.YANMA;
            case NORMAL -> null;
        };
    }

    /** Damla ikonunun alttan dolu satırı (1–9); görünür ıslaklıkta en az bir satır. */
    public static int damlaDolulugu(float islaklik) {
        return Math.max(1, Math.round(Math.min(1f, Math.max(0f, islaklik)) * IKON));
    }

    /** Hap çerçevesi ve sayı rengi (ARGB) ile hap atlas sırası. */
    public static int renk(SicaklikDurumu durum) {
        return switch (durum) {
            case NORMAL -> 0xFFE8E6DE;
            case USUME -> 0xFFA6DDF8;
            case DONMA -> 0xFF5EAEFF;
            case BUNALMA -> 0xFFFFB54A;
            case YANMA -> 0xFFFF5A36;
        };
    }

    /**
     * Hapın vücut sayısının rengi (ARGB): durum renginin %65 beyaza açılmışı. Koyu hap içinde, nabızın en parlak anında
     * bile en az 5:1 karşıtlık; çevresinde {@link #KONTUR}.
     */
    public static int sayiRengi(SicaklikDurumu durum) {
        return switch (durum) {
            case NORMAL -> 0xFFF7F6F3;
            case USUME -> 0xFFE0F3FD;
            case DONMA -> 0xFFC7E3FF;
            case BUNALMA -> 0xFFFFE5C0;
            case YANMA -> 0xFFFFC5B9;
        };
    }

    public static int hapSirasi(SicaklikDurumu durum) {
        return switch (durum) {
            case NORMAL -> 0;
            case USUME -> 1;
            case DONMA -> 2;
            case BUNALMA -> 3;
            case YANMA -> 4;
        };
    }

    /**
     * Bant titreşimi {@code {dx, dy}}: donmada her 2 tick'te iki eksende ±1 px, üşümede 15 tick'te bir 3 tick süren 1 px
     * yatay ürperme; diğerlerinde yok.
     */
    public static int[] titreme(SicaklikDurumu durum, long tick) {
        if (durum == SicaklikDurumu.DONMA) {
            return new int[] {(int) Math.floorMod(tick / 2 * 7 + 3, 3) - 1, (int) Math.floorMod(tick / 2 * 5 + 1, 3) - 1};
        }
        if (durum == SicaklikDurumu.USUME && (tick / 3) % 5 == 0) {
            return new int[] {(tick / 3) % 10 == 0 ? 1 : -1, 0};
        }
        return new int[] {0, 0};
    }

    /** Nabız periyodu (tick): bunalma eşiğinde 70 vuruş/dk, aşırı ısınma hasarı eşiğinde ve ötesinde 150. */
    public static float nabizPeriyodu(float vucut, float sicak, float yanma) {
        float siddet = Math.min(1f, Math.max(0f, (vucut - sicak) / Math.max(1f, yanma - sicak)));
        return 1200f / (70f + 80f * siddet);
    }

    /** Hap nabız parlaklığı 0–1: bunalma ve yanmada kalp atışı gibi hızlı yükselip yavaş söner; diğerlerinde 0. */
    public static float nabiz(SicaklikDurumu durum, float vucut, float sicak, float yanma, float zaman) {
        if (durum != SicaklikDurumu.BUNALMA && durum != SicaklikDurumu.YANMA) {
            return 0f;
        }
        float periyot = nabizPeriyodu(vucut, sicak, yanma);
        float faz = (zaman % periyot) / periyot;
        float genlik = durum == SicaklikDurumu.YANMA ? 0.6f : 0.32f;
        return genlik * (float) Math.exp(-faz * 6.0);
    }

    /** Durum değişim parlaması 0–1: değişim anında 1, {@link #PARLAMA_TICK} tick'te 0'a iner. */
    public static float parlama(long tick, long degisimTick, float partialTick) {
        if (degisimTick < 0) {
            return 0f;
        }
        float gecen = tick - degisimTick + partialTick;
        if (gecen < 0f || gecen >= PARLAMA_TICK) {
            return 0f;
        }
        return 1f - gecen / PARLAMA_TICK;
    }
}

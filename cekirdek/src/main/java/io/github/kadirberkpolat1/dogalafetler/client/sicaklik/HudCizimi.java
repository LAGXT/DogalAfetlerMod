package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import java.util.ArrayList;
import java.util.List;

/**
 * Kafa ikonlu sıcaklık göstergesinin saf hesapları (K-E74): yerleşim, şerit dolumu, çentik ve iğne konumları, kafa tonu,
 * durum efektlerinin (ter, alev/duman, buhar, buz) piksel listeleri, titreme ve animasyon eğrileri. Minecraft'a bağlı
 * değildir; {@link SicaklikHud} çizer. Koordinatlar göstergenin sol üst köşesine göredir ve vanilla can ile açlık
 * barları arasındaki 20 piksellik boşluğa tam sığar:
 * <pre>
 * y  0..2   başlık payı (alev, duman)
 * y  2..19  çerçeve halkası (durum renginde), içinde y 3..18 oyuncunun 16x16 kafası (x 2..17)
 * y 19..22  hissedilen iğnesi (şeridi kesen 1 px beyaz çizgi)
 * y 20..21  şerit: ortadan dışa dolan soğuk (sol, mavi) ve sıcak (sağ, turuncu→kırmızı) yarılar, yarımı 9 piksel
 * x 20..26, y 2..11 ıslaklık damlası (yalnız ıslakken; kafanın sağında, göstergenin dışına taşar, can ve açlık barlarının üstünde)
 * </pre>
 */
public final class HudCizimi {
    /** Vanilla can (…-10) ile açlık (+10) arasındaki boşluk. */
    public static final int GENISLIK = 20;
    public static final int YUKSEKLIK = 24;
    /** Oyuncu yüzünün 8x8 dokusu 2 kat ölçeklenir. */
    public static final int KAFA = 16;
    public static final int KAFA_X = 2;
    public static final int KAFA_Y = 3;
    public static final int SERIT_Y = 20;
    public static final int SERIT_YUKSEKLIK = 2;
    public static final int IGNE_Y = 19;
    public static final int IGNE_YUKSEKLIK = 4;
    /** Islaklık damlasının sol üst köşesi (gösterge içi): kafa halkasının hemen sağı, vanilla bar ve baloncukların üstü. */
    public static final int DAMLA_X = KAFA_X + KAFA + 2;
    public static final int DAMLA_Y = 2;
    public static final int DAMLA_GENISLIK = 7;
    public static final int DAMLA_YUKSEKLIK = 10;
    /** Şeridin bir yarısındaki dolum pikseli (iç genişlik 18) ve sağ yarının ilk piksel sütunu. */
    public static final int YARIM_IC = 9;
    public static final int MERKEZ = GENISLIK / 2;
    /** Durum değişince kafanın beyaz parladığı süre (tick, TaN fikri). */
    public static final int PARLAMA_TICK = 6;
    /** Donma başladıktan sonra kafanın sarsıldığı süre (tick); sonrasında donup kalır. */
    public static final int DONMA_SARSINTI_TICK = 40;
    private static final int TER_PERIYODU = 36;
    private static final int NEFES_PERIYODU = 42;
    private static final int DUMAN_PERIYODU = 48;

    /** Ekrana çizilecek tek düz dikdörtgen (gösterge koordinatı, ARGB). */
    public record Parcacik(int x, int y, int w, int h, int argb) {
    }

    private HudCizimi() {
    }

    /** Şeridin bir yarısında dolu piksel sayısı (kesirli, 0–9): ±100 vucut birimi yarımın tamamıdır. */
    public static float yariDolum(float b) {
        return Math.min(100f, Math.abs(b)) / 100f * YARIM_IC;
    }

    /** Merkezden {@code i}. dolum pikselinin x'i: sıcakta merkezin sağından, soğukta solundan başlar. */
    public static int piksel(boolean sicak, int i) {
        return sicak ? MERKEZ + i : MERKEZ - 1 - i;
    }

    /** Dolum pikselinin rengi (ARGB): ortada açık, dışa doğru koyulaşır. */
    public static int seritRengi(boolean sicak, int i) {
        float t = i / (float) (YARIM_IC - 1);
        return sicak ? karistir(0xFFFFBE46, 0xFFE6281E, t) : karistir(0xFFC8F0FF, 0xFF3476D6, t);
    }

    /**
     * Eşik çentiğinin x'i: eşiğin düştüğü piksel; dolum o piksele girdiğinde kademe de değişmek üzeredir. İki yarı merkeze
     * göre ayna simetriktir.
     */
    public static int centik(float esik) {
        int i = Math.min(YARIM_IC - 1, (int) yariDolum(esik));
        return piksel(esik >= 0, i);
    }

    /** Hissedilen eşdeğerinin (vücut birimi) iğne x'i: şeridin iç pikselleri 1–18 arasında kalır, nötr merkezdedir. */
    public static int igneX(float ortamBirim) {
        float p = Math.max(-1f, Math.min(1f, ortamBirim / 100f)) * YARIM_IC;
        return Math.max(1, Math.min(GENISLIK - 2, (int) Math.floor(MERKEZ + p)));
    }

    /**
     * Islaklık damlası deseni (7x10): {@code O} çerçeve, {@code I} iç, {@code H} parlak vurgu. İç satırları (1–8) ıslaklık oranında
     * alttan dolar.
     */
    private static final String[] DAMLA = {
        "...O...",
        "..OIO..",
        "..OIO..",
        ".OIIIO.",
        ".OIIIO.",
        "OHIIIIO",
        "OHIIIIO",
        "OIIIIIO",
        ".OIIIO.",
        "..OOO.."
    };
    private static final int DAMLA_IC_SATIR = 8;
    private static final int DAMLA_PERIYODU = 70;

    /**
     * Isı damlasının piksel listesi (gösterge koordinatı): yalnız ıslaklık görünür eşiğinı ({@link Islaklik#gorunur}) geçince
     * çizilir, yoksa boş. Çerçeve koyu mavi, iç ıslaklık oranında alttan açık mavi dolar (boş kısım soluk), solda parlak vurgu;
     * damla yavaşça 1 piksel süzlür, yoğun ıslaklıkta altından ince bir damlacık düşer ve söner.
     */
    public static List<Parcacik> damla(float islaklik, float zaman) {
        if (!Islaklik.gorunur(islaklik)) {
            return List.of();
        }
        float oran = Math.min(1f, islaklik);
        int dolu = Math.max(1, Math.round(oran * DAMLA_IC_SATIR));
        int ilkDolu = DAMLA_IC_SATIR + 1 - dolu;
        int sizma = Math.round(0.5f * (float) Math.sin(zaman * 0.12f) + 0.5f);
        int ox = DAMLA_X;
        int oy = DAMLA_Y + sizma;
        List<Parcacik> l = new ArrayList<>(DAMLA.length * 3 + 2);
        for (int r = 0; r < DAMLA.length; r++) {
            String satir = DAMLA[r];
            int bas = 0;
            while (bas < satir.length()) {
                char c = satir.charAt(bas);
                int son = bas + 1;
                while (son < satir.length() && satir.charAt(son) == c) {
                    son++;
                }
                int renk = damlaRengi(c, r >= ilkDolu);
                if (renk != 0) {
                    l.add(new Parcacik(ox + bas, oy + r, son - bas, 1, renk));
                }
                bas = son;
            }
        }
        if (oran >= 0.5f) {
            float p = (zaman % DAMLA_PERIYODU) / DAMLA_PERIYODU;
            if (p > 0.3f) {
                float u = (p - 0.3f) / 0.7f;
                l.add(new Parcacik(ox + DAMLA_GENISLIK / 2, oy + DAMLA.length + 1 + Math.round(5f * u * u), 1, 2,
                        alfa(1f - u, 0x7FC4FF)));
            }
        }
        return l;
    }

    private static int damlaRengi(char c, boolean dolu) {
        return switch (c) {
            case 'O' -> 0xFF2A6FB5;
            case 'H' -> dolu ? 0xFFEAF6FF : 0x55EAF6FF;
            case 'I' -> dolu ? 0xFF5FB4FF : 0x553C78B4;
            default -> 0;
        };
    }

    /** Başın yanındaki vücut sayısı: {@code "36.4°"}. */
    public static String sayiMetni(float vucut) {
        return VucutOlcegi.metin(VucutOlcegi.santigrat(vucut)) + "°";
    }

    /** Çerçeve halkasının rengi (ARGB, opak). */
    public static int renk(SicaklikDurumu durum) {
        return switch (durum) {
            case NORMAL -> 0xFFE8E6DE;
            case USUME -> 0xFFA6DDF8;
            case DONMA -> 0xFF5EAEFF;
            case BUNALMA -> 0xFFFFB54A;
            case YANMA -> 0xFFFF5A36;
        };
    }

    /** Sayı rengi (ARGB): durum renginin %65 beyaza açılmışı; gölgeli yazılır. */
    public static int sayiRengi(SicaklikDurumu durum) {
        return switch (durum) {
            case NORMAL -> 0xFFF7F6F3;
            case USUME -> 0xFFE0F3FD;
            case DONMA -> 0xFFC7E3FF;
            case BUNALMA -> 0xFFFFE5C0;
            case YANMA -> 0xFFFFC5B9;
        };
    }

    /**
     * Kafa dokusunun çarpan tonu {@code {r, g, b}} (0–1): normalde yok; üşümede hafif, donmada belirgin mavi; bunalmada hafif
     * sıcak; yanmada kırmızı ve nabızla derinleşir.
     */
    public static float[] kafaTonu(SicaklikDurumu durum, float nabiz) {
        return switch (durum) {
            case NORMAL -> new float[] {1f, 1f, 1f};
            case USUME -> new float[] {0.84f, 0.93f, 1f};
            case DONMA -> new float[] {0.55f, 0.78f, 1f};
            case BUNALMA -> new float[] {1f, 0.9f - nabiz * 0.2f, 0.84f - nabiz * 0.2f};
            case YANMA -> new float[] {1f, 0.62f - nabiz * 0.25f, 0.55f - nabiz * 0.25f};
        };
    }

    /** Durumun kafa efektinin adı (görsel kanıt log'u); efekt yoksa boş. */
    public static String efektAdi(SicaklikDurumu durum) {
        return switch (durum) {
            case NORMAL -> "";
            case USUME -> "BUHAR";
            case DONMA -> "BUZ";
            case BUNALMA -> "TER";
            case YANMA -> "ALEV";
        };
    }

    /** Durumun kafa efektinin parlakçıkları, çizim sırasıyla ({@code zaman}: istemci tick'i + partialTick). */
    public static List<Parcacik> efekt(SicaklikDurumu durum, float zaman) {
        return switch (durum) {
            case NORMAL -> List.of();
            case USUME -> nefes(zaman);
            case DONMA -> buz(zaman);
            case BUNALMA -> terDamlalari(zaman);
            case YANMA -> alevVeDuman(zaman);
        };
    }

    private static int alfa(float oran, int renk) {
        return Math.round(Math.max(0f, Math.min(1f, oran)) * 255f) << 24 | renk;
    }

    /** Alından kayarak yüzü geçen üç ter damlası: hızlanarak düşer, sonda söner; üstünde parlak bir nokta. */
    static List<Parcacik> terDamlalari(float zaman) {
        List<Parcacik> l = new ArrayList<>(6);
        int[] yerler = {3, 8, 12};
        for (int i = 0; i < yerler.length; i++) {
            float p = ((zaman + i * (TER_PERIYODU / 3f)) % TER_PERIYODU) / TER_PERIYODU;
            int x = KAFA_X + yerler[i];
            int y = KAFA_Y + 2 + Math.round(11f * p * p);
            float a = p > 0.75f ? (1f - p) / 0.25f : 1f;
            l.add(new Parcacik(x, y, 1, 2, alfa(a, 0x8FD0FF)));
            l.add(new Parcacik(x, y, 1, 1, alfa(a, 0xE8F6FF)));
        }
        return l;
    }

    /** Kafanın üstünde beş dalgalanan alev sütunu (taban sarı, gövde turuncu, uç kırmızı) ve yükselen üç duman parçası. */
    static List<Parcacik> alevVeDuman(float zaman) {
        List<Parcacik> l = new ArrayList<>(20);
        int taban = KAFA_Y + 1;
        for (int c = 0; c < 5; c++) {
            int x = KAFA_X + 1 + c * 3;
            int boy = 2 + Math.round(2f * (0.5f + 0.5f * (float) Math.sin(zaman * 0.9f + c * 2.1f)));
            l.add(new Parcacik(x, taban - boy, 1 + (c & 1), boy - 1, 0xFFFF8C1A));
            l.add(new Parcacik(x, taban - 1, 2, 1, 0xFFFFD84A));
            l.add(new Parcacik(x + (c & 1), taban - boy, 1, 1, 0xFFE03A12));
        }
        for (int i = 0; i < 3; i++) {
            float p = ((zaman + i * (DUMAN_PERIYODU / 3f)) % DUMAN_PERIYODU) / DUMAN_PERIYODU;
            int x = KAFA_X + 3 + i * 5 + Math.round(2f * (float) Math.sin(zaman * 0.15f + i * 2f));
            l.add(new Parcacik(x, -Math.round(p * 6f), 2, 2, alfa(0.6f * (1f - p), 0x707070)));
        }
        return l;
    }

    /** Ağızdan (yüzün alt ortası) çıkan, büyüyerek yanlara sönüp yükselen üç buhar bulutu. */
    static List<Parcacik> nefes(float zaman) {
        List<Parcacik> l = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            float p = ((zaman + i * (NEFES_PERIYODU / 3f)) % NEFES_PERIYODU) / NEFES_PERIYODU;
            int boyut = 1 + (int) (p * 3f);
            int yon = i % 2 == 0 ? -1 : 1;
            int x = MERKEZ + Math.round(yon * (1f + p * 6f)) - boyut / 2;
            int y = KAFA_Y + 12 - Math.round(p * 3f);
            l.add(new Parcacik(x, y, boyut, boyut, alfa(0.85f * (1f - p), 0xEAF6FF)));
        }
        return l;
    }

    /** Kafa kenarında kıra şeridi ve beş buz kristali (artı biçiminde, birbirinden farklı fazda parlayıp söner). */
    static List<Parcacik> buz(float zaman) {
        List<Parcacik> l = new ArrayList<>(18);
        l.add(new Parcacik(KAFA_X, KAFA_Y, KAFA, 1, 0x66E8F8FF));
        l.add(new Parcacik(KAFA_X, KAFA_Y, 1, KAFA, 0x44E8F8FF));
        l.add(new Parcacik(KAFA_X + KAFA - 1, KAFA_Y, 1, KAFA, 0x44E8F8FF));
        int[][] kristaller = {{1, 4}, {18, 6}, {1, 15}, {18, 16}, {9, 2}};
        for (int i = 0; i < kristaller.length; i++) {
            int cx = kristaller[i][0];
            int cy = kristaller[i][1];
            float parilti = 0.7f + 0.3f * (float) Math.sin(zaman * 0.25f + i * 1.7f);
            l.add(new Parcacik(cx - 1, cy, 3, 1, alfa(parilti, 0xBFEAFF)));
            l.add(new Parcacik(cx, cy - 1, 1, 3, alfa(parilti, 0xBFEAFF)));
            l.add(new Parcacik(cx, cy, 1, 1, alfa(parilti, 0xFFFFFF)));
        }
        return l;
    }

    private static int karistir(int a, int b, float t) {
        int s = 0xFF000000;
        for (int kaydir = 0; kaydir <= 16; kaydir += 8) {
            int c = Math.round((a >> kaydir & 0xFF) * (1f - t) + (b >> kaydir & 0xFF) * t);
            s |= c << kaydir;
        }
        return s;
    }

    /**
     * Gösterge titreşimi {@code {dx, dy}}: donmada önce {@link #DONMA_SARSINTI_TICK} tick her 2 tick'te iki eksende ±1 px sarsılır,
     * sonra donup kalır (yalnız 80 tick'te bir 4 tick'lik ince titreme); üşümede 15 tick'te bir 3 tick süren 1 px yatay ürperme;
     * diğerlerinde yok.
     *
     * @param degisimTick durumun son değiştiği istemci tick'i; hiç değişmediyse -1 (donma zaten sürüyor, sarsıntı bitmiş)
     */
    public static int[] titreme(SicaklikDurumu durum, long tick, long degisimTick) {
        if (durum == SicaklikDurumu.DONMA) {
            boolean sarsinti = degisimTick >= 0 && tick - degisimTick < DONMA_SARSINTI_TICK
                    || Math.floorMod(tick, 80L) < 4;
            if (!sarsinti) {
                return new int[] {0, 0};
            }
            return new int[] {(int) Math.floorMod(tick / 2 * 7 + 3, 3L) - 1, (int) Math.floorMod(tick / 2 * 5 + 1, 3L) - 1};
        }
        if (durum == SicaklikDurumu.USUME && (tick / 3) % 5 == 0) {
            return new int[] {(tick / 3) % 10 == 0 ? 1 : -1, 0};
        }
        return new int[] {0, 0};
    }

    /** Nabız periyodu (tick): bunalma eşinde 70 vuruş/dk, aşırı ısınma hasarı eşiğinde ve ötüsünde 150. */
    public static float nabizPeriyodu(float vucut, float sicak, float yanma) {
        float siddet = Math.min(1f, Math.max(0f, (vucut - sicak) / Math.max(1f, yanma - sicak)));
        return 1200f / (70f + 80f * siddet);
    }

    /** Kafa nabız parlaklığı 0–1: bunalma ve yanmada kalp atışı gibi hızlı yükselip yavaş söner; diğerlerinde 0. */
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

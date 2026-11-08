package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi.IstemciAy;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.MonotonIlerleme;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * İstemcinin karede gördüğü gökyüzü: {@link IstemciDonem}'deki ay/evre/ilerleme ve evre başlangıcından türetilir. Geçişler
 * ölçekli afet saatinden hesaplandığı için herkes (sonradan giren dahil) aynı kareyi görür; önceki evre zincirin sabit
 * sırasından ve paketin pulsar yolu bayraklarından bilinir. Yalnız istemci ana thread'inde.
 */
public final class IstemciGokyuzu {
    /** Etkin kanca kademesi (K0-26): sarmalayıcı (1. ve 2. kademe) ya da son çare {@code AFTER_SKY}. */
    enum Kanca { SARMALAYICI, AFTER_SKY }

    /**
     * Bir karenin gökyüzü durumu. {@code onceki} null ise geçiş yoktur. {@code pulsarYolu}: GUNESSIZ/KIZIL_CUCE pulsar
     * yolunda (kalıntı nebulası yok, sonunda eski güneş döner).
     */
    record Gorunum(GunesEvresi evre, GunesEvresi onceki, float gecis, float ilerleme, float saniye, boolean ayVar,
            boolean pulsarYolu, float boyutOrani, float renkGucu, float[] isik) {
        boolean vanillaMi() {
            return evre == GunesEvresi.NORMAL && ayVar && (onceki == null || gecis >= 1f) && !IstemciAy.gokyuzuEtkin();
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "evre=%s onceki=%s gecis=%.2f ilerleme=%.3f sn=%.1f ay=%s yol=%s",
                    evre.kimlik(), onceki == null ? "-" : onceki.kimlik(), gecis, ilerleme, saniye, ayVar, pulsarYolu);
        }
    }

    static volatile Kanca kanca = Kanca.SARMALAYICI;
    private static final MonotonIlerleme ILERLEME = new MonotonIlerleme();
    private static long sonGuncelleme = Util.getMillis();
    private static Gorunum son;
    private static long sonMs = Long.MIN_VALUE;
    private static float sonKismi = Float.NaN;
    private static int sonSurum = -1;
    /** Yalnız görsel otomasyon: sabit evre saniyesi; NaN = canlı. */
    private static volatile float sabitSaniye = Float.NaN;

    private IstemciGokyuzu() {
    }

    /**
     * Zincirin sabit sırasında bu evreden önceki evre. Pulsar yolunda GUNESSIZ kara delik yutmasından gelir; eski güneşin
     * dönüşünde NORMAL kızıl cüceden 20 sn geçişle gelir; diğer NORMAL (sıfırlama) geçişsizdir.
     */
    static GunesEvresi oncekiEvre(GunesEvresi evre, boolean donusYolu, boolean gunesDondu) {
        return switch (evre) {
            case BUYUYEN -> GunesEvresi.NORMAL;
            case SUPERNOVA -> GunesEvresi.BUYUYEN;
            case GUNESSIZ -> donusYolu ? GunesEvresi.KARADELIK_YUTMA : GunesEvresi.SUPERNOVA;
            case KIZIL_CUCE, PULSAR -> GunesEvresi.GUNESSIZ;
            case KARADELIK_YUTMA -> GunesEvresi.PULSAR;
            case NORMAL -> gunesDondu ? GunesEvresi.KIZIL_CUCE : null;
        };
    }

    /**
     * Yalnız görsel otomasyonda ({@link GorselOtomasyon#AKTIF}) evrenin başından beri geçen saniyeyi sabitler; NaN canlı saate
     * döner (G2-10 kalıbı: düşük FPS'te de kare istenen anı gösterir). Üretimde etkisizdir.
     */
    public static void sabitSaniye(float sn) {
        if (GorselOtomasyon.AKTIF) {
            sabitSaniye = sn;
            son = null;
        }
    }

    /**
     * Güneş yolunda, güneşin (pulsarın) yerinden {@code aciDerece} ötedeki noktanın bu karedeki dünya yönü: 0 pulsar,
     * {@code KaraDelikYutmaEgrileri.KARADELIK_ACI} yutan kara delik (görsel otomasyonun kadrajı; çizimle aynı dönüş).
     */
    public static org.joml.Vector3f gunesYoluYonu(float aciDerece, net.minecraft.client.multiplayer.ClientLevel level,
            float pt) {
        return GokyuzuCizici.gunesYoluYonu(aciDerece, level.getTimeOfDay(pt));
    }

    /**
     * Bu karede kameradan pulsara dünya çerçevesinde birim yön: gecikmeli çapanın paralaksı ve yutmadaki çekim dahil
     * (çizimle aynı; otomasyon kamerayı buna çevirir).
     */
    public static Vector3f pulsarGorunurYonu(ClientLevel level, float pt) {
        Vec3 goz = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f yon = GokyuzuCizici.gunesYoluYonu(GokyuzuCizici.pulsarAcisi(gorunum(pt)), level.getTimeOfDay(pt));
        return GokCapasi.PULSAR.merkez(yon, goz).normalize();
    }

    /** Bu karede kameradan yutan kara deliğe dünya çerçevesinde birim yön (gecikmeli çapa dahil; çizimle aynı). */
    public static Vector3f karaDelikGorunurYonu(ClientLevel level, float pt) {
        Vec3 goz = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f yon = GokyuzuCizici.gunesYoluYonu(KaraDelikYutmaEgrileri.KARADELIK_ACI, level.getTimeOfDay(pt));
        return GokCapasi.KARADELIK.merkez(yon, goz).normalize();
    }

    /** {@code i}. kara deliğin bu karedeki dünya yönü (görsel otomasyonun yakın kadrajı; çizimle aynı dönüş). */
    public static org.joml.Vector3f karaDelikYonu(int i, net.minecraft.client.multiplayer.ClientLevel level, float pt) {
        return GokyuzuCizici.karaDelikYonu(i, level.getTimeOfDay(pt), ((level.getGameTime() % 72000L) + pt) / 20f);
    }

    /**
     * Karenin görünümü; aynı milisaniye, aynı {@code kismiTick} ve aynı paket için önbellekten döner (ışık haritası bunu
     * piksel başına çağırır).
     */
    static Gorunum gorunum(float kismiTick) {
        long simdi = Util.getMillis();
        if (son != null && simdi == sonMs && kismiTick == sonKismi && IstemciDonem.surum() == sonSurum) {
            return son;
        }
        DonemPaketi d = IstemciDonem.durum();
        float gecenTick = (simdi - sonGuncelleme) / 50f;
        sonGuncelleme = simdi;
        long anahtar = d.evreBaslangicSaati() * 8 + d.gunesEvresi().ordinal();
        float ilerleme = ILERLEME.guncelle(anahtar, d.evreIlerlemesi(), gecenTick);
        float sabit = sabitSaniye;
        double gecen;
        if (!Float.isNaN(sabit)) {
            gecen = sabit * 20.0;
        } else if (d.duraklatildi() && d.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA) {
            // Kural duraklatmasında ölçekli saat sürer; açılınca sunucu evre başlangıcını ileri kaydırır (K-C46).
            gecen = PulsarZinciri.duraklamaSaniyesi(d.evreIlerlemesi()) * PulsarZinciri.SANIYE_TICK;
        } else {
            gecen = Math.max(0.0, IstemciDonem.saat(kismiTick) - d.evreBaslangicSaati());
        }
        float boyut = 0.6f;
        float renk = 1f;
        try {
            boyut = (float) GokyuzuAyarlari.kizilCuceBoyutOrani();
            renk = (float) GokyuzuAyarlari.kizilCuceRenkGucu();
        } catch (IllegalStateException yuklenmedi) {
            // Sunucu config'i dünyaya girmeden eşitlenmez; o ana kadar varsayılan görünüm.
        }
        GunesEvresi onceki = oncekiEvre(d.gunesEvresi(), d.donusYolu(), d.gunesDondu());
        float gecis = GokyuzuEgrileri.gecis(gecen);
        float saniye = (float) (gecen / 20.0);
        son = new Gorunum(d.gunesEvresi(), onceki, gecis, ilerleme, saniye, d.ayVar(), d.donusYolu(), boyut, renk,
                GokyuzuRenkleri.isikTonu(d.gunesEvresi(), onceki, gecis, ilerleme, saniye, renk));
        sonMs = simdi;
        sonKismi = kismiTick;
        sonSurum = IstemciDonem.surum();
        return son;
    }
}

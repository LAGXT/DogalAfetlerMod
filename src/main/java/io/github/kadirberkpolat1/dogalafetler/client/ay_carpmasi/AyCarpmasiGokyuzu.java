package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Ay çarpmasının gök çizimi (ay_carpmasi.md §2, §5; gökyüzü §A.5): ortak gökyüzü çizicisi zaman dönüşünden sonra,
 * ufuk altı diskinden önce çağırır. Yaklaşan ay gökte dünyaya göre sabit bir yönde (doğu-güneydoğu, ufkun 35°
 * üstü) büyür, çatlakları parlar, kopan parçalar yörünge yayı boyunca uzar, son 10 sn'de ufka inip alttan kızarır.
 * Çarpma anında havada 3 boyutlu patlama (gökyüzü-2): dünyada sabit bir noktada genişleyen topaklı ateş topu, şok kabuğu,
 * savrulan enkaz ve duman; {@link AyEgrileri#PATLAMA_SN} sn'de tamamen kaybolur. Sonra 3 günlük enkaz halkası ve kayan
 * yıldızlar. Yaklaşan ay ve halka gök küresinde dokulu karelerdir; patlama kamera konumuyla paralaks yapar.
 */
public final class AyCarpmasiGokyuzu {
    private static final ResourceLocation YUZEY = doku("ay_yuzey");
    private static final ResourceLocation[] CATLAKLAR = {doku("ay_catlak_1"), doku("ay_catlak_2"), doku("ay_catlak_3")};
    private static final ResourceLocation KIZIL = doku("ay_kizil");
    private static final ResourceLocation PARCA = doku("ay_parca");
    private static final ResourceLocation HARE = doku("ay_hare");
    private static final ResourceLocation HALKA = doku("enkaz_halkasi");

    /** Vanilla ay diskinin 100 birimdeki yarı çapı ~10; dokumuzda disk karenin 0,92'si. */
    private static final float AY_YARI = 10f / 0.92f;
    /** Parça yayının yataya eğimi (yörünge düzlemi). */
    private static final float YAY_EGIMI = (float) Math.toRadians(20);
    /** Enkaz halkası: doğu-batı ekseninden geçen, tepeden güneye 30° eğik büyük çember. */
    private static final Vector3f HALKA_X = new Vector3f(1f, 0f, 0f);
    private static final Vector3f HALKA_W = new Vector3f(0f, (float) Math.cos(Math.toRadians(30)),
            (float) Math.sin(Math.toRadians(30)));
    private static final Vector3f HALKA_N = new Vector3f(HALKA_X).cross(HALKA_W).normalize();
    private static final int HALKA_DILIM = 96;
    private static final int KAYAN_OMUR_TICK = 20;

    private record Parca(float konum, float sapma, float boy, float donus, float hiz, boolean onde) {
    }

    /** Enkaz parçası: çıkış yönü, hızı (yarıçap katı), boyu (blok), dönüş ekseni ve hızı. */
    private record Enkaz(Vector3f yon, float hiz, float boy, Vector3f eksen, float donus) {
    }

    /** Duman topağı: merkezden yönü, boy katı, topak tohumu. */
    private record Duman(Vector3f yon, float boy, float tohum) {
    }

    private record Kayan(Vector3f bas, Vector3f yon, int dogus) {
    }

    private static EfektKalitesi kurulanKalite;
    private static Parca[] parcalar = new Parca[0];
    private static Enkaz[] enkazlar = new Enkaz[0];
    private static Duman[] dumanlar = new Duman[0];
    private static int kureDilimi = 8;
    /**
     * Patlamanın dünyadaki merkezi: istemcinin patlamayı ilk çizdiği karede kameradan yaklaşma yönünde
     * {@link AyEgrileri#PATLAMA_UZAKLIK} blok öte; sonra sabit (oyuncu uçunca/yürüyünce paralaks yapar).
     */
    private static Vec3 patlamaMerkezi;
    private static long patlamaOlayi = Long.MIN_VALUE;
    private static final List<Kayan> KAYANLAR = new ArrayList<>();
    private static int istemciTick;

    private AyCarpmasiGokyuzu() {
    }

    private static ResourceLocation doku(String ad) {
        return DogalAfetler.konum("textures/environment/ay_carpmasi/" + ad + ".png");
    }

    /** Kalite değişince parça ve bulut sayıları (tasarım §5) sabit tohumla yeniden üretilir. */
    private static void hazirla() {
        EfektKalitesi k = IstemciAyarlari.efektKalitesi();
        if (k == kurulanKalite) {
            return;
        }
        Random r = new Random(20261106L);
        parcalar = new Parca[AyEgrileri.parcaSayisi(k.ordinal())];
        for (int i = 0; i < parcalar.length; i++) {
            float yan = r.nextBoolean() ? 1f : -1f;
            parcalar[i] = new Parca(yan * (0.55f + 1.9f * r.nextFloat()), (float) r.nextGaussian(),
                    0.035f + 0.09f * r.nextFloat() * r.nextFloat(), r.nextFloat() * 360f, (r.nextFloat() - 0.5f) * 40f,
                    r.nextFloat() < 0.6f);
        }
        enkazlar = new Enkaz[AyEgrileri.enkazSayisi(k.ordinal())];
        for (int i = 0; i < enkazlar.length; i++) {
            Vector3f y = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian()).normalize();
            Vector3f eksen = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian())
                    .normalize();
            enkazlar[i] = new Enkaz(y, 0.7f + 0.8f * r.nextFloat(), 6f + 16f * r.nextFloat() * r.nextFloat(), eksen,
                    (r.nextFloat() - 0.5f) * 6f);
        }
        dumanlar = new Duman[AyEgrileri.dumanSayisi(k.ordinal())];
        for (int i = 0; i < dumanlar.length; i++) {
            Vector3f y = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian() * 0.6f + 0.3f,
                    (float) r.nextGaussian()).normalize();
            dumanlar[i] = new Duman(y, 0.35f + 0.25f * r.nextFloat(), r.nextFloat() * 40f);
        }
        kureDilimi = AyEgrileri.kureDilimi(k.ordinal());
        kurulanKalite = k;
    }

    /** Ufuktan {@code egim} derece yükseklikte, dünyada sabit yaklaşma azimutunda birim yön ({@code +X} doğu). */
    static Vector3f yon(float egim, float azimutEki) {
        float e = (float) Math.toRadians(egim);
        float a = (float) Math.toRadians(AyEgrileri.AZIMUT_DERECE + azimutEki);
        return new Vector3f(Mth.cos(e) * Mth.cos(a), Mth.sin(e), Mth.cos(e) * Mth.sin(a));
    }

    /**
     * Ortak gökyüzü çizicisinden (zaman dönüşü olmayan kamera pozuyla) çağrılır; sis kurulumunu çıkışta geri yükler.
     */
    public static void ciz(Matrix4f m, ClientLevel level, float pt, float yagmursuz, Runnable sisKur) {
        IstemciAy.Durum d = IstemciAy.durum(pt);
        boolean kayan = !KAYANLAR.isEmpty();
        if (!d.yaklasiyor() && !d.carpti() && !kayan) {
            return;
        }
        hazirla();
        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        float a = Math.max(0.35f, yagmursuz);
        float zaman = level.getTimeOfDay(pt);
        float gecelik = Mth.clamp(1f - (Mth.cos(zaman * Mth.TWO_PI) * 2f + 0.25f), 0f, 1f);
        float oyunSn = ((level.getGameTime() % 72000L) + pt) / 20f;
        if (d.yaklasiyor()) {
            yaklasma(m, d.saniye(), a, oyunSn);
        } else if (d.carpti()) {
            carpmaSonrasi(m, d.saniye(), a, gecelik, oyunSn);
        }
        if (kayan) {
            kayanlar(m, pt, gecelik * yagmursuz);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        toplamali();
        sisKur.run();
    }

    // ------------------------------------------------------------------ yaklaşma (siren, 0–AyEgrileri.YAKLASMA_SN)

    private static void yaklasma(Matrix4f m, float s, float a, float oyunSn) {
        Vector3f yon = yon(AyEgrileri.egim(s), 0f);
        Vector3f sag = new Vector3f(yon).cross(0f, 1f, 0f).normalize();
        Vector3f ust = new Vector3f(sag).cross(yon).normalize();
        float yari = AY_YARI * AyEgrileri.olcek(s);
        float yay = AyEgrileri.parcaYayilmasi(s);
        float kiz = AyEgrileri.kizarma(s);
        Vector3f teget = new Vector3f(sag).mul(Mth.cos(YAY_EGIMI)).add(new Vector3f(ust).mul(Mth.sin(YAY_EGIMI)));
        Vector3f dik = new Vector3f(teget).cross(yon).normalize();

        toplamali();
        kuad(m, HARE, yon, sag, ust, yari * 2.4f * (1f + 0.6f * yay), 0.78f, 0.76f, 0.74f, a * (0.12f + 0.35f * yay));
        normal();
        parcalariCiz(m, yon, teget, dik, yari, yay, kiz, s, a, false);
        kuad(m, YUZEY, yon, sag, ust, yari, 1f, 1f - 0.12f * kiz, 1f - 0.25f * kiz, a);
        toplamali();
        for (int i = 0; i < CATLAKLAR.length; i++) {
            float c = AyEgrileri.catlak(i + 1, s);
            float nabiz = 0.85f + 0.15f * Mth.sin(oyunSn * 5f + i * 1.7f);
            kuad(m, CATLAKLAR[i], yon, sag, ust, yari, 1f, 0.78f, 0.5f, a * c * nabiz);
        }
        kuad(m, KIZIL, yon, sag, ust, yari, 1f, 0.48f, 0.22f, a * kiz);
        normal();
        parcalariCiz(m, yon, teget, dik, yari, yay, kiz, s, a, true);
        // Atmosfere giren alt kenar ufku alttan turuncuya boyar.
        toplamali();
        Vector3f ufuk = yon(0f, 0f);
        Vector3f usag = new Vector3f(ufuk).cross(0f, 1f, 0f).normalize();
        Vector3f uust = new Vector3f(usag).cross(ufuk).normalize();
        kuad(m, HARE, ufuk, usag, uust, 70f + 50f * kiz, 1f, 0.45f, 0.18f, a * 0.6f * kiz);
    }

    private static void parcalariCiz(Matrix4f m, Vector3f yon, Vector3f teget, Vector3f dik, float yari, float yay,
            float kiz, float s, float a, boolean onde) {
        if (yay <= 0f) {
            return;
        }
        float gorunur = AyEgrileri.yumusak(yay * 6f);
        for (Parca p : parcalar) {
            if (p.onde() != onde) {
                continue;
            }
            float uzaklik = p.konum() * yari / 100f * (0.6f + 2.6f * yay);
            float sapma = p.sapma() * yari / 100f * 0.22f * (0.3f + yay);
            Vector3f y = new Vector3f(yon).add(new Vector3f(teget).mul(uzaklik)).add(new Vector3f(dik).mul(sapma)).normalize();
            Vector3f sag = new Vector3f(y).cross(0f, 1f, 0f).normalize();
            Vector3f ust = new Vector3f(sag).cross(y).normalize();
            float boy = p.boy() * yari * (0.6f + 0.6f * yay);
            float donus = (float) Math.toRadians(p.donus() + p.hiz() * s);
            Vector3f dsag = new Vector3f(sag).mul(Mth.cos(donus)).add(new Vector3f(ust).mul(Mth.sin(donus)));
            Vector3f dust = new Vector3f(ust).mul(Mth.cos(donus)).sub(new Vector3f(sag).mul(Mth.sin(donus)));
            kuad(m, PARCA, y, dsag, dust, boy, 1f, 1f - 0.2f * kiz, 1f - 0.35f * kiz, a * gorunur);
            if (kiz > 0f) {
                toplamali();
                kuad(m, HARE, y, dsag, dust, boy * 1.8f, 1f, 0.5f, 0.2f, a * gorunur * kiz * 0.7f);
                normal();
            }
        }
    }

    // ------------------------------------------------------------------ çarpmadan sonra

    private static void carpmaSonrasi(Matrix4f m, float s, float a, float gecelik, float oyunSn) {
        if (AyEgrileri.patlamaGorunur(s)) {
            float isik = AyEgrileri.dunyaParlamasi(s) * (float) IstemciAy.ayar(AyCarpmasiAyarlari::parlamaOlcegi);
            if (isik > 0.003f) {
                gokParlamasi(m, isik);
            }
            patlama(m, s, a);
        }
        if (IstemciAy.halkaAcik()) {
            float ha = AyEgrileri.halkaAlfa(s) * (0.3f + 0.7f * gecelik) * a;
            if (ha > 0.003f) {
                halka(m, ha, oyunSn);
            }
        }
    }

    /** Gök küresine sığdırma yarıçapı: patlama kamera merkezli tek ölçekle bunun içine küçültülür (açılar korunur). */
    private static final float SIGDIRMA = 90f;

    /** Çarpma anında bütün göğe toplamalı mavi-beyaz örtü (kamera merkezli kutu). */
    private static void gokParlamasi(Matrix4f m, float isik) {
        toplamali();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float a = Math.min(1f, 0.6f * isik);
        float d = 50f;
        for (int eksen = 0; eksen < 3; eksen++) {
            for (int yan = -1; yan <= 1; yan += 2) {
                for (int k = 0; k < 4; k++) {
                    float u = k == 0 || k == 3 ? -d : d;
                    float v = k < 2 ? -d : d;
                    float w = yan * d;
                    float x = eksen == 0 ? w : u;
                    float y = eksen == 0 ? u : eksen == 1 ? w : v;
                    float z = eksen == 2 ? w : v;
                    bb.vertex(m, x, y, z).color(0.75f, 0.85f, 1f, a).endVertex();
                }
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * 3 boyutlu patlama: duman (normal karışım) → iki katmanlı topaklı ateş topu → şok kabuğu → enkaz izleri ve
     * takılan kaya parçaları. Konumlar dünya bloklarında hesaplanır, sonra kamera merkezli tek ölçekle göğe sığdırılır.
     */
    private static void patlama(Matrix4f m, float s, float a) {
        Vec3 goz = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        long olay = IstemciDonem.durum().ayOlaySaati();
        if (patlamaMerkezi == null || patlamaOlayi != olay) {
            Vector3f y = yon(AyEgrileri.PATLAMA_EGIMI, 0f).mul(AyEgrileri.PATLAMA_UZAKLIK);
            patlamaMerkezi = goz.add(y.x, y.y, y.z);
            patlamaOlayi = olay;
            DogalAfetler.LOGGER.info("[ay_carpmasi] patlama merkezi {} (kamera {}), ömür {} sn", patlamaMerkezi, goz,
                    AyEgrileri.PATLAMA_SN);
        }
        Vector3f rel = new Vector3f((float) (patlamaMerkezi.x - goz.x), (float) (patlamaMerkezi.y - goz.y),
                (float) (patlamaMerkezi.z - goz.z));
        float k = SIGDIRMA / (rel.length() + AyEgrileri.PATLAMA_YARICAP * 2.2f);
        Vector3f c = rel.mul(k);
        Vector3f bakis = new Vector3f(c).negate().normalize();
        float r = AyEgrileri.PATLAMA_YARICAP * k;
        float buyume = AyEgrileri.patlamaBuyume(s);
        float sicak = AyEgrileri.patlamaSicakligi(s);
        float duman = AyEgrileri.dumanAlfa(s) * a;
        if (duman > 0.003f) {
            normal();
            float[] gri = {Mth.lerp(sicak, 0.24f, 0.62f), Mth.lerp(sicak, 0.21f, 0.3f), Mth.lerp(sicak, 0.19f, 0.14f)};
            float[] koyu = {gri[0] * 0.45f, gri[1] * 0.45f, gri[2] * 0.45f};
            for (Duman d : dumanlar) {
                Vector3f dm = new Vector3f(d.yon()).mul(r * (0.35f + 0.55f * buyume)).add(c);
                kure(m, dm, r * d.boy() * (0.5f + 0.7f * buyume), Math.max(5, kureDilimi / 2), d.tohum() + s * 0.3f,
                        0.22f, bakis, gri, koyu, duman * 0.6f, false);
            }
        }
        toplamali();
        float ates = AyEgrileri.atesTopuAlfa(s) * a;
        if (ates > 0.003f) {
            float[] ic = {1f, Mth.lerp(sicak, 0.55f, 0.97f), Mth.lerp(sicak, 0.2f, 0.85f)};
            float[] dis = {Mth.lerp(sicak, 0.55f, 1f), Mth.lerp(sicak, 0.12f, 0.45f), Mth.lerp(sicak, 0.04f, 0.12f)};
            kure(m, c, r * (0.2f + 0.8f * buyume), kureDilimi, 3.1f + s * 0.9f, 0.28f, bakis, ic, dis, ates * 0.55f, false);
            kure(m, c, r * 0.55f * (0.25f + 0.75f * buyume), kureDilimi, 11.7f + s * 1.4f, 0.2f, bakis, ic, ic,
                    ates * sicak, false);
        }
        float kabuk = AyEgrileri.kabukAlfa(s) * a;
        if (kabuk > 0.003f) {
            float[] mavi = {0.8f, 0.88f, 1f};
            kure(m, c, r * 1.6f * AyEgrileri.kabukBuyume(s), kureDilimi, 0f, 0.04f, bakis, mavi, mavi, kabuk, true);
        }
        float enkaz = AyEgrileri.enkazAlfa(s) * a;
        if (enkaz > 0.003f) {
            enkaz(m, c, k, s, sicak, enkaz);
        }
    }

    /**
     * Topaklı küre (enlem-boylam ağı): köşe rengi bakışa dönüklükten gelir. Hacim kipinde merkez {@code ic}, kenar
     * {@code dis} ve daha saydam (dolu top hissi); kabuk kipinde yalnız kenar görünür (ince şok kabuğu).
     */
    private static void kure(Matrix4f m, Vector3f c, float r, int dilim, float tohum, float topak, Vector3f bakis,
            float[] ic, float[] dis, float alfa, boolean kabuk) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int boylam = dilim * 2;
        for (int i = 0; i < dilim; i++) {
            for (int j = 0; j < boylam; j++) {
                for (int q = 0; q < 4; q++) {
                    float teta = Mth.PI * (i + (q == 1 || q == 2 ? 1 : 0)) / dilim;
                    float fi = Mth.TWO_PI * (j + (q >= 2 ? 1 : 0)) / boylam;
                    float nx = Mth.sin(teta) * Mth.cos(fi);
                    float ny = Mth.cos(teta);
                    float nz = Mth.sin(teta) * Mth.sin(fi);
                    float rr = r * (1f + topak * (2f * topak(nx, ny, nz, tohum) - 1f));
                    float f = Math.max(0f, nx * bakis.x + ny * bakis.y + nz * bakis.z);
                    float t = kabuk ? 1f : (float) Math.sqrt(f);
                    float e = 1f - f;
                    float ca = kabuk ? alfa * e * e * e : alfa * (0.2f + 0.8f * t);
                    bb.vertex(m, c.x + nx * rr, c.y + ny * rr, c.z + nz * rr).color(Mth.lerp(t, dis[0], ic[0]),
                            Mth.lerp(t, dis[1], ic[1]), Mth.lerp(t, dis[2], ic[2]), Math.min(1f, ca)).endVertex();
                }
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /** Düzgün, ucuz yön gürültüsü (0–1): ateş topunun topakları; {@code t} ilerledikçe topaklar kaynar. */
    private static float topak(float x, float y, float z, float t) {
        return 0.5f + 0.2f * Mth.sin(x * 5.1f + t) * Mth.cos(y * 4.3f - t * 0.7f)
                + 0.15f * Mth.sin(z * 7.7f + x * 3.1f + t * 1.3f) + 0.15f * Mth.sin((y + z) * 9.3f - t);
    }

    /** Enkaz: sıcak izler (toplamalı, kuyruğu saydam) ve ateş topunun ışığıyla iç yüzü aydınlanan, takılan kaya küpleri. */
    private static void enkaz(Matrix4f m, Vector3f c, float k, float s, float sicak, float a) {
        float sOnce = Math.max(0f, s - 0.45f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float g = 0.5f + 0.3f * sicak;
        for (Enkaz e : enkazlar) {
            Vector3f bas = enkazKonumu(e, s, c, k);
            Vector3f son = enkazKonumu(e, sOnce, c, k);
            Vector3f en = new Vector3f(bas).sub(son).cross(bas);
            if (en.lengthSquared() < 1e-12f) {
                continue;
            }
            en.normalize().mul(e.boy() * k * 0.35f);
            bb.vertex(m, son.x - en.x, son.y - en.y, son.z - en.z).color(1f, g, 0.2f, 0f).endVertex();
            bb.vertex(m, son.x + en.x, son.y + en.y, son.z + en.z).color(1f, g, 0.2f, 0f).endVertex();
            bb.vertex(m, bas.x + en.x, bas.y + en.y, bas.z + en.z).color(1f, g, 0.2f, a * 0.8f).endVertex();
            bb.vertex(m, bas.x - en.x, bas.y - en.y, bas.z - en.z).color(1f, g, 0.2f, a * 0.8f).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        normal();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float kor = 0.8f * sicak;
        for (Enkaz e : enkazlar) {
            Vector3f p = enkazKonumu(e, s, c, k);
            Quaternionf q = new Quaternionf().rotationAxis(e.donus() * s, e.eksen());
            float h = e.boy() * k * 0.5f;
            for (int eksen = 0; eksen < 3; eksen++) {
                for (int yan = -1; yan <= 1; yan += 2) {
                    Vector3f n = q.transform(new Vector3f(eksen == 0 ? yan : 0, eksen == 1 ? yan : 0, eksen == 2 ? yan : 0));
                    float isik = 0.35f + 0.65f * Math.max(0f, -n.dot(e.yon()));
                    float cr = Mth.lerp(kor, 0.27f, 1f) * isik;
                    float cg = Mth.lerp(kor, 0.24f, 0.5f) * isik;
                    float cb = Mth.lerp(kor, 0.22f, 0.18f) * isik;
                    for (int i = 0; i < 4; i++) {
                        float u = (i == 0 || i == 3 ? -h : h);
                        float v = (i < 2 ? -h : h);
                        float w = yan * h;
                        Vector3f yerel = new Vector3f(eksen == 0 ? w : u, eksen == 0 ? u : eksen == 1 ? w : v,
                                eksen == 2 ? w : v);
                        Vector3f d = q.transform(yerel).add(p);
                        bb.vertex(m, d.x, d.y, d.z).color(cr, cg, cb, a).endVertex();
                    }
                }
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /** Enkazın {@code s} saniyedeki yeri: dışa yavaşlayarak savrulur, hafifçe düşer (bloklarda), sonra ölçeklenir. */
    private static Vector3f enkazKonumu(Enkaz e, float s, Vector3f c, float k) {
        return new Vector3f(e.yon()).mul(AyEgrileri.PATLAMA_YARICAP * e.hiz() * AyEgrileri.enkazYolu(s))
                .add(0f, -4f * s * s, 0f).mul(k).add(c);
    }

    /** Soluk gri-beyaz enkaz halkası ve içinde yavaş kayan parıltılar. */
    private static void halka(Matrix4f m, float a, float oyunSn) {
        toplamali();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, HALKA);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float genislik = 1.6f;
        for (int i = 0; i < HALKA_DILIM; i++) {
            float t0 = -0.08f + (Mth.PI + 0.16f) * i / HALKA_DILIM;
            float t1 = -0.08f + (Mth.PI + 0.16f) * (i + 1) / HALKA_DILIM;
            float a0 = a * AyEgrileri.yumusak((Mth.sin(t0) + 0.05f) / 0.2f);
            float a1 = a * AyEgrileri.yumusak((Mth.sin(t1) + 0.05f) / 0.2f);
            Vector3f p0 = halkaNoktasi(t0);
            Vector3f p1 = halkaNoktasi(t1);
            float u0 = 4f * i / HALKA_DILIM;
            float u1 = 4f * (i + 1) / HALKA_DILIM;
            kose(bb, m, p0, -genislik, u0, 0f, a0);
            kose(bb, m, p1, -genislik, u1, 0f, a1);
            kose(bb, m, p1, genislik, u1, 1f, a1);
            kose(bb, m, p0, genislik, u0, 1f, a0);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferUploader.drawWithShader(bb.end());
        for (int k = 0; k < 24; k++) {
            float t = (float) ((k * 0.618034 + oyunSn * 0.0015) % 1.0) * Mth.PI;
            float parilti = a * 1.3f * (0.5f + 0.5f * Mth.sin(oyunSn * 1.7f + k * 2.3f));
            Vector3f y = halkaNoktasi(t).div(100f);
            Vector3f sag = new Vector3f(y).cross(HALKA_N).normalize();
            kuad(m, HARE, y, sag, new Vector3f(HALKA_N), 0.9f, 0.9f, 0.93f, 1f, parilti);
        }
    }

    private static Vector3f halkaNoktasi(float teta) {
        return new Vector3f(HALKA_X).mul(Mth.cos(teta)).add(new Vector3f(HALKA_W).mul(Mth.sin(teta))).mul(100f);
    }

    private static void kose(BufferBuilder bb, Matrix4f m, Vector3f p, float kaydir, float u, float v, float a) {
        bb.vertex(m, p.x + HALKA_N.x * kaydir, p.y + HALKA_N.y * kaydir, p.z + HALKA_N.z * kaydir).uv(u, v)
                .color(0.86f, 0.86f, 0.9f, a).endVertex();
    }

    // ------------------------------------------------------------------ kayan yıldızlar (halka döneminde)

    /** İstemci tick'i: halka sürerken oyuncu başına ~3 dk'da bir zararsız kayan yıldız doğar. */
    static void tick(RandomSource r) {
        istemciTick++;
        KAYANLAR.removeIf(k -> istemciTick - k.dogus() > KAYAN_OMUR_TICK);
        IstemciAy.Durum d = IstemciAy.durum(0f);
        if (!d.carpti() || !IstemciAy.halkaAcik() || AyEgrileri.halkaAlfa(d.saniye()) <= 0f
                || r.nextInt(3 * 60 * 20) != 0) {
            return;
        }
        kayanYildizZorla(r);
    }

    /**
     * Gökte rastgele yönde bir kayan yıldız doğurur; halka koşulunu sormaz. {@link #tick} ve görsel otomasyon (yalnız
     * {@code -Potomasyon}, halka karesinden önce) çağırır.
     */
    static void kayanYildizZorla(RandomSource r) {
        float az = r.nextFloat() * Mth.TWO_PI;
        float el = (float) Math.toRadians(30 + 50 * r.nextFloat());
        Vector3f bas = new Vector3f(Mth.cos(el) * Mth.cos(az), Mth.sin(el), Mth.cos(el) * Mth.sin(az));
        Vector3f yon = new Vector3f(r.nextFloat() - 0.5f, -0.35f - 0.3f * r.nextFloat(), r.nextFloat() - 0.5f);
        yon.sub(new Vector3f(bas).mul(yon.dot(bas))).normalize();
        KAYANLAR.add(new Kayan(bas, yon, istemciTick));
    }

    static void sifirla() {
        KAYANLAR.clear();
        patlamaMerkezi = null;
        patlamaOlayi = Long.MIN_VALUE;
    }

    static int kayanSayisi() {
        return KAYANLAR.size();
    }

    private static void kayanlar(Matrix4f m, float pt, float gorunurluk) {
        if (gorunurluk <= 0.01f) {
            return;
        }
        toplamali();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Kayan k : KAYANLAR) {
            float t = Mth.clamp((istemciTick - k.dogus() + pt) / KAYAN_OMUR_TICK, 0f, 1f);
            float uzunluk = 0.22f;
            Vector3f bas = new Vector3f(k.bas()).add(new Vector3f(k.yon()).mul(uzunluk * t)).normalize().mul(100f);
            Vector3f kuyruk = new Vector3f(k.bas()).add(new Vector3f(k.yon()).mul(uzunluk * Math.max(0f, t - 0.35f)))
                    .normalize().mul(100f);
            Vector3f en = new Vector3f(k.yon()).cross(k.bas()).normalize().mul(0.18f);
            float alfa = gorunurluk * Mth.sin(t * Mth.PI);
            bb.vertex(m, kuyruk.x - en.x, kuyruk.y - en.y, kuyruk.z - en.z).color(0.8f, 0.85f, 1f, 0f).endVertex();
            bb.vertex(m, kuyruk.x + en.x, kuyruk.y + en.y, kuyruk.z + en.z).color(0.8f, 0.85f, 1f, 0f).endVertex();
            bb.vertex(m, bas.x + en.x, bas.y + en.y, bas.z + en.z).color(1f, 1f, 1f, alfa).endVertex();
            bb.vertex(m, bas.x - en.x, bas.y - en.y, bas.z - en.z).color(1f, 1f, 1f, alfa).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    // ------------------------------------------------------------------ yardımcılar

    private static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    private static void toplamali() {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    /** Gök küresine teğet dokulu kare: merkez {@code yon*100}, yarı boy {@code yari}, kenarlar {@code sag}/{@code ust}. */
    private static void kuad(Matrix4f m, ResourceLocation doku, Vector3f yon, Vector3f sag, Vector3f ust, float yari,
            float r, float g, float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        float cx = yon.x * 100f;
        float cy = yon.y * 100f;
        float cz = yon.z * 100f;
        float sx = sag.x * yari;
        float sy = sag.y * yari;
        float sz = sag.z * yari;
        float ux = ust.x * yari;
        float uy = ust.y * yari;
        float uz = ust.z * yari;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, cx - sx - ux, cy - sy - uy, cz - sz - uz).uv(0f, 1f).color(r, g, b, Math.min(1f, a)).endVertex();
        bb.vertex(m, cx + sx - ux, cy + sy - uy, cz + sz - uz).uv(1f, 1f).color(r, g, b, Math.min(1f, a)).endVertex();
        bb.vertex(m, cx + sx + ux, cy + sy + uy, cz + sz + uz).uv(1f, 0f).color(r, g, b, Math.min(1f, a)).endVertex();
        bb.vertex(m, cx - sx + ux, cy - sy + uy, cz - sz + uz).uv(0f, 0f).color(r, g, b, Math.min(1f, a)).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }
}

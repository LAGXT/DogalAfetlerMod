package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15C;

/**
 * Yalnız büyüyen GPU deposuyla köşe tamponu (K-E48). {@link VertexBuffer#upload} her yüklemede tam boyla {@code glBufferData}
 * çağırır; depo büyüyünce sürücü yeniden ayırır (10–12 ms, inceleme-su-gorsel P2-1). Burada depo {@code glBufferData(kapasite,
 * null)} ile yalnız kapasite aşılınca {@link #PAY} payıyla ayrılır, her yüklemede kullanılan kısım {@code glBufferSubData} ile
 * yazılır ve çizim yalnız kullanılan indeks sayısıyla yapılır: boş dolgu köşesi yazılmaz, yüklenmez, çizilmez. Çizim
 * {@link VertexBuffer#drawWithShader} ile (şader uniform'ları vanilla yolunda); onun okuduğu özel alanlar (biçim, sıralı indeks
 * tamponu, indeks sayısı, kip) yüklemede yazılır. Yalnız sıralı indeksli çizim (sıralanmamış {@code QUADS}); başka çizim durumu
 * vanilla {@code upload}'a düşer. Yalnız render thread'inde.
 */
final class KapasiteliTampon {
    static final float PAY = 1.25f;
    private static final Field VBO = alan("f_231217_");
    private static final Field BICIM = alan("f_85917_");
    private static final Field SIRALI = alan("f_166865_");
    private static final Field INDEKS_TIPI = alan("f_166861_");
    private static final Field INDEKS_SAYISI = alan("f_166863_");
    private static final Field KIP = alan("f_166864_");

    private final VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    /** GPU deposunun bayt boyu; 0: vanilla {@code upload} sonrası (boy bilinmiyor, ilk yüklemede yeniden ayrılır). */
    private int kapasite;

    private static Field alan(String srg) {
        return ObfuscationReflectionHelper.findField(VertexBuffer.class, srg);
    }

    /** Kurucunun çıktısını yükler ve bırakır; depo büyüdüyse {@code true}. */
    boolean yukle(BufferBuilder.RenderedBuffer cikti) {
        RenderSystem.assertOnRenderThread();
        BufferBuilder.DrawState durum = cikti.drawState();
        try {
            vb.bind();
            if (!durum.sequentialIndex() || durum.indexOnly()) {
                kapasite = 0;
                vb.upload(cikti);
                return false;
            }
            ByteBuffer kose = cikti.vertexBuffer();
            int bayt = kose.remaining();
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, VBO.getInt(vb));
            boolean buyudu = bayt > kapasite;
            if (buyudu) {
                kapasite = (int) Math.ceil(bayt * PAY);
                GlStateManager._glBufferData(GL15C.GL_ARRAY_BUFFER, kapasite, GL15C.GL_DYNAMIC_DRAW);
            }
            GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, kose);
            VertexFormat bicim = durum.format();
            if (BICIM.get(vb) != bicim) {
                VertexFormat eski = (VertexFormat) BICIM.get(vb);
                if (eski != null) {
                    eski.clearBufferState();
                }
                bicim.setupBufferState();
                BICIM.set(vb, bicim);
            }
            RenderSystem.AutoStorageIndexBuffer sirali = RenderSystem.getSequentialBuffer(durum.mode());
            sirali.bind(durum.indexCount());
            SIRALI.set(vb, sirali);
            INDEKS_TIPI.set(vb, durum.indexType());
            INDEKS_SAYISI.setInt(vb, durum.indexCount());
            KIP.set(vb, durum.mode());
            return buyudu;
        } catch (IllegalAccessException hata) {
            throw new IllegalStateException("VertexBuffer alanlarına erişilemedi", hata);
        } finally {
            cikti.release();
            VertexBuffer.unbind();
        }
    }

    void ciz(Matrix4f mv, Matrix4f proj, ShaderInstance sh) {
        vb.bind();
        vb.drawWithShader(mv, proj, sh);
    }
}

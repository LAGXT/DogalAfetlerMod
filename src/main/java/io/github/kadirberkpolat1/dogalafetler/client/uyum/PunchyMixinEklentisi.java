package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * {@code dogalafetler.uyum.mixins.json} eklentisi (K-E74): Punchy uyumunun mixin'i yalnız Punchy yüklüyken uygulanır. Mod listesi
 * mixin yüklenirken ({@code ModList}) henüz kurulmadığından Forge'un yükleme listesine ({@link LoadingModList}) bakılır.
 * Bu sınıf mixin paketinin DIŞINDA durur ({@code client.uyum.mixin} altındaki sınıflar doğrudan yüklenemez).
 * Oculus yüklüyken {@code GameRendererElMixin} uygulanmaz (K-E82): Oculus aynı {@code renderHandsWithItems} çağrısını zorunlu
 * bir {@code @Redirect} ile alır, ikinci redirect onun enjeksiyonunu kırar ve oyun yüklenmez.
 * K-E83: {@code EmbeddiumSivilariMixin} yalnız Embeddium (Oculus'un bağlı olduğu sıvı çizicisi) yüklükken uygulanır; Punchy'ye bakmaz.
 */
public final class PunchyMixinEklentisi implements IMixinConfigPlugin {
    private static final String PUNCHY = "punchy";
    private static final String OCULUS = "oculus";
    private static final String EL_MIXIN = "GameRendererElMixin";
    private static final String EMBEDDIUM = "embeddium";
    private static final String EMBEDDIUM_MIXIN = "EmbeddiumSivilariMixin";

    private boolean punchyVar;
    private boolean oculusVar;
    private boolean embeddiumVar;

    @Override
    public void onLoad(String mixinPaketi) {
        try {
            LoadingModList liste = LoadingModList.get();
            punchyVar = liste != null && liste.getModFileById(PUNCHY) != null;
            oculusVar = liste != null && liste.getModFileById(OCULUS) != null;
            embeddiumVar = liste != null && liste.getModFileById(EMBEDDIUM) != null;
        } catch (RuntimeException | LinkageError hata) {
            punchyVar = false;
            oculusVar = false;
            embeddiumVar = false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String hedefSinif, String mixinSinifi) {
        if (mixinSinifi.endsWith(EMBEDDIUM_MIXIN)) {
            return embeddiumVar;
        }
        if (oculusVar && mixinSinifi.endsWith(EL_MIXIN)) {
            return false;
        }
        return punchyVar;
    }

    @Override
    public void acceptTargets(Set<String> kendiHedefleri, Set<String> digerHedefler) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String hedefSinif, ClassNode hedef, String mixinSinifi, IMixinInfo bilgi) {
    }

    @Override
    public void postApply(String hedefSinif, ClassNode hedef, String mixinSinifi, IMixinInfo bilgi) {
    }
}

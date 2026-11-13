package io.github.kadirberkpolat1.dogalafetler.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Curios köprüsü: bir varlığın Curios yuvalarında (kolye, yüzük...) takılı yığınları. Curios API jar'ı çevrimdışı derlemede yok
 * (ve mod isteğe bağlı) olduğu için yöntemler yansıtmayla (5.2+ API: {@code CuriosApi.getCuriosInventory(LivingEntity)} ve
 * {@code ICuriosItemHandler.getEquippedCurios()}) bir kez çözülür ve önbellekte tutulur. Sıcaklık ölçümü ({@code IsiKaynaklari})
 * bu yığınları envanter yığınlarıyla aynı döngüde sayar; böylece Serin Kolye (ve kayıtlı her ısı eşyası) kolye yuvasında
 * da çalışır. Curios yüklü değilse ya da API beklediğimiz gibi değilse sonuç hep boş listedir; API uyuşmazlığı bir kez
 * günlüğe yazılır.
 */
public final class CuriosKopru {
    private static final String API_SINIFI = "top.theillusivec4.curios.api.CuriosApi";
    private static final String YONETICI_SINIFI = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler";

    private static boolean cozuldu;
    private static Method envanterYontemi;
    private static Method takiliYontemi;

    private CuriosKopru() {
    }

    /** {@code varlik}ın boş olmayan Curios yuva yığınları; Curios yoksa ya da API uyuşmuyorsa boş (değişmez) liste. */
    public static List<ItemStack> takiliYiginlar(LivingEntity varlik) {
        if (!Uyum.yuklu(Uyum.CURIOS) || !yontemlerHazir()) {
            return List.of();
        }
        try {
            Object lazy = envanterYontemi.invoke(null, varlik);
            if (!(lazy instanceof LazyOptional<?> secenek)) {
                return List.of();
            }
            Optional<?> yonetici = secenek.resolve();
            if (yonetici.isEmpty() || !(takiliYontemi.invoke(yonetici.get()) instanceof IItemHandlerModifiable yuvalar)) {
                return List.of();
            }
            List<ItemStack> yiginlar = new ArrayList<>(yuvalar.getSlots());
            for (int i = 0; i < yuvalar.getSlots(); i++) {
                ItemStack yigin = yuvalar.getStackInSlot(i);
                if (!yigin.isEmpty()) {
                    yiginlar.add(yigin);
                }
            }
            return yiginlar;
        } catch (ReflectiveOperationException | RuntimeException hata) {
            DogalAfetler.LOGGER.warn("[uyum] Curios sorgusu başarısız, Curios yuvaları yok sayılıyor: {}", hata.toString());
            envanterYontemi = null;
            return List.of();
        }
    }

    private static boolean yontemlerHazir() {
        if (!cozuldu) {
            cozuldu = true;
            try {
                ClassLoader yukleyici = CuriosKopru.class.getClassLoader();
                Class<?> api = Class.forName(API_SINIFI, true, yukleyici);
                Class<?> yonetici = Class.forName(YONETICI_SINIFI, true, yukleyici);
                envanterYontemi = api.getMethod("getCuriosInventory", LivingEntity.class);
                takiliYontemi = yonetici.getMethod("getEquippedCurios");
            } catch (ReflectiveOperationException | LinkageError hata) {
                DogalAfetler.LOGGER.warn("[uyum] Curios yüklü ama beklenen API (5.2+) bulunamadı, Curios yuvaları kapalı: {}", hata.toString());
            }
        }
        return envanterYontemi != null && takiliYontemi != null;
    }
}

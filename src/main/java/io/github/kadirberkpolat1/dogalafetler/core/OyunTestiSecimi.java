package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunTestiFiltresi;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * GameTest filtresinin sunucu ucu (K-E20). Forge {@code @GameTestHolder} metotlarını {@code RegisterGameTestsEvent}'ten sonra
 * kaydeder ve {@code Main} batch'leri kayıttan hemen sonra kurar; aradaki bir kanca yoktur. Bu yüzden kayıt defteri tam kalır
 * (bekçi {@link CekirdekOyunTestleri#hicBirBatchSatirKirmaz} bütün batch'leri sayar) ve süzme {@link GameTestServer}'ın koşacak
 * batch listesinde, testler başlamadan yapılır. Bekçi filtreden bağımsız her koşuda kalır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class OyunTestiSecimi {
    /** {@code GameTestServer.testBatches} (SRG); kurucu onu {@code Lists.newArrayList} ile değiştirilebilir kurar. */
    private static final String BATCH_LISTESI = "f_177587_";
    private static final String BEKCI = "hicBirBatchSatirKirmaz";

    private OyunTestiSecimi() {
    }

    @SubscribeEvent
    public static void sunucuBasliyor(ServerAboutToStartEvent olay) {
        if (!(olay.getServer() instanceof GameTestServer sunucu)) {
            return;
        }
        Optional<OyunTestiFiltresi> filtre = OyunTestiFiltresi.coz(System.getProperty(OyunTestiFiltresi.OZELLIK));
        List<GameTestBatch> batchler = ObfuscationReflectionHelper.getPrivateValue(GameTestServer.class, sunucu,
                BATCH_LISTESI);
        int toplam = batchler.stream().mapToInt(b -> b.getTestFunctions().size()).sum();
        if (filtre.isEmpty()) {
            DogalAfetler.LOGGER.info("[dogalafetler] GameTest filtresi yok: {} test", toplam);
            return;
        }
        Map<String, List<Method>> kaynaklar = testMetotlari();
        Method bekci = bekciMetodu();
        List<GameTestBatch> kalan = new ArrayList<>();
        int secilen = 0;
        for (GameTestBatch batch : batchler) {
            List<TestFunction> testler = batch.getTestFunctions().stream()
                    .filter(test -> kalir(test, kaynaklar.getOrDefault(anahtar(test.getBatchName(), test.getTestName()),
                            List.of()), bekci, filtre.get()))
                    .toList();
            if (testler.isEmpty()) {
                continue;
            }
            secilen += testler.size();
            kalan.add(testler.size() == batch.getTestFunctions().size() ? batch : new GameTestBatch(batch.getName(), testler,
                    batch::runBeforeBatchFunction, batch::runAfterBatchFunction));
        }
        batchler.clear();
        batchler.addAll(kalan);
        DogalAfetler.LOGGER.info("[dogalafetler] GameTest filtresi '{}': {}/{} test", filtre.get().desen(), secilen, toplam);
    }

    /**
     * Kaynak metodu bulunamayan test (ör. {@code @GameTestGenerator} üretimi) sınıf ve test adı yerine kayıtlı test adıyla
     * eşlenir.
     */
    private static boolean kalir(TestFunction test, List<Method> kaynaklar, Method bekci, OyunTestiFiltresi filtre) {
        if (kaynaklar.isEmpty()) {
            return filtre.uyar(test.getTestName(), test.getBatchName(), test.getTestName());
        }
        return kaynaklar.stream().anyMatch(metot -> metot.equals(bekci)
                || filtre.uyar(metot.getDeclaringClass().getSimpleName(), test.getBatchName(), metot.getName()));
    }

    /**
     * {@code @GameTestHolder} sınıflarının {@code @GameTest} metotları, vanilla test adı ve batch ile anahtarlanır. Vanilla test
     * adını küçük harfe çevirir; anahtar büyük/küçük harfe duyarsızdır (sınıf ve metot adları kaynaktaki yazımıyla kalır).
     */
    private static Map<String, List<Method>> testMetotlari() {
        Map<String, List<Method>> kaynaklar = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ModList.get().getAllScanData().stream()
                .flatMap(veri -> veri.getAnnotations().stream())
                .filter(ek -> GameTestHolder.class.getName().equals(ek.annotationType().getClassName()))
                .map(ek -> sinif(ek.clazz().getClassName()))
                .distinct()
                .flatMap(sinif -> Arrays.stream(sinif.getDeclaredMethods()))
                .forEach(metot -> {
                    GameTest test = metot.getAnnotation(GameTest.class);
                    if (test == null) {
                        return;
                    }
                    String ad = (ForgeGameTestHooks.prefixGameTestTemplate(metot)
                            ? metot.getDeclaringClass().getSimpleName() + "." : "") + metot.getName();
                    kaynaklar.computeIfAbsent(anahtar(test.batch(), ad), k -> new ArrayList<>()).add(metot);
                });
        return kaynaklar;
    }

    private static String anahtar(String batch, String testAdi) {
        return batch + "/" + testAdi;
    }

    private static Class<?> sinif(String ad) {
        try {
            return Class.forName(ad, false, OyunTestiSecimi.class.getClassLoader());
        } catch (ClassNotFoundException hata) {
            throw new IllegalStateException("GameTest sınıfı yüklenemedi: " + ad, hata);
        }
    }

    private static Method bekciMetodu() {
        try {
            return CekirdekOyunTestleri.class.getDeclaredMethod(BEKCI, GameTestHelper.class);
        } catch (NoSuchMethodException hata) {
            throw new IllegalStateException("Yerleşim bekçisi bulunamadı (K-E16, K-E20): " + BEKCI, hata);
        }
    }
}

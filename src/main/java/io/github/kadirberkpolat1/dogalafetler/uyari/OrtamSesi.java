package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamPaketi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Afet ortam sesleri için ortak sunucu API'si (docs/tasarim/ses.md §4; afet işçileri kullanır). Bir <b>katman</b> döngülü
 * bir sestir (uğultu, gürleme): sunucu başlatır, konumunu günceller, durdurur; istemci hacim eğrisini, mesafe sönümünü,
 * {@code ortamTavan} tavanını, siren sırasında -3 dB bastırmayı ve eşzamanlı katman sınırını uygular. Yalnız sunucu ana
 * thread'inde çağrılır.
 *
 * <pre>{@code
 * long afetNo = afet.baglam().baslangicSaati();
 * OrtamSesi.baslat(server, "kasirga/ugultu", afetNo, KasirgaSesleri.UGULTU, SoundSource.WEATHER,
 *         OrtamSesi.Yer.nokta(level.dimension(), konum, 160f), HacimEgrisi.giris(60, 1f));
 * OrtamSesi.konumGuncelle(server, "kasirga/ugultu", yeniKonum);  // hareketli afette en çok 4 Hz
 * OrtamSesi.durdur(server, "kasirga/ugultu", 60);               // 3 sn fade
 * }</pre>
 *
 * Afet bitince ({@link AfetBittiEvent}) o afetin ({@code afetNo}) unutulan katmanları {@value #VARSAYILAN_FADE_TICK} tick'te
 * kendiliğinden söner. Sonradan giren, boyut değiştiren ya da (ölüm, End çıkışı) yeniden doğan oyuncuya süren katmanlar
 * geçen süreyle gider.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class OrtamSesi {
    public static final int VARSAYILAN_FADE_TICK = 40;
    /** Hiçbir afete bağlı olmayan katman (sahibi kendisi durdurur). */
    public static final long AFETSIZ = -1L;

    private static final Map<String, Katman> KATMANLAR = new LinkedHashMap<>();

    /** Katmanın yeri: konumsuz (her boyutta aynı düzey) ya da bir boyutta bir nokta ve menzil (doğrusal sönüm). */
    public record Yer(ResourceKey<Level> boyut, Vec3 nokta, float menzil) {
        public static Yer global() {
            return new Yer(null, Vec3.ZERO, 0f);
        }

        public static Yer nokta(ResourceKey<Level> boyut, Vec3 nokta, float menzil) {
            if (!(menzil > 0f)) {
                throw new IllegalArgumentException("Konumlu katmanın menzili pozitif olmalı: " + menzil);
            }
            return new Yer(Objects.requireNonNull(boyut, "boyut"), Objects.requireNonNull(nokta, "nokta"), menzil);
        }

        public boolean globalMi() {
            return boyut == null;
        }
    }

    private record Katman(String kimlik, long afetNo, ResourceLocation ses, SoundSource kategori, Yer yer,
            HacimEgrisi egri, long baslangic) {
        OrtamPaketi paket(MinecraftServer server) {
            int gecen = (int) Math.max(0, Math.min(Integer.MAX_VALUE, server.getTickCount() - baslangic));
            return new OrtamPaketi(kimlik, afetNo, baslangic, ses, kategori,
                    yer.globalMi() ? null : yer.boyut().location(), yer.nokta().x, yer.nokta().y, yer.nokta().z,
                    yer.menzil(), egri, gecen);
        }

        boolean duyar(ServerPlayer oyuncu) {
            return yer.globalMi() || oyuncu.level().dimension().equals(yer.boyut());
        }
    }

    private OrtamSesi() {
    }

    /**
     * Katmanı başlatır; aynı kimlikte süren katman varsa yerine geçer (istemcide zaman çizelgesi sıfırdan başlar).
     *
     * @param kimlik {@code <alt sistem ≤ 15>/<katman ≤ 48>} (küçük ASCII, {@link OrtamKimligi}), aynı anda benzersiz
     * @param afetNo bağlı afetin başlangıç saati ({@code afet.baglam().baslangicSaati()}) ya da {@link #AFETSIZ}
     * @param ses ses olayı (modun kendi döngülü dosyası ya da vanilla ses)
     * @param kategori vanilla kaydırıcısı (ses.md §4 kural 6: WEATHER, BLOCKS, AMBIENT)
     */
    public static void baslat(MinecraftServer server, String kimlik, long afetNo, ResourceLocation ses,
            SoundSource kategori, Yer yer, HacimEgrisi egri) {
        OrtamKimligi.dogrula(kimlik);
        Katman katman = new Katman(kimlik, afetNo, Objects.requireNonNull(ses, "ses"),
                Objects.requireNonNull(kategori, "kategori"), Objects.requireNonNull(yer, "yer"),
                Objects.requireNonNull(egri, "egri"), server.getTickCount());
        KATMANLAR.put(kimlik, katman);
        gonder(server, katman);
        DogalAfetler.LOGGER.info("[uyari] ortam katmanı başladı: {} ({}, {}, {}) afetNo {}", kimlik, ses,
                kategori.getName(), yer.globalMi() ? "global" : yer.boyut().location() + " " + yer.nokta() + " menzil "
                        + yer.menzil(), afetNo);
    }

    /** Hareketli afetin katmanını taşır (boyut ve menzil aynı kalır); sahibi en çok 4 Hz çağırmalı. */
    public static void konumGuncelle(MinecraftServer server, String kimlik, Vec3 nokta) {
        Katman eski = KATMANLAR.get(kimlik);
        if (eski == null || eski.yer().globalMi()) {
            return;
        }
        Katman yeni = new Katman(kimlik, eski.afetNo(), eski.ses(), eski.kategori(),
                Yer.nokta(eski.yer().boyut(), nokta, eski.yer().menzil()), eski.egri(), eski.baslangic());
        KATMANLAR.put(kimlik, yeni);
        gonder(server, yeni);
    }

    /** Katmanı {@code fadeTick} içinde söndürür; yoksa bir şey yapmaz. */
    public static void durdur(MinecraftServer server, String kimlik, int fadeTick) {
        if (KATMANLAR.remove(kimlik) == null) {
            return;
        }
        AgKanali.herkeseGonder(new OrtamDurdurPaketi(kimlik, Math.max(1, fadeTick)));
        DogalAfetler.LOGGER.info("[uyari] ortam katmanı durdu: {} (fade {} tick)", kimlik, fadeTick);
    }

    /** Bir afetin bütün katmanlarını söndürür. */
    public static void afetKatmanlariniDurdur(MinecraftServer server, long afetNo, int fadeTick) {
        List<String> kimlikler = new ArrayList<>();
        KATMANLAR.forEach((kimlik, katman) -> {
            if (katman.afetNo() == afetNo) {
                kimlikler.add(kimlik);
            }
        });
        for (String kimlik : kimlikler) {
            durdur(server, kimlik, fadeTick);
        }
    }

    public static boolean aktifMi(String kimlik) {
        return KATMANLAR.containsKey(kimlik);
    }

    public static Set<String> aktifKatmanlar() {
        return Set.copyOf(KATMANLAR.keySet());
    }

    private static void gonder(MinecraftServer server, Katman katman) {
        OrtamPaketi paket = katman.paket(server);
        if (katman.yer().globalMi()) {
            AgKanali.herkeseGonder(paket);
            return;
        }
        ServerLevel level = server.getLevel(katman.yer().boyut());
        if (level != null) {
            for (ServerPlayer oyuncu : level.players()) {
                AgKanali.oyuncuyaGonder(oyuncu, paket);
            }
        }
    }

    private static void oyuncuyaSurenleri(ServerPlayer oyuncu) {
        for (Katman katman : KATMANLAR.values()) {
            if (katman.duyar(oyuncu)) {
                AgKanali.oyuncuyaGonder(oyuncu, katman.paket(oyuncu.server));
            }
        }
    }

    @SubscribeEvent
    public static void oyuncuGirdi(PlayerEvent.PlayerLoggedInEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            oyuncuyaSurenleri(oyuncu);
        }
    }

    @SubscribeEvent
    public static void boyutDegisti(PlayerEvent.PlayerChangedDimensionEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            oyuncuyaSurenleri(oyuncu);
        }
    }

    /** Ölüm ve End çıkışı {@code PlayerList.respawn} yolundan geçer; {@code PlayerChangedDimensionEvent} gelmez. */
    @SubscribeEvent
    public static void yenidenDogdu(PlayerEvent.PlayerRespawnEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            oyuncuyaSurenleri(oyuncu);
        }
    }

    @SubscribeEvent
    public static void afetBitti(AfetBittiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            afetKatmanlariniDurdur(server, olay.baslangicSaati(), VARSAYILAN_FADE_TICK);
        }
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        KATMANLAR.clear();
        SirenYoneticisi.sifirla();
    }
}

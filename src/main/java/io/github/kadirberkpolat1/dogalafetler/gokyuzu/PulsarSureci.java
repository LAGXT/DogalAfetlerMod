package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasi;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiSesleri;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Pulsar dönemi süreci (gökyüzü-2; çekirdek §3a): kızıl cüce yerine pulsar doğunca dünya buzul kıyametinin tersine
 * kavrulur. Evre ilerlemesi {@code pulsarIsinmaGun} boyunca 0 → 1 (ısınma) bu sürecindir; etkiler bu gücün yumuşak
 * basamağıyla ölçeklenir: oyuncuların çevresinde güneşe açık çimen kaba toprağa döner (güneş patlaması kavrulmasının
 * kuralı, değişen chunk sanal suya bildirilir), yanıcı yüzeyde ortak yangın sistemiyle yangın odakları çıkar (gerçek
 * tick'le, TPS kısması ve yangın tavanının yarısıyla sınırlı), kavurucu rüzgâr katmanı çalar. Sıcaklık (pulsar artışı),
 * buharlaşma ve buzun çözülmesi kendi sistemlerinde evreden okunur. {@code afetBlokHasari=false} iken blok değişmez ve
 * yangın çıkmaz. Pulsar kalıcı bir dönemdir: süreç evre PULSAR olduğu sürece aktiftir; {@code stop} ya da kural kapalı
 * etkileri durdurur (ulaşılmış evre korunur), yangınlar yayılmadan yanıp biter. İlerleme {@code zamanAdimi()} ile
 * biriktiği için {@link #devamEt} boştur. Döngü kimliği PULSAR'ın {@code evreBaslangicSaati} ({@code ekVeri("pulsar")}),
 * yangın kaynağı {@code GUNES_PATLAMASI} + bu saat.
 */
public final class PulsarSureci implements DonemSureci {
    public static final String KIMLIK = "pulsar";
    public static final String AD_ANAHTARI = "donem_sureci.dogalafetler." + KIMLIK;
    static final String DONGU = "dongu";
    static final String RUZGAR_KATMANI = "gokyuzu/pulsar";
    /** Oyuncu başına bir tick'teki en çok kavrulma denemesi ({@code /dogalafet hiz} yüksekken TPS korunur). */
    private static final int KAVRULMA_DENEME_TAVANI = 32;
    /** Yangın odağının oyuncudan uzaklığı (blok). */
    private static final int YANGIN_EN_YAKIN = 16;
    private static final int YANGIN_EN_UZAK = 96;
    /** Bir yangın denemesinde yanıcı sütun aranırken bakılan en çok sütun. */
    private static final int YANGIN_SUTUN_DENEMESI = 4;

    private static PulsarSureci ornek;

    private PulsarSureci() {
    }

    static void kaydet() {
        ornek = new PulsarSureci();
        AfetYoneticisi.donemSureciKaydet(ornek);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, ornek::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, DonemDegistiEvent.class, PulsarSureci::donemDegisti);
    }

    public static PulsarSureci al() {
        return ornek;
    }

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Component ad() {
        return Component.translatable(AD_ANAHTARI);
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.GUNES_PATLAMASI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        return DunyaDonemi.al(ustDunya).gunesEvresi() == GunesEvresi.PULSAR;
    }

    /** Işınmayı tamamlar (etkiler tam güce çıkar); tamamsa boş. */
    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        if (donem.gunesEvresi() != GunesEvresi.PULSAR || donem.evreIlerlemesi() >= 1f) {
            return Optional.empty();
        }
        donem.evreIlerlemesiAyarla(1f);
        return Optional.of(Component.translatable(GokyuzuKomutlari.ASAMA_PULSAR_TAMAM, ad()));
    }

    /** Etkiler durur: yangınlar yayılmadan yanıp biter, rüzgâr söner; ulaşılmış evre ve ısınma korunur. */
    @Override
    public void durdur(ServerLevel ustDunya, DurdurmaNedeni neden) {
        etkileriBitir(ustDunya.getServer(), DunyaDonemi.al(ustDunya), "durduruldu (" + neden + ")");
    }

    /** Yangın kaynağı: güneş patlaması türü + pulsar döngüsü (gerçek bir afetin {@code afetNo}'suyla çakışmaz). */
    static YanginKaynagi kaynak(DunyaDonemi donem) {
        return YanginKaynagi.afet(AfetTuru.GUNES_PATLAMASI, donem.ekVeri(KIMLIK).getLong(DONGU));
    }

    /** Rüzgâr söner; döngü biliniyorsa ({@code sifirla} öncesi) o döngünün ateşleri yayılmadan yanıp biter. */
    private static void etkileriBitir(MinecraftServer server, DunyaDonemi donem, String neden) {
        boolean ruzgar = OrtamSesi.aktifMi(RUZGAR_KATMANI);
        boolean dongu = donem.ekVeri(KIMLIK).contains(DONGU);
        if (ruzgar) {
            OrtamSesi.durdur(server, RUZGAR_KATMANI, 80);
        }
        if (dongu) {
            YanginYoneticisi.kaynakBitti(server, kaynak(donem), false);
        }
        if (ruzgar || dongu) {
            DogalAfetler.LOGGER.info("Pulsar etkileri bitti: {}", neden);
        }
    }

    /** Evre PULSAR'dan çıkınca (komutla kızıl cüce, {@code sifirla}) yangın ve rüzgâr biter. */
    private static void donemDegisti(DonemDegistiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || DunyaDonemi.al(server) != olay.donem()
                || olay.alan() != DonemAlani.GUNES_EVRESI && olay.alan() != DonemAlani.SIFIRLAMA
                || olay.donem().gunesEvresi() == GunesEvresi.PULSAR) {
            return;
        }
        etkileriBitir(server, olay.donem(), "evre " + olay.donem().gunesEvresi().kimlik());
    }

    private void tick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        ServerLevel ust = server.overworld();
        if (!aktifMi(ust)) {
            return;
        }
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        CompoundTag veri = donem.ekVeri(KIMLIK);
        if (!veri.contains(DONGU) || veri.getLong(DONGU) != donem.evreBaslangicSaati()) {
            veri.putLong(DONGU, donem.evreBaslangicSaati());
            donem.ekVeriDegisti(KIMLIK);
            yonetici.donemSureciBasladi(this);
            DogalAfetler.LOGGER.info("Pulsar süreci yeni döngüde: pulsar saat {}", donem.evreBaslangicSaati());
        }
        int adim = yonetici.zamanAdimi();
        if (yonetici.surecDurdu(this) || adim <= 0) {
            return;
        }
        float ilerleme = GunesZinciri.ilerlet(donem.evreIlerlemesi(), adim, GokyuzuAyarlari.pulsarIsinmaTick());
        donem.evreIlerlemesiAyarla(ilerleme);
        float guc = PulsarEgrileri.yogunluk(ilerleme);
        if (!OrtamSesi.aktifMi(RUZGAR_KATMANI)) {
            OrtamSesi.baslat(server, RUZGAR_KATMANI, OrtamSesi.AFETSIZ, GunesPatlamasiSesleri.KAVRULMA_RUZGAR,
                    SoundSource.WEATHER, OrtamSesi.Yer.global(), HacimEgrisi.giris(200, 0.85f));
        }
        if (guc <= 0f || !Kurallar.blokHasariAcik(ust.getGameRules())) {
            return;
        }
        double kisma = TpsOlcer.kismaCarpani();
        double kavrulma = GokyuzuAyarlari.pulsarKavrulmaOlasiligi() * guc * kisma;
        double yangin = GokyuzuAyarlari.pulsarYanginDakikaBasina() / 1200.0 * guc * kisma;
        boolean yanginYeri = YanginYoneticisi.aktifSayi(ust) < YanginAyarlari.aktifTavan() / 2;
        int deneme = Math.min(adim, KAVRULMA_DENEME_TAVANI);
        int yaricap = GunesPatlamasiAyarlari.kavrulmaYaricap();
        RandomSource r = ust.getRandom();
        YanginKaynagi kaynak = kaynak(donem);
        for (ServerPlayer oyuncu : ust.players()) {
            if (oyuncu.isSpectator()) {
                continue;
            }
            for (int i = 0; i < deneme; i++) {
                if (r.nextDouble() < kavrulma) {
                    int x = oyuncu.getBlockX() + r.nextInt(2 * yaricap + 1) - yaricap;
                    int z = oyuncu.getBlockZ() + r.nextInt(2 * yaricap + 1) - yaricap;
                    if (ust.hasChunk(x >> 4, z >> 4)) {
                        GunesPatlamasi.kavur(ust, new BlockPos(x,
                                ust.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z));
                    }
                }
            }
            if (yanginYeri && r.nextDouble() < yangin) {
                yanginCikar(ust, oyuncu, r, kaynak);
            }
        }
    }

    /** Oyuncunun çevresinde göğe açık ilk yanıcı yüzeyi (yaprak, çimen, odun) tutuşturur. */
    private static void yanginCikar(ServerLevel level, ServerPlayer oyuncu, RandomSource r, YanginKaynagi kaynak) {
        for (int i = 0; i < YANGIN_SUTUN_DENEMESI; i++) {
            double aci = r.nextDouble() * Math.PI * 2;
            double uzaklik = YANGIN_EN_YAKIN + r.nextDouble() * (YANGIN_EN_UZAK - YANGIN_EN_YAKIN);
            int x = oyuncu.getBlockX() + (int) Math.round(Math.cos(aci) * uzaklik);
            int z = oyuncu.getBlockZ() + (int) Math.round(Math.sin(aci) * uzaklik);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos ust = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            BlockState durum = level.getBlockState(ust);
            if (YanginYoneticisi.yanabilir(durum, level, ust) && YanginYoneticisi.tutustur(level, ust, kaynak, 1.0) > 0) {
                return;
            }
        }
    }
}

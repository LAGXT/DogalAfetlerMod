package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetIzi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.ArdisikSayac;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.CorbaTakibi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.DenizTakibi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.KutupSayaci;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.OdulTablosu;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.SagCikanTuretimi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.SagCikmaKosulu;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenIstegiEvent;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.Gokyuzu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SureliEtki;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Başarım ölçütlerini tetikleyen olay dinleyicileri (madde 12, N16; tablo {@code .vibe/kararlar/basarim.md} §A). Hepsi
 * sunucu ana thread'inde ve oyuncu başınadır. Afet sonuçları çekirdeğin {@link AfetBittiEvent}'inden (yalnız doğal
 * bitiş), ilk siren {@link SirenIstegiEvent}'ten (sireni gerçekten alan oyuncular), dönem eşikleri
 * {@link DonemDegistiEvent}'ten ve saniyelik denetimden gelir. Sahte oyuncular (GameTest) tick almadığı için
 * {@link #saniyelikDenetim} ve {@link #tickDenetimi} testten doğrudan çağrılabilir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class BasarimOlaylari {
    /** Sinkhole: etki alanında AKTIF aşamada bu kadar blokluk düşüş "çukura düştü" sayılır (afet ipucu yoksa). */
    static final float CUKUR_DUSUSU = 4.0f;
    /** Orman yangını: oyuncunun bu kadar blok yakınında yanan hücre varsa "yangın alanında" (afet ipucu yoksa). */
    static final double YANGIN_YAKINLIGI = 8.0;
    private static final float DONMA_TAM = DunyaDonemi.DONMA_EN_COK - 0.001f;
    private static final Map<UUID, Gecici> GECICI = new HashMap<>();
    private static final double[] ITME = new double[2];
    private static float sonDonma;

    /** Bellekte tutulan kısa ömürlü durum (oyuncu çıkınca silinir). */
    private static final class Gecici {
        private final ArdisikSayac yildizli = new ArdisikSayac();
        private final CorbaTakibi corba = new CorbaTakibi();
        private boolean yemeBasindaSoguk;
        private long yanginSaniyesi = Long.MIN_VALUE;
    }

    private BasarimOlaylari() {
    }

    private static Gecici gecici(ServerPlayer oyuncu) {
        return GECICI.computeIfAbsent(oyuncu.getUUID(), k -> new Gecici());
    }

    // ---------------------------------------------------------------- afet yaşam döngüsü

    /** Kök "Felaket Başlıyor": sireni alan her oyuncu (siren sürerken alana girenler ve sonradan bağlananlar da). */
    @SubscribeEvent
    public static void sirenDuyuldu(SirenIstegiEvent olay) {
        for (ServerPlayer oyuncu : olay.oyuncular()) {
            BasarimOlcutleri.SIREN_DUYULDU.tetikle(oyuncu);
        }
    }

    /**
     * Afet bittiğinde sağ çıkanlar: afet başarımları (tür, seviye, o afetteki koşullar), "Hepsini Atlattım" kümesi ve
     * "Birlikte Hayatta". Doğal olmayan bitişte (stop, kural, iptal, hata) hiçbiri sayılmaz.
     */
    @SubscribeEvent
    public static void afetBitti(AfetBittiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        long no = olay.baslangicSaati();
        Set<UUID> cevrimici = new HashSet<>();
        Set<UUID> payliAlanda = new HashSet<>();
        Set<UUID> payliOlenler = new HashSet<>();
        Map<UUID, AfetIzi> izler = new HashMap<>();
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            UUID kimlik = oyuncu.getUUID();
            cevrimici.add(kimlik);
            AfetIzi iz = BasarimVerisi.afetIzi(oyuncu);
            if (iz.no() == no) {
                izler.put(kimlik, iz);
                if (iz.alanda) {
                    payliAlanda.add(kimlik);
                }
                if (iz.oldu) {
                    payliOlenler.add(kimlik);
                }
            }
        }
        Set<UUID> sagCikanlar = SagCikanTuretimi.sagCikanlar(olay.dogalBitti(), olay.etkilenenler(), olay.sagCikanlar(),
                payliAlanda, payliOlenler, cevrimici);
        for (UUID kimlik : sagCikanlar) {
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
            if (oyuncu == null) {
                continue;
            }
            AfetIzi iz = izler.get(kimlik);
            Set<SagCikmaKosulu> kosullar = iz != null ? iz.kosullar() : EnumSet.of(SagCikmaKosulu.HASARSIZ);
            BasarimOlcutleri.AFETTEN_SAG_CIKTI.tetikle(oyuncu, olay.tur(), olay.seviye(), kosullar);
            AfetKumesi kume = BasarimVerisi.atlatilanlar(oyuncu);
            if (kume.ekle(olay.tur())) {
                BasarimVerisi.atlatilanlariYaz(oyuncu, kume);
            }
            if (kume.tamamMi()) {
                BasarimOlcutleri.HEPSINI_ATLATTI.tetikle(oyuncu);
            }
            BasarimOlcutleri.BIRLIKTE_HAYATTA.tetikle(oyuncu, sagCikanlar.size());
        }
    }

    /** Faz 4 afetlerinin ipucu: süren afetteki ize yazılır. */
    @SubscribeEvent
    public static void ipucuGeldi(BasarimIpucuEvent olay) {
        ServerPlayer oyuncu = olay.oyuncu();
        aktifAsamadakiAfet(oyuncu.server).ifPresent(afet -> isaretle(oyuncu, afet, olay.ipucu()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hasarAldi(LivingDamageEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && olay.getAmount() > 0) {
            aktifAsamadakiAfet(oyuncu.server).filter(afet -> alanda(oyuncu, afet))
                    .ifPresent(afet -> isaretle(oyuncu, afet, Ipucu.HASAR_ALDI));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void oldu(LivingDeathEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        MinecraftServer server = oyuncu.server;
        AfetYoneticisi.al(server).aktifAfet().ifPresent(afet -> {
            AfetIzi iz = BasarimVerisi.afetIzi(oyuncu);
            if (iz.no() == afetNo(afet) && !iz.oldu) {
                iz.oldu = true;
                BasarimVerisi.afetIziYaz(oyuncu, iz);
            }
        });
        KutupSayaci kutup = BasarimVerisi.kutup(oyuncu);
        kutup.olum();
        BasarimVerisi.kutupYaz(oyuncu, kutup);
        gecici(oyuncu).yildizli.sifirla();
        DenizSeviyesi deniz = DunyaDonemi.al(server).deniz();
        if (DenizTakibi.yukseliyorMu(deniz.hedefArtis(), deniz.mevcutArtis())) {
            BasarimVerisi.denizOlumYaz(oyuncu, deniz.baslangicSaati());
        }
    }

    // ---------------------------------------------------------------- tick denetimleri

    @SubscribeEvent
    public static void oyuncuTick(TickEvent.PlayerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || olay.side != LogicalSide.SERVER
                || !(olay.player instanceof ServerPlayer oyuncu)) {
            return;
        }
        tickDenetimi(oyuncu);
        if (oyuncu.tickCount % 20 == 0) {
            saniyelikDenetim(oyuncu);
        }
    }

    /** Her tick: tsunami sürüklemesi ve sinkhole düşüşü (kısa süren olaylar; yalnız o afet AKTIF iken). */
    public static void tickDenetimi(ServerPlayer oyuncu) {
        if (!oyuncu.isAlive()) {
            return;
        }
        Optional<Afet> aktif = aktifAsamadakiAfet(oyuncu.server);
        if (aktif.isEmpty()) {
            return;
        }
        Afet afet = aktif.get();
        if (afet.tur() == AfetTuru.TSUNAMI) {
            if (SanalSu.itmeHesapla(oyuncu, ITME) != null && alanda(oyuncu, afet)) {
                isaretle(oyuncu, afet, Ipucu.SURUKLENDI);
            }
        } else if (afet.tur() == AfetTuru.SINKHOLE && oyuncu.fallDistance >= CUKUR_DUSUSU && alanda(oyuncu, afet)) {
            isaretle(oyuncu, afet, Ipucu.CUKURA_DUSTU);
        }
    }

    /**
     * Saniyede bir: süren afetteki iz (alan payı, yangın alanı), süpernova, yıldızlı gece sayacı, donma eşiği, Kutup
     * Sığınağı sayacı, yünlü zırh ve çorba.
     */
    public static void saniyelikDenetim(ServerPlayer oyuncu) {
        DunyaDonemi donem = DunyaDonemi.al(oyuncu.server);
        Gecici gecici = gecici(oyuncu);
        boolean canli = oyuncu.isAlive();
        afetIziniGuncelle(oyuncu);
        if (canli && Gokyuzu.supernovayiGoruyorMu(oyuncu)) {
            BasarimOlcutleri.SUPERNOVA_GORUNDU.tetikle(oyuncu);
        }
        int saniye = gecici.yildizli.adim(canli && Gokyuzu.yildizliGeceMi(oyuncu));
        if (saniye > 0) {
            BasarimOlcutleri.YILDIZLI_GECE.tetikle(oyuncu, saniye);
        }
        float donma = donem.donmaIlerlemesi();
        if (canli && donma > 0) {
            BasarimOlcutleri.DONMA_ILERLEMESI.tetikle(oyuncu, donma);
        }
        kutupDenetimi(oyuncu, donem, canli);
        int yunlu = ZirhEkleri.yalitimGirdisi(oyuncu).yunluParca();
        if (yunlu > 0) {
            BasarimOlcutleri.YUNLU_PARCA.tetikle(oyuncu, yunlu);
        }
        corbaDenetimi(oyuncu, gecici);
    }

    private static void afetIziniGuncelle(ServerPlayer oyuncu) {
        Optional<Afet> aktif = aktifAsamadakiAfet(oyuncu.server);
        if (aktif.isEmpty() || !oyuncu.isAlive() || oyuncu.isSpectator() || !alanda(oyuncu, aktif.get())) {
            return;
        }
        Afet afet = aktif.get();
        AfetIzi iz = BasarimVerisi.afetIzi(oyuncu);
        boolean degisti = iz.afeteBagla(afetNo(afet));
        if (BasarimAyarlari.etkiAlaniPayi() > 0 && !iz.alanda) {
            iz.alanda = true;
            degisti = true;
        }
        if (degisti) {
            BasarimVerisi.afetIziYaz(oyuncu, iz);
        }
        if (afet.tur() == AfetTuru.ORMAN_YANGINI && oyuncu.level() instanceof ServerLevel level
                && YanginYoneticisi.aktifSayi(level, new AABB(oyuncu.blockPosition()).inflate(YANGIN_YAKINLIGI)) > 0) {
            isaretle(oyuncu, afet, Ipucu.YANGIN_ALANINDA);
        }
    }

    private static void kutupDenetimi(ServerPlayer oyuncu, DunyaDonemi donem, boolean canli) {
        long saat = oyuncu.server.overworld().getGameTime() + donem.uykuBirikimi();
        boolean aktif = canli
                && (donem.donmaIlerlemesi() >= DONMA_TAM || donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE);
        KutupSayaci sayac = BasarimVerisi.kutup(oyuncu);
        sayac.guncelle(saat, aktif);
        BasarimVerisi.kutupYaz(oyuncu, sayac);
        if (sayac.birikim() > 0) {
            BasarimOlcutleri.KUTUP_SIGINAGI.tetikle(oyuncu, sayac.gun());
        }
    }

    // ---------------------------------------------------------------- çorba (N16)

    @SubscribeEvent
    public static void kullanimBasladi(LivingEntityUseItemEvent.Start olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            gecici(oyuncu).yemeBasindaSoguk = sogukMu(oyuncu);
        }
    }

    /**
     * Yiyecek/içecek bitti: yerken (anlık ısınma eklenmeden önce) donma eşiğinin altındaysa ve yiyecek sıcaksa
     * (etiket ya da taze ısınma etkisi) çorba takibi başlar.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void kullanimBitti(LivingEntityUseItemEvent.Finish olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        Gecici gecici = gecici(oyuncu);
        boolean soguk = gecici.yemeBasindaSoguk || sogukMu(oyuncu);
        gecici.yemeBasindaSoguk = false;
        int sure = tazeIsinmaSuresi(oyuncu, 1);
        boolean taninan = olay.getItem().is(BasarimKimlikleri.SICAK_YIYECEK);
        gecici.corba.yedi(oyuncu.level().getGameTime(), soguk, taninan || sure > 0, sure);
    }

    private static void corbaDenetimi(ServerPlayer oyuncu, Gecici gecici) {
        long simdi = oyuncu.level().getGameTime();
        if (gecici.corba.teyitBekliyor(simdi)) {
            int sure = tazeIsinmaSuresi(oyuncu, CorbaTakibi.ADAY_PENCERESI);
            if (sure > 0) {
                gecici.corba.isinmaGozlendi(simdi, sure);
            }
        }
        if (gecici.corba.kontrol(simdi, kademe(oyuncu) == Kademe.NOTR)) {
            BasarimOlcutleri.CORBA_ICER_ISINIR.tetikle(oyuncu);
        }
    }

    private static Kademe kademe(ServerPlayer oyuncu) {
        return SicaklikAyarlari.al().esikler().kademe(VucutSicakligi.al(oyuncu));
    }

    /** N16 "donma eşiğinin altında": soğuk kademesi ya da daha aşağısı (yavaşlamanın başladığı eşik). */
    private static boolean sogukMu(ServerPlayer oyuncu) {
        Kademe k = kademe(oyuncu);
        return k == Kademe.SOGUK || k == Kademe.COK_SOGUK || k == Kademe.DONUYOR;
    }

    /** En çok {@code enCokGecen} tick önce eklenmiş ısınma etkilerinin en uzun kalan süresi; yoksa 0. */
    private static int tazeIsinmaSuresi(ServerPlayer oyuncu, int enCokGecen) {
        int sure = 0;
        for (SureliEtki etki : VucutSicakligi.etkiler(oyuncu)) {
            if (etki.toplam() > 0 && !etki.bitti() && etki.gecen() <= enCokGecen) {
                sure = Math.max(sure, etki.sure() - etki.gecen());
            }
        }
        return sure;
    }

    // ---------------------------------------------------------------- eşyalar

    @SubscribeEvent
    public static void esyaUretildi(PlayerEvent.ItemCraftedEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            esyaUretildi(oyuncu, BasarimKimlikleri.esya(olay.getCrafting()));
        }
    }

    /** #15, #16: üretilen eşyanın kimliği. */
    public static void esyaUretildi(ServerPlayer oyuncu, ResourceLocation esya) {
        BasarimOlcutleri.ESYA_URETILDI.tetikle(oyuncu, esya);
    }

    /** #7, #22: yerden alınan eşya (envantere girdikten sonra gelen olay; dolu envanter saymaz). */
    @SubscribeEvent
    public static void esyaToplandi(PlayerEvent.ItemPickupEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            BasarimOlcutleri.ESYA_TOPLANDI.tetikle(oyuncu, BasarimKimlikleri.esya(olay.getStack()));
        }
    }

    /** #17: Tüy Upgrade'i sağ tıkı işlendikten sonra (sıcaklığın {@code TuyUpgrade}'i olayı iptal eder). */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void esyaSagTik(PlayerInteractEvent.RightClickItem olay) {
        tuyDenetimi(olay.getEntity(), olay.getHand());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void blokSagTik(PlayerInteractEvent.RightClickBlock olay) {
        tuyDenetimi(olay.getEntity(), olay.getHand());
    }

    private static void tuyDenetimi(Player oyuncu, InteractionHand el) {
        if (el != InteractionHand.MAIN_HAND || !(oyuncu instanceof ServerPlayer sunucuOyuncusu)) {
            return;
        }
        ItemStack zirh = sunucuOyuncusu.getOffhandItem();
        int tuy = ZirhEkleri.zirhMi(zirh) ? ZirhEkleri.tuySayisi(zirh) : 0;
        if (tuy > 0) {
            BasarimOlcutleri.TUY_UPGRADE.tetikle(sunucuOyuncusu, tuy);
        }
    }

    /** #20, #21: kanca bir buz deliğindeyken tutulan yenebilir eşyalar (balık; çöp ve kitap yenmez). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void balikTutuldu(ItemFishedEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu) || !buzDeliginden(olay.getHookEntity())) {
            return;
        }
        int balik = 0;
        for (ItemStack yigin : olay.getDrops()) {
            if (yigin.isEdible()) {
                balik += yigin.getCount();
            }
        }
        buzDeligiBaligi(oyuncu, balik);
    }

    /** Buz deliğinden {@code adet} balık; toplam oyuncu verisinde birikir (balıkçılık olayı da buraya bağlanır). */
    public static void buzDeligiBaligi(ServerPlayer oyuncu, int adet) {
        if (adet > 0) {
            BasarimOlcutleri.BALIK_TUTULDU.tetikle(oyuncu, BasarimVerisi.balikEkle(oyuncu, adet));
        }
    }

    private static boolean buzDeliginden(FishingHook kanca) {
        Level level = kanca.level();
        BlockPos konum = kanca.blockPosition();
        for (int dy = -1; dy <= 1; dy++) {
            ResourceLocation blok = ForgeRegistries.BLOCKS.getKey(level.getBlockState(konum.above(dy)).getBlock());
            if (BasarimKimlikleri.BUZ_DELIGI.equals(blok)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- dünya dönemi

    /**
     * Dönem eşikleri hemen: komutla sıçrayan donma (süreçteki küçük adımları saniyelik denetim yakalar), deniz
     * yükselmesinin tamamlanması ve süpernovanın başlaması.
     */
    @SubscribeEvent
    public static void donemDegisti(DonemDegistiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || olay.donem() != DunyaDonemi.al(server)) {
            return;
        }
        DunyaDonemi donem = olay.donem();
        switch (olay.alan()) {
            case DONMA -> {
                float donma = donem.donmaIlerlemesi();
                if (Math.abs(donma - sonDonma) >= 1f) {
                    for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
                        if (oyuncu.isAlive()) {
                            BasarimOlcutleri.DONMA_ILERLEMESI.tetikle(oyuncu, donma);
                        }
                    }
                }
                sonDonma = donma;
            }
            case DENIZ -> denizDenetimi(server, donem.deniz());
            case GUNES_EVRESI -> {
                if (donem.gunesEvresi() == GunesEvresi.SUPERNOVA) {
                    for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
                        if (oyuncu.isAlive() && Gokyuzu.supernovayiGoruyorMu(oyuncu)) {
                            BasarimOlcutleri.SUPERNOVA_GORUNDU.tetikle(oyuncu);
                        }
                    }
                }
            }
            case SIFIRLAMA -> sonDonma = donem.donmaIlerlemesi();
            default -> {
            }
        }
    }

    /** #14: yükseliş tamamlandığı an çevrim içi, canlı ve bu döngüde ölmemiş oyuncular. */
    private static void denizDenetimi(MinecraftServer server, DenizSeviyesi deniz) {
        if (!DenizTakibi.tamamlandiMi(deniz.hedefArtis(), deniz.mevcutArtis())) {
            return;
        }
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            if (oyuncu.isAlive() && DenizTakibi.hakKazandiMi(BasarimVerisi.denizOlum(oyuncu), deniz.baslangicSaati())) {
                BasarimOlcutleri.DENIZ_YUKSELDI.tetikle(oyuncu);
            }
        }
    }

    // ---------------------------------------------------------------- ödül ve ünvan

    /** Config XP farkı (JSON varsayılanı verdi) ve meydan okuma ünvanı. */
    @SubscribeEvent
    public static void kazanildi(AdvancementEvent.AdvancementEarnEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        ResourceLocation kimlik = olay.getAdvancement().getId();
        if (!DogalAfetler.MOD_ID.equals(kimlik.getNamespace()) || !kimlik.getPath().startsWith("dogalafetler/")) {
            return;
        }
        Basarimlar.yoldan(kimlik.getPath()).ifPresent(basarim -> {
            int fark = OdulTablosu.xpFarki(basarim.tur(), BasarimAyarlari.xp(basarim.tur()));
            if (fark != 0) {
                oyuncu.giveExperiencePoints(fark);
            }
            if (basarim.unvan() != null) {
                Unvanlar.ayarla(oyuncu, Optional.of(basarim.unvan()));
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sohbetAdi(PlayerEvent.NameFormat olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && BasarimAyarlari.unvanGoster()) {
            BasarimVerisi.unvan(oyuncu).ifPresent(u -> olay.setDisplayname(Unvanlar.onekli(u, olay.getDisplayname())));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void tabAdi(PlayerEvent.TabListNameFormat olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && BasarimAyarlari.unvanGoster()) {
            BasarimVerisi.unvan(oyuncu).ifPresent(u -> {
                Component ad = olay.getDisplayName() != null ? olay.getDisplayName() : oyuncu.getName();
                olay.setDisplayName(Unvanlar.onekli(u, ad));
            });
        }
    }

    // ---------------------------------------------------------------- oyuncu yaşam döngüsü

    @SubscribeEvent
    public static void kopyalandi(PlayerEvent.Clone olay) {
        if (olay.getOriginal() instanceof ServerPlayer eski && olay.getEntity() instanceof ServerPlayer yeni) {
            BasarimVerisi.kopyala(eski, yeni);
        }
    }

    @SubscribeEvent
    public static void yenidenDogdu(PlayerEvent.PlayerRespawnEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && BasarimVerisi.unvan(oyuncu).isPresent()) {
            Unvanlar.yenile(oyuncu);
        }
    }

    /** Giriş: Kutup sayacı çevrim dışı süreyi saymasın; ünvan yüklenen veriyle yeniden hesaplansın. */
    @SubscribeEvent
    public static void girdi(PlayerEvent.PlayerLoggedInEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            KutupSayaci sayac = BasarimVerisi.kutup(oyuncu);
            sayac.girisYapti();
            BasarimVerisi.kutupYaz(oyuncu, sayac);
            if (BasarimVerisi.unvan(oyuncu).isPresent()) {
                Unvanlar.yenile(oyuncu);
            }
        }
    }

    @SubscribeEvent
    public static void cikti(PlayerEvent.PlayerLoggedOutEvent olay) {
        GECICI.remove(olay.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        GECICI.clear();
        sonDonma = 0;
    }

    // ---------------------------------------------------------------- yardımcılar

    private static Optional<Afet> aktifAsamadakiAfet(MinecraftServer server) {
        return AfetYoneticisi.al(server).aktifAfet().filter(afet -> afet.asama() == AfetAsamasi.AKTIF);
    }

    /** Afet numarası: başlangıç saati ({@code AfetBittiEvent.baslangicSaati()} ile aynı). */
    private static long afetNo(Afet afet) {
        return afet.baglam().baslangicSaati();
    }

    /** Oyuncu afetin boyutunda ve etki alanı + {@code basarim.etkiAlaniPayi} içinde mi. */
    private static boolean alanda(ServerPlayer oyuncu, Afet afet) {
        if (!afet.tur().sunucuGeneli() && !oyuncu.level().dimension().equals(afet.baglam().boyut())) {
            return false;
        }
        Optional<EtkiAlani> alan = AfetYoneticisi.al(oyuncu.server).etkiAlani(afet);
        return alan.isPresent()
                && alan.get().iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), BasarimAyarlari.etkiAlaniPayi());
    }

    private static void isaretle(ServerPlayer oyuncu, Afet afet, Ipucu ipucu) {
        AfetIzi iz = BasarimVerisi.afetIzi(oyuncu);
        boolean degisti = iz.afeteBagla(afetNo(afet));
        switch (ipucu) {
            case SURUKLENDI -> {
                degisti |= !iz.suruklendi;
                iz.suruklendi = true;
            }
            case CUKURA_DUSTU -> {
                degisti |= !iz.cukuraDustu;
                iz.cukuraDustu = true;
            }
            case HASAR_ALDI -> {
                degisti |= !iz.hasarAldi;
                iz.hasarAldi = true;
            }
            case YANGIN_ALANINDA -> {
                Gecici gecici = gecici(oyuncu);
                long saniye = oyuncu.level().getGameTime() / 20;
                if (gecici.yanginSaniyesi != saniye) {
                    gecici.yanginSaniyesi = saniye;
                    iz.yanginSaniye++;
                    degisti = true;
                }
            }
        }
        if (degisti) {
            BasarimVerisi.afetIziYaz(oyuncu, iz);
        }
    }
}

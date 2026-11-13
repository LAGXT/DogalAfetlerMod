package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikTutulduEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetIzi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.ArdisikSayac;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.CorbaTakibi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.DenizTakibi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
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
import io.github.kadirberkpolat1.dogalafetler.core.PulsarYolu;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenIstegiEvent;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.Gokyuzu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorZirhi;
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
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Başarım ölçütlerini tetikleyen olay dinleyicileri (madde 12, N16; tablo {@code .vibe/kararlar/basarim.md} §A). Hepsi
 * sunucu ana thread'inde ve oyuncu başınadır. Afet sonuçları çekirdeğin {@link AfetBittiEvent}'inden (yalnız doğal
 * bitiş), ilk siren {@link SirenIstegiEvent}'ten (sireni gerçekten alan oyuncular), dönem eşikleri
 * {@link DonemDegistiEvent}'ten ve saniyelik denetimden gelir. Afete özgü koşulların kaynağı, türü yayıncı olarak kayıtlıysa
 * afetin {@link BasarimIpucuEvent}'i, değilse başarımın yedek denetimidir (A-31, A-35). Sahte
 * oyuncular (GameTest) tick almadığı için {@link #saniyelikDenetim} ve {@link #tickDenetimi} testten doğrudan
 * çağrılabilir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class BasarimOlaylari {
    /** Sinkhole: etki alanında AKTIF aşamada bu kadar blokluk düşüş "çukura düştü" sayılır (afet ipucu yoksa). */
    static final float CUKUR_DUSUSU = 4.0f;
    /** Orman yangını: oyuncunun bu kadar blok yakınında yanan hücre varsa "yangın alanında" (afet ipucu yoksa). */
    static final double YANGIN_YAKINLIGI = 8.0;
    /** K-E74 "Sallanan Kule": deprem sürerken yerde ve çevresindeki en alçak zeminden en az bu kadar blok yüksekte kesintisiz saniye. */
    static final int KULE_SANIYE = 5;
    /** Kule ölçümü: oyuncudan dört yönde bu kadar blok uzakta zemin yüksekliği örneklenir (yüklü chunk'ta). */
    static final int KULE_MESAFE = 6;
    static final int KULE_FARKI = 12;
    /** K-E74 "Çifte Bela": iki AKTIF afetin alanında kesintisiz saniye. */
    static final int CIFTE_SANIYE = 30;
    /** K-E74 "Yüksek Zemin": zemin, tsunami dalga yüksekliğinin (deniz seviyesinden) bu kadar blok üstünde. */
    static final double TSUNAMI_YUKSEK_PAYI = 2.0;
    private static final int SANIYE_TICK = 20;
    /** Bir Minecraft günü (tick). */
    private static final long GUN_TICK = 24000L;
    private static final int[] KULE_X = {1, -1, 0, 0};
    private static final int[] KULE_Z = {0, 0, 1, -1};
    private static final float DONMA_TAM = DunyaDonemi.DONMA_EN_COK - 0.001f;
    private static final Map<UUID, Gecici> GECICI = new HashMap<>();
    private static final double[] ITME = new double[2];
    private static float sonDonma;

    /** Bellekte tutulan kısa ömürlü durum (oyuncu çıkınca silinir). */
    private static final class Gecici {
        private final ArdisikSayac yildizli = new ArdisikSayac();
        private final CorbaTakibi corba = new CorbaTakibi();
        private final ArdisikSayac kule = new ArdisikSayac();
        private final ArdisikSayac cifte = new ArdisikSayac();
        private boolean yemeBasindaDonuyor;
        /** Süren tsunamide dalga tepesinden yüksek zeminde bulundu; tsunami bitince sağ çıkan "Yüksek Zemin" alır. */
        private boolean tsunamiYuksekte;
        /** Kasırga havaya kaldırdı; kasırga bitince sağ çıkan "Bir Tur Attım" alır. */
        private boolean kasirgaKaldirdi;

        private void olumdeSifirla() {
            yildizli.sifirla();
            kule.sifirla();
            cifte.sifirla();
            tsunamiYuksekte = false;
            kasirgaKaldirdi = false;
        }
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
     * "Birlikte Hayatta". Komutla başlayan ve stop ile durdurulan afet de sayılır; kural, iptal ve hata bitişinde hiçbiri
     * sayılmaz ({@link AfetBittiEvent#basarimSayilir()}, AS-13).
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
        Set<UUID> sagCikanlar = SagCikanTuretimi.sagCikanlar(olay.basarimSayilir(), olay.etkilenenler(), olay.sagCikanlar(),
                payliAlanda, payliOlenler, cevrimici);
        Set<Ipucu> olayli = BasarimIpucuEvent.YAYINCILAR.olayli(olay.tur());
        for (UUID kimlik : sagCikanlar) {
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
            if (oyuncu == null) {
                continue;
            }
            AfetIzi iz = izler.get(kimlik);
            Set<SagCikmaKosulu> kosullar = iz != null ? iz.kosullar(olayli) : EnumSet.of(SagCikmaKosulu.HASARSIZ);
            BasarimOlcutleri.AFETTEN_SAG_CIKTI.tetikle(oyuncu, olay.tur(), olay.seviye(), kosullar);
            AfetKumesi kume = BasarimVerisi.atlatilanlar(oyuncu);
            if (kume.ekle(olay.tur())) {
                BasarimVerisi.atlatilanlariYaz(oyuncu, kume);
            }
            if (kume.tamamMi()) {
                BasarimOlcutleri.HEPSINI_ATLATTI.tetikle(oyuncu);
            }
            BasarimOlcutleri.BIRLIKTE_HAYATTA.tetikle(oyuncu, sagCikanlar.size());
            BasarimOlcutleri.SERI_HAYATTA.tetikle(oyuncu, BasarimVerisi.seriArtir(oyuncu));
            ozelAfetSonucu(oyuncu, olay.tur());
        }
        bayraklariTemizle(olay.tur());
    }

    /**
     * Faz 4 afetinin ipucu (§A.3): türü o ipucunun kayıtlı yayıncısıysa oyuncunun izine olay kaynağı olarak yazılır.
     * Kayıtsız kaynaktan gelen olay yok sayılır ve tür + ipucu başına bir kez uyarılır: o türde tek kaynak yedek
     * denetimdir (A-35). Yalnız AKTIF aşamada ve yalnız olayın kaynak afetine yazılır (AS-17: aynı anda küresel bir afet
     * AKTIF olsa da bölgesel afetin ipucu kaybolmaz).
     */
    @SubscribeEvent
    public static void ipucuGeldi(BasarimIpucuEvent olay) {
        ServerPlayer oyuncu = olay.oyuncu();
        AfetYoneticisi.al(oyuncu.server).aktifAfet(olay.kaynakTur())
                .filter(afet -> afetNo(afet) == olay.kaynakNo() && afet.asama() == AfetAsamasi.AKTIF).ifPresent(afet -> {
            switch (BasarimIpucuEvent.YAYINCILAR.olayGeldi(afet.tur(), olay.ipucu())) {
                case KABUL -> isaretle(oyuncu, afet, olay.ipucu(), true);
                case UYAR_YOK_SAY -> DogalAfetler.LOGGER.warn("[basarim] {} ipucu olayı {} afetinden geldi ama tür "
                        + "yayıncı olarak kayıtlı değil (BasarimIpucuEvent.yayinciKaydet); olay yok sayıldı, başarım yedek "
                        + "denetimle sayılır", olay.ipucu(), afet.tur().kimlik());
                case YOK_SAY -> { }
            }
        });
    }

    /** Çatı Altı yedeği (A-10): afet {@code HASAR_ALDI} yayınlamıyorsa AKTIF afette alandayken her hasar. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hasarAldi(LivingDamageEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && olay.getAmount() > 0) {
            aktifAsamadakiAfet(oyuncu)
                    .filter(afet -> yedekAcik(afet, Ipucu.HASAR_ALDI) && alanda(oyuncu, afet))
                    .ifPresent(afet -> isaretle(oyuncu, afet, Ipucu.HASAR_ALDI, false));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void oldu(LivingDeathEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        MinecraftServer server = oyuncu.server;
        for (Afet afet : AfetYoneticisi.al(server).aktifAfetler()) {
            AfetIzi iz = BasarimVerisi.afetIzi(oyuncu);
            if (iz.no() == afetNo(afet) && !iz.oldu) {
                iz.oldu = true;
                BasarimVerisi.afetIziYaz(oyuncu, iz);
            }
        }
        KutupSayaci kutup = BasarimVerisi.kutup(oyuncu);
        kutup.olum();
        BasarimVerisi.kutupYaz(oyuncu, kutup);
        gecici(oyuncu).olumdeSifirla();
        BasarimVerisi.hayatSayaclariniSifirla(oyuncu);
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

    /**
     * Her tick, yedek denetim (A-11, A-12; afet o ipucunu yayınlamıyorsa): tsunami sürüklemesi ve sinkhole düşüşü
     * (kısa süren olaylar; yalnız o afet AKTIF iken).
     */
    public static void tickDenetimi(ServerPlayer oyuncu) {
        if (!oyuncu.isAlive()) {
            return;
        }
        Optional<Afet> aktif = aktifAsamadakiAfet(oyuncu);
        if (aktif.isEmpty()) {
            return;
        }
        Afet afet = aktif.get();
        if (afet.tur() == AfetTuru.TSUNAMI && yedekAcik(afet, Ipucu.SURUKLENDI)) {
            if (SanalSu.itmeHesapla(oyuncu, ITME) != null && alanda(oyuncu, afet)) {
                isaretle(oyuncu, afet, Ipucu.SURUKLENDI, false);
            }
        } else if (afet.tur() == AfetTuru.SINKHOLE && yedekAcik(afet, Ipucu.CUKURA_DUSTU)
                && oyuncu.fallDistance >= CUKUR_DUSUSU && alanda(oyuncu, afet)) {
            isaretle(oyuncu, afet, Ipucu.CUKURA_DUSTU, false);
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
        ozelDenetim(oyuncu, gecici, canli);
    }

    /**
     * K-E74 (saniyede bir): hayatta kalma süresi ("On Gün On Gece"), meteor zırhı, deprem kulesi, tsunamide yüksek zemin ve
     * iki afetin alanında kalma ("Çifte Bela"). Afet taraması yalnız AKTIF afet varken iş yapar; chunk yükseklikleri yalnız
     * depremde ve yerdeyken okunur.
     */
    private static void ozelDenetim(ServerPlayer oyuncu, Gecici gecici, boolean canli) {
        if (canli) {
            long tick = BasarimVerisi.hayatTick(oyuncu) + SANIYE_TICK;
            BasarimVerisi.hayatTickYaz(oyuncu, tick);
            BasarimOlcutleri.HAYATTA_GUN.tetikle(oyuncu, (double) tick / GUN_TICK);
            for (ItemStack parca : oyuncu.getArmorSlots()) {
                if (meteorZirhiMi(parca)) {
                    OzelBasarim.ver(oyuncu, OzelBasarim.GOKTEN_ZIRH);
                    break;
                }
            }
        }
        boolean depremde = false;
        int alandakiAfet = 0;
        if (canli && !oyuncu.isSpectator()) {
            for (Afet afet : AfetYoneticisi.al(oyuncu.server).aktifAfetler()) {
                if (afet.asama() != AfetAsamasi.AKTIF || !alanda(oyuncu, afet)) {
                    continue;
                }
                alandakiAfet++;
                if (afet.tur() == AfetTuru.DEPREM) {
                    depremde = true;
                } else if (afet.tur() == AfetTuru.TSUNAMI && !gecici.tsunamiYuksekte && oyuncu.onGround()
                        && oyuncu.getY() >= oyuncu.level().getSeaLevel()
                                + TsunamiSeviyesi.al(afet.seviye()).yukseklik() + TSUNAMI_YUKSEK_PAYI) {
                    gecici.tsunamiYuksekte = true;
                }
            }
        }
        boolean kulede = depremde && oyuncu.onGround() && !oyuncu.getAbilities().flying && yuksekYerdeMi(oyuncu);
        if (gecici.kule.adim(kulede) >= KULE_SANIYE) {
            OzelBasarim.ver(oyuncu, OzelBasarim.SALLANAN_KULE);
        }
        if (gecici.cifte.adim(alandakiAfet >= 2) >= CIFTE_SANIYE) {
            OzelBasarim.ver(oyuncu, OzelBasarim.CIFTE_BELA);
        }
    }

    /** Oyuncu, dört yönde {@link #KULE_MESAFE} blok ötedeki en alçak zeminden en az {@link #KULE_FARKI} blok yüksekte mi (yüklü olmayan chunk: hayır). */
    private static boolean yuksekYerdeMi(ServerPlayer oyuncu) {
        if (!(oyuncu.level() instanceof ServerLevel level)) {
            return false;
        }
        int x = oyuncu.getBlockX();
        int z = oyuncu.getBlockZ();
        int enAlcak = Integer.MAX_VALUE;
        for (int i = 0; i < KULE_X.length; i++) {
            int px = x + KULE_X[i] * KULE_MESAFE;
            int pz = z + KULE_Z[i] * KULE_MESAFE;
            if (!level.hasChunk(px >> 4, pz >> 4)) {
                return false;
            }
            enAlcak = Math.min(enAlcak, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz));
        }
        return oyuncu.getY() - enAlcak >= KULE_FARKI;
    }

    /** Meteorit ya da Kozmik zirh eki taşıyan zırh parçası mi ("Gökten Dövme"). */
    private static boolean meteorZirhiMi(ItemStack yigin) {
        return ZirhEkleri.zirhMi(yigin) && (ZirhEkleri.varMi(yigin, MeteorZirhi.METEORITLI)
                || ZirhEkleri.varMi(yigin, MeteorZirhi.KOZMIK));
    }

    /** Tsunami ya da kasırga bitti: oyuncunun özel bayrağı varsa özel başarımı verir (yalnız sağ çıkanlar çağırır). */
    private static void ozelAfetSonucu(ServerPlayer oyuncu, AfetTuru tur) {
        Gecici gecici = gecici(oyuncu);
        if (tur == AfetTuru.TSUNAMI && gecici.tsunamiYuksekte) {
            OzelBasarim.ver(oyuncu, OzelBasarim.YUKSEK_ZEMIN);
        } else if (tur == AfetTuru.KASIRGA && gecici.kasirgaKaldirdi) {
            OzelBasarim.ver(oyuncu, OzelBasarim.GOZUN_ICINDE);
        }
    }

    /** Biten türün bayrakları herkes için silinir (sağ çıkmayanların eski bayrağı sonraki afete taşınmasın). */
    private static void bayraklariTemizle(AfetTuru tur) {
        for (Gecici gecici : GECICI.values()) {
            if (tur == AfetTuru.TSUNAMI) {
                gecici.tsunamiYuksekte = false;
            } else if (tur == AfetTuru.KASIRGA) {
                gecici.kasirgaKaldirdi = false;
            }
        }
    }

    /**
     * Kasırga bu oyuncuyu havaya kaldırıyor (kasırga lift sürerken her tick çağırır; yalnız bir bayrak yazar). Kasırga biterse
     * sağ çıkan oyuncu "Bir Tur Attım" kazanır.
     */
    public static void kasirgaKaldirdi(ServerPlayer oyuncu) {
        gecici(oyuncu).kasirgaKaldirdi = true;
    }

    private static void afetIziniGuncelle(ServerPlayer oyuncu) {
        Optional<Afet> aktif = aktifAsamadakiAfet(oyuncu);
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
        if (afet.tur() == AfetTuru.ORMAN_YANGINI && yedekAcik(afet, Ipucu.YANGIN_ALANINDA)
                && oyuncu.level() instanceof ServerLevel level
                && YanginYoneticisi.aktifSayi(level, new AABB(oyuncu.blockPosition()).inflate(YANGIN_YAKINLIGI)) > 0) {
            isaretle(oyuncu, afet, Ipucu.YANGIN_ALANINDA, false);
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
            gecici(oyuncu).yemeBasindaDonuyor = donuyorMu(oyuncu);
        }
    }

    /**
     * Yiyecek/içecek bitti: yemeye başlarken (anlık ısınma eklenmeden önce) donma durumundaysa ve yiyecek sıcaksa
     * (etiket ya da taze ısınma etkisi) çorba takibi başlar (A-14, A-29).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void kullanimBitti(LivingEntityUseItemEvent.Finish olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        Gecici gecici = gecici(oyuncu);
        boolean donuyor = gecici.yemeBasindaDonuyor || donuyorMu(oyuncu);
        gecici.yemeBasindaDonuyor = false;
        int sure = tazeIsinmaSuresi(oyuncu, 1);
        boolean taninan = olay.getItem().is(BasarimKimlikleri.SICAK_YIYECEK);
        gecici.corba.yedi(oyuncu.level().getGameTime(), donuyor, taninan || sure > 0, sure);
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

    /** Madde 12 #19 "donma durumundayken", N16 "donma eşiğinin altındayken": donma hasarı kademesi (A-29). */
    private static boolean donuyorMu(ServerPlayer oyuncu) {
        return CorbaTakibi.donmaDurumunda(kademe(oyuncu));
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

    /** K-E74 "Gökten Dövme": zırh eki tarifiyle meteor zırhı üretildi (giyilince de saniyelik denetim verir). */
    @SubscribeEvent
    public static void zirhEkiUretildi(PlayerEvent.ItemCraftedEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && meteorZirhiMi(olay.getCrafting())) {
            OzelBasarim.ver(oyuncu, OzelBasarim.GOKTEN_ZIRH);
        }
    }

    /** #15, #16: üretilen eşyanın kimliği. */
    public static void esyaUretildi(ServerPlayer oyuncu, ResourceLocation esya) {
        BasarimOlcutleri.ESYA_URETILDI.tetikle(oyuncu, esya);
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

    /** #20, #21: balıkçılığın delikten her çekmede bir kez yayınladığı olay (tek kaynak). */
    @SubscribeEvent
    public static void balikTutuldu(BalikTutulduEvent olay) {
        buzDeligiBaligi(olay.oyuncu(), olay.balikSayisi());
    }

    /** Buz deliğinden {@code adet} balık; toplam oyuncu verisinde birikir (balıkçılık olayı da buraya bağlanır). */
    public static void buzDeligiBaligi(ServerPlayer oyuncu, int adet) {
        if (adet > 0) {
            BasarimOlcutleri.BALIK_TUTULDU.tetikle(oyuncu, BasarimVerisi.balikEkle(oyuncu, adet));
        }
    }

    // ---------------------------------------------------------------- dünya dönemi

    /**
     * Dönem eşikleri hemen: komutla sıçrayan donma (süreçteki küçük adımları saniyelik denetim yakalar), deniz
     * yükselmesinin tamamlanması ve süpernovanın başlaması. {@code sifirla} (K-E11): kazanılmış başarımlar ve oyuncu
     * verisindeki kalıcı ilerleme (atlatılan türler, balık, ünvan) kalır; döneme bağlı ara sayaç (donma %100'den beri
     * gün) yeni döngü işaretiyle sıfırdan başlar.
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
                } else if (donem.gunesEvresi() == GunesEvresi.NORMAL && PulsarYolu.gunesDondu(donem)) {
                    // N29: gunesDondu evreden önce yazılır; süpernova yolu ve geri alma (bayrak yok) sayılmaz.
                    for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
                        if (oyuncu.isAlive() && oyuncu.level().dimension() == Level.OVERWORLD) {
                            BasarimOlcutleri.YENI_SAFAK.tetikle(oyuncu);
                        }
                    }
                }
            }
            case SIFIRLAMA -> {
                sonDonma = donem.donmaIlerlemesi();
                BasarimVerisi.sifirlandi(donem, server.overworld().getGameTime());
            }
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

    /**
     * Meydan okuma ünvanı (A-22, A-32): ilerleme olayı kazanım olayından önce gelir; ünvan burada verilince meydan
     * okumanın kendi duyurusu ({@link #kazanildi}) da yeni ünvanı taşır.
     */
    @SubscribeEvent
    public static void ilerledi(AdvancementEvent.AdvancementProgressEvent olay) {
        if (olay.getProgressType() == AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT
                && olay.getAdvancementProgress().isDone() && olay.getEntity() instanceof ServerPlayer oyuncu) {
            bizimBasarim(olay.getAdvancement()).filter(basarim -> basarim.unvan() != null)
                    .ifPresent(basarim -> Unvanlar.ayarla(oyuncu, Optional.of(basarim.unvan())));
        }
    }

    /**
     * Config XP farkı (JSON varsayılanı verdi) ve sohbet duyurusu (A-37): bizim advancement'larda vanilla duyuru kapalı
     * ({@code announce_to_chat=false}); duyuru {@code announceAdvancements} açıkken burada, ünvan önekiyle yapılır. Veri
     * paketi {@code announce_to_chat=true} yaparsa vanilla duyurur, mod susar (çift mesaj yok).
     */
    @SubscribeEvent
    public static void kazanildi(AdvancementEvent.AdvancementEarnEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            Advancement advancement = olay.getAdvancement();
            bizimBasarim(advancement).ifPresent(basarim -> {
                int fark = OdulTablosu.xpFarki(basarim.tur(), BasarimAyarlari.xp(basarim.tur()));
                if (fark != 0) {
                    oyuncu.giveExperiencePoints(fark);
                }
                if (advancement.getDisplay() != null && !advancement.getDisplay().shouldAnnounceChat()
                        && oyuncu.level().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
                    oyuncu.server.getPlayerList().broadcastSystemMessage(
                            Unvanlar.duyuru(oyuncu, basarim.tur(), advancement.getChatComponent()), false);
                }
            });
        }
    }

    private static Optional<Basarimlar> bizimBasarim(Advancement advancement) {
        ResourceLocation kimlik = advancement.getId();
        if (!DogalAfetler.MOD_ID.equals(kimlik.getNamespace()) || !kimlik.getPath().startsWith("dogalafetler/")) {
            return Optional.empty();
        }
        return Basarimlar.yoldan(kimlik.getPath());
    }

    // ---------------------------------------------------------------- oyuncu yaşam döngüsü

    @SubscribeEvent
    public static void kopyalandi(PlayerEvent.Clone olay) {
        if (olay.getOriginal() instanceof ServerPlayer eski && olay.getEntity() instanceof ServerPlayer yeni) {
            BasarimVerisi.kopyala(eski, yeni);
        }
    }

    /** Giriş: Kutup sayacı çevrim dışı süreyi saymasın. */
    @SubscribeEvent
    public static void girdi(PlayerEvent.PlayerLoggedInEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            KutupSayaci sayac = BasarimVerisi.kutup(oyuncu);
            sayac.girisYapti();
            BasarimVerisi.kutupYaz(oyuncu, sayac);
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
        BasarimVerisi.sunucuDurdu();
    }

    // ---------------------------------------------------------------- yardımcılar

    /**
     * Oyuncu için AKTIF aşamadaki afet: birden çok sürüyorsa (AS-8) oyuncunun alanında olduğu ilk bölgesel afet, yoksa
     * alanında olduğu ilk küresel afet (ay, güneş: herkes alanda sayılır; AS-17), hiçbirinin alanında değilse AKTIF olanların
     * ilki. Oyuncunun izi tek afete bağlıdır ({@link AfetIzi}): küresel + bölgesel afette iz bölgesel afettedir.
     */
    private static Optional<Afet> aktifAsamadakiAfet(ServerPlayer oyuncu) {
        Afet kuresel = null;
        Afet ilk = null;
        for (Afet afet : AfetYoneticisi.al(oyuncu.server).aktifAfetler()) {
            if (afet.asama() != AfetAsamasi.AKTIF) {
                continue;
            }
            if (alanda(oyuncu, afet)) {
                if (!afet.tur().sunucuGeneli()) {
                    return Optional.of(afet);
                }
                if (kuresel == null) {
                    kuresel = afet;
                }
            }
            if (ilk == null) {
                ilk = afet;
            }
        }
        return Optional.ofNullable(kuresel != null ? kuresel : ilk);
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

    /** Yedek denetim bu afet türünde çalışır mı: tür o ipucunu kendisi yayınlıyorsa hayır (A-31). */
    private static boolean yedekAcik(Afet afet, Ipucu ipucu) {
        return !BasarimIpucuEvent.YAYINCILAR.yayinliyor(afet.tur(), ipucu);
    }

    /** İpucunu oyuncunun bu afetteki izine kaynağıyla yazar ({@code olaydan}: afetin olayı; değilse yedek tahmin). */
    private static void isaretle(ServerPlayer oyuncu, Afet afet, Ipucu ipucu, boolean olaydan) {
        AfetIzi iz = BasarimVerisi.afetIzi(oyuncu);
        boolean degisti = iz.afeteBagla(afetNo(afet));
        degisti |= iz.isaretle(ipucu, olaydan, oyuncu.level().getGameTime() / 20);
        if (degisti) {
            BasarimVerisi.afetIziYaz(oyuncu, iz);
        }
    }
}

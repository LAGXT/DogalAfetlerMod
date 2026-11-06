package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Ay çarpması (bölüm H; tasarım {@code docs/tasarim/ay_carpmasi.md}). Sunucu geneli, seviyesiz, tek seferlik:
 *
 * <ul>
 * <li>SIREN (60 sn, K0-19): ay {@link AyDurumu#YAKLASIYOR}; istemci yaklaşan ve parçalanan ayı olay saatinden çizer;
 * 40. sn'den uğultu, 55. sn'den çıtırtı (global ortam katmanı).</li>
 * <li>AKTIF başı = çarpma: ay {@link AyDurumu#YOK} (çarpma saatiyle; deniz yükselmesi süreci bunu dinler), tsunami
 * çarpanı eklenir, siren çekirdekte fade-out olur. 1,5 sn sonra güm (her oyuncuya), 3–15 sn sarsıntı (kamera
 * istemcide, düşen bloklar burada), 10. sn'den {@code meteorSuresiSn} ay parçası yağmuru ({@link MeteorAPI}).</li>
 * <li>Yağmur süresi dolunca doğal bitiş ("Ay Düştü").</li>
 * </ul>
 *
 * Yaklaşma sırasında durdurulan (komut, kural, hata) afette ay {@link AyDurumu#VAR}'a döner (ulaşılmamış dönem, ay
 * çarpması yeniden seçilebilir); çarpmadan sonra ayın yokluğu ve tsunami çarpanı kalır (madde 11, K-C7).
 */
public final class AyCarpmasi extends Afet {
    static final String YAKLASMA_KATMANI = "ay_carpmasi/yaklasma";
    static final String CITIRTI_KATMANI = "ay_carpmasi/citirti";
    static final int GUM_TICK = Math.round(AyEgrileri.GUM_SN * 20);
    static final int SARSINTI_BAS_TICK = Math.round(AyEgrileri.SARSINTI_BAS_SN * 20);
    static final int SARSINTI_SON_TICK = Math.round(AyEgrileri.SARSINTI_SON_SN * 20);
    static final int METEOR_BAS_TICK = Math.round(AyEgrileri.METEOR_BAS_SN * 20);

    /** Ortam katmanlarının eğrisi gerçek tick'tir: uğultu 40–55 sn'de, çıtırtı 55–59 sn'de girer. */
    private static final int UGULTU_BAS = 800;
    private static final int UGULTU_TAM = 1100;
    private static final int CITIRTI_TAM = 1180;

    private long aktifGecen;
    private boolean gumCaldi;
    private boolean meteorBasladi;
    private final SarsintiDusmeleri dusmeler = new SarsintiDusmeleri();

    public AyCarpmasi() {
        super(AfetTuru.AY_CARPMASI);
    }

    @Override
    public EtkiAlani etkiAlani() {
        return EtkiAlani.herYer();
    }

    /** Madde 4: ay yok olduktan sonra ay çarpması elle de başlamaz (yeniden denemek için {@code /dogalafet sifirla}). */
    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        return DunyaDonemi.al(level).ayVar();
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.SIREN) {
            DunyaDonemi donem = DunyaDonemi.al(level);
            donem.ayDurumuAyarla(AyDurumu.YAKLASIYOR, donem.saat());
            sesleriBaslat(level.getServer(), 0);
            DogalAfetler.LOGGER.info("[ay_carpmasi] ay yaklaşıyor: olay saati {}", donem.ayOlaySaati());
        } else if (yeni == AfetAsamasi.AKTIF) {
            carp(level);
        }
    }

    private void carp(ServerLevel level) {
        MinecraftServer server = level.getServer();
        DunyaDonemi donem = DunyaDonemi.al(level);
        donem.ayDurumuAyarla(AyDurumu.YOK, donem.saat());
        AfetYoneticisi.al(server).ayCarpmasiTsunamiCarpaniniEkle();
        // Parlama anında tam sessizlik: yaklaşma katmanları hemen kesilir, güm 1,5 sn sonra gelir.
        OrtamSesi.durdur(server, YAKLASMA_KATMANI, 4);
        OrtamSesi.durdur(server, CITIRTI_KATMANI, 4);
        aktifGecen = 0;
        altAsamaAyarla(0);
        DogalAfetler.LOGGER.info("[ay_carpmasi] ay çarptı: saat {}, tsunami çarpanı x{}", donem.ayOlaySaati(),
                String.format(Locale.ROOT, "%.2f", donem.carpan(AfetTuru.TSUNAMI)));
    }

    /** Yaklaşma katmanları; {@code gecen}: sirenin başından beri geçen gerçek tick (kayıttan dönüşte). */
    private void sesleriBaslat(MinecraftServer server, long gecen) {
        long no = baglam().baslangicSaati();
        OrtamSesi.baslat(server, YAKLASMA_KATMANI, no, AyCarpmasiSesleri.YAKLASMA, SoundSource.AMBIENT,
                OrtamSesi.Yer.global(), giris(gecen, UGULTU_BAS, UGULTU_TAM, 1f));
        OrtamSesi.baslat(server, CITIRTI_KATMANI, no, AyCarpmasiSesleri.CITIRTI, SoundSource.AMBIENT,
                OrtamSesi.Yer.global(), giris(gecen, UGULTU_TAM, CITIRTI_TAM, 0.8f));
    }

    /** {@code bas}'a kadar sessiz, {@code tam}'da {@code hacim}; {@code gecen} kadar ilerlemiş eğri. */
    static HacimEgrisi giris(long gecen, int bas, int tam, float hacim) {
        int b = (int) Math.max(0, bas - gecen);
        int t = (int) Math.max(1, tam - gecen);
        if (b <= 0) {
            float simdi = hacim * Math.min(1f, (float) Math.max(0, gecen - bas) / (tam - bas));
            return t <= 1 ? HacimEgrisi.sabit(hacim)
                    : HacimEgrisi.of(new HacimEgrisi.Nokta(0, simdi), new HacimEgrisi.Nokta(t, hacim));
        }
        return HacimEgrisi.of(new HacimEgrisi.Nokta(0, 0f), new HacimEgrisi.Nokta(b, 0f),
                new HacimEgrisi.Nokta(Math.max(b + 1, t), hacim));
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        long once = aktifGecen;
        aktifGecen += Math.max(0, adim);
        if (!gumCaldi && aktifGecen >= GUM_TICK) {
            gum(level.getServer());
        }
        if (aktifGecen > SARSINTI_BAS_TICK && once < SARSINTI_SON_TICK) {
            int pay = (int) (Math.min(aktifGecen, SARSINTI_SON_TICK) - Math.max(once, SARSINTI_BAS_TICK));
            dusmeler.tick(level, pay, SARSINTI_SON_TICK - SARSINTI_BAS_TICK);
            if (aktifGecen >= SARSINTI_SON_TICK) {
                DogalAfetler.LOGGER.info("[ay_carpmasi] sarsıntı bitti: {} blok düştü", dusmeler.toplam());
            }
        }
        if (!meteorBasladi && aktifGecen >= METEOR_BAS_TICK) {
            meteorBaslat(level);
        }
    }

    /** Derin güm ve uzun gürleme: her boyuttaki her oyuncuya kendi konumunda (mesafeden bağımsız, madde 3). */
    private void gum(MinecraftServer server) {
        gumCaldi = true;
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            oyuncu.playNotifySound(AyCarpmasiSesleri.carpma(), SoundSource.AMBIENT, 1f, 1f);
        }
    }

    private void meteorBaslat(ServerLevel level) {
        meteorBasladi = true;
        altAsamaAyarla(1);
        yagmuruKur(level, meteorSuresiTick() - (aktifGecen - METEOR_BAS_TICK));
    }

    /** Kalan ölçekli süre için ay parçası yağmuru; yağmur süresi gerçek tick'tir. */
    private void yagmuruKur(ServerLevel level, long kalanOlcekli) {
        if (kalanOlcekli <= 0) {
            return;
        }
        int gercek = (int) Math.max(1, Math.min(Integer.MAX_VALUE,
                AfetYoneticisi.al(level.getServer()).zamanOlcegi().gercekTick(kalanOlcekli)));
        int kimlik = MeteorAPI.yagmur(level, List.of(), gercek, AyCarpmasiAyarlari.meteorDakikaBasina(),
                AyCarpmasiAyarlari.meteorBuyukOrani(), kaynak());
        DogalAfetler.LOGGER.info("[ay_carpmasi] ay parçası yağmuru #{} başladı: {} tick", kimlik, gercek);
    }

    private YanginKaynagi kaynak() {
        return YanginKaynagi.afet(AfetTuru.AY_CARPMASI, baglam().baslangicSaati());
    }

    private static long meteorSuresiTick() {
        return AyCarpmasiAyarlari.meteorSuresiSn() * 20L;
    }

    @Override
    protected boolean aktifBitti() {
        return aktifGecen >= METEOR_BAS_TICK + meteorSuresiTick();
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, METEOR_BAS_TICK + meteorSuresiTick() - aktifGecen);
    }

    /** {@code asama ileri} AKTIF'te: sarsıntı → yağmur (hemen), yağmurdan sonra BITIS. */
    @Override
    protected boolean altAsamaIleri(ServerLevel level) {
        if (meteorBasladi) {
            return false;
        }
        if (!gumCaldi) {
            gum(level.getServer());
        }
        aktifGecen = Math.max(aktifGecen, METEOR_BAS_TICK);
        meteorBaslat(level);
        return true;
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        MinecraftServer server = level.getServer();
        DunyaDonemi donem = DunyaDonemi.al(level);
        if (donem.ayDurumu() == AyDurumu.YAKLASIYOR) {
            donem.ayDurumuAyarla(AyDurumu.VAR, AyDurumu.OLAY_YOK);
            DogalAfetler.LOGGER.info("[ay_carpmasi] yaklaşma durduruldu: ay yerine döndü");
        }
        OrtamSesi.durdur(server, YAKLASMA_KATMANI, 20);
        OrtamSesi.durdur(server, CITIRTI_KATMANI, 20);
        if (temiz) {
            dusmeler.geriAl(level);
        }
    }

    @Override
    protected void yuklendi(ServerLevel level) {
        if (asama() == AfetAsamasi.SIREN) {
            long gecenGercek = AfetYoneticisi.al(level.getServer()).zamanOlcegi().gercekTick(asamaGecenTick());
            sesleriBaslat(level.getServer(), gecenGercek);
        } else if (asama() == AfetAsamasi.AKTIF && meteorBasladi && !aktifBitti()) {
            yagmuruKur(level, METEOR_BAS_TICK + meteorSuresiTick() - aktifGecen);
        }
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        etiket.putLong("aktifGecen", aktifGecen);
        etiket.putBoolean("gum", gumCaldi);
        etiket.putBoolean("meteor", meteorBasladi);
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        aktifGecen = Math.max(0, etiket.getLong("aktifGecen"));
        gumCaldi = etiket.getBoolean("gum");
        meteorBasladi = etiket.getBoolean("meteor");
    }
}

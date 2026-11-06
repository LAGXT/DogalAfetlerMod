package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Ay çarpması sonrası deniz seviyesi yükselmesi (bölüm H, N10, N18; sözleşme §3a sahip deseni). Ay yok olunca
 * ({@code DonemDegistiEvent(AY)}) {@code aysonrasiBeklemeDk} sayacı başlar; dolunca artış {@code denizYukselmeDk} boyunca
 * doğrusal yükselir ve kalıcı kalır (config ile zirvede bekleyip geri dönebilir). Bekleme ve yükselme
 * {@link AfetYoneticisi#zamanAdimi()} ile birikir: durdurulunca ilerlemez, kural açılınca kaldığı yerden sürer. Döngü
 * kimliği {@code deniz().baslangicSaati()}. Buharlaşma (N10) başlayınca artış derinlik kadar kademeli sıfıra iner ve bir
 * daha gelmez. Çalışma verisi dönemin {@code ekVeri("su")} etiketindedir.
 */
public final class DenizYukselmesiSureci implements DonemSureci {
    public static final String KIMLIK = "deniz_yukselmesi";
    static final String VERI = "su";
    static final DenizYukselmesiSureci ORNEK = new DenizYukselmesiSureci();

    /** {@code /dogalafet aysonrasi atla} sonucu. */
    public enum AtlaSonucu {
        BASLADI, AY_VAR, BITTI, ZATEN
    }

    private DenizYukselmesiSureci() {
    }

    static void kaydet() {
        AfetYoneticisi.donemSureciKaydet(ORNEK);
    }

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Component ad() {
        return Component.translatable("donem_sureci.dogalafetler." + KIMLIK);
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.AY_CARPMASI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        DunyaDonemi d = DunyaDonemi.al(ustDunya);
        DenizSeviyesi s = d.deniz();
        return d.ekVeri(VERI).contains("bekleme") || s.mevcutArtis() != s.hedefArtis() || !s.kalici() && s.mevcutArtis() > 0;
    }

    /** Ay yok oldu: bekleme sayacı kurulur (yükselme bir kez olur; güneş patlaması sonrası dönemlerde gelmez). */
    void ayDegisti(DunyaDonemi d) {
        CompoundTag v = d.ekVeri(VERI);
        GunesEvresi evre = d.gunesEvresi();
        if (d.ayVar() || v.getBoolean("bitti") || v.contains("bekleme") || d.deniz().hedefArtis() > 0
                || evre != GunesEvresi.NORMAL && evre != GunesEvresi.BUYUYEN) {
            return;
        }
        long bekleme = DenizAdimi.dakikaTick(SuAyarlari.sunucu().aysonrasiBeklemeDk.get());
        v.putLong("bekleme", bekleme);
        d.ekVeriDegisti(VERI);
        DogalAfetler.LOGGER.info("Sanal su: ay yok oldu, deniz yükselmesi {} ölçekli tick sonra başlayacak", bekleme);
    }

    void tick(MinecraftServer server) {
        AfetYoneticisi y = AfetYoneticisi.al(server);
        DunyaDonemi d = y.donem();
        CompoundTag v = d.ekVeri(VERI);
        DenizSeviyesi s = d.deniz();
        float buhar = d.buharlasmaDerinligi();
        if (buhar > 0) {
            if (!v.getBoolean("bitti")) {
                v.putBoolean("bitti", true);
                v.putFloat("buharTaban", s.mevcutArtis());
                v.remove("bekleme");
                d.ekVeriDegisti(VERI);
            }
            if (s.mevcutArtis() > 0 || s.hedefArtis() > 0) {
                d.denizAyarla(new DenizSeviyesi(0f, DenizAdimi.buharlasmaSonrasi(s.mevcutArtis(), v.getFloat("buharTaban"), buhar),
                        s.baslangicSaati(), s.kalici()));
            }
            return;
        }
        int adim = y.zamanAdimi();
        if (v.contains("bekleme")) {
            if (y.surecDurdu(this) || adim <= 0) {
                return;
            }
            long kalan = v.getLong("bekleme") - adim;
            if (kalan > 0) {
                v.putLong("bekleme", kalan);
                if (kalan / 20 != (kalan + adim) / 20) {
                    d.ekVeriDegisti(VERI);
                }
            } else {
                baslat(y, d);
            }
            return;
        }
        if (s.hedefArtis() != s.mevcutArtis() && v.getLong("dongu") != s.baslangicSaati()) {
            v.putLong("dongu", s.baslangicSaati());
            d.ekVeriDegisti(VERI);
            y.donemSureciBasladi(this);
        }
        if (y.surecDurdu(this) || adim <= 0) {
            return;
        }
        SuAyarlari.Sunucu a = SuAyarlari.sunucu();
        if (s.mevcutArtis() != s.hedefArtis()) {
            double hiz = v.getDouble("hiz");
            if (!(hiz > 0)) {
                hiz = Math.abs(s.hedefArtis() - s.mevcutArtis()) / Math.max(1, DenizAdimi.dakikaTick(a.denizYukselmeDk.get()));
            }
            float yeni = DenizAdimi.yaklas(s.mevcutArtis(), s.hedefArtis(), hiz, adim);
            d.denizAyarla(new DenizSeviyesi(s.hedefArtis(), yeni, s.baslangicSaati(), s.kalici()));
            if (yeni == s.hedefArtis() && yeni > 0 && !s.kalici()) {
                v.putLong("zirve", DenizAdimi.dakikaTick(a.denizGeriDonusBeklemeDk.get()));
                d.ekVeriDegisti(VERI);
            }
        } else if (!s.kalici() && s.mevcutArtis() > 0) {
            long kalan = v.getLong("zirve") - adim;
            v.putLong("zirve", kalan);
            if (kalan <= 0) {
                v.remove("zirve");
                v.putDouble("hiz", s.mevcutArtis() / (double) Math.max(1, DenizAdimi.dakikaTick(a.denizGeriDonusDk.get())));
                d.denizAyarla(new DenizSeviyesi(0f, s.mevcutArtis(), d.saat(), false));
                d.ekVeriDegisti(VERI);
            }
        }
    }

    private void baslat(AfetYoneticisi y, DunyaDonemi d) {
        SuAyarlari.Sunucu a = SuAyarlari.sunucu();
        CompoundTag v = d.ekVeri(VERI);
        float hedef = DenizAdimi.hedef(a.denizHedefArtis.get().floatValue(), a.denizUstSinir.get());
        long saat = d.saat();
        float mevcut = d.deniz().mevcutArtis();
        v.remove("bekleme");
        v.putDouble("hiz", Math.abs(hedef - mevcut) / Math.max(1, DenizAdimi.dakikaTick(a.denizYukselmeDk.get())));
        v.putLong("dongu", saat);
        d.denizAyarla(new DenizSeviyesi(hedef, mevcut, saat, a.denizKalici.get()));
        d.ekVeriDegisti(VERI);
        y.donemSureciBasladi(this);
        DogalAfetler.LOGGER.info("Sanal su: deniz yükselmesi başladı, hedef +{} blok, {} dk", hedef, a.denizYukselmeDk.get());
    }

    /**
     * N10: buharlaşma başladığında sanal yükselmenin miktarı (blok). Buharlaşma derinliği önce bunu yer; gerçek su ancak
     * {@code max(0, derinlik - bu değer)} kadar kurumalıdır (donma işçisinin okuması). Buharlaşma başlamadıysa 0.
     */
    public static float buharlasmaPayi(MinecraftServer server) {
        return DunyaDonemi.al(server).ekVeri(VERI).getFloat("buharTaban");
    }

    /** {@code /dogalafet aysonrasi atla} (N18): ay yoksa bekleme beklenmeden yükselme hemen başlar. */
    public AtlaSonucu atla(MinecraftServer server) {
        AfetYoneticisi y = AfetYoneticisi.al(server);
        DunyaDonemi d = y.donem();
        CompoundTag v = d.ekVeri(VERI);
        if (d.ayVar()) {
            return AtlaSonucu.AY_VAR;
        }
        if (v.getBoolean("bitti")) {
            return AtlaSonucu.BITTI;
        }
        if (!v.contains("bekleme") && d.deniz().hedefArtis() > 0) {
            return AtlaSonucu.ZATEN;
        }
        baslat(y, d);
        return AtlaSonucu.BASLADI;
    }

    /** {@code /dogalafet denizseviyesi}: artışı hemen ya da (kademeli) yükselme süresiyle ayarlar; üst sınıra kırpılır. */
    public float ayarla(MinecraftServer server, float deger, boolean kademeli) {
        AfetYoneticisi y = AfetYoneticisi.al(server);
        DunyaDonemi d = y.donem();
        SuAyarlari.Sunucu a = SuAyarlari.sunucu();
        float hedef = DenizAdimi.hedef(deger, a.denizUstSinir.get());
        DenizSeviyesi s = d.deniz();
        if (kademeli) {
            d.ekVeri(VERI).putDouble("hiz", Math.abs(hedef - s.mevcutArtis())
                    / Math.max(1, DenizAdimi.dakikaTick(a.denizYukselmeDk.get())));
            d.ekVeriDegisti(VERI);
            d.denizAyarla(new DenizSeviyesi(hedef, s.mevcutArtis(), d.saat(), a.denizKalici.get()));
        } else {
            d.denizAyarla(new DenizSeviyesi(hedef, hedef, d.saat(), s.kalici()));
        }
        return hedef;
    }

    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        MinecraftServer server = ustDunya.getServer();
        AfetYoneticisi y = AfetYoneticisi.al(server);
        DunyaDonemi d = y.donem();
        CompoundTag v = d.ekVeri(VERI);
        DenizSeviyesi s = d.deniz();
        if (v.contains("bekleme")) {
            baslat(y, d);
            return Optional.of(Component.translatable(SuKomutlari.ASAMA_BASLADI));
        }
        if (s.mevcutArtis() != s.hedefArtis()) {
            d.denizAyarla(new DenizSeviyesi(s.hedefArtis(), s.hedefArtis(), s.baslangicSaati(), s.kalici()));
            return Optional.of(Component.translatable(SuKomutlari.ASAMA_HEDEF, SuKomutlari.sayi(s.hedefArtis())));
        }
        if (!s.kalici() && s.mevcutArtis() > 0) {
            v.putLong("zirve", 0);
            d.ekVeriDegisti(VERI);
            return Optional.of(Component.translatable(SuKomutlari.ASAMA_GERI));
        }
        return Optional.empty();
    }
}

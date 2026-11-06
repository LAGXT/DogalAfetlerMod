package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Mağaraya dolan suyun yaşamı (bölüm B; sözleşme §3a): dolum, kalış, kuruma. Mağara zaman çizelgesi deterministik
 * olduğu için sürecin tick'i yoktur. {@code stop}: bütün mağara suyu hemen kalkar (geçici etki; dünya zaten
 * değişmedi). {@code tsunamiAfeti} kuralı kapanınca su o anki seviyesinde donar, açılınca kaldığı yerden sürer.
 * {@code asama ileri}: her cep bir sonraki aşamaya (dolu → kuruma → bitti) geçer.
 */
public final class MagaraSureci implements DonemSureci {
    public static final String KIMLIK = "magara_suyu";
    static final MagaraSureci ORNEK = new MagaraSureci();

    private MagaraSureci() {
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
        return Optional.of(AfetTuru.TSUNAMI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        return SanalSu.BOYUTLAR.values().stream().anyMatch(b -> !b.dunya.magaralar().isEmpty());
    }

    @Override
    public void durdur(ServerLevel ustDunya, DurdurmaNedeni neden) {
        for (ServerLevel l : ustDunya.getServer().getAllLevels()) {
            SanalSu.Boyut b = SanalSu.BOYUTLAR.get(l.dimension());
            if (b == null || b.dunya.magaralar().isEmpty()) {
                continue;
            }
            for (int id : List.copyOf(b.dunya.magaralar().keySet())) {
                if (neden == DurdurmaNedeni.KOMUT) {
                    b.magaraKutusu.remove(id);
                    b.dunya.magaraKaldir(id);
                } else {
                    b.dunya.magaraDondur(id, SanalSu.sunucuSaati);
                }
            }
            SanalSu.yayinla(l);
        }
    }

    @Override
    public void devamEt(ServerLevel ustDunya) {
        for (ServerLevel l : ustDunya.getServer().getAllLevels()) {
            SanalSu.Boyut b = SanalSu.BOYUTLAR.get(l.dimension());
            if (b != null && !b.dunya.magaralar().isEmpty()) {
                for (int id : List.copyOf(b.dunya.magaralar().keySet())) {
                    b.dunya.magaraCoz(id, SanalSu.sunucuSaati);
                }
                SanalSu.yayinla(l);
            }
        }
    }

    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        int n = 0;
        for (ServerLevel l : ustDunya.getServer().getAllLevels()) {
            SanalSu.Boyut b = SanalSu.BOYUTLAR.get(l.dimension());
            if (b == null || b.dunya.magaralar().isEmpty()) {
                continue;
            }
            for (Map.Entry<Integer, SanalSuDunyasi.Magara> e : List.copyOf(b.dunya.magaralar().entrySet())) {
                SanalSuDunyasi.Magara m = e.getValue();
                double t = Double.isNaN(m.donma()) ? SanalSu.sunucuSaati : m.donma();
                b.dunya.magaraKoy(e.getKey(), new SanalSuDunyasi.Magara(m.cep(), m.zaman().ileri(t), m.donma()));
                n++;
            }
            SanalSu.yayinla(l);
        }
        return n == 0 ? Optional.empty() : Optional.of(Component.translatable(SuKomutlari.ASAMA_MAGARA, n));
    }
}

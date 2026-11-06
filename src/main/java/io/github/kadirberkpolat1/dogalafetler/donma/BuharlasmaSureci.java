package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.BuharlasmaHedefi;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Güneş patlamasındaki buharlaşma dönem süreci ({@code buharlasma}, N9; çekirdek §3a): güneş büyürken (BUYUYEN) global
 * buharlaşma derinliği {@code donma.buharlasmaSuresi} boyunca {@code donma.buharlasmaEnCok}'a çıkar. Afet sürerken de her
 * ilerlemeden önce {@code surecDurdu} sorulur (gamerule ile durur, açılınca sürer). Evre BUYUYEN'den çıkınca (süpernova ya da
 * durdurulan güneş patlaması) {@link DonmaSistemi} derinliği 0'a çeker ve chunk'lar suyu geri doldurur; donma bu chunk'ta
 * geri dolmadan başlamaz (N10). Tek döngülüdür: evre bitince süreç tamamlanır, durdurma kaydı çekirdekçe silinir. Ayrık
 * aşaması yoktur: {@code asama ileri} bu süreci geçer (K-D31); test aracı {@code /dogalafet buharlasma <derinlik>}.
 */
final class BuharlasmaSureci implements DonemSureci {
    static final String KIMLIK = "buharlasma";

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Component ad() {
        return Component.translatable(DonmaCevirileri.SUREC_BUHARLASMA);
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.GUNES_PATLAMASI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        DunyaDonemi d = DunyaDonemi.al(ustDunya);
        return d.gunesEvresi() == GunesEvresi.BUYUYEN && d.buharlasmaDerinligi() < DonmaAyarlari.buharlasmaEnCok() - 1e-3;
    }

    void tick(MinecraftServer server) {
        AfetYoneticisi y = AfetYoneticisi.al(server);
        if (!aktifMi(server.overworld()) || y.surecDurdu(this) || y.zamanAdimi() <= 0) {
            return;
        }
        DunyaDonemi d = y.donem();
        d.buharlasmaDerinligiAyarla(BuharlasmaHedefi.derinlikAdimi(d.buharlasmaDerinligi(), y.zamanAdimi(),
                DonmaAyarlari.buharlasmaSuresiGun(), DonmaAyarlari.buharlasmaEnCok()));
    }
}

package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Eşzamanlı doğal afet kuralları (K-E67, kullanıcı isteği "gün geçtikçe sayıları artsın"): oyun gününe göre en çok
 * {@link ZamanlayiciAyarlari#eszamanliSinir} doğal afet aynı anda sürer, hepsi farklı türden. Güneş patlaması ve ay çarpması
 * her zaman tek başınadır: doğal olanı sürerken doğal afet başlamaz, başka afet sürerken onlar seçilmez. Yuvayı yalnız doğal
 * afetler doldurur: komutla başlatılan afet (uzun güneş patlaması dahil) doğal takvimi durdurmaz; komutla başlatma da bu
 * kurallara bağlı değildir (AS-8, K-E62). TPS kısılıyken (K-C37) herhangi bir afet sürerken yenisi ertelenir. Etki alanları
 * çakışan afet başlamaz ({@code EtkiAlani.kesisir}, yönetici). Saf Java.
 */
public final class EszamanliAfetler {
    private EszamanliAfetler() {
    }

    /**
     * Yeni bir doğal afet için yuva yok mu. {@code dogalAktifler}: süren doğal afetlerin türleri (ön-uyarı dahil);
     * {@code toplamAktif}: komutla başlatılanlar dahil süren afet sayısı.
     */
    public static boolean yuvaDolu(Collection<AfetTuru> dogalAktifler, int toplamAktif, int sinir, boolean tpsKisik) {
        if (toplamAktif <= 0) {
            return false;
        }
        if (tpsKisik) {
            return true;
        }
        for (AfetTuru tur : dogalAktifler) {
            if (tur.sunucuGeneli()) {
                return true;
            }
        }
        return dogalAktifler.size() >= Math.max(1, sinir);
    }

    /** Doğal seçimin aday türleri: süren türler dışta; bir afet sürerken sunucu geneli türler de dışta. */
    public static Set<AfetTuru> adaylar(Set<AfetTuru> uygun, Collection<AfetTuru> aktifler) {
        Set<AfetTuru> sonuc = EnumSet.noneOf(AfetTuru.class);
        sonuc.addAll(uygun);
        sonuc.removeAll(aktifler);
        if (!aktifler.isEmpty()) {
            sonuc.removeIf(AfetTuru::sunucuGeneli);
        }
        return sonuc;
    }
}

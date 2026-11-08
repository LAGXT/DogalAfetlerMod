package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Eşzamanlı doğal afet kuralları (K-E67, kullanıcı isteği "gün geçtikçe sayıları artsın"): oyun gününe göre en çok
 * {@link ZamanlayiciAyarlari#eszamanliSinir} doğal afet aynı anda sürer, hepsi farklı türden. Güneş patlaması ve ay çarpması
 * her zaman tek başınadır: biri sürerken doğal afet başlamaz, başka afet sürerken onlar seçilmez. TPS kısılıyken (K-C37)
 * ikinci ve üçüncü afet ertelenir. Komutla başlatma bu kurallara bağlı değildir (AS-8, K-E62); süren komut afetleri yuvayı
 * yine doldurur. Etki alanları çakışan afet başlamaz ({@code EtkiAlani.kesisir}, yönetici). Saf Java.
 */
public final class EszamanliAfetler {
    private EszamanliAfetler() {
    }

    /** Yeni bir doğal afet için yuva yok mu. {@code aktifler}: süren afetlerin türleri (ön-uyarı dahil). */
    public static boolean yuvaDolu(Collection<AfetTuru> aktifler, int sinir, boolean tpsKisik) {
        if (aktifler.isEmpty()) {
            return false;
        }
        if (tpsKisik) {
            return true;
        }
        for (AfetTuru tur : aktifler) {
            if (tur.sunucuGeneli()) {
                return true;
            }
        }
        return aktifler.size() >= Math.max(1, sinir);
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

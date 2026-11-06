package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * İpuçlarının kaynağı (A-31, A-35): afet türü bir ipucunu yayınladığını kaydetmişse ({@link #kaydet}, Faz 4 afet işçisi mod
 * kurucusunda) o türde ipucunun tek kaynağı afetin olayıdır ve başarımın yedek denetimi hiç çalışmaz; kaydetmemişse tek
 * kaynak yedek denetimdir ve o türden gelen olay yok sayılır ({@link #olayGeldi}). Böylece sonuç başka oyuncuların
 * olaylarına bağlı değildir; kaydı unutulan yayıncı ilk olayında bir kez uyarı alır. Thread-safe (mod kurucuları paralel
 * koşar).
 */
public final class IpucuKaynaklari {
    /** Gelen ipucu olayına ne yapılacağı. */
    public enum OlayKarari {
        /** Tür bu ipucunun kayıtlı yayıncısı: olay ize yazılır. */
        KABUL,
        /** Kayıtsız kaynak, bu tür ve ipucu için ilk kez: uyarı yazılır, olay yok sayılır. */
        UYAR_YOK_SAY,
        /** Kayıtsız kaynak, daha önce uyarıldı: olay sessizce yok sayılır. */
        YOK_SAY
    }

    private final Map<AfetTuru, Integer> yayincilar = new EnumMap<>(AfetTuru.class);
    private final Map<AfetTuru, Integer> uyarilanlar = new EnumMap<>(AfetTuru.class);

    /** Afet türü bu ipucunu kendisi yayınlar; yeni kayıtsa true. */
    public synchronized boolean kaydet(AfetTuru tur, Ipucu ipucu) {
        int eski = yayincilar.getOrDefault(tur, 0);
        yayincilar.put(tur, eski | ipucu.bit());
        return (eski & ipucu.bit()) == 0;
    }

    /** Kaydı kaldırır; kayıtlıydıysa true. */
    public synchronized boolean kaldir(AfetTuru tur, Ipucu ipucu) {
        int eski = yayincilar.getOrDefault(tur, 0);
        yayincilar.put(tur, eski & ~ipucu.bit());
        return (eski & ipucu.bit()) != 0;
    }

    public synchronized boolean yayinliyor(AfetTuru tur, Ipucu ipucu) {
        return (yayincilar.getOrDefault(tur, 0) & ipucu.bit()) != 0;
    }

    /** Bu türde tek kaynağı olay olan ipuçları: türün kaydı. */
    public synchronized Set<Ipucu> olayli(AfetTuru tur) {
        return Ipucu.bitlerden(yayincilar.getOrDefault(tur, 0));
    }

    /** Süren {@code tur} afetinde {@code ipucu} olayı geldi: kayıtlı yayıncıdan değilse yok sayılır. */
    public synchronized OlayKarari olayGeldi(AfetTuru tur, Ipucu ipucu) {
        if (yayinliyor(tur, ipucu)) {
            return OlayKarari.KABUL;
        }
        int uyarilan = uyarilanlar.getOrDefault(tur, 0);
        if ((uyarilan & ipucu.bit()) != 0) {
            return OlayKarari.YOK_SAY;
        }
        uyarilanlar.put(tur, uyarilan | ipucu.bit());
        return OlayKarari.UYAR_YOK_SAY;
    }
}

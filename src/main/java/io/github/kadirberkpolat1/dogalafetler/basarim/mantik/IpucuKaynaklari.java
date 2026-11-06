package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * İpuçlarının kaynağı (A-31): bir ipucunun tek kaynağı afetin olayıdır, başarımın yedek denetimi yalnız olay hiç
 * gelmediğinde sayılır. Olay kaynaktır, (a) afet türü o ipucunu yayınladığını kaydetmişse ({@link #kaydet}, Faz 4 afet işçisi
 * mod kurucusunda; kayıtlı türde yedek denetim hiç çalışmaz) ya da (b) o afet sürerken herhangi bir oyuncu için o türden
 * olay geldiyse. Thread-safe (mod kurucuları paralel koşar).
 */
public final class IpucuKaynaklari {
    private final Map<AfetTuru, Integer> yayincilar = new EnumMap<>(AfetTuru.class);

    /** Afet türü bu ipucunu kendisi yayınlar; yeni kayıtsa true. */
    public synchronized boolean kaydet(AfetTuru tur, Ipucu ipucu) {
        int eski = yayincilar.getOrDefault(tur, 0);
        yayincilar.put(tur, eski | ipucu.bit());
        return (eski & ipucu.bit()) == 0;
    }

    public synchronized void kaldir(AfetTuru tur, Ipucu ipucu) {
        yayincilar.computeIfPresent(tur, (k, bitler) -> bitler & ~ipucu.bit());
    }

    public synchronized boolean yayinliyor(AfetTuru tur, Ipucu ipucu) {
        return (yayincilar.getOrDefault(tur, 0) & ipucu.bit()) != 0;
    }

    /** Bu afette tek kaynağı olay olan ipuçları: türün kaydı ∪ bu afette olayı gelmiş ipuçları ({@code gelenBitler}). */
    public synchronized Set<Ipucu> olayli(AfetTuru tur, int gelenBitler) {
        return Ipucu.bitlerden(yayincilar.getOrDefault(tur, 0) | gelenBitler);
    }
}

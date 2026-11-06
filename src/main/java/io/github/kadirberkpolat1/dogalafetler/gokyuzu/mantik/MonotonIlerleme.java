package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import java.util.Objects;

/**
 * Sunucudan saniyede bir gelen evre ilerlemesini karede yumuşatır: gösterilen değer ~1 sn zaman sabitiyle hedefe yaklaşır
 * ve aynı evre içinde geri gitmez (paket gecikmesi ya da saat düzeltmesi görüntüyü geri sardırmaz). Anahtar (evre ve
 * başlangıcı) değişince ilk hedef doğrudan alınır; sonradan giren oyuncu da aynı kareyi görür.
 */
public final class MonotonIlerleme {
    private static final float ZAMAN_SABITI_TICK = 20f;

    private Object anahtar;
    private float gosterilen;

    /** {@code gecenTick}: son çağrıdan beri geçen istemci tick'i (kare arası kesirli olabilir). */
    public float guncelle(Object yeniAnahtar, float hedef, float gecenTick) {
        if (!Objects.equals(anahtar, yeniAnahtar)) {
            anahtar = yeniAnahtar;
            gosterilen = hedef;
        } else if (hedef > gosterilen) {
            float oran = 1f - (float) Math.exp(-Math.max(0f, gecenTick) / ZAMAN_SABITI_TICK);
            gosterilen += (hedef - gosterilen) * oran;
            if (hedef - gosterilen < 1e-4f) {
                gosterilen = hedef;
            }
        }
        return gosterilen;
    }
}

package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/** Hava Radarı'nın bir afeti nasıl gösterdiği (madde 13a, N7). */
public enum RadarGorunumu {
    /** Gösterilecek afet yok (ya da gizli, menzil dışı, başka boyutta): radar "sakin" ekranını çizer. */
    GIZLI,
    /** Bölgesel afet: ad, yön oku, mesafe, süre. */
    AFET,
    /** Güneş patlaması / ay çarpması sirenden sonra: "gökyüzü anomalisi" + geri sayım, yön ve mesafe yok. */
    ANOMALI
}

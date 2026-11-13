package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

/**
 * Madde 12'deki üç başarım türü ve vanilla çerçeve adı. Varsayılan XP advancement JSON'una ({@code rewards.experience})
 * yazılır; config'teki değer bundan farklıysa fark kazanılırken eklenir (A-05). Ödüller oyunu bozmayacak kadar küçüktür:
 * 100 XP yaklaşık 7 seviyedir.
 */
public enum BasarimTuru {
    GOREV("task", 15),
    HEDEF("goal", 40),
    MEYDAN_OKUMA("challenge", 100);

    private final String cerceve;
    private final int varsayilanXp;

    BasarimTuru(String cerceve, int varsayilanXp) {
        this.cerceve = cerceve;
        this.varsayilanXp = varsayilanXp;
    }

    /** Vanilla {@code FrameType} adı ({@code task}, {@code goal}, {@code challenge}). */
    public String cerceve() {
        return cerceve;
    }

    public int varsayilanXp() {
        return varsayilanXp;
    }
}

package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Vanilla pişmiş yiyeceklerin çok küçük ve kısa ısınması (madde 14 son kural, config ile kapatılabilir). Config satırı
 * {@code eşya=saniye}; miktar {@link Buyukluk#COK_KUCUK} kademesidir. Başka modların yiyecekleri de eklenebilir.
 */
public final class VanillaIsinma {
    public static final int EN_UZUN_SANIYE = 600;
    private static final Pattern KIMLIK = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** Varsayılan liste: sıcak pişmiş yiyecekler (et/balık/patates 30 sn, çorbalar 45 sn). */
    public static final List<String> VARSAYILAN = List.of("minecraft:baked_potato=30", "minecraft:cooked_beef=30",
            "minecraft:cooked_porkchop=30", "minecraft:cooked_chicken=30", "minecraft:cooked_mutton=30",
            "minecraft:cooked_rabbit=30", "minecraft:cooked_cod=30", "minecraft:cooked_salmon=30",
            "minecraft:mushroom_stew=45", "minecraft:rabbit_stew=45", "minecraft:beetroot_soup=45");

    public record Girdi(String esya, int sureSaniye) {
    }

    private VanillaIsinma() {
    }

    public static boolean gecerliMi(Object deger) {
        if (!(deger instanceof String satir)) {
            return false;
        }
        try {
            coz(satir);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Girdi coz(String satir) {
        String[] parca = satir.split("=", -1);
        if (parca.length != 2) {
            throw new IllegalArgumentException("biçim eşya=saniye olmalı: " + satir);
        }
        String esya = parca[0].trim();
        if (!KIMLIK.matcher(esya).matches()) {
            throw new IllegalArgumentException("geçersiz eşya kimliği: " + esya);
        }
        int saniye;
        try {
            saniye = Integer.parseInt(parca[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("süre tamsayı olmalı: " + satir, e);
        }
        if (saniye < 1 || saniye > EN_UZUN_SANIYE) {
            throw new IllegalArgumentException("süre 1–" + EN_UZUN_SANIYE + " sn olmalı: " + satir);
        }
        return new Girdi(esya, saniye);
    }

    public static YiyecekTanimi tanim(Girdi girdi) {
        return new YiyecekTanimi(girdi.esya(), Yon.ISINMA, Buyukluk.COK_KUCUK, girdi.sureSaniye(), false);
    }
}

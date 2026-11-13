package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Vanilla (ya da başka modların) yiyecek ve içeceklerinin kademeli sıcaklık etkisi (madde 14 son kural, S3-06; config ile
 * kapatılabilir). Config satırı {@code eşya=kademe:saniye}; kademe {@link Buyukluk#anahtar()} ({@code cok_kucuk} …
 * {@code cok_buyuk}), miktar kademenin config değeridir. Eski {@code eşya=saniye} satırı {@link Buyukluk#COK_KUCUK} sayılır.
 * Isınma ve serinlik ayrı listelerdir; yön listeden gelir. {@value #SU_SISESI} aynı kimliği bütün iksirlerle paylaştığı
 * için yalnız su şişesinde etki verir (denetim sunucu tarafında, eşya NBT'siyle).
 */
public final class VanillaSicaklik {
    public static final int EN_UZUN_SANIYE = 600;
    public static final String SU_SISESI = "minecraft:potion";
    private static final Pattern KIMLIK = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** Ayar sürümü 0'ın ısınma listesi (eski biçim); göç yalnız bu listeye birebir eşit dosyayı yeniler. */
    public static final List<String> ESKI_ISINMA = List.of("minecraft:baked_potato=30", "minecraft:cooked_beef=30",
            "minecraft:cooked_porkchop=30", "minecraft:cooked_chicken=30", "minecraft:cooked_mutton=30",
            "minecraft:cooked_rabbit=30", "minecraft:cooked_cod=30", "minecraft:cooked_salmon=30",
            "minecraft:mushroom_stew=45", "minecraft:rabbit_stew=45", "minecraft:beetroot_soup=45");

    /**
     * Sıcak servis edilenler: fırın ürünleri ve pişmiş etler çok küçük ve kısa; balık eti çabuk soğur, biraz daha kısa;
     * çorbalar sıvı ve sıcak olduğu için bir kademe güçlü ve uzun, et ve sebzeyle koyu tavşan yahnisi en uzun.
     */
    public static final List<String> VARSAYILAN_ISINMA = List.of("minecraft:baked_potato=cok_kucuk:30",
            "minecraft:pumpkin_pie=cok_kucuk:30", "minecraft:cooked_beef=cok_kucuk:45",
            "minecraft:cooked_porkchop=cok_kucuk:45", "minecraft:cooked_chicken=cok_kucuk:45",
            "minecraft:cooked_mutton=cok_kucuk:45", "minecraft:cooked_rabbit=cok_kucuk:45",
            "minecraft:cooked_cod=cok_kucuk:40", "minecraft:cooked_salmon=cok_kucuk:40",
            "minecraft:mushroom_stew=kucuk:60", "minecraft:beetroot_soup=kucuk:60",
            "minecraft:suspicious_stew=kucuk:60", "minecraft:rabbit_stew=kucuk:90");

    /**
     * Serin olanlar: sulu meyveler ve soğuk sudan çıkan çiğ balıklar çok küçük ve kısa; kovadan soğuk süt ve su şişesi
     * içecek olduğu için bir kademe güçlü.
     */
    public static final List<String> VARSAYILAN_SERINLIK = List.of("minecraft:melon_slice=cok_kucuk:30",
            "minecraft:apple=cok_kucuk:20", "minecraft:sweet_berries=cok_kucuk:20", "minecraft:glow_berries=cok_kucuk:20",
            "minecraft:cod=cok_kucuk:30", "minecraft:salmon=cok_kucuk:30", "minecraft:tropical_fish=cok_kucuk:30",
            "minecraft:pufferfish=cok_kucuk:20", "minecraft:milk_bucket=kucuk:45", SU_SISESI + "=kucuk:30");

    public record Girdi(String esya, Buyukluk buyukluk, int sureSaniye) {
    }

    private VanillaSicaklik() {
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
            throw new IllegalArgumentException("biçim eşya=kademe:saniye olmalı: " + satir);
        }
        String esya = parca[0].trim();
        if (!KIMLIK.matcher(esya).matches()) {
            throw new IllegalArgumentException("geçersiz eşya kimliği: " + esya);
        }
        String[] deger = parca[1].split(":", -1);
        if (deger.length > 2) {
            throw new IllegalArgumentException("biçim eşya=kademe:saniye olmalı: " + satir);
        }
        Buyukluk buyukluk = deger.length == 2 ? kademe(deger[0].trim(), satir) : Buyukluk.COK_KUCUK;
        String sure = deger[deger.length - 1].trim();
        int saniye;
        try {
            saniye = Integer.parseInt(sure);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("süre tamsayı olmalı: " + satir, e);
        }
        if (saniye < 1 || saniye > EN_UZUN_SANIYE) {
            throw new IllegalArgumentException("süre 1–" + EN_UZUN_SANIYE + " sn olmalı: " + satir);
        }
        return new Girdi(esya, buyukluk, saniye);
    }

    private static Buyukluk kademe(String anahtar, String satir) {
        for (Buyukluk b : Buyukluk.values()) {
            if (b.anahtar().equals(anahtar)) {
                return b;
            }
        }
        throw new IllegalArgumentException("geçersiz kademe (cok_kucuk, kucuk, orta, buyuk, cok_buyuk): " + satir);
    }

    public static YiyecekTanimi tanim(Girdi girdi, Yon yon) {
        return new YiyecekTanimi(girdi.esya(), yon, girdi.buyukluk(), girdi.sureSaniye(), false);
    }
}

package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import java.util.Arrays;

/**
 * Su sütunlarının kıyıya uzaklığı (tasarım notu §2 "Kenardan içeri"): bölgenin yüzey türleri üzerinde çok kaynaklı
 * BFS. Izgara bölgenin çevresinde 1 hücrelik halka içerir: {@code (genislik+2) × (derinlik+2)}, satır z, sütun x.
 * Halka yalnız kaynak bulmak için okunur; bilinmeyen (yüklü olmayan komşu chunk) hücre su sayılır.
 */
public final class KiyiUzakligi {
    public static final byte SU = 0;
    public static final byte KARA = 1;
    public static final byte BILINMIYOR = 2;

    private KiyiUzakligi() {
    }

    /** Su hücreleri için kıyıya uzaklık (0 = karaya komşu, kıyı görülmeyen = sınır), diğerleri -1. */
    public static int[] hesapla(int genislik, int derinlik, byte[] tur, int sinir) {
        int gw = genislik + 2;
        if (tur.length != gw * (derinlik + 2)) {
            throw new IllegalArgumentException("ızgara boyutu yanlış");
        }
        int[] uzaklik = new int[genislik * derinlik];
        Arrays.fill(uzaklik, -1);
        int[] kuyruk = new int[genislik * derinlik];
        int bas = 0;
        int son = 0;
        for (int z = 0; z < derinlik; z++) {
            for (int x = 0; x < genislik; x++) {
                int h = (z + 1) * gw + x + 1;
                if (tur[h] != SU) {
                    continue;
                }
                int i = z * genislik + x;
                if (tur[h - 1] == KARA || tur[h + 1] == KARA || tur[h - gw] == KARA || tur[h + gw] == KARA) {
                    uzaklik[i] = 0;
                    kuyruk[son++] = i;
                } else {
                    uzaklik[i] = sinir;
                }
            }
        }
        while (bas < son) {
            int i = kuyruk[bas++];
            int yeni = uzaklik[i] + 1;
            if (yeni >= sinir) {
                continue;
            }
            int x = i % genislik;
            int z = i / genislik;
            son = komsu(uzaklik, kuyruk, son, x - 1, z, genislik, derinlik, yeni);
            son = komsu(uzaklik, kuyruk, son, x + 1, z, genislik, derinlik, yeni);
            son = komsu(uzaklik, kuyruk, son, x, z - 1, genislik, derinlik, yeni);
            son = komsu(uzaklik, kuyruk, son, x, z + 1, genislik, derinlik, yeni);
        }
        return uzaklik;
    }

    private static int komsu(int[] uzaklik, int[] kuyruk, int son, int x, int z, int genislik, int derinlik, int yeni) {
        if (x < 0 || z < 0 || x >= genislik || z >= derinlik) {
            return son;
        }
        int j = z * genislik + x;
        if (uzaklik[j] > yeni) {
            uzaklik[j] = yeni;
            kuyruk[son++] = j;
        }
        return son;
    }
}

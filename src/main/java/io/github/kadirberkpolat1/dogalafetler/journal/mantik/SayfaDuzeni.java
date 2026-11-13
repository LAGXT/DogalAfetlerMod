package io.github.kadirberkpolat1.dogalafetler.journal.mantik;

/**
 * Defter satırlarını sayfalara böler (saf Java; ekran bir kez, açılışta çağırır). Satırlar sırayla dizilir; aynı {@code grup}
 * numarasını taşıyan ardışık satırlar (bir başlık ve altındaki satırlar) kırılmaz, sığmıyorsa tümü sonraki sayfaya geçer;
 * tek başına bir sayfaya sığmayan grup satır satır bölünür. {@link #SAYFA_SONU} yükseklikli satır yeni sayfa başlatır.
 */
public final class SayfaDuzeni {
    /** Yüksekliği bu değerde olan satır yer kaplamaz; sonraki satır yeni sayfanın başında başlar (boş sayfa açmaz). */
    public static final int SAYFA_SONU = -1;
    /** Grupsuz (tek başına kırılabilir) satır. */
    public static final int GRUPSUZ = -1;

    private SayfaDuzeni() {
    }

    /**
     * @param yukseklik her satırın piksel yüksekliği ({@link #SAYFA_SONU} ya da ≥ 0)
     * @param grup      satırın grubu ({@link #GRUPSUZ} ya da ≥ 0); ardışık eşit numaralar bir grubu oluşturur
     * @param kapasite  bir sayfanın piksel yüksekliği (&gt; 0)
     * @return her satırın sayfa numarası (0'dan); satır yoksa boş dizi
     */
    public static int[] sayfala(int[] yukseklik, int[] grup, int kapasite) {
        if (kapasite <= 0) {
            throw new IllegalArgumentException("kapasite > 0 olmalı: " + kapasite);
        }
        if (yukseklik.length != grup.length) {
            throw new IllegalArgumentException("yukseklik ve grup aynı uzunlukta olmalı");
        }
        int n = yukseklik.length;
        int[] sayfa = new int[n];
        int no = 0;
        int dolu = 0;
        int i = 0;
        while (i < n) {
            if (yukseklik[i] == SAYFA_SONU) {
                if (dolu > 0) {
                    no++;
                    dolu = 0;
                }
                sayfa[i] = no;
                i++;
                continue;
            }
            int son = i + 1;
            int toplam = yukseklik[i];
            if (grup[i] != GRUPSUZ) {
                while (son < n && grup[son] == grup[i] && yukseklik[son] != SAYFA_SONU) {
                    toplam += yukseklik[son];
                    son++;
                }
            }
            if (dolu > 0 && dolu + toplam > kapasite) {
                no++;
                dolu = 0;
            }
            if (toplam <= kapasite) {
                for (int k = i; k < son; k++) {
                    sayfa[k] = no;
                }
                dolu += toplam;
            } else {
                for (int k = i; k < son; k++) {
                    if (dolu > 0 && dolu + yukseklik[k] > kapasite) {
                        no++;
                        dolu = 0;
                    }
                    sayfa[k] = no;
                    dolu += yukseklik[k];
                }
            }
            i = son;
        }
        return sayfa;
    }

    /** Sayfa sayısı (en az 1: içerik boş olsa da defter bir boş sayfa gösterir). */
    public static int sayfaSayisi(int[] sayfaNo) {
        return sayfaNo.length == 0 ? 1 : sayfaNo[sayfaNo.length - 1] + 1;
    }
}

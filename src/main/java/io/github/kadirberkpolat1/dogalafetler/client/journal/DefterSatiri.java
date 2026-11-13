package io.github.kadirberkpolat1.dogalafetler.client.journal;

import io.github.kadirberkpolat1.dogalafetler.journal.DefterCevirileri;
import io.github.kadirberkpolat1.dogalafetler.journal.mantik.SayfaDuzeni;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Defter sayfalarının satır türleri. Her satır kendi piksel yüksekliğini ve grubunu ({@link SayfaDuzeni}) söyler ve bir
 * sayfa sütununda çizilir; satırlar defter açılırken bir kez kurulur, kare başına yalnız çizilir (düzen hesabı ve metin
 * kırma yok).
 */
final class DefterSatiri {
    static final int METIN = 0xFF3A2A1A;
    static final int BASLIK = 0xFF7B2D14;
    static final int ALT_BASLIK = 0xFF5B4126;
    static final int DEGER = 0xFF1B4B73;
    static final int SOLUK = 0xFF7A6A55;
    static final int CIZGI = 0xFFB89F78;

    static final int BASLIK_YUKSEKLIGI = 12;
    static final int ALT_BASLIK_YUKSEKLIGI = 11;
    static final int SATIR_YUKSEKLIGI = 10;
    static final int ESYA_YUKSEKLIGI = 18;

    private DefterSatiri() {
    }

    /** Sayfa sütununda çizilen bir satır. */
    interface Satir {
        int yukseklik();

        int grup();

        void ciz(GuiGraphics g, Font font, int x, int y, int genislik);
    }

    /** Bölüm başlığı: kahverengi, altı çizgili. */
    record Baslik(Component metin, int grup) implements Satir {
        @Override
        public int yukseklik() {
            return BASLIK_YUKSEKLIGI;
        }

        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
            g.drawString(font, metin, x, y, BASLIK, false);
            g.fill(x, y + 10, x + genislik, y + 11, CIZGI);
        }
    }

    /** Küçük başlık. */
    record AltBaslik(Component metin, int grup) implements Satir {
        @Override
        public int yukseklik() {
            return ALT_BASLIK_YUKSEKLIGI;
        }

        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
            g.drawString(font, metin, x, y, ALT_BASLIK, false);
        }
    }

    /** Sütun genişliğine önceden kırılmış tek metin satırı. */
    record Metin(FormattedCharSequence satir, int renk, int grup) implements Satir {
        @Override
        public int yukseklik() {
            return SATIR_YUKSEKLIGI;
        }

        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
            g.drawString(font, satir, x, y, renk, false);
        }
    }

    /** Solda etiket, sağa yaslı değer; etiket sığmazsa kısaltılır. */
    record Deger(Component etiket, Component deger, int grup) implements Satir {
        @Override
        public int yukseklik() {
            return SATIR_YUKSEKLIGI;
        }

        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
            int degerGenisligi = font.width(deger);
            g.drawString(font, deger, x + genislik - degerGenisligi, y, DEGER, false);
            g.drawString(font, kirp(font, etiket.getString(), genislik - degerGenisligi - 6), x, y, METIN, false);
        }
    }

    /** Tutulan eşya: simge, ad ve adet. */
    record Esya(ItemStack yigin, Component ad, int adet, int grup) implements Satir {
        @Override
        public int yukseklik() {
            return ESYA_YUKSEKLIGI;
        }

        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
            g.renderItem(yigin, x, y + 1);
            Component adetMetni = Component.translatable(DefterCevirileri.ON + "adet", Integer.toString(adet));
            int adetGenisligi = font.width(adetMetni);
            g.drawString(font, adetMetni, x + genislik - adetGenisligi, y + 5, DEGER, false);
            g.drawString(font, kirp(font, ad.getString(), genislik - 20 - adetGenisligi - 4), x + 20, y + 5, METIN, false);
        }
    }

    /** Boş aralık. */
    record Bosluk(int yukseklik, int grup) implements Satir {
        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
        }
    }

    /** Sonraki satır yeni sayfada başlar. */
    record SayfaSonu() implements Satir {
        @Override
        public int yukseklik() {
            return SayfaDuzeni.SAYFA_SONU;
        }

        @Override
        public int grup() {
            return SayfaDuzeni.GRUPSUZ;
        }

        @Override
        public void ciz(GuiGraphics g, Font font, int x, int y, int genislik) {
        }
    }

    /** Metni {@code en} pikseline sığdırır; sığmazsa sonuna "..." koyar. */
    static String kirp(Font font, String metin, int en) {
        if (font.width(metin) <= en) {
            return metin;
        }
        return font.plainSubstrByWidth(metin, Math.max(0, en - font.width("..."))) + "...";
    }
}

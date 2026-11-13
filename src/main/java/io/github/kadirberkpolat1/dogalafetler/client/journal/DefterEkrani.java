package io.github.kadirberkpolat1.dogalafetler.client.journal;

import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Esya;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Satir;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterCevirileri;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi;
import io.github.kadirberkpolat1.dogalafetler.journal.mantik.SayfaDuzeni;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Afet Defteri ekranı: iki sayfalık açık kitap, üstte dört sekme (Genel, Afetler, Balık/Eşya, Rehber), altta sayfa çevirme
 * düğmeleri. Zemin ve sayfalar düz renk dolgusudur (doku yok); satırlar {@link DefterIcerigi}'nden bir kez kurulur ve
 * {@link SayfaDuzeni} ile sayfalara bölünür (açılışta ve pencere boyutu değişince). Kare başına yalnız görünen iki sayfa
 * çizilir (en fazla ~28 satır, 14 eşya simgesi). Sayfa çevirme: düğmeler, tekerlek, sol/sağ ok; sekmeler tıklanır.
 * Oyunu durdurmaz. Veri bir kez gelir; ekran açıkken sunucuya paket yollamaz.
 */
public final class DefterEkrani extends Screen {
    private static final int GENISLIK = 320;
    private static final int YUKSEKLIK = 204;
    /** Bir sayfa sütununun genişliği ve içerik yüksekliği (piksel). */
    private static final int SAYFA_G = 140;
    private static final int ICERIK_YUKSEKLIK = 148;
    private static final int ICERIK_Y = 30;
    private static final int SOL_X = 14;
    private static final int SAG_X = 166;
    private static final int SAYFA_Y0 = 27;
    private static final int SAYFA_Y1 = 180;

    private static final int SEKME_Y = 7;
    private static final int SEKME_G = 70;
    private static final int SEKME_YUKSEKLIK = 17;
    private static final int SEKME_ARA = 4;
    private static final int ALT_Y = 184;
    private static final int DUGME_G = 40;
    private static final int DUGME_YUKSEKLIK = 16;

    private static final int KAPAK = 0xFF6E4524;
    private static final int KAPAK_ACIK = 0xFF8C5E36;
    private static final int KAPAK_KOYU = 0xFF4C2E16;
    private static final int KENAR = 0xFF1E120A;
    private static final int SAYFA_KENAR = 0xFF3A2312;
    private static final int SAYFA = 0xFFEADFC0;
    private static final int SAYFA_GOLGE = 0xFFE0D2AE;
    private static final int CILT = 0xFFC4AC82;
    private static final int SEKME_PASIF = 0xFF9C7448;
    private static final int SEKME_UZERINDE = 0xFFB38655;
    private static final int SEKME_YAZI = 0xFFF2E6CC;
    private static final int ALT_YAZI = 0xFFE8D8B8;

    private static final String[] SEKME_ANAHTARI = {"sekme.genel", "sekme.afetler", "sekme.esyalar", "sekme.rehber"};

    private DefterPaketi veri;
    private int sekme;
    private final int[] acilim = new int[DefterIcerigi.SEKME_SAYISI];
    /** [sekme][sayfa][satır]. */
    private List<List<List<Satir>>> sayfalar = List.of();
    private int sol;
    private int ust;
    private Button onceki;
    private Button sonraki;

    public DefterEkrani(DefterPaketi veri) {
        super(Component.translatable(DefterCevirileri.ON + "baslik"));
        this.veri = veri;
    }

    /** Açık ekranda veriyi tazeler (sekme ve sayfa yerinde kalır). */
    void guncelle(DefterPaketi yeni) {
        veri = yeni;
        sayfalariKur();
        dugmeleriGuncelle();
    }

    @Override
    protected void init() {
        sol = (width - GENISLIK) / 2;
        ust = (height - YUKSEKLIK) / 2;
        sayfalariKur();
        onceki = addRenderableWidget(Button.builder(Component.literal("<"), b -> cevir(-1))
                .bounds(sol + SOL_X, ust + ALT_Y, DUGME_G, DUGME_YUKSEKLIK)
                .tooltip(Tooltip.create(Component.translatable(DefterCevirileri.ON + "onceki"))).build());
        sonraki = addRenderableWidget(Button.builder(Component.literal(">"), b -> cevir(1))
                .bounds(sol + GENISLIK - SOL_X - DUGME_G, ust + ALT_Y, DUGME_G, DUGME_YUKSEKLIK)
                .tooltip(Tooltip.create(Component.translatable(DefterCevirileri.ON + "sonraki"))).build());
        dugmeleriGuncelle();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- sayfalar

    private void sayfalariKur() {
        List<List<List<Satir>>> tum = new ArrayList<>(DefterIcerigi.SEKME_SAYISI);
        for (int t = 0; t < DefterIcerigi.SEKME_SAYISI; t++) {
            List<Satir> satirlar = DefterIcerigi.sekme(t, veri, font, SAYFA_G);
            int n = satirlar.size();
            int[] yukseklik = new int[n];
            int[] grup = new int[n];
            for (int i = 0; i < n; i++) {
                yukseklik[i] = satirlar.get(i).yukseklik();
                grup[i] = satirlar.get(i).grup();
            }
            int[] sayfaNo = SayfaDuzeni.sayfala(yukseklik, grup, ICERIK_YUKSEKLIK);
            List<List<Satir>> sayfalar = new ArrayList<>();
            for (int p = 0; p < SayfaDuzeni.sayfaSayisi(sayfaNo); p++) {
                sayfalar.add(new ArrayList<>());
            }
            for (int i = 0; i < n; i++) {
                if (yukseklik[i] != SayfaDuzeni.SAYFA_SONU) {
                    sayfalar.get(sayfaNo[i]).add(satirlar.get(i));
                }
            }
            tum.add(sayfalar);
        }
        sayfalar = tum;
        for (int t = 0; t < acilim.length; t++) {
            acilim[t] = Math.min(acilim[t], acilimSayisi(t) - 1);
        }
    }

    /** Bir sekmenin açılım (iki sayfalık) sayısı. */
    private int acilimSayisi(int sekmeNo) {
        return (sayfalar.get(sekmeNo).size() + 1) / 2;
    }

    private void cevir(int yon) {
        int yeni = Mth.clamp(acilim[sekme] + yon, 0, acilimSayisi(sekme) - 1);
        if (yeni != acilim[sekme]) {
            acilim[sekme] = yeni;
            ses(SoundEvents.BOOK_PAGE_TURN);
        }
        dugmeleriGuncelle();
    }

    private void dugmeleriGuncelle() {
        if (onceki == null || sonraki == null) {
            return;
        }
        onceki.active = acilim[sekme] > 0;
        sonraki.active = acilim[sekme] < acilimSayisi(sekme) - 1;
    }

    private void ses(SoundEvent olay) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(olay, 1.0f));
        }
    }

    // ---------------------------------------------------------------- giriş

    @Override
    public boolean mouseClicked(double fareX, double fareY, int dugme) {
        if (dugme == 0 && fareY >= ust + SEKME_Y && fareY < ust + SAYFA_Y0) {
            for (int i = 0; i < DefterIcerigi.SEKME_SAYISI; i++) {
                int x = sekmeX(i);
                if (fareX >= x && fareX < x + SEKME_G) {
                    if (i != sekme) {
                        sekme = i;
                        dugmeleriGuncelle();
                        ses(SoundEvents.UI_BUTTON_CLICK.value());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(fareX, fareY, dugme);
    }

    @Override
    public boolean mouseScrolled(double fareX, double fareY, double miktar) {
        cevir(miktar < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int tus, int tarama, int degistirici) {
        if (tus == GLFW.GLFW_KEY_LEFT || tus == GLFW.GLFW_KEY_PAGE_UP) {
            cevir(-1);
            return true;
        }
        if (tus == GLFW.GLFW_KEY_RIGHT || tus == GLFW.GLFW_KEY_PAGE_DOWN) {
            cevir(1);
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(tus, tarama)) {
            onClose();
            return true;
        }
        return super.keyPressed(tus, tarama, degistirici);
    }

    // ---------------------------------------------------------------- çizim

    @Override
    public void render(GuiGraphics g, int fareX, int fareY, float kismiTick) {
        renderBackground(g);
        zemin(g);
        sekmeleriCiz(g, fareX, fareY);
        ItemStack ipucu = sayfaCiz(g, acilim[sekme] * 2, sol + SOL_X, fareX, fareY);
        ItemStack sagIpucu = sayfaCiz(g, acilim[sekme] * 2 + 1, sol + SAG_X, fareX, fareY);
        super.render(g, fareX, fareY, kismiTick);
        g.drawCenteredString(font, Component.translatable(DefterCevirileri.ON + "sayfa", acilim[sekme] + 1,
                acilimSayisi(sekme)), sol + GENISLIK / 2, ust + ALT_Y + 4, ALT_YAZI);
        if (ipucu == null) {
            ipucu = sagIpucu;
        }
        if (ipucu != null) {
            g.renderTooltip(font, ipucu, fareX, fareY);
        }
    }

    /** Kapak, iki sayfa ve cilt. */
    private void zemin(GuiGraphics g) {
        int x1 = sol + GENISLIK;
        int y1 = ust + YUKSEKLIK;
        g.fill(sol - 1, ust - 1, x1 + 1, y1 + 1, KENAR);
        g.fill(sol, ust, x1, y1, KAPAK);
        g.fill(sol, ust, x1, ust + 1, KAPAK_ACIK);
        g.fill(sol, y1 - 1, x1, y1, KAPAK_KOYU);
        int sayfaX0 = sol + 8;
        int sayfaX1 = x1 - 8;
        int sayfaY0 = ust + SAYFA_Y0;
        int sayfaY1 = ust + SAYFA_Y1;
        g.fill(sayfaX0 - 1, sayfaY0 - 1, sayfaX1 + 1, sayfaY1 + 1, SAYFA_KENAR);
        g.fill(sayfaX0, sayfaY0, sayfaX1, sayfaY1, SAYFA);
        int orta = sol + GENISLIK / 2;
        g.fill(orta - 5, sayfaY0, orta + 5, sayfaY1, SAYFA_GOLGE);
        g.fill(orta - 1, sayfaY0, orta + 1, sayfaY1, CILT);
    }

    private int sekmeX(int sira) {
        return sol + SOL_X + sira * (SEKME_G + SEKME_ARA);
    }

    private void sekmeleriCiz(GuiGraphics g, int fareX, int fareY) {
        for (int i = 0; i < DefterIcerigi.SEKME_SAYISI; i++) {
            int x = sekmeX(i);
            int y = ust + SEKME_Y;
            boolean secili = i == sekme;
            boolean uzerinde = fareX >= x && fareX < x + SEKME_G && fareY >= y && fareY < y + SEKME_YUKSEKLIK;
            int alt = secili ? ust + SAYFA_Y0 : y + SEKME_YUKSEKLIK;
            g.fill(x - 1, y - 1, x + SEKME_G + 1, alt, KENAR);
            g.fill(x, y, x + SEKME_G, alt, secili ? SAYFA : uzerinde ? SEKME_UZERINDE : SEKME_PASIF);
            g.drawCenteredString(font, Component.translatable(DefterCevirileri.ON + SEKME_ANAHTARI[i]), x + SEKME_G / 2,
                    y + 5, secili ? DefterSatiri.BASLIK : SEKME_YAZI);
        }
    }

    /** Bir sayfa sütununu çizer; fare bir eşya satırındaysa o eşyayı (ipucu için) döner. */
    private ItemStack sayfaCiz(GuiGraphics g, int sayfaNo, int x, int fareX, int fareY) {
        List<List<Satir>> bu = sayfalar.get(sekme);
        if (sayfaNo >= bu.size()) {
            return null;
        }
        ItemStack ipucu = null;
        int y = ust + ICERIK_Y;
        for (Satir satir : bu.get(sayfaNo)) {
            satir.ciz(g, font, x, y, SAYFA_G);
            if (satir instanceof Esya esya && fareX >= x && fareX < x + SAYFA_G && fareY >= y
                    && fareY < y + satir.yukseklik()) {
                ipucu = esya.yigin();
            }
            y += satir.yukseklik();
        }
        return ipucu;
    }
}

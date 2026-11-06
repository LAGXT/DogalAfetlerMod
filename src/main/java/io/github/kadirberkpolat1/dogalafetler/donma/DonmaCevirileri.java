package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Donma sisteminin dil anahtarları (datagen; {@code en_us} ve {@code tr_tr}). */
public final class DonmaCevirileri {
    private static final String ON = "commands.dogalafetler.donma.";
    static final String SUREC_DONMA = "donem_sureci.dogalafetler.donma";
    static final String SUREC_BUHARLASMA = "donem_sureci.dogalafetler.buharlasma";
    static final String AYARLANDI = ON + "ayarlandi";
    static final String BUHARLASMA_AYARLANDI = ON + "buharlasma_ayarlandi";
    static final String DONEM_DONMA = ON + "donem.donma";
    static final String DONEM_COZULME = ON + "donem.cozulme";
    static final String BILGI_ILERLEME = ON + "bilgi.ilerleme";
    static final String BILGI_CHUNK = ON + "bilgi.chunk";
    static final String BILGI_CHUNK_YOK = ON + "bilgi.chunk_yok";
    static final String BILGI_SUTUN = ON + "bilgi.sutun";
    static final String BILGI_BUTCE = ON + "bilgi.butce";
    static final String BILGI_SURE = ON + "bilgi.sure";
    static final String BILGI_KUYRUK = ON + "bilgi.kuyruk";
    static final String SUREC_SURUYOR = ON + "surec.suruyor";
    static final String SUREC_DURDU = ON + "surec.durdu";
    static final String SUREC_YOK = ON + "surec.yok";
    static final String KIS_SABIT = ON + "kis.sabit";
    static final String KIS_CONFIG = ON + "kis.config";
    static final String KIS_EVET = ON + "kis.evet";
    static final String KIS_HAYIR = ON + "kis.hayir";
    static final String KIS_SINIR = ON + "kis.sinir";

    private DonmaCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        y.ekle("block.dogalafetler.kiragili_cimen", "Frosted Grass Block", "Kırağılı Çimen");
        y.ekle("block.dogalafetler.ince_buz", "Thin Ice", "İnce Buz");
        y.ekle(SUREC_DONMA, "World freeze", "Dünya donması");
        y.ekle(SUREC_BUHARLASMA, "Evaporation", "Buharlaşma");
        y.ekle(AYARLANDI, "Freeze progress set to %s%% (%s period).", "Donma ilerlemesi %%%s olarak ayarlandı (%s dönemi).");
        y.ekle(BUHARLASMA_AYARLANDI, "Evaporation depth set to %s blocks (%s applied to real water).",
                "Buharlaşma derinliği %s blok olarak ayarlandı (gerçek suya %s).");
        y.ekle(DONEM_DONMA, "freeze", "donma");
        y.ekle(DONEM_COZULME, "thaw", "çözülme");
        y.ekle(BILGI_ILERLEME, "Freeze: %s%% (%s period, sun %s); evaporation %s blocks (%s on real water); process: %s",
                "Donma: %%%s (%s dönemi, güneş %s); buharlaşma %s blok (gerçek suya %s); süreç: %s");
        y.ekle(BILGI_CHUNK, "Chunk [%s, %s]: last processed %s (%s), target %s, evaporation %s, half step %s",
                "Chunk [%s, %s]: son işlenen %s (%s), hedef %s, buharlaşma %s, yarım adım %s");
        y.ekle(BILGI_CHUNK_YOK, "Chunk [%s, %s]: not loaded", "Chunk [%s, %s]: yüklü değil");
        y.ekle(BILGI_SUTUN, "Column (%s, %s) surface Y=%s: target level %s, winter weight %s (mask %s, biome %s, height %s),"
                + " warm offset %s, parameters %s", "Sütun (%s, %s) yüzey Y=%s: hedef seviye %s, kış ağırlığı %s (maske %s,"
                + " biyom %s, yükseklik %s), ılık ofset %s, parametreler %s");
        y.ekle(BILGI_BUTCE, "Budget: %s blocks/tick (throttle x%s, TPS %s); most blocks in one tick %s",
                "Bütçe: %s blok/tick (kısma x%s, TPS %s); bir tick'te en çok %s blok");
        y.ekle(BILGI_SURE, "Longest worker tick: %s ms after warm-up (warm-up window %s ms); time cap %s ms",
                "En uzun işçi tick'i: ısınmadan sonra %s ms (ısınma penceresi %s ms); süre tavanı %s ms");
        y.ekle(BILGI_KUYRUK, "Queue: loaded %s, waiting %s, running %s; done %s steps (%s at target), %s blocks",
                "Kuyruk: yüklü %s, bekleyen %s, süren %s; tamamlanan %s adım (%s hedefte), %s blok");
        y.ekle(SUREC_SURUYOR, "running", "sürüyor");
        y.ekle(SUREC_DURDU, "stopped (%s)", "durduruldu (%s)");
        y.ekle(SUREC_YOK, "not running", "sürmüyor");
        y.ekle(KIS_SABIT, "pinned to world", "dünyaya sabit");
        y.ekle(KIS_CONFIG, "from config", "config'ten");
        y.ekle(KIS_EVET, "Permanent winter zone: yes (column %s, %s, weight %s, target %s)",
                "Kalıcı kış bölgesi: evet (sütun %s, %s, ağırlık %s, hedef %s)");
        y.ekle(KIS_HAYIR, "Permanent winter zone: no (column %s, %s, weight %s, target %s)",
                "Kalıcı kış bölgesi: hayır (sütun %s, %s, ağırlık %s, hedef %s)");
        y.ekle(KIS_SINIR, "Permanent winter zone: border belt (column %s, %s, weight %s, target %s)",
                "Kalıcı kış bölgesi: sınır kuşağı (sütun %s, %s, ağırlık %s, hedef %s)");
    }
}

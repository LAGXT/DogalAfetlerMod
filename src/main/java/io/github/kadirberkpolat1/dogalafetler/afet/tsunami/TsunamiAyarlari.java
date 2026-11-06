package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** Tsunami SERVER ayarları ({@code dogalafetler-server.toml} → {@code [tsunami]}); N17 satırları {@code .vibe/kararlar/tsunami.md} §C. */
public final class TsunamiAyarlari {
    private static AyarKaydi.Bolum<TsunamiAyarlari> bolum;

    final ForgeConfigSpec.IntValue enAzDenizSutunu;
    final ForgeConfigSpec.IntValue aramaYaricapi;
    final ForgeConfigSpec.DoubleValue dalgaGecikmeSn;
    final ForgeConfigSpec.DoubleValue cekilmeSn;
    final ForgeConfigSpec.DoubleValue ayCekilmeCarpani;
    final ForgeConfigSpec.DoubleValue yikimCarpani;
    final ForgeConfigSpec.IntValue yikimBolumTick;
    final ForgeConfigSpec.IntValue yikimChunkTick;
    final ForgeConfigSpec.IntValue yikimEfektTick;
    final ForgeConfigSpec.IntValue magaraHacmi;

    private TsunamiAyarlari(ForgeConfigSpec.Builder k) {
        enAzDenizSutunu = k.comment("N14: hedefin çevresinde en az bu kadar açık (donmamış), en az 3 blok derin su sütunu yoksa yer uygun değil.")
                .defineInRange("enAzDenizSutunu", 400, 1, 20000);
        aramaYaricapi = k.comment("Deniz aramasının yatay yarıçapı (blok).").defineInRange("aramaYaricapi", 96, 32, 160);
        dalgaGecikmeSn = k.comment("Çekilme başladıktan sonra dalganın açıkta belirmesine kadar geçen süre (sn).")
                .defineInRange("dalgaGecikmeSn", 10.0, 0.0, 60.0);
        cekilmeSn = k.comment("Suyun çekilmesinin tamamlandığı süre (sn).").defineInRange("cekilmeSn", 18.0, 1.0, 120.0);
        ayCekilmeCarpani = k.comment("Bölüm H: ay çarpması sonrası tsunami çarpanı sürerken çekilme derinliği ve mesafesi çarpanı.")
                .defineInRange("ayCekilmeCarpani", 1.5, 1.0, 3.0);
        yikimCarpani = k.comment("Tick başına yıkım bütçesi çarpanı (S1 16 … S5 48 blok; ayrıca TPS kısmasıyla çarpılır).")
                .defineInRange("yikimCarpani", 1.0, 0.1, 4.0);
        yikimBolumTick = k.comment("Tick başına yıkımın değiştirdiği en çok farklı 16x16x16 chunk bölümü (her biri istemcide yeniden "
                + "derlenir; TPS kısmasıyla çarpılır).").defineInRange("yikimBolumTick", 2, 1, 64);
        yikimChunkTick = k.comment("Tick başına yıkımın değiştirdiği en çok farklı chunk sütunu (ışık değişimi sütun boyunca "
                + "bölümleri istemcide yeniden derletir).").defineInRange("yikimChunkTick", 1, 1, 64);
        yikimEfektTick = k.comment("Tick başına kırılma efekti (vanilla kırılma sesi + ~64 blok parçacığı); 0 kapalı.")
                .defineInRange("yikimEfektTick", 1, 0, 6);
        magaraHacmi = k.comment("Seviye başına bir mağara cebine dolan en çok hücre (su.magaraMaksHacim ile sınırlı).")
                .defineInRange("magaraHacmi", 400, 16, 4000);
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("tsunami", "Tsunami (bölüm B)", TsunamiAyarlari::new);
    }

    static TsunamiAyarlari al() {
        return bolum.get();
    }
}

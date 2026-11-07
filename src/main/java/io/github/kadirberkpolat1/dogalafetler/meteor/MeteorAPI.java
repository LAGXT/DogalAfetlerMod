package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.Yorunge;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Meteorun dış API'si (Faz 4 güneş patlaması ve ay çarpması; sözleşme {@code .vibe/kararlar/meteor-termostat.md} §A).
 * Yalnız sunucu ana thread'inde çağrılır. Ateşler ortak yangına {@code kaynak} ile gider; afet bitince ({@code
 * AfetBittiEvent}) o kaynağın yağmurları kendiliğinden durur (temiz bitişte uçanlar da kalkar).
 */
public final class MeteorAPI {
    private MeteorAPI() {
    }

    /**
     * {@code hedef} sütununa (y = yüzey, yapraklar hariç) sınıfın tipik ölçeğinde bir meteor düşürür. Görünüm kaynaktan
     * gelir (ay çarpması → ay kayası, diğerleri → güneş). Hedef chunk yüklü değilse ya da meteor tavanı doluysa boş döner
     * (meteor düşmez, beklemez). Hedef varlık işleyen bir chunk'ta değilse (uçuş görülemez) çarpma hemen olur ve yine boş
     * döner.
     *
     * @param sarsinti çarpma yakındaki oyuncuların ekranını sarssın mı
     */
    public static Optional<MeteorVarligi> dusur(ServerLevel level, BlockPos hedef, MeteorBoyutu boyut,
            YanginKaynagi kaynak, boolean sarsinti) {
        return dusur(level, hedef, boyut.tipikOlcek(), MeteorGorunumu.kaynaktan(kaynak), kaynak, sarsinti);
    }

    public static Optional<MeteorVarligi> dusur(ServerLevel level, BlockPos hedef, MeteorBoyutu boyut,
            MeteorGorunumu gorunum, YanginKaynagi kaynak, boolean sarsinti) {
        return dusur(level, hedef, boyut.tipikOlcek(), gorunum, kaynak, sarsinti);
    }

    /**
     * Sürekli ölçekli meteor ({@code mantik.MeteorOlcegi}, 0,5–2,5): görünür boyut, hız, iz, krater, yangın, sarsıntı ve ses
     * ölçekle; ödül sınıfı ölçeğin bantı.
     */
    public static Optional<MeteorVarligi> dusur(ServerLevel level, BlockPos hedef, double olcek, MeteorGorunumu gorunum,
            YanginKaynagi kaynak, boolean sarsinti) {
        int enAz = MeteorAyarlari.girisAcisiEnAz();
        int enCok = Math.max(enAz, MeteorAyarlari.girisAcisiEnCok());
        double aci = enAz + level.random.nextDouble() * (enCok - enAz);
        return dusur(level, hedef, olcek, gorunum, kaynak, sarsinti, aci, level.random.nextDouble() * 2 * Math.PI, null);
    }

    /**
     * Giriş açısı (derece, yatayla) ve geliş yönü (radyan; 0 = +X'ten, π/2 = +Z'den) seçilmiş meteor (görsel otomasyon,
     * test). Başlangıç noktası varlık işleyen bir chunk'ta değilse açı dikleştirilir.
     */
    public static Optional<MeteorVarligi> dusur(ServerLevel level, BlockPos hedef, MeteorBoyutu boyut,
            MeteorGorunumu gorunum, YanginKaynagi kaynak, boolean sarsinti, double aci, double azimut) {
        return dusur(level, hedef, boyut.tipikOlcek(), gorunum, kaynak, sarsinti, aci, azimut, null);
    }

    static Optional<MeteorVarligi> dusur(ServerLevel level, BlockPos hedef, double olcek, MeteorGorunumu gorunum,
            YanginKaynagi kaynak, boolean sarsinti, double aci, double azimut, OdulZari.Sanslar sanslar) {
        Objects.requireNonNull(kaynak, "kaynak");
        if (!level.hasChunkAt(hedef)) {
            return Optional.empty();
        }
        if (MeteorYoneticisi.aktifMeteorSayisi(level) >= MeteorAyarlari.varlikTavani()) {
            DogalAfetler.LOGGER.debug("meteor tavanı dolu ({}), {} atılmadı", MeteorAyarlari.varlikTavani(), hedef);
            return Optional.empty();
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hedef.getX(), hedef.getZ());
        Vec3 nokta = new Vec3(hedef.getX() + 0.5, y, hedef.getZ() + 0.5);
        BlockPos zemin = BlockPos.containing(nokta.x, y - 1, nokta.z);
        if (!level.isPositionEntityTicking(zemin)) {
            double sanal = SanalSu.suSeviyesi(level, zemin.getX(), zemin.getZ());
            boolean suya = level.getFluidState(zemin).is(FluidTags.WATER) || !Double.isNaN(sanal) && sanal > nokta.y;
            MeteorCarpmasi.carp(level, zemin, nokta, olcek, gorunum, kaynak, sarsinti,
                    sanslar != null ? sanslar : MeteorAyarlari.sanslar(), suya);
            return Optional.empty();
        }
        double girisAcisi = aci;
        // Büyük meteor biraz hızlı (tipik küçük ×0,95, büyük ×1,15); eksen başına paket sınırı 3,9.
        double hiz = Math.min(3.8, MeteorAyarlari.dusmeHizi() * MeteorOlcegi.ara(olcek, 0.95, 1.15, 0.9, 1.2));
        Yorunge yorunge = null;
        for (int deneme = 0; deneme < 6; deneme++) {
            yorunge = Yorunge.hesapla(nokta.x, nokta.y, nokta.z, girisAcisi, azimut, MeteorAyarlari.girisYuksekligi(), hiz);
            if (level.isPositionEntityTicking(BlockPos.containing(yorunge.baslangicX(), nokta.y, yorunge.baslangicZ()))) {
                break;
            }
            girisAcisi = deneme == 4 ? 90 : Math.min(90, girisAcisi + 10);
        }
        MeteorVarligi m = MeteorVarligi.olustur(level, yorunge, olcek, gorunum, kaynak, sarsinti, sanslar);
        level.addFreshEntity(m);
        MeteorYoneticisi.meteorEkle(level, m);
        return Optional.of(m);
    }

    /**
     * {@code sureTick} boyunca oyuncu başına dakikada {@code dakikaBasinaAdet} meteor yağdırır; meteorlar oyunculara sırayla
     * ve her birinin çevresine dengeli dağılır (bölüm D). {@code oyuncular} boşsa boyuttaki bütün oyuncular (giren/çıkan
     * dahil) kullanılır. Her meteorun ölçeği {@link MeteorOlcegi#ornekle}: büyük bant {@code buyukOran}, orta bant
     * {@code meteor.ortaMeteorOrani}, küçük kalanı. Tavan doluyken sıradaki meteor düşmez. Yağmur kimliğini döndürür.
     */
    public static int yagmur(ServerLevel level, Collection<ServerPlayer> oyuncular, int sureTick, double dakikaBasinaAdet,
            double buyukOran, YanginKaynagi kaynak) {
        Objects.requireNonNull(kaynak, "kaynak");
        Set<UUID> kimlikler = new HashSet<>();
        if (oyuncular != null) {
            for (ServerPlayer o : oyuncular) {
                kimlikler.add(o.getUUID());
            }
        }
        long tohum = level.random.nextLong();
        return MeteorYoneticisi.yagmurEkle(level,
                k -> new MeteorYagmuru(k, kimlikler, sureTick, dakikaBasinaAdet, buyukOran, kaynak, tohum));
    }

    /** Test: dağıtım halkası ve giriş açısı sabit yağmur (meteorlar test sahnesine düşsün). */
    static int yagmur(ServerLevel level, Collection<ServerPlayer> oyuncular, int sureTick, double dakikaBasinaAdet,
            double buyukOran, YanginKaynagi kaynak, int ic, int dis, double aci) {
        Set<UUID> kimlikler = new HashSet<>();
        for (ServerPlayer o : oyuncular) {
            kimlikler.add(o.getUUID());
        }
        long tohum = level.random.nextLong();
        return MeteorYoneticisi.yagmurEkle(level, k -> new MeteorYagmuru(k, kimlikler, sureTick, dakikaBasinaAdet, buyukOran,
                kaynak, tohum).testAyari(ic, dis, aci));
    }

    public static boolean yagmurDurdur(ServerLevel level, int kimlik) {
        return MeteorYoneticisi.yagmurDurdur(level, kimlik);
    }

    /**
     * Yağmurun oyuncu başına dakikadaki yoğunluğunu çalışırken değiştirir (afetin yağmur eğrisi; birikim korunur). Yağmur
     * yoksa (süresi doldu, durduruldu, sunucu yeniden açıldı) {@code false}: çağıran yenisini kurar.
     */
    public static boolean yagmurYogunluguAyarla(ServerLevel level, int kimlik, double dakikaBasinaAdet) {
        return MeteorYoneticisi.yogunlukAyarla(level, kimlik, dakikaBasinaAdet);
    }

    /** Yağmurun oyuncu başına dakikadaki yoğunluğu; yağmur yoksa NaN. */
    public static double yagmurYogunlugu(ServerLevel level, int kimlik) {
        return MeteorYoneticisi.yogunluk(level, kimlik);
    }

    /** Kaynağın bütün boyutlardaki yağmurlarını durdurur ({@code ucanlariAt}: uçan meteorlarını da kaldırır). */
    public static int yagmurlariDurdur(MinecraftServer server, YanginKaynagi kaynak, boolean ucanlariAt) {
        return MeteorYoneticisi.durdur(server, Objects.requireNonNull(kaynak, "kaynak"), ucanlariAt);
    }

    /**
     * Test ödülü ({@code /dogalafet odul meteor}, KOM.13): {@code parca} Meteorit Parçası ve {@code cekirdek} Kozmik Çekirdek
     * soğuk olarak envantere girer; sığmayan oyuncunun önüne düşer. Verilen toplam eşya sayısını döndürür.
     */
    public static int odulVer(ServerPlayer oyuncu, int parca, int cekirdek) {
        Objects.requireNonNull(oyuncu, "oyuncu");
        ver(oyuncu, MeteorEsyaKayitlari.METEORIT_PARCASI.get(), parca);
        ver(oyuncu, MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get(), cekirdek);
        return Math.max(0, parca) + Math.max(0, cekirdek);
    }

    private static void ver(ServerPlayer oyuncu, Item esya, int adet) {
        while (adet > 0) {
            ItemStack yigin = new ItemStack(esya, Math.min(adet, esya.getMaxStackSize()));
            adet -= yigin.getCount();
            if (!oyuncu.getInventory().add(yigin) && !yigin.isEmpty()) {
                oyuncu.drop(yigin, false);
            }
        }
        oyuncu.inventoryMenu.broadcastChanges();
    }

    public static int aktifMeteorSayisi(ServerLevel level) {
        return MeteorYoneticisi.aktifMeteorSayisi(level);
    }

    public static int aktifYagmurSayisi(ServerLevel level) {
        return MeteorYoneticisi.yagmurSayisi(level);
    }

    public static int bekleyenKraterSayisi(ServerLevel level) {
        return MeteorYoneticisi.bekleyenKraterSayisi(level);
    }

    /**
     * Kaynağın bu boyuttaki etkin yağmurlarının (başlangıçlarından beri) atmaya kalkıştığı meteor; biten ya da durdurulan yağmur sayılmaz: tavan ya da yüklü olmayan chunk
     * yüzünden uçmayanlar dahil (komut, test).
     */
    public static int atilanMeteorSayisi(ServerLevel level, YanginKaynagi kaynak) {
        return MeteorYoneticisi.atilanSayisi(level, kaynak);
    }

    /** Son tick'te krater işlerinin kullandığı blok işlemi; bütçe ({@code meteor.kraterBlokButcesi} × TPS kısması) aşılmaz. */
    public static int sonKraterIslemi(ServerLevel level) {
        return MeteorYoneticisi.kraterIslemi(level)[0];
    }

    /** Son tick'in krater blok bütçesi. */
    public static int sonKraterButcesi(ServerLevel level) {
        return MeteorYoneticisi.kraterIslemi(level)[1];
    }
}

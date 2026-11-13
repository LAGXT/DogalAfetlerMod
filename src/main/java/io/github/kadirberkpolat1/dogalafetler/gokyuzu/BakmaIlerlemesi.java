package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.BakmaKurallari;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * Gökyüzüne bakma başarımlarının sunucu tarafı ({@code network.BakmaPaketi}): istemci bir bakma tamamlanınca (devasa kara
 * delik 5 sn, gezegen başına 3 sn) tek paket yollar. Gezegen ilerlemesi oyuncunun kalıcı verisinde ({@code getPersistentData}) bir
 * bit maskesi olarak durur (K-E84: eskiden yalnız bellekteydi ve çıkışta siliniyordu; sekiz gezegenin hepsine tek oturumda bakmak
 * gerekiyordu, oyundan çıkıp girince ya da ölünce ilerleme kayboluyordu). Sekiz gezegenin (Plüton dahil) hepsi tamamlanınca
 * {@code astronom} verilir. Sunucu bakışı doğrulayamaz; yalnız geçerli numara, canlı oyuncu ve Overworld denetlenir, tekrarlar
 * sessizce düşer. Yalnız ana thread.
 */
public final class BakmaIlerlemesi {
    /** Oyuncunun kalıcı verisinde ilerlemenin saklanıdığı etiket ve gezegen maskesi anahtarı. */
    static final String VERI = "dogalafetler_bakma";
    static final String GEZEGEN_MASKESI = "gezegenler";
    private static final Set<UUID> DEVASA_VERILDI = new HashSet<>();

    private BakmaIlerlemesi() {
    }

    static void kaydet() {
        MinecraftForge.EVENT_BUS.addListener(BakmaIlerlemesi::oyuncuCikti);
        MinecraftForge.EVENT_BUS.addListener(BakmaIlerlemesi::oyuncuKopyasi);
        MinecraftForge.EVENT_BUS.addListener(BakmaIlerlemesi::sunucuDurdu);
    }

    /** Oyuncunun kayıtlı gezegen maskesi (hiç bakmadıysa 0). */
    public static int gezegenMaskesi(ServerPlayer oyuncu) {
        return oyuncu.getPersistentData().getCompound(VERI).getInt(GEZEGEN_MASKESI);
    }

    private static void maskeyiYaz(ServerPlayer oyuncu, int maske) {
        CompoundTag veri = oyuncu.getPersistentData().getCompound(VERI);
        veri.putInt(GEZEGEN_MASKESI, maske);
        oyuncu.getPersistentData().put(VERI, veri);
    }

    /** Sunucu ana thread'inde, paket işleyicisinden. */
    public static void isle(ServerPlayer oyuncu, int tur, int hedef) {
        if (!oyuncu.isAlive() || oyuncu.level().dimension() != Level.OVERWORLD) {
            return;
        }
        UUID kimlik = oyuncu.getUUID();
        if (tur == BakmaKurallari.TUR_DEVASA) {
            if (DEVASA_VERILDI.add(kimlik)) {
                OzelBasarim.ver(oyuncu, OzelBasarim.DEVASA);
            }
        } else if (tur == BakmaKurallari.TUR_GEZEGEN && BakmaKurallari.gecerliGezegen(hedef)) {
            int eski = gezegenMaskesi(oyuncu);
            int yeni = BakmaKurallari.maskeyeEkle(eski, hedef);
            if (yeni == eski) {
                return;
            }
            maskeyiYaz(oyuncu, yeni);
            if (BakmaKurallari.tamMi(yeni)) {
                OzelBasarim.ver(oyuncu, OzelBasarim.ASTRONOM);
            }
        }
    }

    private static void oyuncuCikti(PlayerEvent.PlayerLoggedOutEvent olay) {
        DEVASA_VERILDI.remove(olay.getEntity().getUUID());
    }

    /** Ölüm ve End dönüşünde yeni oyuncu nesnesine ilerleme taşınır. */
    private static void oyuncuKopyasi(PlayerEvent.Clone olay) {
        CompoundTag eski = olay.getOriginal().getPersistentData();
        if (eski.contains(VERI)) {
            olay.getEntity().getPersistentData().put(VERI, eski.getCompound(VERI).copy());
        }
    }

    private static void sunucuDurdu(ServerStoppedEvent olay) {
        DEVASA_VERILDI.clear();
    }
}

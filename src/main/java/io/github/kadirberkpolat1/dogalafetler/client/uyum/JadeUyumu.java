package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.sicaklik.IstemciSicaklik;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.KomurlesmisKutukBlogu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.KulKatmaniBlogu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KutukIsisi;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyum.UyumCevirileri;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade uyumu (K-E74): bakılan bloğa göre afet ve sıcaklık satırları. Tüm veri istemcide zaten vardır (blok durumu, sıcaklık
 * paketi, afet paketi); sunucu veri sağlayıcısı ve paket gerekmez.
 * <ul>
 * <li>Kömürleşmiş kütük: ısı kademesi ve durumu (sönük/ılık/sıcak/çok sıcak); eğilirken lav/kar ipucu.</li>
 * <li>Kül katmanı: kaç kat kül.</li>
 * <li>Her blokta (Jade ayarından açılır, varsayılan kapalı): hissedilen sıcaklık ve aktif afet.</li>
 * </ul>
 * Bu sınıf yalnız Jade yüklükken Forge'un tarama verisiyle bulunur; Jade yoksa hiç yüklenmez. API imzaları {@code src/uyumApi}
 * taslaklarından derlenir.
 */
@WailaPlugin
public final class JadeUyumu implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration kayit) {
        kayit.registerBlockComponent(KutukSaglayici.INSTANCE, KomurlesmisKutukBlogu.class);
        kayit.registerBlockComponent(KulSaglayici.INSTANCE, KulKatmaniBlogu.class);
        kayit.registerBlockComponent(OrtamSaglayici.INSTANCE, Block.class);
    }

    private enum KutukSaglayici implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation KIMLIK = DogalAfetler.konum("kutuk_isi");

        @Override
        public void appendTooltip(ITooltip ipucu, BlockAccessor blok, IPluginConfig ayar) {
            BlockState durum = blok.getBlockState();
            if (!durum.hasProperty(KomurlesmisKutukBlogu.ISI)) {
                return;
            }
            int isi = durum.getValue(KomurlesmisKutukBlogu.ISI);
            String ek = KutukIsisi.gorunum(isi).name().toLowerCase(Locale.ROOT);
            ipucu.add(Component.translatable(UyumCevirileri.JADE_KUTUK, isi, KutukIsisi.MAKS,
                    Component.translatable(UyumCevirileri.JADE_KUTUK_DURUM + ek)));
            if (isi < KutukIsisi.MAKS && blok.getPlayer() != null && blok.getPlayer().isShiftKeyDown()) {
                ipucu.add(Component.translatable(UyumCevirileri.JADE_KUTUK_IPUCU).withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return KIMLIK;
        }
    }

    private enum KulSaglayici implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation KIMLIK = DogalAfetler.konum("kul_kati");

        @Override
        public void appendTooltip(ITooltip ipucu, BlockAccessor blok, IPluginConfig ayar) {
            BlockState durum = blok.getBlockState();
            if (durum.hasProperty(SnowLayerBlock.LAYERS)) {
                ipucu.add(Component.translatable(UyumCevirileri.JADE_KUL, durum.getValue(SnowLayerBlock.LAYERS),
                        SnowLayerBlock.MAX_HEIGHT));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return KIMLIK;
        }
    }

    /** Her blokta hissedilen sıcaklık ve aktif afet; Jade ayarlarından açılana dek kapalı. */
    private enum OrtamSaglayici implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation KIMLIK = DogalAfetler.konum("ortam");

        @Override
        public void appendTooltip(ITooltip ipucu, BlockAccessor blok, IPluginConfig ayar) {
            if (IstemciSicaklik.acik()) {
                ipucu.add(Component.translatable(UyumCevirileri.JADE_HISSEDILEN,
                        String.format(Locale.ROOT, "%.1f", IstemciSicaklik.hissedilen(0f))));
            }
            Optional<AfetDurumPaketi> afet = IstemciAfetDurumu.aktif().filter(AfetDurumPaketi::aktif);
            afet.ifPresent(p -> ipucu.add(Component.translatable(UyumCevirileri.JADE_AFET,
                    Component.translatable(p.tur().ceviriAnahtari()), p.seviye())));
        }

        @Override
        public boolean enabledByDefault() {
            return false;
        }

        @Override
        public ResourceLocation getUid() {
            return KIMLIK;
        }
    }
}

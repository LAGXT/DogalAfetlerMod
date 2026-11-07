/**
 * Çekirdeğin istemci tarafı (N15): sunucudan gelen dünya dönemi ve afet durumu önbellekleri. Ortak kod bu paketi yalnız
 * {@code DistExecutor} üzerinden çağırır; olay aboneleri {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)} ile kayıtlıdır.
 * Diğer istemci alt sistemleri (gökyüzü, sıcaklık, su, radar) durumu {@link IstemciDonem} ve {@link IstemciAfetDurumu}'ndan
 * okur. Görsel otomasyonların (yalnız {@code -Potomasyon}) ortak ölçüm satırı ve kare kapısı {@link GorselOtomasyon}'dadır.
 */
package io.github.kadirberkpolat1.dogalafetler.client.core;

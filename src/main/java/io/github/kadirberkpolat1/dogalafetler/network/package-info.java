/**
 * Ağ (sahibi: çekirdek işçisi). Tek bir {@code SimpleChannel} vardır: {@link io.github.kadirberkpolat1.dogalafetler.network.AgKanali}.
 * Her alt sistem kendi paket sınıflarını kendi paketinde yazar ve mod kurucusunda {@code AgKanali.kaydet(k -> ...)} ile
 * kayıt sırasına ekler; çekirdek ortak kurulumda (FMLCommonSetupEvent) önce kendi paketlerini, sonra katkıları eklendikleri
 * sırayla kaydeder (K0-07). İstemci tarafı işleyiciler N15'e uyar: istemci sınıfına yalnız {@code DistExecutor} ya da
 * {@code FMLEnvironment.dist} kontrolü üzerinden gidilir.
 */
package io.github.kadirberkpolat1.dogalafetler.network;

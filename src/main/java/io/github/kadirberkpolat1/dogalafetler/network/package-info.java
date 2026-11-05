/**
 * Ağ (sahibi: çekirdek işçisi). Tek bir {@code SimpleChannel} vardır: {@link io.github.kadirberkpolat1.dogalafetler.network.AgKanali}.
 * Her alt sistem kendi paket sınıflarını kendi paketinde yazar ve ortak kurulumda (FMLCommonSetupEvent)
 * {@code AgKanali.sunucudanIstemciye(...)} ya da {@code AgKanali.istemcidenSunucuya(...)} ile kaydeder. İstemci
 * tarafı işleyiciler N15'e uyar: istemci sınıfına yalnız {@code DistExecutor} ya da {@code FMLEnvironment.dist}
 * kontrolü üzerinden gidilir.
 */
package io.github.kadirberkpolat1.dogalafetler.network;

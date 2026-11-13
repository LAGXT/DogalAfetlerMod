/**
 * İstemci kodu (sahibi: ilgili alt sistemin işçisi; alt paketler {@code client.<alt sistem>}). HUD, render, ses,
 * ekran sarsıntısı, radar/sismograf göstergeleri. N15: bu paketteki sınıflar yalnız
 * {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)} aboneleri, {@code DistExecutor} ya da {@code FMLEnvironment.dist}
 * kontrolü üzerinden çağrılır; ortak/sunucu kodu buradaki sınıfları doğrudan import etmez.
 */
package io.github.kadirberkpolat1.dogalafetler.client;

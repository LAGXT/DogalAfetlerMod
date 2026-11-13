/**
 * Yapılandırma (sahibi: çekirdek işçisi). Oynanış değerleri SERVER config'te (dünya başına, istemciye senkron),
 * görsel, ses ve HUD ayarları CLIENT config'te durur. Tek SERVER ve tek CLIENT spec vardır; alt sistemler kendi
 * bölümlerini mod kurucusunda {@link io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi#sunucuBolumu} ve
 * {@link io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi#istemciBolumu} ile ekler.
 */
package io.github.kadirberkpolat1.dogalafetler.config;

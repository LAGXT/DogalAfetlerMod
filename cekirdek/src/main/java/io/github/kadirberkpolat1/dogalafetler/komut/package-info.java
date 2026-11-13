/**
 * Komutlar (sahibi: çekirdek işçisi). {@code /dogalafet} kökü yalnız yetki seviyesi 2 (OP) ile kullanılır.
 * Çekirdeğin kendi alt komutları ({@code durum}, {@code start}, {@code stop}, {@code gun}, {@code hiz}, {@code sifirla},
 * {@code asama ileri}) burada; diğer alt sistemler kendi alt komutlarını kendi paketlerinde yazar ve mod inşasında
 * {@link io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi#ekle} ile ekler.
 */
package io.github.kadirberkpolat1.dogalafetler.komut;

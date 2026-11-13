/**
 * Afet Defteri (EN: Disaster Journal): oyuncunun afet geçmişini, balıkçılığını, mesafesini ve gün sayısını tuttuğu defter
 * eşyası, {@code /dogalafet defter} komutu ve oyuncu başına kalıcı istatistikler. Veri oyuncunun {@code getPersistentData()}
 * etiketindedir (ölümde korunur), mevcut olay kancalarından toplanır ve istemciye yalnız defter açılırken tek paketle gider
 * (periyodik yayın yok). Ekran {@code client.journal} paketindedir; bu paket istemci sınıfı import etmez.
 */
package io.github.kadirberkpolat1.dogalafetler.journal;

/**
 * Çekirdek (sahibi: çekirdek işçisi). Afet yöneticisi, ön-uyarı penceresi, şiddet seçimi, geçici olasılık
 * çarpanı, tek seferlik olaylar, afet yaşam döngüsü ({@code ON_UYARI → SIREN → AKTIF → BITIS}), dünya dönemi,
 * SavedData ve TPS ölçer burada durur. Minecraft sınıflarına dokunmayan saf mantık ayrı sınıflarda tutulur ve
 * JUnit ile test edilir.
 */
package io.github.kadirberkpolat1.dogalafetler.core;

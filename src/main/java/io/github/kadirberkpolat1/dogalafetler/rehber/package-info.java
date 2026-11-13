/**
 * Rehber yazıları (K-E74): oyuncu karakterinin kendi kendine söylediği yönlendirme cümleleri. Buzul kıyameti başlayınca
 * "Sanırım Biraz Balık Tutmalıyım", buzul kıyametinde buza boş elle sağ tıklayınca "Bunun İçin Bir Alete İhtiyacım Var.",
 * kazma ya da baltayla ayakta sağ tıklayınca "Eğilmeliyim.". Her cümle her dünyada ve oyuncu başına yalnız bir kez söylenir
 * ({@link io.github.kadirberkpolat1.dogalafetler.rehber.RehberVerisi}, oyuncu NBT'si, ölümde korunur). Sunucu tek küçük
 * paket yollar ({@link io.github.kadirberkpolat1.dogalafetler.rehber.RehberMesajPaketi}); yazı ve oyuncunun kendi buharı
 * {@code client.rehber} paketindedir, başkalarının gördüğü ağız buharını sunucu gönderir. Buz deliğini açan mantık
 * {@code balikcilik.BuzDeligiAcma}'dadır; bu paket yalnız ona yönlendiren ipucunu söyler.
 */
package io.github.kadirberkpolat1.dogalafetler.rehber;

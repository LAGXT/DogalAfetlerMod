package io.github.kadirberkpolat1.dogalafetler.core.olay;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.BitisNedeni;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

/**
 * Afet bitti (GOREV §4 "afet bitiş olayı"). {@code etkilenenler}: AKTIF aşamada etki alanına en az bir kez girmiş oyuncular;
 * {@code sagCikanlar}: bunlardan afet bitene kadar ölmeyen ve bitişte çevrim içi olanlar (madde 12 "sağ çıkmak").
 * Zamanlar ölçekli afet saatidir; {@code baslangicSaati} aynı zamanda afet numarasıdır (benzersiz, AS-16). Başarımlar yalnız
 * {@link #basarimSayilir()} olanları sayar (AS-13, AS-15).
 */
public class AfetBittiEvent extends Event {
    private final AfetTuru tur;
    private final int seviye;
    private final BlockPos merkez;
    private final ResourceKey<Level> boyut;
    private final Set<UUID> etkilenenler;
    private final Set<UUID> sagCikanlar;
    private final long baslangicSaati;
    private final long bitisSaati;
    private final BitisNedeni neden;
    private final boolean aktifeGirdi;
    private final boolean surecGeriAlindi;

    /** AKTIF'e girmiş, süreci geri alınmamış afetin bitişi (sentetik olaylar ve testler için). */
    public AfetBittiEvent(AfetTuru tur, int seviye, BlockPos merkez, ResourceKey<Level> boyut, Set<UUID> etkilenenler,
            Set<UUID> sagCikanlar, long baslangicSaati, long bitisSaati, BitisNedeni neden) {
        this(tur, seviye, merkez, boyut, etkilenenler, sagCikanlar, baslangicSaati, bitisSaati, neden, true, false);
    }

    /**
     * @param aktifeGirdi     afet bitmeden AKTIF aşamaya girmişti
     * @param surecGeriAlindi bitişte afetin tamamlanmamış dönem süreci geri alındı (K-C26: güneş patlamasının büyümesi)
     */
    public AfetBittiEvent(AfetTuru tur, int seviye, BlockPos merkez, ResourceKey<Level> boyut, Set<UUID> etkilenenler,
            Set<UUID> sagCikanlar, long baslangicSaati, long bitisSaati, BitisNedeni neden, boolean aktifeGirdi,
            boolean surecGeriAlindi) {
        this.tur = tur;
        this.seviye = seviye;
        this.merkez = merkez;
        this.boyut = boyut;
        this.etkilenenler = Set.copyOf(etkilenenler);
        this.sagCikanlar = Set.copyOf(sagCikanlar);
        this.baslangicSaati = baslangicSaati;
        this.bitisSaati = bitisSaati;
        this.neden = neden;
        this.aktifeGirdi = aktifeGirdi;
        this.surecGeriAlindi = surecGeriAlindi;
    }

    public AfetTuru tur() {
        return tur;
    }

    /** Seviye (1–5); seviyesiz afette 0. */
    public int seviye() {
        return seviye;
    }

    public BlockPos merkez() {
        return merkez;
    }

    public ResourceKey<Level> boyut() {
        return boyut;
    }

    public Set<UUID> etkilenenler() {
        return etkilenenler;
    }

    public Set<UUID> sagCikanlar() {
        return sagCikanlar;
    }

    public long baslangicSaati() {
        return baslangicSaati;
    }

    public long bitisSaati() {
        return bitisSaati;
    }

    public BitisNedeni neden() {
        return neden;
    }

    public boolean dogalBitti() {
        return neden == BitisNedeni.DOGAL;
    }

    public boolean aktifeGirdi() {
        return aktifeGirdi;
    }

    public boolean surecGeriAlindi() {
        return surecGeriAlindi;
    }

    /**
     * AS-13, AS-15: AKTIF'e girmiş afetin kendiliğinden bitişi ya da {@code /dogalafet stop} ile durdurulması başarımda sayılır
     * (komutla başlatma da). Kural, iptal ({@code sifirla} dahil) ve hata bitişi sayılmaz; stop afetin tamamlanmamış sürecini
     * geri aldıysa (güneş patlaması büyümesi) da sayılmaz.
     */
    public boolean basarimSayilir() {
        return aktifeGirdi && !surecGeriAlindi && (neden == BitisNedeni.DOGAL || neden == BitisNedeni.KOMUT);
    }
}

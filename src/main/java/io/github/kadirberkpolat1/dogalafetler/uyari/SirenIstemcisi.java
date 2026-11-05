package io.github.kadirberkpolat1.dogalafetler.uyari;

/** İstemcide sireni çalan taraf; uygulaması {@code client.uyari} paketindedir ve istemci kurulumunda bağlanır. */
public interface SirenIstemcisi {
    /** İstemci bağlanmadan gelen paket (olmamalı) sessizce yok sayılır. */
    SirenIstemcisi YOK = new SirenIstemcisi() {
        @Override
        public void baslat(SirenTuru tur, int sureTick) {
        }

        @Override
        public void durdur() {
        }
    };

    void baslat(SirenTuru tur, int sureTick);

    void durdur();
}

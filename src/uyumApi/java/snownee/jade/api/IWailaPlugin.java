package snownee.jade.api;

/** Jade API taslağı (jar'a girmez): Jade 11.x'in eklenti arayüzü, yalnız kullanılan üyeler. */
public interface IWailaPlugin {
    default void register(IWailaCommonRegistration registration) {
    }

    default void registerClient(IWailaClientRegistration registration) {
    }
}

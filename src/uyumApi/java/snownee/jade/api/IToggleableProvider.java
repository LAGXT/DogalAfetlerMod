package snownee.jade.api;

/** Jade API taslağı (jar'a girmez): kullanıcının açıp kapatabildiği sağlayıcı; {@code enabledByDefault} Jade 11.x ile aynı. */
public interface IToggleableProvider extends IJadeProvider {
    default boolean enabledByDefault() {
        return true;
    }
}

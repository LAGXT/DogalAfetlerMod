package snownee.jade.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Jade API taslağı (jar'a girmez): Jade eklenti sınıfı açıklaması, Jade 11.x ile aynı ({@code value} varsayılanı boş). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WailaPlugin {
    String value() default "";
}

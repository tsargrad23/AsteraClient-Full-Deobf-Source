package me.lyrica.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterModule {
    String name();
    String description() default "No description.";
    Module.Category category();
    boolean persistent() default false;
    boolean toggled() default false;
    boolean drawn() default true;
    int bind() default 0;
}

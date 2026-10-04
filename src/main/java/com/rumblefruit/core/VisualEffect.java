package com.rumblefruit.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marks pure presentation code (particle emission, cosmetic positioning):
// excluded from the mutation-testing scope, so the score reflects behavior
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface VisualEffect {
}

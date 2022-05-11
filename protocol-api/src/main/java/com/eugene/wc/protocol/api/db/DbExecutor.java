package com.eugene.wc.protocol.api.db;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Documented
@Qualifier
@Target({METHOD, PARAMETER, FIELD})
public @interface DbExecutor {
}

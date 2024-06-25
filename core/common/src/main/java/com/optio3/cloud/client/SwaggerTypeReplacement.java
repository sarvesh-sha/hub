/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * When applied to a Hibernate association, this controls what happens when the referenced entity is deleted.
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface SwaggerTypeReplacement
{
    Class<?> targetElement();

    Class<? extends Collection> targetCollection() default Collection.class;
}

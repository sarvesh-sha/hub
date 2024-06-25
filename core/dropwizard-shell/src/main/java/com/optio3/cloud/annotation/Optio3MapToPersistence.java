/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Control the mapping between Model classes and Persistence entities.
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD })
public @interface Optio3MapToPersistence
{
    /**
     * The name of the property on the Persistence class to map to this field.<br>
     * By default, it uses the name of the field to find the corresponding property.
     */
    String value();

    /**
     * Some collections might not have a setter.<br>
     * This flag controls that behavior.
     *
     * @return If true, the elements from the model's collection will be copied to the entity's collection, instead of setting a new collection
     */
    boolean useGetterForUpdate() default false;
}

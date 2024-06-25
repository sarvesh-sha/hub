/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({})
@Retention(RUNTIME)
public @interface SerializationScaling
{
    boolean assumeUnsigned() default (false);

    double preScalingOffset() default (0.0);

    double scalingFactor() default (1.0);

    double postScalingOffset() default (0.0);
}

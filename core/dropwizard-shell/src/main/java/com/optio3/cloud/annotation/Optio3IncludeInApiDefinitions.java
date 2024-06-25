/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Any type annotated with this will be included in the Swagger specification for the application.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Optio3IncludeInApiDefinitions
{
}

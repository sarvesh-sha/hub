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
 * Annotation used to inject the origin identifier for a Remote Procedure Call.
 * <p>
 * Usage example:
 *
 * <pre>
 * <code>
 * {@literal @}Optio3RemoteOrigin
 * protected String m_origin;
 * </code>
 * </pre>
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD })
public @interface Optio3RemoteOrigin
{
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.can;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(CanExtendedMessageTypes.class)
public @interface CanExtendedMessageType
{
    boolean littleEndian();

    int priority();

    boolean extendedDataPage() default false;

    boolean dataPage();

    int pduFormat();

    int destinationAddress();

    int sourceAddress();
}

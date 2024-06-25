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
 * Automatically register a MessageBus Channel, a class that derives from {@link com.optio3.cloud.messagebus.MessageBusChannelProvider}.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Optio3MessageBusChannel
{
    /**
     * The name of the MessageBus channel
     */
    String name();
}

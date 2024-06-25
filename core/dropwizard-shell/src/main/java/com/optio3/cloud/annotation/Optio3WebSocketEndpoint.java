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
 * Automatically register a WebSocket endpoint, a class that derives from {@link com.optio3.cloud.JsonWebSocket}.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Optio3WebSocketEndpoint
{
    /**
     * The name of the WebSocket
     */
    String name();

    /**
     * The display name of the WebSocket
     */
    String displayName() default "";

    /**
     * The URL patterns of the WebSocket
     */
    String[] urlPatterns();

    /**
     * The time in ms (milliseconds) that a websocket may be idle before closing.
     */
    long timeout() default 10000;
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.optio3.asyncawait.converter.KnownMethod;
import com.optio3.asyncawait.converter.KnownMethodId;

//
// Annotation injected by the AsyncTransformer to tag synthetic methods with information about their original source. 
//
@Retention(RUNTIME)
@Target(TYPE)
public @interface AsyncOrigin
{
    @KnownMethod(KnownMethodId.AsyncOrigin_type) Class<?> type();

    @KnownMethod(KnownMethodId.AsyncOrigin_method) String method();

    @KnownMethod(KnownMethodId.AsyncOrigin_signature) String signature();
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

public abstract class GenericMemberInfo
{
    public final TypeResolver typeResolver;

    protected GenericMemberInfo(TypeResolver typeResolver)
    {
        this.typeResolver = typeResolver;
    }
}

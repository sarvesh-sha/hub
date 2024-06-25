/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.Set;

public abstract class GenericFieldInfo extends GenericMemberInfo
{
    protected GenericFieldInfo(TypeResolver typeResolver)
    {
        super(typeResolver);
    }

    //--//

    public abstract GenericTypeInfo getDeclaringGenericType();

    public abstract String getName();

    public abstract Set<FieldAccess> getModifiers();

    public abstract GenericType getType();

    public abstract void addModifiers(FieldAccess... modifiers);

    public abstract void removeModifiers(FieldAccess... modifiers);

    //--//

    public boolean isStatic()
    {
        return getModifiers().contains(FieldAccess.Static);
    }

    //--//

    @Override
    public String toString()
    {
        return String.format("%s.%s(%s)",
                             getDeclaringGenericType().asType()
                                                      .getClassName(),
                             getName(),
                             getType());
    }
}

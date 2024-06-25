/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.Set;

public abstract class GenericMethodInfo extends GenericMemberInfo
{
    protected GenericMethodInfo(TypeResolver typeResolver)
    {
        super(typeResolver);
    }

    //--//

    public abstract GenericTypeInfo getDeclaringGenericType();

    public abstract String getName();

    public abstract Set<MethodAccess> getModifiers();

    public abstract GenericType.MethodDescriptor getSignature();

    public abstract void addModifiers(MethodAccess... modifiers);

    public abstract void removeModifiers(MethodAccess... modifiers);

    //--//

    public boolean hasCode()
    {
        Set<MethodAccess> access = getModifiers();

        if (access.contains(MethodAccess.Abstract))
        {
            return false;
        }

        if (access.contains(MethodAccess.Native))
        {
            return false;
        }

        return true;
    }

    public boolean isStatic()
    {
        return getModifiers().contains(MethodAccess.Static);
    }

    public boolean isConstructor()
    {
        return getName().equals("<init>");
    }

    //--//

    @Override
    public String toString()
    {
        return String.format("%s.%s%s",
                             getDeclaringGenericType().asType()
                                                      .getClassName(),
                             getName(),
                             getSignature());
    }
}

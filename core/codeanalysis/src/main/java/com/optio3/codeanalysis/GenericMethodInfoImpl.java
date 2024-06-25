/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.Collections;
import java.util.Set;

import com.optio3.codeanalysis.GenericType.MethodDescriptor;

class GenericMethodInfoImpl extends GenericMethodInfo
{
    private final GenericTypeInfo              m_declaringType;
    private final String                       m_name;
    private final Set<MethodAccess>            m_modifiers;
    private final GenericType.MethodDescriptor m_signature;

    GenericMethodInfoImpl(GenericTypeInfo declaringType,
                          String name,
                          Set<MethodAccess> modifiers,
                          GenericType.MethodDescriptor signature)
    {
        super(declaringType.typeResolver);

        this.m_declaringType = declaringType;
        this.m_name = name;
        this.m_modifiers = modifiers;
        this.m_signature = signature;
    }

    @Override
    public GenericTypeInfo getDeclaringGenericType()
    {
        return m_declaringType;
    }

    @Override
    public String getName()
    {
        return m_name;
    }

    @Override
    public Set<MethodAccess> getModifiers()
    {
        return Collections.unmodifiableSet(m_modifiers);
    }

    @Override
    public MethodDescriptor getSignature()
    {
        return m_signature;
    }

    @Override
    public void addModifiers(MethodAccess... modifiers)
    {
        for (MethodAccess modifier : modifiers)
            m_modifiers.add(modifier);
    }

    @Override
    public void removeModifiers(MethodAccess... modifiers)
    {
        for (MethodAccess modifier : modifiers)
            m_modifiers.remove(modifier);
    }
}

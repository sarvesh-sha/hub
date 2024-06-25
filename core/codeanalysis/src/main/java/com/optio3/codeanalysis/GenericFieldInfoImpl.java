/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.Collections;
import java.util.Set;

class GenericFieldInfoImpl extends GenericFieldInfo
{
    private final GenericTypeInfo  m_declaringType;
    private final String           m_name;
    private final Set<FieldAccess> m_modifiers;
    private final GenericType      m_signature;

    GenericFieldInfoImpl(GenericTypeInfo declaringType,
                         String name,
                         Set<FieldAccess> modifiers,
                         GenericType signature)
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
    public Set<FieldAccess> getModifiers()
    {
        return Collections.unmodifiableSet(m_modifiers);
    }

    @Override
    public GenericType getType()
    {
        return m_signature;
    }

    @Override
    public void addModifiers(FieldAccess... modifiers)
    {
        for (FieldAccess modifier : modifiers)
            m_modifiers.add(modifier);
    }

    @Override
    public void removeModifiers(FieldAccess... modifiers)
    {
        for (FieldAccess modifier : modifiers)
            m_modifiers.remove(modifier);
    }
}

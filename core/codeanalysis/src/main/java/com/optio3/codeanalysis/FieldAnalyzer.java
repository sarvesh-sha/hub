/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import org.objectweb.asm.tree.FieldNode;

public class FieldAnalyzer
{
    public final ClassAnalyzer       declaringClass;
    public final GenericFieldInfo    genericFieldInfo;
    public final AnnotationsAnalyzer annotations;

    private FieldNode m_fieldNode;
    private boolean   m_modified;

    FieldAnalyzer(ClassAnalyzer ca,
                  GenericFieldInfo gfi,
                  FieldNode fn,
                  AnnotationsAnalyzer annotations)
    {
        declaringClass = ca;
        genericFieldInfo = gfi;

        m_fieldNode = fn;
        this.annotations = annotations;
    }

    //--//

    public FieldNode getFieldNode()
    {
        return m_fieldNode;
    }

    public boolean isStatic()
    {
        return genericFieldInfo.isStatic();
    }

    public String getName()
    {
        return genericFieldInfo.getName();
    }

    public GenericType getType()
    {
        return genericFieldInfo.getType();
    }

    public boolean wasModified()
    {
        return m_modified;
    }

    void setModified()
    {
        m_modified = true;

        declaringClass.setModified();
    }

    //--//

    public void addModifiers(FieldAccess... modifiers)
    {
        genericFieldInfo.removeModifiers(modifiers);

        flushModifiers();
    }

    public void removeModifiers(FieldAccess... modifiers)
    {
        genericFieldInfo.removeModifiers(modifiers);

        flushModifiers();
    }

    private void flushModifiers()
    {
        m_fieldNode.access = FieldAccess.toValue(genericFieldInfo.getModifiers());

        setModified();
    }

    //--//

    @Override
    public String toString()
    {
        return String.format("%s %s.%s %s", genericFieldInfo.getModifiers(), declaringClass.getInternalName(), getName(), getType());
    }
}

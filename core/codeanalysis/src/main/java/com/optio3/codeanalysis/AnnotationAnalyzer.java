/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import org.objectweb.asm.tree.AnnotationNode;

public class AnnotationAnalyzer
{
    public final ClassAnalyzer                   declaringClass;
    public final GenericType.GenericMethodOrType context;

    private GenericType    m_type;
    private AnnotationNode m_annoNode;
    private boolean        m_modified;

    AnnotationAnalyzer(ClassAnalyzer ca,
                       GenericType.GenericMethodOrType context,
                       GenericType type,
                       AnnotationNode an)
    {
        declaringClass = ca;
        this.context = context;

        m_type = type;
        m_annoNode = an;
    }

    //--//

    public AnnotationNode getAnnotationNode()
    {
        return m_annoNode;
    }

    public GenericType getType()
    {
        return m_type;
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

    @Override
    public String toString()
    {
        return String.format("%s @%s", declaringClass.getInternalName(), getType());
    }
}

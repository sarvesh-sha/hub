/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import com.optio3.codeanalysis.GenericType;
import org.objectweb.asm.Type;

public class SourceCodeVariable
{
    public final LocalVariable storage;

    public final String name;

    public final GenericType signature;

    SourceCodeVariable(LocalVariable storge,
                       String name,
                       GenericType signature)
    {
        this.storage = storge;
        this.name = name;
        this.signature = signature;
    }

    public Integer getIndex()
    {
        return storage.getIndex();
    }

    public Type getType()
    {
        return storage.type;
    }

    //--//

    @Override
    public final boolean equals(Object obj)
    {
        //
        // NOTE: We rely on Referential Identity for SourceCodeVariables.
        //
        return this == obj;
    }

    public boolean matches(SourceCodeVariable scv)
    {
        if (this == scv)
        {
            return true;
        }

        if (!name.equals(scv.name))
        {
            return false;
        }

        if (!storage.equals(scv.storage))
        {
            return false;
        }

        if (!signature.equals(scv.signature))
        {
            return false;
        }

        return true;
    }

    public boolean matches(LocalVariable obj)
    {
        return storage.equals(obj);
    }

    @Override
    public String toString()
    {
        return String.format("%d (%s: %s)", getIndex(), signature, name);
    }
}

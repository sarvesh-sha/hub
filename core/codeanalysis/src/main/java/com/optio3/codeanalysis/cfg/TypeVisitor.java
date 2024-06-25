/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import org.objectweb.asm.Type;

public abstract class TypeVisitor
{
    public abstract void visitType(Type type);
}

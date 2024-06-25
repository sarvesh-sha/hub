/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;

public abstract class MetadataInstruction extends AbstractInstruction
{
    protected MetadataInstruction()
    {
        super(-1);
    }

    @Override
    public int popSize()
    {
        return 0;
    }

    @Override
    public int pushSize()
    {
        return 0;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input)
    {
        return input;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
    }
}

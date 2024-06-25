/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.converter;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.codeanalysis.FieldAnalyzer;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.LocalVariable;

class ContinuationStateDescriptor
{
    static class AnnotatedField
    {
        final FieldAnalyzer storageField;
        final FrameValue    frameValue;

        AnnotatedField(FieldAnalyzer storageField,
                       FrameValue frameValue)
        {
            this.storageField = storageField;
            this.frameValue = frameValue;
        }
    }

    boolean hasTimeout;
    boolean dontUnwrapException;

    // These fields belong to the StateMachine subclass.
    final Map<LocalVariable, ContinuationStateDescriptor.AnnotatedField> locals       = Maps.newHashMap();
    final List<LocalVariable>                                            localsSorted = Lists.newArrayList();

    // These fields belong to the RestorePoint subclass.
    final List<ContinuationStateDescriptor.AnnotatedField> stack = Lists.newArrayList();

    int stateId;

    FrameState frame;
    FrameValue future;
    FrameValue timeout;
    FrameValue timeUnits;

    BasicBlock savePoint;
    BasicBlock restorePoint;
    BasicBlock restartPointFromRP;
    BasicBlock restartPoint;

    public void sortLocals()
    {
        localsSorted.clear();
        localsSorted.addAll(locals.keySet());
        localsSorted.sort((v1, v2) -> v1.getIndex() - v2.getIndex());
    }

    public void addLocal(LocalVariable localVar,
                         FieldAnalyzer fa,
                         FrameValue fv)
    {
        locals.put(localVar, new AnnotatedField(fa, fv));
    }

    public void addStack(FieldAnalyzer fa,
                         FrameValue fv)
    {
        stack.add(new AnnotatedField(fa, fv));
    }

    public boolean hasAnyUninitializedValuesOnStack()
    {
        for (ContinuationStateDescriptor.AnnotatedField stack : stack)
        {
            if (stack.frameValue.isUninitialized())
            {
                return true;
            }
        }

        return false;
    }
}

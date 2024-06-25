/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.ConsumerWithException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = EngineStatement.class), @JsonSubTypes.Type(value = EngineExpression.class), @JsonSubTypes.Type(value = EngineComputation.class) })
@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
public abstract class EngineBlock
{
    public static class ScratchPad
    {
        public int stateMachine;
    }

    public String id;

    public int x;

    public int y;

    public abstract void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                          EngineExecutionStack stack) throws
                                                                      Exception;

    public boolean handleLoopRequest(EngineExecutionContext<?, ?> ctx,
                                     EngineExecutionStack stack,
                                     boolean shouldBreak)
    {
        //
        // The default behavior of a block is to exit the loop.
        // Only loop blocks will override the default behavior to implement the correct semantics.
        //
        ctx.popBlock();
        return false;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", getClass().getSimpleName(), id);
    }

    //--//

    protected <B1 extends EngineBlock, T1 extends EngineValue> void extractParams(EngineExecutionContext<?, ?> ctx,
                                                                                  EngineExecutionStack stack,
                                                                                  B1 block1,
                                                                                  Class<T1> clz1,
                                                                                  ConsumerWithException<T1> callback) throws
                                                                                                                      Exception
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        switch (scratchPad.stateMachine)
        {
            case 0:
            {
                ctx.pushBlock(block1);
                scratchPad.stateMachine = 1;
            }
            return;

            case 1:
            {
                T1 val1 = stack.popChildResult(clz1);

                callback.accept(val1);
            }
            return;
        }

        throw stack.unexpected();
    }

    protected <B1 extends EngineBlock, T1 extends EngineValue, B2 extends EngineBlock, T2 extends EngineValue> void extractParams(EngineExecutionContext<?, ?> ctx,
                                                                                                                                  EngineExecutionStack stack,
                                                                                                                                  B1 block1,
                                                                                                                                  Class<T1> clz1,
                                                                                                                                  B2 block2,
                                                                                                                                  Class<T2> clz2,
                                                                                                                                  BiConsumerWithException<T1, T2> callback) throws
                                                                                                                                                                            Exception
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        switch (scratchPad.stateMachine)
        {
            case 0:
            {
                ctx.pushBlock(block1);
                scratchPad.stateMachine = 1;
            }
            return;

            case 1:
            {
                ctx.pushBlock(block2);
                scratchPad.stateMachine = 2;
            }
            return;

            case 2:
            {
                T1 val1 = stack.popChildResult(clz1);
                T2 val2 = stack.popChildResult(clz2);

                callback.accept(val1, val2);
            }
            return;
        }

        throw stack.unexpected();
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V>
    {
        void accept(T t,
                    U u,
                    V v);
    }

    protected <B1 extends EngineBlock, T1 extends EngineValue, B2 extends EngineBlock, T2 extends EngineValue, B3 extends EngineBlock, T3 extends EngineValue> void extractParams(EngineExecutionContext<?, ?> ctx,
                                                                                                                                                                                  EngineExecutionStack stack,
                                                                                                                                                                                  B1 block1,
                                                                                                                                                                                  Class<T1> clz1,
                                                                                                                                                                                  B2 block2,
                                                                                                                                                                                  Class<T2> clz2,
                                                                                                                                                                                  B3 block3,
                                                                                                                                                                                  Class<T3> clz3,
                                                                                                                                                                                  TriConsumer<T1, T2, T3> callback)
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        switch (scratchPad.stateMachine)
        {
            case 0:
            {
                ctx.pushBlock(block1);
                scratchPad.stateMachine = 1;
            }
            return;

            case 1:
            {
                ctx.pushBlock(block2);
                scratchPad.stateMachine = 2;
            }
            return;

            case 2:
            {
                ctx.pushBlock(block3);
                scratchPad.stateMachine = 3;
            }
            return;

            case 3:
            {
                T1 val1 = stack.popChildResult(clz1);
                T2 val2 = stack.popChildResult(clz2);
                T3 val3 = stack.popChildResult(clz3);

                callback.accept(val1, val2, val3);
            }
            return;
        }

        throw stack.unexpected();
    }
}

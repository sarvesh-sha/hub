/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;

public class Batcher
{
    public static <I, O> List<O> splitInBatches(List<I> inputList,
                                                int inputOffset,
                                                int inputCount,
                                                int batchSize,
                                                Function<List<I>, List<O>> callback)
    {
        List<O> outputList = Lists.newArrayList();
        int     inputEnd   = Math.min(inputOffset + inputCount, inputList.size());

        for (int cursor = inputOffset; cursor < inputEnd; cursor += batchSize)
        {
            int     end   = Math.min(inputEnd, cursor + batchSize);
            List<I> slice = inputList.subList(cursor, end);
            outputList.addAll(callback.apply(slice));
        }

        return outputList;
    }
}

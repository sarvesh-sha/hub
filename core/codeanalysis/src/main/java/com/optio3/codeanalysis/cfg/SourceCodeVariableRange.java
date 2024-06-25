/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import com.google.common.base.Preconditions;

public class SourceCodeVariableRange
{
    public final SourceCodeVariable target;
    public       int                rangeStart;
    public       int                rangeEnd;

    public SourceCodeVariableRange(SourceCodeVariable target)
    {
        this(target, -1, -1);
    }

    public SourceCodeVariableRange(SourceCodeVariable target,
                                   int rangeStart,
                                   int rangeEnd)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkArgument(rangeStart == -1 || rangeStart >= 0);
        Preconditions.checkArgument(rangeEnd == -1 || (rangeEnd > 0 && rangeStart < rangeEnd));

        this.target = target;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public boolean isStartOfRangeLessThan(int offset)
    {
        return rangeStart < 0 || rangeStart < offset;
    }

    public boolean isStartOfRangeEqualTo(int offset)
    {
        return rangeStart == offset;
    }

    public boolean isStartOfRangeLessThanOrEqualTo(int offset)
    {
        return isStartOfRangeLessThan(offset) || isStartOfRangeEqualTo(offset);
    }

    public boolean isEndOfRangeGreaterThan(int offset)
    {
        return rangeEnd < 0 || offset < rangeEnd;
    }

    public boolean isEndOfRangeEqualTo(int offset)
    {
        return offset == rangeEnd;
    }

    public boolean isEndOfRangeGreaterThanOrEqualTo(int offset)
    {
        return isEndOfRangeGreaterThan(offset) || isEndOfRangeEqualTo(offset);
    }

    public boolean isAliveAt(int offset)
    {
        if (!isStartOfRangeLessThanOrEqualTo(offset))
        {
            return false;
        }

        if (!isEndOfRangeGreaterThan(offset))
        {
            return false;
        }

        return true;
    }

    public SourceCodeVariableRange getRangeBeforeSplit(int splitPoint)
    {
        if (!isStartOfRangeLessThanOrEqualTo(splitPoint))
        {
            // Variable not alive before the split.
            return null;
        }

        if (!isEndOfRangeGreaterThan(splitPoint))
        {
            // Variable is alive before the split, with same range.
            return this;
        }

        //
        // Variable is alive past the end of the range, or past the split point.
        // Create a new limited range.
        //
        return new SourceCodeVariableRange(target, rangeStart, splitPoint);
    }

    public SourceCodeVariableRange getRangeAfterSplit(int splitPoint)
    {
        if (!isEndOfRangeGreaterThan(splitPoint))
        {
            // Variable not alive after the split.
            return null;
        }

        if (!isStartOfRangeLessThanOrEqualTo(splitPoint))
        {
            // Variable is alive after the split, with same range.
            return this;
        }

        //
        // Variable is alive past the end of the range, or past the split point.
        // Create a new limited range.
        //
        return new SourceCodeVariableRange(target, splitPoint, rangeEnd);
    }
}

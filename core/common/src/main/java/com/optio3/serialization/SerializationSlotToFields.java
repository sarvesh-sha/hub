/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

public class SerializationSlotToFields
{
    static class MyComparator implements Comparator<SerializationSlotToFields>
    {
        static final Comparator<SerializationSlotToFields> c_instance = new MyComparator();

        @Override
        public int compare(SerializationSlotToFields o1,
                           SerializationSlotToFields o2)
        {
            return o1.sequence - o2.sequence;
        }
    }

    public final int                     sequence;
    public final SerializablePiece       normalPiece;
    public final List<SerializablePiece> bitfieldPieces;

    SerializationSlotToFields(int seq,
                              Collection<SerializablePiece> collection)
    {
        sequence = seq;

        boolean                 isBitField = false;
        List<SerializablePiece> list       = Lists.newArrayList(collection);
        for (SerializablePiece piece : list)
        {
            if (piece.bitOffset >= 0)
            {
                isBitField = true;
            }
        }

        if (list.size() > 1)
        {
            isBitField = true;
        }

        if (isBitField)
        {
            for (SerializablePiece piece : list)
            {
                piece.validateForBitfield();
            }

            list.sort(SerializablePiece.MyComparator.c_instance);

            normalPiece = null;
            bitfieldPieces = Collections.unmodifiableList(list);
        }
        else
        {
            normalPiece = list.get(0);
            bitfieldPieces = Collections.emptyList();
        }
    }

    public boolean isOptional()
    {
        if (normalPiece != null)
        {
            return normalPiece.isOptional;
        }

        for (SerializablePiece piece : bitfieldPieces)
        {
            if (!piece.isOptional)
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("[Sequence=%d", sequence));

        if (normalPiece != null)
        {
            sb.append(String.format(", NormalPiece=%s", normalPiece));
        }
        else
        {
            sb.append(String.format(", bitFieldPieces=["));

            boolean got = false;
            for (SerializablePiece piece : bitfieldPieces)
            {
                if (got)
                {
                    sb.append(", ");
                }

                sb.append(piece.toString());
                got = true;
            }

            sb.append("]");
        }

        sb.append("]");
        return sb.toString();
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.IOException;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.serialization.ObjectMappers;

@MappedSuperclass
public abstract class CommonLogRecord extends RecordWithCommonFields
{
    @Lob
    @Column(name = "block", nullable = false)
    private byte[] block;

    @Optio3UpgradeValue("0")
    @Column(name = "sequence_start", nullable = false)
    private int sequenceStart;

    @Optio3UpgradeValue("0")
    @Column(name = "sequence_end", nullable = false)
    private int sequenceEnd;

    //--//

    protected CommonLogRecord()
    {
    }

    @Override
    public void onEviction()
    {
        super.onEviction();

        block = null;
    }

    //--//

    public LogBlock decodeBlock(ObjectMapper mapper) throws
                                                     IOException
    {
        LogBlock res;

        if (block[0] == '{') // Backward compatibility for when we stored log entries as uncompressed JSON.
        {
            res = mapper.readValue(block, LogBlock.class);
        }
        else
        {
            res = ObjectMappers.deserializeFromGzip(mapper, block, LogBlock.class);
        }

        if (res != null)
        {
            res.fixup();
        }

        return res;
    }

    public boolean encodeBlock(ObjectMapper mapper,
                               LogBlock block)
    {
        byte[] compressedBlock = ObjectMappers.serializeToGzip(mapper, block);
        int    len             = compressedBlock.length;

        //
        // Instead of increasing the size of the record with every update, do it in a chunked way, by padding the buffer.
        //
        int roundedLength = 3 * 1024;
        while (roundedLength < len)
        {
            roundedLength += 4 * 1024;
        }

        byte[] newBlock = Arrays.copyOf(compressedBlock, roundedLength);

        boolean modified = !Arrays.equals(this.block, newBlock);
        if (modified)
        {
            this.block = newBlock;
        }

        return modified;
    }

    public int getSequenceStart()
    {
        return sequenceStart;
    }

    public void setSequenceStart(int sequenceStart)
    {
        this.sequenceStart = sequenceStart;
    }

    public int getSequenceEnd()
    {
        return sequenceEnd;
    }

    public void setSequenceEnd(int sequenceEnd)
    {
        this.sequenceEnd = sequenceEnd;
    }
}

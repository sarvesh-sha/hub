/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class DockerLayerChunks
{
    public byte[] encoded;

    public List<DockerLayerChunkCompact> decode()
    {
        try (InputBuffer ib = InputBuffer.createFrom(encoded))
        {
            int num = ib.read4BytesSigned();

            List<DockerLayerChunkCompact> chunks = Lists.newArrayListWithCapacity(num);
            for (int i = 0; i < num; i++)
            {
                chunks.add(new DockerLayerChunkCompact(ib));
            }

            return chunks;
        }
    }

    public static DockerLayerChunks build(List<DockerLayerChunkCompact> chunks)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            ob.emit4Bytes(chunks.size());
            for (DockerLayerChunkCompact chunk : chunks)
            {
                chunk.emit(ob);
            }

            DockerLayerChunks res = new DockerLayerChunks();
            res.encoded = ob.toByteArray();
            return res;
        }
    }
}

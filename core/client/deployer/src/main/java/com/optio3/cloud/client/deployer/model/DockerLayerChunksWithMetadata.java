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

public class DockerLayerChunksWithMetadata
{
    public byte[] encoded;

    public List<DockerLayerChunkCompactWithMetadata> decode()
    {
        try (InputBuffer ib = InputBuffer.createFrom(encoded))
        {
            int num = ib.read4BytesSigned();

            List<DockerLayerChunkCompactWithMetadata> chunks = Lists.newArrayListWithCapacity(num);
            for (int i = 0; i < num; i++)
            {
                chunks.add(new DockerLayerChunkCompactWithMetadata(ib));
            }

            return chunks;
        }
    }

    public static DockerLayerChunksWithMetadata build(List<DockerLayerChunkCompactWithMetadata> chunks)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            ob.emit4Bytes(chunks.size());
            for (DockerLayerChunkCompactWithMetadata chunk : chunks)
            {
                chunk.emit(ob);
            }

            DockerLayerChunksWithMetadata res = new DockerLayerChunksWithMetadata();
            res.encoded = ob.toByteArray();
            return res;
        }
    }
}

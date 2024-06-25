/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Encryption;

public class DockerLayerChunkCompactWithMetadata
{
    private static final long c_mask_Size            = 0x0FFF_FFFF;
    private static final long c_mask_HasSubStructure = 0x8000_0000;

    public final Encryption.Sha1Hash hash;
    public final long                size;
    public final boolean             hasSubstructure;

    //--//

    public DockerLayerChunkCompactWithMetadata(Encryption.Sha1Hash hash,
                                               long size,
                                               boolean hasSubstructure)
    {
        this.hash = hash;
        this.size = size;
        this.hasSubstructure = hasSubstructure;
    }

    public DockerLayerChunkCompactWithMetadata(InputBuffer ib)
    {
        hash = new Encryption.Sha1Hash(ib);

        long word = ib.read4BytesUnsigned();

        this.hasSubstructure = (word & c_mask_HasSubStructure) != 0;
        this.size = word & c_mask_Size;
    }

    public void emit(OutputBuffer ob)
    {
        hash.emit(ob);

        long word = size & c_mask_Size;

        if (hasSubstructure)
        {
            word |= c_mask_HasSubStructure;
        }

        ob.emit4Bytes((int) word);
    }
}

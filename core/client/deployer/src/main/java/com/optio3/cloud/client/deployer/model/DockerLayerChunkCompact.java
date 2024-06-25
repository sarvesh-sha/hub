/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Encryption;

public class DockerLayerChunkCompact
{
    public final Encryption.Sha1Hash hash;
    public final long                size;

    //--//

    public DockerLayerChunkCompact(Encryption.Sha1Hash hash,
                                   long size)
    {
        this.hash = hash;
        this.size = size;
    }

    public DockerLayerChunkCompact(InputBuffer ib)
    {
        hash = new Encryption.Sha1Hash(ib);
        size = ib.read8BytesSigned();
    }

    public void emit(OutputBuffer ob)
    {
        hash.emit(ob);
        ob.emit8Bytes(size);
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

public class DockerCompressedLayerDescription
{
    public byte[] version;
    public byte[] json;
    public long   size;
    public long   sizeCompressed;
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.directory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SshKey
{
    public String   user;
    public FileInfo publicKeyFile;
    public FileInfo privateKeyFile;
    public String   passphrase;

    //--//

    @JsonIgnore
    CredentialDirectory credDir;

    //--//

    @JsonIgnore
    public byte[] getPublicKey()
    {
        return credDir.getContents(publicKeyFile);
    }

    @JsonIgnore
    public byte[] getPrivateKey() throws
                                  Exception
    {
        return credDir.decrypt(privateKeyFile, passphrase);
    }

    @JsonIgnore
    public byte[] getRawPrivateKey()
    {
        return credDir.getContents(privateKeyFile);
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.Base64;

import javax.persistence.Embeddable;
import javax.persistence.Lob;

import com.optio3.util.Encryption;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Embeddable
public class EncryptedPayload
{
    @Lob
    private byte[] contents;

    public EncryptedPayload()
    {
    }

    public static EncryptedPayload build(String key,
                                         String value) throws
                                                       Exception
    {
        EncryptedPayload ep = new EncryptedPayload();
        ep.encrypt(key, value);
        return ep;
    }

    public static EncryptedPayload decodeFromBase64(String value)
    {
        EncryptedPayload ep = new EncryptedPayload();
        ep.contents = Base64.getDecoder()
                            .decode(value);
        return ep;
    }

    public String encodeAsBase64()
    {
        return Base64.getEncoder()
                     .encodeToString(contents);
    }

    //--//

    public void encrypt(String key,
                        String value) throws
                                      Exception
    {
        contents = Encryption.wrapFromText(value, (input, output) ->
        {
            if (StringUtils.isEmpty(key)) // Special case for no encryption.
            {
                IOUtils.copy(input, output);
            }
            else
            {
                Encryption.AES128.encrypt(key, input, output);
            }
        });
    }

    public String decrypt(String key) throws
                                      Exception
    {
        return Encryption.unwrapToText(contents, (input, output) ->
        {
            if (StringUtils.isEmpty(key)) // Special case for no encryption.
            {
                IOUtils.copy(input, output);
            }
            else
            {
                Encryption.AES128.decrypt(key, input, output);
            }
        });
    }

    //--//

    public byte[] getContents()
    {
        return contents;
    }

    public void setContents(byte[] contents)
    {
        this.contents = contents;
    }
}

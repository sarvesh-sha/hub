/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;

import com.optio3.test.common.Optio3Test;
import com.optio3.util.Encryption;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class EncryptionTest extends Optio3Test
{
    @Test
    public void testEncryption() throws
                                 Exception
    {
        String key = "this is a test";

        String                clearText = "another test";
        ByteArrayInputStream  input     = new ByteArrayInputStream(clearText.getBytes());
        ByteArrayOutputStream output    = new ByteArrayOutputStream();

        Encryption.AES128.encrypt(key, input, output);

        ByteArrayInputStream  input2  = new ByteArrayInputStream(output.toByteArray());
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        Encryption.AES128.decrypt(key, input2, output2);

        String clearText2 = new String(output2.toByteArray());

        assertEquals(clearText, clearText2);
    }

    @Test
    public void testEncryptionLong() throws
                                     Exception
    {
        String key = Encryption.generateRandomKeyAsBase64();

        SecureRandom rnd = new SecureRandom();
        int          len = 100 + rnd.nextInt(9999);

        byte[] data = new byte[len];
        rnd.nextBytes(data);

        ByteArrayInputStream  input  = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Encryption.AES128.encrypt(key, input, output);

        ByteArrayInputStream  input2  = new ByteArrayInputStream(output.toByteArray());
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        Encryption.AES128.decrypt(key, input2, output2);

        assertArrayEquals(data, output2.toByteArray());
    }

    @Test
    public void testEncryptionStream() throws
                                       Exception
    {
        String key = Encryption.generateRandomKeyAsBase64();

        SecureRandom rnd = new SecureRandom();
        int          len = 1093;

        byte[] data = new byte[len];
        rnd.nextBytes(data);

        ByteArrayInputStream  input  = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OutputStream encryptingStream = Encryption.AES128.encryptingStream(key, output))
        {
            IOUtils.copyLarge(input, encryptingStream);
        }

        ByteArrayInputStream  input2  = new ByteArrayInputStream(output.toByteArray());
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        try (CipherInputStream decryptingStream = Encryption.AES128.decryptingStream(key, input2))
        {
            IOUtils.copyLarge(decryptingStream, output2);
        }

        assertArrayEquals(data, output2.toByteArray());
    }

    @Test
    public void testAES() throws
                          Exception
    {
        SecretKey key = Encryption.AES128SingleBlock.prepareKey(Encryption.generateRandomValues(Encryption.AES128SingleBlock.BLOCKSIZE));

        byte[] data          = Encryption.generateRandomValues(Encryption.AES128SingleBlock.BLOCKSIZE);
        byte[] dataEncrypted = new byte[Encryption.AES128SingleBlock.BLOCKSIZE];
        byte[] dataDecrypted = new byte[Encryption.AES128SingleBlock.BLOCKSIZE];

        Encryption.AES128SingleBlock.Encrypt encrypt = new Encryption.AES128SingleBlock.Encrypt(key);
        encrypt.exec(data, 0, dataEncrypted, 0);

        Encryption.AES128SingleBlock.Decrypt decrypt = new Encryption.AES128SingleBlock.Decrypt(key);
        decrypt.exec(dataEncrypted, 0, dataDecrypted, 0);

        assertArrayEquals(data, dataDecrypted);
    }
}

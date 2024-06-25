/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.persistence.Embeddable;

import com.optio3.util.Encryption;

@Embeddable
public class HashedPassword
{
    private byte[] salt;
    private byte[] hash;

    public HashedPassword()
    {
    }

    public HashedPassword(String rawValue)
    {
        salt = new SecureRandom().generateSeed(16);
        hash = hash(rawValue);
    }

    public HashedPassword(byte[] salt,
                          byte[] hash)
    {
        this.salt = Arrays.copyOf(salt, salt.length);
        this.hash = Arrays.copyOf(hash, hash.length);
    }

    public byte[] getSalt()
    {
        return Arrays.copyOf(salt, salt.length);
    }

    public byte[] getHash()
    {
        return Arrays.copyOf(hash, hash.length);
    }

    public boolean authenticate(String value)
    {
        byte[] newHash = hash(value);

        return Arrays.equals(hash, newHash);
    }

    private byte[] hash(String value)
    {
        try
        {
            return Encryption.PBKDF2WithHmacSHA256.encryptPassword(value, salt, 1000, 32);
        }
        catch (GeneralSecurityException e)
        {
            throw new RuntimeException(e);
        }
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Charsets;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.function.BiConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public class Encryption
{
    public static final class Sha1Hash
    {
        public final int word1;
        public final int word2;
        public final int word3;
        public final int word4;
        public final int word5;

        //--//

        @JsonCreator
        public Sha1Hash(String hexText)
        {
            word1 = BufferUtils.convertFromHex32(hexText, 0 * 8);
            word2 = BufferUtils.convertFromHex32(hexText, 1 * 8);
            word3 = BufferUtils.convertFromHex32(hexText, 2 * 8);
            word4 = BufferUtils.convertFromHex32(hexText, 3 * 8);
            word5 = BufferUtils.convertFromHex32(hexText, 4 * 8);
        }

        public Sha1Hash(byte[] digest)
        {
            try (InputBuffer ib = InputBuffer.createFrom(digest))
            {
                word1 = ib.read4BytesSigned();
                word2 = ib.read4BytesSigned();
                word3 = ib.read4BytesSigned();
                word4 = ib.read4BytesSigned();
                word5 = ib.read4BytesSigned();
            }
        }

        public Sha1Hash(InputBuffer ib)
        {
            word1 = ib.read4BytesSigned();
            word2 = ib.read4BytesSigned();
            word3 = ib.read4BytesSigned();
            word4 = ib.read4BytesSigned();
            word5 = ib.read4BytesSigned();
        }

        public void emit(OutputBuffer ob)
        {
            ob.emit4Bytes(word1);
            ob.emit4Bytes(word2);
            ob.emit4Bytes(word3);
            ob.emit4Bytes(word4);
            ob.emit4Bytes(word5);
        }

        public byte[] toByteArray()
        {
            try (OutputBuffer ob = new OutputBuffer())
            {
                emit(ob);
                return ob.toByteArray();
            }
        }

        @Override
        public boolean equals(Object o)
        {
            Sha1Hash that = Reflection.as(o, Sha1Hash.class);
            if (that == null)
            {
                return false;
            }

            return word1 == that.word1 && word2 == that.word2 && word3 == that.word3 && word4 == that.word4 && word5 == that.word5;
        }

        @Override
        public int hashCode()
        {
            return Long.hashCode(word1);
        }

        @JsonValue
        @Override
        public String toString()
        {
            byte[] buf = toByteArray();

            return BufferUtils.convertToHex(buf, 0, buf.length, 0, false, false);
        }
    }

    public static class AES128
    {
        public static CipherOutputStream encryptingStream(String key,
                                                          OutputStream encryptedStream) throws
                                                                                        GeneralSecurityException,
                                                                                        IOException
        {
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] ivData = new byte[aes.getBlockSize()];
            new SecureRandom().nextBytes(ivData);

            encryptedStream.write(ivData);

            aes.init(Cipher.ENCRYPT_MODE, importKey(key, aes), new IvParameterSpec(ivData));
            return new CipherOutputStream(encryptedStream, aes);
        }

        public static CipherInputStream decryptingStream(String key,
                                                         InputStream encryptedStream) throws
                                                                                      GeneralSecurityException,
                                                                                      IOException
        {
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] ivData = new byte[aes.getBlockSize()];
            //noinspection ResultOfMethodCallIgnored
            encryptedStream.read(ivData); // Skip the salt part.

            aes.init(Cipher.DECRYPT_MODE, importKey(key, aes), new IvParameterSpec(ivData));

            return new CipherInputStream(encryptedStream, aes);
        }

        public static void encrypt(String key,
                                   InputStream clearTextStream,
                                   OutputStream encryptedStream) throws
                                                                 GeneralSecurityException,
                                                                 IOException
        {
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] ivData = new byte[aes.getBlockSize()];
            new SecureRandom().nextBytes(ivData);

            encryptedStream.write(ivData);

            aes.init(Cipher.ENCRYPT_MODE, importKey(key, aes), new IvParameterSpec(ivData));
            copy(aes, clearTextStream, encryptedStream);
        }

        public static void decrypt(String key,
                                   InputStream encryptedStream,
                                   OutputStream clearTextStream) throws
                                                                 GeneralSecurityException,
                                                                 IOException
        {
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] ivData = new byte[aes.getBlockSize()];
            //noinspection ResultOfMethodCallIgnored
            encryptedStream.read(ivData); // Skip the salt part.

            aes.init(Cipher.DECRYPT_MODE, importKey(key, aes), new IvParameterSpec(ivData));
            copy(aes, encryptedStream, clearTextStream);
        }

        private static void copy(Cipher aes,
                                 InputStream input,
                                 OutputStream output) throws
                                                      GeneralSecurityException,
                                                      IOException
        {
            byte[] bufIn  = new byte[1024];
            byte[] bufOut = new byte[aes.getOutputSize(bufIn.length)];

            while (true)
            {
                int read = input.read(bufIn);
                if (read < 0)
                {
                    break;
                }

                int written = aes.update(bufIn, 0, read, bufOut);
                output.write(bufOut, 0, written);
            }

            // Flush the rest of the cipher.
            int written = aes.doFinal(bufOut, 0);
            output.write(bufOut, 0, written);
        }

        private static SecretKey importKey(String key,
                                           Cipher cipher) throws
                                                          GeneralSecurityException
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(key.getBytes());
            byte[] digest          = md.digest();
            byte[] truncatedDigest = Arrays.copyOf(digest, cipher.getBlockSize());

            return new SecretKeySpec(truncatedDigest, "AES");
        }
    }

    public static class AES128SingleBlock
    {
        public static final int BLOCKSIZE = 16;

        public static SecretKey prepareKey(byte[] key)
        {
            return new SecretKeySpec(key, "AES");
        }

        public static class Encrypt
        {
            private final Cipher m_aes;

            public Encrypt(SecretKey key)
            {
                try
                {
                    m_aes = Cipher.getInstance("AES/ECB/NoPadding");
                    m_aes.init(Cipher.ENCRYPT_MODE, key);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            public void exec(byte[] input,
                             int inputOffset,
                             byte[] output,
                             int outputOffset) throws
                                               Exception
            {
                m_aes.doFinal(input, inputOffset, BLOCKSIZE, output, outputOffset);
            }
        }

        public static class Decrypt
        {
            private final Cipher m_aes;

            public Decrypt(SecretKey key)
            {
                try
                {
                    m_aes = Cipher.getInstance("AES/ECB/NoPadding");
                    m_aes.init(Cipher.DECRYPT_MODE, key);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            public void exec(byte[] input,
                             int inputOffset,
                             byte[] output,
                             int outputOffset) throws
                                               Exception
            {
                m_aes.doFinal(input, inputOffset, BLOCKSIZE, output, outputOffset);
            }
        }
    }

    public static class PBKDF2WithHmacSHA256
    {
        public static byte[] encryptPassword(String password,
                                             byte[] salt,
                                             int iterations,
                                             int derivedKeyLength) throws
                                                                   GeneralSecurityException
        {
            final PBEKeySpec       spec      = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength * 8);
            final SecretKeyFactory f         = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            final SecretKey        secretKey = f.generateSecret(spec);

            return secretKey.getEncoded();
        }
    }

    public static byte[] wrapFromText(String value,
                                      BiConsumerWithException<InputStream, OutputStream> callback) throws
                                                                                                   Exception
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }

        ByteArrayInputStream  input  = new ByteArrayInputStream(value.getBytes(Charsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        callback.accept(input, output);

        return output.toByteArray();
    }

    public static String unwrapToText(byte[] value,
                                      BiConsumerWithException<InputStream, OutputStream> callback) throws
                                                                                                   Exception
    {
        if (value == null)
        {
            return null;
        }

        ByteArrayInputStream  input  = new ByteArrayInputStream(value);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        callback.accept(input, output);

        return new String(output.toByteArray(), Charsets.UTF_8);
    }

    public static byte[] generateRandomValues(int len)
    {
        byte[] key = new byte[len];

        synchronized (s_secureRandom)
        {
            s_secureRandom.nextBytes(key);
        }

        return key;
    }

    public static String generateRandomKeyAsBase64()
    {
        byte[] key = generateRandomValues(32);

        return Base64.getEncoder()
                     .encodeToString(key);
    }

    public static int generateRandomValue32Bit()
    {
        synchronized (s_secureRandom)
        {
            return s_secureRandom.nextInt();
        }
    }

    public static int generateRandomValue32Bit(int bound)
    {
        synchronized (s_secureRandom)
        {
            return s_secureRandom.nextInt(bound);
        }
    }

    public static long generateRandomValue64Bit()
    {
        synchronized (s_secureRandom)
        {
            return s_secureRandom.nextLong();
        }
    }

    //--//

    private static final ObjectRecycler<MessageDigest> s_sha1 = ObjectRecycler.build(10, 0, MessageDigest.class, () ->
    {
        try
        {
            return MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }, MessageDigest::reset);

    private static final SecureRandom s_secureRandom;

    static
    {
        SecureRandom secureRandom;

        try
        {
            secureRandom = SecureRandom.getInstanceStrong();
        }
        catch (NoSuchAlgorithmException e)
        {
            // Fallback to regular secure random.
            secureRandom = new SecureRandom();
        }

        s_secureRandom = secureRandom;
    }

    //--//

    public static byte[] computeSha1(String text)
    {
        return computeSha1(text.getBytes());
    }

    public static byte[] computeSha1(byte[] buffer)
    {
        try (ObjectRecycler<MessageDigest>.Holder holder = s_sha1.acquire())
        {
            MessageDigest md = holder.get();

            md.update(buffer);
            return md.digest();
        }
    }

    public static byte[] computeSha1(InputStream stream) throws
                                                         IOException
    {
        try (ObjectRecycler<MessageDigest>.Holder holder = s_sha1.acquire())
        {
            MessageDigest md = holder.get();

            byte[] buf = ExpandableArrayOfBytes.getTempBuffer(1024);
            while (true)
            {
                int got = stream.read(buf, 0, buf.length);
                if (got <= 0)
                {
                    break;
                }

                md.update(buf, 0, got);
            }

            return md.digest();
        }
    }

    public static String computeSha1AsText(String text)
    {
        byte[] hash = computeSha1(text);

        return convertHashToText(hash);
    }

    public static String convertHashToText(byte[] hash)
    {
        return BufferUtils.convertToHex(hash, 0, hash.length, 0, false, false);
    }
}

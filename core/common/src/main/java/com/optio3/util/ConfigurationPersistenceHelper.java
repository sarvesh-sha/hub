/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import com.google.common.collect.Lists;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;

public class ConfigurationPersistenceHelper
{
    private final File   m_persistenceDirectory;
    private final String m_encryptionKey;

    public ConfigurationPersistenceHelper(String persistenceDirectory,
                                          String encryptionKey)
    {
        this(persistenceDirectory != null ? new File(persistenceDirectory) : null, encryptionKey);
    }

    public ConfigurationPersistenceHelper(File persistenceDirectory,
                                          String encryptionKey)
    {
        if (persistenceDirectory != null)
        {
            try
            {
                FileSystem.createDirectory(persistenceDirectory.toPath());
            }
            catch (IOException e)
            {
                m_persistenceDirectory = null;
                m_encryptionKey        = null;
                return;
            }
        }

        m_persistenceDirectory = persistenceDirectory;
        m_encryptionKey        = encryptionKey;
    }

    //--//

    public boolean isActive()
    {
        return m_persistenceDirectory != null;
    }

    public List<File> listFiles(String path)
    {
        List<File> results = Lists.newArrayList();

        File target = path != null ? getFile(path) : m_persistenceDirectory;
        if (target != null)
        {
            for (File file : target.listFiles())
            {
                if (!file.isFile())
                {
                    continue;
                }

                results.add(file);
            }
        }

        return results;
    }

    public File getFile(String file)
    {
        return m_persistenceDirectory != null ? new File(m_persistenceDirectory, file) : null;
    }

    public File getFileIfExists(String file)
    {
        File h = getFile(file);
        return h != null && h.isFile() ? h : null;
    }

    public void saveToFile(File file,
                           ConsumerWithException<OutputStream> callback) throws
                                                                         Exception
    {
        if (file != null)
        {
            try (FileOutputStream stream = new FileOutputStream(file))
            {
                if (m_encryptionKey != null)
                {
                    try (CipherOutputStream encryptingStream = Encryption.AES128.encryptingStream(m_encryptionKey, stream))
                    {
                        callback.accept(encryptingStream);
                    }
                }
                else
                {
                    callback.accept(stream);
                }
            }
        }
    }

    public <T> T loadFromFile(File file,
                              FunctionWithException<InputStream, T> callback) throws
                                                                              Exception
    {
        if (file == null)
        {
            return null;
        }

        try (FileInputStream stream = new FileInputStream(file))
        {
            if (m_encryptionKey != null)
            {
                try (CipherInputStream decryptingStream = Encryption.AES128.decryptingStream(m_encryptionKey, stream))
                {
                    return callback.apply(decryptingStream);
                }
            }
            else
            {
                return callback.apply(stream);
            }
        }
    }

    //--//

    public void serializeToFile(File file,
                                Object obj) throws
                                            Exception
    {
        saveToFile(file, (stream) -> ObjectMappers.SkipNulls.writeValue(stream, obj));
    }

    public boolean serializeToDiskNoThrow(File file,
                                          Object obj)
    {
        try
        {
            saveToFile(file, (stream) -> ObjectMappers.SkipNulls.writeValue(stream, obj));
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    public <T> T deserializeFromFile(File file,
                                     Class<T> clz) throws
                                                   Exception
    {
        return loadFromFile(file, (stream) -> ObjectMappers.SkipNulls.readValue(stream, clz));
    }

    public <T> T deserializeFromFileNoThrow(File file,
                                            Class<T> clz)
    {
        try
        {
            return loadFromFile(file, (stream) -> ObjectMappers.SkipNulls.readValue(stream, clz));
        }
        catch (Throwable t)
        {
            return null;
        }
    }
}
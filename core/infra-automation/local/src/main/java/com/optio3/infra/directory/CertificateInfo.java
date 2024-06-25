/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.directory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.io.Files;
import com.optio3.util.FileSystem;
import com.optio3.util.ProcessUtils;

public class CertificateInfo
{
    public FileInfo publicFile;
    public FileInfo privateFile;
    public String   passphrase;

    public List<FileInfo> trustChain;

    //--//

    @JsonIgnore
    CredentialDirectory credDir;

    //--//

    public byte[] readPublicFile()
    {
        return credDir.getContents(publicFile);
    }

    public byte[] readPublicFileAndCertificateChain() throws
                                                      Exception
    {
        try (ByteArrayOutputStream tmpCertStream = new ByteArrayOutputStream())
        {
            tmpCertStream.write(credDir.getContents(publicFile));

            for (FileInfo chain : trustChain)
                tmpCertStream.write(credDir.getContents(chain));

            return tmpCertStream.toByteArray();
        }
    }

    public byte[] readAndDecryptPrivateFile() throws
                                              Exception
    {
        return credDir.decrypt(privateFile, passphrase);
    }

    public byte[] generateJavaKeyStore(String passPhrase) throws
                                                          Exception
    {
        try (FileSystem.TmpFileHolder tmpCertPkcs12Holder = FileSystem.createTempFile("tmpCertPkcs12", ".aes"))
        {
            String tmpCertPkcs12 = tmpCertPkcs12Holder.getAbsolutePath();

            try (FileSystem.TmpFileHolder tmpCertHolder = FileSystem.createTempFile("tmpCert", ".aes"))
            {
                String tmpCertFile = tmpCertHolder.getAbsolutePath();

                try (FileOutputStream tmpCertStream = new FileOutputStream(tmpCertFile))
                {
                    tmpCertStream.write(credDir.getContents(publicFile));

                    for (FileInfo chain : trustChain)
                        tmpCertStream.write(credDir.getContents(chain));
                }

                try (FileSystem.TmpFileHolder keyHolder = FileSystem.createTempFile("key", ".aes"))
                {
                    String keyFile = keyHolder.getAbsolutePath();

                    keyHolder.write(readAndDecryptPrivateFile());

                    ProcessUtils.exec(null, 10, TimeUnit.SECONDS, "openssl", "pkcs12", "-inkey", keyFile, "-in", tmpCertFile, "-export", "-out", tmpCertPkcs12, "-passout", "pass:" + passPhrase);
                }
            }

            try (FileSystem.TmpFileHolder keyStoreHolder = FileSystem.createTempFile("keyStore", ".aes"))
            {
                String keyStoreFile = keyStoreHolder.getAbsolutePath();

                // The 'keytool' application doesn't work properly if the output file exists but is empty. 
                keyStoreHolder.delete();

                ProcessUtils.exec(null,
                                  10,
                                  TimeUnit.SECONDS,
                                  "keytool",
                                  "-importkeystore",
                                  "-srckeystore",
                                  tmpCertPkcs12,
                                  "-srcstoretype",
                                  "PKCS12",
                                  "-deststoretype",
                                  "JKS",
                                  "-destkeystore",
                                  keyStoreFile,
                                  "-srcstorepass",
                                  passPhrase,
                                  "-storepass",
                                  passPhrase);

                return keyStoreHolder.read();
            }
        }
    }

    public void generateRawCert(String caBundle,
                                String publicCert,
                                String privateCert) throws
                                                    Exception
    {
        try (FileOutputStream stream = new FileOutputStream(caBundle))
        {
            for (FileInfo chain : trustChain)
                stream.write(credDir.getContents(chain));
        }

        Files.write(readPublicFile(), new File(publicCert));
        Files.write(readAndDecryptPrivateFile(), new File(privateCert));
    }
}

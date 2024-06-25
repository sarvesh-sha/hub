/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.serialization.ObjectMappers;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class SkySparkHelper
{
    @Path("/")
    public interface MainApi
    {
        @GET
        @Path("/api/{projName}/about")
        @Produces({ "application/json" })
        ApiResults about(@PathParam("projName") String projName);

        //
        // We need to use any API to handle the SCRAM authentication process.
        //
        @GET
        @Path("/api/{projName}/about")
        String login(@PathParam("projName") String projName);
    }

    public static class ApiResults
    {
        public static class Column
        {
            public String name;
        }

        public final Map<String, JsonNode>       meta = Maps.newHashMap();
        public final List<Column>                cols = Lists.newArrayList();
        public final List<Map<String, JsonNode>> rows = Lists.newArrayList();
    }

    //--//

    private static final ObjectMapper        s_objectMapper;
    private static final JacksonJsonProvider s_jsonProvider;

    static
    {
        ObjectMapper mapper = new ObjectMapper();

        ObjectMappers.configureCaseInsensitive(mapper);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        //
        // As the Twilio API evolves, new optional properties could be added to the models.
        // Just ignore what we don't understand.
        //
        ObjectMappers.configureToIgnoreMissingProperties(mapper);

        ObjectMappers.configureEnumAsStrings(mapper, true);

        mapper.registerModules(new JavaTimeModule());

        s_objectMapper = mapper;
        s_jsonProvider = new JacksonJsonProvider(mapper);
    }

    //--//

    private final String m_baseUrl;
    private final String m_projectId;
    private final String m_userName;
    private final String m_password;

    private final Base64.Decoder m_decoder = Base64.getDecoder();
    private final Base64.Encoder m_encoder = Base64.getEncoder()
                                                   .withoutPadding();

    private final Base64.Decoder m_decoderUrl = Base64.getUrlDecoder();
    private final Base64.Encoder m_encoderUrl = Base64.getUrlEncoder()
                                                      .withoutPadding();

    private String         m_token;
    private MonotonousTime m_expiration;

    public SkySparkHelper(String baseUrl,
                          String projectId,
                          String userName,
                          String password)
    {
        m_baseUrl   = baseUrl;
        m_projectId = projectId;
        m_userName  = userName;
        m_password  = password;
    }

    public <P> P createProxy(Class<P> cls) throws
                                           Exception
    {
        ensureLoggedIn();

        return createProxy(cls, m_token);
    }

    private void ensureLoggedIn() throws
                                  Exception
    {
        if (m_token != null)
        {
            if (!TimeUtils.isTimeoutExpired(m_expiration))
            {
                return;
            }

            m_token = null;
        }

        //
        // Step 1, send HELLO.
        //
        Map<String, String> parameters = Maps.newHashMap();
        setRequestValue(parameters, "username", m_userName);

        performAuthenticationStep("HELLO", parameters, false);

        //
        // Step 2, send Challenge request.
        //
        String handshakeToken = getResponseValueAsString(parameters, "handshakeToken");

        // construct client-first-message
        String c_nonce = m_encoderUrl.encodeToString(Encryption.generateRandomValues(15));
        String c1_bare = "n=" + m_userName + ",r=" + c_nonce;
        String c1_msg  = "n,," + c1_bare; // gs1_header;

        parameters.clear();
        setRequestValue(parameters, "data", c1_msg);
        setRequestValue(parameters, "handshakeToken", handshakeToken);

        performAuthenticationStep("SCRAM", parameters, false);

        //
        // Step 3, send Challenge results
        //
        Map<String, String> data   = Maps.newHashMap();
        String              s1_msg = getResponseValueAsString(parameters, "data");
        decodeMap(s1_msg, false, data);

        //--//

        // c2-no-proof
        String cbind_input     = "n,,"; // gs2_header;
        String channel_binding = m_encoderUrl.encodeToString(strBytes(cbind_input));
        String nonce           = data.get("r");
        String c2_no_proof     = "c=" + channel_binding + ",r=" + nonce;

        // proof
        String hash       = parameters.get("hash");
        String salt       = data.get("s");
        int    iterations = Integer.parseInt(data.get("i"));
        String authMsg    = c1_bare + "," + s1_msg + "," + c2_no_proof;

        byte[] saltedPassword = pbk(hash, m_password, salt, iterations);
        String clientProof    = createClientProof(hash, saltedPassword, strBytes(authMsg));
        String c2_msg         = c2_no_proof + ",p=" + clientProof;

        parameters.clear();
        setRequestValue(parameters, "data", c2_msg);
        setRequestValue(parameters, "handshakeToken", handshakeToken);

        performAuthenticationStep("SCRAM", parameters, true);

        m_expiration = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
    }

    private void performAuthenticationStep(String prefix,
                                           Map<String, String> parameters,
                                           boolean finalRequest)
    {
        MainApi proxy = createProxy(MainApi.class, encodeRequest(prefix, parameters));

        if (finalRequest)
        {
            proxy.login(m_projectId);

            Client client = (Client) proxy;

            m_token = "BEARER " + client.getResponse()
                                        .getHeaderString("Authentication-Info");
        }
        else
        {
            try
            {
                proxy.login(m_projectId);

                throw Exceptions.newRuntimeException("Unexpected non-failure during login...");
            }
            catch (NotAuthorizedException e)
            {
                String scheme = decodeResponse(e.getResponse(), parameters);
                if (!StringUtils.equalsIgnoreCase(scheme, "SCRAM"))
                {
                    throw Exceptions.newIllegalArgumentException("Unexpected scheme '%s'", scheme);
                }
            }
        }
    }

    //--//

    private void setRequestValue(Map<String, String> parameters,
                                 String key,
                                 String value)
    {
        if (value != null)
        {
            setRequestValue(parameters, key, strBytes(value));
        }
    }

    private void setRequestValue(Map<String, String> parameters,
                                 String key,
                                 byte[] value)
    {
        if (value != null)
        {
            parameters.put(key, m_encoderUrl.encodeToString(value));
        }
    }

    private String getResponseValueAsString(Map<String, String> parameters,
                                            String key)
    {
        byte[] val = getResponseValue(parameters, key);
        return val != null ? new String(val, StandardCharsets.UTF_8) : null;
    }

    private byte[] getResponseValue(Map<String, String> parameters,
                                    String key)
    {
        String value = parameters.get(key);
        if (value == null)
        {
            return null;
        }

        return m_decoderUrl.decode(value);
    }

    private String encodeRequest(String scheme,
                                 Map<String, String> parameters)
    {
        StringBuilder s = new StringBuilder();

        s.append(scheme);

        boolean first = true;
        for (String key : parameters.keySet())
        {
            if (!first)
            {
                s.append(',');
            }
            else
            {
                first = false;
            }

            s.append(' ');
            s.append(key);
            s.append("=");
            s.append(parameters.get(key));
        }

        return s.toString();
    }

    private String decodeResponse(Response resp,
                                  Map<String, String> parameters)
    {
        String val = resp.getHeaderString("WWW-Authenticate");
        return decodeMap(val, true, parameters);
    }

    private String decodeMap(String val,
                             boolean extractFirstAsWhole,
                             Map<String, String> parameters)
    {
        String[] parts = StringUtils.split(val, ", ");

        parameters.clear();

        for (int i = extractFirstAsWhole ? 1 : 0; i < parts.length; i++)
        {
            String[] subParts = StringUtils.split(parts[i], '=');

            parameters.put(subParts[0], subParts[1]);
        }

        return extractFirstAsWhole ? parts[0] : null;
    }

    private static byte[] strBytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    //--//

    private byte[] pbk(String hash,
                       String password,
                       String salt,
                       int iterations) throws
                                       Exception
    {
        int keyBits;

        switch (hash)
        {
            case "SHA-1":
                keyBits = 160;
                break;

            case "SHA-256":
                keyBits = 256;
                break;

            case "SHA-512":
                keyBits = 512;
                break;

            default:
                throw Exceptions.newIllegalArgumentException("Unsupported hash function: %s", hash);
        }

        String algorithm = "PBKDF2WithHmac" + hash.replace("-", "");

        return pbk(algorithm, strBytes(password), m_decoder.decode(salt), iterations, keyBits / 8);
    }

    private static String createClientProof(String hash,
                                            byte[] saltedPassword,
                                            byte[] authMsg) throws
                                                            Exception
    {
        byte[] clientKey = hmac(hash, strBytes("Client Key"), saltedPassword);
        byte[] storedKey = MessageDigest.getInstance(hash)
                                        .digest(clientKey);
        byte[] clientSig = hmac(hash, authMsg, storedKey);

        byte[] clientProof = new byte[clientKey.length];
        for (int i = 0; i < clientKey.length; i++)
             clientProof[i] = (byte) (clientKey[i] ^ clientSig[i]);

        return Base64.getEncoder()
                     .encodeToString(clientProof);
    }

    private static byte[] hmac(String algorithm,
                               byte[] data,
                               byte[] key) throws
                                           Exception
    {
        // get digest algorthim
        MessageDigest md        = MessageDigest.getInstance(algorithm);
        int           blockSize = 64;

        // key is greater than block size we hash it first
        int keySize = key.length;
        if (keySize > blockSize)
        {
            md.update(key, 0, keySize);
            key     = md.digest();
            keySize = key.length;
            md.reset();
        }

        // RFC 2104:
        //   ipad = the byte 0x36 repeated B times
        //   opad = the byte 0x5C repeated B times
        //   H(K XOR opad, H(K XOR ipad, text))

        // inner digest: H(K XOR ipad, text)
        for (int i = 0; i < blockSize; ++i)
        {
            if (i < keySize)
            {
                md.update((byte) (key[i] ^ 0x36));
            }
            else
            {
                md.update((byte) 0x36);
            }
        }
        md.update(data, 0, data.length);
        byte[] innerDigest = md.digest();

        // outer digest: H(K XOR opad, innerDigest)
        md.reset();
        for (int i = 0; i < blockSize; ++i)
        {
            if (i < keySize)
            {
                md.update((byte) (key[i] ^ 0x5C));
            }
            else
            {
                md.update((byte) 0x5C);
            }
        }
        md.update(innerDigest);

        return md.digest();
    }

    /**
     * Derive a Password-Based Key.  The only currently supported algorithm is "PBKDF2WithHmacSHA256".
     */
    private static byte[] pbk(String algorithm,
                              byte[] password,
                              byte[] salt,
                              int iterationCount,
                              int derivedKeyLength) throws
                                                    Exception
    {
        if (!algorithm.equals("PBKDF2WithHmacSHA256"))
        {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }

        return deriveKey(password, salt, iterationCount, derivedKeyLength);
    }

    private static byte[] deriveKey(byte[] password,
                                    byte[] salt,
                                    int iterations,
                                    int dkLen) throws
                                               Exception
    {
        SecretKeySpec keyspec = new SecretKeySpec(password, "HmacSHA256");
        Mac           prf     = Mac.getInstance("HmacSHA256");
        prf.init(keyspec);

        int    hLen      = prf.getMacLength();
        int    l         = Math.max(dkLen, hLen);
        byte[] buf       = new byte[l * hLen];
        int    ti_offset = 0;

        for (int i = 1; i <= l; i++)
        {
            F(buf, ti_offset, prf, salt, iterations, i);

            ti_offset += hLen;
        }

        return Arrays.copyOf(buf, dkLen);
    }

    private static void F(byte[] dest,
                          int offset,
                          Mac prf,
                          byte[] salts,
                          int iterations,
                          int blockIndex)
    {
        final int hLen = prf.getMacLength();

        try (OutputBuffer ob = new OutputBuffer())
        {
            ob.littleEndian = false;
            ob.emit(salts);
            ob.emit4Bytes(blockIndex);

            byte[] arrayI = ob.toByteArray();

            for (int i = 0; i < iterations; i++)
            {
                arrayI = prf.doFinal(arrayI);

                for (int j = 0; j < hLen; j++)
                {
                    dest[offset + j] ^= arrayI[j];
                }
            }
        }
    }

    //--//

    private <P> P createProxy(Class<P> cls,
                              String auth)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(m_baseUrl);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        // Handle authentication.
        Map<String, String> map = Maps.newHashMap();
        map.put("Authorization", auth);
        bean.setHeaders(map);

        Client client = bean.createWithValues();

        HTTPConduit conduit = WebClient.getConfig(client)
                                       .getHttpConduit();
        conduit.getClient()
               .setReceiveTimeout(10_000);

        return cls.cast(client);
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Maps;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.github.api.OrganizationApi;
import com.optio3.infra.github.api.RepoApi;
import com.optio3.infra.github.model.Organization;
import com.optio3.infra.github.model.Repository;
import com.optio3.infra.github.model.WebHook;
import com.optio3.serialization.ObjectMappers;
import io.dropwizard.jackson.Jackson;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;

public class GitHubHelper
{
    public static final  String API_CREDENTIALS_SITE = "github.com";
    private static final String API_URL              = "https://api.github.com";

    private final static JacksonJsonProvider s_jsonProvider;

    static
    {
        final ObjectMapper mapper = Jackson.newObjectMapper();

        //
        // GitHub doesn't like when JSON objects contain extra null values.
        // To deal with that, we have to install a custom serializer provider that ignores nulls.
        //
        ObjectMappers.configureToSkipNulls(mapper);

        s_jsonProvider = new JacksonJsonProvider(mapper);
    }

    private final String m_baseAddress;
    @SuppressWarnings("unused")
    private final String m_apiUser;
    private final String m_apiSecret;

    private Organization     m_org;
    private List<Repository> m_repos;

    public GitHubHelper(CredentialDirectory credDir,
                        String account)
    {
        ApiInfo ai = credDir.findFirstApiCredential(API_CREDENTIALS_SITE, account);

        m_baseAddress = API_URL;
        m_apiUser = ai.user;
        m_apiSecret = ai.secretKey;
    }

    public GitHubHelper(String apiUser,
                        String apiSecret)
    {
        this(API_URL, apiUser, apiSecret);
    }

    public GitHubHelper(String baseAddress,
                        String apiUser,
                        String apiSecret)
    {
        m_baseAddress = baseAddress;
        m_apiUser = apiUser;
        m_apiSecret = apiSecret;
    }

    //--//

    public static boolean validateSignature(String secret,
                                            byte[] payload,
                                            String signature) throws
                                                              InvalidKeyException,
                                                              NoSuchAlgorithmException
    {
        if (signature == null)
        {
            return secret != null ? false : true;
        }

        String[] parts = signature.split("=");
        if (parts.length != 2)
        {
            return false;
        }

        switch (parts[0])
        {
            case "sha1":
            {
                final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

                SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA1_ALGORITHM);
                Mac           mac        = Mac.getInstance(HMAC_SHA1_ALGORITHM);
                mac.init(signingKey);

                byte[] computedHash = mac.doFinal(payload);
                String computedStr  = toHexString(computedHash);

                return computedStr.equalsIgnoreCase(parts[1]);
            }
        }

        return false;
    }

    private static String toHexString(byte[] bytes)
    {
        try (Formatter formatter = new Formatter())
        {
            for (byte b : bytes)
                formatter.format("%02x", b);

            return formatter.toString();
        }
    }

    public Organization getOrganization()
    {
        if (m_org == null)
        {
            OrganizationApi proxy = createProxy(OrganizationApi.class);

            List<Organization> orgs = proxy.getOrganization();
            for (Organization org : orgs)
            {
                if (org.login != null)
                {
                    // Pick the first one.
                    m_org = org;
                    break;
                }
            }
        }

        return m_org;
    }

    public List<Repository> listRepos()
    {
        if (m_repos == null)
        {
            Organization org = getOrganization();

            if (org == null)
            {
                m_repos = Collections.emptyList();
            }
            else
            {
                OrganizationApi proxy = createProxy(OrganizationApi.class);
                m_repos = Collections.unmodifiableList(proxy.listRepositories(org.login));
            }
        }

        return m_repos;
    }

    public Repository findRepo(String url)
    {
        //
        // Github does not return the ".git" extension.
        //
        final String suffix = ".git";
        if (url.endsWith(suffix))
        {
            url = url.substring(0, url.length() - suffix.length());
        }

        for (Repository repo : listRepos())
        {
            if (repo.html_url.equals(url))
            {
                return repo;
            }
        }

        return null;
    }

    public List<WebHook> listHooks(Repository repo)
    {
        RepoApi proxy = createProxy(RepoApi.class);

        return proxy.listHooks(repo.owner.login, repo.name);
    }

    public void removeHook(Repository repo,
                           WebHook hook)
    {
        RepoApi proxy = createProxy(RepoApi.class);

        proxy.deleteHook(repo.owner.login, repo.name, hook.id);
    }

    public WebHook addHook(Repository repo,
                           WebHook hook)
    {
        RepoApi proxy = createProxy(RepoApi.class);

        return proxy.createHook(repo.owner.login, repo.name, hook);
    }

    //--//

    public <P> P createProxy(Class<P> cls)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(m_baseAddress);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        // Handle authentication.
        Map<String, String> map = Maps.newHashMap();
        map.put("Authorization", String.format("token %s", m_apiSecret));
        bean.setHeaders(map);

        Client client = bean.createWithValues();
        return cls.cast(client);
    }
}

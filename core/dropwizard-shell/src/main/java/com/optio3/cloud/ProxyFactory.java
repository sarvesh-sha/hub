/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import javax.ws.rs.core.NewCookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.optio3.serialization.ObjectMappers;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.InvocationHandlerAware;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.logging.NoOpFaultListener;
import org.apache.cxf.service.Service;

public class ProxyFactory
{
    private final JacksonJsonProvider m_jsonProvider;

    private final CookieProvider m_cookies = new CookieProvider();

    public ProxyFactory()
    {
        this(ObjectMappers.SkipNulls);
    }

    public ProxyFactory(ObjectMapper objectMapper)
    {
        //
        // As APIs evolve, new optional properties could be added to the models.
        // Just ignore what we don't understand.
        //
        objectMapper = objectMapper.copy();
        ObjectMappers.configureToIgnoreMissingProperties(objectMapper);

        m_jsonProvider = new JacksonJsonProvider(objectMapper);
    }

    //--//

    public <P> P createProxy(String baseAddress,
                             Class<P> cls,
                             Object... varValues)
    {
        JAXRSClientFactoryBean bean = createProxyFactory(baseAddress, cls);
        return instantiateProxy(cls, bean, null, varValues);
    }

    public <P> P createProxyWithCookie(String baseAddress,
                                       Class<P> cls,
                                       NewCookie cookie,
                                       Object... varValues)
    {
        JAXRSClientFactoryBean bean = createProxyFactory(baseAddress, cls);
        return instantiateProxy(cls, bean, cookie, varValues);
    }

    public <P> P createProxyWithCredentials(String baseAddress,
                                            Class<P> cls,
                                            String userName,
                                            String password,
                                            Object... varValues)
    {
        JAXRSClientFactoryBean bean = createProxyFactory(baseAddress, cls);
        bean.setUsername(userName);
        bean.setPassword(password);
        return instantiateProxy(cls, bean, null, varValues);
    }

    private JAXRSClientFactoryBean createProxyFactory(String baseAddress,
                                                      Class<?> cls)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.setResourceClass(cls);
        bean.setProvider(m_jsonProvider);
        return bean;
    }

    private <P> P instantiateProxy(Class<P> cls,
                                   JAXRSClientFactoryBean bean,
                                   NewCookie cookie,
                                   Object... varValues)
    {
        Client client = bean.createWithValues(varValues);

        //
        // Silence the default PhaseInterceptorChain fault logging.
        //
        {
            final JAXRSServiceFactoryBean serviceFactory = bean.getServiceFactory();
            final Service                 service        = serviceFactory.getService();
            service.put(FaultListener.class.getName(), new NoOpFaultListener());
        }

        if (cookie != null)
        {
            m_cookies.addCookie(client, cookie);
        }

        ClientProxyWithCookies proxyImpl = new ClientProxyWithCookies(m_cookies, client, cls);

        Class<?>[] ifaces = new Class[] { Client.class, InvocationHandlerAware.class, cls };

        Client actualClient = (Client) ProxyHelper.getProxy(cls.getClassLoader(), ifaces, proxyImpl);
        return cls.cast(actualClient);
    }
}

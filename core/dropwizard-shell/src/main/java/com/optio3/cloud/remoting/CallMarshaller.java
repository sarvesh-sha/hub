/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.concurrency.TwoWayConcurrentMap;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.ProxyHelper;

public class CallMarshaller
{
    private static class MethodToCallDescriptor
    {
        final ConcurrentMap<Method, CallDescriptor> map = Maps.newConcurrentMap();
    }

    //--//

    private final TypeFactory m_typeFactory = TypeFactory.defaultInstance();

    private final ConcurrentMap<Type, MethodToCallDescriptor> m_typeToMethods           = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Class<?>>             m_remotableEndpointLookup = Maps.newConcurrentMap();
    private final TwoWayConcurrentMap<Type, String>           m_remotableProxyLookup    = new TwoWayConcurrentMap<>();

    //--//

    public void addRemotableEndpoint(Class<?> t)
    {
        try
        {
            Objects.requireNonNull(t.getConstructor());

            Optio3RemotableEndpoint anno = t.getAnnotation(Optio3RemotableEndpoint.class);
            if (anno == null)
            {
                throw Exceptions.newIllegalArgumentException("Type %s is not marked as a Remotable Endpoint ", t);
            }

            Class<?> itf = anno.itf();

            Optio3RemotableProxy annoItf = itf.getAnnotation(Optio3RemotableProxy.class);
            if (annoItf == null)
            {
                throw Exceptions.newIllegalArgumentException("Remotable Endpoint %s does not point to a valid Remotable Proxy", t);
            }

            if (!Reflection.canAssignTo(itf, t))
            {
                throw Exceptions.newIllegalArgumentException("Remotable Endpoint %s does not implement declared Proxy %s", t, itf);
            }

            String id = extractIdOfRemotableProxy(itf, annoItf);
            m_remotableEndpointLookup.put(id, t);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    //--//

    public RemoteCallDescriptor encode(Type target,
                                       Method m,
                                       Object... args)
    {
        requireNonNull(target);
        requireNonNull(m);

        CallDescriptor cd = lookup(target, m);

        RemoteCallDescriptor res = new RemoteCallDescriptor();
        res.classId = encodeTypeId(target);
        res.methodName = m.getName();
        res.parameters = Lists.newArrayList();

        for (int i = 0; i < cd.parameters.length; i++)
        {
            CallDescriptor.Param param = cd.parameters[i];
            Object               value = args[i];

            RemoteArgument rv = new RemoteArgument();
            rv.typeId = encodeTypeId(param.jsonType);

            if (param.isCallback)
            {
                rv.callbackId = IdGenerator.newGuid();
            }
            else
            {
                rv.value = JsonWebSocket.serializeValueAsTree(value);
            }

            res.parameters.add(rv);
        }

        return res;
    }

    public LocalCall decode(RemoteCallDescriptor remote,
                            BiFunction<Type, RemoteArgument, Object> callbackHandler) throws
                                                                                      Exception
    {
        JavaType jsonType = decodeProxyId(remote.classId);
        Class<?> rawClass = jsonType.getRawClass();

        JavaType[] paramTypes = convertParameterTypes(remote.parameters);

        for (Method m : Reflection.collectMethods(rawClass)
                                  .get(remote.methodName))
        {
            if (!Reflection.isMethodReturningAPromise(jsonType, m))
            {
                continue;
            }

            CallDescriptor cd = lookup(jsonType, m);
            if (!cd.sameSignature(paramTypes))
            {
                continue;
            }

            Object[] args = new Object[cd.parameters.length];

            for (int i = 0; i < args.length; i++)
            {
                CallDescriptor.Param localArgument  = cd.parameters[i];
                RemoteArgument       remoteArgument = remote.parameters.get(i);
                JavaType             valueType      = paramTypes[i];

                if (remoteArgument.callbackId != null)
                {
                    if (callbackHandler == null)
                    {
                        throw Exceptions.newRuntimeException("Can't handle callbacks for parameter #%d for %s", i, remote);
                    }

                    args[i] = callbackHandler.apply(localArgument.jsonType, remoteArgument);
                }
                else
                {
                    JsonNode tree = remoteArgument.value;
                    if (tree != null)
                    {
                        args[i] = JsonWebSocket.deserializeValue(valueType, tree);
                    }
                }
            }

            return new LocalCall(jsonType, m, cd.returnValue.jsonType, args);
        }

        throw Exceptions.newRuntimeException("Can't find target for remote invocation %s", remote);
    }

    //--//

    public RemoteResult encodeResult(Type type,
                                     Object object,
                                     Throwable t)
    {
        RemoteResult rv = new RemoteResult();
        rv.typeId = type != null ? encodeTypeId(type) : null;
        rv.value = JsonWebSocket.serializeValueAsTree(object);
        rv.exception = RemoteExceptionResult.encode(t);

        return rv;
    }

    public Object decodeResult(RemoteResult v) throws
                                               Throwable
    {
        if (v.exception != null)
        {
            throw v.exception.decode();
        }

        JsonNode tree = v.value;
        if (tree == null)
        {
            return null;
        }

        return JsonWebSocket.deserializeValue(decodeTypeId(v.typeId), tree);
    }

    //--//

    private JavaType toJsonType(Type type)
    {
        return m_typeFactory.constructType(type);
    }

    public JavaType decodeProxyId(String id)
    {
        if (!m_remotableEndpointLookup.containsKey(id))
        {
            throw Exceptions.newIllegalArgumentException("Attempt to call unregistered RPC type '%s'", id);
        }

        return decodeTypeId(id);
    }

    public JavaType decodeTypeId(String id)
    {
        Type impl = m_remotableEndpointLookup.get(id);
        if (impl == null)
        {
            impl = m_remotableProxyLookup.getReverse(id);
        }

        if (impl != null)
        {
            id = encodeTypeId(impl);
        }

        return m_typeFactory.constructFromCanonical(id);
    }

    public String encodeTypeId(Type type)
    {
        String id = lookupId(type);
        if (id != null)
        {
            return id;
        }

        return toJsonType(type).toCanonical();
    }

    public String encodeTypeId(JavaType type)
    {
        String id = lookupId(type.getRawClass());
        if (id != null)
        {
            return id;
        }

        return type.toCanonical();
    }

    public <P> P createRemotableProxy(Type itf,
                                      boolean isCallback,
                                      CallDispatcher dispatcher)
    {
        Class<?> itfRaw = Reflection.getRawType(itf);

        if (!isCallback)
        {
            if (!m_remotableProxyLookup.containsKey(itf))
            {
                Optio3RemotableProxy annoItf = itfRaw.getAnnotation(Optio3RemotableProxy.class);
                if (annoItf == null)
                {
                    throw Exceptions.newRuntimeException("Interface %s is not a Remotable Proxy", itf);
                }

                String id = extractIdOfRemotableProxy(itf, annoItf);
                m_remotableProxyLookup.add(itf, id);
            }
        }

        ProxyForRemoteInvocation proxyImpl = new ProxyForRemoteInvocation(this, dispatcher, itf);

        Class<?>[] ifaces = new Class[] { itfRaw };

        @SuppressWarnings("unchecked") P proxy = (P) ProxyHelper.getProxy(itfRaw.getClassLoader(), ifaces, proxyImpl);

        return proxy;
    }

    //--//

    private static String extractIdOfRemotableProxy(Type itf,
                                                    Optio3RemotableProxy annoItf)
    {
        String uniqueIdentifier = annoItf.uniqueIdentifier();
        if (!StringUtils.isEmpty(uniqueIdentifier))
        {
            return uniqueIdentifier;
        }

        JavaType type = TypeFactory.defaultInstance()
                                   .constructType(itf);
        return type.toCanonical();
    }

    private String lookupId(Type type)
    {
        while (type != null)
        {
            String id = m_remotableProxyLookup.getForward(type);
            if (id != null)
            {
                return id;
            }

            ParameterizedType pt = Reflection.as(type, ParameterizedType.class);
            if (pt != null)
            {
                type = pt.getRawType();
            }

            Class<?> clz = Reflection.as(type, Class.class);
            if (clz != null)
            {
                type = clz.getGenericSuperclass();
                continue;
            }

            break;
        }

        return null;
    }

    private JavaType[] convertParameterTypes(List<RemoteArgument> list)
    {
        JavaType[] res = new JavaType[list.size()];

        for (int i = 0; i < res.length; i++)
        {
            String typeId = list.get(i).typeId;
            res[i] = decodeTypeId(typeId);
        }

        return res;
    }

    private CallDescriptor lookup(Type type,
                                  Method m)
    {
        MethodToCallDescriptor methodLookup = m_typeToMethods.get(type);
        if (methodLookup == null)
        {
            methodLookup = new MethodToCallDescriptor();

            MethodToCallDescriptor methodLookupOld = m_typeToMethods.putIfAbsent(type, methodLookup);
            if (methodLookupOld != null)
            {
                methodLookup = methodLookupOld; // Lost race, use existing value.
            }
        }

        CallDescriptor cd = methodLookup.map.get(m);
        if (cd == null)
        {
            cd = new CallDescriptor(m_typeFactory, type, m);

            CallDescriptor cdOld = methodLookup.map.putIfAbsent(m, cd);
            if (cdOld != null)
            {
                cd = cdOld; // Lost race, use existing value.
            }
        }

        return cd;
    }
}

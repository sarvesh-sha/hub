/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.remoting;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.Lists;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.cloud.remoting.LocalCall;
import com.optio3.cloud.remoting.RemoteCallDescriptor;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Test;

public class MarshallingTest extends Optio3Test
{
    public static class Role
    {
        public String id;

        public String displayName;
    }

    @Test
    @TestOrder(10)
    public void testCreate() throws
                             Throwable
    {
        CallMarshaller cm = new CallMarshaller()
        {
            @Override
            public JavaType decodeProxyId(String id)
            {
                return decodeTypeId(id);
            }
        };

        Method m = this.getClass()
                       .getMethod("testMethod", List.class, List.class);

        List<String> foo = Lists.newArrayList();
        foo.add("test1");
        foo.add("test2");

        List<Role> bar  = Lists.newArrayList();
        Role       role = new Role();
        role.id          = "id1";
        role.displayName = "role1";
        bar.add(role);

        RemoteCallDescriptor rc = cm.encode(this.getClass(), m, foo, bar);

        LocalCall lc2 = cm.decode(rc, null);

        InjectionManager injectionManager = Injections.createInjectionManager((InjectionManager) null);

        lc2.invoke(lc2.instantiateTarget(injectionManager));
    }

    public void testMethod(List<String> foo,
                           List<Role> bar)
    {
        assertEquals(2, foo.size());
        assertEquals(1, bar.size());

        assertEquals("test1", foo.get(0));
        assertEquals("test2", foo.get(1));

        assertEquals("id1", bar.get(0).id);
        assertEquals("role1", bar.get(0).displayName);
    }
}

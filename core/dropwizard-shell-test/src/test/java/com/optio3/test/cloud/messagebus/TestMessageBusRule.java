/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.messagebus;

import java.util.Collections;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.AbstractConfiguration;
import com.optio3.cloud.annotation.Optio3WebSocketEndpoint;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalRoleResolver;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.MessageBusServerWebSocket;
import com.optio3.test.dropwizard.TestApplicationRule;
import com.optio3.util.function.ConsumerWithException;

public class TestMessageBusRule extends TestApplicationRule<TestMessageBusRule.FakeApplication, TestMessageBusRule.FakeConfiguration>
{
    public static final String ADMIN_USER = "adminUser";
    public static final String ADMIN_PWD  = "adminPwd";

    public static final String MACHINE_USER = "machineUser";
    public static final String MACHINE_PWD  = "machinePwd";

    public static final String NORMAL_USER = "normalUser";
    public static final String NORMAL_PWD  = "normalPwd";

    @Optio3WebSocketEndpoint(name = "Message Bus WebSocket Servlet", timeout = 60 * 1000, urlPatterns = { "/v1/message-bus" }) // For Optio3 Shell
    public static class MessageBusImpl extends MessageBusServerWebSocket
    {
    }

    public static class FakeApplication extends AbstractApplication<FakeConfiguration>
    {
        @Override
        protected void initialize()
        {
            enableAuthentication(new CookiePrincipalRoleResolver()
            {
                @Override
                public boolean stillValid(@NotNull CookiePrincipal principal)
                {
                    return true;
                }

                @Override
                public boolean hasRole(@NotNull CookiePrincipal principal,
                                       String role)
                {
                    WellKnownRole r = WellKnownRole.parse(role);

                    switch (principal.getName())
                    {
                        case ADMIN_USER:
                            switch (r)
                            {
                                case Administrator:
                                    return true;

                                default:
                                    return false;
                            }

                        case MACHINE_USER:
                            switch (r)
                            {
                                case Machine:
                                    return true;

                                default:
                                    return false;
                            }

                        case NORMAL_USER:
                            switch (r)
                            {
                                case User:
                                    return true;

                                default:
                                    return false;
                            }
                    }

                    return false;
                }

                @Override
                public Set<String> getRoles(@NotNull CookiePrincipal principal)
                {
                    switch (principal.getName())
                    {
                        case ADMIN_USER:
                            return Sets.newHashSet(WellKnownRole.Administrator.getId());

                        case MACHINE_USER:
                            return Sets.newHashSet(WellKnownRole.Machine.getId());

                        case NORMAL_USER:
                            return Sets.newHashSet(WellKnownRole.User.getId());
                    }

                    return Collections.emptySet();
                }

                @Override
                public boolean authenticate(@NotNull CookiePrincipal principal,
                                            String password)
                {
                    switch (principal.getName())
                    {
                        case ADMIN_USER:
                            return ADMIN_PWD.equals(password);

                        case MACHINE_USER:
                            return MACHINE_PWD.equals(password);

                        case NORMAL_USER:
                            return NORMAL_PWD.equals(password);
                    }

                    return false;
                }
            });
        }

        @Override
        protected boolean enablePeeringProtocol()
        {
            return true;
        }

        @Override
        protected void run()
        {
            FakeConfiguration cfg = getServiceNonNull(FakeConfiguration.class);
            discoverWebSockets("/api", cfg.webSocketPackage);
        }

        public <C extends MessageBusChannelProvider<?, ?>> C addChannel(Class<C> t)
        {
            return addMessageBusChannel(t);
        }
    }

    public static class FakeConfiguration extends AbstractConfiguration
    {
        public String webSocketPackage;
    }

    //--//

    public TestMessageBusRule(String configurationResource,
                              ConsumerWithException<FakeConfiguration> configurationCallback)
    {
        super(FakeApplication.class, configurationResource, configurationCallback, null, null);
    }

    public TestMessageBusRule(String configurationResource,
                              ConsumerWithException<FakeConfiguration> configurationCallback,
                              ConsumerWithException<FakeApplication> applicatonCallback)
    {
        super(FakeApplication.class, configurationResource, configurationCallback, applicatonCallback, null);
    }
}

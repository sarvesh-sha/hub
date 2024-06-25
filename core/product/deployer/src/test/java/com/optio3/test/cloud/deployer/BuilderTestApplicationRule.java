/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import java.util.concurrent.atomic.AtomicReference;

import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.BuilderUserLogic;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.function.ConsumerWithException;

public class BuilderTestApplicationRule extends TestApplicationWithDbRule<BuilderApplication, BuilderConfiguration>
{
    public BuilderTestApplicationRule(ConsumerWithException<BuilderConfiguration> configurationCallback,
                                      ConsumerWithException<BuilderApplication> applicationCallback)
    {
        super(BuilderApplication.class, "builder-test.yml", configurationCallback, applicationCallback);

        dropDatabaseOnExit(false);
    }

    public static BuilderTestApplicationRule newInstance(ConsumerWithException<BuilderApplication> applicationCallback)
    {
        AtomicReference<BuilderTestApplicationRule> lazyBuilder = new AtomicReference<>();

        BuilderTestApplicationRule builder = new BuilderTestApplicationRule((configuration) ->
                                                                            {
                                                                                configuration.userLogic = new BuilderUserLogic(lazyBuilder.get()
                                                                                                                                          .getApplication())
                                                                                {

                                                                                    @Override
                                                                                    protected UserRecord ensureUserRecord(SessionHolder holder,
                                                                                                                          String principal)
                                                                                    {
                                                                                        return createFakeUser(principal);
                                                                                    }

                                                                                    @Override
                                                                                    protected boolean verifyUserPassword(SessionHolder holder,
                                                                                                                         UserRecord user,
                                                                                                                         String password)
                                                                                    {
                                                                                        return true;
                                                                                    }

                                                                                    @Override
                                                                                    protected boolean changeUserPassword(SessionHolder holder,
                                                                                                                         UserRecord user,
                                                                                                                         String password)
                                                                                    {
                                                                                        return false;
                                                                                    }

                                                                                    @Override
                                                                                    public String createUser(SessionHolder holder,
                                                                                                             String principal,
                                                                                                             String firstName,
                                                                                                             String lastName,
                                                                                                             String password)
                                                                                    {
                                                                                        return null;
                                                                                    }

                                                                                    @Override
                                                                                    public boolean deleteUser(SessionHolder holder,
                                                                                                              UserRecord user)
                                                                                    {
                                                                                        return false;
                                                                                    }

                                                                                    @Override
                                                                                    public boolean addUserToGroup(SessionHolder holder,
                                                                                                                  String emailAddress,
                                                                                                                  RoleRecord role)
                                                                                    {
                                                                                        return false;
                                                                                    }

                                                                                    @Override
                                                                                    public boolean removeUserFromGroup(SessionHolder holder,
                                                                                                                       String emailAddress,
                                                                                                                       RoleRecord role)
                                                                                    {
                                                                                        return false;
                                                                                    }

                                                                                    //--//
                                                                                    private UserRecord createFakeUser(String id)
                                                                                    {
                                                                                        UserRecord user = new UserRecord();
                                                                                        user.setEmailAddress(id);

                                                                                        RoleRecord role = new RoleRecord();
                                                                                        role.setSysId(WellKnownRole.Machine.getId());
                                                                                        role.setName(WellKnownRole.Machine.getId());
                                                                                        role.setDisplayName(WellKnownRole.Machine.getDisplayName());

                                                                                        user.getRoles()
                                                                                            .add(role);

                                                                                        return user;
                                                                                    }
                                                                                };
                                                                            }, applicationCallback);

        lazyBuilder.set(builder);

        return builder;
    }
}

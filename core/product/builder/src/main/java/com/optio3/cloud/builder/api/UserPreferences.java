/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.config.UserPreference;
import com.optio3.cloud.builder.persistence.config.UserPreferenceRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "UserPreferences" }) // For Swagger
@Optio3RestEndpoint(name = "UserPreferences") // For Optio3 Shell
@Path("/v1/user-preferences")
@Optio3RequestLogLevel(Severity.Debug)
public class UserPreferences
{
    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("{id}/subkey/list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> listSubKeys(@PathParam("id") String userId,
                                   @FormParam("path") String path) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);
            UserPreferenceRecord.Tree node = tree.getNode(path, false);

            return node != null ? node.subKeys.keySet() : Collections.emptySet();
        }
    }

    @POST
    @Path("{id}/subkey/remove")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean removeSubKeys(@PathParam("id") String userId,
                                 @FormParam("path") String path) throws
                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);
            UserPreferenceRecord.Tree node = tree.getNode(path, false);

            if (node != null)
            {
                node.removeChildren();

                sessionHolder.commit();
                return true;
            }

            return false;
        }
    }

    @POST
    @Path("{id}/value/list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> listValues(@PathParam("id") String userId,
                                  @FormParam("path") String path) throws
                                                                  Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);
            UserPreferenceRecord.Tree node = tree.getNode(path, false);

            return node != null ? node.values.keySet() : Collections.emptySet();
        }
    }

    @POST
    @Path("{id}/value/get")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public UserPreference getValue(@PathParam("id") String userId,
                                   @FormParam("path") String path,
                                   @FormParam("name") String name) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);
            UserPreferenceRecord.Tree node = tree.getNode(path, false);

            if (node != null)
            {
                UserPreferenceRecord rec_pref = node.values.get(name);
                return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_pref);
            }

            return null;
        }
    }

    @POST
    @Path("{id}/value/remove")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean removeValue(@PathParam("id") String userId,
                               @FormParam("path") String path,
                               @FormParam("name") String name) throws
                                                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);
            UserPreferenceRecord.Tree node = tree.getNode(path, false);

            boolean res = node != null && node.removeNode(name);

            sessionHolder.commit();

            return res;
        }
    }

    @POST
    @Path("{id}/value/check")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean checkValueFormat(@PathParam("id") String userId,
                                    @FormParam("path") String path,
                                    @FormParam("name") String name,
                                    @FormParam("value") String value) throws
                                                                      Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);

            return tree.checkValueFormat(path, name, value);
        }
    }

    @POST
    @Path("{id}/value/set")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String setValue(@PathParam("id") String userId,
                           @FormParam("path") String path,
                           @FormParam("name") String name,
                           @FormParam("value") String value) throws
                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            UserPreferenceRecord.Tree tree = parsePreferences(sessionHolder, userId);

            UserPreferenceRecord rec_pref = tree.setValue(path, name, value);

            sessionHolder.commit();

            return rec_pref.getSysId();
        }
    }

    //--//

    private UserPreferenceRecord.Tree parsePreferences(SessionHolder sessionHolder,
                                                       String userId) throws
                                                                      Exception
    {
        RecordHelper<UserPreferenceRecord> helper = sessionHolder.createHelper(UserPreferenceRecord.class);

        UserRecord rec_user = m_cfg.userLogic.getUserWithAuthentication(sessionHolder, userId, m_principalAccessor, null);
        return rec_user.getPreferencesTree(helper);
    }
}

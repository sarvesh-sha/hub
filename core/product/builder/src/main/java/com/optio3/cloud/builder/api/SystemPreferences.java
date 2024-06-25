/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.Collections;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.builder.model.config.SystemPreference;
import com.optio3.cloud.builder.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "SystemPreferences" }) // For Swagger
@Optio3RestEndpoint(name = "SystemPreferences") // For Optio3 Shell
@Path("/v1/system-preferences")
@Optio3RequestLogLevel(Severity.Debug)
public class SystemPreferences
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("subkey/list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> listSubKeys(@FormParam("path") String path)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(path, false);

            return node != null ? node.subKeys.keySet() : Collections.emptySet();
        }
    }

    @POST
    @Path("subkey/remove")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean removeSubKeys(@FormParam("path") String path)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(path, false);

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
    @Path("value/list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> listValues(@FormParam("path") String path)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(path, false);

            return node != null ? node.values.keySet() : Collections.emptySet();
        }
    }

    @POST
    @Path("value/get")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public SystemPreference getValue(@FormParam("path") String path,
                                     @FormParam("name") String name)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(path, false);

            if (node != null)
            {
                SystemPreferenceRecord rec_pref = node.values.get(name);
                return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_pref);
            }

            return null;
        }
    }

    @POST
    @Path("value/remove")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean removeValue(@FormParam("path") String path,
                               @FormParam("name") String name)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(path, false);

            boolean res = node != null && node.removeNode(name);

            sessionHolder.commit();

            return res;
        }
    }

    @POST
    @Path("value/check")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean checkValueFormat(@FormParam("path") String path,
                                    @FormParam("name") String name,
                                    @FormParam("value") String value)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);

            return tree.checkValueFormat(path, name, value);
        }
    }

    @POST
    @Path("value/set")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public String setValue(@FormParam("path") String path,
                           @FormParam("name") String name,
                           @FormParam("value") String value) throws
                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);

            SystemPreferenceRecord rec_pref = tree.setValue(path, name, value);

            sessionHolder.commit();

            return rec_pref.getSysId();
        }
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookieAuthRequestFilter;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.identity.Role;
import com.optio3.cloud.hub.model.identity.UserGroup;
import com.optio3.cloud.hub.model.identity.UserGroupCreationRequest;
import com.optio3.cloud.hub.model.identity.UserGroupImportExport;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.UserGroupRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "UserGroups" }) // For Swagger
@Optio3RestEndpoint(name = "UserGroups") // For Optio3 Shell
@Path("/v1/user-groups")
public class UserGroups
{
    @Inject
    private HubConfiguration m_cfg;

    @Inject
    private CookieAuthRequestFilter m_filter;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<UserGroupRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<UserGroupRecord> helper = sessionHolder.createHelper(UserGroupRecord.class);

            return UserGroupRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<UserGroup> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<UserGroupRecord> helper = sessionHolder.createHelper(UserGroupRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, UserGroupRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public UserGroup create(UserGroupCreationRequest request)
    {
        ModelMapper.trimModel(request);

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            if (StringUtils.isBlank(request.name))
            {
                throw Exceptions.newIllegalArgumentException("Group needs a name");
            }

            UserGroupRecord rec_newUserGroup = UserGroupRecord.findByName(sessionHolder, request.name);
            if (rec_newUserGroup != null)
            {
                throw Exceptions.newIllegalArgumentException("Group already exists with same name: %s", request.name);
            }

            rec_newUserGroup = new UserGroupRecord();
            rec_newUserGroup.setName(request.name);
            rec_newUserGroup.setDescription(request.description);

            sessionHolder.persistEntity(rec_newUserGroup);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_newUserGroup);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public UserGroup get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, UserGroupRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    UserGroup model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            UserRecord rec_user = m_cfg.getUserFromAccessor(validation.sessionHolder, m_principalAccessor);
            boolean    isMaint  = rec_user.hasRole(WellKnownRoleIds.Maintenance);

            UserGroupRecord rec_userGroup = validation.sessionHolder.getEntity(UserGroupRecord.class, id);

            if (StringUtils.isBlank(model.name))
            {
                validation.addFailure("name", "Name cannot be empty");
            }
            else
            {
                if (!StringUtils.equals(rec_userGroup.getName(), model.name) && UserGroupRecord.findByName(validation.sessionHolder, model.name) != null)
                {
                    validation.addFailure("name", "Name already used in another group");
                }

                updateRoles(validation, rec_userGroup, model.roles, isMaint);
                updateSubgroups(validation, rec_userGroup, model.subGroups, isMaint);

                if (validation.canProceed())
                {
                    ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_userGroup);
                }
            }

            return validation.getResults();
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<UserGroupRecord> helper        = validation.sessionHolder.createHelper(UserGroupRecord.class);
            UserGroupRecord               rec_userGroup = helper.get(id);
            if (rec_userGroup != null)
            {
                if (validation.canProceed())
                {
                    helper.delete(rec_userGroup);
                }
            }

            return validation.getResults();
        }
    }

    //--//

    @GET
    @Path("batch-export")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public UserGroupImportExport batchExport()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RoleRecord>      helper_roles  = sessionHolder.createHelper(RoleRecord.class);
            RecordHelper<UserGroupRecord> helper_groups = sessionHolder.createHelper(UserGroupRecord.class);

            UserGroupImportExport res = new UserGroupImportExport();

            for (RoleRecord rec_role : helper_roles.listAll())
            {
                res.roles.add(ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_role));
            }

            for (UserGroupRecord rec_group : helper_groups.listAll())
            {
                res.groups.add(ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_group));
            }

            return res;
        }
    }

    @POST
    @Path("batch-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public UserGroupImportExport batchImport(UserGroupImportExport batch)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<RoleRecord>      helper_roles  = sessionHolder.createHelper(RoleRecord.class);
            RecordHelper<UserGroupRecord> helper_groups = sessionHolder.createHelper(UserGroupRecord.class);

            Map<String, RoleRecord>      lookupRole  = Maps.newHashMap();
            Map<String, UserGroupRecord> lookupGroup = Maps.newHashMap();

            for (Role role : batch.roles)
            {
                RoleRecord rec_role = RoleRecord.findByName(sessionHolder, role.name);
                if (rec_role == null)
                {
                    rec_role = new RoleRecord();
                    rec_role.setSysId(role.sysId);
                    rec_role.setName(role.name);
                    rec_role.setDisplayName(role.displayName);
                    rec_role.setAddAllowed(role.addAllowed);
                    rec_role.setRemoveAllowed(role.removeAllowed);

                    helper_roles.persist(rec_role);
                }

                lookupRole.put(role.sysId, rec_role);
            }

            for (UserGroup group : batch.groups)
            {
                UserGroupRecord rec_group = UserGroupRecord.findByName(sessionHolder, group.name);
                if (rec_group == null)
                {
                    rec_group = helper_groups.getOrNull(group.sysId);
                }

                if (rec_group == null)
                {
                    rec_group = new UserGroupRecord();
                    rec_group.setSysId(group.sysId);
                    rec_group.setName(group.name);
                    rec_group.setDescription(group.description);

                    helper_groups.persist(rec_group);
                }

                lookupGroup.put(group.sysId, rec_group);
            }

            for (UserGroup group : batch.groups)
            {
                UserGroupRecord rec_group = lookupGroup.get(group.sysId);

                for (TypedRecordIdentity<RoleRecord> role : group.roles)
                {
                    SessionHolder.addIfMissingAndNotNull(rec_group.getRoles(), lookupRole.get(role.sysId));
                }

                for (TypedRecordIdentity<UserGroupRecord> subGroup : group.subGroups)
                {
                    SessionHolder.addIfMissingAndNotNull(rec_group.getSubGroups(), lookupGroup.get(subGroup.sysId));
                }
            }

            sessionHolder.commit();

            //--//

            UserGroupImportExport res = new UserGroupImportExport();

            for (RoleRecord rec_role : helper_roles.listAll())
            {
                res.roles.add(ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_role));
            }

            for (UserGroupRecord rec_group : helper_groups.listAll())
            {
                res.groups.add(ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_group));
            }

            return res;
        }
    }

    //--//

    private static void updateRoles(ValidationResultsHolder validation,
                                    UserGroupRecord rec_userGroup,
                                    TypedRecordIdentityList<RoleRecord> roleIds,
                                    boolean isMaint)
    {
        List<RoleRecord> desiredRoles  = RoleRecord.getBatch(validation.sessionHolder.createHelper(RoleRecord.class), roleIds);
        List<RoleRecord> existingRoles = rec_userGroup.getRoles();

        existingRoles.removeIf((role) ->
                               {
                                   if (desiredRoles.contains(role))
                                   {
                                       return false;
                                   }

                                   if (!role.isRemoveAllowed())
                                   {
                                       validation.addFailure("role", "Can't remove '%s' role", role.getDisplayName());
                                   }

                                   return validation.canProceed();
                               });

        for (RoleRecord desiredRole : desiredRoles)
        {
            if (!existingRoles.contains(desiredRole))
            {
                if (!desiredRole.isAddAllowed() && !isMaint)
                {
                    validation.addFailure("role", "Can't assign '%s' role", desiredRole.getDisplayName());
                }

                if (validation.canProceed())
                {
                    existingRoles.add(desiredRole);
                }
            }
        }
    }

    private static void updateSubgroups(ValidationResultsHolder validation,
                                        UserGroupRecord rec_userGroup,
                                        TypedRecordIdentityList<UserGroupRecord> userGroupIds,
                                        boolean isMaint)
    {
        List<UserGroupRecord> desiredUserGroups  = UserGroupRecord.getBatch(validation.sessionHolder.createHelper(UserGroupRecord.class), userGroupIds);
        List<UserGroupRecord> existingUserGroups = rec_userGroup.getSubGroups();

        existingUserGroups.removeIf((userGroup) ->
                                    {
                                        if (desiredUserGroups.contains(userGroup))
                                        {
                                            return false;
                                        }

                                        return validation.canProceed();
                                    });

        for (UserGroupRecord desiredUserGroup : desiredUserGroups)
        {
            if (!existingUserGroups.contains(desiredUserGroup))
            {
                if (desiredUserGroup == rec_userGroup)
                {
                    validation.addFailure("group", "Can't add group to itself: '%s'", desiredUserGroup.getName());
                }

                if (desiredUserGroup.getGroupsClosure()
                                    .contains(rec_userGroup))
                {
                    validation.addFailure("group", "Can't create loops between groups: '%s' -> '%s'", desiredUserGroup.getName(), rec_userGroup.getName());
                }

                for (RoleRecord role : desiredUserGroup.getRoles())
                {
                    if (!role.isAddAllowed() && !isMaint)
                    {
                        validation.addFailure("group", "Can't assign sub group '%s' role", desiredUserGroup.getName());
                    }
                }

                if (validation.canProceed())
                {
                    existingUserGroups.add(desiredUserGroup);
                }
            }
        }
    }
}

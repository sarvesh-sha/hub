/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.Optio3RequestLogFactory;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookieAuthRequestFilter;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.identity.User;
import com.optio3.cloud.builder.model.identity.UserCreationRequest;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.exception.DetailedApplicationException;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.exception.NotAuthorizedException;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.dropwizard.auth.AuthenticationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = { "Users" }) // For Swagger
@Optio3RestEndpoint(name = "Users") // For Optio3 Shell
@Path("/v1/users")
public class Users
{
    @Inject
    private BuilderConfiguration m_cfg;

    @Inject
    private CookieAuthRequestFilter m_filter;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({ @ApiResponse(code = 412, message = "Error", response = DetailedApplicationException.ErrorDetails.class) })
    @Optio3NoAuthenticationNeeded
    public User login(@Context ContainerRequestContext requestContext,
                      @FormParam("username") Optional<String> username,
                      @FormParam("password") Optional<String> password) throws
                                                                        AuthenticationException
    {
        if (password.isEmpty())
        {
            Optio3RequestLogFactory.dontLog();
        }

        //
        // We want to support these requirements:
        //
        // * A client should be able to check if it's logged in.
        // * A client should be able to log in.
        // * Any client should be able to call this method.
        //
        // In order to fulfill these requirements, we cannot use "@RequireLogin".
        // Not having such an annotation prevents the AuthFilter from being installed on this method.
        //
        // To work around that, we manually call the AuthFilter.process, which tries to log the user in *only* if username/password are present.  
        //
        m_filter.process(requestContext, username, password);

        CookiePrincipal principal = CookiePrincipal.getFromContext(requestContext);
        principal.ensureAuthenticated();

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserRecord rec_user = m_cfg.userLogic.findUser(sessionHolder, principal, true);
            if (rec_user != null)
            {
                return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_user);
            }

            return m_cfg.userLogic.getSyntheticUserFromPrincipal(sessionHolder, principal);
        }
    }

    @GET
    @Path("logout")
    @Optio3NoAuthenticationNeeded
    public String logout(@Context ContainerRequestContext requestContext)
    {
        CookiePrincipal principal = CookiePrincipal.getFromContext(requestContext);
        principal.markAsLoggedOut();

        return "Logged Out";
    }

    //--//

    @POST
    @Path("item/{id}/changePwd")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public User changePassword(@PathParam("id") String id,
                               @FormParam("currentPassword") String currentPassword,
                               @FormParam("newPassword") String newPassword)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<UserRecord> helper   = sessionHolder.createHelper(UserRecord.class);
            UserRecord               rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);

            UserRecord rec_targetUser = helper.get(id);

            if (rec_user == rec_targetUser)
            {
                CookiePrincipal principal = CookiePrincipalAccessor.get(m_principalAccessor);
                if (!m_cfg.userLogic.authenticate(sessionHolder, principal.getName(), currentPassword, false))
                {
                    throw new InvalidArgumentException("Incorrect password");
                }
            }
            else
            {
                if (!rec_user.hasAnyRoles(WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator))
                {
                    throw new NotAuthorizedException("Can't reset password of another user");
                }
            }

            rec_targetUser.setPassword(sessionHolder, m_cfg.userLogic, newPassword);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_user);
        }
    }

    @POST
    @Path("forgotPwd")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3NoAuthenticationNeeded
    public User forgotPassword(@FormParam("emailAddress") String emailAddress)
    {
        throw new InvalidStateException("Not Implemented");
    }

    @POST
    @Path("resetPwd")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3NoAuthenticationNeeded
    public User resetPassword(@FormParam("token") String token,
                              @FormParam("newPassword") String newPassword)
    {
        throw new InvalidStateException("Not Implemented");
    }

    //--//

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<User> getAll() throws
                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithoutTransaction())
        {
            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, m_cfg.userLogic.listUsers(sessionHolder));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public User create(UserCreationRequest request)
    {
        ModelMapper.trimModel(request);

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            UserRecord rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);
            boolean    isAdmin  = rec_user.hasRole(WellKnownRoleIds.Administrator);

            UserRecord rec_newUser = m_cfg.userLogic.findUser(sessionHolder, request.emailAddress, false);
            if (rec_newUser != null)
            {
                throw new InvalidArgumentException("User already exists with same email address");
            }

            rec_newUser = UserRecord.createNewUser(sessionHolder, m_cfg.userLogic, request, sessionHolder.createHelper(RoleRecord.class), rec_user, isAdmin);
            if (rec_newUser == null)
            {
                throw new InvalidArgumentException("Creation of new user failed");
            }

            sessionHolder.persistEntity(rec_newUser);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_newUser);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public User get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, UserRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    User model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<UserRecord> helper = validation.sessionHolder.createHelper(UserRecord.class);

            UserRecord rec_user = m_cfg.getUserFromAccessor(validation.sessionHolder, m_principalAccessor);
            boolean    isAdmin  = rec_user.hasRole(WellKnownRoleIds.Administrator);

            UserRecord rec_targetUser = helper.get(id);

            if (rec_user != rec_targetUser)
            {
                if (!isAdmin)
                {
                    validation.addFailure("auth", "Can't change user details");
                }
            }

            rec_targetUser.updateRoles(validation, m_cfg.userLogic, validation.sessionHolder.createHelper(RoleRecord.class), model.roles, rec_user, isAdmin);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_targetUser);
            }

            return validation.getResults();
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<UserRecord> helper = validation.sessionHolder.createHelper(UserRecord.class);

            UserRecord rec = helper.getOrNull(id);
            if (rec != null)
            {
                UserRecord rec_user = m_cfg.getUserFromAccessor(validation.sessionHolder, m_principalAccessor);
                if (rec_user == rec)
                {
                    validation.addFailure("sysId", "Can't delete yourself");
                }

                if (validation.canProceed())
                {
                    m_cfg.userLogic.deleteUser(validation.sessionHolder, rec);

                    helper.delete(rec);
                }
            }

            return validation.getResults();
        }
    }
}

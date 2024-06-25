/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.hub.model.location.LocationHierarchy;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "Locations" }) // For Swagger
@Optio3RestEndpoint(name = "Locations") // For Optio3 Shell
@Path("/v1/locations")
public class Locations
{
    @Inject
    private HubApplication m_app;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("hierarchy")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LocationHierarchy> getTopLevel()
    {
        LocationsEngine          locationsEngine   = m_app.getServiceNonNull(LocationsEngine.class);
        LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(true);

        return locationsSnapshot.extractHierarchy();
    }

    //--//

    @GET
    @Path("options/{id}/email/get")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeliveryOptions getEmailOptions(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            LocationRecord rec = sessionHolder.getEntity(LocationRecord.class, id);

            return rec.getMetadata(LocationRecord.WellKnownMetadata.locationEmailSettings);
        }
    }

    @POST
    @Path("options/{id}/email/set")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public DeliveryOptions setEmailOptions(@PathParam("id") String id,
                                           DeliveryOptions options)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            LocationRecord rec = sessionHolder.getEntity(LocationRecord.class, id);

            rec.putMetadata(LocationRecord.WellKnownMetadata.locationEmailSettings, options);

            sessionHolder.commit();

            return options;
        }
    }

    //--//

    @GET
    @Path("options/{id}/sms/get")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeliveryOptions getSmsOptions(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            LocationRecord rec = sessionHolder.getEntity(LocationRecord.class, id);

            return rec.getMetadata(LocationRecord.WellKnownMetadata.locationSmsSettings);
        }
    }

    @POST
    @Path("options/{id}/sms/set")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public DeliveryOptions setSmsOptions(@PathParam("id") String id,
                                         DeliveryOptions options)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            LocationRecord rec = sessionHolder.getEntity(LocationRecord.class, id);

            rec.putMetadata(LocationRecord.WellKnownMetadata.locationSmsSettings, options);

            sessionHolder.commit();

            return options;
        }
    }
}

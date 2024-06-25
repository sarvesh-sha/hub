/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.protocol.model.BaseObjectModel;
import io.swagger.annotations.Api;

@Api(tags = { "DeviceElements" }) // For Swagger
@Optio3RestEndpoint(name = "DeviceElements") // For Optio3 Shell
@Path("/v1/device-elements")
public class DeviceElements
{
    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("set-desired-state/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults setDesiredState(@PathParam("id") String id,
                                             Map<String, JsonNode> state) throws
                                                                          Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, false, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<DeviceElementRecord> helper = validation.sessionHolder.createHelper(DeviceElementRecord.class);
            DeviceElementRecord               rec    = helper.get(id);

            if (validation.canProceed())
            {
                try
                {
                    BaseObjectModel obj = rec.getContentsAsObject(true);
                    if (obj == null)
                    {
                        obj = rec.getContentsAsObject(false);
                        if (obj != null)
                        {
                            obj = BaseObjectModel.copySingleProperty(obj, null);
                        }
                    }

                    if (obj != null)
                    {
                        obj.updateState(state);
                        ObjectMapper om = obj.getObjectMapperForInstance();
                        rec.setDesiredContents(validation.sessionHolder, om, obj);
                    }

                    rec.assetPostUpdate(validation.sessionHolder);
                }
                catch (Throwable t)
                {
                    validation.addFailure("desiredState", "Failed to set desired state, due to %s", t.getMessage());
                }
            }

            return validation.getResults();
        }
    }
}

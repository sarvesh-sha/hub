/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.model.EnumDescriptor;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import io.swagger.annotations.Api;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

@Api(tags = { "Enums" }) // For Swagger
@Optio3RestEndpoint(name = "Enums") // For Optio3 Shell
@Path("/v1/enums")
@Optio3RequestLogLevel(Severity.Debug)
public class Enums
{
    private static final Supplier<Map<String, List<IEnumDescription>>> s_lookup = Suppliers.memoize(Enums::computeLookup);

    @GET
    @Path("describe/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EnumDescriptor> describe(@PathParam("id") String id)
    {
        Map<String, List<IEnumDescription>> lookup = s_lookup.get();
        List<IEnumDescription>              enums  = lookup.get(id.toLowerCase());

        return EnumDescriptor.describe(enums);
    }

    //--//

    @SuppressWarnings("unchecked")
    private static Map<String, List<IEnumDescription>> computeLookup()
    {
        Reflections                            reflections = new Reflections("com.optio3.", new SubTypesScanner(false));
        Set<Class<? extends IEnumDescription>> targets     = reflections.getSubTypesOf(IEnumDescription.class);

        Map<String, List<IEnumDescription>> res = Maps.newHashMap();
        for (Class<? extends IEnumDescription> target : targets)
        {
            final List<?> enumValues = Reflection.collectEnumValues((Class<Enum>) (Class) target);
            if (!enumValues.isEmpty())
            {
                String name    = target.getName();
                int    lastDot = name.lastIndexOf('.');
                if (lastDot >= 0)
                {
                    name = name.substring(lastDot + 1);
                }

                res.put(name.toLowerCase(), (List<IEnumDescription>) enumValues);
            }
        }

        return res;
    }
}

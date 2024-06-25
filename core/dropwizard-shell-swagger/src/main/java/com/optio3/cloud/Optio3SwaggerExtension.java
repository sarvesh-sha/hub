/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.PathParam;

import com.optio3.cloud.annotation.Optio3RawPathParam;
import com.optio3.cloud.client.SwaggerExtensions;
import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.models.Operation;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import org.apache.commons.lang3.StringUtils;

public class Optio3SwaggerExtension implements SwaggerExtension
{
    @Override
    public String extractOperationMethod(ApiOperation apiOperation,
                                         Method method,
                                         Iterator<SwaggerExtension> chain)
    {
        return chain.next()
                    .extractOperationMethod(apiOperation, method, chain);
    }

    @Override
    public List<Parameter> extractParameters(List<Annotation> annotations,
                                             Type type,
                                             Set<Type> typesToSkip,
                                             Iterator<SwaggerExtension> chain)
    {
        return chain.next()
                    .extractParameters(annotations, type, typesToSkip, chain);
    }

    @Override
    public void decorateOperation(Operation operation,
                                  Method method,
                                  Iterator<SwaggerExtension> chain)
    {
        chain.next()
             .decorateOperation(operation, method, chain);

        for (Parameter parameter : operation.getParameters())
        {
            if (parameter instanceof PathParameter)
            {
                for (var methodParameter : method.getParameters())
                {
                    if (methodParameter.getAnnotation(Optio3RawPathParam.class) != null)
                    {
                        var pathParam = methodParameter.getAnnotation(PathParam.class);
                        if (pathParam != null)
                        {
                            if (StringUtils.equals(pathParam.value(), parameter.getName()))
                            {
                                parameter.getVendorExtensions()
                                         .put(SwaggerExtensions.RAW_PATH_PARAM.getText(), "true");
                            }
                        }
                    }
                }
            }
        }
    }
}

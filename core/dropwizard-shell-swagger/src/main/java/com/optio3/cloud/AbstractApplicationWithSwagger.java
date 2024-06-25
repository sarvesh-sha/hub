/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Model;
import io.swagger.models.Swagger;

public abstract class AbstractApplicationWithSwagger<T extends AbstractConfiguration> extends AbstractApplication<T>
{
    protected AbstractApplicationWithSwagger()
    {
        registerService(AbstractApplicationWithSwagger.class, () -> this);
    }

    //--//

    protected Swagger enableSwagger(Consumer<BeanConfig> visitor,
                                    BiConsumer<Type, Model> visitorModel,
                                    String jerseyRootPath,
                                    String basePath,
                                    String... packages)
    {
        serveAssets("/assets/swagger-ui", "/swagger-ui", "index.html", "SwaggerUI", false, null);

        Swagger swagger = SwaggerListingResource.configure(m_environment.jersey(), jerseyRootPath, visitor, visitorModel, m_extraModels, basePath, packages);

        enableCORS("content-type", "accept", "origin", "authorization", "api_key", "ngsw-bypass");

        return swagger;
    }
}

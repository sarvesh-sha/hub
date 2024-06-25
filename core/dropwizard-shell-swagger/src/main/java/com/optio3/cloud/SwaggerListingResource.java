/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.client.SwaggerExtensions;
import com.optio3.cloud.client.SwaggerTypeReplacement;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import io.dropwizard.jersey.jsr310.LocalDateTimeParam;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.converter.ModelConverters;
import io.swagger.jackson.ModelResolver;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.Resource;

public class SwaggerListingResource
{
    static boolean prettyPrint = false;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/yaml" })
    @Optio3NoAuthenticationNeeded
    public Response getListing(@Context UriInfo uriInfo,
                               @PathParam("type") String type)
    {
        boolean returnYaml = (StringUtils.isNotBlank(type) && type.trim()
                                                                  .equalsIgnoreCase("yaml"));

        try
        {
            String path = uriInfo.getPath();
            path = getBasePath(path);

            SwaggerContextService ctxService = new SwaggerContextService().withConfigId(path);

            Swagger swagger = ctxService.getSwagger();
            if (swagger != null)
            {
                String result;
                String contentType;

                if (returnYaml)
                {
                    result      = Yaml.mapper()
                                      .writeValueAsString(swagger);
                    contentType = "application/yaml";
                }
                else
                {
                    if (prettyPrint)
                    {
                        result = Json.pretty()
                                     .writeValueAsString(swagger);
                    }
                    else
                    {
                        result = Json.mapper()
                                     .writeValueAsString(swagger);
                    }

                    contentType = MediaType.APPLICATION_JSON;
                }

                return Response.ok()
                               .entity(result)
                               .type(contentType)
                               .build();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return Response.status(404)
                       .build();
    }

    private static String getBasePath(String path)
    {
        int lastPath = path.lastIndexOf('/');
        if (lastPath <= 0)
        {
            return "/"; // Return at least one slash.
        }

        return joinPath("/", path.substring(0, lastPath));
    }

    private static String getRelativePath(String rootPath,
                                          String basePath)
    {
        String relativePath;

        if (basePath.startsWith(rootPath))
        {
            relativePath = basePath.substring(rootPath.length());
        }
        else
        {
            relativePath = basePath;
        }

        if (relativePath.length() == 0)
        {
            relativePath = "/";
        }

        return relativePath;
    }

    private static String joinPath(String basePath,
                                   String extPath)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(basePath);

        if (!basePath.endsWith("/"))
        {
            sb.append('/');
        }

        if (extPath.startsWith("/"))
        {
            sb.append(extPath.substring(1));
        }
        else
        {
            sb.append(extPath);
        }

        return sb.toString();
    }

    public static Swagger configure(JerseyEnvironment jersey,
                                    String jerseyRootPath,
                                    Consumer<BeanConfig> visitor,
                                    BiConsumer<Type, Model> visitorModel,
                                    Set<Class<?>> modelsForWebSockets,
                                    String basePath,
                                    String... packages)
    {
        Set<Class<?>> wellKnownTypesWithDeserializers = Sets.newHashSet();
        wellKnownTypesWithDeserializers.add(ZonedDateTime.class);

        String relativePath = getRelativePath(jerseyRootPath, basePath);

        //
        // Out of the box, Swagger doesn't handle 'basePath' correctly.
        //
        // So we have to filter out classes that don't belong under the basePath and post-process paths to strip the common root.
        // 
        BeanConfig magicBean = new BeanConfig()
        {
            @Override
            public Set<Class<?>> classes()
            {
                Set<Class<?>> res = super.classes();

                Iterator<Class<?>> it = res.iterator();
                while (it.hasNext())
                {
                    Class<?> clz = it.next();

                    javax.ws.rs.Path path = clz.getAnnotation(javax.ws.rs.Path.class);
                    if (path != null)
                    {
                        if (path.value()
                                .startsWith(relativePath))
                        {
                            continue;
                        }
                    }

                    it.remove();
                }

                return res;
            }

            @Override
            public Swagger configure(Swagger swagger)
            {
                swagger = super.configure(swagger);

                Map<String, Path> paths = swagger.getPaths();
                if (paths == null)
                {
                    return swagger;
                }

                Map<String, Path> pathsNew = Maps.newHashMap();

                for (String path : paths.keySet())
                {
                    if (!path.startsWith(relativePath))
                    {
                        continue;
                    }

                    String pathNew = path.substring(relativePath.length());
                    pathNew = joinPath("/", pathNew);
                    pathsNew.put(pathNew, paths.get(path));
                }

                swagger.setPaths(pathsNew);

                return swagger;
            }
        };

        //
        // Out of the box, Swagger doesn't handle various object-oriented constructs correctly.
        //
        // We inject a ModelResolver, which allows Vendor Extensions to be added to the model.
        // 
        {
            //
            // Swagger actually recreates the same Model over and over again.
            // In case of models with sub-types, something the wrong Model gets picked.
            // Let's keep track of the correct Model.
            //
            Map<Type, Model>   globalResolutionMap = Maps.newHashMap();
            Map<String, Model> globalDefinitionMap = Maps.newHashMap();

            ModelResolver resolver = new ModelResolver(Json.mapper())
            {
                @Override
                protected String _findTypeName(JavaType type,
                                               BeanDescription beanDesc)
                {
                    //
                    // We want to give proper names to nested classes.
                    // The default behavior is to just use the simple name of the nested class, not the hierarchical one.
                    //
                    Class<?> clz = type.getRawClass();

                    String simpleName = clz.getSimpleName();
                    String name       = super._findTypeName(type, beanDesc);

                    if (StringUtils.equals(simpleName, name))
                    {
                        String fullName = clz.getName();

                        if (fullName.indexOf('$') >= 0)
                        {
                            name = fullName.substring(fullName.lastIndexOf(".") + 1); // strip the package name
                            name = StringUtils.replace(name, "$", "");
                        }
                    }

                    return name;
                }

                @Override
                public Property resolveProperty(JavaType propType,
                                                ModelConverterContext context,
                                                Annotation[] annotations,
                                                Iterator<ModelConverter> next)
                {
                    //
                    // Swap a JsonNode for an Object.
                    // The code generator will do the opposite.
                    //
                    propType = remap(propType, JsonNode.class, Object.class);

                    propType = remap(propType, ZonedDateTimeParam.class, ZonedDateTime.class);
                    propType = remap(propType, LocalDateTimeParam.class, LocalDateTime.class);

                    propType = remapByAnnotation(propType);

                    //
                    // Out of the box, Swagger doesn't handle Enums correctly.
                    // All Enums are flattened to strings with a set of allowable values.
                    // If an Enum is used in multiple Models, the two references won't point to the same Enum.
                    //
                    // We add a Vendor Extension that is used during Codegen to link an Enum property back to a shared type. 
                    //
                    Property prop = super.resolveProperty(propType, context, annotations, next);

                    Class<?> propRawClass = propType.getRawClass();
                    if (propRawClass.isEnum())
                    {
                        String enumName;

                        if (propRawClass == com.optio3.logging.Severity.class)
                        {
                            enumName = "LogSeverity";
                        }
                        else
                        {
                            enumName = propRawClass.getName();
                        }

                        prop.getVendorExtensions()
                            .put(SwaggerExtensions.ENUM_TYPE.getText(), enumName);
                    }

                    return prop;
                }

                @Override
                public Model resolve(JavaType type,
                                     ModelConverterContext context,
                                     Iterator<ModelConverter> next)
                {
                    //
                    // Swap a JsonNode for an Object.
                    // The code generator will do the opposite.
                    //
                    type = remap(type, JsonNode.class, Object.class);

                    type = remapByAnnotation(type);

                    Class<?> rawClass = type.getRawClass();
                    if (!Reflection.isAbstractClass(rawClass) && !wellKnownTypesWithDeserializers.contains(rawClass))
                    {
                        boolean found = false;

                        for (Constructor<?> constructor : rawClass.getConstructors())
                        {
                            if (constructor.getParameterTypes().length == 0)
                            {
                                found = true;
                                break;
                            }

                            if (constructor.isAnnotationPresent(JsonCreator.class))
                            {
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                        {
                            for (Method method : rawClass.getMethods())
                            {
                                if (method.getAnnotation(JsonCreator.class) != null)
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found)
                        {
                            throw Exceptions.newRuntimeException("Type '%s' doesn't have a public constructor for JSON deserialization", type);
                        }
                    }

                    //--//

                    Type lookupType;

                    if (type.isContainerType())
                    {
                        lookupType = type;
                    }
                    else
                    {
                        // Unless it's a container type, always use the raw class, since Swagger doesn't handle generics.
                        lookupType = rawClass;

                        type = Json.mapper()
                                   .getTypeFactory()
                                   .constructType(lookupType);
                    }

                    Model model = globalResolutionMap.get(lookupType);
                    if (model != null)
                    {
                        return model;
                    }

                    //
                    // Create a sub-context, to monitor the definition of new models.
                    //
                    ModelConverterContext subContext = new ModelConverterContext()
                    {
                        @Override
                        public void defineModel(String name,
                                                Model model)
                        {
                            context.defineModel(name, model);
                        }

                        @Override
                        public void defineModel(String name,
                                                Model model,
                                                Type type,
                                                String prevName)
                        {
                            context.defineModel(name, model, type, prevName);

                            Type rawType = unwrapJavaType(type);
                            globalResolutionMap.put(rawType, model);
                            globalDefinitionMap.put(name, model);
                        }

                        @Override
                        public Property resolveProperty(Type type,
                                                        Annotation[] annotations)
                        {
                            return context.resolveProperty(type, annotations);
                        }

                        @Override
                        public Model resolve(Type type)
                        {
                            return context.resolve(type);
                        }

                        @Override
                        public Iterator<ModelConverter> getConverters()
                        {
                            return context.getConverters();
                        }
                    };

                    model = super.resolve(type, subContext, next);
                    if (model == null)
                    {
                        return null;
                    }

                    //
                    // Make sure to look for the most recent model, which is typically the Composed one.
                    //
                    Model alreadyRegisteredModel = globalResolutionMap.get(lookupType);
                    if (alreadyRegisteredModel != null)
                    {
                        model = alreadyRegisteredModel;
                    }
                    else
                    {
                        globalResolutionMap.put(lookupType, model);
                    }

                    //
                    // To support inheritance at runtime, client and server have to agree on the identify of the type.
                    //
                    // We add a Vendor Extension that is used during Codegen to annotate the client side with the proper type information. 
                    //
                    JsonTypeName typeName = rawClass.getAnnotation(JsonTypeName.class);
                    if (typeName != null)
                    {
                        model.getVendorExtensions()
                             .put(SwaggerExtensions.TYPE_NAME.getText(), typeName.value());
                    }

                    return model;
                }

                private JavaType remapByAnnotation(JavaType propType)
                {
                    Class<?>               rawClz = propType.getRawClass();
                    SwaggerTypeReplacement anno   = rawClz.getAnnotation(SwaggerTypeReplacement.class);
                    if (anno != null)
                    {
                        if (anno.targetCollection() == Collection.class)
                        {
                            propType = Json.mapper()
                                           .getTypeFactory()
                                           .constructType(anno.targetElement());
                        }
                        else
                        {
                            propType = Json.mapper()
                                           .getTypeFactory()
                                           .constructCollectionType(anno.targetCollection(), anno.targetElement());
                        }
                    }

                    return propType;
                }

                private JavaType remap(JavaType propType,
                                       Class<?> from,
                                       Class<?> to)
                {
                    if (propType.getRawClass() == from)
                    {
                        propType = Json.mapper()
                                       .getTypeFactory()
                                       .constructType(to);
                    }

                    return propType;
                }

                private JavaType remap(JavaType propType,
                                       Class<?> from,
                                       Class<? extends Collection> toColl,
                                       Class<?> toCollElement)
                {
                    if (propType.getRawClass() == from)
                    {
                        propType = Json.mapper()
                                       .getTypeFactory()
                                       .constructCollectionType(toColl, toCollElement);
                    }

                    return propType;
                }
            };

            try
            {
                ModelConverters.getInstance()
                               .addConverter(resolver);

                magicBean.setBasePath(basePath);
                magicBean.setResourcePackage(String.join(",", packages));
                visitor.accept(magicBean);
                magicBean.scanAndRead();

                Swagger swaggerTmp = magicBean.getSwagger();

                //
                // Make sure all the Body parameters are marked as NOT required, otherwise the body parameter is not emitted as the last one...
                //
                for (Path path : swaggerTmp.getPaths()
                                           .values())
                {
                    for (Operation operation : path.getOperations())
                    {
                        for (Parameter parameter : operation.getParameters())
                        {
                            BodyParameter bp = Reflection.as(parameter, BodyParameter.class);
                            if (bp != null)
                            {
                                bp.setRequired(false);
                            }
                        }
                    }
                }

                Map<String, Model> definitions = swaggerTmp.getDefinitions();

                definitions.putAll(globalDefinitionMap);

                //
                // Add the extra models for the web socket to the spec.
                //
                if (modelsForWebSockets != null)
                {
                    for (Type t : modelsForWebSockets)
                    {
                        definitions.putAll(ModelConverters.getInstance()
                                                          .readAll(t));
                    }
                }

                //
                // When Swagger converts a Model to a ComposedModel, it doesn't copy the Vendor Extensions to the outer context.
                // This makes the extensions invisible to the Codegen. 
                //
                for (Model model : definitions.values())
                {
                    ComposedModel composedModel = Reflection.as(model, ComposedModel.class);
                    if (composedModel != null)
                    {
                        Model childModel = composedModel.getChild();

                        //
                        // Move extensions from child to composed model.
                        //
                        Map<String, Object> composedExts = composedModel.getVendorExtensions();
                        Map<String, Object> childExts    = childModel.getVendorExtensions();
                        composedExts.putAll(childExts);
                        childExts.clear();
                    }
                }

                //
                // Collect all the type names as subtypes in the root of each type hierarchy.
                //
                for (String modelName : definitions.keySet())
                {
                    Model model = definitions.get(modelName);

                    ComposedModel composedModel = Reflection.as(model, ComposedModel.class);
                    if (composedModel != null)
                    {
                        String typeName = getTypeNameFromExtensions(composedModel);
                        if (typeName != null)
                        {
                            Model                                                    rootModel  = getRoot(definitions, composedModel);
                            Map<String, Object>                                      parentExts = rootModel.getVendorExtensions();
                            @SuppressWarnings("unchecked") List<Map<String, String>> subTypes   = (List<Map<String, String>>) parentExts.get(SwaggerExtensions.SUBTYPES.getText());
                            if (subTypes == null)
                            {
                                subTypes = Lists.newArrayList();
                                parentExts.put(SwaggerExtensions.SUBTYPES.getText(), subTypes);
                            }

                            Map<String, String> map = Maps.newHashMap();
                            map.put(SwaggerExtensions.SUBTYPE_MODEL.getText(), modelName);
                            map.put(SwaggerExtensions.SUBTYPE_NAME.getText(), typeName);
                            subTypes.add(map);
                            subTypes.sort(Comparator.comparing(x -> x.get(SwaggerExtensions.SUBTYPE_NAME.getText())));
                        }
                    }
                }

                for (Map.Entry<Type, Model> entry : globalResolutionMap.entrySet())
                {
                    visitorModel.accept(entry.getKey(), entry.getValue());
                }
            }
            finally
            {
                ModelConverters.getInstance()
                               .removeConverter(resolver);
            }
        }

        Swagger swagger = magicBean.getSwagger();
        swagger = magicBean.configure(swagger);

        substituteMap(swagger::getPaths, swagger::setPaths);
        substituteMap(swagger::getDefinitions, swagger::setDefinitions);
        substituteMap(swagger::getParameters, swagger::setParameters);
        substituteMap(swagger::getResponses, swagger::setResponses);

        new SwaggerContextService().withConfigId(relativePath)
                                   .initConfig(swagger);

        // Dynamically create a Resource, such that it's located in the correct location under the base path.
        Resource.Builder resourceBuilder = Resource.builder(SwaggerListingResource.class);

        resourceBuilder.path(joinPath(relativePath, "swagger.{type:json|yaml}"));

        jersey.getResourceConfig()
              .registerResources(resourceBuilder.build());

        return swagger;
    }

    private static <T> void substituteMap(Callable<Map<String, T>> getter,
                                          Consumer<Map<String, T>> setter)
    {
        try
        {
            Map<String, T> map = getter.call();
            if (map != null)
            {
                setter.accept(new TreeMap<>(map));
            }
        }
        catch (Exception e)
        {
            // Doesn't really happen.
        }
    }

    private static Model getRoot(Map<String, Model> definitions,
                                 ComposedModel model)
    {
        while (true)
        {
            RefModel refModel    = (RefModel) model.getParent();
            String   ref         = refModel.getSimpleRef();
            Model    parentModel = definitions.get(ref);

            ComposedModel composedModel = Reflection.as(parentModel, ComposedModel.class);
            if (composedModel == null)
            {
                return parentModel;
            }

            model = composedModel;
        }
    }

    private static String getTypeNameFromExtensions(ComposedModel model)
    {
        Map<String, Object> composedExts = model.getVendorExtensions();
        return (String) composedExts.get(SwaggerExtensions.TYPE_NAME.getText());
    }

    private static Type unwrapJavaType(Type type)
    {
        JavaType javaType = Reflection.as(type, JavaType.class);
        if (javaType != null)
        {
            if (!javaType.isContainerType())
            {
                return javaType.getRawClass();
            }
        }

        return type;
    }
}

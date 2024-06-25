/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.sdk.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.SwaggerExtensions;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.codegen.AbstractGenerator;
import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenSecurity;
import io.swagger.codegen.Generator;
import io.swagger.codegen.GlobalSupportingFile;
import io.swagger.codegen.InlineModelResolver;
import io.swagger.codegen.SupportingFile;
import io.swagger.models.ComposedModel;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.AbstractProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Optio3Generator extends AbstractGenerator implements Generator
{
    public static class EnumDefinition
    {
        public final String       exportName;
        public final String       enumName;
        public final String       finalEnumName;
        public final List<String> lines = Lists.newArrayList();
        public final Set<String>  values;

        public EnumDefinition lookup;

        EnumDefinition(String exportName,
                       String str)
        {
            // OPTIO3_ENUM_DEF_START: {{enumName}} {{{values}}}

            str = str.replaceFirst(" ", ", ")
                     .replace("[", "")
                     .replaceAll("]", "");
            String[] parts = str.split(", ");

            this.exportName = exportName;
            this.enumName   = parts[0];
            this.values     = Sets.newHashSet();

            for (int i = 1; i < parts.length; i++)
            {
                values.add(parts[i]);
            }

            finalEnumName = getFinalEnumTypeName(enumName);
        }

        EnumDefinition(String name,
                       List<String> values)
        {
            this.exportName = null;
            this.enumName   = name;
            this.values     = Sets.newHashSet(values);

            finalEnumName = getFinalEnumTypeName(enumName);
        }

        public String getReferenceName()
        {
            return exportName == null ? enumName : exportName + "." + enumName;
        }

        private static String getFinalEnumTypeName(String enumName)
        {
            String name = enumName.replace("$", "");
            int    pos  = enumName.lastIndexOf(".");
            if (pos >= 0)
            {
                name = name.substring(pos + 1);
            }

            return name;
        }
    }

    public class ProcessedTemplate
    {
        private static final String LICENSE_END = "// OPTIO3_LICENSE_END";

        private static final String DEF_START = "// OPTIO3_ENUM_DEF_START: ";
        private static final String DEF_END   = "// OPTIO3_ENUM_DEF_END";

        private static final String EXPORT_START             = "// OPTIO3_ENUM_EXPORT_START: ";
        private static final String EXPORT_START_NONAMESPACE = "// OPTIO3_ENUM_EXPORT_START_NONAMESPACE";
        private static final String EXPORT_END               = "// OPTIO3_ENUM_EXPORT_END";

        public final String       fileName;
        public final List<String> lines;
        public final int          licenseEnd;

        private final Map<String, String> enumRenaming = Maps.newHashMap();

        ProcessedTemplate(String fileName,
                          String contents)
        {
            this.fileName = fileName;
            this.lines    = Lists.newArrayList();

            boolean        adding     = true;
            String         exportName = null;
            EnumDefinition def        = null;
            int            licenseEnd = -1;

            Map<String, EnumDefinition> enumDefinitions = Maps.newHashMap();

            for (String line : Lists.newArrayList(contents.split("\n")))
            {
                if (line.equals("import io.swagger.annotations.ApiModel;"))
                {
                    // Bogus import from Swagger CodeGen.
                    continue;
                }

                if (m_processEnumDirectives)
                {
                    if (line.contains(LICENSE_END))
                    {
                        licenseEnd = lines.size();
                        continue;
                    }
                    if (line.contains(EXPORT_START_NONAMESPACE))
                    {
                        exportName = null;
                        adding     = false;
                        continue;
                    }

                    int pos = line.indexOf(EXPORT_START);
                    if (pos >= 0)
                    {
                        exportName = line.substring(pos + EXPORT_START.length());
                        adding     = false;
                        continue;
                    }
                }

                if (adding)
                {
                    lines.add(line);
                }
                else
                {
                    int pos = line.indexOf(DEF_START);
                    if (pos >= 0)
                    {
                        String str = line.substring(pos + DEF_START.length());
                        def = new EnumDefinition(exportName, str);

                        if (def.values.isEmpty())
                        {
                            throw Exceptions.newRuntimeException("Invalid definition for %s: %s", def.getReferenceName(), line);
                        }

                        for (EnumDefinition er : m_enumMapping.values())
                        {
                            if (computeDifference(def.values, er.values) == null)
                            {
                                def.lookup = er;
                                break;
                            }
                        }

                        if (def.lookup == null)
                        {
                            throw Exceptions.newRuntimeException("Can't find definition for %s", def.getReferenceName());
                        }

                        enumDefinitions.put(def.getReferenceName(), def);
                        continue;
                    }

                    if (line.contains(DEF_END))
                    {
                        def = null;
                    }

                    if (def != null)
                    {
                        def.lines.add(line);
                    }
                }

                if (line.contains(EXPORT_END))
                {
                    adding = true;
                }
            }

            this.licenseEnd = licenseEnd;

            for (EnumDefinition ed : enumDefinitions.values())
            {
                EnumDefinition target = ed.lookup;

                enumRenaming.put(ed.getReferenceName(), m_prefix + target.finalEnumName);

                if (target.lines.isEmpty())
                {
                    if (licenseEnd >= 0)
                    {
                        target.lines.addAll(lines.subList(0, licenseEnd));

                        //
                        // Compute how much indentation we should remove.
                        //
                        int max = Integer.MAX_VALUE;
                        for (String line : ed.lines)
                        {
                            int pos = 0;
                            while (pos < line.length() && line.charAt(pos) == ' ')
                            {
                                pos++;
                            }

                            max = Math.min(max, pos);
                        }

                        for (String line : ed.lines)
                        {
                            String lineTrimmed = line.substring(max);
                            lineTrimmed = lineTrimmed.replace(ed.enumName, target.finalEnumName);

                            target.lines.add(lineTrimmed);
                        }
                    }
                }
            }
        }

        File emit()
        {
            try
            {
                StringBuilder sb = new StringBuilder();

                for (String line : lines)
                {
                    String       lineOut = line;
                    List<String> keys    = Lists.newArrayList(enumRenaming.keySet());

                    keys.sort((l, r) ->
                              {
                                  //
                                  // Longer keys should be placed at the top, otherwise we could perform a partial replacement.
                                  // For example, mapping StatusEnum => Foo and LongStatusEnum => Bar would lead to LongFoo, instead of Bar.
                                  //
                                  int diff = -(l.length() - r.length());
                                  if (diff == 0)
                                  {
                                      diff = l.compareTo(r);
                                  }

                                  return diff;
                              });

                    Map<String, String> disambiguate = Maps.newHashMap();
                    var                 random       = new Random();

                    for (String key : keys)
                    {
                        disambiguate.put(key, "__random__" + random.nextDouble() + "__");
                    }

                    for (String key : keys)
                    {
                        lineOut = lineOut.replace(key, disambiguate.get(key));
                    }

                    for (String key : keys)
                    {
                        lineOut = lineOut.replace(disambiguate.get(key), enumRenaming.get(key));
                    }

                    //
                    // TrimAtEnd
                    //
                    {
                        int len = lineOut.length();

                        while (len > 0 && (lineOut.charAt(len - 1) <= ' '))
                        {
                            len--;
                        }

                        lineOut = lineOut.substring(0, len);
                    }

                    sb.append(lineOut);
                    sb.append("\n");
                }

                return writeToFile(fileName, sb.toString());
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    public static final Logger LOGGER = LoggerFactory.getLogger(Optio3Generator.class);

    protected CodegenConfig  config;
    protected ClientOptInput opts;
    protected Swagger        swagger;
    private   String         m_basePath;
    private   String         m_basePathWithoutHost;
    private   String         m_contextPath;

    private String  m_prefix;
    private boolean m_processEnumDirectives;

    private final List<ProcessedTemplate> m_processedFiles = Lists.newArrayList();

    private final Map<String, EnumDefinition> m_enumMapping     = Maps.newHashMap();
    private final Map<String, Model>          m_modelsWithFixup = Maps.newHashMap();

    public Optio3Generator(String referencePrefix,
                           boolean processEnumDirectives)
    {
        m_prefix                = referencePrefix;
        m_processEnumDirectives = processEnumDirectives;
    }

    @Override
    public Generator opts(ClientOptInput opts)
    {
        this.opts    = opts;
        this.swagger = opts.getSwagger();
        this.config  = opts.getConfig();
        this.config.additionalProperties()
                   .putAll(opts.getOpts()
                               .getProperties());

        configureGeneratorProperties();
        configureSwaggerInfo();

        return this;
    }

    public void dumpSwagger()
    {
        Json.prettyPrint(swagger);
    }

    public List<ProcessedTemplate> getProcessedTemplates()
    {
        return m_processedFiles;
    }

    public List<File> emitProcessedFiles()
    {
        List<File> res = Lists.newArrayList();

        for (ProcessedTemplate pt : m_processedFiles)
        {
            res.add(pt.emit());
        }

        return res;
    }

    private String getScheme()
    {
        String scheme;
        if (swagger.getSchemes() != null && !swagger.getSchemes()
                                                    .isEmpty())
        {
            scheme = config.escapeText(swagger.getSchemes()
                                              .get(0)
                                              .toValue());
        }
        else
        {
            scheme = "https";
        }
        scheme = config.escapeText(scheme);
        return scheme;
    }

    private String getHost()
    {
        StringBuilder hostBuilder = new StringBuilder();
        hostBuilder.append(getScheme());
        hostBuilder.append("://");
        if (swagger.getHost() != null)
        {
            hostBuilder.append(swagger.getHost());
        }
        else
        {
            hostBuilder.append("localhost");
        }
        if (swagger.getBasePath() != null)
        {
            hostBuilder.append(swagger.getBasePath());
        }
        return hostBuilder.toString();
    }

    private void configureGeneratorProperties()
    {
        // Additional properties added for tests to exclude references in project related files
        config.additionalProperties()
              .put(CodegenConstants.GENERATE_API_TESTS, false);
        config.additionalProperties()
              .put(CodegenConstants.GENERATE_MODEL_TESTS, false);
        config.additionalProperties()
              .put(CodegenConstants.EXCLUDE_TESTS, true);

        config.processOpts();
        config.preprocessSwagger(swagger);
        config.additionalProperties()
              .put("generatedDate", new Date().toString());
        config.additionalProperties()
              .put("generatorClass",
                   config.getClass()
                         .getName());
        config.additionalProperties()
              .put("inputSpec", config.getInputSpec());
        if (swagger.getVendorExtensions() != null)
        {
            config.vendorExtensions()
                  .putAll(swagger.getVendorExtensions());
        }

        m_contextPath         = config.escapeText(swagger.getBasePath() == null ? "" : swagger.getBasePath());
        m_basePath            = config.escapeText(getHost());
        m_basePathWithoutHost = config.escapeText(swagger.getBasePath());
    }

    private void configureSwaggerInfo()
    {
        Info info = swagger.getInfo();
        if (info == null)
        {
            return;
        }

        Map<String, Object> additionalProperties = config.additionalProperties();

        if (info.getTitle() != null)
        {
            additionalProperties.put("appName", config.escapeText(info.getTitle()));
        }

        if (info.getVersion() != null)
        {
            additionalProperties.put("appVersion", config.escapeText(info.getVersion()));
        }

        if (StringUtils.isEmpty(info.getDescription()))
        {
            // set a default description if none if provided
            additionalProperties.put("appDescription", "No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)");
            additionalProperties.put("unescapedAppDescription", "No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)");
        }
        else
        {
            additionalProperties.put("appDescription", config.escapeText(info.getDescription()));
            additionalProperties.put("unescapedAppDescription", info.getDescription());
        }

        if (info.getContact() != null)
        {
            Contact contact = info.getContact();
            additionalProperties.put("infoUrl", config.escapeText(contact.getUrl()));
            if (contact.getEmail() != null)
            {
                additionalProperties.put("infoEmail", config.escapeText(contact.getEmail()));
            }
        }

        if (info.getLicense() != null)
        {
            License license = info.getLicense();
            if (license.getName() != null)
            {
                additionalProperties.put("licenseInfo", config.escapeText(license.getName()));
            }
            if (license.getUrl() != null)
            {
                additionalProperties.put("licenseUrl", config.escapeText(license.getUrl()));
            }
        }

        if (info.getVersion() != null)
        {
            additionalProperties.put("version", config.escapeText(info.getVersion()));
        }

        if (info.getTermsOfService() != null)
        {
            additionalProperties.put("termsOfService", config.escapeText(info.getTermsOfService()));
        }
    }

    private List<Object> generateModels(List<ProcessedTemplate> files)
    {
        List<Object> allModels = Lists.newArrayList();

        final Map<String, Model> definitions = swagger.getDefinitions();
        if (definitions != null)
        {
            //
            // In the Swagger input model, subtypes are tracked as an array of maps.
            // But they are just ObjectNodes, which Mustache can't handle,
            // so we convert the array of ObjectNodes into arrays of maps.
            // This allows the Mustache template to access the value of the type name and its discriminator.
            //
            for (Model model : definitions.values())
            {
                Model modelActual = extractChildModel(model);

                List<ObjectNode> subtypes = Optio3ModelUtils.getVendorExtensionListValue(modelActual, SwaggerExtensions.SUBTYPES, ObjectNode.class);
                if (subtypes != null)
                {
                    List<Map<String, String>> subTypesAsMap = Lists.newArrayList();

                    String subtype_name  = SwaggerExtensions.SUBTYPE_NAME.getText();
                    String subtype_model = SwaggerExtensions.SUBTYPE_MODEL.getText();

                    for (ObjectNode subtype : subtypes)
                    {
                        String vName = subtype.get(subtype_name)
                                              .asText();
                        String vModel = subtype.get(subtype_model)
                                               .asText();

                        Map<String, String> map = Maps.newHashMap();
                        map.put(subtype_name, vName);
                        map.put(subtype_model, config.toModelName(vModel));
                        subTypesAsMap.add(map);
                    }

                    Optio3ModelUtils.setVendorExtensionValue(modelActual, SwaggerExtensions.SUBTYPES, subTypesAsMap);
                }
            }

            //
            // Collect the definitions of Enums, which are copied in each Model referring to them,
            // so we can create a standalone file for them, in order to reference the same definition from all Models.
            //
            for (Model model : definitions.values())
            {
                extractEnumInformation(model);
            }

            Set<String>              modelKeys                     = definitions.keySet();
            Multimap<String, String> hierarchyFromParentToChildren = HashMultimap.create();
            Map<String, String>      hierarchyFromChildToParent    = Maps.newHashMap();
            Set<String>              typesWithFixup                = Sets.newHashSet();

            for (String modelName : modelKeys)
            {
                Model  model           = definitions.get(modelName);
                String modelParentName = Optio3ModelUtils.getParentName(model);
                if (modelParentName != null)
                {
                    if (!modelParentName.equals(modelName))
                    {
                        hierarchyFromParentToChildren.put(modelParentName, modelName);
                        hierarchyFromChildToParent.put(modelName, modelParentName);
                    }

                    typesWithFixup.add(modelParentName);
                }

                typesWithFixup.add(modelName);
            }

            for (String modelName : hierarchyFromParentToChildren.keySet())
            {
                for (String modelChildName : hierarchyFromParentToChildren.get(modelName))
                {
                    if (modelChildName.equals(modelName))
                    {
                        // Don't add tag to root.
                        continue;
                    }

                    Model modelRaw = definitions.get(modelChildName);
                    Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.TYPE_NAME_SUPER, modelName);
                }
            }

            for (boolean needMoreFixup = !typesWithFixup.isEmpty(); needMoreFixup; )
            {
                needMoreFixup = false;

                for (String modelName : modelKeys)
                {
                    Model modelRaw = definitions.get(modelName);
                    Model model    = extractChildModel(modelRaw);

                    List<Map<String, String>> fixupNumber     = Lists.newArrayList();
                    List<Map<String, String>> fixupDate       = Lists.newArrayList();
                    List<Map<String, String>> fixupSimple     = Lists.newArrayList();
                    List<Map<String, String>> fixupArray      = Lists.newArrayList();
                    List<Map<String, String>> fixupArrayDate  = Lists.newArrayList();
                    List<Map<String, String>> fixupArrayArray = Lists.newArrayList();
                    List<Map<String, String>> fixupMap        = Lists.newArrayList();

                    while (model != null)
                    {
                        Map<String, Property> props = model.getProperties();
                        if (props != null)
                        {
                            for (String propName : props.keySet())
                            {
                                Property prop = props.get(propName);

                                String varName = config.toApiVarName(propName);

                                FixupDetails details = FixupDetails.check(hierarchyFromChildToParent, typesWithFixup, varName, prop);
                                if (details != null)
                                {
                                    if (details.match())
                                    {
                                        if (details.isNumber)
                                        {
                                            fixupNumber.add(details.map);
                                        }
                                        else if (details.isDate)
                                        {
                                            fixupDate.add(details.map);
                                        }
                                        else
                                        {
                                            fixupSimple.add(details.map);
                                        }
                                    }
                                    else if (details.match(ArrayProperty.class))
                                    {
                                        if (details.isNumber)
                                        {
                                            // No need to fixup arrays of numbers.
                                        }
                                        else if (details.isDate)
                                        {
                                            fixupArrayDate.add(details.map);
                                        }
                                        else
                                        {
                                            fixupArray.add(details.map);
                                        }
                                    }
                                    else if (details.match(ArrayProperty.class, ArrayProperty.class))
                                    {
                                        if (details.isNumber)
                                        {
                                            // No need to fixup arrays of numbers.
                                        }
                                        else
                                        {
                                            fixupArrayArray.add(details.map);
                                        }
                                    }
                                    else if (details.match(MapProperty.class))
                                    {
                                        if (details.isNumber)
                                        {
                                            // No need to fixup maps of numbers.
                                        }
                                        else
                                        {
                                            fixupMap.add(details.map);
                                        }
                                    }
                                    else
                                    {
                                        throw Exceptions.newRuntimeException("Unsupported structure for field requiring fixup: %s.%s", modelName, propName);
                                    }
                                }
                            }
                        }

                        ComposedModel model2 = Reflection.as(model, ComposedModel.class);
                        if (model2 == null)
                        {
                            break;
                        }

                        model = model2.getChild();
                    }

                    if (!fixupNumber.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_NUMBER, fixupNumber);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }

                    if (!fixupDate.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_DATE, fixupDate);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }

                    if (!fixupSimple.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_SIMPLE, fixupSimple);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }

                    if (!fixupArray.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_ARRAY, fixupArray);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }

                    if (!fixupArrayDate.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_ARRAY_DATE, fixupArrayDate);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }

                    if (!fixupArrayArray.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_ARRAY_ARRAY, fixupArrayArray);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }

                    if (!fixupMap.isEmpty())
                    {
                        Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP_MAP, fixupMap);

                        needMoreFixup |= typesWithFixup.add(modelName);
                    }
                }
            }

            for (String modelName : typesWithFixup)
            {
                Model modelRaw = definitions.get(modelName);
                Optio3ModelUtils.setVendorExtensionValue(modelRaw, SwaggerExtensions.FIXUP, "true");

                m_modelsWithFixup.put(modelName, modelRaw);
            }

            //--//

            Comparator<String> comparator = (o1, o2) ->
            {
                Model model1 = definitions.get(o1);
                Model model2 = definitions.get(o2);

                int model1InheritanceDepth = Optio3ModelUtils.getInheritanceDepth(definitions, model1);
                int model2InheritanceDepth = Optio3ModelUtils.getInheritanceDepth(definitions, model2);

                if (model1InheritanceDepth == model2InheritanceDepth)
                {
                    return ObjectUtils.compare(config.toModelName(o1), config.toModelName(o2));
                }
                else if (model1InheritanceDepth > model2InheritanceDepth)
                {
                    return 1;
                }
                else
                {
                    return -1;
                }
            };

            //
            // Select all models to emit.
            //
            TreeMap<String, Object> allProcessedModels_v1 = new TreeMap<>(comparator);
            for (String name : modelKeys)
            {
                try
                {
                    //
                    // Don't generate models that have an import mapping
                    //
                    if (config.importMapping()
                              .containsKey(name))
                    {
                        LOGGER.info("Model " + name + " not imported due to import mapping");
                        continue;
                    }

                    Model              model    = definitions.get(name);
                    Map<String, Model> modelMap = new HashMap<String, Model>();
                    modelMap.put(name, model);
                    Map<String, Object> models = processModels(config, modelMap, definitions);
                    models.put("classname", config.toModelName(name));
                    models.putAll(config.additionalProperties());
                    allProcessedModels_v1.put(name, models);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Could not process model '" + name + "'" + ".Please make sure that your schema is correct!", e);
                }
            }

            // post process all processed models
            TreeMap<String, Object> allProcessedModels_v2 = new TreeMap<>(comparator);
            allProcessedModels_v2.putAll(config.postProcessAllModels(allProcessedModels_v1));

            // generate files based on processed models
            for (String modelName : allProcessedModels_v2.keySet())
            {
                @SuppressWarnings("unchecked") Map<String, Object> models = (Map<String, Object>) allProcessedModels_v2.get(modelName);
                try
                {
                    //don't generate models that have an import mapping
                    if (config.importMapping()
                              .containsKey(modelName))
                    {
                        continue;
                    }

                    for (String templateName : config.modelTemplateFiles()
                                                     .keySet())
                    {
                        String suffix = config.modelTemplateFiles()
                                              .get(templateName);
                        String filename = config.modelFileFolder() + File.separator + config.toModelFilename(modelName) + suffix;

                        ProcessedTemplate written = processTemplateToFile(models, templateName, filename);
                        if (written != null)
                        {
                            files.add(written);
                        }
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Could not generate model '" + modelName + "'", e);
                }
            }

            for (EnumDefinition ed : m_enumMapping.values())
            {
                for (String templateName : config.modelTemplateFiles()
                                                 .keySet())
                {
                    String suffix = config.modelTemplateFiles()
                                          .get(templateName);

                    String name = ed.finalEnumName;

                    String filename = config.modelFileFolder() + File.separator + config.toModelFilename(name) + suffix;

                    files.add(new ProcessedTemplate(filename, String.join("\n", ed.lines)));

                    List<Map<String, Object>> list = Lists.newArrayList();

                    {
                        Map<String, Object> modelMap = Maps.newHashMap();
                        CodegenModel        model    = new CodegenModel();
                        model.classname     = config.toModelName(name);
                        model.classFilename = config.toModelFilename(name);
                        modelMap.put("model", model);
                        list.add(modelMap);
                    }

                    Map<String, Object> models = Maps.newHashMap();
                    models.put("classname", config.toModelName(name));
                    models.put("models", list);

                    allProcessedModels_v2.put(name, models);
                }
            }

            // return a list of processed models
            for (String modelName : allProcessedModels_v2.keySet())
            {
                @SuppressWarnings("unchecked") Map<String, Object> models = (Map<String, Object>) allProcessedModels_v2.get(modelName);
                try
                {
                    //don't generate models that have an import mapping
                    if (config.importMapping()
                              .containsKey(modelName))
                    {
                        continue;
                    }

                    @SuppressWarnings("unchecked") List<Object> list = (List<Object>) models.get("models");
                    allModels.add(list.get(0));
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Could not generate model '" + modelName + "'", e);
                }
            }
        }

        if (System.getProperty("debugModels") != null)
        {
            LOGGER.info("############ Model info ############");
            Json.prettyPrint(allModels);
        }

        return allModels;
    }

    private static class FixupDetails
    {
        List<Class<? extends AbstractProperty>> path = Lists.newArrayList();
        Map<String, String>                     map  = Maps.newHashMap();
        boolean                                 isNumber;
        boolean                                 isDate;

        private static FixupDetails check(Map<String, String> hierarchyFromChildToParent,
                                          Set<String> hierarchicalTypes,
                                          String propName,
                                          Property prop)
        {
            FixupDetails details = null;

            if (prop instanceof RefProperty)
            {
                RefProperty refProperty = (RefProperty) prop;
                String      refName     = refProperty.getSimpleRef();

                if (hierarchicalTypes.contains(refName))
                {
                    details = new FixupDetails();

                    String typeName = refName;
                    while (hierarchyFromChildToParent.containsKey(typeName))
                    {
                        typeName = hierarchyFromChildToParent.get(typeName);
                    }

                    details.map.put("field", propName);
                    details.map.put("type", typeName);
                }
            }
            else if (prop instanceof ArrayProperty)
            {
                ArrayProperty arrayProperty = (ArrayProperty) prop;
                Property      arrayElements = arrayProperty.getItems();

                details = check(hierarchyFromChildToParent, hierarchicalTypes, propName, arrayElements);
                if (details != null)
                {
                    details.path.add(0, ArrayProperty.class);
                }
            }
            else if (prop instanceof MapProperty)
            {
                MapProperty mapProperty = (MapProperty) prop;
                Property    mapValues   = mapProperty.getAdditionalProperties();

                details = check(hierarchyFromChildToParent, hierarchicalTypes, propName, mapValues);
                if (details != null)
                {
                    details.path.add(0, MapProperty.class);
                }
            }
            else if (prop instanceof AbstractNumericProperty)
            {
                details          = new FixupDetails();
                details.isNumber = true;

                details.map.put("field", propName);
            }
            else if (prop instanceof DateProperty || prop instanceof DateTimeProperty)
            {
                details        = new FixupDetails();
                details.isDate = true;

                details.map.put("field", propName);
            }

            return details;
        }

        private boolean match(Class<?>... args)
        {
            if (path.size() != args.length)
            {
                return false;
            }

            for (int i = 0; i < args.length; i++)
            {
                Class<? extends AbstractProperty> actual   = path.get(i);
                Class<?>                          required = args[i];

                if (actual != required)
                {
                    return false;
                }
            }

            return true;
        }
    }

    //--//

    private void extractEnumInformation(Model model)
    {
        if (model instanceof ComposedModel)
        {
            ComposedModel model2 = (ComposedModel) model;

            extractEnumInformation(model2.getChild());
            return;
        }

        Map<String, Property> props = model.getProperties();
        if (props != null)
        {
            for (Property prop : props.values())
            {
                extractEnumInformation(prop);
            }
        }
    }

    //--//

    private Model extractChildModel(Model model)
    {
        while (model instanceof ComposedModel)
        {
            ComposedModel model2 = (ComposedModel) model;

            model2.getInterfaces();

            model = model2.getChild();
        }

        return model;
    }

    private void extractEnumInformation(Property prop)
    {
        if (prop instanceof ArrayProperty)
        {
            ArrayProperty array = (ArrayProperty) prop;

            extractEnumInformation(array.getItems());
            return;
        }

        if (prop instanceof MapProperty)
        {
            MapProperty map = (MapProperty) prop;

            extractEnumInformation(map.getAdditionalProperties());
            return;
        }

        if (prop instanceof StringProperty)
        {
            StringProperty prop2 = (StringProperty) prop;

            Map<String, Object> exts = prop.getVendorExtensions();
            String              val  = (String) exts.get(SwaggerExtensions.ENUM_TYPE.getText());
            if (val != null)
            {
                EnumDefinition ref = new EnumDefinition(val, prop2.getEnum());

                EnumDefinition refOld = m_enumMapping.get(val);
                if (refOld != null)
                {
                    Map<String, Integer> diff = computeDifference(refOld.values, ref.values);
                    if (diff != null)
                    {
                        LOGGER.info("Enum {} defined twice:", val);

                        for (String s : diff.keySet())
                        {
                            if (diff.get(s) < 0)
                            {
                                LOGGER.info(" OldToNew: {}", s);
                            }
                            else
                            {
                                LOGGER.info(" NewToOld: {}", s);
                            }
                        }

                        throw Exceptions.newRuntimeException("Enum %s defined twice", prop.getName());
                    }
                }

                m_enumMapping.put(val, ref);
            }
        }
    }

    private Map<String, Integer> computeDifference(Set<String> left,
                                                   Set<String> right)
    {
        Map<String, Integer> res = null;

        for (String s : left)
        {
            if (!right.contains(s))
            {
                if (res == null)
                {
                    res = Maps.newHashMap();
                }

                res.put(s, -1);
            }
        }

        for (String s : right)
        {
            if (!left.contains(s))
            {
                if (res == null)
                {
                    res = Maps.newHashMap();
                }

                res.put(s, 1);
            }
        }

        return res;
    }

    private List<Object> generateApis(List<ProcessedTemplate> files)
    {
        List<Object>                        allOperations = Lists.newArrayList();
        Map<String, List<CodegenOperation>> paths         = processPaths(swagger.getPaths());

        for (String tag : paths.keySet())
        {
            try
            {
                List<CodegenOperation> ops = paths.get(tag);
                ops.sort((one, another) -> ObjectUtils.compare(one.operationId, another.operationId));

                Map<String, Object> operation = processOperations(config, tag, ops);

                operation.put("basePath", m_basePath);
                operation.put("basePathWithoutHost", m_basePathWithoutHost);
                operation.put("contextPath", m_contextPath);
                operation.put("baseName", tag);
                operation.put("modelPackage", config.modelPackage());
                operation.putAll(config.additionalProperties());
                operation.put("classname", config.toApiName(tag));
                operation.put("classVarName", config.toApiVarName(tag));
                operation.put("importPath", config.toApiImport(tag));
                operation.put("classFilename", config.toApiFilename(tag));

                if (!config.vendorExtensions()
                           .isEmpty())
                {
                    operation.put("vendorExtensions", config.vendorExtensions());
                }

                // Pass sortParamsByRequiredFlag through to the Mustache template...
                boolean sortParamsByRequiredFlag = true;
                if (this.config.additionalProperties()
                               .containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG))
                {
                    sortParamsByRequiredFlag = Boolean.valueOf(this.config.additionalProperties()
                                                                          .get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)
                                                                          .toString());
                }
                operation.put("sortParamsByRequiredFlag", sortParamsByRequiredFlag);

                processMimeTypes(swagger.getConsumes(), operation, "consumes");
                processMimeTypes(swagger.getProduces(), operation, "produces");

                allOperations.add(Maps.newHashMap(operation));
                for (int i = 0; i < allOperations.size(); i++)
                {
                    @SuppressWarnings("unchecked") Map<String, Object> oo = (Map<String, Object>) allOperations.get(i);
                    if (i < (allOperations.size() - 1))
                    {
                        oo.put("hasMore", "true");
                    }
                }

                for (String templateName : config.apiTemplateFiles()
                                                 .keySet())
                {
                    String filename = config.apiFilename(templateName, tag);

                    ProcessedTemplate written = processTemplateToFile(operation, templateName, filename);
                    if (written != null)
                    {
                        files.add(written);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException("Could not generate api file for '" + tag + "'", e);
            }
        }

        if (System.getProperty("debugOperations") != null)
        {
            LOGGER.info("############ Operation info ############");
            Json.prettyPrint(allOperations);
        }

        return allOperations;
    }

    private void generateSupportingFiles(List<ProcessedTemplate> files,
                                         Map<String, Object> bundle)
    {
        for (SupportingFile support : config.supportingFiles())
        {
            try
            {
                String outputFolder = config.outputFolder();
                if (StringUtils.isNotEmpty(support.folder))
                {
                    outputFolder += File.separator + support.folder;
                }

                String outputFilename = outputFolder + File.separator + support.destinationFilename.replace('/', File.separatorChar);

                String templateFile;
                if (support instanceof GlobalSupportingFile)
                {
                    templateFile = null;// config.getCommonTemplateDir() + File.separator + support.templateFile;
                }
                else
                {
                    templateFile = getLocalFullTemplateFile(config, support.templateFile);
                }

                if (templateFile == null)
                {
                    LOGGER.info("Skipped generation of " + support.templateFile + ", no local template file");
                    continue;
                }

                if (templateFile.endsWith("mustache"))
                {
                    String            template = readTemplate(templateFile);
                    Mustache.Compiler compiler = Mustache.compiler();
                    compiler = config.processCompiler(compiler);
                    Template tmpl = compiler.withLoader(new Mustache.TemplateLoader()
                                            {
                                                @Override
                                                public Reader getTemplate(String name)
                                                {
                                                    return getTemplateReader(getFullTemplateFile(config, name + ".mustache"));
                                                }
                                            })
                                            .defaultValue("")
                                            .compile(template);

                    files.add(new ProcessedTemplate(outputFilename, tmpl.execute(bundle)));
                }
                else
                {
                    InputStream in = null;

                    try
                    {
                        in = new FileInputStream(templateFile);
                    }
                    catch (Exception e)
                    {
                        // continue
                    }

                    if (in == null)
                    {
                        in = this.getClass()
                                 .getClassLoader()
                                 .getResourceAsStream(getCPResourcePath(templateFile));
                    }

                    files.add(new ProcessedTemplate(outputFilename, IOUtils.toString(in, Charset.defaultCharset())));
                    in.close();
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException("Could not generate supporting file '" + support + "'", e);
            }
        }
    }

    private String getLocalFullTemplateFile(CodegenConfig config,
                                            String templateFile)
    {
        String template = config.templateDir() + File.separator + templateFile;
        if (new File(template).exists())
        {
            return template;
        }

        if (embeddedTemplateExists(template))
        {
            return template;
        }

        return null;
    }

    private Map<String, Object> buildSupportFileBundle(List<Object> allOperations,
                                                       List<Object> allModels)
    {

        Map<String, Object> bundle = new HashMap<String, Object>();
        bundle.putAll(config.additionalProperties());
        bundle.put("apiPackage", config.apiPackage());

        Map<String, Object> apis = new HashMap<String, Object>();
        apis.put("apis", allOperations);

        if (swagger.getHost() != null)
        {
            bundle.put("host", swagger.getHost());
        }

        bundle.put("swagger", this.swagger);
        bundle.put("basePath", m_basePath);
        bundle.put("basePathWithoutHost", m_basePathWithoutHost);
        bundle.put("scheme", getScheme());
        bundle.put("contextPath", m_contextPath);
        bundle.put("apiInfo", apis);
        bundle.put("models", allModels);
        bundle.put("apiFolder",
                   config.apiPackage()
                         .replace('.', File.separatorChar));
        bundle.put("modelPackage", config.modelPackage());
        List<CodegenSecurity> authMethods = config.fromSecurity(swagger.getSecurityDefinitions());
        if (authMethods != null && !authMethods.isEmpty())
        {
            // sort auth methods to maintain the same order
            authMethods.sort(new Comparator<CodegenSecurity>()
            {
                @Override
                public int compare(CodegenSecurity one,
                                   CodegenSecurity another)
                {
                    return ObjectUtils.compare(one.name, another.name);
                }
            });
            bundle.put("authMethods", authMethods);
            bundle.put("hasAuthMethods", true);
        }
        if (swagger.getExternalDocs() != null)
        {
            bundle.put("externalDocs", swagger.getExternalDocs());
        }
        for (int i = 0; i < allModels.size() - 1; i++)
        {
            @SuppressWarnings("unchecked") HashMap<String, CodegenModel> cm = (HashMap<String, CodegenModel>) allModels.get(i);
            CodegenModel                                                 m  = cm.get("model");
            m.hasMoreModels = true;
        }

        config.postProcessSupportingFileData(bundle);

        if (System.getProperty("debugSupportingFiles") != null)
        {
            LOGGER.info("############ Supporting file info ############");
            Json.prettyPrint(bundle);
        }
        return bundle;
    }

    @Override
    public List<File> generate()
    {
        if (swagger == null || config == null)
        {
            throw new RuntimeException("missing swagger input or config!");
        }

        // resolve inline models
        InlineModelResolver inlineModelResolver = new InlineModelResolver();
        inlineModelResolver.flatten(swagger);

        // models
        List<Object> allModels = generateModels(m_processedFiles);

        // apis
        List<Object> allOperations = generateApis(m_processedFiles);

        Map<String, Object> bundle = buildSupportFileBundle(allOperations, allModels);
        generateSupportingFiles(m_processedFiles, bundle);

        return null;
    }

    private ProcessedTemplate processTemplateToFile(Map<String, Object> templateData,
                                                    String templateName,
                                                    String outputFilename)
    {
        String adjustedOutputFilename = outputFilename.replaceAll("//", "/")
                                                      .replace('/', File.separatorChar);

        String templateFile = getLocalFullTemplateFile(config, templateName);
        if (templateFile == null)
        {
            LOGGER.info("Skipped generation of " + adjustedOutputFilename + ", no template file named '" + templateName + "'");
            return null;
        }

        String            template = readTemplate(templateFile);
        Mustache.Compiler compiler = Mustache.compiler();
        compiler = config.processCompiler(compiler);
        Template tmpl = compiler.withLoader(new Mustache.TemplateLoader()
                                {
                                    @Override
                                    public Reader getTemplate(String name)
                                    {
                                        return getTemplateReader(getLocalFullTemplateFile(config, name + ".mustache"));
                                    }
                                })
                                .defaultValue("")
                                .compile(template);

        return new ProcessedTemplate(adjustedOutputFilename, tmpl.execute(templateData));
    }

    private static void processMimeTypes(List<String> mimeTypeList,
                                         Map<String, Object> operation,
                                         String source)
    {
        if (mimeTypeList == null || mimeTypeList.isEmpty())
        {
            return;
        }
        List<Map<String, String>> c     = new ArrayList<Map<String, String>>();
        int                       count = 0;
        for (String key : mimeTypeList)
        {
            Map<String, String> mediaType = new HashMap<String, String>();
            mediaType.put("mediaType", key);
            count += 1;
            if (count < mimeTypeList.size())
            {
                mediaType.put("hasMore", "true");
            }
            else
            {
                mediaType.put("hasMore", null);
            }
            c.add(mediaType);
        }
        operation.put(source, c);
        String flagFieldName = "has" + source.substring(0, 1)
                                             .toUpperCase() + source.substring(1);
        operation.put(flagFieldName, true);
    }

    public Map<String, List<CodegenOperation>> processPaths(Map<String, Path> paths)
    {
        Map<String, List<CodegenOperation>> ops = new TreeMap<String, List<CodegenOperation>>();
        for (String resourcePath : paths.keySet())
        {
            Path path = paths.get(resourcePath);
            processOperation(resourcePath, "get", path.getGet(), ops, path);
            processOperation(resourcePath, "head", path.getHead(), ops, path);
            processOperation(resourcePath, "put", path.getPut(), ops, path);
            processOperation(resourcePath, "post", path.getPost(), ops, path);
            processOperation(resourcePath, "delete", path.getDelete(), ops, path);
            processOperation(resourcePath, "patch", path.getPatch(), ops, path);
            processOperation(resourcePath, "options", path.getOptions(), ops, path);
        }
        return ops;
    }

    private void processOperation(String resourcePath,
                                  String httpMethod,
                                  Operation operation,
                                  Map<String, List<CodegenOperation>> operations,
                                  Path path)
    {
        if (operation == null)
        {
            return;
        }
        if (System.getProperty("debugOperations") != null)
        {
            LOGGER.info("processOperation: resourcePath= " + resourcePath + "\t;" + httpMethod + " " + operation + "\n");
        }
        List<String> tags = operation.getTags();
        if (tags == null)
        {
            tags = new ArrayList<String>();
            tags.add("default");
        }
        /*
         * build up a set of parameter "ids" defined at the operation level
         * per the swagger 2.0 spec "A unique parameter is defined by a combination of a name and location"
         * i'm assuming "location" == "in"
         */
        Set<String> operationParameters = new HashSet<String>();
        if (operation.getParameters() != null)
        {
            for (Parameter parameter : operation.getParameters())
            {
                operationParameters.add(generateParameterId(parameter));
            }
        }

        //need to propagate path level down to the operation
        if (path.getParameters() != null)
        {
            for (Parameter parameter : path.getParameters())
            {
                //skip propagation if a parameter with the same name is already defined at the operation level
                if (!operationParameters.contains(generateParameterId(parameter)))
                {
                    operation.addParameter(parameter);
                }
            }
        }

        for (String tag : tags)
        {
            try
            {
                CodegenOperation codegenOperation = config.fromOperation(resourcePath, httpMethod, operation, swagger.getDefinitions(), swagger);
                codegenOperation.tags = new ArrayList<String>();
                codegenOperation.tags.add(config.sanitizeTag(tag));
                config.addOperationToGroup(config.sanitizeTag(tag), resourcePath, operation, codegenOperation, operations);

                List<Map<String, List<String>>> securities = operation.getSecurity();
                if (securities == null && swagger.getSecurity() != null)
                {
                    securities = new ArrayList<Map<String, List<String>>>();
                    for (SecurityRequirement sr : swagger.getSecurity())
                    {
                        securities.add(sr.getRequirements());
                    }
                }
                if (securities == null || swagger.getSecurityDefinitions() == null)
                {
                    continue;
                }
                Map<String, SecuritySchemeDefinition> authMethods = new HashMap<String, SecuritySchemeDefinition>();
                for (Map<String, List<String>> security : securities)
                {
                    for (String securityName : security.keySet())
                    {
                        SecuritySchemeDefinition securityDefinition = swagger.getSecurityDefinitions()
                                                                             .get(securityName);
                        if (securityDefinition == null)
                        {
                            continue;
                        }
                        if (securityDefinition instanceof OAuth2Definition)
                        {
                            OAuth2Definition oauth2Definition = (OAuth2Definition) securityDefinition;
                            OAuth2Definition oauth2Operation  = new OAuth2Definition();
                            oauth2Operation.setType(oauth2Definition.getType());
                            oauth2Operation.setAuthorizationUrl(oauth2Definition.getAuthorizationUrl());
                            oauth2Operation.setFlow(oauth2Definition.getFlow());
                            oauth2Operation.setTokenUrl(oauth2Definition.getTokenUrl());
                            oauth2Operation.setScopes(new HashMap<String, String>());
                            for (String scope : security.get(securityName))
                            {
                                if (oauth2Definition.getScopes()
                                                    .containsKey(scope))
                                {
                                    oauth2Operation.addScope(scope,
                                                             oauth2Definition.getScopes()
                                                                             .get(scope));
                                }
                            }
                            authMethods.put(securityName, oauth2Operation);
                        }
                        else
                        {
                            authMethods.put(securityName, securityDefinition);
                        }
                    }
                }
                if (!authMethods.isEmpty())
                {
                    codegenOperation.authMethods    = config.fromSecurity(authMethods);
                    codegenOperation.hasAuthMethods = true;
                }
            }
            catch (Exception ex)
            {
                String msg = "Could not process operation:\n" //
                        + "  Tag: " + tag + "\n"//
                        + "  Operation: " + operation.getOperationId() + "\n" //
                        + "  Resource: " + httpMethod + " " + resourcePath + "\n"//
                        + "  Definitions: " + swagger.getDefinitions() + "\n" //
                        + "  Exception: " + ex.getMessage();
                throw new RuntimeException(msg, ex);
            }
        }
    }

    private static String generateParameterId(Parameter parameter)
    {
        return parameter.getName() + ":" + parameter.getIn();
    }

    private Map<String, Object> processOperations(CodegenConfig config,
                                                  String tag,
                                                  List<CodegenOperation> ops)
    {
        Map<String, Object> operations = new HashMap<String, Object>();
        Map<String, Object> objs       = new HashMap<String, Object>();
        objs.put("classname", config.toApiName(tag));
        objs.put("pathPrefix", config.toApiVarName(tag));

        // check for operationId uniqueness
        Set<String> opIds   = new HashSet<String>();
        int         counter = 0;
        for (CodegenOperation op : ops)
        {
            if (op.returnBaseType != null && op.returnBaseType.startsWith(m_prefix))
            {
                Optio3ModelUtils.setVendorExtensionValue(op.vendorExtensions, SwaggerExtensions.FIXUP, "true");

                if (StringUtils.isEmpty(op.returnContainer))
                {
                    Optio3ModelUtils.setVendorExtensionValue(op.vendorExtensions, SwaggerExtensions.FIXUP_SIMPLE, "true");
                }
                else if (StringUtils.equals(op.returnContainer, "array"))
                {
                    Optio3ModelUtils.setVendorExtensionValue(op.vendorExtensions, SwaggerExtensions.FIXUP_ARRAY, "true");
                }
                else if (StringUtils.equals(op.returnContainer, "map"))
                {
                    Optio3ModelUtils.setVendorExtensionValue(op.vendorExtensions, SwaggerExtensions.FIXUP_MAP, "true");
                }
                else
                {
                    throw Exceptions.newRuntimeException("Unsupported structure for operation requiring fixup: %s, %s", tag, op.returnContainer);
                }
            }

            String opId = op.nickname;
            if (opIds.contains(opId))
            {
                counter++;
                op.nickname += "_" + counter;
            }
            opIds.add(opId);
        }
        objs.put("operation", ops);

        operations.put("operations", objs);
        operations.put("package", config.apiPackage());

        Set<String> allImports = new TreeSet<>();
        for (CodegenOperation op : ops)
        {
            allImports.addAll(op.imports);
        }

        Map<String, String>       importMapping = config.importMapping();
        List<Map<String, String>> imports       = Lists.newArrayList();
        for (String nextImport : allImports)
        {
            String mapping = importMapping.get(nextImport);
            if (mapping == null)
            {
                mapping = config.toModelImport(nextImport);
            }

            if (mapping != null)
            {
                Map<String, String> im = new LinkedHashMap<>();
                im.put("import", mapping);
                imports.add(im);
            }
        }

        operations.put("imports", imports);

        // add a flag to indicate whether there's any {{import}}
        if (!imports.isEmpty())
        {
            operations.put("hasImport", true);
        }
        config.postProcessOperations(operations);
        if (!objs.isEmpty())
        {
            @SuppressWarnings("unchecked") List<CodegenOperation> os = (List<CodegenOperation>) objs.get("operation");

            CodegenOperation op = CollectionUtils.lastElement(os);
            if (op != null)
            {
                op.hasMore = false;
            }
        }
        return operations;
    }

    private Map<String, Object> processModels(CodegenConfig config,
                                              Map<String, Model> definitions,
                                              Map<String, Model> allDefinitions)
    {
        Map<String, Object> objs = new HashMap<String, Object>();
        objs.put("package", config.modelPackage());
        List<Object> models     = new ArrayList<Object>();
        Set<String>  allImports = new LinkedHashSet<String>();
        for (String key : definitions.keySet())
        {
            Model               mm = definitions.get(key);
            CodegenModel        cm = config.fromModel(key, mm, allDefinitions);
            Map<String, Object> mo = new HashMap<String, Object>();
            mo.put("model", cm);
            mo.put("importPath", config.toModelImport(cm.classname));
            models.add(mo);

            allImports.addAll(cm.imports);
        }

        objs.put("models", models);
        Set<String> importSet = new TreeSet<String>();
        for (String nextImport : allImports)
        {
            String mapping = config.importMapping()
                                   .get(nextImport);
            if (mapping == null)
            {
                mapping = config.toModelImport(nextImport);
            }
            if (mapping != null && !config.defaultIncludes()
                                          .contains(mapping))
            {
                importSet.add(mapping);
            }
            // add instantiation types
            mapping = config.instantiationTypes()
                            .get(nextImport);
            if (mapping != null && !config.defaultIncludes()
                                          .contains(mapping))
            {
                importSet.add(mapping);
            }
        }
        List<Map<String, String>> imports = new ArrayList<Map<String, String>>();
        for (String s : importSet)
        {
            Map<String, String> item = new HashMap<String, String>();
            item.put("import", s);
            imports.add(item);
        }
        objs.put("imports", imports);
        config.postProcessModels(objs);
        return objs;
    }
}

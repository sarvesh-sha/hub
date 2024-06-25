/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.sdk.codegen;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.optio3.cloud.client.SwaggerExtensions;
import com.optio3.serialization.Reflection;
import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenResponse;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.codegen.config.CodegenConfigurator;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.codegen.languages.JavaCXFClientCodegen;
import io.swagger.codegen.languages.PythonClientCodegen;
import io.swagger.codegen.languages.TypeScriptAngular2ClientCodegen;
import io.swagger.models.Model;
import io.swagger.models.Response;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import org.apache.commons.lang3.StringUtils;

public class Optio3Coordinator
{
    public static class Optio3JavaCodegen extends JavaCXFClientCodegen
    {
        public Optio3JavaCodegen()
        {
            super();

            specialCharReplacements.put("new", "_new");
            specialCharReplacements.put("private", "_private");
            specialCharReplacements.put("true", "_true");
            specialCharReplacements.put("false", "_false");
        }

//        @Override
//        public String toApiName(String name)
//        {
//            return super.toApiName(name);
//        }
//
//        @Override
//        public String toModelName(String name)
//        {
//            return escapeUnderscores(name, super::toModelName);
//        }

        @Override
        public String toVarName(String name)
        {
            return escapeUnderscores(name, super::toVarName);
        }

        @Override
        public String toApiVarName(String name)
        {
            return escapeUnderscores(name, super::toApiVarName);
        }

        @Override
        public String toEnumVarName(String value,
                                    String datatype)
        {
            value = value.replace(" ", "_");

            String value2 = super.toEnumVarName(value, datatype);
            if (value2.equals(value.toUpperCase()))
            {
                return value;
            }

            return value2;
        }

        @Override
        public String getName()
        {
            return "optio3-jaxrs-cxf-client";
        }

        @Override
        public CodegenProperty fromProperty(String name,
                                            Property p)
        {
            CodegenProperty property = super.fromProperty(name, p);

            String type = Optio3ModelUtils.extractOptio3Type(p, SwaggerExtensions.TYPE);
            if (type != null)
            {
                property.datatype = type;
                property.baseType = type;
            }

            return property;
        }

        @Override
        public CodegenResponse fromResponse(String responseCode,
                                            Response response)
        {
            CodegenResponse r = super.fromResponse(responseCode, response);

            Property schema = response.getSchema();
            if (schema != null)
            {
                String type = Optio3ModelUtils.extractOptio3Type(schema, SwaggerExtensions.TYPE);
                if (type != null)
                {
                    r.baseType = type;
                    r.dataType = type;
                }
            }

            return r;
        }

        @Override
        public CodegenParameter fromParameter(Parameter param,
                                              Set<String> imports)
        {
            CodegenParameter p = super.fromParameter(param, imports);

            if (p.isEnum)
            {
                AbstractSerializableParameter<?> ap = Reflection.as(param, AbstractSerializableParameter.class);
                if (ap != null)
                {
                    String sub;

                    if (p.isListContainer)
                    {
                        sub = Optio3ModelUtils.extractOptio3ShortType(ap.getItems(), SwaggerExtensions.ENUM_TYPE);
                        if (sub != null)
                        {
                            imports.add(sub);
                            sub = "List<" + sub + ">";
                        }
                    }
                    else if (p.isMapContainer)
                    {
                        sub = Optio3ModelUtils.extractOptio3ShortType(ap.getItems(), SwaggerExtensions.ENUM_TYPE);
                        if (sub != null)
                        {
                            imports.add(sub);
                            sub = "Map<String, " + sub + ">";
                        }
                    }
                    else
                    {
                        sub = Optio3ModelUtils.extractOptio3ShortType(ap, SwaggerExtensions.ENUM_TYPE);
                        if (sub != null)
                        {
                            imports.add(sub);
                        }
                    }

                    if (sub != null)
                    {
                        p.baseType = sub;
                        p.dataType = sub;
                    }
                }
            }

            if (p.isListContainer)
            {
                String sub = p.dataType;

                int start = sub.lastIndexOf('<');
                int end   = sub.indexOf('>');

                if (start > 0 && end > 0 && start < end)
                {
                    sub = sub.substring(start + 1, end);
                    imports.add(sub);
                }
            }

            BodyParameter bp = Reflection.as(param, BodyParameter.class);
            if (bp != null)
            {
                Model model = bp.getSchema();

                String type = Optio3ModelUtils.extractOptio3Type(model, SwaggerExtensions.TYPE);
                if (type != null)
                {
                    p.baseType = type;
                    p.dataType = type;
                }
            }

            return p;
        }
    }

    private CodegenConfigurator m_configurator;
    private ClientOptInput      m_opts;
    private Optio3Generator     m_generator;
    private boolean             m_processEnumDirectives = true;

    public Optio3Coordinator(String spec,
                             String outputDir)
    {
        m_configurator = new CodegenConfigurator();

        m_configurator.setInputSpec(spec);
        m_configurator.setOutputDir(outputDir);
    }

    //--//

    public void setProcessEnumDirectives(boolean active)
    {
        m_processEnumDirectives = active;
    }

    public Optio3Generator configureForJava(String pkg)
    {
        Map<String, String> importMappings = m_configurator.getImportMappings();
        importMappings.put("LocalDate", "java.time.LocalDate");
        importMappings.put("ZonedDateTime", "java.time.ZonedDateTime");
        importMappings.put("InputStream", "java.io.InputStream");
        importMappings.put("Object", "com.fasterxml.jackson.databind.JsonNode");
        importMappings.put("JsonNode", "com.fasterxml.jackson.databind.JsonNode");
        importMappings.put("JsonNodeType", "com.fasterxml.jackson.databind.JsonNodeType");

        Map<String, String> typeMappings = m_configurator.getTypeMappings();
        typeMappings.put("date", "LocalDate");
        typeMappings.put("DateTime", "ZonedDateTime");
        typeMappings.put("Object", "JsonNode");

        m_configurator.setLang(Optio3JavaCodegen.class.getName());

        m_configurator.setApiPackage(pkg + ".api");
        m_configurator.setModelPackage(pkg + ".model");

        m_opts = m_configurator.toClientOptInput();

        AbstractJavaCodegen codegen = getCodegen(AbstractJavaCodegen.class);
        codegen.setSourceFolder("src/main/java");

        Map<String, Object> props = codegen.additionalProperties();
        props.put(CodegenConstants.TEMPLATE_DIR, "templates/Java");

        m_generator = new Optio3Generator("", m_processEnumDirectives);
        m_generator.opts(m_opts);

        return m_generator;
    }

    //
    // The stock TypeScript codegen insists on camelizing enum names and removing any underscores.
    // We subclass it and override the behavior of just the 'toEnumVarName' method.
    //
    public static class Optio3TypeScriptAngular2ClientCodegen extends TypeScriptAngular2ClientCodegen
    {
        public Optio3TypeScriptAngular2ClientCodegen()
        {
            super();

            specialCharReplacements.put("new", "_new");
            specialCharReplacements.put("private", "_private");
            specialCharReplacements.put("true", "_true");
            specialCharReplacements.put("false", "_false");
        }

//        @Override
//        public String toApiName(String name)
//        {
//            return super.toApiName(name);
//        }
//
//        @Override
//        public String toModelName(String name)
//        {
//            return escapeUnderscores(name, super::toModelName);
//        }

        @Override
        public String toVarName(String name)
        {
            return escapeUnderscores(name, super::toVarName);
        }

        @Override
        public String toApiVarName(String name)
        {
            return escapeUnderscores(name, super::toApiVarName);
        }

        @Override
        public String toEnumVarName(String name,
                                    String datatype)
        {
            if (name.length() == 0)
            {
                return "Empty";
            }

            // for symbol, e.g. $, #
            if (getSymbolName(name) != null)
            {
                return camelize(getSymbolName(name));
            }

            // number
            if ("number".equals(datatype))
            {
                String varName = "NUMBER_" + name;

                varName = varName.replaceAll("-", "MINUS_");
                varName = varName.replaceAll("\\+", "PLUS_");
                varName = varName.replaceAll("\\.", "_DOT_");
                return varName;
            }

            // string
            String enumName = sanitizeName(name);
            enumName = enumName.replaceFirst("^_", "");
            enumName = enumName.replaceFirst("_$", "");

            //
            // Optio3: we don't camelize enums.
            //
            //// camelize the enum variable name
            //// ref: https://basarat.gitbooks.io/typescript/content/docs/enums.html
            //enumName = camelize(enumName);

            if (enumName.matches("\\d.*"))
            { // starts with number
                return "_" + enumName;
            }
            else
            {
                return enumName;
            }
        }

        @Override
        public Map<String, Object> postProcessOperations(Map<String, Object> operations)
        {
            @SuppressWarnings("unchecked") Map<String, Object>    objs = (Map<String, Object>) operations.get("operations");
            @SuppressWarnings("unchecked") List<CodegenOperation> ops  = (List<CodegenOperation>) objs.get("operation");
            for (CodegenOperation op : ops)
            {
                // Convert path to TypeScript template string
                op.path = op.path.replaceAll("\\{(.*?)\\}", "\\$\\{$1\\}");
            }

            return operations;
        }
    }

    public Optio3Generator configureForTypescript()
    {
        Map<String, String> importMappings = m_configurator.getImportMappings();
        importMappings.put("InputStream", "Blob");
        importMappings.put("JsonNode", "any");
        importMappings.put("JsonNodeType", "any");

        Map<String, String> typeMappings = m_configurator.getTypeMappings();
        typeMappings.put("InputStream", "Blob");
        typeMappings.put("JsonNode", "any");
        typeMappings.put("JsonNodeType", "any");

        m_configurator.setLang(Optio3TypeScriptAngular2ClientCodegen.class.getName());

        m_opts = m_configurator.toClientOptInput();

        TypeScriptAngular2ClientCodegen codegen = getCodegen(TypeScriptAngular2ClientCodegen.class);
        codegen.languageSpecificPrimitives()
               .add("Blob");

        Map<String, Object> props = codegen.additionalProperties();
        props.put(CodegenConstants.TEMPLATE_DIR, "templates/Typescript");

        m_generator = new Optio3Generator("models.", m_processEnumDirectives);
        m_generator.opts(m_opts);

        return m_generator;
    }

    //
    // The stock Python codegen does not process enums correctly so we override to make them available when
    // writing the models.
    // We also override all methods that use the underscore method because it does not behave as expected
    //
    public static class Optio3PythonClientCodegen extends PythonClientCodegen
    {
        @Override
        public Map<String, Object> postProcessAllModels(Map<String, Object> objs)
        {
            objs = super.postProcessAllModels(objs);
            // Index all CodegenModels by model name.
            Map<String, CodegenModel> allModels = new HashMap<>();
            for (Map.Entry<String, Object> entry : objs.entrySet())
            {
                String                                                   modelName = toModelName(entry.getKey());
                @SuppressWarnings("unchecked") Map<String, Object>       inner     = (Map<String, Object>) entry.getValue();
                @SuppressWarnings("unchecked") List<Map<String, Object>> models    = (List<Map<String, Object>>) inner.get("models");
                for (Map<String, Object> mo : models)
                {
                    CodegenModel cm = (CodegenModel) mo.get("model");
                    allModels.put(modelName, cm);
                }
            }

            // Let parent know about all its children
            for (String name : allModels.keySet())
            {
                CodegenModel cm     = allModels.get(name);
                CodegenModel parent = allModels.get(cm.parent);
                while (parent != null)
                {
                    if (parent.discriminator != null)
                    {
                        cm.vendorExtensions.putIfAbsent("x-optio3-parent-discriminator", parent.discriminator);
                        break;
                    }

                    parent = allModels.get(parent.parent);
                }
            }

            return objs;
        }

        @Override
        public Map<String, Object> postProcessModels(Map<String, Object> objs)
        {
            // process enum in models
            @SuppressWarnings("unchecked") List<Object> models = (List<Object>) postProcessModelsEnum(objs).get("models");
            for (Object _mo : models)
            {
                @SuppressWarnings("unchecked") Map<String, Object> mo = (Map<String, Object>) _mo;
                CodegenModel                                       cm = (CodegenModel) mo.get("model");
                cm.imports = new TreeSet<>(cm.imports);
                for (CodegenProperty var : cm.vars)
                {
                    // name enum with model name, e.g. StatuEnum => Pet.StatusEnum
                    if (Boolean.TRUE.equals(var.isEnum))
                    {
                        var.datatypeWithEnum = var.datatypeWithEnum.replace(var.enumName, cm.classname + "." + var.enumName);
                    }
                }
            }

            return objs;
        }

        /**
         * Custom implementation of underscore similar to DefaultCodegen but does not split consecutive capital letters.
         */
        public static String underscore(String word)
        {
            String secondPattern      = "([a-z\\d])([A-Z])";
            String replacementPattern = "$1_$2";
            // Replace package separator with slash.
            word = word.replaceAll("\\.", "/");
            // Replace $ with two underscores for inner classes.
            word = word.replaceAll("\\$", "__");
            word = word.replaceAll(secondPattern, replacementPattern);
            word = word.replace('-', '_');
            word = word.toLowerCase();
            return word;
        }

        private static String dropDots(String str)
        {
            return str.replaceAll("\\.", "_");
        }

        @Override
        public String toVarName(String name)
        {
            // sanitize name
            name = sanitizeName(name);

            // remove dollar sign
            name = name.replaceAll("$", "");

            // if it's all uppper case, convert to lower case
            if (name.matches("^[A-Z_]*$"))
            {
                name = name.toLowerCase();
            }

            // underscore the variable name
            // petId => pet_id
            name = underscore(name);

            // remove leading underscore
            name = name.replaceAll("^_*", "");

            // for reserved word or word starting with number, append _
            if (isReservedWord(name) || name.matches("^\\d.*"))
            {
                name = escapeReservedWord(name);
            }

            return name;
        }

        @Override
        public String toModelFilename(String name)
        {
            name = sanitizeName(name);
            // remove dollar sign
            name = name.replaceAll("$", "");

            // model name cannot use reserved keyword, e.g. return
            if (isReservedWord(name))
            {
                LOGGER.warn(name + " (reserved word) cannot be used as model filename. Renamed to " + underscore(dropDots("model_" + name)));
                name = "model_" + name; // e.g. return => ModelReturn (after camelize)
            }

            // model name starts with number
            if (name.matches("^\\d.*"))
            {
                LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + underscore("model_" + name));
                name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
            }

            if (!StringUtils.isEmpty(modelNamePrefix))
            {
                name = modelNamePrefix + "_" + name;
            }

            if (!StringUtils.isEmpty(modelNameSuffix))
            {
                name = name + "_" + modelNameSuffix;
            }

            // underscore the model file name
            // PhoneNumber => phone_number
            return underscore(dropDots(name));
        }

        @Override
        public String toApiFilename(String name)
        {
            // replace - with _ e.g. created-at => created_at
            name = name.replaceAll("-", "_");

            // e.g. PhoneNumberApi.rb => phone_number_api.rb
            return underscore(name) + "_api";
        }

        @Override
        public String toApiVarName(String name)
        {
            if (name.length() == 0)
            {
                return "default_api";
            }
            return underscore(name) + "_api";
        }

        @Override
        public String toOperationId(String operationId)
        {
            // throw exception if method name is empty (should not occur as an auto-generated method name will be used)
            if (StringUtils.isEmpty(operationId))
            {
                throw new RuntimeException("Empty method name (operationId) not allowed");
            }

            // method name cannot use reserved keyword, e.g. return
            if (isReservedWord(operationId))
            {
                LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + underscore(sanitizeName("call_" + operationId)));
                operationId = "call_" + operationId;
            }

            return underscore(sanitizeName(operationId));
        }

        public String generatePackageName(String packageName)
        {
            return underscore(packageName.replaceAll("[^\\w]+", ""));
        }
    }

    public Optio3Generator configureForPython()
    {
        m_configurator.setLang(Optio3PythonClientCodegen.class.getName());

        Map<String, String> importMappings = m_configurator.getImportMappings();
        importMappings.put("JsonNode", "object");
        importMappings.put("JsonNodeType", "object");

        Map<String, String> typeMappings = m_configurator.getTypeMappings();
        typeMappings.put("JsonNode", "object");
        typeMappings.put("JsonNodeType", "object");

        m_opts = m_configurator.toClientOptInput();

        PythonClientCodegen codegen = getCodegen(PythonClientCodegen.class);

        Map<String, Object> props = codegen.additionalProperties();
        props.put(CodegenConstants.TEMPLATE_DIR, "templates/python");
        props.put(CodegenConstants.PACKAGE_NAME, "optio3_sdk");

        m_generator = new Optio3Generator("models.", m_processEnumDirectives);
        m_generator.opts(m_opts);

        return m_generator;
    }

    public void deleteOldFiles()
    {
        DefaultCodegen codegen = getCodegen(DefaultCodegen.class);

        deleteAllFiles(codegen.apiFileFolder());
        deleteAllFiles(codegen.modelFileFolder());
    }

    //--//

    private <T extends DefaultCodegen> T getCodegen(Class<T> clz)
    {
        return clz.cast(m_opts.getConfig());
    }

    private void deleteAllFiles(String dir)
    {
        File apiDir = new File(dir);
        if (apiDir.exists() && apiDir.isDirectory())
        {
            for (File f : apiDir.listFiles())
            {
                Optio3Generator.LOGGER.info("Deleting old file {}", f.toPath());
                f.delete();
            }
        }
    }

    private static String escapeUnderscores(String value,
                                            Function<String, String> func)
    {
        value = value.replace(' ', '_');
        value = StringUtils.replace(value, "(", "");
        value = StringUtils.replace(value, ")", "");
        String[] parts = StringUtils.split(value, '_');
        for (int i = 0; i < parts.length; i++)
        {
            if (i > 0 && parts[i].matches("^\\d.*"))
            {
                continue;
            }

            parts[i] = func.apply(parts[i]);
        }

        return String.join("_", parts);
    }
}

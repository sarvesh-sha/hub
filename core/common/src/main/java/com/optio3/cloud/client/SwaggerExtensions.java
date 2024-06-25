/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client;

public enum SwaggerExtensions
{
    //
    // To override the type of a BodyParameter, we add this extension:
    //
    //    "x-optio3-type":"Response"
    //
    TYPE("x-optio3-type"),

    //
    // When a Model or API refers to an enum, Swagger inlines its definition.
    // To reconcile all these inlined definitions with the original type, we add this extension:
    //
    //    "x-optio3-enum-type":"com.optio3.cloud.builder.model.JobStatus"
    //
    ENUM_TYPE("x-optio3-enum-type"),

    //
    // When a Model has a discriminator, we add this extension to list all the subtypes, such that Codegen can emit the correct code:
    //    
    //    "x-optio3-subtypes":[
    //                         { "subtype_model": "JobDefinitionStepForDockerBuild", "subtype_name": "StepForDockerBuild" },
    //                         { "subtype_model": "JobDefinitionStepForDockerRun", "subtype_name": "StepForDockerRun" },
    //                         { "subtype_model": "JobDefinitionStepForGit", "subtype_name": "StepForGit" },
    //                         { "subtype_model": "JobDefinitionStepForMaven", "subtype_name": "StepForMaven" }
    //                      ]
    //
    //
    SUBTYPES("x-optio3-subtypes"),
    SUBTYPE_MODEL("subtype_model"),
    SUBTYPE_NAME("subtype_name"),

    //
    // When a Model is a subtype of another Model, we add this extension, such that Codegen can emit the correct code:
    //
    //    "x-optio3-type-name":"JobDefinitionStepForDockerRun"
    //
    TYPE_NAME("x-optio3-type-name"),

    //
    // When a Model is a subtype of another Model, we add this extension at codegen time (not in the Swagger json) to point to the root of hierarchy:
    //
    //    "x-optio3-type-name-super":"JobDefinitionStep"
    //
    TYPE_NAME_SUPER("x-optio3-type-name-super"),

    //
    // When a Model is a DTO for a Hibernate entity, we add this extension to point to the id of entity:
    //
    //    "x-optio3-external-record-id":"JobDefinition"
    //
    TYPE_TABLE("x-optio3-external-record-id"),

    //
    // When a Model contains subtype or fields of hierarchical types, we add this extension at codegen time (not in the Swagger json):
    //
    //    "x-optio3-fixup":"true"
    //
    FIXUP("x-optio3-fixup"),

    //
    // When a Model contains numeric fields, we add this extension at codegen time (not in the Swagger json):
    //
    //    "x-optio3-fixup-number":[{ "field": "foo"}]
    //
    FIXUP_NUMBER("x-optio3-fixup-number"),

    //
    // When a Model contains date fields, we add this extension at codegen time (not in the Swagger json):
    //
    //    "x-optio3-fixup-date":[{ "field": "foo"}]
    //
    FIXUP_DATE("x-optio3-fixup-date"),

    //
    // When a Model contains fields of hierarchical types, we add this extension at codegen time (not in the Swagger json):
    //
    //    "x-optio3-fixup-simple":[{ "field": "foo", "type", "bar"}]
    //
    FIXUP_SIMPLE("x-optio3-fixup-simple"),

    //
    // When a Model contains array fields of hierarchical types, we add this extension at codegen time (not in the Swagger json):
    //
    //    "x-optio3-fixup-array":[{ "field": "foo", "type", "bar"}]
    //
    FIXUP_ARRAY("x-optio3-fixup-array"),
    FIXUP_ARRAY_DATE("x-optio3-fixup-array-date"),
    FIXUP_ARRAY_ARRAY("x-optio3-fixup-array-array"),

    //
    // When a Model contains map fields of hierarchical types, we add this extension at codegen time (not in the Swagger json):
    //
    //    "x-optio3-fixup-map":[{ "field": "foo", "type", "bar"}]
    //
    FIXUP_MAP("x-optio3-fixup-map"),

    //
    // When an API Path parameter has to be emitted un-encoded:
    //
    //    "x-optio3-raw-path-param": true
    //
    RAW_PATH_PARAM("x-optio3-raw-path-param");

    //--//

    private final String m_text;

    SwaggerExtensions(String text)
    {
        m_text = text;
    }

    public String getText()
    {
        return m_text;
    }
}

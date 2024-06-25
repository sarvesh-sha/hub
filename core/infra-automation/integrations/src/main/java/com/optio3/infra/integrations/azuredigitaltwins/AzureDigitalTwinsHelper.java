/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins;

import java.util.List;
import java.util.Map;

import com.azure.core.util.Configuration;
import com.azure.digitaltwins.core.BasicDigitalTwin;
import com.azure.digitaltwins.core.BasicDigitalTwinMetadata;
import com.azure.digitaltwins.core.BasicRelationship;
import com.azure.digitaltwins.core.DigitalTwinsClient;
import com.azure.digitaltwins.core.DigitalTwinsClientBuilder;
import com.azure.digitaltwins.core.implementation.models.Error;
import com.azure.digitaltwins.core.implementation.models.ErrorResponse;
import com.azure.digitaltwins.core.implementation.models.ErrorResponseException;
import com.azure.digitaltwins.core.models.DigitalTwinsModelData;
import com.azure.digitaltwins.core.models.IncomingRelationship;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.infra.integrations.azuredigitaltwins.schema.BaseSchemaModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.function.CallableWithoutException;
import org.apache.commons.lang3.StringUtils;

public class AzureDigitalTwinsHelper
{
    public static class Credentials
    {
        public String tenantId;
        public String clientId;
        public String clientSecret;
        public String endpoint;

        public void obfuscate()
        {
            char[] secret = clientSecret.toCharArray();

            for (int i = 0; i < secret.length - 3; i++)
            {
                secret[i] = '*';
            }

            clientSecret = new String(secret);
        }
    }

    private final DigitalTwinsClient m_client;

    public final Map<String, InterfaceModel> interfaces = Maps.newHashMap();
    private      boolean                     m_modelsLoaded;

    public AzureDigitalTwinsHelper(Credentials cred)
    {
        Configuration cfg = new Configuration().put(Configuration.PROPERTY_AZURE_TENANT_ID, cred.tenantId)
                                               .put(Configuration.PROPERTY_AZURE_CLIENT_ID, cred.clientId);

        ClientSecretCredential credential = new ClientSecretCredentialBuilder().tenantId(cred.tenantId)
                                                                               .clientId(cred.clientId)
                                                                               .clientSecret(cred.clientSecret)
                                                                               .build();

        m_client = new DigitalTwinsClientBuilder().endpoint(cred.endpoint)
                                                  .credential(credential)
                                                  .configuration(cfg)
                                                  .buildClient();
    }

    public List<DigitalTwinsModelData> listModels()
    {
        List<DigitalTwinsModelData> models = Lists.newArrayList();

        for (DigitalTwinsModelData model : m_client.listModels())
        {
            models.add(model);
        }

        return models;
    }

    public BaseModel parseModel(DigitalTwinsModelData model) throws
                                                             JsonProcessingException
    {
        JsonNode tree = ObjectMappers.SkipNulls.readTree(model.getDtdlModel());

        InterfaceModel itf = InterfaceModel.tryParse(tree);
        if (itf != null)
        {
            return itf;
        }

        RelationshipModel rel = RelationshipModel.tryParse(Maps.newHashMap(), tree);
        if (rel != null)
        {
            return rel;
        }

        return null;
    }

    public void loadModels() throws
                             JsonProcessingException
    {
        if (!m_modelsLoaded)
        {
            for (DigitalTwinsModelData model : m_client.listModels())
            {
                BaseModel parsedModel = parseModel(model);
                if (parsedModel != null)
                {
                    InterfaceModel itf = Reflection.as(parsedModel, InterfaceModel.class);
                    if (itf != null)
                    {
                        interfaces.put(itf.id, itf);
                    }
                }
            }

            for (InterfaceModel itf : interfaces.values())
            {
                if (itf.superClasses != null)
                {
                    for (ExtendModel ext : itf.superClasses.values())
                    {
                        ext.target = interfaces.get(ext.id);
                        if (ext.target == null)
                        {
                            throw Exceptions.newIllegalArgumentException("Cannot resolve schema '%s'", ext.id);
                        }
                    }
                }

                if (itf.properties != null)
                {
                    for (PropertyModel prop : itf.properties.values())
                    {
                        BaseSchemaModel.resolve(prop.schema, interfaces);
                    }
                }

                if (itf.components != null)
                {
                    for (ComponentModel comp : itf.components.values())
                    {
                        BaseSchemaModel.resolve(comp.schema, interfaces);
                    }
                }
            }

            m_modelsLoaded = true;
        }
    }

    public void deleteModel(BaseModelWithId model)
    {
        m_client.deleteModel(model.id);
    }

    //--//

    public boolean areTwinsEquivalent(BasicDigitalTwin twinNew,
                                      BasicDigitalTwin twinExisting)
    {
        if (twinNew != null && twinExisting != null)
        {
            BasicDigitalTwinMetadata metadataNew      = twinNew.getMetadata();
            BasicDigitalTwinMetadata metadataExisting = twinExisting.getMetadata();
            if (StringUtils.equals(metadataExisting.getModelId(), metadataNew.getModelId()))
            {
                Map<String, Object> contentsNew      = ObjectMappers.cloneThroughJson(ObjectMappers.SkipNulls, twinNew.getContents());
                Map<String, Object> contentsExisting = ObjectMappers.cloneThroughJson(ObjectMappers.SkipNulls, twinExisting.getContents());
                return contentsNew.equals(contentsExisting);
            }
        }

        return false;
    }

    public void sanitizeTwin(BasicDigitalTwin basicTwin) throws
                                                         Exception
    {
        loadModels();

        InterfaceModel itf = interfaces.get(basicTwin.getMetadata()
                                                     .getModelId());
        if (itf != null)
        {
            itf.ensureComponentsOnTwin(basicTwin);
        }
    }

    public BasicDigitalTwin createOrReplaceTwin(BasicDigitalTwin basicTwin) throws
                                                                            Exception
    {
        sanitizeTwin(basicTwin);

        return m_client.createOrReplaceDigitalTwin(basicTwin.getId(), basicTwin, BasicDigitalTwin.class);
    }

    public BasicDigitalTwin getTwin(String id)
    {
        return convertNotFoundToNull(() -> m_client.getDigitalTwin(id, BasicDigitalTwin.class));
    }

    public void deleteTwin(String id)
    {
        convertNotFoundToNull(() -> m_client.deleteDigitalTwin(id));
    }

    //--//

    public boolean areRelationshipsEquivalent(BasicRelationship relNew,
                                              BasicRelationship relExisting)
    {
        if (relNew != null && relExisting != null)
        {
            if (StringUtils.equals(relNew.getName(), relExisting.getName()))
            {
                if (StringUtils.equals(relNew.getTargetId(), relExisting.getTargetId()))
                {
                    if (StringUtils.equals(relNew.getSourceId(), relExisting.getSourceId()))
                    {
                        Map<String, Object> contentsNew      = relNew.getProperties();
                        Map<String, Object> contentsExisting = relExisting.getProperties();
                        return contentsNew.equals(contentsExisting);
                    }
                }
            }
        }

        return false;
    }

    public BasicRelationship createOrReplaceRelationship(BasicRelationship rel)
    {
        return m_client.createOrReplaceRelationship(rel.getSourceId(), rel.getId(), rel, BasicRelationship.class);
    }

    public List<BasicRelationship> listRelationships(String digitalTwinId)
    {
        return convertNotFoundToNull(() -> Lists.newArrayList(m_client.listRelationships(digitalTwinId, BasicRelationship.class)));
    }

    public List<IncomingRelationship> listReverseRelationships(String digitalTwinId)
    {
        return convertNotFoundToNull(() -> Lists.newArrayList(m_client.listIncomingRelationships(digitalTwinId)));
    }

    public BasicRelationship getRelationship(String digitalTwinId,
                                             String relationshipId)
    {
        return convertNotFoundToNull(() -> m_client.getRelationship(digitalTwinId, relationshipId, BasicRelationship.class));
    }

    public void deleteRelationship(String digitalTwinId,
                                   String relationshipId)
    {
        convertNotFoundToNull(() -> m_client.deleteRelationship(digitalTwinId, relationshipId));
    }

    //--//

    public List<BasicDigitalTwin> queryTwins(String query)
    {
        List<BasicDigitalTwin> res = Lists.newArrayList();

        for (BasicDigitalTwin twin : m_client.query(query, BasicDigitalTwin.class))
        {
            res.add(twin);
        }

        return res;
    }

    public List<BasicRelationship> queryRelationships(String query)
    {
        List<BasicRelationship> res = Lists.newArrayList();

        for (BasicRelationship twin : m_client.query(query, BasicRelationship.class))
        {
            res.add(twin);
        }

        return res;
    }

    public String getTwinProperty(BasicDigitalTwin twin,
                                  String... path)
    {
        return getProperty(twin.getContents(), path);
    }

    public void setTwinProperty(BasicDigitalTwin twin,
                                Object value,
                                String... path)
    {
        setProperty(twin.getContents(), value, path);
    }

    public String getRelationshipProperty(BasicRelationship relationship,
                                          String... path)
    {
        return getProperty(relationship.getProperties(), path);
    }

    public void setRelationshipProperty(BasicRelationship relationship,
                                        Object value,
                                        String... path)
    {
        setProperty(relationship.getProperties(), value, path);
    }

    @SuppressWarnings("unchecked")
    private String getProperty(Object cursor,
                               String[] path)
    {
        for (String part : path)
        {
            if (cursor instanceof Map)
            {
                Map<String, Object> map = (Map<String, Object>) cursor;

                if (!map.containsKey(part))
                {
                    return null;
                }

                cursor = map.get(part);
            }
            else
            {
                return null;
            }
        }

        return cursor != null ? cursor.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private void setProperty(Map<String, Object> cursor,
                             Object value,
                             String[] path)
    {
        int last = path.length - 1;
        if (last < 0)
        {
            return;
        }

        for (int i = 0; i < last; i++)
        {
            String part = path[i];

            Object next = cursor.get(part);
            if (next instanceof Map)
            {
                cursor = (Map<String, Object>) next;
            }
            else
            {
                Map<String, Object> mapNext = Maps.newHashMap();

                cursor.put(part, mapNext);
                cursor = mapNext;
            }
        }

        cursor.put(path[last], value);
    }

    //--//

    private <T> T convertNotFoundToNull(CallableWithoutException<T> callback)
    {
        try
        {
            return callback.call();
        }
        catch (ErrorResponseException e)
        {
            if (isNotFound(e))
            {
                return null;
            }

            throw e;
        }
    }

    private void convertNotFoundToNull(Runnable callback)
    {
        try
        {
            callback.run();
        }
        catch (ErrorResponseException e)
        {
            if (isNotFound(e))
            {
                return;
            }

            throw e;
        }
    }

    private static boolean isNotFound(ErrorResponseException e)
    {
        ErrorResponse value = e.getValue();
        Error         error = value.getError();
        String        code  = error.getCode();

        if (StringUtils.equals(code, "DigitalTwinNotFound"))
        {
            return true;
        }

        if (StringUtils.equals(code, "RelationshipNotFound"))
        {
            return true;
        }

        return false;
    }
}

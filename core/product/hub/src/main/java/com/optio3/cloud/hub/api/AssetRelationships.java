/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.logic.tags.TagsStreamNextAction;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.AssetRelationshipRequest;
import com.optio3.cloud.hub.model.asset.AssetRelationshipResponse;
import com.optio3.cloud.hub.model.asset.EquipmentReportProgress;
import com.optio3.cloud.hub.model.tags.TagsConditionIsEquipment;
import com.optio3.cloud.hub.orchestration.tasks.TaskForEquipmentReport;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;

@Api(tags = { "AssetRelationships" }) // For Swagger
@Optio3RestEndpoint(name = "AssetRelationships") // For Optio3 Shell
@Path("/v1/assets-relationships")
public class AssetRelationships
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("top-equipments")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<AssetRecord> getTopEquipments()
    {
        TypedRecordIdentityList<AssetRecord> lst = new TypedRecordIdentityList<>();

        TagsEngine.Snapshot          snapshot   = getTagsSnapshot();
        TagsEngine.Snapshot.AssetSet equipments = snapshot.evaluateCondition(new TagsConditionIsEquipment());

        equipments.streamResolved((ri) ->
                                  {
                                      if (snapshot.countRelations(ri.sysId, AssetRelationship.controls, true) == 0)
                                      {
                                          @SuppressWarnings("unchecked") TypedRecordIdentity<AssetRecord> ri2 = (TypedRecordIdentity<AssetRecord>) ri;
                                          lst.add(ri2);
                                      }

                                      return TagsStreamNextAction.Continue;
                                  });

        return lst;
    }

    //--//

    @POST
    @Path("lookup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<AssetRelationshipResponse> lookupBatch(List<AssetRelationshipRequest> reqs)
    {
        TagsEngine.Snapshot snapshot = getTagsSnapshot();

        List<AssetRelationshipResponse> lst = Lists.newArrayList();

        for (AssetRelationshipRequest request : reqs)
        {
            AssetRelationshipResponse response = new AssetRelationshipResponse();

            TagsEngine.Snapshot.AssetSet assetSet = snapshot.resolveRelations(request.assetId, request.relationship, !request.fromParentToChildren);
            assetSet.streamResolved((ri) ->
                                    {
                                        @SuppressWarnings("unchecked") TypedRecordIdentity<AssetRecord> ri2 = (TypedRecordIdentity<AssetRecord>) ri;
                                        response.assets.add(ri2);

                                        return TagsStreamNextAction.Continue;
                                    });

            lst.add(response);
        }

        return lst;
    }

    //--//

    @POST
    @Path("equipment/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String startEquipmentReport(@QueryParam("id") String parentEquipmentSysId) throws
                                                                                      Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider,
                                                                                             (sessionHolder) -> TaskForEquipmentReport.scheduleTask(sessionHolder, parentEquipmentSysId));

        return loc_task.getIdRaw();
    }

    @GET
    @Path("equipment/report/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public EquipmentReportProgress checkEquipmentReport(@PathParam("id") String id,
                                                        @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForEquipmentReport.class);
        }
    }

    @GET
    @Path("equipment/report/excel/{id}/{fileName}")
    @Produces("application/octet-stream")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream streamEquipmentReport(@PathParam("id") String id,
                                             @PathParam("fileName") String fileName) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.streamContents(helper, id, TaskForEquipmentReport.class);
        }
    }

    //--//

    private TagsEngine.Snapshot getTagsSnapshot()
    {
        return m_sessionProvider.getService(TagsEngine.class)
                                .acquireSnapshot(false);
    }
}

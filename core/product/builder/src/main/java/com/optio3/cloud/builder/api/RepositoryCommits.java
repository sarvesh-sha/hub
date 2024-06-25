/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.model.jobs.input.RepositoryCommit;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryCommitRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "RepositoryCommits" }) // For Swagger
@Optio3RestEndpoint(name = "RepositoryCommits") // For Optio3 Shell
@Path("/v1/repository-commits")
public class RepositoryCommits
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RepositoryCommit> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RepositoryCommitRecord> helper = sessionHolder.createHelper(RepositoryCommitRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, RepositoryCommitRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public RepositoryCommit get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, RepositoryCommitRecord.class, id);
    }

    //--//

    @GET
    @Path("item/{id}/parents")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RecordIdentity> getAncestors(@PathParam("id") String id,
                                             @QueryParam("depth") Integer depth)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RepositoryCommitRecord> helper    = sessionHolder.createHelper(RepositoryCommitRecord.class);
            RepositoryCommitRecord               rec       = helper.get(id);
            List<RecordIdentity>                 ancestors = Lists.newArrayList();
            Set<String>                          visited   = Sets.newHashSet();

            walkAncestors(helper, rec.getRepository(), ancestors, visited, rec, depth != null ? depth : Integer.MAX_VALUE);

            return ancestors;
        }
    }

    private static void walkAncestors(RecordHelper<RepositoryCommitRecord> helper,
                                      RepositoryRecord repo,
                                      List<RecordIdentity> ancestors,
                                      Set<String> visited,
                                      RepositoryCommitRecord rec,
                                      int depth)
    {
        while (depth-- > 0)
        {
            String[] parents = rec.getParents();
            if (parents == null)
            {
                return;
            }

            if (parents.length == 1)
            {
                //
                // Special case for single parent, to avoid deep recursions.
                //
                RepositoryCommitRecord rec_parent = shouldProcessAncestor(helper, repo, ancestors, visited, parents[0]);
                if (rec_parent == null)
                {
                    return;
                }

                // Iterate, instead of recurse.
                rec = rec_parent;
                continue;
            }

            for (String parent : parents)
            {
                RepositoryCommitRecord rec_parent = shouldProcessAncestor(helper, repo, ancestors, visited, parent);
                if (rec_parent != null)
                {
                    walkAncestors(helper, repo, ancestors, visited, rec_parent, depth);
                }
            }

            return;
        }
    }

    private static RepositoryCommitRecord shouldProcessAncestor(RecordHelper<RepositoryCommitRecord> helper,
                                                                RepositoryRecord repo,
                                                                List<RecordIdentity> ancestors,
                                                                Set<String> visited,
                                                                String parent)
    {
        if (!visited.add(parent))
        {
            return null;
        }

        RepositoryCommitRecord rec_parent = repo.findCommitByHash(helper, parent);
        if (rec_parent != null)
        {
            ancestors.add(RecordIdentity.newTypedInstance(helper, rec_parent));
        }

        return rec_parent;
    }
}

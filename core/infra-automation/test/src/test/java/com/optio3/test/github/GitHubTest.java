/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.github;

import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import com.optio3.infra.GitHubHelper;
import com.optio3.infra.github.api.OrganizationApi;
import com.optio3.infra.github.api.RepoApi;
import com.optio3.infra.github.model.Organization;
import com.optio3.infra.github.model.Repository;
import com.optio3.infra.github.model.WebHook;
import com.optio3.test.infra.Optio3InfraTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GitHubTest extends Optio3InfraTest
{
    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, false);
    }

    @Ignore("Manually enable to test, since it requires access to GitHub.")
    @Test
    public void listRepos()
    {
        GitHubHelper helper = new GitHubHelper(credDir, null);

        try
        {
            OrganizationApi proxy1 = helper.createProxy(OrganizationApi.class);

            List<Organization> orgs = proxy1.getOrganization();
            for (Organization org : orgs)
            {
                System.out.printf("Org: %s %s  %s %s%n", org.id, org.type, org.name, org.login);
                List<Repository> repos = proxy1.listRepositories(org.login);
                for (Repository repo : repos)
                {
                    System.out.printf("  Repo: %s %s  %s%n", repo.id, repo.name, repo.html_url);
                    RepoApi proxy2 = helper.createProxy(RepoApi.class);

                    List<WebHook> hooks = proxy2.listHooks(repo.owner.login, repo.name);
                    for (WebHook hook : hooks)
                    {
                        System.out.printf("    Hook: %s %s%n", hook.id, hook.url);
                    }
                }
            }
        }
        catch (ClientErrorException e)
        {
            Response response = e.getResponse();
            System.out.println(response.getMediaType());
            response.bufferEntity();
            Object o = response.getEntity();
            System.out.println(o);
        }
    }

    @Ignore("Manually enable to test, since it requires access to GitHub.")
    @Test
    public void listReposWithHelper()
    {
        GitHubHelper helper = new GitHubHelper(credDir, null);

        try
        {
            Organization org = helper.getOrganization();
            System.out.printf("Org: %s %s  %s %s%n", org.id, org.type, org.name, org.login);
            List<Repository> repos = helper.listRepos();
            for (Repository repo : repos)
            {
                System.out.printf("Repo: %s %s  %s%n", repo.id, repo.name, repo.html_url);

                List<WebHook> hooks = helper.listHooks(repo);
                for (WebHook hook : hooks)
                {
                    System.out.printf("Hook: %s %s%n", hook.id, hook.url);
                }
            }
        }
        catch (ClientErrorException e)
        {
            Response response = e.getResponse();
            System.out.println(response.getMediaType());
            response.bufferEntity();
            Object o = response.getEntity();
            System.out.println(o);
        }
    }

    @Ignore("Manually enable to test, since it requires access to GitHub.")
    @Test
    public void testHooks()
    {
        GitHubHelper helper = new GitHubHelper(credDir, null);

        try
        {
            Repository repo = helper.findRepo("https://github.com/optio3/homebrew-devtools.git");
            System.out.printf("Repo: %s %s  %s%n", repo.id, repo.name, repo.html_url);

            List<WebHook> hooks = helper.listHooks(repo);
            for (WebHook hook : hooks)
            {
                System.out.printf("Hook: %s %s%n", hook.id, hook.url);
            }

            WebHook wh = new WebHook();
            wh.name                = "web";
            wh.config.url          = "http://dev.optio3.com";
            wh.config.content_type = "json";
            wh.active              = true;

            helper.addHook(repo, wh);
        }
        catch (ClientErrorException e)
        {
            Response response = e.getResponse();
            System.out.println(response.getMediaType());
            response.bufferEntity();
            Object o = response.getEntity();
            System.out.println(o);
        }
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.optio3.cloud.client.hub.api.EventsApi;
import com.optio3.cloud.client.hub.api.UserMessagesApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.api.WorkflowsApi;
import com.optio3.cloud.client.hub.model.PaginatedRecordIdentityList;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.client.hub.model.UserMessageFilterRequest;
import com.optio3.cloud.client.hub.model.UserMessageWorkflow;
import com.optio3.cloud.client.hub.model.Workflow;
import com.optio3.cloud.client.hub.model.WorkflowDetailsForAssignControlPointsToEquipment;
import com.optio3.cloud.client.hub.model.WorkflowFilterRequest;
import com.optio3.cloud.client.hub.model.WorkflowPriority;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.model.workflow.WorkflowDetails;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.CollectionUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class WorkflowsTest extends Optio3Test
{
    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    static         String sysId_workflow1;
    private static User   user_admin;
    private static User   user_user;

    @Test
    @TestOrder(10)
    public void testCreate()
    {
        loginAsAdmin();

        UsersApi   proxy_users = applicationRule.createProxy("api/v1", UsersApi.class);
        List<User> users       = proxy_users.getAll();
        user_admin = CollectionUtils.findFirst(users, (user2) -> user2.emailAddress.equals("admin@demo.optio3.com"));
        user_user  = CollectionUtils.findFirst(users, (user2) -> user2.emailAddress.equals("user@demo.optio3.com"));

        deleteAllMessages();

        //--//

        WorkflowsApi proxy_workflows = applicationRule.createProxy("api/v1", WorkflowsApi.class);

        Workflow                                         workflow1 = new Workflow();
        WorkflowDetailsForAssignControlPointsToEquipment details   = new WorkflowDetailsForAssignControlPointsToEquipment();
        workflow1.details  = details;
        workflow1.priority = WorkflowPriority.Normal;

        Workflow workflow2 = proxy_workflows.create(workflow1);
        sysId_workflow1 = workflow2.sysId;

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            WorkflowRecord  rec_workflow = holder.getEntity(WorkflowRecord.class, workflow2.sysId);
            WorkflowDetails details2     = rec_workflow.getDetails();
            assertSameClassName(details, details2);
        }

        EventsApi                   proxy_events = applicationRule.createProxy("api/v1", EventsApi.class);
        PaginatedRecordIdentityList ids          = proxy_events.getFiltered(new WorkflowFilterRequest());
        assertEquals(1, ids.results.size());

        Workflow workflow3 = assertCast(Workflow.class, proxy_events.get(ids.results.get(0).sysId));
        assertCast(WorkflowDetailsForAssignControlPointsToEquipment.class, workflow3.details);

        assertEquals(user_admin.sysId, workflow3.createdBy.sysId);
    }

    @Test
    @TestOrder(20)
    public void testAssign()
    {
        WorkflowsApi proxy_workflows = applicationRule.createProxy("api/v1", WorkflowsApi.class);

        Workflow workflowPost = proxy_workflows.assign(sysId_workflow1, user_user.sysId);
        assertEquals(user_admin.sysId, workflowPost.createdBy.sysId);
        assertEquals(user_user.sysId, workflowPost.assignedTo.sysId);

        {
            UserMessagesApi      proxy_userMessages = applicationRule.createProxy("api/v1", UserMessagesApi.class);
            List<RecordIdentity> messages           = proxy_userMessages.getFiltered(new UserMessageFilterRequest());
            assertEquals(1, messages.size());

            UserMessageWorkflow msg = assertCast(UserMessageWorkflow.class, proxy_userMessages.get(messages.get(0).sysId));
            assertEquals("Workflow has been assigned to fulfiller", msg.subject);
        }

        logout();
        loginAsUser();

        {
            UserMessagesApi      proxy_userMessages = applicationRule.createProxy("api/v1", UserMessagesApi.class);
            List<RecordIdentity> messages           = proxy_userMessages.getFiltered(new UserMessageFilterRequest());
            assertEquals(1, messages.size());

            UserMessageWorkflow msg = assertCast(UserMessageWorkflow.class, proxy_userMessages.get(messages.get(0).sysId));
            assertEquals("Workflow assigned to you", msg.subject);
        }
    }

    //--//

    private void assertSameClassName(Object expected,
                                     Object actual)
    {
        final Class<?> expectedClass = expected.getClass();
        final Class<?> actualClass   = actual.getClass();
        assertEquals(expectedClass.getSimpleName(), actualClass.getSimpleName());
    }

    //--//

    private void logout()
    {
        UsersApi proxy_users = applicationRule.createProxy("api/v1", UsersApi.class);
        proxy_users.logout();
    }

    private void loginAsAdmin()
    {
        UsersApi proxy_users = applicationRule.createProxy("api/v1", UsersApi.class);

        User user = proxy_users.login("admin@demo.optio3.com", "adminPwd");
        assertNotNull(user);
    }

    private void loginAsUser()
    {
        UsersApi proxy_users = applicationRule.createProxy("api/v1", UsersApi.class);

        User user = proxy_users.login("user@demo.optio3.com", "userPwd");
        assertNotNull(user);
    }

    private void deleteAllMessages()
    {
        UserMessagesApi proxy_userMessages = applicationRule.createProxy("api/v1", UserMessagesApi.class);

        List<RecordIdentity> messages = proxy_userMessages.getFiltered(new UserMessageFilterRequest());
        for (RecordIdentity ri : messages)
        {
            proxy_userMessages.remove(ri.sysId, null);
        }
    }
}

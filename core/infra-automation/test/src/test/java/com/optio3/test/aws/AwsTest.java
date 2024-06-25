/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.aws;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.util.Base64;
import com.google.common.collect.Maps;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.test.infra.Optio3InfraTest;
import com.optio3.util.CollectionUtils;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AwsTest extends Optio3InfraTest
{
    AwsHelper aws;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(true, true);

        aws = AwsHelper.buildWithDirectoryLookup(credDir, WellKnownSites.optio3DomainName(), Regions.US_WEST_2);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void listMetrics()
    {
        Map<String, AwsHelper.MetricNamespace> metrics = aws.listMetrics();

        for (String namespace : metrics.keySet())
        {
            System.out.printf("Namespace: %s\n", namespace);

            AwsHelper.MetricNamespace namespaceValues = metrics.get(namespace);
            for (String name : namespaceValues.metrics.keySet())
            {
                System.out.printf("    Metric: %s\n", name);

                AwsHelper.MetricName metricType = namespaceValues.metrics.get(name);
                for (AwsHelper.MetricIdentity metricInstances : metricType.instances)
                {
                    System.out.printf("        Dimensions: %s\n",
                                      CollectionUtils.transformToList(metricInstances.dimensions, (dimension) -> String.format("%s=%s", dimension.getName(), dimension.getValue())));
                }
            }
        }

        ZonedDateTime now = TimeUtils.now();

        Map<String, Instance> lookup = Maps.newHashMap();

        for (Instance instance : aws.listInstances(true))
        {
            lookup.put(instance.getInstanceId(), instance);
        }

        for (AwsHelper.MetricIdentity cpuCreditBalance : aws.listMetric("AWS/EC2", "CPUCreditBalance"))
        {
            String   value    = cpuCreditBalance.findByName("InstanceId");
            Instance instance = lookup.get(value);
            if (instance != null)
            {
                Datapoint dp = CollectionUtils.lastElement(aws.getMetric(cpuCreditBalance, now.minus(1, ChronoUnit.HOURS), now, 3600, "Minimum"));
                System.out.printf("Instance '%s': %s %s\n", value, dp.getMinimum(), instance.getTags());
            }
        }

        // instance.blockDeviceMappings.get(0).ebs.volumeId
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void listInstances()
    {
        for (Instance instance : aws.listInstances(true))
        {
            System.out.println(instance.toString());
        }

        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
        describeImagesRequest.withFilters(new Filter("virtualization-type").withValues("hvm"), new Filter("owner-alias").withValues("amazon"));
        DescribeImagesResult describeImagesResponse = aws.getEc2()
                                                         .describeImages(describeImagesRequest);
        for (Image img : describeImagesResponse.getImages())
        {
            System.out.printf("IMAGE: %s%n", img.toString());
        }
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void listUserData()
    {
        DescribeInstancesRequest request = new DescribeInstancesRequest().withFilters(new Filter("instance-state-name").withValues("running"));

        DescribeInstancesResult result = aws.getEc2()
                                            .describeInstances(request);

        for (Reservation res : result.getReservations())
        {
            for (Instance instance : res.getInstances())
            {
                DescribeInstanceAttributeRequest request2 = new DescribeInstanceAttributeRequest().withAttribute("userData")
                                                                                                  .withInstanceId(instance.getInstanceId());
                DescribeInstanceAttributeResult result2 = aws.getEc2()
                                                             .describeInstanceAttribute(request2);
                System.out.println();
                System.out.printf("UserData for %s%n", instance.getInstanceId());
                String base64 = result2.getInstanceAttribute()
                                       .getUserData();
                for (String line : new String(Base64.decode(base64)).split("\n"))
                    System.out.printf(" >> %s%n", line);
                System.out.println();
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void listSecurityGroups()
    {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();

        DescribeSecurityGroupsResult result = aws.getEc2()
                                                 .describeSecurityGroups(request);

        for (SecurityGroup sg : result.getSecurityGroups())
        {
            System.out.printf("%s: %s%n", sg.getGroupName(), sg.getVpcId());

            for (IpPermission perm : sg.getIpPermissions())
            {
                System.out.printf("INBOUND: %s%n", perm);
            }

            for (IpPermission perm : sg.getIpPermissionsEgress())
            {
                System.out.printf("OUTBOUND: %s%n", perm);
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void testElasticIps()
    {
        for (Address addr : aws.listElasticAddresses())
        {
            System.out.println(addr);
        }

        Address addrNew = aws.findUnusedOrAllocateNewElasticAddress();
        System.out.println("NEW " + addrNew);
        aws.releaseElasticAddress(addrNew);
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void uploadFile() throws
                             Exception
    {
        AwsHelper.S3Entry before = aws.listFilesOnS3("test/");
        assertEquals(0,
                     before.getDirectories()
                           .size());
        assertEquals(0,
                     before.getFiles()
                           .size());

        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            try (FileOutputStream stream = new FileOutputStream(holder.get()))
            {
                for (int i = 0; i < 256; i++)
                {
                    stream.write(i);
                }
            }

            aws.saveFileToS3("test/file", holder.get());
        }

        AwsHelper.S3Entry after = aws.listFilesOnS3("test/");
        assertEquals(0,
                     after.getDirectories()
                          .size());
        assertEquals(1,
                     after.getFiles()
                          .size());
        AwsHelper.S3Entry afterFile = after.getFiles()
                                           .get(0);
        assertEquals("test/file", afterFile.getKey());
        assertEquals(256, afterFile.getSize());

        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            aws.loadFileFromS3("test/file", holder.get());

            try (FileInputStream stream = new FileInputStream(holder.get()))
            {
                for (int i = 0; i < 256; i++)
                {
                    assertEquals(i, stream.read());
                }
            }
        }

        aws.deleteFileFromS3("test/file");

        AwsHelper.S3Entry post = aws.listFilesOnS3("test/");
        assertEquals(0,
                     post.getDirectories()
                         .size());
        assertEquals(0,
                     post.getFiles()
                         .size());
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void listAllFiles()
    {
        AwsHelper.S3Entry res = aws.listFilesOnS3("");

        dumpDir(res, "");
    }

    private void dumpDir(AwsHelper.S3Entry res,
                         String indent)
    {
        for (AwsHelper.S3Entry file : res.getFiles())
        {
            System.out.printf("%sFile: %s: %,d : %s%n", indent, file.getKey(), file.getSize(), file.getLastModified());
        }

        indent += "  ";
        for (AwsHelper.S3Entry dir : res.getDirectories())
        {
            System.out.printf("%sDir: %s%n", indent, dir.getKey());
            dumpDir(dir, indent);
        }
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.alert.Alert;
import com.optio3.cloud.hub.model.alert.AlertHistory;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.identity.Role;
import com.optio3.cloud.hub.model.identity.User;
import com.optio3.cloud.hub.model.location.Location;
import com.optio3.cloud.hub.model.message.UserMessage;
import com.optio3.cloud.hub.model.normalization.Normalization;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;

public class DataLoader
{
    public static class LocationAndParent
    {
        public Location       details;
        public RecordIdentity parent;
    }

    public List<Role>              roles        = Lists.newArrayList();
    public List<User>              users        = Lists.newArrayList();
    public List<UserMessage>       userMessages = Lists.newArrayList();
    public List<LocationAndParent> locations    = Lists.newArrayList();

    public List<Asset>        assets        = Lists.newArrayList();
    public List<Alert>        alerts        = Lists.newArrayList();
    public List<AlertHistory> alertsHistory = Lists.newArrayList();

    public List<Normalization> normalizationRules = Lists.newArrayList();

    public DevicesSamplingTemplate samplingTemplate;

    public InstanceConfiguration instanceConfiguration;

    //--//

    public Map<String, String> passwords = Maps.newHashMap();

    public static DataLoader fetch(ObjectMapper mapper,
                                   String path) throws
                                                IOException
    {
        if (path.startsWith("file://"))
        {
            try (InputStream stream = new URL(path).openStream())
            {
                return mapper.readValue(stream, DataLoader.class);
            }
        }
        else
        {
            try (InputStream stream = ClassLoader.getSystemClassLoader()
                                                 .getResourceAsStream(path))
            {
                return mapper.readValue(stream, DataLoader.class);
            }
        }
    }

    public void apply(HubApplication app,
                      SessionHolder sessionHolder,
                      boolean loadIfMissing) throws
                                             Exception
    {
        handleRoles(sessionHolder, loadIfMissing);
        handleUsers(sessionHolder, loadIfMissing);
        handleLocations(sessionHolder, loadIfMissing);

        handleAssets(sessionHolder, loadIfMissing);
        handleAlerts(sessionHolder, loadIfMissing);
        handleAlertsHistory(sessionHolder, loadIfMissing);

        handleUserMessages(sessionHolder, loadIfMissing);

        handleNormalizationRules(sessionHolder, loadIfMissing);

        handleSamplingTemplate(app, sessionHolder, loadIfMissing);

        if (instanceConfiguration != null)
        {
            HubConfiguration.setInstanceConfiguration(sessionHolder, instanceConfiguration);
        }
    }

    private void handleRoles(SessionHolder sessionHolder,
                             boolean loadIfMissing)
    {
        for (Role input : roles)
        {
            if (input.sysId == null)
            {
                throw new RuntimeException("Null ID for Role");
            }

            RoleRecord rec = new RoleRecord();
            rec.setSysId(input.sysId);
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, input, rec);

            sessionHolder.persistEntity(rec);
        }

        sessionHolder.flush();
    }

    private void handleUsers(SessionHolder sessionHolder,
                             boolean loadIfMissing)
    {
        for (User input : users)
        {
            if (input.sysId == null)
            {
                throw new RuntimeException("Null ID for user");
            }

            UserRecord rec = new UserRecord();
            rec.setSysId(input.sysId);
            rec.setEmailAddress(input.emailAddress);
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, input, rec);

            if (passwords.containsKey(input.emailAddress))
            {
                rec.setPassword(sessionHolder, null, passwords.get(input.emailAddress));
            }

            for (RecordIdentity roleId : input.roles)
            {
                RoleRecord rec_role = RoleRecord.findByName(sessionHolder, roleId.sysId);
                if (rec_role == null)
                {
                    rec_role = sessionHolder.getEntity(RoleRecord.class, roleId.sysId);
                }

                rec.getRoles()
                   .add(rec_role);
            }

            sessionHolder.persistEntity(rec);
        }

        sessionHolder.flush();
    }

    private void handleUserMessages(SessionHolder sessionHolder,
                                    boolean loadIfMissing)
    {
        RecordHelper<UserMessageRecord> helper = sessionHolder.createHelper(UserMessageRecord.class);

        for (UserMessage input : userMessages)
        {
            if (loadIfMissing && helper.getOrNull(input.sysId) != null)
            {
                continue;
            }

            UserMessageRecord rec = input.newRecord();
            rec.setSysId(input.sysId);
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, input, rec);

            rec.populateFromDemo(sessionHolder, input);

            sessionHolder.persistEntity(rec);
        }
    }

    private void handleLocations(SessionHolder sessionHolder,
                                 boolean loadIfMissing) throws
                                                        Exception
    {
        final RecordHelper<LocationRecord> helper_location = sessionHolder.createHelper(LocationRecord.class);
        Map<String, LocationRecord>        map             = Maps.newHashMap();

        for (LocationAndParent loc : locations)
        {
            if (loc.details.sysId == null)
            {
                throw new RuntimeException("Null ID for Location");
            }

            if (map.containsKey(loc.details.sysId))
            {
                continue;
            }

            LocationRecord rec = new LocationRecord();
            rec.setSysId(loc.details.sysId);

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, loc.details, rec);
            rec.setPhysicalName(loc.details.name);

            helper_location.persist(rec);

            map.put(loc.details.sysId, rec);
        }

        for (LocationAndParent loc : locations)
        {
            if (loc.parent != null)
            {
                LocationRecord rec_parent = map.get(loc.parent.sysId);
                if (rec_parent == null)
                {
                    throw Exceptions.newRuntimeException("Invalid rec_parent id for location '%s'", loc.details.sysId);
                }

                LocationRecord rec_child = map.get(loc.details.sysId);
                rec_child.linkToParent(helper_location, rec_parent);
            }
        }

        sessionHolder.flush();
    }

    private void handleAssets(SessionHolder sessionHolder,
                              boolean loadIfMissing)
    {
        Random rnd = new Random(34567);

        RecordHelper<AssetRecord>    helper         = sessionHolder.createHelper(AssetRecord.class);
        RecordHelper<LocationRecord> helperLocation = sessionHolder.createHelper(LocationRecord.class);

        Map<LocationRecord, List<LocationRecord>> lookupLocations = Maps.newHashMap();

        for (Asset input : assets)
        {
            if (input.sysId == null)
            {
                throw new RuntimeException("Null ID for Device");
            }

            if (input.state == null)
            {
                input.state = AssetState.operational;
            }

            AssetRecord rec_asset = input.newRecord();
            rec_asset.setSysId(input.sysId);

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, input, rec_asset);
            rec_asset.setPhysicalName(input.name);

            DeviceRecord rec_device = Reflection.as(rec_asset, DeviceRecord.class);
            if (rec_device != null)
            {
                String productName = rec_device.getProductName();

                if (productName.length() > 200)
                {
                    productName = productName.substring(0, 200);
                }

                rec_asset.setPhysicalName(productName);

                //
                // TODO BUGBUG: Randomly moving devices through a building, for demo purposes.
                //
                LocationRecord rec_loc = rec_asset.getLocation();
                if (rec_loc != null)
                {
                    while ((rnd.nextInt() & 1) != 0)
                    {
                        List<LocationRecord> children = lookupLocations.get(rec_loc);
                        if (children == null)
                        {
                            children = rec_loc.getChildren(helperLocation);
                            lookupLocations.put(rec_loc, children);
                        }

                        if (children.isEmpty())
                        {
                            break;
                        }

                        rec_loc = children.get(rnd.nextInt(children.size()));
                    }

                    rec_asset.setLocation(rec_loc);
                }
            }

            helper.persist(rec_asset);

            rec_asset.assetPostCreate(sessionHolder);
        }

        sessionHolder.flush();
    }

    private void handleAlerts(SessionHolder sessionHolder,
                              boolean loadIfMissing)
    {
        RecordHelper<AlertRecord> helper_alert = sessionHolder.createHelper(AlertRecord.class);

        Duration diff = adjustAlertTime();

        for (Alert input : alerts)
        {
            if (input.sysId == null)
            {
                throw new RuntimeException("Null ID for Alert");
            }

            if (input.status == null)
            {
                input.status = AlertStatus.active;
            }

            AssetRecord rec_asset = sessionHolder.fromIdentity(input.asset);
            AlertRecord rec       = AlertRecord.newInstance(helper_alert, input.sequenceNumber, null, rec_asset, input.type);
            rec.setSysId(input.sysId);
            rec.setCreatedOn(input.createdOn.plus(diff));
            rec.setUpdatedOn(input.createdOn.plus(diff));

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, input, rec);

            sessionHolder.persistEntity(rec);
        }

        sessionHolder.flush();
    }

    private void handleAlertsHistory(SessionHolder sessionHolder,
                                     boolean loadIfMissing)
    {
        Duration diff = adjustAlertTime();

        for (AlertHistory input : alertsHistory)
        {
            if (input.sysId == null)
            {
                throw new RuntimeException("Null ID for AlertHistory");
            }

            AlertRecord rec_alert = sessionHolder.getEntity(AlertRecord.class, input.alert.sysId);

            AlertHistoryRecord rec = AlertHistoryRecord.newInstance(rec_alert, input.level, input.type);
            rec.setSysId(input.sysId);
            rec.setCreatedOn(input.createdOn.plus(diff));
            rec.setUpdatedOn(input.createdOn.plus(diff));

            rec.setText(input.text);

            sessionHolder.persistEntity(rec);
        }

        sessionHolder.flush();
    }

    private Duration adjustAlertTime()
    {
        ZonedDateTime last = null;
        for (Alert input : alerts)
        {
            last = TimeUtils.updateIfAfter(last, input.createdOn);
        }
        for (AlertHistory input : alertsHistory)
        {
            last = TimeUtils.updateIfAfter(last, input.createdOn);
        }

        if (last == null)
        {
            return Duration.ZERO;
        }

        ZonedDateTime now  = TimeUtils.now();
        Duration      diff = Duration.between(last, now);
        return diff.minus(3, ChronoUnit.HOURS);
    }

    private void handleNormalizationRules(SessionHolder sessionHolder,
                                          boolean loadIfMissing)
    {
        RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);

        for (Normalization norm : normalizationRules)
        {
            try
            {
                NormalizationRecord rec = NormalizationRecord.findVersion(helper, norm.version);

                if (rec == null)
                {
                    rec = NormalizationRecord.newInstance(helper, norm.rules, norm.version);
                }
                else
                {
                    if (!loadIfMissing)
                    {
                        continue;
                    }

                    // Update rules
                    rec.setRules(norm.rules);
                }

                if (norm.active)
                {
                    if (NormalizationRecord.findActive(helper) == null)
                    {
                        rec.makeActive(helper);
                    }
                }
            }
            catch (Exception e)
            {
                // Ignore failures
            }
        }
    }

    private void handleSamplingTemplate(HubApplication app,
                                        SessionHolder sessionHolder,
                                        boolean loadIfMissing)
    {
        try
        {
            if (samplingTemplate != null)
            {
                if (loadIfMissing)
                {
                    DevicesSamplingTemplate settings = SystemPreferenceRecord.getTypedValue(sessionHolder, SystemPreferenceTypedValue.SamplingTemplate, DevicesSamplingTemplate.class);
                    if (settings != null)
                    {
                        return;
                    }
                }

                SystemPreferenceRecord.setTypedValue(sessionHolder, SystemPreferenceTypedValue.SamplingTemplate, samplingTemplate);
            }
        }
        catch (Exception e)
        {
            // Ignore failures
        }
    }

    //--//

    private static void setCommonFields(RecordWithCommonFields rec,
                                        BaseModel input)
    {
        rec.setSysId(input.sysId);
        rec.setCreatedOn(input.createdOn);
        rec.setUpdatedOn(input.updatedOn);
    }

    private static <M extends BaseModel, R extends RecordWithCommonFields & ModelMapperTarget<M, ?>> void handleSub(SessionHolder sessionHolder,
                                                                                                                    Class<M> clz,
                                                                                                                    List<M> sourceCollection,
                                                                                                                    TypedRecordIdentityList<?> references,
                                                                                                                    Supplier<R> factory)
    {
        outer:
        for (RecordIdentity ri : references)
        {
            for (M source : sourceCollection)
            {
                if (source.sysId.equals(ri.sysId))
                {
                    R obj = factory.get();
                    setCommonFields(obj, source);

                    ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, source, obj);

                    sessionHolder.persistEntity(obj);
                    continue outer;
                }
            }

            throw Exceptions.newRuntimeException("Null ID for %s", clz.getName());
        }
    }

    //--//

    public void dump(HubApplication app)
    {
        try
        {
            ObjectWriter writer = app.getServiceNonNull(ObjectMapper.class)
                                     .writer(new DefaultPrettyPrinter());

            System.out.println("DUMP START ########################");
            writer.writeValue(new PrintStream(System.out)
            {
                @Override
                public void close()
                {
                }
            }, this);
            System.out.println("DUMP END ########################");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.LocationClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.workflow.WorkflowOverrides;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.collection.MapWithWeakValues;
import com.optio3.logging.Logger;
import com.optio3.util.CollectionUtils;
import com.optio3.util.IdGenerator;

public abstract class BaseTaskForNormalization extends AbstractHubActivityHandler implements BackgroundActivityHandler.ICleanupOnComplete,
                                                                                             BackgroundActivityHandler.ICleanupOnFailure
{
    public static final Logger LoggerInstance = new Logger(BaseTaskForNormalization.class);

    private static final MapWithWeakValues<String, NormalizationEngine> s_cacheEngine = new MapWithWeakValues<>();

    //--//

    public String        cacheId = IdGenerator.newGuid();
    public ZonedDateTime startTime;

    public NormalizationRules rules;

    private WorkflowOverrides m_workflowOverrides;

    //--//

    protected final Supplier<Map<String, String>> m_lookupPointAzureModel = Suppliers.memoize(() ->
                                                                                              {
                                                                                                  Map<String, String> map = Maps.newHashMap();
                                                                                                  for (PointClass pointClass : rules.pointClasses)
                                                                                                  {
                                                                                                      if (pointClass.azureDigitalTwin != null)
                                                                                                      {
                                                                                                          map.put(Integer.toString(pointClass.id), pointClass.azureDigitalTwin);
                                                                                                      }
                                                                                                  }

                                                                                                  return map;
                                                                                              });

    protected final Supplier<Map<String, String>> m_lookupEquipmentAzureModel = Suppliers.memoize(() ->
                                                                                                  {
                                                                                                      Map<String, String> map = Maps.newHashMap();
                                                                                                      for (EquipmentClass equipmentClass : rules.equipmentClasses)
                                                                                                      {
                                                                                                          if (equipmentClass.azureDigitalTwin != null)
                                                                                                          {
                                                                                                              map.put(Integer.toString(equipmentClass.id), equipmentClass.azureDigitalTwin);
                                                                                                          }
                                                                                                      }

                                                                                                      return map;
                                                                                                  });

    protected final Supplier<Map<String, List<String>>> m_lookupEquipmentTags = Suppliers.memoize(() ->
                                                                                                  {
                                                                                                      Map<String, List<String>> map = Maps.newHashMap();
                                                                                                      for (EquipmentClass equipmentClass : rules.equipmentClasses)
                                                                                                      {
                                                                                                          if (CollectionUtils.isNotEmpty(equipmentClass.tags))
                                                                                                          {
                                                                                                              map.put(Integer.toString(equipmentClass.id), equipmentClass.tags);
                                                                                                          }
                                                                                                      }

                                                                                                      return map;
                                                                                                  });

    protected final Supplier<Map<LocationType, String>> m_lookupLocationAzureModel = Suppliers.memoize(() ->
                                                                                                       {
                                                                                                           Map<LocationType, String> map = Maps.newHashMap();
                                                                                                           for (LocationClass locationClass : rules.locationClasses)
                                                                                                           {
                                                                                                               if (locationClass.azureDigitalTwin != null)
                                                                                                               {
                                                                                                                   map.put(locationClass.id, locationClass.azureDigitalTwin);
                                                                                                               }
                                                                                                           }

                                                                                                           return map;
                                                                                                       });

    protected final Supplier<Map<LocationType, List<String>>> m_lookupLocationTags = Suppliers.memoize(() ->
                                                                                                       {
                                                                                                           Map<LocationType, List<String>> map = Maps.newHashMap();
                                                                                                           for (LocationClass locationClass : rules.locationClasses)
                                                                                                           {
                                                                                                               if (CollectionUtils.isNotEmpty(locationClass.tags))
                                                                                                               {
                                                                                                                   map.put(locationClass.id, locationClass.tags);
                                                                                                               }
                                                                                                           }

                                                                                                           return map;
                                                                                                       });

    //--//

    @Override
    public void cleanupOnFailure(Throwable t) throws
                                              Exception
    {
        flushCachedItems();
    }

    @Override
    public void cleanupOnComplete() throws
                                    Exception
    {
        flushCachedItems();
    }

    //--//

    //
    // Creating the Normalization Engine is expensive, so we use a weak cache to try and reuse it.
    //
    protected NormalizationEngine ensureEngine(Callable<NormalizationEngine> callback) throws
                                                                                       Exception
    {
        synchronized (s_cacheEngine)
        {
            NormalizationEngine val = s_cacheEngine.get(cacheId);
            if (val == null)
            {
                val = callback.call();
                s_cacheEngine.put(cacheId, val);
            }

            return val;
        }
    }

    protected WorkflowOverrides ensureWorkflowOverrides(SessionHolder sessionHolder)
    {
        if (m_workflowOverrides == null)
        {
            m_workflowOverrides = ensureChunkNoThrow("ChunkForWorkflowOverrides", WorkflowOverrides.class, () -> WorkflowOverrides.load(sessionHolder));
        }

        return m_workflowOverrides;
    }

    private void flushCachedItems()
    {
        synchronized (s_cacheEngine)
        {
            s_cacheEngine.remove(cacheId);
        }
    }
}

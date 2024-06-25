/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineBlock;
import com.optio3.cloud.hub.engine.EngineTab;
import com.optio3.cloud.hub.engine.core.block.EngineExpressionFunctionCall;
import com.optio3.cloud.hub.engine.core.block.EngineProcedureDeclaration;
import com.optio3.cloud.hub.engine.core.block.EngineStatementProcedureCall;
import com.optio3.cloud.hub.engine.core.block.EngineThread;
import com.optio3.cloud.hub.engine.normalizations.block.NormalizationEngineExpressionGetControlPointName;
import com.optio3.cloud.hub.engine.normalizations.block.NormalizationEngineOperatorUnaryNormalize;
import com.optio3.cloud.hub.engine.normalizations.block.NormalizationEngineStatementSetOutputValue;
import com.optio3.cloud.hub.engine.normalizations.block.NormalizationEngineStatementSetPointClassFromTermScoring;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationOverrides;
import com.optio3.cloud.hub.model.normalization.NormalizationDefinitionDetails;
import com.optio3.cloud.hub.model.normalization.NormalizationDefinitionDetailsForUserProgram;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipment;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.ModelSanitizerHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.serialization.Reflection;
import com.optio3.util.IdGenerator;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public class NormalizationRules
{
    public static class KnownTerm
    {
        public String acronym;
        public Double positiveWeight;
        public Double negativeWeight;
        public String weightReason;

        public Set<String> synonyms = Sets.newHashSet();
    }

    static class FixupPrePostLogic
    {
        private EngineThread m_startBlock;

        private final Map<String, String>            m_functionIds = Maps.newHashMap();
        private final NormalizationDefinitionDetails m_logic       = new NormalizationDefinitionDetailsForUserProgram();

        public NormalizationDefinitionDetails generateLogic(NormalizationDefinitionDetails preLogic,
                                                            NormalizationDefinitionDetails postLogic)
        {
            ensureStartTab();
            fixupPre(preLogic);
            fixupPost(postLogic);

            return m_logic;
        }

        private void fixupPre(NormalizationDefinitionDetails preLogic)
        {
            if (preLogic != null)
            {
                fixup(preLogic, "PreProcessing");
            }

            NormalizationEngineOperatorUnaryNormalize normalize = new NormalizationEngineOperatorUnaryNormalize();
            normalize.id   = IdGenerator.newGuid();
            normalize.a    = new NormalizationEngineExpressionGetControlPointName();
            normalize.a.id = IdGenerator.newGuid();

            NormalizationEngineStatementSetOutputValue setOutput = new NormalizationEngineStatementSetOutputValue();
            setOutput.id    = IdGenerator.newGuid();
            setOutput.value = normalize;
            m_startBlock.statements.add(setOutput);
        }

        private void fixupPost(NormalizationDefinitionDetails postLogic)
        {
            if (postLogic != null)
            {
                ModelSanitizerContext ctx = new ModelSanitizerContext.Simple(null)
                {
                    @Override
                    protected ModelSanitizerHandler.Target processInner(Object obj,
                                                                        ModelSanitizerHandler handler)
                    {
                        EngineBlock block = Reflection.as(obj, EngineBlock.class);
                        if (block != null)
                        {
                            EngineProcedureDeclaration func = Reflection.as(block, EngineProcedureDeclaration.class);
                            if (func != null)
                            {
                                func.functionId = getNewFunctionId(func.functionId);
                                func.name       = "Post-" + func.name;
                            }

                            EngineStatementProcedureCall call = Reflection.as(block, EngineStatementProcedureCall.class);
                            if (call != null)
                            {
                                call.functionId = getNewFunctionId(call.functionId);
                            }

                            EngineExpressionFunctionCall call2 = Reflection.as(block, EngineExpressionFunctionCall.class);
                            if (call2 != null)
                            {
                                call2.functionId = getNewFunctionId(call2.functionId);
                            }
                        }

                        return super.processInner(obj, handler);
                    }
                };

                ctx.process(postLogic.tabs);

                fixup(postLogic, "PostProcessing");
            }

            NormalizationEngineStatementSetPointClassFromTermScoring classify = new NormalizationEngineStatementSetPointClassFromTermScoring();
            classify.id    = IdGenerator.newGuid();
            classify.value = new NormalizationEngineExpressionGetControlPointName();

            m_startBlock.statements.add(classify);
        }

        private void fixup(NormalizationDefinitionDetails oldLogic,
                           String prefix)
        {
            if (oldLogic.tabs.isEmpty())
            {
                // Nothing to fix.
                return;
            }

            EngineTab startTab = oldLogic.tabs.get(0);

            List<EngineBlock> startBlockChain = null;
            EngineThread      startBlock      = null;

            for (List<EngineBlock> blockChain : startTab.blockChains)
            {
                EngineThread topBlock = EngineTab.findTopBlock(blockChain, EngineThread.class);
                if (topBlock != null)
                {
                    startBlockChain = blockChain;
                    startBlock      = topBlock;
                    break;
                }
            }

            if (startBlock != null)
            {
                EngineProcedureDeclaration wrapper = new EngineProcedureDeclaration();
                wrapper.id         = startBlock.id;
                wrapper.functionId = IdGenerator.newGuid();
                wrapper.name       = prefix + " Logic";
                wrapper.statements = Lists.newArrayList();
                wrapper.statements.addAll(startBlock.statements);

                EngineTab preTab = new EngineTab();
                preTab.name = prefix;
                preTab.blockChains.add(List.of(wrapper));
                m_logic.tabs.add(preTab);

                boolean missing = false;

                for (List<EngineBlock> blockChainOther : startTab.blockChains)
                {
                    if (blockChainOther != startBlockChain)
                    {
                        preTab.blockChains.add(blockChainOther);
                        missing = true;
                    }
                }

                if (missing)
                {
                    HubApplication.LoggerInstance.warn("Detected dropped normalization functions!");
                }

                EngineStatementProcedureCall call = new EngineStatementProcedureCall();
                call.id         = IdGenerator.newGuid();
                call.functionId = wrapper.functionId;

                m_startBlock.statements.add(call);
            }

            m_logic.tabs.addAll(oldLogic.tabs.subList(1, oldLogic.tabs.size()));
        }

        private String getNewFunctionId(String functionId)
        {
            return m_functionIds.computeIfAbsent(functionId, (id) -> IdGenerator.newGuid());
        }

        private void ensureStartTab()
        {
            EngineTab startTab = new EngineTab();
            startTab.name = "Start";
            m_logic.tabs.add(startTab);

            m_startBlock            = new EngineThread();
            m_startBlock.id         = IdGenerator.newGuid();
            m_startBlock.statements = Lists.newArrayList();

            startTab.blockChains.add(List.of(m_startBlock));
        }
    }

    //--//

    private NormalizationDefinitionDetails m_preLogic;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setPreProcessingDetails(NormalizationDefinitionDetails preProcessingDetails)
    {
        m_preLogic = preProcessingDetails;
    }

    private NormalizationDefinitionDetails m_postLogic;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setPostProcessingDetails(NormalizationDefinitionDetails postProcessingDetails)
    {
        m_postLogic = postProcessingDetails;
    }

    private NormalizationDefinitionDetails m_logic;

    public NormalizationDefinitionDetails getLogic()
    {
        if (m_logic == null)
        {
            var fixup = new FixupPrePostLogic();
            return fixup.generateLogic(m_preLogic, m_postLogic);
        }

        return m_logic;
    }

    public void setLogic(NormalizationDefinitionDetails logic)
    {
        m_logic = logic;
    }

    //--//

    public final List<PointClass>     pointClasses     = Lists.newArrayList();
    public final List<EquipmentClass> equipmentClasses = Lists.newArrayList();
    public final List<LocationClass>  locationClasses  = Lists.newArrayList();

    //--//

    public final TreeMap<String, KnownTerm> knownTerms = new TreeMap<>();

    public final TreeMap<String, List<String>> abbreviations   = new TreeMap<>(); // Map from target text to list of source texts.
    public final TreeMap<String, String>       startsWith      = new TreeMap<>(); // Map from source text to target text.
    public final TreeMap<String, String>       endsWith        = new TreeMap<>(); // Map from source text to target text.
    public final TreeMap<String, String>       contains        = new TreeMap<>(); // Map from source text to target text.
    public final TreeMap<String, String>       disambiguations = new TreeMap<>(); // Map from source text to target text.

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setPointClassificationModel(Object model)
    {
    }

    public double scoreThreshold = 2;

    //--//

    public ValidationRules validation;

    //--//

    public TreeMap<String, Set<String>>            equipmentRelationships = new TreeMap<>();
    public TreeMap<String, NormalizationEquipment> equipments             = new TreeMap<>();

    public TreeMap<String, DeviceElementClassificationOverrides> pointOverrides = new TreeMap<>();

    public void cleanUp(SessionHolder holder)
    {
        DeviceElementClassificationOverrides.cleanUp(holder, pointOverrides);
    }

    //--//

    public void populateWithWellKnownClasses() throws
                                               Exception
    {
        for (WellKnownPointClass pointClass : WellKnownPointClass.values())
        {
            if (WellKnownPointClass.isValid(pointClass))
            {
                addPointClassIfAbsent(pointClass.getId(), (pc) ->
                {
                    pc.pointClassName        = pointClass.getDisplayName();
                    pc.pointClassDescription = pointClass.getDescription();
                    pc.wellKnown             = pointClass;
                    pc.tags                  = pointClass.getTags();
                    pc.type                  = PointClassType.Value;

                    extractTerms(pc.pointClassDescription);
                });
            }
        }

        for (WellKnownEquipmentClass equipClass : WellKnownEquipmentClass.values())
        {
            if (WellKnownEquipmentClass.isValid(equipClass))
            {
                addEquipmentClassIfAbsent(equipClass.getId(), (ec) ->
                {
                    ec.equipClassName = equipClass.getDisplayName();
                    ec.description    = equipClass.getDescription();
                    ec.wellKnown      = equipClass;

                    extractTerms(ec.description);
                });
            }
        }

        populateWellKnownLocationClasses();
    }

    public void populateWellKnownLocationClasses() throws
                                                   Exception
    {
        for (LocationType value : LocationType.values())
        {
            if (value.getAzureDigitalTwin() != null)
            {
                addLocationClassIfAbsent(value, (lc) ->
                {
                    lc.description      = value.getDescription();
                    lc.azureDigitalTwin = value.getAzureDigitalTwin();

                    extractTerms(lc.description);
                });
            }
        }
    }

    //--//

    public EquipmentClass findEquipmentClass(int id)
    {
        for (EquipmentClass ec : equipmentClasses)
        {
            if (ec.id == id)
            {
                return ec;
            }
        }

        return null;
    }

    public EquipmentClass findEquipmentClass(String id)
    {
        for (EquipmentClass ec : equipmentClasses)
        {
            if (StringUtils.equals(ec.idAsString(), id))
            {
                return ec;
            }
        }

        return null;
    }

    public EquipmentClass addEquipmentClassIfAbsent(int id,
                                                    ConsumerWithException<EquipmentClass> builder) throws
                                                                                                   Exception
    {
        EquipmentClass ec = findEquipmentClass(id);
        if (ec == null)
        {
            ec    = new EquipmentClass();
            ec.id = id;

            builder.accept(ec);

            equipmentClasses.add(ec);
        }

        return ec;
    }

    public PointClass findPointClass(int id)
    {
        for (PointClass pc : pointClasses)
        {
            if (pc.id == id)
            {
                return pc;
            }
        }

        return null;
    }

    public PointClass addPointClassIfAbsent(int id,
                                            ConsumerWithException<PointClass> builder) throws
                                                                                       Exception
    {
        PointClass pc = findPointClass(id);
        if (pc == null)
        {
            pc    = new PointClass();
            pc.id = id;

            builder.accept(pc);

            pointClasses.add(pc);
        }

        return pc;
    }

    public LocationClass findLocationClass(LocationType id)
    {
        for (LocationClass lc : locationClasses)
        {
            if (lc.id == id)
            {
                return lc;
            }
        }

        return null;
    }

    public LocationClass addLocationClassIfAbsent(LocationType id,
                                                  ConsumerWithException<LocationClass> builder) throws
                                                                                                Exception
    {
        LocationClass lc = findLocationClass(id);
        if (lc == null)
        {
            lc    = new LocationClass();
            lc.id = id;

            builder.accept(lc);

            locationClasses.add(lc);
        }

        return lc;
    }

    private void extractTerms(String text)
    {
        if (StringUtils.isNotBlank(text))
        {
            for (String part : NormalizationEngine.splitAndLowercase(text))
            {
                knownTerms.computeIfAbsent(part, (key) -> new KnownTerm());
            }
        }
    }
}

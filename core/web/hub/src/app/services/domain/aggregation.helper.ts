import {AppContext} from "app/app.service";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AssetExtended, AssetsService, DeviceElementExtended} from "app/services/domain/assets.service";
import {UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {HierarchyResult, HierarchyResultNode, HierarchyResultNodeType, ResultTable} from "app/shared/charting/hierarchical-visualization/hierarchy-result-table";
import {TagsJoinQueryExtended} from "app/shared/charting/hierarchical-visualization/tags-join-query.extended";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {AggregationTrendGroup, AggregationTrendGroupAggregation} from "framework/ui/charting/aggregation-trend-group";
import {ChartValueRange} from "framework/ui/charting/core/basics";
import {StepwiseColorMapper} from "framework/ui/charting/core/colors";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {inParallel, mapInParallelNoNulls} from "framework/utils/concurrency";
import moment from "framework/utils/moment";

export class AggregationHelper
{
    private static readonly removeDecimalCutoffValue = 100;

    public static async aggregateByBinding(app: AppContext,
                                           graph: Models.AssetGraph,
                                           binding: Models.AssetGraphBinding,
                                           filterableRange: Models.FilterableTimeRange,
                                           aggType: Models.AggregationTypeId,
                                           units: Models.EngineeringUnitsFactors,
                                           unitsDisplay?: string): Promise<AggregationResult[]>
    {
        const graphExt    = new AssetGraphExtended(app.domain, graph);
        const resolved    = await graphExt.resolve();
        const tuples      = resolved.resolveBindingTuples([binding], true)
                                    .map((tuple) => tuple.tuple);
        const assetTuples = await app.domain.assets.loadTable(tuples);

        unitsDisplay ||= await app.domain.units.getUnitsDisplay(units);
        const ranges      = [filterableRange];
        const cpSelection = Models.ControlPointsSelection.newInstance({identities: []});
        for (let tuple of assetTuples)
        {
            cpSelection.identities.push(DeviceElementExtended.newIdentity(tuple[tuple.length - 1].sysId));
        }

        const aggregationRequest = AggregationHelper.getAggregationRequest(ranges, aggType, units, cpSelection);
        const response           = await app.domain.assets.computeAggregation(aggregationRequest);
        const cpAggregations     = AggregationHelper.processControlPointAggregations(app.domain.assets, assetTuples, response.resultsPerRange, ranges, unitsDisplay);
        for (let aggregation of cpAggregations) aggregation.aggType = aggType;

        return cpAggregations;
    }

    public static async aggregateControlPointsGroupSingle(app: AppContext,
                                                          group: Models.ControlPointsGroup,
                                                          filterableRanges: Models.FilterableTimeRange[],
                                                          displayType: Models.ControlPointDisplayType = Models.ControlPointDisplayType.NameOnly): Promise<AggregationResult>
    {
        let roots = await AggregationHelper.aggregateControlPointsGroup(app, group, [filterableRanges], displayType);
        return roots[0];
    }

    public static async aggregateControlPointsGroup(app: AppContext,
                                                    group: Models.ControlPointsGroup,
                                                    filterableRangeSets: Models.FilterableTimeRange[][],
                                                    displayType: Models.ControlPointDisplayType): Promise<AggregationResult[]>
    {
        let flattenedFilterableRanges: Models.FilterableTimeRange[] = [];
        for (let filterableRangeSet of filterableRangeSets)
        {
            flattenedFilterableRanges.push(...filterableRangeSet);
        }

        AggregationHelper.fixupGroup(group);
        let request  = this.getAggregationRequest(flattenedFilterableRanges, group.aggregationType, group.unitsFactors, group.selections, group.graph, group.pointInput);
        let response = await app.domain.assets.computeAggregation(request);
        let models   = await app.domain.assets.loadTable(response.records);

        let rangeSetOffset = 0;
        let results        = [];
        for (let filterableRangeSet of filterableRangeSets)
        {
            let rangeSetOffsetEnd = rangeSetOffset + filterableRangeSet.length;
            let hierarchy: HierarchyResult;

            if (request.query)
            {
                // Build a result table
                let table = new ResultTable(models, request.query);
                hierarchy = table.toHierarchyResult();
            }
            else
            {
                hierarchy = null;
            }

            let unitsDisplay   = group.unitsDisplay || await app.domain.units.getUnitsDisplay(group.unitsFactors);
            let relevantAggs   = response.resultsPerRange.slice(rangeSetOffset, rangeSetOffsetEnd);
            let cpAggregations = AggregationHelper.processControlPointAggregations(app.domain.assets, models, relevantAggs, filterableRangeSet, unitsDisplay, group, 1);
            cpAggregations     = await AggregationHelper.processAggregationResults(group, cpAggregations, filterableRangeSet, displayType);

            let aggregationRoot = AggregationResult.empty();
            if (!hierarchy)
            {
                aggregationRoot.children = cpAggregations;
            }
            else
            {
                let idToAgg: Lookup<AggregationLookup> = {};

                let pos = 0;

                for (let agg of cpAggregations)
                {
                    if (agg.asset && agg.asset.model)
                    {
                        idToAgg[agg.asset.model.sysId] = {
                            agg: agg,
                            pos: pos++
                        };
                    }
                }

                hierarchy.roots.forEach((root) => AggregationHelper.getAggregationTree(aggregationRoot, root, idToAgg));
            }

            AggregationGroupAggregator.doGroupAggregations(aggregationRoot, group.groupAggregationType, group.granularity, group, 0, filterableRangeSet.length, unitsDisplay);
            aggregationRoot.unitsDisplay = unitsDisplay;
            aggregationRoot.cpLabel      = group.name;

            results.push(aggregationRoot.isValid() ? aggregationRoot : null);
            rangeSetOffset = rangeSetOffsetEnd;
        }

        return results;
    }

    private static getAggregationRequest(filterableRanges: Models.FilterableTimeRange[],
                                         aggregationType: Models.AggregationTypeId,
                                         units: Models.EngineeringUnitsFactors,
                                         selections?: Models.ControlPointsSelection,
                                         graph?: Models.AssetGraph,
                                         binding?: Models.AssetGraphBinding,
                                         retainNodeIds?: boolean): Models.AggregationRequest
    {
        let query: Models.TagsJoinQuery;
        if (graph && binding && !binding.graphId)
        {
            query = TagsJoinQueryExtended.fromHierarchicalGraph(graph, binding.nodeId, retainNodeIds);
        }

        return Models.AggregationRequest.newInstance({
                                                         aggregationType    : aggregationType,
                                                         unitsFactors       : units,
                                                         selections         : selections,
                                                         query              : query,
                                                         filterableRanges   : filterableRanges,
                                                         localTimeZoneOffset: -new Date().getTimezoneOffset()
                                                     });
    }

    public static async getAggregationTrendGroups(app: AppContext,
                                                  filterableRange: Models.FilterableTimeRange,
                                                  granularity: Models.AggregationGranularity,
                                                  groups: Models.ControlPointsGroup[]): Promise<AggregationTrendGroup[]>
    {
        let overallFilterableRange = Models.FilterableTimeRange.newInstance(filterableRange);
        if (!overallFilterableRange.range)
        {
            overallFilterableRange.range = RangeSelectionExtended.newModel();
        }

        let filterableRanges: Models.FilterableTimeRange[] = [];

        let overallRange = new RangeSelectionExtended(overallFilterableRange.range);
        let ranges       = overallRange.splitBasedOnGranularity(granularity);

        for (let range of ranges)
        {
            let start = MomentHelper.parse(range.start, range.zone);
            let name  = this.getIntervalName(start, granularity);

            let filterableRange = Models.FilterableTimeRange.newInstance({
                                                                             name  : name,
                                                                             range : range,
                                                                             filter: overallFilterableRange.filter
                                                                         });
            filterableRanges.push(filterableRange);
        }

        let aggregations = await mapInParallelNoNulls(groups, (group) => AggregationHelper.aggregateControlPointsGroupSingle(app, group, filterableRanges));

        let results: AggregationTrendGroup[] = [];
        for (let result of aggregations)
        {
            let aggInfo = [];
            for (let i = 0; i < filterableRanges.length; i++)
            {
                let cpAgg = result.getAggByTimeRange(i);
                aggInfo.push(new AggregationTrendGroupAggregation(cpAgg.value, cpAgg.formattedValue, filterableRanges[i].name));
            }
            results.push(new AggregationTrendGroup(result.cpLabel, AggregationHelper.aggregationToLabel(result), aggInfo, result.group.valuePrecision, result.group.colorConfig?.segments?.[0]?.color));
        }

        return results;
    }

    private static getIntervalName(time: moment.Moment,
                                   granularity: Models.AggregationGranularity): string
    {
        switch (granularity)
        {
            case Models.AggregationGranularity.Hour:
                return time.format("H");

            case Models.AggregationGranularity.Day:
                return time.format("MMM D");

            case Models.AggregationGranularity.Week:
                return "Week " + time.format("w");

            case Models.AggregationGranularity.Month:
                return time.format("MMMM");

            case Models.AggregationGranularity.Quarter:
                return "Quarter " + time.format("Q");

            case Models.AggregationGranularity.Year:
                return time.format("Y");
        }

        return "";
    }

    private static getAggregationTree(parentAggregation: AggregationResult,
                                      hierarchyNode: HierarchyResultNode,
                                      idToAgg: Lookup<AggregationLookup>)
    {
        if (hierarchyNode.type === HierarchyResultNodeType.group)
        {
            // not a control point: move on
            let currAggNode     = AggregationResult.empty();
            currAggNode.cpLabel = hierarchyNode.label;
            parentAggregation.children.push(currAggNode);

            hierarchyNode.children.forEach((hierarchyChild) => AggregationHelper.getAggregationTree(currAggNode, hierarchyChild, idToAgg));

            currAggNode.children = currAggNode.children.filter((node) => !!node);
        }
        else
        {
            // control point: create link in map
            let lookup = idToAgg[hierarchyNode.id];
            if (lookup && lookup.agg)
            {
                parentAggregation.children[lookup.pos] = lookup.agg;
            }
        }
    }

    public static async processAggregationResults(group: Models.ControlPointsGroup,
                                                  results: AggregationResult[],
                                                  ranges: Models.FilterableTimeRange[],
                                                  displayType: Models.ControlPointDisplayType): Promise<AggregationResult[]>
    {
        switch (displayType)
        {
            case Models.ControlPointDisplayType.LocationOnly:
            case Models.ControlPointDisplayType.NameLocation:
            case Models.ControlPointDisplayType.LocationName:
                await inParallel(results, async (result) => result.locationDisplay = await AggregationHelper.resolveLocationDetails(result.asset));
                break;

            case Models.ControlPointDisplayType.FullLocationOnly:
            case Models.ControlPointDisplayType.NameFullLocation:
            case Models.ControlPointDisplayType.FullLocationName:
                await inParallel(results, async (result) => result.locationDisplay = await AggregationHelper.resolveFullLocationDetails(result.asset));
                break;

            case Models.ControlPointDisplayType.EquipmentOnly:
            case Models.ControlPointDisplayType.NameEquipment:
            case Models.ControlPointDisplayType.EquipmentName:
                await inParallel(results, async (result) => result.equipmentDisplay = await AggregationHelper.resolveEquipmentDetails(result.asset));
                break;
        }

        if (group)
        {
            let numChildren = results.length;
            let limitValue  = group.limitValue;
            switch (group.limitMode)
            {
                case Models.AggregationLimit.TopNPercent:
                case Models.AggregationLimit.BottomNPercent:
                    limitValue = Math.ceil(limitValue * numChildren / 100);
                // Fallthrough

                case Models.AggregationLimit.TopN:
                case Models.AggregationLimit.BottomN:
                    if (numChildren > limitValue)
                    {
                        let aggregationGroup      = AggregationResult.empty();
                        aggregationGroup.children = results;

                        let descending             = group.limitMode === Models.AggregationLimit.TopN || group.limitMode === Models.AggregationLimit.TopNPercent;
                        let sortedAggregationGroup = aggregationGroup.sortByRange(ranges.length - 1, !descending);

                        results = sortedAggregationGroup.children.slice(0, limitValue);
                    }
                    break;
            }
        }

        return results;
    }

    public static processControlPointAggregations(assetsService: AssetsService,
                                                  tuples: Models.Asset[][],
                                                  resultsPerRange: number[][],
                                                  ranges: Models.FilterableTimeRange[],
                                                  unitsDisplay: string,
                                                  group?: Models.ControlPointsGroup,
                                                  depth?: number): AggregationResult[]
    {
        let children: AggregationResult[] = [];
        for (let controlPointIdx = 0; controlPointIdx < tuples.length; controlPointIdx++)
        {
            let tuple   = tuples[controlPointIdx];
            let sigElem = <DeviceElementExtended>AssetExtended.newInstance(assetsService, tuple[tuple.length - 1]);

            let currAggregationsByTimeRange: ControlPointAggregation[] = [];
            for (let timeRangeIdx = 0; timeRangeIdx < ranges.length; timeRangeIdx++)
            {
                const aggValue        = resultsPerRange[timeRangeIdx][controlPointIdx];
                const significantElem = isFinite(aggValue) && sigElem || undefined;
                currAggregationsByTimeRange.push(new ControlPointAggregation(aggValue, significantElem, ranges[timeRangeIdx].range, unitsDisplay));
            }

            let aggregation                     = new AggregationResult(sigElem, sigElem ? sigElem.model.name : "", tuple, depth);
            aggregation.aggregationsByTimeRange = currAggregationsByTimeRange;
            aggregation.unitsDisplay            = unitsDisplay;
            if (group)
            {
                aggregation.group       = group;
                aggregation.granularity = group.granularity;
                aggregation.aggType     = group.aggregationType;
            }

            children.push(aggregation);
        }

        return children;
    }

    public static async resolveLocationDetails(asset: AssetExtended): Promise<string>
    {
        let locationExt = await asset?.getLocation();
        return locationExt?.model.name;
    }

    public static async resolveFullLocationDetails(asset: AssetExtended): Promise<string>
    {
        let locationExt = await asset?.getLocation();
        return locationExt?.getRecursiveName();
    }

    public static async resolveEquipmentDetails(asset: AssetExtended): Promise<string>
    {
        let parentEquipments = await asset?.getExtendedParentsOfRelation(Models.AssetRelationship.controls);
        if (parentEquipments?.length === 1) return parentEquipments[0].model.name;

        return undefined;
    }

    public static getMomentUnitFromGranularity(granularity: Models.AggregationGranularity): moment.unitOfTime.Diff
    {
        switch (granularity)
        {
            case Models.AggregationGranularity.Hour:
                return "hours";

            case Models.AggregationGranularity.Day:
                return "days";

            case Models.AggregationGranularity.Week:
                return "weeks";

            case Models.AggregationGranularity.Month:
                return "months";

            case Models.AggregationGranularity.Quarter:
                return "quarters";

            case Models.AggregationGranularity.Year:
                return "years";

            default:
                return null;
        }
    }

    public static fixupGroup(group: Models.ControlPointsGroup): Models.ControlPointsGroup
    {
        let controlPointAggregationType = group.aggregationType;
        if (controlPointAggregationType && !group.groupAggregationType)
        {
            let groupAggregationType: Models.AggregationTypeId;
            switch (controlPointAggregationType)
            {
                case Models.AggregationTypeId.DELTA:
                    controlPointAggregationType = Models.AggregationTypeId.INCREASE;
                    groupAggregationType        = Models.AggregationTypeId.SUM;
                    break;

                case Models.AggregationTypeId.AVGDELTA:
                    controlPointAggregationType = Models.AggregationTypeId.INCREASE;
                    groupAggregationType        = Models.AggregationTypeId.MEAN;
                    break;

                default:
                    groupAggregationType = controlPointAggregationType;
            }

            group.groupAggregationType = groupAggregationType;
            group.aggregationType      = controlPointAggregationType;
        }

        return group;
    }

    public static numberWithUnitDisplay(num: number,
                                        units: string)
    {
        if (units == UnitsService.noUnitsDisplayName) units = undefined;

        if (typeof num != "number" || isNaN(num))
        {
            return "N/A";
        }

        let maximumFractionDigits = num >= AggregationHelper.removeDecimalCutoffValue ? 0 : 2;

        // Convert to fixed decimal string of value
        let fixed = num.toLocaleString(undefined, {maximumFractionDigits: maximumFractionDigits});

        // Return value with units
        return AggregationHelper.interpolateNumberWithUnits(fixed, units);
    }

    public static aggregationToLabel(aggResult: AggregationResult): string
    {
        if (!aggResult) return "";
        let groupAgg = aggResult.aggType;
        let cpResult = aggResult;
        while (cpResult.children.length > 0) cpResult = cpResult.children[0];

        let cpAgg       = cpResult.aggType;
        let granularity = cpResult.granularity;
        if (aggResult !== cpResult)
        {
            return AggregationHelper.aggregationCategorizationDisplay(groupAgg, cpAgg, granularity, cpResult.group.limitMode, cpResult.group.limitValue);
        }
        else
        {
            return AggregationHelper.aggTransformPluralWithGranularity(cpAgg, granularity);
        }
    }

    private static interpolateNumberWithUnits(number: string,
                                              units?: string): string
    {
        if (units && units.startsWith("$"))
        {
            let parts = units.split(" ");
            return `${parts[0]}${number.toLocaleString()} ${parts[1] || ""}`;
        }
        return `${number.toLocaleString()} ${units || ""}`;
    }

    public static aggregationCategorizationDisplay(groupAgg: Models.AggregationTypeId,
                                                   cpAgg: Models.AggregationTypeId,
                                                   granularity: Models.AggregationGranularity,
                                                   limitMode: Models.AggregationLimit,
                                                   limitValue: number): string
    {
        if (!granularity) granularity = Models.AggregationGranularity.None;

        let inner;
        let outer  = this.aggTransformSingular(groupAgg);
        let suffix = this.aggLimitSuffix(limitMode, limitValue);
        if (outer)
        {
            if (granularity != Models.AggregationGranularity.None)
            {
                inner = this.aggTransformPluralWithGranularity(cpAgg, granularity);
                return `Average ${outer.toLowerCase()} of ${inner} ${suffix}`;
            }
            else
            {
                inner = this.aggTransformPlural(cpAgg);
            }

        }
        return inner ? `${outer} of ${inner} ${suffix}` : "";
    }

    public static aggTransformPluralWithGranularity(aggType: Models.AggregationTypeId,
                                                    granularity: Models.AggregationGranularity)
    {
        if (!granularity) granularity = Models.AggregationGranularity.None;

        if (granularity != Models.AggregationGranularity.None)
        {
            return `${this.aggTransformPlural(aggType)} per ${granularity}`.toLowerCase();
        }
        else
        {
            return this.aggTransformPlural(aggType);
        }
    }

    public static aggLimitSuffix(limitMode: Models.AggregationLimit,
                                 limitValue: number): string
    {
        if (limitMode && limitValue && limitMode !== Models.AggregationLimit.None)
        {
            let isTop     = limitMode === Models.AggregationLimit.TopN || limitMode === Models.AggregationLimit.TopNPercent;
            let isPercent = limitMode === Models.AggregationLimit.TopNPercent || limitMode === Models.AggregationLimit.BottomNPercent;
            return `for ${isTop ? "top" : "bottom"} ${limitValue}${isPercent ? "%" : ""}`;
        }

        return "";
    }

    public static aggTransformSingular(aggType: Models.AggregationTypeId): string
    {
        switch (aggType)
        {
            case Models.AggregationTypeId.NONE:
                return "No aggregation";

            case Models.AggregationTypeId.MAX:
                return "Max";

            case Models.AggregationTypeId.MIN:
                return "Min";

            case Models.AggregationTypeId.MEAN:
                return "Mean";

            case Models.AggregationTypeId.DELTA:
                return "Delta";

            case Models.AggregationTypeId.SUM:
                return "Sum";

            case Models.AggregationTypeId.INCREASE:
                return "Increase";

            case Models.AggregationTypeId.DECREASE:
                return "Decrease";

            case Models.AggregationTypeId.FIRST:
                return "First";

            case Models.AggregationTypeId.LAST:
                return "Last";
        }

        return "";
    }

    public static aggTransformPlural(aggType: Models.AggregationTypeId): string
    {
        switch (aggType)
        {
            case Models.AggregationTypeId.NONE:
                return "no aggregations";

            case Models.AggregationTypeId.MAX:
                return "maxes";

            case Models.AggregationTypeId.MIN:
                return "mins";

            case Models.AggregationTypeId.MEAN:
                return "means";

            case Models.AggregationTypeId.DELTA:
                return "deltas";

            case Models.AggregationTypeId.SUM:
                return "sums";

            case Models.AggregationTypeId.INCREASE:
                return "increases";

            case Models.AggregationTypeId.DECREASE:
                return "decreases";

            case Models.AggregationTypeId.FIRST:
                return "firsts";

            case Models.AggregationTypeId.LAST:
                return "lasts";
        }

        return "";
    }
}

class AggregationLookup
{
    agg: AggregationResult;
    pos: number;
}

export class AggregationResult
{
    group: Models.ControlPointsGroup;

    aggregationsByTimeRange: ControlPointAggregation[] = [];

    children: AggregationResult[] = [];

    aggType: Models.AggregationTypeId;
    granularity: Models.AggregationGranularity;

    unitsDisplay?: string;

    locationDisplay?: string;
    equipmentDisplay?: string;

    primaryLabel: string;
    secondaryLabel: string;

    protected m_displayType: Models.ControlPointDisplayType;
    set displayType(type: Models.ControlPointDisplayType)
    {
        this.m_displayType = type;

        let labels          = AggregationResult.getLabels(type, this.cpLabel, this.locationDisplay, this.equipmentDisplay);
        this.primaryLabel   = labels.primaryLabel;
        this.secondaryLabel = labels.secondaryLabel;
    }

    constructor(public asset: DeviceElementExtended,
                public cpLabel: string,
                public tuple: Models.Asset[],
                public depth?: number)
    {
        this.displayType = Models.ControlPointDisplayType.NameOnly;
    }

    public isValid(): boolean
    {
        return this.aggregationsByTimeRange.length > 0 && this.children.every((childAgg) => childAgg.isValid());
    }

    public getAggByTimeRange(timeRangeIdx: number): ControlPointAggregation
    {
        return this.aggregationsByTimeRange[timeRangeIdx];
    }

    public getColorByTimeRange(timeRangeIdx: number): string
    {
        let segments = this.group.colorConfig?.segments;
        if (segments)
        {
            if (segments.length === 1)
            {
                return segments[0].color;
            }
            else
            {
                let colorConfigExt        = new ColorConfigurationExtended(this.group.colorConfig);
                let stepwiseColorComputer = new StepwiseColorMapper(colorConfigExt.computeStops());
                return stepwiseColorComputer.getColor(this.aggregationsByTimeRange[timeRangeIdx]?.value);
            }
        }

        return undefined;
    }

    public getAggValueByTimeRange(timeRangeIdx: number): number
    {
        let agg = this.getAggByTimeRange(timeRangeIdx);
        return agg && agg.value ? agg.value : 0;
    }

    public calculateAggregationRanges(valueRanges?: ChartValueRange[]): ChartValueRange[]
    {
        if (!valueRanges) valueRanges = [];

        for (let rangeIdx = 0; rangeIdx < this.aggregationsByTimeRange.length; rangeIdx++)
        {
            let valueRange = valueRanges[rangeIdx];
            if (!valueRange) valueRange = valueRanges[rangeIdx] = new ChartValueRange();

            let value = this.aggregationsByTimeRange[rangeIdx].value;
            if (!isNaN(value)) valueRange.expandForValue(value);
        }
        for (let child of this.children) child.calculateAggregationRanges(valueRanges);

        return valueRanges;
    }

    public sortByRange(timeRangeIdx: number,
                       ascending: boolean): AggregationResult
    {
        return this.sortInner((a: AggregationResult,
                               b: AggregationResult) => UtilsService.compareNumbers(a.getAggValueByTimeRange(timeRangeIdx), b.getAggValueByTimeRange(timeRangeIdx), ascending));
    }

    private sortInner(sortFn: (a: AggregationResult,
                               b: AggregationResult) => number): AggregationResult
    {
        let newAgg      = this.clone();
        newAgg.children = newAgg.children.map((child) => child.sortInner(sortFn))
                                .sort(sortFn);
        return newAgg;
    }

    public clone(): AggregationResult
    {
        let newAgg                     = new AggregationResult(this.asset, this.cpLabel, this.tuple, this.depth);
        newAgg.group                   = this.group;
        newAgg.granularity             = this.granularity;
        newAgg.aggType                 = this.aggType;
        newAgg.aggregationsByTimeRange = this.aggregationsByTimeRange;
        newAgg.children                = UtilsService.arrayCopy(this.children);
        newAgg.unitsDisplay            = this.unitsDisplay;
        newAgg.locationDisplay         = this.locationDisplay;
        newAgg.equipmentDisplay        = this.equipmentDisplay;

        newAgg.displayType = this.m_displayType;

        return newAgg;
    }

    public static empty(): AggregationResult
    {
        return new AggregationResult(null, null, null);
    }

    public static getLabels(type: Models.ControlPointDisplayType,
                            name: string,
                            location: string,
                            equipment: string): ControlPointDisplayLabels
    {
        let res: ControlPointDisplayLabels = {primaryLabel: name};
        switch (type)
        {
            case Models.ControlPointDisplayType.LocationOnly:
            case Models.ControlPointDisplayType.FullLocationOnly:
                res.primaryLabel = location || name;
                break;

            case Models.ControlPointDisplayType.EquipmentOnly:
                res.primaryLabel = equipment || name;
                break;

            case Models.ControlPointDisplayType.NameLocation:
            case Models.ControlPointDisplayType.NameFullLocation:
                res.primaryLabel = name;
                if (location) res.secondaryLabel = location;
                break;

            case Models.ControlPointDisplayType.LocationName:
            case Models.ControlPointDisplayType.FullLocationName:
                res.primaryLabel = location || name;
                if (res.primaryLabel === location) res.secondaryLabel = name;
                break;

            case Models.ControlPointDisplayType.NameEquipment:
                res.primaryLabel = name;
                if (equipment) res.secondaryLabel = equipment;
                break;

            case Models.ControlPointDisplayType.EquipmentName:
                res.primaryLabel = equipment || name;
                if (res.primaryLabel === equipment) res.secondaryLabel = name;
                break;
        }

        return res;
    }
}

export interface ControlPointDisplayLabels
{
    primaryLabel: string;
    secondaryLabel?: string;
}

export function compareControlPointDisplayLabels(a: ControlPointDisplayLabels,
                                                 b: ControlPointDisplayLabels,
                                                 ascending: boolean): number
{
    let res = UtilsService.compareStrings(a.primaryLabel, b.primaryLabel, ascending);
    return res === 0 ? UtilsService.compareStrings(a.secondaryLabel, b.secondaryLabel, ascending) : res;
}

export function hasTwoControlPointDisplayLabels(type: Models.ControlPointDisplayType): boolean
{
    switch (type)
    {
        case Models.ControlPointDisplayType.NameLocation:
        case Models.ControlPointDisplayType.NameFullLocation:
        case Models.ControlPointDisplayType.LocationName:
        case Models.ControlPointDisplayType.FullLocationName:
        case Models.ControlPointDisplayType.NameEquipment:
        case Models.ControlPointDisplayType.EquipmentName:
            return true;
    }

    return false;
}

interface DeviceElementTimeSeries
{
    element: DeviceElementExtended;
    timeSeries: Models.TimeSeriesSinglePropertyResponse;
}

export class ControlPointAggregation
{
    color: string;

    public readonly formattedValue: string;

    constructor(public readonly value: number,
                public readonly significantElement?: DeviceElementExtended,
                public readonly range?: Models.RangeSelection,
                public readonly unitsDisplay?: string)
    {
        this.formattedValue = AggregationHelper.numberWithUnitDisplay(value, unitsDisplay);
    }
}

abstract class Aggregator<TInput, TOutput>
{
    aggregate(numDataPoints: number,
              unitsDisplay?: string): TOutput
    {
        if (this.isReadyForAggregating())
        {
            for (let i = 0; i < numDataPoints; i++) this.processData(i);
        }
        return this.getFinalizedResult(unitsDisplay);
    }

    protected abstract isReadyForAggregating(): boolean;

    protected abstract processData(index: number): void;

    protected abstract getFinalizedResult(unitsDisplay?: string): TOutput;
}

export abstract class AggregationGroupAggregator extends Aggregator<ControlPointAggregation[], ControlPointAggregation>
{
    constructor(protected validChildAggs: ControlPointAggregation[])
    {
        super();
    }

    static doGroupAggregations(aggregationRoot: AggregationResult,
                               groupAggregationType: Models.AggregationTypeId,
                               granularity: Models.AggregationGranularity,
                               group: Models.ControlPointsGroup,
                               currDepth: number,
                               numTimeRanges: number,
                               unitsDisplay: string,
                               maxDepth = -1)
    {
        let children = aggregationRoot.children;
        if (children.length > 0)
        {
            if (maxDepth < 0 || currDepth < maxDepth)
            {
                children.forEach((childAggregation) =>
                                 {
                                     AggregationGroupAggregator.doGroupAggregations(childAggregation, groupAggregationType, granularity, group, currDepth + 1, numTimeRanges, unitsDisplay, maxDepth);
                                 });
            }

            let aggregationsByTimeRange = [];
            for (let timeRangeIdx = 0; timeRangeIdx < numTimeRanges; timeRangeIdx++)
            {
                let currValidChildAggregations = aggregationRoot.children
                                                                .map((child) => child.getAggByTimeRange(timeRangeIdx))
                                                                .filter((childAgg) => childAgg && !isNaN(childAgg.value));
                let groupAggregator            = AggregationGroupAggregator.getAggregator(groupAggregationType, currValidChildAggregations);
                aggregationsByTimeRange.push(groupAggregator.aggregate(currValidChildAggregations.length, unitsDisplay));
            }

            aggregationRoot.group                   = group;
            aggregationRoot.granularity             = granularity;
            aggregationRoot.aggType                 = groupAggregationType;
            aggregationRoot.aggregationsByTimeRange = aggregationsByTimeRange;
            aggregationRoot.depth                   = currDepth;
            aggregationRoot.unitsDisplay            = unitsDisplay;
            aggregationRoot.children                = children;
        }
    }

    private static getAggregator(childrenAggType: Models.AggregationTypeId,
                                 validChildAggs: ControlPointAggregation[]): AggregationGroupAggregator
    {
        switch (childrenAggType)
        {
            case Models.AggregationTypeId.NONE:
                return new AggregationGroupNoneAggregator(validChildAggs);

            case Models.AggregationTypeId.MAX:
                return new AggregationGroupMaxAggregator(validChildAggs);

            case Models.AggregationTypeId.MIN:
                return new AggregationGroupMinAggregator(validChildAggs);

            case Models.AggregationTypeId.SUM:
                return new AggregationGroupSumAggregator(validChildAggs);

            case Models.AggregationTypeId.MEAN:
                return new AggregationGroupMeanAggregator(validChildAggs);
        }

        throw new Error(`Unknown aggregation type. ${childrenAggType}`);
    }

    protected isReadyForAggregating(): boolean
    {
        return !!this.validChildAggs && !!this.validChildAggs.length;
    }
}

class AggregationGroupMaxAggregator extends AggregationGroupAggregator
{
    private max: number = -Number.MAX_VALUE;
    private significantElement: DeviceElementExtended;

    protected processData(index: number): void
    {
        let dataPoint = this.validChildAggs[index];
        if (dataPoint.value > this.max)
        {
            this.max                = dataPoint.value;
            this.significantElement = dataPoint.significantElement;
        }
    }

    protected getFinalizedResult(unitsDisplay?: string): ControlPointAggregation
    {
        if (this.max === -Number.MAX_VALUE)
        {
            this.max                = undefined;
            this.significantElement = undefined;
        }

        return new ControlPointAggregation(this.max, this.significantElement, undefined, unitsDisplay);
    }
}

class AggregationGroupMinAggregator extends AggregationGroupAggregator
{
    private min: number = Number.MAX_VALUE;
    private significantElement: DeviceElementExtended;

    protected processData(index: number): void
    {
        let dataPoint = this.validChildAggs[index];
        if (dataPoint.value < this.min)
        {
            this.min                = dataPoint.value;
            this.significantElement = dataPoint.significantElement;
        }
    }

    protected getFinalizedResult(unitsDisplay?: string): ControlPointAggregation
    {
        if (this.min === Number.MAX_VALUE)
        {
            this.min                = undefined;
            this.significantElement = undefined;
        }

        return new ControlPointAggregation(this.min, this.significantElement, undefined, unitsDisplay);
    }
}

class AggregationGroupSumAggregator extends AggregationGroupAggregator
{
    private sum: number = 0;

    protected processData(index: number): void
    {
        this.sum += this.validChildAggs[index].value;
    }

    protected getFinalizedResult(unitsDisplay?: string): ControlPointAggregation
    {
        return new ControlPointAggregation(this.validChildAggs.length ? this.sum : undefined, undefined, undefined, unitsDisplay);
    }
}

class AggregationGroupMeanAggregator extends AggregationGroupSumAggregator
{
    aggregate(numDataPoints?: number,
              unitsDisplay?: string): ControlPointAggregation
    {
        let len  = this.validChildAggs.length;
        let sum  = super.aggregate(len, unitsDisplay).value;
        let mean = !isNaN(sum) ? sum / len : undefined;
        return new ControlPointAggregation(mean, undefined, undefined, unitsDisplay);
    }
}

class AggregationGroupNoneAggregator extends AggregationGroupAggregator
{
    protected processData(index: number): void
    {
        // Noop
    }

    protected getFinalizedResult(): ControlPointAggregation
    {
        return new ControlPointAggregation(undefined);
    }
}

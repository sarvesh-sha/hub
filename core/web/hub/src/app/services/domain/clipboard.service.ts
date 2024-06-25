import {Injectable} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import {ErrorService} from "framework/services/error.service";
import {Lookup, UtilsService} from "framework/services/utils.service";

import {BehaviorSubject} from "rxjs";

@Injectable()
export class ClipboardService
{
    private m_clipboard: Map<any, ClipboardMeta<any>> = new Map();

    public contentsChanged = new BehaviorSubject<void>(null);

    constructor(private errorSvc: ErrorService)
    {
    }

    public getAll(): ClipboardEntryData<any, any>[]
    {
        let entries: ClipboardEntryData<any, any>[] = [];
        for (let key of this.m_clipboard.keys())
        {
            for (let entry of this.m_clipboard.get(key)?.all || [])
            {
                entries.push(entry);
            }
        }

        return entries;
    }

    public copy(...data: ClipboardEntryData<any, any>[])
    {
        // Clear old clipboard
        this.clear();

        // Insert new clipboard data
        for (let entry of data)
        {
            this.processEntry(entry);
        }

        // Notify of copy
        this.notifyCopy();
        this.contentsChanged.next();
    }

    public clear()
    {
        this.m_clipboard.clear();
        this.contentsChanged.next();
    }

    private processEntry(entry: ClipboardEntryData<any, any>)
    {
        let dataPrototype = Object.getPrototypeOf(entry);
        // Initialize type meta if none present
        if (!this.m_clipboard.has(dataPrototype))
        {
            this.m_clipboard.set(dataPrototype, new ClipboardMeta(this.m_clipboard.size));
        }

        // Add entry to type container
        this.m_clipboard.get(dataPrototype)
            .add(entry);
    }

    private notifyCopy()
    {
        let items        = Array.from(this.m_clipboard.values());
        let descriptions = items.map((meta) => `${meta.count} ${UtilsService.pluralize(meta.first.description, meta.count)}`);
        let message: string;

        if (descriptions.length > 1)
        {
            let last = descriptions.pop();
            message  = `${descriptions.join(", ")}, and ${last}`;
        }
        else
        {
            message = descriptions[0];
        }

        this.errorSvc.success(`Copied ${message} to clipboard`, 1500);
    }
}

export abstract class ClipboardEntryData<D extends Models.WidgetConfiguration, R extends Models.ReportLayoutBase>
{
    public readonly selectorGraphs: Models.SharedAssetGraph[] = [];
    public readonly selectors: Models.SharedAssetSelector[]   = [];
    public widgetOutline: Models.WidgetOutline;

    protected constructor(public readonly description: string)
    {
    }

    public abstract getDashboardWidget(): D;

    public abstract getReportItem(oldToNewGraphId: Lookup<string>): R;

    public getReportGraphs(): Models.SharedAssetGraph[]
    {
        return [];
    }

    //--//

    protected static getDashboardGroups(groups: Models.ControlPointsGroup[],
                                        sharedGraphs: Models.SharedAssetGraph[]): Models.ControlPointsGroup[]
    {
        let graphLookup  = UtilsService.extractLookup(sharedGraphs);
        let outputGroups = [];
        for (let group of groups)
        {
            let clone = Models.ControlPointsGroup.deepClone(group);
            if (group.pointInput)
            {
                let sharedGraph = graphLookup[group.pointInput.graphId];
                if (!sharedGraph) continue; // should not happen

                clone.pointInput = Models.AssetGraphBinding.newInstance({nodeId: group.pointInput.nodeId});
                clone.graph      = Models.AssetGraph.deepClone(sharedGraph.graph);
            }
            outputGroups.push(clone);
        }
        return outputGroups;
    }

    protected static getReportGroups(groups: Models.ControlPointsGroup[],
                                     sharedGraphs: Models.SharedAssetGraph[],
                                     lookup: Lookup<string>): Models.ControlPointsGroup[]
    {
        let outputGroups = [];
        for (let group of groups || [])
        {
            let clone = Models.ControlPointsGroup.deepClone(group);
            if (group.graph)
            {
                let graphId: string;
                if (lookup)
                {
                    let sharedGraph = sharedGraphs.find((sharedGraph) => UtilsService.compareJson(sharedGraph.graph, group.graph));
                    graphId         = lookup[sharedGraph?.id];

                    if (!graphId) continue;
                }

                clone.pointInput = Models.AssetGraphBinding.newInstance({
                                                                            graphId: graphId,
                                                                            nodeId : group.pointInput.nodeId
                                                                        });
                clone.graph      = null;
            }
            outputGroups.push(clone);
        }
        return outputGroups;
    }
}

class ClipboardMeta<T extends ClipboardEntryData<any, any>>
{
    public first: ClipboardEntryData<any, any>;
    public all: ClipboardEntryData<any, any>[] = [];
    public count                               = 0;

    constructor(public order: number)
    {}

    public add(entry: ClipboardEntryData<any, any>)
    {
        if (!this.first) this.first = entry;

        this.all.push(entry);
        this.count++;
    }
}

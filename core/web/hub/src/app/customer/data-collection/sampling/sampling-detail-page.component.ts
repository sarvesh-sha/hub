import {Component, Injector} from "@angular/core";

import {ReportError} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";

@Component({
               selector   : "o3-sampling-detail-page",
               templateUrl: "./sampling-detail-page.component.html",
               styleUrls  : ["./sampling-detail-page.component.scss"]
           })
export class SamplingDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    defaultPeriod: number = 1800;

    definitions: Models.DevicesTemplate;
    settings: Models.DevicesSamplingTemplate;

    root: SamplingTreeNode;
    nodes: SamplingTreeNode[];
    nodesLookup: Lookup<SamplingTreeNode> = {};
    selectedNodes                         = new Set<string>();
    selectedNodesEdited: string[]         = [];

    //--//

    constructor(inj: Injector)
    {
        super(inj);

    }

    protected onNavigationComplete(): void
    {
        this.load();
    }

    async load()
    {
        this.definitions = await this.app.domain.apis.discovery.describeDeviceTemplates();
        this.settings    = await this.app.domain.apis.discovery.getDeviceSamplingTemplate();

        let root                             = new SamplingTreeNode(null, null, null);
        let lookup: Lookup<SamplingTreeNode> = {};
        let selected                         = new Set<string>();

        for (let id in this.definitions.devices)
        {
            let node     = root;
            let nodePath = "";

            for (let path of id.split("/"))
            {
                nodePath = `${nodePath}/${path}`;

                let child = lookup[nodePath];
                if (!child)
                {
                    child = new SamplingTreeNode(node, nodePath, path);
                    node.children.push(child);
                    lookup[nodePath] = child;
                }

                node = child;
            }

            let device = this.definitions.devices[id];
            for (let field in device.elements)
            {
                let element = device.elements[field];

                let elementPath = `${nodePath}/${field}`;

                let child = lookup[elementPath];
                if (!child)
                {
                    child = new SamplingTreeNode(node, elementPath, element.displayName);
                    node.children.push(child);
                    lookup[elementPath] = child;

                    child.device  = id;
                    child.element = field;
                }

                let elementSampling = this.resolvePeriod(child, this.settings);
                if (elementSampling !== undefined)
                {
                    selected.add(elementPath);
                    child.period     = elementSampling;
                    child.periodOrig = elementSampling;
                }
            }
        }

        root.sort();

        this.root                = root;
        this.nodes               = root.children;
        this.nodesLookup         = lookup;
        this.selectedNodes       = selected;
        this.selectedNodesEdited = Array.from(selected);
    }

    private resolvePeriod(node: SamplingTreeNode,
                          settings: Models.DevicesSamplingTemplate): number
    {
        if (settings && settings.devices)
        {
            let deviceCfg = settings.devices[node.device];
            if (deviceCfg && deviceCfg.elements)
            {
                return deviceCfg.elements[node.element];
            }
        }

        return undefined;
    }

    private setPeriod(node: SamplingTreeNode)
    {
        if (!this.settings)
        {
            this.settings = new Models.DevicesSamplingTemplate();
        }

        if (!this.settings.devices)
        {
            this.settings.devices = {};
        }

        let deviceCfg = this.settings.devices[node.device];
        if (!deviceCfg)
        {
            deviceCfg                          = new Models.DeviceSamplingTemplate();
            deviceCfg.elements                 = {};
            this.settings.devices[node.device] = deviceCfg;
        }

        deviceCfg.elements[node.element] = node.period;
    }

    private removePeriod(node: SamplingTreeNode)
    {
        if (this.settings && this.settings.devices)
        {
            let deviceCfg = this.settings.devices[node.device];
            if (deviceCfg)
            {
                delete deviceCfg.elements[node.element];

                if (UtilsService.isMapEmpty(deviceCfg.elements))
                {
                    delete this.settings.devices[node.device];

                    if (UtilsService.isMapEmpty(this.settings.devices))
                    {
                        this.settings = null;
                    }
                }
            }
        }
    }

    //-//

    @ReportError
    async save()
    {
        await this.app.domain.apis.discovery.setDeviceSamplingTemplate(this.settings);
        this.load();
    }

    cancel()
    {
        this.load();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    selectionChanged()
    {
        let newSelected = new Set<string>();

        for (let path of this.selectedNodesEdited)
        {
            newSelected.add(path);
        }

        for (let oldId of this.selectedNodes)
        {
            if (!newSelected.has(oldId))
            {
                let node = this.nodesLookup[oldId];

                node.period = undefined;
                this.removePeriod(node);
            }
        }

        for (let newId of newSelected)
        {
            if (!this.selectedNodes.has(newId))
            {
                let node = this.nodesLookup[newId];

                node.period = this.defaultPeriod;
                this.setPeriod(node);
            }
        }

        this.selectedNodes = newSelected;
    }

    periodChanged(node: SamplingTreeNode)
    {
        this.setPeriod(node);
    }

    //--//

    async importSettings()
    {
        let result = await ImportDialogComponent.open(this, "Settings Import", {
            returnRawBlobs: () => false,
            parseFile     : async (contents: string) =>
            {
                let settings = <Models.DevicesSamplingTemplate>JSON.parse(contents);
                Models.DevicesSamplingTemplate.fixupPrototype(settings);
                return settings;
            }
        });

        if (result)
        {
            this.root.forEach((node) =>
                              {
                                  let elementSampling = this.resolvePeriod(node, result);
                                  if (elementSampling != node.period)
                                  {
                                      if (elementSampling)
                                      {
                                          this.selectedNodes.add(node.id);

                                          node.period = elementSampling;
                                          this.setPeriod(node);
                                      }
                                      else
                                      {
                                          this.selectedNodes.delete(node.id);

                                          node.period = undefined;
                                          this.removePeriod(node);
                                      }
                                  }
                              });

            this.selectedNodesEdited = Array.from(this.selectedNodes);
        }
    }

    async exportSettings()
    {
        return DownloadDialogComponent.open(this, "Settings Export", DownloadDialogComponent.fileName("sampling_settings"), this.settings);
    }
}

class SamplingTreeNode implements ITreeNode<string>
{
    constructor(public readonly parent: SamplingTreeNode,
                public readonly id: string,
                public readonly label: string)
    {
    }

    public device: string;
    public element: string;

    public periodOrig: number;

    public get added(): boolean
    {
        return this.periodOrig === undefined && this.period !== undefined;
    }

    public get changed(): boolean
    {
        return this.periodOrig !== undefined && this.period !== undefined && this.periodOrig != this.period;
    }

    public get removed(): boolean
    {
        return this.periodOrig !== undefined && this.period === undefined;
    }

    public get hasChanged(): boolean
    {
        return this.numberOfAddedNodes.value > 0 || this.numberOfChangedNodes.value > 0 || this.numberOfRemovedNodes.value > 0;
    }

    private m_period: number;
    public get period(): number
    {
        return this.m_period;
    }

    public set period(val: number)
    {
        this.m_period = val;

        let obj: SamplingTreeNode = this;
        while (obj)
        {
            obj.numberOfSelectedNodes.invalidate();
            obj.numberOfAddedNodes.invalidate();
            obj.numberOfChangedNodes.invalidate();
            obj.numberOfRemovedNodes.invalidate();

            obj = obj.parent;
        }
    }

    public children: SamplingTreeNode[] = [];

    public get hasChildren(): boolean
    {
        return this.children.length > 0;
    }

    public get disableSelection(): boolean
    {
        return this.children.length > 0;
    }

    public numberOfNodes = new LazyComputation(() =>
                                               {
                                                   let count = 0;
                                                   this.children.forEach((child) => count += child.numberOfNodes.value);
                                                   return Math.max(1, count); // If we are a leaf, we count as one, otherwise zero.
                                               });

    public numberOfSelectedNodes = new LazyComputation(() =>
                                                       {
                                                           let count = this.period !== undefined ? 1 : 0;
                                                           this.children.forEach((child) => count += child.numberOfSelectedNodes.value);
                                                           return count;
                                                       });

    public numberOfAddedNodes = new LazyComputation(() =>
                                                    {
                                                        let count = this.added ? 1 : 0;
                                                        this.children.forEach((child) => count += child.numberOfAddedNodes.value);
                                                        return count;
                                                    });

    public numberOfChangedNodes = new LazyComputation(() =>
                                                      {
                                                          let count = this.changed ? 1 : 0;
                                                          this.children.forEach((child) => count += child.numberOfChangedNodes.value);
                                                          return count;
                                                      });

    public numberOfRemovedNodes = new LazyComputation(() =>
                                                      {
                                                          let count = this.removed ? 1 : 0;
                                                          this.children.forEach((child) => count += child.numberOfRemovedNodes.value);
                                                          return count;
                                                      });

    public sort()
    {
        this.children.sort((a,
                            b) => UtilsService.compareStrings(a.label, b.label, true));

        for (let child of this.children)
        {
            child.sort();
        }
    }

    public forEach(callback: (node: SamplingTreeNode) => void)
    {
        if (this.id)
        {
            callback(this);
        }

        for (let child of this.children)
        {
            child.forEach(callback);
        }
    }
}

class LazyComputation<T>
{
    constructor(private readonly callback: () => T) {}

    private m_valid = false;
    private m_value: T;

    invalidate()
    {
        this.m_valid = false;
        this.m_value = undefined;
    }

    get value(): T
    {
        if (!this.m_valid)
        {
            this.m_value = this.callback();
            this.m_valid = true;
        }

        return this.m_value;
    }
}

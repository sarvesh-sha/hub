import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {AssetGraphExtended, AssetGraphResponseHolder} from "app/services/domain/asset-graph.service";
import {AssetExtended, DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {AssetGraphWizardState} from "app/shared/assets/asset-graph-wizard/asset-graph-wizard.component";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-gps-map-configurer[gpsAssets][config]",
               templateUrl    : "./gps-map-configurer.component.html",
               styleUrls      : ["./gps-map-configurer.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class GpsMapConfigurerComponent extends SharedSvc.BaseApplicationComponent implements OverlayController
{
    @Input() dimension: string = DeviceElementExtended.PRESENT_VALUE;

    private m_gpsAssets: AssetExtended[];
    @Input() set gpsAssets(gpsAssets: AssetExtended[])
    {
        if (gpsAssets && this.m_gpsAssets !== gpsAssets)
        {
            this.m_gpsAssets = gpsAssets;
            this.updateContext();
        }
    }

    private m_config: Models.TimeSeriesTooltipConfiguration = Models.TimeSeriesTooltipConfiguration.newInstance({entries: []});
    @Input() set config(config: Models.TimeSeriesTooltipConfiguration)
    {
        if (config && this.m_config !== config)
        {
            this.m_config = config;
            if (this.m_gpsAssets) this.updateSelections();
        }
    }

    private originalGraph: Models.TimeSeriesGraphConfiguration;

    private m_graph: Models.TimeSeriesGraphConfiguration;
    @Input() set graph(graph: Models.TimeSeriesGraphConfiguration)
    {
        if (graph)
        {
            if (!this.originalGraph) this.originalGraph = graph;

            this.m_graph              = graph;
            this.editGraphState.graph = this.m_graph;

            this.updateGraph();
        }
    }

    pristine: boolean;

    editGraphState: AssetGraphWizardState = new AssetGraphWizardState();

    private graphContext: Models.AssetGraphContextLocations;
    private responseHolder: AssetGraphResponseHolder;
    graphSelections: string[] = [];

    private activeGraphExt: AssetGraphExtended;

    graphSourceEntries: SourceEntry[];

    @ViewChild(StandardFormOverlayComponent, {static: true}) configurer: StandardFormOverlayComponent;
    @ViewChild(OverlayComponent, {static: true}) graphConfigurer: OverlayComponent;

    @Output() configChange = new EventEmitter<Models.TimeSeriesTooltipConfiguration>();
    @Output() graphChange  = new EventEmitter<Models.TimeSeriesGraphConfiguration>();

    configurerConfig = OverlayConfig.onTopDraggable({
                                                        maxWidth : 450,
                                                        maxHeight: 550
                                                    });

    graphConfigurerConfig = OverlayConfig.newInstance({
                                                          containerClasses: ["dialog-xl"],
                                                          showBackdrop    : false,
                                                          showCloseButton : true
                                                      });

    private async updateContext()
    {
        this.graphContext = Models.AssetGraphContextLocations.newInstance({locationSysIds: this.m_gpsAssets.map((gpsAsset) => gpsAsset.model.location.sysId)});

        if (this.m_config)
        {
            await this.updateSelections();
        }
        else
        {
            await this.updateGraph();
        }
    }

    private async updateSelections()
    {
        this.graphSelections = this.m_config.entries.map((entry) => entry.binding.nodeId);
        await this.updateGraph();
    }

    private async updateGraph()
    {
        let graph = this.m_graph?.sharedGraphs[0]?.graph;
        if (!graph || !this.graphContext) return;

        this.activeGraphExt = new AssetGraphExtended(this.app.domain, graph);
        this.responseHolder = await this.activeGraphExt.resolveWithContext([this.graphContext]);

        await this.updateGraphEntries();
    }

    public setGraphSelections()
    {
        this.graphSelections = this.editGraphState.graphSelections;
        this.graph           = this.editGraphState.graph;
    }

    private async updateGraphEntries()
    {
        this.editGraphState.graphSelections = this.graphSelections;

        let bindingAssetIds: { bindingId: string, assetRecord: Models.RecordIdentity }[] = [];
        for (let bindingId of this.graphSelections)
        {
            let assetRecords = this.responseHolder.resolveIdentities(Models.AssetGraphBinding.newInstance({nodeId: bindingId}));
            let assetRecord  = assetRecords[0];
            if (assetRecord)
            {
                bindingAssetIds.push({
                                         bindingId  : bindingId,
                                         assetRecord: assetRecord
                                     });
            }
        }

        let deviceElementRecordIds = bindingAssetIds.map((bindingTuple) => bindingTuple.assetRecord);
        let helperDeviceElemExts   = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, deviceElementRecordIds);

        this.graphSourceEntries = await mapInParallel(bindingAssetIds, async (bindingTuple,
                                                                              idx) =>
        {
            let deviceElem = helperDeviceElemExts[idx];

            let entryConfig          = this.m_config.entries.find((entry) => entry.binding.nodeId === bindingTuple.bindingId);
            let schema               = await deviceElem.getSchemaProperty(this.dimension);
            let selectedUnitsFactors = entryConfig?.unitsFactors || await this.app.domain.units.findPreferred(schema?.unitsFactors);
            let selectedUnits        = await this.app.domain.units.resolveDescriptor(selectedUnitsFactors, false);

            return new SourceEntry(bindingTuple.bindingId, this.activeGraphExt.getNodeName(bindingTuple.bindingId), schema?.unitsFactors, selectedUnits);
        });

        this.updatePristine();

        this.markForCheck();
    }

    public updatePristine()
    {
        this.pristine = false;

        if (this.m_graph !== this.originalGraph) return;

        if (this.graphSourceEntries.length !== this.m_config.entries.length) return;
        for (let i = 0; i < this.m_config.entries.length; i++)
        {
            let entry     = this.m_config.entries[i];
            let compareTo = this.graphSourceEntries[i];
            if (entry.binding.nodeId !== compareTo.bindingId) return;
            if (!compareTo.units.sameFactors(entry.unitsFactors)) return;
        }

        this.pristine = true;
    }

    public emitChanges()
    {
        this.graphChange.emit(this.originalGraph = this.m_graph);

        this.configChange.emit(this.m_config = Models.TimeSeriesTooltipConfiguration.newInstance(
            {
                entries: this.graphSourceEntries.map((entry) => entry.toModel())
                             .filter((entry) => !!entry)
            }));

        this.pristine = true;
    }

    public closeOverlay()
    {
        this.configurer?.closeOverlay();
    }

    public isOpen(): boolean
    {
        return this.configurer?.isOpen();
    }

    public async openOverlay()
    {
        this.m_graph = this.originalGraph;

        await this.updateSelections();

        this.configurer?.openOverlay();
    }

    public toggleOverlay(open?: boolean)
    {
        let isOpen = this.isOpen();
        if (isOpen && open !== true)
        {
            this.closeOverlay();
        }
        else if (!isOpen && open !== false)
        {
            this.openOverlay();
        }
    }
}

class SourceEntry
{
    constructor(readonly bindingId: string,
                readonly label: string,
                readonly unitsFactors: Models.EngineeringUnitsFactors,
                public units: EngineeringUnitsDescriptorExtended)
    {
    }

    toModel(): Models.TimeSeriesTooltipEntry
    {
        return Models.TimeSeriesTooltipEntry.newInstance(
            {
                binding     : Models.AssetGraphBinding.newInstance({nodeId: this.bindingId}),
                unitsFactors: EngineeringUnitsDescriptorExtended.extractFactors(this.units)
            }
        );
    }
}

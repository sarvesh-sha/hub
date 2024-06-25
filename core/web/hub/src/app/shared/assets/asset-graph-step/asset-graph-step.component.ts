import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {AssetGraphExtended, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {AssetGraphEditorComponent} from "app/shared/assets/asset-graph-editor/asset-graph-editor.component";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {StateHistory} from "app/shared/undo/undo-redo-state";

import {SelectComponent} from "framework/ui/forms/select.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector       : "o3-asset-graph-step",
               styleUrls      : ["./asset-graph-step.component.scss"],
               templateUrl    : "./asset-graph-step.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AssetGraphStepComponent extends BaseApplicationComponent
{
    normalization: Models.NormalizationRules;

    stateHistory: StateHistory<AssetGraphExtended> = new StateHistory(null);

    sharedGraphId: string = "";

    @Input() graphId: string;
    @Input() host: GraphConfigurationHost;

    @Input() graph: Models.AssetGraph;
    @Output() graphChange = new EventEmitter<Models.AssetGraph>();

    graphExtended: AssetGraphExtended;

    structureName = "";

    hasSavedStructures = false;

    @ViewChild("test_importButton", {read: ElementRef}) test_importButton: ElementRef;
    @ViewChild("test_importStandardForm") test_importStandardForm: StandardFormOverlayComponent;
    @ViewChild("editor") test_graphEditor: AssetGraphEditorComponent;
    @ViewChild("test_importSelect") test_importSelect: SelectComponent<string>;

    get copyFromSharedTooltip(): string
    {
        if (this.hasSavedStructures)
        {
            return "Copy From Shared Asset Structure";
        }
        else
        {
            return "No Shared Asset Structures Available";
        }
    }

    async ngOnInit()
    {
        super.ngOnInit();

        if (!this.graph)
        {
            this.graph = AssetGraphExtended.emptyModel();
            this.graphChange.emit(this.graph);
        }

        this.graphExtended = new AssetGraphExtended(this.app.domain, this.graph);

        this.normalization      = await this.app.bindings.getActiveNormalizationRules();
        let savedIds            = await this.app.domain.assetGraphs.getGraphIds();
        this.hasSavedStructures = savedIds?.length > 0;
        this.markForCheck();
    }

    graphExtUpdated()
    {
        this.graph = this.graphExtended.model;
        this.graphChange.emit(this.graph);
    }

    async importStructure()
    {
        let graph = await this.app.domain.assetGraphs.getConfig(this.sharedGraphId);

        if (graph)
        {
            this.graph = graph.graph;
        }
        else
        {
            // Graph was deleted, start from scratch.
            this.graph = AssetGraphExtended.emptyModel();
        }

        this.graphExtended = new AssetGraphExtended(this.app.domain, this.graph);
        this.graphChange.emit(this.graph);

        this.sharedGraphId = null;
        this.markForCheck();
    }

    async saveStructure()
    {
        let model  = SharedAssetGraphExtended.newModel(this.graph, null, this.structureName);
        let shared = new SharedAssetGraphExtended(this.app.domain, model);
        await shared.save();
        this.structureName      = "";
        this.hasSavedStructures = true;
        this.markForCheck();
    }
}

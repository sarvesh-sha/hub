import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {AssetGraphExtended, AssetGraphResponseExtended} from "app/services/domain/asset-graph.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {PaneConfigurationExtended, PaneFieldConfigurationExtended} from "app/services/domain/panes.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {ControlOption} from "framework/ui/control-option";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector   : "o3-pane-fields-editor",
               styleUrls  : ["./pane-fields-editor.component.scss"],
               templateUrl: "./pane-fields-editor.component.html"
           })
export class PaneFieldsEditorComponent extends BaseApplicationComponent
{
    @Input() data: PaneConfigurationExtended;

    @Output() dataUpdated = new EventEmitter<void>();

    @ViewChild(StandardFormOverlayComponent) paneConfigOverlay: StandardFormOverlayComponent;
    @ViewChild(OverlayComponent, {static: true}) fieldConfigOverlay: OverlayComponent;

    //--//

    private m_focus: Models.PaneConfiguration | Models.PaneCardConfiguration | Models.PaneFieldConfiguration;
    public get focus(): Models.PaneConfiguration | Models.PaneCardConfiguration | Models.PaneFieldConfiguration
    {
        return this.m_focus;
    }

    public set focus(value: Models.PaneConfiguration | Models.PaneCardConfiguration | Models.PaneFieldConfiguration)
    {
        this.m_focus = value;
    }

    //--//

    cardContext: Models.PaneCardConfiguration;

    range = RangeSelectionExtended.newModel();

    overlayConfig = OverlayConfig.onTopDraggable({
                                                     containerClasses: ["dialog-md"],
                                                     width           : 1000
                                                 });

    graph: AssetGraphExtended;

    graphOptions: ControlOption<string>[];
    selectedGraph: string;
    selectedGraphContexts: Models.AssetGraphContextAsset[];
    currentResponse: AssetGraphResponseExtended;

    paneFields = new Map<Models.PaneFieldConfiguration, Models.PaneField>();

    units: ControlOption<EngineeringUnitsDescriptorExtended>[] = [];

    badStructure = false;

    async ngOnInit()
    {
        super.ngOnInit();

        if (this.data)
        {
            this.focus = this.data.model;
        }

        let descriptors = await this.app.domain.units.describeEngineeringUnits();
        this.units      = descriptors.map((descExt) => descExt.controlPointWithDescription);

        this.graph = this.data.graph;

        let graphResponse = await this.graph.resolve();
        let allOptions    = await graphResponse.getControlOptions();

        // Only consider single roots
        let root           = this.graph.getRootNodes()[0]?.id;
        let options        = allOptions.get(root) || [];
        this.graphOptions  = options;
        this.selectedGraph = options[0]?.id;
        if (!this.selectedGraph)
        {
            this.badStructure = true;
        }
        else
        {
            await this.evaluate();
        }
    }

    async evaluate()
    {
        if (this.selectedGraph)
        {
            this.selectedGraphContexts = [Models.AssetGraphContextAsset.newInstance({sysId: this.selectedGraph})];
            let response               = await this.graph.resolveWithContext(this.selectedGraphContexts);
            this.currentResponse       = response.responses[0];
            this.paneFields.clear();
            this.detectChanges();
            for (let card of this.data.elements)
            {
                for (let field of card.fields)
                {
                    let ext = PaneFieldConfigurationExtended.newInstance(field, this.currentResponse);
                    if (ext.canResolve())
                    {
                        this.paneFields.set(field, await ext.resolve());
                    }
                }
            }
        }
    }

    get paneFocus(): Models.PaneConfiguration
    {
        return this.focus instanceof Models.PaneConfiguration ? this.focus : undefined;
    }

    get cardFocus(): Models.PaneCardConfiguration
    {
        return this.focus instanceof Models.PaneCardConfiguration ? this.focus : undefined;
    }

    get fieldFocus(): Models.PaneFieldConfiguration
    {
        return this.focus instanceof Models.PaneFieldConfiguration ? this.focus : undefined;
    }

    get configTitle(): string
    {
        if (this.paneFocus)
        {
            return "Pane Configuration";
        }

        if (this.cardFocus)
        {
            return "Card Configuration";
        }

        if (this.fieldFocus)
        {
            return "Field Configuration";
        }

        return "";
    }

    get titleInputName(): string
    {
        if (this.data.model.titleInput && this.graph)
        {
            let node = this.graph.getNodeById(this.data.model.titleInput.nodeId);
            return node.name;
        }

        return "";
    }

    configureField(card: Models.PaneCardConfiguration,
                   field: Models.PaneFieldConfiguration)
    {
        this.focus       = field;
        this.cardContext = card;
        this.fieldConfigOverlay?.toggleOverlay();
    }

    configurePane()
    {
        this.focus = this.data.model;
        this.paneConfigOverlay.openOverlay();
    }

    drop(event: CdkDragDrop<any>,
         card: Models.PaneCardConfiguration)
    {
        moveItemInArray(card.fields, event.previousIndex, event.currentIndex);
    }

    getInput(id: string): Models.AssetGraphBinding
    {
        if (id)
        {
            return Models.AssetGraphBinding.newInstance({nodeId: id});
        }

        return null;
    }

    addField(field: Models.PaneFieldConfiguration)
    {
        this.cardContext.fields.push(field);
        this.updateField(field);
    }

    async updateField(field: Models.PaneFieldConfiguration)
    {
        let index = this.cardContext.fields.indexOf(this.fieldFocus);
        this.cardContext.fields.splice(index, 1, field);
        this.paneFields.delete(this.fieldFocus);
        this.detectChanges();

        let ext = PaneFieldConfigurationExtended.newInstance(field, this.currentResponse);
        if (ext.canResolve())
        {
            this.paneFields.set(field, await ext.resolve());
        }

        this.closeFieldOverlay();
        this.dataUpdated.emit();
    }

    closeFieldOverlay()
    {
        this.focus = null;
        this.fieldConfigOverlay?.closeOverlay();
    }

    addCard()
    {
        let card = Models.PaneCardConfiguration.newInstance({
                                                                title : "New Card",
                                                                fields: []
                                                            });
        this.data.model.elements.push(card);
        this.focus = card;
    }

    async removeCard(card: Models.PaneCardConfiguration)
    {
        if (!card.fields.length || await this.confirmOperation("Are you sure you want to delete the card and all of it's fields?"))
        {
            for (let field of card.fields) this.paneFields.delete(field);

            this.data.elements.splice(this.data.elements.indexOf(card), 1);
            if (this.focus === card) this.focus = null;

            this.dataUpdated.emit();
        }
    }

    async removeField(card: Models.PaneCardConfiguration,
                      field: Models.PaneFieldConfiguration)
    {
        if (await this.confirmOperation("Are you sure you want to delete this field?"))
        {
            let fields = card.fields;
            fields.splice(fields.indexOf(field), 1);
            this.paneFields.delete(field);
            if (this.focus === field) this.closeFieldOverlay();

            this.dataUpdated.emit();
        }
    }
}

import {Component, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-graph-selector-step",
               templateUrl: "./widget-editor-wizard-graph-selector-step.component.html",
               providers  : [WizardStep.createProvider(WidgetEditorWizardGraphSelectorStepComponent)]
           })
export class WidgetEditorWizardGraphSelectorStepComponent extends WizardStep<WidgetEditorWizardState>
{
    graphOptions: ControlOption<string>[] = [];

    private m_selectorOptions: ControlOption<string>[];

    private get typedWidget(): Models.AssetGraphSelectorWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AssetGraphSelectorWidgetConfiguration);
    }

    private m_prevSelector: Models.SharedAssetSelector;
    private m_selector: Models.SharedAssetSelector;
    private m_graphId: string;
    public get graphId(): string
    {
        return this.m_graphId;
    }

    public set graphId(graphId: string)
    {
        let widget = this.typedWidget;
        if (widget)
        {
            let dashboardExt = this.data.editor.dashboardExt;

            if (this.m_prevSelector)
            {
                dashboardExt.selectors[this.m_selector.id] = this.m_prevSelector;
            }

            this.m_selector = Models.SharedAssetSelector.newInstance({
                                                                         id     : UUID.UUID(),
                                                                         name   : this.data.editor.dashboardExt.getUniqueSelectorName(graphId),
                                                                         graphId: graphId
                                                                     });
            this.updateSelectors();
            widget.selectorId = this.m_selector.id;

            this.m_graphId = graphId;
            this.updateSelectorOptions();
        }
    }

    public get selectorName(): string
    {
        return this.m_selector?.name;
    }

    public set selectorName(name: string)
    {
        if (this.m_selector)
        {
            this.m_selector.name = name;
        }
    }

    @ViewChild("test_graphs") test_graphs: SelectComponent<string>;

    public getLabel(): string
    {
        return "Selector";
    }

    public isEnabled(): boolean
    {
        return !!this.typedWidget;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (!this.m_selector) return false;
        if (!this.m_selectorOptions) return false;

        let selectorId = this.typedWidget?.selectorId;
        if (!selectorId) return false;

        for (let option of this.m_selectorOptions)
        {
            if (option.label === this.m_selector.name && option.id !== selectorId)
            {
                return false;
            }
        }

        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected(): Promise<void>
    {
        let allGraphs     = await this.data.editor.dashboardExt.resolveGraphs();
        this.graphOptions = [];
        for (let [key, graph] of allGraphs.entries())
        {
            this.graphOptions.push(new ControlOption<string>(key, graph.name));
        }
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        let widget = this.typedWidget;
        if (widget && this.m_selector?.id != widget.selectorId)
        {
            this.m_prevSelector = this.data.editor.dashboardExt.selectors[widget.selectorId];
            this.m_selector     = Models.SharedAssetSelector.deepClone(this.m_prevSelector);

            this.updateSelectors();

            this.m_graphId = this.m_selector?.graphId;
        }

        await this.updateSelectorOptions();
    }

    private updateSelectors()
    {
        let dashboardExt                           = this.data.editor.dashboardExt;
        dashboardExt.selectors[this.m_selector.id] = this.m_selector;
        dashboardExt.model.sharedSelectors         = UtilsService.extractKeysFromMap(dashboardExt.selectors)
                                                                 .map((selectorId) => dashboardExt.selectors[selectorId]);
    }

    private async updateSelectorOptions()
    {
        if (this.m_graphId)
        {
            this.m_selectorOptions = await this.data.editor.dashboardExt.getSelectorOptions(this.m_graphId);
            this.markForCheck();
        }
    }
}

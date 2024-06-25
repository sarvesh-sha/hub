import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-interaction-behavior-step",
               templateUrl: "./widget-editor-wizard-interaction-behavior-step.component.html",
               providers  : [WizardStep.createProvider(WidgetEditorWizardInteractionBehaviorStepComponent)]
           })
export class WidgetEditorWizardInteractionBehaviorStepComponent extends WizardStep<WidgetEditorWizardState>
{
    hasPaneOptions: boolean = false;

    standardBehaviorLabel: string;
    behaviorHeader: string;

    private m_clickMode = Models.InteractionBehaviorType.Standard;
    get clickMode(): Models.InteractionBehaviorType
    {
        return this.m_clickMode;
    }

    set clickMode(mode: Models.InteractionBehaviorType)
    {
        this.m_clickMode = mode || Models.InteractionBehaviorType.Standard;

        let clickBehavior = this.typedWidget?.clickBehavior;
        if (clickBehavior)
        {
            clickBehavior.type = this.m_clickMode;

            if (this.m_clickMode === Models.InteractionBehaviorType.Pane)
            {
                if (this.m_clickMode !== Models.InteractionBehaviorType.Pane)
                {
                    clickBehavior.paneConfigId = null;
                }
                else
                {
                    clickBehavior.paneConfigId = this.paneConfigId;
                }
            }
        }
    }

    get typedDataAggregationWidget(): Models.AggregationTableWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AggregationTableWidgetConfiguration);
    }

    private get typedWidget(): Models.AggregationTableWidgetConfiguration | Models.AlertMapWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AlertMapWidgetConfiguration) || this.typedDataAggregationWidget;
    }

    private m_paneConfigId: string;
    get paneConfigId(): string
    {
        return this.m_paneConfigId || "";
    }

    set paneConfigId(id: string)
    {
        if (id) this.m_paneConfigId = id;

        let clickBehavior = this.typedWidget?.clickBehavior;
        if (clickBehavior)
        {
            clickBehavior.paneConfigId = id;
        }
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        let cfg = this.typedWidget;
        if (cfg)
        {
            if (!cfg.clickBehavior) cfg.clickBehavior = Models.InteractionBehavior.newInstance({type: Models.InteractionBehaviorType.Standard});

            if (cfg.clickBehavior.type === Models.InteractionBehaviorType.Pane)
            {
                this.m_paneConfigId = cfg.clickBehavior.paneConfigId;
                if (!this.m_paneConfigId) cfg.clickBehavior.type = Models.InteractionBehaviorType.Standard;
            }
            this.clickMode = cfg.clickBehavior.type;
        }

        let paneOptions     = await this.data.app.bindings.getPaneConfigurations();
        this.hasPaneOptions = !!paneOptions?.length;
    }

    public getLabel() { return "Interactivity"; }

    public isEnabled()
    {
        let cfg = this.data.editor.widget;

        if (cfg instanceof Models.AggregationTableWidgetConfiguration)
        {
            return cfg.visualizationMode === Models.HierarchicalVisualizationType.TABLE ||
                   cfg.visualizationMode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR;
        }
        if (cfg instanceof Models.AlertMapWidgetConfiguration) return this.hasPaneOptions;

        return false;
    }

    public isValid()
    {
        if (this.clickMode === Models.InteractionBehaviorType.Pane) return !!this.paneConfigId;

        return true;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
        this.updateOptions();
    }

    private updateOptions()
    {
        if (this.data.editor.widget instanceof Models.AlertMapWidgetConfiguration)
        {
            this.behaviorHeader        = "How will your pins respond to clicks?";
            this.standardBehaviorLabel = "Show default pin tooltip";
        }
        else if (this.typedDataAggregationWidget)
        {
            this.behaviorHeader        = "How will your table respond to clicks?";
            this.standardBehaviorLabel = "Navigate to control point";
        }
    }
}

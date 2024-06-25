import {Component, Injector} from "@angular/core";

import {AppContext} from "app/app.service";
import * as Models from "app/services/proxy/model/models";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-data-source-wizard-interaction-behavior-step",
               templateUrl: "./data-source-wizard-interaction-behavior-step.component.html",
               providers  : [WizardStep.createProvider(DataSourceWizardInteractionBehaviorStepComponent)]
           })
export class DataSourceWizardInteractionBehaviorStepComponent extends WizardStep<DataSourceWizardState>
{
    get model(): Models.InteractionBehavior
    {
        let hierarchy = this.data.hierarchy;
        if (hierarchy && !hierarchy.interactionBehavior) hierarchy.interactionBehavior = new Models.InteractionBehavior();

        return hierarchy?.interactionBehavior;
    }

    get interactionBehavior(): Models.InteractionBehaviorType
    {
        return this.model?.type || Models.InteractionBehaviorType.Standard;
    }

    set interactionBehavior(behavior: Models.InteractionBehaviorType)
    {
        if (this.model) this.model.type = behavior;

        if (behavior === Models.InteractionBehaviorType.Pane)
        {
            this.model.paneConfigId = this.m_paneConfigId;
        }
        else
        {
            this.model.paneConfigId = null;
        }
    }

    get behaviorHeader(): string
    {
        return "How will your visualization respond to clicks?";
    }

    private m_paneIds = new Set<string>();
    private m_paneConfigId: string;
    get paneConfigId(): string
    {
        return this.m_paneConfigId || "";
    }

    set paneConfigId(id: string)
    {
        if (id) this.m_paneConfigId = id;
        if (this.model) this.model.paneConfigId = id;
    }

    constructor(inj: Injector,
                private m_app: AppContext)
    {
        super(inj);
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        for (let option of await this.m_app.bindings.getPaneConfigurations()) this.m_paneIds.add(option.id);
    }

    public getLabel(): string
    {
        return "Interactivity";
    }

    public isEnabled(): boolean
    {
        return this.data.purpose === DataSourceWizardPurpose.visualization && this.data.type === Models.TimeSeriesChartType.HIERARCHICAL;
    }

    public isNextJumpable(): boolean
    {
        return false;
    }

    public isValid(): boolean
    {
        if (!this.model) return false;
        if (this.model.type === Models.InteractionBehaviorType.Pane) return this.m_paneIds.has(this.model.paneConfigId);

        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected(): Promise<void>
    {
    }

    public async onData(): Promise<void>
    {
        await super.onData();

        if (this.model?.type === Models.InteractionBehaviorType.Pane) this.m_paneConfigId = this.model.paneConfigId;
    }
}

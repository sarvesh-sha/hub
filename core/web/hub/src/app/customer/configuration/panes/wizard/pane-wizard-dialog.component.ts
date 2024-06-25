import {Component, Inject, Injector} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {PaneConfigurationExtended, PaneFieldConfigurationExtended} from "app/services/domain/panes.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./pane-wizard-dialog.component.html"
           })
export class PaneWizardDialogComponent extends WizardDialogComponent<PaneWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: PaneWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, PaneWizardDialogComponent);
    }
}

export class PaneWizardState extends WizardDialogState
{
    pane: PaneConfigurationExtended;
    normalization: Models.NormalizationRules;

    nodeIds: Set<string>;

    constructor(domainContext: AppDomainContext,
                model?: PaneConfigurationExtended)
    {
        super(!model);
        this.pane = model ? model : new PaneConfigurationExtended(domainContext);

        this.updateNodeIds();
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/configuration/panes", [this.pane.model.id]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            await this.pane.save();
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            this.normalization = await comp.app.bindings.getActiveNormalizationRules();
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public updateNodeIds()
    {
        this.nodeIds = new Set();
        for (let element of this.pane.model.elements || [])
        {
            for (let field of element.fields)
            {
                PaneFieldConfigurationExtended.newInstance(field, null)
                                              .collectNodeIds(this.nodeIds);
            }
        }
    }

    public nodeInUse(nodeId: string): boolean
    {
        return this.nodeIds?.has(nodeId);
    }
}


import {Component, Inject, Injector} from "@angular/core";
import {AppContext} from "app/app.service";
import {AlertDefinitionVersionExtended} from "app/services/domain/alert-definition-versions.service";
import * as Alerts from "app/services/domain/alert-definitions.service";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {Future} from "framework/utils/concurrency";

@Component({
               templateUrl: "./alert-rule-wizard-dialog.component.html"
           })
export class AlertRuleWizardDialogComponent extends WizardDialogComponent<AlertRuleWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: AlertRuleWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, AlertRuleWizardDialogComponent);
    }
}

export class AlertRuleWizardState extends WizardDialogState
{
    rules: AlertRulesData;

    version: string    = "new";
    versionId: string  = null;
    isLibrary: boolean = false;
    isCopy: boolean    = false;

    constructor(version: string   = "new",
                versionId: string = null,
                library: boolean  = false,
                copy: boolean     = false,
                step: string      = null)
    {
        super(version === "new" || copy, step);

        this.version   = version;
        this.versionId = versionId;
        this.isLibrary = library;
        this.isCopy    = copy;

        this.rules = new AlertRulesData();
    }


    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/configuration/alert-rules/alert-rule", [this.rules.alertId]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            this.rules.alertDefinition = await this.rules.alertDefinition.save();
            this.rules.alertId         = this.rules.alertDefinition.model.sysId;

            this.rules.version.setDefinition(this.rules.alertDefinition);
            this.rules.version        = await this.rules.version.save();
            this.rules.alertVersionId = this.rules.version.model.sysId;

            await this.rules.alertDefinition.flush();

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
            await this.rules.init(this.version, this.versionId, this.isLibrary, this.isCopy, comp.app);
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    showRulesInputStep(): boolean
    {
        return this.rules.version && this.rules.version.getDetails() instanceof Models.AlertDefinitionDetailsForUserProgram;
    }
}

export class AlertRulesData
{
    alertId: string;
    alertVersionId: string;

    alertDefinition: Alerts.AlertDefinitionExtended;
    app: AppContext;

    details: Alerts.AlertDefinitionDetailsExtended;
    version: AlertDefinitionVersionExtended;

    selectedTemplateId: string;

    initialized: Future<void>;

    async init(alertId: string,
               alertVersionId: string,
               isLibrary: boolean,
               copy: boolean,
               app: AppContext)
    {
        if (!this.initialized)
        {
            this.initialized = new Future<void>();
        }
        else
        {
            return this.initialized;
        }

        try
        {
            this.alertId            = alertId;
            this.alertVersionId     = alertVersionId;
            this.app                = app;
            this.selectedTemplateId = null;

            if (this.alertId && this.alertId !== "new")
            {
                this.alertDefinition = await app.domain.alertDefinitions.getExtendedById(this.alertId);
                let version: AlertDefinitionVersionExtended;
                if (this.alertVersionId)
                {
                    version = await app.domain.alertDefinitionVersions.getExtendedById(this.alertVersionId);
                }
                else
                {
                    version = await this.alertDefinition.getHead();
                }
                this.version = version.getNewVersion();

                if (copy)
                {
                    this.version.model.predecessor    = null;
                    this.alertDefinition.model.sysId  = null;
                    this.alertDefinition.model.active = false;
                    this.alertDefinition.model.title  = `Copy of ${this.alertDefinition.model.title}`;
                }
            }
            else
            {
                this.alertDefinition               = app.domain.alertDefinitions.allocateInstance();
                this.alertDefinition.model.active  = false; // Don't activate alerts by default.
                this.alertDefinition.model.purpose = isLibrary ? Models.AlertDefinitionPurpose.Library : Models.AlertDefinitionPurpose.Definition;

                this.version               = app.domain.alertDefinitionVersions.allocateInstance();
                this.version.model.details = new Models.AlertDefinitionDetailsForUserProgram();
            }

            await this.initDetails();
        }
        finally
        {
            this.initialized.resolve();
        }
    }

    async initFromTemplate(template: AlertDefinitionExtended)
    {
        this.version = this.app.domain.alertDefinitionVersions.allocateInstance();

        if (!template)
        {
            this.version.model.details = new Models.AlertDefinitionDetailsForUserProgram();
        }
        else
        {
            let release                = await template.getRelease();
            this.version.model.details = release.model.details;
        }

        await this.initDetails();
    }

    get userProgramDetails(): Alerts.AlertDefinitionDetailsForUserProgramExtended
    {
        return <Alerts.AlertDefinitionDetailsForUserProgramExtended>this.details;
    }

    async initDetails()
    {
        this.details = await this.version.getDetailsExtended();
    }
}

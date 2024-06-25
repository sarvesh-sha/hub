import {Component, Inject, Injector} from "@angular/core";
import {AppContext} from "app/app.service";
import {DynamicReport} from "app/reports/dynamic/dynamic-report";
import {ApiService} from "app/services/domain/api.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import * as Reports from "app/services/domain/report-definitions.service";
import {ReportConfigurationExtended, ReportSchedulingOptionsExtended, SchedulingType} from "app/services/domain/reporting.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {Future} from "framework/utils/concurrency";

@Component({
               templateUrl: "./report-wizard-dialog.component.html"
           })
export class ReportWizardDialogComponent extends WizardDialogComponent<ReportWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: ReportWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, ReportWizardDialogComponent);
    }
}

export class ReportWizardState extends WizardDialogState
{
    reportDefinition: Reports.ReportDefinitionExtended;
    version: ReportDefinitionVersionExtended;
    app: AppContext;
    configuration: ReportConfigurationExtended;
    details: Reports.ReportDefinitionDetailsExtended;
    schedulingOptions: ReportSchedulingOptionsExtended;
    report: DynamicReport;
    initialized: Future<void>;

    constructor(public reportId: string        = null,
                public reportVersionId: string = null,
                step: string                   = null)
    {
        super(!(reportId && reportVersionId), step);
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.push([
                                            "report",
                                            this.reportId
                                        ]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            // Set active appropriately
            let schedule                       = this.details.getSchedulingOptions();
            this.reportDefinition.model.active = schedule.schedulingType !== SchedulingType.OnDemand;

            let reportDefinition          = await this.reportDefinition.save();
            this.version.model.definition = reportDefinition.getIdentity();
            this.report.synchronize();
            await this.version.save();

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
            await this.init(this.reportId, this.reportVersionId, comp.app);
            return true;
        }
        catch (e)
        {
            return false;
        }
    }


    async init(reportID: string,
               reportVersionId: string,
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
            this.reportId        = reportID;
            this.reportVersionId = reportVersionId;
            this.app             = app;

            if (this.reportId && this.reportId !== "new")
            {
                this.reportDefinition = await app.domain.reportDefinitions.getExtendedById(this.reportId);

                let version: ReportDefinitionVersionExtended;
                if (this.reportVersionId)
                {
                    version = await app.domain.reportDefinitionVersions.getExtendedById(this.reportVersionId);
                }
                else
                {
                    version = await this.reportDefinition.getHead();
                }

                this.version = version.getNewVersion();
            }
            else
            {
                this.reportDefinition              = app.domain.reportDefinitions.allocateInstance();
                this.version                       = app.domain.reportDefinitionVersions.allocateInstance();
                this.version.model.details         = Models.ReportDefinitionDetails.newInstance({});
                this.reportDefinition.model.active = false;
            }

            this.initDetails();
        }
        finally
        {
            this.initialized.resolve();
        }
    }

    initDetails()
    {
        this.details           = this.version.getDetailsExtended();
        this.report            = DynamicReport.create(this.app.domain, this.reportDefinition, this.version);
        this.configuration     = this.report.getData();
        this.schedulingOptions = this.details.getSchedulingOptions();
    }
}

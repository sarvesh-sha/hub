import {Component, ComponentFactoryResolver, Injector, Input, ViewContainerRef} from "@angular/core";
import {BaseReportComponent} from "app/reports/base-report.component";
import {DynamicReportComponent} from "app/reports/dynamic/dynamic-report.component";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportConfig} from "app/services/domain/reporting.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector: "o3-report",
               template: ""
           })
export class ReportContainerComponent extends SharedSvc.BaseComponentWithRouter
{
    private token: string;
    private m_config: ReportConfig;

    @Input()
    public set config(config: ReportConfig)
    {
        this.m_config = config;
        if (this.m_config)
        {
            this.initializeReport();
        }
    }

    constructor(inj: Injector,
                private componentFactoryResolver: ComponentFactoryResolver,
                private viewContainerRef: ViewContainerRef)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        // retrieve params
        this.token = this.getPathParameter("token");

        if (this.token)
        {
            this.m_config = new ReportConfig();

            // retrieve report config
            let report               = await this.app.domain.reports.getExtendedById(this.token);
            this.m_config.definition = await report.getDefinition();
            this.m_config.version    = await report.getDefinitionVersion();
            this.m_config.rangeEnd   = report.model.rangeEnd;
            this.m_config.rangeStart = report.model.rangeStart;

            this.initializeReport();
        }
    }

    private initializeReport()
    {
        this.viewContainerRef.clear();

        let componentFactory                     = this.componentFactoryResolver.resolveComponentFactory(DynamicReportComponent);
        let reportComponent: BaseReportComponent = this.viewContainerRef.createComponent(componentFactory).instance;

        reportComponent.reportDefinition        = this.m_config.definition;
        reportComponent.reportDefinitionVersion = this.m_config.version;

        let details                = this.m_config.version.getDetailsExtended();
        let zone                   = details.getSchedulingOptions().model.schedule.zoneDesired;
        reportComponent.rangeEnd   = MomentHelper.parseOrNull(this.m_config.rangeEnd, zone);
        reportComponent.rangeStart = MomentHelper.parseOrNull(this.m_config.rangeStart, zone);
    }
}

import {Component} from "@angular/core";

import {BaseReportComponent, REPORT_METADATA} from "app/reports/base-report.component";
import {DynamicReport} from "app/reports/dynamic/dynamic-report";
import {ReportElementConfigurationBase, ReportElementModel, ReportSection, ReportSectionColumn} from "app/shared/reports/elements/report-element-base.component";

import {Future} from "framework/utils/concurrency";

@Component({
               selector   : "o3-dynamic-report",
               templateUrl: "./dynamic-report.component.html",
               providers  : [
                   {
                       provide    : REPORT_METADATA,
                       useExisting: DynamicReportComponent
                   }
               ]
           })
export class DynamicReportComponent extends BaseReportComponent
{
    private m_pendingElements           = new Set<ReportElementConfigurationBase>();
    private m_sections: ReportSection[] = [];
    private m_triggered                 = false;

    reportProgress = 0;

    getType()
    {
        return DynamicReportComponent;
    }

    public get sections(): ReportSection[]
    {
        return this.m_sections;
    }

    async init()
    {
        let report = DynamicReport.create(this.app.domain, this.reportDefinition, this.reportDefinitionVersion);

        report.startReport(this.app, this, this.rangeStart, this.rangeEnd);

        while (this.reportProgress !== 100)
        {
            await Future.delayed(1000);
            let res             = report.checkProgress();
            this.reportProgress = res.progress;
            this.m_sections     = res.results;
        }

        this.collectElements(this.m_pendingElements, this.m_sections || []);
    }

    renderComplete(config: ReportElementConfigurationBase)
    {
        this.m_pendingElements.delete(config);

        if (this.m_pendingElements.size == 0 && !this.m_triggered)
        {
            this.m_triggered = true;
            this.triggerReport();
        }
    }

    asSectionColumn(column: ReportSectionColumn): ReportSectionColumn
    {
        if (column instanceof ReportSectionColumn) return column;
        return null;
    }

    asReportSection(element: ReportSection | ReportElementModel): ReportSection
    {
        if (element instanceof ReportSection) return element;
        return null;
    }

    asElementModel(element: ReportSection | ReportElementModel): ReportElementModel
    {
        if (element instanceof ReportElementModel) return element;
        return null;
    }

    private collectElements(elements: Set<ReportElementConfigurationBase>,
                            sections: (ReportSection | ReportSectionColumn | ReportElementModel)[]): void
    {
        for (let section of sections || [])
        {
            if (section instanceof ReportElementModel)
            {
                elements.add(section.configuration);
            }
            else if (section instanceof ReportSection)
            {
                this.collectElements(elements, section.columns);
            }
            else if (section instanceof ReportSectionColumn)
            {
                this.collectElements(elements, section.models);
            }
        }
    }
}

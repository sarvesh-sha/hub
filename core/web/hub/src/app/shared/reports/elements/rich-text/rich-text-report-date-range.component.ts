import {Component, Inject} from "@angular/core";
import {REPORT_METADATA, ReportMetadata} from "app/reports/base-report.component";
import {ComponentBlotDefinition} from "framework/ui/markdown/formats/basic-component";
import {RichTextEditorComponent} from "framework/ui/markdown/rich-text-editor.component";

@Component({
               template: `<span class="custom-blot">{{metadata.rangeStart|o3ShortDate}} to {{metadata.rangeEnd|o3ShortDate}}</span>`
           })
export class RichTextReportDateRangeComponent
{
    constructor(@Inject(REPORT_METADATA) public metadata: ReportMetadata) {}
}

@Component({
               template: `
                   <span class="custom-blot">
                        {{metadata.rangeStart|o3ShortDate}} at {{metadata.rangeStart|o3ShortTime}} to {{metadata.rangeEnd|o3ShortDate}} at {{metadata.rangeEnd|o3ShortTime}}
                   </span>`
           })
export class RichTextReportDateTimeRangeComponent
{
    constructor(@Inject(REPORT_METADATA) public metadata: ReportMetadata) {}
}

export const ReportDateRangeBlot: ComponentBlotDefinition<RichTextReportDateRangeComponent, void> = {
    blotName     : "reportDateRange",
    componentType: RichTextReportDateRangeComponent
};

export const ReportDateTimeRangeBlot: ComponentBlotDefinition<RichTextReportDateTimeRangeComponent, void> = {
    blotName     : "reportDateTimeRange",
    componentType: RichTextReportDateTimeRangeComponent
};

RichTextEditorComponent.RegisterComponentBlot(ReportDateRangeBlot);
RichTextEditorComponent.RegisterComponentBlot(ReportDateTimeRangeBlot);

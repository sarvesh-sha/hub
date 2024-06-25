import {Component, ElementRef, Input, ViewChild} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ReportElementModel} from "app/shared/reports/elements/report-element-base.component";
import {ReportElementContainerComponent} from "app/shared/reports/elements/report-element-container.component";

@Component({
               selector   : "o3-custom-report-builder-field",
               templateUrl: "./custom-report-builder-field.component.html",
               styleUrls  : ["./custom-report-builder-field.component.scss"]
           })
export class CustomReportBuilderFieldComponent extends BaseApplicationComponent
{
    @Input() public element: Models.CustomReportElement;
    @Input() public model: ReportElementModel;
    @Input() public range: Models.RangeSelection;

    private m_elementRef: ElementRef<HTMLElement>;
    @ViewChild(ReportElementContainerComponent, {read: ElementRef}) set elementRef(elementRef: ElementRef)
    {
        if (this.m_elementRef !== elementRef)
        {
            this.m_elementRef = elementRef;
            this.detectChanges();
        }
    }

    get showTruncationFade(): boolean
    {
        return this.m_elementRef?.nativeElement.clientHeight > 250;
    }
}

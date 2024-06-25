import {Component, Inject, OnInit} from "@angular/core";
import {ReportSchedulingOptionsExtended} from "app/services/domain/reporting.service";
import * as Models from "app/services/proxy/model/models";

import {ChartTimeUtilities, TimeRange, TimeRanges, TimeRangeValues} from "framework/ui/charting/core/time";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./report-view-dialog.component.html",
               styleUrls  : ["./report-view-dialog.component.scss"]
           })
export class ReportViewDialogComponent implements OnInit
{
    rangeEnd: Date;

    timeRange: TimeRange;
    timeRangeOptions: ControlOption<TimeRange>[] = [];

    values: TimeRangeValues;

    get customSubmitText(): string
    {
        return this.data.customSubmitText;
    }

    get resolvedRangeText(): string
    {
        if (!this.values) return "";
        let start = this.values.range.minAsMoment.format("L LT");
        let end   = this.values.range.maxAsMoment.format("L LT");
        return `${start} - ${end}`;
    }

    constructor(public dialogRef: OverlayDialogRef<TimeRangeValues>,
                @Inject(OVERLAY_DATA) private data: DialogConfig)
    {
    }

    public static open(comp: BaseComponent,
                       schedulingOptions: ReportSchedulingOptionsExtended,
                       submitText?: string,
                       width?: any): Promise<TimeRangeValues>
    {
        let cfg              = new DialogConfig();
        cfg.customSubmitText = submitText;
        if (schedulingOptions)
        {
            cfg.defaultTimeRange = schedulingOptions.model.range;
        }

        return OverlayComponent.open(comp, ReportViewDialogComponent, {
            config: OverlayConfig.newInstance({width: width}),
            data  : cfg
        });
    }

    ngOnInit()
    {
        this.timeRangeOptions = ChartTimeUtilities.getTimeRangeControlOptions();
        this.rangeEnd         = new Date();
        this.rangeEnd.setSeconds(0, 0);

        if (this.data.defaultTimeRange)
        {
            this.timeRange = TimeRanges.resolve(this.data.defaultTimeRange, false);
        }
        else
        {
            this.timeRange = TimeRanges.Last24Hours;
        }

        this.updateRange();
    }

    updateRange()
    {
        this.values = this.timeRange.getTimeRangeValues(undefined, this.rangeEnd);
    }
}

class DialogConfig
{
    customSubmitText: string;

    defaultTimeRange: Models.TimeRangeId;
}

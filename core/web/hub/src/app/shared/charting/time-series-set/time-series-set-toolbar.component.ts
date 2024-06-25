import {Component, ContentChildren, ElementRef, EventEmitter, Injector, Input, Output, QueryList, ViewChild} from "@angular/core";

import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector: "o3-time-series-set-toolbar-action",
               template: "<ng-template></ng-template>"
           })
export class TimeSeriesSetToolbarActionComponent
{
    @Input() public label: string;

    @Input() public icon: string;

    @Input() public disabled: boolean;

    @Input() public tooltip: string;

    @Output() click = new EventEmitter<any>();

    constructor() { }

    onClick(event: Event)
    {
        this.click.emit(event);
    }
}

@Component({
               selector   : "o3-time-series-set-toolbar",
               templateUrl: "./time-series-set-toolbar.component.html",
               styleUrls  : ["./time-series-set-toolbar.component.scss"]
           })
export class TimeSeriesSetToolbarComponent extends SharedSvc.BaseApplicationComponent
{
    // Time range input to sync across all charts
    @Input() range: Models.RangeSelection;
    @Output() rangeChange = new EventEmitter<Models.RangeSelection>();

    @Input() staticRange: boolean = false;
    @Input() staticRangeTooltip: string;

    // Time series input to control which charts to display
    @Input() chartsExt: TimeSeriesChartConfigurationExtended[];
    @Output() chartsExtChange = new EventEmitter<TimeSeriesChartConfigurationExtended[]>();

    @Input() embedded: boolean;
    @Input() canDeleteAllCharts: boolean = false;

    actions: TimeSeriesSetToolbarActionComponent[] = [];

    @ViewChild("test_addChartButton", {read: ElementRef}) test_addChartButton: ElementRef<HTMLElement>;

    get elementRef(): ElementRef
    {
        return this.m_elem;
    }

    constructor(inj: Injector,
                private m_elem: ElementRef)
    {
        super(inj);
    }

    @ContentChildren(TimeSeriesSetToolbarActionComponent)
    set actionTemplates(actionTemplates: QueryList<TimeSeriesSetToolbarActionComponent>)
    {
        this.actions = [];
        if (actionTemplates) this.actions = actionTemplates.toArray();
    }

    get hasVisualizations(): boolean
    {
        return this.chartsExt && this.chartsExt.length > 0;
    }

    rangeChanged(value: Models.RangeSelection): void
    {
        this.range = value;
        this.rangeChange.emit(value);
    }

    addHandler(): void
    {
        // Add an empty visualization at the bottom of the stack
        let chartExt = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);
        this.chartsExt.push(chartExt);
        this.chartsExtChange.emit(this.chartsExt);
    }

    async deleteHandler(override: boolean): Promise<void>
    {
        if (override ||
            (this.hasVisualizations && await this.confirmOperation("Click Yes to delete all visualizations, click No to cancel and return.")))
        {
            // Reset list
            this.chartsExt = [];
            this.chartsExtChange.emit(this.chartsExt);
        }
    }
}

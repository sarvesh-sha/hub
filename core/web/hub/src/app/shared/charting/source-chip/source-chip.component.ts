import {Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";
import {TooltipPosition} from "@angular/material/tooltip";

import {InteractableSource, InteractableSourcesChart} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import {TimeDurationExtended} from "app/shared/forms/time-range/time-duration-extended";
import {Lookup} from "framework/services/utils.service";

import {ChartColorUtilities, ColorGradientContinuous, ColorGradientStop} from "framework/ui/charting/core/colors";
import {ChartPointSource, isVisible, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";

import {Subscription} from "rxjs";


@Component({
               selector   : "o3-source-chip[chart][sourceId]",
               templateUrl: "./source-chip.component.html",
               styleUrls  : ["./source-chip.component.scss"]
           })
export class SourceChipComponent extends SharedSvc.BaseApplicationComponent
{
    public static readonly sourceIndicatorDiameterDecimal = 1 / Math.sqrt(2);
    public readonly maxChipWidth: number                  = 300;

    private updateSub: Subscription;
    private stateSub: Subscription;

    private m_chart: InteractableSourcesChart;
    @Input() set chart(chart: InteractableSourcesChart)
    {
        this.m_chart = chart;
        this.configUpdated();

        this.updateSub?.unsubscribe();
        this.updateSub = this.subscribeToObservable(chart.chartUpdated, () => this.configUpdated());

        this.stateSub?.unsubscribe();
        this.stateSub = this.subscribeToObservable(chart.sourceStatesUpdated, () => this.stateChanged());
    }

    source: InteractableSource;

    private m_sourceState: VisualizationDataSourceState;
    get sourceState(): VisualizationDataSourceState
    {
        if (this.m_sourceState == undefined) this.stateChanged();
        return this.m_sourceState;
    }

    private m_sourceId: string;
    @Input() set sourceId(id: string)
    {
        if (this.m_sourceId != id)
        {
            this.m_sourceId = id;
            this.configUpdated();
        }
    }

    @Input() actions: SourceAction[] = [];
    @Input() deletable: boolean      = true;
    @Input() catchClicks: boolean    = false;

    @Input() disableRipple: boolean = false;
    @Input() embedded: boolean      = false;
    @Input() listable: boolean      = false;
    @Input() printable: boolean     = false;
    @Input() showTooltips: boolean  = true;

    private m_primaryTextOverride: string;
    @Input() set primaryTextOverride(text: string)
    {
        this.m_primaryTextOverride = text;
        this.updatePrimaryText();
    }

    private m_secondaryTextOverride: string;
    @Input() set secondaryTextOverride(text: string)
    {
        this.m_secondaryTextOverride = text;
        this.updateSecondaryText();
    }

    @Input() hideSecondaryText: boolean = false;

    //--//

    @Output() deleteTriggered = new EventEmitter<void>();
    @Output() deleteCancelled = new EventEmitter<void>();

    get isOff(): boolean
    {
        let state = this.sourceState;
        return state && !isVisible(state);
    }

    get isDeleting(): boolean
    {
        let state = this.sourceState;
        return state === VisualizationDataSourceState.Deleted;
    }

    primaryText: string;
    secondaryText: string;
    sourceTooltip: string;
    chipClasses: Lookup<any>;
    disableIndicatorTooltip: string;

    private m_timeOffsetFriendly: string = "";

    private m_notVisibleIndicator: string;
    private m_visibleIndicator: string;

    get chipIndicatorBackgroundStyle(): string
    {
        if (!this.source) return "grey";

        return this.isOff ? this.m_notVisibleIndicator : this.m_visibleIndicator;
    }

    get element(): ElementRef<HTMLElement>
    {
        return this.m_element;
    }

    @ViewChild("test_delete", {read: ElementRef}) test_delete: ElementRef;
    @ViewChild("test_disable", {read: ElementRef}) test_disable: ElementRef;
    @ViewChildren("test_action", {read: ElementRef}) test_actions: QueryList<ElementRef>;

    constructor(inj: Injector,
                private m_element: ElementRef<HTMLElement>)
    {
        super(inj);
    }

    emitClick()
    {
        // mat-chip catches click events by default. Create new click if catchClicks isn't set
        if (!this.catchClicks) this.m_element.nativeElement.click();
    }

    private stateChanged()
    {
        if (!this.m_sourceId || !this.m_chart || !this.source) return;

        let state = this.m_chart.getSourceState(this.m_sourceId);
        if (state != this.m_sourceState)
        {
            this.m_sourceState = state;

            this.chipClasses = {
                "deleted"      : this.isDeleting,
                "target-source": this.m_sourceState === VisualizationDataSourceState.Target
            };

            this.updateDisableIndicatorTooltip();

            this.detectChanges();
        }
    }

    private updateDisableIndicatorTooltip()
    {
        let visible         = isVisible(this.m_sourceState);
        let viewableSources = this.m_chart.getSources(this.source.panel, false);
        let allSourcesOn    = viewableSources.length === 1 && viewableSources[0].getChartData() === this.source.getChartData() || viewableSources.length === 0;

        this.disableIndicatorTooltip = ChartPointSource.generateTooltipEntryText("Click", "toggle " + (visible ? "off" : "on")) +
                                       ChartPointSource.generateTooltipEntryText("Double Click", allSourcesOn ? "turn all sources on" : "turn all sources but this one off", false);
    }

    private configUpdated()
    {
        if (!this.m_sourceId || !this.m_chart) return;

        this.source = this.m_chart.getSource(this.m_sourceId);
        if (!this.source) return;

        this.updateTimeOffsetString();
        this.updatePrimaryText();
        this.updateSecondaryText();
        this.updateTooltip();
        this.updateChipIndicatorBackgroundStyles();

        this.stateChanged();
    }

    private updateTimeOffsetString()
    {
        this.m_timeOffsetFriendly = this.source && TimeDurationExtended.getTimeOffsetString(this.source.timeOffset) || "";
    }

    private updatePrimaryText()
    {
        this.primaryText = this.m_primaryTextOverride || (this.source ? `${this.source.name} ${this.m_timeOffsetFriendly}` : "");
    }

    private updateSecondaryText()
    {
        this.secondaryText = this.m_secondaryTextOverride || this.source?.description || "";
    }

    private updateTooltip()
    {
        this.sourceTooltip = "";
        if (!this.source) return;

        let tooltip          = this.source.name;
        let timeOffsetString = TimeDurationExtended.getTimeOffsetString(this.source.timeOffset);
        if (timeOffsetString) tooltip += " " + timeOffsetString;

        let description = this.source.description;
        if (description) tooltip += "\n" + this.source.description;

        this.sourceTooltip = tooltip;
    }

    private updateChipIndicatorBackgroundStyles()
    {
        if (this.source.colorStops)
        {
            let colors    = this.source.colorStops.map((color,
                                                        idx) => new ColorGradientStop(idx, color.color));
            let colorsOn  = ColorGradientContinuous.generateGradientColors(colors, false);
            let colorsOff = colorsOn.map((color) => ChartColorUtilities.safeChroma(color)
                                                                       .alpha(0.5)
                                                                       .hex("rgba"));

            let edgeBuffer   = 100 * (1 - SourceChipComponent.sourceIndicatorDiameterDecimal) / 2;
            let lastColorIdx = colorsOn.length - 1;
            let step         = (100 - edgeBuffer * 2) / lastColorIdx;

            const segmentsToBackgroundMapper = (color: string,
                                                index: number) =>
            {
                let percent = index * step + edgeBuffer;
                if (lastColorIdx === index) percent -= 0.01;
                return `${color} ${percent}%`;
            };

            colorsOn                   = colorsOn.map(segmentsToBackgroundMapper);
            colorsOff                  = colorsOff.map(segmentsToBackgroundMapper);
            this.m_visibleIndicator    = `linear-gradient(45deg, ${colorsOn.join(", ")})`;
            this.m_notVisibleIndicator = `linear-gradient(45deg, ${colorsOff.join(", ")})`;
        }
        else
        {
            this.m_visibleIndicator    = this.source.color;
            this.m_notVisibleIndicator = ChartColorUtilities.safeChroma(this.m_visibleIndicator)
                                                            .alpha(0.5)
                                                            .hex("rgba");
        }
    }

    toggleTarget(fromMouseover: boolean,
                 on?: boolean): void
    {
        if (this.isDeleting) return;
        if (fromMouseover)
        {
            let state = this.sourceState;
            if (!isVisible(state)) return;

            let isTarget = state === VisualizationDataSourceState.Target;
            if (on === isTarget) return;
        }

        this.m_chart.toggleTarget(this.m_sourceId, fromMouseover);
    }

    toggleEnabled(clickEvent: MouseEvent)
    {
        clickEvent.stopPropagation();
        if (this.isDeleting) return;

        this.m_chart.toggleEnabled(this.m_sourceId);
    }

    toggleOnOffStrong(clickEvent: MouseEvent)
    {
        clickEvent.stopPropagation();
        if (this.isDeleting) return;

        this.m_chart.multiToggleEnabled(this.m_sourceId);
    }

    triggerDelete(event: MouseEvent)
    {
        event.stopPropagation();
        this.deleteTriggered.emit();
    }

    cancelDelete(event: MouseEvent)
    {
        event.stopPropagation();
        this.deleteCancelled.emit();
    }
}

export class SourceAction
{
    get tooltipPosition(): TooltipPosition
    {
        return this.tooltip?.positioning || "below";
    }

    constructor(public icon: string,
                public callback: (source: InteractableSource) => void,
                public tooltip: TooltipConfiguration = TooltipConfiguration.defaultEditSource(),
                private readonly isEnabledCallback?: (source: InteractableSource) => boolean)
    {}

    optio3TestId(sourceId: string): string
    {
        return `source-action-${sourceId}-${this.icon}`;
    }

    isEnabled(source: InteractableSource): boolean
    {
        return source && (!this.isEnabledCallback || this.isEnabledCallback(source));
    }
}

export class TooltipConfiguration
{
    constructor(public content: string   = null,
                public showDelay: number = 0,
                public hideDelay: number = 0,
                public positioning?: TooltipPosition)
    {}

    static defaultEditSource(): TooltipConfiguration
    {
        return new TooltipConfiguration("Configure source", 500);
    }
}

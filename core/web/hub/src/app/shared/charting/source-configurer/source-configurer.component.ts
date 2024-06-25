import {Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {TimeSeriesSourceConfigurationExtended, ToggleableNumericRangeExtended} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {AddRelatedSourceComponent, AddTimeSeriesSourceEvent} from "app/shared/charting/add-related-source/add-related-source.component";
import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {ColorPickerComponent} from "app/shared/colors/color-picker.component";
import {TimeDurationExtended} from "app/shared/forms/time-range/time-duration-extended";

import {UtilsService} from "framework/services/utils.service";
import {PaletteId} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";


@Component({
               selector   : "o3-source-configurer[source]",
               templateUrl: "./source-configurer.component.html",
               styleUrls  : ["./source-configurer.component.scss"]
           })
export class SourceConfigurerComponent extends SharedSvc.BaseApplicationComponent implements OverlayController
{
    private static readonly noChangeBeforeUpdateDelay: number = 200;

    @Input() sources: TimeSeriesSourceConfigurationExtended[];

    private originalValues: Models.TimeSeriesSourceConfiguration;
    private m_source: TimeSeriesSourceConfigurationExtended;
    @Input() set source(source: TimeSeriesSourceConfigurationExtended)
    {
        if (source && this.m_source !== source)
        {
            this.cancelled();
            this.bindSource(source);
        }
    }

    get source(): TimeSeriesSourceConfigurationExtended
    {
        return this.m_source;
    }

    get typedPalette(): PaletteId
    {
        return <any>this.m_source?.ownerPanel?.owner.model.palette;
    }

    set palette(palette: string)
    {
        let ownerPanel = this.m_source?.ownerPanel;
        if (ownerPanel)
        {
            ownerPanel.owner.model.palette = palette;
        }
    }

    @Input() primaryButtonText: string = "Save";

    private m_disableAddSource: boolean = false;
    @Input() set disableAddSource(disable: boolean)
    {
        this.m_disableAddSource = disable;
        this.updateActions();
    }

    pristine: boolean = true;

    movingAverageKind: MovingAverage = "NONE";
    movingAverageValue: number;

    get movingAverageValueEnabled(): boolean
    {
        switch (this.movingAverageKind)
        {
            case "NONE":
            case "MEAN":
                return false;

            default:
                return true;
        }
    }

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;
    @ViewChild(AddRelatedSourceComponent, {static: true}) addRelated: AddRelatedSourceComponent;

    @ViewChild("test_color") test_color: ColorPickerComponent;

    private m_shiftChanged = false;
    private m_updater: number;
    @Output() updated      = new EventEmitter<boolean>();
    @Output() addSource    = new EventEmitter<AddTimeSeriesSourceEvent>();

    tabActions: TabActionDirective[] = [];

    public readonly movingAverageOptions: ControlOption<MovingAverage>[] = [
        new ControlOption("NONE", "Don't Show"),
        new ControlOption("MEAN", "Mean Value"),
        new ControlOption("MINUTES", "Moving Average in Minutes"),
        new ControlOption("HOURS", "Moving Average in Hours"),
        new ControlOption("DAYS", "Moving Average in Days")
    ];

    public readonly highlightOptions: ControlOption<Models.TimeSeriesDecimationDisplay>[] = [
        new ControlOption(Models.TimeSeriesDecimationDisplay.Minimum, "Minimum"),
        new ControlOption(Models.TimeSeriesDecimationDisplay.Average, "Average"),
        new ControlOption(Models.TimeSeriesDecimationDisplay.Maximum, "Maximum")
    ];

    config = ColorPickerConfigurationComponent.colorOverlayConfig(true);

    constructor(inj: Injector)
    {
        super(inj);

        this.config.showCloseButton = false;

        this.updateActions();
    }

    // -- //

    private async bindSource(source: TimeSeriesSourceConfigurationExtended)
    {
        source.onDelete.then(() =>
                             {
                                 if (source === this.m_source) this.closeOverlay();
                             });

        if (source.valid)
        {
            this.m_source = null;
            await source.dataSourceReady;
        }
        this.m_source = source;
        this.reset();
    }

    // -- //

    openOverlay()
    {
        this.overlay.openOverlay();
    }

    closeOverlay()
    {
        if (this.overlay.isOpen())
        {
            this.cancelled();
            this.overlay.closeOverlay();
        }
    }

    toggleOverlay(open?: boolean)
    {
        this.overlay.toggleOverlay(open);
    }

    isOpen(): boolean
    {
        return this.overlay.isOpen();
    }

    // -- //

    movingAverageChanged()
    {
        switch (this.movingAverageKind)
        {
            case "NONE":
                this.source.model.showMovingAverage     = 0;
                this.source.model.onlyShowMovingAverage = false;
                break;

            case "MEAN":
                this.source.model.showMovingAverage     = 1E10; // Anything larger than a year will do.
                this.source.model.onlyShowMovingAverage = false;
                break;

            case "MINUTES":
                this.source.model.showMovingAverage = this.movingAverageValue * 60;
                break;

            case "HOURS":
                this.source.model.showMovingAverage = this.movingAverageValue * 60 * 60;
                break;

            case "DAYS":
                this.source.model.showMovingAverage = this.movingAverageValue * 24 * 60 * 60;
                break;
        }

        this.update();
    }

    timeShifted()
    {
        this.m_shiftChanged = true;
        this.update();
    }

    // -- //

    update()
    {
        this.pristine = UtilsService.compareJson(this.originalValues, this.m_source.model);

        this.cancelUpdater();
        this.m_updater = setTimeout(() =>
                                    {
                                        this.updated.emit(this.m_shiftChanged);
                                        this.m_shiftChanged = false;
                                        this.m_updater      = null;
                                    }, SourceConfigurerComponent.noChangeBeforeUpdateDelay);
    }

    submitted()
    {
        this.cancelUpdater();
        ToggleableNumericRangeExtended.cleanModel(this.m_source.model.range);
        this.reset();

        this.updated.emit();
    }

    cancelled()
    {
        this.cancelUpdater();

        if (!this.pristine)
        {
            let model      = this.m_source.model;
            let editOffset = model.timeOffset;

            model.color             = this.originalValues.color;
            model.showMovingAverage = this.originalValues.showMovingAverage;
            model.range             = this.originalValues.range;
            model.timeOffset        = this.originalValues.timeOffset;
            model.decimationDisplay = this.originalValues.decimationDisplay;
            model.showDecimation    = this.originalValues.showDecimation;
            this.reset();

            this.updated.emit(UtilsService.compareJson(editOffset, model.timeOffset));
        }
    }

    // -- //

    private reset()
    {
        let showMovingAverage = this.m_source.model.showMovingAverage;
        if (isNaN(showMovingAverage) || showMovingAverage <= 0)
        {
            this.movingAverageKind  = "NONE";
            this.movingAverageValue = 20;
        }
        else if (showMovingAverage > 1E9)
        {
            this.movingAverageKind  = "MEAN";
            this.movingAverageValue = 20;
        }
        else if (showMovingAverage > 2 * 24 * 60 * 60)
        {
            this.movingAverageKind  = "DAYS";
            this.movingAverageValue = showMovingAverage / (24 * 60 * 60);
        }
        else if (showMovingAverage > 2 * 60 * 60)
        {
            this.movingAverageKind  = "HOURS";
            this.movingAverageValue = showMovingAverage / (60 * 60);
        }
        else
        {
            this.movingAverageKind  = "MINUTES";
            this.movingAverageValue = showMovingAverage / 60;
        }

        if (!this.m_source.model.timeOffset) this.m_source.model.timeOffset = TimeDurationExtended.newModel();

        this.originalValues = Models.TimeSeriesSourceConfiguration.deepClone(this.m_source.model);
        this.m_shiftChanged = false;
        this.pristine       = true;
    }

    private updateActions()
    {
        let addRelatedSource      = new TabActionDirective();
        addRelatedSource.label    = "Add Related Source";
        addRelatedSource.callback = () => this.addRelated.toggleOverlay();
        addRelatedSource.disabled = this.m_disableAddSource;
        this.tabActions           = [addRelatedSource];
    }

    private cancelUpdater()
    {
        if (this.m_updater)
        {
            clearTimeout(this.m_updater);
            this.m_updater = null;
        }
    }
}

type MovingAverage = "NONE" | "MEAN" | "MINUTES" | "HOURS" | "DAYS";

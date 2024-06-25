import {Component, EventEmitter, Input, Output, SimpleChanges, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {InteractiveTreeComponent} from "app/shared/charting/interactive-tree/interactive-tree.component";

import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {SyncDebouncer} from "framework/utils/debouncers";

@Component({
               selector   : "o3-hierarchical-visualization-configuration",
               templateUrl: "./hierarchical-visualization-configuration.component.html",
               styleUrls  : ["./hierarchical-visualization-configuration.component.scss"]
           })
export class HierarchicalVisualizationConfigurationComponent extends SharedSvc.BaseApplicationComponent
{
    get minRowHeight(): number
    {
        return InteractiveTreeComponent.MIN_ROW_HEIGHT;
    }

    inputChangeDebouncer = new SyncDebouncer<void>(750, () => this.write());

    @Input() onlyEmitWhenValid: boolean   = false;
    @Input() emitNullWhenInvalid: boolean = true;

    private m_model: Models.HierarchicalVisualizationConfiguration;
    @Input() set model(model: Models.HierarchicalVisualizationConfiguration)
    {
        this.inputChangeDebouncer.forceProcessing();
        this.m_model = model;
    }

    @Output() modelChange = new EventEmitter<Models.HierarchicalVisualizationConfiguration>();

    @ViewChild("test_type") test_type: SelectComponent<Models.HierarchicalVisualizationType>;
    @ViewChild("test_rowSizing") test_rowSizing: SelectComponent<Models.HierarchicalVisualizationSizing>;

    get isHeatmap(): boolean
    {
        return this.type === Models.HierarchicalVisualizationType.HEATMAP;
    }

    get axisScalingPlaceholder(): string
    {
        let prefix = this.isHeatmap ? "Color" : "Axis";
        return prefix + " Scaling";
    }

    get fixedSizing(): boolean
    {
        return this.sizing === Models.HierarchicalVisualizationSizing.FIXED;
    }

    get fixedAxisSizing(): boolean
    {
        return this.axisSizing === Models.HierarchicalVisualizationAxisSizing.FIXED;
    }

    type: Models.HierarchicalVisualizationType             = null;
    sizing: Models.HierarchicalVisualizationSizing         = null;
    size: number                                           = null;
    axisSizing: Models.HierarchicalVisualizationAxisSizing = null;
    axisRange: Models.NumericRange                         = null;

    types: ControlOption<Models.HierarchicalVisualizationType>[]             = [
        new ControlOption(Models.HierarchicalVisualizationType.LINE, "Line"),
        new ControlOption(Models.HierarchicalVisualizationType.HEATMAP, "Heatmap")
    ];
    sizings: ControlOption<Models.HierarchicalVisualizationSizing>[]         = [
        new ControlOption(Models.HierarchicalVisualizationSizing.FIT, "Automatic"),
        new ControlOption(Models.HierarchicalVisualizationSizing.FIXED, "Fixed Size")
    ];
    axisSizings: ControlOption<Models.HierarchicalVisualizationAxisSizing>[] = [
        new ControlOption(Models.HierarchicalVisualizationAxisSizing.INDIVIDUAL, "Individual Range"),
        new ControlOption(Models.HierarchicalVisualizationAxisSizing.SHARED, "Shared Range"),
        new ControlOption(Models.HierarchicalVisualizationAxisSizing.FIXED, "Fixed Range")
    ];

    ngOnInit()
    {
        super.ngOnInit();
        this.read();
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.read();
    }

    ensureAxisSizing()
    {
        if (this.isHeatmap)
        {
            this.axisSizing = Models.HierarchicalVisualizationAxisSizing.SHARED;
        }
    }

    updateSize(val: number)
    {
        this.size = Math.max(this.minRowHeight, Math.round(val));
        this.inputChangeDebouncer.invoke();
    }

    write()
    {
        let model = new Models.HierarchicalVisualizationConfiguration();

        if (this.type) model.type = this.type;
        if (this.sizing) model.sizing = this.sizing;
        if (this.size) model.size = this.size;
        if (this.axisSizing) model.axisSizing = this.axisSizing;
        if (this.axisRange) model.axisRange = this.axisRange;

        this.m_model = model;

        // only mess with emit value if these are set
        if (this.onlyEmitWhenValid || this.emitNullWhenInvalid)
        {
            if (!this.isValid())
            {
                // invalid: figure out how this impacts what is emitted

                if (this.onlyEmitWhenValid) return;

                if (this.emitNullWhenInvalid) model = null;
            }
        }

        this.modelChange.emit(model);
    }

    private read()
    {
        if (this.m_model)
        {
            if (this.m_model.type) this.type = this.m_model.type;
            if (this.m_model.sizing) this.sizing = this.m_model.sizing;
            if (this.m_model.size) this.size = this.m_model.size;
            if (this.m_model.axisRange) this.axisRange = this.m_model.axisRange;
            if (this.m_model.axisSizing)
            {
                this.axisSizing = this.m_model.axisSizing;
                this.ensureAxisSizing();
            }
        }
    }

    private isValid(): boolean
    {
        if (!this.type || !this.sizing) return false;
        if (this.fixedSizing && !this.size) return false;
        return true;
    }
}

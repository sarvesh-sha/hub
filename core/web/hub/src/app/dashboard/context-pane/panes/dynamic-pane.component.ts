import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {TimeRangeType} from "framework/ui/charting/core/time";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector       : "o3-dynamic-pane",
               styleUrls      : ["./dynamic-pane.component.scss"],
               templateUrl    : "./dynamic-pane.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DynamicPaneComponent
{
    public options: ControlOption<Models.Pane>[];
    private m_models: Models.Pane[];
    public get models(): Models.Pane[]
    {
        return this.m_models;
    }

    @Input()
    public set models(models: Models.Pane[])
    {
        this.m_models = models || [];
        if (this.m_models.length)
        {
            this.model = this.m_models[0];
        }

        this.options = this.m_models.map((m) => new ControlOption(m, m.title));
    }

    @Input() loading: boolean;

    @Input()
    public range: Models.RangeSelection = RangeSelectionExtended.newModel(Models.TimeRangeId.Last24Hours);

    public ranges = TimeRangeType.Relative;

    private m_rangeId: Models.TimeRangeId = Models.TimeRangeId.Last24Hours;
    public get rangeId(): Models.TimeRangeId
    {
        return this.m_rangeId;
    }

    public set rangeId(range: Models.TimeRangeId)
    {
        this.m_rangeId   = range;
        this.range       = Models.RangeSelection.newInstance({range: this.m_rangeId});
        this.model.cards = [...this.model.cards];
    }

    private m_selectedModel: Models.Pane;

    public get model(): Models.Pane
    {
        return this.m_selectedModel;
    }

    public set model(model: Models.Pane)
    {
        this.m_selectedModel = model;
    }
}

import {Component} from "@angular/core";

import {WidgetLayoutConfig, WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-grid-step",
               templateUrl: "./widget-editor-wizard-grid-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-grid-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardGridStepComponent)]
           })
export class WidgetEditorWizardGridStepComponent extends WizardStep<WidgetEditorWizardState>
{
    readonly maxDimensionLength: number = 100;

    minRows: number = 1;
    minCols: number = 1;

    widgetLayout: WidgetManipulator;

    private m_rowState: GridDimensionState;
    private m_colState: GridDimensionState;

    private m_densityDecreaseScalar: number;
    private readonly m_densityIncreaseScalar: number = 2;

    get canDecreaseDensity(): boolean
    {
        return !isNaN(this.m_densityDecreaseScalar);
    }

    get canIncreaseDensity(): boolean
    {
        return this.numRows * this.m_densityIncreaseScalar <= this.maxDimensionLength && this.numCols * this.m_densityIncreaseScalar <= this.maxDimensionLength;
    }

    private get typedWidget(): Models.GroupingWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.GroupingWidgetConfiguration);
    }

    get numRows(): number
    {
        return this.typedWidget?.numRows;
    }

    set numRows(targetRows: number)
    {
        this.updateDimension(targetRows, this.m_rowState,
                             () => this.numRows, (cols) => this.typedWidget.numRows = cols,
                             (idx) => this.insertRow(idx), (idx) => this.removeRow(idx));
    }

    get numCols(): number
    {
        return this.typedWidget?.numCols;
    }

    set numCols(targetCols: number)
    {
        this.updateDimension(targetCols, this.m_colState,
                             () => this.numCols, (cols) => this.typedWidget.numCols = cols,
                             (idx) => this.insertCol(idx), (idx) => this.removeCol(idx));
    }

    get widgets(): Models.WidgetComposition[]
    {
        return this.typedWidget?.widgets || [];
    }

    protected afterLayoutChange()
    {
        super.afterLayoutChange();

        this.widgetLayout?.refresh();
    }

    private updateDimension(targetLength: number,
                            dimensionState: GridDimensionState,
                            getter: () => number,
                            setter: (length: number) => void,
                            insert: (idx: number) => void,
                            remove: (idx: number) => void)
    {
        targetLength = UtilsService.clamp(1, this.maxDimensionLength, targetLength || getter());
        while (targetLength !== getter())
        {
            if (targetLength > getter())
            {
                insert(dimensionState.getNextAdd());
                setter(getter() + 1);
            }
            else
            {
                let nextRemoval = dimensionState.getNextRemoval();
                if (isNaN(nextRemoval)) break;

                remove(nextRemoval);
                setter(getter() - 1);
            }
        }

        this.rebuildLayout();
    }

    public getLabel(): string
    {
        return "Grid";
    }

    public isEnabled(): boolean
    {
        return this.data.editor.allowWidgetTypes(Models.GroupingWidgetConfiguration);
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onStepSelected()
    {
        if (!this.m_rowState || !this.m_colState)
        {
            this.rebuildDimensionStates();
        }

        this.widgetLayout.refresh();
    }

    private rebuildDimensionStates()
    {
        let widgetOutlines     = this.widgets.map((widget) => widget.outline);
        let gridRepresentation = WidgetManipulator.generateMinimalWidgetRepresentation(widgetOutlines, this.numRows, this.numCols);
        let deletableRows      = [];
        let deletableCols      = [];

        for (let r = 0; r < this.numRows; r++)
        {
            if (!gridRepresentation.hasAnyEntriesOnRow(r)) deletableRows.push(r);
        }
        for (let c = 0; c < this.numCols; c++)
        {
            if (!gridRepresentation.hasAnyEntriesOnColumn(c)) deletableCols.push(c);
        }

        // todo: fix the changedAfterCheckedError that appears to be coming from the ngModel value updating after then input's min
        // which can result in a min > registered value -> WizardComponent's isValid() returns false transiently -> changedAfterCheckedError
        this.minRows = this.numRows - deletableRows.length;
        this.minCols = this.numCols - deletableCols.length;

        this.m_rowState = new GridDimensionState(deletableRows, this.numRows);
        this.m_colState = new GridDimensionState(deletableCols, this.numCols);

        this.rebuildLayout();
    }

    public increaseDensity()
    {
        if (this.canIncreaseDensity)
        {
            this.changeDensity(this.m_densityIncreaseScalar);
        }
    }

    public decreaseDensity()
    {
        if (this.canDecreaseDensity)
        {
            this.changeDensity(this.m_densityDecreaseScalar);
        }
    }

    private changeDensity(scalar: number)
    {
        this.typedWidget.numRows = Math.round(this.numRows * scalar);
        this.typedWidget.numCols = Math.round(this.numCols * scalar);

        for (let widget of this.widgets)
        {
            let outline    = widget.outline;
            outline.top    = Math.round(outline.top * scalar);
            outline.left   = Math.round(outline.left * scalar);
            outline.height = Math.round(outline.height * scalar);
            outline.width  = Math.round(outline.width * scalar);
        }

        this.rebuildDimensionStates();

        this.markForCheck();
    }

    private rebuildLayout()
    {
        this.widgetLayout = new WidgetManipulator(this.injector, new WidgetLayoutConfig(this.numCols, this.numRows), null, this.typedWidget?.id, null);

        let potentialFactors = [
            2,
            3,
            5
        ];

        let validFactor = potentialFactors.find((factor) =>
                                                {
                                                    if (this.numCols % factor !== 0) return false;
                                                    if (this.numRows % factor !== 0) return false;

                                                    return this.widgets.every((widget) =>
                                                                              {
                                                                                  let outline = widget.outline;
                                                                                  if (outline.top % factor !== 0) return false;
                                                                                  if (outline.left % factor !== 0) return false;
                                                                                  if (outline.width % factor !== 0) return false;
                                                                                  return outline.height % factor === 0;
                                                                              });
                                                });

        if (!isNaN(validFactor)) validFactor = 1 / validFactor;
        this.m_densityDecreaseScalar = validFactor;
    }

    private insertRow(idx: number)
    {
        let widgetsToAdjust = this.widgets.filter((widget) => widget.outline.top >= idx);
        for (let widget of widgetsToAdjust) widget.outline.top++;
    }

    private removeRow(idx: number)
    {
        let widgetsToAdjust = this.widgets.filter((widget) => widget.outline.top > idx);
        for (let widget of widgetsToAdjust) widget.outline.top--;
    }

    private insertCol(idx: number)
    {
        let widgetsToAdjust = this.widgets.filter((widget) => widget.outline.left >= idx);
        for (let widget of widgetsToAdjust) widget.outline.left++;
    }

    private removeCol(idx: number)
    {
        let widgetsToAdjust = this.widgets.filter((widget) => widget.outline.left > idx);
        for (let widget of widgetsToAdjust) widget.outline.left--;
    }
}

class GridDimensionState
{
    public readonly deleted: number[] = [];
    public readonly added: number[]   = [];

    constructor(public readonly deletable: number[],
                public readonly startDimension: number)
    {}

    getNextAdd(): number
    {
        let nextAdd;
        if (this.deleted.length === 0)
        {
            nextAdd = this.startDimension + this.added.length;
            this.added.push(nextAdd);
        }
        else
        {
            nextAdd = this.deleted.pop();
            this.deletable.push(nextAdd);
        }

        return nextAdd;
    }

    getNextRemoval(): number
    {
        let nextRemoval;
        if (this.added.length === 0)
        {
            if (this.deletable.length === 0) return NaN;
            nextRemoval = this.deletable.pop();
            this.deleted.push(nextRemoval);
        }
        else
        {
            nextRemoval = this.added.pop();
        }

        return nextRemoval;
    }
}

import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {BaseComponent} from "framework/ui/components";

@Component({
               selector       : "o3-grid-background[numRows][numCols]",
               templateUrl    : "./grid-background.component.html",
               styleUrls      : ["./grid-background.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class GridBackgroundComponent extends BaseComponent
{
    private m_numRows: number;
    @Input() set numRows(num: number)
    {
        if (num > 0)
        {
            this.m_numRows = num;
            this.updateGrid();
        }
    }

    private m_numCols: number;
    @Input() set numCols(num: number)
    {
        if (num > 0)
        {
            this.m_numCols = num;
            this.updateGrid();
        }
    }

    grid: void[][];

    // trackby fn: don't re-create any elements
    public idxFn = (idx: number) => idx;

    updateGrid()
    {
        if (!this.m_numRows || !this.m_numCols) return;

        this.grid = [];
        for (let i = 0; i < this.m_numRows; i++)
        {
            this.grid.push(new Array(this.m_numCols));
        }
    }
}

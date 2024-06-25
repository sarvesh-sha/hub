export class WidgetGridManipulator<U>
{
    private m_grid: U[][] = [];

    constructor(private readonly m_maxRows: number,
                private readonly m_maxCols: number)
    {
        if (m_maxRows < 0)
        {
            this.m_maxRows = 1_000_000; // Really large number, to let it grow.
        }
        else
        {
            this.expandRows(m_maxRows);
        }
    }

    get numRows(): number
    {
        return this.m_grid.length;
    }

    get numCols(): number
    {
        return this.m_maxCols;
    }

    expandRows(newNumRows: number)
    {
        while (this.numRows < newNumRows && this.numRows < this.m_maxRows)
        {
            let rowArray = new Array(this.m_maxCols);
            this.m_grid.push(rowArray);
        }
    }

    hasAnyEntriesOnRow(row: number): boolean
    {
        let rowArray = this.m_grid[row];
        if (rowArray)
        {
            for (let entry of rowArray)
            {
                if (entry !== undefined)
                {
                    return true;
                }
            }
        }

        return false;
    }

    hasAnyEntriesOnColumn(col: number): boolean
    {
        for (let rowArray of this.m_grid)
        {
            if (rowArray?.[col] !== undefined) return true;
        }

        return false;
    }

    getEntry(row: number,
             col: number): U
    {
        return this.m_grid[row]?.[col];
    }

    setEntry(row: number,
             col: number,
             value: U): boolean
    {
        if (col < 0 || col >= this.m_maxCols) return false;
        if (row < 0 || row >= this.m_maxRows) return false;

        this.expandRows(row + 1);

        this.m_grid[row][col] = value;
        return true;
    }

    findInsertPosition(width: number,
                       height: number): { left: number, top: number }
    {
        for (let r = 0; r < this.m_maxRows; r++)
        {
            for (let c = 0; c <= this.m_maxCols - width; c++)
            {
                if (this.positionFits(r, c, width, height))
                {
                    return {
                        left: c,
                        top : r
                    };
                }
            }
        }

        return null;
    }

    private positionFits(left: number,
                         top: number,
                         width: number,
                         height: number): boolean
    {
        if (top < 0 || top + width > this.m_maxCols) return false;
        if (left < 0 || left + height > this.m_maxRows) return false;

        for (let r = left; r < left + height; r++)
        {
            for (let c = top; c < top + width; c++)
            {
                if (this.getEntry(r, c) !== undefined) return false;
            }
        }

        return true;
    }
}

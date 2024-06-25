import {CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";

import {ToggleableNumericRangeExtended} from "app/customer/visualization/time-series-utils";
import {AggregationResult, ControlPointDisplayLabels} from "app/services/domain/aggregation.helper";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartFont} from "framework/ui/charting/core/text";
import {getSortDirection, SortArrowComponent, SortArrowDirection} from "framework/ui/shared/sort/sort-arrow.component";

import {Subject, Subscription} from "rxjs";

@Component({
               selector       : "o3-aggregation-table",
               templateUrl    : "./aggregation-table.component.html",
               styleUrls      : ["./aggregation-table.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationTableComponent extends BaseApplicationComponent
{
    private static readonly PREDICT_WIDTH_ROW_CT_THRESHOLD = 50;
    private static readonly FONT_SIZE                      = 14;
    private static readonly SORT_ARROW_WIDTH               = 21;
    private static readonly EXPAND_COLLAPSE_ALL_WIDTH      = 27;
    private static readonly LABEL_LEFT_PADDING             = 30;
    private static readonly INDENTATION_OFFSET             = 10;

    private m_rowChangeSub: Subscription;

    private m_tableStructure: AggregationTableStructure;
    @Input() set tableStructure(tableStructure: AggregationTableStructure)
    {
        if (this.m_rowChangeSub)
        {
            this.m_rowChangeSub.unsubscribe();
            this.m_rowChangeSub = null;
        }

        this.m_tableStructure = tableStructure;
        if (this.m_tableStructure)
        {
            this.m_rowChangeSub = this.subscribeToObservable(this.m_tableStructure.rowsUpdated, () => this.markForCheck());

            this.childrenExist = !!this.m_tableStructure.hierarchicalRows?.some((row) => row.children.length);
            this.updateAllContracted();
            this.generateFlexBases();
        }
    }

    get tableStructure(): AggregationTableStructure
    {
        return this.m_tableStructure;
    }

    get rows(): AggregationTableRow[]
    {
        return this.m_tableStructure?.rows || [];
    }

    get cols(): AggregationTableColumn[]
    {
        return this.m_tableStructure?.dataColumns || [];
    }

    initialDirection: SortArrowDirection = "asc";

    private m_currSort: Models.SortCriteria;
    @Input() set currSort(sort: Models.SortCriteria)
    {
        if (sort)
        {
            if (!this.m_currSort) this.initialDirection = getSortDirection(sort.ascending);
            this.m_currSort = sort;
        }
    }

    get currSort(): Models.SortCriteria
    {
        return this.m_currSort;
    }

    get sortColumn(): string
    {
        return this.m_currSort?.column;
    }

    @Input() emptyRowsMessage: string;
    @Input() barMode: boolean = false;
    @Input() enableTooltips: boolean;
    @Input() firstColumnHeader: string;
    @Input() twoLabels: boolean;

    get rowHeight(): number
    {
        return this.twoLabels ? 42 : 34;
    }

    private readonly m_textMeasurer = new ChartHelpers();
    private readonly m_normalFont: string;
    private readonly m_headerFont: string;
    colFlexBases: number[]          = [];

    childrenExist: boolean;
    allContracted: boolean;

    @ViewChild(CdkVirtualScrollViewport) tableViewport: CdkVirtualScrollViewport;

    @ViewChildren("test_sort") test_sorts: QueryList<SortArrowComponent>;

    @Output() currSortChange = new EventEmitter<Models.SortCriteria>();
    @Output() cellSelected   = new EventEmitter<AggregationTableCell>();

    get virtualScrollTranslationInverse(): number
    {
        let contentOffset = this.tableViewport && this.tableViewport["_renderedContentOffset"];
        if (isNaN(contentOffset)) return 0;

        // bug in chrome where borders aren't rendered correctly which leads to flickering effect as
        // content goes by and is seen through the gap created from the border not being rendered --
        // workaround: subtract by 1 to hide this gap
        return -contentOffset - 1;
    }

    constructor(inj: Injector)
    {
        super(inj);

        const font        = new ChartFont(undefined, undefined, AggregationTableComponent.FONT_SIZE);
        this.m_normalFont = font.toFontName();
        font.style        = "bold";
        this.m_headerFont = font.toFontName();
    }

    refreshSize(): boolean
    {
        if (this.tableViewport)
        {
            this.tableViewport.checkViewportSize();
            return true;
        }

        return false;
    }

    private measureText(label: string,
                        isHeader: boolean): number
    {
        if (!label) return 0;

        const font = isHeader ? this.m_headerFont : this.m_normalFont;
        let length = this.m_textMeasurer.measureText(font, label);

        if (isHeader)
        {
            // check if header has space to wrap
            if (label.match(/\s/)) length *= 0.7;

            length += AggregationTableComponent.SORT_ARROW_WIDTH;
        }

        return length;
    }

    private generateFlexBases()
    {
        const baseHorizontalPadding = 10;
        const predictingLengths     = this.rows.length > AggregationTableComponent.PREDICT_WIDTH_ROW_CT_THRESHOLD;

        const padding = AggregationTableComponent.LABEL_LEFT_PADDING + baseHorizontalPadding;
        let firstColWidth: number;
        if (predictingLengths)
        {
            if (this.childrenExist)
            {
                firstColWidth = UtilsService.predictSmartTreeLength(this.m_tableStructure.hierarchicalRows,
                                                                    AggregationTableComponent.FONT_SIZE,
                                                                    AggregationTableComponent.INDENTATION_OFFSET,
                                                                    padding,
                                                                    null);
            }
            else
            {
                const labels  = this.m_tableStructure.rows.map((row) => row.label);
                firstColWidth = UtilsService.predictSmartLength(labels, AggregationTableComponent.FONT_SIZE, padding, null);
            }
        }
        else
        {
            if (this.childrenExist)
            {
                firstColWidth = UtilsService.smartTreeLength(this.m_tableStructure.hierarchicalRows,
                                                             (label) => this.measureText(label, false),
                                                             AggregationTableComponent.INDENTATION_OFFSET,
                                                             padding,
                                                             null);
            }
            else
            {
                const labels  = this.m_tableStructure.rows.map((row) => row.label);
                firstColWidth = UtilsService.smartLength(labels,
                                                         (label: string) => this.measureText(label, false),
                                                         padding,
                                                         undefined);
            }
        }
        let firstColHeaderWidth = this.measureText(this.firstColumnHeader, true);
        firstColHeaderWidth += this.childrenExist ? AggregationTableComponent.EXPAND_COLLAPSE_ALL_WIDTH : baseHorizontalPadding;

        const multiplier  = 1.1; // give 10% buffer to first column because flex-grow = 0
        this.colFlexBases = [multiplier * Math.max(firstColWidth, firstColHeaderWidth)];

        let cellTexts = this.m_tableStructure.dataColumns.map((col,
                                                               colIdx) => this.m_tableStructure.rows.map((row) => row.cells[colIdx].formattedValue));
        for (let i = 0; i < cellTexts.length; i++)
        {
            let headerWidth     = this.measureText(this.m_tableStructure.dataColumns[i].headerLabel, true);
            let smartValueWidth = UtilsService.predictSmartLength(cellTexts[i], AggregationTableComponent.FONT_SIZE, baseHorizontalPadding, null);
            this.colFlexBases.push(Math.max(headerWidth, smartValueWidth));
        }
    }

    barWidthCss(cell: AggregationTableCell): string
    {
        if (!cell.barWidthCss)
        {
            let value = cell.value;
            if (isNaN(value ?? undefined))
            {
                cell.barWidthCss = "0";
            }
            else
            {
                // Get bounds to use
                let bounds        = cell.aggRange.clone();
                const leftPadding = 0.1;
                bounds.min -= bounds.diff * leftPadding;

                let barRange = cell.barRange;
                if (ToggleableNumericRangeExtended.isActive(barRange))
                {
                    if (!barRange.minInvalid) bounds.min = barRange.min;
                    if (!barRange.maxInvalid) bounds.max = barRange.max;
                }

                // Calculate a percentage
                const maxWidthPercent = 95;
                if (bounds.min !== bounds.max)
                {
                    let clampedValue = UtilsService.clamp(bounds.min, bounds.max, value);
                    cell.barWidthCss = `${((clampedValue - bounds.min) / (bounds.max - bounds.min)) * maxWidthPercent}%`;
                }
                else
                {
                    cell.barWidthCss = `${value < bounds.min ? 0 : maxWidthPercent}%`;
                }
            }
        }

        return cell.barWidthCss;
    }

    labelPaddingLeftPx(row?: AggregationTableRow): number
    {
        if (!row) return 0;

        let padding = row.depth * AggregationTableComponent.INDENTATION_OFFSET;

        // provide padding to match open/close chevrons
        if (!row.children.length) padding += AggregationTableComponent.LABEL_LEFT_PADDING;

        return padding;
    }

    rowHeaderTooltip(row: AggregationTableRow): string
    {
        return row.secondaryLabel ? row.primaryLabel + ": " + row.secondaryLabel : row.primaryLabel;
    }

    showValue(group: AggregationResult): boolean
    {
        return group.children.length === 0 || group.aggType !== Models.AggregationTypeId.NONE;
    }

    //--//

    updateSort(column: string,
               direction: SortArrowDirection)
    {
        this.m_currSort = Models.SortCriteria.newInstance({
                                                              column   : column,
                                                              ascending: direction === "asc"
                                                          });
        this.currSortChange.emit(this.m_currSort);
    }

    expandContractAll()
    {
        this.allContracted = !this.allContracted;

        for (let row of this.rows)
        {
            if (this.allContracted === row.showChildren) this.expandContractRow(row, true);
        }
    }

    expandContractRow(row: AggregationTableRow,
                      skipCheckAll?: boolean)
    {
        row.showChildren = !row.showChildren;
        row.children.forEach((child) =>
                             {
                                 if (child.showThisNode !== row.showChildren)
                                 {
                                     child.showThisNode = row.showChildren;
                                     this.trickleDownShowingChange(child);
                                 }
                             });

        if (!skipCheckAll) this.updateAllContracted();
    }

    private trickleDownShowingChange(row: AggregationTableRow)
    {
        row.children.forEach((child) =>
                             {
                                 if ((!row.showThisNode && child.showThisNode) || (row.showThisNode && row.showChildren && !child.showThisNode))
                                 {
                                     child.showThisNode = row.showThisNode;
                                     this.trickleDownShowingChange(child);
                                 }
                             });
    }

    private updateAllContracted()
    {
        if (this.childrenExist)
        {
            this.allContracted = true;
            for (let row of this.m_tableStructure.rows)
            {
                if (row.showChildren && row.children.length > 0)
                {
                    this.allContracted = false;
                    break;
                }
            }
        }
    }
}

export class AggregationTableStructure
{
    constructor(public readonly dataColumns: AggregationTableColumn[])
    {}

    hierarchicalRows: AggregationTableRow[];

    private m_rows: AggregationTableRow[] = [];
    set rows(rows: AggregationTableRow[])
    {
        if (this.m_rows.length)
        {
            let lookup = UtilsService.extractLookup(this.m_rows);
            for (let row of rows)
            {
                row.showChildren = lookup[row.id]?.showChildren ?? true;
                row.showThisNode = lookup[row.id]?.showThisNode ?? true;
            }
        }

        this.m_rows = rows;
        this.rowsUpdated.next();
    }

    get rows(): AggregationTableRow[]
    {
        return this.m_rows;
    }

    rowsUpdated = new Subject<void>();
}

export interface AggregationTableColumn
{
    identifier: string;
    headerLabel: string;
    headerTooltip: string;
}

export interface AggregationTableRow extends ControlPointDisplayLabels
{
    id: string;
    primaryLabel: string;
    secondaryLabel: string;
    label: string; // driver for header column sizing

    cells: AggregationTableCell[];

    topLevelRowNumber: number;
    children: AggregationTableRow[];
    depth: number;
    valid: boolean;
    showThisNode: boolean;
    showChildren: boolean;
}

export interface AggregationTableCell
{
    value: number;
    formattedValue: string;
    significantElement: DeviceElementExtended;
    color: string;
    tooltip: string;
    aggRange: ChartValueRange;
    barRange: Models.ToggleableNumericRange;
    barWidthCss: string;
    colIdx: number;
    italicize: boolean;
}

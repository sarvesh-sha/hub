import {ExportsApi} from "app/services/proxy/api/ExportsApi";
import * as Models from "app/services/proxy/model/models";

import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {Future} from "framework/utils/concurrency";

export class ExcelExporter implements DownloadGenerator
{
    private m_id: string;
    private m_values: Models.ExportCell[][] = [];

    private m_rows = 0;
    private m_fileGeneration: Promise<void>;

    private m_isDone = false;

    public readonly body: Models.ExportHeader;

    private readonly m_headerTypes: ExcelColumnType[] = [];

    constructor(private m_exportsApi: ExportsApi,
                sheetName: string,
                public readonly emptyCellText: string   = "",
                public readonly maxRowsPerFlush: number = 1000)
    {
        this.body = Models.ExportHeader.newInstance({
                                                        sheetName: sheetName,
                                                        columns  : []
                                                    });
    }

    public getProgressMessage(): string
    {
        if (this.m_fileGeneration) return "Generating file...";

        return `${this.m_rows} rows added`;
    }

    public getProgressPercent(): number
    {
        return NaN;
    }

    public isDeterminate(): boolean
    {
        return false;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        return this.m_isDone;
    }

    public async sleepForProgress(): Promise<void>
    {
        await Future.delayed(500);
    }

    addColumnHeader(label: string,
                    timeFormat?: string)
    {
        let col = Models.ExportColumn.newInstance({title: label});
        if (timeFormat)
        {
            col.dateFormatter = timeFormat;
            this.m_headerTypes.push(ExcelColumnType.DateTime);
        }
        else
        {
            this.m_headerTypes.push(ExcelColumnType.Text);
        }

        this.body.columns.push(col);
    }

    async addRow(): Promise<ExcelExporterRow>
    {
        this.m_rows++;
        await this.flushRows(true);

        let cells: Models.ExportCell[] = [];
        this.m_values.push(cells);
        return new ExcelExporterRow(cells, this.m_headerTypes, this.emptyCellText);
    }

    public finish(): void
    {
        this.m_isDone = true;
    }

    async generateFile()
    {
        if (!this.m_fileGeneration)
        {
            this.m_fileGeneration = this.generateFileImpl();
        }

        return this.m_fileGeneration;
    }


    private async generateFileImpl()
    {
        if (this.m_id)
        {
            await this.flushRows();

            await this.m_exportsApi.generateExcel(this.m_id);
        }
    }

    async getResults(fileName: string): Promise<DownloadResults>
    {
        if (!this.m_id) return null;

        await this.generateFile();

        return {
            url: this.m_exportsApi.streamExcel__generateUrl(this.m_id, fileName)
        };
    }

    private async flushRows(flushIfNeeded?: boolean)
    {
        if (!this.m_id)
        {
            this.m_id = await this.m_exportsApi.start(this.body);
        }

        let requiredNumValues = flushIfNeeded ? this.maxRowsPerFlush : 1;
        if (this.m_values.length >= requiredNumValues)
        {
            await this.m_exportsApi.add(this.m_id, this.m_values);
            this.m_values = [];
        }
    }
}

export class ExcelExporterRow
{
    constructor(private readonly m_cells: Models.ExportCell[],
                private readonly m_headerTypes: ExcelColumnType[],
                public readonly emptyCellText: string)
    {}

    push(...values: (string | number | Date)[])
    {
        for (let value of values) this.pushHelper(value);
    }

    private pushHelper(value: string | number | Date)
    {
        if (this.m_headerTypes[this.m_cells.length] === ExcelColumnType.DateTime)
        {
            let dateValue = <Date>value;
            if (dateValue)
            {
                this.m_cells.push(Models.ExportCell.newInstance({dateTime: dateValue}));
                return;
            }
        }
        else
        {
            if (typeof value == "number" && !isNaN(value))
            {
                this.m_cells.push(Models.ExportCell.newInstance({decimal: value}));
                return;
            }
            else if (value)
            {
                this.m_cells.push(Models.ExportCell.newInstance({text: <string>value}));
                return;
            }
        }

        this.m_cells.push(Models.ExportCell.newInstance({text: this.emptyCellText}));
    }
}

enum ExcelColumnType
{
    Text,
    DateTime,
    Decimal
}

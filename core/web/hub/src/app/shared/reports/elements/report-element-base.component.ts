import {Directive, EventEmitter, Injector, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

export class ReportElementModel
{
    id: string;

    configuration: ReportElementConfigurationBase;

    data: ReportElementDataBase;

    type: ReportElementType;
}

export enum ReportElementType
{
    AlertExecutionSummary,
    AlertFeed,
    AlertsList,
    AlertTable,
    AlertsSummary,
    Chart,
    ControlPointList,
    DevicesList,
    DevicesSummary,
    EquipmentList,
    Header,
    TimeSeriesContainer,
    AggregatedValue,
    AggregationTable,
    AggregationTrend,
    ChartSet,
    RichText,
    PageBreak
}

export class ReportSection
{
    constructor(public readonly columns: ReportSectionColumn[] = [])
    {}
}

export class ReportSectionColumn
{
    widthPercent: number;

    constructor(public models: (ReportSection | ReportElementModel)[] = [],
                public widthRatio: number)
    {}
}

export class ReportSectionBuilder
{
    columns: ReportSectionColumn[] = [];

    private m_cumWidth: number = 0;

    withColumn(layout: Models.ReportLayoutBase,
               ...models: (ReportSection | ReportElementModel)[])
    {
        let widthRatio = layout.widthRatio || 1;
        this.m_cumWidth += widthRatio;
        this.columns.push(new ReportSectionColumn(models.filter((item) => !!item), widthRatio));
    }

    build(): ReportSection
    {
        for (let col of this.columns)
        {
            col.widthPercent = 100 * col.widthRatio / this.m_cumWidth || 100;
        }

        return new ReportSection(this.columns);
    }
}

export abstract class ReportElementDataBase
{
}

export abstract class ReportElementConfigurationBase
{
    forPreview: boolean = false;
    widthRatio: number  = 1;
}

@Directive()
export abstract class ReportElementBaseComponent<TData extends ReportElementDataBase, TConfiguration extends ReportElementConfigurationBase> extends SharedSvc.BaseApplicationComponent
{
    private m_id: string;

    public get id(): string
    {
        return this.m_id;
    }

    @Input()
    public set id(value: string)
    {
        this.m_id = value;
    }

    private m_config: TConfiguration;

    public get config(): TConfiguration
    {
        return this.m_config;
    }

    @Input()
    public set config(value: TConfiguration)
    {
        this.m_config = value;
        this.reportConfigurationChanges();
    }

    private m_data: TData;

    public get data(): TData
    {
        return this.m_data;
    }

    @Input()
    public set data(value: TData)
    {
        this.m_data = value;
        this.reportConfigurationChanges();
    }

    private m_renderComplete = new EventEmitter<ReportElementConfigurationBase>();

    //--//

    constructor(inj: Injector)
    {
        super(inj);
    }

    markAsComplete()
    {
        this.m_renderComplete.emit(this.config);
    }

    subscribeToCompleted(callback: (value: ReportElementConfigurationBase) => void)
    {
        this.m_renderComplete.subscribe(callback);
    }
}

// -- //

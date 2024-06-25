import {Injectable, Injector} from "@angular/core";
import {DeliveryOptionsExtended} from "app/customer/configuration/common/delivery-options";
import {AppDomainContext} from "app/services/domain/domain.module";
import {ReportDefinitionVersionExtended, ReportDefinitionVersionsService} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionDetailsExtended, ReportDefinitionExtended, ReportDefinitionsService} from "app/services/domain/report-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {DaysOfWeek} from "app/shared/forms/time-range/range-selection-extended";

import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Injectable()
export class ReportingService
{
    private reportDefinitions: ReportDefinitionsService;
    private reportDefinitionVersions: ReportDefinitionVersionsService;

    constructor(private errors: ErrorService,
                inj: Injector)
    {
        this.reportDefinitionVersions = inj.get(ReportDefinitionVersionsService);
        this.reportDefinitions        = inj.get(ReportDefinitionsService);
    }

    /**
     * Get the configuration for an on demand report
     *
     * @param report
     * @param title
     * @param description
     * @param [rangeStart]
     * @param [rangeEnd]
     * @param [autoDelete]
     */
    getReportConfig(report: ReportDefinitionDetailsExtended,
                    title: string,
                    description: string,
                    rangeStart?: Date,
                    rangeEnd?: Date,
                    autoDelete?: Date): ReportConfig
    {
        let definition = this.reportDefinitions.allocateInstance();
        let version    = this.reportDefinitionVersions.allocateInstance();

        version.model.details = report.model;

        let detailsExtended   = version.getDetailsExtended();
        let schedulingOptions = detailsExtended.getSchedulingOptions();
        if (schedulingOptions)
        {
            schedulingOptions.model.schedule = null;
        }

        if (!rangeStart || !rangeEnd)
        {
            rangeStart = new Date();
            rangeEnd   = new Date();
            rangeStart.setDate(rangeStart.getDate() - 7);
        }

        definition.model.title       = title;
        definition.model.description = description;
        definition.model.autoDelete  = autoDelete;

        let config        = new ReportConfig();
        config.definition = definition;
        config.version    = version;
        config.rangeStart = rangeStart;
        config.rangeEnd   = rangeEnd;

        return config;
    }

    /**
     * Launch the PDF export window.
     * @param report
     * @param title
     * @param [rangeStart]
     * @param [rangeEnd]
     */
    async savePdf(report: ReportDefinitionDetailsExtended,
                  title: string,
                  rangeStart?: Date,
                  rangeEnd?: Date): Promise<void>
    {
        let config = this.getReportConfig(report,
                                          `On demand ${title} report`,
                                          "",
                                          rangeStart,
                                          rangeEnd,
                                          MomentHelper.now()
                                                      .add("7", "days")
                                                      .toDate());

        await this.errors.success("Generating report", -1);

        config.definition               = await config.definition.save();
        config.version.model.definition = config.definition.getIdentity();
        config.version                  = await config.version.save();

        await config.version.triggerReport(config.rangeStart, config.rangeEnd);
    }
}

export class ReportSchedulingOptionsExtended
{
    private m_deliveryOptions: DeliveryOptionsExtended;

    private m_time: Date;

    constructor(public model: Models.ReportSchedulingOptions,
                domain: AppDomainContext,
                initCurrentUser: boolean)
    {
        if (!model.deliveryOptions)
        {
            model.deliveryOptions = new Models.DeliveryOptions();
        }

        if (!model.schedule)
        {
            model.schedule = this.getDefaultOnDemandSchedule();
        }

        this.m_time = new Date();
        this.m_time.setHours(model.schedule.hour, model.schedule.minute, 0, 0);

        this.m_deliveryOptions = new DeliveryOptionsExtended(model.deliveryOptions, domain, initCurrentUser);
    }

    get schedulingType(): SchedulingType
    {
        if (this.model.schedule instanceof Models.ReportScheduleDaily)
        {
            return SchedulingType.Daily;
        }
        else if (this.model.schedule instanceof Models.ReportScheduleWeekly)
        {
            return SchedulingType.Weekly;
        }
        else if (this.model.schedule instanceof Models.ReportScheduleMonthly)
        {
            return SchedulingType.Monthly;
        }

        return SchedulingType.OnDemand;
    }

    set schedulingType(type: SchedulingType)
    {
        switch (type)
        {
            case SchedulingType.OnDemand:
                this.model.schedule = this.getDefaultOnDemandSchedule();
                break;

            case SchedulingType.Daily:
                this.model.schedule = this.getDefaultDailySchedule();
                break;

            case SchedulingType.Weekly:
                this.model.schedule = this.getDefaultWeeklySchedule();
                break;

            case SchedulingType.Monthly:
                this.model.schedule = this.getDefaultMonthlySchedule();
                break;
        }
    }

    get daysOfWeek(): Models.DayOfWeek[]
    {
        if (this.model.schedule instanceof Models.ReportScheduleDaily)
        {
            return this.model.schedule.days;
        }
        else if (this.model.schedule instanceof Models.ReportScheduleWeekly)
        {
            return [this.model.schedule.dayOfWeek];
        }

        return [];
    }

    set daysOfWeek(days: Models.DayOfWeek[])
    {
        if (this.model.schedule instanceof Models.ReportScheduleDaily)
        {
            this.model.schedule.days = days;
        }
        else if (this.model.schedule instanceof Models.ReportScheduleWeekly)
        {
            this.model.schedule.dayOfWeek = days[0];
        }
    }

    get dayOfMonth(): number
    {
        if (this.model.schedule instanceof Models.ReportScheduleMonthly)
        {
            return this.model.schedule.dayOfMonth;
        }

        return 1;
    }

    set dayOfMonth(day: number)
    {
        if (this.model.schedule instanceof Models.ReportScheduleMonthly)
        {
            this.model.schedule.dayOfMonth = UtilsService.clamp(1, 31, day);
        }
    }

    get timeOfDay(): Date
    {
        return this.m_time;
    }

    set timeOfDay(time: Date)
    {
        this.m_time                = time;
        this.model.schedule.hour   = time.getHours();
        this.model.schedule.minute = time.getMinutes();
    }

    get deliveryOptions(): DeliveryOptionsExtended
    {
        return this.m_deliveryOptions;
    }

    private getDefaultOnDemandSchedule(): Models.ReportScheduleOnDemand
    {
        let schedule = new Models.ReportScheduleOnDemand();
        this.assignTime(schedule);
        return schedule;
    }

    private getDefaultDailySchedule(): Models.ReportScheduleDaily
    {
        let schedule = new Models.ReportScheduleDaily();
        this.assignTime(schedule);
        schedule.days = DaysOfWeek;
        return schedule;
    }

    private getDefaultWeeklySchedule(): Models.ReportScheduleWeekly
    {
        let schedule = new Models.ReportScheduleWeekly();
        this.assignTime(schedule);
        schedule.dayOfWeek = Models.DayOfWeek.SUNDAY;
        return schedule;
    }

    private getDefaultMonthlySchedule(): Models.ReportScheduleMonthly
    {
        let schedule = new Models.ReportScheduleMonthly();
        this.assignTime(schedule);
        schedule.dayOfMonth = 1;
        return schedule;
    }

    private assignTime(schedule: Models.ReportSchedule)
    {
        schedule.zoneDesired = this.model.schedule?.zoneDesired || MomentHelper.getLocalZone();
        schedule.hour        = 0;
        schedule.minute      = 0;
    }

}

export enum SchedulingType
{
    OnDemand = "OnDemand",
    Daily    = "Daily",
    Weekly   = "Weekly",
    Monthly  = "Monthly"
}

export const SchedulingTypeOptions: ControlOption<SchedulingType>[] = [
    new ControlOption(SchedulingType.OnDemand, "On Demand"),
    new ControlOption(SchedulingType.Daily, "Daily"),
    new ControlOption(SchedulingType.Weekly, "Weekly"),
    new ControlOption(SchedulingType.Monthly, "Monthly")
];

export class ReportConfigurationExtended
{
    constructor(protected m_model: Models.ReportConfiguration)
    {
    }

    public get model(): Models.ReportConfiguration
    {
        return this.m_model;
    }

    get container(): Models.ReportLayoutColumn
    {
        if (!this.model.container)
        {
            this.model.container = Models.ReportLayoutColumn.newInstance({
                                                                             widthRatio: 1,
                                                                             children  : this.getDefaultRows()
                                                                         });
        }

        return this.model.container;
    }

    get sharedGraphs(): Models.SharedAssetGraph[]
    {
        if (!this.model.sharedGraphs)
        {
            this.model.sharedGraphs = [];
        }

        return this.model.sharedGraphs;
    }

    public static getRichTextHeader(title = "Custom Report"): Models.ReportLayoutItem
    {
        let text = Models.CustomReportElementRichText.newInstance({
                                                                      backgroundColor: "#FFFFFF",
                                                                      data           : [
                                                                          {
                                                                              "attributes": {
                                                                                  "width": "153"
                                                                              },
                                                                              "insert"    : {
                                                                                  "image": "data:image/png;base64,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"
                                                                              }
                                                                          },
                                                                          {
                                                                              "attributes": {
                                                                                  "align": "right"
                                                                              },
                                                                              "insert"    : "\n"
                                                                          },
                                                                          {
                                                                              "insert": title
                                                                          },
                                                                          {
                                                                              "attributes": {
                                                                                  "align" : "center",
                                                                                  "header": 2
                                                                              },
                                                                              "insert"    : "\n"
                                                                          },
                                                                          {
                                                                              "insert": "from "
                                                                          },
                                                                          {
                                                                              "insert": {
                                                                                  "reportDateRange": true
                                                                              }
                                                                          },
                                                                          {
                                                                              "attributes": {
                                                                                  "align": "center"
                                                                              },
                                                                              "insert"    : "\n"
                                                                          }
                                                                      ]
                                                                  });

        return Models.ReportLayoutItem.newInstance({element: text});
    }

    public get isLandscape(): boolean
    {
        return !!this.model.landscape;
    }

    public set isLandscape(landscape: boolean)
    {
        this.model.landscape = landscape;
    }

    public get pdfFormat(): Models.PaperFormat
    {
        if (!this.model.pdfFormat)
        {
            this.model.pdfFormat = Models.PaperFormat.legal;
        }

        return this.model.pdfFormat;
    }

    public set pdfFormat(pdfFormat: Models.PaperFormat)
    {
        this.model.pdfFormat = pdfFormat;
    }

    public static areSimilarLayouts(layoutA: Models.ReportLayoutBase,
                                    layoutB: Models.ReportLayoutBase,
                                    seenLayouts: Set<Models.ReportLayoutBase> = new Set()): boolean
    {
        if (!layoutA && !layoutB) return true;
        if (!layoutA || !layoutB) return false;

        if (seenLayouts.has(layoutA) || seenLayouts.has(layoutB)) throw new Error("loop in report layout config");
        seenLayouts.add(layoutA);
        seenLayouts.add(layoutB);

        if (layoutA instanceof Models.ReportLayoutItem && layoutB instanceof Models.ReportLayoutItem)
        {
            return Object.getPrototypeOf(layoutA.element) == Object.getPrototypeOf(layoutB.element);
        }

        if (layoutA.children?.length === layoutB.children?.length && layoutA.children?.length != null)
        {
            return layoutA.children.every((childA,
                                           idx) => ReportConfigurationExtended.areSimilarLayouts(childA, layoutB.children[idx], seenLayouts));
        }

        return false;
    }

    private getDefaultRows(): Models.ReportLayoutBase[]
    {
        return [
            ReportConfigurationExtended.getRichTextHeader(),
            Models.ReportLayoutRow.newInstance({children: []})
        ];
    }
}

export class ReportConfig
{
    public definition: ReportDefinitionExtended;
    public version: ReportDefinitionVersionExtended;
    public rangeStart: Date;
    public rangeEnd: Date;
}

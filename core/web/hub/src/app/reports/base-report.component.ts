import {Directive, InjectionToken, Injector, Type} from "@angular/core";
import {AppService} from "app/app.service";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionDetailsExtended, ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {Logger, LoggingService} from "framework/services/logging.service";
import moment from "framework/utils/moment";

@Directive()
export abstract class BaseReportComponent extends SharedSvc.BaseApplicationComponent implements ReportMetadata
{
    public reportDefinition: ReportDefinitionExtended;
    public reportDefinitionVersion: ReportDefinitionVersionExtended;
    public rangeStart: moment.Moment;
    public rangeEnd: moment.Moment;
    public reportDetails: ReportDefinitionDetailsExtended;
    protected logger: Logger;

    constructor(inj: Injector,
                private appService: AppService)
    {
        super(inj);
        this.logger = inj.get(LoggingService)
                         .getLogger(this.getType());
    }

    async ngOnInit()
    {
        try
        {
            super.ngOnInit();
            this.reportDetails = this.reportDefinitionVersion.getDetailsExtended();

            await this.init();
        }
        catch (err)
        {
            this.logger.error(err);
            this.appService.triggerReportFailure();
        }
    }

    protected triggerReport()
    {
        this.appService.triggerReport();
    }

    protected abstract getType(): Type<BaseReportComponent>;

    protected abstract init(): Promise<void>;
}


export interface ReportMetadata
{
    rangeStart: moment.Moment;
    rangeEnd: moment.Moment;
}

export const REPORT_METADATA = new InjectionToken<ReportMetadata>("REPORT_METADATA");

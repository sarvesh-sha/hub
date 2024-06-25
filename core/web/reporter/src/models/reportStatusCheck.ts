import {Enum, Property, Schema} from "@tsed/schema";
import {ReportStatus} from "./reportStatus";

export class ReportStatusCheck
{
    @Property()
    reportId: string;

    @Property()
    @Enum(ReportStatus)
    @Schema({"x-optio3-enum-type": "com.optio3.reporter.ReportStatus"})
    status: ReportStatus;

    constructor(reportId?: string,
                status?: ReportStatus)
    {
        this.reportId = reportId;
        this.status   = status;
    }
}

import {Property, Description} from "@tsed/schema";

export class ReportIdentity
{
    @Property()
    @Description("Identifier used to access the report")
    reportId: string;

    constructor(reportId?: string)
    {
        this.reportId = reportId;
    }
}

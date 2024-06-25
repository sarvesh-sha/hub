import {Property, Integer} from "@tsed/schema";

export class ReportsHealthCheck
{
    @Property()
    @Integer()
    reportsGenerated: number;

    @Property()
    @Integer()
    reportsFailed: number;

    @Property()
    @Integer()
    reportsQueued: number;

    constructor(reportsGenerated?: number,
                reportsFailed?: number,
                reportsQueued?: number)
    {
        this.reportsGenerated = reportsGenerated;
        this.reportsFailed    = reportsFailed;
        this.reportsQueued    = reportsQueued;
    }
}

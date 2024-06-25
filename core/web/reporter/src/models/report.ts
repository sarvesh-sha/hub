import {Property} from "@tsed/schema";

export class Report
{
    @Property()
    reportId: string;

    @Property()
    bytes: string;
}

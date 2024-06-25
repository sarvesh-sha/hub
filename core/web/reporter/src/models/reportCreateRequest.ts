import {Enum, Integer, Property, Required, Schema} from "@tsed/schema";
import {PaperFormat} from "./paperFormat";

export class ReportCreateRequest
{
    @Property()
    @Required()
    baseUrl: string;

    @Property()
    @Required()
    reportToken: string;

    @Property()
    @Required()
    sessionToken: string;

    @Property()
    reportTime: string;

    @Property()
    @Enum(PaperFormat)
    @Schema({"x-optio3-enum-type": "com.optio3.reporter.PaperFormat"})
    pdfFormat: PaperFormat;

    @Property()
    landscape: boolean;

    @Property()
    @Integer()
    maxWaitTimeSeconds: number;

    @Property()
    @Integer()
    screenWidth: number;

    @Property()
    @Integer()
    screenHeight: number;
}

import {CollectionOf, Enum, Property, Schema} from "@tsed/schema";
import {TestResultStatus} from "./testResultStatus";

export class TestResult
{
    @Property()
    id: string;

    @Property()
    name: string;

    @Property()
    message: string;

    @Property()
    @Enum(TestResultStatus)
    @Schema({"x-optio3-enum-type": "com.optio3.tester.TestResultStatus"})
    status: TestResultStatus;

    @Property()
    @CollectionOf(String)
    logs: string[];

    @Property()
    @CollectionOf(String)
    consoleLogs: string[];

    @Property()
    testStart: Date;

    @Property()
    testEnd: Date;

    @Property()
    videoId: string;

    public static getRelativeTime(result: TestResult): string
    {
        const now     = new Date();
        const diff    = now.valueOf() - result.testStart.valueOf();
        const seconds = Math.round(diff / 1000);
        return `${seconds}s`;
    }

    public static isComplete(test: TestResult): boolean
    {
        switch (test.status)
        {
            case TestResultStatus.Cancelled:
            case TestResultStatus.Failed:
            case TestResultStatus.Passed:
                return true;
        }

        return false;
    }
}

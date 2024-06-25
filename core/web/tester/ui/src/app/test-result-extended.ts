import * as Models from "app/services/proxy/model/models";

export class TestResultExtended
{
    constructor(public readonly model: Models.TestResult) {}

    public static isComplete(test: Models.TestResult): boolean
    {
        switch (test.status)
        {
            case Models.TestResultStatus.Cancelled:
            case Models.TestResultStatus.Failed:
            case Models.TestResultStatus.Passed:
                return true;
        }

        return false;
    }
}

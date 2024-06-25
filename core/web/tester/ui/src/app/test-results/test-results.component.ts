import {animate, style, transition, trigger} from "@angular/animations";
import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as Models from "app/services/proxy/model/models";
import {TestResultExtended} from "app/test-result-extended";

@Component({
               selector   : "o3-test-results",
               templateUrl: "./test-results.component.html",
               animations : [
                   trigger("expand", [
                       transition(":enter", [
                           style({height: 0}),
                           animate(".5s ease-out", style({height: "216px"}))
                       ])
                   ])
               ],
               styleUrls  : ["./test-results.component.scss"]
           })
export class TestResultsComponent implements OnChanges
{
    @Input()
    public testResults: Models.TestResult[];

    @Input()
    public testStart: Date;

    private m_lastCheck: Date;

    public readonly trackTestResult = (index: number,
                                       result: Models.TestResult) => this.testStart?.valueOf() + result.id;

    @Output() cancelTest = new EventEmitter<string>();

    constructor(private apis: ApiService)
    {
    }

    ngOnChanges(changes: SimpleChanges)
    {
        this.m_lastCheck = new Date();
    }

    testPending(test: Models.TestResult): boolean
    {
        return test.status === Models.TestResultStatus.Pending;
    }

    testCancelled(test: Models.TestResult): boolean
    {
        return test.status === Models.TestResultStatus.Cancelled;
    }

    testRunning(test: Models.TestResult): boolean
    {
        return test.status === Models.TestResultStatus.Running;
    }

    testFailed(test: Models.TestResult): boolean
    {
        return test.status === Models.TestResultStatus.Failed;
    }

    testPassed(test: Models.TestResult): boolean
    {
        return test.status === Models.TestResultStatus.Passed;
    }

    testIncomplete(test: Models.TestResult): boolean
    {
        return !TestResultExtended.isComplete(test);
    }

    getStatus(test: Models.TestResult): string
    {
        let status = <string>test.status;

        if (test.testStart)
        {
            let start = new Date(test.testStart);
            let end   = new Date(test.testEnd || this.m_lastCheck);

            status = `${status} - ${(end.valueOf() - start.valueOf()) / 1000}s`;
        }

        return status;
    }

    getVideoSource(test: Models.TestResult): string
    {
        return `${this.apis.basePath}/api/v1/tests/video/${test.videoId}`;
    }

    canLoadVideo(test: Models.TestResult): boolean
    {
        return test.testEnd && (new Date().valueOf() - new Date(test.testEnd).valueOf()) > 5000;
    }
}

import {animate, style, transition, trigger} from "@angular/animations";
import {Component} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {TestResultExtended} from "app/test-result-extended";
import {TestGroup} from "app/test-selector/test-selector.component";
import {ApiService} from "app/services/domain/api.service";

@Component({
               selector   : "app-root",
               templateUrl: "./app.component.html",
               animations : [
                   trigger("expand", [
                       transition(":enter", [
                           style({height: 0}),
                           animate(".5s ease-out", style({height: "216px"}))
                       ])
                   ])
               ],
               styleUrls  : ["./app.component.scss"]
           })
export class AppComponent
{
    public baseUrl: string = "http://localhost:4201";

    public tests: Models.TestCase[] = [];
    public runningTests: Models.TestCase[];

    public testGroups: TestGroup[] = [];
    public filteredTestGroups: TestGroup[];

    private m_selectedCategories: Set<string>;

    public readonly prevResults: ResultSet[] = [];
    public testResults: Models.TestResult[];

    public readonly selectedTests = new Set<string>();

    private m_testTracker: number;

    get testsRunning(): boolean
    {
        return !!this.m_testTracker;
    }

    get testResultsSummary(): string
    {
        if (!this.testResults?.length)
        {
            return "";
        }

        let numPassed    = 0;
        let numCancelled = 0;
        for (let test of this.testResults)
        {
            switch (test.status)
            {
                case Models.TestResultStatus.Passed:
                    numPassed++;
                    break;

                case Models.TestResultStatus.Cancelled:
                    numCancelled++;
                    break;
            }
        }
        let result = `${numPassed} of ${this.testResults.length} passed.`;
        if (numCancelled) result += ` ${numCancelled} cancelled.`;
        if (this.m_testEnd)
        {
            const elapsed = Math.round((this.m_testEnd.valueOf() - this.testStart.valueOf()) / 1000);
            result += ` Total time: ${elapsed}s`;
        }

        return result;
    }

    public testStart: Date;
    private m_testEnd: Date;
    private m_testsCancelled: boolean;

    constructor(private apis: ApiService)
    {
    }

    async getTests()
    {
        this.tests = await this.apis.tests.getTests(Models.TestsInitializeRequest.newInstance({url: this.baseUrl})) || [];

        const noneCategory = "(none)";
        const groups       = new Map<string, TestGroup>();
        for (let test of this.tests)
        {
            if (!test.categories?.length)
            {
                test.categories = [noneCategory];
            }

            for (let category of test.categories)
            {
                if (!groups.has(category))
                {
                    groups.set(category, new TestGroup(category, []));
                }

                const group = groups.get(category);
                group.tests.push(test);
            }

            this.selectedTests.add(test.id);
        }

        this.testGroups = Array.from(groups.values())
                               .sort((a,
                                      b) => a.category.localeCompare(b.category));
        for (let group of this.testGroups)
        {
            group.tests.sort((a,
                              b) => a.name.localeCompare(b.name));
        }

        this.filteredTestGroups   = [...this.testGroups];
        this.m_selectedCategories = new Set(this.testGroups.map((group) => group.category));
    }

    updateFilteredGroups(categories: Set<string>)
    {
        this.m_selectedCategories = categories;
        this.filteredTestGroups   = this.testGroups.filter((group) => this.m_selectedCategories.has(group.category));
    }

    async runTests()
    {
        if (this.testResults)
        {
            this.prevResults.unshift(new ResultSet(this.testResults, this.testResultsSummary, this.testStart));
            this.testResults = null;
        }

        if (this.m_testTracker) this.clearTestTracker();

        this.m_testsCancelled = false;

        this.testStart = new Date();
        this.m_testEnd = null;

        this.runningTests = await this.apis.tests.startTests(Models.TestsRunRequest.newInstance({
                                                                                                    url       : this.baseUrl,
                                                                                                    ids       : Array.from(this.selectedTests),
                                                                                                    categories: Array.from(this.m_selectedCategories)
                                                                                                }));
        await this.getProgress();

        this.m_testTracker = setInterval(async () =>
                                         {
                                             await this.getProgress();

                                             if (this.testResults.every((result) => TestResultExtended.isComplete(result)))
                                             {
                                                 this.m_testEnd = new Date();
                                                 this.clearTestTracker();
                                             }
                                         }, 1000);
    }

    private async getProgress()
    {
        const results = await this.apis.tests.checkProgress() || [];

        const resultsLookup: { [id: string]: Models.TestResult } = {};
        for (let result of results) resultsLookup[result.id] = result;

        this.testResults = this.runningTests.map((testCase) => resultsLookup[testCase.id]);
    }

    private clearTestTracker()
    {
        clearInterval(this.m_testTracker);
        this.m_testTracker = null;
    }

    async cancelTests()
    {
        if (this.m_testTracker)
        {
            this.m_testsCancelled = await this.apis.tests.cancelTests();
        }
    }

    async cancelTest(testId: string)
    {
        const result = this.testResults.find((result) => result.id === testId);
        if (!TestResultExtended.isComplete(result))
        {
            await this.apis.tests.cancelTest(testId);
        }
    }
}

class ResultSet
{
    constructor(public readonly results: Models.TestResult[],
                public readonly summary: string,
                public readonly testStart: Date)
    {
    }
}
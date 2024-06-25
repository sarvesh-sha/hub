import * as fs from "fs";
import {ConsoleMessage, Page} from "puppeteer";
import {PuppeteerScreenRecorder} from "puppeteer-screen-recorder";
import {TestCase} from "../models/testCase";
import {TestResultStatus} from "../models/testResultStatus";
import {TestResult} from "../models/testResult";
import {Future} from "../utils/concurrency";
import {TestStartEvent} from "./events";

export class TestResultTracker
{
    private readonly m_results: TestResult[]                          = [];
    private readonly m_resultLookup: { [testId: string]: TestResult } = {};

    private m_recorder: PuppeteerScreenRecorder;

    private m_finished: boolean;
    private m_testFinishing: { [id: string]: Future<void> } = {};

    private m_outputId = new Date().valueOf();

    constructor(private outputDirectory,
                private recordTests: boolean,
                testCases: TestCase[])
    {
        for (let testCase of testCases)
        {
            const testResult = {
                id         : testCase.id,
                name       : testCase.name,
                message    : "",
                status     : TestResultStatus.Pending,
                logs       : [],
                consoleLogs: [],
                testStart  : null,
                testEnd    : null,
                videoId    : `${this.m_outputId}-${testCase.id}`
            };
            this.m_results.push(testResult);
            this.m_resultLookup[testResult.id] = testResult;
        }
    }

    public logInfo(text: string,
                   testId?: string): void
    {
        console.log(text);
        this.addToTestLog(" INFO", text, testId);
    }

    public logError(text: string,
                    testId?: string): void
    {
        console.error(text);
        this.addToTestLog("ERROR", text, testId);
    }

    public logDebug(text: string,
                    testId?: string): void
    {
        this.addToTestLog("DEBUG", text, testId);
    }

    public async logConsole(message: ConsoleMessage,
                            testId: string): Promise<void>
    {
        // Evaluate arguments in the page context, to get the real errors
        const args             = message.args();
        const argumentPromises = args.map(arg => arg.executionContext()
                                                    .evaluate((arg2) =>
                                                              {
                                                                  if (arg2 instanceof Error)
                                                                  {
                                                                      return arg2.message;
                                                                  }

                                                                  return arg2;
                                                              }, arg));

        const resolvedArgs = await Promise.all(argumentPromises);

        const results = this.m_results.find((r) => r.id === testId);
        if (results)
        {
            for (let arg of resolvedArgs) results.consoleLogs.push(`${new Date().toLocaleTimeString()} - ${message.type()}: ${arg}`);
        }
        else
        {
            switch (message.type())
            {
                case "error":
                    for (let arg of resolvedArgs) console.error(arg);
                    break;
            }
        }
    }

    public getResult(id: string): TestResult
    {
        return this.m_resultLookup[id];
    }

    public getProgress(): TestResult[]
    {
        return this.m_results;
    }

    private addToTestLog(level: string,
                         text: string,
                         testId?: string): void
    {
        if (testId)
        {
            const results = this.m_results.find((r) => r.id === testId);
            if (results)
            {
                results.logs.push(`${TestResult.getRelativeTime(results)} - ${level}: ${text}`);
            }
        }
    }

    public async testStart(event: TestStartEvent,
                           page: Page): Promise<void>
    {
        const result     = this.m_resultLookup[event.testId];
        result.status    = TestResultStatus.Running;
        result.testStart = new Date();

        this.logInfo(`Test "${event.testName}" starting`, event.testId);
        if (this.recordTests)
        {
            const path      = `${this.outputDirectory}/${result.videoId}.mp4`;
            this.m_recorder = new PuppeteerScreenRecorder(page, {fps: 12});
            await this.m_recorder.start(path);
        }
    }

    public testFinished(testId: string,
                        message: string)
    {
        const result = this.m_resultLookup[testId];
        if (!result) return;

        if (result.testEnd) return this.m_testFinishing[testId];
        this.m_testFinishing[testId] = new Future();

        if (result.status === TestResultStatus.Running)
        {
            result.status = message ? TestResultStatus.Failed : TestResultStatus.Passed;
        }
        result.message = message;
        result.testEnd = new Date();

        const duration = result.testEnd.valueOf() - result.testStart.valueOf();

        this.logInfo(`Test "${result.name}" ${result.status.toLocaleLowerCase()}${message ? " with \"" + message + "\"" : ""} (${duration / 1000} seconds)`, testId);

        if (this.recordTests)
        {
            this.m_recorder.stop();
            this.m_recorder = null;
        }

        this.m_testFinishing[testId].resolve();
    }

    public async reportResults(): Promise<boolean>
    {
        if (this.m_finished)
        {
            return;
        }
        this.m_finished = true;

        let numTests    = 0;
        let numFailures = 0;

        for (let result of this.m_results)
        {
            numTests++;
            if (result.status === TestResultStatus.Failed)
            {
                numFailures++;
            }
        }

        fs.writeFileSync(this.getResultPath(), JSON.stringify(this.m_results, null, 2), {});

        this.logInfo(`${numTests - numFailures} / ${numTests} tests passed.`);

        return numFailures === 0;
    }

    private getResultPath(): string
    {
        return `${this.outputDirectory}/results-${new Date().toISOString()}.json`;
    }
}


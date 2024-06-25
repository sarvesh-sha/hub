import {Service} from "@tsed/common";
import * as puppeteer from "puppeteer";
import {TestCase} from "../models/testCase";
import {TestResult} from "../models/testResult";
import {TestResultStatus} from "../models/testResultStatus";
import {TestsRunRequest} from "../models/testsRunRequest";
import {Future} from "../utils/concurrency";
import {ConfigService} from "./config";
import {asEvent, ClickEvent, ClickPointEvent, DataGeneratedEvent, DragEvent, HoverEvent, JitterMouseInitializeEvent, JitterMouseTerminateEvent, MouseMoveEvent, ScrollEvent, TestDefinitionsEvent, TestEvent, TestResultEvent, TestStartEvent, TypeEvent, WheelEvent} from "./events";
import {JitterMouse} from "./jitter-mouse";
import {TestResultTracker} from "./test-result-tracker";

const consolePrefix = "puppeteer-test-event:";


@Service()
export class TestService
{
    private static readonly CANCEL_ERROR = "___CANCELLED____";

    private reporter: TestResultTracker;

    private browser: puppeteer.Browser;
    private page: puppeteer.Page;

    private mouseJitter: JitterMouse;

    private currentTestId: string;
    private testStart: Future<string>;
    private testFinish: Future<boolean>;
    private testsCancelled: boolean;
    private dataGenerated: Future<boolean>;

    constructor(private config: ConfigService)
    {
        const puppeteerOptions = {
            headless         : this.config.tester.headless,
            ignoreHTTPSErrors: true,
            defaultViewport  : {
                width : 1024,
                height: 768
            },
            args             : [
                "--window-size=1024,900",
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",
                "--disable-accelerated-2d-canvas",
                "--no-first-run",
                "--no-zygote",
                "--disable-gpu"
            ]
        };

        puppeteer.launch(puppeteerOptions)
                 .then(async (browser) =>
                       {
                           console.log(`Puppeteer started. ${await browser.version()}`);
                           this.browser = browser;
                       });
    }

    public async initialize(request: TestsRunRequest): Promise<boolean>
    {
        try
        {
            await this.cleanupPage();

            const testCases = request.ids?.map((id) => this.m_testLookup[id]) || [];
            this.reporter   = new TestResultTracker(this.config.tester.outputDirectory, this.config.tester.record, testCases);

            await this.createPage();

            const testUrl = `${request.url}/#/tests`;
            this.reporter.logInfo(`Navigating to ${testUrl}`);

            await this.page.goto(testUrl);
            this.testsCancelled = false;

            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    private m_testLookup: { [testId: string]: TestCase } = {};
    private m_tests: TestCase[];

    public getTests(): TestCase[]
    {
        return this.m_tests;
    }

    public async startTests(request: TestsRunRequest): Promise<TestCase[]>
    {
        if (!this.page)
        {
            return null;
        }

        let testsToRun = [...this.m_tests] || [];
        if (request.ids?.length)
        {
            testsToRun = testsToRun.filter((t) => request.ids.indexOf(t.id) >= 0);
        }

        if (request.categories?.length)
        {
            testsToRun = testsToRun.filter((t) => request.categories.some((c) => (t.categories || []).indexOf(c) >= 0));
        }

        testsToRun.sort((a,
                         b) => a.name.localeCompare(b.name));

        this.run(testsToRun);

        return testsToRun;
    }

    public cancelAll(): boolean
    {
        if (this.currentTestId)
        {
            this.testsCancelled = true;
            this.cancelCurrentTest();

            return true;
        }

        return false;
    }

    public cancel(id: string): boolean
    {
        const result = this.reporter.getResult(id);
        if (result && !TestResult.isComplete(result))
        {
            if (this.currentTestId === id)
            {
                this.cancelCurrentTest();
            }
            else
            {
                result.status = TestResultStatus.Cancelled;
            }

            return true;
        }

        return false;
    }

    private cancelCurrentTest()
    {
        this.testStart?.reject(TestService.CANCEL_ERROR);
        this.testFinish?.reject(TestService.CANCEL_ERROR);
    }

    public async runAll(url: string): Promise<boolean>
    {
        try
        {
            const allTests = this.getTests();
            await this.initialize({
                                      url: url,
                                      ids: allTests.map((test) => test.id)
                                  });
            await this.generateData();
            this.run(allTests);
            return true;
        }
        catch (e)
        {
            console.error("Failure while running all tests");
        }

        await this.cleanupPage();
        return false;
    }

    public getProgress(): TestResult[]
    {
        return this.reporter.getProgress();
    }

    private async createPage()
    {
        if (!this.browser)
        {
            return;
        }

        this.page = await this.browser.newPage();
        this.page.on("console", (event: puppeteer.ConsoleMessage) =>
        {
            const text = event.text();
            if (text.startsWith(consolePrefix))
            {
                let event = JSON.parse(text.substr(consolePrefix.length));
                this.handleEvent(event);
            }
            else
            {
                this.reporter.logConsole(event, this.currentTestId);
            }
        });
    }

    private async handleEvent(event: TestEvent)
    {
        let result = false;
        try
        {
            switch (event.type)
            {
                case "testDefinitions":
                    result = await this.handleTestDefinitionsEvent(asEvent("testDefinitions", event));
                    break;

                case "testStart":
                    result = await this.handleTestStartEvent(asEvent("testStart", event));
                    break;

                case "testResult":
                    result = await this.handleTestResultEvent(asEvent("testResult", event));
                    break;

                case "click":
                    result = await this.handleClickEvent(asEvent("click", event));
                    break;

                case "clickPoint":
                    result = await this.handleClickPointEvent(asEvent("clickPoint", event));
                    break;

                case "drag":
                    result = await this.handleDragEvent(asEvent("drag", event));
                    break;

                case "hover":
                    result = await this.handleHoverEvent(asEvent("hover", event));
                    break;

                case "jitterMouseInitialize":
                    result = await this.handleJitterMouseInitializeEvent(asEvent("jitterMouseInitialize", event));
                    break;

                case "jitterMouseTerminate":
                    result = await this.handleJitterMouseTerminateEvent(asEvent("jitterMouseTerminate", event));
                    break;

                case "mouseMove":
                    result = await this.handleMouseMoveEvent(asEvent("mouseMove", event));
                    break;

                case "scroll":
                    result = await this.handleScrollEvent(asEvent("scroll", event));
                    break;

                case "type":
                    result = await this.handleTypeEvent(asEvent("type", event));
                    break;

                case "wheel":
                    result = await this.handleWheelEvent(asEvent("wheel", event));
                    break;

                case "dataGenerated":
                    result = this.handleDataGenerated(asEvent("dataGenerated", event));
                    break;
            }
        }
        catch (e)
        {
            result = false;
        }

        await this.respondToEvent(event.eventId, result);
    }

    private async handleClickEvent(event: ClickEvent): Promise<boolean>
    {
        let element = this.getElementString(event.elementName, event.selector);
        this.reporter.logDebug(`Clicking on ${element}`, this.currentTestId);
        let success = true;
        try
        {
            await this.page.click(event.selector, {clickCount: event.numClicks});
        }
        catch (err)
        {
            this.reporter.logError(`Failed to click on ${element}. ${err}`, this.currentTestId);
            success = false;
        }

        return success;
    }

    private async handleClickPointEvent(event: ClickPointEvent): Promise<boolean>
    {
        let pointDescription = this.getElementString(event.pointDescription);
        this.reporter.logDebug(`Clicking on "${pointDescription}"`, this.currentTestId);
        let success = true;
        try
        {
            await this.page.mouse.click(event.point.x, event.point.y);
        }
        catch (err)
        {
            this.reporter.logError(`Failed to click "${pointDescription}". ${err}`, this.currentTestId);
            success = false;
        }

        return success;
    }

    private async handleMouseMoveEvent(event: MouseMoveEvent): Promise<boolean>
    {
        let pointDescription = this.getElementString(event.pointDescription);
        this.reporter.logDebug(`Mousing over to "${pointDescription}"`, this.currentTestId);
        let success = true;
        try
        {
            await this.page.mouse.move(event.point.x, event.point.y);
        }
        catch (err)
        {
            this.reporter.logError(`Failed to mouse over "${pointDescription}". ${err}`, this.currentTestId);
            success = false;
        }

        return success;
    }

    private async handleJitterMouseInitializeEvent(event: JitterMouseInitializeEvent): Promise<boolean>
    {
        this.reporter.logDebug("Turning on mouse jittering", this.currentTestId);
        let success = true;
        try
        {
            if (this.mouseJitter) throw Error("Mouse jittering is already active");

            this.mouseJitter = new JitterMouse(this.page, this.reporter, this.currentTestId, event);
        }
        catch (err)
        {
            this.reporter.logError(`Mouse jitter initialization failed. ${err}`, this.currentTestId);
            success = false;
        }

        return success;
    }

    private async handleJitterMouseTerminateEvent(event: JitterMouseTerminateEvent): Promise<boolean>
    {
        this.reporter.logDebug("Turning off mouse jittering", this.currentTestId);
        let success = true;
        try
        {
            if (!this.mouseJitter) throw Error("No mouse jittering active");

            this.terminateMouseJitter();
        }
        catch (err)
        {
            this.reporter.logError(`Mouse jitter termination failed. ${err}`, this.currentTestId);
            success = false;
        }

        return success;
    }

    private terminateMouseJitter()
    {
        if (this.mouseJitter)
        {
            this.mouseJitter.terminate();
            this.mouseJitter = null;
        }
    }

    private async handleDragEvent(event: DragEvent): Promise<boolean>
    {
        let element = this.getElementString(event.elementName, event.selector);
        this.reporter.logDebug(`Click and dragging ${element ? "on " + element : "from (" + event.source.x + ", " + event.source.y + ")"}`, this.currentTestId);
        let success = true;
        try
        {
            if (event.selector)
            {
                await this.page.hover(event.selector);
            }
            else
            {
                await this.page.mouse.move(event.source.x, event.source.y);
            }

            await this.page.mouse.down();
            await Future.delayed(50);
            await this.page.mouse.move(event.destination.x, event.destination.y, {steps: 3});
            await Future.delayed(50);
            if (event.release) await this.page.mouse.up();
        }
        catch (err)
        {
            this.reporter.logError(`Failed to click and drag on ${event.selector}. ${err}`, this.currentTestId);
            success = false;
        }

        return success;
    }

    private async handleHoverEvent(event: HoverEvent): Promise<boolean>
    {
        let element = this.getElementString(event.elementName, event.selector);
        this.reporter.logDebug(`Scrolling to and then hovering over ${element}`, this.currentTestId);
        let success = true;
        try
        {
            await this.page.hover(event.selector);
        }
        catch (err)
        {
            this.reporter.logError(`Failed to scroll to and then hover over ${element}`);
            success = false;
        }

        return success;
    }

    private async handleScrollEvent(event: ScrollEvent): Promise<boolean>
    {
        let success = true;
        if (event.dy != 0)
        {
            this.reporter.logDebug(`Scrolling ${event.dy < 0 ? "up" : "down"} ${Math.abs(event.dy)}px`, this.currentTestId);
            try
            {
                await this.page.evaluate((event) =>
                                         {
                                             const scrollContainer     = document.querySelector(event.selector || ".o3-standard-layout--scroll-container");
                                             scrollContainer.scrollTop = Math.max(Math.min(scrollContainer.scrollTop + event.dy, scrollContainer.scrollHeight), 0);
                                         }, <any>event);
            }
            catch (err)
            {
                this.reporter.logError("Failed to scroll");
                success = false;
            }
        }
        return success;
    }

    private async handleTypeEvent(event: TypeEvent): Promise<boolean>
    {
        let logMsg  = event.text ? `Typing "${event.text}"` : "Pressing " + event.keyEvents;
        let element = this.getElementString(event.elementName, event.selector);
        if (element) logMsg += ` into ${element}`;
        this.reporter.logDebug(logMsg, this.currentTestId);

        let success = true;
        try
        {
            if (event.text)
            {
                if (event.overwrite)
                {
                    await this.page.click(event.selector);
                    await Future.delayed(600);
                    await this.page.click(event.selector, {clickCount: 3});
                    await this.page.keyboard.press("Backspace");
                }

                await this.page.type(event.selector, event.text);
            }
            else if (event.keyEvents?.length)
            {
                if (event.selector) await this.page.click(event.selector);

                for (let keyEvent of event.keyEvents)
                {
                    switch (keyEvent.type)
                    {
                        case "down":
                            await this.page.keyboard.down(keyEvent.key);
                            break;

                        case "up":
                            await this.page.keyboard.up(keyEvent.key);
                            break;

                        case "press":
                            await this.page.keyboard.press(keyEvent.key);
                            break;
                    }
                }
            }
        }
        catch (err)
        {
            let errorMsg = "Failed to type";
            if (element) errorMsg += ` into ${element}`;
            errorMsg += ". " + err;
            this.reporter.logError(errorMsg, this.currentTestId);
            success = false;
        }

        return success;
    }

    private async handleWheelEvent(event: WheelEvent): Promise<boolean>
    {
        let success = true;
        if (event.dx || event.dy)
        {
            this.reporter.logDebug(`Processing wheel event with vector (${event.dx}, ${event.dy})`, this.currentTestId);
            try
            {
                await this.page.mouse.wheel({
                                                deltaX: event.dx,
                                                deltaY: event.dy
                                            });
            }
            catch (err)
            {
                this.reporter.logError("Failed to process wheel event");
                success = false;
            }
        }
        return success;
    }

    private async handleTestStartEvent(event: TestStartEvent): Promise<boolean>
    {
        await this.reporter.testStart(event, this.page);

        if (this.testStart)
        {
            this.testStart.resolve(event.testId);
        }

        return true;
    }

    private async handleTestResultEvent(event: TestResultEvent): Promise<boolean>
    {
        if (this.currentTestId === event.testId)
        {
            await this.reporter.testFinished(event.testId, event.failure);

            if (this.testFinish)
            {
                this.testFinish.resolve(!event.failure);
            }

            return true;
        }

        return false;
    }

    private async generateData(): Promise<boolean>
    {
        try
        {
            console.log("Generating test data");
            this.dataGenerated = new Future<boolean>();

            await this.sendEvent({type: "generateData"});
            let success = await this.dataGenerated;

            if (success)
            {
                console.log("Successfully generated data.");
                return true;
            }
            else
            {
                console.error("Failed to generate data.");
            }
        }
        catch (e)
        {
        }
        finally
        {
            this.dataGenerated = null;
        }

        throw Error("Failed to generate data.");
    }

    private handleDataGenerated(event: DataGeneratedEvent): boolean
    {
        this.dataGenerated.resolve(event.success);
        return event.success;
    }

    private async handleTestDefinitionsEvent(event: TestDefinitionsEvent): Promise<boolean>
    {
        this.m_tests      = [];
        this.m_testLookup = {};

        for (let test of event.tests)
        {
            const testCase: TestCase = {
                id        : test.testId,
                name      : test.testName,
                timeout   : test.testTimeout,
                categories: test.categories
            };

            this.m_tests.push(testCase);
            this.m_testLookup[testCase.id] = testCase;
        }

        return true;
    }

    private async respondToEvent(eventId: string,
                                 success: boolean)
    {
        return this.sendEvent({
                                  type   : "ack",
                                  eventId: eventId,
                                  success: success
                              });
    }

    private async sendEvent<T extends TestEvent>(event: T)
    {
        await this.page.evaluate((event) =>
                                 {
                                     const anyWindow = <any>window;
                                     anyWindow.o3TestEvent.next(event);
                                 }, <any>event);
    }

    private async run(tests: TestCase[])
    {
        for (let test of tests)
        {
            const result = this.reporter.getResult(test.id);
            if (this.testsCancelled || TestResult.isComplete(result))
            {
                result.status = TestResultStatus.Cancelled;
                continue;
            }

            this.currentTestId = test.id;
            let error: string;
            try
            {
                this.testFinish = new Future<boolean>();
                this.testFinish.setCancellationTimeout(test.timeout * 1000 || 30000, "Test timeout.");

                this.testStart = new Future<string>();
                this.testStart.setCancellationTimeout(10000, "Failed to start test.");

                await this.sendEvent({
                                         type  : "testStart",
                                         testId: test.id
                                     });

                await this.testStart;
                await this.testFinish;
            }
            catch (e)
            {
                error = e?.toString();

                if (error === TestService.CANCEL_ERROR)
                {
                    error = "";

                    if (result.status === TestResultStatus.Running) result.status = TestResultStatus.Cancelled;
                }
            }
            finally
            {
                this.testStart     = null;
                this.testFinish    = null;
                this.currentTestId = null;
                this.terminateMouseJitter();
                this.reporter.testFinished(result.id, error);
            }
        }

        const success = await this.reporter.reportResults();

        await this.cleanupPage();
    }

    public async cleanupPage()
    {
        if (this.page)
        {
            await this.page.close({runBeforeUnload: true});
            this.page = null;
        }
    }

    private getElementString(elementName: string,
                             selector?: string): string
    {
        let result = "";
        if (elementName)
        {
            result = `"${elementName}" `;
        }

        if (selector)
        {
            result = `${result}(${selector})`;
        }

        return result.trim();
    }
}
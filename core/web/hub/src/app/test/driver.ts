import {ComponentType} from "@angular/cdk/portal";
import {ElementRef, Injectable, Injector, NgZone, Type} from "@angular/core";
import {ChildrenOutletContexts, NavigationExtras, Router} from "@angular/router";
import {UUID} from "angular2-uuid";

import {AppContext} from "app/app.service";
import {StandardLayoutComponent} from "app/layouts/standard-layout.component";
import {GatewayExtended} from "app/services/domain/assets.service";

import * as Models from "app/services/proxy/model/models";
import {Test} from "app/test/base-tests";
import {ClickEvent, ClickPointEvent, DragEvent, HoverEvent, isEvent, JitterMouseInitializeEvent, JitterMouseTerminateEvent, KeyEvent, KeyInput, MouseMoveEvent, OPTIO3_ATTRIBUTE, Point, ScrollEvent, TestEvent, TestEventAck, TestStartEvent, TypeEvent, WheelEvent} from "app/test/events";

import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {Future} from "framework/utils/concurrency";

import {Subject} from "rxjs";

export class TestDescriptor
{
    timeout: number;

    constructor(public id: string,
                public name: string,
                public categories: string[],
                public test: new(driver: TestDriver) => Test)
    {}
}

interface EventHandle
{
    eventId: string;
    response: Future<boolean>;
    timer: number;
}

@Injectable({providedIn: "root"})
export class TestDriver
{
    private static readonly s_tests: TestDescriptor[] = [];
    private static readonly s_gatewayName             = "TEST-GENERATED-GATEWAY";

    static TestCase(config: TestConfig): any
    {
        return function (definition: Type<Test>)
        {
            let desc = new TestDescriptor(config.id, config.name, config.categories, definition);
            if (config.timeout) desc.timeout = config.timeout;

            TestDriver.s_tests.push(desc);

            definition.prototype.m_descriptor = desc;

            return definition;
        };
    }

    private m_events = new Map<string, EventHandle>();

    constructor(private inj: Injector,
                private navigation: AppNavigationService,
                private router: Router,
                private ngZone: NgZone,
                public app: AppContext)
    {
        const testEventResponse = new Subject<TestEvent>();

        const anyWindow       = <any>window;
        anyWindow.o3TestEvent = testEventResponse;

        testEventResponse.subscribe((response) => this.onEvent(response));

        let tests = this.getTests();

        this.sendEvent({
                           type : "testDefinitions",
                           tests: tests.map((t) =>
                                            {
                                                return {
                                                    testId     : t.id,
                                                    testName   : t.name,
                                                    testTimeout: t.timeout,
                                                    categories : t.categories
                                                };
                                            })
                       });
    }

    async getStandardLayoutComponent(): Promise<StandardLayoutComponent>
    {
        let standardLayout;
        await UtilsService.executeWithRetries(async () =>
                                              {
                                                  let anyRouter  = <any>this.router;
                                                  standardLayout = anyRouter.rootContexts.getContext("primary")?.outlet.activated.instance?.standardLayout;

                                                  return !!standardLayout;
                                              }, 5, 2000, () => { throw new Error("Unable to find standard layout component"); });

        return standardLayout;
    }

    async getComponents<T>(type: ComponentType<T>,
                           timeout: number = 2000,
                           retries: number = 5): Promise<T[]>
    {
        return this.getComponentsHelper(type, timeout, retries);
    }

    async getComponent<T>(type: ComponentType<T>,
                          timeout: number = 2000,
                          retries: number = 5): Promise<T>
    {
        let components = await this.getComponentsHelper(type, timeout, retries, true);
        return components[0];
    }

    private async getComponentsHelper<T>(type: ComponentType<T>,
                                         timeout: number,
                                         retries: number,
                                         onlyFirst?: boolean): Promise<T[]>
    {
        let anyRouter = <any>this.router;

        while (retries > 0)
        {
            let components = this.findComponents(type, anyRouter.rootContexts, onlyFirst);
            if (components.length) return components;

            if (onlyFirst)
            {
                let component = OverlayComponent.findDialogComponent(type);
                if (component) return [component];
            }
            else
            {
                components = OverlayComponent.findDialogComponents(type);
                if (components) return components;
            }

            await Future.delayed(timeout);
            retries--;
        }

        throw new Error("Unable to find component of type " + type.name);
    }

    private findComponents<T>(type: ComponentType<T>,
                              contexts: ChildrenOutletContexts,
                              returnWithFirst?: boolean,
                              components?: T[]): T[]
    {
        if (!components) components = [];

        let outletContext: any = contexts?.getContext("primary");
        if (outletContext)
        {
            let instance = outletContext.outlet?.activated?.instance;
            if (instance instanceof type)
            {
                components.push(instance);
                if (returnWithFirst) return components;
            }

            return this.findComponents(type, outletContext.children, returnWithFirst, components);
        }

        return components;
    }

    getTests(): TestDescriptor[]
    {
        return [...TestDriver.s_tests];
    }

    async navigate<T>(type: ComponentType<T>,
                      url: string,
                      segments?: any[],
                      params?: { param: string, value: any }[],
                      extras?: NavigationExtras): Promise<T>
    {
        await this.navigation.go("/tests");
        await this.navigation.go(url, segments, params, extras);
        return this.getComponent(type);
    }

    async generateSimulatedData()
    {
        let success = false;
        try
        {
            await this.app.domain.users.login("admin@demo.optio3.com", "adminPwd");
            let gateways = await this.app.domain.assets.getTypedExtendedAll(GatewayExtended, new Models.GatewayFilterRequest());
            let gateway  = gateways.find((g) => g.model.name === TestDriver.s_gatewayName);
            if (gateway)
            {
                // already generated data
                success = true;
            }
            else
            {
                let gatewayId = await this.app.domain.apis.demoTasks.createGateway(TestDriver.s_gatewayName, 3);

                success = await UtilsService.executeWithRetries(() => this.app.domain.apis.demoTasks.checkGatewayProgress(gatewayId), 180, 1000);
            }
        }
        catch (e)
        {
            console.error("Failed to generate simulated data. " + e.message);
        }

        await this.sendEvent({
                                 type   : "dataGenerated",
                                 success: success
                             });
    }

    async runTest(test: TestDescriptor)
    {
        let failure        = "";
        const testInstance = new test.test(this);
        try
        {
            await this.sendEvent({
                                     type    : "testStart",
                                     testId  : test.id,
                                     testName: test.name
                                 });

            failure = await this.runSafe(() => testInstance.init(), "init");
            if (!failure)
            {
                failure            = await this.runSafe(() => testInstance.execute(), "execute");
                let cleanupFailure = await this.runSafe(() => testInstance.cleanup(), "cleanup");

                if (cleanupFailure)
                {
                    failure += `\n${cleanupFailure}`;
                }
            }
        }
        catch (e)
        {
            failure = e.toString();
        }

        await this.sendEvent({
                                 type    : "testResult",
                                 testId  : test.id,
                                 testName: test.name,
                                 failure : failure
                             });
    }

    private async runSafe(callback: () => Promise<any>,
                          phase: string): Promise<string>
    {
        try
        {
            await this.ngZone.run(callback);
        }
        catch (e)
        {
            return `Failure during ${phase}: ${e.toString()}`;
        }

        return "";
    }

    private clickEvent(element: ElementRef | HTMLElement,
                       elementName: string,
                       numClicks: number): ClickEvent
    {
        return {
            type       : "click",
            selector   : this.getO3Selector(this.setTestId(element)),
            numClicks  : numClicks,
            elementName: elementName
        };
    }

    async click(element: ElementRef<HTMLElement> | HTMLElement,
                elementName: string,
                numClicks: number = 1): Promise<void>
    {
        if (!await this.sendEvent(this.clickEvent(element, elementName, numClicks)))
        {
            if (element instanceof ElementRef) element = element.nativeElement;
            throw Error(`Failed to click on "${element?.getAttribute(OPTIO3_ATTRIBUTE)}"`);
        }
    }

    async clickPoint(point: Point,
                     pointDescription: string): Promise<void>
    {
        const clickPoint: ClickPointEvent = {
            type            : "clickPoint",
            point           : point,
            pointDescription: pointDescription
        };

        if (!await this.sendEvent(clickPoint))
        {
            throw Error(`Failed to click "${pointDescription}"`);
        }
    }

    async mouseMove(point: Point,
                    pointDescription: string): Promise<void>
    {
        const mouseMovePoint: MouseMoveEvent = {
            type            : "mouseMove",
            point           : point,
            pointDescription: pointDescription
        };

        if (!await this.sendEvent(mouseMovePoint))
        {
            throw Error(`Failed to move to "${pointDescription}"`);
        }
    }

    async mouseWheel(dx: number,
                     dy: number): Promise<void>
    {
        const mouseWheel: WheelEvent = {
            type: "wheel",
            dx  : dx || 0,
            dy  : dy || 0
        };

        if (!await this.sendEvent(mouseWheel))
        {
            throw Error(`Failed to turn wheel for vector (${mouseWheel.dx}, ${mouseWheel.dy})`);
        }
    }

    async jitterMouseUntil(jitterUntilFn: () => Promise<void>,
                           x: number,
                           y: number,
                           timeout: number)
    {
        const initializeEvent: JitterMouseInitializeEvent = {
            type    : "jitterMouseInitialize",
            x       : x,
            y       : y,
            interval: timeout
        };
        if (!await this.sendEvent(initializeEvent))
        {
            throw Error("Failed to initialize mouse jittering");
        }

        try
        {
            await jitterUntilFn();
        }
        finally
        {
            const terminationEvent: JitterMouseTerminateEvent = {type: "jitterMouseTerminate"};
            if (!await this.sendEvent(terminationEvent))
            {
                throw Error("Failed to terminate mouse jittering");
            }
        }
    }

    async clickAndDrag(source: Point | HTMLElement,
                       destination: Point | HTMLElement,
                       elementName?: string,
                       noRelease?: boolean): Promise<void>
    {
        if (destination instanceof HTMLElement) destination = getCenterPoint(destination);

        let dragEvent: DragEvent = {
            type       : "drag",
            destination: destination,
            release    : !noRelease,
            elementName: elementName
        };

        let errorMessage = "Failed to click and drag";
        if (source instanceof HTMLElement)
        {
            dragEvent.selector = this.getO3Selector(this.setTestId(source));
            errorMessage += " " + source.getAttribute(OPTIO3_ATTRIBUTE);
        }
        else
        {
            dragEvent.source = source;
        }

        if (!await this.sendEvent(dragEvent))
        {
            throw Error(errorMessage);
        }
    }

    // scrolls element into view and then hovers mouse over
    async hover(element: ElementRef<HTMLElement> | HTMLElement,
                elementName: string): Promise<void>
    {
        let hoverEvent: HoverEvent = {
            type       : "hover",
            selector   : this.getO3Selector(this.setTestId(element)),
            elementName: elementName
        };

        if (!await this.sendEvent(hoverEvent))
        {
            if (element instanceof ElementRef) element = element.nativeElement;
            throw Error(`Failed to scroll to and then hover over ${element?.getAttribute(OPTIO3_ATTRIBUTE)}`);
        }
    }

    async scroll(dy: number,
                 element?: ElementRef<HTMLElement> | HTMLElement): Promise<void>
    {
        const event: ScrollEvent = {
            type: "scroll",
            dy  : dy
        };
        if (element) event.selector = this.getO3Selector(this.setTestId(element));

        if (!await this.sendEvent(event))
        {
            throw Error("Failed to scroll");
        }
    }

    async clickO3Element(o3TestId: string,
                         elementName: string): Promise<void>
    {
        await this.click(<HTMLElement>document.querySelector(this.getO3Selector(o3TestId)), elementName);
    }

    getO3Selector(o3TestId: string): string
    {
        return `[${OPTIO3_ATTRIBUTE}="${o3TestId}"]`;
    }

    private setTestId(element: ElementRef | HTMLElement): string
    {
        if (!element) return null;

        const htmlElement: HTMLElement = element instanceof ElementRef ? element.nativeElement : element;
        let id                         = htmlElement.getAttribute(OPTIO3_ATTRIBUTE);
        if (!id)
        {
            id = UUID.UUID();
            htmlElement.setAttribute(OPTIO3_ATTRIBUTE, id);
        }

        return id;
    }

    async sendText(element: ElementRef,
                   elementName: string,
                   text: string,
                   overwrite?: boolean): Promise<void>
    {
        const id    = this.setTestId(element);
        let success = await this.sendEvent({
                                               type       : "type",
                                               selector   : this.getO3Selector(id),
                                               text       : text,
                                               overwrite  : !!overwrite,
                                               elementName: elementName
                                           });

        if (!success)
        {
            throw Error(`Failed to type in "${id}"`);
        }
    }

    async sendKeysExplicit(keyEvents: KeyEvent[],
                           element?: ElementRef,
                           elementName?: string): Promise<void>
    {
        let event: TypeEvent = {
            type       : "type",
            keyEvents  : keyEvents,
            elementName: elementName
        };
        if (element) event.selector = this.getO3Selector(this.setTestId(element));

        let success = await this.sendEvent(event);
        if (!success)
        {
            let errorMsg = `Failed to type the key sequence "${event.keyEvents}"`;
            if (event.selector) errorMsg += ` to "${event.selector}"`;
            throw Error(errorMsg);
        }
    }

    async sendKeys(keys: KeyInput[],
                   modifiers?: KeyInput[],
                   element?: ElementRef,
                   elementName?: string): Promise<void>
    {
        let keyEvents: KeyEvent[] = [];

        const appendKey = (key: KeyInput,
                           type: "down" | "up" | "press") =>
        {
            keyEvents.push({
                               type: type,
                               key : key
                           });
        };

        for (let modifier of modifiers || []) appendKey(modifier, "down");
        for (let key of keys) appendKey(key, "press");
        for (let modifier of modifiers || []) appendKey(modifier, "up");

        await this.sendKeysExplicit(keyEvents, element, elementName);
    }

    private sendEvent<T extends TestEvent>(event: T,
                                           timeout = 30000): Promise<boolean>
    {
        const response            = new Future<boolean>();
        const eventId             = UUID.UUID();
        event.eventId             = eventId;
        const handle: EventHandle = {
            eventId : eventId,
            response: response,
            timer   : setTimeout(() =>
                                 {
                                     response.reject(`${event.type} event timed out.`);
                                     this.m_events.delete(eventId);
                                 }, timeout)
        };

        this.m_events.set(eventId, handle);
        console.log(`puppeteer-test-event:${JSON.stringify(event)}`);
        return response;
    }

    private onEvent(event: TestEvent)
    {
        if (isEvent("ack", event))
        {
            this.onAck(event);
        }
        else if (isEvent("testStart", event))
        {
            this.onStart(event);
        }
        else if (isEvent("generateData", event))
        {
            this.generateSimulatedData();
        }
    }

    private onAck(response: TestEventAck)
    {
        const handle = this.m_events.get(response.eventId);
        if (handle)
        {
            clearTimeout(handle.timer);
            handle.response.resolve(response.success);
        }
    }

    private onStart(start: TestStartEvent)
    {
        const test = TestDriver.s_tests.find((t) => t.id === start.testId);
        if (test)
        {
            this.runTest(test);
        }
    }

    async getComponentValue<T extends BaseComponent, U>(parentComponent: T,
                                                        getValueFn: (component: T) => U,
                                                        valueName: string): Promise<U>
    {
        return waitFor(() =>
                       {
                           let item = getValueFn(parentComponent);
                           if (!item) parentComponent.markForCheck();
                           return item;
                       }, `Could not get ${valueName}`, 200, 9);
    }

    async waitForStabilization(element: HTMLElement,
                               elementName: string)
    {
        let previousBoundingBox: DOMRect;
        await waitFor(() =>
                      {
                          let rect            = element.getBoundingClientRect();
                          const isSame        = rect.x === previousBoundingBox?.x && rect.y === previousBoundingBox.y;
                          previousBoundingBox = rect;
                          return isSame && !isNaN(rect.x) && !isNaN(rect.y);
                      }, elementName + " did not stabilize its position", 100, 9);
    }

    getDriver<T>(driverClass: Type<T>): T
    {
        return this.inj.get(driverClass);
    }
}

export function querySelectorHelper(query: string,
                                    identifyFn: (elem: HTMLElement) => boolean,
                                    queryRoot?: ParentNode): HTMLElement
{
    if (!queryRoot) queryRoot = document;

    let matchedElements = <NodeListOf<HTMLElement>>queryRoot.querySelectorAll(query);
    let found: HTMLElement;
    matchedElements.forEach((elem) =>
                            {
                                if (!found && identifyFn(elem)) found = elem;
                            });
    return found;
}

export function getCenterPoint(element: HTMLElement): Point
{
    if (!element) return null;

    let boundingRect = element.getBoundingClientRect();
    return {
        x: (boundingRect.left + boundingRect.right) / 2,
        y: (boundingRect.top + boundingRect.bottom) / 2
    };
}

export async function waitFor<T>(callback: () => T,
                                 failureMessage: string,
                                 timeout: number = 2000,
                                 retries: number = 4): Promise<T>
{
    while (retries >= 0)
    {
        try
        {
            let value = await callback();
            if (value) return value;
        }
        catch (e)
        {
        }

        await Future.delayed(timeout);
        retries--;
    }

    throw Error(failureMessage);
}

export function assertIsDefined<T>(val: T,
                                   valName: string): asserts val is NonNullable<T>
{
    assertTrue(val !== null && val !== undefined, `Expected ${valName} to be defined, but received ${val}`);
}

export function assertTrue(condition: boolean,
                           error: string)
{
    if (!condition) throw Error(error);
}

export function areEqual(a: number,
                         b: number,
                         allowedDelta: number): boolean
{
    return Math.abs(a - b) <= allowedDelta;
}

interface TestConfig
{
    id: string;

    name: string;

    timeout?: number;

    categories: string[];
}

export const TestCase = TestDriver.TestCase;

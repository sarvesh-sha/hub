import {Component, ElementRef, Injector, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

import {Future} from "framework/utils/concurrency";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";
import moment from "framework/utils/moment";

@Component({
               selector   : "o3-provision-page",
               templateUrl: "./provision-page.component.html",
               styleUrls  : ["./provision-page.component.scss"]
           })
export class ProvisionPageComponent extends SharedSvc.BaseApplicationComponent
{
    private fakeState = false; // Split for testing the display of different state.

    @ViewChild("screenSaverOverlay", {static: true}) screenSaverOverlay: OverlayComponent;
    @ViewChild("screenSaver", {static: true}) public screenSaverRef: ElementRef;

    private screenSaver: ScreenSaver;
    private screenSaverDeadline: moment.Moment;

    overlayConfig: OverlayConfig;

    //--//

    isProductionMode: boolean;
    isFactoryFloorMode: boolean;
    powerSwitchSupported: boolean;
    relayOn: boolean;

    //--//

    lastCheckin: Date;
    checkins: Models.ProvisionReportExt[] = [];
    checkinSelected: Models.ProvisionReportExt;

    get readyForPrint(): boolean
    {
        return this.checkinSelected && !this.checkinSelected.printed;
    }

    get readyForReprint(): boolean
    {
        return this.checkinSelected && this.checkinSelected.printed;
    }

    //--//

    private m_state = State.Idle;

    get state(): State
    {
        return this.m_state;
    }

    set state(value: State)
    {
        if (this.m_state != value)
        {
            this.logDebug(`Transition from ${this.m_state} to ${value}`);
            if (!this.fakeState) this.m_state = value;
        }
    }

    downloadTotal = 0;
    boardDetected: string;
    flashingInProgress: Models.FlashingProgress;
    testWaiting: string;
    testFailure: string;

    get readyForBoard(): boolean
    {
        return this.state == State.BoardDetected;
    }

    get isDownloadingFirmware(): boolean
    {
        return this.state == State.DownloadFirmware;
    }

    get canTurnPowerOn(): boolean
    {
        if (this.relayOn) return false;

        switch (this.state)
        {
            case State.Idle:
                return true;

            default:
                return false;
        }
    }

    get canTurnPowerOff(): boolean
    {
        if (!this.relayOn) return false;

        switch (this.state)
        {
            case State.Idle:
            case State.BoardDetected:
            case State.BoardRegistered:
            case State.FlashingFailure:
            case State.TestPass:
            case State.TestFailure:
                return true;

            default:
                return false;
        }
    }

    get isFlashing(): boolean
    {
        return this.state == State.Flashing;
    }

    get isWaitingForRegistration(): boolean
    {
        return this.state == State.WaitingForRegistration;
    }

    get hasBoardRegistered(): boolean
    {
        return this.state == State.BoardRegistered;
    }

    get hasFlashingFailed(): boolean
    {
        return this.state == State.FlashingFailure;
    }

    get isWaitingForTests(): boolean
    {
        return this.state == State.WaitingForTests;
    }

    get hasTestPassed(): boolean
    {
        return this.state == State.TestPass;
    }

    get hasTestFailed(): boolean
    {
        return this.state == State.TestFailure;
    }

    //--//

    public get checkinsPrinted(): Models.ProvisionReportExt[]
    {
        return this.checkinsPrintedImpl();
    }

    @Memoizer
    private checkinsPrintedImpl(): Models.ProvisionReportExt[]
    {
        return ProvisionPageComponent.sortCheckins(this.checkins.filter((checkin) => checkin.printed));
    }

    public get checkinsNotPrinted(): Models.ProvisionReportExt[]
    {
        return this.checkinsNotPrintedImpl();
    }

    @Memoizer
    private checkinsNotPrintedImpl(): Models.ProvisionReportExt[]
    {
        return ProvisionPageComponent.sortCheckins(this.checkins.filter((checkin) => !checkin.printed));
    }

    static sortCheckins(array: Models.ProvisionReportExt[]): Models.ProvisionReportExt[]
    {
        return array.sort((a,
                           b) => MomentHelper.compare(MomentHelper.parse(a.info.timestamp), MomentHelper.parse(b.info.timestamp)));
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.overlayConfig                     = new OverlayConfig();
        this.overlayConfig.closableViaBackdrop = false;
        this.overlayConfig.width               = "100%";
        this.overlayConfig.height              = "100%";

        this.stopScreenSaver();
    }

    ngAfterViewInit()
    {
        this.initialize();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.stopScreenSaver();
    }

    async initialize()
    {
        this.isProductionMode     = await this.app.domain.apis.adminTasks.isProductionMode();
        this.isFactoryFloorMode   = await this.app.domain.apis.adminTasks.isFactoryFloorMode();
        this.powerSwitchSupported = await this.app.domain.apis.provision.powerSwitchSupported() == "Supported";

        await this.logInfo(`this.isProductionMode = ${this.isProductionMode}`);
        await this.logInfo(`this.isFactoryFloorMode = ${this.isFactoryFloorMode}`);
        await this.logInfo(`this.powerSwitchSupported = ${this.powerSwitchSupported}`);

        this.loopForCheckins();
        this.loopForCard();
    }

    async loopForCheckins()
    {
        while (!this.wasDestroyed())
        {
            try
            {
                let checkins = await this.app.domain.apis.provision.getNewCheckins(this.lastCheckin);
                for (let checkin of checkins)
                {
                    await this.logDebug(`Checkin detected: ${JSON.stringify(checkin)}`);

                    this.addNewCheckin(checkin);

                    this.stopScreenSaver();

                    if (this.boardDetected && checkin.info?.boardSerialNumber == this.boardDetected)
                    {
                        if (this.isWaitingForRegistration)
                        {
                            this.state = State.BoardRegistered;
                        }
                        else if (this.isWaitingForTests)
                        {
                            this.state = State.TestPass;

                            for (let test of checkin.info?.tests || [])
                            {
                                if (test.result == Models.ProvisionTestResult.Failed)
                                {
                                    this.state       = State.TestFailure;
                                    this.testFailure = `Test ${test.name} failed!!`;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            catch (e)
            {
                //
            }

            await Future.delayed(1000);

            if (this.fakeState)
            {
                await Future.delayed(4000);

                switch (this.m_state)
                {
                    case State.Idle:
                        this.m_state       = State.DownloadFirmware;
                        this.downloadTotal = 300000;
                        break;

                    case State.DownloadFirmware:
                        this.m_state       = State.BoardDetected;
                        this.downloadTotal = 200_000_000;
                        break;

                    case State.BoardDetected:
                        this.m_state                        = State.Flashing;
                        this.flashingInProgress             = new Models.FlashingProgress();
                        this.flashingInProgress.phase       = 3;
                        this.flashingInProgress.phaseName   = "rootfs";
                        this.flashingInProgress.imageOffset = 23_000_000;
                        this.flashingInProgress.imageSize   = 100_000_000;
                        break;

                    case State.Flashing:
                        this.m_state = State.FlashingFailure;
                        break;

                    case State.FlashingFailure:
                        this.m_state     = State.WaitingForRegistration;
                        this.testWaiting = `(timeout in 216 seconds)`;
                        break;

                    case State.WaitingForRegistration:
                        this.m_state = State.BoardRegistered;
                        break;

                    case State.BoardRegistered:
                        this.m_state     = State.WaitingForTests;
                        this.testWaiting = `(timeout in 216 seconds)`;
                        break;

                    case State.WaitingForTests:
                        this.m_state = State.TestPass;
                        break;

                    case State.TestPass:
                        this.m_state     = State.TestFailure;
                        this.testFailure = "Foo";
                        break;

                    case State.TestFailure:
                        this.m_state = State.Idle;
                        break;
                }
            }

            this.startScreenSaverIfIdle();
        }
    }

    async loopForCard()
    {
        while (!this.wasDestroyed())
        {
            try
            {
                switch (this.state)
                {
                    case State.Idle:
                    case State.DownloadFirmware:
                        this.downloadTotal = await this.app.domain.apis.provision.downloadingFirmware();
                        if (this.downloadTotal < 0)
                        {
                            this.state              = State.Idle;
                            this.flashingInProgress = null;

                            if (this.powerSwitchSupported && !this.relayOn)
                            {
                                break;
                            }

                            this.boardDetected = await this.app.domain.apis.provision.detectBoard();
                            if (this.boardDetected)
                            {
                                this.state = State.BoardDetected;
                                console.log(`Board detected: ${this.boardDetected}`);

                                this.stopScreenSaver();
                            }
                        }
                        else
                        {
                            this.state = State.DownloadFirmware;
                        }
                        break;
                }
            }
            catch (e)
            {
                //
            }

            await Future.delayed(1000);
            this.startScreenSaverIfIdle();
        }

    }

    @ResetMemoizers
    public addNewCheckin(checkin: Models.ProvisionReportExt)
    {
        if (!this.lastCheckin || checkin.info.timestamp > this.lastCheckin)
        {
            this.lastCheckin = checkin.info.timestamp;
        }

        if (!this.readyForPrint)
        {
            this.checkinSelected = checkin;
        }

        this.checkins.push(checkin);

        this.detectChanges();
    }

    //--//

    startScreenSaverIfIdle()
    {
        if (MomentHelper.compare(MomentHelper.now(), this.screenSaverDeadline) > 0)
        {
            this.startScreenSaver();
        }
    }

    startScreenSaver()
    {
        if (!this.screenSaver)
        {
            this.screenSaverOverlay.toggleOverlay();
            this.screenSaver = new ScreenSaver(<HTMLCanvasElement>this.screenSaverRef.nativeElement);
        }
    }

    stopScreenSaver()
    {
        this.screenSaverDeadline = MomentHelper.add(MomentHelper.now(), 1, "hour");

        if (this.screenSaver)
        {
            this.screenSaverOverlay.closeOverlay();

            this.screenSaver.stop();
            this.screenSaver = undefined;
        }
    }

    public labelForCheckin(checkin: Models.ProvisionReportExt)
    {
        return `S/N ${checkin.info.hostId}`;
    }

    public async print()
    {
        if (await this.app.domain.apis.provision.printCheckin(this.checkinSelected.info.hostId))
        {
            this.checkinSelected.printed = true;
            this.detectChanges();
        }
        else
        {
            this.app.framework.errors.success("Check printer, it might not be ready...");
        }
    }

    async powerBoard(on: boolean)
    {
        if (this.powerSwitchSupported)
        {
            if (on)
            {
                await this.app.domain.apis.provision.boardPowerOn();
                this.relayOn = true;
            }
            else
            {
                await this.app.domain.apis.provision.boardPowerOff();
                this.relayOn = false;

                this.state         = State.Idle;
                this.testWaiting   = undefined;
                this.testFailure   = undefined;
                this.boardDetected = undefined;
            }
        }
    }

    public async flash()
    {
        let status = await this.app.domain.apis.provision.startBoardFlashing(this.boardDetected);
        if (status.state == Models.FlashingStatus.Flashing)
        {
            this.flashingInProgress = status;
            this.state              = State.Flashing;

            while (true)
            {
                this.detectChanges();

                this.flashingInProgress = await this.app.domain.apis.provision.checkBoardFlashing();

                if (this.flashingInProgress.state != Models.FlashingStatus.Flashing)
                {
                    switch (this.flashingInProgress.state)
                    {
                        case Models.FlashingStatus.NoBoard:
                            this.state = State.FlashingFailure;
                            this.app.framework.errors.success("Board got disconnected!!");
                            break;

                        case Models.FlashingStatus.Failed:
                            this.state = State.FlashingFailure;
                            this.app.framework.errors.success("Failed to flash board!!");
                            break;

                        case Models.FlashingStatus.Done:
                            if (this.isFactoryFloorMode)
                            {
                                this.state = State.WaitingForTests;
                            }
                            else
                            {
                                this.state = State.WaitingForRegistration;
                            }

                            let start = MomentHelper.now();
                            let end   = MomentHelper.add(start, 3, "minutes");

                            while (this.isWaitingForTests || this.isWaitingForRegistration)
                            {
                                await Future.delayed(1000);

                                let now = MomentHelper.now();

                                let diff = end.diff(now, "seconds");
                                if (diff < 0)
                                {
                                    this.state       = State.TestFailure;
                                    this.testFailure = "Tests timeout!!";
                                    break;
                                }

                                this.testWaiting = `(timeout in ${diff} seconds)`;

                                let boardDetected = await this.app.domain.apis.provision.detectBoard();
                                if (boardDetected)
                                {
                                    this.state       = State.TestFailure;
                                    this.testFailure = "Board rebooted during tests!!";
                                    break;
                                }
                            }
                            break;
                    }

                    break;
                }

                await Future.delayed(1000);
            }
        }
        else
        {
            this.app.framework.errors.success("Failed to start flashing...");
        }
    }

    public async checkFirmware()
    {
        this.app.domain.apis.provision.triggerFirmwareDownload();
    }

    private async logInfo(text: string)
    {
        await this.app.domain.apis.adminTasks.log("Info", text);
    }

    private async logDebug(text: string)
    {
        await this.app.domain.apis.adminTasks.log("Debug", text);
    }
}

enum State
{
    Idle, DownloadFirmware, BoardDetected, Flashing, FlashingFailure, WaitingForRegistration, BoardRegistered, WaitingForTests, TestPass, TestFailure
}

//--//

class Ray
{
    constructor(public angle: number,
                public radius: number)
    {
    }
}

class ScreenSaver
{
    private readonly maxStars       = 100;
    private readonly framePerSecond = 5;

    private stars: Ray[] = [];
    private centerX: number;
    private centerY: number;
    private viewportRadius: number;
    private updateInterval: any;

    constructor(private canvas: HTMLCanvasElement)
    {
        let width  = window.innerWidth;
        let height = window.innerHeight;

        canvas.width  = width;
        canvas.height = height;

        this.centerX        = width / 2;
        this.centerY        = height / 2;
        this.viewportRadius = Math.min(height, width) / 2;

        for (let i = 0; i < this.maxStars; i++)
        {
            this.animate();
        }

        this.updateInterval = setInterval(() =>
                                          {
                                              this.animate();
                                              this.render();
                                          }, 1000 / this.framePerSecond);
    }

    stop()
    {
        if (this.updateInterval)
        {
            clearInterval(this.updateInterval);
            this.updateInterval = undefined;
        }
    }

    animate()
    {
        if (this.stars.length < this.maxStars && Math.random() > 0.5)
        {
            this.stars.push(new Ray(Math.random(), 5));
        }

        for (let star of this.stars)
        {
            star.radius += 0.2 + (star.radius / 50);
        }
    }

    render()
    {
        let width  = this.canvas.width;
        let height = this.canvas.height;
        let ctx    = this.canvas.getContext("2d");

        ctx.fillStyle = "#000000";
        ctx.clearRect(0, 0, width, height);
        ctx.beginPath();
        ctx.rect(0, 0, width, height);
        ctx.closePath();
        ctx.fill();

        for (let i = 0; i < this.stars.length;)
        {
            let star = this.stars[i];

            // Compute
            let x = this.centerX + star.radius * Math.cos(Math.PI * 2 * star.angle);
            let y = this.centerY + star.radius * Math.sin(Math.PI * 2 * star.angle);

            if (x >= 0 && y >= 0 && x <= width && y <= height)
            {
                ctx.fillStyle = "rgba(255, 255, 255, " + ((.1 + star.radius) / this.viewportRadius) + ")";
                ctx.beginPath();
                ctx.rect(x, y, 2, 2);
                ctx.closePath();
                ctx.fill();

                i++;
            }
            else
            {
                this.stars.splice(i, 1);
            }
        }
    }
}

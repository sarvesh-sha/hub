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
    @ViewChild("screenSaverOverlay", {static: true}) screenSaverOverlay: OverlayComponent;
    @ViewChild("screenSaver", {static: true}) public screenSaverRef: ElementRef;

    private screenSaver: ScreenSaver;
    private screenSaverDeadline: moment.Moment;

    overlayConfig: OverlayConfig;
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

    downloadTotal: number = -1;

    cardDetected: number = -1;
    flashingInProgress: Models.FlashingProgress;

    get readyForCard(): boolean
    {
        if (this.downloadTotal > 0) return false; // Downloading firmware...
        if (this.cardDetected <= 0) return false; // No card.

        if (this.flashingInProgress) return false; // Flashing in progress or done.

        return true;
    }

    get isFlashing(): boolean
    {
        return this.flashingInProgress && this.flashingInProgress.state == Models.FlashingStatus.Flashing;
    }

    get isFlashingDone(): boolean
    {
        return this.flashingInProgress && this.flashingInProgress.state == Models.FlashingStatus.Done;
    }

    get hasFlashingFailed(): boolean
    {
        return this.flashingInProgress && this.flashingInProgress.state == Models.FlashingStatus.Failed;
    }

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
        this.loopForCheckins();
        this.loopForCard();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.stopScreenSaver();
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
                    console.log(`Checkin detected: ${JSON.stringify(checkin)}`);

                    this.addNewCheckin(checkin);

                    this.stopScreenSaver();
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

    async loopForCard()
    {
        while (!this.wasDestroyed())
        {
            try
            {
                if (!this.isFlashing)
                {
                    this.downloadTotal = await this.app.domain.apis.provision.downloadingFirmware();
                    if (this.downloadTotal <= 0)
                    {
                        let cardSize = await this.app.domain.apis.provision.detectCard();
                        if (cardSize > 0)
                        {
                            console.log(`Card detected: ${cardSize}`);

                            this.cardDetected = cardSize;

                            this.stopScreenSaver();
                        }
                        else
                        {
                            this.cardDetected       = -1;
                            this.flashingInProgress = null;
                        }
                    }
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
        this.screenSaverDeadline = MomentHelper.now()
                                               .add(1, "hour");

        if (this.screenSaver)
        {
            this.screenSaverOverlay.closeOverlay();

            this.screenSaver.stop();
            this.screenSaver = undefined;
        }
    }

    public labelForCheckin(checkin: Models.ProvisionReportExt)
    {
        return `Station ${checkin.info.stationNumber} - ${checkin.info.hostId} - Program ${checkin.info.stationProgram}`;
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

    public async flash()
    {
        let status = await this.app.domain.apis.provision.startCardFlashing();
        if (status.state == Models.FlashingStatus.Flashing)
        {
            this.flashingInProgress = status;

            while (true)
            {
                this.detectChanges();

                this.flashingInProgress = await this.app.domain.apis.provision.checkCardFlashing();

                if (this.flashingInProgress.state != Models.FlashingStatus.Flashing)
                {
                    if (this.flashingInProgress.state == Models.FlashingStatus.Failed)
                    {
                        this.app.framework.errors.success("Failed to flash card!!");
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

    public async restart()
    {
        if (await this.confirmOperation(`Are you sure you want to restart the unit?`))
        {
            this.app.domain.apis.adminTasks.reboot();
        }
    }

    public async checkFirmware()
    {
        this.app.domain.apis.provision.triggerFirmwareDownload();
    }
}

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

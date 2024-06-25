import {Injectable} from "@angular/core";

@Injectable()
export class PuppeteerService
{

    triggerCapture()
    {
        setTimeout(() =>
                   {
                       // Puppeteer will be listening for this
                       console.log("puppeteer-report-ready");
                   }, 500);
    }

    triggerFailure()
    {
        console.log("puppeteer-report-error");
    }
}

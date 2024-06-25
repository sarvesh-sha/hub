import {Page} from "puppeteer";
import {JitterMouseInitializeEvent} from "src/services/events";
import {TestResultTracker} from "src/services/test-result-tracker";

export class JitterMouse
{
    private m_interval: NodeJS.Timeout;

    get active(): boolean
    {
        return !!this.m_interval;
    }

    constructor(page: Page,
                reporter: TestResultTracker,
                currentTest: string,
                event: JitterMouseInitializeEvent)
    {
        let wentLeft = true;
        let point    = {
            x: event.x - 1,
            y: event.y
        };

        this.m_interval = setInterval(async () =>
                                      {
                                          try
                                          {
                                              point.x  = wentLeft ? point.x + 2 : point.x - 2;
                                              wentLeft = !wentLeft;
                                              await page.mouse.move(point.x, point.y);
                                          }
                                          catch (err)
                                          {
                                              reporter.logError(`Failed to move mouse for jittering: ${err}`, currentTest);
                                          }
                                      }, event.interval);
    }

    terminate()
    {
        if (this.active)
        {
            clearInterval(this.m_interval);
            this.m_interval = null;
        }
    }
}

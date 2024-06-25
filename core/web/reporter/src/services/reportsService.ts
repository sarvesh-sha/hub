import {Service} from "@tsed/common";
import * as fs from "fs";
import * as puppeteer from "puppeteer";
import {Browser, Page} from "puppeteer";
import * as url from "url";
import * as uuid from "uuid";
import {PaperFormat} from "../models/paperFormat";
import {ReportCreateRequest} from "../models/reportCreateRequest";
import {ReportIdentity} from "../models/reportIdentity";
import {ReportsHealthCheck} from "../models/reportsHealthCheck";
import {ReportStatus} from "../models/reportStatus";
import {Future} from "../utils/concurrency";
import {WorkerPool} from "../utils/workerPool";
import {ConfigService} from "./configService";

const REPORT_READY_MESSAGE = "puppeteer-report-ready";
const REPORT_ERROR_MESSAGE = "puppeteer-report-error";

const BACKWARDS_COMPATIBLE_MAGIC_SCALING_FACTOR = 117;

const paperFormats = {
    letter : {
        width : 8.5,
        height: 11
    },
    legal  : {
        width : 8.5,
        height: 14
    },
    tabloid: {
        width : 11,
        height: 17
    },
    ledger : {
        width : 17,
        height: 11
    },
    a0     : {
        width : 33.1,
        height: 46.8
    },
    a1     : {
        width : 23.4,
        height: 33.1
    },
    a2     : {
        width : 16.54,
        height: 23.4
    },
    a3     : {
        width : 11.7,
        height: 16.54
    },
    a4     : {
        width : 8.27,
        height: 11.7
    },
    a5     : {
        width : 5.83,
        height: 8.27
    },
    a6     : {
        width : 4.13,
        height: 5.83
    }
};

@Service()
export class ReportsService
{
    private reportStatuses: { [id: string]: ReportStatus } = {};
    private reportGenerations: { [id: string]: number }    = {};
    private reportPages: { [id: string]: Promise<Page> }   = {};
    private numReportsGenerated                            = 0;
    private numReportsFailed                               = 0;
    private numReportsQueued                               = 0;
    private browser: Browser;
    private workerPool: WorkerPool;

    constructor(private config: ConfigService)
    {
        puppeteer.launch({
                             args: [
                                 "--no-sandbox",
                                 "--disable-setuid-sandbox",
                                 "--disable-dev-shm-usage",
                                 "--disable-accelerated-2d-canvas",
                                 "--no-zygote",
                                 "--disable-gpu",
                                 "--use-webgl=swiftshader"
                             ]
                         })
                 .then(browser =>
                       {
                           this.browser = browser;
                           return browser.version();
                       })
                 .then(version =>
                       {
                           console.log(`Browser version: ${version}`);
                       });

        this.workerPool = new WorkerPool(config.reports.maxConcurrentPages || 5);
    }

    public createReport(request: ReportCreateRequest): ReportIdentity
    {
        const identity                            = new ReportIdentity(uuid.v4());
        this.reportGenerations[identity.reportId] = this.workerPool.addTask(() =>
                                                                            {
                                                                                return this.generatePdf(request, identity.reportId);
                                                                            });
        this.setReportStatus(identity.reportId, ReportStatus.QUEUED);


        return identity;
    }

    public getReportStatus(reportId: string): ReportStatus
    {
        return this.reportStatuses[reportId] || ReportStatus.UNKNOWN;
    }

    public async getReportBytes(reportId: string): Promise<string>
    {
        const bytes = new Future<string>();
        fs.readFile(this.getPath(reportId),
                    (err,
                     buf) =>
                    {
                        if (err)
                        {
                            return bytes.reject(err);
                        }

                        bytes.resolve(buf.toString("base64"));
                    });
        return bytes;
    }

    public async deleteReport(reportId: string): Promise<void>
    {
        const status = this.getReportStatus(reportId);
        if (status === ReportStatus.UNKNOWN)
        {
            return;
        }

        if (status === ReportStatus.QUEUED)
        {
            this.workerPool.removeTask(this.reportGenerations[reportId]);
            this.setReportStatus(reportId, ReportStatus.FAILURE);
        }

        if (status === ReportStatus.PROCESSING)
        {
            const page = await this.reportPages[reportId];
            await page.close();
            this.setReportStatus(reportId, ReportStatus.FAILURE);
        }

        delete this.reportStatuses[reportId];
        delete this.reportGenerations[reportId];
        delete this.reportPages[reportId];

        try
        {
            fs.unlinkSync(this.getPath(reportId));
        }
        catch (err)
        {
        }
    }

    public getReportsHealth(): ReportsHealthCheck
    {
        return new ReportsHealthCheck(this.numReportsGenerated, this.numReportsFailed, this.numReportsQueued);
    }

    private async generatePdf(request: ReportCreateRequest,
                              reportId: string): Promise<void>
    {
        let page: Page;
        this.setReportStatus(reportId, ReportStatus.PROCESSING);
        const futurePage           = new Future<Page>();
        this.reportPages[reportId] = futurePage;

        if (!paperFormats[request.pdfFormat])
        {
            request.pdfFormat = PaperFormat.legal;
        }

        const size   = paperFormats[request.pdfFormat];
        const width  = Math.round((size.width) * BACKWARDS_COMPATIBLE_MAGIC_SCALING_FACTOR);
        const height = Math.round((size.height) * BACKWARDS_COMPATIBLE_MAGIC_SCALING_FACTOR);

        const resolvedWidth = request.landscape ? height : width;

        try
        {
            const path      = this.getPath(reportId);
            const reportUrl = this.buildUrl(request, resolvedWidth);

            page = await this.newIsolatedPage(reportId);

            futurePage.resolve(page);

            await Promise.all([
                                  page.goto(reportUrl),
                                  this.waitForReportReady(page, request, reportId)
                              ]);

            await page.pdf({
                               path               : path,
                               format             : request.pdfFormat,
                               landscape          : request.landscape,
                               printBackground    : true,
                               displayHeaderFooter: true,
                               footerTemplate     : this.getFooter(request.reportTime),
                               margin             : {
                                   left  : 0,
                                   top   : "0.5cm",
                                   right : 0,
                                   bottom: "1cm"
                               }
                           });

            this.setReportStatus(reportId, ReportStatus.SUCCESS);

            this.scheduleDelete(reportId);
        }
        catch (err)
        {
            console.error(reportId, err);
            this.setReportStatus(reportId, ReportStatus.FAILURE);
        }
        finally
        {
            if (page)
            {
                await page.close();
            }
        }
    }

    private getFooter(time: string): string
    {
        let pagesStyle  = `style="float: right;"`;
        let dateStyle   = `style="float: left;"`;
        let footerStyle = `style="width: 100%; font-size: 8px; padding: 0 0.5cm"`;

        // Backwards compatible
        time = time || new Date().toUTCString();

        let current = `<span class="pageNumber"></span>`;
        let total   = `<span class="totalPages"></span>`;
        let date    = `<div ${dateStyle}>${time}</div>`;
        let pages   = `<div ${pagesStyle}>${current} / ${total}</div>`;
        return `<div class="footer" ${footerStyle}>${date}${pages}</div>`;
    }

    private getPath(reportId: string): string
    {
        const reportsDirectory = this.config.reports.directory;
        return `${reportsDirectory}/${reportId}.pdf`;
    }

    private setReportStatus(reportId: string,
                            status: ReportStatus)
    {
        let currentStatus = this.reportStatuses[reportId];
        if (currentStatus === undefined && status !== ReportStatus.QUEUED)
        {
            // Do not set status if report has already been removed;
            return;
        }

        if (currentStatus === ReportStatus.SUCCESS || currentStatus === ReportStatus.FAILURE)
        {
            // Do not set status if report has already been marked as done
            return;
        }

        switch (status)
        {
            case ReportStatus.SUCCESS:
                this.numReportsQueued--;
                this.numReportsGenerated++;
                break;
            case ReportStatus.FAILURE:
                this.numReportsQueued--;
                this.numReportsFailed++;
                break;
            case ReportStatus.QUEUED:
                this.numReportsQueued++;
                break;
        }

        this.reportStatuses[reportId] = status;
    }

    private scheduleDelete(reportId: string)
    {
        setTimeout(() => this.deleteReport(reportId), (this.config.reports.storageTimeMinutes || 10) * 60 * 1000);
    }

    private waitForReportReady(page: Page,
                               request: ReportCreateRequest,
                               reportId: string): Future<void>
    {
        const reportWaitTimeout = (request.maxWaitTimeSeconds || 30) * 1000;
        const reportReady       = new Future<void>();

        // Assume report ready after some time
        const rejectTimer = setTimeout(() => reportReady.reject(new Error("Timeout while generating report.")), reportWaitTimeout);

        page.on("console", async (msg) =>
        {
            if (msg.text() === REPORT_READY_MESSAGE)
            {
                clearTimeout(rejectTimer);
                reportReady.resolve();
            }

            if (msg.text() === REPORT_ERROR_MESSAGE)
            {
                clearTimeout(rejectTimer);
                reportReady.reject(new Error("Error generating report."));
            }

            if (msg.type() === "error")
            {
                // Evaluate arguments in the page context, to get the real errors
                const args             = msg.args();
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

                for (let arg of resolvedArgs)
                {
                    console.log(`${request.baseUrl} - ${reportId} - ${arg}`);
                }
            }
        });

        return reportReady;
    }

    /**
     * Creates a new page object with a fresh browser context to prevent and cookie sharing.
     * See https://github.com/GoogleChrome/puppeteer/issues/85
     */
    private async newIsolatedPage(reportId: string)
    {
        const context = await this.browser.createIncognitoBrowserContext();
        const page    = await context.newPage();

        context.on("targetdestroyed", async () =>
        {
            try
            {
                await context.close();
            }
            catch (e)
            {
                // Assume it's already been closed
            }
        });

        page.on("error", (err) =>
        {
            console.error(`Page crashed! ReportID: ${reportId}`);
            console.error(`Error: ${err}`);
            this.setReportStatus(reportId, ReportStatus.FAILURE);
            context.close();
        });

        return page;
    }

    private buildUrl(request: ReportCreateRequest,
                     width: number): string
    {
        this.validateUrl(request.baseUrl);

        /* tslint:disable */
        return `${request.baseUrl}/#/reports;sys_asreport=true;sys_sessiontoken=${encodeURIComponent(request.sessionToken)};token=${request.reportToken};sys_width=${width}`;
    }

    private validateUrl(baseUrl: string)
    {
        let parsed         = url.parse(baseUrl);
        let allowedDomains = this.config.reports.allowedDomains || [];
        if (parsed.protocol !== "http:" && parsed.protocol !== "https:")
        {
            throw new Error(`Invalid protocol in url: ${baseUrl}`);
        }

        if (allowedDomains.length && !allowedDomains.find((allowed) => parsed.hostname.endsWith(allowed)))
        {
            throw new Error(`Url is not allowed: ${baseUrl}`);
        }
    }
}


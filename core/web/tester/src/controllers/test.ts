import {BodyParams, Controller, Get, PathParams, PlatformRequest, PlatformResponse, Post, Req, Res} from "@tsed/common";
import {InternalServerError} from "@tsed/exceptions";
import {Consumes, OperationId, Produces, Returns} from "@tsed/schema";
import * as fs from "fs";
import {TestCase} from "../models/testCase";
import {TestResult} from "../models/testResult";
import {TestsInitializeRequest} from "../models/testsInitializeRequest";
import {TestsRunRequest} from "../models/testsRunRequest";
import {ConfigService} from "../services/config";
import {TestService} from "../services/tester";


@Controller("/v1/tests")
export class Tests
{
    constructor(private m_testService: TestService,
                private config: ConfigService)
    {
    }

    @Post("/")
    @OperationId("getTests")
    @Produces("application/json")
    @Consumes("application/json")
    @Returns(200, Array)
        .Of(TestCase)
    async getTests(@BodyParams() request: TestsInitializeRequest): Promise<TestCase[]>
    {
        try
        {
            await this.m_testService.initialize({
                                                    url: request.url,
                                                    ids: []
                                                });
            return this.m_testService.getTests();
        }
        finally
        {
            await this.m_testService.cleanupPage();
        }
    }

    @Post("/start")
    @OperationId("startTests")
    @Produces("application/json")
    @Consumes("application/json")
    @Returns(200, Array)
        .Of(TestCase)
    async startTests(@BodyParams() request: TestsRunRequest): Promise<TestCase[]>
    {
        try
        {
            await this.m_testService.initialize(request);
            return this.m_testService.startTests(request);
        }
        catch (e)
        {
            throw new InternalServerError(`Unable to start test: "${e}"`);
        }
    }

    @Post("/cancel")
    @OperationId("cancelTests")
    @Produces("application/json")
    @Consumes("application/json")
    @Returns(200, Boolean)
    cancelTests(): boolean
    {
        return this.m_testService.cancelAll();
    }

    @Post("/cancel/:id")
    @OperationId("cancelTest")
    @Produces("application/json")
    @Consumes("application/json")
    @Returns(200, Boolean)
    cancelTest(@PathParams("id") id: string): boolean
    {
        return this.m_testService.cancel(id);
    }

    @Post("/run-all")
    @OperationId("runAll")
    @Produces("application/json")
    @Consumes("application/json")
    @Returns(200, Boolean)
    async runAll(@BodyParams() request: TestsInitializeRequest): Promise<boolean>
    {
        return this.m_testService.runAll(request.url);
    }

    @Get("/progress")
    @OperationId("checkProgress")
    @Produces("application/json")
    @Consumes("application/json")
    @Returns(200, Array)
        .Of(TestResult)
    checkProgress(): TestResult[]
    {
        return this.m_testService.getProgress();
    }

    @Get("/video/:id")
    @OperationId("getVideo")
    @Produces("video/mp4")
    getVideo(@PathParams("id") id: string,
             @Req() req: PlatformRequest,
             @Res() response: PlatformResponse)
    {
        const path     = `${this.config.tester.outputDirectory}/${id}.mp4`;
        const stat     = fs.statSync(path);
        const fileSize = stat.size;
        const range    = req.headers.range;
        const res      = response.getRes();
        if (range)
        {
            const parts     = range.replace(/bytes=/, "")
                                   .split("-");
            const start     = parseInt(parts[0], 10);
            const end       = parts[1]
                ? parseInt(parts[1], 10)
                : fileSize - 1;
            const chunksize = (end - start) + 1;
            const file      = fs.createReadStream(path,
                                                  {
                                                      start,
                                                      end
                                                  });
            const head      = {
                "Content-Range" : `bytes ${start}-${end}/${fileSize}`,
                "Accept-Ranges" : "bytes",
                "Content-Length": chunksize,
                "Content-Type"  : "video/mp4"
            };
            res.writeHead(206, head);
            file.pipe(res);
        }
        else
        {
            const head = {
                "Content-Length": fileSize,
                "Content-Type"  : "video/mp4"
            };
            res.writeHead(200, head);
            fs.createReadStream(path)
              .pipe(res);
        }
    }
}

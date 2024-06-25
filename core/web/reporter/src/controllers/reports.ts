import {BodyParams, Controller, Delete, Get, PathParams, Post, Res} from "@tsed/common";
import {BadRequest, NotFound} from "@tsed/exceptions";
import {Consumes, OperationId, Produces, Returns} from "@tsed/schema";
import {Response} from "express";
import {Report} from "../models/report";
import {ReportCreateRequest} from "../models/reportCreateRequest";
import {ReportIdentity} from "../models/reportIdentity";
import {ReportsHealthCheck} from "../models/reportsHealthCheck";
import {ReportStatus} from "../models/reportStatus";
import {ReportStatusCheck} from "../models/reportStatusCheck";
import {ReportsService} from "../services/reportsService";


@Controller("/v1/reports")
export class Reports
{
    constructor(private reportsService: ReportsService)
    {
    }

    @Post("/")
    @OperationId("create")
    @Consumes("application/json")
    @Produces("application/json")
    @Returns(200, ReportIdentity)
    async create(@BodyParams() request: ReportCreateRequest): Promise<ReportIdentity>
    {
        if (!request)
        {
            throw new BadRequest("URL and token are required.");
        }

        return await this.reportsService.createReport(request);
    }

    @Get("/health")
    @OperationId("getReportsHealth")
    @Consumes("application/json")
    @Produces("application/json")
    @Returns(200, ReportsHealthCheck)
    async getReportsHealth()
    {
        return this.reportsService.getReportsHealth();
    }

    @Get("/:reportId/status")
    @OperationId("getStatus")
    @Consumes("application/json")
    @Produces("application/json")
    @Returns(200, ReportStatusCheck)
    async getStatus(@PathParams("reportId") reportId: string): Promise<ReportStatusCheck>
    {
        const status: ReportStatus = this.reportsService.getReportStatus(reportId);
        if (status === ReportStatus.UNKNOWN)
        {
            throw new NotFound("Report ID not found.");
        }
        return new ReportStatusCheck(reportId, status);
    }

    @Get("/:reportId/download")
    @OperationId("download")
    @Consumes("application/json")
    @Produces("application/json")
    @Returns(200, Report)
    async download(@Res() response: Response,
                   @PathParams("reportId") reportId: string)
    {
        const status = this.reportsService.getReportStatus(reportId);
        if (status === ReportStatus.UNKNOWN)
        {
            throw new NotFound("Report ID not found.");
        }

        if (status !== ReportStatus.SUCCESS)
        {
            throw new BadRequest(`Report: ${reportId} is not ready.`);
        }

        const bytes     = await this.reportsService.getReportBytes(reportId);
        const report    = new Report();
        report.reportId = reportId;
        report.bytes    = bytes;

        return report;
    }

    @Delete("/:reportId")
    @OperationId("delete")
    @Consumes("application/json")
    @Produces("application/json")
    @Returns(204)
    async delete(@PathParams("reportId") reportId: string)
    {
        await this.reportsService.deleteReport(reportId);
        return true;
    }
}

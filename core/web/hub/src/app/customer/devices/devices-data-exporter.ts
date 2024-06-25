import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {Future} from "framework/utils/concurrency";

export class DevicesDataExporter implements DownloadGenerator
{
    private m_reportId: string;
    private m_progress: Models.DevicesReportProgress;
    private m_success: boolean;

    constructor(private domain: AppDomainContext,
                private m_fileName: string)
    {
    }

    public getProgressPercent(): number
    {
        return (100 * this.m_progress.devicesProcessed / this.m_progress.totalDevices);
    }

    public getProgressMessage(): string
    {
        if (this.m_success === undefined)
        {
            let message = "Exporting devices... ";

            if (this.m_progress)
            {
                if (this.m_progress.generatingFile)
                {
                    return "Generating file...";
                }

                if (this.m_progress.totalDevices)
                {
                    message += `${(100 * this.m_progress.devicesProcessed / this.m_progress.totalDevices).toFixed(0)}% complete`;
                }
            }

            return message;
        }

        if (this.m_success) return `Successfully exported ${this.m_progress.totalDevices} devices`;

        return "Export failed...";
    }

    public async makeProgress(): Promise<boolean>
    {
        if (!this.m_reportId) this.m_reportId = await this.domain.apis.devices.startSummaryReport();

        this.m_progress = await this.domain.apis.devices.checkSummaryReport(this.m_reportId, false);
        switch (this.m_progress?.status)
        {
            case Models.BackgroundActivityStatus.COMPLETED:
                this.m_progress = await this.domain.apis.devices.checkSummaryReport(this.m_reportId, true);
                this.m_success  = true;
                return true;

            case Models.BackgroundActivityStatus.FAILED:
            case Models.BackgroundActivityStatus.CANCELLED:
                this.m_success = false;
                return true;
        }

        return false;
    }

    public async sleepForProgress(): Promise<void>
    {
        await Future.delayed(500);
    }

    public isDeterminate(): boolean
    {
        return true;
    }

    public async getResults(): Promise<DownloadResults>
    {
        return {
            url: this.domain.apis.devices.streamSummaryReport__generateUrl(this.m_reportId, this.m_fileName)
        };
    }
}

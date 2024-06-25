import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {Future} from "framework/utils/concurrency";

export class DeviceElementDataExporter implements DownloadGenerator
{
    private m_reportId: string;
    private m_progress: Models.DeviceElementReportProgress;
    private m_success: boolean;

    constructor(private domain: AppDomainContext,
                private m_fileName: string,
                private m_parentSysId: string)
    {
    }

    public getProgressPercent(): number
    {
        return (100 * this.m_progress.deviceElementsProcessed / this.m_progress.totalDeviceElements);
    }

    public getProgressMessage(): string
    {
        if (this.m_success === undefined)
        {
            let message = "Exporting device elements... ";

            if (this.m_progress)
            {
                if (this.m_progress.generatingFile)
                {
                    return "Generating file...";
                }

                if (this.m_progress.totalDeviceElements)
                {
                    message += `${(100 * this.m_progress.deviceElementsProcessed / this.m_progress.totalDeviceElements).toFixed(0)}% complete`;
                }
            }

            return message;
        }

        if (this.m_success) return `Successfully exported ${this.m_progress.totalDeviceElements} device elements`;

        return "Export failed...";
    }

    public async makeProgress(): Promise<boolean>
    {
        if (!this.m_reportId) this.m_reportId = await this.domain.apis.assets.startDeviceElementsReport(this.m_parentSysId);

        this.m_progress = await this.domain.apis.assets.checkDeviceElementReport(this.m_reportId, false);
        switch (this.m_progress?.status)
        {
            case Models.BackgroundActivityStatus.COMPLETED:
                this.m_progress = await this.domain.apis.assets.checkDeviceElementReport(this.m_reportId, true);
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
        return false;
    }

    public async getResults(): Promise<DownloadResults>
    {
        return {
            url: this.domain.apis.assets.streamDeviceElementReport__generateUrl(this.m_reportId, this.m_fileName)
        };
    }
}

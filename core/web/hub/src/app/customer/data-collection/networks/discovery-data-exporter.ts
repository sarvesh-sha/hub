import {NetworkExtended} from "app/services/domain/assets.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {Future} from "framework/utils/concurrency";

export class DiscoveryDataExporter implements DownloadGenerator
{
    private m_reportId: string;
    private m_progress: Models.DiscoveryReportProgress;
    private m_success: boolean;

    constructor(private domain: AppDomainContext,
                private m_fileName: string,
                private m_networks: NetworkExtended[],
                private m_filter: Models.DeviceElementFilterRequest)
    {
    }

    public getProgressPercent(): number
    {
        return NaN;
    }

    public getProgressMessage(): string
    {
        if (this.m_success === undefined)
        {
            return `Processed ${this.m_progress.devicesProcessed} devices, ${this.m_progress.elementsProcessed} elements...`;
        }

        if (this.m_success) return `Successfully exported ${this.m_progress.devicesProcessed} discovered elements`;

        return "Export failed...";
    }

    public async makeProgress(): Promise<boolean>
    {
        if (!this.m_reportId)
        {
            let networks = this.m_networks.map((n) => n.getIdentity());
            let run      = Models.DiscoveryReportRun.newInstance({
                                                                     networks: networks,
                                                                     filter  : this.m_filter
                                                                 });


            this.m_reportId = await this.domain.apis.discovery.startReport(run);
        }

        this.m_progress = await this.domain.apis.discovery.checkReport(this.m_reportId, false);
        switch (this.m_progress?.status)
        {
            case Models.BackgroundActivityStatus.COMPLETED:
                this.m_progress = await this.domain.apis.discovery.checkReport(this.m_reportId, true);
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
            url: this.domain.apis.discovery.streamReport__generateUrl(this.m_reportId, this.m_fileName)
        };
    }
}

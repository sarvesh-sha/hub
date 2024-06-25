import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {Future} from "framework/utils/concurrency";

export class AzuredigitaltwinExporter implements DownloadGenerator
{
    private m_reportId: string;
    private m_progress: Models.AzureDigitalTwinSyncProgress;
    private m_success: boolean;
    private m_message: string;

    constructor(private domain: AppDomainContext,
                private m_fileName: string)
    {
    }

    public getProgressPercent(): number
    {
        return NaN;
    }

    public getProgressMessage(): string
    {
        return this.m_message;
    }

    public async makeProgress(): Promise<boolean>
    {
        if (!this.m_reportId)
        {
            this.m_reportId = await this.domain.apis.discovery.pushToAzureDigitalTwin();
            if (!this.m_reportId)
            {
                this.m_success = false;
                return true;
            }
        }

        let progress = await this.domain.apis.discovery.checkPushToAzureDigitalTwin(this.m_reportId, false);
        if (!progress)
        {
            this.m_success = false;
            return true;
        }

        switch (progress?.status)
        {
            case Models.BackgroundActivityStatus.COMPLETED:
                this.m_success  = true;
                this.m_message  = `Successfully exported ${progress.twinsProcessed} twins and ${progress.relationshipsProcessed} relationships`;
                this.m_progress = progress;
                return true;

            case Models.BackgroundActivityStatus.FAILED:
            case Models.BackgroundActivityStatus.CANCELLED:
                this.m_success = false;
                this.m_message = "Publish failed...";
                return true;

            case Models.BackgroundActivityStatus.ACTIVE:
            case Models.BackgroundActivityStatus.EXECUTING:
                this.m_message = `Processed ${progress.devicesProcessed} devices, ${progress.elementsProcessed} elements.\n` +
                                 `Found ${progress.twinsFound} twins and ${progress.relationshipsFound} relationships.\n` +
                                 `Processed ${progress.twinsProcessed} twins and ${progress.relationshipsProcessed} relationships.`;
                break;
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
        let progress = this.m_progress;
        if (!progress) return null;

        return {
            lines: [
                `Processed ${progress.devicesProcessed} devices, ${progress.elementsProcessed} elements.`,
                `Found ${progress.twinsFound} twins and ${progress.relationshipsFound} relationships.`,
                `Processed ${progress.twinsProcessed} twins and ${progress.relationshipsProcessed} relationships.`
            ]
        };
    }
}

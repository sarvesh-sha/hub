import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {Future} from "framework/utils/concurrency";

export class EquipmentDataExporter implements DownloadGenerator
{
    private m_reportId: string;
    private m_progress: Models.EquipmentReportProgress;
    private m_success: boolean;

    constructor(private domain: AppDomainContext,
                private m_fileName: string,
                private m_parentEquipmentSysId?: string)
    {
    }

    public getProgressPercent(): number
    {
        return 100 * this.m_progress.equipmentProcessed / this.m_progress.totalEquipment;
    }

    public getProgressMessage(): string
    {
        if (this.m_success === undefined)
        {
            let message = "Exporting equipment... ";

            if (this.m_progress)
            {
                if (this.m_progress.generatingFile)
                {
                    return "Generating file...";
                }

                if (this.m_progress.totalEquipment)
                {
                    message += `${UtilsService.getRoundedValue(this.getProgressPercent(), 0)}% complete`;
                }
            }

            return message;
        }

        if (this.m_success) return `Successfully exported ${this.m_progress.totalEquipment} equipment`;

        return "Export failed...";
    }

    public async makeProgress(): Promise<boolean>
    {
        if (!this.m_reportId)
        {
            this.m_reportId = await this.domain.apis.assetRelationships.startEquipmentReport(this.m_parentEquipmentSysId);
        }

        this.m_progress = await this.domain.apis.assetRelationships.checkEquipmentReport(this.m_reportId, false);
        switch (this.m_progress?.status)
        {
            case Models.BackgroundActivityStatus.COMPLETED:
                this.m_progress = await this.domain.apis.assetRelationships.checkEquipmentReport(this.m_reportId, true);
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
            url: this.domain.apis.assetRelationships.streamEquipmentReport__generateUrl(this.m_reportId, this.m_fileName)
        };
    }
}

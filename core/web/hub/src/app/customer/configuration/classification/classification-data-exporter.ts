import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";

export class ClassificationDataExporter implements DownloadGenerator
{
    private readonly m_result: DataExport;
    private m_deviceIds: string[];
    private m_totalDevices: number;
    private m_processedDevices: number;

    constructor(private domain: AppDomainContext,
                rules: Models.NormalizationRules)
    {
        this.m_result         = new DataExport();
        this.m_result.rules   = rules;
        this.m_result.devices = [];
    }

    public getProgressPercent(): number
    {
        return (100 * this.m_processedDevices / this.m_totalDevices);
    }

    public getProgressMessage(): string
    {
        return `${this.m_processedDevices} of ${this.m_totalDevices} devices exported`;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        await this.ensureDevices();
        if (this.m_deviceIds.length === 0)
        {
            return true;
        }

        let deviceId     = this.m_deviceIds.pop();
        let deviceExport = await this.domain.apis.normalization.exportNormalization(deviceId);
        this.m_result.devices.push(deviceExport);
        this.m_processedDevices++;

        return false;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate(): boolean
    {
        return true;
    }

    public async getResults(): Promise<DownloadResults>
    {
        return {text: JSON.stringify(this.m_result)};
    }

    private async ensureDevices()
    {
        if (!this.m_deviceIds)
        {
            let response            = await this.domain.assets.getList(new Models.DeviceFilterRequest());
            this.m_deviceIds        = response.results.map((id) => id.sysId);
            this.m_totalDevices     = this.m_deviceIds.length;
            this.m_processedDevices = 0;
        }

        return this.m_deviceIds;
    }
}

class DataExport
{
    rules: Models.NormalizationRules;

    devices: Models.DeviceNormalizationExport[];
}

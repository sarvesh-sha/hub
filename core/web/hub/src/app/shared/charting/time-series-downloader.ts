import {DeviceElementExtended} from "app/services/domain/assets.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {ExcelExporter} from "app/shared/utils/excel-exporter";

import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";

import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

import moment from "framework/utils/moment";

export interface TimeSeriesDownloaderElement
{
    name: string;
    element: DeviceElementExtended;
    units?: Models.EngineeringUnitsFactors;
}

export class TimeSeriesDownloader implements DownloadGenerator
{
    private m_response: Models.TimeSeriesMultiPropertyResponse;

    private m_exporter: ExcelExporter;

    private m_rowsProcessed  = 0;
    private m_generatingFile = false;

    constructor(private m_domain: AppDomainContext,
                private m_elements: TimeSeriesDownloaderElement[],
                private m_rangeStart: moment.Moment,
                private m_rangeEnd: moment.Moment,
                private m_zone?: string)
    {
    }

    public getProgressPercent(): number
    {
        if (!this.m_response) return 0;
        return 100 * this.m_rowsProcessed / this.m_response.timestamps.length;
    }

    public getProgressMessage(): string
    {
        if (!this.m_response?.timestamps) return "Fetching samples...";

        if (this.m_generatingFile) return "Generating file...";

        return `${this.m_rowsProcessed} / ${this.m_response.timestamps.length} samples processed.`;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        // Fetch Samples
        if (!this.m_response)
        {
            let requests = this.m_elements.map((entry) =>
                                               {
                                                   return Models.TimeSeriesPropertyRequest.newInstance(
                                                       {
                                                           sysId    : entry.element.model.sysId,
                                                           prop     : DeviceElementExtended.PRESENT_VALUE,
                                                           convertTo: entry.units
                                                       }
                                                   );
                                               });

            this.m_response = await this.m_domain.assets.getInterpolatedValues(requests, this.m_rangeStart, this.m_rangeEnd);

            return false;
        }

        // Setup Exporter
        if (!this.m_exporter)
        {
            this.m_exporter = new ExcelExporter(this.m_domain.apis.exports, "Samples", "NO DATA");

            this.m_exporter.addColumnHeader("Timestamp", "m/d/yy h:mm:ss \"GMT\"");
            this.m_exporter.addColumnHeader("Millis");
            this.m_exporter.addColumnHeader("Raw Seconds");
            for (let elem of this.m_elements)
            {
                this.m_exporter.addColumnHeader(elem.name);
            }

            return false;
        }

        // Export samples
        let timestamps = this.m_response.timestamps || [];
        for (let i = this.m_rowsProcessed; i < timestamps.length; i++)
        {
            let rawSeconds = timestamps[i];
            let timestamp  = MomentHelper.parse(rawSeconds * 1_000, this.m_zone);

            let row = await this.m_exporter.addRow();
            row.push(timestamp.toDate(), (rawSeconds * 1000) % 1000, rawSeconds);

            for (let j = 0; j < this.m_response.results.length; j++)
            {
                let series = this.m_response.results[j];
                let value  = series.values[i] ?? undefined;
                if (series.enumLookup)
                {
                    row.push(series.enumLookup[value]);
                }
                else
                {
                    row.push(value);
                }
            }

            this.m_rowsProcessed++;
            if (this.m_rowsProcessed % 1000 === 0) return false;
        }

        this.m_generatingFile = true;
        await this.m_exporter.generateFile();

        return true;
    }

    public async sleepForProgress(): Promise<void>
    {
        // No need to sleep.
    }

    public isDeterminate(): boolean
    {
        return true;
    }

    public async getResults(fileName: string): Promise<DownloadResults>
    {
        return this.m_exporter.getResults(fileName);
    }
}

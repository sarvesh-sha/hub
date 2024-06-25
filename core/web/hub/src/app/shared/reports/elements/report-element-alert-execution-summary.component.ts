import {Component, Injector, Optional, ViewChild} from "@angular/core";
import {ReportLivePreviewComponent} from "app/customer/configuration/reports/report-live-preview/report-live-preview.component";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";

import {AlertDefinitionVersionExtended} from "app/services/domain/alert-definition-versions.service";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import {AlertColorStrategy, AlertDetails, AlertMarker, AlertShapeStrategy, AlertSizeStrategy} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {AlertEventType} from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {isVisible, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {mapInParallel} from "framework/utils/concurrency";
import moment from "framework/utils/moment";

@Component({
               selector   : "o3-report-element-alert-execution-summary",
               templateUrl: "./report-element-alert-execution-summary.component.html"
           })
export class ReportElementAlertExecutionSummaryComponent extends ReportElementBaseComponent<ReportElementAlertExecutionSummaryData, ReportElementAlertExecutionSummaryConfiguration> implements IConsoleLogProvider
{
    @ViewChild("log", {static: true}) log: ConsoleLogComponent;
    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    alertsFlat: PointAlertsFlat[] = [];
    failures: string[]            = [];

    chartConfig: TimeSeriesChartConfigurationExtended;
    sourceIdToIdentifiers: Lookup<string[]>;

    logDialogConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});
    logLockScroll: boolean;

    allAlertsInsignificant: boolean = false;

    constructor(inj: Injector,
                @Optional() private m_livePreview: ReportLivePreviewComponent)
    {
        super(inj);
    }

    protected async afterConfigurationChanges()
    {
        super.afterConfigurationChanges();

        let map = new Map<string, PointAlertsFlat>();
        for (let alert of this.data.alerts)
        {
            let flat = map.get(alert.assetSysId);
            if (!flat)
            {
                flat        = new PointAlertsFlat();
                flat.sysId  = alert.assetSysId;
                flat.alerts = [];
                map.set(alert.assetSysId, flat);
            }

            flat.alerts.push(alert);
        }

        this.failures   = UtilsService.extractKeysFromMap(this.data.failures);
        this.alertsFlat = [...map.values()];

        let assets    = await mapInParallel(this.alertsFlat, (flat) => this.app.domain.assets.getExtendedById(flat.sysId));
        let devices   = await mapInParallel(assets, (asset) => asset.getParent());
        let locations = await mapInParallel(assets, (asset) => asset.getLocation());
        for (let flat of this.alertsFlat)
        {
            let asset       = assets.shift();
            let device      = devices.shift();
            let location    = locations.shift();
            flat.deviceName = `${device.model.name} - ${asset.model.name}`;
            if (location)
            {
                flat.deviceName += ` - ${location.model.name}`;
            }
            flat.alertCount = flat.alerts.filter((a) =>
                                                 {
                                                     switch (a.eventType)
                                                     {
                                                         case Models.AlertEventType.created:
                                                         case Models.AlertEventType.reopened:
                                                             return true;

                                                         default:
                                                             return false;
                                                     }
                                                 }).length;
        }

        this.alertsFlat.sort((a,
                              b) =>
                             {
                                 let compare = b.alerts.length - a.alerts.length;
                                 if (compare === 0)
                                 {
                                     return UtilsService.compareStrings(a.deviceName, b.deviceName, true);
                                 }
                                 return compare;
                             });

        if (this.alertsFlat.length) await this.buildChart();

        if (this.m_livePreview)
        {
            this.m_livePreview.registerSecondaryButton("Show Logs", () => this.overlay.toggleOverlay(), () => this.getLogCount() == 0);
        }

        this.markAsComplete();
    }

    public getLogCount(): number
    {
        return this.data.logEntries?.length || 0;
    }

    public async getLogPage(start: number,
                            end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = this.data.logEntries || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    public async performFilter(filters: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return [];
    }

    refreshLog()
    {
        this.log.bind(this);

        this.logLockScroll = true;
        this.log.refresh();
    }

    private async buildChart()
    {
        let host     = new TimeSeriesSourceHost(this);
        let range    = new RangeSelectionExtended(this.data.range);
        let chartExt = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);

        let sources = await mapInParallel(this.alertsFlat, async (flat) =>
        {
            let sourceExt           = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, flat.sysId);
            sourceExt.includeAlerts = false;
            await sourceExt.fetch(host, range);
            let chartSource = sourceExt.getChartData();
            for (let alertDetails of flat.alerts)
            {
                let marker           = new AlertMarker(this.app, alertDetails, chartSource, true);
                marker.sizeStrategy  = AlertSizeStrategy.ALERT_REPORT;
                marker.colorStrategy = AlertColorStrategy.ALERT_REPORT;
                marker.shapeStrategy = AlertShapeStrategy.ALERT_REPORT;
                chartSource.markers.push(marker);
            }
            return sourceExt;
        });

        await chartExt.applySourceChanges(sources, []);

        this.sourceIdToIdentifiers = {};
        for (let source of chartExt.sourcesExt)
        {
            let id          = source.model.id;
            let identifiers = this.sourceIdToIdentifiers[id];
            if (!identifiers) identifiers = this.sourceIdToIdentifiers[id] = [];
            identifiers.push(source.identifier);
        }

        this.chartConfig = chartExt;
    }

    public updateAlerts(identifierToState: Lookup<VisualizationDataSourceState>)
    {
        let someAreSignificant = false;
        for (let alert of this.alertsFlat)
        {
            let identifiers = this.sourceIdToIdentifiers[alert.sysId];
            for (let identifier of identifiers)
            {
                let state                = identifierToState[identifier];
                alert.significantInChart = !state || isVisible(state) && state !== VisualizationDataSourceState.Muted;
                if (alert.significantInChart)
                {
                    someAreSignificant = true;
                    break;
                }
            }
        }

        this.allAlertsInsignificant = !someAreSignificant;
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();
        if (this.m_livePreview)
        {
            this.m_livePreview.unregisterSecondaryButton();
        }
    }
}

export class ReportElementAlertExecutionSummaryData extends ReportElementDataBase
{
    public static async newInstance(alertDefinition: AlertDefinitionExtended,
                                    alertVersion: AlertDefinitionVersionExtended,
                                    rangeStart: moment.Moment,
                                    rangeEnd: moment.Moment,
                                    maxSteps = 5000,
                                    interval = 60): Promise<() => Promise<{ progress: number, results: ReportElementAlertExecutionSummaryData }>>
    {
        let data                    = new ReportElementAlertExecutionSummaryData();
        data.alertDefinitionVersion = alertVersion;
        data.alertDefinition        = alertDefinition;

        let progressId = await alertVersion.evaluateOverRange(rangeStart.toDate(), rangeEnd.toDate(), maxSteps, interval);

        return async () =>
        {
            let progress = await alertVersion.checkEvaluation(progressId, false);
            if (progress.current < progress.end)
            {
                let percentage = 100 * (progress.current.valueOf() - progress.start.valueOf()) / (progress.end.valueOf() - progress.start.valueOf());
                return {
                    progress: percentage,
                    results : null
                };
            }

            progress = await alertVersion.checkEvaluation(progressId, true);

            let data                    = new ReportElementAlertExecutionSummaryData();
            data.alertDefinitionVersion = alertVersion;
            data.alertDefinition        = alertDefinition;
            data.logEntries             = progress.logEntries;

            let alerts: Lookup<AlertDetails> = {};

            for (let actions of progress.results?.evaluationResults || [])
            {
                let time = MomentHelper.parse(actions.timestamp);

                for (let step of actions.steps)
                {
                    if (step.failure)
                    {
                        let count = data.failures[step.failure];
                        if (count === undefined)
                        {
                            count = 0;
                        }

                        data.failures[step.failure] = count + 1;
                    }

                    if (step instanceof Models.AlertEngineExecutionStepCreateAlert)
                    {
                        let alert            = new AlertDetails();
                        alert.assetSysId     = step.controlPoint.sysId;
                        alert.alertSysId     = step.record.sysId;
                        alert.status         = Models.AlertStatus.active;
                        alert.type           = step.type;
                        alert.severity       = step.severity;
                        alert.eventTimestamp = MomentHelper.parse(step.timestamp || time);
                        alert.eventType      = AlertEventType.created;

                        this.registerAlert(data, alerts, alert);
                    }

                    if (step instanceof Models.AlertEngineExecutionStepSetAlertStatus)
                    {
                        let previousAlert = alerts[step.record.sysId];
                        if (previousAlert)
                        {
                            let alert            = new AlertDetails();
                            alert.assetSysId     = previousAlert.assetSysId;
                            alert.alertSysId     = previousAlert.alertSysId;
                            alert.status         = step.status;
                            alert.type           = previousAlert.type;
                            alert.severity       = previousAlert.severity;
                            alert.eventTimestamp = MomentHelper.parse(step.timestamp || time);

                            switch (step.status)
                            {
                                case Models.AlertStatus.active:
                                    switch (previousAlert.status)
                                    {
                                        case Models.AlertStatus.muted:
                                            alert.eventType = Models.AlertEventType.unmuted;
                                            break;

                                        case Models.AlertStatus.resolved:
                                        case Models.AlertStatus.closed:
                                            alert.eventType = Models.AlertEventType.reopened;
                                            break;

                                        default:
                                            alert.eventType = Models.AlertEventType.updatedWithNotes;
                                            break;
                                    }
                                    break;

                                case Models.AlertStatus.muted:
                                    alert.eventType = Models.AlertEventType.muted;
                                    break;

                                case Models.AlertStatus.resolved:
                                    alert.eventType = Models.AlertEventType.resolved;
                                    break;

                                case Models.AlertStatus.closed:
                                    alert.eventType = Models.AlertEventType.closed;
                                    break;
                            }

                            this.registerAlert(data, alerts, alert);
                        }
                    }
                }
            }

            return {
                progress: 100,
                results : data
            };
        };
    }

    private static registerAlert(data: ReportElementAlertExecutionSummaryData,
                                 alerts: Lookup<AlertDetails>,
                                 alert: AlertDetails)
    {
        data.alerts.push(alert);

        alerts[alert.alertSysId] = alert;
    }

    alertDefinition: AlertDefinitionExtended;
    alertDefinitionVersion: AlertDefinitionVersionExtended;
    alerts: AlertDetails[]   = [];
    failures: Lookup<number> = {};
    logEntries: Models.LogLine[];
    range: Models.RangeSelection;
}

export class ReportElementAlertExecutionSummaryConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AlertExecutionSummary;
        model.configuration = new ReportElementAlertExecutionSummaryConfiguration();
        return model;
    }
}

class PointAlertsFlat
{
    sysId: string;

    deviceName: string;

    alerts: AlertDetails[];

    alertCount: number;

    significantInChart: boolean = true;
}

import {Component, Input, ViewChild} from "@angular/core";

import {TimeSeriesChartConfigurationExtended, TimeSeriesChartHandler} from "app/customer/visualization/time-series-utils";
import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {GraphContextUpdater} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";
import {ChartSetComponent, ExternalGraphChart} from "app/shared/charting/chart-set.component";

import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {mapInParallel} from "framework/utils/concurrency";
import {AsyncDebouncer} from "framework/utils/debouncers";

import {Subscription} from "rxjs";

@Component({
               selector   : "o3-widget-editor-wizard-time-series-step",
               templateUrl: "./widget-editor-wizard-time-series-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardTimeSeriesStepComponent)]
           })
export class WidgetEditorWizardTimeSeriesStepComponent extends WizardStep<WidgetEditorWizardState>
{
    @Input() viewWindow: VerticalViewWindow;

    externalGraphCharts: ExternalGraphChart[];

    private m_contextChangedSubs: Subscription[][];
    private m_externalContextUpdaters: GraphContextUpdater[][];
    private m_graphChangeListener: Subscription;

    private m_validCharts: boolean        = true;
    private readonly m_validatorDebouncer = new AsyncDebouncer(100, async () =>
    {
        this.m_validCharts = await this.areValidCharts();
        if (this.isValid()) this.wizard.markForCheck();
    });

    @ViewChild("test_chartSet") test_chartSet: ChartSetComponent;

    public getLabel() { return "Configure Charts"; }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.TimeSeriesWidgetConfiguration);
    }

    public isValid()
    {
        if (!this.data.editor.timeseriesRange) return false;
        if (!this.data.editor.timeSeriesCharts?.length) return false;
        if (this.m_validatorDebouncer.scheduled || this.m_validatorDebouncer.processing) return false;
        return this.m_validCharts;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
        if (!this.m_externalContextUpdaters) await this.updateExternalContextUpdaters();

        if (!this.m_graphChangeListener)
        {
            this.m_graphChangeListener = this.subscribeToObservable(this.data.editor.dashboardGraphsHost.graphsChanged, async () =>
            {
                await this.updateExternalContextUpdaters();
                await this.data.editor.updateExternalGraphs();
                this.updateExternalGraphCharts();
                this.markForCheck();
            });
        }

        if (!this.externalGraphCharts) this.updateExternalGraphCharts();
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.destroyExternalContextUpdaters();
    }

    private destroyExternalContextUpdaters()
    {
        if (this.m_externalContextUpdaters)
        {
            for (let updaters of this.m_externalContextUpdaters)
            {
                for (let updater of updaters)
                {
                    updater.destroy();
                }
            }
        }
    }

    private async updateExternalContextUpdaters()
    {
        this.destroyExternalContextUpdaters();
        if (this.m_contextChangedSubs)
        {
            for (let subs of this.m_contextChangedSubs)
            {
                for (let sub of subs) sub.unsubscribe();
            }
            this.m_contextChangedSubs = null;
        }

        this.m_externalContextUpdaters = await mapInParallel(this.data.editor.timeSeriesCharts, async (chart) =>
        {
            switch (chart.type)
            {
                case Models.TimeSeriesChartType.GRAPH:
                case Models.TimeSeriesChartType.STANDARD:
                case Models.TimeSeriesChartType.COORDINATE:
                    let selectorIds = new Set<string>();
                    for (let binding of chart.graph?.externalBindings || [])
                    {
                        selectorIds.add(binding.selectorId);
                    }

                    let contextUpdaters = await mapInParallel([...selectorIds], (selectorId) => this.data.editor.dashboardExt.getContextUpdater(selectorId));
                    return contextUpdaters.filter((updater) => !!updater);

                default:
                    return [];
            }
        });

        this.m_contextChangedSubs = this.m_externalContextUpdaters.map((updaters) => updaters.map(
            (updater) => this.subscribeToObservable(updater.selectionChanged, async () =>
            {
                await this.data.editor.updateExternalGraphs();
                this.updateExternalGraphCharts();
                this.markForCheck();
            })));

        this.markForCheck();
    }

    private updateExternalGraphCharts()
    {
        if (this.externalGraphCharts?.length === this.data.editor.timeSeriesCharts.length)
        {
            for (let i = 0; i < this.externalGraphCharts.length; i++)
            {
                let externalGraphChart = this.externalGraphCharts[i];

                externalGraphChart.externalGraph   = this.data.editor.externalGraphs[i];
                externalGraphChart.contextUpdaters = this.m_externalContextUpdaters[i];
            }
        }
        else
        {
            this.externalGraphCharts = [];
            for (let i = 0; i < this.data.editor.timeSeriesCharts.length; i++)
            {
                this.externalGraphCharts[i] = new ExternalGraphChart(this.data.editor.timeSeriesCharts[i], this.data.editor.externalGraphs[i], this.m_externalContextUpdaters[i]);
            }
        }
    }

    public async chartConfigsChanged(configs: Models.TimeSeriesChartConfiguration[])
    {
        this.data.editor.timeSeriesCharts = configs;
        await this.data.editor.updateExternalGraphs();
        this.updateExternalGraphCharts();
        this.triggerChartValidation();
    }

    public handleChartUpdated(configUpdated: boolean)
    {
        if (configUpdated) this.triggerChartValidation();
    }

    private triggerChartValidation()
    {
        if (!this.m_validatorDebouncer.scheduled && !this.m_validatorDebouncer.processing) this.wizard.markForCheck();
        this.m_validatorDebouncer.invoke();
    }

    private async areValidCharts(): Promise<boolean>
    {
        this.data.editor.syncTimeseriesConfig();
        let hasSources = await mapInParallel(this.data.editor.timeSeriesCharts, async (model: Models.TimeSeriesChartConfiguration) =>
        {
            let modelClone   = Models.TimeSeriesChartConfiguration.deepClone(model);
            let configExt    = await TimeSeriesChartConfigurationExtended.newInstance(this.data.app, modelClone, undefined, this.data.editor.externalGraphs?.[0]);
            let chartHandler = await TimeSeriesChartHandler.newInstance(configExt);
            return chartHandler.hasSources();
        });

        return hasSources.every((valid) => valid);
    }
}

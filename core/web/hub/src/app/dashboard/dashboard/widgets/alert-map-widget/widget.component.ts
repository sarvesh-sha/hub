import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {ContextPaneComponent} from "app/dashboard/context-pane/panes/context-pane.component";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {AlertLocation, WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AlertMapComponent, AlertPinMetadata} from "app/shared/mapping/alert-map/alert-map.component";

import {UtilsService} from "framework/services/utils.service";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector       : "o3-alert-map-widget",
               templateUrl    : "./widget.template.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertMapWidgetComponent extends WidgetBaseComponent<Models.AlertMapWidgetConfiguration, AlertMapWidgetConfigurationExtended>
{
    @ViewChild(AlertMapComponent, {static: true}) private alertMap: AlertMapComponent;
    @ViewChild(OverlayComponent) private paneOverlay: OverlayComponent;

    contextPaneDialogConfig: OverlayConfig;
    paneModels: Models.Pane[];

    selectedPin: AlertPinMetadata;

    private m_paneConfig: Models.PaneConfiguration;

    get hasPaneConfig(): boolean
    {
        return !!this.m_paneConfig;
    }

    get popupTitle(): string
    {
        let count = this.selectedPin?.count;
        if (isNaN(count)) return "";

        return `${count} ${UtilsService.pluralize("Alert", count)}`;
    }

    async handleAlertFocus(alertMetadata: AlertPinMetadata)
    {
        if (this.hasPaneConfig)
        {
            this.paneModels = await this.getPaneModel(alertMetadata.alert);
            if (this.paneModels.length)
            {
                this.detectChanges();
                this.paneOverlay.toggleOverlay();
                return;
            }
        }

        this.selectedPin = alertMetadata;
        this.markForCheck();
    }

    async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Alert Map";

        let clickBehavior = this.config.clickBehavior;
        let paneConfigId  = clickBehavior?.type === Models.InteractionBehaviorType.Pane && clickBehavior.paneConfigId;
        if (paneConfigId)
        {
            try
            {
                this.m_paneConfig            = await this.app.domain.panes.getConfig(paneConfigId);
                this.contextPaneDialogConfig = ContextPaneComponent.getOverlayConfig(this.app.ui.overlay);
            }
            catch (err)
            {
                console.error("Failed to load pane configuration");
            }
        }
    }

    public async refreshSize(): Promise<boolean>
    {
        return !!this.alertMap?.refreshSize();
    }

    public async refreshContent(): Promise<void>
    {
        await super.refreshContent();
        await this.alertMap.refreshPins();
    }

    private async getPaneModel(alertLocation: AlertLocation): Promise<Models.Pane[]>
    {
        let context = Models.AssetGraphContextLocation.newInstance({locationSysId: alertLocation.id});
        return this.app.domain.panes.evaluate(this.m_paneConfig, [context]);
    }

    protected getClipboardData(): ClipboardEntryData<Models.AlertMapWidgetConfiguration, null>
    {
        let model = Models.AlertMapWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.AlertMapWidgetConfiguration, null>
        {
            constructor()
            {
                super("alert map");
            }

            public getDashboardWidget(): Models.AlertMapWidgetConfiguration
            {
                return Models.AlertMapWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Alert Map",
               typeName          : "ALERT_MAP",
               model             : Models.AlertMapWidgetConfiguration,
               component         : AlertMapWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 5,
               hostScalableText  : false,
               needsProtector    : true,
               documentation     : {
                   description: "The Alert Map widget allows you to display alerts on a map, aggregated by location. The Map can be configured to the desired pin style and pin color.",
                   examples   : [
                       {
                           file       : "widgets/ALERT_MAP/single.png",
                           label      : "Single Alert",
                           description: "A single alert plotted on a map, displayed as a 6x5 widget."
                       },
                       {
                           file       : "widgets/ALERT_MAP/multiple.png",
                           label      : "Multiple Alert",
                           description: "Multiple alerts plotted and aggregated on a map, displayed as a 6x6 widget."
                       }
                   ]
               }

           })
export class AlertMapWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AlertMapWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        let model = this.model;

        model.center        = "United States of America";
        model.options       = Models.AlertMapOptions.newInstance({zoom: 10});
        model.clickBehavior = Models.InteractionBehavior.newInstance({type: Models.InteractionBehaviorType.Standard});
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}

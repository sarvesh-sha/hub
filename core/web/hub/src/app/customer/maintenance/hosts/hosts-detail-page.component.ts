import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {DeviceElementExtended, HostExtended} from "app/services/domain/assets.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-hosts-detail-page",
               templateUrl: "./hosts-detail-page.component.html",
               styleUrls  : ["./hosts-detail-page.component.scss"]
           })
export class HostsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    hostId: string;

    host: HostExtended;
    hostRemoveChecks: Models.ValidationResult[];
    hostNoDeleteReason: string;

    operationalStates: ControlOption<Models.AssetState>[];

    //--//

    range: Models.RangeSelection                 = new Models.RangeSelection();
    config: TimeSeriesChartConfigurationExtended = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);

    chartInitialized                  = false;
    elements: ControlOption<string>[] = [];
    deviceElementId: string           = "";
    deviceElement: DeviceElementExtended;

    sources: ControlOption<string>[] = [];
    selectedSource: string           = "";
    secondarySource: string          = "";

    //--//


    @ViewChild("hostForm", {static: true}) hostForm: NgForm;

    constructor(inj: Injector)
    {
        super(inj);

        // Select default time range
        this.range.range = Models.TimeRangeId.Last24Hours;

        this.host = this.app.domain.assets.wrapTypedModel(HostExtended, new Models.HostAsset());
    }

    protected async onNavigationComplete()
    {
        this.hostId             = this.getPathParameter("id");
        this.hostNoDeleteReason = "<checking...>";

        if (this.hostId)
        {
            await this.loadHost();
        }
    }

    async loadHost()
    {
        // load host info
        this.host = await this.app.domain.assets.getTypedExtendedById(HostExtended, this.hostId);
        if (!this.host)
        {
            this.exit();
            return;
        }

        this.operationalStates = await this.app.domain.assets.getOperationalStates();

        this.hostRemoveChecks   = await this.host.checkRemove();
        this.hostNoDeleteReason = this.fromValidationToReason("Remove is disabled because:", this.hostRemoveChecks);

        // set breadcrumbs
        let model                                     = this.host.typedModel;
        this.app.ui.navigation.breadcrumbCurrentLabel = model.name;
    }

    async initializeTrendChart()
    {
        await this.waitUntilTrue(10, () => !!this.hostId);

        if (!this.chartInitialized)
        {
            this.chartInitialized = true;

            let filters = Models.DeviceElementFilterRequest.newInstance({parentIDs: [this.hostId]});

            let response = await this.app.domain.assets.getList(filters);
            let ids      = response.results;
            let points   = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, ids);

            points.sort((a,
                         b) => UtilsService.compareStrings(a.model.name, b.model.name, true));

            this.elements      = [];
            this.deviceElement = null;

            for (let point of points)
            {
                let label      = point.model.name || "";
                let labelParts = label.split("/");
                let elements   = this.elements;
                let parentOption: ControlOption<string>;

                for (let i = 0; i < labelParts.length; i++)
                {
                    let labelPart = labelParts[i];
                    if (labelPart.length == 0) continue;

                    let option = elements.find((el) => el.label == labelPart);
                    if (!option)
                    {
                        option                  = new ControlOption<string>(undefined, labelPart);
                        option.disableSelection = true;

                        elements.push(option);
                    }

                    parentOption = option;
                    elements     = option.children;
                }

                if (parentOption)
                {
                    parentOption.id               = point.model.sysId;
                    parentOption.disableSelection = false;
                }
            }
        }
    }

    async refreshDeviceElement()
    {
        this.deviceElement = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, this.deviceElementId);

        this.refreshSources();
        this.updateConfig();
    }

    async updateConfig()
    {
        if (this.deviceElement)
        {
            let host         = new TimeSeriesSourceHost(this);
            let mainExt      = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, this.deviceElement.typedModel.sysId, this.selectedSource);
            let secondaryExt = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, this.deviceElement.typedModel.sysId, this.secondarySource);

            this.config                                    = await TimeSeriesChartConfigurationExtended.generateNewInstanceFromSources(this.app, mainExt, secondaryExt);
            this.config.model.display.automaticAggregation = true;
        }
        else
        {
            this.config = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);
        }
    }

    @ReportError
    async save()
    {
        await this.host.save();
        this.exit();
    }

    @ReportError
    async remove()
    {
        if (this.host)
        {
            if (await this.confirmOperation("Click Yes to confirm deletion of this Host."))
            {
                let name = this.host.model.name || this.host.model.sysId;
                let msg  = this.app.framework.errors.success(`Deleting Host '${name}'...`, -1);

                let promise = this.host.remove();

                // Navigate away without waiting for deletion, since it can take a long time.
                this.exit();

                if (await promise)
                {
                    this.app.framework.errors.dismiss(msg);
                    this.app.framework.errors.success(`Host '${name}' deleted`, -1);
                }
            }
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    showLog()
    {
        this.app.ui.navigation.go("/hosts/host", [
            this.hostId,
            "log"
        ]);
    }

    //--//

    private async refreshSources()
    {
        let sources = [];

        if (this.deviceElement)
        {
            let pointSchema = await this.deviceElement.fetchSchema();
            let keys        = UtilsService.extractKeysFromMap(pointSchema);

            keys.sort((a,
                       b) => UtilsService.compareStrings(a, b, true));

            let oldSource1 = this.selectedSource || "requests";
            let oldSource2 = this.secondarySource;

            this.selectedSource  = null;
            this.secondarySource = null;

            for (let prop of keys)
            {
                let schema = pointSchema[prop];

                sources.push(new ControlOption<string>(schema.name, schema.name));

                if (schema.name == oldSource1) this.selectedSource = oldSource1;
                if (schema.name == oldSource2) this.secondarySource = oldSource2;
            }
        }
        else
        {
            this.selectedSource  = null;
            this.secondarySource = null;
        }

        this.sources = sources;
        await this.updateConfig();
    }
}

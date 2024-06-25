import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {AppContext, ReportError} from "app/app.service";

import {ReportViewDialogComponent} from "app/customer/configuration/reports/report-view-dialog.component";
import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";

import {DeviceElementExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {BookmarkEditorDialogComponent} from "app/shared/bookmarks/bookmark-editor-dialog.component";
import {TimeSeriesDownloader} from "app/shared/charting/time-series-downloader";
import {DatatablePair, IProviderForMapHost, ProviderForString} from "app/shared/tables/provider-for-map";
import {AlertTimelineItem, TimelineItem} from "app/shared/timelines/timeline.component";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {DatatableComponent} from "framework/ui/datatables/datatable.component";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-device-elements-detail-page",
               templateUrl: "./device-elements-detail-page.component.html",
               styleUrls  : ["./device-elements-detail-page.component.scss"]
           })
export class DeviceElementsDetailPageComponent extends SharedSvc.BaseComponentWithRouter implements IProviderForMapHost
{
    deviceElementID: string;
    deviceElement: DeviceElementExtended;
    deviceElementManualTags: string[] = [];
    pointClassDisplayName: string     = "Loading...";

    deviceElementUpdateChecks: Models.ValidationResult[];
    deviceElementNoUpdateReason: string;

    currUnits: EngineeringUnitsDescriptorExtended;

    private m_currValue: any;
    get currValue(): string
    {
        let value = this.m_currValue ?? undefined;
        return value === undefined ? "Loading..." : `${value}`;
    }

    get bookmarkOptionLabel(): string
    {
        return this.existingBookmark ? "Edit Bookmark" : "Add to Bookmarks";
    }

    canSetValue: boolean;
    isTextValue: boolean;
    isNumberValue: boolean;
    isEnumeratedValue: boolean;
    possibleValues: Models.TimeSeriesEnumeratedValue[];

    desiredValue: any;
    newValue: any;

    location: LocationExtended;
    locationModel: Models.Location;
    locationModelReady = false;

    sampling: DeviceElementSamplingExtended[];
    samplingLookup: Map<string, DeviceElementSamplingExtended>;
    unitsFactors: Models.EngineeringUnitsFactors;

    cpDetailsAvailable: number;
    cpPropertyExpanded: Lookup<boolean>;
    cpDetailProvider: ProviderForString;

    get isPristine(): boolean
    {
        return this.formPristine && this.samplingPristine;
    }

    private get formPristine(): boolean
    {
        if (this.deviceElementForm)
        {
            if (!this.deviceElementForm.pristine) return false;
        }

        return UtilsService.compareArraysAsSets(this.deviceElementManualTags, this.deviceElement.model.manualTags);
    }

    private get samplingPristine(): boolean
    {
        for (let sampling of this.sampling || [])
        {
            if (!sampling.pristine)
            {
                return false;
            }
        }

        return true;
    }

    //--//

    existingBookmark: Models.BookmarkConfiguration;

    history: AlertTimelineItem[];

    workflowOverlayConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    @ViewChild("deviceElementForm") deviceElementForm: NgForm;
    @ViewChild("detailDatatable") detailDatatable: DatatableComponent<string, string, DatatablePair<string, string>>;

    constructor(inj: Injector)
    {
        super(inj);

        this.deviceElement = this.app.domain.assets.wrapTypedModel(DeviceElementExtended, new Models.DeviceElement());
    }

    static navigate(app: AppContext,
                    controlPoint: DeviceElementExtended)
    {
        let segments = [
            controlPoint.model.parentAsset.sysId,
            "element",
            controlPoint.model.sysId
        ];

        app.ui.navigation.go("/devices/device", segments);
    }

    protected async onNavigationComplete()
    {
        this.deviceElementID = this.getPathParameter("elementId");

        await this.loadDeviceElement();
    }

    protected shouldDelayNotifications(): boolean
    {
        return !this.deviceElementForm.pristine;
    }

    async loadDeviceElement()
    {
        let deviceElement = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, this.deviceElementID);
        if (!deviceElement)
        {
            this.exit();
            return;
        }

        this.deviceElement = deviceElement;

        this.deviceElementManualTags = deviceElement.model.manualTags;

        // set breadcrumbs
        this.app.ui.navigation.breadcrumbCurrentLabel = deviceElement.typedModel.identifier || "<unknown>";

        //--//

        this.removeAllDbSubscriptions();

        this.subscribeOneShot(deviceElement,
                              async (ext,
                                     action) => await this.loadDeviceElement());

        await Promise.all([
                              this.loadSchema(),
                              this.getCurrValue(),
                              this.loadPointClass(),
                              this.loadAlertHistory(),
                              this.checkUpdate(),
                              this.refreshLocation(),
                              this.loadBookmarks()
                          ]);
    }

    private async loadSchema()
    {
        let deviceElement = this.deviceElement;

        let sampling: DeviceElementSamplingExtended[] = [];
        let samplingLookup                            = new Map<string, DeviceElementSamplingExtended>();
        let deviceSchema                              = await deviceElement.fetchSchema();

        for (let prop in deviceSchema)
        {
            let propSchema = deviceSchema[prop];
            if (propSchema)
            {
                let ext = new DeviceElementSamplingExtended(this, propSchema, await this.app.domain.units.resolveDescriptor(propSchema.unitsFactors, false));
                sampling.push(ext);
                samplingLookup.set(prop, ext);
            }
        }

        let deviceElementSchema = deviceSchema[DeviceElementExtended.PRESENT_VALUE];

        this.generateControlPointDetails();

        //--//

        this.canSetValue       = undefined;
        this.isNumberValue     = undefined;
        this.isTextValue       = undefined;
        this.isEnumeratedValue = undefined;
        this.possibleValues    = undefined;

        if (deviceElement.typedModel.ableToUpdateState)
        {
            switch (deviceElementSchema?.type)
            {
                case Models.TimeSeriesSampleType.Enumerated:
                    if (deviceElementSchema.values)
                    {
                        this.possibleValues    = deviceElementSchema.values;
                        this.isEnumeratedValue = true;
                        this.canSetValue       = true;
                    }
                    break;

                case Models.TimeSeriesSampleType.Integer:
                case Models.TimeSeriesSampleType.Decimal:
                    this.isNumberValue = true;
                    this.canSetValue   = true;
                    break;
            }

            let desiredValue  = deviceElement.typedModel.desiredContents?.present_value;
            this.desiredValue = await deviceElement.getEnumValueDisplay(desiredValue) || desiredValue || "";
        }

        for (let val of deviceElement.typedModel.samplingSettings || [])
        {
            let propOut = samplingLookup.get(val.propertyName);
            if (propOut)
            {
                propOut.stateCurrent                = propOut.newState();
                propOut.stateCurrent.samplingPeriod = val.samplingPeriod;
                propOut.stateNext                   = propOut.newState();
                propOut.stateNext.samplingPeriod    = val.samplingPeriod;
            }
        }

        this.sampling       = sampling;
        this.samplingLookup = samplingLookup;

        let propSchema    = samplingLookup?.get(DeviceElementExtended.PRESENT_VALUE)?.info;
        this.unitsFactors = propSchema?.unitsFactors;
    }

    private generateControlPointDetails(): void
    {
        let keys                         = UtilsService.extractKeysFromMap(this.deviceElement.typedModel.contents, true, "__type", "__type_bacnet", "__type_can", "__type_ipn");
        let contentsKeys: Lookup<string> = {};
        this.cpPropertyExpanded          = {};
        this.cpDetailsAvailable          = 0;
        for (let key of keys)
        {
            contentsKeys[key]            = "" + this.getElementValue(key);
            this.cpPropertyExpanded[key] = false;
            this.cpDetailsAvailable++;
        }

        if (!this.cpDetailProvider)
        {
            this.cpDetailProvider = new ProviderForString(this, "control point details", "", "Property", "Value");
        }

        this.cpDetailProvider.bind(contentsKeys);
    }

    private async checkUpdate()
    {
        this.deviceElementUpdateChecks   = await this.deviceElement.checkUpdate();
        this.deviceElementNoUpdateReason = this.fromValidationToReason("Update is disabled because:", this.deviceElementUpdateChecks);
    }

    async loadAlertHistory()
    {
        let history  = await this.deviceElement.getAlertHistory();
        this.history = AlertTimelineItem.createList(history.map((ext) => ext.model));
    }

    async loadPointClass()
    {
        let normalization           = await this.app.domain.normalization.getActiveRules();
        let pointClassOptions       = await this.app.bindings.getPointClasses(false, normalization.rules);
        let pointClass              = this.deviceElement.model.pointClassId;
        let pointClassControlOption = pointClassOptions.find((option) => option.id === pointClass);

        this.pointClassDisplayName = pointClassControlOption?.label || "No Point Class";
    }

    //--//

    private async generateBookmarkName(): Promise<string>
    {
        let deviceElementParent = await this.deviceElement.getParent();

        let deviceElemName       = this.deviceElement.model.name;
        let deviceElemParentName = deviceElementParent?.model.name;
        if (deviceElemParentName)
        {
            deviceElemName = `${deviceElemName} of ${deviceElemParentName}`;
        }

        let locationName = this.location?.model.name;
        if (locationName)
        {
            deviceElemName = `${deviceElemName} in ${locationName}`;
        }

        return deviceElemName;
    }

    async loadBookmarks()
    {
        this.existingBookmark = await this.app.domain.bookmarks.getBookmarkByRecordID(this.deviceElement.model.sysId);
    }

    async openBookmarkOverlay()
    {
        BookmarkEditorDialogComponent.open(this, !!this.existingBookmark, await this.generateBookmarkName(),
                                           (name,
                                            description) => this.saveBookmark(name, description),
                                           this.existingBookmark);
    }

    async getCurrValue()
    {
        let lastValue = await this.deviceElement.getLastValue(DeviceElementExtended.PRESENT_VALUE, this.currUnits?.model.factors);
        if (lastValue)
        {
            let value        = await this.deviceElement.getEnumValueDisplay(lastValue.value) || lastValue.value;
            this.m_currValue = value ?? "N/A";
        }
        else
        {
            this.m_currValue = "N/A";
        }
    }

    @ReportError
    async updateDesiredValue()
    {
        let state: Lookup<any>                     = {};
        state[DeviceElementExtended.PRESENT_VALUE] = this.newValue;
        await this.deviceElement.setDesiredState(state);
        this.newValue = undefined;
    }

    async locationChanged(selectedLocationID: string)
    {
        this.deviceElement.setLocation(selectedLocationID);

        await this.refreshLocation();

        this.deviceElementForm.form.markAsDirty();
    }

    private async refreshLocation()
    {
        this.location           = await this.deviceElement.getLocation();
        this.locationModel      = this.location ? this.location.typedModel : new Models.Location();
        this.locationModelReady = true;
    }

    getElementValue(key: string): string | number | boolean
    {
        let value = this.deviceElement.typedModel.contents[key];
        switch (typeof value)
        {
            case "boolean":
            case "number":
            case "string":
                return value;
        }

        return JSON.stringify(value, null);
    }

    getSamplePossibleValues(values: Models.TimeSeriesEnumeratedValue[]): string
    {
        if (values && values.length)
        {
            return values.map((value) => value.name)
                         .join(", ");
        }

        return "";
    }

    async visualize()
    {
        await DataExplorerPageComponent.visualizeDeviceElement(this, this.app.ui.navigation, this.app.ui.viewstate, this.deviceElementID);
    }

    @ReportError
    async trimSamples(maxDays: number)
    {
        await this.deviceElement.trimValues(maxDays);
    }

    @ReportError
    private async saveBookmark(name: string,
                               description: string): Promise<Models.BookmarkConfiguration>
    {
        if (this.existingBookmark)
        {
            this.existingBookmark.description = description;
            await this.app.domain.bookmarks.updateBookmark(this.existingBookmark);
        }
        else if (this.deviceElement)
        {
            let deviceElementParent = await this.deviceElement.getParent();

            this.existingBookmark = await this.app.domain.bookmarks.generateBookmark(await this.generateBookmarkName(), description, Models.BookmarkType.DEVICE_ELEMENT,
                                                                                     this.deviceElement.model.sysId, deviceElementParent?.model.sysId);
        }

        if (this.existingBookmark) this.app.framework.errors.success("Bookmark saved", -1);

        return this.existingBookmark;
    }

    private updateSampling(): void
    {
        let samplingSettings = this.deviceElement.typedModel.samplingSettings || [];
        // get all that were not in samplingLookup so those not in schema but in sampling settings do not get removed - unnecessary?
        samplingSettings     = samplingSettings.filter((samplingSetting) => !this.samplingLookup.get(samplingSetting.propertyName));

        // add in all enabled locally
        for (let samplingSetting of this.sampling)
        {
            if (samplingSetting.enabled)
            {
                samplingSettings.push(samplingSetting.stateNext);
                samplingSetting.stateCurrent = samplingSetting.stateNext;
            }
        }

        // now update all
        this.deviceElement.typedModel.samplingSettings = samplingSettings;
        this.sampling                                  = [...this.sampling];
    }

    @ReportError
    async save()
    {
        if (!this.samplingPristine) this.updateSampling();

        await this.deviceElement.save();
        this.deviceElementForm.form.markAsPristine();

        this.deviceElementManualTags = this.deviceElement.model.manualTags;

        this.app.framework.errors.success("Device element updated", -1);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    viewAlert(item: TimelineItem): void
    {
        if (item)
        {
            this.app.ui.navigation.go("/alerts/alert", [item.eventId]);
        }
    }

    async exportToExcel()
    {
        let range = await ReportViewDialogComponent.open(this, null, "Generate Excel");
        if (!range) return;

        let elements = [
            {
                name   : this.deviceElement.model.name,
                element: this.deviceElement,
                units  : this.currUnits?.model.factors
            }
        ];

        let min            = range.range.minAsMoment;
        let max            = range.range.maxAsMoment;
        let dataDownloader = new TimeSeriesDownloader(this.app.domain, elements, min.clone(), max.clone());
        let minDate        = MomentHelper.fileNameFormat(min);
        let maxDate        = MomentHelper.fileNameFormat(max);
        let fileName       = `timeseries__${this.deviceElement.model.name}__${minDate}-${maxDate}.xlsx`;
        DownloadDialogComponent.openWithGenerator(this, "Export Data", fileName, dataDownloader);
    }

    //--//

    public setDirty(): void
    {
    }

    public samplingChanged(period: number): void
    {
        //
        // Make sure all the properties have the same sampling period, since they all share the same timestamp.
        //
        for (let row of this.sampling)
        {
            if (row.stateNext)
            {
                row.stateNext.samplingPeriod = period;
            }
        }
    }
}

class DeviceElementSamplingExtended
{
    stateCurrent: Models.DeviceElementSampling;
    stateNext: Models.DeviceElementSampling;

    expanded: boolean = false;

    get pristine(): boolean
    {
        if (this.stateCurrent)
        {
            if (this.stateNext)
            {
                return this.stateCurrent.samplingPeriod == this.stateNext.samplingPeriod;
            }

            return false;
        }
        else
        {
            return !this.stateNext;
        }
    }

    get enabled(): boolean
    {
        return !!this.stateNext;
    }

    set enabled(val: boolean)
    {
        if (val)
        {
            this.stateNext                = this.newState();
            this.stateNext.samplingPeriod = this.stateCurrent ? this.stateCurrent.samplingPeriod : 900;
        }
        else
        {
            this.stateNext = undefined;
        }
    }

    get samplingPeriod(): number
    {
        return this.stateNext ? this.stateNext.samplingPeriod : undefined;
    }

    set samplingPeriod(period: number)
    {
        if (this.stateNext && period > 0)
        {
            this.owner.samplingChanged(period);
        }
    }

    get units(): string
    {
        if (this.unitsExt?.model?.family)
        {
            return `${this.info.unitsFactors?.primary} (${this.unitsExt.model.family})`;
        }

        return `${this.info.unitsFactors?.primary}`;
    }

    constructor(public owner: DeviceElementsDetailPageComponent,
                public info: Models.TimeSeriesPropertyType,
                public unitsExt: EngineeringUnitsDescriptorExtended)
    {
    }

    newState(): Models.DeviceElementSampling
    {
        let state          = new Models.DeviceElementSampling();
        state.propertyName = this.info.name;
        return state;
    }
}

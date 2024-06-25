import {Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import {TimeSeriesSourceConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {AssetsService, DeviceElementExtended} from "app/services/domain/assets.service";
import * as Models from "app/services/proxy/model/models";
import {TimeDurationExtended} from "app/shared/forms/time-range/time-duration-extended";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {ILazyLoader, ILazyTreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-add-related-source[source]",
               templateUrl: "./add-related-source.component.html"
           })
export class AddRelatedSourceComponent extends BaseComponent
{
    private app: AppContext;
    config = OverlayConfig.newInstance({width: "550px"});

    private m_source: TimeSeriesSourceConfigurationExtended;
    @Input() set source(source: TimeSeriesSourceConfigurationExtended)
    {
        if (source)
        {
            this.m_source = source;
            this.updateLazyLoader();
        }
    }

    lazyLoader: ILazyLoader<string>;

    @Input() sources: TimeSeriesSourceConfigurationExtended[];

    @Output() addSource: EventEmitter<AddTimeSeriesSourceEvent> = new EventEmitter();

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;

    shiftAmount: Models.TimeDuration;

    selectedId: string;

    constructor(inj: Injector,
                private assets: AssetsService)
    {
        super(inj);

        this.app = new AppContext(inj);

        this.resetInputs();
    }

    requestNewSource()
    {
        this.addSource.emit(new AddTimeSeriesSourceEvent(this.selectedId, this.isSameSource() ? this.shiftAmount : TimeDurationExtended.newModel()));
    }

    resetInputs()
    {
        this.selectedId         = undefined;
        this.shiftAmount        = TimeDurationExtended.newModel();
        this.shiftAmount.amount = 1;
    }

    isSameSource(): boolean
    {
        return this.selectedId && this.selectedId === this.m_source.model.id;
    }

    async updateLazyLoader(): Promise<void>
    {
        this.lazyLoader = null;
        this.detectChanges();

        let deviceElemExt = await this.assets.getTypedExtendedById(DeviceElementExtended, this.m_source.model.id);
        if (!deviceElemExt) return;

        let enumValues   = [
            Models.AssetRelationship.controls,
            Models.AssetRelationship.structural
        ];
        let parentArrays = await mapInParallel(enumValues, (relationship) => deviceElemExt.getParentsOfRelation(relationship));

        let filters = parentArrays.map((parentArr) => Models.DeviceElementFilterRequest.newInstance({
                                                                                                        parentIDs     : parentArr.map(ri => ri.sysId),
                                                                                                        hasAnySampling: true
                                                                                                    }));

        let ofSameParentArrs = await mapInParallel(filters, async (filter) =>
        {
            let response = await this.app.domain.assets.getList(filter);
            return response.results || [];
        });

        let [sameEquipmentChildren, sameDeviceChildren] = ofSameParentArrs.map(
            (ofSameParentArr) => ofSameParentArr.map((sameParentItem) => <ILazyTreeNode<string>>{id: sameParentItem.sysId}));

        const sameDevicesId: string   = "sameDeviceElem";
        const sameEquipmentId: string = "sameEquipment";
        const sameDeviceId: string    = "sameDevice";

        this.lazyLoader = {
            getTree: async () =>
            {
                return [
                    AddRelatedSourceComponent.newLazyNode(sameDevicesId, [AddRelatedSourceComponent.newLazyNode(deviceElemExt.model.sysId, null)]),
                    AddRelatedSourceComponent.newLazyNode(sameEquipmentId),
                    AddRelatedSourceComponent.newLazyNode(sameDeviceId)
                ];
            },

            // nodes are ofSameEquipment children, ofSameDevice children, or top level defined in getTree
            loadNodes: async (nodes) =>
            {
                let options: ControlOption<string>[] = [];

                if (nodes[0].id === sameDevicesId)
                {
                    let sameDeviceElemOption = new ControlOption(deviceElemExt.model.sysId, deviceElemExt.model.name);
                    let ofSameDeviceElement  = new ControlOption(sameDevicesId, "From Same Control Point", [sameDeviceElemOption], true);
                    let ofSameEquipment      = new ControlOption(sameEquipmentId, "From Same Equipment", null, true);
                    let ofSameDevice         = new ControlOption(sameDeviceId, "From Same Device", null, true);

                    options.push(ofSameDeviceElement);
                    if (sameEquipmentChildren.length > 0)
                    {
                        ofSameEquipment.lazyChildren = sameEquipmentChildren;
                        ofSameEquipment.hasChildren  = true;
                        options.push(ofSameEquipment);
                    }
                    if (sameDeviceChildren.length > 0)
                    {
                        ofSameDevice.lazyChildren = sameDeviceChildren;
                        ofSameDevice.hasChildren  = true;
                        options.push(ofSameDevice);
                    }
                }
                else
                {
                    let childRecords     = nodes.map((node) => DeviceElementExtended.newIdentity(node.id));
                    let childDeviceElems = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, childRecords);

                    options = childDeviceElems.map((deviceElem) => new ControlOption(deviceElem.model.sysId, deviceElem.model.name));
                }

                return options;
            },
            getLabel : async (selectedId: string) =>
            {
                let deviceElemExt = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, selectedId);
                return deviceElemExt?.model.name || "";
            }
        };

        this.markForCheck();
    }

    private static newLazyNode(id: string,
                               children: ILazyTreeNode<string>[] = []): ILazyTreeNode<string>
    {
        return {
            id      : id,
            children: children
        };
    }

    public toggleOverlay()
    {
        this.overlay.toggleOverlay();
    }
}

export class AddTimeSeriesSourceEvent
{
    constructor(public sysId: string,
                public duration: Models.TimeDuration)
    {}
}

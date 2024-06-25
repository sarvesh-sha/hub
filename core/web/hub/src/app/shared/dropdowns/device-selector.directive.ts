import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {DeviceExtended} from "app/services/domain/assets.service";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";
import {ILazyTreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";
import {mapInParallel} from "framework/utils/concurrency";

@Directive({
               selector: "[o3DeviceSelector]"
           })
export class DeviceSelectorDirective
{
    @Input("o3DeviceSelector")
    public sink: boolean;

    private m_isCRE: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    private async labelFromDeviceExt(deviceExt: DeviceExtended): Promise<string>
    {
        let label = deviceExt.model.name;
        if (this.m_isCRE)
        {
            label += ` - ${deviceExt.getIdentityDescriptor()}`;
        }
        else
        {
            let location     = await deviceExt.getLocation();
            let locationName = location?.model?.name || "<unknown>";
            label += ` - ${locationName}`;
        }

        return label;
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Devices";

        this.m_isCRE = await this.app.domain.settings.isCRE();

        this.selectComponent.lazyLoader = {
            getTree  : async () =>
            {
                let filters    = new Models.DeviceFilterRequest();
                filters.sortBy = [
                    Models.SortCriteria.newInstance({
                                                        column   : "name",
                                                        ascending: true
                                                    })
                ];

                let ids = await this.app.domain.assets.getList(filters);
                return ids.results.map((id) =>
                                       {
                                           return {
                                               id      : id.sysId,
                                               children: []
                                           };
                                       });
            },
            loadNodes: async (nodes: ILazyTreeNode<string>[]) =>
            {
                let ids     = nodes.map((n) => DeviceExtended.newIdentity(n.id));
                let devices = await this.app.domain.assets.getTypedExtendedBatch(DeviceExtended, ids);
                return mapInParallel(devices, async (device) =>
                {
                    let label = await this.labelFromDeviceExt(device);
                    return new ControlOption<string>(device.model.sysId, label, null);
                });
            },
            getLabel : async (selectedId: string) =>
            {
                let deviceExt = await this.app.domain.assets.getTypedExtendedById(DeviceExtended, selectedId);
                return this.labelFromDeviceExt(deviceExt);
            }
        };
    }
}

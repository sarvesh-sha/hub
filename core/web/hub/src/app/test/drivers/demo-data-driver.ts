import {Injectable} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {EquipmentFiltersAdapterComponent} from "app/shared/filter/asset/equipment-filters-adapter.component";
import {TestDriver} from "app/test/driver";

@Injectable({providedIn: "root"})
export class DemoDataDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async getNumAhus(): Promise<number>
    {
        const eqIdQuery    = EquipmentFiltersAdapterComponent.generateEquipmentClassIDsQuery([`${SimulatedEquipmentClassIdAHU}`]);
        const listResponse = await this.m_driver.app.domain.assets.getList(Models.AssetFilterRequest.newInstance({tagsQuery: eqIdQuery}));
        return listResponse.results.length;
    }
}

const SimulatedEquipmentClassIdAHU = 1;

export const ahuOptionLabel              = "AHU - Air Handler";
export const vavOptionLabel              = "VAV - Variable Air Volume Terminal Unit";
export const datOptionLabel              = "DAT - Discharge Air Temp";
export const co2OptionLabel              = "CO2 - CO2";
export const airflowHeatingSpOptionLabel = "AFlw-Htg Sp";

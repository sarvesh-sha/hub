import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";

export class BrandingConfigurationExtended
{
    constructor(private model: Models.BrandingConfiguration)
    {}

    static areEquivalent(modelA: Models.BrandingConfiguration,
                         modelB: Models.BrandingConfiguration,
                         considerHorizontal: boolean = true,
                         considerVertical: boolean   = true): boolean
    {
        if (UtilsService.compareJson(modelA, modelB)) return true;

        if (!considerHorizontal || !considerVertical)
        {
            modelA = Models.BrandingConfiguration.deepClone(modelA);
            modelB = Models.BrandingConfiguration.deepClone(modelB);

            if (!considerHorizontal) modelA.horizontalPlacement = modelB.horizontalPlacement = undefined;
            if (!considerVertical) modelA.verticalPlacement = modelB.verticalPlacement = undefined;

            return UtilsService.compareJson(modelA, modelB);
        }

        return false;
    }
}

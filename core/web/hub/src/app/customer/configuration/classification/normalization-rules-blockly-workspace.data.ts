import {BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

export class NormalizationRulesBlocklyWorkspaceData extends BlocklyWorkspaceData
{

    constructor(m_tabs: Models.EngineTab[],
                public rules: Models.NormalizationRules)
    {
        super(m_tabs);
    }

    getEquipmentClassDropdownOptions(): ControlOption<string>[]
    {
        let equipmentClasses = this.rules.equipmentClasses || [];

        let options: ControlOption<string>[] = [];
        for (let ec of equipmentClasses)
        {
            options.push(new ControlOption<string>(`${ec.id}`, `${ec.equipClassName} - ${ec.description}`));
        }

        options.sort((a,
                      b) => UtilsService.compareStrings(a.label, b.label ,true));

        return options;
    }
}

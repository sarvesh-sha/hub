import {BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";
import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";
import {ControlOption} from "framework/ui/control-option";

export class MetricConfigurationValues
{
    tags: ControlOption<string>[];
    pointClasses: ControlOption<string>[];
    equipmentClasses: ControlOption<string>[];

    static toDropdown(options: ControlOption<string>[]): Blockly.FieldDropdown
    {
        if (!options || !options.length)
        {
            options = [new ControlOption("", "<no values to select>")];
        }

        return new Blockly.FieldDropdown(options.map((cp) => [
            cp.label,
            cp.id
        ]));
    }
}

export class MetricBlocklyWorkspaceData extends BlocklyWorkspaceData
{
    constructor(public readonly configValues: MetricConfigurationValues,
                public readonly resolveNodes: () => string[][],
                tabs: Models.EngineTab[])
    {
        super(tabs);
    }
}

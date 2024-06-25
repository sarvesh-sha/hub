import {Block} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";

export class SelectField extends ModalDialogField<SelectComponent<string>, typeof SelectComponent, string>
{
    private m_value: string;

    private m_options: ControlOption<string>[];

    constructor(block: Block<any>,
                title: string,
                private m_optionsGetter: () => ControlOption<string>[])
    {
        super(block, SelectComponent, title);

        this.m_options = m_optionsGetter();
    }

    public getModel(): string
    {
        return this.m_value;
    }

    protected getText(): string
    {
        if (this.m_value && this.m_options)
        {
            let option = this.m_options.find((o) => o.id === this.m_value);
            if (option) return option.label;
        }

        return this.title;
    }

    protected initComponent(component: SelectComponent<string>): void
    {
        this.m_options               = this.m_optionsGetter();
        component.multiSelect        = false;
        component.singleClick        = true;
        component.preventDeselection = true;
        component.options            = this.m_options;
        component.value              = this.m_value;
        component.valueChange.subscribe((value) =>
                                        {
                                            if (typeof value === "string")
                                            {
                                                this.m_value = value;
                                            }
                                        });
    }

    protected initData(data: string): void
    {
        this.m_value = data;

        if (data)
        {
            this.m_value = data;
        }
    }
}

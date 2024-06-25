import {ComponentType} from "@angular/cdk/portal";
import * as Blockly from "blockly";
import {Block} from "framework/ui/blockly/block";
import {BlocklyDialogComponent} from "framework/ui/blockly/blockly-dialog.component";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";

export abstract class ModalDialogField<S, T extends ComponentType<S>, M>
{
    private readonly m_field: Blockly.FieldTextInput;

    constructor(protected readonly m_block: Block<any>,
                private readonly component: T,
                protected readonly title: string)
    {
        this.m_field             = new Blockly.FieldTextInput("");
        this.m_field.showEditor_ = () =>
        {
            let comp = m_block.getComponent();
            BlocklyDialogComponent.open(comp, this.title, this.component, {
                initComponent: (c) => this.initComponent(c),
                onCancel     : () =>
                {
                    // Init from the previously saved data
                    this.init();
                },
                onAccept     : () =>
                {
                    // Save data, then init from the new data.
                    let dirty = this.sync();
                    if (dirty)
                    {
                        this.refreshText();

                        // Fire change event
                        let changeEvent        = new Blockly.Events.BlockChange(
                            this.m_block, "field", this.m_field.name, "...", this.getText());
                        changeEvent.recordUndo = false;

                        Blockly.Events.fire(changeEvent);
                    }
                },
                getActions   : (c) => this.getActions(c)
            });
        };

        // Blockly sets data after constructing block so we need to init again if data is set.
        // Needed for block duplication and toolbox scenarios
        this.m_block.onData(() =>
                            {
                                this.init();
                            });

        this.init();
    }

    protected abstract initData(data: M): void;

    protected abstract initComponent(component: S): void;

    protected abstract getText(): string;

    protected getActions(component: S): TabActionDirective[]
    {
        return null;
    }

    public abstract getModel(): M;

    public get field(): Blockly.FieldTextInput
    {
        return this.m_field;
    }

    public init(data?: M)
    {
        if (data)
        {
            this.m_block.setData(data);
        }

        this.initData(this.m_block.getData());
        this.sync();

        this.refreshText();
    }

    protected refreshText(): void
    {
        // Set field text (does not fire events)
        Blockly.Events.disable();
        this.m_field.setValue(this.getText());
        Blockly.Events.enable();
    }

    private sync(): boolean
    {
        return this.m_block.setData(this.getModel());
    }
}

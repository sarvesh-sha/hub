import {ContentChild, Directive, EventEmitter, Input, Output, TemplateRef} from "@angular/core";
import {NgForm} from "@angular/forms";

import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

const TabActionPriorityPrivate = {
    primary    : "primary",
    secondary  : "secondary",
    tertiary   : "tertiary",
    informative: "informative"
};

export type TabActionPriority = keyof typeof TabActionPriorityPrivate;
export const TabActionPriority: { [P in TabActionPriority]: P } = <any>TabActionPriorityPrivate;

@Directive({
               selector: "o3-tab-action"
           })
export class TabActionDirective
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_disabled: boolean | "";
    static ngAcceptInputType_toBeginning: boolean | "";

    private static id = 0;

    public readonly id = TabActionDirective.id++;
    get optio3TestId(): string
    {
        return "o3-tab-action-" + this.id;
    }

    @Input() public label: string;
    @Input() public icon: string;

    @Input() public labelFirstLevel: string;
    @Input() public labelSecondLevel: string;
    @Input() public tooltip: string;

    @Input() public priority: TabActionPriority;
    @Input() public type: string;

    @Input() @CoerceBoolean() public disabled: boolean;
    @Input() @CoerceBoolean() public toBeginning: boolean;
    @Input() public form: NgForm;

    @ContentChild("optionTemplate", {static: true}) template: TemplateRef<any>;

    @Output() click = new EventEmitter<any>();

    callback: () => void;

    onClick(event: Event)
    {
        if (this.type == "submit")
        {
            if (this.form)
            {
                this.form.onSubmit(event);
            }
        }
        else
        {
            this.click.emit(event);
        }
    }

    static filterActions(priority: TabActionPriority,
                         ...actions: TabActionDirective[]): TabActionDirective[]
    {
        return actions.filter((action) => action.priority == priority);
    }
}

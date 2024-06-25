import {forwardRef, Input, TemplateRef, ViewChild, Directive} from "@angular/core";
import {BaseComponent} from "framework/ui/components";


import {WizardStepContentTemplateDirective} from "framework/ui/wizards/wizard-step-content-template.directive";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

@Directive()
export abstract class WizardStep<T> extends BaseComponent
{
    @ViewChild(WizardStepContentTemplateDirective,
               {
                   read  : TemplateRef,
                   static: true
               })
    contentTemplate: TemplateRef<WizardStepContentTemplateDirective>;

    /**
     * An optional name used to reference the step in code.
     */
    @Input() public name: string;

    private m_data: T;

    public async setData(data: T)
    {
        this.m_data = data;
        await this.onData();
    }

    public get data(): T
    {
        return this.m_data;
    }

    public wizard: WizardComponent<T>;

    public stepSelected: boolean;

    public async onData()
    {
        // Steps can override to react to data
    }

    public abstract getLabel(): string;

    public abstract isEnabled(): boolean;

    public abstract isValid(): boolean;

    public invalidExplanation(): string
    {
        return "";
    }

    public abstract onNext(): Promise<boolean>;

    public abstract isNextJumpable(): boolean;

    public abstract onStepSelected(): Promise<void>;

    public static createProvider(component: any)
    {
        return {
            provide    : WizardStep,
            useExisting: forwardRef(() => component)
        };
    }
}

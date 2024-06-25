import {Directive, Input} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";

@Directive()
export abstract class PaneFieldComponent extends BaseApplicationComponent
{
    @Input()
    public label: string;

    @Input()
    public hint: string;

    public isLoading: boolean;

    abstract isClickable(): boolean;
}

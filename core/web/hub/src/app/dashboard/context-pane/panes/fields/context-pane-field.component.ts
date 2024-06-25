import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";

@Component({
               selector       : "o3-context-pane-field",
               changeDetection: ChangeDetectionStrategy.OnPush,
               templateUrl    : "./context-pane-field.component.html",
               styleUrls      : ["./context-pane-field.component.scss"]
           })
export class ContextPaneFieldComponent
{
    @Input() public label: string;
    @Input() public tooltip: string;
    @Input() public loading: boolean;
    @Input() public text: string;
    @Input() public hint: string;
    @Input() public color: string;
    @Input() public clickable: boolean                    = true;
    @Output() public valueClick: EventEmitter<MouseEvent> = new EventEmitter();

    public get textValue(): string
    {
        if (this.loading)
        {
            return "Loading...";
        }
        return this.text || "";
    }
}

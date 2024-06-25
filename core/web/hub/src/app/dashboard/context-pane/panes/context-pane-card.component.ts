import {Component, EventEmitter, Input, Output} from "@angular/core";


@Component({
               selector   : "o3-context-pane-card",
               styleUrls  : ["./context-pane-card.component.scss"],
               templateUrl: "./context-pane-card.component.html"
           })
export class ContextPaneCardComponent
{
    @Input() cardTitle: string;
    @Input() primaryColor: string;
    @Input() secondaryColor: string;
    @Input() editing: boolean = false;

    @Output() cardTitleChange = new EventEmitter<string>();
}

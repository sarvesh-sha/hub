import {Component, Input} from "@angular/core";
import {ProviderForMapBase} from "app/shared/tables/provider-for-map";

@Component({
               selector: "o3-map-list-actions",
               template: `
                   <div class="mt-3" *ngIf="provider">
                       <button mat-raised-button class="mr-2" type="button" (click)="provider.add()" [disabled]="!!provider.edited">
                           New Entry
                       </button>
                       <ng-content></ng-content>
                   </div>
               `
           })
export class MapListActionsComponent
{
    @Input()
    public provider: ProviderForMapBase<any>;
}

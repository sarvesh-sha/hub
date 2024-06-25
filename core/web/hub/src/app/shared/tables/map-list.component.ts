import {ChangeDetectionStrategy, Component, ContentChild, Directive, Input, OnInit, TemplateRef, ViewChild} from "@angular/core";
import {ProviderForMapBase} from "app/shared/tables/provider-for-map";
import {DatatableDetailsTemplateDirective} from "framework/ui/datatables/datatable.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Directive({selector: "[o3MapListForm]"})
export class MapListFormDirective<T>
{
    constructor(public template: TemplateRef<{ $implicit: any; row: any; }>)
    {
    }
}

@Component({
               selector       : "o3-map-list",
               styles         : [
                   ".reorder-button { user-select: none; }",
                   ":host { margin-bottom: 10px; }"
               ],
               templateUrl    : "./map-list.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class MapListComponent<T> implements OnInit
{
    @Input() public details: DatatableDetailsTemplateDirective;
    @Input() public provider: ProviderForMapBase<T>;
    @Input() public dialogClass: string = "dialog-lg";

    @ContentChild(MapListFormDirective, {static: true}) public form: MapListFormDirective<T>;
    @ViewChild(StandardFormOverlayComponent, {static: true}) public overlay: StandardFormOverlayComponent;

    public ngOnInit()
    {
        if (this.details)
        {
            this.provider.table.enableSimpleExpansion((key) => key, (value) => value.key, true, false);
        }

        if (this.form)
        {
            this.provider.registerOnClick(() =>
                                          {
                                              this.overlay.toggleOverlay();
                                          });
        }
    }
}

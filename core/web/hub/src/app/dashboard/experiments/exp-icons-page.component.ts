import {Component, Injector} from "@angular/core";

import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";

@Component({
               selector   : "o3-experiments-icons-page",
               templateUrl: "./exp-icons-page.component.html",
               styleUrls  : ["./exp-icons-page.component.scss"]
           })
export class ExperimentsIconsPageComponent extends ExperimentsBasePageComponent
{
    icons: Icon[] = [
        new Icon("Material", "expand_more"),
        new Icon("Material", "chevron_left"),
        new Icon("Material", "chevron_right"),
        new Icon("Material", "add"),
        new Icon("Material", "done"),
        new Icon("Material", "clear"),
        new Icon("Material", "filter_list"),
        new Icon("Material", "bookmarks"),
        new Icon("Material", "remove_red_eye"),
        new Icon("Material", "keyboard_arrow_left"),
        new Icon("Material", "search"),
        new Icon("Material", "more_vert"),
        new Icon("Material", "delete"),
        new Icon("Material", "add_circle_outline"),
        new Icon("Material", "skip_previous"),
        new Icon("Material", "fast_rewind"),
        new Icon("Material", "play_arrow"),
        new Icon("Material", "pause"),
        new Icon("Material", "stop"),
        new Icon("Material", "skip_next"),
        new Icon("Material", "settings"),
        new Icon("Material", "notifications"),
        new Icon("Material", "schedule"),
        new Icon("Material", "arrow_drop_up"),
        new Icon("Optio3", "o3 o3-lg o3-device"),
        new Icon("Optio3", "o3 o3-2x o3-device"),
        new Icon("Optio3", "o3 o3-3x o3-device"),
        new Icon("Optio3", "o3 o3-4x o3-device"),
        new Icon("Optio3", "o3 o3-5x o3-device"),
        new Icon("Optio3", "o3 o3-spin o3-device"),
        new Icon("Optio3", "o3 o3-pulse o3-device"),
        new Icon("Optio3", "o3 o3-rotate-90 o3-device"),
        new Icon("Optio3", "o3 o3-rotate-180 o3-device"),
        new Icon("Optio3", "o3 o3-rotate-270 o3-device"),
        new Icon("Optio3", "o3 o3-flip-horizontal o3-device"),
        new Icon("Optio3", "o3 o3-flip-vertical o3-device"),
        new Icon("Optio3", "o3 o3-inverse o3-device"),
        new Icon("Optio3", "o3 o3-bookmark"),
        new Icon("Optio3", "o3 o3-search"),
        new Icon("Optio3", "o3 o3-chevron-left"),
        new Icon("Optio3", "o3 o3-chevron-right"),
        new Icon("Optio3", "o3 o3-edit"),
        new Icon("Optio3", "o3 o3-failure"),
        new Icon("Optio3", "o3 o3-information"),
        new Icon("Optio3", "o3 o3-change-password"),
        new Icon("Optio3", "o3 o3-lock-open"),
        new Icon("Optio3", "o3 o3-logout"),
        new Icon("Optio3", "o3 o3-menu"),
        new Icon("Optio3", "o3 o3-pause"),
        new Icon("Optio3", "o3 o3-usage-trend"),
        new Icon("Optio3", "o3 o3-trending-down"),
        new Icon("Optio3", "o3 o3-home"),
        new Icon("Optio3", "o3 o3-device"),
        new Icon("Optio3", "o3 o3-cancel"),
        new Icon("Optio3", "o3 o3-alert"),
        new Icon("Optio3", "o3 o3-share"),
        new Icon("Optio3", "o3 o3-configure"),
        new Icon("Optio3", "o3 o3-view"),
        new Icon("Optio3", "o3 o3-notification"),
        new Icon("Optio3", "o3 o3-enabled"),
        new Icon("Optio3", "o3 o3-me"),
        new Icon("Optio3", "o3 o3-profile"),
        new Icon("Optio3", "o3 o3-view-item"),
        new Icon("Optio3", "o3 o3-ok"),
        new Icon("Optio3", "o3 o3-unknown"),
        new Icon("Optio3", "o3 o3-warning"),
        new Icon("Optio3", "o3 o3-end-of-life"),
        new Icon("Optio3", "o3 o3-out-of-range"),
        new Icon("Optio3", "o3 o3-device-recalled"),
        new Icon("Optio3", "o3 o3-refresh"),
        new Icon("Optio3", "o3 o3-delete"),
        new Icon("Optio3", "o3 o3-filter-outline"),
        new Icon("Optio3", "o3 o3-email"),
        new Icon("Optio3", "o3 o3-database-search"),
        new Icon("Optio3", "o3 o3-equipment"),
        new Icon("FA", "fa fa-lg fa-certificate"),
        new Icon("FA", "fa fa-2x fa-certificate"),
        new Icon("FA", "fa fa-3x fa-certificate"),
        new Icon("FA", "fa fa-4x fa-certificate"),
        new Icon("FA", "fa fa-spin fa-spinner"),
        new Icon("FA", "fa fa-chevron-up"),
        new Icon("FA", "fa fa-chevron-right"),
        new Icon("FA", "fa fa-chevron-down"),
        new Icon("FA", "fa fa-chevron-left"),
        new Icon("FA", "fa fa-close"),
        new Icon("FA", "fa fa-times-circle"),
        new Icon("FA", "fa fa-search"),
        new Icon("FA", "fa fa-refresh"),
        new Icon("FA", "fa fa-certificate"),
        new Icon("FA", "fa fa-plus-circle"),
        new Icon("FA", "fa fa-trash"),
        new Icon("FA", "fa fa-cloud-upload"),
        new Icon("FA", "fa fa-file-text-o"),
        new Icon("FA", "fa fa-plug"),
        new Icon("FA", "fa fa-warning"),
        new Icon("FA", "fa fa-building-o"),
        new Icon("FA", "fa fa-user-o"),
        new Icon("FA", "fa fa-flask"),
        new Icon("FA", "fa fa-crosshairs"),
        new Icon("FA", "fa fa-ellipsis-v"),
        new Icon("FA", "fa fa-arrows-h")
    ];

    constructor(inj: Injector)
    {
        super(inj);
    }
}

class Icon
{
    constructor(public type: "Material" | "Optio3" | "FA",
                public iconClass: string)
    {
    }
}

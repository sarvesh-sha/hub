import {AfterViewInit, Component, ElementRef, Input} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "gauge-health",
               templateUrl: "./gauge-health.component.html",
               styleUrls  : ["./gauge-health.component.scss"]
           })
export class HealthGaugeComponent implements AfterViewInit
{
    @Input() alertSeverity: Models.AlertSeverity;
    @Input() lowLabel: string;
    @Input() highLabel: string;

    private ready: boolean = false;

    constructor(public elementRef: ElementRef)
    {}

    ngAfterViewInit()
    {
        requestAnimationFrame(() =>
                              {
                                  this.ready = true;
                              });
    }

    public position(): string
    {
        return `${this.ready ? this.getValue() : 0}%`;
    }

    private getValue(): number
    {
        switch (this.alertSeverity)
        {
            case Models.AlertSeverity.CRITICAL:
                return 87.5;
            case Models.AlertSeverity.SIGNIFICANT:
                return 62.5;
            case Models.AlertSeverity.LOW:
                return 37.5;
            default:
                return 12.5;
        }
    }
}

import {Component, Injector, ViewEncapsulation} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";


@Component({
               selector     : "o3-report-grid-view",
               templateUrl  : "./report-grid-view.component.html",
               styleUrls    : ["./report-grid-view.component.scss"],
               encapsulation: ViewEncapsulation.None
           })
export class ReportGridViewComponent extends SharedSvc.BaseComponentWithRouter
{
    rowCount: number    = 52;
    columnCount: number = 40;

    measurement: string = "px";
    columnWidth: number = 25;
    borderWidth: number = 1;

    rows    = this.range(1, 1);
    columns = this.range(1, 1);

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        // set measurment
        let measurement = this.getPathParameter("measurement");
        if (measurement == "px")
        {
            this.px();
        }
        else if (measurement == "em")
        {
            this.em();
        }
        else if (measurement == "pt")
        {
            this.pt();
        }
        else
        {
            this.px();
        }

        // set ranges
        this.rows    = this.range(1, this.rowCount);
        this.columns = this.range(1, this.columnCount);
    }

    px()
    {
        // set measurement type (px)
        this.measurement = "px";
        this.columnWidth = 25;
        this.borderWidth = 1;
        this.rowCount    = 52;
    }

    pt()
    {
        // set measurement type (pt)
        this.measurement = "pt";
        this.columnWidth = 20;
        this.borderWidth = 1;
        this.rowCount    = 51;
    }

    em()
    {
        // set measurement type (em)
        this.measurement = "em";
        this.columnWidth = 2;
        this.borderWidth = .1;
        this.rowCount    = 52;
    }

    range(low: number,
          high: number)
    {
        return new Range(low, high);
    }
}

class Range implements Iterable<number>
{
    constructor(public readonly low: number,
                public readonly high: number,
                public readonly step: number = 1)
    {
    }

    * [Symbol.iterator]()
    {
        for (let x = this.low; x <= this.high; x += this.step)
        {
            yield x;
        }
    }
}

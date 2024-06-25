import {Component, Injector} from "@angular/core";
import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-experiments-components-page",
               templateUrl: "./exp-components-page.component.html",
               styleUrls  : ["./exp-components-page.component.scss"]
           })
export class ExperimentsComponentsPageComponent extends ExperimentsBasePageComponent implements IDatatableDataProvider<DemoFlat, DemoFlat, DemoFlat>
{
    private readonly icons = [
        "assets/img/amazon-aws-icon.png",
        "assets/img/google-gcp-icon.png",
        "assets/img/microsoft-azure-icon.png",
        "assets/img/oracle-oci-icon.png"
    ];

    public percent: number                                       = 0;
    public table: DatatableManager<DemoFlat, DemoFlat, DemoFlat> = new DatatableManager(this, () => null);
    public demo: DemoFlat[]                                      = [];
    public compact: boolean                                      = false;

    constructor(inj: Injector)
    {
        super(inj);
        this.randomize();
        new Looper(0.25, 750, value => this.percent = value);
    }

    public getItemName(): string
    {
        return "";
    }

    public getList(): Promise<DemoFlat[]>
    {
        return Promise.resolve(this.demo);
    }

    public getPage(offset: number,
                   limit: number): Promise<DemoFlat[]>
    {
        return Promise.resolve(this.demo.slice((offset * limit), (offset * limit) + limit));
    }

    public itemClicked(columnId: string,
                       item: DemoFlat): void
    {
    }

    public transform(rows: DemoFlat[]): Promise<DemoFlat[]>
    {
        return Promise.resolve(rows);
    }

    public randomize()
    {
        this.demo = [];

        let colors = ChartColorUtilities.getPaletteOptions("");
        for (let i = 0; i < 100; i++)
        {
            let color              = colors[this.randomInt(0, colors.length - 1)];
            let tMax               = Date.now();
            let tMin               = tMax - (1000 * 60 * 60 * 24 * 7);
            let t1                 = this.randomInt(tMin, tMax);
            let t2                 = t1 - this.randomInt(0, 1000 * 60 * 60 * 24);
            let progress: number[] = [];
            for (let j = 0; j < 4; j++) progress.push(this.randomInt(0, 100));
            this.demo.push(new DemoFlat(`Entry ${i + 1}`, color.hex, progress, new Date(t1).toISOString(), new Date(t2).toISOString()));
        }

        this.table.refreshData();
    }

    public getIcon(index: number): string
    {
        return this.icons[index % this.icons.length];
    }

    public progressDemo()
    {
        for (let i = 0; i < this.demo.length; i++)
        {
            for (let j = 0; j < this.demo[i].progress.length; j++)
            {
                let value                = this.demo[i].progress[j];
                this.demo[i].progress[j] = value + Math.min(this.randomInt(0, 100 - value), 5);
            }
        }
    }

    private randomInt(min: number,
                      max: number): number
    {
        min = Math.ceil(min);
        max = Math.floor(max);
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }
}

class Looper
{
    private value = 0;

    constructor(private step: number,
                private pause: number,
                private callback: (value: number) => void)
    {
        this.callback(this.value);
        requestAnimationFrame(() => this.loop());
    }

    private loop()
    {
        this.value = (this.value + this.step) % (100 + this.step);
        this.callback(this.value);

        if (this.value === 0 || this.value === 100)
        {
            setTimeout(() => requestAnimationFrame(() => this.loop()), this.pause);
        }
        else
        {
            requestAnimationFrame(() => this.loop());
        }
    }
}

class DemoFlat
{
    constructor(public label: string,
                public status: string,
                public progress: number[],
                public updatedOn: string,
                public createdOn: string)
    {}
}

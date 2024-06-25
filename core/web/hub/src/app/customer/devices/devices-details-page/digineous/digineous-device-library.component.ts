import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {Lookup} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-digineous-device-library-page",
               templateUrl: "./digineous-device-library.component.html"
           })
export class DigineousDeviceLibraryComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<string, string, PointFlat>
{
    private m_library: Models.DigineousDeviceLibrary;
    public get library(): Models.DigineousDeviceLibrary
    {
        return this.m_library;
    }

    @Input()
    public set library(value: Models.DigineousDeviceLibrary)
    {
        if (value != this.m_library)
        {
            this.m_library = value;
            this.dirty     = false;

            this.rows = null;

            this.table.refreshData();
        }
    }

    private m_filterText: string;
    public get filterText(): string
    {
        return this.m_filterText;
    }

    public set filterText(value: string)
    {
        if (this.m_filterText != value)
        {
            this.m_filterText = value;
            this.table.refreshData();
        }
    }

    private m_onlyEnabled: boolean;
    public get onlyEnabled(): boolean
    {
        return this.m_onlyEnabled;
    }

    public set onlyEnabled(value: boolean)
    {
        this.m_onlyEnabled = value;
        this.table.refreshData();
    }

    dirty: boolean;

    public get ready(): boolean
    {
        if (!this.m_library) return false;

        for (let point of this.m_library.points)
        {
            if (point.enabled && !point.description) return false;
        }

        return true;
    }

    rows: Lookup<PointFlat>;

    table: DatatableManager<string, string, PointFlat>;

    unitsOptions: ControlOption<Models.EngineeringUnits>[] = [];
    pointClassOptions: ControlOption<number>[]             = [];

    rules: Models.NormalizationRules;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<string, string, PointFlat>(this, () =>
        {
            let view = this.getViewState();
            return view.getSubView("digi", true);
        });
    }

    public async ngAfterViewInit()
    {
        await super.ngAfterViewInit();

        await this.loadSettings();
    }

    private async loadSettings()
    {
        if (!this.rules)
        {
            this.unitsOptions = await this.app.domain.units.getEngineeringUnits();
            this.rules        = await this.app.bindings.getActiveNormalizationRules();

            this.pointClassOptions = this.rules.pointClasses.map((v) => new ControlOption(v.id, v.pointClassDescription));
        }
    }

    public markAsDirty()
    {
        this.dirty = true;
    }

    resolvePointClass(pointClass: Models.WellKnownPointClassOrCustom): Models.PointClass
    {
        if (pointClass?.known)
        {
            return this.rules?.pointClasses?.find((v) => v.wellKnown == pointClass.known);
        }

        if (pointClass?.custom)
        {
            return this.rules?.pointClasses?.find((v) => v.id == pointClass.custom);
        }

        return null;
    }

    public updatePointClass(row: PointFlat)
    {
        let pc = this.rules?.pointClasses?.find((v) => v.id == row.pointClass);
        if (pc)
        {
            if (pc.wellKnown)
            {
                row.point.pointClass = Models.WellKnownPointClassOrCustom.newInstance({
                                                                                          known : pc.wellKnown,
                                                                                          custom: 0
                                                                                      });
            }
            else
            {
                row.point.pointClass = Models.WellKnownPointClassOrCustom.newInstance({
                                                                                          known : null,
                                                                                          custom: pc.id
                                                                                      });
            }
        }

        this.markAsDirty();
    }

    //--//

    public getItemName(): string
    {
        return "points";
    }

    public async getList(): Promise<string[]>
    {
        let ids = [];

        let filterText = this.m_filterText?.toLowerCase();

        for (let point of this.m_library?.points || [])
        {
            if (filterText)
            {
                let match: boolean = false;
                match ||= this.matchFilter(filterText, point.identifier);
                match ||= this.matchFilter(filterText, point.description);
                if (!match) continue;
            }

            if (this.m_onlyEnabled && !point.enabled) continue;

            ids.push(point.identifier);
        }

        return ids;
    }

    public async getPage(offset: number,
                         limit: number): Promise<string[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public itemClicked(columnId: string,
                       item: PointFlat): void
    {
    }

    public async transform(rows: string[]): Promise<PointFlat[]>
    {
        if (!this.rows)
        {
            await this.loadSettings();

            this.rows = {};

            for (let point of this.m_library?.points || [])
            {
                let pc = this.resolvePointClass(point.pointClass);

                this.rows[point.identifier] = {
                    point     : point,
                    pointClass: pc?.id
                };
            }
        }

        return rows.map((id) => this.rows[id]);
    }
}

class PointFlat
{
    point: Models.DigineousPointLibrary;
    pointClass: number;
}

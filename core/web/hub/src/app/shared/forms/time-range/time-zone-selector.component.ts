import {Component, EventEmitter, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {TimeZonesJson} from "app/shared/forms/time-range/country-codes";
import {TimeZoneLocal} from "app/shared/forms/time-range/range-selection-extended";
import {Lookup} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {ITreeNode, ITreeNodeFilter} from "framework/ui/dropdowns/filterable-tree.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import moment from "framework/utils/moment";

@Component({
               selector   : "o3-time-zone-selector",
               templateUrl: "./time-zone-selector.component.html",
               styleUrls  : ["./time-zone-selector.component.scss"]
           })
export class TimeZoneSelectorComponent extends SharedSvc.BaseApplicationComponent
{
    private m_zone: string;
    @Input() set zone(zone: string)
    {
        if (this.m_includeLocal)
        {
            this.m_zone = zone === TimeZoneLocal ? undefined : zone;
        }
        else
        {
            this.m_zone = zone;
        }
    }

    get zone(): string
    {
        return this.m_zone === undefined && this.m_includeLocal ? TimeZoneLocal : this.m_zone;
    }

    @Input() readonly: boolean = false;

    private m_includeLocal: boolean = false;
    @Input() set includeLocal(include: boolean)
    {
        include = !!include;
        if (this.m_includeLocal !== include)
        {
            this.m_includeLocal = include;
            this.getZoneOptions();
        }
    }

    zoneOptions: TimeZoneOption[];

    filterFn: ITreeNodeFilter<string> = (option: ITreeNode<string>,
                                         filterText: string) =>
    {
        let zoneOption = <TimeZoneOption>option;
        return zoneOption.searchString.includes(filterText);
    };

    @Output() zoneChange = new EventEmitter<string>();

    public ngOnInit(): void
    {
        super.ngOnInit();

        this.getZoneOptions();
    }

    private getZoneOptions()
    {
        let idToZonePre: Lookup<TimeZoneOptionPre> = {};
        for (let zone of MomentHelper.getZoneNames())
        {
            let name = zone;
            let continent;

            let slashIndex = zone.indexOf("/");
            if (slashIndex >= 0)
            {
                continent = zone.substring(0, slashIndex);
                name      = zone.substring(slashIndex + 1);

                slashIndex = name.indexOf("/");
                if (slashIndex >= 0) name = name.substring(slashIndex + 1) + ", " + name.substring(0, slashIndex);
            }

            name              = name.replace("_", " ");
            idToZonePre[zone] = new TimeZoneOptionPre(zone, name, continent);
        }

        let countries: Lookup<TimeZoneCountryJsonEntry> = TimeZonesJson.countries;
        for (let iso in countries)
        {
            let countryEntry = countries[iso];
            for (let zone of countryEntry.zones) idToZonePre[zone].countries.push(countryEntry.name);
        }

        let zoneOptions = [];
        if (this.m_includeLocal)
        {
            zoneOptions.push(new TimeZoneOption(TimeZoneLocal, "Local", null, null));
        }
        for (let id in idToZonePre)
        {
            zoneOptions.push(idToZonePre[id].toOption());
        }

        this.zoneOptions = zoneOptions;
    }
}

interface TimeZoneCountryJsonEntry
{
    name: string;
    abbr: string;
    zones: string[];
}

class TimeZoneOptionPre
{
    readonly countries: string[] = [];

    constructor(readonly id: string,
                readonly label: string,
                readonly continent: string)
    {
    }

    toOption(): TimeZoneOption
    {
        return new TimeZoneOption(this.id, this.label, this.continent, this.countries);
    }
}

class TimeZoneOption extends ControlOption<string>
{
    readonly searchString: string;
    readonly identifier: string;

    constructor(id: string,
                label: string,
                continent: string,
                associatedCountries: string[])
    {
        super(id, label);

        if (id == TimeZoneLocal)
        {
            this.identifier = MomentHelper.nowWithLocalZone()
                                          .format("z");
        }
        else
        {
            this.identifier = moment.tz(id)
                                    .format("z");
        }

        let searchItems = [this.identifier.toLocaleLowerCase()];
        if (label) searchItems.push(label.toLocaleLowerCase());
        if (continent) searchItems.push(continent.toLocaleLowerCase());
        if (associatedCountries?.length) searchItems.push(...associatedCountries.map((country) => country.toLocaleLowerCase()));

        this.searchString = searchItems.join();
    }
}

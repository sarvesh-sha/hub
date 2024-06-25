import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import {AssetsService, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DatabaseActivityService, DatabaseActivitySubscriber} from "app/services/domain/database-activity.service";
import {EnumsService} from "app/services/domain/enums.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

@Injectable()
export class LocationsService
{
    private m_sub: DatabaseActivitySubscriber;

    constructor(private api: ApiService,
                private assets: AssetsService,
                private enums: EnumsService,
                mas: DatabaseActivityService)
    {
        this.m_sub = mas.registerSimpleNotification(Models.Location.RECORD_IDENTITY, (action) => { this.flushState(); });
    }

    @Memoizer
    async getLocationHierarchy(): Promise<Models.LocationHierarchy[]>
    {
        return this.api.locations.getTopLevel();
    }

    async getLocations(): Promise<LocationExtended[]>
    {
        let hierarchy                    = await this.getLocationHierarchy();
        let res: Models.RecordIdentity[] = [];

        this.collectLocations(res, hierarchy);

        return this.getExtended(res);
    }

    async getLocationsUnder(loc: LocationExtended,
                            inclusive: boolean = true): Promise<LocationExtended[]>
    {
        let hierarchy                    = await this.getLocationHierarchy();
        let subtree                      = this.findLocation(loc.model.sysId, hierarchy);
        let res: Models.RecordIdentity[] = [];

        if (subtree === null)
        {
            return [loc];
        }

        this.collectLocations(res, inclusive ? [subtree] : subtree.subLocations);

        return this.getExtended(res);
    }

    async getTopLocations(): Promise<LocationExtended[]>
    {
        let hierarchy = await this.getLocationHierarchy();

        let locations = await this.assets.getTypedExtendedBatch(LocationExtended, hierarchy.map((lh) => lh.ri));
        locations.sort((a,
                        b) => UtilsService.compareStrings(a.model.name, b.model.name, true));
        return locations;
    }

    async getTopLocationIdentities(): Promise<Models.RecordIdentity[]>
    {
        let hierarchy = await this.getLocationHierarchy();
        return hierarchy.map((lh) => lh.ri);
    }

    async getExtended(identities: Models.RecordIdentity[]): Promise<LocationExtended[]>
    {
        return this.assets.getTypedExtendedBatch(LocationExtended, identities);
    }

    /**
     * Get location types.
     */
    describeLocationTypes(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("LocationType", true);
    }

    @Memoizer
    async getLocationTypes(): Promise<ControlOption<Models.LocationType>[]>
    {
        let types = await this.describeLocationTypes();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    async getTopLocation(location: LocationExtended): Promise<LocationExtended>
    {
        while (true)
        {
            let outerLocation = await location.getParent();
            if (outerLocation == null)
            {
                return location;
            }

            location = outerLocation;
        }
    }

    async getLocationFieldsets(): Promise<Array<LocationFieldset>>
    {
        let locationTypeInfos                     = await this.describeLocationTypes();
        let locationFieldsets: LocationFieldset[] = [];
        for (let typeInfo of locationTypeInfos)
        {
            let fieldset = new LocationFieldset(typeInfo);
            locationFieldsets.push(fieldset);
        }
        return locationFieldsets;
    }

    //--//

    @ResetMemoizers
    private flushState()
    {
        // The decoration does the work.
    }

    private collectLocations(res: Models.RecordIdentity[],
                             hierarchy: Models.LocationHierarchy[])
    {
        if (hierarchy)
        {
            for (let lh of hierarchy)
            {
                res.push(lh.ri);
                this.collectLocations(res, lh.subLocations);
            }
        }
    }

    private findLocation(sysId: string,
                         hierarchy: Models.LocationHierarchy[]): Models.LocationHierarchy
    {
        if (hierarchy)
        {
            // Scan level.
            for (let lh of hierarchy)
            {
                if (lh.ri.sysId === sysId)
                {
                    return lh;
                }
            }

            for (let lh of hierarchy)
            {
                // Recurse and search.
                let searchResult = this.findLocation(sysId, lh.subLocations);
                if (searchResult)
                {
                    return searchResult;
                }
            }
        }

        return null;
    }
}

export class LocationFieldset
{
    public fields: Field[];

    constructor(public locationTypeInfo: Models.EnumDescriptor)
    {
        switch (<any>locationTypeInfo.id)
        {
            case Models.LocationType.BUILDING:
            case Models.LocationType.DISTRIBUTION_CENTER:
            case Models.LocationType.FACTORY:
            case Models.LocationType.REGIONAL_CENTER:
            case Models.LocationType.SCHOOL:
                this.fields = [
                    new Field("name", true),
                    new Field("customerNotes", false),
                    new Field("address", true),
                    new Field("geo", true),
                    new Field("phone", false)
                ];
                break;

            case Models.LocationType.OTHER:
                this.fields = [
                    new Field("typeName", true),
                    new Field("name", true),
                    new Field("customerNotes", false),
                    new Field("address", false),
                    new Field("geo", false),
                    new Field("phone", false)
                ];
                break;

            default:
                this.fields = [
                    new Field("name", true),
                    new Field("customerNotes", false),
                    new Field("address", false),
                    new Field("geo", false),
                    new Field("phone", false)
                ];
                break;
        }
    }

    getField(name: string): Field
    {
        return this.fields.find((field) => { return field.name == name; });
    }

    hasField(name: string): boolean
    {
        return this.getField(name) != null;
    }

    isFieldRequired(name: string): boolean
    {
        let field = this.getField(name);
        return field != null && field.isRequired;
    }

    hasAddress(): boolean
    {
        return this.hasField("address");
    }

    hasLatLng(): boolean
    {
        return this.hasField("geo");
    }
}

export class Field
{
    constructor(public name: string,
                public isRequired: boolean)
    {
    }
}

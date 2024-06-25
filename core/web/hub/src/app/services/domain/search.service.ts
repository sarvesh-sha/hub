import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import {AssetExtended, AssetsService, DeviceElementExtended, DeviceExtended, GatewayExtended, LocationExtended, LogicalAssetExtended, NetworkExtended} from "app/services/domain/assets.service";
import {DevicesService} from "app/services/domain/devices.service";
import {AlertExtended, EventsService} from "app/services/domain/events.service";
import {LocationsService} from "app/services/domain/locations.service";
import {SettingsService} from "app/services/domain/settings.service";

import * as Models from "app/services/proxy/model/models";
import {ControlBindingService} from "app/services/ui/control-binding.service";

import {ErrorService} from "framework/services/error.service";
import {inParallel, mapInParallelNoNulls} from "framework/utils/concurrency";

@Injectable()
export class SearchService
{
    /**
     * Constructor
     */
    constructor(private errors: ErrorService,
                private api: ApiService,
                private binding: ControlBindingService,
                private locations: LocationsService,
                private assets: AssetsService,
                private devices: DevicesService,
                private events: EventsService,
                private settings: SettingsService)
    {
    }

    async getSearchResultSet(request: Models.SearchRequest,
                             limit: number,
                             offset: number = 0): Promise<Models.SearchResultSet>
    {
        try
        {
            request.query = request.query || "";
            request.query = request.query.trim();

            if (request.query == "")
            {
                return null;
            }

            return await this.api.search.search(request, offset, limit);
        }
        catch (err)
        {
            this.errors.error(err.code, err.message);
            return null;
        }
    }

    async getSearchResults(searchResult: Models.SearchResultSet): Promise<SearchResult[]>
    {
        let results: SearchResult[] = [];

        if (searchResult)
        {
            // get all alerts and devices
            let [
                    alertList, deviceList, deviceElementList, gatewayList, networkList, locationList, equipmentList
                ] = await Promise.all([
                                          this.events.getTypedExtendedBatch(AlertExtended, searchResult.alerts),
                                          this.assets.getTypedExtendedBatch(DeviceExtended, searchResult.devices),
                                          this.assets.getTypedExtendedBatch(DeviceElementExtended, searchResult.deviceElements),
                                          this.assets.getTypedExtendedBatch(GatewayExtended, searchResult.gateways),
                                          this.assets.getTypedExtendedBatch(NetworkExtended, searchResult.networks),
                                          this.assets.getTypedExtendedBatch(LocationExtended, searchResult.locations),
                                          this.assets.getTypedExtendedBatch(LogicalAssetExtended, searchResult.logicalGroups)
                                      ]);

            // TODO: Make user service follow same pattern as the rest.
            let userIds                 = searchResult.users.map((ri) => ri.sysId);
            let userList: Models.User[] = [];
            if (userIds.length > 0)
            {
                userList = (await this.settings.getUsersList(false)).filter((u) => userIds.indexOf(u.sysId) >= 0);
            }

            // populate extended properties of alerts
            let alertLocationRequest     = inParallel(alertList, (alert) => this.fetchAlertLocation(alert));
            let deviceLocationRequest    = inParallel(<DeviceExtended[]>deviceList, (device) => this.fetchAssetLocation(device));
            let equipmentLocationRequest = inParallel(equipmentList, (equipment) => this.fetchAssetLocation(equipment));

            await Promise.all([
                                  alertLocationRequest,
                                  deviceLocationRequest,
                                  equipmentLocationRequest
                              ]);

            await Promise.all([
                                  this.getAlertResults(alertList, results),
                                  this.getDeviceResults(deviceList, results),
                                  this.getDeviceElementsResults(deviceElementList, results),
                                  this.getGatewayResults(gatewayList, results),
                                  this.getNetworkResults(networkList, results),
                                  this.getEquipmentResults(equipmentList, results)
                              ]);

            this.getLocationResults(<LocationExtended[]>locationList, results);
            this.getUserResults(userList, results);
        }

        return results;
    }

    async getAlertResults(alertList: AlertExtended[],
                          result: SearchResult[])
    {
        let lst = await mapInParallelNoNulls(alertList, async (matchingAlert) =>
        {
            let sr  = new SearchResult();
            sr.type = SearchResultType.ALERT;
            sr.id   = matchingAlert.model.sysId;
            sr.text = `#${matchingAlert.model.sequenceNumber} - ${matchingAlert.typedModel.type}`;
            sr.url  = "/alerts/alert/" + matchingAlert.model.sysId;

            let di = await matchingAlert.getDevice();
            if (di)
            {
                sr.text += ` ${di.typedModel.productName}`;
            }

            let location = await matchingAlert.getLocation();
            if (location)
            {
                sr.subtext = await location.getRecursiveName();
            }

            return sr;
        });

        result.push(...lst);
    }

    async getDeviceResults(deviceList: DeviceExtended[],
                           result: SearchResult[])
    {
        let lst = await mapInParallelNoNulls(deviceList, async (device) =>
        {
            let sr  = new SearchResult();
            sr.type = SearchResultType.DEVICE;
            sr.id   = device.model.sysId;
            sr.url  = "/devices/device/" + device.model.sysId;

            sr.text = this.joinIfNotNull(device.typedModel.productName, device.model.name, this.getProtocolIdentifier(device));

            let location = await device.getLocation();
            if (location)
            {
                sr.subtext = await location.getRecursiveName();
            }

            return sr;
        });

        result.push(...lst);
    }

    async getDeviceElementsResults(deviceElementList: DeviceElementExtended[],
                                   result: SearchResult[])
    {
        // populate the search results
        let results = await mapInParallelNoNulls(deviceElementList, async (element) =>
        {
            let parentAsset = await element.getParent();
            if (parentAsset instanceof DeviceExtended)
            {
                let sr  = new SearchResult();
                sr.type = SearchResultType.DEVICE_ELEMENT;
                sr.id   = element.model.sysId;
                sr.url  = "/devices/device/" + parentAsset.model.sysId + "/element/" + element.model.sysId;

                let location = await element.getLocation();
                if (location)
                {
                    sr.subtext = await location.getRecursiveName();
                }

                let parentEquips            = await element.getExtendedParentsOfRelation(Models.AssetRelationship.controls);
                let parentEquipName: string = null;
                if (parentEquips && parentEquips.length)
                {
                    parentEquipName = `Equipment: ${parentEquips[0].model.name}`;
                }

                sr.text = this.joinIfNotNull(element.model.name, parentEquipName, `Device: ${parentAsset.model.name}`, this.getProtocolIdentifier(parentAsset));
                return sr;
            }

            if (parentAsset instanceof LogicalAssetExtended)
            {
                let sr  = new SearchResult();
                sr.type = SearchResultType.DEVICE_ELEMENT;
                sr.id   = element.model.sysId;
                sr.url  = "/devices/device/" + parentAsset.model.sysId + "/element/" + element.model.sysId;

                let location = await element.getLocation();
                if (location)
                {
                    sr.subtext = await location.getRecursiveName();
                }

                let parentEquips            = await element.getExtendedParentsOfRelation(Models.AssetRelationship.controls);
                let parentEquipName: string = null;
                if (parentEquips && parentEquips.length)
                {
                    parentEquipName = `Equipment: ${parentEquips[0].model.name}`;
                }

                sr.text = this.joinIfNotNull(element.model.name, parentEquipName);
                return sr;
            }

            return null;
        });

        result.push(...results);
    }

    private getProtocolIdentifier(device: DeviceExtended)
    {
        let desc = device.model.identityDescriptor;
        if (desc instanceof Models.BACnetDeviceDescriptor)
        {
            return `device/${desc.address.instanceNumber}`;
        }

        if (desc instanceof Models.IpnDeviceDescriptor)
        {
            return desc.name;
        }

        return "";
    }

    async getGatewayResults(gatewaysList: GatewayExtended[],
                            result: SearchResult[])
    {
        for (let gateway of gatewaysList)
        {
            let loc          = await gateway.getLocation();
            let locationName = loc ? loc.recursiveName : "No Location Assigned";
            let name         = gateway.model.name;

            result.push({
                            type   : SearchResultType.GATEWAY,
                            id     : gateway.model.sysId,
                            text   : name,
                            subtext: locationName,
                            url    : "/gateways/gateway/" + gateway.model.sysId
                        });
        }
    }

    async getEquipmentResults(equipmentList: LogicalAssetExtended[],
                              result: SearchResult[])
    {
        let equipmentClasses = await this.binding.getEquipmentClasses(false, null);

        for (let equipment of equipmentList)
        {
            let loc            = await equipment.getLocation();
            let locationName   = loc ? loc.recursiveName : "No Location Assigned";
            let name           = equipment.model.name;
            let equipmentClass = equipmentClasses.find((ec) => ec.id === equipment.typedModel.equipmentClassId);
            if (equipmentClass)
            {
                name += " - " + equipmentClass.label;
            }

            result.push({
                            type   : SearchResultType.EQUIPMENT,
                            id     : equipment.model.sysId,
                            text   : name,
                            subtext: locationName,
                            url    : "/equipment/equipment/" + equipment.model.sysId
                        });
        }
    }

    async getNetworkResults(networksList: NetworkExtended[],
                            result: SearchResult[])
    {
        for (let network of networksList)
        {
            let loc          = await network.getLocation();
            let locationName = loc ? loc.recursiveName : "No Location Assigned";
            let name         = network.model.name;

            result.push({
                            type   : SearchResultType.NETWORK,
                            id     : network.model.sysId,
                            text   : name,
                            subtext: locationName,
                            url    : "/networks/network/" + network.model.sysId
                        });
        }
    }

    getLocationResults(locationList: LocationExtended[],
                       result: SearchResult[])
    {
        for (let location of locationList)
        {
            result.push({
                            type   : SearchResultType.LOCATION,
                            id     : location.model.sysId,
                            text   : `${location.model.name}`,
                            subtext: location.typedModel.address,
                            url    : "/configuration/locations/location/" + location.model.sysId
                        });
        }
    }

    getUserResults(userList: Models.User[],
                   result: SearchResult[])
    {
        // filter devices
        for (let match of userList)
        {
            result.push({
                            type   : SearchResultType.USER,
                            id     : match.sysId,
                            text   : `${match.firstName} ${match.lastName}`,
                            subtext: match.emailAddress,
                            url    : "/configuration/users/user/" + match.sysId
                        });
        }
    }

    private async fetchAlertLocation(row: AlertExtended): Promise<void>
    {
        let location = await row.getLocation();
        if (location) await location.getRecursiveName();
    }

    private async fetchAssetLocation(row: AssetExtended): Promise<void>
    {
        let location = await row.getLocation();
        if (location) await location.getRecursiveName();
    }

    getSearchGroups(searchArea: string): SearchResultGroups
    {
        let groups: SearchResultGroups = new SearchResultGroups();

        if (searchArea == "alerts")
        {
            groups.push(SearchResultType.ALERT, "Alert");
            groups.push(SearchResultType.DEVICE, "Device");
            groups.push(SearchResultType.EQUIPMENT, "Equipment");
            groups.push(SearchResultType.DEVICE_ELEMENT, "Control Point");
            groups.push(SearchResultType.LOCATION, "Location");
            groups.push(SearchResultType.USER, "User");
            groups.push(SearchResultType.GATEWAY, "Gateway");
            groups.push(SearchResultType.NETWORK, "Network");
        }
        else if (searchArea == "devices")
        {
            groups.push(SearchResultType.DEVICE, "Device");
            groups.push(SearchResultType.EQUIPMENT, "Equipment");
            groups.push(SearchResultType.DEVICE_ELEMENT, "Control Point");
            groups.push(SearchResultType.ALERT, "Alert");
            groups.push(SearchResultType.LOCATION, "Location");
            groups.push(SearchResultType.USER, "User");
            groups.push(SearchResultType.GATEWAY, "Gateway");
            groups.push(SearchResultType.NETWORK, "Network");
        }
        else if (searchArea == "configuration")
        {
            groups.push(SearchResultType.LOCATION, "Location");
            groups.push(SearchResultType.USER, "User");
            groups.push(SearchResultType.GATEWAY, "Gateway");
            groups.push(SearchResultType.NETWORK, "Network");
            groups.push(SearchResultType.ALERT, "Alert");
            groups.push(SearchResultType.DEVICE, "Device");
            groups.push(SearchResultType.EQUIPMENT, "Equipment");
            groups.push(SearchResultType.DEVICE_ELEMENT, "Control Point");
        }
        else if (searchArea == "equipment")
        {
            groups.push(SearchResultType.EQUIPMENT, "Equipment");
            groups.push(SearchResultType.ALERT, "Alert");
            groups.push(SearchResultType.DEVICE_ELEMENT, "Control Point");
            groups.push(SearchResultType.DEVICE, "Device");
            groups.push(SearchResultType.LOCATION, "Location");
            groups.push(SearchResultType.USER, "User");
            groups.push(SearchResultType.GATEWAY, "Gateway");
            groups.push(SearchResultType.NETWORK, "Network");
        }
        else
        {
            groups.push(SearchResultType.ALERT, "Alert");
            groups.push(SearchResultType.EQUIPMENT, "Equipment");
            groups.push(SearchResultType.DEVICE_ELEMENT, "Control Point");
            groups.push(SearchResultType.DEVICE, "Device");
            groups.push(SearchResultType.LOCATION, "Location");
            groups.push(SearchResultType.USER, "User");
            groups.push(SearchResultType.GATEWAY, "Gateway");
            groups.push(SearchResultType.NETWORK, "Network");
        }

        return groups;
    }

    private handleErrors<T>(promise: Promise<T>): Promise<T>
    {
        promise.catch(
            error =>
            {
                this.errors.error(error.code, error.message);
            });

        return promise;
    }

    private joinIfNotNull(...array: string[]): string
    {
        return array.filter((n) => !!n)
                    .join(" - ");
    }
}

export class SearchResultGroup
{
    type: SearchResultType;
    name: string;
    results: SearchResult[]          = [];
    displayedResults: SearchResult[] = [];
    total: number                    = 0;
    initiallyExpanded: boolean       = true;

    pageIndex: number = 0;
    pageSize: number  = 10;

    public get hasResults(): boolean
    {
        return this.results.length > 0;
    }

    public get remainingCount(): number
    {
        if (this.total - this.displayedResults.length > 0)
        {
            return this.total - this.displayedResults.length;
        }

        return 0;
    }

    setTotal(resultSet: Models.SearchResultSet)
    {
        if (!resultSet) return;

        switch (this.type)
        {
            case SearchResultType.ALERT:
                this.total = resultSet.totalAlerts;
                break;
            case SearchResultType.DEVICE:
                this.total = resultSet.totalDevices;
                break;
            case SearchResultType.DEVICE_ELEMENT:
                this.total = resultSet.totalDeviceElements;
                break;
            case SearchResultType.EQUIPMENT:
                this.total = resultSet.totalLogicalGroups;
                break;
            case SearchResultType.GATEWAY:
                this.total = resultSet.totalGateways;
                break;
            case SearchResultType.LOCATION:
                this.total = resultSet.totalLocations;
                break;
            case SearchResultType.NETWORK:
                this.total = resultSet.totalNetworks;
                break;
            case SearchResultType.USER:
                this.total = resultSet.totalUsers;
                break;
            default:
                this.total = 0;
        }
    }

    setTotalByValue(total: number)
    {
        this.total = total;
    }

    showFirstN(n: number)
    {
        for (let i = 0; i < n && i < this.results.length; i++)
        {
            this.displayedResults.push(this.results[i]);
        }
    }
}

export class SearchResultGroups
{
    groups: SearchResultGroup[] = [];

    get hasResults(): boolean
    {
        for (let group of this.groups)
        {
            if (group.hasResults)
            {
                return true;
            }
        }

        return false;
    }

    get countOfGroupsWithResults(): number
    {
        let count = 0;
        for (let group of this.groups)
        {
            if (group.hasResults) count++;
        }

        return count;
    }

    push(type: SearchResultType,
         name: string)
    {
        let group  = new SearchResultGroup();
        group.type = type;
        group.name = name;
        this.groups.push(group);
    }

    showFirstN(n: number = 3)
    {
        for (let group of this.groups)
        {
            group.showFirstN(n);
        }
    }

    getGroup(type: SearchResultType): SearchResultGroup
    {
        return this.groups.find(
            g => g.type === type);
    }

    limitToType(type: SearchResultType): SearchResultGroups
    {
        for (let group of this.groups)
        {
            group.initiallyExpanded = group.type == type;
        }

        return this;
    }

    setTotals(resultSet: Models.SearchResultSet)
    {
        if (!resultSet) return;

        for (let group of this.groups)
        {
            group.setTotal(resultSet);
        }
    }
}

export class SearchResult
{
    type: SearchResultType;
    id: string;
    text: string;
    subtext?: string;
    url: string;
    isSummary?: boolean = false;
    checked?: boolean;
}

const SearchResultTypePrivate = {
    ALERT         : "ALERT",
    DEVICE        : "DEVICE",
    DEVICE_ELEMENT: "DEVICE_ELEMENT",
    EQUIPMENT     : "EQUIPMENT",
    USER          : "USER",
    LOCATION      : "LOCATION",
    GATEWAY       : "GATEWAY",
    NETWORK       : "NETWORK",
    SEARCHALL     : "SEARCHALL"
};

export type SearchResultType = keyof typeof SearchResultTypePrivate;

export const SearchResultType: { [P in SearchResultType]: P } = <any>SearchResultTypePrivate;


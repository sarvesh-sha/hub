import {Component, NgZone} from "@angular/core";
import {AppDomainContext} from "app/services/domain";
import {TrendFetcher} from "app/services/domain/data-connection.service";

import * as Models from "app/services/proxy/model/models";
import {MomentHelper} from "framework/utils/moment-helper";
import {uuid} from "framework/utils/uuid";
import * as moment from "moment-timezone";
import {Column, Connection, Connector, Table} from "./tableau-utils";

@Component({
               selector   : "o3-wdc",
               styleUrls  : ["./web-data-connector.component.scss"],
               templateUrl: "./web-data-connector.component.html"
           })
export class WebDataConnectorComponent
{
    userSysId: string;
    isAuthenticated = false;
    isInitializing  = false;
    loginFailed     = false;
    initialized     = false;
    timezone        = "America/Los_Angeles";

    username         = "";
    password         = "";
    lastNDays        = 30;
    loadAll          = true;
    unclassified     = false;
    belowThresholdId = 335; // Special NoPtClass for McKinstry

    filterOptions: Models.FilterPreferences[] = [];
    selectedFilterId: string                  = "";
    selectedFilter: Models.FilterPreferences;

    showAdvanced = false;

    timezones = moment.tz.names();

    constructor(private ngZone: NgZone,
                private domain: AppDomainContext)
    {
        let connector  = Connector();
        let equipTable = Table("equipment")
            .addColumn(Column("EquipID", tableau.dataTypeEnum.string))
            .addColumn(Column("ParentEquipID", tableau.dataTypeEnum.string))
            .addColumn(Column("EquipName", tableau.dataTypeEnum.string))
            .addColumn(Column("EquipClassID", tableau.dataTypeEnum.int))
            .addColumn(Column("BuildingID", tableau.dataTypeEnum.string))
            .setGetData(this.fetchEquipments.bind(this));

        let buildingTable = Table("building")
            .addColumn(Column("BuildingID", tableau.dataTypeEnum.string))
            .addColumn(Column("ParentBuildingID", tableau.dataTypeEnum.string))
            .addColumn(Column("Type", tableau.dataTypeEnum.string))
            .addColumn(Column("BuildingName", tableau.dataTypeEnum.string))
            .addColumn(Column("BuildingShortName", tableau.dataTypeEnum.string))
            .addColumn(Column("SiteID", tableau.dataTypeEnum.int))
            .setGetData(this.fetchBuildings.bind(this));

        let pointTable = Table("point")
            .addColumn(Column("PointID", tableau.dataTypeEnum.string))
            .addColumn(Column("PointDescription", tableau.dataTypeEnum.string))
            .addColumn(Column("PointName", tableau.dataTypeEnum.string))
            .addColumn(Column("PointNameRaw", tableau.dataTypeEnum.string))
            .addColumn(Column("PointNameBackup", tableau.dataTypeEnum.string))
            .addColumn(Column("BuildingID", tableau.dataTypeEnum.string))
            .addColumn(Column("EquipID", tableau.dataTypeEnum.string))
            .addColumn(Column("PointClassID", tableau.dataTypeEnum.int))
            .addColumn(Column("SiteID", tableau.dataTypeEnum.int))
            .addColumn(Column("NetworkID", tableau.dataTypeEnum.int))
            .addColumn(Column("InstanceID", tableau.dataTypeEnum.int))
            .addColumn(Column("ObjectID", tableau.dataTypeEnum.string))
            .setGetData(this.fetchPoints.bind(this));

        let trendTable = Table("trendrecord")
            .addColumn(Column("DateTime", tableau.dataTypeEnum.datetime))
            .addColumn(Column("NumericValue", tableau.dataTypeEnum.float))
            .addColumn(Column("StringValue", tableau.dataTypeEnum.string))
            .addColumn(Column("PointID", tableau.dataTypeEnum.string)
                           .setFilterable(true)
                           .setForeignKey(pointTable, "PointID"))
            .setJoinOnly(true)
            .setIncrementColumnId("DateTime")
            .setGetData(this.fetchTrends.bind(this));

        let siteTable = Table("site")
            .addColumn(Column("SiteID", tableau.dataTypeEnum.int))
            .addColumn(Column("SiteName", tableau.dataTypeEnum.string))
            .addColumn(Column("SiteShortName", tableau.dataTypeEnum.string))
            .setGetData(this.fetchSites.bind(this));

        let pointClassTable = Table("pointclass")
            .addColumn(Column("PointClassID", tableau.dataTypeEnum.int))
            .addColumn(Column("PointClassDescription", tableau.dataTypeEnum.string))
            .addColumn(Column("PointClassName", tableau.dataTypeEnum.string))
            .addColumn(Column("PointClassType", tableau.dataTypeEnum.string))
            .addColumn(Column("KindID", tableau.dataTypeEnum.int))
            .addColumn(Column("UnitID", tableau.dataTypeEnum.int))
            .setGetData(this.fetchPointClasses.bind(this));

        let equipClassTable = Table("equipclass")
            .addColumn(Column("EquipClassID", tableau.dataTypeEnum.int))
            .addColumn(Column("EquipClassDescription", tableau.dataTypeEnum.string))
            .addColumn(Column("EquipClassName", tableau.dataTypeEnum.string))
            .setGetData(this.fetchEquipmentClasses.bind(this));

        let kindTable = Table("kind")
            .addColumn(Column("KindID", tableau.dataTypeEnum.int))
            .addColumn(Column("KindDescription", tableau.dataTypeEnum.string))
            .setGetData(this.fetchKinds.bind(this));

        let unitTable = Table("unit")
            .addColumn(Column("UnitID", tableau.dataTypeEnum.int))
            .addColumn(Column("UnitDescription", tableau.dataTypeEnum.string))
            .setGetData(this.fetchUnits.bind(this));


        connector.addTable(siteTable);
        connector.addTable(equipTable);
        connector.addTable(buildingTable);
        connector.addTable(kindTable);
        connector.addTable(unitTable);
        connector.addTable(pointTable);
        connector.addTable(trendTable);
        connector.addTable(pointClassTable);
        connector.addTable(equipClassTable);

        connector.addConnection(Connection("Standard Join")
                                    .addJoin(siteTable, "SiteID", buildingTable, "SiteID")
                                    .addJoin(buildingTable, "BuildingID", equipTable, "BuildingID")
                                    .addJoin(equipTable, "EquipID", pointTable, "EquipID")
                                    .addJoin(equipTable, "EquipClassID", equipClassTable, "EquipClassID", tableau.joinEnum.left)
                                    .addJoin(pointTable, "PointClassID", pointClassTable, "PointClassID", tableau.joinEnum.left)
                                    .addJoin(pointTable, "PointID", trendTable, "PointID"));

        connector.addConnection(Connection("Unclassified Join")
                                    .addJoin(pointTable, "BuildingID", buildingTable, "BuildingID", tableau.joinEnum.left)
                                    .addJoin(pointTable, "SiteID", siteTable, "SiteID", tableau.joinEnum.left)
                                    .addJoin(pointTable, "EquipID", equipTable, "EquipID", tableau.joinEnum.left)
                                    .addJoin(equipTable, "EquipClassID", equipClassTable, "EquipClassID", tableau.joinEnum.left)
                                    .addJoin(pointTable, "PointClassID", pointClassTable, "PointClassID", tableau.joinEnum.left)
                                    .addJoin(pointTable, "PointID", trendTable, "PointID"));

        connector.onInit((initCallback) =>
                         {
                             this.ngZone.run(() => this.init(initCallback));
                         });

        connector.register();
    }

    public submit(event)
    {
        event.preventDefault();
        this.connectionData = {
            lastNDays         : this.lastNDays,
            loadAll           : this.loadAll,
            unclassified      : this.unclassified,
            belowThresholdId  : this.belowThresholdId,
            timezone          : this.timezone,
            setupTime         : MomentHelper.now(),
            connectionId      : uuid(),
            selectedFilterJSON: this.selectedFilter ? JSON.stringify(this.selectedFilter) : ""
        };
        tableau.submit();
    }

    public async processLogin(event)
    {
        event.preventDefault();
        await this.login(this.username, this.password);
        await this.loadFilterOptions();
    }

    public toggleAdvanced(event)
    {
        event.preventDefault();
        this.showAdvanced = !this.showAdvanced;
    }

    private async init(initCallback: () => void)
    {
        tableau.log(`Initializing Optio3 Web Data Connector. phase=${tableau.phase}`);
        tableau.connectionName = "Optio3"; // This will be the data source name in Tableau
        tableau.authType       = tableau.authTypeEnum.custom;

        await this.initFromTableauData();
        await this.loadFilterOptions();
        this.initialized = true;
        initCallback();

        if (tableau.phase === tableau.phaseEnum.gatherDataPhase)
        {
            if (!this.isAuthenticated)
            {
                tableau.abortForAuth("Needs authentication.");
                return;
            }
        }

        if (this.isAuthenticated)
        {
            this.setPassword();
        }
    }

    private async initFromTableauData()
    {
        if (tableau.connectionData)
        {
            let {lastNDays, loadAll, unclassified, belowThresholdId, timezone, selectedFilterJSON} = this.connectionData;

            this.lastNDays        = lastNDays;
            this.loadAll          = loadAll;
            this.unclassified     = unclassified;
            this.belowThresholdId = belowThresholdId !== undefined ? belowThresholdId : 335;
            this.timezone         = timezone;

            if (selectedFilterJSON)
            {
                this.selectedFilter   = Models.FilterPreferences.newInstance(JSON.parse(selectedFilterJSON));
                this.selectedFilterId = this.selectedFilter.id;
            }
        }

        if (tableau.username && tableau.password)
        {
            await this.login(tableau.username, tableau.password);
        }
    }

    public updateFilter()
    {
        this.selectedFilter = this.filterOptions.find((opt) => opt.id === this.selectedFilterId);
    }

    private async loadFilterOptions()
    {
        try
        {
            this.filterOptions = [];
            let ids            = await this.domain.apis.userPreferences.listValues(this.userSysId, "SAVED_FILTERS");
            for (let id of ids)
            {
                let pref = await this.domain.apis.userPreferences.getValue(this.userSysId, "SAVED_FILTERS", id);
                this.filterOptions.push(Models.FilterPreferences.newInstance(JSON.parse(pref.value)));
            }

            this.updateFilter();
        }
        catch (e)
        {
        }
    }

    private async fetchPoints(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getPoints(this.selectedFilter, this.connectionData.unclassified, this.connectionData.belowThresholdId));
    }

    private async fetchTrends(table: tableau.Table)
    {
        let filterValues                                            = table.filterValues;
        let {lastNDays, setupTime, loadAll, connectionId, timezone} = this.connectionData;

        tableau.log("Fetching Trends.");

        let defaultStartDate = setupTime;
        if (lastNDays && !loadAll)
        {
            defaultStartDate = defaultStartDate.subtract(lastNDays, "days");
        }
        else if (loadAll)
        {
            defaultStartDate = undefined;
        }

        if (!table.isJoinFiltered)
        {
            // Uncomment for simulator testing
            //filterValues = (await this.domain.assets.getList(new Models.DeviceElementFilterRequest()).map((ri) => ri.sysId);
            tableau.abortWithError("The table must be filtered first.");
            return;
        }

        if (filterValues.length === 0)
        {
            tableau.log("No filters applied. Can't Fetch Trends!");
            return;
        }

        let trendFetcher = new TrendFetcher(this.domain.apis, this.domain.dataConnection, filterValues, connectionId, defaultStartDate, timezone, (trends) => table.appendRows(trends));
        await trendFetcher.getTrendRecords();
    }

    private async fetchPointClasses(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getPointClasses());
    }

    private async fetchEquipmentClasses(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getEquipmentClasses());
    }

    private async fetchUnits(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getUnits());
    }

    private async fetchKinds(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getKinds());
    }

    private async fetchEquipments(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getEquipments(this.selectedFilter, this.connectionData.unclassified, this.connectionData.belowThresholdId));
    }

    private async fetchSites(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getSites());
    }

    private async fetchBuildings(table: tableau.Table)
    {
        table.appendRows(await this.domain.dataConnection.getBuildings(this.selectedFilter, this.connectionData.unclassified, this.connectionData.belowThresholdId));
    }

    private async login(username: string,
                        password: string)
    {
        if (!this.isInitializing)
        {
            this.isInitializing = true;
            this.loginFailed    = false;

            try
            {
                let user             = await this.domain.apis.users.login(username, password);
                this.userSysId       = user.sysId;
                this.isAuthenticated = true;
            }
            catch (err)
            {
                this.loginFailed = true;
                console.log(err);
            }
            finally
            {
                this.isInitializing = false;
            }
            this.setPassword();
        }
    }

    private setPassword()
    {
        tableau.username = this.username || tableau.username;
        tableau.password = this.password || tableau.password;

        if (tableau.phase === tableau.phaseEnum.authPhase && this.isAuthenticated)
        {
            tableau.submit();
        }
    }

    private get connectionData(): ConnectionData
    {
        let {lastNDays: lastNDaysRaw, setupTime: setupTimeRaw, loadAll, connectionId, unclassified, belowThresholdId, timezone, selectedFilterJSON} = JSON.parse(tableau.connectionData || "{}");

        let setupTime = setupTimeRaw ? MomentHelper.parse(setupTimeRaw) : null;
        let lastNDays = parseInt(lastNDaysRaw);

        return {
            connectionId,
            lastNDays,
            loadAll,
            setupTime,
            unclassified,
            belowThresholdId,
            timezone,
            selectedFilterJSON
        };
    }

    private set connectionData(data: ConnectionData)
    {
        tableau.connectionData = JSON.stringify({
                                                    setupTime         : data.setupTime ? data.setupTime.toISOString() : null,
                                                    lastNDays         : data.lastNDays,
                                                    loadAll           : data.loadAll,
                                                    connectionId      : data.connectionId,
                                                    unclassified      : data.unclassified,
                                                    belowThresholdId  : data.belowThresholdId,
                                                    timezone          : data.timezone,
                                                    selectedFilterJSON: data.selectedFilterJSON
                                                });
        tableau.log("Setting connection data: " + tableau.connectionData);
    }
}

interface ConnectionData
{
    connectionId: string;
    setupTime: moment.Moment;
    lastNDays: number;
    loadAll: boolean;
    unclassified: boolean;
    belowThresholdId: number;
    timezone: string;
    selectedFilterJSON: string;
}

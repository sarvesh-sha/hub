declare namespace tableau
{
    export enum phaseEnum
    {
        interactivePhase = "interactive", authPhase = "auth", gatherDataPhase = "gatherData"
    }

    export enum authPurposeEnum
    {
        ephemeral = "ephemeral", enduring = "enduring"
    }

    export enum authTypeEnum
    {
        none = "none", basic = "basic", custom = "custom"
    }

    export enum dataTypeEnum
    {
        bool = "bool", date = "date", datetime = "datetime", float = "float", int = "int", string = "string", geometry = "geometry"
    }

    export enum columnRoleEnum
    {
        dimension = "dimension", measure = "measure"
    }

    export enum columnTypeEnum
    {
        continuous = "continuous", discrete = "discrete"
    }

    export enum aggTypeEnum
    {
        sum = "sum", avg = "avg", median = "median", count = "count", countd = "count_dist"
    }

    export enum geographicRoleEnum
    {
        area_code = "area_code", cbsa_msa = "cbsa_msa", city = "city", congressional_district = "congressional_district", country_region = "country_region", county = "county", state_province = "state_province", zip_code_postcode = "zip_code_postcode", latitude = "latitude", longitude = "longitude"
    }

    export enum unitsFormatEnum
    {
        thousands = "thousands", millions = "millions", billions_english = "billions_english", billions_standard = "billions_standard"
    }

    export enum numberFormatEnum
    {
        number = "number", currency = "currency", scientific = "scientific", percentage = "percentage"
    }

    export enum localeEnum
    {
        america = "en-us", brazil = "pt-br", china = "zh-cn", france = "fr-fr", germany = "de-de", japan = "ja-jp", korea = "ko-kr", spain = "es-es"
    }

    export enum joinEnum
    {
        inner = "inner", left = "left"
    }

    export interface DataDoneCallback
    {
        (): void
    }

    export interface InitCallback
    {
        (): void
    }

    export interface SchemaCallback
    {
        (tableInfo: TableInfo[],
         connections?: StandardConnection[]): void
    }

    export interface ShutdownCallback
    {
        (): void
    }

    export interface WebDataConnector
    {
        getData(table: Table,
                doneCallback: DataDoneCallback): void;

        getSchema(schemaCallback: SchemaCallback): void;

        init(initCallBack: InitCallback): void;

        shutdown(shutdownCallback: ShutdownCallback): void;
    }

    export interface ForeignKeyInfo
    {
        tableId: string;
        columnId: string;
    }

    export interface ColumnInfo
    {
        id: string;
        dataType: dataTypeEnum;
        aggType?: aggTypeEnum;
        alias?: string;
        description?: string;
        columnRole?: columnRoleEnum;
        columnType?: columnTypeEnum;
        geographicRole?: geographicRoleEnum;
        numberFormat?: numberFormatEnum;
        unitsFormat?: unitsFormatEnum;
        filterable?: boolean;
        foreignKey?: ForeignKeyInfo;
    }

    export interface TableInfo
    {
        id: string;
        columns: ColumnInfo[];
        alias?: string;
        description?: string;
        incrementColumnId?: string;
        joinOnly?: boolean;
    }

    export interface Table
    {
        tableInfo: TableInfo;
        incrementValue: string;
        filterValues: string[];
        isJoinFiltered: boolean;

        appendRows(rows: any[]): any;
    }

    export interface JoinTableReference
    {
        tableAlias: string;
        columnId: string;
    }

    export interface JoinTable
    {
        id: string;
        alias: string;
    }

    export interface Join
    {
        left: JoinTableReference,
        right: JoinTableReference,
        joinType: joinEnum
    }

    export interface StandardConnection
    {
        alias: string;
        tables: JoinTable[];
        joins: Join[];
    }

    let versionNumber: string;
    let version: string;

    function makeConnector(): WebDataConnector;

    function registerConnector(WebDataConnector): void;

    function submit(): void;

    function log(msg: string): void;

    function abortWithError(msg: string): void;

    function abortForAuth(msg: string): void;

    let authPurpose: authPurposeEnum;
    let authType: authTypeEnum;
    let phase: phaseEnum;

    let connectionName: string;
    let connectionData: any;
    let locale: string;
    let username: string;
    let password: any;
}

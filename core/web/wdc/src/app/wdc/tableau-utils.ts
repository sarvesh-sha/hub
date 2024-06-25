class TableInfoFluent implements tableau.TableInfo
{
    id: string;
    columns: tableau.ColumnInfo[];
    alias: string;
    description: string;
    getData: (table: tableau.Table) => Promise<void>;
    joinOnly: boolean;
    incrementColumnId: string;

    constructor(id: string)
    {
        this.id      = id;
        this.columns = [];
    }

    addColumn(column: tableau.ColumnInfo): TableInfoFluent
    {
        this.columns.push(column);
        return this;
    }

    setAlias(alias: string): TableInfoFluent
    {
        this.alias = alias;
        return this;
    }

    setDescription(description: string): TableInfoFluent
    {
        this.description = description;
        return this;
    }

    setGetData(getData: (table: tableau.Table) => Promise<void>): TableInfoFluent
    {
        this.getData = getData;
        return this;
    }

    setJoinOnly(joinOnly: boolean)
    {
        this.joinOnly = joinOnly;
        return this;
    }

    setIncrementColumnId(incrementColumnId: string)
    {
        this.incrementColumnId = incrementColumnId;
        return this;
    }
}

class ColumnInfoFluent implements tableau.ColumnInfo
{
    id: string;
    dataType: tableau.dataTypeEnum;
    aggType: tableau.aggTypeEnum;
    alias: string;
    description: string;
    columnRole: tableau.columnRoleEnum;
    columnType: tableau.columnTypeEnum;
    geographicRole: tableau.geographicRoleEnum;
    numberFormat: tableau.numberFormatEnum;
    unitsFormat: tableau.unitsFormatEnum;
    filterable: boolean;
    foreignKey: tableau.ForeignKeyInfo;

    constructor(id: string,
                type: tableau.dataTypeEnum)
    {
        this.id       = id;
        this.dataType = type;
    }

    setAggType(aggType: tableau.aggTypeEnum)
    {
        this.aggType = aggType;
        return this;
    }

    setAlias(alias: string)
    {
        this.alias = alias;
        return this;
    }

    setDescription(description: string)
    {
        this.description = description;
        return this;
    }

    setColumnRole(columnRole: tableau.columnRoleEnum)
    {
        this.columnRole = columnRole;
        return this;
    }

    setColumnType(columnType: tableau.columnTypeEnum)
    {
        this.columnType = columnType;
        return this;
    }

    setGeographicRole(geographicRole: tableau.geographicRoleEnum)
    {
        this.geographicRole = geographicRole;
        return this;
    }

    setNumberFormat(numberFormat: tableau.numberFormatEnum)
    {
        this.numberFormat = numberFormat;
        return this;
    }

    setUnitsFormat(unitsFormat: tableau.unitsFormatEnum)
    {
        this.unitsFormat = unitsFormat;
        return this;
    }

    setFilterable(filterable: boolean)
    {
        this.filterable = filterable;
        return this;
    }

    setForeignKey(table: TableInfoFluent,
                  column: string)
    {
        this.foreignKey = {
            tableId : table.id,
            columnId: column
        };
        return this;
    }
}

class ConnectionFluent implements tableau.StandardConnection
{
    alias: string;
    tables: tableau.JoinTable[];
    joins: tableau.Join[];

    constructor(alias: string)
    {
        this.alias  = alias;
        this.tables = [];
        this.joins  = [];
    }

    addJoin(left: JoinTableFluent,
            leftColumnId: string,
            right: JoinTableFluent,
            rightColumnId: string,
            joinType: tableau.joinEnum = tableau.joinEnum.inner): ConnectionFluent
    {
        this.addTableIfMissing(left);
        this.addTableIfMissing(right);
        this.joins.push({
                            left    : {
                                tableAlias: left.alias || left.id,
                                columnId  : leftColumnId
                            },
                            right   : {
                                tableAlias: right.alias || right.id,
                                columnId  : rightColumnId
                            },
                            joinType: joinType
                        });

        return this;
    }

    private addTableIfMissing(table: JoinTableFluent)
    {
        if (!this.tables.find((t) => t === table || t.alias === (table.alias || table.id)))
        {
            this.tables.push({
                                 id   : table.id,
                                 alias: table.alias || table.id
                             });
        }
    }
}

class JoinTableFluent implements tableau.JoinTable
{
    id: string;
    alias: string;

    constructor(table: TableInfoFluent,
                alias: string)
    {
        this.id    = table.id;
        this.alias = alias;
    }
}

class WebDataConnectorFluent implements tableau.WebDataConnector
{
    tables: TableInfoFluent[] = [];
    connections: ConnectionFluent[] = [];
    initCallback: (cb: tableau.InitCallback) => void;

    init(initCallBack: tableau.InitCallback)
    {
        if (this.initCallback)
        {
            this.initCallback(initCallBack);
        }
    }

    shutdown(shutdownCallback: tableau.ShutdownCallback)
    {
    }

    getData(table: tableau.Table,
            doneCallback: tableau.DataDoneCallback)
    {
        let tableInfo = this.tables.find((t) => t.id === table.tableInfo.id);
        if (tableInfo && tableInfo.getData)
        {
            tableInfo.getData(table)
                     .then(doneCallback)
                     .catch(doneCallback);
        }
        else
        {
            doneCallback();
        }
    }

    getSchema(schemaCallback: tableau.SchemaCallback)
    {
        schemaCallback(this.tables, this.connections);
    }

    addTable(tableInfo: TableInfoFluent)
    {
        this.tables.push(tableInfo);
        return this;
    }

    addConnection(connection: ConnectionFluent)
    {
        this.connections.push(connection);
    }

    onInit(callback: (cb: tableau.InitCallback) => void)
    {
        this.initCallback = callback;
    }

    register()
    {
        (<any>window).optio3Connector = this;
    }
}

export function Table(id: string): TableInfoFluent
{
    return new TableInfoFluent(id);
}

export function Column(id: string,
                       type: tableau.dataTypeEnum): ColumnInfoFluent
{
    return new ColumnInfoFluent(id, type);
}

export function Connection(alias: string)
{
    return new ConnectionFluent(alias);
}

export function JoinTable(table: TableInfoFluent,
                          alias: string)
{
    return new JoinTableFluent(table, alias);
}

export function Connector(): WebDataConnectorFluent
{
    return new WebDataConnectorFluent();
}

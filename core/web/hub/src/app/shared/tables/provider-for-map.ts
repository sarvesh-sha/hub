import {Lookup, UtilsService} from "framework/services/utils.service";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {ViewState} from "framework/ui/navigation/view-state.service";

export interface IProviderForMapHost
{
    getViewState(): ViewState;

    setDirty(): void;

    detectChanges(): void;

    wasDestroyed(): boolean;
}

export abstract class ProviderForMapBase<V> implements IDatatableDataProvider<string, string, DatatablePair<string, V>>
{
    keys: string[]  = [];
    data: Lookup<V> = {};

    table: DatatableManager<string, string, DatatablePair<string, V>>;

    selected: DatatablePair<string, V>;
    edited: DatatablePair<string, V>;

    public readonly tableId: string = "";

    private m_filter: string;

    protected m_selectedState: string;

    private m_onClick: () => void;

    constructor(private host: IProviderForMapHost,
                subViewPrefix: string,
                private displayName: string,
                public labelForKey: string,
                public labelForValue: string)
    {
        this.table = new DatatableManager<string, string, DatatablePair<string, V>>(this, () =>
        {
            let view = host.getViewState();
            return view.getSubView(subViewPrefix, true);
        });
    }

    protected abstract getText(data: V): string;

    protected abstract shouldInclude(data: V,
                                     filterLowercase: string): boolean;

    protected abstract allocate(): V;

    public abstract isOrdered(): boolean;

    protected abstract reorderList(index: number,
                                   newIndex: number): void;

    protected abstract getAllKeys(): string[];

    public get filter(): string
    {
        return this.m_filter;
    }

    public set filter(value: string)
    {
        this.m_filter = value;
        this.bindInner();
    }

    protected getKeyText(key: string,
                         data: V): string
    {
        return key;
    }

    //--//

    public add()
    {
        this.edited = {
            index  : undefined,
            key    : "",
            keyText: "",
            value  : this.allocate(),
            text   : ""
        };

        if (this.m_onClick)
        {
            this.m_onClick();
        }
    }

    public remove()
    {
        if (this.selected)
        {
            delete this.data[this.selected.key];
        }

        this.selected = undefined;
        this.edited   = undefined;

        this.bindInner();
        this.host.setDirty();
    }

    public save()
    {
        if (this.selected && this.selected.key != this.edited.key)
        {
            delete this.data[this.selected.key];
        }

        this.data[this.edited.key] = this.edited.value;

        this.selected        = undefined;
        this.m_selectedState = undefined;
        this.edited          = undefined;

        this.bindInner();
        this.host.setDirty();
    }

    public cancel()
    {
        this.selected        = undefined;
        this.m_selectedState = undefined;
        this.edited          = undefined;
        this.host.detectChanges();
    }

    get modified(): boolean
    {
        if (!this.edited) return false;

        if (!this.edited.key) return false;

        if (!this.selected) return true;

        if (this.edited.key != this.selected.key) return true;

        let text = this.getText(this.edited.value);
        if (text != this.selected.text) return true;

        if (this.m_selectedState !== JSON.stringify(this.edited.value)) return true;

        return false;
    }

    get valid(): boolean
    {
        if (!this.edited) return true;

        if (!this.edited.key) return false;

        let text = this.getText(this.edited.value);
        if (!text) return false;

        return true;
    }

    //--//

    public bind(data: Lookup<V>)
    {
        this.data = data;
        this.bindInner();
        this.table.resetPagination();
    }

    protected bindInner()
    {
        this.keys = this.getAllKeys();

        if (this.m_filter)
        {
            let filter = this.m_filter.toLocaleLowerCase();

            let filteredKeys = [];
            for (let key of this.keys)
            {
                if (this.contains(key, filter) || this.shouldInclude(this.data[key], filter))
                {
                    filteredKeys.push(key);
                }
            }
            this.keys = filteredKeys;
        }

        this.table.refreshData();
    }

    public reorder(event: Event,
                   index: number,
                   newIndex: number)
    {
        event.stopPropagation();
        this.reorderList(index, newIndex);
    }

    public wasDestroyed(): boolean
    {
        return this.host.wasDestroyed();
    }

    public getItemName(): string
    {
        return this.displayName;
    }

    public async getList(): Promise<string[]>
    {
        if (!this.keys) return [];

        if (this.isOrdered())
        {
            return this.keys;
        }

        let [sortBy] = this.table.sort || [];
        if (sortBy)
        {
            switch (sortBy.prop)
            {
                case "key":
                    this.keys.sort((a,
                                    b) =>
                                   {
                                       let res = UtilsService.compareStrings(a, b, true);

                                       return sortBy.dir == "asc" ? res : -res;
                                   });
                    break;

                case "text":
                    this.keys.sort((a,
                                    b) =>
                                   {
                                       let valA = this.data[a];
                                       let valB = this.data[b];

                                       let res: number;

                                       if (typeof valA == "number")
                                       {
                                           res = valA < valB ? -1 : valA == valB ? 0 : 1;
                                       }
                                       else
                                       {
                                           let textA = this.getText(valA);
                                           let textB = this.getText(valB);

                                           res = UtilsService.compareStrings(textA, textB, true);
                                       }

                                       return sortBy.dir == "asc" ? res : -res;
                                   });
                    break;
            }
        }

        return this.keys;
    }

    public async getPage(offset: number,
                         limit: number): Promise<string[]>
    {
        if (!this.keys || this.keys.length == 0)
        {
            return [];
        }

        const start = offset * limit;
        const end   = start + limit;

        let res: string[] = [];

        for (let i = start; i < end; i++)
        {
            let key = this.keys[i];
            if (key) res.push(key);
        }

        return res;
    }

    public async transform(rows: string[]): Promise<DatatablePair<string, V>[]>
    {
        return rows.map((key) =>
                        {
                            let v = this.data[key];
                            return {
                                index  : this.getIndex(key),
                                key    : key,
                                keyText: this.getKeyText(key, v),
                                value  : v,
                                text   : this.getText(v)
                            };
                        });
    }

    private getIndex(key: string): number
    {
        if (this.isOrdered())
        {
            let allKeys = this.getAllKeys();
            return allKeys.indexOf(key);
        }

        return undefined;
    }

    public itemClicked(columnId: string,
                       item: DatatablePair<string, V>)
    {
        this.selected        = item;
        this.m_selectedState = JSON.stringify(item.value);

        this.edited = {
            index  : item.index,
            keyText: item.keyText,
            key    : item.key,
            value  : JSON.parse(this.m_selectedState),
            text   : item.text
        };

        if (this.m_onClick)
        {
            this.m_onClick();
        }

        this.host.detectChanges();
    }

    public registerOnClick(callback: () => void)
    {
        this.m_onClick = callback;
    }

    protected contains(target: string,
                       filter: string): boolean
    {
        return target && target.toLocaleLowerCase()
                               .indexOf(filter) >= 0;
    }

    //--//

    getTableConfigId(): string
    {
        return null;
    }

    getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    setColumnConfigs(columnConfigs: ColumnConfiguration[]): Promise<boolean>
    {
        return null;
    }
}

export abstract class ProviderForMap<T> extends ProviderForMapBase<T>
{

    public isOrdered(): boolean
    {
        return false;
    }

    protected reorderList(index: number,
                          newIndex: number): void
    {
        // Nothing to do
    }

    protected getAllKeys()
    {
        let keys = [];
        for (let key in this.data)
        {
            if (key) keys.push(key);
        }

        return keys;
    }
}

export class ProviderForString extends ProviderForMap<string>
{
    protected getText(v: string): string
    {
        return v || "";
    }

    protected shouldInclude(data: string,
                            filterLowercase: string): boolean
    {
        return this.contains(data, filterLowercase);
    }

    protected allocate(): string
    {
        return "";
    }
}

export class ProviderForNumber extends ProviderForMap<number>
{
    protected getText(v: number): string
    {
        if (v === undefined) return "";

        return v + "";
    }

    protected shouldInclude(data: number,
                            filterLowercase: string): boolean
    {
        return false;
    }

    protected allocate(): number
    {
        return 0;
    }
}

export abstract class ProviderForMappableList<T> extends ProviderForMapBase<T>
{
    private m_list: T[]      = [];
    private m_map: Lookup<T> = {};

    public bindList(list: T[])
    {
        this.m_list = list || [];
        this.m_map  = {};
        for (let item of this.m_list)
        {
            this.m_map[this.getKey(item)] = item;
        }

        this.data = this.m_map;
        this.bindInner();
    }

    abstract getKey(item: T): string;

    abstract setKey(item: T,
                    key: string): void;

    abstract sort(a: T,
                  b: T): number;

    protected getAllKeys()
    {
        return this.m_list.map((val) => this.getKey(val));
    }

    protected reorderList(index: number,
                          newIndex: number)
    {
        if (newIndex < 0 || newIndex >= this.m_list.length)
        {
            return;
        }

        let a                 = this.m_list[index];
        let b                 = this.m_list[newIndex];
        this.m_list[index]    = b;
        this.m_list[newIndex] = a;
        this.bindList(this.m_list);
    }

    get modified(): boolean
    {
        if (!this.edited) return false;

        if (!this.edited.key && !this.getKey(this.edited.value)) return false;

        if (!this.selected) return true;

        if (this.edited.key != this.selected.key) return true;

        let text = this.getText(this.edited.value);
        if (text != this.selected.text) return true;

        if (JSON.stringify(this.edited.value) !== this.m_selectedState) return true;

        return false;
    }

    get valid(): boolean
    {
        if (!this.edited) return true;
        if (!this.edited.key && !this.getKey(this.edited.value)) return false;
        let text = this.getText(this.edited.value);
        if (!text) return false;

        return true;
    }

    save()
    {
        this.setKey(this.edited.value, this.edited.key);
        if (this.selected)
        {
            this.m_list.splice(this.m_list.indexOf(this.selected.value), 1, this.edited.value);
        }
        else if (this.data[this.edited.key])
        {
            this.m_list.splice(this.m_list.indexOf(this.data[this.edited.key]), 1, this.edited.value);
        }
        else
        {
            this.m_list.push(this.edited.value);
        }
        this.sortList();
        super.save();
    }

    remove()
    {
        this.m_list.splice(this.m_list.indexOf(this.selected.value), 1);
        this.sortList();
        super.remove();
    }

    private sortList()
    {
        if (!this.isOrdered())
        {
            this.m_list.sort((a,
                              b) => this.sort(a, b));
        }
    }
}

export class DatatablePair<K, V>
{
    index: number;
    key: K;
    keyText: string;
    value: V;
    text: string;
}

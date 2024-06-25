import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";

@Injectable()
export class ViewStateService
{
    private state: { [key: string]: ViewState } = {};

    constructor(private route: Router,
                private navigation: AppNavigationService)
    {

    }

    /**
     * Get viewstate for the current page.
     */
    restore(): ViewState
    {
        const url = this.route.routerState.snapshot.url;

        let state = this.state[url];
        if (!state)
        {
            state           = new ViewState(this);
            this.state[url] = state;
        }

        return state;
    }

    /**
     * Save viewstate for the current page.
     * @param state
     */
    save(state: ViewState)
    {
        const url       = this.route.routerState.snapshot.url;
        this.state[url] = state;
    }

    /**
     * Set viewstate for another page.
     * @param state
     * @param url
     */
    setForUrl(state: ViewState,
              url: string)
    {
        this.state[url] = state;
    }

    public async navigateToBookmark(bookmarkUrl: string)
    {
        return this.navigation.go(bookmarkUrl);
    }

    private static removeParameterFromUrl(url: string,
                                          parameter: string): string
    {
        if (parameter)
        {
            let prefix = parameter + "=";
            let parts  = url.split(";");
            let kept   = parts.filter((part) => !part.startsWith(prefix));

            return kept.join(";");
        }

        return url;
    }
}

export class ViewState
{
    public readonly state: { [key: string]: ViewStateItem } = {};
    public readonly subStates: { [key: string]: ViewState } = {};

    constructor(private service: ViewStateService,
                private m_parent?: ViewState)
    {
    }

    getSubView(prefix: string,
               autoSave: boolean): ViewState
    {
        let subview = this.subStates[prefix];
        if (!subview)
        {
            subview                = new ViewState(this.service, this);
            this.subStates[prefix] = subview;

            if (autoSave) this.save();
        }

        return subview;
    }

    save()
    {
        if (this.m_parent)
        {
            this.m_parent.save();
        }
        else
        {
            this.service.save(this);
        }
    }

    hasKey(key: string): boolean
    {
        if (this.state)
        {
            let viewStateItem = this.state[key];
            if (viewStateItem)
            {
                return true;
            }
        }

        return false;
    }

    set(key: string,
        value: any,
        saveInBookmark: boolean = true,
        autoSave: boolean = true)
    {
        this.state[key] = new ViewStateItem(value, saveInBookmark);

        if (autoSave) this.save();
    }

    get<T>(key: string,
           defaultValue: T = null): T
    {
        if (this.hasKey(key))
        {
            return <T>this.state[key].value;
        }
        else
        {
            return defaultValue;
        }
    }

    getNumber(key: string,
              defaultValue: number = 0): number
    {
        return this.get<number>(key, defaultValue);
    }

    getString(key: string,
              defaultValue: string = null): string
    {
        return this.get<string>(key, defaultValue);
    }

    getBoolean(key: string,
               defaultValue: boolean = false): boolean
    {
        return this.get<boolean>(key, defaultValue);
    }

    remove(key: string)
    {
        delete this.state[key];

        this.save();
    }

    removeKeys(prefix: string)
    {
        let keys = this.getKeys(prefix);
        for (let key of keys)
        {
            this.remove(key);
        }
    }

    getKeys(prefix: string = null): string[]
    {
        let keys: string[] = [];
        for (let key in this.state)
        {
            if (!prefix || key.startsWith(prefix))
            {
                keys.push(key);
            }
        }
        return keys;
    }
}

export class ViewStateItem
{
    constructor(public value: any,
                public saveInBookmark: boolean)
    {
    }
}

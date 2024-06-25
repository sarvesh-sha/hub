import {moveItemInArray} from "@angular/cdk/drag-drop";
import {WorkspaceTab} from "app/customer/engines/shared/workspace-tab";
import * as Models from "app/services/proxy/model/models";
import {Lookup} from "framework/services/utils.service";
import {ToolboxCategory} from "framework/ui/blockly/toolbox-category";

export interface BlocklyWorkspaceBlocks
{
    getToolboxCategories(): Lookup<ToolboxCategory>;

    getNamespaces(): string[];
}

export abstract class BlocklyWorkspaceData
{
    private readonly m_tabsExtended: WorkspaceTab[] = [];

    constructor(private m_tabs: Models.EngineTab[] = [])
    {
        if (m_tabs.length === 0)
        {
            m_tabs.push(Models.EngineTab.newInstance({
                                                         name       : "Start",
                                                         blockChains: []
                                                     }));
        }

        this.m_tabsExtended = m_tabs.map((tab) => new WorkspaceTab(tab));
    }

    public destroy(): void
    {
        for (let tab of this.m_tabsExtended)
        {
            tab.destroy();
        }
        this.m_tabsExtended.length = 0;
        this.m_tabs                = [];
    }

    public get tabs(): WorkspaceTab[]
    {
        return this.m_tabsExtended;
    }

    public getState(): string
    {
        return JSON.stringify(this.m_tabs);
    }

    public newTab(): WorkspaceTab
    {
        let newTab = Models.EngineTab.newInstance({
                                                      name       : "New Tab",
                                                      blockChains: []
                                                  });

        let newTabExtended = new WorkspaceTab(newTab);

        this.m_tabs.push(newTab);
        this.m_tabsExtended.push(newTabExtended);
        return newTabExtended;
    }

    public closeTab(tab: WorkspaceTab): WorkspaceTab
    {
        let tabIndex = this.m_tabsExtended.indexOf(tab);
        this.m_tabs.splice(tabIndex, 1);
        this.m_tabsExtended.splice(tabIndex, 1);
        let currentTabIndex = tabIndex > 0 ? tabIndex - 1 : 0;
        return this.m_tabsExtended[currentTabIndex];
    }

    public rearrangeTabs(previousIndex: number,
                         currentIndex: number)
    {
        moveItemInArray(this.m_tabs, previousIndex, currentIndex);
        moveItemInArray(this.m_tabsExtended, previousIndex, currentIndex);
    }

    public getTabForBlock(id: string): WorkspaceTab
    {
        for (let tab of this.m_tabsExtended)
        {
            if (tab.getBlock(id))
            {
                return tab;
            }
        }

        return null;
    }

    public getInputParameters(): Models.EngineInputParameter[]
    {
        return this.filterBlocks((bl) => bl instanceof Models.EngineInputParameter);
    }

    public filterBlocks<T extends Models.EngineBlock>(cb: (block: Models.EngineBlock) => boolean): T[]
    {
        let result: T[] = [];
        this.enumerateAllBlocks((bl) =>
                                {
                                    if (cb(bl))
                                    {
                                        result.push(<T>bl);
                                    }
                                });

        return result;
    }

    public enumerateAllBlocks(cb: (block: Models.EngineBlock) => void)
    {
        for (let tab of this.m_tabsExtended)
        {
            tab.enumerateBlocks((block) => cb(block));
        }
    }

    public renameVariable(oldName: string,
                          newName: string)
    {
        for (let tab of this.m_tabsExtended)
        {
            tab.renameVariable(oldName, newName);
        }
    }
}

import {ElementRef, Injectable} from "@angular/core";

import {AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";
import {querySelectorHelper, TestDriver, waitFor} from "app/test/driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";

import {getTreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";
import {Future} from "framework/utils/concurrency";

@Injectable({providedIn: "root"})
export class SelectionDriver
{
    constructor(private m_driver: TestDriver,
                private m_overlayDriver: OverlayDriver)
    {
    }

    async makeOnlyAvailableSelection<T>(elementName: string,
                                        component: SelectComponent<T>,
                                        multi: boolean): Promise<void>
    {
        await waitFor(() => component.options?.length, `${elementName} select's options did not load`);

        const ids  = [];
        let option = component.options[0];
        while (option)
        {
            ids.push(option.id);
            option = option.children[0];
        }

        if (multi)
        {
            await this.makeMultiSelection(elementName, component, [ids]);
        }
        else
        {
            await this.makeSelection(elementName, component, ids);
        }
    }

    async makeSelection<T>(elementName: string,
                           component: SelectComponent<T>,
                           selectionHierarchy: T[],
                           searchText?: string): Promise<void>
    {
        // todo: still not testing virtual scrolling with scrolling to item
        await waitFor(() => component?.options?.length, "Can't make selection if there is nothing to pick from");

        if (!component.dropdownOpen)
        {
            await this.m_driver.click(component.elementRef, elementName);
        }

        if (searchText)
        {
            const searchInput = await waitFor(() => component.test_filterableTree?.test_filterSearch, "Could not get search input for searching " + searchText);
            await this.m_overlayDriver.waitForOpen(component.overlayConfig.optio3TestId);
            await this.m_driver.sendText(searchInput, "search input", searchText);
            await this.m_driver.sendKeys(["Enter"]);
            await Future.delayed(500);
        }

        await this.makeSelectionHelper(component, selectionHierarchy);
        await this.m_overlayDriver.waitForClose(component.overlayConfig.optio3TestId);
    }

    async makeMultiSelection<T>(elementName: string,
                                component: SelectComponent<T>,
                                selectionHierarchies: T[][])
    {
        await waitFor(() => component?.options?.length, "Can't make multi selection if there is nothing to pick from");

        await this.m_driver.click(component.elementRef, elementName);

        if (!component.multiSelectBehavior)
        {
            if (!component.allowSingleOrMulti) throw new Error("This select component is a single select and cannot be made to be a multi-select");
            const pluralityToggle = await waitFor(() => component.test_plurality, "Could not get select plurality toggle");
            await this.m_driver.click(pluralityToggle, "Select plurality toggle");
        }

        for (let selection of selectionHierarchies)
        {
            await this.makeSelectionHelper(component, selection);
        }

        await waitFor(() => component.showOkButton && component.areAcceptableChanges, "Ok button is not shown");
        await this.m_driver.click(component.test_submit, "Select component's 'Ok'");
        await this.m_overlayDriver.waitForClose(component.overlayConfig.optio3TestId);
    }

    async makeBindingSelection(bindingSelect: SelectComponent<string>,
                               graphExt: SharedAssetGraphExtended,
                               nodeId: string)
    {
        let selectionHierarchy = this.buildNodeSelectionHierarchies(graphExt, [nodeId])[0];
        await this.makeSelection("asset graph binding", bindingSelect, selectionHierarchy);
    }

    async makeBindingSelections(bindingSelect: SelectComponent<string>,
                                graphExt: SharedAssetGraphExtended,
                                nodeIds: string[])
    {
        let selectionHierarchies = this.buildNodeSelectionHierarchies(graphExt, nodeIds);
        await this.makeMultiSelection("asset graph bindings", bindingSelect, selectionHierarchies);
    }

    private buildNodeSelectionHierarchies(graphExt: SharedAssetGraphExtended,
                                          nodeIds: string[]): string[][]
    {
        let graphId = graphExt.id;
        return nodeIds.map((nodeId) =>
                           {
                               let hierarchy = [];
                               while (nodeId)
                               {
                                   hierarchy.push(nodeId);
                                   nodeId = graphExt.getNodeParentId(nodeId);
                               }

                               return hierarchy.map((nodeId) => Models.AssetGraphBinding.newInstance({
                                                                                                         nodeId : nodeId,
                                                                                                         graphId: graphId
                                                                                                     }))
                                               .map((nodeBinding) => AssetGraphTreeNode.getIdFromBinding(nodeBinding));
                           });
    }

    private async makeSelectionHelper<T>(component: SelectComponent<T>,
                                         selection: T[])
    {
        for (let id of selection)
        {
            const option = getTreeNode(component.options, id);
            if (!option) throw Error(`option with id '${id}' not found`);

            if (!option.children?.length || !await this.selectNodeExpanded(component, id))
            {
                await this.selectOptionHelper(".o3-select--tree-container label", option?.label, false);
            }
        }
    }

    private async selectNodeExpanded<T>(component: SelectComponent<T>,
                                        id: T): Promise<boolean>
    {
        await waitFor(() => component.test_isNodeExpanded(id) != null, "whether or not node is expanded was not resolved", 100);
        return component.test_isNodeExpanded(id);
    }

    async makeMatSelection(matSelectName: string,
                           matSelect: ElementRef<HTMLElement>,
                           targetLabel: string): Promise<void>
    {
        if (!matSelect) throw new Error("Unable to trigger mat-select. Was going to press " + targetLabel);

        await this.m_driver.click(matSelect, matSelectName);
        await this.selectOptionHelper(".mat-option", targetLabel, false);
    }

    async selectMenuOption(menuName: string,
                           menuTrigger: ElementRef<HTMLElement>,
                           ...optionLabels: string[]): Promise<void>
    {
        if (!menuTrigger) throw new Error("Unable to trigger menu. Was going to press " + optionLabels);

        await this.m_driver.click(menuTrigger, menuName + " menu trigger");
        for (let i = 0; i < optionLabels.length; i++)
        {
            await this.selectOptionHelper(".mat-menu-item", optionLabels[i], i < optionLabels.length - 1);
        }
    }

    private async selectOptionHelper(query: string,
                                     label: string,
                                     hover: boolean): Promise<void>
    {
        const option = await waitFor(() => querySelectorHelper(query, (elem) => elem.innerText.trim() === label.trim()),
                                     "Unable to find option with label " + label);
        await this.m_driver.waitForStabilization(option, "select option");

        let element = `option with label '${label}'`;
        if (hover)
        {
            await this.m_driver.hover(option, element);
        }
        else
        {
            await this.m_driver.click(option, element);
        }
    }
}

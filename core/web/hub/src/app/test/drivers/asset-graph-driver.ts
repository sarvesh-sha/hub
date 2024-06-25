import {ElementRef, Injectable} from "@angular/core";

import {AssetStructureDetailsPageComponent} from "app/customer/configuration/asset-structures/asset-structure-details-page.component";
import {AssetStructureListPageComponent} from "app/customer/configuration/asset-structures/asset-structure-list-page.component";
import {AssetStructureWizardDataStepComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-data-step.component";
import {AssetStructureWizardDialogComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {AssetStructureWizardNameStepComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-name-step.component";
import {DashboardPageComponent} from "app/dashboard/dashboard/dashboard-page.component";
import {WidgetEditorWizardGraphsStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-graphs-step.component";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {SharedAssetGraph} from "app/services/proxy/model/SharedAssetGraph";
import {AssetGraphEditorComponent} from "app/shared/assets/asset-graph-editor/asset-graph-editor.component";
import {AssetGraphStepComponent} from "app/shared/assets/asset-graph-step/asset-graph-step.component";
import {MultipleGraphConfigurationComponent} from "app/shared/assets/configuration/multiple-graph-configuration.component";
import {TagConditionNodeComponent} from "app/shared/assets/tag-condition-builder/tag-condition-node.component";
import {ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {DataSourceWizardGraphsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graphs-step.component";
import {querySelectorHelper, TestDriver, waitFor} from "app/test/driver";
import {ConfirmationDriver} from "app/test/drivers/confirmation-driver";
import {FormDriver} from "app/test/drivers/form-driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";
import {SelectionDriver} from "app/test/drivers/selection-driver";
import {WizardDriver} from "app/test/drivers/wizard-driver";

@Injectable({providedIn: "root"})
export class AssetGraphDriver
{
    constructor(private m_driver: TestDriver,
                private m_confirmationDriver: ConfirmationDriver,
                private m_formDriver: FormDriver,
                private m_overlayDriver: OverlayDriver,
                private m_selectionDriver: SelectionDriver,
                private m_wizardDriver: WizardDriver)
    {
    }

    async ensureAssetGraph(assetGraphName: string,
                           buildFn: (graphEditor: AssetGraphEditorComponent) => Promise<void>,
                           expectNameStep: boolean): Promise<boolean>
    {
        try
        {
            if (!await this.hasAssetGraph(assetGraphName))
            {
                await this.m_driver.navigate(AssetStructureListPageComponent, "/configuration/asset-structures");
                const structureListPage = await this.m_driver.getComponent(AssetStructureListPageComponent);
                const menuButton        = await waitFor(() => structureListPage.test_tabGroup?.test_menuTrigger, "Could not get asset structure page's menu trigger");
                await this.m_selectionDriver.selectMenuOption("asset structure list page", menuButton, structureListPage.newStructureLabel);

                const cfgExt = await this.assetStructureWizardFlow(assetGraphName, async (step) => await buildFn(step.test_graphEditor), expectNameStep);

                await waitFor(() => this.m_driver.app.domain.assetGraphs.getConfig(cfgExt.id), "Failed to create new asset graph");
                return true;
            }
            return false;
        }
        catch (e)
        {
            try
            {
                await this.removeAssetGraph(assetGraphName);
            }
            catch (e2)
            {
                // clean up failed but don't distract from original error
            }

            throw e;
        }
    }

    async removeAssetGraph(assetGraphName: string): Promise<void>
    {
        if (await this.hasAssetGraph(assetGraphName))
        {
            await this.m_driver.navigate(DashboardPageComponent, "/home");
            await this.m_driver.navigate(AssetStructureListPageComponent, "/configuration/asset-structures");
            const structureListPage = await this.m_driver.getComponent(AssetStructureListPageComponent);
            const structureList     = await waitFor(() => structureListPage.test_assetStructureList, "Could not grab asset structure list");
            const rowOfInterest     = await querySelectorHelper(".mat-row pre", (elem) => elem.innerText === assetGraphName, structureList.nativeElement);
            await this.m_driver.click(rowOfInterest, "asset structure table row");

            const detailPage       = await this.m_driver.getComponent(AssetStructureDetailsPageComponent);
            const internalTab      = await waitFor(() => detailPage.test_tabGroup?.activeTabs[0], "Could not get asset structure detail page's internal tab");
            const actionOfInterest = internalTab.actions.find((action) => action.icon === "delete");
            await this.m_driver.clickO3Element(actionOfInterest.optio3TestId, "asset structure delete button");
            await this.m_confirmationDriver.handleConfirmationDialog();
        }
    }

    getMultipleGraphsConfig(graphsStep: WidgetEditorWizardGraphsStepComponent | DataSourceWizardGraphsStepComponent,
                            local: boolean): Promise<MultipleGraphConfigurationComponent>
    {
        return this.m_driver.getComponentValue(graphsStep,
                                               (graphsStep) => local ? graphsStep.test_localGraphs : (<DataSourceWizardGraphsStepComponent>graphsStep).test_externalGraphs,
                                               `${local ? "local " : "external "} graph configuration component`);
    }

    async importAssetGraphFlow(graphsStep: WidgetEditorWizardGraphsStepComponent | DataSourceWizardGraphsStepComponent,
                               assetGraphName: string,
                               expectNameStep: boolean): Promise<void>
    {
        const localGraphs          = await this.getMultipleGraphsConfig(graphsStep, true);
        const configureGraphButton = await waitFor(() => localGraphs.test_configureGraphs.last, "Could not get last asset graph configure trigger");
        await this.m_driver.click(configureGraphButton, "configure asset graph");
        await this.assetStructureWizardFlow(assetGraphName, (step) => this.importSharedAssetGraph(step, assetGraphName), expectNameStep);
    }

    async addImportAssetGraphFlow(graphsStep: WidgetEditorWizardGraphsStepComponent | DataSourceWizardGraphsStepComponent,
                                  assetGraphName: string,
                                  expectNameStep: boolean,
                                  local: boolean): Promise<void>
    {
        const multipleGraphs = await this.getMultipleGraphsConfig(graphsStep, local);
        const addGraphButton = await waitFor(() => multipleGraphs.test_table?.test_add, "Could not get add asset graph button");
        await this.m_driver.click(addGraphButton, "add asset graph");

        await this.assetStructureWizardFlow(assetGraphName, (step) => this.importSharedAssetGraph(step, assetGraphName), expectNameStep);
    }

    async assetStructureWizardFlow(assetGraphName: string,
                                   callback: (step: AssetGraphStepComponent) => Promise<void>,
                                   expectNameStep: boolean): Promise<SharedAssetGraphExtended>
    {
        const assetStructureWizardDialog = await this.m_driver.getComponent(AssetStructureWizardDialogComponent);
        const assetStructureWizard       = assetStructureWizardDialog.wizard;

        const editorStep  = await this.m_wizardDriver.getStep(assetStructureWizard, AssetStructureWizardDataStepComponent);
        const graphEditor = await waitFor(() => editorStep.test_graphStep, "Could not grab asset structure editor");

        await callback(graphEditor);

        if (expectNameStep)
        {
            await this.m_wizardDriver.stepNTimes(assetStructureWizard, 1);

            const nameStep = await this.m_wizardDriver.getStep(assetStructureWizard, AssetStructureWizardNameStepComponent);
            if (assetGraphName && assetGraphName !== nameStep.data.graph.name)
            {
                await waitFor(() => nameStep.test_name, "Could not grab asset structure wizard's name input");
                await this.m_driver.sendText(nameStep.test_name, "asset structure name", assetGraphName, true);
            }
        }

        const cfgExt = assetStructureWizardDialog.data.graph;
        await this.m_wizardDriver.save(assetStructureWizard, `Failed to save new asset structure "${assetGraphName}"`);

        return cfgExt;
    }

    async getAssetGraph(assetGraphName: string): Promise<SharedAssetGraph>
    {
        const ids    = await this.m_driver.app.domain.assetGraphs.getGraphIds();
        const graphs = await this.m_driver.app.domain.assetGraphs.getConfigBatch(ids);
        return graphs.find((graph) => graph.name === assetGraphName);
    }

    async hasAssetGraph(assetGraphName: string): Promise<boolean>
    {
        return !!await this.getAssetGraph(assetGraphName);
    }

    async addAssetGraphNode(graphEditor: AssetGraphEditorComponent,
                            parentNodeId: string,
                            nodeName: string,
                            nodeType: ConditionNodeType,
                            negatedType: boolean,
                            condition: string): Promise<string>
    {
        const addNodeButtons = <ElementRef<HTMLElement>[]>(await waitFor(() => graphEditor.test_newNodes.length && graphEditor.test_newNodes, "Could not grab add buttons")).toArray();
        let addButton: ElementRef;
        if (parentNodeId)
        {
            addButton = await waitFor(() => addNodeButtons.find((buttonRef) => buttonRef.nativeElement.className.indexOf(parentNodeId) >= 0),
                                      "Could not find add button to append node to: " + parentNodeId);
        }
        else
        {
            addButton = addNodeButtons[0];
        }
        await this.m_driver.click(addButton, "Add asset node button");
        await this.m_overlayDriver.waitForOpen(graphEditor.overlayConfig.optio3TestId);

        const nodeId = graphEditor.editNode.id;
        if (nodeName)
        {
            const nameElem = await waitFor(() => graphEditor.test_nodeName, "Could not grab node name input");
            await this.m_driver.sendText(nameElem, "node name", nodeName);
        }

        const conditionNodeComponent = await waitFor(() => graphEditor.test_tagBuilder?.test_conditionNode?.test_type && graphEditor.test_tagBuilder.test_conditionNode,
                                                     "Could not get type mat-menu");
        await this.makeConditionSelection(conditionNodeComponent, nodeType, negatedType, condition);
        await this.m_formDriver.submitOverlayForm(graphEditor.test_nodeDialog, "Save");

        return nodeId;
    }

    async configureAssetGraphNode(graphEditor: AssetGraphEditorComponent,
                                  nodeId: string,
                                  editFn: (graphEditor: AssetGraphEditorComponent) => Promise<void>): Promise<void>
    {
        const configureNodeButtons = <ElementRef<HTMLElement>[]>(await waitFor(() => graphEditor.test_configureNodes.length && graphEditor.test_configureNodes,
                                                                               "Could not grab configure node buttons")).toArray();
        let configureButton        = await waitFor(() => configureNodeButtons.find((buttonRef) => buttonRef.nativeElement.className.indexOf(nodeId) >= 0),
                                                   "Could not find configure node button)");
        await this.m_driver.click(configureButton, "Configure asset graph node button");

        await this.m_overlayDriver.waitForOpen(graphEditor.overlayConfig.optio3TestId);
        await editFn(graphEditor);
        await this.m_formDriver.submitOverlayForm(graphEditor.test_nodeDialog, "Save");
    }

    async makeConditionSelection(conditionNode: TagConditionNodeComponent,
                                 nodeType: ConditionNodeType,
                                 negateType: boolean,
                                 condition: string): Promise<void>
    {
        const conditionOption = conditionNode.conditionTypeOptions.find((option) => option.type === nodeType && negateType === option.negate);
        await this.m_selectionDriver.makeSelection(conditionOption.label, conditionNode.test_type, [conditionOption.id], conditionOption.label);

        const conditionSelect  = await waitFor(() => conditionNode.conditionSelector?.options && conditionNode.conditionSelector, "Could not get condition selector");
        const optionOfInterest = await waitFor(() => conditionSelect.options.find((option) => option.label.startsWith(condition)),
                                               "Could not find option with label that starts with " + condition);
        await this.m_selectionDriver.makeSelection("condition select", conditionNode.conditionSelector, [optionOfInterest.id], condition);
    }

    async importSharedAssetGraph(step: AssetGraphStepComponent,
                                 assetGraphName: string): Promise<void>
    {
        await waitFor(() => step.hasSavedStructures, "Could not find any asset structures to import");
        const importButton = await waitFor(() => step.test_importButton, "Could not get import asset structure button");
        await this.m_driver.click(importButton, "import shared asset structure");
        await this.m_overlayDriver.waitForOpen(step.test_importStandardForm.overlayConfig.optio3TestId);
        const select          = await waitFor(() => step.test_importSelect, "Could not get import asset graph select component");
        const graphOfInterest = await this.getAssetGraph(assetGraphName);
        await this.m_selectionDriver.makeSelection("asset graph selection", select, [graphOfInterest.id]);

        await waitFor(() => step.sharedGraphId === graphOfInterest.id, "Graph to import was not selected");
        await this.m_formDriver.submitOverlayForm(step.test_importStandardForm, "Import");
        await waitFor(() => !step.sharedGraphId, "sharedGraphId should have been cleared");
    }
}

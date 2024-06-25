import {ComponentType} from "@angular/cdk/portal";
import {Injectable} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {DataSourceWizardPointsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-points-step.component";
import {DataSourceWizardTypeStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-type-step.component";

import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {TestDriver, waitFor} from "app/test/driver";
import {DatatableDriver} from "app/test/drivers/datatable-driver";
import {SelectionDriver} from "app/test/drivers/selection-driver";

import {UtilsService} from "framework/services/utils.service";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {WizardComponent} from "framework/ui/wizards/wizard.component";
import {Future} from "framework/utils/concurrency";

@Injectable({providedIn: "root"})
export class WizardDriver
{
    constructor(private m_driver: TestDriver,
                private m_datatableDriver: DatatableDriver,
                private m_selectionDriver: SelectionDriver)
    {
    }

    async getWizard<T extends WizardDialogState, U extends WizardDialogComponent<T>>(wizardType: ComponentType<U>): Promise<WizardComponent<T>>
    {
        let wizardDialogComponent = await this.m_driver.getComponent(wizardType);
        let wizard                = await waitFor(() => wizardDialogComponent.wizard, "Could not get wizard of type " + wizardType);
        await this.waitForWizard(wizard);
        return wizard;
    }

    async getStep<T, U extends WizardStep<T>>(wizard: WizardComponent<T>,
                                              stepType: ComponentType<U>): Promise<U>
    {
        return waitFor(() => wizard.getStep(stepType), "Failed to get " + stepType);
    }

    async waitForWizard(wizard: WizardComponent<any>): Promise<void>
    {
        await waitFor(() => !wizard.loading, "Wizard did not stop loading/animating");
    }

    async stepNTimes(wizard: WizardComponent<any>,
                     numSteps: number): Promise<void>
    {
        let prevIdx: number;
        let successfulSteps = -1;
        while (await UtilsService.executeWithRetries(async () => wizard.selectedIndex !== prevIdx && !wizard.loading, 7, 250, undefined, 2, true))
        {
            successfulSteps++;
            if (successfulSteps >= numSteps) break;

            prevIdx = wizard.selectedIndex;
            await this.m_driver.click(wizard.test_next, "Wizard next button");
        }

        await waitFor(() => successfulSteps === numSteps, `Stepped ${successfulSteps} times instead of ${numSteps} times`);
    }

    async save(wizard: WizardComponent<any>,
               errorMsg: string): Promise<void>
    {
        await waitFor(() => wizard.allStepsValid() && wizard.test_finish, errorMsg);
        await this.m_driver.click(wizard.test_finish, "Wizard finish button");
        await Future.delayed(OverlayComponent.animationDuration);
    }

    async selectDataSourceType(wizard: WizardComponent<DataSourceWizardState>,
                               type: Models.TimeSeriesChartType): Promise<void>
    {
        await this.waitForWizard(wizard);
        const dataSourceTypeStep = await this.getStep(wizard, DataSourceWizardTypeStepComponent);
        const typeOption         = await waitFor(() => dataSourceTypeStep.typeOptions.find((option) => option.id === type), `could not find ${type} type`);
        const testTypes          = await waitFor(() => dataSourceTypeStep.test_preview.test_selector, "could not get test type select");
        await this.m_selectionDriver.makeSelection("type", testTypes, [typeOption.id]);
        await waitFor(() => wizard.data.type === typeOption.id, `type did not update from ${wizard.data.type} to ${typeOption.id}`);
        await this.stepNTimes(wizard, 1);
    }

    async standardSelectControlPoints(wizard: WizardComponent<DataSourceWizardState>,
                                      searchText: string,
                                      expectedCt: number,
                                      maxSelectCt?: number): Promise<number>
    {
        const pointsStep    = await this.getStep(wizard, DataSourceWizardPointsStepComponent);
        const cpSearchInput = await waitFor(() => pointsStep.test_cpSelector?.test_searchFilters?.test_searchInput, "Could not get search input");
        await this.m_driver.sendText(cpSearchInput, "control point search", searchText);
        let datRows: NodeListOf<HTMLElement>;
        await waitFor(() =>
                      {
                          datRows = document.querySelectorAll("o3-control-point-selector mat-row");
                          return datRows?.length && (!expectedCt || datRows.length === expectedCt);
                      },
                      `Found ${datRows?.length} rows. ${expectedCt ? "Expected " + expectedCt : ""}`);

        let ct = 0;
        for (let row of Array.from(datRows))
        {
            await this.m_driver.click(row, "control point row");
            ct++;

            if (maxSelectCt != null && ct >= maxSelectCt) break;
        }

        return ct;
    }
}

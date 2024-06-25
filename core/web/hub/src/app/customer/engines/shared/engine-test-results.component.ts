import {Component, Input, OnDestroy, ViewChild} from "@angular/core";
import {MatSliderChange} from "@angular/material/slider";

import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import * as Models from "app/services/proxy/model/models";
import {LogFormatter} from "app/shared/logging/application-log";
import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";

import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {AsyncDebouncer} from "framework/utils/debouncers";

@Component({
               selector   : "o3-engine-test-results",
               templateUrl: "./engine-test-results.component.html",
               styleUrls  : ["./engine-test-results.component.scss"]
           })
export class EngineTestResultsComponent implements OnDestroy,
                                                   IConsoleLogProvider
{
    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    private m_workspace: AppBlocklyWorkspaceComponent;

    private m_testResults: Models.EngineExecutionStep[] = [];
    private m_processedResults: StepExtended[]          = [];
    private m_currentStep: StepExtended;

    actions: Action[]   = [];
    failures: Failure[] = [];

    logDialogConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    private m_tabDebouncer = new AsyncDebouncer<void>(200, async ([id]) =>
    {
        this.m_workspace.switchToTabWithBlock(id);
        this.resolveHighlights();
    });

    private m_playInterval: number;

    sliderSteps: number;
    sliderPosition: number = 0;

    @Input()
    public set testResults(testResults: Models.EngineExecutionStep[])
    {
        this.m_testResults = testResults || [];
        this.updateState();
    }

    @Input()
    public set workspace(workspace: AppBlocklyWorkspaceComponent)
    {
        this.m_workspace = workspace;
        this.updateState();
    }

    @Input() public logLines: Models.LogLine[];

    refreshLog()
    {
        this.log.bind(this);
        this.log.refresh(true);
    }

    ngOnDestroy()
    {
        this.stop();
        this.clearHighlights();
    }

    public get assignments(): Assignment[]
    {
        return this.m_currentStep?.assignments;
    }

    public hasPrevious(): boolean
    {
        return this.m_currentStep && this.m_currentStep.stepIndex > 0;
    }

    public hasNext(): boolean
    {
        return this.m_currentStep && (this.m_currentStep.stepIndex + 1) < this.m_processedResults.length;
    }

    public next(stepOver: boolean = false)
    {
        let step         = this.m_currentStep;
        let siblingDepth = stepOver ? step?.stack?.depth : 1E9;

        while (true)
        {
            let stepNext = this.m_processedResults[step ? step.stepIndex + 1 : 0];
            if (!stepNext) break;

            step = stepNext;

            if (step.stack.depth <= siblingDepth)
            {
                break;
            }
        }

        this.setCurrentStep(step);
    }

    public previous(stepOver: boolean = false)
    {
        let step         = this.m_currentStep;
        let siblingDepth = stepOver ? step?.stack?.depth : 1E9;

        while (true)
        {
            let stepPrevious = this.m_processedResults[step ? step.stepIndex - 1 : 0];
            if (!stepPrevious) break;

            step = stepPrevious;

            if (step.stack.depth <= siblingDepth)
            {
                break;
            }
        }

        this.setCurrentStep(step);
    }

    public togglePlay()
    {
        if (this.canPlay())
        {
            this.play();
        }
        else
        {
            this.pause();
        }
    }

    public stop()
    {
        this.pause();
        this.reset();
    }

    public fastForward()
    {
        this.pause();
        this.play(true, 150);
    }

    public fastRewind()
    {
        this.pause();
        this.play(false, 150);
    }

    public canPlay()
    {
        return !this.m_playInterval;
    }

    public slide(event: MatSliderChange)
    {
        this.pause();

        for (let step of this.m_processedResults)
        {
            if (step.callIndex >= event.value)
            {
                this.setCurrentStep(step);
                return;
            }
        }

        // Handle case where execution terminated early (likely due to failure)
        this.setCurrentStep(this.m_processedResults[this.m_processedResults.length - 1]);
    }

    setCurrentStep(step: StepExtended)
    {
        this.m_currentStep = step;

        let stack = step?.stack;
        if (stack)
        {
            this.m_tabDebouncer.invoke(stack.id);
        }

        this.updateSliderPosition();
        this.resolveHighlights();
    }

    public getLogCount(): number
    {
        return this.logLines?.length || 0;
    }

    public async getLogPage(start: number,
                            end: number): Promise<IConsoleLogEntry[]>
    {
        let logs = this.logLines?.slice(start, end) || [];
        return logs.map((l) => this.log.newLogEntry(l));
    }

    public downloadLog()
    {
        const formatted = LogFormatter.formatLines(this.logLines)
                                      .join("\n");

        DownloadDialogComponent.open(this.m_workspace,
                                     "Engine Log",
                                     DownloadDialogComponent.fileName("engine-log", ".txt"),
                                     formatted);
    }

    public async performFilter(filter: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return [];
    }

    private updateSliderPosition()
    {
        this.sliderPosition = this.m_currentStep?.callIndex || 0;
    }

    private resolveHighlights()
    {
        let workspace = this.m_workspace?.workspace;
        if (workspace)
        {
            workspace.highlightBlock(null);

            let stack = this.m_currentStep?.stack;
            while (stack)
            {
                workspace.highlightBlock(stack.id, true);
                stack = stack.previous;
            }
        }
    }

    private clearHighlights()
    {
        this.m_workspace?.workspace?.highlightBlock(null);
    }

    private play(forward = true,
                 speed   = 500)
    {
        if (!this.canPlay())
        {
            return;
        }

        this.m_playInterval = setInterval(() =>
                                          {
                                              if (forward && this.hasNext())
                                              {
                                                  this.next();
                                              }
                                              else if (!forward && this.hasPrevious())
                                              {
                                                  this.previous();
                                              }
                                              else
                                              {
                                                  this.pause();
                                              }
                                          }, speed);
    }

    private canPause()
    {
        return !this.canPlay();
    }

    private pause()
    {
        if (!this.canPause())
        {
            return;
        }

        clearInterval(this.m_playInterval);
        this.m_playInterval = undefined;
    }

    private reset()
    {
        this.m_currentStep = this.m_processedResults[0];
        this.updateSliderPosition();
        this.resolveHighlights();
    }

    private updateState()
    {
        if (this.m_workspace)
        {
            let callIndex                 = 0;
            let assignments: Assignment[] = [];
            let actions: Action[]         = [];
            let failures: Failure[]       = [];
            let stack: BlockStack         = new BlockStack();
            stack.depth                   = 0;

            let processedResults: StepExtended[] = [];

            for (let step of this.m_testResults)
            {
                let newStep = new StepExtended();

                if (step.failure)
                {
                    failures.push({
                                      text: step.failure,
                                      step: newStep
                                  });
                }

                let actionText = this.getStepActionText(step);
                if (actionText)
                {
                    actions.push({
                                     text: actionText,
                                     step: newStep
                                 });
                }

                if (step.assignment)
                {
                    assignments = assignments.filter(a => a.name != step.assignment.name);
                    assignments.push(new Assignment(step.assignment.name, step.assignment.value));
                }

                if (step.enteringBlockId)
                {
                    callIndex++;

                    let newHighlightedBlocks = new BlockStack();

                    newHighlightedBlocks.previous = stack;
                    newHighlightedBlocks.depth    = stack.depth + 1;
                    newHighlightedBlocks.id       = step.enteringBlockId;

                    stack = newHighlightedBlocks;
                }

                if (step.leavingBlockId)
                {
                    callIndex++;

                    if (stack.previous)
                    {
                        stack = stack.previous;
                    }

                    continue;
                }

                newStep.step        = step;
                newStep.stepIndex   = processedResults.length;
                newStep.callIndex   = callIndex;
                newStep.assignments = assignments;
                newStep.stack       = stack;

                processedResults.push(newStep);
            }

            // Always have a step at the end.
            let lastStep         = new StepExtended();
            lastStep.stepIndex   = processedResults.length;
            lastStep.callIndex   = callIndex + 1;
            lastStep.assignments = assignments;
            lastStep.stack       = stack;

            processedResults.push(lastStep);

            this.sliderSteps        = callIndex;
            this.m_processedResults = processedResults;
            this.actions            = actions;
            this.failures           = failures;
        }

        this.reset();
    }

    private getStepActionText(step: Models.EngineExecutionStep): string
    {
        if (step instanceof Models.AlertEngineExecutionStepCreateAlert)
        {
            return `Create alert ${step.record.sysId} for ${step.controlPoint.sysId}`;
        }

        if (step instanceof Models.AlertEngineExecutionStepSetAlertStatus)
        {
            return `Set alert ${step.record.sysId} to status ${step.status}`;
        }

        if (step instanceof Models.AlertEngineExecutionStepCommitAction)
        {
            if (step.details instanceof Models.AlertEngineValueEmail)
            {
                return `Email Sent: ${step.details.subject}`;
            }

            if (step.details instanceof Models.AlertEngineValueSms)
            {
                return "SMS Sent";
            }

            if (step.details instanceof Models.AlertEngineValueTicket)
            {
                return "Ticket Created";
            }
        }

        if (step instanceof Models.AlertEngineExecutionStepSetControlPointValue)
        {
            return `Set control point ${step.controlPoint.sysId} to value ${Assignment.getValueString(step.value)}`;
        }

        if (step instanceof Models.NormalizationEngineExecutionStepPointClassification)
        {
            let result = `Point Classified: ${step.pointClassId ? step.pointClassId : "Unclassified"}`;
            if (step.classificationAssignment?.regex)
            {
                result += ` Regex: ${step.classificationAssignment.regex}`;
            }

            if (step.classificationAssignment?.comment)
            {
                result += ` - ${step.classificationAssignment.comment}`;
            }

            if (step.classificationDocument)
            {
                result += ` Score: ${step.classificationDocument.score.toFixed(3)} Text: ${step.classificationDocument.text}`;
            }

            return result;
        }

        if (step instanceof Models.NormalizationEngineExecutionStepPushEquipment)
        {
            return `Push Equipment - ${Assignment.getEquipmentString(step.equipment)}, Parent - ${Assignment.getEquipmentString(step.parentEquipment)} `;
        }

        if (step instanceof Models.NormalizationEngineExecutionStepEquipmentClassification)
        {
            return `Equipment Classified: ${Assignment.getEquipmentString(step.equipment)}${step.classificationAssignment ? ", Regex: " + step.classificationAssignment.regex : ""}`;
        }

        if (step instanceof Models.NormalizationEngineStatementSetEngineeringUnits)
        {
            return `Point assigned units: ${step.units}`;
        }

        return "";
    }
}

class Assignment
{
    constructor(private m_name: string,
                private m_value: Models.EngineValue)
    {
    }

    public get name()
    {
        return this.m_name;
    }

    public get value()
    {
        if (!this.m_value)
        {
            return null;
        }

        return Assignment.getValueString(this.m_value);
    }

    public static getValueString(executionValue: Models.EngineValue): string
    {
        if (executionValue instanceof Models.EngineValuePrimitiveBoolean || executionValue instanceof Models.EngineValuePrimitiveNumber || executionValue instanceof Models.EngineValuePrimitiveString)
        {
            if (executionValue.value === null || executionValue.value === undefined)
            {
                return "null";
            }

            return executionValue.value.toString();
        }

        if (executionValue instanceof Models.EngineValueDateTime)
        {
            return Assignment.formatDate(executionValue.value);
        }

        if (executionValue instanceof Models.EngineValueDateTimeRange)
        {
            let start = Assignment.formatDate(executionValue.start);
            let end   = Assignment.formatDate(executionValue.end);

            return `${start} - ${end}`;
        }

        if (executionValue instanceof Models.EngineValueList)
        {
            if (executionValue.length <= 16 && executionValue instanceof Models.EngineValueListConcrete)
            {
                return `[ ${executionValue.elements.map((v) => Assignment.getValueString(v))
                                          .join(", ")} ]`;
            }

            return `[ <array of ${executionValue.length} elements> ]`;
        }

        if (executionValue instanceof Models.EngineValueDuration)
        {
            return `${executionValue.amount} ${executionValue.unit}`;
        }

        if (executionValue instanceof Models.AlertEngineValueAlert)
        {
            return `Alert -- Type: ${executionValue.type}, Severity: ${executionValue.severity}, Status: ${executionValue.status}, Control Point: ${executionValue.controlPoint.sysId}`;
        }

        if (executionValue instanceof Models.AlertEngineValueControlPoint)
        {
            return `Control Point: ${executionValue.record ? executionValue.record.sysId : "null"}`;
        }

        if (executionValue instanceof Models.AlertEngineValueDevice)
        {
            return `Device: ${executionValue.record ? executionValue.record.sysId : "null"}`;
        }

        if (executionValue instanceof Models.AlertEngineValueLogicalAsset)
        {
            return `Logical Asset: ${executionValue.record ? executionValue.record.sysId : "null"}`;
        }

        if (executionValue instanceof Models.AlertEngineValueAsset)
        {
            return `Asset: ${executionValue.record ? executionValue.record.sysId : "null"}`;
        }

        if (executionValue instanceof Models.AlertEngineValueDeliveryOptions)
        {
            return `${executionValue.resolvedUsers.length} users`;
        }

        if (executionValue instanceof Models.AlertEngineValueSample)
        {
            return `Sample: ${Assignment.formatDate(executionValue.timestamp)}`;
        }

        if (executionValue instanceof Models.NormalizationEngineValueEquipment)
        {
            return `Equipment - ${Assignment.getEquipmentString(executionValue)}`;
        }

        if (executionValue instanceof Models.NormalizationEngineValueDocument)
        {
            return `"ID: ${executionValue.id} Text: ${executionValue.text}"`;
        }

        if (executionValue instanceof Models.NormalizationEngineValueController)
        {
            return `${executionValue.objectId} - ${executionValue.name}`;
        }

        if (executionValue instanceof Models.NormalizationEngineValuePoint)
        {
            return `${executionValue.objectId} - ${executionValue.name}`;
        }

        if (executionValue instanceof Models.EngineValueLookupTable)
        {
            return `Lookup Table`;
        }

        return "";
    }

    public static getEquipmentString(equipment: Models.NormalizationEngineValueEquipment): string
    {
        if (!equipment) return "None";
        let classified = "";
        if (equipment.setUnclassified) classified = "Unclassified";
        if (equipment.equipmentClassId) classified = equipment.equipmentClassId;
        return `Name: ${equipment.name}${classified ? ", Class: " + classified : ""}`;
    }

    private static formatDate(date: Date): string
    {
        if (date)
        {
            return MomentHelper.parse(date)
                               .format("YYYY-MM-DD HH:mm:ss.SSS");
        }
        return "";
    }
}

class BlockStack
{
    previous: BlockStack;
    depth: number;
    id: string;
}

class StepExtended
{
    step: Models.EngineExecutionStep;

    stepIndex: number;
    callIndex: number;

    assignments: Assignment[];
    stack: BlockStack;
}

class Action
{
    text: string;
    step: StepExtended;
}

class Failure
{
    text: string;
    step: StepExtended;
}

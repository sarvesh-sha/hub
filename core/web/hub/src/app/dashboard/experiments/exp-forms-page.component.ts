import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";

import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";

import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import Shepherd from "shepherd.js";

@Component({
               selector   : "o3-experiments-forms-page",
               templateUrl: "./exp-forms-page.component.html",
               styleUrls  : ["./exp-forms-page.component.scss"]
           })
export class ExperimentsFormsPageComponent extends ExperimentsBasePageComponent
{
    selectedDateTime: Date;
    selectedDate: Date;
    selectedTime: Date;
    minimumDate: Date = new Date("4/1/2018");
    maximumDate: Date = new Date("6/12/2018");

    dropdownValue: string        = "dropdown test";
    dialogValue: string          = "dialog test";
    draggableDialogValue: string = "draggable dialog test";

    units = Models.EngineeringUnitsFactors.newInstance({
                                                           numeratorUnits  : [
                                                               Models.EngineeringUnits.us_gallons,
                                                               Models.EngineeringUnits.feet
                                                           ],
                                                           denominatorUnits: [Models.EngineeringUnits.hours]
                                                       });

    sampleOptions: ControlOption<string>[] = [
        new ControlOption<string>("a", "Option 1"),
        new ControlOption<string>("b", "Option 2"),
        new ControlOption<string>("c", "Option 3")
    ];

    @ViewChild("widgetsForm") widgetsForm: NgForm;

    dialogConfig = new OverlayConfig();

    draggableDialogConfig = OverlayConfig.newInstance({
                                                          isDraggable    : true,
                                                          showCloseButton: true
                                                      });
    dropdownConfig        = OverlayConfig.dropdown({width: 250});

    shepherdTest: Shepherd.Tour;

    editorData: any[];

    constructor(inj: Injector)
    {
        super(inj);

        const builtInButtons = {
            cancel: {
                classes  : "cancel-button",
                secondary: true,
                text     : "Exit",
                action   : function (this: Shepherd.Tour) { this.cancel(); }
            },
            next  : {
                classes: "next-button",
                text   : "Next",
                action : function (this: Shepherd.Tour) { this.next(); }
            },
            back  : {
                classes: "back-button",
                text   : "Back",
                action : function (this: Shepherd.Tour) { this.back(); }
            }
        };

        const steps: Shepherd.Step.StepOptions[] = [
            {
                attachTo: {
                    element: "#datePicker1",
                    on     : "bottom"
                },
                buttons : [
                    builtInButtons.cancel,
                    builtInButtons.next
                ],
                classes : "custom-class-name-1 custom-class-name-2",
                id      : "step1",
                title   : "Welcome to Shepherd!",
                text    : `
          <p>
            Shepherd is a JavaScript library for guiding users through your app.
            View the github for <a href="https://github.com/shipshapecode/shepherd">Shepherd</a>
            and extends its functionality. Shepherd uses <a href="https://atomiks.github.io/tippyjs/">Tippy.js</a>,
            another open source library, to position all of its steps and enable entrance and exit animations.
          </p>

          <p>
            Tippy makes sure your steps never end up off screen or cropped by an
            overflow. Try resizing your browser to see what we mean.
          </p>`
            },
            {
                attachTo: {
                    element: "#datePicker2",
                    on     : "top"
                },
                buttons : [
                    builtInButtons.cancel,
                    builtInButtons.back,
                    builtInButtons.next
                ],
                classes : "custom-class-name-1 custom-class-name-2",
                id      : "step2",
                title   : "Step 2",
                text    : "Hey look we're on to the next step."
            },
            {
                attachTo: {
                    element: "#select",
                    on     : "right"
                },
                buttons : [
                    builtInButtons.cancel,
                    builtInButtons.back,
                    builtInButtons.next
                ],
                classes : "custom-class-name-1 custom-class-name-2",
                id      : "step3",
                title   : "Step 3",
                text    : "Now we have a step off to the right"
            },
            {
                buttons: [
                    builtInButtons.cancel,
                    builtInButtons.back
                ],
                id     : "step4",
                title  : "Centered Modals",
                classes: "custom-class-name-1 custom-class-name-2",
                text   : "If no attachTo is specified, the modal will appear in the center of the screen, as per the Shepherd docs."
            }
        ];

        this.shepherdTest = new Shepherd.Tour({
                                                  defaultStepOptions: {
                                                      classes       : "custom-default-class",
                                                      scrollTo      : true,
                                                      showCancelLink: true,

                                                      tippyOptions: {
                                                          duration: 500
                                                      }
                                                  },
                                                  useModalOverlay   : true,
                                                  confirmCancel     : false,
                                                  styleVariables    : {
                                                      shepherdThemePrimary  : "#ff0000",
                                                      shepherdTextBackground: "#00213b"
                                                  },
                                                  steps             : steps
                                              });
    }

    public launchTour(): void
    {
        this.shepherdTest.start();
    }
}

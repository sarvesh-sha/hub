import {Component} from "@angular/core";
import {AuthorizationDef} from "app/services/domain/auth.guard";
import {TestDescriptor, TestDriver} from "app/test/driver";

@AuthorizationDef({
                      noAuth: true
                  })
@Component({
               template: `
                   <div style="background: white">
                       <div>Testing</div>
                       <div *ngFor="let test of tests">
                           <span>{{test.id}}&nbsp;</span>
                           <span>{{test.name}}</span>
                       </div>
                   </div>
               `
           })
export class TestRootComponent
{
    tests: TestDescriptor[];

    constructor(private m_driver: TestDriver)
    {
    }

    async ngOnInit()
    {
        this.tests = this.m_driver.getTests();
    }
}

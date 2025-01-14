import {enableProdMode} from "@angular/core";
import {platformBrowserDynamic} from "@angular/platform-browser-dynamic";
import {RootModule} from "app/root.module";
import {environment} from "environments/environment";
import "./polyfills.ts";

if (environment.production)
{
    enableProdMode();
}

platformBrowserDynamic()
    .bootstrapModule(RootModule);

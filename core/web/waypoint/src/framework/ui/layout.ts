import {Injectable} from "@angular/core";
import {Subject} from "rxjs";

@Injectable({ providedIn: "root" })
export class CommonLayout
{
    contentSizeChanged = new Subject<void>();
}

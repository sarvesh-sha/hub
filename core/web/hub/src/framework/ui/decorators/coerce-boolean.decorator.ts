import {coerceBooleanProperty} from "@angular/cdk/coercion";
import {Lookup} from "framework/services/utils.service";

const coerce_hook = "$$optio3_CoerceBoolean";

function ensureHook(target: any): Lookup<any>
{
    let cache = target[coerce_hook];
    if (!cache)
    {
        cache = {};

        target[coerce_hook] = cache;
    }

    return cache;
}

export function CoerceBoolean()
{
    return function (target: any,
                     propertyKey: string)
    {
        // property getter
        let getter = function (this: any)
        {
            let hook = ensureHook(this);
            return hook[propertyKey];
        };

        // property setter
        let setter = function (this: any,
                               newVal: any)
        {
            let hook          = ensureHook(this);
            hook[propertyKey] = coerceBooleanProperty(newVal);
        };

        Object.defineProperty(target,
                              propertyKey,
                              {
                                  set         : setter,
                                  get         : getter,
                                  enumerable  : true,
                                  configurable: true
                              });
    };
}

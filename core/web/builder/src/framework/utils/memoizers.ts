import {Lookup} from "framework/services/utils.service";

const memoizer_hook = "$$optio3_memoizer_cache";

/**
 * This decorator caches the return value of a method, and returns the cached value on subsequent calls.
 */
export function Memoizer(target: any,
                         propertyName: string,
                         descriptor: TypedPropertyDescriptor<any>)
{
    let method       = descriptor.value;
    descriptor.value = function (this: Lookup<any>)
    {
        let cache = this[memoizer_hook];
        if (!cache)
        {
            cache = {};

            Object.defineProperty(this, memoizer_hook, {
                configurable: false,
                enumerable  : false,
                writable    : true,
                value       : cache
            });
        }

        let value = cache[propertyName];
        if (value === undefined)
        {
            value = method.apply(this, arguments);

            cache[propertyName] = value;
        }

        return value;
    };
}

/**
 * This decorator flushes the cache of return values.
 */
export function ResetMemoizers(target: any,
                               propertyName: string,
                               descriptor: TypedPropertyDescriptor<any>)
{
    let method       = descriptor.value;
    descriptor.value = function (this: Lookup<any>)
    {
        let cache = this[memoizer_hook];
        if (cache)
        {
            this[memoizer_hook] = {};
        }

        return method.apply(this, arguments);
    };
}

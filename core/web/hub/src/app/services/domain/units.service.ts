import {Injectable} from "@angular/core";
import {DomSanitizer, SafeHtml} from "@angular/platform-browser";

import {ApiService} from "app/services/domain/api.service";
import {UsersService} from "app/services/domain/users.service";

import * as Apis from "app/services/proxy/api/api";
import * as Models from "app/services/proxy/model/models";
import {Lookup} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";
import {Memoizer} from "framework/utils/memoizers";

@Injectable({providedIn: "root"})
export class UnitsService
{
    private static readonly preferenceName: string = "sys_unitsPreference";

    private readonly m_descriptorExtCache: Lookup<EngineeringUnitsDescriptorExtended[]> = {};

    private m_userPreferenceCache: Lookup<Models.EngineeringUnitsPreferencePair>;
    private m_userPreferenceRefresh: any;

    public static readonly noUnitsDisplayName: string = "<no unit>";

    constructor(private api: ApiService,
                private users: UsersService,
                private sanitizer: DomSanitizer)
    {
        this.users.loggedIn.subscribe(() => this.m_userPreferenceCache = null);
        this.users.loggedOut.subscribe(() => this.m_userPreferenceCache = null);
    }

    //--//

    @Memoizer
    private async acquireState(): Promise<State>
    {
        let descriptors = await this.api.units.describe();
        let state       = new State(this.api.units, descriptors);

        for (let desc of state.descriptors)
        {
            desc.controlPointWithHtml.safeLabel = this.getDisplayHtmlStringInner(state, desc.model.factors);
        }

        return state;
    }

    @Memoizer
    public async describeEngineeringUnits(): Promise<EngineeringUnitsDescriptorExtended[]>
    {
        let state = await this.acquireState();
        return state.descriptors;
    }

    @Memoizer
    async getEngineeringUnits(): Promise<ControlOption<Models.EngineeringUnits>[]>
    {
        let types = await this.describeEngineeringUnits();

        return types.map((descExt) => new ControlOption(descExt.model.units, descExt.controlPointWithDescription.label));
    }

    @Memoizer
    public async mapEngineeringUnits(): Promise<Map<Models.EngineeringUnits, EngineeringUnitsDescriptorExtended>>
    {
        let state = await this.acquireState();
        return state.lookupDescription;
    }

    async findPreferred(unitsFactors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnitsFactors>
    {
        let desc = await this.resolveRootDescriptor(unitsFactors);
        if (desc)
        {
            let lookup = await this.ensurePreferences();
            let pair   = lookup[desc.factorsHash];
            if (pair)
            {
                return pair.selected;
            }
        }

        return unitsFactors;
    }

    async setPreferred(factors: Models.EngineeringUnitsFactors)
    {
        let lookup = await this.ensurePreferences();

        let descKey      = await this.resolveRootDescriptor(factors);
        let descSelected = await this.resolveDescriptor(factors, false);
        if (descSelected)
        {
            let pair = Models.EngineeringUnitsPreferencePair.newInstance({
                                                                             key     : descKey.model.factors,
                                                                             selected: descSelected.model.factors
                                                                         });

            let oldPair = lookup[descKey.factorsHash];
            if (oldPair)
            {
                let descSelectedOld = await this.resolveDescriptor(oldPair.selected, false);
                if (descSelected == descSelectedOld)
                {
                    return;
                }
            }

            lookup[descKey.factorsHash] = pair;
        }

        this.refreshPreferences();
    }

    private async ensurePreferences(): Promise<Lookup<Models.EngineeringUnitsPreferencePair>>
    {
        if (!this.m_userPreferenceCache)
        {
            let userPreferenceCache: Lookup<Models.EngineeringUnitsPreferencePair> = {};

            try
            {
                let cfg = await this.users.getTypedPreference<Models.EngineeringUnitsPreference>(null, UnitsService.preferenceName, Models.EngineeringUnitsPreference.fixupPrototype);
                if (cfg)
                {
                    for (let pair of cfg.units)
                    {
                        let desc                              = await this.resolveRootDescriptor(pair.key);
                        userPreferenceCache[desc.factorsHash] = pair;
                    }
                }
            }
            catch (e)
            {
            }

            this.m_userPreferenceCache = userPreferenceCache;
        }

        return this.m_userPreferenceCache;
    }

    private refreshPreferences()
    {
        if (!this.m_userPreferenceRefresh)
        {
            this.m_userPreferenceRefresh = setTimeout(async () =>
                                                      {
                                                          let lookup = this.m_userPreferenceCache;
                                                          if (lookup)
                                                          {
                                                              let units = [];
                                                              for (let hash in lookup)
                                                              {
                                                                  units.push(lookup[hash]);
                                                              }

                                                              let cfg = Models.EngineeringUnitsPreference.newInstance({units: units});
                                                              await this.users.setTypedPreference<Models.EngineeringUnitsPreference>(null, UnitsService.preferenceName, cfg);
                                                          }

                                                          this.m_userPreferenceRefresh = undefined;
                                                      }, 1000);
        }
    }

    //--//

    async getEquivalenceSetFromEnum(unit: Models.EngineeringUnits): Promise<Models.EngineeringUnits[]>
    {
        let state = await this.acquireState();

        let desc = state.getDescriptor(unit);
        return desc ? state.getEquivalenceSetFromFactors(desc.model.factors) : [];
    }

    async getEquivalentUnits(unitsFactors: Models.EngineeringUnitsFactors): Promise<ControlOption<EngineeringUnitsDescriptorExtended>[]>
    {
        let equivalentUnits: ControlOption<EngineeringUnitsDescriptorExtended>[] = [];

        if (unitsFactors)
        {
            let primaryUnits: Models.EngineeringUnits;

            let desc = await this.resolveDescriptor(unitsFactors, false);
            if (desc)
            {
                equivalentUnits.push(desc.controlPointWithDescription);

                primaryUnits = desc.model.units;
            }
            else
            {
                primaryUnits = unitsFactors.primary;
            }

            let equivalenceSet = await this.getEquivalenceSetFromFactors(unitsFactors);
            if (equivalenceSet)
            {
                let units = await this.mapEngineeringUnits();

                for (let equivalentUnit of equivalenceSet)
                {
                    if (equivalentUnit != primaryUnits)
                    {
                        let desc = units.get(equivalentUnit);
                        if (desc) equivalentUnits.push(desc.controlPointWithDescription);
                    }
                }
            }

            if (equivalentUnits.length == 0)
            {
                let desc = await this.resolveDescriptor(unitsFactors, false);
                if (desc)
                {
                    equivalentUnits.push(desc.controlPointWithDescription);
                }
            }
        }

        return equivalentUnits;
    }

    async getEquivalenceSetFromFactors(unitsFactors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnits[]>
    {
        unitsFactors = await this.simplify(unitsFactors);

        let state = await this.acquireState();
        return state.getEquivalenceSetFromFactors(unitsFactors);
    }

    async getDescriptor(unit: Models.EngineeringUnits): Promise<EngineeringUnitsDescriptorExtended>
    {
        if (!unit) return null;

        let state = await this.acquireState();
        return state.getDescriptor(unit);
    }

    async getUnitsDisplay(unitsFactors: Models.EngineeringUnitsFactors): Promise<string>
    {
        let ext = await this.resolveDescriptor(unitsFactors, false);
        return ext && !ext.noDimensions && ext.model.displayName || UnitsService.noUnitsDisplayName;
    }

    async getDimensionlessFlavor(unitsFactors: Models.EngineeringUnitsFactors)
    {
        if (unitsFactors)
        {
            let desc = await this.resolveDescriptor(unitsFactors, false);
            return desc?.model.description;
        }

        return null;
    }

    async resolveRootDescriptor(factors: Models.EngineeringUnitsFactors): Promise<EngineeringUnitsDescriptorExtended>
    {
        if (!factors) return null;

        factors         = await this.simplify(factors);
        factors.scaling = Models.EngineeringUnitsFactorsScaling.newInstance({
                                                                                multiplier: 1,
                                                                                offset    : 0
                                                                            });

        return this.resolveDescriptor(factors, false);
    }

    async resolveRootFactors(factors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnitsFactors>
    {
        const descriptor = await this.resolveRootDescriptor(factors);
        return EngineeringUnitsDescriptorExtended.extractFactors(descriptor);
    }

    async resolveDescriptor(factors: Models.EngineeringUnitsFactors,
                            simplify: boolean): Promise<EngineeringUnitsDescriptorExtended>
    {
        if (!factors) return null;

        if (simplify)
        {
            factors = await this.simplify(factors);
        }

        let ext = await this.getDescriptorFromFactors(factors);
        if (!ext)
        {
            let hash         = UnitsService.computeHash(factors, true);
            let cachedByHash = this.m_descriptorExtCache[hash];
            if (!cachedByHash)
            {
                cachedByHash                    = [];
                this.m_descriptorExtCache[hash] = cachedByHash;
            }

            for (let cachedExt of cachedByHash)
            {
                if (cachedExt.sameFactors(factors))
                {
                    if (hash == null && factors.primary != cachedExt.model.factors.primary) continue;

                    return cachedExt;
                }
            }

            let model         = new Models.EngineeringUnitsDescriptor();
            model.factors     = factors;
            model.displayName = await this.getDisplayNameFromFactors(factors);

            ext = new EngineeringUnitsDescriptorExtended(model);
            cachedByHash.push(ext);

            if (!simplify)
            {
                // To get the proper noDimensions value, resolve again with simplification.
                let ext2         = await this.resolveDescriptor(ext.model.factors, true);
                ext.noDimensions = ext2.noDimensions;
            }
        }

        return ext;
    }

    async getDescriptorFromFactors(factors: Models.EngineeringUnitsFactors): Promise<EngineeringUnitsDescriptorExtended>
    {
        if (!factors) return null;

        let state             = await this.acquireState();
        let factorsSimplified = await state.simplify(factors);

        let set = state.getEquivalenceSetFromFactors(factorsSimplified);

        //
        // First try an exact matching.
        //
        for (let unit of set)
        {
            if (factors.primary == unit)
            {
                let desc = state.getDescriptor(unit);
                if (desc.sameFactors(factors))
                {
                    return desc;
                }
            }
        }

        //
        // Then try an equivalence matching.
        //
        for (let unit of set)
        {
            let desc = state.getDescriptor(unit);
            if (desc.sameFactors(factors))
            {
                return desc;
            }
        }

        //
        // Then look for something with the same scaling values.
        //
        for (let unit of set)
        {
            let desc = state.getDescriptor(unit);
            if (desc.sameScaling(factorsSimplified))
            {
                return desc;
            }
        }

        //
        // If all else fails, pick the first one.
        //
        for (let unit of set)
        {
            return state.getDescriptor(unit);
        }

        return null;
    }

    async getDisplayNameFromFactors(unitsFactors: Models.EngineeringUnitsFactors): Promise<string>
    {
        let primary = unitsFactors?.primary;

        unitsFactors = await this.compact(unitsFactors);
        if (!unitsFactors) return UnitsService.noUnitsDisplayName;

        let numText = null;
        let denText = null;

        for (let num of unitsFactors.numeratorUnits)
        {
            let text = await this.getDisplayNameFromEnum(num);

            numText = numText ? `${numText} * ${text}` : text;
        }

        for (let den of unitsFactors.denominatorUnits)
        {
            let text = await this.getDisplayNameFromEnum(den);

            denText = denText ? `${denText} * ${text}` : text;
        }

        if (numText)
        {
            return denText ? `${numText} / ${denText}` : numText;
        }
        else if (denText)
        {
            return `1 / ${denText}`;
        }

        if (primary)
        {
            let text = await this.getDisplayNameFromEnum(primary);
            return text;
        }

        return UnitsService.noUnitsDisplayName;
    }

    async getDisplayNameFromEnum(units: Models.EngineeringUnits): Promise<string>
    {
        let desc = await this.getDescriptor(units);
        if (!desc) return null;

        return desc.model.displayName;
    }

    async simplify(unitsFactors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnitsFactors>
    {
        let state = await this.acquireState();
        return state.simplify(unitsFactors);
    }

    async compact(unitsFactors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnitsFactors>
    {
        let state = await this.acquireState();
        return state.compact(unitsFactors);
    }

    async convert(value: number,
                  fromUnits: Models.EngineeringUnitsFactors,
                  toUnits: Models.EngineeringUnitsFactors): Promise<number>
    {
        let state = await this.acquireState();
        return state.convert(value, fromUnits, toUnits);
    }

    async getDisplayHtmlString(unitsFactors: Models.EngineeringUnitsFactors): Promise<SafeHtml>
    {
        if (!unitsFactors) return null;

        let state = await this.acquireState();

        return this.getDisplayHtmlStringInner(state, unitsFactors);
    }

    private getDisplayHtmlStringInner(state: State,
                                      unitsFactors: Models.EngineeringUnitsFactors): SafeHtml
    {
        let numParts = state.getCompositeName(unitsFactors.numeratorUnits);
        let denParts = state.getCompositeName(unitsFactors.denominatorUnits);

        let separator = " &#x25CF; ";
        let numText   = numParts.join(separator);
        let denText   = denParts.join(separator);

        let result: string = "&lt;no unit&gt;";
        if (numText)
        {
            result = denText ? `${numText} / ${denText}` : numText;
        }
        else if (denText)
        {
            result = `1 / ${denText}`;
        }

        return this.sanitizer.bypassSecurityTrustHtml(result);
    }

    public static computeHash(unitsFactors: Models.EngineeringUnitsFactors,
                              includeScaling: boolean): string
    {
        if (!unitsFactors)
        {
            return null;
        }

        let num = unitsFactors.numeratorUnits || [];
        let den = unitsFactors.denominatorUnits || [];

        if (num.length == 0 && den.length == 0)
        {
            return null;
        }

        let hash = num.join(",") + "/" + den.join(",");

        if (includeScaling && unitsFactors.scaling)
        {
            hash = `${hash} # ${unitsFactors.scaling.multiplier} # ${unitsFactors.scaling.offset}`;
        }

        return hash;
    }

    public static areIdentical(a: Models.EngineeringUnitsFactors,
                               b: Models.EngineeringUnitsFactors): boolean
    {
        return this.areEquivalent(a, b, true) && a?.primary == b?.primary;
    }

    public static areEquivalent(a: Models.EngineeringUnitsFactors,
                                b: Models.EngineeringUnitsFactors,
                                includeScaling: boolean = false): boolean
    {
        let hashA = this.computeHash(a, includeScaling);
        let hashB = this.computeHash(b, includeScaling);

        return hashA == hashB;
    }

    public static matchingScaling(a: Models.EngineeringUnitsFactors,
                                  b: Models.EngineeringUnitsFactors): boolean
    {
        if (a === b) return true;
        if (!a?.scaling || !b?.scaling) return false;

        return a.scaling.multiplier == b.scaling.multiplier && a.scaling.offset == b.scaling.offset;
    }
}

class State
{
    readonly descriptors: EngineeringUnitsDescriptorExtended[] = [];
    readonly lookupDescription                                 = new Map<Models.EngineeringUnits, EngineeringUnitsDescriptorExtended>();

    private readonly m_equivalenceCache: Lookup<Models.EngineeringUnits[]>              = {};
    private readonly m_simplifiedCache: Lookup<Promise<Models.EngineeringUnitsFactors>> = {};
    private readonly m_compactedCache: Lookup<Promise<Models.EngineeringUnitsFactors>>  = {};

    constructor(private units: Apis.UnitsApi,
                descriptors: Models.EngineeringUnitsDescriptor[])
    {
        for (let desc of descriptors)
        {
            let descExt = new EngineeringUnitsDescriptorExtended(desc);
            this.descriptors.push(descExt);
            this.lookupDescription.set(desc.units, descExt);

            if (descExt.factorsHash)
            {
                let equivalence = this.m_equivalenceCache[descExt.factorsHash];
                if (!equivalence)
                {
                    equivalence = [];

                    this.m_equivalenceCache[descExt.factorsHash] = equivalence;
                }

                equivalence.push(desc.units);
            }
        }
    }

    async simplify(unitsFactors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnitsFactors>
    {
        let key = UnitsService.computeHash(unitsFactors, true);
        if (!key) return unitsFactors;

        let unitsFactorsSimplifiedPromise = this.m_simplifiedCache[key];
        if (!unitsFactorsSimplifiedPromise)
        {
            unitsFactorsSimplifiedPromise = this.units.simplify(unitsFactors);
            this.m_simplifiedCache[key]   = unitsFactorsSimplifiedPromise;
        }

        let units = await unitsFactorsSimplifiedPromise;

        return Models.EngineeringUnitsFactors.deepClone(units);
    }

    async compact(unitsFactors: Models.EngineeringUnitsFactors): Promise<Models.EngineeringUnitsFactors>
    {
        let key = UnitsService.computeHash(unitsFactors, true);
        if (!key) return unitsFactors;

        let unitsFactorsCompactedPromise = this.m_compactedCache[key];
        if (!unitsFactorsCompactedPromise)
        {
            unitsFactorsCompactedPromise = this.units.compact(unitsFactors);
            this.m_compactedCache[key]   = unitsFactorsCompactedPromise;
        }

        let units = await unitsFactorsCompactedPromise;

        return Models.EngineeringUnitsFactors.deepClone(units);
    }

    async convert(value: number,
                  fromUnits: Models.EngineeringUnitsFactors,
                  toUnits: Models.EngineeringUnitsFactors): Promise<number>
    {
        let fromKey = UnitsService.computeHash(fromUnits, true);
        let toKey   = UnitsService.computeHash(toUnits, true);
        if (!fromKey || !toKey) return undefined;

        let conversionResponse = await this.units.convert(Models.EngineeringUnitsConversionRequest.newInstance(
            {
                value      : value,
                convertFrom: fromUnits,
                convertTo  : toUnits
            }
        ));

        return conversionResponse.value;
    }

    getCompositeName(units: Models.EngineeringUnits[]): string[]
    {
        let components: string[]    = [];
        let unitsCt: Lookup<number> = {};

        for (let unit of units)
        {
            unitsCt[unit] = unitsCt[unit] ? unitsCt[unit] + 1 : 1;
        }

        for (let unit in unitsCt)
        {
            let text  = this.getDisplayNameFromEnum(<any>unit);
            let count = unitsCt[unit];
            if (count > 1)
            {
                components.push(`${text}<sup>${count}</sup>`);
            }
            else
            {
                components.push(text);
            }
        }

        return components;
    }

    getDescriptor(units: Models.EngineeringUnits): EngineeringUnitsDescriptorExtended
    {
        return units ? this.lookupDescription.get(units) : null;
    }

    getDisplayNameFromEnum(units: Models.EngineeringUnits): string
    {
        let desc = this.lookupDescription.get(units);
        if (!desc) return null;

        return desc.model.displayName;
    }

    getEquivalenceSetFromFactors(unitsFactors: Models.EngineeringUnitsFactors): Models.EngineeringUnits[]
    {
        let key = UnitsService.computeHash(unitsFactors, false);
        return key ? (this.m_equivalenceCache[key] || []) : [];
    }
}

export class EngineeringUnitsDescriptorExtended
{
    private static s_counter: number = 0;

    public readonly counter = EngineeringUnitsDescriptorExtended.s_counter++;
    public readonly rawFactors: Models.EngineeringUnitsFactors;
    public readonly factorsHash: string;
    public readonly factorsHashWithScaling: string;
    public readonly rawFactorsHashWithScaling: string;
    public readonly controlPointWithHtml: ControlOption<EngineeringUnitsDescriptorExtended>;
    public readonly controlPointWithDescription: ControlOption<EngineeringUnitsDescriptorExtended>;

    public noDimensions: boolean;

    constructor(public readonly model: Models.EngineeringUnitsDescriptor)
    {
        this.controlPointWithHtml        = new ControlOption(this);
        this.controlPointWithDescription = new ControlOption(this, model.description ? `${model.displayName} - ${model.description}` : model.displayName);

        this.factorsHash            = UnitsService.computeHash(model.factors, false);
        this.factorsHashWithScaling = UnitsService.computeHash(model.factors, true);

        this.noDimensions = !this.factorsHash;

        if (model.units)
        {
            let scaling = Models.EngineeringUnitsFactorsScaling.newInstance({
                                                                                multiplier: 1,
                                                                                offset    : 0
                                                                            });

            this.rawFactors = Models.EngineeringUnitsFactors.newInstance({
                                                                             scaling         : scaling,
                                                                             numeratorUnits  : [model.units],
                                                                             denominatorUnits: [],
                                                                             primary         : model.units
                                                                         });

            this.rawFactorsHashWithScaling = UnitsService.computeHash(this.rawFactors, true);
        }
    }

    public static areEquivalent(a: EngineeringUnitsDescriptorExtended,
                                b: EngineeringUnitsDescriptorExtended): boolean
    {
        let hashA = a ? a.factorsHash : null;
        let hashB = b ? b.factorsHash : null;

        return hashA == hashB;
    }

    public static extractFactors(ext: EngineeringUnitsDescriptorExtended): Models.EngineeringUnitsFactors
    {
        return ext ? ext.model.factors : null;
    }

    generateLabel(label?: string)
    {
        if (!this.model) return label || "";
        if (this.noDimensions) return label;

        if (label)
        {
            if (this.model.units == Models.EngineeringUnits.enumerated) return label;

            return `${label} ( ${this.model.displayName} )`;
        }

        if (this.model.description)
        {
            return `${this.model.displayName} (${this.model.description})`;
        }

        return this.model.displayName;
    }

    sameFactors(factors: Models.EngineeringUnitsFactors): boolean
    {
        let hash = UnitsService.computeHash(factors, true);

        return this.rawFactorsHashWithScaling == hash || this.factorsHashWithScaling == hash;
    }

    sameScaling(factors: Models.EngineeringUnitsFactors): boolean
    {
        return UnitsService.matchingScaling(this.model.factors, factors);
    }
}

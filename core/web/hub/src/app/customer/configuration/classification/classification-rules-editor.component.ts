import {ChangeDetectionStrategy, Component, Input, Optional} from "@angular/core";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import * as Models from "app/services/proxy/model/models";
import {ProviderForMap, ProviderForString} from "app/shared/tables/provider-for-map";

@Component({
               selector       : "o3-classification-rules-editor",
               templateUrl    : "./classification-rules-editor.component.html",
               styleUrls      : ["./classification-detail-page.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationRulesEditorComponent
{
    knownTerms: ProviderForKnownTerm;
    abbreviations: ProviderForStringArray;
    disambiguations: ProviderForString;
    startsWith: ProviderForString;
    endsWith: ProviderForString;
    contains: ProviderForString;

    private m_rules: Models.NormalizationRules;

    @Input()
    public set rules(rules: Models.NormalizationRules)
    {
        this.m_rules = rules;
        if (this.m_rules)
        {
            this.bind();
        }
    }

    public get rules(): Models.NormalizationRules
    {
        return this.m_rules;
    }

    constructor(@Optional() public host: ClassificationDetailPageComponent)
    {
        this.knownTerms = new ProviderForKnownTerm(host, "knownTerms", "Scoring Term(s)", "Term", "Details");

        this.abbreviations = new ProviderForStringArray(host, "abbreviations", "Abbreviation(s)", "Full Text", "Abbreviations");

        this.disambiguations = new ProviderForString(host, "disambiguations", "Disambiguation(s)", "Ambiguous Term", "Plain Term");

        this.startsWith = new ProviderForString(host, "startsWith", "Start(s)", "Matching Text", "Plain Term");

        this.endsWith = new ProviderForString(host, "endsWith", "End(s)", "Matching Text", "Plain Term");

        this.contains = new ProviderForString(host, "contains", "Contain(s)", "Matching Text", "Plain Term");
    }

    bind()
    {
        this.knownTerms.bind(this.rules.knownTerms);

        this.abbreviations.bind(this.rules.abbreviations);

        this.disambiguations.bind(this.rules.disambiguations);

        this.startsWith.bind(this.rules.startsWith);

        this.endsWith.bind(this.rules.endsWith);

        this.contains.bind(this.rules.contains);
    }

    exportAbbreviations()
    {
        this.host.exportSection("Abbreviation Export", "Abbreviation", this.rules.abbreviations);
    }

    async importAbbreviations()
    {
        return this.host.importSection("Abbreviation Import", (contents) =>
        {
            let rules           = this.rules;
            rules.abbreviations = JSON.parse(contents);
            return JSON.stringify(rules);
        });
    }

    exportTermScoring()
    {
        this.host.exportSection("Term Scoring Export", "TermScoring", this.rules.knownTerms);
    }

    async importTermScoring()
    {
        return this.host.importSection("Term Scoring Import", (contents) =>
        {
            let rules        = this.rules;
            rules.knownTerms = JSON.parse(contents);
            return JSON.stringify(rules);
        });
    }

    exportDisambiguations()
    {
        this.host.exportSection("Disambiguations Export", "Disambiguations", this.rules.disambiguations);
    }

    async importDisambiguations()
    {
        return this.host.importSection("Disambiguations Import", (contents) =>
        {
            let rules             = this.rules;
            rules.disambiguations = JSON.parse(contents);
            return JSON.stringify(rules);
        });
    }

    exportStartsWith()
    {
        this.host.exportSection("Starts With Export", "StartsWith", this.rules.startsWith);
    }

    async importStartsWith()
    {
        return this.host.importSection("Starts With Import", (contents) =>
        {
            let rules        = this.rules;
            rules.startsWith = JSON.parse(contents);
            return JSON.stringify(rules);
        });
    }

    exportEndsWith()
    {
        this.host.exportSection("Ends With Export", "EndsWith", this.rules.endsWith);
    }

    async importEndsWith()
    {
        return this.host.importSection("Ends With Import", (contents) =>
        {
            let rules      = this.rules;
            rules.endsWith = JSON.parse(contents);
            return JSON.stringify(rules);
        });
    }

    exportContains()
    {
        this.host.exportSection("Contains Export", "Contains", this.rules.knownTerms);
    }

    async importContains()
    {
        return this.host.importSection("Contains Import", (contents) =>
        {
            let rules      = this.rules;
            rules.contains = JSON.parse(contents);
            return JSON.stringify(rules);
        });
    }

}


class ProviderForKnownTerm extends ProviderForMap<Models.NormalizationRulesKnownTerm>
{
    protected getText(v: Models.NormalizationRulesKnownTerm): string
    {
        if (!v) return "";

        let res = [];

        if (v.acronym)
        {
            res.push(`Acronym: ${v.acronym}`);
        }

        if (v.synonyms && v.synonyms.length > 0)
        {
            res.push(`Synonyms: ${v.synonyms.join(", ")}`);
        }

        if (v.positiveWeight)
        {
            res.push(`Positive Weight: ${v.positiveWeight}`);
        }
        else
        {
            res.push(`Positive Weight: 1 (default)`);
        }

        if (v.negativeWeight)
        {
            res.push(`Negative Weight: ${v.negativeWeight}`);
        }

        if (v.weightReason)
        {
            res.push(`Weight Reason: ${v.weightReason}`);
        }

        return res.join(" | ");
    }

    protected shouldInclude(term: Models.NormalizationRulesKnownTerm,
                            filterLowercase: string): boolean
    {
        if (term.acronym)
        {
            if (this.contains(term.acronym, filterLowercase)) return true;
        }

        for (let synonym of term.synonyms || [])
        {
            if (this.contains(synonym, filterLowercase)) return true;
        }

        return false;
    }


    protected allocate(): Models.NormalizationRulesKnownTerm
    {
        return Models.NormalizationRulesKnownTerm.newInstance({synonyms: []});
    }
}

class ProviderForStringArray extends ProviderForMap<string[]>
{
    protected getText(v: string[]): string
    {
        if (!v) return "";

        return v.join(" | ");
    }

    protected shouldInclude(data: string[],
                            filterLowercase: string): boolean
    {
        for (let text of data || [])
        {
            if (this.contains(text, filterLowercase)) return true;
        }

        return false;
    }

    protected allocate(): string[]
    {
        return [];
    }
}

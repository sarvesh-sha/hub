import * as SharedSvc from "app/services/domain/base.service";
import {StateHistory, StateRestorable, StateSnapshot} from "app/shared/undo/undo-redo-state";
import * as Models from "app/services/proxy/model/models";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

export class VersionManager<M extends VersionModel, E extends SharedSvc.ExtendedModel<M>, S extends VersionService<E>, V>
{
    private static readonly draftIdPrefix = "draft__";

    private m_drafting: boolean = false;
    get drafting(): boolean
    {
        return this.m_drafting;
    }

    private m_baseVersion: E;
    get baseVersion(): E
    {
        return this.m_baseVersion;
    }

    private m_stateHistory: StateHistory<V>;
    get stateHistory(): StateHistory<V>
    {
        return this.m_stateHistory;
    }

    private m_state: VersionManagerState<M, E, V>;

    get options(): ControlOption<string>[]
    {
        return this.m_state?.options;
    }

    get activeVersionId(): string
    {
        if (!this.m_baseVersion) return null;

        let id = this.m_baseVersion.model.sysId;
        if (this.m_drafting) id = VersionManager.draftIdPrefix + id;

        return id;
    }

    constructor(private svc: S) {}

    public async initialize()
    {
        let versions = await this.svc.getAllVersions();

        let idToVersion: Lookup<E> = {};
        let options                = new Array(versions.length);
        let prevVersion            = null;
        for (let i = versions.length - 1; i >= 0; i--)
        {
            let version                      = versions[i];
            idToVersion[version.model.sysId] = version;
            options[i]                       = this.newVersionOption(version, prevVersion, idToVersion);
            prevVersion                      = version;
        }

        let idToSuccessor: Lookup<E> = {};
        for (let version of versions)
        {
            let predId = version.model.predecessor?.sysId;
            if (predId)
            {
                let successor = idToSuccessor[predId];
                if (!successor || MomentHelper.compareDates(successor.model.createdOn, version.model.createdOn) < 0)
                {
                    idToSuccessor[predId] = version;
                }
            }
        }

        this.m_state = new VersionManagerState(idToVersion, idToSuccessor, options);
    }

    public updateVersionInfo(fromVersionLoad: boolean = false)
    {
        let state = this.m_state;
        if (!state) return;

        this.m_drafting = this.m_stateHistory.onHistory();

        let baseModel      = this.m_baseVersion.model;
        let options        = state.options;
        let firstNonDraft  = options.findIndex((option) => !option.id.startsWith(VersionManager.draftIdPrefix));
        let draftOptions   = options.slice(0, firstNonDraft === -1 ? 0 : firstNonDraft);
        let draftOptionIdx = draftOptions.findIndex((option) => VersionManager.getBaseSysId(option.id) == baseModel.sysId);
        if (this.m_drafting)
        {
            if (draftOptionIdx < 0)
            {
                let draftOption = new ControlOption(VersionManager.draftIdPrefix + baseModel.sysId, "Draft - from v" + baseModel.version);
                let optionIdx   = draftOptions.findIndex((option) => baseModel.version > state.idToVersion[VersionManager.getBaseSysId(option.id)]?.model.version);
                options.splice(optionIdx === -1 ? draftOptions.length : optionIdx, 0, draftOption);
                state.optionsUpdated();
            }
        }
        else if (!fromVersionLoad && draftOptionIdx >= 0)
        {
            options.splice(draftOptionIdx, 1);
            state.optionsUpdated();
        }
    }

    public clearCurrentDraft()
    {
        let state = this.m_state;
        if (!state) return;

        let activeVersionId = this.activeVersionId;
        let options         = state.options;
        let draftIdx        = options.findIndex((option) => option.id == activeVersionId);
        if (draftIdx >= 0)
        {
            let baseId = VersionManager.getBaseSysId(activeVersionId);
            delete state.idToDraft[baseId];

            options.splice(draftIdx, 1);
            state.optionsUpdated();

            this.m_drafting = false;
        }
    }

    public getVersion(sysId: string): E
    {
        return this.m_state?.idToVersion[sysId];
    }

    private getDraft(draftSysId: string): VersionDraft<V>
    {
        if (draftSysId.startsWith(VersionManager.draftIdPrefix))
        {
            return this.m_state?.idToDraft[VersionManager.getBaseSysId(draftSysId)];
        }

        return null;
    }

    public getPredecessors(): E[]
    {
        let state = this.m_state;
        if (!state) return null;

        let predId       = this.m_baseVersion.model.predecessor?.sysId;
        let predecessors = [];
        while (predId)
        {
            let pred = state.idToVersion[predId];
            if (!pred) break;

            predecessors.push(pred);
            predId = pred.model.predecessor?.sysId;
        }

        return predecessors.reverse();
    }

    public getSuccessors(): E[]
    {
        let state = this.m_state;
        if (!state) return null;

        let successors = [];
        let successor  = state.idToSuccessor[this.m_baseVersion.model.sysId];
        while (successor)
        {
            successors.push(successor);
            successor = state.idToSuccessor[successor.model.sysId];
        }

        return successors;
    }

    public async updateBaseVersion(versionId: Models.RecordIdentity)
    {
        let state = this.m_state;
        let sysId = versionId?.sysId;
        if (!state || !sysId) return;

        if (this.m_baseVersion?.model.sysId !== sysId)
        {
            this.m_baseVersion = state.idToVersion[sysId];
            if (!this.m_baseVersion)
            {
                // new head version: update version info
                let baseVersion = await this.svc.getVersion(versionId);
                if (!baseVersion) return;

                this.m_baseVersion = baseVersion;

                state.idToVersion[sysId] = baseVersion;
                let predecessorId        = baseVersion.model.predecessor?.sysId;
                if (predecessorId) state.idToSuccessor[predecessorId] = baseVersion;

                let options       = state.options;
                let nonDraftIdx   = options.findIndex((option) => !option.id.startsWith(VersionManager.draftIdPrefix));
                let prevVersion   = state.idToVersion[options[nonDraftIdx]?.id];
                let versionOption = this.newVersionOption(this.m_baseVersion, prevVersion, state.idToVersion);
                options.splice(nonDraftIdx, 0, versionOption);
            }
        }
    }

    public async updateStateHistory(component: StateRestorable<V>,
                                    baseState?: StateSnapshot<V>,
                                    history?: StateSnapshot<V>[],
                                    pointer?: number,
                                    predecessors?: StateSnapshot<V>[],
                                    successors?: StateSnapshot<V>[])
    {
        if (!this.m_state) await this.initialize();

        this.m_stateHistory = new StateHistory(component, baseState, history, pointer, predecessors, successors);
    }

    private newVersionOption(version: E,
                             prevVersion: E,
                             idToVersion: Lookup<E>): ControlOption<string>
    {
        let predecessor = idToVersion[version.model.predecessor?.sysId];

        let label = "v" + version.model.version;
        if (prevVersion && predecessor && predecessor !== prevVersion) label += ` - from v${predecessor.model.version}`;

        return new ControlOption(version.model.sysId, label);
    }

    public async recordStateChange(changeDescription: string)
    {
        if (!this.m_stateHistory) return;

        await this.m_stateHistory.record(changeDescription);

        this.updateVersionInfo();
    }

    public saveDraft(baseState: StateSnapshot<V>)
    {
        if (!this.m_stateHistory) return;

        if (this.m_drafting)
        {
            let historyState = this.m_stateHistory.clear();

            this.m_state.idToDraft[this.m_baseVersion.model.sysId] = {
                baseState: baseState,
                history  : historyState.history,
                pointer  : historyState.pointer
            };
        }
    }

    public getVersionInfo(versionId: string): VersionStateInfo<E, V>
    {
        let state = this.m_state;
        if (!state) return null;

        let baseId = VersionManager.getBaseSysId(versionId);
        return {
            version: state.idToVersion[baseId],
            draft  : this.getDraft(versionId)
        };
    }

    private static getBaseSysId(versionId: string): string
    {
        if (versionId.startsWith(VersionManager.draftIdPrefix)) return versionId.substring(VersionManager.draftIdPrefix.length);
        return versionId;
    }
}

interface VersionModel
{
    sysId: string;
    version: number;
    predecessor: Models.RecordIdentity;
    createdOn: Date;
}

class VersionManagerState<M extends VersionModel, E extends SharedSvc.ExtendedModel<M>, V>
{
    public readonly idToDraft: Lookup<VersionDraft<V>> = {};

    constructor(public readonly idToVersion: Lookup<E>,
                public readonly idToSuccessor: Lookup<E>,
                public options: ControlOption<string>[])
    {
    }

    public optionsUpdated()
    {
        this.options = UtilsService.arrayCopy(this.options);
    }
}

export interface VersionService<E>
{
    getAllVersions(): Promise<E[]>;

    getVersion(id: Models.RecordIdentity): Promise<E>;
}

export type VersionDraft<V> = { baseState: StateSnapshot<V>, history: StateSnapshot<V>[], pointer: number };
type VersionStateInfo<E, V> = { version: E, draft: VersionDraft<V> };

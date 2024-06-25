import * as Models from "app/services/proxy/model/models";
import {Block} from "framework/ui/blockly/block";
import {BlocklyWorkspaceComponent} from "framework/ui/blockly/blockly-workspace.component";

export class WorkspaceTab
{
    private m_blocks: Map<string, Models.EngineBlock>        = new Map();
    private m_blockChains: Map<string, Models.EngineBlock[]> = new Map();
    private m_blockParents: Map<string, Models.EngineBlock>  = new Map();

    constructor(private m_model: Models.EngineTab)
    {
        this.initializeFromModel();
    }

    public get name(): string
    {
        return this.m_model.name;
    }

    public set name(name: string)
    {
        this.m_model.name = name;
    }

    public isEmpty()
    {
        return this.m_model.blockChains.length === 0;
    }

    public destroy()
    {
        this.m_blocks       = null;
        this.m_blockChains  = null;
        this.m_blockParents = null;
    }

    public addBlocksToWorkspace(workspace: BlocklyWorkspaceComponent)
    {
        for (let blockChain of this.m_model.blockChains)
        {
            if (blockChain)
            {
                let blocks = [];
                for (let block of blockChain)
                {
                    if (block)
                    {
                        blocks.push(Block.fromModel(workspace.workspace, block));
                    }
                }

                Block.chain(blocks);
            }
        }
    }

    public collectBlocksFromWorkspace(workspace: BlocklyWorkspaceComponent)
    {
        let blockChains = [];

        let topBlocks = workspace.getTopBlocks();

        for (let topBlock of topBlocks)
        {
            let model = topBlock.toModel();

            let blockChain = [model];

            let next = Block.cast(topBlock.getNextBlock());
            while (next)
            {
                blockChain.push(next.toModel());
                next = Block.cast(next.getNextBlock());
            }

            blockChains.push(blockChain);
        }

        this.m_model.blockChains = blockChains;
        this.m_blocks.clear();
        this.m_blockChains.clear();
        this.m_blockParents.clear();
        this.initializeFromModel();
    }

    public addBlock(block: Models.EngineBlock)
    {
        let chain = [block];
        this.m_model.blockChains.push(chain);

        // Load block and any child blocks
        this.loadBlock(block, chain);
        this.initializeFromBlock(block);
    }

    public updateBlock(id: string,
                       block: Models.EngineBlock)
    {
        let existingModel: any = this.getBlock(id);
        let updatedModel: any  = block;

        // Remove existing properties
        for (let key of Object.keys(existingModel))
        {
            existingModel[key] = undefined;
        }

        // Add new properties
        for (let key of Object.keys(block))
        {
            existingModel[key] = updatedModel[key];
        }

        this.initializeFromBlock(existingModel);
    }

    public deleteBlock(id: string,
                       deletedIds: string[])
    {
        let chain = this.m_blockChains.get(id);
        this.removeBlockChain(chain);

        for (let id of deletedIds)
        {
            this.m_blocks.delete(id);
            this.m_blockChains.delete(id);
            this.m_blockParents.delete(id);
        }
    }

    public getBlock(id: string)
    {
        return this.m_blocks.get(id);
    }

    public getDescendants(id: string): Models.EngineBlock[]
    {
        let blocks: Models.EngineBlock[] = [];
        let root                         = this.getBlock(id);
        this.streamBlockProperties(root,
                                   [],
                                   (block,
                                    container) =>
                                   {
                                       if (block instanceof Models.EngineBlock && block !== root)
                                       {
                                           blocks.push(block);
                                       }
                                   });
        return blocks;
    }

    public connectBlock(id: string,
                        parentBlock: Block<any>,
                        newInputName: string)
    {
        let parentModel      = this.getBlock(parentBlock.id);
        let blockChain       = this.getBlockChain(id);
        let parentBlockChain = this.getBlockChain(parentBlock.id);

        if (newInputName)
        {
            // Connected directly to a block
            this.m_blockChains.delete(id);
            let newParentModel = parentBlock.toModel();
            this.updateModelInPlace(parentModel, newParentModel);
            this.initializeFromBlock(parentModel);
        }
        else if (blockChain && parentBlockChain)
        {
            // Inserting into a chain
            let parentIndex = parentBlockChain.indexOf(parentModel);
            parentBlockChain.splice(parentIndex + 1, 0, ...blockChain);
            for (let block of blockChain)
            {
                this.m_blockChains.set(block.id, parentBlockChain);
            }
        }

        // Remove the old chain
        this.removeBlockChain(blockChain);
    }

    public disconnectBlock(id: string)
    {
        let model       = this.getBlock(id);
        let blockChain  = this.getBlockChain(id);
        let blockParent = this.getBlockParent(id);

        let newChain: Models.EngineBlock[];

        if (blockChain)
        {
            // Part of a chain, remove block and any connected blocks from the chain
            let modelIndex    = blockChain.indexOf(model);
            newChain          = blockChain.slice(modelIndex);
            blockChain.length = modelIndex;
            for (let block of newChain)
            {
                if (block)
                {
                    this.m_blockChains.set(block.id, newChain);
                }
            }
        }
        else if (blockParent)
        {
            // Block is directly referenced, remove the reference
            for (let key in blockParent)
            {
                if (blockParent.hasOwnProperty(key) && (<any>blockParent)[key] === model)
                {
                    (<any>blockParent)[key] = null;
                    newChain                = [model];
                    this.m_blockChains.set(id, newChain);
                    this.m_blockParents.delete(id);
                    break;
                }
            }
        }

        this.m_model.blockChains.push(newChain);
    }

    public enumerateBlocks(cb: (block: Models.EngineBlock) => void)
    {
        for (let block of this.m_blocks.values())
        {
            cb(block);
        }
    }

    public renameVariable(oldName: string,
                          newName: string)
    {
        this.streamAllNestedObjects((block) =>
                                    {
                                        if (block instanceof Models.EngineVariable || block instanceof Models.EngineVariableReference)
                                        {
                                            if (block.name === oldName)
                                            {
                                                block.name = newName;
                                            }
                                        }
                                    });
    }

    private getBlockParent(id: string)
    {
        return this.m_blockParents.get(id);
    }

    private getBlockChain(id: string)
    {
        return this.m_blockChains.get(id);
    }

    private removeBlockChain(blockChain: Models.EngineBlock[]): void
    {
        this.m_model.blockChains.splice(this.m_model.blockChains.indexOf(blockChain), 1);
    }

    private initializeFromBlock(block: Models.EngineBlock)
    {
        let container = this.getBlockChain(block.id) || this.getBlockParent(block.id);
        this.streamBlockProperties(block,
                                   container,
                                   (val,
                                    container) =>
                                       this.loadBlock(val, container));
    }

    private initializeFromModel()
    {
        this.streamAllNestedObjects((value,
                                     container) =>
                                        this.loadBlock(value, container));
    }

    private loadBlock(block: any,
                      container: any)
    {
        if (block instanceof Models.EngineBlock)
        {
            this.m_blocks.set(block.id, block);
            if (Array.isArray(container))
            {
                this.m_blockChains.set(block.id, container);
            }
            else
            {
                this.m_blockParents.set(block.id, container);
            }
        }
    }

    private streamAllNestedObjects(cb: (value: any,
                                        container: any) => void)
    {
        for (let chain of (this.m_model.blockChains || []))
        {
            for (let block of (chain || []))
            {
                this.streamBlockProperties(block,
                                           chain,
                                           (val,
                                            container) => cb(val, container));
            }
        }
    }

    private streamBlockProperties(block: Models.EngineBlock,
                                  container: any,
                                  cb: (value: any,
                                       container: any) => void)
    {
        if (!block)
        {
            return;
        }

        cb(block, container);
        for (let property in block)
        {
            if (block.hasOwnProperty(property))
            {
                let value = (<any>block)[property];
                if (Array.isArray(value))
                {
                    for (let subValue of value)
                    {
                        this.streamBlockProperties(subValue, value, cb);
                    }
                }
                else if (value && typeof value === "object")
                {
                    this.streamBlockProperties(value, block, cb);
                }
            }
        }
    }

    private updateModelInPlace<T>(model: T,
                                  newModel: T)
    {
        // Delete any props that have gone away
        for (let prop in model)
        {
            if (model.hasOwnProperty(prop) && !newModel.hasOwnProperty(prop))
            {
                delete model[prop];
            }
        }

        // Assign all props from the new model
        Object.assign(model, newModel);
    }
}

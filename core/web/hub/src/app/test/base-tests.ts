import {AssetGraphEditorComponent} from "app/shared/assets/asset-graph-editor/asset-graph-editor.component";
import {ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {TestDriver} from "app/test/driver";
import {AssetGraphDriver} from "app/test/drivers/asset-graph-driver";
import {ahuOptionLabel, datOptionLabel, vavOptionLabel} from "app/test/drivers/demo-data-driver";

export abstract class Test
{
    protected constructor(protected m_driver: TestDriver)
    {
    }

    async init(): Promise<void>
    {
        await this.ensureLoggedIn();
    }

    abstract execute(): Promise<void>

    abstract cleanup(): Promise<void>

    //--//

    protected async ensureLoggedIn(): Promise<void>
    {
        try
        {
            await this.m_driver.app.domain.users.checkLoggedIn();
        }
        catch (e)
        {
            await this.m_driver.app.domain.users.login("admin@demo.optio3.com", "adminPwd");
        }
    }

    protected async ensureLoggedOut(): Promise<void>
    {
        try
        {
            await this.m_driver.app.domain.users.checkLoggedIn();
            await this.m_driver.app.domain.users.logout();
        }
        catch (e)
        {
        }
    }
}

export abstract class AssetGraphTest extends Test
{
    protected m_assetGraphDriver: AssetGraphDriver = this.m_driver.getDriver(AssetGraphDriver);

    public static readonly ahuVavDatGraphName = "AHU / VAV / DAT";
    public static readonly vavDatGraphName    = "VAV / DAT";

    private async addVavDatNodes(graphEditor: AssetGraphEditorComponent,
                                 ahuNodeId?: string)
    {
        const vavNodeId = await this.m_assetGraphDriver.addAssetGraphNode(graphEditor, ahuNodeId, null, ConditionNodeType.EQUIPMENT, false, vavOptionLabel);
        await this.m_assetGraphDriver.addAssetGraphNode(graphEditor, vavNodeId, null, ConditionNodeType.POINT, false, datOptionLabel);
    }

    protected async ensureAhuVavDatGraph(): Promise<boolean>
    {
        return this.m_assetGraphDriver.ensureAssetGraph(AssetGraphTest.ahuVavDatGraphName, async (graphEditor) =>
        {
            const ahuNodeId = await this.m_assetGraphDriver.addAssetGraphNode(graphEditor, null, null, ConditionNodeType.EQUIPMENT, false, ahuOptionLabel);
            await this.addVavDatNodes(graphEditor, ahuNodeId);
        }, true);
    }

    protected async ensureVavDatGraph(): Promise<boolean>
    {
        return this.m_assetGraphDriver.ensureAssetGraph(AssetGraphTest.vavDatGraphName, (graphEditor) => this.addVavDatNodes(graphEditor), true);
    }
}

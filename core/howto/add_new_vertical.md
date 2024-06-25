To add a new vertical, several files need to be touched:

1) Define a subclass of InstanceConfiguration (there are multiple helper subclasses to choose from, for example:

```java
@JsonTypeName("InstanceConfigurationForMontage")
public class InstanceConfigurationForMontage extends InstanceConfigurationForTransportation
{
```

2) Rebuild Hub SDK:
```
o3 sdk hub
```
3) Add a corresponding entry in the enum CustomerVertical. For example:

```java
    MontageWalmart("Montage Walmart")
        {
            @Override
            public boolean shouldApplyToInstance()
            {
                return true;
            }

            @Override
            public Class<? extends InstanceConfiguration> getHandler()
            {
                return com.optio3.cloud.client.hub.model.InstanceConfigurationForMontage.class;
            }

            @Override
            public void fixupDeployer(CommonDeployer deployer)
            {
            }
        },
```


4) Rebuild Builder SDK:
```
o3 sdk builder
```
5) Rebuild and redeploy Builder.

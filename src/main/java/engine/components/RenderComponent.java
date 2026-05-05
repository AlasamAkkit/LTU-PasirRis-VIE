package engine.components;

public class RenderComponent {
    public String meshHandle;
    public String materialHandle;
    public boolean visible = true;

    public RenderComponent() {
    }

    public RenderComponent(String meshHandle, String materialHandle) {
        this.meshHandle = meshHandle;
        this.materialHandle = materialHandle;
    }
}
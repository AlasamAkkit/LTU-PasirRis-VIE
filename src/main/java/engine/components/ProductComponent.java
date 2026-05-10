package engine.components;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProductComponent {
    public String productType;
    public final Map<String, String> metadata = new LinkedHashMap<>();
    public int holderEntityId = -1;
    public boolean availableInWorld = true;
    public boolean respawnOnPickup = false;
    public float respawnX;
    public float respawnY;
    public float respawnZ;

    public ProductComponent() {
    }

    public ProductComponent(String productType) {
        this.productType = productType;
    }
}
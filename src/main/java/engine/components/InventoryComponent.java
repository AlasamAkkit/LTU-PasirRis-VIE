package engine.components;

import java.util.ArrayList;
import java.util.List;

public class InventoryComponent {
    public final List<Integer> heldEntityIds = new ArrayList<>();
    public int maxCapacity = 1;

    public boolean isFull() {
        return heldEntityIds.size() >= maxCapacity;
    }

    public boolean contains(int entityId) {
        return heldEntityIds.contains(entityId);
    }
}
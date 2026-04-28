package engine.components;

import java.util.ArrayList;
import java.util.List;

public class OrderBoxComponent {
    public final List<String> requiredProductTypes = new ArrayList<>();
    public final List<Integer> receivedItemEntityIds = new ArrayList<>();
    public boolean complete;
}
package engine.components;

import engine.math.Vector3;

import java.util.ArrayList;
import java.util.List;

public class NavigationAgentComponent {
    public int targetEntityId = -1;
    public final Vector3 destination = new Vector3();
    public boolean hasDestination;
    public boolean pathDirty;
    public boolean pathAvailable;
    public boolean reachedDestination = true;
    public final List<Vector3> path = new ArrayList<>();
    public int currentWaypointIndex;
    public float arriveDistance = 0.18f;
}

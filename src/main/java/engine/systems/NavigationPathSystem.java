package engine.systems;

import engine.components.NavigationAgentComponent;
import engine.components.NavigationGridComponent;
import engine.components.NavigationObstacleComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class NavigationPathSystem implements GameSystem {
    private static final int[][] NEIGHBORS = {
            { 1, 0 },
            { -1, 0 },
            { 0, 1 },
            { 0, -1 }
    };

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        NavigationGridComponent grid = findGrid(world);
        if (grid == null || grid.cellSize <= 0.0f) {
            return;
        }

        List<Integer> entityIds = world.getActiveEntityIds();
        boolean needsPath = false;
        for (int entityId : entityIds) {
            NavigationAgentComponent navigation = world.getComponent(entityId, NavigationAgentComponent.class);
            if (navigation != null && navigation.hasDestination && navigation.pathDirty) {
                needsPath = true;
                break;
            }
        }

        if (!needsPath) {
            return;
        }

        GridShape shape = createShape(grid);
        boolean[][] blocked = buildBlockedCells(world, entityIds, grid, shape);

        for (int entityId : entityIds) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            NavigationAgentComponent navigation = world.getComponent(entityId, NavigationAgentComponent.class);
            if (transform == null || navigation == null || !navigation.hasDestination || !navigation.pathDirty) {
                continue;
            }

            Vector3 destination = navigation.destination;
            if (navigation.targetEntityId != -1 && world.isAlive(navigation.targetEntityId)) {
                TransformComponent targetTransform = world.getComponent(navigation.targetEntityId, TransformComponent.class);
                if (targetTransform != null) {
                    destination = targetTransform.position;
                }
            }

            List<Vector3> path = findPath(transform.position, destination, grid, shape, blocked);
            navigation.path.clear();
            navigation.path.addAll(path);
            navigation.currentWaypointIndex = 0;
            navigation.pathAvailable = !navigation.path.isEmpty();
            navigation.reachedDestination = navigation.path.isEmpty();
            navigation.pathDirty = false;
        }
    }

    private NavigationGridComponent findGrid(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            NavigationGridComponent grid = world.getComponent(entityId, NavigationGridComponent.class);
            if (grid != null) {
                return grid;
            }
        }
        return null;
    }

    private GridShape createShape(NavigationGridComponent grid) {
        int columns = Math.max(1, (int) Math.floor((grid.maxX - grid.minX) / grid.cellSize) + 1);
        int rows = Math.max(1, (int) Math.floor((grid.maxZ - grid.minZ) / grid.cellSize) + 1);
        return new GridShape(columns, rows);
    }

    private boolean[][] buildBlockedCells(EcsWorld world, List<Integer> entityIds, NavigationGridComponent grid,
            GridShape shape) {
        boolean[][] blocked = new boolean[shape.columns][shape.rows];

        for (int entityId : entityIds) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            NavigationObstacleComponent obstacle = world.getComponent(entityId, NavigationObstacleComponent.class);
            if (transform == null || obstacle == null) {
                continue;
            }

            for (int x = 0; x < shape.columns; x++) {
                float worldX = cellToWorldX(x, grid);
                for (int z = 0; z < shape.rows; z++) {
                    float worldZ = cellToWorldZ(z, grid);
                    boolean insideX = Math.abs(worldX - transform.position.x) <= obstacle.halfExtents.x + obstacle.padding;
                    boolean insideZ = Math.abs(worldZ - transform.position.z) <= obstacle.halfExtents.z + obstacle.padding;
                    if (insideX && insideZ) {
                        blocked[x][z] = true;
                    }
                }
            }
        }

        return blocked;
    }

    private List<Vector3> findPath(Vector3 start, Vector3 goal, NavigationGridComponent grid, GridShape shape,
            boolean[][] blocked) {
        int startX = worldToCellX(start.x, grid, shape);
        int startZ = worldToCellZ(start.z, grid, shape);
        int goalX = worldToCellX(goal.x, grid, shape);
        int goalZ = worldToCellZ(goal.z, grid, shape);

        blocked[startX][startZ] = false;
        blocked[goalX][goalZ] = false;

        float[][] bestCost = new float[shape.columns][shape.rows];
        int[][] previousX = new int[shape.columns][shape.rows];
        int[][] previousZ = new int[shape.columns][shape.rows];
        for (int x = 0; x < shape.columns; x++) {
            Arrays.fill(bestCost[x], Float.MAX_VALUE);
            Arrays.fill(previousX[x], -1);
            Arrays.fill(previousZ[x], -1);
        }

        PriorityQueue<PathNode> open = new PriorityQueue<>((left, right) -> Float.compare(left.score, right.score));
        bestCost[startX][startZ] = 0.0f;
        open.add(new PathNode(startX, startZ, heuristic(startX, startZ, goalX, goalZ)));

        while (!open.isEmpty()) {
            PathNode current = open.poll();
            if (current.x == goalX && current.z == goalZ) {
                return reconstructPath(previousX, previousZ, startX, startZ, goalX, goalZ, goal, grid);
            }

            for (int[] neighbor : NEIGHBORS) {
                int nextX = current.x + neighbor[0];
                int nextZ = current.z + neighbor[1];
                if (!isInside(nextX, nextZ, shape) || blocked[nextX][nextZ]) {
                    continue;
                }

                float nextCost = bestCost[current.x][current.z] + 1.0f;
                if (nextCost >= bestCost[nextX][nextZ]) {
                    continue;
                }

                bestCost[nextX][nextZ] = nextCost;
                previousX[nextX][nextZ] = current.x;
                previousZ[nextX][nextZ] = current.z;
                float score = nextCost + heuristic(nextX, nextZ, goalX, goalZ);
                open.add(new PathNode(nextX, nextZ, score));
            }
        }

        return Collections.emptyList();
    }

    private List<Vector3> reconstructPath(int[][] previousX, int[][] previousZ, int startX, int startZ, int goalX,
            int goalZ, Vector3 exactGoal, NavigationGridComponent grid) {
        List<Vector3> reversed = new ArrayList<>();
        int currentX = goalX;
        int currentZ = goalZ;

        while (currentX != startX || currentZ != startZ) {
            reversed.add(new Vector3(cellToWorldX(currentX, grid), 0.0f, cellToWorldZ(currentZ, grid)));
            int nextX = previousX[currentX][currentZ];
            int nextZ = previousZ[currentX][currentZ];
            if (nextX == -1 || nextZ == -1) {
                return Collections.emptyList();
            }
            currentX = nextX;
            currentZ = nextZ;
        }

        Collections.reverse(reversed);
        if (reversed.isEmpty()) {
            reversed.add(new Vector3(exactGoal.x, 0.0f, exactGoal.z));
        } else {
            reversed.set(reversed.size() - 1, new Vector3(exactGoal.x, 0.0f, exactGoal.z));
        }
        return reversed;
    }

    private float heuristic(int x, int z, int goalX, int goalZ) {
        return Math.abs(goalX - x) + Math.abs(goalZ - z);
    }

    private boolean isInside(int x, int z, GridShape shape) {
        return x >= 0 && z >= 0 && x < shape.columns && z < shape.rows;
    }

    private int worldToCellX(float x, NavigationGridComponent grid, GridShape shape) {
        int cell = Math.round((x - grid.minX) / grid.cellSize);
        return Math.max(0, Math.min(shape.columns - 1, cell));
    }

    private int worldToCellZ(float z, NavigationGridComponent grid, GridShape shape) {
        int cell = Math.round((z - grid.minZ) / grid.cellSize);
        return Math.max(0, Math.min(shape.rows - 1, cell));
    }

    private float cellToWorldX(int x, NavigationGridComponent grid) {
        return grid.minX + x * grid.cellSize;
    }

    private float cellToWorldZ(int z, NavigationGridComponent grid) {
        return grid.minZ + z * grid.cellSize;
    }

    private static final class GridShape {
        private final int columns;
        private final int rows;

        private GridShape(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    private static final class PathNode {
        private final int x;
        private final int z;
        private final float score;

        private PathNode(int x, int z, float score) {
            this.x = x;
            this.z = z;
            this.score = score;
        }
    }
}

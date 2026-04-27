package vie.game.agents;

import vie.game.OrderSystem;

public class WorkerAgent {
    // private String state = "IDLE";
    private String currentOrder = null;
    private OrderSystem orderSystem;

    public WorkerAgent(OrderSystem orderSystem) {
        this.orderSystem = orderSystem;
    }

    public void update(float deltaTime) {
        if (currentOrder == null) {
            if (orderSystem.hasOrders()) {
                currentOrder = orderSystem.getNextOrder();
                System.out.println("Worker picked order: " + currentOrder);
            } else {
                // System.out.println("No orders available ");
            }
        } else {
            System.out.println("Worker delivering: " + currentOrder);
            currentOrder = null;
        }
    }

    /*
     * public void update() {
     * 
     * switch (state) {
     * case "IDLE":
     * System.out.println("Robot Searching for box...");
     * state = "MOVING_TO_BOX";
     * break;
     * 
     * case "MOVING_TO_BOX":
     * System.out.println("Robot moving to box...");
     * state = "PICKING_BOX";
     * break;
     * 
     * case "PICKING_BOX":
     * System.out.println("Robot picked up box the box.");
     * state = "DELIVERING";
     * break;
     * 
     * case "DELIVERING":
     * System.out.println("Robot delivering the box to shelf...");
     * state = "IDLE";
     * break;
     * }
     * }
     */
}

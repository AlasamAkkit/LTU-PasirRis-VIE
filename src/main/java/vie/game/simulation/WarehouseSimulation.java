package vie.game.simulation;

import vie.game.OrderSystem;

public class WarehouseSimulation {

    private OrderSystem orderSystem;
    private float workTimer = 0f;
    private float workDuration = 3f;
    private String currentOrder = null;

    public WarehouseSimulation() {
        orderSystem = new OrderSystem();
    }

    public void update(float deltaTime) {

        if (currentOrder == null) {
            if (orderSystem.hasOrders()) {
                currentOrder = orderSystem.getNextOrder();
                workTimer = 0f;
                System.out.println("Worker started order: " + currentOrder);
            }
        }

        if (currentOrder != null) {
            workTimer += deltaTime;

            if (workTimer >= workDuration) {
                System.out.println("Worker delivered: " + currentOrder);
                currentOrder = null;
            }
        }
    }

    public OrderSystem getOrderSystem() {
        return orderSystem;
    }
}

package vie.game;

import java.util.LinkedList;
import java.util.Queue;

public class OrderSystem {

    private Queue<String> orders = new LinkedList<>();

    public OrderSystem() {
        orders.add("Milk");
        orders.add("Bread");
        orders.add("Apples");
        System.out.println("Order System created");
        System.out.println("Orders loaded: " + orders.size());
    }

    public String getNextOrder() {
        return orders.poll();
    }

    public boolean hasOrders() {
        return !orders.isEmpty();
    }
}
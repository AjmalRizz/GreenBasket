package model;

/**
 * Concrete subclass representing a Store Manager.
 * Demonstrates Object-Oriented Inheritance and Polymorphism.
 */
public class StoreManager extends User {

    private static final long serialVersionUID = 2L;

    public StoreManager(String username, String passwordHash, String fullName) {
        super(username, passwordHash, fullName);
    }

    @Override
    public String getRoleName() {
        return "Store Manager";
    }

    @Override
    public boolean canManageUsers() {
        return true;
    }

    @Override
    public boolean canDeleteProducts() {
        return true;
    }

    @Override
    public boolean canEditProducts() {
        return true;
    }

    @Override
    public boolean canAddProducts() {
        return true;
    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    public boolean canPerformSales() {
        return true;
    }
}

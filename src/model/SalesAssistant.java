package model;

/**
 * Concrete subclass representing a Sales Assistant.
 * Demonstrates Object-Oriented Inheritance and Polymorphism.
 */
public class SalesAssistant extends User {

    private static final long serialVersionUID = 2L;

    public SalesAssistant(String username, String passwordHash, String fullName) {
        super(username, passwordHash, fullName);
    }

    @Override
    public String getRoleName() {
        return "Sales Assistant";
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }

    @Override
    public boolean canDeleteProducts() {
        return false;
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

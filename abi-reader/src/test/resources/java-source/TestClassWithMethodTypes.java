class TestClassWithMethodTypes {
    void testMethod() {
        ExtendsBase base = new ExtendsBase();
        if (base instanceof Base) {
            System.err.println("Extends Base");
        }

        WithPrivateField[] arrObject = new WithPrivateField[0];
    }
}

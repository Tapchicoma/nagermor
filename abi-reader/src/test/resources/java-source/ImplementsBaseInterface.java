class ImplementsBaseInterface implements BaseInterface {
    public Base methodOne() {
        try {
            return methodThree();
        } catch (TestException exception) {
            return new Base();
        }
    }

    public void methodTwo() {}

    private ExtendsBase methodThree() throws TestException {
        return new ExtendsBase();
    }
}

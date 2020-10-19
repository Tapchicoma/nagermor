public class ClassWithAnonymousClass {
    public BaseInterface testField = new BaseInterface() {
        public Base methodOne() {
            return new Base();
        }

        public void methodTwo() {}
    };
}

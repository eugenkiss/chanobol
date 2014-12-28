package anabolicandroids.chanobol;

final class Modules {
    static Object[] list(App app) {
        return new Object[] {
                new AppModule(app),
                new DebugAppModule()
        };
    }
    private Modules() {}
}

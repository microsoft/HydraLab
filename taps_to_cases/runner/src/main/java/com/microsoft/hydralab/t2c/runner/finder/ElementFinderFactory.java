package com.microsoft.hydralab.t2c.runner.finder;

import com.microsoft.hydralab.t2c.runner.controller.AndroidDriverController;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.EdgeDriverController;
import com.microsoft.hydralab.t2c.runner.controller.WindowsDriverController;
import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public final class ElementFinderFactory {

    private static final Map<Class<?>, Class<?>> REGISTRY_MAP = new HashMap<>();

    private ElementFinderFactory() {

    }

    static {
        REGISTRY_MAP.put(AndroidDriverController.class, AndroidElementFinder.class);
        REGISTRY_MAP.put(EdgeDriverController.class, EdgeElementFinder.class);
        REGISTRY_MAP.put(WindowsDriverController.class, WindowsElementFinder.class);
    }

    /**
     * Register your element finder in the factory, by default we provide {@link AndroidElementFinder},
     * {@link EdgeElementFinder} and {@link WindowsElementFinder}
     *
     * @param controller see {@link BaseDriverController}
     * @param finder     see {@link ElementFinder}
     */
    public static void registerFinder(Class<? extends BaseDriverController> controller, Class<? extends ElementFinder<? extends BaseElementInfo>> finder) {
        REGISTRY_MAP.put(controller, finder);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends BaseElementInfo> ElementFinder<T> createElementFinder(BaseDriverController driverController) {
        Class<?> finderClz = REGISTRY_MAP.get(driverController.getClass());
        if (finderClz != null) {
            try {
                return (ElementFinder<T>) finderClz.getDeclaredConstructor(BaseDriverController.class).newInstance(driverController);
            } catch (InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException("ElementFinder must be a public class, exception: " + e.getMessage());
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException("ElementFinder must have a constructor with param BaseDriverController");
            }
        }
        throw new UnsupportedOperationException("Unsupported driver controller!");
    }
}

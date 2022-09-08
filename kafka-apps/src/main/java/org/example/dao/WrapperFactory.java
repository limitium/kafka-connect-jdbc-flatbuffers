package org.example.dao;

import com.google.flatbuffers.Table;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory for wrappers. 
 *
 * @param <FB> - flatbuffers table
 * @param <WRAPPER> - wrapper class around FB
 */
public class WrapperFactory<FB extends Table, WRAPPER extends AbstractFlatbufferWrapper<FB>> {
    protected final Class<FB> fbClass;
    private final Class<WRAPPER> wrapperClass;
    private final Constructor<WRAPPER> wrapConstructor;
    private final Constructor<WRAPPER> createConstructor;


    @SuppressWarnings("unchecked")
    public WrapperFactory(Class<? extends FlatbuffersDAO<FB, WRAPPER>> daoClass) {
        ResolvableType resolvableType = ResolvableType.forClass(daoClass).as(FlatbuffersDAO.class);

        fbClass = (Class<FB>) resolvableType.getGeneric(0).resolve();
        wrapperClass = (Class<WRAPPER>) resolvableType.getGeneric(1).resolve();

        try {
            wrapConstructor = wrapperClass.getDeclaredConstructor(fbClass);
            createConstructor = wrapperClass.getDeclaredConstructor(long.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Wrapper must have 2 constructors new Wrapper(Flatbuffers fb) and new Wrapper(long id)", e);
        }
    }

    /**
     * Used by {@link FlatbuffersDAO#get(long)} to create wrapper around available flatbuffers object
     * 
     * @param object - flatbuffers object
     * @return
     */
    WRAPPER wrap(FB object) {
        try {
            return wrapConstructor.newInstance(object);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to create wrapper with wrapped constructor for flatbuffers", e);
        }
    }

    /**
     * Used by {@link FlatbuffersDAO#create(long)} to create wrapper around empty flatbuffers
     *
     * @param id - new key for store
     * @return
     */
    WRAPPER create(long id) {
        try {
            return createConstructor.newInstance(id);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to create wrapper with create constructor for id", e);
        }
    }
}
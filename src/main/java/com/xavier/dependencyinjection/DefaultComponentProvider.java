package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.xavier.dependencyinjection.ContextConfig.ComponentProvider;
import static com.xavier.dependencyinjection.ContextConfig.Context;
import static java.util.Arrays.stream;

class DefaultComponentProvider<T> implements ComponentProvider<T> {

    private final Constructor<T> constructor;
    private final List<Field> fields;
    private final List<Method> methdos;

    DefaultComponentProvider(Class<T> implementationClass) {
        this.constructor = (Constructor<T>) getInjectionConstructor(implementationClass);
        fields = getInjectionFields(implementationClass);
        methdos = getInjectionMethods(implementationClass);
    }

    private List<Method> getInjectionMethods(Class<T> implementationClass) {
        return stream(implementationClass.getMethods()).filter(method -> method.isAnnotationPresent(Inject.class)).toList();
    }

    private List<Field> getInjectionFields(final Class<T> implementationClass) {
        List<Field> injectionFields = new ArrayList<>();
        Class<?> currentClass = implementationClass;
        while (currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            injectionFields.addAll(stream(declaredFields).filter(field -> field.isAnnotationPresent(Inject.class)).toList());
            injectionFields.forEach(field -> {
                if (Modifier.isFinal(field.getModifiers()))
                    throw new FinalDependencyFoundException();
            });
            currentClass = currentClass.getSuperclass();
        }

        return injectionFields;
    }

    private Constructor<?> getInjectionConstructor(Class<?> implementationClass) {
        int modifiers = implementationClass.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers))
            throw new UnsupportedOperationException();

        Constructor<?>[] injectionConstructors = stream(implementationClass.getConstructors())
                .filter(implementationConstructor -> implementationConstructor.isAnnotationPresent(Inject.class))
                .toArray(Constructor<?>[]::new);
        if (injectionConstructors.length > 1) throw new MultipleInjectionFoundException();

        return stream(injectionConstructors).findFirst().orElseGet(() -> {
            try {
                return implementationClass.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException(e);
            }
        });
    }

    @Override
    public T get(Context context) {
        try {
            Object[] constructorParameters = stream(constructor.getParameterTypes())
                    .map(parameterType -> context.get(parameterType).orElse(null))
                    .toArray();
            T instance = constructor.newInstance(constructorParameters);
            fields.forEach(field -> {
                try {
                    Class<?> injectionType = field.getType();
                    Object injectionComponent = context.get(injectionType).orElse(null);
                    field.setAccessible(true);
                    field.set(instance, injectionComponent);
                } catch (IllegalAccessException e) {
                    throw new UnsupportedOperationException(e);
                }
            });
            methdos.forEach(method -> {
                Object[] parameters = stream(method.getParameterTypes()).map(type -> context.get(type).orElse(null)).toArray();
                try {
                    method.invoke(instance, parameters);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new UnsupportedOperationException(e);
                }
            });
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        List<Class<?>> dependencies = new ArrayList<>(Arrays.asList(constructor.getParameterTypes()));
        dependencies.addAll(fields.stream().map(Field::getType).toList());
        return dependencies;
    }
}

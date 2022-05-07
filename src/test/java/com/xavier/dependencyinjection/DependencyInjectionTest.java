package com.xavier.dependencyinjection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyInjectionTest {
    private Context context;

    // 对于组件构造部分，我分解的任务如下：

    @Nested
    class InstanceComponentBind {

        @BeforeEach
        void setup() {
            context = new Context();
        }

        // 无需构造的组件——组件实例
        @Test
        void should_bind_if_giving_a_instance() {
            Component component = new Component() {
            };
            context.bind(Component.class, component);

            Optional<Component> bindComponent = context.get(Component.class);
            assertTrue(bindComponent.isPresent());
            assertSame(component, bindComponent.get());
        }

        // 如果注册的组件不可实例化，则抛出异常
        @Test
        void should_throw_exception_if_no_default_constructor_nor_injection_constructor() {
            assertThrows(UnsupportedOperationException.class, () -> context.bind(Component.class, CannotInstanceComponent.class));
        }

        // 抽象类
        @Test
        void should_throw_exception_if_implementation_is_abstract_class() {
            assertThrows(UnsupportedOperationException.class, () -> context.bind(Component.class, AbstractComponent.class));
        }

        // 接口
        @Test
        void should_throw_exception_if_implementation_is_interface() {
            assertThrows(UnsupportedOperationException.class, () -> context.bind(Component.class, Component.class));
        }
    }

    // 构造函数注入
    @Nested
    class ConstructorComponentBind {

        @BeforeEach
        void setup() {
            context = new Context();
        }

        // 无依赖的组件应该通过默认构造函数生成组件实例
        @Test
        void should_bind_if_class_has_default_constructor() {
            context.bind(Component.class, DefaultConstructorComponent.class);

            Optional<Component> bindComponent = context.get(Component.class);
            assertTrue(bindComponent.isPresent());
            Assertions.assertTrue(bindComponent.get() instanceof DefaultConstructorComponent);
        }

        // 有依赖的组件，通过 Inject 标注的构造函数生成组件实例
        @Test
        void should_bind_and_inject_if_class_has_injection_constructor() {
            Dependency dependency = new Dependency() {
            };

            context.bind(Dependency.class, dependency);
            context.bind(Component.class, InjectionConstructorComponent.class);

            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency, ((InjectionConstructorComponent) component.get()).dependency());
        }

        // 如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入
        @Test
        void should_bind_and_inject_if_class_has_transitive_constructor() {
            String stringComponent = "this is a string component";
            context.bind(String.class, stringComponent);
            context.bind(Dependency.class, StringConstructorDependency.class);
            context.bind(Component.class, InjectionConstructorComponent.class);

            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isPresent());
            StringConstructorDependency dependency = (StringConstructorDependency) ((InjectionConstructorComponent) component.get()).dependency();
            assertNotNull(dependency);
            assertSame(stringComponent, dependency.stringComponent());
        }

        // 如果组件有多于一个 Inject 标注的构造函数，则抛出异常
        @Test
        void should_throw_exception_if_implementation_has_multiple_injection_constructors() {
            assertThrows(MultipleInjectionFoundException.class, () -> context.bind(Component.class, MultipleInjectionConstructorComponent.class));
        }

        // 如果组件需要的依赖不存在，则抛出异常
        @Test
        void should_throw_exception_if_dependency_not_exist() {
            context.bind(Component.class, InjectionConstructorComponent.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));

            List<Class<?>> dependencies = exception.getDependencies();
            assertEquals(2, dependencies.size());
            assertTrue(dependencies.contains(Component.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertEquals("Component -> Dependency not found", exception.getMessage());
        }

        // 如果组件间存在循环依赖，则抛出异常
        @Test
        void should_throw_exception_if_cyclic_dependency_found() {
            context.bind(Component.class, InjectionConstructorComponent.class);
            context.bind(Dependency.class, DependencyDependOnComponent.class);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> context.get(Dependency.class));

            List<?> dependencies = exception.getDependencies();
            assertEquals(3, dependencies.size());
            assertTrue(dependencies.contains(Component.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertEquals("Dependency -> Component -> Dependency", exception.getMessage());
        }

        @Test
        void should_throw_exception_if_transitive_cyclic_dependency_found() {
            context.bind(Component.class, InjectionConstructorComponent.class);
            context.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
            context.bind(AnotherDependency.class, AnotherDependencyDependOnComponentDependency.class);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> context.get(Dependency.class));

            List<?> dependencies = exception.getDependencies();
            assertEquals(4, dependencies.size());
            assertTrue(dependencies.contains(Component.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertTrue(dependencies.contains(AnotherDependency.class));
            assertEquals("Dependency -> AnotherDependency -> Component -> Dependency", exception.getMessage());
        }
    }

    // 字段注入
    // 通过 Inject 标注将字段声明为依赖组件
    // 如果组件需要的依赖不存在，则抛出异常
    // 如果字段为 final 则抛出异常
    // 如果组件间存在循环依赖，则抛出异常

    // 方法注入
    // 通过 Inject 标注的方法，其参数为依赖组件
    // 通过 Inject 标注的无参数方法，会被调用
    // 按照子类中的规则，覆盖父类中的 Inject 方法
    // 如果组件需要的依赖不存在，则抛出异常
    // 如果方法定义类型参数，则抛出异常
    //  如果组件间存在循环依赖，则抛出异常

    // 对于依赖选择部分，我分解的任务列表如下：

    // 对 Provider 类型的依赖
    // 注入构造函数中可以声明对于 Provider 的依赖
    // 注入字段中可以声明对于 Provider 的依赖
    // 注入方法中可声明对于 Provider 的依赖

    // 自定义 Qualifier 的依赖
    // 注册组件时，可额外指定 Qualifier
    // 注册组件时，可从类对象上提取 Qualifier
    // 寻找依赖时，需同时满足类型与自定义 Qualifier 标注
    // 支持默认 Qualifier——Named

    // 对于生命周期管理部分，我分解的任务列表如下：

    // Singleton 生命周期
    // 注册组件时，可额外指定是否为 Singleton
    // 注册组件时，可从类对象上提取 Singleton 标注
    // 对于包含 Singleton 标注的组件，在容器范围内提供唯一实例
    // 容器组件默认不是 Single 生命周期

    // 自定义 Scope 标注
    // 可向容器注册自定义 Scope 标注的回调
}

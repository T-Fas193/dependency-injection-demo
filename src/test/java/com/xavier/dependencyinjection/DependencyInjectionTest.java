package com.xavier.dependencyinjection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyInjectionTest {
    private ContextConfig contextConfig;

    // 对于组件构造部分，我分解的任务如下：

    @Nested
    class InstanceComponentBind {

        @BeforeEach
        void setup() {
            contextConfig = new ContextConfig();
        }

        // 无需构造的组件——组件实例
        @Test
        void should_bind_if_giving_a_instance() {
            Component component = new Component() {
            };
            contextConfig.bind(Component.class, component);

            Optional<Component> bindComponent = contextConfig.getContext().get(Component.class);
            assertTrue(bindComponent.isPresent());
            assertSame(component, bindComponent.get());
        }

        // 如果注册的组件不可实例化，则抛出异常
        @Test
        void should_throw_exception_if_no_default_constructor_nor_injection_constructor() {
            assertThrows(UnsupportedOperationException.class, () -> contextConfig.bind(Component.class, CannotInstanceComponent.class));
        }

        // 抽象类
        @Test
        void should_throw_exception_if_implementation_is_abstract_class() {
            assertThrows(UnsupportedOperationException.class, () -> contextConfig.bind(Component.class, AbstractComponent.class));
        }

        // 接口
        @Test
        void should_throw_exception_if_implementation_is_interface() {
            assertThrows(UnsupportedOperationException.class, () -> contextConfig.bind(Component.class, Component.class));
        }
    }

    // 构造函数注入
    @Nested
    class ConstructorComponentBind {

        @BeforeEach
        void setup() {
            contextConfig = new ContextConfig();
        }

        // 无依赖的组件应该通过默认构造函数生成组件实例
        @Test
        void should_bind_if_class_has_default_constructor() {
            contextConfig.bind(Component.class, DefaultConstructorComponent.class);

            Optional<Component> bindComponent = contextConfig.getContext().get(Component.class);
            assertTrue(bindComponent.isPresent());
            Assertions.assertTrue(bindComponent.get() instanceof DefaultConstructorComponent);
        }

        // 有依赖的组件，通过 Inject 标注的构造函数生成组件实例
        @Test
        void should_bind_and_inject_if_class_has_injection_constructor() {
            Dependency dependency = new Dependency() {
            };

            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, InjectionConstructorComponent.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency, ((InjectionConstructorComponent) component.get()).dependency());
        }

        // 如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入
        @Test
        void should_bind_and_inject_if_class_has_transitive_constructor() {
            String stringComponent = "this is a string component";
            contextConfig.bind(String.class, stringComponent);
            contextConfig.bind(Dependency.class, StringConstructorDependency.class);
            contextConfig.bind(Component.class, InjectionConstructorComponent.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            StringConstructorDependency dependency = (StringConstructorDependency) ((InjectionConstructorComponent) component.get()).dependency();
            assertNotNull(dependency);
            assertSame(stringComponent, dependency.stringComponent());
        }

        // 如果组件有多于一个 Inject 标注的构造函数，则抛出异常
        @Test
        void should_throw_exception_if_implementation_has_multiple_injection_constructors() {
            assertThrows(MultipleInjectionFoundException.class, () -> contextConfig.bind(Component.class, MultipleInjectionConstructorComponent.class));
        }

        // 如果组件需要的依赖不存在，则抛出异常
        @Test
        void should_throw_exception_if_dependency_not_exist() {
            contextConfig.bind(Component.class, InjectionConstructorComponent.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());

            List<Class<?>> dependencies = exception.getDependencies();
            assertEquals(2, dependencies.size());
            assertTrue(dependencies.contains(Component.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertEquals("Component -> Dependency not found", exception.getMessage());
        }

        @Test
        void should_throw_exception_if_transitive_dependency_not_exist() {
            contextConfig.bind(Component.class, InjectionConstructorComponent.class);
            contextConfig.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());

            List<Class<?>> dependencies = exception.getDependencies();
            assertTrue(dependencies.size() >= 2);
            assertTrue(dependencies.contains(AnotherDependency.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertTrue(exception.getMessage().split("->").length >= 2);
        }

        // 如果组件间存在循环依赖，则抛出异常
        @Test
        void should_throw_exception_if_cyclic_dependency_found() {
            contextConfig.bind(Component.class, InjectionConstructorComponent.class);
            contextConfig.bind(Dependency.class, DependencyDependOnComponent.class);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> contextConfig.getContext());

            List<?> dependencies = exception.getDependencies();
            assertEquals(3, dependencies.size());
            assertTrue(dependencies.contains(Component.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertEquals(3, exception.getMessage().split("->").length);
        }

        @Test
        void should_throw_exception_if_transitive_cyclic_dependency_found() {
            contextConfig.bind(Component.class, InjectionConstructorComponent.class);
            contextConfig.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
            contextConfig.bind(AnotherDependency.class, AnotherDependencyDependOnComponentDependency.class);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> contextConfig.getContext());

            List<?> dependencies = exception.getDependencies();
            assertEquals(4, dependencies.size());
            assertTrue(dependencies.contains(Component.class));
            assertTrue(dependencies.contains(Dependency.class));
            assertTrue(dependencies.contains(AnotherDependency.class));
            assertEquals(4, exception.getMessage().split("->").length);
        }
    }

    // 字段注入
    @Nested
    class FieldComponentBind {

        @BeforeEach
        void setup() {
            contextConfig = new ContextConfig();
        }

        // 通过 Inject 标注将字段声明为依赖组件
        @Test
        void should_bind_if_field_annotated_with_inject() {
            Dependency dependency = new Dependency() {
            };
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, ComponentDependOnDependencyFieldInjection.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertEquals(dependency, ((ComponentDependOnDependencyFieldInjection) component.get()).getDependency());
        }

        @Test
        void should_bind_if_super_class_field_annotated_with_inject() {
            Dependency dependency = new Dependency() {
            };
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, SubcomponentFieldInjection.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertEquals(dependency, ((SubcomponentFieldInjection) component.get()).getDependency());
        }

        // 如果组件需要的依赖不存在，则抛出异常
        @Test
        void should_throw_exception_if_field_dependency_not_found() {
            contextConfig.bind(Component.class, ComponentDependOnDependencyFieldInjection.class);

            assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());
        }

        @Test
        void should_throw_exception_if_transitive_field_dependency_not_found() {
            contextConfig.bind(Component.class, ComponentDependOnDependencyFieldInjection.class);
            contextConfig.bind(Dependency.class, DependencyDependOnAnotherDependencyFieldInjection.class);

            assertThrows(DependencyNotFoundException.class, contextConfig::getContext);
        }

        // 如果字段为 final 则抛出异常
        @Test
        void should_throw_exception_if_field_is_declared_with_final() {
            assertThrows(FinalDependencyFoundException.class, () -> contextConfig.bind(Component.class, ComponentDependOnFinalDependencyFieldInjection.class));
        }

        // 如果组件间存在循环依赖，则抛出异常
        @Test
        void should_throw_exception_if_cyclic_field_dependency_injection_found() {
            contextConfig.bind(Component.class, ComponentDependOnDependencyFieldInjection.class);
            contextConfig.bind(Dependency.class, DependencyDependOnComponentFieldInjection.class);

            assertThrows(CyclicDependencyFoundException.class, () -> contextConfig.getContext());
        }

        @Test
        void should_throw_exception_if_transitive_cyclic_field_dependency_injection_found() {
            contextConfig.bind(Component.class, ComponentDependOnDependencyFieldInjection.class);
            contextConfig.bind(Dependency.class, DependencyDependOnAnotherDependencyFieldInjection.class);
            contextConfig.bind(AnotherDependency.class, AnotherDependencyDependOnComponentFieldInjection.class);

            assertThrows(CyclicDependencyFoundException.class, contextConfig::getContext);
        }
    }

    // 方法注入
    @Nested
    class MethodComponentBind {

        @BeforeEach
        void setup() {
            contextConfig = new ContextConfig();
        }

        // 通过 Inject 标注的方法，其参数为依赖组件
        @Test
        void should_bind_if_method_annotated_with_injection() {
            Dependency dependency = Mockito.mock(Dependency.class);

            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, ComponentDependOnDependencyMethodInjection.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertEquals(dependency, ((ComponentDependOnDependencyMethodInjection) component.get()).getDependency());
        }

        @Test
        void should_bind_if_super_class_method_is_annotated_with_injection() {
            Dependency dependency = Mockito.mock(Dependency.class);

            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, SubcomponentDependOnDependencyMethodInjection.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertEquals(dependency, ((SubcomponentDependOnDependencyMethodInjection) component.get()).getDependency());
        }

        // 通过 Inject 标注的无参数方法，会被调用
        @Test
        void should_call_method_if_no_parameters_method_annotated_with_injection() {
            contextConfig.bind(Component.class, NoParameterComponentMethodInjection.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertEquals("this is a test string", ((NoParameterComponentMethodInjection) component.get()).getTestString());
        }

        // 按照子类中的规则，覆盖父类中的 Inject 方法
        @Test
        void should_use_override_method_if_super_method_annotated_with_injection() {
            Dependency dependency = Mockito.mock(Dependency.class);
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, OverrideComponentDependOnDependencyMethodInjection.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertEquals(dependency, ((OverrideComponentDependOnDependencyMethodInjection) component.get()).getDependency());
            assertEquals("this is a override test string", ((OverrideComponentDependOnDependencyMethodInjection) component.get()).getTestString());
        }

        // 如果组件需要的依赖不存在，则抛出异常
        @Test
        void should_throw_exception_if_dependency_not_found() {
            contextConfig.bind(Component.class, ComponentDependOnDependencyMethodInjection.class);

            assertThrows(DependencyNotFoundException.class, contextConfig::getContext);
        }
        // 如果方法定义类型参数，则抛出异常
        //  如果组件间存在循环依赖，则抛出异常
    }

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

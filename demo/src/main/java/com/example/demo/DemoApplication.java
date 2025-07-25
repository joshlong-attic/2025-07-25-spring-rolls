package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashSet;

@ImportRuntimeHints(DemoApplication.MyHints.class)
@SpringBootApplication
//@RegisterReflectionForBinding(Person.class)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    static final Resource MESSAGE_RESOURCE = new ClassPathResource("/message");

    static class BFIAP implements BeanFactoryInitializationAotProcessor {

        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
            var listOfClassesThatAreSerialized = new HashSet<Class<?>>();
            for (var beanName : beanFactory.getBeanDefinitionNames()) {
                var type = beanFactory.getType(beanName);
                if (Serializable.class.isAssignableFrom(type)) {
                    listOfClassesThatAreSerialized.add(type);
                }
            }
            return (generationContext, _) -> {
                for (var s : listOfClassesThatAreSerialized) {
                    generationContext.getRuntimeHints().serialization().registerType(TypeReference.of(s));
                    log.info("registering serialization hint for class [{}]", s.getName());
                }
            };
        }

    }

    static class MyHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerResource(DemoApplication.MESSAGE_RESOURCE);
            hints.reflection().registerType(Person.class, MemberCategory.values());
        }

    }

    @Bean
    static BFIAP bfiap() {
        return new BFIAP();
    }


    @Bean
    ApplicationRunner runner() {
        return _ -> {

            // RESOURCE LOADING
            var contents = MESSAGE_RESOURCE.getContentAsString(Charset.defaultCharset());
            System.out.println("the message is [" + contents + "]");

            // REFLECTION
            // i had to make it so static analysis would not pick up that we're doing something with a class called com.example.demo.Person
            // the ways to fix this are to either register a hint or use @RegisterReflectionForBinding
            // if you want to invoke methods as below, you'll need to register a hint. simple enumeration of properties is possible with the annotation
            var personClass = Class.forName("com.example"
                    + (1 > 0 ? " .demo. ".strip() : ".demo.") + "Person");
            var declaredConstructor = personClass.getDeclaredConstructor(String.class);
            declaredConstructor.setAccessible(true);
            var person = declaredConstructor.newInstance("Clement");
            var hello = personClass.getDeclaredMethod("hello");
            hello.setAccessible(true);
            hello.invoke(person);
        };
    }
}


class Person {

    String name;

    Person(String name) {
        this.name = name;
    }

    void hello() {
        System.out.println("hello, " + this.name);
    }
}

@Component
class Cart implements Serializable {
}


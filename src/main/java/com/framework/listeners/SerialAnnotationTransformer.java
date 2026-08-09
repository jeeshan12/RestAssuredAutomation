package com.framework.listeners;

import com.framework.annotations.Serial;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;


public class SerialAnnotationTransformer implements IAnnotationTransformer {

    public static final String SERIAL_GROUP = "serial";

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (!isSerial(testClass, testMethod)) {
            return;
        }
        Set<String> groups = new LinkedHashSet<>(Arrays.asList(annotation.getGroups()));
        groups.add(SERIAL_GROUP);
        annotation.setGroups(groups.toArray(new String[0]));
    }

    private boolean isSerial(Class<?> testClass, Method testMethod) {
        if (testMethod != null && testMethod.isAnnotationPresent(Serial.class)) {
            return true;
        }
        if (testMethod != null && testMethod.getDeclaringClass().isAnnotationPresent(Serial.class)) {
            return true;
        }
        return testClass != null && testClass.isAnnotationPresent(Serial.class);
    }
}

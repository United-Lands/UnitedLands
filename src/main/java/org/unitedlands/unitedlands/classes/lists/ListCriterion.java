package org.unitedlands.unitedlands.classes.lists;

import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public record ListCriterion<T>(
        String key,
        Function<T, Comparable> extractor,
        BiFunction<T, Object, String> display,
        boolean descendingByDefault) {
}

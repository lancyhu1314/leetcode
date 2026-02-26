package com.suning.fab.tup4ml.service;

import java.util.function.Function;

@FunctionalInterface
interface UncheckedFunction<T, R> {

    public R apply(T t) throws Exception;
    
    public static <T> Function<T, T> identity() {
        return t -> t;
    }
}

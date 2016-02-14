package fr.gaellalire.vestige.core.function;

public interface Function<T, R, E extends Throwable> {

    R apply(T t) throws E;

}

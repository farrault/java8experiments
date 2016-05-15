package me.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


public class Main {

    public static void main(String[] args) {
        List<?> values = Arrays.asList("hello", 1, 1.5);

        values.stream()
                .map(
                        match()
                                .whenIs("hello").then(__ -> "hello world")
                                .whenTypeIs(Integer.class).then(i -> "int: " + i)
                                .otherwise(__ -> "what else!")
                )
                .forEach(System.out::println);
    }


    static <T, R> MatchCase<T, R> match() {
        return new MatchCase<>();
    }
    
    static class MatchCase<T, R> implements Function<T, R> {

        final List<Case<T, R>> cases;

        MatchCase() {
            this(Collections.emptyList());
        }

        MatchCase(List<Case<T, R>> cases) {
            this.cases = cases;
        }

        When<T, R> when(Predicate<? super T> predicate) {
            return function -> addCase(predicate, function);
        }
        
    	private MatchCase<T, R> addCase(Predicate<? super T> predicate, Function<? super T, ? extends R> function) {
    		Case<T, R> newCase = new Case<T, R>(predicate, function);
    		
    		List<Case<T, R>> cases = new ArrayList<>();
    		cases.addAll(this.cases); // // Attention : capture du contexte => "leak" mémoire => ou pas ... on peut chainer ...
			cases.add(newCase);
    		
    		return new MatchCase<T, R>(cases); // parcequ'on est immutable à la base là... mais bon ...
    	}

        When<T, R> whenIs(Object pattern) {
            return when(pattern::equals);
        }

        When<T, R> whenTypeIs(Class<?> cls) {
            return when(v -> cls.isAssignableFrom(v.getClass()));
        }

        Otherwise<T, R> otherwise(Function<? super T, ? extends R> function) {
        	MatchCase<T, R> then = addCase(value -> true, function);
            return (Otherwise<T, R>) ( then::apply );
        }


        @Override
        public R apply(T value) {
            return cases.stream()
                    .filter(c -> c.isApplicable(value))
                    .findFirst()
                    .map(c -> c.apply(value))
                    .orElseThrow(IllegalArgumentException::new);
        }

    }
    
    interface When<T,R> {
    	MatchCase<T, R> then(Function<? super T, ? extends R> function);
    }

    interface Otherwise<T,R> extends Function<T, R> {
    	
    };
    
    static class Case<T, R> { // implements Function<Object, R> {
    	
    	final Predicate<Object> predicate;
    	final Function<Object, ? extends R> function;
    	
    	@SuppressWarnings("unchecked")
    	Case(Predicate<? super T> predicate, Function<? super T, ? extends R> function) {
    		this.predicate = (Predicate<Object>) predicate;
    		this.function = (Function<Object, ? extends R>) function;
    	}
    	
    	boolean isApplicable(Object object) {
    		return predicate.test(object);
    	}
    	
//    	@Override
    	public R apply(Object object) {
    		return function.apply(object);
    	}
    	
    }

}

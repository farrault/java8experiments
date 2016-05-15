package me.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


public class Main {

    public static void main(String[] args) {
        List<?> values = Arrays.asList(new Child(), "hello", 1, 1.5);

        values.stream()
                .map(
                        match()
                                .whenIs("hello").then(__ -> "hello world")
                                .whenTypeIs(Integer.class).then(i -> "int: " + i)
                                .whenTypeIs(Root.class).then(r -> "name : " + r.getName())
                                .otherwise(__ -> "what else!")
                )
                .forEach(System.out::println);
    }

    static abstract class Root {
    	abstract String getName();
    }
    
    static class Child extends Root {

		@Override
		String getName() {
			return "Child";
		}
    	
    }
    

    static <R> MatchCase<R> match() {
        return new MatchCase<>();
    }
    
    static class MatchCase<R> implements Function<Object, R> {

        final List<Case<?,R>> cases;

        MatchCase() {
            this(Collections.emptyList());
        }

        MatchCase(List<Case<?,R>> cases) {
            this.cases = cases;
        }

        <T> When<T,R> when(Predicate<Object> predicate) {
        	return function -> addCase(predicate,function);
        }
        
    	private <T> MatchCase<R> addCase(Predicate<Object> predicate, Function<T, ? extends R> function) {
    		Case<T,R> newCase = new Case<T,R>(predicate, function);
    		
    		List<Case<?,R>> cases = new ArrayList<>();
    		cases.addAll(this.cases); // // Attention : capture du contexte => "leak" mémoire => ou pas ... on peut chainer ...
			cases.add(newCase);
    		
    		return new MatchCase<R>(cases); // parcequ'on est immutable à la base là... mais bon ...
    	}

        <T> When<T,R> whenIs(T pattern) {
            return when(pattern::equals);
        }

        <T> When<T,R> whenTypeIs(Class<T> cls) {
            return when(v -> {
        		System.out.println("---->"+v);
        		return cls.isAssignableFrom(v.getClass()); // isAssignableFrom est-il encore nécessaire ? !! OUI carrément !
            });
        }

        Otherwise<R> otherwise(Function<Object, ? extends R> function) {
        	MatchCase<R> then = addCase(value -> true, function);
            return (Otherwise<R>) ( then::apply );
        }


        @Override
        public R apply(Object value) {
            return cases.stream()
                    .filter(c -> c.isApplicable(value))
                    .findFirst()
                    .map(c -> c.apply(value))
                    .orElseThrow(IllegalArgumentException::new);
        }

    }
    
    interface When<T,R> {
    	MatchCase<R> then(Function<T, ? extends R> function);
    }

    interface Otherwise<R> extends Function<Object,R> {
    	
    };
    
    static class Case<T,R> { // implements Function<ObjecR> {
    	
    	final Predicate<Object> predicate; // TODO : passer le prédicate en T ?
    	final Function<T, ? extends R> function;
    	
    	@SuppressWarnings("unchecked")
    	Case(Predicate<Object> predicate, Function<T, ? extends R> function) {
    		this.predicate = predicate;
    		this.function = function;
    	}
    	
    	boolean isApplicable(Object object) {
    		System.out.println("->"+object);
    		// TODO : tester que c'est bien un T ?
    		return predicate.test((Object)object);   // Comment ca peut fonctionner ??? => C'est à cause du type erasure ... au runtime c'est un (Object)
    	}
    	
//    	@Override
//    	/*public*/ R apply(T object) {
    	/*public*/ R apply(Object object) { // peut pas être T sinon impossible de faire le cast au dessus
//    		return function.apply((T)object);
    		return function.apply((T)object);
    	}
    	
    }

}

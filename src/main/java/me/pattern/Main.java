package me.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


public class Main {

    public static void main(String[] args) {
//        List<?> values = Arrays.asList(new Child(), "hello", 1, 1.5);
        List<? extends Root> values = Arrays.asList(new Child(), new Child2());

        values.stream()
                .map(
//                		Main.<Root,String>match()
                		matchTo(String.class)
//                			match()
//                                .whenIs("hello").then(__ -> "hello world")
//                                .whenTypeIs(Integer.class).then(i -> "int: " + i)
                                .whenTypeIs(Root.class).then(r -> "name : " + r.getName())
                                .otherwise(__ -> "what else!")
//                                .otherwise(__ -> 1)
                )
                .forEach(System.out::println);
    }

    // ----
    
    static abstract class Root {
    	abstract String getName();
    }
    
    static class Child extends Root {

		@Override
		String getName() {
			return "Child";
		}
    	
    }
    static class Child2 extends Child {
    	
    	@Override
    	String getName() {
    		return "Child2";
    	}
    	
    }

    // ----

    // A l'usage : le param�tre I ne sert pas � grand chose => il faut le sp�cifier pour qu'il soit utile
    static <I,R> MatchCase<I,R> match() {
        return new MatchCase<>();
    }
    static <I,R> MatchCase<I,R> matchTo(Class<R> clazz) {
    	return new MatchCase<>();
    }
    
    static class MatchCase<I,R> implements Function<I, R> {

        final List<Case<? extends I,R>> cases;

        MatchCase() {
            this(Collections.emptyList());
        }

        MatchCase(List<Case<? extends I,R>> cases) {
            this.cases = cases;
        }

        <T extends I> When<I, T,R> when(Predicate<Object> predicate) {
        	return function -> addCase(predicate,function);
        }
        
    	private <T extends I> MatchCase<I,R> addCase(Predicate<Object> predicate, Function<T, ? extends R> function) {
    		Case<T,R> newCase = new Case<T,R>(predicate, function);
    		
    		List<Case<? extends I,R>> cases = new ArrayList<>();
    		cases.addAll(this.cases); // // Attention : capture du contexte => "leak" m�moire => ou pas ... on peut chainer ...
			cases.add(newCase);
    		
    		return new MatchCase<I,R>(cases); // parcequ'on est immutable � la base l�... mais bon ...
    	}

        <T extends I> When<I,T,R> whenIs(T pattern) {
            return when(pattern::equals);
        }

        <T extends I> When<I,T,R> whenTypeIs(Class<T> cls) {
            return when(v -> cls.isAssignableFrom(v.getClass())); // isAssignableFrom est-il encore n�cessaire ? !! OUI carr�ment !
        }

        Otherwise<I,R> otherwise(Function<I, ? extends R> function) {
        	MatchCase<I,R> then = addCase(value -> true, function);
            return (Otherwise<I,R>) ( then::apply );
        }


        @Override
        public R apply(I value) {
            return cases.stream()
                    .filter(c -> c.isApplicable(value))
                    .findFirst()
                    .map(c -> c.apply(value))
                    .orElseThrow(IllegalArgumentException::new);
        }

    }
    
    interface When<I,T,R> {
    	MatchCase<I,R> then(Function<T, ? extends R> function);
    }

    interface Otherwise<I,R> extends Function<I,R> {
    	
    };
    
    static class Case<T,R> {
    	
    	final Predicate<Object> predicate; // TODO : passer le pr�dicate en T ?
    	final Function<T, ? extends R> function;
    	
    	@SuppressWarnings("unchecked")
    	Case(Predicate<Object> predicate, Function<T, ? extends R> function) {
    		this.predicate = predicate;
    		this.function = function;
    	}
    	
    	boolean isApplicable(Object object) {
    		return predicate.test((Object)object);
    	}
    	
    	/*public*/ R apply(Object object) { // peut pas �tre T sinon impossible de faire le cast au dessus
    		return function.apply((T)object);
    	}
    	
    }

}

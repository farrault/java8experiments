package me.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


public class Main {

    public static void main(String[] args) {
        List<?> values = Arrays.asList(new Child("mon nom"), "hello", 1, 1.5);
//    	List<? extends Root> values = Arrays.asList(new Child(), new Child2());

        values.stream()
                .map(
//                		Main.<Root,String>match()
                		matchTo(String.class)
//                			match()
                                .whenIs("hello").then(__ -> "hello world")
                                .whenTypeIs(Integer.class).then((Object i) -> "int: " + i)
                                .whenTypeIs(Root.class).then(r -> "name : " + r.getName())
                                .otherwise(__ -> "what else!")
//                                .otherwise(__ -> 1)
                )
                .forEach(System.out::println);
        
        Function<Object, Child> function = __ -> new Child();
		values.stream()
        .map(
        		matchTo(Root.class)
        		.whenTypeIs(Root.class).then(r -> r)
        		.otherwise(function)
        		)
        .forEach(System.out::println);
    }

    // ----
    
    static abstract class Root {
    	abstract String getName();
    	@Override
    	public String toString() {
    		return getClass() + " - name : "+getName();
    	}
    }
    
    static class Child extends Root {

    	String name = "Unamed";
    	
    	public Child() {
    		
    	}
		public Child(String string) {
			this.name = string;
		}

		@Override
		String getName() {
			return name;
		}
    	
    }
    static class Child2 extends Child {
    	
    	public Child2(String string) {
			super("Child2");
		}
    }

    // -------------------------
    // PUBLIC :
    
    // A l'usage : le paramètre I ne sert pas à grand chose => il faut le spécifier pour qu'il soit utile
    static <I,R> MatchCase<I,R> match() {
        return new MatchCase<>();
    }
    static <I,R> MatchCase<I,R> matchTo(Class<R> clazz) {
    	return new MatchCase<>();
    }

    interface When<I,T,R> {
    	MatchCase<I,R> then(Function<? super T, ? extends R> function);
    }

    interface Otherwise<I,R> extends Function<I,R> {
    	
    };
    
    // -------------------------
    // PRIVATE : 

    static class MatchCase<I,R> implements Function<I, R> {

        final List<Case<? extends I,R>> cases;

        MatchCase() {
            this(Collections.emptyList());
        }

        MatchCase(List<Case<? extends I,R>> cases) {
            this.cases = cases;
        }

        <T extends I> When<I, T, R> when(Predicate<? super T> predicate) {
        	return function -> addCase(predicate,function);
        }
        
    	private <T extends I> MatchCase<I,R> addCase(Predicate<? super T> predicate, Function<? super T, ? extends R> function) {
    		Case<T,R> newCase = new Case<T,R>(predicate, function);
    		
    		List<Case<? extends I,R>> cases = new ArrayList<>();
    		cases.addAll(this.cases); // // Attention : capture du contexte => "leak" mémoire => ou pas ... on peut chainer ...
			cases.add(newCase);
    		
    		return new MatchCase<I,R>(cases); // parcequ'on est immutable à la base là... mais bon ...
    	}

        <T extends I> When<I,T,R> whenIs(T pattern) {
            return when(pattern::equals);
        }

        <T extends I> When<I,T,R> whenTypeIs(Class<T> cls) {
            return when(v -> cls.isAssignableFrom(v.getClass())); // isAssignableFrom est-il encore nécessaire ? !! OUI carrément !
        }

        Otherwise<I,R> otherwise(Function<? super I, ? extends R> function) {
//       	Otherwise<I,R> otherwise(Function<I, R> function) {
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
    
    static class Case<T,R> {
    	
    	final Predicate<Object> predicate; // TODO : passer le prédicate en T ?
    	final Function<Object, ? extends R> function;
    	
    	@SuppressWarnings("unchecked")
    	Case(Predicate<?> predicate, Function<? super T, ? extends R> function) {
    		this.predicate = (Predicate<Object>) predicate;
    		this.function = (Function<Object, ? extends R>) function; // On veut pouvoir appeler la function sur n'importe quel Object (sur n'importe quel I en fait) dans le apply du MatchCase
    	}
    	
    	boolean isApplicable(Object object) {
    		return predicate.test((Object)object);
    	}
    	
    	/*public*/ R apply(Object object) {
    		return function.apply(object);
    	}
    	
    }

}

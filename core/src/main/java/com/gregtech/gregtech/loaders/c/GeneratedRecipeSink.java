package com.gregtech.gregtech.loaders.c;
/** Scoped native consumer for the single original recipe-row catalog. No platform objects live here. */
public final class GeneratedRecipeSink {
 @FunctionalInterface public interface Sink {void accept(String map,long eut,long ticks,long[] chances,String[] itemsIn,String[] fluidsIn,String[] fluidsOut,String[] itemsOut);}
 private static final ThreadLocal<Sink> CURRENT=new ThreadLocal<>();
 private GeneratedRecipeSink(){}
 public static void emit(Sink sink,Runnable rows){var previous=CURRENT.get();CURRENT.set(java.util.Objects.requireNonNull(sink));try{rows.run();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
 static void register(String map,long eut,long ticks,long[] chances,String[] itemsIn,String[] fluidsIn,String[] fluidsOut,String[] itemsOut){var sink=CURRENT.get();if(sink==null)throw new IllegalStateException("Recipe rows need a platform resolver");sink.accept(map,eut,ticks,chances,itemsIn,fluidsIn,fluidsOut,itemsOut);}
}

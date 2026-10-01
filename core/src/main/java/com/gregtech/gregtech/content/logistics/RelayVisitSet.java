package com.gregtech.gregtech.content.logistics;
import java.util.Set;import java.util.HashSet;
/** Independent per-operation thread visits: prevents relay cycles, removes thread state at the outer return. */
public final class RelayVisitSet<T>{private final ThreadLocal<Set<T>> visited=ThreadLocal.withInitial(HashSet::new);public Set<T> get(){return visited.get();}public void remove(){visited.remove();}}

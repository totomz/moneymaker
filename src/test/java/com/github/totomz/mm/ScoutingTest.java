package com.github.totomz.mm;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javaslang.Tuple3;
import javaslang.collection.Array;
import javaslang.collection.List;
import javaslang.collection.Stream;
import javaslang.collection.TreeMap;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author Tommaso Doninelli
 */
public class ScoutingTest {

    @Ignore
    public void ListPartitioning() {
        
        List<Array<Integer>> listOfList = javaslang.collection.Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .grouped(4)
                .toList();
 
        System.out.println("Number of batch:" + listOfList.size());
        listOfList.forEach(arr -> {
            
            System.out.println("====================");
            System.out.println(":::" + arr.length());
            arr.forEach(System.out::println);
            System.out.println("====================");
                
        });
        
    }
    
    
    @Test
    public void grouping() {
        List<Tuple3> numbers = Stream.of(
                new Tuple3(1, 12, 89),
                new Tuple3(1, 22, 89),
                new Tuple3(7, 22, 89),
                new Tuple3(1, 12, 89)
        ).toList();
        
        
        Map<Tuple3, Long> pippo = 
                numbers
                .toJavaStream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        
        pippo.forEach((k, v) -> {
        
            System.out.println("========================");
            System.out.println("k --> " + k);
            System.out.println("v --> " + v);
            System.out.println("========================");
            
        });
        
        
    }
}

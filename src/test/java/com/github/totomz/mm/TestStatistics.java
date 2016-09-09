/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.totomz.mm;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javaslang.Function2;
import javaslang.Tuple6;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Tommaso Doninelli <tommaso.doninelli@gmail.com>
 */
public class TestStatistics {
    
    @Test
    public void testCountOccurrencies() {
        
        Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> drawings = new HashMap<>();
        drawings.put(new Tuple6(10,20,30,40,50,60), 1l);
        drawings.put(new Tuple6(11,20,31,41,51,61), 1l);
        drawings.put(new Tuple6(10,21,32,41,52,63), 1l);
        
        List<ConcurrentHashMap<Integer, AtomicInteger>> occurenciesByPosition = Main.getStatistics.apply(drawings);
        
        Function2<Integer, Integer, Integer> getCountsByPosAndNum = (position, number) -> {
          
            return occurenciesByPosition.get(position-1).getOrDefault(number, new AtomicInteger(0)).get();
        };
        
        Assert.assertEquals(2, getCountsByPosAndNum.apply(1, 10).intValue());
        Assert.assertEquals(1, getCountsByPosAndNum.apply(1, 11).intValue());
        Assert.assertEquals(1, getCountsByPosAndNum.apply(3, 30).intValue());   // There is 1 occurrency of number 30 in the third position of the tuple
        Assert.assertEquals(1, getCountsByPosAndNum.apply(3, 31).intValue());
        Assert.assertEquals(1, getCountsByPosAndNum.apply(3, 32).intValue());
        Assert.assertEquals(2, getCountsByPosAndNum.apply(4, 41).intValue());
    }
    
}

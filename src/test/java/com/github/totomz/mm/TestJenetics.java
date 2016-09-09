package com.github.totomz.mm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javaslang.Function1;
import javaslang.Tuple6;
import org.jenetics.BitGene;
import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.util.Factory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author Tommaso Doninelli
 */
public class TestJenetics {

    private static Integer eval(Genotype<IntegerGene> genotype) {
                
        
        List<Integer> numbers = genotype.getChromosome().stream().mapToInt(IntegerGene::intValue).distinct().sorted().boxed().collect(Collectors.toList());
        
        int score = (numbers.size() == 6)?numbers.stream().mapToInt(Integer::intValue).sum():0;
        
        System.out.println("1=============");        
        genotype.getChromosome().stream().forEach(System.out::println);        
        System.out.println("score: " + score);
        System.out.println("2=============");
        return score;
    }
    
    @Ignore
    public void testDoesNotExistsEval() {
        Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> drawings = new HashMap<>();
        drawings.put(new Tuple6(10,20,30,40,50,60), 1l);
        drawings.put(new Tuple6(11,20,31,41,51,61), 1l);
        drawings.put(new Tuple6(10,21,32,41,52,63), 1l);
        
        Function1<int[], Integer> eval = Main.doesNotExistsFitness.apply(drawings);
        Assert.assertEquals(0, eval.apply(new int[]{10, 20, 30, 40, 50, 60}).intValue());
        Assert.assertEquals(1, eval.apply(new int[]{19, 20, 30, 40, 50, 60}).intValue());        
    }
    
    @Test
    public void testScore() {
        Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> drawings = new HashMap<>();
        drawings.put(new Tuple6(10,20,30,40,50,60), 1l);
        drawings.put(new Tuple6(11,20,31,41,51,61), 1l);
        drawings.put(new Tuple6(10,21,32,41,52,63), 1l);
        
        Function1<int[], Integer> eval = Main.preferNeverSeenNumbers.apply(Main.getStatistics.apply(drawings), drawings);

        Assert.assertEquals("An extracted sequence should have a fitness of 0", 0, eval.apply(new int[]{10, 20, 30, 40, 50, 60}).intValue());
        Assert.assertEquals("A sequence with numbers never seen on the position should have 100", 100, eval.apply(new int[]{60, 50, 40, 30, 20, 10}).intValue());
        Assert.assertEquals("Only 1 'never seen' number ", 61, eval.apply(new int[]{10, 20, 30, 49, 50, 60}).intValue());        
    }
    
    /**
     * Simple test to try Jenetics
     */
    @Ignore
    public void simpleTest() {
        
        final Factory<Genotype<IntegerGene>> factory = Genotype.of(IntegerChromosome.of(1, 90, 6));
        
        Engine<IntegerGene, Integer> engine = Engine.builder(TestJenetics::eval, factory).build();
        
        Genotype<IntegerGene> result = engine.stream().limit(1000).collect(EvolutionResult.toBestGenotype());
        
        System.out.println("CazzoFigata? ");
        System.out.println(result);
        System.out.println("BestFitVal:" + eval(result));
        
    }
}

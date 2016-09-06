package com.github.totomz.mm;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javaslang.Tuple6;
import org.jenetics.BitGene;
import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.util.Factory;
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
    
    
    @Test
    public void simpleTest() {
        
        final Factory<Genotype<IntegerGene>> factory = Genotype.of(IntegerChromosome.of(1, 90, 6));
        
        Engine<IntegerGene, Integer> engine = Engine.builder(TestJenetics::eval, factory).build();
        
        Genotype<IntegerGene> result = engine.stream().limit(1000).collect(EvolutionResult.toBestGenotype());
        
        System.out.println("CazzoFigata? ");
        System.out.println(result);
        System.out.println("BestFitVal:" + eval(result));
        
    }
}

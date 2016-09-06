package com.github.totomz.mm;

import io.restassured.RestAssured;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javaslang.Function0;
import javaslang.Function1;
import javaslang.Function2;
import javaslang.Tuple6;
import javaslang.collection.Stream;
import org.jenetics.Genotype;
import org.jenetics.IntegerChromosome;
import org.jenetics.IntegerGene;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    ///////////////
    // Functions //
    ///////////////
    
    /**
     * Download the drawings of SuperEnalotto for a given year
     */
    public static final Function1<Integer, List<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >>> downloadExtractedSequenceByYear = (year) -> {
        
        log.info("Downloading SuperEnalotto drawings for year " + year);
        
        List<String> numbers = RestAssured
                .get("http://www.superenalotto.com/risultati/{year}.asp", year)
                .htmlPath()
                .getList("**.findAll { it.@class == 'ball-24px' }");
        
                // Create batch of 6 numbers
        return Stream.ofAll(numbers)
                .map(Integer::parseInt)
                .grouped(6)
                .map(s -> {
                    List<Integer> l = s.toJavaList();
                    return new Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5));
                })
                .toJavaList();
    };


    
    // Genetic algorithm eval strategies
    /**
     * Simply returns 1 if the sequence is unknown
     */
    private static final Function2<Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>, Genotype<IntegerGene>, Integer > doesNotExistsFitness = (map, gt) -> {
        
        int[] n = gt.getChromosome().stream().mapToInt(IntegerGene::intValue).distinct().sorted().toArray();
        int score = (n.length == 6)?
                map.containsKey(new Tuple6<>(n[0], n[1], n[2], n[3], n[4], n[5]))?0:1
                :0;        
        return score;
    };
    
    private static final Function1<String, Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>> loadDrawingsFromFile = file -> {
    
        log.info("Loading drawings from file " + file);
        
        try(java.util.stream.Stream<String> stream = Files.lines(Paths.get(file))) {
            HashMap<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> res = new HashMap<>();
            stream.skip(1).forEach(line -> {                
                    Integer[] t = Stream.of(line.split(",")).map(String::trim).map(Integer::parseInt).toJavaArray(Integer.class);
                    res.put(new Tuple6<>(t[1], t[2], t[3], t[4], t[5], t[6]), 
                            new Long(t[0]));                            
                });
            
            return res;
        }
        catch (Exception e){
            log.error("Error loading drawings from file", e);
        }
        
        return null;
    };
    
    /**
     * Writes a Map in a file and return the map
     */
    private static final Function2<String, 
                    Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>, 
                    Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>> writeToFile = (filename, map) -> {                        
        
        log.info("Writing data to file " + filename);
        
        String header = "frequency, n1, n2, n3, n4, n5, n6 \n";        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)))) {
            bw.write(header);
            
            map.forEach((tuple, num) -> {
                try {
                    bw.write(num + ", " + tuple.toSeq().mkString(",") + "\n");
                } catch (IOException ex) {
                    log.error("Error writing a line of drawings", ex);
                }
            });            
            bw.close();
        }
        catch (Exception ex) {
            log.error("Error writing drawings on file", ex);
        }
        
        return map;
    };
    
    /**
     * Load the historical drawings from a remote service
     */
    private static final Function0< Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> > loadSisalData = () -> {
        return IntStream.range(1997, 2017).parallel()
                .mapToObj(downloadExtractedSequenceByYear::apply)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    };
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
   
       Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> drawings = 
               Optional.ofNullable(loadDrawingsFromFile.apply("output.txt"))
               .orElseGet(loadSisalData.andThen(writeToFile.curried().apply("output.txt")));

        // Choose the evaluating strategy
        Function<Genotype<IntegerGene>, Integer> evalFunction = doesNotExistsFitness.curried().apply(drawings);
        
        // Set a genetic algorithm to find an unknown sequence
        final Factory<Genotype<IntegerGene>> factory = Genotype.of(IntegerChromosome.of(1, 90, 6));        
        Engine<IntegerGene, Integer> engine = Engine.builder(evalFunction, factory).build();
        
        Genotype<IntegerGene> result = engine.stream().limit(1000).collect(EvolutionResult.toBestGenotype());
        
        log.info("Wanna be rich? Try this numbers!");
        
        String winnerSeq = result.getChromosome().stream().mapToInt(IntegerGene::intValue).sorted().boxed().map(Object::toString).collect(Collectors.joining(", "));
        log.info(winnerSeq);
    }       
}

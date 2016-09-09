package com.github.totomz.mm;

import com.google.gson.Gson;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javaslang.Function0;
import javaslang.Function1;
import javaslang.Function2;
import javaslang.Function3;
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
import spark.Spark;

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
    
    public static final Function1<String, Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>> loadDrawingsFromFile = file -> {
    
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
    
    
    private static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; 
    }

    ///////////////////////////////////////
    // Genetic algorithm eval strategies //
    ///////////////////////////////////////
    public static final Function1<Genotype<IntegerGene>, int[]> mapGeneToValues = (gt) -> {
        return gt.getChromosome().stream().mapToInt(IntegerGene::intValue).distinct().sorted().toArray();
    };
    
    /**
     * Simply returns 1 if the sequence is unknown
     */
    public static final Function2<Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>, int[], Integer > 
            doesNotExistsFitness = (map, n) -> {
        
//        int[] n = gt.getChromosome().stream().mapToInt(IntegerGene::intValue).distinct().sorted().toArray();
        int score = (n.length == 6)?
                map.containsKey(new Tuple6<>(n[0], n[1], n[2], n[3], n[4], n[5]))?0:1
                :0;        
        return score;
    };
    
    /**
     * Return a number [100,0] that depends on the uniqueness of each number in each position in the sequence
     * IF the sequence is present in memeory, returns 0
     * 
     * This algorithm sucks, the best sequence is 1,2,3,4,5,6 !
     */
    public static final Function3<List<ConcurrentHashMap<Integer, AtomicInteger>>, Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>, int[], Integer > 
            preferNeverSeenNumbers = (occurrencies, map, n) -> {
        
//        int[] n = gt.getChromosome().stream().mapToInt(IntegerGene::intValue).distinct().sorted().toArray();        
        
        if( (n.length != 6) ||
               (map.containsKey(new Tuple6<>(n[0], n[1], n[2], n[3], n[4], n[5]))) ) {
            return 0;
        }
        
        int score = map.size() * 6;
        
        for(int i=0;i<6;i++){            
            if(occurrencies.get(i).get(n[i]).get() > 0){
                score -= occurrencies.get(i).get(n[i]).get();
            }
        }        
        return (int)((double)score/(map.size() * 6)  * 100);
    };
    
    public static Function1<Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long>, List<ConcurrentHashMap<Integer, AtomicInteger>>>
            getStatistics = (draws) -> {
              
                List<ConcurrentHashMap<Integer, AtomicInteger>> occurenciesByPosition = Stream.of(
                    new ConcurrentHashMap<Integer, AtomicInteger>(), new ConcurrentHashMap<Integer, AtomicInteger>(), 
                    new ConcurrentHashMap<Integer, AtomicInteger>(), new ConcurrentHashMap<Integer, AtomicInteger>(),
                    new ConcurrentHashMap<Integer, AtomicInteger>(), new ConcurrentHashMap<Integer, AtomicInteger>()).toJavaList();
                
                // Initialize the occurrencies
                IntStream.range(1, 91).forEachOrdered(n -> {
                    occurenciesByPosition.forEach(occ -> {
                        occ.put(n, new AtomicInteger(0));
                    });
                });

                draws.keySet().stream().forEach(t -> {
                    occurenciesByPosition.get(0).get(t._1).incrementAndGet();
                    occurenciesByPosition.get(1).get(t._2).incrementAndGet();
                    occurenciesByPosition.get(2).get(t._3).incrementAndGet();
                    occurenciesByPosition.get(3).get(t._4).incrementAndGet();
                    occurenciesByPosition.get(4).get(t._5).incrementAndGet();
                    occurenciesByPosition.get(5).get(t._6).incrementAndGet();                    
                });
                
                return occurenciesByPosition;
            };
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
   
        // drawings contains all the extracted tuples
        Map<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >, Long> drawings = 
               Optional.ofNullable(loadDrawingsFromFile.apply("output.txt"))
               .orElseGet(loadSisalData.andThen(writeToFile.curried().apply("output.txt")));

        // Count occurrencies on the i-th element
        List<ConcurrentHashMap<Integer, AtomicInteger>> occurenciesByPosition = getStatistics.apply(drawings);
        
//        System.out.println("============ STATISTICS ============");
//        occurenciesByPosition.forEach(occ -> {        
//            System.out.println("*** ");
//            System.out.print("{");
//            occ.keySet().stream().sorted().forEach(n -> {
//                System.out.print(String.format(" [%s, %s] ", n, occ.getOrDefault(n, new AtomicInteger(0)).get()));
//            });                
//        });
//        System.out.println("============ STATISTICS ============");
        
        // Choose the evaluating strategy
        final Function<Genotype<IntegerGene>, Integer> evalFunction =  mapGeneToValues.andThen(doesNotExistsFitness.curried().apply(drawings));        
//        Function<Genotype<IntegerGene>, Integer> evalFunction = mapGeneToValues.andThen(preferNeverSeenNumbers.curried().apply(occurenciesByPosition).curried().apply(drawings));
        
        // Use this function to give a score to the user
        final Function<Genotype<IntegerGene>, Integer> publicScore = mapGeneToValues.andThen(preferNeverSeenNumbers.curried().apply(occurenciesByPosition).curried().apply(drawings));

        // Set-up a genetic algorithm to find an unknown sequence
        final Factory<Genotype<IntegerGene>> factory = Genotype.of(IntegerChromosome.of(1, 90, 6));        
        final Engine<IntegerGene, Integer> engine = Engine.builder(evalFunction, factory).build();
        
        // Answer to clients!
        Spark.port(getHerokuAssignedPort());
        
        log.info("Starting web server");
        
        Gson gson = new Gson();
        Spark.get("/magicnumbers", (req, resp)-> {
        
            Genotype<IntegerGene> result = engine.stream().limit(1000).collect(EvolutionResult.toBestGenotype());
            
            int score = publicScore.apply(result);
            resp.type("application/json");
            return new Result(score, result.getChromosome().stream().mapToInt(IntegerGene::intValue).sorted().toArray());
            
        }, gson::toJson);             
        
        final AtomicBoolean keepRunning = new AtomicBoolean(true);
        Spark.get("/stop", (req, resp)-> {
        
            keepRunning.set(false);
            return "Ciao";
            
        }, gson::toJson);             
        
        Spark.awaitInitialization();
        log.info("Main Thread is going to sleed forever");
        
        while(keepRunning.get()){
            Thread.sleep(1000);
        }
        
        log.info("Shutting down");
        Spark.stop();
        log.info("Bye");
    }       
}

class Result {
    
    private int score;
    private int[] numbers;

    public Result(int score, int[] numbers) {
        this.score = score;
        this.numbers = numbers;
    }
    
}
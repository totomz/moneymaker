package com.github.totomz.mm;

import org.junit.Test;
import static com.github.totomz.mm.Main.*;
import java.util.List;
import javaslang.Tuple6;
import org.junit.Assert;

/**
 * 
 * @author Tommaso Doninelli
 */
public class TestFunctions {

    @Test
    public void testDownloadFunction() {
        
        int year = 1997;
        
        List<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer >> extractedNumbers = downloadExtractedSequenceByYear.apply(year);
        
        // In 1997 there have been 9 drawings
        Assert.assertEquals(9, extractedNumbers.size());
        
        // Each drawing should have 6 number
        
        long numbers = extractedNumbers.stream().flatMap(t -> {
            return t.toSeq().toStream().toJavaStream();
        }).count();
        Assert.assertEquals(9 * 6, numbers);
    }
    
    
}

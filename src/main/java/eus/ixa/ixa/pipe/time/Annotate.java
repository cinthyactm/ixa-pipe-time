/*
 *  Copyright 2018 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.time;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import eus.ixa.ixa.pipe.ml.StatisticalSequenceLabeler;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.utils.Span;
import info.bethard.timenorm.Temporal;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Timex3;
import ixa.kaflib.WF;

import info.bethard.timenorm.TemporalExpressionParser;
import info.bethard.timenorm.TimeSpan;
//import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;




import scala.util.Try;
        /**
 * Annotation class for Named Entities in ixa-pipe-time. Use this class for
 * examples on using ixa-pipe-ml API for Named Entity tagging.
 * 
 * @author ragerri
 * @version 2018-05-14
 * 
 */
public class Annotate {

  /**
   * The SequenceLabeler to do the annotation.
   */
  private StatisticalSequenceLabeler temporalTagger;
  /**
   * Clear features after every sentence or when a -DOCSTART- mark appears.
   */
  private String clearFeatures;

  public Annotate(final Properties properties) throws IOException {

    this.clearFeatures = properties.getProperty("clearFeatures");
    temporalTagger = new StatisticalSequenceLabeler(properties);
  }

  public final void annotateTimeToKAF(final KAFDocument kaf)
      throws IOException {
 //   String  creationTime=kaf.createTimestamp();
    //  LocalDate DateTime = LocalDate.parse("2018-06-26");//, DateFormatter..ofPattern("yyyy-mm-dd"));
    LocalDateTime DateTime = LocalDateTime.parse(kaf.createTimestamp(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
 
    List<List<WF>> sentences = kaf.getSentences();
    TemporalExpressionParser parserT = TemporalExpressionParser.en();
    for (List<WF> sentence : sentences) {
      // process each sentence
      String[] tokens = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }
      if (clearFeatures.equalsIgnoreCase("docstart")
          && tokens[0].startsWith("-DOCSTART-")) {
        temporalTagger.clearAdaptiveData();
      }
      Span[] statSpans = temporalTagger.seqToSpans(tokens);
      Span[] allSpansArray = SequenceLabelerME.dropOverlappingSpans(statSpans);
      List<SequenceLabel> names = temporalTagger.getSequencesFromSpans(tokens,
          allSpansArray);
     for (SequenceLabel name : names) {
        Integer startIndex = name.getSpan().getStart();
        Integer endIndex = name.getSpan().getEnd();
        List<WF> nameWFs = sentence.subList(startIndex, endIndex);
        ixa.kaflib.Span<WF> neSpan = KAFDocument.newWFSpan(nameWFs);
        Timex3 timex3 = kaf.newTimex3(name.getType());
        Try<Temporal> temp = parserT.parse(name.getString(), TimeSpan.of(DateTime.getYear(),DateTime.getMonthValue(), DateTime.getDayOfMonth()));// "2018-06-26T12:35:47+0200");
         timex3.setValue(temp.get().timeMLValue());
     
                 
        timex3.setSpan(neSpan);
     
     
      }
      if (clearFeatures.equalsIgnoreCase("yes")) {
        temporalTagger.clearAdaptiveData();
      }
    }
    if (clearFeatures.equalsIgnoreCase("yes")) {
      temporalTagger.clearAdaptiveData();
    }
  }

  public String annotateToTimeML(KAFDocument kaf) {
      
    return kaf.toString();
  }

}


package nl.amc.ebioscience.rosemary.core.search

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.util.Version
import org.apache.lucene.store.NIOFSDirectory
import java.io.File

object SearchConfig {

  val LOCATION = "search"
  val ID_FIELD = "$I"
  val ALL_FIELD = "$A"
  val NAME_FIELD = "name"

  val version = Version.LUCENE_4_9

  val analyzer = new StandardAnalyzer(version)

  val directory = new NIOFSDirectory(new File(LOCATION))

}

/*
 * Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
 * 
 * This program is semi-free software: you can redistribute it and/or modify it
 * under the terms of the Rosemary license. You may obtain a copy of this
 * license at:
 * 
 * https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * You should have received a copy of the Rosemary license
 * along with this program. If not, 
 * see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
 * 
 *        Project: https://github.com/AMCeScience/Rosemary-Vanilla
 *        AMC eScience Website: http://www.ebioscience.amc.nl/
 */
package nl.amc.ebioscience.rosemary.services.search

import javax.inject._
import scala.concurrent.Future
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import nl.amc.ebioscience.rosemary.models.Searchable
import nl.amc.ebioscience.rosemary.models.core.Valunit
import org.apache.lucene.document.{ Document, StringField, Field, TextField }
import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }

@Singleton
class SearchWriter @Inject() (lifecycle: ApplicationLifecycle) {

  val writerConfig = new IndexWriterConfig(SearchConfig.version, SearchConfig.analyzer)
  // writerConfig.setRAMBufferSizeMB(256.0)
  val writer = new IndexWriter(SearchConfig.directory, writerConfig)
  commit

  lifecycle.addStopHook { () =>
    Future.successful(close)
  }

  def add(item: Searchable) {
    Logger.debug(s"Indexing Item: ${item.id}")
    val doc = new Document()

    doc.add(new StringField(SearchConfig.ID_FIELD, item.id.toString, Field.Store.YES))

    doc.add(new TextField(SearchConfig.NAME_FIELD, item.name, Field.Store.NO))

    generateTextFieldsWithPermutatedKeys(item.info.dict).foreach { tf => doc.add(tf) }

    // Collect all values
    val all = new StringBuilder(item.name)
    item.info.dict.foreach(entry => {
      all.append(' ')
      all.append(entry._2.value)
    })
    doc.add(new TextField(SearchConfig.ALL_FIELD, all.toString, Field.Store.NO))

    Logger.trace("Indexing: " + doc.toString)

    try {
      writer.addDocument(doc)
    } catch {
      case e: Exception => Logger.error(s"SearchWriter addDocument exception: ${e.getMessage}")
    }
  }

  /**
   * Sample input:
   * <pre><code>
   * Map(this/is/a/test -> Valunit(value1,None),
   *   key -> Valunit(value2,None),
   *   foo/bar -> Valunit(value3,Some(unit)))
   * </code></pre>
   *
   * Sample output:
   * <pre><code>
   * Map(test -> value1, a/test -> value1, is/a/test -> value1, this/is/a/test -> value1,
   *   key -> value2,
   *   bar -> value3, foo/bar -> value3)
   * </code></pre>
   */
  private def generateTextFieldsWithPermutatedKeys(dict: Map[String, Valunit]): Iterable[TextField] =
    for {
      entry <- dict
      permKeyStr <- entry._1.toLowerCase.split('/').scanRight(List[String]())(_ +: _).filterNot(_.isEmpty).map(_.mkString("/"))
    } yield new TextField(permKeyStr, entry._2.value, Field.Store.NO)

  def deleteAllAndCommit {
    Logger.info("Deleting the old index...")
    try {
      writer.deleteAll
      writer.commit
    } catch {
      case e: Exception => Logger.error(s"SearchWriter deleteAll and commit exception: ${e.getMessage}")
    }
  }

  def commit {
    Logger.debug("Committing search writer...")
    try {
      writer.commit
    } catch {
      case e: Exception => Logger.error(s"SearchWriter commit exception: ${e.getMessage}")
    }
  }

  def close {
    Logger.debug("Closing search writer...")
    try {
      writer.close
    } catch {
      case e: Exception => Logger.error(s"SearchWriter close exception: ${e.getMessage}")
    }
  }
}

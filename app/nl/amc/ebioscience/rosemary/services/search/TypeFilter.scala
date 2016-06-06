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

import nl.amc.ebioscience.rosemary.models.{ Datum, Processing, ProcessingGroup }
import org.apache.lucene.util.{ Bits, OpenBitSet }
import org.apache.lucene.search.{ Filter, DocIdSet, DocIdSetIterator }
import org.apache.lucene.index.{ AtomicReaderContext, Term }
import play.api.Logger
import java.time.{ Clock, Instant }

class TypeFilter(requestedType: SupportedTypes.Value) extends Filter {

  /**
   * Creates a DocIdSet enumerating the documents that should be permitted in search results.
   *
   * Note: null can be returned if no documents are accepted by this Filter.
   *
   * Note: This method will be called once per segment in the index during searching.
   * The returned DocIdSet must refer to document IDs for that segment, not for the top-level reader.
   */
  override def getDocIdSet(context: AtomicReaderContext, acceptDocs: Bits): DocIdSet = {

    //    val start: Instant = Clock.systemDefaultZone.instant

    val items = requestedType match {
      case SupportedTypes.Datum           => Datum.findAll()
      case SupportedTypes.Processing      => Processing.findAll()
      case SupportedTypes.ProcessingGroup => ProcessingGroup.findAll()
    }
    val itemIds = items.map { x => x.id }
    if (itemIds.isEmpty) {
      null
    } else {
      val reader = context.reader
      Logger.trace(s"reader docs = ${reader.getDocCount(SearchConfig.ID_FIELD).toString}")

      // To collect all document IDs that are allowed to be returned as hits
      val bits = new OpenBitSet(reader.maxDoc)

      // Find the ID for this document in Lucene's index and add it to bits
      val itemIdsList = itemIds.toList

      itemIdsList.foreach { itemId =>
        val term = new Term(SearchConfig.ID_FIELD, itemId.toString)
        val docsEnum = reader.termDocsEnum(term)
        if (docsEnum != null) {
          val doc = docsEnum.nextDoc()
          if (doc != DocIdSetIterator.NO_MORE_DOCS) {
            bits.set(doc)
          }
        }
      }
      Logger.trace(s"TypeFilter: Item Ids size = ${itemIdsList.size}")

      Logger.trace(s"TypeFilter: bits cardinality = ${bits.cardinality}")

      //      val stop: Instant = Clock.systemDefaultZone.instant
      //      val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
      //      Logger.trace(s"Type filter took ${runningTime} miliseconds")

      if (bits.isEmpty) null else bits
    }
  }
}

object SupportedTypes extends Enumeration {
  val Datum, Processing, ProcessingGroup = Value
}

package nl.amc.ebioscience.rosemary.core.search

import nl.amc.ebioscience.rosemary.models.{ Tag, Datum, Processing, ProcessingGroup, Searchable }
import org.apache.lucene.util.{ Bits, OpenBitSet }
import org.apache.lucene.search.{ Filter, DocIdSet, DocIdSetIterator }
import org.apache.lucene.index.{ AtomicReaderContext, Term }
import play.api.Logger
import java.time.{ Clock, Instant }

class TagsFilter(workspaceTags: List[Tag.Id], tags: List[Tag.Id], kind: Option[SupportedTypes.Value]) extends Filter {

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

    val (ditems, pitems, pgitems) = (workspaceTags, tags) match {
      case (Nil, Nil) =>
        (Datum.emptyCursor,
          Processing.emptyCursor,
          ProcessingGroup.emptyCursor)

      case (Nil, ts) =>
        // The searchable items that has all these tags 
        (Datum.findWithAllTagsNoPage(ts.toSet),
          Processing.findWithAllTagsNoPage(ts.toSet),
          ProcessingGroup.findWithAllTagsNoPage(ts.toSet))

      case (wts, Nil) =>
        // The searchable items that has any of workspace tags
        (Datum.findWithAnyTagsNoPage(wts.toSet),
          Processing.findWithAnyTagsNoPage(wts.toSet),
          ProcessingGroup.findWithAnyTagsNoPage(wts.toSet))

      case (wts, ts) =>
        // The searchable items that has any of the workspace tags and all other tags
        (Datum.findWithAnyWorkspaceTagAndWithAllTagsNoPage(wts, ts),
          Processing.findWithAnyWorkspaceTagAndWithAllTagsNoPage(wts, ts),
          ProcessingGroup.findWithAnyWorkspaceTagAndWithAllTagsNoPage(wts, ts))
    }
    val (ditemIds, pitemIds, pgitemIds) = (ditems.map(_.id), pitems.map(_.id), pgitems.map(_.id))

    if (ditemIds.isEmpty && pitemIds.isEmpty && pgitemIds.isEmpty) {
      null
    } else {
      val reader = context.reader
      Logger.trace(s"reader docs = ${reader.getDocCount(SearchConfig.ID_FIELD).toString}")

      // To collect all document IDs that are allowed to be returned as hits
      val bits = new OpenBitSet(reader.maxDoc)

      // Find the ID for this document in Lucene's index and add it to bits
      val itemIdsList: List[Searchable.Id] = kind match {
        case None => ditemIds.toList ::: pitemIds.toList ::: pgitemIds.toList
        case Some(requestedType) => requestedType match {
          case SupportedTypes.Datum           => ditemIds.toList
          case SupportedTypes.Processing      => pitemIds.toList
          case SupportedTypes.ProcessingGroup => pgitemIds.toList
        }
      }

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
      Logger.trace(s"TagsFilter: Item Ids size = ${itemIdsList.size}")

      Logger.trace(s"TagsFilter: bits cardinality = ${bits.cardinality}")

//      val stop: Instant = Clock.systemDefaultZone.instant
//      val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
//      Logger.trace(s"Tags filter took ${runningTime} miliseconds")

      if (bits.isEmpty) null else bits
    }
  }
}

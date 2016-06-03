package nl.amc.ebioscience.rosemary.core.search

import play.api.Logger
import nl.amc.ebioscience.rosemary.models.{ Tag }
import nl.amc.ebioscience.rosemary.models.Searchable
import org.apache.lucene.queryparser.classic.{ QueryParser, ParseException }
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{ IndexSearcher, Sort, SortField }
import org.apache.lucene.queries.ChainedFilter
import org.apache.lucene.util.Version
import org.bson.types.ObjectId
import java.time.{ Clock, Instant }

object SearchReader {

  val ITEMS_PER_PAGE = 10

  val reader = DirectoryReader.open(SearchConfig.directory)
  val searcher = new IndexSearcher(reader)

  def search(
    query: String,
    workspaceTags: List[Tag.Id],
    tags: List[Tag.Id],
    kind: Option[SupportedTypes.Value],
    page: Int): Either[String, List[Searchable.Id]] = {

    val start: Instant = Clock.systemDefaultZone.instant

    val pattern = "(\\S*:)(\\S*)".r

    val queryTermList = query.toLowerCase.replace("/", "\\/").split(' ')
    val correctedQueryTermList = for (qt <- queryTermList) yield qt match {
      case str @ ("and" | "or" | "not") => str.toUpperCase
      case pattern(field, term)         => s"$field/.*$term.*/"
      case str @ _                      => s"/.*${str}.*/"
    }

    val correctedQuery = correctedQueryTermList.mkString(" ")

    Logger.trace("Searching: " + correctedQuery)

    try {
      val queryParser = new QueryParser(SearchConfig.version, SearchConfig.ALL_FIELD, SearchConfig.analyzer)
      queryParser.setDefaultOperator(QueryParser.Operator.AND)
      val q = queryParser.parse(correctedQuery)

      val sort = new Sort(new SortField(SearchConfig.NAME_FIELD, SortField.Type.STRING))

      val topFieldDocs = kind match {
        case None =>
          searcher.search(q, new TagsFilter(workspaceTags, tags, None), ITEMS_PER_PAGE * (page + 1), sort)
        case Some(requestedType) =>
          searcher.search(q,
            new ChainedFilter(Array(
              new TagsFilter(workspaceTags, tags, Some(requestedType)),
              new TypeFilter(requestedType)),
              ChainedFilter.AND),
            ITEMS_PER_PAGE * (page + 1), sort)
      }

      val docs = topFieldDocs.scoreDocs.drop(ITEMS_PER_PAGE * page)
      val result = docs.map(hit => {
        val doc = searcher.doc(hit.doc)
        val id = doc.getField(SearchConfig.ID_FIELD).stringValue()
        new ObjectId(id)
      }).toList

      val stop: Instant = Clock.systemDefaultZone.instant
      val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
      Logger.trace(s"Search took ${runningTime} miliseconds")

      Right(result)
    } catch {
      case e: ParseException => Left(e.getMessage())
    }
  }

  def close {
    Logger.debug("Closing search reader...")
    try {
      reader.close
    } catch {
      case e: Exception => Logger.error(s"SearchReader close exception: ${e.getMessage}")
    }
  }
}

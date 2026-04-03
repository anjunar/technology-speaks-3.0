package app.pages.curation

import app.domain.core.Data
import app.domain.curation.*
import app.domain.documents.Document
import app.support.{RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.ComboBox
import jfx.form.ComboBox.*
import jfx.form.Control.placeholder
import jfx.form.Input.{input, inputType, stringValueProperty}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.statement.ObserveRender.observeRender

import scala.concurrent.ExecutionContext.Implicits.global

class CurationPage(
  val statusFilterProperty: Property[String],
  val typeFilterProperty: Property[String],
  val candidatesProperty: RemoteListProperty[Data[CurationCandidate], RemotePageQuery],
  val clustersProperty: RemoteListProperty[Data[CurationCluster], RemotePageQuery],
  val documentsProperty: RemoteListProperty[Data[Document], RemotePageQuery]
) extends PageComposite("Verdichtungsraum") {

  override def pageWidth: Int = 1320
  override def pageHeight: Int = 860

  private val pageSize = 50

  private val selectedCandidateProperty: Property[Data[CurationCandidate] | Null] = Property(null)
  private val selectedClusterProperty: Property[Data[CurationCluster] | Null] = Property(null)
  private val selectedDocumentProperty: Property[Data[Document] | Null] = Property(null)
  private val selectedSectionProperty: Property[String] = Property("")
  private val clusterSummaryProperty: Property[String] = Property("")
  private val newClusterTitleProperty: Property[String] = Property("")

  override protected def compose(using DslContext): Unit = {
    classProperty += "curation-page"

    addDisposable(
      statusFilterProperty.observeWithoutInitial { _ =>
        RemoteTableList.reloadFirstPage(candidatesProperty, pageSize = pageSize)
      }
    )
    addDisposable(
      typeFilterProperty.observeWithoutInitial { _ =>
        RemoteTableList.reloadFirstPage(candidatesProperty, pageSize = pageSize)
      }
    )

    withDslContext {
      hbox {
        classes = "documents-layout"
        style {
          height = "100%"
          width = "100%"
          overflow = "hidden"
          columnGap = "18px"
        }

        leftColumn()
        centerColumn()
        rightColumn()
      }
    }
  }

  private def leftColumn()(using DslContext): Unit = {
    vbox {
      classes = "documents-sidebar"
      style {
        width = "300px"
        minWidth = "300px"
        rowGap = "16px"
      }

      panel("Eingang") {
        filterButton("Eingang", CandidateStatus.Eingang, statusFilterProperty)
        filterButton("In Pruefung", CandidateStatus.InPruefung, statusFilterProperty)
        filterButton("Spannungen", CandidateStatus.Zugeordnet, statusFilterProperty)
        filterButton("Zurueckgestellt", CandidateStatus.Zurueckgestellt, statusFilterProperty)
        filterButton("Uebernommen", CandidateStatus.Uebernommen, statusFilterProperty)
        filterButton("Verworfen", CandidateStatus.Verworfen, statusFilterProperty)
      }

      panel("Resonanztypen") {
        filterButton("Alle", "", typeFilterProperty)
        ResonanceType.values.foreach(value => filterButton(value, value, typeFilterProperty))
      }

      panel("Cluster") {
        input("curationClusterTitle") {
          placeholder = "Neuer Cluster"
          inputType = "text"
          subscribeBidirectional(newClusterTitleProperty, stringValueProperty)
        }

        actionButton("Neuen Cluster anlegen") {
          createCluster()
        }

        div {
          style {
            flex = "1"
            minHeight = "220px"
          }

          virtualList(clustersProperty, estimateHeightPx = 92, overscanPx = 140, prefetchItems = 20) { (entry, _) =>
            clusterCard(entry)
          }
        }
      }
    }
  }

  private def centerColumn()(using DslContext): Unit = {
    vbox {
      classes = "doc-editor-stage"
      style {
        flex = "1"
        minWidth = "0px"
        rowGap = "16px"
      }

      panel("Verdichtungsraum") {
        span {
          text = "Hier werden Impulse, Fragen, Einwaende und Ergaenzungen geprueft, geordnet und einem Dokumentziel zugefuehrt."
        }
      }

      div {
        style {
          flex = "1"
          minHeight = "0px"
        }

        virtualList(candidatesProperty, estimateHeightPx = 170, overscanPx = 220, prefetchItems = 30) { (entry, _) =>
          candidateCard(entry)
        }
      }
    }
  }

  private def rightColumn()(using DslContext): Unit = {
    vbox {
      classes = "documents-sidebar"
      style {
        width = "320px"
        minWidth = "320px"
        rowGap = "16px"
      }

      panel("Ziel und Entscheidung") {
        observeRender(selectedCandidateProperty) { selected =>
          val candidate = if (selected == null) null else selected.data
          selectedSectionProperty.set(
            if (candidate == null || candidate.target.get == null || candidate.target.get.nn.sectionId == null) ""
            else candidate.target.get.nn.sectionId.nn
          )
          vbox {
            style {
              rowGap = "10px"
            }

            summaryLine("Status", if (candidate == null) "Kein Kandidat ausgewaehlt" else candidate.status.get)
            summaryLine("Typ", if (candidate == null) "" else candidate.resonanceType.get)
            summaryLine("Ziel-Dokument", if (candidate == null || candidate.target.get == null) "Noch offen" else candidate.target.get.nn.documentId)
            summaryLine("Abschnitt", if (candidate == null || candidate.target.get == null || candidate.target.get.nn.sectionId == null) "Noch offen" else candidate.target.get.nn.sectionId.nn)

            val documentRef = comboBox[Data[Document]]("curationTargetDocument", standalone = true) {
              ComboBox.items = documentsProperty
              placeholder = "Ziel-Dokument waehlen"
              identityBy = { (item: Data[Document]) => Option(item.data.id.get).getOrElse(item) }
              multipleSelection = false
              rowHeightPx = 42.0
              dropdownHeightPx = 260.0

              valueRenderer = {
                div {
                  text =
                    Option(selectedItem)
                      .map(_.data.title.get)
                      .filter(_.trim.nonEmpty)
                      .getOrElse("Ziel-Dokument waehlen")
                }
              }

              itemRenderer = {
                val item = comboItem[Data[Document]]
                div {
                  text = Option(item.data.title.get).filter(_.trim.nonEmpty).getOrElse("Unbenanntes Dokument")
                }
              }
            }

            addDisposable(
              selectedDocumentProperty.observeWithoutInitial { selectedDocument =>
                documentRef.setSelectedItem(selectedDocument)
              }
            )
            addDisposable(
              documentRef.selectedItemProperty.observe { item =>
                selectedDocumentProperty.set(item)
              }
            )

            input("curationSection") {
              placeholder = "Ziel-Abschnitt"
              inputType = "text"
              subscribeBidirectional(selectedSectionProperty, stringValueProperty)
            }

            actionButton("Als Impuls lesen") {
              classifySelectedCandidate(ResonanceType.Impuls)
            }
            actionButton("Als Widerspruch markieren") {
              classifySelectedCandidate(ResonanceType.Einwand)
            }
            actionButton("Als offene Frage markieren") {
              classifySelectedCandidate(ResonanceType.Frage)
            }
            actionButton("Ziel uebernehmen") {
              assignSelectedTarget()
            }
            actionButton("Uebernehmen") {
              acceptSelectedCluster()
            }
            actionButton("Zurueckstellen") {
              deferSelectedCluster()
            }
            actionButton("Verwerfen") {
              rejectSelectedCluster()
            }
          }
        }
      }

      panel("Spannung") {
        observeRender(selectedClusterProperty) { selected =>
          val cluster = if (selected == null) null else selected.data
          clusterSummaryProperty.set(if (cluster == null || cluster.summary.get == null) "" else cluster.summary.get.nn)
          vbox {
            style {
              rowGap = "10px"
            }

            summaryLine("Cluster", if (cluster == null || cluster.title.get.isBlank) "Noch kein Cluster gewaehlt" else cluster.title.get)
            summaryLine("Widersprueche", if (cluster == null) "0" else cluster.contradictionCount.get.toString)
            summaryLine("Fragen", if (cluster == null) "0" else cluster.questionCount.get.toString)
            summaryLine("Uebernommen", if (cluster == null) "0" else cluster.acceptedCount.get.toString)
            summaryLine("Verworfen", if (cluster == null) "0" else cluster.rejectedCount.get.toString)

            input("clusterSummary") {
              placeholder = "Verdichtungsnotiz"
              inputType = "text"
              subscribeBidirectional(clusterSummaryProperty, stringValueProperty)
            }

            actionButton("Mit Cluster verbinden") {
              addSelectedCandidateToCluster()
            }
            actionButton("Verdichtungsnotiz schreiben") {
              writeSelectedClusterSummary()
            }
          }
        }
      }
    }
  }

  private def panel(title: String)(content: => Unit)(using DslContext): Unit = {
    div {
      classes = "glass-border"
      style {
        padding = "18px"
        borderRadius = "18px"
        background = "rgba(255,255,255,0.02)"
      }

      vbox {
        style {
          rowGap = "14px"
        }

        span {
          style {
            fontWeight = "600"
            fontSize = "18px"
          }
          text = title
        }

        content
      }
    }
  }

  private def filterButton(label: String, value: String, property: Property[String])(using DslContext): Unit = {
    button(label) {
      buttonType = "button"
      classes = "home-page__button home-page__button--ghost"
      style {
        justifyContent = "flex-start"
      }
      onClick(_ => property.set(value))
    }
  }

  private def actionButton(label: String)(action: => Unit)(using DslContext): Unit = {
    button(label) {
      buttonType = "button"
      classes = "home-page__button home-page__button--secondary"
      onClick(_ => action)
    }
  }

  private def summaryLine(label: String, value: String)(using DslContext): Unit = {
    vbox {
      style {
        rowGap = "4px"
      }

      span {
        style {
          fontSize = "12px"
          opacity = "0.66"
        }
        text = label
      }

      span {
        text = value
      }
    }
  }

  private def classifySelectedCandidate(nextType: String): Unit =
    Option(selectedCandidateProperty.get)
      .map(_.data)
      .foreach { candidate =>
        candidate.classify(nextType).foreach { updated =>
          selectedCandidateProperty.set(updated)
          RemoteTableList.reloadFirstPage(candidatesProperty, pageSize = pageSize)
        }
      }

  private def assignSelectedTarget(): Unit =
    Option(selectedCandidateProperty.get)
      .map(_.data)
      .zip(Option(selectedDocumentProperty.get).map(_.data))
      .headOption
      .foreach { case (candidate, document) =>
        val sectionId =
          Option(selectedSectionProperty.get)
            .map(_.trim)
            .filter(_.nonEmpty)
            .orNull

        candidate.assignTarget(document.id.get.toString, sectionId).foreach { updated =>
          selectedCandidateProperty.set(updated)
          RemoteTableList.reloadFirstPage(candidatesProperty, pageSize = pageSize)
        }
      }

  private def addSelectedCandidateToCluster(): Unit =
    Option(selectedClusterProperty.get)
      .map(_.data)
      .zip(Option(selectedCandidateProperty.get).map(_.data))
      .headOption
      .foreach { case (cluster, candidate) =>
        cluster.addCandidate(candidate.id.get.toString).foreach { updated =>
          selectedClusterProperty.set(updated)
          RemoteTableList.reloadFirstPage(clustersProperty, pageSize = pageSize)
        }
      }

  private def writeSelectedClusterSummary(): Unit =
    Option(selectedClusterProperty.get)
      .map(_.data)
      .foreach { cluster =>
        cluster.writeSummary(clusterSummaryProperty.get).foreach { updated =>
          selectedClusterProperty.set(updated)
          RemoteTableList.reloadFirstPage(clustersProperty, pageSize = pageSize)
        }
      }

  private def acceptSelectedCluster(): Unit =
    updateSelectedCluster(_.accept())

  private def deferSelectedCluster(): Unit =
    updateSelectedCluster(_.defer())

  private def rejectSelectedCluster(): Unit =
    updateSelectedCluster(_.reject())

  private def updateSelectedCluster(action: CurationCluster => scala.concurrent.Future[Data[CurationCluster]]): Unit =
    Option(selectedClusterProperty.get)
      .map(_.data)
      .foreach { cluster =>
        action(cluster).foreach { updated =>
          selectedClusterProperty.set(updated)
          RemoteTableList.reloadFirstPage(clustersProperty, pageSize = pageSize)
          RemoteTableList.reloadFirstPage(candidatesProperty, pageSize = pageSize)
        }
      }

  private def createCluster(): Unit = {
    val title = Option(newClusterTitleProperty.get).map(_.trim).getOrElse("")
    if (title.nonEmpty) {
      CurationCluster.create(title).foreach { created =>
        selectedClusterProperty.set(created)
        newClusterTitleProperty.set("")
        RemoteTableList.reloadFirstPage(clustersProperty, pageSize = pageSize)
      }
    }
  }

  private def clusterCard(entry: Data[CurationCluster] | Null)(using Scope) =
    if (entry == null) {
      CompositeSupport.buildComposite(new EmptyCard("Cluster werden geladen"))
    } else {
      CompositeSupport.buildComposite(new ClusterCard(entry, selectedClusterProperty))
    }

  private def candidateCard(entry: Data[CurationCandidate] | Null)(using Scope) =
    if (entry == null) {
      CompositeSupport.buildComposite(new EmptyCard("Kandidaten werden geladen"))
    } else {
      CompositeSupport.buildComposite(new CandidateCard(entry, selectedCandidateProperty))
    }
}

object CurationPage {
  def curationPage(
    statusFilterProperty: Property[String],
    typeFilterProperty: Property[String],
    candidatesProperty: RemoteListProperty[Data[CurationCandidate], RemotePageQuery],
    clustersProperty: RemoteListProperty[Data[CurationCluster], RemotePageQuery],
    documentsProperty: RemoteListProperty[Data[Document], RemotePageQuery]
  )(using Scope): CurationPage =
    CompositeSupport.buildPage(new CurationPage(statusFilterProperty, typeFilterProperty, candidatesProperty, clustersProperty, documentsProperty))
}

private final class EmptyCard(label: String) extends DivComposite {
  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"
    style {
      minHeight = "80px"
      padding = "14px"
      borderRadius = "14px"
    }
    span {
      text = label
    }
  }
}

private final class ClusterCard(entry: Data[CurationCluster], selected: Property[Data[CurationCluster] | Null]) extends DivComposite {
  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"
    style {
      padding = "14px"
      borderRadius = "14px"
      cursor = "pointer"
    }
    element.onclick = _ => selected.set(entry)

    vbox {
      style {
        rowGap = "8px"
      }

      span {
        style {
          fontWeight = "600"
        }
        text = if (entry.data.title.get.isBlank) "Offener Cluster" else entry.data.title.get
      }

      span {
        text = s"${entry.data.contradictionCount.get} Widersprueche, ${entry.data.questionCount.get} Fragen"
      }
    }
  }
}

private final class CandidateCard(entry: Data[CurationCandidate], selected: Property[Data[CurationCandidate] | Null]) extends DivComposite {
  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"
    style {
      padding = "16px"
      borderRadius = "16px"
      cursor = "pointer"
    }
    element.onclick = _ => selected.set(entry)

    vbox {
      style {
        rowGap = "10px"
      }

      hbox {
        style {
          columnGap = "8px"
          alignItems = "center"
        }

        span {
          style {
            fontWeight = "600"
          }
          text = entry.data.resonanceType.get
        }

        span {
          style {
            opacity = "0.66"
          }
          text = entry.data.status.get
        }
      }

      span {
        text = Option(entry.data.title.get).filter(_.trim.nonEmpty).getOrElse("Unbenannter Impuls")
      }

      span {
        text = entry.data.excerpt.get
      }
    }
  }
}

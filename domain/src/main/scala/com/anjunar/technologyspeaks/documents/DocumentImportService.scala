package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.security.IdentityHolder
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Using

@Service
class DocumentImportService(val identityHolder: IdentityHolder) {

  def importDirectory(request: DocumentImportService.ImportRequest): DocumentImportService.ImportResult = {
    val directory = validateDirectory(request.path)
    val matches = findImportFiles(directory)

    val result = new DocumentImportService.ImportResult
    result.path = directory.toAbsolutePath.normalize.toString
    result.files = matches.size

    matches.foreach { file =>
      val item = importFile(file, request.overwriteExisting)
      result.items.add(item)

      item.status match {
        case "created" => result.created += 1
        case "updated" => result.updated += 1
        case "skipped" => result.skipped += 1
        case _ => ()
      }
    }

    result
  }

  private def validateDirectory(rawPath: String): Path = {
    val normalized = Option(rawPath).map(_.trim).getOrElse("")
    if (normalized.isEmpty) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import path is required")
    }

    val directory = Path.of(normalized).toAbsolutePath.normalize

    if (!Files.exists(directory)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, s"Directory not found: $directory")
    }
    if (!Files.isDirectory(directory)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, s"Path is not a directory: $directory")
    }

    directory
  }

  private def findImportFiles(directory: Path): Seq[Path] =
    Using.resource(Files.walk(directory)) { stream =>
      stream.iterator().asScala
        .filter(Files.isRegularFile(_))
        .filter(path => path.getFileName.toString.equalsIgnoreCase("Text.md"))
        .toVector
        .sortBy(_.toString.toLowerCase)
    }

  private def importFile(file: Path, overwriteExisting: Boolean): DocumentImportService.ImportItem = {
    val item = new DocumentImportService.ImportItem
    item.path = file.toAbsolutePath.normalize.toString
    item.title = deriveTitle(file)
    item.bookname = deriveBookname(file)

    val existing = Document.query("title" -> item.title)

    if (existing != null && !overwriteExisting) {
      item.status = "skipped"
      item.message = "Document with same title already exists"
      item.id = existing.id.toString
      return item
    }

    val markdown = Files.readString(file, StandardCharsets.UTF_8)
    val editor = MarkdownToDocumentNode.convert(markdown)

    val document =
      if (existing != null) existing
      else new Document(item.title)

    document.title = item.title
    document.bookname = item.bookname
    document.editor = editor

    if (document.user == null) {
      document.user = identityHolder.user
    }

    if (existing == null) {
      document.persist()
      item.status = "created"
      item.message = "Document created"
    } else {
      if (identityHolder.user != existing.user && !identityHolder.hasRole("Administrator")) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, s"Cannot overwrite document owned by another user: ${item.title}")
      }
      item.status = "updated"
      item.message = "Document updated"
    }

    item.id = document.id.toString
    item
  }

  private def deriveTitle(file: Path): String = {
    val fileName = file.getFileName.toString
    val dotIndex = fileName.lastIndexOf('.')
    val baseName =
      if (dotIndex > 0) fileName.substring(0, dotIndex).trim
      else fileName.trim

    if (baseName.equalsIgnoreCase("Text")) {
      val segments =
        Option(file.getParent)
          .flatMap(path => Option(path.getFileName))
          .map(_.toString.trim)
          .filter(_.nonEmpty)

      segments.getOrElse(baseName)
    } else {
      baseName
    }
  }

  private def deriveBookname(file: Path): String =
    Option(file.getParent)
      .flatMap(path => Option(path.getParent))
      .flatMap(path => Option(path.getFileName))
      .map(_.toString.trim)
      .filter(_.nonEmpty)
      .orNull
}

object DocumentImportService {

  class ImportRequest {
    var path: String = null
    var overwriteExisting: Boolean = false
  }

  class ImportResult {
    var path: String = null
    var files: Int = 0
    var created: Int = 0
    var updated: Int = 0
    var skipped: Int = 0
    val items: java.util.List[ImportItem] = new java.util.ArrayList[ImportItem]()
  }

  class ImportItem {
    var path: String = null
    var title: String = null
    var bookname: String = null
    var id: String = null
    var status: String = null
    var message: String = null
  }
}

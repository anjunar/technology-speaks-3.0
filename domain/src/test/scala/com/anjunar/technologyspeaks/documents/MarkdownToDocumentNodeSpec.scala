package com.anjunar.technologyspeaks.documents

import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters.*

class MarkdownToDocumentNodeSpec extends AnyFunSuite {

  test("convert creates prose mirror document with headings, paragraphs, lists and links") {
    val markdown =
      """# Titel
        |
        |Ein *Text* mit **starkem** [Link](/ziel).
        |
        |- Eins
        |- Zwei
        |""".stripMargin

    val result = MarkdownToDocumentNode.convert(markdown)

    assert(result.nodeType == "doc")
    assert(result.content.size() == 3)

    val heading = result.content.get(0)
    assert(heading.nodeType == "heading")
    assert(heading.attrs.get("level") == 1)
    assert(heading.content.get(0).text == "Titel")

    val paragraph = result.content.get(1)
    assert(paragraph.nodeType == "paragraph")
    val paragraphTexts = paragraph.content.asScala.filter(_.nodeType == "text").toVector
    assert(paragraphTexts.exists(_.text == "Ein "))
    assert(paragraphTexts.exists(node => node.text == "Text" && node.marks.asScala.exists(_.nodeType == "em")))
    assert(paragraphTexts.exists(node => node.text == "starkem" && node.marks.asScala.exists(_.nodeType == "strong")))
    assert(paragraphTexts.exists(node => node.text == "Link" && node.marks.asScala.exists(mark => mark.nodeType == "link" && mark.attrs.get("href") == "/ziel")))

    val list = result.content.get(2)
    assert(list.nodeType == "bullet_list")
    assert(list.content.size() == 2)
    assert(list.content.get(0).nodeType == "list_item")
    assert(list.content.get(0).content.get(0).nodeType == "paragraph")
  }
}

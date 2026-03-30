package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.shared.editor.Node
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Document as MarkdownDocument, Node as MarkdownNode}
import com.vladsch.flexmark.util.sequence.BasedSequence

import java.util
import scala.jdk.CollectionConverters.*

object MarkdownToDocumentNode {

  private val parser = Parser.builder().build()

  def convert(markdown: String): Node = {
    val document = node("doc")
    val parsed = parser.parse(Option(markdown).getOrElse(""))

    appendChildren(document, parsed)

    if (document.content.isEmpty) {
      document.content.add(paragraph(text("")))
    }

    document
  }

  private def appendChildren(parent: Node, markdownParent: MarkdownNode): Unit = {
    var child = markdownParent.getFirstChild

    while (child != null) {
      convertBlock(child).foreach(parent.content.add)
      child = child.getNext
    }
  }

  private def convertBlock(markdownNode: MarkdownNode): Option[Node] =
    markdownNode match {
      case paragraphNode: Paragraph =>
        Some(paragraph(convertInlineChildren(paragraphNode)*))

      case headingNode: Heading =>
        Some(
          block("heading", Map("level" -> Int.box(headingNode.getLevel)), convertInlineChildren(headingNode)*)
        )

      case bulletList: BulletList =>
        Some(list("bullet_list", bulletList))

      case orderedList: OrderedList =>
        Some(
          list(
            "ordered_list",
            orderedList,
            Map("order" -> Int.box(math.max(1, orderedList.getStartNumber)))
          )
        )

      case blockQuote: BlockQuote =>
        Some(container("blockquote", blockQuote))

      case fencedCode: FencedCodeBlock =>
        val attrs =
          Option(fencedCode.getInfo)
            .map(_.toString.trim)
            .filter(_.nonEmpty)
            .map(language => Map("params" -> language))
            .getOrElse(Map.empty)
        Some(block("code_block", attrs, text(trimTrailingNewline(fencedCode.getContentChars))))

      case indentedCode: IndentedCodeBlock =>
        Some(block("code_block", text(trimTrailingNewline(indentedCode.getContentChars))))

      case _: ThematicBreak =>
        Some(node("horizontal_rule"))

      case document: MarkdownDocument =>
        val root = node("doc")
        appendChildren(root, document)
        Some(root)

      case other if isIgnorableBlock(other) =>
        None

      case fallback =>
        val inlineNodes = convertInlineChildren(fallback)
        if (inlineNodes.nonEmpty) Some(paragraph(inlineNodes*))
        else {
          val plain = fallback.getChars.toString.trim
          if (plain.nonEmpty) Some(paragraph(text(plain)))
          else None
        }
    }

  private def list(nodeType: String, markdownList: MarkdownNode, attrs: Map[String, Any] = Map.empty): Node = {
    val listNode = block(nodeType, attrs)
    var child = markdownList.getFirstChild

    while (child != null) {
      child match {
        case listItem: ListItem =>
          listNode.content.add(container("list_item", listItem))
        case _ =>
          convertBlock(child).foreach(listNode.content.add)
      }
      child = child.getNext
    }

    listNode
  }

  private def container(nodeType: String, markdownNode: MarkdownNode): Node = {
    val current = block(nodeType)
    appendChildren(current, markdownNode)
    current
  }

  private def convertInlineChildren(markdownNode: MarkdownNode, inheritedMarks: Seq[Node] = Seq.empty): Seq[Node] = {
    val result = Vector.newBuilder[Node]
    var child = markdownNode.getFirstChild

    while (child != null) {
      result ++= convertInline(child, inheritedMarks)
      child = child.getNext
    }

    result.result()
  }

  private def convertInline(markdownNode: MarkdownNode, inheritedMarks: Seq[Node]): Seq[Node] =
    markdownNode match {
      case textNode: Text =>
        Seq(text(textNode.getChars.toString, inheritedMarks))

      case codeNode: Code =>
        Seq(text(codeNode.getText.toString, inheritedMarks :+ mark("code")))

      case _: SoftLineBreak =>
        Seq(text("\n", inheritedMarks))

      case _: HardLineBreak =>
        Seq(node("hard_break"))

      case emphasis: Emphasis =>
        convertInlineChildren(emphasis, inheritedMarks :+ mark("em"))

      case strong: StrongEmphasis =>
        convertInlineChildren(strong, inheritedMarks :+ mark("strong"))

      case linkNode: Link =>
        val href = Option(linkNode.getUrl).map(_.toString).getOrElse("")
        val title = Option(linkNode.getTitle).map(_.toString).filter(_.nonEmpty).orNull
        val attrs =
          if (title == null) Map("href" -> href)
          else Map("href" -> href, "title" -> title)
        convertInlineChildren(linkNode, inheritedMarks :+ mark("link", attrs))

      case _: HtmlEntity =>
        Seq(text(markdownNode.getChars.unescape.toString, inheritedMarks))

      case _: HtmlInline | _: HtmlInlineComment =>
        val plain = markdownNode.getChars.toString.trim
        if (plain.nonEmpty) Seq(text(plain, inheritedMarks)) else Seq.empty

      case other if isIgnorableInline(other) =>
        Seq.empty

      case other if other.getFirstChild != null =>
        convertInlineChildren(other, inheritedMarks)

      case other =>
        val plain = other.getChars.toString
        if (plain.nonEmpty) Seq(text(plain, inheritedMarks)) else Seq.empty
    }

  private def node(nodeType: String): Node = {
    val result = new Node
    result.nodeType = nodeType
    result.content = new util.ArrayList[Node]()
    result.attrs = new util.HashMap[String, Any]()
    result.marks = new util.ArrayList[Node]()
    result
  }

  private def block(nodeType: String, content: Node*): Node =
    block(nodeType, Map.empty, content*)

  private def block(nodeType: String, attrs: Map[String, Any], content: Node*): Node = {
    val current = node(nodeType)
    attrs.foreach { case (key, value) => current.attrs.put(key, value) }
    content.foreach(current.content.add)
    current
  }

  private def paragraph(content: Node*): Node =
    block("paragraph", content*)

  private def text(value: String, marks: Seq[Node] = Seq.empty): Node = {
    val current = node("text")
    current.text = Option(value).getOrElse("")
    marks.foreach(current.marks.add)
    current
  }

  private def mark(nodeType: String, attrs: Map[String, Any] = Map.empty): Node = {
    val current = node(nodeType)
    attrs.foreach { case (key, value) => current.attrs.put(key, value) }
    current
  }

  private def trimTrailingNewline(sequence: BasedSequence): String =
    sequence.toString.stripSuffix("\n").stripSuffix("\r")

  private def isIgnorableBlock(markdownNode: MarkdownNode): Boolean =
    markdownNode.getChars.toString.trim.isEmpty

  private def isIgnorableInline(markdownNode: MarkdownNode): Boolean =
    markdownNode.isInstanceOf[SoftLineBreak] && markdownNode.getChars.length() == 0
}

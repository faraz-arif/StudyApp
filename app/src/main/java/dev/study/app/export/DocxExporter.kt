package dev.study.app.export

import dev.study.app.domain.model.BlockType
import dev.study.app.domain.model.Note
import dev.study.app.domain.model.RichTextDocument
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxExporter {

    fun exportNoteToDocx(note: Note, outputStream: OutputStream): Result<Unit> {
        return try {
            val zip = ZipOutputStream(outputStream)

            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypesXml.toByteArray())
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(relsXml.toByteArray())
            zip.closeEntry()

            // 3. word/_rels/document.xml.rels
            zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zip.write(documentRelsXml.toByteArray())
            zip.closeEntry()

            // 4. word/document.xml
            val docXml = generateDocumentXml(note)
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(docXml.toByteArray())
            zip.closeEntry()

            zip.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { outputStream.close() } catch (ignored: Exception) {}
        }
    }

    private fun generateDocumentXml(note: Note): String {
        val doc = try {
            Json.decodeFromString<RichTextDocument>(note.contentJson)
        } catch (e: Exception) {
            RichTextDocument(emptyList())
        }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        // Add Note Title as Heading 1
        sb.append("<w:p>")
        sb.append("<w:pPr><w:pStyle w:val=\"Heading1\"/><w:b/></w:pPr>")
        sb.append("<w:r><w:t>${escapeXml(note.title)}</w:t></w:r>")
        sb.append("</w:p>")

        doc.blocks.forEach { block ->
            sb.append("<w:p>")
            
            // Format properties
            val style = when (block.type) {
                BlockType.HEADING -> "<w:pPr><w:pStyle w:val=\"Heading2\"/><w:b/></w:pPr>"
                BlockType.BULLET_ITEM -> "<w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr>"
                BlockType.NUMBERED_ITEM -> "<w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"2\"/></w:numPr></w:pPr>"
                BlockType.CHECKBOX_ITEM -> {
                    val status = if (block.isChecked == true) "[X] " else "[ ] "
                    "<w:pPr><w:rPr><w:b w:val=\"false\"/></w:rPr></w:pPr>"
                }
                else -> ""
            }
            sb.append(style)

            // In our block document, run support allows custom inline bold/italic
            if (block.runs.isNotEmpty()) {
                block.runs.forEach { run ->
                    sb.append("<w:r>")
                    val rPr = StringBuilder("<w:rPr>")
                    if (run.bold) rPr.append("<w:b/>")
                    if (run.italic) rPr.append("<w:i/>")
                    if (run.underline) rPr.append("<w:u w:val=\"single\"/>")
                    rPr.append("</w:rPr>")
                    sb.append(rPr.toString())
                    sb.append("<w:t>${escapeXml(run.text)}</w:t>")
                    sb.append("</w:r>")
                }
            } else {
                val prefix = if (block.type == BlockType.CHECKBOX_ITEM) {
                    if (block.isChecked == true) "[X] " else "[ ] "
                } else ""
                sb.append("<w:r>")
                sb.append("<w:t>${escapeXml(prefix + block.text)}</w:t>")
                sb.append("</w:r>")
            }

            sb.append("</w:p>")
        }

        sb.append("</w:body>")
        sb.append("</w:document>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private val documentRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""
}

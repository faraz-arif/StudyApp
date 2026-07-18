package dev.study.app.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dev.study.app.domain.model.BlockType
import dev.study.app.domain.model.Note
import dev.study.app.domain.model.RichTextDocument
import kotlinx.serialization.json.Json
import java.io.OutputStream

class PdfExporter {

    fun exportNoteToPdf(note: Note, outputStream: OutputStream): Result<Unit> {
        return try {
            val document = PdfDocument()
            // Standard letter/A4 style page size: 595 x 842 points (A4 size)
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val paintText = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            val paintTitle = Paint().apply {
                color = Color.BLACK
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val paintHeader = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var yPosition = 50f
            val margin = 40f
            val lineSpacing = 20f
            val printableWidth = pageWidth - (margin * 2)

            // Draw Note Title
            canvas.drawText(note.title, margin, yPosition, paintTitle)
            yPosition += 40f

            val doc = try {
                Json.decodeFromString<RichTextDocument>(note.contentJson)
            } catch (e: Exception) {
                RichTextDocument(emptyList())
            }

            doc.blocks.forEach { block ->
                val lines = mutableListOf<String>()

                // Split long lines of text to fit print boundaries
                val textToWrap = when (block.type) {
                    BlockType.BULLET_ITEM -> "• ${block.text}"
                    BlockType.NUMBERED_ITEM -> "1. ${block.text}"
                    BlockType.CHECKBOX_ITEM -> "[ ] ${block.text}"
                    else -> block.text
                }

                wrapText(textToWrap, paintText, printableWidth, lines)

                val paintToUse = if (block.type == BlockType.HEADING) paintHeader else paintText

                lines.forEach { line ->
                    if (yPosition > pageHeight - margin) {
                        // Overflow, start new page
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = margin + 20f
                    }
                    canvas.drawText(line, margin, yPosition, paintToUse)
                    yPosition += lineSpacing
                }
                yPosition += 10f // gap between blocks
            }

            document.finishPage(page)
            document.writeTo(outputStream)
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { outputStream.close() } catch (ignored: Exception) {}
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float, list: MutableList<String>) {
        var start = 0
        while (start < text.length) {
            var count = paint.breakText(text, start, text.length, true, maxWidth, null)
            if (count <= 0) break
            
            // Try to break at a space if possible
            if (start + count < text.length) {
                val lastSpace = text.substring(start, start + count).lastIndexOf(' ')
                if (lastSpace > 0) {
                    count = lastSpace + 1
                }
            }
            list.add(text.substring(start, start + count).trimEnd())
            start += count
        }
    }
}

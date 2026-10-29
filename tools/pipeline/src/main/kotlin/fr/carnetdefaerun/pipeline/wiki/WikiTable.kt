package fr.carnetdefaerun.pipeline.wiki

/** Cellule d'un tableau wiki : contenu brut (wikitexte), en-tête ou non, fusions. */
data class TableCell(val text: String, val header: Boolean, val rowspan: Int = 1, val colspan: Int = 1)

/**
 * Tableau wiki (`{| … |}`), lignes telles qu'écrites. [grid] déplie les fusions de cellules :
 * chaque ligne y a une cellule par colonne, une cellule fusionnée étant répétée.
 */
data class WikiTable(val caption: String?, val rows: List<List<TableCell>>) {

    fun grid(): List<List<TableCell>> {
        val out = mutableListOf<MutableList<TableCell?>>()
        // Cellules encore « ouvertes » vers le bas : colonne → (cellule, lignes restantes).
        val pending = mutableMapOf<Int, Pair<TableCell, Int>>()
        for (row in rows) {
            val line = mutableListOf<TableCell?>()
            var col = 0
            fun fillPending() {
                while (true) {
                    val (cell, left) = pending[col] ?: return
                    line += cell
                    if (left <= 1) pending.remove(col) else pending[col] = cell to left - 1
                    col++
                }
            }
            for (cell in row) {
                fillPending()
                repeat(cell.colspan) {
                    line += cell
                    if (cell.rowspan > 1) pending[col] = cell to cell.rowspan - 1
                    col++
                }
            }
            fillPending()
            // Fusions qui descendent au-delà de la dernière cellule écrite de la ligne.
            pending.keys.filter { it >= col }.sorted().forEach { c ->
                while (line.size < c) line += null
                val (cell, left) = pending.getValue(c)
                line += cell
                if (left <= 1) pending.remove(c) else pending[c] = cell to left - 1
            }
            out += line
        }
        return out.map { line -> line.map { it ?: TableCell("", header = false) } }
    }
}

object WikiTables {

    /** Tableaux de premier niveau du texte, dans l'ordre (un tableau imbriqué reste dans le texte de sa cellule). */
    fun parse(text: String): List<WikiTable> {
        val lines = Wikitext.stripComments(text).lines()
        val tables = mutableListOf<WikiTable>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].trimStart().startsWith("{|")) {
                val (table, next) = parseTable(lines, i)
                tables += table
                i = next
            } else {
                i++
            }
        }
        return tables
    }

    private fun parseTable(lines: List<String>, start: Int): Pair<WikiTable, Int> {
        var caption: String? = null
        val rows = mutableListOf<MutableList<TableCell>>()
        var current: MutableList<TableCell>? = null
        var i = start + 1
        var nested = 0
        val nestedText = StringBuilder()
        fun appendToLastCell(extra: String) {
            val row = current ?: return
            val last = row.removeLastOrNull() ?: return
            row += last.copy(text = (last.text + "\n" + extra).trim())
        }
        while (i < lines.size) {
            val line = lines[i]
            val t = line.trimStart()
            if (nested > 0) {
                // Tableau imbriqué : rattaché tel quel au texte de la cellule courante.
                nestedText.appendLine(line)
                if (t.startsWith("{|")) nested++
                if (t.startsWith("|}")) nested--
                if (nested == 0) { appendToLastCell(nestedText.toString().trimEnd()); nestedText.clear() }
                i++
                continue
            }
            when {
                t.startsWith("|}") -> return WikiTable(caption, rows) to i + 1
                t.startsWith("{|") -> { nested = 1; nestedText.appendLine(line) }
                t.startsWith("|+") -> caption = t.removePrefix("|+").let(::content).trim()
                t.startsWith("|-") -> { current = mutableListOf<TableCell>().also { rows += it } }
                t.startsWith("!") -> {
                    val row = current ?: mutableListOf<TableCell>().also { rows += it; current = it }
                    splitCells(t.substring(1), header = true).forEach { row += cell(it, header = true) }
                }
                t.startsWith("|") -> {
                    val row = current ?: mutableListOf<TableCell>().also { rows += it; current = it }
                    splitCells(t.substring(1), header = false).forEach { row += cell(it, header = false) }
                }
                t.isNotBlank() -> appendToLastCell(line)
            }
            i++
        }
        return WikiTable(caption, rows) to i
    }

    /** Cellules d'une ligne : séparées par `||` (ou `!!` dans un en-tête), hors liens et modèles. */
    private fun splitCells(body: String, header: Boolean): List<String> {
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var k = 0
        while (k < body.length) {
            val two = if (k + 1 < body.length) body.substring(k, k + 2) else ""
            when {
                two == "{{" || two == "[[" -> { depth++; cur.append(two); k += 2 }
                (two == "}}" || two == "]]") && depth > 0 -> { depth--; cur.append(two); k += 2 }
                depth == 0 && (two == "||" || (header && two == "!!")) -> { parts += cur.toString(); cur.clear(); k += 2 }
                else -> { cur.append(body[k]); k++ }
            }
        }
        parts += cur.toString()
        return parts
    }

    private fun cell(raw: String, header: Boolean): TableCell {
        val (attributes, text) = attributesAndContent(raw)
        fun span(name: String) = Regex("$name\\s*=\\s*\"?(\\d+)").find(attributes)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        return TableCell(text.trim(), header, span("rowspan"), span("colspan"))
    }

    private fun content(raw: String) = attributesAndContent(raw).second

    /**
     * Attributs et contenu d'une cellule : `style="…" | contenu`. La première barre de premier niveau
     * ne sépare des attributs que si ce qui la précède en a la forme (`nom=valeur`, sans lien ni modèle).
     */
    private fun attributesAndContent(raw: String): Pair<String, String> {
        var depth = 0
        var k = 0
        while (k < raw.length) {
            val two = if (k + 1 < raw.length) raw.substring(k, k + 2) else ""
            when {
                two == "{{" || two == "[[" -> { depth++; k += 2 }
                (two == "}}" || two == "]]") && depth > 0 -> { depth--; k += 2 }
                raw[k] == '|' && depth == 0 -> {
                    val before = raw.substring(0, k)
                    return if (ATTRIBUTES.matches(before.trim())) before to raw.substring(k + 1) else "" to raw
                }
                else -> k++
            }
        }
        return "" to raw
    }

    private val ATTRIBUTES = Regex("""(\s*[\w-]+\s*=\s*("[^"]*"|'[^']*'|[\w%#.-]+)\s*)*""")
}

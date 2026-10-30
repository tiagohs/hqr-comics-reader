package com.tiagohs.hqr.sources.opds

/** An XML element with its attributes, children and text. Names keep their prefix ("opds:price"); [local] drops it. */
internal class XmlNode(
    val name: String,
    val attributes: Map<String, String>,
    val children: MutableList<XmlNode> = mutableListOf(),
    val text: StringBuilder = StringBuilder(),
) {
    val local: String get() = name.substringAfter(':')

    fun child(local: String): XmlNode? = children.firstOrNull { it.local == local }

    fun all(local: String): List<XmlNode> = children.filter { it.local == local }

    /** An attribute by its local name (`pse:count` answers to "count"). */
    fun attr(local: String): String? = attributes[local] ?: attributes.entries.firstOrNull { it.key.substringAfter(':') == local }?.value

    fun textOf(local: String): String? =
        child(local)
            ?.text
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}

/**
 * A small XML reader for Atom feeds (OPDS 1.2 and OpenSearch descriptions): elements, attributes, text, CDATA and
 * entities. No DTDs nor external entities are ever read (RNF-26).
 */
internal object Xml {
    fun parse(text: String): XmlNode {
        val root = XmlNode("#document", emptyMap())
        val stack = ArrayDeque<XmlNode>().apply { addLast(root) }
        var i = 0
        while (i < text.length) {
            val lt = text.indexOf('<', i)
            if (lt < 0) {
                stack.last().text.append(decode(text.substring(i)))
                break
            }
            if (lt > i) stack.last().text.append(decode(text.substring(i, lt)))
            i =
                when {
                    text.startsWith("<!--", lt) -> end(text, "-->", lt)
                    text.startsWith("<![CDATA[", lt) -> {
                        val close = text.indexOf("]]>", lt).also { check(it >= 0) { "CDATA aberto" } }
                        stack.last().text.append(text, lt + CDATA_OPEN, close)
                        close + CDATA_CLOSE
                    }
                    text.startsWith("<?", lt) -> end(text, "?>", lt)
                    text.startsWith("<!", lt) -> end(text, ">", lt)
                    text.startsWith("</", lt) -> {
                        val close = end(text, ">", lt)
                        if (stack.size > 1) stack.removeLast()
                        close
                    }
                    else -> openTag(text, lt, stack)
                }
        }
        return root.children.firstOrNull() ?: throw IllegalArgumentException("XML sem elemento")
    }

    private fun end(
        text: String,
        token: String,
        from: Int,
    ): Int {
        val at = text.indexOf(token, from)
        require(at >= 0) { "XML incompleto" }
        return at + token.length
    }

    // A hand-written tokenizer: its branches are the grammar.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun openTag(
        text: String,
        lt: Int,
        stack: ArrayDeque<XmlNode>,
    ): Int {
        var i = lt + 1
        val nameStart = i
        while (i < text.length && !text[i].isWhitespace() && text[i] != '>' && text[i] != '/') i++
        val name = text.substring(nameStart, i)
        val attributes = LinkedHashMap<String, String>()
        while (i < text.length) {
            while (i < text.length && text[i].isWhitespace()) i++
            require(i < text.length) { "tag aberta" }
            when (text[i]) {
                '>' -> {
                    val node = XmlNode(name, attributes)
                    stack.last().children += node
                    stack.addLast(node)
                    return i + 1
                }
                '/' -> {
                    stack.last().children += XmlNode(name, attributes)
                    return end(text, ">", i)
                }
                else -> {
                    val keyStart = i
                    while (i < text.length && text[i] != '=' && !text[i].isWhitespace() && text[i] != '>') i++
                    val key = text.substring(keyStart, i)
                    while (i < text.length && text[i].isWhitespace()) i++
                    if (i < text.length && text[i] == '=') {
                        i++
                        while (i < text.length && text[i].isWhitespace()) i++
                        val quote = text[i]
                        require(quote == '"' || quote == '\'') { "atributo sem aspas" }
                        val close = text.indexOf(quote, i + 1)
                        require(close >= 0) { "atributo aberto" }
                        attributes[key] = decode(text.substring(i + 1, close))
                        i = close + 1
                    } else {
                        attributes[key] = ""
                    }
                }
            }
        }
        throw IllegalArgumentException("tag aberta")
    }

    fun decode(raw: String): String {
        if ('&' !in raw) return raw
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            val semi = if (c == '&') raw.indexOf(';', i) else -1
            if (semi < 0 || semi - i > MAX_ENTITY) {
                out.append(c)
                i++
                continue
            }
            val entity = raw.substring(i + 1, semi)
            val value =
                when {
                    entity == "amp" -> "&"
                    entity == "lt" -> "<"
                    entity == "gt" -> ">"
                    entity == "quot" -> "\""
                    entity == "apos" -> "'"
                    entity.startsWith("#x") -> entity.drop(2).toIntOrNull(HEX)?.let(::codePoint)
                    entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePoint)
                    else -> null
                }
            if (value == null) {
                out.append(c)
                i++
            } else {
                out.append(value)
                i = semi + 1
            }
        }
        return out.toString()
    }

    private fun codePoint(cp: Int): String =
        if (cp < SUPPLEMENTARY) {
            cp.toChar().toString()
        } else {
            val v = cp - SUPPLEMENTARY
            charArrayOf(
                (HIGH_SURROGATE + (v shr SURROGATE_BITS)).toChar(),
                (LOW_SURROGATE + (v and SURROGATE_MASK)).toChar(),
            ).concatToString()
        }

    private const val CDATA_OPEN = 9
    private const val CDATA_CLOSE = 3
    private const val MAX_ENTITY = 10
    private const val HEX = 16
    private const val SUPPLEMENTARY = 0x10000
    private const val HIGH_SURROGATE = 0xD800
    private const val LOW_SURROGATE = 0xDC00
    private const val SURROGATE_BITS = 10
    private const val SURROGATE_MASK = 0x3FF
}

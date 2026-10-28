package fr.carnetdefaerun.core.model.content

/**
 * Schéma de la base de contenu embarquée (`content.db`, lecture seule), partagé par le pipeline
 * qui la crée (JDBC) et par l'appli qui la lit (SQLite d'Android). Toute modification du schéma
 * incrémente [VERSION] : l'appli remplace alors sa copie de la base.
 *
 * Chaque ligne porte son étiquette de spoiler en colonnes (rang de l'acte et des niveaux) :
 * les écrans ne lisent jamais ces tables directement, le dépôt filtre par `SpoilerGate`.
 */
object ContentSchema {
    const val VERSION = 2
    const val FILE_NAME = "content.db"

    object Entry {
        const val TABLE = "entry"
        const val ID = "id"
        const val TYPE = "type"
        const val SUBTYPE = "subtype"
        const val NAME_FR = "name_fr"
        const val NAME_EN = "name_en"
        const val NAME_STATUS = "name_status"
        const val SORT_KEY = "sort_key"
        const val ACT = "act"
        const val LEVEL = "level"
        const val OTHER_WORKS = "other_works"
        const val TITLE_ACT = "title_act"
        const val TITLE_LEVEL = "title_level"
        const val TITLE_OTHER_WORKS = "title_other_works"
        const val PAYLOAD = "payload"
    }

    object Link {
        const val TABLE = "link"
        const val SOURCE = "source"
        const val TARGET = "target"
        const val RELATION = "relation"
        const val ACT = "act"
        const val LEVEL = "level"
        const val OTHER_WORKS = "other_works"
    }

    object SearchName {
        const val TABLE = "search_name"
        const val ENTRY_ID = "entry_id"
        const val FORM = "form"
        const val KIND = "kind"
    }

    object SearchText {
        const val TABLE = "search_text"
    }

    object Reference {
        const val TABLE = "reference"
        const val ID = "id"
        const val PAYLOAD = "payload"
    }

    object Meta {
        const val TABLE = "meta"
        const val KEY = "key"
        const val VALUE = "value"
        const val SCHEMA_VERSION = "schema_version"
        const val GENERATED = "generated"
        const val GAME_VERSION = "game_version"
        const val ENTRY_COUNT = "entry_count"
    }

    val CREATE: List<String> = listOf(
        """CREATE TABLE ${Entry.TABLE} (
            ${Entry.ID} TEXT PRIMARY KEY NOT NULL,
            ${Entry.TYPE} TEXT NOT NULL,
            ${Entry.SUBTYPE} TEXT,
            ${Entry.NAME_FR} TEXT NOT NULL,
            ${Entry.NAME_EN} TEXT NOT NULL,
            ${Entry.NAME_STATUS} TEXT NOT NULL,
            ${Entry.SORT_KEY} TEXT NOT NULL,
            ${Entry.ACT} INTEGER NOT NULL,
            ${Entry.LEVEL} INTEGER NOT NULL,
            ${Entry.OTHER_WORKS} INTEGER NOT NULL,
            ${Entry.TITLE_ACT} INTEGER NOT NULL,
            ${Entry.TITLE_LEVEL} INTEGER NOT NULL,
            ${Entry.TITLE_OTHER_WORKS} INTEGER NOT NULL,
            ${Entry.PAYLOAD} TEXT NOT NULL
        ) WITHOUT ROWID""",
        "CREATE INDEX entry_by_type ON ${Entry.TABLE}(${Entry.TYPE}, ${Entry.SORT_KEY})",
        """CREATE TABLE ${Link.TABLE} (
            ${Link.SOURCE} TEXT NOT NULL,
            ${Link.TARGET} TEXT NOT NULL,
            ${Link.RELATION} TEXT NOT NULL,
            ${Link.ACT} INTEGER NOT NULL,
            ${Link.LEVEL} INTEGER NOT NULL,
            ${Link.OTHER_WORKS} INTEGER NOT NULL,
            PRIMARY KEY (${Link.SOURCE}, ${Link.TARGET}, ${Link.RELATION})
        ) WITHOUT ROWID""",
        "CREATE INDEX link_by_target ON ${Link.TABLE}(${Link.TARGET})",
        """CREATE TABLE ${SearchName.TABLE} (
            ${SearchName.ENTRY_ID} TEXT NOT NULL,
            ${SearchName.FORM} TEXT NOT NULL,
            ${SearchName.KIND} TEXT NOT NULL
        )""",
        "CREATE INDEX search_name_by_form ON ${SearchName.TABLE}(${SearchName.FORM})",
        // Texte intégral : FTS4, accents ignorés ; l'étiquette de chaque bloc est stockée hors index.
        """CREATE VIRTUAL TABLE ${SearchText.TABLE} USING fts4(
            entry_id, block_id, act, level, other_works, text,
            notindexed=entry_id, notindexed=block_id, notindexed=act, notindexed=level, notindexed=other_works,
            tokenize=unicode61 "remove_diacritics=2"
        )""",
        """CREATE TABLE ${Reference.TABLE} (
            ${Reference.ID} TEXT PRIMARY KEY NOT NULL,
            ${Reference.PAYLOAD} TEXT NOT NULL
        ) WITHOUT ROWID""",
        """CREATE TABLE ${Meta.TABLE} (
            ${Meta.KEY} TEXT PRIMARY KEY NOT NULL,
            ${Meta.VALUE} TEXT NOT NULL
        ) WITHOUT ROWID""",
    )
}

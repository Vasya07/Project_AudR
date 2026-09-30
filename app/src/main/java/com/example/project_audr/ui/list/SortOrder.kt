package com.example.project_audr.ui.list

enum class SortOrder(val label: String) {
    DATE_NEWEST("Дата создания: новые"),
    DATE_OLDEST("Дата создания: старые"),
    TITLE_ASC("Название: От А до Я"),
    TITLE_DESC("Название: От Я до А"),
    DURATION_SHORT("Длительность: короткие"),
    DURATION_LONG("Длительность: длинные")
}
package com.example.ui.localization

object Strings {
    fun get(key: String, lang: String): String {
        val isArabic = lang == "ar"
        return when (key) {
            "app_title" -> if (isArabic) "لوحة قماشية للقصص" else "Story Canvas"
            "settings" -> if (isArabic) "الإعدادات" else "Settings"
            "language" -> if (isArabic) "اللغة / Language" else "Language"
            "dark_mode" -> if (isArabic) "الوضع الداكن" else "Dark Mode"
            "light_mode" -> if (isArabic) "الوضع الفاتح" else "Light Mode"
            "theme" -> if (isArabic) "المظهر (ليل / نهار)" else "Theme (Dark / Light)"
            "new_canvas" -> if (isArabic) "إنشاء مخطط قصة جديد" else "Create New Canvas"
            "my_projects" -> if (isArabic) "مشاريع القصص" else "Story Projects"
            "search_projects" -> if (isArabic) "البحث في المشاريع..." else "Search projects..."
            "project_title" -> if (isArabic) "عنوان المشروع" else "Project Title"
            "project_desc" -> if (isArabic) "وصف المشروع" else "Project Description"
            "cancel" -> if (isArabic) "إلغاء" else "Cancel"
            "create" -> if (isArabic) "إنشاء" else "Create"
            "add_node" -> if (isArabic) "إضافة صندوق" else "Add Box"
            "connect" -> if (isArabic) "ربط" else "Connect"
            "minimap" -> if (isArabic) "خريطة مصغرة" else "Minimap"
            "search_nodes" -> if (isArabic) "بحث في الصناديق..." else "Search nodes..."
            "node_title" -> if (isArabic) "عنوان الصندوق" else "Box Title"
            "node_content" -> if (isArabic) "المحتوى التفصيلي" else "Detailed Content"
            "node_type" -> if (isArabic) "نوع العنصر" else "Element Type"
            "tags" -> if (isArabic) "الوسوم (مفصولة بفواصل)" else "Tags (comma separated)"
            "color" -> if (isArabic) "اللون" else "Color"
            "sketch" -> if (isArabic) "رسم / سكتش" else "Sketch / Drawing"
            "open_subcanvas" -> if (isArabic) "فتح مساحة فرعية داخلية" else "Open Inner Sub-Canvas"
            "save" -> if (isArabic) "حفظ" else "Save"
            "delete" -> if (isArabic) "حذف" else "Delete"
            "duplicate" -> if (isArabic) "نسخ" else "Duplicate"
            "back" -> if (isArabic) "رجوع" else "Back"
            "chapter" -> if (isArabic) "فصل" else "Chapter"
            "event" -> if (isArabic) "حدث" else "Event"
            "character" -> if (isArabic) "شخصية" else "Character"
            "idea" -> if (isArabic) "فكرة" else "Idea"
            "conclusion" -> if (isArabic) "خاتمة" else "Conclusion"
            "select_connect_target" -> if (isArabic) "اختر الصندوق المراد ربطه" else "Select target box to connect"
            "sample_project_title" -> if (isArabic) "ملحمة زمن الكرونوس" else "The Chronos Ring Epic"
            "sample_project_desc" -> if (isArabic) "مخطط خيالي لقصة السفر عبر الزمن والإنقاذ." else "A time-travel sci-fi thriller plot map."
            else -> key
        }
    }
}

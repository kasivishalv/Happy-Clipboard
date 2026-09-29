package com.example.data.model

enum class ContentType(val displayName: String) {
    TEXT("Text"),
    URL("Link"),
    EMAIL("Email"),
    PHONE("Phone"),
    CODE("Code"),
    COLOR("Color"),
    NUMBER("Number"),
    SENSITIVE("Sensitive"),
    IMAGE("Image")
}

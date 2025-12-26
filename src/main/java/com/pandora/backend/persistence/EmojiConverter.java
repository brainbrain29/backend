package com.pandora.backend.persistence;

import com.pandora.backend.enums.Emoji;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public final class EmojiConverter implements AttributeConverter<Emoji, Integer> {

    @Override
    public Integer convertToDatabaseColumn(final Emoji emoji) {
        if (emoji == null) {
            return Emoji.PEACE.getCode();
        }
        return emoji.getCode();
    }

    @Override
    public Emoji convertToEntityAttribute(final Integer dbValue) {
        if (dbValue == null) {
            return Emoji.PEACE;
        }
        return Emoji.fromCode(dbValue);
    }
}

package com.vgc.entity.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 이 컨버터가 다루는 POJO({@link PhotoBingoConfig}) 는 반드시 equals/hashCode 를 값 기반으로
 * 구현해야 한다. Hibernate 는 @Convert 매핑된 mutable 속성의 dirty-check 를 {@code Objects.equals}
 * 로 수행하는데, equals 가 없으면 레퍼런스 비교로 떨어져 매 로드마다 false dirty → spurious
 * UPDATE → FK 관련 락 경합으로 self-block 되는 버그가 재발한다.
 */
@Converter
public class PhotoBingoConfigConverter implements AttributeConverter<PhotoBingoConfig, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PhotoBingoConfig attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize PhotoBingoConfig", e);
        }
    }

    @Override
    public PhotoBingoConfig convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return MAPPER.readValue(dbData, PhotoBingoConfig.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize PhotoBingoConfig: " + dbData, e);
        }
    }
}

package com.support.mapper;

import com.support.dto.BulkUploadHistoryDTO;
import com.support.entity.BulkUploadHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BulkUploadHistoryMapper {

    @Mapping(target = "errors", expression = "java(splitErrors(entity.getErrorDetails()))")
    BulkUploadHistoryDTO toDTO(BulkUploadHistory entity);

    List<BulkUploadHistoryDTO> toDTOList(List<BulkUploadHistory> entities);

    default List<String> splitErrors(String errorDetails) {
        if (errorDetails == null || errorDetails.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(errorDetails.split("\\r?\\n"));
    }
}

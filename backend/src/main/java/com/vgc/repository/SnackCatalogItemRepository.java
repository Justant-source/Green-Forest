package com.vgc.repository;

import com.vgc.entity.SnackCatalogItem;
import com.vgc.entity.SnackSsgCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SnackCatalogItemRepository extends JpaRepository<SnackCatalogItem, Long> {
    Optional<SnackCatalogItem> findBySsgItemIdAndCategory(String ssgItemId, SnackSsgCategory category);

    List<SnackCatalogItem> findByCategory(SnackSsgCategory category);

    @Query("select distinct i.category from SnackCatalogItem i "
            + "where i.available = true and i.hiddenFromUsers = false")
    List<SnackSsgCategory> findVisibleCategories();

    @Query("select i from SnackCatalogItem i where i.available = true and i.hiddenFromUsers = false "
            + "and (:category is null or i.category = :category) "
            + "and (:q = '' or lower(i.name) like lower(concat('%', :q, '%')))")
    Page<SnackCatalogItem> findVisibleCatalog(@Param("q") String q,
                                               @Param("category") SnackSsgCategory category,
                                               Pageable pageable);

    @Query("select i from SnackCatalogItem i where "
            + "exists (select r from SnackRequest r where r.catalogItem = i "
            + "and r.status = com.vgc.entity.RequestStatus.PENDING) "
            + "and (:category is null or i.category = :category) "
            + "and (:q = '' or lower(i.name) like lower(concat('%', :q, '%')))")
    Page<SnackCatalogItem> findRequestedCatalog(@Param("q") String q,
                                               @Param("category") SnackSsgCategory category,
                                               Pageable pageable);

    @Query("select i from SnackCatalogItem i where (:includeUnavailable = true or i.available = true) "
            + "and (:category is null or i.category = :category) "
            + "and (:q = '' or lower(i.name) like lower(concat('%', :q, '%')))")
    Page<SnackCatalogItem> findAdminCatalog(@Param("q") String q,
                                            @Param("includeUnavailable") boolean includeUnavailable,
                                            @Param("category") SnackSsgCategory category,
                                            Pageable pageable);
}
